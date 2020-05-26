package com.sjtu.composition.controller;

import com.alibaba.fastjson.JSONObject;
import com.sjtu.composition.graph.CompositionSolution;
import com.sjtu.composition.graph.executionPath.ExecutionPath;
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
    public JSONObject substitute(@PathVariable("serviceId") int serviceId,
                                 @RequestParam(value = "k", required = false, defaultValue = "1") int topK,
                                 @RequestParam(value = "execute", required = false, defaultValue = "false") boolean toExecute,
                                 @RequestParam("similarity") Double similarityLimit,
                                 @RequestParam("layer") Integer layerLimit,
                                 @RequestBody JSONObject inputArgs) {
        JSONObject result = new JSONObject();
        result.put("isResolved", false);
        result.put("message", "");
        result.put("recommend", new ArrayList<>());

        Service targetService = serviceRepository.getServiceById(serviceId);
        if (targetService == null) {
            result.put("message", "no such service");
            return result;
        }
        Set<Service> serviceCluster = serviceRepository.getServiceClusterById(serviceId);

        CompositionSolution solution = new CompositionSolution(
                targetService, //目标服务
                serviceCluster, //聚类范围
                similarityUtils //相似度工具类
        );

        boolean isBuilt;
        try {
            isBuilt = solution.build(similarityLimit, layerLimit);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("message", e.toString());
            return result;
        }
        if (isBuilt) { // 可尝试执行

            System.out.println(solution);
            List<ExecutionPath> paths = null;
            try {
                paths = solution.extractExecutionPaths(topK);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                result.put("message", e.toString());
                return result;
            }
            result.put("recommend", paths);
            if (paths == null) {
                return result;
            }
            System.out.println("Path count = " + paths.size());

            if (toExecute) {
                List<JSONObject> executions = new ArrayList<>();//TODO:应该只有一个JSONObject（非list）
                for (ExecutionPath path : paths) {
                    System.out.println(path);
                    if (path.isAvailable()) {
                        result.put("isResolved", true);
                        JSONObject executionResult = null;
                        try {
                            executionResult = path.run(inputArgs);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        System.out.println(executionResult);
                        executions.add(executionResult);
                    } else {
                        executions.add(null);
                    }
                }
                result.put("result", executions);
            }
            result.put("message", "ok");
        } else { //图构建失败
            result.put("message", "build failed");
        }
        return result;

    }

}
