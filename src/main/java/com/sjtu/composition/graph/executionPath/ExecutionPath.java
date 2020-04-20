package com.sjtu.composition.graph.executionPath;

import com.sjtu.composition.graph.CompositionSolution;
import com.sjtu.composition.graph.serviceGraph.DataNode;
import com.sjtu.composition.graph.serviceGraph.ServiceNode;
import com.sjtu.composition.serviceUtils.Service;

import java.util.*;

public class ExecutionPath implements Cloneable {
    private CompositionSolution solution;
    //可以类公用的虚拟头尾节点
    public static final ExecutionNode START_NODE = new ExecutionNode(ExecutionNode.Type.START, null);
    public static final ExecutionNode END_NODE = new ExecutionNode(ExecutionNode.Type.END, null);
    // 用来表示某一条具体路径的边信息
    private Map<ExecutionPathNode, Set<ExecutionPathNode>> pathMap = new HashMap<>();//连接边
    private Map<ExecutionNode, Set<MatchNode>> preMatchNodes = new HashMap<>();//每个运行节点需要的前置matchNode
    // 提取过程中的辅助数据结构
    private volatile Map<ExecutionNode, Set<DataNode>> unresolvedExecutionHeads = new HashMap<>();//<需要匹配的头节点，其需要匹配的输入>
    private volatile Map<ExecutionNode, Integer> unresolvedHeadLength = new HashMap<>();//待匹配节点之后的路径长度
    // TODO: 保存path参数，用于path间的比较选择（需要clone）
    // 例如：服务数量、组合长度、涉及到的服务的集合、QoS……
    private Set<ServiceNode> involvedServiceNodes = new HashSet<>();
    // 执行时保存的信息（提取后执行，不涉及clone）
    private Map<MatchNode, Object> matchArgs;// 记录对应每个 matchNode 的实际数据对象
    private Set<ExecutionPathNode> nextExecutions;// 记录接下来可能执行的 execution


    public ExecutionPath(CompositionSolution solution) {
        this.solution = solution;
        Set<DataNode> toMatchSet = new HashSet<>(solution.getTargetServiceNode().getOutputs());
        unresolvedExecutionHeads.put(END_NODE, toMatchSet);
        unresolvedHeadLength.put(END_NODE, 0);
    }

    public enum ConnectType {
        EXECUTION_TO_MATCH, MATCH_TO_EXECUTION,
    }

    // 连接路径
    public void connect(ExecutionPathNode former, ExecutionPathNode latter, ConnectType type) {
        pathMap.computeIfAbsent(former, k -> new HashSet<>()).add(latter);
        if (type == ConnectType.MATCH_TO_EXECUTION
                && former.getClass() == MatchNode.class
                && latter.getClass() == ExecutionNode.class) {
            preMatchNodes.computeIfAbsent((ExecutionNode) latter, k -> new HashSet<>())
                    .add((MatchNode) former);
        }
        if (type == ConnectType.EXECUTION_TO_MATCH
                && former.getClass() == ExecutionNode.class
                && latter.getClass() == MatchNode.class) {
            ServiceNode serviceNode = ((ExecutionNode) former).getServiceNode();
            if (former != START_NODE && serviceNode != null) {
                involvedServiceNodes.add(serviceNode);
            }
        }
    }

    // 对路径的重复部分进行合并
    public void merge() {
        this.mergeMatchNode(START_NODE);
    }

    private boolean canMerge(ExecutionPathNode node1, ExecutionPathNode node2) {
        if (node1.getClass() != node2.getClass()) return false;
        if (node1.getClass() == MatchNode.class) {
            return ((MatchNode) node1).mayMergeWith((MatchNode) node2);
        }
        if (node1.getClass() == ExecutionNode.class) {
            ExecutionNode n1 = (ExecutionNode) node1;
            ExecutionNode n2 = (ExecutionNode) node2;
            Set<MatchNode> preMatch1 = preMatchNodes.get(n1);
            Set<MatchNode> preMatch2 = preMatchNodes.get(n2);
            return n1.mayMergeWith(n2) &&
                    preMatch1.containsAll(preMatch2) && preMatch2.containsAll(preMatch1);
        }
        return false;
    }

