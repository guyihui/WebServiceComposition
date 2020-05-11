package com.sjtu.composition.graph.executionPath;

import com.sjtu.composition.graph.serviceGraph.ParamNode;

public class MatchNode implements ExecutionPathNode {
    private ParamNode matchSourceNode;
    private ParamNode matchTargetNode;

    public MatchNode(ParamNode matchSourceNode, ParamNode matchTargetNode) {
        this.matchSourceNode = matchSourceNode;
        this.matchTargetNode = matchTargetNode;
    }

    public boolean mayMergeWith(MatchNode other) {
        return this != other
                && this.matchSourceNode == other.matchSourceNode
                && this.matchTargetNode == other.matchTargetNode;
    }

    public ParamNode getMatchSourceNode() {
        return matchSourceNode;
    }

    public ParamNode getMatchTargetNode() {
        return matchTargetNode;
    }

    @Override
    public String toString() {
        return "(" + matchSourceNode + "->" + matchTargetNode + ")";
    }
}
