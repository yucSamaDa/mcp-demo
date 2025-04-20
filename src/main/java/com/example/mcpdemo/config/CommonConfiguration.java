package com.example.mcpdemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransport;
import org.springframework.ai.autoconfigure.mcp.server.McpServerProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.function.RouterFunction;

@Configuration
public class CommonConfiguration {

    private final String SSE_ENDPOINT = "/sse";

    @Autowired
    private ToolCallbackProvider fileSearchTools;

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient
                .builder(chatModel)
                .defaultTools(fileSearchTools)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public WebMvcSseServerTransport webMvcSseServerTransport(ObjectMapper objectMapper,
                                                             McpServerProperties serverProperties) {
        return new WebMvcSseServerTransport(objectMapper,
                serverProperties.getSseMessageEndpoint(),
                SSE_ENDPOINT);
    }

    @Bean
    public RouterFunction<ServerResponse> mvcMcpRouterFunction(WebMvcSseServerTransport transport) {
        return transport.getRouterFunction();
    }
}