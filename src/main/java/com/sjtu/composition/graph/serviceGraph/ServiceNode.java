package com.sjtu.composition.graph.serviceGraph;

import com.sjtu.composition.serviceUtils.Service;

import java.util.Set;

public class ServiceNode {
    private Service service;
    private Set<ParamNode> inputs;
    private Set<ParamNode> outputs;

    public ServiceNode(Service service) {
        this.service = service;
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

    @Override
    public String toString() {
        return String.valueOf(service.getId());
    }
}
