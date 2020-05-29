package com.sjtu.composition.serviceUtils;

public class Parameter<T> {

    public enum ParamCategory {
        PATH, QUERY, BODY,
        RESPONSE,
    }

    public enum ParamType {
        STRING,
        INTEGER, NUMBER,
        BOOLEAN,
        OBJECT,
        ARRAY,
        //TODO: enum , format , object schema
    }

    private String name;        // *uri参数名 | e.g. "query"(=/:)
    private ParamCategory paramCategory;// *传参类型 | e.g. QUERY == "?xxx=xxx"
    private String description; // *参数含义 | e.g. "检索关键字"
    private boolean isRequired; // *是否必选
    private ParamType paramType;// *参数类型

    private String info;        //  补充描述 | e.g. "行政区划区域检索不支持多关键字检索。..."
    private T defaultValue;
    //TODO:与其他参数的依赖关系

    public Parameter(String name, ParamCategory paramCategory, String description, boolean isRequired, ParamType paramType) {
        this.name = name;
        this.paramCategory = paramCategory;
        this.description = description;
        this.isRequired = isRequired;
        this.paramType = paramType;
    }


    // getter & setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ParamCategory getParamCategory() {
        return paramCategory;
    }

    public void setParamCategory(ParamCategory paramCategory) {
        this.paramCategory = paramCategory;
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

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    public ParamType getParamType() {
        return paramType;
    }

    public void setParamType(ParamType paramType) {
        this.paramType = paramType;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
    }

}
