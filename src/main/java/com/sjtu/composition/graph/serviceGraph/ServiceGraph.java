package com.sjtu.composition.graph.serviceGraph;

import com.sjtu.composition.serviceUtils.Parameter;
import com.sjtu.composition.serviceUtils.Service;
import com.sjtu.composition.similarityUtils.SimilarityUtils;

import java.util.*;

public class ServiceGraph {

    // 相似度工具类
    protected SimilarityUtils similarityUtils;
    // 服务图的范围
    protected Service targetService;
    protected Set<Service> serviceCluster;
    // 图相关结构
    protected ServiceNode sourceNode = new ServiceNode(ServiceNode.Type.SOURCE);
    protected ServiceNode sinkNode = new ServiceNode(ServiceNode.Type.SINK);

    // 构造约束
    protected boolean isBuilt = false;
    protected double similarityLimit = 1.0;
    protected int layerLimit = 0;
    // 匹配关系
    protected Map<ParamNode, Map<ParamNode, Double>> matchEdgeMap = new HashMap<>();// 匹配边
    private Set<ParamNode> matchSource = new HashSet<>();//包括初始输入(source节点的输出)和服务的输出
    private Set<ParamNode> matchTarget = new HashSet<>();//包括最终输出(sink节点的输入)和服务的输入
    // 可用服务/节点
    protected Map<Integer, ServiceNode> serviceNodeMap = new HashMap<>();
    protected Set<ParamNode> paramNodeSet = new HashSet<>();


    public ServiceGraph(Service targetService, Set<Service> serviceCluster,
                        SimilarityUtils similarityUtils) {
        // 注入工具类
        this.similarityUtils = similarityUtils;
        // 图内容
        this.targetService = targetService;
        this.serviceCluster = serviceCluster;
        // source node
        Set<ParamNode> targetInputs = new HashSet<>();
        for (Parameter parameter : targetService.getRequestParams()) {
            targetInputs.add(new ParamNode(ParamNode.Type.OUTPUT, parameter, this.sourceNode));
        }
        this.sourceNode.setOutputs(targetInputs);
        // sink node
        Set<ParamNode> targetOutputs = new HashSet<>();
        for (Parameter parameter : targetService.getResponseParams()) {
            targetOutputs.add(new ParamNode(ParamNode.Type.INPUT, parameter, this.sinkNode));
        }
        this.sinkNode.setInputs(targetOutputs);
    }

    protected boolean build(double similarityLimit, int layerLimit) throws Exception {
        clear();
        prepare(similarityLimit, layerLimit);
        this.isBuilt = expand();
        if (this.isBuilt) {
            dropUnreachableServices();
            cleanInvalidMatchEdge();
            calculateQoSFloor();
        }
        return this.isBuilt;
    }

    /**
     * clear: 清空构造环境
     */
    private void clear() {
        // 约束条件
        this.isBuilt = false;
        this.similarityLimit = 1.0;
        this.layerLimit = 0;
        // 匹配关系
        this.matchEdgeMap.clear();
        this.matchSource.clear();
        this.matchTarget.clear();
        // 可用服务/Param节点
        this.serviceNodeMap.clear();
        this.paramNodeSet.clear();
    }

    /**
     * prepare: 准备构造环境
     * 1. 设置构造约束
     * 2. 服务转换成图节点
     *
     * @param layerLimit      构造层数限制
     * @param similarityLimit 相似度匹配的阈值
     */
    private void prepare(double similarityLimit, int layerLimit) {
        this.similarityLimit = similarityLimit;
        this.layerLimit = layerLimit;
        for (Service service : this.serviceCluster) {
            if (service == this.targetService) {
                continue;
            }
            ServiceNode serviceNode = this.convertServiceToGraph(service);
            // 分类到 matchSource / matchTarget
            this.matchSource.addAll(serviceNode.getOutputs());
            this.matchTarget.addAll(serviceNode.getInputs());
        }
        this.matchSource.addAll(this.sourceNode.getOutputs());
        this.matchTarget.addAll(this.sinkNode.getInputs());
    }

