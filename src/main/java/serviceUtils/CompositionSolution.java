package serviceUtils;

import serviceUtils.executionPath.ExecutionNode;
import serviceUtils.executionPath.ExecutionPath;
import serviceUtils.executionPath.MatchNode;
import serviceUtils.serviceGraph.DataNode;
import serviceUtils.serviceGraph.ServiceGraph;
import serviceUtils.serviceGraph.ServiceNode;

import java.util.*;

// 包含了可能的多种执行路径的整体解决方案
public class CompositionSolution extends ServiceGraph {

    // 继承属性
    // protected Set<DataNode> dataNodeSet = new HashSet<>();
    // protected Set<ServiceNode> serviceNodeSet = new HashSet<>();
    // 父类静态成员属性
    // similarityMap

    // 问题描述和约束
    private Service targetService;
    private double similarityLimit;
    private int roundLimit;

    public CompositionSolution(Service targetService, double similarityLimit, int roundLimit) {
        this.targetService = targetService;
        this.similarityLimit = similarityLimit;
        this.roundLimit = roundLimit;
    }

    // 方案结果属性
    public boolean isResolved = false;
    public boolean isExistingService = false;
    private ServiceNode targetServiceNode = null;
    public int round;

    public ServiceNode getTargetServiceNode() {
        return targetServiceNode;
    }

    public void setTargetServiceNode(ServiceNode targetServiceNode) {
        this.targetServiceNode = targetServiceNode;
    }

    // 匹配关系
    private int matchCount;
    private Map<DataNode, Map<DataNode, Double>> matchEdge = new HashMap<>();// 匹配边
    private Set<DataNode> matchSource = new HashSet<>();//包括初始输入和服务的输出
    private Set<DataNode> matchTarget = new HashSet<>();//包括最终输出和服务的输入

    // 剪枝：删去反向不可达路径、节点
    public void prune() {
        List<DataNode> outputs = targetServiceNode.getOutputs();
        Set<DataNode> validDataNode = new HashSet<>();// 已经分析过的节点
        Set<DataNode> validDataNodeNew = new HashSet<>(outputs);// 通过反向路径查找
        Set<ServiceNode> validServiceNode = new HashSet<>();// 反向可达服务
        Set<ServiceNode> validServiceNodeNew = new HashSet<>();// 反向可达服务

        // 扩展 solution 中的其他节点
        Set<DataNode> expandSet = new HashSet<>();
        reverseExpand(validDataNodeNew, expandSet);

        // 根据反向遍历的新节点，判断服务反向可达性
        while (validDataNodeNew.size() != 0) {
            for (DataNode node : validDataNodeNew) {
                if (node.type == DataNode.Type.INPUT) {// 跳过输入节点
                    continue;
                }
                // output节点对应的服务直接标记可达
                ServiceNode serviceNode = node.getServiceNode();
                if (this.serviceNodeSet.contains(serviceNode)) {
                    validServiceNodeNew.add(serviceNode);
                }
            }
            // 获取服务完毕的节点，移入valid
            validDataNode.addAll(validDataNodeNew);
            validDataNodeNew.clear();
            // 得到反向可达服务后，将输入节点作为下一轮 data node
            for (ServiceNode serviceNode : validServiceNodeNew) {
                validDataNodeNew.addAll(serviceNode.getInputs());
            }
            // 扩展
            reverseExpand(validDataNodeNew, expandSet);
            // 确定反向可达的服务移入valid
            validServiceNode.addAll(validServiceNodeNew);
            this.serviceNodeSet.removeAll(validServiceNodeNew);
            validServiceNodeNew.clear();
        }
        // 反向遍历结束，直接使用 valid 作为 solution 内容，另外加上初始输入
        this.serviceNodeSet = validServiceNode;
        this.dataNodeSet = validDataNode;
        this.dataNodeSet.addAll(targetServiceNode.getInputs());
    }

    // 整理图中的匹配边
    public void collectMatchEdge() {
        matchCount = 0;
        matchEdge.clear();
        matchSource.clear();
        matchTarget.clear();
        for (DataNode node : this.dataNodeSet) {
            if (targetServiceNode.getInputs().contains(node)) { // 初始输入∈source
                matchSource.add(node);
            } else if (targetServiceNode.getOutputs().contains(node)) { // 最终输出∈target
                matchTarget.add(node);
            } else if (node.type == DataNode.Type.OUTPUT) { // 服务输出∈source
                matchSource.add(node);
            } else if (node.type == DataNode.Type.INPUT) { // 服务输入∈target
                matchTarget.add(node);
            }
        }
        // 匹配边：{source节点}->{target节点} = 使用{source}来代替{target}使用
        for (DataNode source : matchSource) {
            for (DataNode target : matchTarget) {
                Double similarity = similarityMap.get(source).get(target);
                if (similarity >= similarityLimit) {
                    matchEdge.computeIfAbsent(source, k -> new HashMap<>());
                    matchEdge.get(source).put(target, similarity);
                    matchEdge.computeIfAbsent(target, k -> new HashMap<>());
                    matchEdge.get(target).put(source, similarity);
                    matchCount++;
                }
            }
        }
    }

