package com.sjtu.composition.serviceUtils;

public class Parameter {

    private String name;// uri参数名 e.g. "query"
    private String description;// 参数含义 e.g. "检索关键字"
    private String info;// 补充描述 e.g. "行政区划区域检索不支持多关键字检索。..."
    private boolean isEssential;
    private String defaultValue;
    // private String examples;// 举例
    //TODO:参数类型（string、int……）
    //TODO:与其他参数的依赖关系

    public Parameter(String name, String description, String info, boolean isEssential) {
        this.name = name;
        this.description = description;
        this.info = info;
        this.isEssential = isEssential;
    }

    public Parameter(String name, String description, String info, boolean isEssential, String defaultValue) {
        this.name = name;
        this.description = description;
        this.info = info;
        this.isEssential = isEssential;
        this.defaultValue = defaultValue;
    }


    // getter & setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public boolean isEssential() {
        return isEssential;
    }

    public void setEssential(boolean essential) {
        isEssential = essential;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
