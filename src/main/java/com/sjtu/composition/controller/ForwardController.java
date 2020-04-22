package com.sjtu.composition.controller;

import com.alibaba.fastjson.JSONObject;
import com.sjtu.composition.serviceUtils.Parameter;
import com.sjtu.composition.serviceUtils.RestfulService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
public class ForwardController {

    private static final Map<Integer, RestfulService> serviceMap = new HashMap<>();

    @PostConstruct
    public void init() {
        Parameter query = new Parameter(
                "query",
                "检索关键字",
                "行政区划区域检索不支持多关键字检索。\n如果需要按POI分类进行检索，请将分类通过query参数进行设置，如query=美食",
                true
        );
        Parameter region = new Parameter(
                "region",
                "检索行政区划区域",
                "（增加区域内数据召回权重，如需严格限制召回数据在区域内，请搭配使用city_limit参数），可输入行政区划名或对应cityCode",
                true
        );
        Parameter ak = new Parameter(
                "ak",
                "开发者的访问密钥",
                "开发者的访问密钥，必填项。v2之前该属性为key。",
                true
        );

        Set<Parameter> requestParams = new HashSet<>();
        requestParams.add(query);
        requestParams.add(region);
        requestParams.add(ak);

        RestfulService restfulService = new RestfulService(
                0,
                "行政区划区域检索",
                "地点检索服务（又名Place API）是一类Web API接口服务；\n" +
                        "服务提供多种场景的地点（POI）检索功能，包括城市检索、圆形区域检索、矩形区域检索。开发者可通过接口获取地点（POI）基础或详细地理信息。\n" +
                        "注意：地点检索服务适用于【XX大厦】、【XX小区】等POI地点名称的检索，若需要检索结构化地址，如【北京市海淀区上地十街十号】，则推荐使用地理编码服务。",
                "http://api.map.baidu.com/place/v2/search",
                RestfulService.RequestType.GET,
                requestParams,
                new HashSet<>()
        );

        serviceMap.put(0, restfulService);

    }

    @GetMapping("/forward/{serviceId}")
    public String forward(@PathVariable("serviceId") int serviceId) {
        return "???";
    }


    private RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/restTemplate")
    public JSONObject restTemplate() {

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://localhost:8080/satellite");
        URI uri = builder
                .queryParam("location", "食堂")
                .build().encode().toUri();
        JSONObject object = restTemplate.getForObject(uri, JSONObject.class);
        return object;
    }
}
