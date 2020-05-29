package com.sjtu.composition.serviceUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

@org.springframework.stereotype.Service
public class ServiceRepository {

    private List<Service> serviceList = Collections.synchronizedList(new ArrayList<>());

    public synchronized int register(Service service) {
        int id = serviceList.size();
        serviceList.add(service);
        service.setId(id);
        return id;
    }

    public synchronized Service getServiceById(int id) {
        return 0 <= id && id < serviceList.size() ? serviceList.get(id) : null;
    }

    public boolean serviceIsAvailable(int id){
        return true;//TODO
    }

    public synchronized Set<Service> getServiceClusterById(int id) {
        // TODO: 服务聚类
        return new HashSet<>(serviceList);
    }

    public synchronized List<Service> getServiceList() {
        return this.serviceList;
    }

    public synchronized List<String> getServiceStringList() {
        List<String> stringList = new ArrayList<>();
        for (Service service : this.serviceList) {
            stringList.add(service.toString());
        }
        return stringList;
    }

    @PostConstruct
    public void initExamples() {
        register(generateExampleServiceX());
        register(generateExampleServiceA());
        register(generateExampleServiceB());
        register(generateExampleServiceC());
        register(generateExampleServiceD());
        register(generateExampleServiceE());
        register(generateExampleBaiduSuggestion());
        register(generateExampleTencentSuggestion());
        System.out.println("Example services initialized.");
    }

    private RestfulService generateExampleServiceX() {
        Set<Parameter> requestParams = new HashSet<>();
        requestParams.add(new Parameter<String>("location", Parameter.ParamCategory.QUERY, "定位", true, Parameter.ParamType.STRING));
        requestParams.add(new Parameter<String>("alternative", Parameter.ParamCategory.QUERY, "任意文本", false, Parameter.ParamType.STRING));
        Set<Parameter> responseParams = new HashSet<>();
        responseParams.add(new Parameter<String>("photo", Parameter.ParamCategory.RESPONSE, "照片", true, Parameter.ParamType.STRING));
        responseParams.add(new Parameter<String>("temperature", Parameter.ParamCategory.RESPONSE, "温度", true, Parameter.ParamType.STRING));
        return new RestfulService("卫星拍摄", "给定定位坐标，获取对应的照片和温度", 200,
                "http://localhost:8080/satellite", Service.Operation.GET, requestParams, responseParams
        );
    }

    private RestfulService generateExampleServiceA() {
        Set<Parameter> requestParams = new HashSet<>();
        requestParams.add(new Parameter<String>("location", Parameter.ParamCategory.QUERY, "定位", true, Parameter.ParamType.STRING));
        Set<Parameter> responseParams = new HashSet<>();
        responseParams.add(new Parameter<String>("temperature", Parameter.ParamCategory.RESPONSE, "温度", true, Parameter.ParamType.STRING));
        return new RestfulService("温度服务", "获取某地的温度", 100,
                "http://localhost:8080/temperature", Service.Operation.GET, requestParams, responseParams
        );
    }

    private RestfulService generateExampleServiceB() {
        Set<Parameter> requestParams = new HashSet<>();
        requestParams.add(new Parameter<String>("location", Parameter.ParamCategory.QUERY, "定位", true, Parameter.ParamType.STRING));
        Set<Parameter> responseParams = new HashSet<>();
        responseParams.add(new Parameter<String>("longitude", Parameter.ParamCategory.RESPONSE, "经度", true, Parameter.ParamType.STRING));
        responseParams.add(new Parameter<String>("latitude", Parameter.ParamCategory.RESPONSE, "纬度", true, Parameter.ParamType.STRING));
        return new RestfulService("经纬度", "地址转换为经纬度坐标", 80,
                "http://localhost:8080/gps", Service.Operation.GET, requestParams, responseParams
        );
    }

    private RestfulService generateExampleServiceC() {
        Set<Parameter> requestParams = new HashSet<>();
        requestParams.add(new Parameter<String>("longitude", Parameter.ParamCategory.QUERY, "经度", true, Parameter.ParamType.STRING));
        requestParams.add(new Parameter<String>("latitude", Parameter.ParamCategory.QUERY, "纬度", true, Parameter.ParamType.STRING));
        requestParams.add(new Parameter<String>("alternative", Parameter.ParamCategory.QUERY, "任意文本", false, Parameter.ParamType.STRING));
        Set<Parameter> responseParams = new HashSet<>();
        responseParams.add(new Parameter<String>("photo", Parameter.ParamCategory.RESPONSE, "照片", true, Parameter.ParamType.STRING));
        return new RestfulService("经纬度拍照", "根据GPS坐标，获取实时照片", 150,
                "http://localhost:8080/photo", Service.Operation.GET, requestParams, responseParams
        );
    }

