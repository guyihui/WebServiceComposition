package com.sjtu.composition.graph.executionPath;

import com.alibaba.fastjson.JSONObject;
import com.sjtu.composition.graph.serviceGraph.ParamNode;
import com.sjtu.composition.graph.serviceGraph.ServiceNode;
import com.sjtu.composition.serviceUtils.Parameter;
import javafx.util.Pair;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;


public class ExecutionPath implements Cloneable {

    // 路径图表示，需要实现 clone
    private ServiceNode sourceNode;
    private ServiceNode sinkNode;
    private Set<ServiceNode> serviceNodeSet = new HashSet<>();
    private Map<ParamNode, Map<ParamNode, Double>> matchEdgeMap = new HashMap<>();// 选取的匹配边
    private Map<ParamNode, Integer> responseTimeBound = new HashMap<>();
    // 构造过程中的辅助结构，需要 clone
    private Map<ParamNode, Set<ServiceNode>> unresolvedInput = new HashMap<>();//当前为解决的输入=该输入后续可达服务（用以检查环）
    // 编排辅助结构：服务依赖关系，需要clone
    private Map<ServiceNode, Integer> dependencyCountMap = new HashMap<>();

    // 路径图参数，super.clone()
    private double similarityFloor = 1.0;
    private boolean allResolved = false;

    // 服务编排，不需要 clone
    private Map<ServiceNode, CompletableFuture> serviceNodeCompletableFutureMap = new HashMap<>();
    private Map<Parameter, Object> parameterObjectMap;


    public ExecutionPath(ServiceNode sourceNode, ServiceNode sinkNode) {
        this.sourceNode = sourceNode;
        this.serviceNodeSet.add(sourceNode);
        this.sinkNode = sinkNode;
        this.serviceNodeSet.add(sinkNode);
        for (ParamNode input : this.sinkNode.getInputs()) {
            Set<ServiceNode> followingServiceNodes = new HashSet<>();
            followingServiceNodes.add(this.sinkNode);
            this.unresolvedInput.put(input, followingServiceNodes);
            this.responseTimeBound.put(input, this.sinkNode.getResponseTimeFloor());
        }
    }

    /**
     * Resolve 解决输入匹配问题
     * 每次调用解决一个输入，并更新图的相关参数/判断是否满足条件（DAG，最优QoS……）
     */
    public boolean resolve(ParamNode matchSource, ParamNode matchTarget, double similarity) {
        //可选参数null匹配，不影响QoS，不产生环，不影响相似度下限，无dependency
        if (matchSource == null) {
            this.unresolvedInput.remove(matchTarget);
            return !matchTarget.getParam().isRequired();
        }
        //更新unresolved
        if (this.serviceNodeSet.contains(matchSource.getServiceNode())) {// 方案包含的 service node 不需要更新
            ServiceNode preServiceNode = matchSource.getServiceNode();
            // 更新QoS Bound
            if (!this.updateResponseTimeBound(preServiceNode, this.responseTimeBound.get(matchTarget))) {
                return false;
            }
            // 传递后续set
            Set<ServiceNode> reachable = this.unresolvedInput.get(matchTarget);
            this.unresolvedInput.remove(matchTarget);//旧unresolved
            for (ParamNode unresolved : this.unresolvedInput.keySet()) {
                if (this.unresolvedInput.get(unresolved).contains(preServiceNode)) {
                    this.unresolvedInput.get(unresolved).addAll(reachable);
                }
            }
        } else {
            ServiceNode preServiceNode = matchSource.getServiceNode();
            if (preServiceNode.getResponseTimeFloor() > this.responseTimeBound.get(matchTarget)) {
                return false;
            }
            this.serviceNodeSet.add(preServiceNode);//更新包含的 service node
            Set<ServiceNode> reachable = this.unresolvedInput.get(matchTarget);
            reachable.add(preServiceNode);
            this.unresolvedInput.remove(matchTarget);//旧unresolved
            for (ParamNode unresolvedNew : preServiceNode.getInputs()) {
                this.unresolvedInput.put(unresolvedNew, new HashSet<>(reachable));//新unresolved + 后续set
                this.responseTimeBound.put(
                        unresolvedNew,
                        this.responseTimeBound.get(matchTarget) - preServiceNode.getService().getResponseTime()
                );//QoS Bound
            }
        }
        // 添加匹配边，更新匹配相似度下限
        this.matchEdgeMap.computeIfAbsent(matchSource, v -> new HashMap<>()).put(matchTarget, similarity);
        this.matchEdgeMap.computeIfAbsent(matchTarget, v -> new HashMap<>()).put(matchSource, similarity);
        ServiceNode matchTargetServiceNode = matchTarget.getServiceNode();
        this.dependencyCountMap.put(matchTargetServiceNode,
                1 + this.dependencyCountMap.getOrDefault(matchTargetServiceNode, 0));
        if (similarity < this.similarityFloor) {
            this.similarityFloor = similarity;
        }
        return true;
    }

