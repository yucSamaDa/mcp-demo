package com.example.mcpdemo.tool;

import com.example.mcpdemo.service.FileSearchService;
import lombok.Setter;

public class FileSearchByContentTool {

    @Setter
    private String directory;

    @Setter
    private String content;

    public FileSearchByContentTool() {}

    private final FileSearchService fileSearchService = new FileSearchService();

    public String searchFilesByContent() {
        return fileSearchService.searchFilesByContent(directory,content).toString();
    }

}
