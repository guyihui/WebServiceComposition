package serviceUtils.executionPath;

import serviceUtils.serviceGraph.ServiceNode;

public class ExecutionNode implements ExecutionPathNode {
    public enum Type {
        START, COMPONENT, END,
    }

    public Type type;
    private ServiceNode serviceNode;

    public ExecutionNode(Type type, ServiceNode serviceNode) {
        this.type = type;
        this.serviceNode = serviceNode;
    }

    public boolean mayMergeWith(ExecutionNode other) {
        return this != other
                && this.type == Type.COMPONENT && other.type == Type.COMPONENT
                && this.serviceNode == other.serviceNode;
    }

    public ServiceNode getServiceNode() {
        return serviceNode;
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
