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
import java.io.File;
import java.io.FileNotFoundException;
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
        Parameter paramAlternative = new Parameter(
                "alternative",
                "可选参数干扰",
                "可选项测试参数",
                false
        );


        Set<Parameter> inputsX = new HashSet<>();
        inputsX.add(paramLocation);
        inputsX.add(paramAlternative);
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
        inputsC.add(paramAlternative);//非必选参数
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


        try {
            Scanner scanner = new Scanner(new File("D:\\sjtuProjects\\WebServiceComposition\\src\\main\\resources\\static\\baidu.key"));
            String ak = scanner.nextLine();

            Set<Parameter> uniqueInputsBaiduSuggestion = new HashSet<>();
            uniqueInputsBaiduSuggestion.add(new Parameter("ak", "百度开发key", "", true, ak));
            uniqueInputsBaiduSuggestion.add(new Parameter("output", "输出格式", "", true, "json"));
            Set<Parameter> inputsBaiduSuggestion = new HashSet<>();
            inputsBaiduSuggestion.add(new Parameter("query", "关键词", "", true));
            inputsBaiduSuggestion.add(new Parameter("region", "地区", "", true));
            Set<Parameter> outputsBaiduSuggestion = new HashSet<>();
            outputsBaiduSuggestion.add(new Parameter("result", "结果", "", true));
            Service serviceBaiduSuggestion = new RestfulService(
                    11, "地点输入提示V2.0", "百度api",
                    "http://api.map.baidu.com/place/v2/suggestion", Service.RequestType.GET,
                    inputsBaiduSuggestion, outputsBaiduSuggestion, uniqueInputsBaiduSuggestion
            );
            graph.addService(serviceBaiduSuggestion);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            Scanner scanner = new Scanner(new File("D:\\sjtuProjects\\WebServiceComposition\\src\\main\\resources\\static\\tencent.key"));
            String key = scanner.nextLine();

            Set<Parameter> uniqueInputsTencentSuggestion = new HashSet<>();
            uniqueInputsTencentSuggestion.add(new Parameter("key", "腾讯开发key", "", true, key));
            Set<Parameter> inputsTencentSuggestion = new HashSet<>();
            inputsTencentSuggestion.add(new Parameter("keyword", "关键词", "", true));
            inputsTencentSuggestion.add(new Parameter("region", "地区", "", true));
            Set<Parameter> outputsTencentSuggestion = new HashSet<>();
            outputsTencentSuggestion.add(new Parameter("data", "结果", "", true));
            Service serviceTencentSuggestion = new RestfulService(
                    12, "关键词输入提示", "腾讯api",
                    "https://apis.map.qq.com/ws/place/v1/suggestion", Service.RequestType.GET,
                    inputsTencentSuggestion, outputsTencentSuggestion, uniqueInputsTencentSuggestion
            );
            graph.addService(serviceTencentSuggestion);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

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
        ServiceNode targetServiceNode = graph.getServiceNodeMap().get(serviceId);
        if (targetServiceNode == null) {
            return null;//TODO:构造对应的返回json
        }
        Service targetService = targetServiceNode.getService();

        CompositionSolution solution = new CompositionSolution(
                targetService,
                0.99999999,
                5,
                inputArgs
        );
        JSONObject result = new JSONObject();

        if (graph.search(solution)) { // 可尝试执行
            System.out.println(solution);
            Set<ExecutionPath> paths = solution.extractExecutionPaths(3);
            System.out.println("Path count = " + paths.size());

            for (ExecutionPath path : paths) {
                System.out.println();
                if (path.isAvailable()) {
                    result.put("isResolved", true);
                    result.put("result", path.run(inputArgs));//TODO:选择
                    System.out.println(result);
//                    return result;
                }
            }
        } else { //失败，推荐/其他情况
//            System.out.println("\n______ test run serviceX ______");
//            System.out.println(targetService.run(inputArgs));
            result.put("isResolved", false);
            result.put("recommend", null);
            return result;
        }
        return result;

    }

}
