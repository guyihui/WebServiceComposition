package serviceUtils;

import serviceUtils.serviceGraph.ServiceGraph;
import serviceUtils.serviceGraph.ServiceNode;

// 包含了可能的多种执行方案的整体
public class CompositionSolution extends ServiceGraph {

    private Service targetService;
    private double similarityLimit;
    private int roundLimit;

    public CompositionSolution(Service targetService, double similarityLimit, int roundLimit) {
        this.targetService = targetService;
        this.similarityLimit = similarityLimit;
        this.roundLimit = roundLimit;
    }

    private CompositionSolution() {
    }

    public boolean isResolved = false;
    public boolean isExistingService = false;
    private ServiceNode targetServiceNode = null;

    public ServiceNode getTargetServiceNode() {
        return targetServiceNode;
    }

    public void setTargetServiceNode(ServiceNode targetServiceNode) {
        this.targetServiceNode = targetServiceNode;
    }

    // TODO: 剪枝：删去反向不可达路径
    // TODO: 提取：得到无歧义/多余路径的实际执行路径（可能存在多种）
    // TODO: 选择：若有多种可能，根据一定约束进行选择

    @Override
    public String toString() {
        return "=======\nComposition solution {\n"
                + "\tTarget: " + this.targetService + "\n"
                + "\tSimilarity limit: " + this.similarityLimit + "\n"
                + "\tRound limit: " + this.roundLimit + "\n"
                + "\tData nodes(" + dataNodeSet.size() + "): " + dataNodeSet.toString() + "\n"
                + "\tService nodes(" + serviceNodeSet.size() + "): " + serviceNodeSet.toString() + "\n"
                + "\tMap size: " + similarityMap.size() + "\n"
                + "}";
    }

}
