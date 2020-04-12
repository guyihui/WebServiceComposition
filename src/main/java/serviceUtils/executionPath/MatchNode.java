package serviceUtils.executionPath;

import serviceUtils.serviceGraph.DataNode;

public class MatchNode implements ExecutionPathNode {
    private DataNode matchSourceNode;
    private DataNode matchTargetNode;

    public MatchNode(DataNode matchSourceNode, DataNode matchTargetNode) {
        this.matchSourceNode = matchSourceNode;
        this.matchTargetNode = matchTargetNode;
    }

    public boolean mayMergeWith(MatchNode other) {
        return this != other
                && this.matchSourceNode == other.matchSourceNode
                && this.matchTargetNode == other.matchTargetNode;
    }

    @Override
    public String toString() {
        return "(" + matchSourceNode + "->" + matchTargetNode + ")";
    }
}
