package serviceUtils.executionPath;


//TODO: 环情况
public class ExecutionPath implements Cloneable {
    public ExecutionNode startNode = new ExecutionNode(ExecutionNode.Type.START, null);
    public ExecutionNode endNode = new ExecutionNode(ExecutionNode.Type.END, null);

    @Override
    public Object clone() throws CloneNotSupportedException {
        Object obj = super.clone();
        return obj;
    }

    //TODO: 对路径的重复部分进行合并
    public void merge() {
        System.out.println("**** Function not implemented. ****");
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("=======\nExecution Path {\n");
        generatePrintString(builder, startNode, 1);
        builder.append("\n}");
        return builder.toString();
    }

    //toString Helper
    private void generatePrintString(StringBuilder builder, ExecutionNode head, int tabCount) {
        for (int i = 0; i < tabCount; i++) {
            builder.append("\t");
        }
        builder.append(head);
        builder.append(head.getOutputMatchNodeSet());
        for (MatchNode matchNode : head.getOutputMatchNodeSet()) {
            builder.append("\n");
            generatePrintString(builder, matchNode.getToNode(), tabCount + 1);
        }
    }
}