package com.sjtu.composition.controller;

import com.alibaba.fastjson.JSONObject;
import com.sjtu.composition.serviceUtils.Parameter;
import com.sjtu.composition.serviceUtils.Service;
import com.sjtu.composition.serviceUtils.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ForwardController {

    private ServiceRepository serviceRepository;

    @Autowired
    public ForwardController(@Qualifier("serviceRepository") ServiceRepository serviceRepository) {
        Assert.notNull(serviceRepository, "service repository must not be null");
        this.serviceRepository = serviceRepository;
    }

    @PostMapping(value = "/forward/{serviceId}", produces = "application/json;charset=UTF-8")
    public JSONObject forward(@PathVariable("serviceId") int serviceId, @RequestBody JSONObject inputArgs) {
        JSONObject result = new JSONObject();
        result.put("status", -1);
        // forward to
        Service service = serviceRepository.getServiceById(serviceId);
        if (service != null) {
            result.put("service", service.toString());
        } else {
            result.put("message", "invalid service");
            return result;
        }
        // args
        Map<Parameter, Object> inputMap = new HashMap<>();
        JSONObject pathArgs = inputArgs.getJSONObject("path");
        JSONObject queryArgs = inputArgs.getJSONObject("query");
        JSONObject bodyArgs = inputArgs.getJSONObject("body");
        try {
            for (Parameter parameter : service.getRequestParams()) {
                JSONObject argsJSONObject;
                switch (parameter.getParamCategory()) {
                    case PATH:
                        argsJSONObject = pathArgs;
                        break;
                    case QUERY:
                        argsJSONObject = queryArgs;
                        break;
                    case BODY:
                        argsJSONObject = bodyArgs;
                        break;
                    default:
                        result.put("message", "invalid service param");
                        return result;
                }
                if (argsJSONObject == null || argsJSONObject.get(parameter.getName()) == null) {
                    if (parameter.isRequired()) {
                        throw new Exception("missing " + parameter.getParamCategory() + " param: " + parameter.getName());
                    }
                } else {
                    inputMap.put(parameter, argsJSONObject.get(parameter.getName()));
                }
            }
            result.put("forward_result", service.run(inputMap));

        } catch (Exception e) {
            result.put("message", e.toString());
            return result;
        }

        result.put("status", 0);
        result.put("message", "success");
        return result;
    }


}