    // resolve过程中，反向遍历，递归更新 QoS Bound
    private boolean updateResponseTimeBound(ServiceNode serviceNode, int outputResponseTimeBound) {
        if (serviceNode == this.sourceNode) {
            return outputResponseTimeBound >= 0;
        }
        int responseTimeBoundNew = outputResponseTimeBound - serviceNode.getService().getResponseTime();
        if (serviceNode.getResponseTimeFloor() > responseTimeBoundNew) {
            return false;
        }
        boolean result = true;
        for (ParamNode input : serviceNode.getInputs()) {
            int responseTimeBound = this.responseTimeBound.get(input);
            if (responseTimeBoundNew < responseTimeBound) {
                this.responseTimeBound.put(input, responseTimeBoundNew);
                for (ParamNode matchSource : this.matchEdgeMap.get(input).keySet()) {//应该只有一个
                    result &= this.updateResponseTimeBound(matchSource.getServiceNode(), responseTimeBoundNew);
                    if (!result) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    // TODO: 验证该路径的可行性（各服务是否可用、结构化兼容情况）
    public boolean isAvailable() {
        return true;
    }

    /**
     * Run 运行
     * 通过 CompletableFuture 进行多线程异步调用编排（Orchestration）
     * 类拓扑排序，减少创建线程时因等待带来的延迟
     */
    public JSONObject run(JSONObject args) throws AutoExecuteException, ExecutionException, InterruptedException {
        System.out.println(args);
        // 编排和参数准备
        Set<ServiceNode> orchestrateSet = new HashSet<>();
        this.parameterObjectMap = new ConcurrentHashMap<>();

        // 处理初始输入
        JSONObject queryArgs = args.getJSONObject("query");
        for (ParamNode matchSource : this.sourceNode.getOutputs()) {
            if (!matchEdgeMap.containsKey(matchSource)) {
                continue;
            }
            switch (matchSource.getParam().getParamCategory()) {
                case QUERY:
                    Object value = queryArgs.get(matchSource.getParam().getName());
                    if (value != null) {
                        for (ParamNode matchTarget : this.matchEdgeMap.get(matchSource).keySet()) {
                            this.parameterObjectMap.putIfAbsent(matchTarget.getParam(), value);
                            int dependencyCount = this.dependencyCountMap.get(matchTarget.getServiceNode());
                            dependencyCount--;
                            if (dependencyCount == 0) {
                                orchestrateSet.add(matchTarget.getServiceNode());
                            }
                            this.dependencyCountMap.put(matchTarget.getServiceNode(), dependencyCount);
                        }
                    } else {
                        throw new AutoExecuteException("missing input param");
                    }
                    break;
                case PATH:
                case BODY:
                default:
                    throw new AutoExecuteException("only support query param");
            }
        }
        //输入参数构造完毕，编排执行服务
        //借鉴拓扑排序思想，降低节点入度至 0 时加入队列构造 allOf
        Orchestration:
        while (!orchestrateSet.isEmpty()) {
            Set<ServiceNode> orchestrateSetNew = new HashSet<>();
            for (ServiceNode node : orchestrateSet) {
                // sinkNode 在 break 后统一处理
                if (node == this.sinkNode) {
                    break Orchestration;
                }
                // 一般服务节点，用 allOf 编排
                List<CompletableFuture> dependFutureList = new ArrayList<>();
                for (ParamNode input : node.getInputs()) {
                    if (this.matchEdgeMap.get(input) == null && !input.getParam().isRequired()) {
                        continue;
                    }
                    ServiceNode dependNode = this.matchEdgeMap.get(input).keySet().iterator().next().getServiceNode();
                    if (dependNode != this.sourceNode) {
                        dependFutureList.add(this.serviceNodeCompletableFutureMap.get(dependNode));
                    }
                }
                CompletableFuture<Void> future;
                if (dependFutureList.isEmpty()) {
                    future = CompletableFuture.runAsync(new ServiceThread(node));
                } else {
                    future = CompletableFuture
                            .allOf(dependFutureList.toArray(new CompletableFuture[0]))
                            .thenRunAsync(new ServiceThread(node));
                }
                this.serviceNodeCompletableFutureMap.put(node, future);
                // 后续节点降低入度，加入New等待下一轮编排
                for (ParamNode output : node.getOutputs()) {
                    if (this.matchEdgeMap.get(output) == null) {
                        continue;
                    }
                    for (ParamNode matchTarget : this.matchEdgeMap.get(output).keySet()) {
                        ServiceNode follow = matchTarget.getServiceNode();
                        int dependencyCount = this.dependencyCountMap.get(follow);
                        if (--dependencyCount == 0) {
                            orchestrateSetNew.add(follow);
                        }
                        this.dependencyCountMap.put(follow, dependencyCount);
                    }
                }
            }
            // 换成下一轮编排对象
            orchestrateSet = orchestrateSetNew;
        }

        // sinkNode 编排
        List<CompletableFuture> dependFutureList = new ArrayList<>();
        for (ParamNode input : this.sinkNode.getInputs()) {
            ServiceNode dependNode = this.matchEdgeMap.get(input).keySet().iterator().next().getServiceNode();
            if (dependNode != this.sourceNode) {
                dependFutureList.add(this.serviceNodeCompletableFutureMap.get(dependNode));
            }
        }
        CompletableFuture<Void> sinkFuture = CompletableFuture
                .allOf(dependFutureList.toArray(new CompletableFuture[0]));

        // 等待 sinkNode，结束后返回最终结果
        sinkFuture.get();
        JSONObject result = new JSONObject();
        for (ParamNode paramNode : this.sinkNode.getInputs()) {
            Object value = this.parameterObjectMap.get(paramNode.getParam());
            result.put(paramNode.getParam().getName(), value);
        }

        return result;
    }

    // 单个服务调用线程
    private class ServiceThread extends Thread {
        private ServiceNode executeNode;

        private ServiceThread(ServiceNode executeNode) {
            this.executeNode = executeNode;
        }

        @Override
        public void run() {
            this.executeNode.getService().run(parameterObjectMap);
            for (ParamNode matchSource : this.executeNode.getOutputs()) {
                if (!matchEdgeMap.containsKey(matchSource)) {
                    continue;
                }
                if (parameterObjectMap.containsKey(matchSource.getParam())) {
                    Object value = parameterObjectMap.get(matchSource.getParam());
                    for (ParamNode matchTarget : matchEdgeMap.get(matchSource).keySet()) {
                        parameterObjectMap.putIfAbsent(matchTarget.getParam(), value);
                    }
                } else {
                    throw new AutoExecuteException("Matching failure: " +
                            "missing output param {" + matchSource.getParam().getName() + "}");
                }
            }
        }
    }


    /**
     * getter & setter
     */
    public Map<ParamNode, Set<ServiceNode>> getUnresolvedInput() {
        return unresolvedInput;
    }

    public Set<ServiceNode> getServiceNodeSet() {
        return serviceNodeSet;
    }

    public double getSimilarityFloor() {
        return similarityFloor;
    }

    public boolean isAllResolved() {
        return allResolved;
    }

    public void setAllResolved(boolean allResolved) {
        this.allResolved = allResolved;
    }


    @Override
    public Object clone() throws CloneNotSupportedException {
        ExecutionPath clone = (ExecutionPath) super.clone();

        clone.serviceNodeSet = new HashSet<>(this.serviceNodeSet);
        clone.matchEdgeMap = new HashMap<>();
        for (ParamNode node : this.matchEdgeMap.keySet()) {
            clone.matchEdgeMap.put(node, new HashMap<>(this.matchEdgeMap.get(node)));
        }
        clone.responseTimeBound = new HashMap<>(this.responseTimeBound);

        clone.unresolvedInput = new HashMap<>();
        for (ParamNode node : this.unresolvedInput.keySet()) {
            clone.unresolvedInput.put(node, new HashSet<>(this.unresolvedInput.get(node)));
        }

        clone.dependencyCountMap = new HashMap<>(this.dependencyCountMap);

        return clone;
    }

    @Override
    public String toString() {
        Set<ServiceNode> nodeSet = new HashSet<>(serviceNodeSet);
        nodeSet.remove(this.sourceNode);
        nodeSet.remove(this.sinkNode);
        return "ExecutionPath{" + nodeSet + "}";
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("similarity", similarityFloor);
        Set<Integer> involvedService = new HashSet<>();
        for (ServiceNode node : serviceNodeSet) {
            if (node == this.sourceNode || node == this.sinkNode) {
                continue;
            }
            involvedService.add(node.getService().getId());
        }
        jsonObject.put("involvedService", involvedService);
        Map<Pair<ParamNode, ParamNode>, Double> matchEdges = new HashMap<>();
        for (Map.Entry<ParamNode, Map<ParamNode, Double>> entry : this.matchEdgeMap.entrySet()) {
            ParamNode matchSource = entry.getKey();
            if (matchSource.getType() != ParamNode.Type.OUTPUT) {
                continue;
            }
            ParamNode matchTarget = entry.getValue().keySet().iterator().next();
            double similarity = entry.getValue().entrySet().iterator().next().getValue();
            matchEdges.put(new Pair<>(matchSource, matchTarget), similarity);
        }
        jsonObject.put("match", matchEdges);
        return jsonObject;
    }

}