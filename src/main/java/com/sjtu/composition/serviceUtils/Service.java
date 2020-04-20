package com.sjtu.composition.serviceUtils;

import java.util.Arrays;

public class Service {

    private int id;
    private String description;
    private String[] inputs;
    private String[] outputs;

    public Service(Integer id, String description, String[] inputs, String[] outputs) {
        this.id = id;
        this.description = description;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    //TODO: 检查服务是否可用
    public boolean isAvailable() {
        return true;
    }

    //TODO: 执行服务，返回结果
    public Object[] run(Object[] args) {
        Object[] results = Arrays.copyOf(outputs, outputs.length);
        for (int i = 0; i < results.length; i++) {
            results[i] += "{" + this.id + "}";
        }
        System.out.println("run Service[" + id + "]:"
                + Arrays.toString(args)
                + " --> "
                + Arrays.toString(results));
        return results;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String[] getInputs() {
        return inputs;
    }

    public void setInputs(String[] inputs) {
        this.inputs = inputs;
    }

    public String[] getOutputs() {
        return outputs;
    }

    public void setOutputs(String[] outputs) {
        this.outputs = outputs;
    }

    @Override
    public String toString() {
        return description;
    }
}
