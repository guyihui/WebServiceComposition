package com.sjtu.composition.serviceUtils;


import com.alibaba.fastjson.JSONObject;

import java.util.Set;

public interface Service {
    enum RequestType {
        GET, POST, PUT, DELETE
    }

    boolean isAvailable();

    //TODO
    JSONObject run(JSONObject input);


    //getter & setter
    int getId();

    void setId(int id);

    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);

    String getEndpoint();

    void setEndpoint(String endpoint);

    RequestType getRequestType();

    void setRequestType(RequestType requestType);

    Set<Parameter> getRequestParams();

    void setRequestParams(Set<Parameter> requestParams);

    Set<Parameter> getResponseParams();

    void setResponseParams(Set<Parameter> responseParams);


}
