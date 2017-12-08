package com.kmarlow.multidirfilesearch;

import android.content.Context;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

/**
 Abstraction for determining available file directories.

 Note that on some devices, the root and home directory are the same.
 */
public class DirectoryRepo {

    private final Context context;

    public DirectoryRepo(Context context) {
        this.context = context;
    }

    public List<File> directoriesToSearch() {
        List<File> directories = new ArrayList<>();
        directories.add(getRootDirectory());
        if (!isRootAndHomeEqual()) {
            directories.add(getHomeDirectory());
        }
        File[] fileDirectories = getExternalFileDirectory();
        if (fileDirectories != null) {
            directories.addAll(Arrays.asList(fileDirectories));
        }

        Timber.d("Searching across %s", directories);
        return directories;
    }

    private File getRootDirectory() {
        return Environment.getRootDirectory();
    }

    private File[] getExternalFileDirectory() {
        return ContextCompat.getExternalFilesDirs(context, null);
    }

    private boolean isRootAndHomeEqual() {
        return getRootDirectory() == getHomeDirectory();
    }

    private File getHomeDirectory() {
        if (Environment.getExternalStorageDirectory().canRead()) {
            return Environment.getExternalStorageDirectory();
        } else {
            return Environment.getRootDirectory();
        }
    }
}
