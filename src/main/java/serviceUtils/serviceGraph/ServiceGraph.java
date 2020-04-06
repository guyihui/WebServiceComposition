package serviceUtils.serviceGraph;

import serviceUtils.CompositionSolution;
import serviceUtils.Service;

import java.util.*;

public class ServiceGraph {

    protected Set<DataNode> dataNodeSet = new HashSet<>();
    protected Set<ServiceNode> serviceNodeSet = new HashSet<>();
    // TODO：作为value的Collection应当预排序
    protected static Map<DataNode, Map<DataNode, Double>> similarityMap = new HashMap<>();

    public ServiceGraph() {
    }

    // TODO: 根据词向量模型获取相似度
    // TODO: 同一个服务的输入输出是否需要设置相似度<=0.0，代表不允许语义匹配
    private double mockSimilarity(DataNode node1, DataNode node2) {
        String word1 = node1.getWord();
        String word2 = node2.getWord();
        return word1.equals(word2) ? 1.0 : 0.0;
    }

    public final void addService(Service service) {
        ServiceNode serviceNode = new ServiceNode(service);
        List<DataNode> inputs = new ArrayList<>();
        List<DataNode> outputs = new ArrayList<>();
        for (String output : service.getOutputs()) {
            DataNode outputNode = new DataNode(DataNode.Type.OUTPUT, output, serviceNode);
            similarityMap.put(outputNode, new HashMap<>());
            for (DataNode node : dataNodeSet) {
                double similarity = mockSimilarity(outputNode, node);
                similarityMap.get(node).put(outputNode, similarity);
                similarityMap.get(outputNode).put(node, similarity);
            }
            dataNodeSet.add(outputNode);
            outputs.add(outputNode);
        }
        for (String input : service.getInputs()) {
            DataNode inputNode = new DataNode(DataNode.Type.INPUT, input, serviceNode);
            similarityMap.put(inputNode, new HashMap<>());
            for (DataNode node : dataNodeSet) {
                double similarity = mockSimilarity(inputNode, node);
                similarityMap.get(node).put(inputNode, similarity);
                similarityMap.get(inputNode).put(node, similarity);
            }
            dataNodeSet.add(inputNode);
            inputs.add(inputNode);
        }
        serviceNode.setInputs(inputs);
        serviceNode.setOutputs(outputs);
        serviceNodeSet.add(serviceNode);
    }

    // TODO: 是否需要允许在满足所有输出后额外多搜几轮
    public final CompositionSolution search(Service service, double similarityLimit, int roundLimit) {
        CompositionSolution solution = new CompositionSolution(service, similarityLimit, roundLimit);
        // 寻找图中是否存在该服务
        ServiceNode targetServiceNode = null;
        for (ServiceNode node : serviceNodeSet) {
            if (node.getService().getId() == service.getId()) {
                solution.isExistingService = true;
                targetServiceNode = node;
                solution.setTargetServiceNode(node);
                break;
            }
        }
        // 对于图中某个已经存在的服务进行方案搜索
        if (solution.isExistingService && solution.getTargetServiceNode() != null) {
            List<DataNode> inputs = targetServiceNode.getInputs();
            List<DataNode> outputs = targetServiceNode.getOutputs();

            Set<DataNode> availableDataNode = new HashSet<>();// 已经扩展过的节点
            Set<DataNode> availableDataNodeNew = new HashSet<>(inputs);// 需要根据相似度进行扩展的节点
            Set<DataNode> unresolvedTarget = new HashSet<>(outputs);// 最终需要得到的输出

            // 首先根据相似度进行扩展
            expandBySimilarity(similarityLimit, availableDataNodeNew);
            // 保存在 solution 中
            solution.getDataNodeSet().addAll(availableDataNodeNew);

            int round = 0;
            // 当 target 输出还不能全部获得时，并且没有超出轮数限制
            while (unresolvedTarget.size() > 0 && round++ < roundLimit) {
                System.out.println("Round " + round);

                // 根据 加入的新输入节点 获取可执行服务
                Set<ServiceNode> availableServiceNode = new HashSet<>();
                Set<ServiceNode> unavailableServiceNode = new HashSet<>();
                for (DataNode node : availableDataNodeNew) {
                    if (node.type == DataNode.Type.OUTPUT) { //跳过输出节点
                        continue;
                    }
                    ServiceNode serviceNode = node.getServiceNode(); // 输入节点指向的服务
                    if (availableServiceNode.contains(serviceNode)
                            || unavailableServiceNode.contains(serviceNode)
                            || serviceNode == targetServiceNode) {
                        continue;
                    }
                    // 对于未判断过的服务，检查服务输入是否 available
                    boolean isAvailable = true;
                    for (DataNode input : serviceNode.getInputs()) {
                        if (availableDataNode.contains(input) || availableDataNodeNew.contains(input)) {
                            continue;
                        } else {
                            isAvailable = false;// 有输入unavailable
                            break;
                        }
                    }
                    if (isAvailable) {
                        availableServiceNode.add(serviceNode);
                    } else {
                        unavailableServiceNode.add(serviceNode);
                    }
                }
                // 获取服务完毕的节点从 availableNew 移至 available
                availableDataNode.addAll(availableDataNodeNew);
                availableDataNodeNew.clear();
                // 根据下一步可使用服务后，得到这些服务的输出
                Set<DataNode> outputsNew = new HashSet<>();
                for (ServiceNode serviceNode : availableServiceNode) {
                    outputsNew.addAll(serviceNode.getOutputs());
                }
                // 根据相似度对结果进行扩展
                expandBySimilarity(similarityLimit, outputsNew);
                // 保存solution
                solution.getServiceNodeSet().addAll(availableServiceNode);
                solution.getDataNodeSet().addAll(outputsNew);
                // 若 target 被满足，从 target 集合中移除
                Set<DataNode> resolvedTarget = new HashSet<>();
                for (DataNode node : unresolvedTarget) {
                    if (outputsNew.contains(node)) {
                        resolvedTarget.add(node);
                    }
                }
                unresolvedTarget.removeAll(resolvedTarget);
                // 如果 target 全部得到，则提前结束
                if (unresolvedTarget.size() == 0) {
                    solution.isResolved = true;
                    break;
                }
                // 已经在 available 中的跳过，新产生的节点加入 availableNew
                // TODO: 如果是产生已有的输出，需不需要考虑新的输出？不会漏相关服务，但是执行路径可能有影响（环）
                for (DataNode node : outputsNew) {
                    if (!availableDataNode.contains(node)) {
                        availableDataNodeNew.add(node);
                    }
                }
            }
        }
        return solution;
    }

    // TODO: 遍历是否影响性能？改为预排序结构后需要重构
    private void expandBySimilarity(double similarityLimit, Set<DataNode> nodeSet) {
        Set<DataNode> expandSet = new HashSet<>();
        for (DataNode node : nodeSet) {
            Map<DataNode, Double> similarities = similarityMap.get(node);
            for (DataNode other : similarities.keySet()) {
                if (similarities.get(other) >= similarityLimit) {// 达到相似度阈值
                    expandSet.add(other);
                }
            }
        }
        nodeSet.addAll(expandSet);
    }

    public Set<DataNode> getDataNodeSet() {
        return dataNodeSet;
    }

    public Set<ServiceNode> getServiceNodeSet() {
        return serviceNodeSet;
    }

}
