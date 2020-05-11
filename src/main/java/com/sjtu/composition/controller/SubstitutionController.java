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
    public JSONObject substitute(@PathVariable("serviceId") int serviceId, @RequestBody JSONObject inputArgs) {
        Service targetService = serviceRepository.getServiceById(serviceId);
        if (targetService == null) {
            return null;//TODO:构造对应的返回json
        }

        CompositionSolution solution = new CompositionSolution(
                targetService,
                serviceRepository.getServiceClusterById(serviceId),
                similarityUtils
        );
        JSONObject result = new JSONObject();

        if (solution.build(0.9, 5)) { // 可尝试执行
            System.out.println(solution);
            Set<ExecutionPath> paths = solution.extractExecutionPaths(3);
            System.out.println("Path count = " + paths.size());

            for (ExecutionPath path : paths) {
                System.out.println();
                if (path.isAvailable()) {
                    result.put("isResolved", true);
                    result.put("result", path.run(inputArgs));//TODO:选择
                    System.out.println(result);
                    return result;
                } else {
                    //TODO:无法自动执行
                }
            }
        } else { //失败，推荐/其他情况
//            System.out.println("\n______ test run serviceX ______");
//            System.out.println(targetService.run(inputArgs));
            result.put("isResolved", false);
            result.put("recommend", null);
        }
        return result;

    }

}
