package com.kmarlow.multidirfilesearch;

import java.io.File;

/**
 Our view model used in the search results list
 */
public class FileItemViewModel {

    private final String name;
    private final File file;
    private final String size;

    public FileItemViewModel(String name, File file, String size) {
        this.name = name;
        this.file = file;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public File getFile() {
        return file;
    }

    public String getSize() {
        return size;
    }
}
