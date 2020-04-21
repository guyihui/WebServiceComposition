package com.sjtu.composition.serviceUtils;

public class Parameter {
    private String name;//e.g. "query"
    private String description;//e.g. "检索关键字"
    private String info;
    //e.g. "行政区划区域检索不支持多关键字检索。如果需要按POI分类进行检索，请将分类通过query参数进行设置，如query=美食"
    private boolean isEssential;//e.g. true == 必选
    //TODO:参数类型（string、int……）
    //TODO:与其他参数的依赖关系
    //TODO:value
    //TODO:defaultValue
    //TODO:举例

    public Parameter(String name, String description, String info, boolean isEssential) {
        this.name = name;
        this.description = description;
        this.info = info;
        this.isEssential = isEssential;
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

}
