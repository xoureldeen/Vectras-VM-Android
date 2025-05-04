package com.vectras.vm.utils;

import android.app.ActivityManager;
import android.content.Context;

public class DeviceUtils {
    public static double totalMemoryCapacity(Context context) {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.totalMem;
    }
}
