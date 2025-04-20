package com.example.mcpdemo.config;

import com.example.mcpdemo.service.FileSearchService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfig {

    @Bean
    public ToolCallbackProvider fileSearchTools(FileSearchService fileSearchService) {
        return MethodToolCallbackProvider.builder().toolObjects(fileSearchService).build();
    }
} 