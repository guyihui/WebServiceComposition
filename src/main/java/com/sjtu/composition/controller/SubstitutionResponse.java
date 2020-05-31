package com.sjtu.composition.controller;

import com.alibaba.fastjson.JSONObject;
import com.sjtu.composition.serviceUtils.Service;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SubstitutionResponse {
    private boolean isResolved = false;
    private String message = "";
    private Set<Service> services = new HashSet<>();
    private List<JSONObject> paths = new LinkedList<>();
    private List<JSONObject> results = new LinkedList<>();

    public SubstitutionResponse setResolved(boolean resolved) {
        isResolved = resolved;
        return this;
    }

    public SubstitutionResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    public SubstitutionResponse setServices(Set<Service> services) {
        this.services = services;
        return this;
    }

    public SubstitutionResponse setPaths(List<JSONObject> paths) {
        this.paths = paths;
        return this;
    }

    public SubstitutionResponse setResults(List<JSONObject> results) {
        this.results = results;
        return this;
    }

    // getter
    public boolean isResolved() {
        return isResolved;
    }

    public String getMessage() {
        return message;
    }

    public Set<Service> getServices() {
        return services;
    }

    public List<JSONObject> getPaths() {
        return paths;
    }

    public List<JSONObject> getResults() {
        return results;
    }

}
