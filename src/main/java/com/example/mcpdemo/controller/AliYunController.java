package com.example.mcpdemo.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.alibaba.dashscope.aigc.conversation.ConversationParam.ResultFormat;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationOutput.Choice;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.tools.FunctionDefinition;
import com.alibaba.dashscope.tools.ToolCallBase;
import com.alibaba.dashscope.tools.ToolCallFunction;
import com.alibaba.dashscope.tools.ToolFunction;
import com.alibaba.dashscope.utils.JsonUtils;
import com.example.mcpdemo.tool.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;

import java.util.Scanner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AliYunController {

    private static ObjectNode generateSchema(Class<?> clazz) {
        SchemaGeneratorConfigBuilder configBuilder =
                new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        SchemaGeneratorConfig config = configBuilder.with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .without(Option.FLATTENED_ENUMS_FROM_TOSTRING).build();
        SchemaGenerator generator = new SchemaGenerator(config);
        return generator.generateSchema(clazz);
    }
    public static void selectTool() throws NoApiKeyException, ApiException, InputRequiredException {
        FunctionDefinition fdWeather = getFunctionDefinition("get_current_weather","获取指定地区的天气",generateSchema(GetWeatherTool.class));
        FunctionDefinition fdTime = getFunctionDefinition("get_current_time","获取当前时刻的时间",generateSchema(GetTimeTool.class));
        FunctionDefinition searchFilesByContent = getFunctionDefinition("searchFilesByContent","在指定目录下搜索包含特定内容的文件，当用户指明需要从文本文件中匹配内容时使用，由于查找时间较长，因此要求用户提供比较详细的路径",generateSchema(FileSearchByContentTool.class));
        FunctionDefinition findFiles = getFunctionDefinition("findFiles","在指定目录下查找文件，当需要查询文件时使用,已实现模糊查询，因此只需要传入用户要求的文件名称",generateSchema(FindFilesTool.class));
        FunctionDefinition FindRecentModifiedFilesTool = getFunctionDefinition("FindRecentModifiedFilesTool","查找指定目录下最近修改的文件，可以指定返回的文件数量",generateSchema(FindRecentModifiedFilesTool.class));
        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("You are a helpful assistant. When asked a question, use tools wherever possible.")
                .build();
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入：");
        String userInput = scanner.nextLine();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(userInput)
                .build();
        List<Message> messages = new ArrayList<>(Arrays.asList(systemMsg, userMsg));
        GenerationParam param = GenerationParam.builder()
                // 模型列表：https://help.aliyun.com/zh/model-studio/getting-started/models
                .model("qwen-plus")
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey("YOUR_API_KEY")
                .messages(messages).resultFormat(ResultFormat.MESSAGE)
                .tools(Arrays.asList(
                        ToolFunction.builder().function(fdWeather).build(),
                        ToolFunction.builder().function(fdTime).build(),
                        ToolFunction.builder().function(searchFilesByContent).build(),
                        ToolFunction.builder().function(findFiles).build(),
                        ToolFunction.builder().function(FindRecentModifiedFilesTool).build()
                )).build();
        Generation gen = new Generation();
        GenerationResult result = gen.call(param);
        System.out.println("首轮输出：" + JsonUtils.toJson(result));
        boolean needToolCall = true;
        while (needToolCall) {
            needToolCall = false;
            for (Choice choice : result.getOutput().getChoices()) {
                messages.add(choice.getMessage());
                if (choice.getMessage().getToolCalls() != null) {
                    for (ToolCallBase toolCall : choice.getMessage().getToolCalls()) {
                        if (toolCall.getType().equals("function")) {
                            String functionName = ((ToolCallFunction) toolCall).getFunction().getName();
                            String functionArgument = ((ToolCallFunction) toolCall).getFunction().getArguments();
                            if (functionName.equals("get_current_weather")) {
                                GetWeatherTool weatherTool = new GetWeatherTool(functionArgument);
                                String weather = weatherTool.callWeather();
                                Message toolResultMessage = Message.builder().role("tool")
                                        .content(weather).toolCallId(toolCall.getId()).build();
                                messages.add(toolResultMessage);
                                System.out.println("工具输出信息：" + weather);
                            } else if (functionName.equals("get_current_time")) {
                                GetTimeTool timeTool = new GetTimeTool();
                                String time = timeTool.getCurrentTime();
                                Message toolResultMessage = Message.builder().role("tool")
                                        .content(time).toolCallId(toolCall.getId()).build();
                                messages.add(toolResultMessage);
                                System.out.println("工具输出信息：" + time);
                            } else if (functionName.equals("searchFilesByContent")) {
                                try {
                                    FileSearchByContentTool fileSearchTool = new FileSearchByContentTool();

                                    ObjectMapper objectMapper = new ObjectMapper();
                                    JsonNode functionArgs = objectMapper.readTree(functionArgument);

                                    String content = functionArgs.get("content").asText();
                                    String directory = functionArgs.get("directory").asText();
                                    fileSearchTool.setDirectory(directory);
                                    fileSearchTool.setContent(content);
                                    String searchResult = fileSearchTool.searchFilesByContent();
                                    Message toolResultMessage = Message.builder().role("tool")
                                            .content(searchResult).toolCallId(toolCall.getId()).build();
                                    messages.add(toolResultMessage);
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            } else if (functionName.equals("findFiles")) {
                                try {
                                    FindFilesTool findFilesTool = new FindFilesTool();
                                    ObjectMapper objectMapper = new ObjectMapper();
                                    JsonNode functionArgs = objectMapper.readTree(functionArgument);
                                    String directory = functionArgs.get("directory").asText();
                                    String namePattern = functionArgs.get("namePattern").asText();
                                    findFilesTool.setDirectory(directory);
                                    findFilesTool.setNamePattern(namePattern);
                                    String findFilesResult = findFilesTool.findFiles();
                                    Message toolResultMessage = Message.builder().role("tool")
                                            .content(findFilesResult).toolCallId(toolCall.getId()).build();
                                    messages.add(toolResultMessage);
                                }catch (JsonProcessingException e){
                                    throw new RuntimeException(e);
                                }
                            } else if (functionName.equals("FindRecentModifiedFilesTool")) {
                                try {
                                    FindRecentModifiedFilesTool findRecentModifiedFilesTool = new FindRecentModifiedFilesTool();
                                    ObjectMapper objectMapper = new ObjectMapper();
                                    JsonNode functionArgs = objectMapper.readTree(functionArgument);
                                    String directory = functionArgs.get("directory").asText();
                                    int limit = 5;
                                    if (functionArgs.has("limit")) {
                                        limit = functionArgs.get("limit").asInt();
                                    }
                                    findRecentModifiedFilesTool.setDirectory(directory);
                                    findRecentModifiedFilesTool.setLimit(limit);
                                    String findFilesResult = findRecentModifiedFilesTool.findRecentModifiedFiles();
                                    Message toolResultMessage = Message.builder().role("tool")
                                            .content(findFilesResult).toolCallId(toolCall.getId()).build();
                                    messages.add(toolResultMessage);
                                }catch (JsonProcessingException e){
                                    throw new RuntimeException(e);
                                }
                            }
                            needToolCall = true;
                        }
                    }
                } else {
                    System.out.println("最终答案：" + choice.getMessage().getContent());
                    return;
                }
            }

            if (needToolCall) {
                param.setMessages(messages);
                result = gen.call(param);
                System.out.println("下一轮输出：" + JsonUtils.toJson(result));
            }
        }

        System.out.println("最终答案：" + result.getOutput().getChoices().get(0).getMessage().getContent());
    }

    private static FunctionDefinition getFunctionDefinition(String name, String description,ObjectNode jsonSchemaWeather) {
        return FunctionDefinition.builder()
                .name(name)
                .description(description)
                .parameters(JsonUtils.parseString(jsonSchemaWeather.toString()).getAsJsonObject()).build();
    }
    // 阿里云模型实现工具调用
    // 功能实现参考阿里云官方文档https://bailian.console.aliyun.com/?tab=doc#/doc/?type=model&url=https%3A%2F%2Fhelp.aliyun.com%2Fdocument_detail%2F2862208.html

    public static void main(String[] args) {
        try {
            selectTool();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            System.out.println(String.format("Exception: %s", e.getMessage()));
        } catch (Exception e) {
            System.out.println(String.format("Exception: %s", e.getMessage()));
        }
        System.exit(0);
    }
}