    private RestfulService generateExampleServiceD() {
        Set<Parameter> requestParams = new HashSet<>();
        requestParams.add(new Parameter<String>("location", Parameter.ParamCategory.QUERY, "定位", true, Parameter.ParamType.STRING));
        Set<Parameter> responseParams = new HashSet<>();
        responseParams.add(new Parameter<String>("longitude", Parameter.ParamCategory.RESPONSE, "经度", true, Parameter.ParamType.STRING));
        return new RestfulService("获取经度", "得到某地的经度", 80,
                "http://localhost:8080/longitude", Service.Operation.GET, requestParams, responseParams
        );
    }

    private RestfulService generateExampleServiceE() {
        Set<Parameter> requestParams = new HashSet<>();
        requestParams.add(new Parameter<String>("longitude", Parameter.ParamCategory.QUERY, "经度", true, Parameter.ParamType.STRING));
        Set<Parameter> responseParams = new HashSet<>();
        responseParams.add(new Parameter<String>("timezone", Parameter.ParamCategory.RESPONSE, "时区", true, Parameter.ParamType.STRING));
        return new RestfulService("时区服务", "得到某地的时区", 200,
                "http://localhost:8080/timezone", Service.Operation.GET, requestParams, responseParams
        );
    }

    private RestfulService generateExampleBaiduSuggestion() {
        // request
        Set<Parameter> requestParams = new HashSet<>();
        requestParams.add(new Parameter<String>("query", Parameter.ParamCategory.QUERY, "输入建议关键字", true, Parameter.ParamType.STRING));
        requestParams.add(new Parameter<String>("region", Parameter.ParamCategory.QUERY, "搜索范围", true, Parameter.ParamType.STRING));
        // response
        Set<Parameter> responseParams = new HashSet<>();
        responseParams.add(new Parameter<Integer>("status", Parameter.ParamCategory.RESPONSE, "本次API访问状态", true, Parameter.ParamType.INTEGER));
        responseParams.add(new Parameter<String>("message", Parameter.ParamCategory.RESPONSE, "对API访问状态值的英文说明", true, Parameter.ParamType.STRING));
        responseParams.add(new Parameter<List>("result", Parameter.ParamCategory.RESPONSE, "结果数组", false, Parameter.ParamType.ARRAY));
        // extra unique
        Set<Parameter> uniqueParams = new HashSet<>();
        Parameter<String> outputFormatParam = new Parameter<>("output", Parameter.ParamCategory.QUERY, "返回格式", true, Parameter.ParamType.STRING);
        outputFormatParam.setDefaultValue("json");
        uniqueParams.add(outputFormatParam);
        // service
        RestfulService service = new RestfulService(
                "地点输入提示", "描述", 200,
                "http://api.map.baidu.com/place/v2/suggestion",
                Service.Operation.GET, requestParams, responseParams, uniqueParams
        );
        String apiKey = getKeyFromFile(new File(
                "D:\\sjtuProjects\\WebServiceComposition\\src\\main\\resources\\static\\baidu.key"
        ));
        service.authorize("ak", apiKey, Parameter.ParamCategory.QUERY);
        return service;
    }

    private RestfulService generateExampleTencentSuggestion() {
        // request
        Set<Parameter> requestParams = new HashSet<>();
        requestParams.add(new Parameter<String>("keyword", Parameter.ParamCategory.QUERY, "关键词", true, Parameter.ParamType.STRING));
        requestParams.add(new Parameter<String>("region", Parameter.ParamCategory.QUERY, "地区", true, Parameter.ParamType.STRING));
        // response
        Set<Parameter> responseParams = new HashSet<>();
        responseParams.add(new Parameter<Integer>("status", Parameter.ParamCategory.RESPONSE, "状态码", true, Parameter.ParamType.INTEGER));
        responseParams.add(new Parameter<String>("message", Parameter.ParamCategory.RESPONSE, "状态说明", true, Parameter.ParamType.STRING));
        responseParams.add(new Parameter<List>("data", Parameter.ParamCategory.RESPONSE, "提示词数组", false, Parameter.ParamType.ARRAY));
        // extra unique
        Set<Parameter> uniqueParams = new HashSet<>();
        // service
        RestfulService service = new RestfulService("关键词输入提示",
                "用于获取输入关键字的补完与提示，帮助用户快速输入。", 180,
                "https://apis.map.qq.com/ws/place/v1/suggestion",
                Service.Operation.GET, requestParams, responseParams, uniqueParams
        );
        String apiKey = getKeyFromFile(new File(
                "D:\\sjtuProjects\\WebServiceComposition\\src\\main\\resources\\static\\tencent.key"
        ));
        service.authorize("key", apiKey, Parameter.ParamCategory.QUERY);
        return service;
    }

    private static String getKeyFromFile(File file) {
        try {
            Scanner scanner = new Scanner(file);
            return scanner.nextLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}

