package com.sjtu.composition.graph.serviceGraph;

import com.sjtu.composition.serviceUtils.Service;

import java.util.Set;

public class ServiceNode {
    private Service service;
    private Set<DataNode> inputs;
    private Set<DataNode> outputs;

    public ServiceNode(Service service) {
        this.service = service;
    }

    public Set<DataNode> getInputs() {
        return inputs;
    }

    public void setInputs(Set<DataNode> inputs) {
        this.inputs = inputs;
    }

    public Set<DataNode> getOutputs() {
        return outputs;
    }

    public void setOutputs(Set<DataNode> outputs) {
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