    // 合并某一个 ExecutionNode 的后续 matchNode
    private void mergeMatchNode(ExecutionNode executionNode) {
        Set<ExecutionPathNode> matchNodes = pathMap.get(executionNode);//后续的匹配节点
        Object[] matchNodeList = matchNodes.toArray();
        // 两两比较并避免重复
        for (int i = 0; i < matchNodeList.length - 1; i++) {
            for (int j = i + 1; j < matchNodeList.length; j++) {
                MatchNode matchNode1 = (MatchNode) matchNodeList[i];
                MatchNode matchNode2 = (MatchNode) matchNodeList[j];
                if (this.canMerge(matchNode1, matchNode2)) {
                    //前节点连接边合并
                    pathMap.get(executionNode).remove(matchNode2);
                    //后连接边（matchNode <- executionNode）
                    for (ExecutionPathNode node : pathMap.get(matchNode2)) {
                        ExecutionNode latterExecutionNode = (ExecutionNode) node;
                        preMatchNodes.get(latterExecutionNode).remove(matchNode2);
                        preMatchNodes.get(latterExecutionNode).add(matchNode1);
                    }
                    //后连接边（matchNode -> executionNode）
                    pathMap.get(matchNode1).addAll(pathMap.get(matchNode2));
                    //合并后续的execution节点
                    this.mergeExecutionNode(matchNode1);
                    //移除被合并的节点
                    pathMap.remove(matchNode2);
                    //修改list中被合并的节点(后者)位置，避免重复
                    matchNodeList[j] = matchNode1;
                }
            }
        }
    }

    // 合并某一个 MatchNode 的后续 executionNode
    private void mergeExecutionNode(MatchNode matchNode) {
        Set<ExecutionPathNode> executionPathNodes = pathMap.get(matchNode);//后续の执行节点
        Object[] executionNodeList = executionPathNodes.toArray();
        // 两两比较并避免重复
        for (int i = 0; i < executionNodeList.length - 1; i++) {
            for (int j = i + 1; j < executionNodeList.length; j++) {
                ExecutionNode executionNode1 = (ExecutionNode) executionNodeList[i];
                ExecutionNode executionNode2 = (ExecutionNode) executionNodeList[j];
                if (this.canMerge(executionNode1, executionNode2)) {
                    // 前节点连接边合并( matchNode -> executionNode )
                    for (MatchNode preMatch : preMatchNodes.get(executionNode1)) {
                        pathMap.get(preMatch).remove(executionNode2);
                    }
                    // 前连接边（matchNode <- executionNode）
                    preMatchNodes.remove(executionNode2);
                    // 后连接边（executionNode -> matchNode）
                    pathMap.get(executionNode1).addAll(pathMap.get(executionNode2));
                    //合并后续的match节点
                    this.mergeMatchNode(executionNode1);
                    //移除被合并的节点
                    pathMap.remove(executionNode2);
                    //修改list中被合并的节点(后者)位置，避免重复
                    executionNodeList[j] = executionNode1;
                }
            }
        }
    }

    // TODO: 验证该路径的可行性（各服务是否可用、结构化兼容情况）
    public boolean isAvailable() {
        boolean isAvailable = true;
        for (ServiceNode serviceNode : involvedServiceNodes) {
            if (!serviceNode.getService().isAvailable()) {
                isAvailable = false;
                break;
            }
        }
        return isAvailable;
    }

    // TODO: 执行，返回执行结果
    public Object[] run(List<Object> args) {
        System.out.println(this);
        System.out.println("**** Run ****");
        System.out.println("Args:" + args);

        matchArgs = new HashMap<>();
        nextExecutions = new HashSet<>();

        // 初始输入，放入二级节点
        // 目标服务的参数列表，args 和 initialInputs 一一对应
        List<DataNode> initialInputs = solution.getTargetServiceNode().getInputs();
        // 把实际参数和 matchNode 对应起来
        Set<ExecutionPathNode> matchNodes = pathMap.get(START_NODE);
        for (ExecutionPathNode node : matchNodes) {
            MatchNode matchNode = (MatchNode) node;
            DataNode sourceDataNode = matchNode.getMatchSourceNode();
            // source 在提供的 args 中的位置
            int argIdx = initialInputs.indexOf(sourceDataNode);
            // 保存对应关系
            matchArgs.put(matchNode, args.get(argIdx));
            // 后续的 execution 放入 nextExecutions 中
            nextExecutions.addAll(pathMap.get(node));
        }

//        System.out.println(matchArgs);
//        System.out.println(nextExecutions);
        return this.recursiveExecute();

    }

