package com.example.mcpdemo.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileSearchService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Tool(description = "在指定目录下查找文件，当需要模糊查询文件时使用")
    public List<String> findFiles(@ToolParam(description = "待搜索的目录")String directory,@ToolParam(description = "文件名模糊匹配模式") String namePattern) {
        List<String> results = new ArrayList<>();
        File dir = new File(directory);

        if (!dir.exists() || !dir.isDirectory()) {
            return List.of("目录不存在或不是一个有效的目录：" + directory);
        }

        searchFiles(dir, namePattern, results);

        if (results.isEmpty()) {
            return List.of("未找到匹配 '" + namePattern + "' 的文件");
        }

        return results;
    }

    @Tool(description = "在指定目录下搜索包含特定内容的文件，当用户指明需要从文本文件中匹配内容时使用，由于查找时间较长，因此要求用户提供比较详细的路径")
    public List<String> searchFilesByContent(@ToolParam(description = "待搜索的目录")String directory,@ToolParam(description = "用户要查找的文本内容") String content) {
        List<String> results = new ArrayList<>();
        File dir = new File(directory);

        if (!dir.exists() || !dir.isDirectory()) {
            return List.of("目录不存在或不是一个有效的目录：" + directory);
        }

        searchFilesByContent(dir, content, results);

        if (results.isEmpty()) {
            return List.of("未找到包含 '" + content + "' 的文件");
        }

        return results;
    }

    @Tool(description = "查找指定目录下最近修改的文件，可以指定返回的文件数量")
    public List<String> findRecentModifiedFiles(@ToolParam(description = "待搜索的目录")String directory,@ToolParam(description = "返回的文件数量") int limit) {
        File dir = new File(directory);

        if (!dir.exists() || !dir.isDirectory()) {
            return List.of("目录不存在或不是一个有效的目录：" + directory);
        }

        List<File> allFiles = new ArrayList<>();
        collectAllFiles(dir, allFiles);

        if (allFiles.isEmpty()) {
            return List.of("目录中没有文件");
        }

        return allFiles.stream()
                .sorted(Comparator.comparing(File::lastModified).reversed())
                .limit(limit)
                .map(file -> {
                    LocalDateTime modTime = Instant.ofEpochMilli(file.lastModified())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                    return file.getAbsolutePath() + " (修改时间: " + FORMATTER.format(modTime) + ")";
                })
                .collect(Collectors.toList());
    }

    private void searchFiles(File directory, String namePattern, List<String> results) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().contains(namePattern.toLowerCase())) {
                results.add(file.getAbsolutePath());
            } else if (file.isDirectory()) {
                searchFiles(file, namePattern, results);
            }
        }
    }

    private void searchFilesByContent(File directory, String content, List<String> results) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile()) {
                try {
                    //当目标文件是文本文件时，才进行内容匹配
                    if (file.getName().toLowerCase().endsWith(".txt")) {
                        if (fileContainsText(file, content)) {
                            results.add(file.getAbsolutePath());
                        }
                    }
                } catch (IOException e) {
                    // 忽略无法读取的文件
                }
            } else if (file.isDirectory()) {
                searchFilesByContent(file, content, results);
            }
        }
    }

    private void collectAllFiles(File directory, List<File> allFiles) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile()) {
                allFiles.add(file);
            } else if (file.isDirectory()) {
                collectAllFiles(file, allFiles);
            }
        }
    }

    private boolean fileContainsText(File file, String searchText) throws IOException {
        // 忽略二进制文件和常见的大型文件类型
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                fileName.endsWith(".png") || fileName.endsWith(".gif") ||
                fileName.endsWith(".pdf") || fileName.endsWith(".zip") ||
                fileName.endsWith(".rar") || fileName.endsWith(".exe") ||
                fileName.endsWith(".dll") || fileName.endsWith(".class")) {
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(searchText)) {
                    return true;
                }
            }
        }
        return false;
    }
} 