    // 服务转换成图结构 input -> service -> output
    private ServiceNode convertServiceToGraph(Service service) {
        ServiceNode serviceNode = new ServiceNode(service);
        // 输入: 节点, 连接边
        Set<ParamNode> inputs = new HashSet<>();
        for (Parameter inputParam : service.getRequestParams()) {
            inputs.add(new ParamNode(ParamNode.Type.INPUT, inputParam, serviceNode));
        }
        serviceNode.setInputs(inputs);
        // 输出
        Set<ParamNode> outputs = new HashSet<>();
        for (Parameter outputParam : service.getResponseParams()) {
            outputs.add(new ParamNode(ParamNode.Type.OUTPUT, outputParam, serviceNode));
        }
        serviceNode.setOutputs(outputs);
        return serviceNode;
    }

    /**
     * expand: 正向扩展构造服务匹配图
     * 1. 根据相似度扩展
     * 2. 扩展节点所输入的服务（筛选）
     * 3. 保存节点 G(V, )
     * 4. 服务的输出作为下一轮的匹配source（筛选）
     *
     * @return true表示目标服务的全部（或指定）输出可以被匹配替换
     * TODO: 无输入/输出的服务
     */
    private boolean expand() throws Exception {

        Set<ParamNode> validParamNodeSet = new HashSet<>(this.sourceNode.getOutputs());
        Map<Integer, ServiceNode> validServiceNodeMap = new HashMap<>();

        Set<ParamNode> unresolvedTarget = new HashSet<>(this.sinkNode.getInputs());// 最终需要得到的输出
        Set<ParamNode> matchSourceNew = new HashSet<>(this.sourceNode.getOutputs());// 输出得到新一轮 matchSource
        Set<ParamNode> matchTargetNew;// 匹配成功的新一轮 matchTarget（作为服务输入）

        // 1. 首先根据相似度匹配，进行第一层扩展
        matchTargetNew = this.matchBySimilarity(matchSourceNew);// 已经在构造的时候保存

        // 固定轮数内（target 输出可能已经提前全部获得）
        for (int round = 0; round < this.layerLimit; round++) {
            // 2. 根据 匹配得到的新输入节点 获取这一轮新增的可执行服务
            Set<ServiceNode> availableServiceNodeNew = new HashSet<>();
            Set<ServiceNode> unavailableServiceNodeNew = new HashSet<>();
            for (ParamNode node : matchTargetNew) {
                ServiceNode serviceNode = node.getServiceNode(); // 输入节点指向的服务
                if (serviceNode == this.sinkNode
                        || this.serviceNodeMap.containsKey(serviceNode.getService().getId())
                        || availableServiceNodeNew.contains(serviceNode)
                        || unavailableServiceNodeNew.contains(serviceNode)
                        || serviceNode.getService() == this.targetService) {
                    continue;//已经判断过，跳过
                }
                // 对于未判断过的服务，检查服务所有必选输入是否全部 available
                boolean isAvailable = true;
                for (ParamNode input : serviceNode.getInputs()) {
                    if (input.getParam().isRequired()
                            && !validParamNodeSet.contains(input)
                            && !matchTargetNew.contains(input)) {
                        isAvailable = false;// 有输入unavailable
                        break;
                    }
                }
                if (isAvailable) {
                    availableServiceNodeNew.add(serviceNode);
                } else {
                    unavailableServiceNodeNew.add(serviceNode);
                }
            }
            // 3. 保存：获取服务完毕的节点、这一轮可执行服务
            validParamNodeSet.addAll(matchTargetNew);
            for (ServiceNode serviceNode : availableServiceNodeNew) {
                validServiceNodeMap.put(serviceNode.getService().getId(), serviceNode);
            }
            // 4. 根据可使用服务，得到这些服务的输出
            matchSourceNew.clear();
            for (ServiceNode serviceNode : availableServiceNodeNew) {
                matchSourceNew.addAll(serviceNode.getOutputs());
            }
            validParamNodeSet.addAll(matchSourceNew);
            // *1. 根据相似度对结果进行下一轮扩展
            matchTargetNew = this.matchBySimilarity(matchSourceNew);
            unresolvedTarget.removeAll(matchTargetNew);// 若 target 被满足，从 target 集合中移除
            matchTargetNew.removeAll(validParamNodeSet);// 筛选匹配得到的输入（避免重复搜索）
        }

        this.paramNodeSet = validParamNodeSet;
        this.serviceNodeMap = validServiceNodeMap;

        return unresolvedTarget.isEmpty();
    }

