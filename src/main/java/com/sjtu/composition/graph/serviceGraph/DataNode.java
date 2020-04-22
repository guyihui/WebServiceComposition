package com.sjtu.composition.graph.serviceGraph;

import com.sjtu.composition.serviceUtils.Parameter;

public class DataNode {
    public enum Type {
        INPUT, OUTPUT,
    }

    public Type type;
    private Parameter param;
    private ServiceNode serviceNode;

    public DataNode(Type type, Parameter param, ServiceNode serviceNode) {
        this.type = type;
        this.param = param;
        this.serviceNode = serviceNode;
    }

    public ServiceNode getServiceNode() {
        return serviceNode;
    }

    public void setServiceNode(ServiceNode serviceNode) {
        this.serviceNode = serviceNode;
    }

    public Parameter getParam() {
        return param;
    }

    public void setParam(Parameter param) {
        this.param = param;
    }

    @Override
    public String toString() {
        return param.getName();
    }
}
