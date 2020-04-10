package serviceUtils.executionPath;

import serviceUtils.serviceGraph.DataNode;

public class MatchNode {
    public DataNode matchSourceNode;
    public DataNode matchTargetNode;
    public ExecutionNode fromNode;
    public ExecutionNode toNode;

    public MatchNode(DataNode matchSourceNode, DataNode matchTargetNode) {
        this.matchSourceNode = matchSourceNode;
        this.matchTargetNode = matchTargetNode;
    }

    public ExecutionNode getFromNode() {
        return fromNode;
    }

    public void setFromNode(ExecutionNode fromNode) {
        this.fromNode = fromNode;
    }

    public ExecutionNode getToNode() {
        return toNode;
    }

    public void setToNode(ExecutionNode toNode) {
        this.toNode = toNode;
    }

    @Override
    public String toString() {
        return "(" + matchSourceNode + "->" + matchTargetNode + ")";
    }
}
