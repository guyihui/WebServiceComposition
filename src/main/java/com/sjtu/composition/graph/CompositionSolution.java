package com.sjtu.composition.graph;

import com.alibaba.fastjson.JSONObject;
import com.sjtu.composition.graph.executionPath.*;
import com.sjtu.composition.graph.serviceGraph.*;
import com.sjtu.composition.serviceUtils.Service;
import com.sjtu.composition.similarityUtils.SimilarityUtils;

import java.util.*;

// 包含了可能的多种执行路径的整体解决方案
public class CompositionSolution extends ServiceGraph {

    // 自动执行需要的输入
    private JSONObject givenInputs;
    //TODO:指定输出中需要获取的部分，而不是全部输出匹配

    public CompositionSolution(Service targetService, //JSONObject givenInputs,
                               Set<Service> serviceCluster,
                               SimilarityUtils similarityUtils) {
        super(targetService, serviceCluster, similarityUtils);
    }

    // 方案结果属性
    private boolean isResolved = false;//TODO:对应原因

    public boolean build(double similarityLimit, int layerLimit) throws Exception {
        this.isResolved = super.build(similarityLimit, layerLimit);
        return isResolved;
    }

    /**
     * Extract execution path：提取执行路径
     *
     * @param topK 提取出的路径的数量
     * @return 在最优QoS的基础上，按 -匹配相似度的下限- 排序
     */
    public List<ExecutionPath> extractExecutionPaths(int topK) throws CloneNotSupportedException {
        // 取前k个结果（匹配相似度下限topK）作为推荐/执行
        List<ExecutionPath> pathList = new ArrayList<>();

        // 按匹配使用的相似度的下限构建大顶堆
        Queue<ExecutionPath> pathPriorityQueue = new PriorityQueue<>((path1, path2) -> {
            Double floor1 = path1.getSimilarityFloor();
            Double floor2 = path2.getSimilarityFloor();
            return -floor1.compareTo(floor2);//大顶堆
        });
        // 初始化起始的path
        ExecutionPath initialPath = new ExecutionPath(this.sourceNode, this.sinkNode);
        pathPriorityQueue.offer(initialPath);

        // 按优先级逐个匹配unresolved，保证最优QoS
        while (pathList.size() < topK && !pathPriorityQueue.isEmpty()) {
            // 取出当前优先级最高的path（目前已匹配相似度下限最高）
            ExecutionPath path = pathPriorityQueue.poll();
            // 选择一个unresolved节点
            Iterator<Map.Entry<ParamNode, Set<ServiceNode>>> iterator = path.getUnresolvedInput().entrySet().iterator();
            if (!iterator.hasNext()) {//代表全部resolve
                path.setAllResolved(true);
                pathList.add(path);
            } else {//尚未全部resolve
                // 得到unresolved节点
                Map.Entry<ParamNode, Set<ServiceNode>> entry = iterator.next();
                ParamNode unresolved = entry.getKey();
                Set<ServiceNode> reachableServiceNodeSet = entry.getValue();
                // 尝试所有resolve匹配
                for (Map.Entry<ParamNode, Double> edgeEntry : this.matchEdgeMap.get(unresolved).entrySet()) {
                    ParamNode resolve = edgeEntry.getKey();
                    ServiceNode preServiceNode = resolve.getServiceNode();//匹配边: pre -> current
                    //若不存在 current -> pre, 则不形成环
                    if (!reachableServiceNodeSet.contains(preServiceNode)) {
                        ExecutionPath clonePath = (ExecutionPath) path.clone();
                        boolean isFeasible = clonePath.resolve(resolve, unresolved, edgeEntry.getValue());
                        if (isFeasible) {//resolve不会导致环、QoS劣化
                            pathPriorityQueue.offer(clonePath);
                        }
                    }
                }
                // 所有匹配尝试结束，已经offer入大顶堆中
            }
            // 一个unresolved的所有方案解决，循环处理其他unresolved
        }

        return pathList;
    }


    // getter & setter


    @Override
    public String toString() {
        return "=======\nComposition solution {\n"
                + "问题约束:\n"
                + "\tTarget: " + this.targetService + "\n"
                + "\tSimilarity limit: " + this.similarityLimit + "\n"
                + "\tLayer limit: " + this.layerLimit + "\n"
                + "solution结果:\n"
                + "\tIs Resolved: " + isResolved + "\n"
                + "solution规模:\n"
                + "\tData nodes(" + paramNodeSet.size() + "): " + paramNodeSet.toString() + "\n"
                + "\tService nodes(" + serviceNodeMap.size() + "): " + serviceNodeMap.toString() + "\n"
                + "}";
    }

}