    // 正向单次扩展：根据语义相似度，获取可以匹配的节点，保存匹配边 G( ,E)
    private Set<ParamNode> matchBySimilarity(Set<ParamNode> source) throws Exception {
        Set<ParamNode> expandSet = new HashSet<>();
        for (ParamNode node : source) {
            for (ParamNode target : this.matchTarget) {
                double similarity = this.getSimilarity(node, target);
                if (similarity >= this.similarityLimit) {//相似度达到阈值，可以匹配
                    expandSet.add(target);
                    this.matchEdgeMap.computeIfAbsent(node, v -> new HashMap<>()).put(target, similarity);
                    this.matchEdgeMap.computeIfAbsent(target, v -> new HashMap<>()).put(node, similarity);
                }
            }
        }
        return expandSet;
    }

    // 计算两个节点间的相似度
    private double getSimilarity(ParamNode node1, ParamNode node2) throws Exception {
        return similarityUtils.similarity(node1.getParam(), node2.getParam());
    }

    /**
     * drop Unreachable Services: 删去反向不可达路径、节点
     * 1. 反向扩展（沿匹配边）
     * 2. 产生节点的服务标记反向可达（筛选）
     * 3. 保存反向可达服务
     */
    private void dropUnreachableServices() {

        Set<ParamNode> validParamNodeSet = new HashSet<>(this.sinkNode.getInputs());// 反向可达节点，遍历完替换
        Map<Integer, ServiceNode> validServiceNodeMap = new HashMap<>();//反向可达服务，遍历完直接替换

        Set<ParamNode> validMatchTargetNew = new HashSet<>(this.sinkNode.getInputs());// 通过反向路径查找
        Set<ParamNode> validMatchSourceNew;

        // 1. 反向扩展
        validMatchSourceNew = this.reverseExpand(validMatchTargetNew);

        // 根据反向遍历的新节点，判断服务反向可达性
        while (!validMatchSourceNew.isEmpty()) {
            Set<ServiceNode> validServiceNodeNew = new HashSet<>();
            // 2. 标记可达服务
            for (ParamNode node : validMatchSourceNew) {
                // output节点对应的服务直接标记可达
                ServiceNode serviceNode = node.getServiceNode();
                if (serviceNode != this.sourceNode
                        && !validServiceNodeMap.containsKey(serviceNode.getService().getId())) {
                    validServiceNodeNew.add(serviceNode);
                }
            }
            // 3. 保存：获取服务完毕的节点，移入valid；反向可达服务
            validParamNodeSet.addAll(validMatchSourceNew);
            for (ServiceNode serviceNode : validServiceNodeNew) {
                validServiceNodeMap.put(serviceNode.getService().getId(), serviceNode);
            }
            // 4. 得到反向可达服务后，将输入节点作为下一轮 data node
            validMatchTargetNew.clear();
            for (ServiceNode serviceNode : validServiceNodeNew) {
                validMatchTargetNew.addAll(serviceNode.getInputs());
            }
            validParamNodeSet.addAll(validMatchTargetNew);
            // *1. 扩展，筛选已遍历过的节点
            validMatchSourceNew = this.reverseExpand(validMatchTargetNew);
            validMatchSourceNew.removeAll(validParamNodeSet);
        }
        // 反向遍历结束，直接使用 valid 作为内容
        this.serviceNodeMap = validServiceNodeMap;
        this.paramNodeSet = validParamNodeSet;
    }

    // 反向单次扩展：output <- input
    private Set<ParamNode> reverseExpand(Set<ParamNode> validMatchTargetNew) {
        Set<ParamNode> validMatchSource = new HashSet<>();
        for (ParamNode node : validMatchTargetNew) {
            if (this.matchEdgeMap.get(node) != null) {//可选参数可能没有匹配边
                validMatchSource.addAll(this.matchEdgeMap.get(node).keySet());
            }
        }
        return validMatchSource;
    }

    /**
     * clean Invalid Match Edge: 根据 G(V, ) 清理无效的匹配边
     * （涉及到expand产生的正向边，drop无用节点后无效的边）
     */
    private void cleanInvalidMatchEdge() {
        this.matchEdgeMap.keySet().removeIf(key -> !this.paramNodeSet.contains(key));
        for (Map<ParamNode, Double> map : this.matchEdgeMap.values()) {
            map.keySet().removeIf(key -> !this.paramNodeSet.contains(key));
        }
        this.matchEdgeMap.keySet().removeIf(key -> this.matchEdgeMap.get(key).isEmpty());
    }

