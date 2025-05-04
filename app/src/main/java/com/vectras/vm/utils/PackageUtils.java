package com.vectras.vm.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.vectras.vm.AppConfig;

public class PackageUtils {
    public static boolean isInstalled(String packagename, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packagename,PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return  false;
        }
    }

    public static int getThisVersionCode(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (Exception ignored){

        }
        return AppConfig.vectrasVersionCode;
    }

    public static String getThisVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception ignored){

        }
        return AppConfig.vectrasVersion;
    }
}
