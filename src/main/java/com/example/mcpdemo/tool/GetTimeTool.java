package com.example.mcpdemo.tool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GetTimeTool {

    public String getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "当前时间：" + now.format(formatter) + "。";
    }
}