    /**
     * calculate QoS Floor: 计算各节点最优 QoS 下限
     * 1. 从一组已更新的节点开始（初始为 sourceNode 的输出,0）
     * 2. 沿匹配边传递（=）
     * 3. 根据服务所有必选输入中最大值计算输出的 QoS 下限（max + responseTime）
     * 重复更新直到稳定
     * 最后，得到 sinkNode（图整体）的 QoS 下限
     */
    private void calculateQoSFloor() {
        this.sourceNode.setResponseTimeFloor(0);
        for (ParamNode node : this.sourceNode.getOutputs()) {
            node.setResponseTimeFloor(0);
        }
        // 1.更新QoS的节点
        Set<ParamNode> updateMatchSource = new HashSet<>(this.sourceNode.getOutputs());

        while (!updateMatchSource.isEmpty()) {
            Set<ParamNode> updateMatchSourceNew = new HashSet<>();
            // 2. 匹配边相连直接传递（=）
            Set<ParamNode> updateMatchTarget = new HashSet<>();
            for (ParamNode node : updateMatchSource) {
                if (!this.matchEdgeMap.containsKey(node)) {
                    continue;
                }
                int floor = node.getResponseTimeFloor();
                for (ParamNode matchTarget : this.matchEdgeMap.get(node).keySet()) {
                    int responseTimeFloor = matchTarget.getResponseTimeFloor();
                    if (floor < responseTimeFloor) {//QoS更新
                        matchTarget.setResponseTimeFloor(floor);
                        updateMatchTarget.add(matchTarget);
                    }
                }
            }
            // 3. 影响到的服务的输出（+x）
            Set<ServiceNode> updateServiceNode = new HashSet<>();
            Set<ServiceNode> unchangedServiceNode = new HashSet<>();
            for (ParamNode node : updateMatchTarget) {
                ServiceNode serviceNode = node.getServiceNode();//input -> service
                if (serviceNode == this.sinkNode
                        || updateServiceNode.contains(serviceNode)
                        || unchangedServiceNode.contains(serviceNode)) {
                    continue;
                }
                // 可能产生QoS更新的服务
                int inputFloorMax = 0;
                for (ParamNode input : serviceNode.getInputs()) {
                    if (!this.paramNodeSet.contains(input)) {
                        throw new RuntimeException();
                    }
                    if (!input.getParam().isRequired()) {
                        continue;
                    }
                    // 图中存在且必选的输入的最大值为 服务开始时QoS floor
                    int inputFloor = input.getResponseTimeFloor();
                    inputFloorMax = inputFloor > inputFloorMax ? inputFloor : inputFloorMax;
                }
                //服务开始时QoS floor更新, 更新其输出
                if (inputFloorMax < serviceNode.getResponseTimeFloor()) {
                    updateServiceNode.add(serviceNode);//标记更新
                    //更新服务
                    serviceNode.setResponseTimeFloor(inputFloorMax);
                    //更新输出
                    int outputFloorNew = inputFloorMax + serviceNode.getService().getResponseTime();
                    serviceNode.getOutputs().forEach(output -> output.setResponseTimeFloor(outputFloorNew));
                    //等待下一轮
                    updateMatchSourceNew.addAll(serviceNode.getOutputs());
                } else {
                    unchangedServiceNode.add(serviceNode);
                }
            }
            // *1. 换成下一轮节点
            updateMatchSource = updateMatchSourceNew;
        }
        // 全部更新结束后，得到 sinkNode 的 QoS Floor
        int floorMax = 0;
        for (ParamNode node : this.sinkNode.getInputs()) {
            if (!this.paramNodeSet.contains(node)) {
                throw new RuntimeException();
            }
            if (!node.getParam().isRequired()) {
                continue;
            }
            int floor = node.getResponseTimeFloor();
            floorMax = floor > floorMax ? floor : floorMax;
        }
        this.sinkNode.setResponseTimeFloor(floorMax);

    }


    // getter
    public Set<ParamNode> getTargetOutput() {
        return this.sinkNode.getInputs();
    }

    public int countMatchEdge() {
        int count = 0;
        for (Map<ParamNode, Double> map : this.matchEdgeMap.values()) {
            count += map.size();
        }
        return count;
    }

    @Override
    public String toString() {
        return "=======\nService Graph {\n"
                + "\tData nodes(" + paramNodeSet.size() + "): " + paramNodeSet.toString() + "\n"
                + "\tService nodes(" + serviceNodeMap.size() + "): " + serviceNodeMap.toString() + "\n"
                + "}";
    }
}
