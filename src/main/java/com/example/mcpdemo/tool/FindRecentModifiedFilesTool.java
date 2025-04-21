package com.example.mcpdemo.tool;

import com.example.mcpdemo.service.FileSearchService;
import lombok.Setter;

public class FindRecentModifiedFilesTool {

    @Setter
    private String directory;

    @Setter
    private int limit;

    private final FileSearchService fileSearchService = new FileSearchService();

    public FindRecentModifiedFilesTool() {}

    public String findRecentModifiedFiles() {
        return fileSearchService.findRecentModifiedFiles(directory,limit).toString();
    }

}
