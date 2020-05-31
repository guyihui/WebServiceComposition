package com.sjtu.composition.serviceUtils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.sjtu.composition.graph.executionPath.AutoExecuteException;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

public class RestfulService extends Service {

    public RestfulService(String name, String description, int responseTime,
                          String endpoint, Operation operation,
                          Set<Parameter> requestParams, Set<Parameter> responseParams) {
        this(name, description, responseTime, endpoint, operation, requestParams, responseParams, new HashSet<>());
    }

    public RestfulService(String name, String description, int responseTime,
                          String endpoint, Operation operation,
                          Set<Parameter> requestParams, Set<Parameter> responseParams,
                          Set<Parameter> uniqueRequestParams) {
        this.name = name;
        this.description = description;
        this.responseTime = responseTime;
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

    //TODO: PATH, BODY
    //      POST, PUT, DELETE
    @Override
    public boolean run(Map<Parameter, Object> matches) throws AutoExecuteException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(endpoint);
        for (Parameter param : uniqueRequestParams) {
            String paramName = param.getName();
            if (param.getDefaultValue() != null) {
                builder.queryParam(paramName, param.getDefaultValue());
            }
        }
        //TODO
        for (Parameter requestParam : requestParams) {
            Object paramValue = matches.get(requestParam);
            switch (requestParam.getParamCategory()) {
                case QUERY:
                    if (paramValue != null) {
                        builder.queryParam(requestParam.getName(), paramValue);
                    }
                    break;
                case PATH:
                case BODY:
                default:
                    throw new AutoExecuteException("Param Type Not Supported:" + requestParam.getParamCategory());
            }
        }
        URI uri = builder
                .build()
                .encode()
                .toUri();
        System.out.println(builder.build());
        System.out.println(uri);
        JSONObject responseJSONObject;
        switch (this.operation) {
            case GET:
                responseJSONObject = restTemplate.getForObject(uri, JSONObject.class);
                break;
            case POST:
            case PUT:
            case DELETE:
            default:
                throw new AutoExecuteException("Operation Type Not Supported:" + this.operation);
        }
        if (responseJSONObject == null) {
            throw new AutoExecuteException("Response null");
        }
        for (Parameter responseParam : responseParams) {
            String paramName = responseParam.getName();
            if (responseJSONObject.containsKey(paramName)) {
                Object paramValue = responseJSONObject.get(paramName);
                matches.put(responseParam, paramValue);
            } else {
                throw new AutoExecuteException("auto execute failure: " +
                        "missing output param {" + responseParam.getName() + "}");
            }
        }
        return true;
    }

}
