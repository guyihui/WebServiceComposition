package com.sjtu.composition.graph.serviceGraph;

import com.sjtu.composition.graph.CompositionSolution;
import com.sjtu.composition.serviceUtils.Service;

import java.util.*;

//TODO: 多线程
public class ServiceGraph {

    protected Set<DataNode> dataNodeSet = new HashSet<>();
    protected Map<Integer, ServiceNode> serviceNodeMap = new HashMap<>();
    // TODO：作为value的Collection应当预排序
    protected static Map<DataNode, Map<DataNode, Double>> similarityMap = new HashMap<>();

    public ServiceGraph() {
    }

    public final boolean addService(Service service) {
        if (this.containsService(service)) {
            return false;// 已经存在的服务不允许重复添加，只能修改
        }
        ServiceNode serviceNode = new ServiceNode(service);
        List<DataNode> inputs = new ArrayList<>();
        List<DataNode> outputs = new ArrayList<>();
        // 构造输入输出节点及其与serviceNode之间的连接边
        for (String output : service.getOutputs()) {
            DataNode outputNode = new DataNode(DataNode.Type.OUTPUT, output, serviceNode);
            this.addDataNode(outputs, outputNode);
        }
        for (String input : service.getInputs()) {
            DataNode inputNode = new DataNode(DataNode.Type.INPUT, input, serviceNode);
            this.addDataNode(inputs, inputNode);
        }
        serviceNode.setInputs(inputs);
        serviceNode.setOutputs(outputs);
        serviceNodeMap.put(service.getId(), serviceNode);
        return true;
    }

    // dataNode 加入list，并且计算与其他所有 dataNode 之间的相似度，存入map
    private void addDataNode(List<DataNode> dataNodeList, DataNode dataNode) {
        similarityMap.put(dataNode, new HashMap<>());
        for (DataNode node : dataNodeSet) {
            double similarity = this.mockSimilarity(dataNode, node);
            similarityMap.get(node).put(dataNode, similarity);
            similarityMap.get(dataNode).put(node, similarity);
        }
        dataNodeSet.add(dataNode);
        dataNodeList.add(dataNode);
    }

    public boolean containsService(Service service) {
        return serviceNodeMap.containsKey(service.getId());
    }

    // TODO: 删除/修改服务

    // TODO: 根据词向量模型获取相似度
    // TODO: 同一个服务的输入输出是否需要设置相似度<=0.0，代表不允许语义匹配
    private double mockSimilarity(DataNode node1, DataNode node2) {
        String word1 = node1.getWord();
        String word2 = node2.getWord();
        return word1.equals(word2) ? 1.0 : 0.0;
    }

    // TODO: 搜索新加入的服务替换方案（图中不包含）
    // TODO: 是否需要允许在满足所有输出后额外多搜几轮
    // TODO: 会不会存在无输入/输出的服务
    public final CompositionSolution search(Service service, double similarityLimit, int roundLimit) {
        CompositionSolution solution = new CompositionSolution(service, similarityLimit, roundLimit);
        // 寻找图中是否存在该服务
        ServiceNode targetServiceNode = serviceNodeMap.get(service.getId());
        if (targetServiceNode == null) {
            return solution;
        } else {
            solution.isExistingService = true;
            solution.setTargetServiceNode(targetServiceNode);
        }

        // 对于图中某个已经存在的服务进行方案搜索
        if (solution.isExistingService && solution.getTargetServiceNode() != null) {
            List<DataNode> inputs = targetServiceNode.getInputs();
            List<DataNode> outputs = targetServiceNode.getOutputs();

            Set<DataNode> availableDataNode = new HashSet<>();// 已经扩展过的节点
            Set<DataNode> availableDataNodeNew = new HashSet<>(inputs);// 需要根据相似度进行扩展的节点
            Set<DataNode> unresolvedTarget = new HashSet<>(outputs);// 最终需要得到的输出

            solution.getDataNodeSet().addAll(inputs);
            solution.getDataNodeSet().addAll(outputs);

            // 首先根据相似度进行扩展
            this.expandBySimilarity(similarityLimit, availableDataNodeNew);

            int round = 0;
            // 当 target 输出还不能全部获得时，并且没有超出轮数限制
            while (unresolvedTarget.size() > 0 && round++ < roundLimit) {
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
                        if (!availableDataNode.contains(input) && !availableDataNodeNew.contains(input)) {
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
                // 根据下一步可使用服务，得到这些服务的输出
                Set<DataNode> outputsNew = new HashSet<>();
                for (ServiceNode serviceNode : availableServiceNode) {
                    outputsNew.addAll(serviceNode.getOutputs());
                }
                // 根据相似度对结果进行扩展
                this.expandBySimilarity(similarityLimit, outputsNew);
                // 保存solution
                for (ServiceNode available : availableServiceNode) {
                    solution.getServiceNodeMap().put(available.getService().getId(), available);
                    solution.getDataNodeSet().addAll(available.getInputs());
                    solution.getDataNodeSet().addAll(available.getOutputs());
                }
                // 若 target 被满足，从 target 集合中移除
                Set<DataNode> resolvedTarget = new HashSet<>();
                for (DataNode node : unresolvedTarget) {
                    if (outputsNew.contains(node)) {
                        resolvedTarget.add(node);
                    }
                }
                unresolvedTarget.removeAll(resolvedTarget);
                // 如果 target 全部得到，则提前结束
                if (unresolvedTarget.isEmpty()) {
                    solution.isResolved = true;
                    solution.round = round;
                    break;
                }
                // 已经在 available 中的跳过，新产生的节点加入 availableNew
                for (DataNode node : outputsNew) {
                    if (!availableDataNode.contains(node)) {
                        availableDataNodeNew.add(node);
                    }
                }
            }
        }
        solution.prune();
        solution.collectMatchEdge();
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

    public Map<Integer, ServiceNode> getServiceNodeMap() {
        return serviceNodeMap;
    }

    @Override
    public String toString() {
        return "=======\nService Graph {\n"
                + "\tData nodes(" + dataNodeSet.size() + "): " + dataNodeSet.toString() + "\n"
                + "\tService nodes(" + serviceNodeMap.size() + "): " + serviceNodeMap.toString() + "\n"
                + "\tMap size: " + similarityMap.size() + "\n"
                + "}";
    }
}
