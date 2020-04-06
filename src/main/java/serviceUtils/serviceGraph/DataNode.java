package serviceUtils.serviceGraph;

public class DataNode {
    enum Type {
        INPUT, OUTPUT,
    }

    Type type;
    private String word;
    private ServiceNode serviceNode;

    public DataNode(Type type, String word, ServiceNode serviceNode) {
        this.type = type;
        this.word = word;
        this.serviceNode = serviceNode;
    }

    private DataNode() {
    }

    public ServiceNode getServiceNode() {
        return serviceNode;
    }

    public void setServiceNode(ServiceNode serviceNode) {
        this.serviceNode = serviceNode;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    @Override
    public String toString() {
        return word;
    }
}
