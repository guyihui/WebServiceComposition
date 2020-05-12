package com.sjtu.composition.serviceUtils;

import com.alibaba.fastjson.JSONObject;

import java.util.Map;
import java.util.Set;

public interface Service {
    enum Operation {
        GET, POST, PUT, DELETE
    }

    //TODO: 检查服务是否可用
    default boolean isAvailable() {
        return true;
    }

    int getResponseTime();

    JSONObject run(Map<Parameter, Object> input);

    //getter & setter
    int getId();

    void setId(int id);

    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);

    String getEndpoint();

    void setEndpoint(String endpoint);

    Operation getOperation();

    void setOperation(Operation operation);

    Set<Parameter> getUniqueRequestParams();

    void setUniqueRequestParams(Set<Parameter> uniqueRequestParams);

    Set<Parameter> getRequestParams();

    void setRequestParams(Set<Parameter> requestParams);

    Set<Parameter> getResponseParams();

    void setResponseParams(Set<Parameter> responseParams);


}
