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

    public CompositionSolution(Service targetService, Set<Service> serviceCluster,
                               SimilarityUtils similarityUtils) {
        super(targetService, serviceCluster, similarityUtils);
    }

    public boolean build(double similarityLimit, int layerLimit) {
        this.isResolved = super.build(similarityLimit, layerLimit);
        return isResolved;
    }

    // 方案结果属性
    public boolean isResolved = false;//TODO:对应原因
    public int round;

    // 提取：得到无歧义/多余路径的实际执行路径（可能存在多种）
    // 返回完整的set
    // TODO: 是否会出现栈溢出，递归/非递归
    public Set<ExecutionPath> extractExecutionPaths(int compositeLengthLimit) {
        Set<ExecutionPath> results = new HashSet<>();
        if (!this.isResolved) {
            return results;
        }
        //需要通过匹配解决的target
        ExecutionPath executionPath = new ExecutionPath(this);
        completeExecutionPath(executionPath, results, compositeLengthLimit);
        //合并反向路径中重复的部分
        for (ExecutionPath path : results) {
            path.merge();
        }
        return results;
    }

    //每次递归完成一个match
    // compositeLengthLimit: 限制组合长度，避免无穷环路径
    private void completeExecutionPath(ExecutionPath path, Set<ExecutionPath> pathSet, int compositeLengthLimit) {
        // 递归终点：所有execution节点的输入匹配完毕
        Map<ExecutionNode, Set<ParamNode>> unresolvedExecutionHeadMap = path.getUnresolvedExecutionHeads();
        if (unresolvedExecutionHeadMap.isEmpty()) {
            pathSet.add(path);
            return;
        }
        //取出这一轮需要匹配的节点（这些节点不能被修改）
        // ExecutionNode及其输入
        Iterator<Map.Entry<ExecutionNode, Set<ParamNode>>> headMapItr = unresolvedExecutionHeadMap.entrySet().iterator();
        Map.Entry<ExecutionNode, Set<ParamNode>> mapEntry = headMapItr.next();
        final ExecutionNode executionHead = mapEntry.getKey();
        Integer headLength = path.getUnresolvedHeadLengthMap().get(executionHead);
        if (headLength == null || headLength > compositeLengthLimit) {
            return;//超出长度上限，path舍弃
        }
        final Set<ParamNode> unresolvedInputs = mapEntry.getValue();
        // 其中的一个输入
        Iterator<ParamNode> dataNodeItr = unresolvedInputs.iterator();
        final ParamNode toMatch = dataNodeItr.next();

        //匹配候选
        //当有多个可能性时, clone副本, 尝试所有匹配可能
        Map<ParamNode, Double> matchEdges = matchEdgeMap.get(toMatch);
        if (toMatch.getParam().isRequired() && (matchEdges == null || matchEdges.isEmpty())) { //必选空候选
            this.isResolved = false;
            return;
        }
        if (matchEdges != null && !matchEdges.isEmpty()) {// 匹配
            //候选非空（必选/可选）
            Set<ParamNode> matchCandidate = matchEdgeMap.get(toMatch).keySet();
            int candidateNo = 0;

            for (ParamNode candidate : matchCandidate) {
                candidateNo++;
                ExecutionPath clonePath;
                if (toMatch.getParam().isRequired() && candidateNo == matchCandidate.size()) {
                    //必选参数的最后一个候选，不需要clone
                    clonePath = path;
                } else { // 否则需要clone（深拷贝）后分开操作
                    try {
                        clonePath = (ExecutionPath) path.clone();
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                        this.isResolved = false;
                        return;
                    }
                }

                //在clonePath上补全
                ExecutionNode preNode;
                if (this.sourceNode.getOutputs().contains(candidate)) {//如果是原始输入，则前置 ExecutionNode 为起点node
                    preNode = ExecutionPath.START_NODE;
                } else {//否则，preNode为产生该candidate输出的node
                    preNode = new ExecutionNode(ExecutionNode.Type.COMPONENT, candidate.getServiceNode());
                }
                MatchNode matchNode = new MatchNode(candidate, toMatch);
                // 连接路径
                // preNode -> matchNode -> executionHead -> ...
                clonePath.connect(preNode, matchNode, ExecutionPath.ConnectType.EXECUTION_TO_MATCH);
                clonePath.connect(matchNode, executionHead, ExecutionPath.ConnectType.MATCH_TO_EXECUTION);
                // 更新待匹配map（需要对clone出的path进行操作）
                Map<ExecutionNode, Integer> unresolvedHeadLengthInClonePath = clonePath.getUnresolvedHeadLengthMap();
                Map<ExecutionNode, Set<ParamNode>> unresolvedExecutionHeadMapInClonePath = clonePath.getUnresolvedExecutionHeads();
                Set<ParamNode> unresolvedInputsInClonePath = unresolvedExecutionHeadMapInClonePath.get(executionHead);
                unresolvedInputsInClonePath.remove(toMatch);//需要解决的头节点的一个输入匹配完成
                int executionHeadLength = unresolvedHeadLengthInClonePath.get(executionHead);
                if (unresolvedInputsInClonePath.isEmpty()) {// 输入全部匹配 = 运行节点匹配完成
                    unresolvedExecutionHeadMapInClonePath.remove(executionHead);
                    unresolvedHeadLengthInClonePath.remove(executionHead);
                }
                if (preNode.type == ExecutionNode.Type.COMPONENT) { // 产生的新的运行节点
                    Set<ParamNode> inputsNew = new HashSet<>(preNode.getServiceNode().getInputs());
                    unresolvedExecutionHeadMapInClonePath.put(preNode, inputsNew);
                    unresolvedHeadLengthInClonePath.put(preNode, executionHeadLength + 1);
                }
                // 递归，完成前置路径
                this.completeExecutionPath(clonePath, pathSet, compositeLengthLimit);
            }
        }
        // 非必选参数可以不使用，并且此处path依旧保存完好，直接在path上操作（可选参数在上述步骤中全部经过clone后操作）
        if (!toMatch.getParam().isRequired()) {
            unresolvedInputs.remove(toMatch);//需要解决的头节点的一个输入匹配完成
            if (unresolvedInputs.isEmpty()) {// 输入全部匹配 = 运行节点匹配完成
                unresolvedExecutionHeadMap.remove(executionHead);
                path.getUnresolvedHeadLengthMap().remove(executionHead);
            }
            // 递归，完成前置路径
            this.completeExecutionPath(path, pathSet, compositeLengthLimit);
        }

    }


    // TODO: 选择：若有多种可能，根据一定约束进行选择


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
                + "\tRound: " + round + "\n"
                + "solution规模:\n"
                + "\tData nodes(" + paramNodeSet.size() + "): " + paramNodeSet.toString() + "\n"
                + "\tService nodes(" + serviceNodeMap.size() + "): " + serviceNodeMap.toString() + "\n"
                + "}";
    }

}
