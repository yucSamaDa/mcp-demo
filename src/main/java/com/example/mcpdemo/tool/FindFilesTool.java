package com.example.mcpdemo.tool;

import com.example.mcpdemo.service.FileSearchService;
import lombok.Setter;

public class FindFilesTool {

    @Setter
    private String directory;

    @Setter
    private String namePattern;

    private final FileSearchService fileSearchService = new FileSearchService();

    public FindFilesTool() {}
    public String findFiles() {
        return fileSearchService.findFiles(directory,namePattern).toString();
    }
}
