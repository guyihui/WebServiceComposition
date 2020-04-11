package serviceUtils.executionPath;

import serviceUtils.serviceGraph.DataNode;

public class MatchNode implements ExecutionPathNode {
    public DataNode matchSourceNode;
    public DataNode matchTargetNode;

    public MatchNode(DataNode matchSourceNode, DataNode matchTargetNode) {
        this.matchSourceNode = matchSourceNode;
        this.matchTargetNode = matchTargetNode;
    }

    @Override
    public String toString() {
        return "(" + matchSourceNode + "->" + matchTargetNode + ")";
    }
}
