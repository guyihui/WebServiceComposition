package serviceUtils;

import serviceUtils.serviceGraph.DataNode;
import serviceUtils.serviceGraph.ServiceGraph;
import serviceUtils.serviceGraph.ServiceNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public int round = -1;

    public ServiceNode getTargetServiceNode() {
        return targetServiceNode;
    }

    public void setTargetServiceNode(ServiceNode targetServiceNode) {
        this.targetServiceNode = targetServiceNode;
    }

    // 剪枝：删去反向不可达路径、节点
    public void prune() {
        List<DataNode> outputs = targetServiceNode.getOutputs();
        Set<DataNode> validDataNode = new HashSet<>();// 已经分析过的节点
        Set<DataNode> validDataNodeNew = new HashSet<>(outputs);// 通过反向路径查找
        Set<ServiceNode> validServiceNode = new HashSet<>();// 反向可达服务
        Set<ServiceNode> validServiceNodeNew = new HashSet<>();// 反向可达服务

        // 扩展 solution 中的其他节点
        Set<DataNode> expandSet = new HashSet<>();
        reverseExpand(validDataNodeNew, expandSet);

        // 根据反向遍历的新节点，判断服务反向可达性
        while (validDataNodeNew.size() != 0) {
            for (DataNode node : validDataNodeNew) {
                if (node.type == DataNode.Type.INPUT) {// 跳过输入节点
                    continue;
                }
                // output节点对应的服务直接标记可达
                ServiceNode serviceNode = node.getServiceNode();
                if (this.serviceNodeSet.contains(serviceNode)) {
                    validServiceNodeNew.add(serviceNode);
                }
            }
            // 获取服务完毕的节点，移入valid
            validDataNode.addAll(validDataNodeNew);
            validDataNodeNew.clear();
            // 得到反向可达服务后，将输入节点作为下一轮 data node
            for (ServiceNode serviceNode : validServiceNodeNew) {
                validDataNodeNew.addAll(serviceNode.getInputs());
            }
            // 扩展
            reverseExpand(validDataNodeNew, expandSet);
            // 确定反向可达的服务移入valid
            validServiceNode.addAll(validServiceNodeNew);
            this.serviceNodeSet.removeAll(validServiceNodeNew);
            validServiceNodeNew.clear();
        }
        // 反向遍历结束，直接使用 valid 作为 solution 内容，另外加上初始输入
        this.serviceNodeSet = validServiceNode;
        this.dataNodeSet = validDataNode;
        this.dataNodeSet.addAll(targetServiceNode.getInputs());
    }

    // 只从输入反向扩展到输出，初始输入额外处理
    private void reverseExpand(Set<DataNode> validDataNodeNew, Set<DataNode> expandSet) {
        for (DataNode node : validDataNodeNew) {
            for (DataNode toExpand : this.dataNodeSet) {
                if (node != toExpand // 不同节点
                        && toExpand.type != DataNode.Type.INPUT // 反向遍历不扩展到其他输入节点
                        && similarityMap.get(node).get(toExpand) >= similarityLimit) {
                    expandSet.add(toExpand);
                }
            }
        }
        validDataNodeNew.addAll(expandSet);
        expandSet.clear();
        this.dataNodeSet.removeAll(validDataNodeNew);
    }

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
                + "\tRound: " + round + "\n"
                + "}";
    }

}
