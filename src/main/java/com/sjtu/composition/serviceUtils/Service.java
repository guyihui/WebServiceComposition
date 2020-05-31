package com.sjtu.composition.serviceUtils;

import java.util.Map;
import java.util.Set;

public abstract class Service {
    enum Operation {
        GET, POST, PUT, DELETE
    }

    // 服务描述信息：id、服务名、功能描述...
    protected int id = -1;//e.g.0?
    protected String name;//e.g.行政区划区域检索
    protected String description;

    // 服务访问：url，操作类型...
    protected String endpoint;//e.g. http://api.map.baidu.com/place/v2/search
    protected Operation operation;//e.g. GET

    // 独有属性，不参与匹配，例如 开发key
    protected Set<Parameter> uniqueRequestParams;

    // 输入/输出
    protected Set<Parameter> requestParams;// path | query | body
    protected Set<Parameter> responseParams;
    // TODO: 保存参数的嵌套结构(type:object)

    protected int responseTime;// TODO: 其他QoS度量（吞吐量等）


    // 需要实现的执行方法
    public abstract boolean run(Map<Parameter, Object> input);


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

    public int getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(int responseTime) {
        this.responseTime = responseTime;
    }

    @Override
    public String toString() {
        return id + ":[" + name + "]" + "(" + endpoint + ")" + "{" + description + "}";
    }
}
