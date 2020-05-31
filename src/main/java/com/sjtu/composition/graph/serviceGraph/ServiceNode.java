package com.sjtu.composition.graph.serviceGraph;

import com.sjtu.composition.serviceUtils.Service;
import org.springframework.util.Assert;

import java.util.Set;

public class ServiceNode {
    public enum Type {
        SOURCE, SINK, COMPONENT,
    }

    private Type type;
    private Service service;
    private Set<ParamNode> inputs;
    private Set<ParamNode> outputs;
    // QoS信息(执行开始时)
    private int responseTimeFloor = Integer.MAX_VALUE;

    public ServiceNode(Type type) {
        Assert.isTrue(type != Type.COMPONENT, "ServiceNode.Type");
        this.type = type;
        this.service = null;
    }

    public ServiceNode(Service service) {
        Assert.notNull(service, "ServiceNode.Service");
        this.type = Type.COMPONENT;
        this.service = service;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Set<ParamNode> getInputs() {
        return inputs;
    }

    public void setInputs(Set<ParamNode> inputs) {
        this.inputs = inputs;
    }

    public Set<ParamNode> getOutputs() {
        return outputs;
    }

    public void setOutputs(Set<ParamNode> outputs) {
        this.outputs = outputs;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public int getResponseTimeFloor() {
        return responseTimeFloor;
    }

    public void setResponseTimeFloor(int responseTimeFloor) {
        this.responseTimeFloor = responseTimeFloor;
    }

    @Override
    public String toString() {
        return this.type == Type.COMPONENT ? String.valueOf(service.getId()) : this.type.toString();
    }
}
