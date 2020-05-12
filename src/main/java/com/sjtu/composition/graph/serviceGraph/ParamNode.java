package com.sjtu.composition.graph.serviceGraph;

import com.sjtu.composition.serviceUtils.Parameter;

public class ParamNode {
    public enum Type {
        INPUT, OUTPUT,
    }

    // 图结构信息
    private Type type;
    private Parameter param;
    private ServiceNode serviceNode;
    // QoS信息
    private int responseTimeFloor = Integer.MAX_VALUE;

    public ParamNode(Type type, Parameter param, ServiceNode serviceNode) {
        this.type = type;
        this.param = param;
        this.serviceNode = serviceNode;
    }


    // getter & setter
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Parameter getParam() {
        return param;
    }

    public void setParam(Parameter param) {
        this.param = param;
    }

    public ServiceNode getServiceNode() {
        return serviceNode;
    }

    public void setServiceNode(ServiceNode serviceNode) {
        this.serviceNode = serviceNode;
    }

    public int getResponseTimeFloor() {
        return responseTimeFloor;
    }

    public void setResponseTimeFloor(int responseTimeFloor) {
        this.responseTimeFloor = responseTimeFloor;
    }

    @Override
    public String toString() {
        return param.getName();
    }
}
