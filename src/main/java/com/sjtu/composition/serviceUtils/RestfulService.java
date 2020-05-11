package com.sjtu.composition.serviceUtils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

public class RestfulService implements Service {

    // 服务描述信息：id、服务名、功能描述...
    private int id = -1;//e.g.0?
    private String name;//e.g.行政区划区域检索
    private String description;

    // 服务访问：url，操作类型...
    private String endpoint;//e.g. http://api.map.baidu.com/place/v2/search
    private Operation operation;//e.g. GET

    // TODO: QoS

    // 独有属性，不参与匹配，例如 开发key
    private Set<Parameter> uniqueRequestParams;

    // 输入/输出
    private Set<Parameter> requestParams;// path | query | body
    private Set<Parameter> responseParams;

    //TODO: 保存参数的嵌套结构(type:object)

    public RestfulService(String name, String description, String endpoint, Operation operation,
                          Set<Parameter> requestParams, Set<Parameter> responseParams) {
        this(name, description, endpoint, operation, requestParams, responseParams, new HashSet<>());
    }

    public RestfulService(String name, String description, String endpoint, Operation operation,
                          Set<Parameter> requestParams, Set<Parameter> responseParams,
                          Set<Parameter> uniqueRequestParams) {
        this.name = name;
        this.description = description;
        this.endpoint = endpoint;
        this.operation = operation;
        this.requestParams = requestParams;
        this.responseParams = responseParams;
        this.uniqueRequestParams = uniqueRequestParams;
    }

    public void authorize(String keyParamName, String keyText, Parameter.ParamCategory category) {
        Parameter<String> keyParam = new Parameter<>(
                keyParamName,
                category,
                null,
                true,
                Parameter.ParamType.STRING
        );
        keyParam.setDefaultValue(keyText);
        this.uniqueRequestParams.add(keyParam);
    }

    //TODO: 每个service一个template会不会有影响？rest template可能需要设置其他参数，不一定单例
    private RestTemplate restTemplate = new RestTemplate();

    { // 处理text/javascript
        MappingJackson2HttpMessageConverter jsonConverter = null;
        for (HttpMessageConverter<?> converter : restTemplate.getMessageConverters()) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                jsonConverter = (MappingJackson2HttpMessageConverter) converter;
            }
        }
        restTemplate.getMessageConverters().remove(jsonConverter);
        restTemplate.getMessageConverters().add(new FastJsonHttpMessageConverter());
    }

    //TODO: request body
    //      POST, PUT, DELETE
    public JSONObject run(Map<Parameter, Object> matches) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(endpoint);
        Map<String, Object> uriVariableMap = new HashMap<>();//path

        for (Parameter param : uniqueRequestParams) {
            String paramName = param.getName();
            if (param.getDefaultValue() != null) {
                builder.queryParam(paramName, param.getDefaultValue());
            }
        }
        for (Parameter requestParam : requestParams) {
            Object paramValue = matches.get(requestParam);
            switch (requestParam.getParamCategory()) {
                case PATH:
                    uriVariableMap.put(requestParam.getName(), paramValue);
                    break;
                case QUERY:
                    if (paramValue != null) {
                        builder.queryParam(requestParam.getName(), paramValue);
                    }
                    break;
                case BODY:
                default:
            }
        }
        System.out.println("!!!!!!!!!");
        System.out.println(uriVariableMap);
        System.out.println(builder.build());
        URI uri = builder
                .build()
//                .expand(uriVariableMap)
                .encode()
                .toUri();
        System.out.println(builder.build());
        System.out.println(uri);
        switch (this.operation) {
            case GET:
                JSONObject jsonObject = restTemplate.getForObject(uri, JSONObject.class);
                if (jsonObject == null) {
                    return null;
                }
                for (Parameter responseParam : responseParams) {
                    String paramName = responseParam.getName();
                    Object paramValue = jsonObject.get(paramName);
                    matches.put(responseParam, paramValue);
                }
                return jsonObject;
            case POST:
            case PUT:
            case DELETE:
            default:
        }
        return null;
    }

    //getter & setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public Set<Parameter> getUniqueRequestParams() {
        return uniqueRequestParams;
    }

    public void setUniqueRequestParams(Set<Parameter> uniqueRequestParams) {
        this.uniqueRequestParams = uniqueRequestParams;
    }

    public Set<Parameter> getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(Set<Parameter> requestParams) {
        this.requestParams = requestParams;
    }

    public Set<Parameter> getResponseParams() {
        return responseParams;
    }

    public void setResponseParams(Set<Parameter> responseParams) {
        this.responseParams = responseParams;
    }

    @Override
    public String toString() {
        return id + ":[" + name + "]" + "(" + endpoint + ")" + "{" + description + "}";
    }
}
