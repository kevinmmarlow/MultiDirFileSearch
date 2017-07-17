package com.kmarlow.multidirfilesearch;

import java.util.List;

/**
 The screen abstraction for our main view
 */
public interface MainScreen {
    void updateFileList(List<FileItemViewModel> fileItemViewModels);
}
