package serviceUtils;

import serviceUtils.executionPath.*;
import serviceUtils.serviceGraph.*;

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


    // 提取：得到无歧义/多余路径的实际执行路径（可能存在多种）
    // 返回完整的set
    // TODO: 是否会出现栈溢出，递归/非递归
    public Set<ExecutionPath> extractExecutionPaths() {
        Set<ExecutionPath> results = new HashSet<>();
        if (!this.isResolved) {
            return results;
        }
        //需要通过匹配解决的target
        ExecutionPath executionPath = new ExecutionPath(this.targetServiceNode.getOutputs());
        completeExecutionPath(executionPath, results);
        //合并反向路径中重复的部分
        for (ExecutionPath path : results) {
            path.merge();
        }
        return results;
    }

    //每次递归完成一个match
    // compositeLengthLimit: 限制组合长度，避免无穷环路径
    private void completeExecutionPath(ExecutionPath path, Set<ExecutionPath> pathSet) {
        // 递归终点：所有execution节点的输入匹配完毕
        Map<ExecutionNode, Set<DataNode>> unresolvedExecutionHeadMap = path.getUnresolvedExecutionHeads();
        if (unresolvedExecutionHeadMap.isEmpty()) {
            pathSet.add(path);
            return;
        }
        //取出这一轮需要匹配的节点（这些节点不能被修改）
        // ExecutionNode及其输入
        Iterator<Map.Entry<ExecutionNode, Set<DataNode>>> headMapItr = unresolvedExecutionHeadMap.entrySet().iterator();
        Map.Entry<ExecutionNode, Set<DataNode>> mapEntry = headMapItr.next();
        final ExecutionNode executionHead = mapEntry.getKey();
        final Set<DataNode> unresolvedInputs = mapEntry.getValue();
        // 其中的一个输入
        Iterator<DataNode> dataNodeItr = unresolvedInputs.iterator();
        final DataNode toMatch = dataNodeItr.next();

        //匹配候选
        //当有多个可能性时, clone副本, 尝试所有匹配可能
        Set<DataNode> matchCandidate = matchEdge.get(toMatch).keySet();
        int candidateNo = 0;
        try {
            for (DataNode candidate : matchCandidate) {
                candidateNo++;
                ExecutionPath clonePath;
                if (candidateNo == matchCandidate.size()) { //最后一个候选，不需要clone
                    clonePath = path;
                } else { // 否则需要clone（深拷贝）后分开操作
                    clonePath = (ExecutionPath) path.clone();
                }

                //在clonePath上补全
                ExecutionNode preNode;
                if (targetServiceNode.getInputs().contains(candidate)) {//如果是原始输入，则前置 ExecutionNode 为起点node
                    preNode = ExecutionPath.START_NODE;
                } else {//否则，preNode为产生该candidate输出的node
                    preNode = new ExecutionNode(ExecutionNode.Type.COMPONENT, candidate.getServiceNode());
                }
                MatchNode matchNode = new MatchNode(candidate, toMatch);
                // 连接路径
                // preNode -> matchNode -> executionHead -> ...
                clonePath.connect(preNode, matchNode);
                clonePath.connect(matchNode, executionHead);
                // 更新待匹配map（需要对clone出的path进行操作）
                Map<ExecutionNode, Set<DataNode>> unresolvedExecutionHeadMapInClonePath = clonePath.getUnresolvedExecutionHeads();
                Set<DataNode> unresolvedInputsInClonePath = unresolvedExecutionHeadMapInClonePath.get(executionHead);
                unresolvedInputsInClonePath.remove(toMatch);//需要解决的头节点的一个输入匹配完成
                if (unresolvedInputsInClonePath.isEmpty()) {// 输入全部匹配 = 运行节点匹配完成
                    unresolvedExecutionHeadMapInClonePath.remove(executionHead);
                }
                if (preNode.type == ExecutionNode.Type.COMPONENT) { // 产生的新的运行节点
                    Set<DataNode> inputsNew = new HashSet<>(preNode.getServiceNode().getInputs());
                    unresolvedExecutionHeadMapInClonePath.put(preNode, inputsNew);
                }
                // 递归，完成前置路径
                completeExecutionPath(clonePath, pathSet);
            }
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
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
