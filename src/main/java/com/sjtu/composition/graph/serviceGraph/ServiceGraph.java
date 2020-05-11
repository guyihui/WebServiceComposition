package com.sjtu.composition.graph.serviceGraph;

import com.sjtu.composition.serviceUtils.Parameter;
import com.sjtu.composition.serviceUtils.Service;
import com.sjtu.composition.similarityUtils.SimilarityUtils;

import java.util.*;


public class ServiceGraph {

    // 相似度工具类
    protected SimilarityUtils similarityUtils;
    // 服务图的范围
    protected Service targetService;
    protected Set<Service> serviceCluster;
    // 图相关结构
    protected ServiceNode sourceNode = new ServiceNode(null);
    protected ServiceNode sinkNode = new ServiceNode(null);

    // 构造约束
    protected boolean isBuilt = false;
    protected double similarityLimit;
    protected int layerLimit;
    // 匹配关系 //TODO: 是否需要保留
    protected Map<ParamNode, Map<ParamNode, Double>> matchEdgeMap = new HashMap<>();// 匹配边
    private Set<ParamNode> matchSource = new HashSet<>();//包括初始输入(source节点的输出)和服务的输出//TODO:能不能删除？
    private Set<ParamNode> matchTarget = new HashSet<>();//包括最终输出(sink节点的输入)和服务的输入

    // 可用服务/节点
    protected Map<Integer, ServiceNode> serviceNodeMap = new HashMap<>();
    protected Set<ParamNode> paramNodeSet = new HashSet<>();


    public ServiceGraph(Service targetService, Set<Service> serviceCluster,
                        SimilarityUtils similarityUtils) {
        // 注入工具类
        this.similarityUtils = similarityUtils;
        // 图内容
        this.targetService = targetService;
        this.serviceCluster = serviceCluster;
        // source node
        Set<ParamNode> targetInputs = new HashSet<>();
        for (Parameter parameter : targetService.getRequestParams()) {
            targetInputs.add(new ParamNode(ParamNode.Type.OUTPUT, parameter, this.sourceNode));
        }
        this.sourceNode.setInputs(new HashSet<>());
        this.sourceNode.setOutputs(targetInputs);
        // sink node
        Set<ParamNode> targetOutputs = new HashSet<>();
        for (Parameter parameter : targetService.getResponseParams()) {
            targetOutputs.add(new ParamNode(ParamNode.Type.INPUT, parameter, this.sinkNode));
        }
        this.sinkNode.setInputs(targetOutputs);
        this.sinkNode.setOutputs(new HashSet<>());
    }

    protected boolean build(double similarityLimit, int layerLimit) {
        clear();
        System.out.println(this);
        prepare(similarityLimit, layerLimit);
        System.out.println(this);
        this.isBuilt = expand();
        System.out.println(this);
        if (this.isBuilt) {
            dropUnreachableServices();//TODO: 整理多余的匹配边（涉及到无用节点）
            System.out.println(this);
            //TODO: prune with QoS
        }
        System.out.println(this.isBuilt);
        return this.isBuilt;
    }

    private void clear() {
        // 约束条件
        this.isBuilt = false;
        this.similarityLimit = 1.0;
        this.layerLimit = 0;
        // 匹配关系
        this.matchEdgeMap.clear();
        this.matchSource.clear();
        this.matchTarget.clear();
        // 可用服务/Param节点
        this.serviceNodeMap.clear();
        this.paramNodeSet.clear();
    }

    private void prepare(double similarityLimit, int layerLimit) {
        this.similarityLimit = similarityLimit;
        this.layerLimit = layerLimit;
        for (Service service : this.serviceCluster) {
            if (service == this.targetService) {
                continue;
            }
            this.convertServiceToGraph(service);
        }
        this.matchSource.addAll(this.sourceNode.getOutputs());
        this.matchTarget.addAll(this.sinkNode.getInputs());
    }

