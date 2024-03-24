package com.undatech.opaque.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;


public class GooglePlayUtils {
    public static boolean isPackageInstalled(Context context, String pkg) {
        boolean result = true;

        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(pkg, PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            result = false;
        }
        return result;
    }
}
