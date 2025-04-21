package com.example.mcpdemo.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GetWeatherTool {

    private String location;

    public GetWeatherTool(String location) {
        this.location = location;
    }

    public String callWeather() {
        // 假设location是一个JSON字符串，例如{"location": "北京"}
        // 需要提取其中的"location"的值
        try {
            // 使用Jackson库解析JSON
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(location);
            String locationName = jsonNode.get("location").asText();
            return locationName + "今天是晴天";
        } catch (Exception e) {
            // 如果解析失败，返回原始字符串
            return location + "今天是晴天";
        }
    }

}