    // 只从输入反向扩展到输出，初始输入额外处理
    private void reverseExpand(Set<DataNode> validDataNodeNew, Set<DataNode> expandSet) {
        for (DataNode node : validDataNodeNew) {
            for (DataNode toExpand : this.dataNodeSet) {
                if (node != toExpand // 不同节点
                        && toExpand.type != DataNode.Type.INPUT // 反向遍历不扩展到其他输入节点
                        && similarityMap.get(node).get(toExpand) >= similarityLimit) {
                    expandSet.add(toExpand);
                }
            }
        }
        validDataNodeNew.addAll(expandSet);
        expandSet.clear();
        this.dataNodeSet.removeAll(validDataNodeNew);
    }


    // TODO: 提取：得到无歧义/多余路径的实际执行路径（可能存在多种）
    // TODO: 返回完整的set（先实现返回一种结果）
    // TODO: 是否会出现栈溢出，递归/非递归
    public Set<ExecutionPath> extractExecutionPaths() {
        Set<ExecutionPath> results = new HashSet<>();
        //需要通过匹配解决的target
        ExecutionPath executionPath = new ExecutionPath();
        completeExecutionPath(executionPath, executionPath.endNode, results);
        results.add(executionPath);
        //合并反向路径中重复的部分
        for (ExecutionPath path : results) {
            path.merge();
        }
        return results;
    }

    //TODO: 需要遍历所有情况
    private void completeExecutionPath(ExecutionPath executionPath, ExecutionNode executionHead, Set<ExecutionPath> pathSet) {
        // 递归终点：起点ExecutionNode
        if (executionHead == executionPath.startNode) {
            return;
        }
        List<DataNode> matchTargetSet;
        //execution节点的输入
        if (executionHead == executionPath.endNode) {
            matchTargetSet = this.targetServiceNode.getOutputs();
        } else {
            matchTargetSet = executionHead.getServiceNode().getInputs();
        }
        //补充每个待匹配节点之前的路径
        for (DataNode node : matchTargetSet) {
            Set<DataNode> matchCandidate = matchEdge.get(node).keySet();
            //TODO:遍历所有匹配可能
            for (DataNode candidate : matchCandidate) {
                ExecutionNode preNode;
                //如果是原始输入，则前置 ExecutionNode 为起点node
                if (targetServiceNode.getInputs().contains(candidate)) {
                    preNode = executionPath.startNode;
                } else {//否则，preNode为产生该candidate输出的node
                    preNode = new ExecutionNode(ExecutionNode.Type.COMPONENT, candidate.getServiceNode());
                }
                MatchNode matchNode = new MatchNode(candidate, node);
                // 连接路径
                // preNode -> matchNode -> executionHead -> ...
                executionHead.addInputMatchNode(matchNode);
                matchNode.setToNode(executionHead);
                matchNode.setFromNode(preNode);
                preNode.addOutputMatchNode(matchNode);
                // 递归，完成前置路径
                completeExecutionPath(executionPath, preNode, pathSet);
                break;//TODO: 当有多个可能性时应该clone副本
            }
        }
    }


    // TODO: 选择：若有多种可能，根据一定约束进行选择

    @Override
    public String toString() {
        return "=======\nComposition solution {\n"
                + "问题约束:\n"
                + "\tTarget: " + this.targetService + "\n"
                + "\tSimilarity limit: " + this.similarityLimit + "\n"
                + "\tRound limit: " + this.roundLimit + "\n"
                + "solution结果:\n"
                + "\tIs Resolved: " + isResolved + "\n"
                + "\tIs Existing Service: " + isExistingService + "\n"
                + "\tTarget Service Node: " + targetServiceNode + "\n"
                + "\tRound: " + round + "\n"
                + "solution规模:\n"
                + "\tData nodes(" + dataNodeSet.size() + "): " + dataNodeSet.toString() + "\n"
                + "\tService nodes(" + serviceNodeSet.size() + "): " + serviceNodeSet.toString() + "\n"
                + "\tMatch count: " + matchCount + "\n"
                + "}";
    }

}
