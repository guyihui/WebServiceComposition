package serviceUtils.serviceGraph;

import serviceUtils.Service;

import java.util.List;

public class ServiceNode {
    private Service service;
    private List<DataNode> inputs;
    private List<DataNode> outputs;

    public ServiceNode(Service service) {
        this.service = service;
    }

    public List<DataNode> getInputs() {
        return inputs;
    }

    public void setInputs(List<DataNode> inputs) {
        this.inputs = inputs;
    }

    public List<DataNode> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<DataNode> outputs) {
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
