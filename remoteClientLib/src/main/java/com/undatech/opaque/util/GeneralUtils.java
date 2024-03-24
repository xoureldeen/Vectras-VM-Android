package com.undatech.opaque.util;

import android.content.Context;
import android.content.pm.PackageManager;

public class GeneralUtils {
    public static Class<?> getClassByName(String name) {
        Class<?> remoteCanvasActivityClass = null;
        try {
            remoteCanvasActivityClass = Class.forName(name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return remoteCanvasActivityClass;
    }

    public static void debugLog(boolean enabled, String tag, String message) {
        if (enabled) {
            android.util.Log.d(tag, message);
        }
    }

    public static boolean isTv(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION)
                || context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }
}
