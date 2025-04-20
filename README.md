# Spring AI + MCP 工具调用演示项目

本项目演示了如何使用 Spring AI 和 MCP（Model Context Protocol）协议让大语言模型（LLM）能够调用自定义工具，特别是文件搜索功能。通过这种方式，可以扩展模型的能力，使其能够与外部系统交互。

## 项目介绍

- **项目名称**: MCP-Demo
- **技术栈**: Spring Boot、Spring AI、MCP 协议
- **主要功能**: 实现 LLM 通过 MCP 协议调用自定义文件搜索工具

## 快速开始

### 前置要求

- JDK 17+
- Maven 3.6+
- OpenAI API 密钥（或其他兼容的 API）

### 运行步骤

1. 克隆项目到本地
2. 在 `application.properties` 中配置 API 密钥
3. 运行 `mvn spring-boot:run` 启动应用
4. 访问 `http://localhost:8080` 测试功能

## 配置步骤

### 1. 添加依赖

首先确保在 `pom.xml` 中添加了以下依赖：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-server-webmvc-spring-boot-starter</artifactId>
    <version>1.0.0-M6</version>
</dependency>
<dependency>
    <groupId>io.modelcontextprotocol.sdk</groupId>
    <artifactId>mcp</artifactId>
    <version>0.8.1</version>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M6</version>
</dependency>
```

### 2. 创建工具服务类

创建一个带有 `@Tool` 注解的服务类，如 `FileSearchService.java`，实现文件搜索功能：

```java
@Service
public class FileSearchService {
    @Tool(description = "在指定目录下查找文件，支持文件名模糊匹配")
    public List<String> findFiles(String directory, String namePattern) {
        // 实现文件查找逻辑
    }
    
    @Tool(description = "在指定目录下搜索包含特定内容的文件")
    public List<String> searchFilesByContent(String directory, String content) {
        // 实现文件内容搜索逻辑
    }
    
    @Tool(description = "查找指定目录下最近修改的文件")
    public List<String> findRecentModifiedFiles(String directory, int limit) {
        // 实现查找最近修改文件的逻辑
    }
}
```

### 3. 注册工具回调提供者

创建一个配置类，注册工具回调提供者：

```java
@Configuration
public class ToolConfig {
    @Bean
    public ToolCallbackProvider fileSearchTools(FileSearchService fileSearchService) {
        return MethodToolCallbackProvider.builder().toolObjects(fileSearchService).build();
    }
}
```

### 4. 创建通用配置类

创建一个通用配置类`CommonConfiguration.java`，配置ChatClient、MCP传输和路由：

```java
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
```

这个配置类实现了以下功能：
- 创建ChatClient实例并注入工具回调提供者
- 配置MCP服务器的SSE(Server-Sent Events)传输
- 设置MCP路由功能

### 5. 配置应用属性

在 `application.properties` 中配置 MCP 服务器和 OpenAI：

```properties
# MCP服务器配置
spring.ai.mcp.server.sseMessageEndpoint=/mcp/message
spring.ai.mcp.server.tools-enabled=true

# OpenAI配置
spring.ai.openai.api-key=YOUR_API_KEY
spring.ai.openai.chat.options.model=gpt-4-turbo
# 或使用其他模型
# spring.ai.openai.chat.options.model=deepseek-chat
```

### 6. 在控制器中使用

在控制器中使用 ChatClient 时，添加系统提示，告知 LLM 它可以使用工具：

```java
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    private final ChatClient chatClient;
    
    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }
    
    @PostMapping
    public String chat(@RequestBody String userPrompt) {
        String systemPrompt = "你是一个功能强大的AI助手，可以帮助用户完成各种任务。" +
                "你可以使用文件查找工具，帮助用户在电脑上查找文件。" +
                "用户可能会要求你查找特定文件、搜索文件内容或找到最近修改的文件。";
        
        String content = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
                
        return content;
    }
}
```

## 代码结构

```
src/main/java/com/example/mcpdemo/
├── config/
│   ├── ToolConfig.java           # 工具配置类
│   └── CommonConfiguration.java  # 通用配置类
├── service/
│   └── FileSearchService.java    # 文件搜索服务
├── controller/
│   └── ChatController.java       # 聊天控制器
└── McpDemoApplication.java       # 应用入口
```

## MCP 配置说明

MCP(Model Context Protocol)是一个允许LLM与外部工具交互的协议。在本项目中：

1. **SSE 传输**: 通过`WebMvcSseServerTransport`配置，使用Server-Sent Events实现客户端和服务器之间的实时通信
2. **路由配置**: `RouterFunction`将传入的HTTP请求路由到MCP处理逻辑
3. **工具集成**: `ChatClient`通过`.defaultTools()`方法集成工具回调提供者
4. **条件Bean**: 使用`@ConditionalOnMissingBean`确保在没有其他实例时才创建Bean

## 工具使用原理

1. **MCP 协议**：工具通过 MCP（Model Context Protocol）协议进行交互
2. **工具注册**：使用 `@Tool` 注解和 `ToolCallbackProvider` 注册工具
3. **工具调用**：LLM 发送工具调用请求，MCP 服务器执行相应方法并返回结果
4. **工具发现**：LLM 通过系统提示了解可用的工具功能

## MCP 协议工作流程

```
用户 → ChatController → ChatClient → LLM → 工具调用请求 → MCP服务器 → 工具方法执行 → 结果返回 → LLM → 用户
```

## 可用的文件查找工具

本项目实现了三种文件查找功能：

1. `findFiles(directory, namePattern)`：根据文件名模式查找文件
2. `searchFilesByContent(directory, content)`：搜索包含特定内容的文件
3. `findRecentModifiedFiles(directory, limit)`：查找最近修改的文件

## 使用示例

用户可以通过聊天界面，使用自然语言请求文件查找功能，例如：

* "帮我在D盘查找名称包含report的文件"
* "在C:/Users/文件夹中找出包含'hello world'内容的文件"
* "显示Documents目录下最近修改的10个文件"

系统会自动识别这些请求并调用相应的工具完成任务。

## 技术参考

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [MCP 协议规范](https://github.com/ModelVerse/Model-Context-Protocol/blob/main/MODEL-CONTEXT-PROTOCOL.md)
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)

## 注意事项

- 确保提供的目录路径存在且有访问权限
- 对于大文件夹的搜索可能需要较长时间
- API密钥请妥善保管，不要提交到代码仓库

## 贡献与反馈

欢迎提交PR或Issue来完善本项目。 