    private void convertServiceToGraph(Service service) {
        ServiceNode serviceNode = new ServiceNode(service);
        Set<ParamNode> inputs = new HashSet<>();
        Set<ParamNode> outputs = new HashSet<>();
        // 构造输入输出节点及其与serviceNode之间的连接边
        for (Parameter inputParam : service.getRequestParams()) {
            inputs.add(new ParamNode(ParamNode.Type.INPUT, inputParam, serviceNode));
        }
        for (Parameter outputParam : service.getResponseParams()) {
            outputs.add(new ParamNode(ParamNode.Type.OUTPUT, outputParam, serviceNode));
        }
        serviceNode.setInputs(inputs);
        serviceNode.setOutputs(outputs);
        // 分类到 matchSource / matchTarget
        this.matchSource.addAll(outputs);
        this.matchTarget.addAll(inputs);
    }

    // TODO: 会不会存在无输入/输出的服务
    private boolean expand() {

        Set<ParamNode> validParamNodeSet = new HashSet<>(this.sourceNode.getOutputs());
        Map<Integer, ServiceNode> validServiceNodeMap = new HashMap<>();

        Set<ParamNode> unresolvedTarget = new HashSet<>(this.sinkNode.getInputs());// 最终需要得到的输出
        Set<ParamNode> matchSourceNew = new HashSet<>(this.sourceNode.getOutputs());// 输出得到新一轮 matchSource
        Set<ParamNode> matchTargetNew;// 匹配成功的新一轮 matchTarget（作为服务输入）

        // 1. 首先根据相似度匹配，进行第一层扩展
        matchTargetNew = this.matchBySimilarity(matchSourceNew);
        // 已经在构造的时候保存

        // 一定轮数内（target 输出可能已经全部获得）
        for (int round = 0; round < this.layerLimit; round++) {
            // 2. 根据 匹配得到的新输入节点 获取这一轮新增的可执行服务
            Set<ServiceNode> availableServiceNode = new HashSet<>();
            Set<ServiceNode> unavailableServiceNode = new HashSet<>();
            for (ParamNode node : matchTargetNew) {
                ServiceNode serviceNode = node.getServiceNode(); // 输入节点指向的服务
                if (availableServiceNode.contains(serviceNode)
                        || unavailableServiceNode.contains(serviceNode)
                        || serviceNode.getService() == this.targetService) {
                    continue;
                }
                // 对于未判断过的服务，检查服务输入是否 available
                boolean isAvailable = true;
                for (ParamNode input : serviceNode.getInputs()) {
                    if (input.getParam().isRequired()
                            && !validParamNodeSet.contains(input)
                            && !matchTargetNew.contains(input)) {
                        isAvailable = false;// 有输入unavailable
                        break;
                    }
                }
                if (isAvailable) {
                    availableServiceNode.add(serviceNode);
                } else {
                    unavailableServiceNode.add(serviceNode);
                }
            }
            // 3. 保存：获取服务完毕的节点、这一轮可执行服务
            validParamNodeSet.addAll(matchTargetNew);
            for (ServiceNode available : availableServiceNode) {
                if (available != this.sinkNode) {
                    validServiceNodeMap.put(available.getService().getId(), available);
                }
            }
            // 4. 根据可使用服务，得到这些服务的输出
            matchSourceNew.clear();
            for (ServiceNode serviceNode : availableServiceNode) {
                matchSourceNew.addAll(serviceNode.getOutputs());
            }
            validParamNodeSet.addAll(matchSourceNew);
            // *1. 根据相似度对结果进行下一轮扩展
            matchTargetNew = this.matchBySimilarity(matchSourceNew);
            // 若 target 被满足，从 target 集合中移除
            Set<ParamNode> resolvedTarget = new HashSet<>();
            for (ParamNode node : unresolvedTarget) {
                if (matchTargetNew.contains(node)) {
                    resolvedTarget.add(node);
                }
            }
            unresolvedTarget.removeAll(resolvedTarget);
            // 如果 target 全部得到，不提前结束
            // *2. 筛选匹配得到的输入（避免重复搜索）
            Set<ParamNode> traversedMatchTarget = new HashSet<>();
            for (ParamNode node : matchTargetNew) {
                if (validParamNodeSet.contains(node)) {
                    traversedMatchTarget.add(node);
                }
            }
            matchTargetNew.removeAll(traversedMatchTarget);
        }

        this.paramNodeSet = validParamNodeSet;
        this.serviceNodeMap = validServiceNodeMap;

        return unresolvedTarget.isEmpty();

//        if (solution.isResolved) {
//            solution.prune();
//            solution.collectMatchEdge();
//            return true;
//        }
//        return false;
    }

