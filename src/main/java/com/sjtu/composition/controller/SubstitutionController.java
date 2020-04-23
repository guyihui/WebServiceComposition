package com.sjtu.composition.controller;

import com.alibaba.fastjson.JSONObject;
import com.sjtu.composition.graph.CompositionSolution;
import com.sjtu.composition.graph.executionPath.ExecutionPath;
import com.sjtu.composition.graph.serviceGraph.ServiceGraph;
import com.sjtu.composition.graph.serviceGraph.ServiceNode;
import com.sjtu.composition.serviceUtils.Parameter;
import com.sjtu.composition.serviceUtils.RestfulService;
import com.sjtu.composition.serviceUtils.Service;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.*;

@RestController
public class SubstitutionController {

    private static final ServiceGraph graph = new ServiceGraph();

    @PostConstruct
    public void initGraph() {

        Parameter paramLocation = new Parameter(
                "location",
                "定位",
                "定位信息，地理位置",
                true
        );
        Parameter paramPhoto = new Parameter(
                "photo",
                "照片",
                "卫星拍照，卫星图像dfdsf",
                true
        );
        Parameter paramTemperature = new Parameter(
                "temperature",
                "温度",
                "摄氏温度",
                true
        );
        Parameter paramLongitude = new Parameter(
                "longitude",
                "经度",
                "经度经度经度",
                true
        );
        Parameter paramLatitude = new Parameter(
                "latitude",
                "纬度",
                "纬度、、、纬度",
                true
        );
        Parameter paramTimezone = new Parameter(
                "timezone",
                "时区",
                "UTC时区",
                true
        );


        Set<Parameter> inputsX = new HashSet<>();
        inputsX.add(paramLocation);
        Set<Parameter> outputsX = new HashSet<>();
        outputsX.add(paramPhoto);
        outputsX.add(paramTemperature);
        Service serviceX = new RestfulService(
                0, "卫星拍摄", "Given certain location, take photo and get temperature.",
                "http://localhost:8080/satellite", Service.RequestType.GET, inputsX, outputsX
        );

        Set<Parameter> inputsA = new HashSet<>();
        inputsA.add(paramLocation);
        Set<Parameter> outputsA = new HashSet<>();
        outputsA.add(paramTemperature);
        Service serviceA = new RestfulService(
                1, "温度服务", "get the temperature somewhere",
                "http://localhost:8080/temperature", Service.RequestType.GET, inputsA, outputsA
        );

        Set<Parameter> inputsB = new HashSet<>();
        inputsB.add(paramLocation);
        Set<Parameter> outputsB = new HashSet<>();
        outputsB.add(paramLongitude);
        outputsB.add(paramLatitude);
        Service serviceB = new RestfulService(
                2, "经纬度", "get longitude and latitude",
                "http://localhost:8080/gps", Service.RequestType.GET, inputsB, outputsB
        );

        Set<Parameter> inputsC = new HashSet<>();
        inputsC.add(paramLatitude);
        inputsC.add(paramLongitude);
        Set<Parameter> outputsC = new HashSet<>();
        outputsC.add(paramPhoto);
        Service serviceC = new RestfulService(
                3, "经纬度拍照", "according to (latitude, longitude), take photo by satellite",
                "http://localhost:8080/photo", Service.RequestType.GET, inputsC, outputsC
        );

        Set<Parameter> inputsD = new HashSet<>();
        inputsD.add(paramLocation);
        Set<Parameter> outputsD = new HashSet<>();
        outputsD.add(paramLongitude);
        Service serviceD = new RestfulService(
                4, "获取经度", "get longitude",
                "http://localhost:8080/longitude", Service.RequestType.GET, inputsD, outputsD
        );

        Set<Parameter> inputsE = new HashSet<>();
        inputsE.add(paramLongitude);
        Set<Parameter> outputsE = new HashSet<>();
        outputsE.add(paramTimezone);
        Service serviceE = new RestfulService(
                5, "时区服务", "get timezone",
                "http://localhost:8080/timezone", Service.RequestType.GET, inputsE, outputsE
        );

        graph.addService(serviceX);
        graph.addService(serviceA);
        graph.addService(serviceB);
        graph.addService(serviceC);
        graph.addService(serviceD);
        graph.addService(serviceE);
        System.out.println(graph);

    }

    @GetMapping("/")
    public Set<Service> getAllServices() {
        Collection<ServiceNode> serviceNodes = graph.getServiceNodeMap().values();
        Set<Service> serviceSet = new HashSet<>();
        for (ServiceNode node : serviceNodes) {
            serviceSet.add(node.getService());
        }
        return serviceSet;
    }

    @GetMapping("/substitution")
    public String substitutionGuide() {
        return "/substitution/{serviceId}?{?}={?}";
    }

    @GetMapping("/substitution/{serviceId}")
    public Service getService(@PathVariable("serviceId") int serviceId) {
        return graph.getServiceNodeMap().get(serviceId).getService();
    }

    @PostMapping(value = "/substitution/{serviceId}", produces = "application/json;charset=UTF-8")
    public JSONObject substitute(@PathVariable("serviceId") int serviceId, @RequestBody JSONObject inputArgs) {
        Service targetService = graph.getServiceNodeMap().get(serviceId).getService();

        CompositionSolution solution = graph.search(targetService, 0.99999999, 5);
        System.out.println(solution);
        Set<ExecutionPath> paths = solution.extractExecutionPaths(3);
        System.out.println("Path count = " + paths.size());

        JSONObject result = new JSONObject();
        for (ExecutionPath path : paths) {
            System.out.println();
            if (path.isAvailable()) {
                result.put("isResolved", true);
                result.put("result", path.run(inputArgs));//TODO:选择
                System.out.println(result);
                return result;
            }
        }
        System.out.println("\n______ test run serviceX ______");
        System.out.println(targetService.run(inputArgs));
        result.put("isResolved", false);
        result.put("recommend", null);
        return result;

    }

}