    private Object[] recursiveExecute() {
        for (ExecutionPathNode pathNode : nextExecutions) {
            ExecutionNode executionNode = (ExecutionNode) pathNode;
            Set<MatchNode> requiredInputMatches = preMatchNodes.get(executionNode);
            // 如果 execution 需要的 matchNode 都已经有了对应数据，则可以执行
            if (matchArgs.keySet().containsAll(requiredInputMatches)) {
                // 如果 end 满足条件，即结束执行
                if (executionNode == END_NODE) {
                    List<DataNode> requiredOutputs = solution.getTargetServiceNode().getOutputs();
                    Set<MatchNode> actualOutputMatches = preMatchNodes.get(END_NODE);
                    Object[] actualOutputs = new Object[requiredOutputs.size()];
                    for (MatchNode matchNode : actualOutputMatches) {
                        DataNode requiredOutput = matchNode.getMatchTargetNode();
                        int outputIdx = requiredOutputs.indexOf(requiredOutput);
                        actualOutputs[outputIdx] = matchArgs.get(matchNode);
                    }
                    return actualOutputs;
                }
                // 本次执行的服务及其节点
                ServiceNode serviceNode = executionNode.getServiceNode();
                Service service = serviceNode.getService();
                // 1. 处理输入
                // 对于每个 matchNode，找到其在调用参数中的位置
                List<DataNode> requiredInputs = serviceNode.getInputs();
                Object[] inputs = new Object[requiredInputs.size()];//实际调用参数列表
                for (MatchNode matchNode : requiredInputMatches) {
                    DataNode requiredArg = matchNode.getMatchTargetNode();
                    int argIdx = requiredInputs.indexOf(requiredArg);
                    inputs[argIdx] = matchArgs.get(matchNode);
                }
                // 2. 执行，移出 nextExecutions
                // inputs构造完毕，调用服务，得到输出
                nextExecutions.remove(pathNode);
                Object[] outputs = service.run(inputs);
                List<Object> outputList = Arrays.asList(outputs);
                // 3. 处理输出
                // 输出存入 matchArgs，并找出后续 execution
                List<DataNode> expectedOutputs = serviceNode.getOutputs();
                Set<ExecutionPathNode> producedMatches = pathMap.get(executionNode);
                for (ExecutionPathNode producedNode : producedMatches) {
                    MatchNode producedMatchNode = (MatchNode) producedNode;
                    DataNode producedOutput = producedMatchNode.getMatchSourceNode();
                    int outputIdx = expectedOutputs.indexOf(producedOutput);
                    matchArgs.put(producedMatchNode, outputList.get(outputIdx));
                    nextExecutions.addAll(pathMap.get(producedNode));
                }
                break;
            }
        }
        return this.recursiveExecute();
    }

    public Map<ExecutionNode, Set<DataNode>> getUnresolvedExecutionHeads() {
        return unresolvedExecutionHeads;
    }

    public Map<ExecutionNode, Integer> getUnresolvedHeadLength() {
        return unresolvedHeadLength;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        ExecutionPath clone = (ExecutionPath) super.clone();
        // 复制匹配连接边
        clone.pathMap = new HashMap<>();
        for (Map.Entry<ExecutionPathNode, Set<ExecutionPathNode>> entry : this.pathMap.entrySet()) {
            ExecutionPathNode key = entry.getKey();
            Set<ExecutionPathNode> value = entry.getValue();
            Set<ExecutionPathNode> cloneValue = new HashSet<>(value);
            clone.pathMap.put(key, cloneValue);
        }
        // 复制前置匹配节点
        clone.preMatchNodes = new HashMap<>();
        for (Map.Entry<ExecutionNode, Set<MatchNode>> entry : this.preMatchNodes.entrySet()) {
            ExecutionNode key = entry.getKey();
            Set<MatchNode> value = entry.getValue();
            Set<MatchNode> cloneValue = new HashSet<>(value);
            clone.preMatchNodes.put(key, cloneValue);
        }
        // 复制尚未匹配的节点map
        clone.unresolvedExecutionHeads = new HashMap<>();
        for (Map.Entry<ExecutionNode, Set<DataNode>> entry : this.unresolvedExecutionHeads.entrySet()) {
            ExecutionNode key = entry.getKey();
            Set<DataNode> value = entry.getValue();
            Set<DataNode> cloneValue = new HashSet<>(value);
            clone.unresolvedExecutionHeads.put(key, cloneValue);
        }
        // 复制待匹配节点之后的路径长度
        clone.unresolvedHeadLength = new HashMap<>();
        for (Map.Entry<ExecutionNode, Integer> entry : this.unresolvedHeadLength.entrySet()) {
            ExecutionNode key = entry.getKey();
            Integer value = entry.getValue();
            clone.unresolvedHeadLength.put(key, value);
        }
        // 复制涉及的服务（节点）
        clone.involvedServiceNodes = new HashSet<>(this.involvedServiceNodes);
        return clone;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("=======\nExecution Path {\n");
        builder.append("involved:\n\t").append(involvedServiceNodes).append("\n");
        builder.append("path:\n");
        generatePrintString(builder, START_NODE, 1);
        builder.append("\n}");
        return builder.toString();
    }

    //toString Helper
    private void generatePrintString(StringBuilder builder, ExecutionPathNode head, int tabCount) {
        for (int i = 0; i < tabCount; i++) {
            builder.append("\t");
        }
        builder.append(head);
        if (head == END_NODE) return;
        builder.append(pathMap.get(head));
        for (ExecutionPathNode matchNode : pathMap.get(head)) {
            for (ExecutionPathNode nextExecutionNode : pathMap.get(matchNode)) {
                builder.append("\n");
                generatePrintString(builder, nextExecutionNode, tabCount + 1);
            }
        }
    }
}