    private Set<ParamNode> matchBySimilarity(Set<ParamNode> source) {
        Set<ParamNode> expandSet = new HashSet<>();
        for (ParamNode node : source) {
            for (ParamNode target : this.matchTarget) {
                double similarity = this.getSimilarity(node, target);
                if (similarity >= this.similarityLimit) {
                    expandSet.add(target);
                    this.matchEdgeMap.computeIfAbsent(node, v -> new HashMap<>()).put(target, similarity);
                    this.matchEdgeMap.computeIfAbsent(target, v -> new HashMap<>()).put(node, similarity);
                }
            }
        }
        return expandSet;
    }

    // TODO: 同一个服务的输入输出是否需要设置相似度<=0.0，代表不允许语义匹配
    private double getSimilarity(ParamNode node1, ParamNode node2) {
        return similarityUtils.similarity(node1.getParam(), node2.getParam());
    }

    // 删去反向不可达路径、节点
    private void dropUnreachableServices() {

        Set<ParamNode> validParamNodeSet = new HashSet<>(this.sinkNode.getInputs());// 已经分析过的节点
        Map<Integer, ServiceNode> validServiceNodeMap = new HashMap<>();

        Set<ParamNode> validMatchTargetNew = new HashSet<>(this.sinkNode.getInputs());// 通过反向路径查找
        Set<ParamNode> validMatchSourceNew;

        // 1. 反向扩展
        validMatchSourceNew = reverseExpand(validMatchTargetNew);

        // 根据反向遍历的新节点，判断服务反向可达性
        while (!validMatchSourceNew.isEmpty()) {
            Set<ServiceNode> validServiceNodeNew = new HashSet<>();
            // 2. 标记可达服务
            for (ParamNode node : validMatchSourceNew) {
                // output节点对应的服务直接标记可达
                ServiceNode serviceNode = node.getServiceNode();
                if (serviceNode != this.sourceNode
                        && !validServiceNodeMap.containsKey(serviceNode.getService().getId())) {
                    validServiceNodeNew.add(serviceNode);
                }
            }
            // 3. 保存：获取服务完毕的节点，移入valid；反向可达服务
            validParamNodeSet.addAll(validMatchSourceNew);
            for (ServiceNode serviceNode : validServiceNodeNew) {
                validServiceNodeMap.put(serviceNode.getService().getId(), serviceNode);
            }
            // 4. 得到反向可达服务后，将输入节点作为下一轮 data node
            validMatchTargetNew.clear();
            for (ServiceNode serviceNode : validServiceNodeNew) {
                validMatchTargetNew.addAll(serviceNode.getInputs());
            }
            validParamNodeSet.addAll(validMatchTargetNew);
            // *1. 扩展，筛选已遍历过的节点
            validMatchSourceNew = reverseExpand(validMatchTargetNew);
            Set<ParamNode> traversedMatchSource = new HashSet<>();
            for (ParamNode node : validMatchSourceNew) {
                if (validParamNodeSet.contains(node)) {
                    traversedMatchSource.add(node);
                }
            }
            validMatchSourceNew.removeAll(traversedMatchSource);
        }
        // 反向遍历结束，直接使用 valid 作为内容
        this.serviceNodeMap = validServiceNodeMap;
        this.paramNodeSet = validParamNodeSet;
    }

    // 只从输入反向扩展到输出
    private Set<ParamNode> reverseExpand(Set<ParamNode> validMatchTargetNew) {
        Set<ParamNode> validMatchSource = new HashSet<>();
        for (ParamNode node : validMatchTargetNew) {
            if (this.matchEdgeMap.get(node) == null && !node.getParam().isRequired()) {
                continue;
            }
            validMatchSource.addAll(this.matchEdgeMap.get(node).keySet());
        }
        return validMatchSource;
    }

    public Set<ParamNode> getTargetOutput() {
        return this.sinkNode.getInputs();
    }

    @Override
    public String toString() {
        return "=======\nService Graph {\n"
                + "\tData nodes(" + paramNodeSet.size() + "): " + paramNodeSet.toString() + "\n"
                + "\tService nodes(" + serviceNodeMap.size() + "): " + serviceNodeMap.toString() + "\n"
                + "}";
    }
}
