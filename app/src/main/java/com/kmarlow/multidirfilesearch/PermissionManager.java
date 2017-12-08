package com.kmarlow.multidirfilesearch;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;

/**
 Abstraction for checking if a permission has been granted. Useful for testing permission not given.
 */
public class PermissionManager {

    private final Context context;

    public PermissionManager(Context context) {
        this.context = context;
    }

    public boolean isPermissionGranted(String permission) {
        if (Build.VERSION.SDK_INT >= 23) {
            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }
}
