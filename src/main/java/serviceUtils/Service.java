package serviceUtils;

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
