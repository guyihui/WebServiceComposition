package serviceUtils.executionPath;

import serviceUtils.serviceGraph.DataNode;

import java.util.*;

//TODO: 环情况
public class ExecutionPath implements Cloneable {
    public static final ExecutionNode START_NODE = new ExecutionNode(ExecutionNode.Type.START, null);
    public static final ExecutionNode END_NODE = new ExecutionNode(ExecutionNode.Type.END, null);
    private Map<ExecutionNode, Set<DataNode>> unresolvedExecutionHeads = new HashMap<>();//<需要匹配的头节点，其需要匹配的输入>
    private Map<ExecutionPathNode, Set<ExecutionPathNode>> pathMap = new HashMap<>();//连接边
    private Map<ExecutionNode, Set<MatchNode>> preMatchNodes = new HashMap<>();//每个运行节点需要的前置matchNode

    public ExecutionPath(Collection<DataNode> matchTargets) {
        Set<DataNode> toMatchSet = new HashSet<>(matchTargets);
        unresolvedExecutionHeads.put(END_NODE, toMatchSet);
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
            return preMatch1.containsAll(preMatch2) && preMatch2.containsAll(preMatch1);
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

    public Map<ExecutionNode, Set<DataNode>> getUnresolvedExecutionHeads() {
        return unresolvedExecutionHeads;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        ExecutionPath clone = (ExecutionPath) super.clone();
        // 复制尚未匹配的节点map
        clone.unresolvedExecutionHeads = new HashMap<>();
        for (Map.Entry<ExecutionNode, Set<DataNode>> entry : this.unresolvedExecutionHeads.entrySet()) {
            ExecutionNode key = entry.getKey();
            Set<DataNode> value = entry.getValue();
            Set<DataNode> cloneValue = new HashSet<>(value);
            clone.unresolvedExecutionHeads.put(key, cloneValue);
        }
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
        return clone;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("=======\nExecution Path {\n");
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