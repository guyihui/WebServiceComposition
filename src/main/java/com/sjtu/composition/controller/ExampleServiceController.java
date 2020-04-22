package com.sjtu.composition.controller;

import com.alibaba.fastjson.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ExampleServiceController {

    @GetMapping("/satellite")
    public JSONObject satellite(@RequestParam("location") String location) {
        List<Object> inputs = new ArrayList<>();
        inputs.add(location);
        JSONObject output = new JSONObject();
        output.put("photo", location + ":照片{0}");
        output.put("temperature", location + ":温度{0}");
        System.out.println("{[0]satellite}:" + inputs + " --> " + output);
        return output;
    }

    @GetMapping("/temperature")
    public JSONObject temperature(@RequestParam("location") String location) {
        List<Object> inputs = new ArrayList<>();
        inputs.add(location);
        JSONObject output = new JSONObject();
        output.put("temperature", location + ":温度{1}");
        System.out.println("{[1]temperature}:" + inputs + " --> " + output);
        return output;
    }

    @GetMapping("/gps")
    public JSONObject gps(@RequestParam("location") String location) {
        List<Object> inputs = new ArrayList<>();
        inputs.add(location);
        JSONObject output = new JSONObject();
        output.put("longitude", location + ":经度{2}");
        output.put("latitude", location + ":纬度{2}");
        System.out.println("{[2]gps}:" + inputs + " --> " + output);
        return output;
    }

    @GetMapping("/photo")
    public JSONObject photo(@RequestParam("latitude") String latitude, @RequestParam("longitude") String longitude) {
        List<Object> inputs = new ArrayList<>();
        inputs.add(latitude);
        inputs.add(longitude);
        JSONObject output = new JSONObject();
        output.put("photo", inputs + ":照片{3}");
        System.out.println("{[3]photo}:" + inputs + " --> " + output);
        return output;
    }

    @GetMapping("/longitude")
    public JSONObject longitude(@RequestParam("location") String location) {
        List<Object> inputs = new ArrayList<>();
        inputs.add(location);
        JSONObject output = new JSONObject();
        output.put("longitude", location + ":经度{4}");
        System.out.println("{[4]longitude}:" + inputs + " --> " + output);
        return output;
    }

    @GetMapping("/timezone")
    public JSONObject timezone(@RequestParam("longitude") String longitude) {
        List<Object> inputs = new ArrayList<>();
        inputs.add(longitude);
        JSONObject output = new JSONObject();
        output.put("timezone", longitude + ":时区{5}");
        System.out.println("{[5]timezone}:" + inputs + " --> " + output);
        return output;
    }

}
