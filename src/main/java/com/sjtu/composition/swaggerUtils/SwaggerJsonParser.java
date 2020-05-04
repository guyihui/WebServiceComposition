package com.sjtu.composition.swaggerUtils;

import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class SwaggerJsonParser {
    public static void main(String[] args) {
        String jsonPath = "D:\\sjtuProjects\\WebServiceComposition\\src\\main\\resources\\static\\baidu.json";
        SwaggerObject object = parse(new File(jsonPath));
        if (object == null) {
            System.out.println("not valid Swagger json");
            return;
        }
        for (String property : object.getJsonObject().keySet()) {
            System.out.println(object.getJsonObject().get(property));
        }
        System.out.println();
    }

    public static SwaggerObject parse(File file) {
        try {
            Scanner scanner = new Scanner(file);
            StringBuilder builder = new StringBuilder();
            while (scanner.hasNextLine()) {
                builder.append(scanner.nextLine());
            }
            String json = builder.toString();
            JSONObject jsonObject = JSONObject.parseObject(json);
            if (jsonObject.get("openapi") == null || !jsonObject.get("openapi").equals("3.0.1")) {
                return null;
            }
            return new SwaggerObject(jsonObject);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
