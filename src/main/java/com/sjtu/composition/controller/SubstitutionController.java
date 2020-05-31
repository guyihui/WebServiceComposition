package com.sjtu.composition.controller;

import com.alibaba.fastjson.JSONObject;
import com.sjtu.composition.graph.CompositionSolution;
import com.sjtu.composition.graph.executionPath.ExecutionPath;
import com.sjtu.composition.graph.serviceGraph.ServiceNode;
import com.sjtu.composition.serviceUtils.Service;
import com.sjtu.composition.serviceUtils.ServiceRepository;
import com.sjtu.composition.similarityUtils.SimilarityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class SubstitutionController {

    private ServiceRepository serviceRepository;
    private SimilarityUtils similarityUtils;

    @Autowired
    public SubstitutionController(@Qualifier("serviceRepository") ServiceRepository serviceRepository,
                                  @Qualifier("similarityUtils") SimilarityUtils similarityUtils) {
        Assert.notNull(serviceRepository, "service repository must not be null");
        Assert.notNull(similarityUtils, "similarityUtils must not be null");
        this.serviceRepository = serviceRepository;
        this.similarityUtils = similarityUtils;
    }

    @GetMapping("/substitution")
    public String substitutionGuide() {
        return "POST /substitution/{serviceId}";
    }

    @GetMapping("/substitution/{serviceId}")
    public Service getService(@PathVariable("serviceId") int serviceId) {
        return serviceRepository.getServiceById(serviceId);
    }

    @PostMapping(value = "/substitution/{serviceId}", produces = "application/json;charset=UTF-8")
    public SubstitutionResponse substitute(@PathVariable("serviceId") int serviceId,
                                           @RequestParam(value = "k", required = false, defaultValue = "1") int topK,
                                           @RequestParam(value = "execute", required = false, defaultValue = "false") boolean toExecute,
                                           @RequestParam("similarity") Double similarityLimit,
                                           @RequestParam("layer") Integer layerLimit,
                                           @RequestBody JSONObject inputArgs) {
        SubstitutionResponse response = new SubstitutionResponse();

        // 获取 目标服务 和 服务聚类
        Service targetService = serviceRepository.getServiceById(serviceId);
        if (targetService == null) {
            return response.setMessage("no such service");
        }
        Set<Service> serviceCluster = serviceRepository.getServiceClusterById(serviceId);

        // 创建解决方案
        CompositionSolution solution = new CompositionSolution(
                targetService, //目标服务
                serviceCluster, //聚类范围
                similarityUtils //相似度工具类
        );

        // 构建服务图
        boolean isBuilt;
        try {
            isBuilt = solution.build(similarityLimit, layerLimit);
        } catch (Exception e) {
            e.printStackTrace();
            return response.setMessage(e.toString());
        }

        // 根据构建成功与否进行处理
        if (isBuilt) { // 构建成功，可以提取路径
            System.out.println(solution);
            // 提取路径
            List<ExecutionPath> paths;
            try {
                paths = solution.extractExecutionPaths(topK);
            } catch (Exception e) {
                e.printStackTrace();
                return response.setMessage(e.toString());
            }
            if (paths.isEmpty()) {
                return response.setMessage("no path");
            }
            // 执行路径转JSONObject表示
            List<JSONObject> pathJSONObjectList = new LinkedList<>();
            Set<Service> involvedServiceSet = new HashSet<>();
            for (ExecutionPath path : paths) {
                pathJSONObjectList.add(path.toJSONObject());
                for (ServiceNode serviceNode : path.getServiceNodeSet()) {
                    if (serviceNode.getType() == ServiceNode.Type.COMPONENT) {
                        involvedServiceSet.add(serviceNode.getService());
                    }
                }
            }
            response.setPaths(pathJSONObjectList)
                    .setServices(involvedServiceSet)
                    .setResolved(true)
                    .setMessage("ok");
            System.out.println("Path count = " + paths.size());

            // 自动执行
            if (toExecute) {
                List<JSONObject> executionResults = new LinkedList<>();//TODO:应该只有一个JSONObject（非list）
                for (ExecutionPath path : paths) {
                    System.out.println(path);
                    if (path.isAvailable()) {
                        JSONObject executionResult = null;
                        try {
                            executionResult = path.run(inputArgs);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        System.out.println("executionResult:" + executionResult);
                        executionResults.add(executionResult);
                    } else {
                        executionResults.add(null);
                    }
                }
                return response.setResults(executionResults);
            }
        } else { //图构建失败
            // 没有得到全部输出
            return response.setMessage("build failed");
        }
        return response;

    }

}
