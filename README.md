# 如何在 Spring AI 项目中让 LLM 调用 MCP 工具

本项目演示了如何使用 Spring AI 和 MCP 协议让大语言模型（LLM）能够调用自定义工具，特别是文件搜索功能。

## 配置步骤

### 1. 添加依赖

首先确保在 `pom.xml` 中添加了以下依赖：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
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

### 4. 配置应用属性

在 `application.properties` 中配置 MCP 服务器和 OpenAI：

```properties
# MCP服务器配置
spring.ai.mcp.server.sseMessageEndpoint=/mcp/message
spring.ai.mcp.server.tools-enabled=true

# OpenAI配置
spring.ai.openai.api-key=YOUR_API_KEY
spring.ai.openai.chat.options.model=gpt-3.5-turbo-0125
```

### 5. 在控制器中使用

在控制器中使用 ChatClient 时，添加系统提示，告知 LLM 它可以使用工具：

```java
String systemPrompt = "你是一个功能强大的AI助手，可以帮助用户完成各种任务。" +
        "你可以使用文件查找工具，帮助用户在电脑上查找文件。" +
        "用户可能会要求你查找特定文件、搜索文件内容或找到最近修改的文件。";

String content = chatClient.prompt()
        .system(systemPrompt)
        .user(userPrompt)
        .call()
        .content();
```

## 工具使用原理

1. **MCP 协议**：工具通过 MCP（Model Context Protocol）协议进行交互
2. **工具注册**：使用 `@Tool` 注解和 `ToolCallbackProvider` 注册工具
3. **工具调用**：LLM 发送工具调用请求，MCP 服务器执行相应方法并返回结果
4. **工具发现**：LLM 通过系统提示了解可用的工具功能

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