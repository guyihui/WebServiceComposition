package serviceUtils.executionPath;

import serviceUtils.serviceGraph.DataNode;

public class MatchNode implements ExecutionPathNode {
    private DataNode matchSourceNode;
    private DataNode matchTargetNode;

    public MatchNode(DataNode matchSourceNode, DataNode matchTargetNode) {
        this.matchSourceNode = matchSourceNode;
        this.matchTargetNode = matchTargetNode;
    }

    @Override
    public String toString() {
        return "(" + matchSourceNode + "->" + matchTargetNode + ")";
    }
}
