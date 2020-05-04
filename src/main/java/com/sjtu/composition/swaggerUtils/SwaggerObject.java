package com.sjtu.composition.swaggerUtils;

import com.alibaba.fastjson.JSONObject;

public class SwaggerObject {
    private JSONObject jsonObject;

    public SwaggerObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }


    public JSONObject getJsonObject() {
        return jsonObject;
    }
}
