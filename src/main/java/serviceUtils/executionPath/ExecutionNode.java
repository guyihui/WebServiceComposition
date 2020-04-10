package serviceUtils.executionPath;

import serviceUtils.serviceGraph.ServiceNode;

import java.util.HashSet;
import java.util.Set;

public class ExecutionNode {
    public enum Type {
        START, COMPONENT, END,
    }

    public Type type;
    public ServiceNode serviceNode;
    public Set<MatchNode> inputMatchNodeSet = new HashSet<>();
    public Set<MatchNode> outputMatchNodeSet = new HashSet<>();

    public ExecutionNode(Type type, ServiceNode serviceNode) {
        this.type = type;
        this.serviceNode = serviceNode;
    }

    public void addInputMatchNode(MatchNode inputMatchNode) {
        this.inputMatchNodeSet.add(inputMatchNode);
    }

    public void addOutputMatchNode(MatchNode inputMatchNode) {
        this.outputMatchNodeSet.add(inputMatchNode);
    }

    public ServiceNode getServiceNode() {
        return serviceNode;
    }

    public Set<MatchNode> getInputMatchNodeSet() {
        return inputMatchNodeSet;
    }

    public Set<MatchNode> getOutputMatchNodeSet() {
        return outputMatchNodeSet;
    }

    @Override
    public String toString() {
        String testString = "";
        switch (this.type) {
            case START:
                testString += "Start";
                break;
            case COMPONENT:
                testString += "Component {" + this.getServiceNode() + "}";
                break;
            case END:
                testString += "End";
                break;
            default:
                testString += "UNKNOWN";
        }
        return testString;
    }
}
