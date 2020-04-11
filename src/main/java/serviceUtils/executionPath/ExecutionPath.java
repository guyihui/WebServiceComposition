package serviceUtils.executionPath;


import serviceUtils.serviceGraph.DataNode;

import java.util.*;

//TODO: 环情况
public class ExecutionPath implements Cloneable {
    public static final ExecutionNode START_NODE = new ExecutionNode(ExecutionNode.Type.START, null);
    public static final ExecutionNode END_NODE = new ExecutionNode(ExecutionNode.Type.END, null);
    private Map<ExecutionNode, Set<DataNode>> unresolvedExecutionHeads = new HashMap<>();//<需要匹配的头节点，其需要匹配的输入>
    private Map<ExecutionPathNode, Set<ExecutionPathNode>> pathMap = new HashMap<>();//连接边

    public ExecutionPath(Collection<DataNode> matchTargets) {
        Set<DataNode> toMatchSet = new HashSet<>(matchTargets);
        unresolvedExecutionHeads.put(END_NODE, toMatchSet);
    }

    // 连接路径
    public void connect(ExecutionPathNode former, ExecutionPathNode latter) {
        pathMap.computeIfAbsent(former, k -> new HashSet<>()).add(latter);
    }

    //TODO: 对路径的重复部分进行合并
    public void merge() {
        System.out.println("**** Function not implemented. ****");
    }

    public Map<ExecutionNode, Set<DataNode>> getUnresolvedExecutionHeads() {
        return unresolvedExecutionHeads;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        ExecutionPath clone = (ExecutionPath) super.clone();
        clone.unresolvedExecutionHeads = new HashMap<>();
        for (Map.Entry<ExecutionNode, Set<DataNode>> entry : this.unresolvedExecutionHeads.entrySet()) {
            ExecutionNode key = entry.getKey();
            Set<DataNode> value = entry.getValue();
            Set<DataNode> cloneValue = new HashSet<>(value);
            clone.unresolvedExecutionHeads.put(key, cloneValue);
        }
        clone.pathMap = new HashMap<>();
        for (Map.Entry<ExecutionPathNode, Set<ExecutionPathNode>> entry : this.pathMap.entrySet()) {
            ExecutionPathNode key = entry.getKey();
            Set<ExecutionPathNode> value = entry.getValue();
            Set<ExecutionPathNode> cloneValue = new HashSet<>(value);
            clone.pathMap.put(key, cloneValue);
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
            builder.append("\n");
            generatePrintString(builder, pathMap.get(matchNode).iterator().next(), tabCount + 1);
        }
    }
}