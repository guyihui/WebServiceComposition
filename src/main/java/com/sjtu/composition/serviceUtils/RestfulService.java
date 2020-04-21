package com.sjtu.composition.serviceUtils;

import java.util.Set;

public class RestfulService {
    public enum RequestType {
        GET, POST,
    }

    // 服务描述信息：id、服务名、功能描述...
    private int id;//e.g.0?
    private String name;//e.g.行政区划区域检索
    private String description;

    // 服务访问：url，操作类型...
    private String endpoint;//e.g. http://api.map.baidu.com/place/v2/search
    private RequestType requestType;//e.g. GET

    // 输入/输出
    private Set<Parameter> requestParams;
    private Set<Parameter> responseParams;

    public RestfulService(Integer id, String name, String description, String endpoint, RequestType requestType,
                          Set<Parameter> requestParams, Set<Parameter> responseParams) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.endpoint = endpoint;
        this.requestType = requestType;
        this.requestParams = requestParams;
        this.responseParams = responseParams;
    }

    //TODO: 检查服务是否可用
    public boolean isAvailable() {
        return true;
    }

    //TODO: 执行服务，返回结果
    public String run() {
        StringBuilder builder = new StringBuilder(endpoint);
        return "[" + this.requestType + "] " + builder.toString();
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

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
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
        return description;
    }
}
