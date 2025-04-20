package com.example.mcpdemo.controller;

import com.alibaba.fastjson2.JSONObject;
import com.example.mcpdemo.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatClient chatClient;

    private AtomicInteger atomicInteger = new AtomicInteger(0);

    private List<Message> messages = new ArrayList<>();

    // 直接输出
    @GetMapping("/chat")
    public String chat(String prompt) {
        // 保存原始用户消息
        messages.add(new Message(atomicInteger.incrementAndGet(), "user", prompt));

        // 将处理后的消息传给AI，但保持历史记录的完整性
        String jsonString = JSONObject.toJSONString(messages);
        System.out.println("原始历史消息: " + jsonString);
        
        // 创建系统提示，告诉AI它可以使用工具
        String systemPrompt = "你是一个功能强大的AI助手，可以帮助用户完成各种任务。" +
                "你可以使用文件查找工具，帮助用户在电脑上查找文件。" +
                "用户可能会要求你查找特定文件、搜索文件内容或找到最近修改的文件。" +
                "请尽可能地帮助用户解决问题。";
        
        // 使用系统消息和用户消息
        String content = chatClient.prompt()
                .system(systemPrompt)
                .user(jsonString)
                .call()
                .content();

        messages.add(new Message(atomicInteger.incrementAndGet(), "assistant", content));
        return content;
    }
}
