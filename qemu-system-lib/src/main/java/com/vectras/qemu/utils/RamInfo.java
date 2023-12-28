package com.vectras.qemu.utils;

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.vectras.qemu.MainSettingsManager;

public class RamInfo {
    public static Activity activity;

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    public static int vectrasMemory() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) activity.getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long freeMem = mi.availMem / 1048576L;
        long totalMem = mi.totalMem / 1048576L;
        long usedMem = totalMem - freeMem;
        int freeRamInt = safeLongToInt(freeMem);
        int totalRamInt = safeLongToInt(totalMem);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (prefs.getBoolean("customMemory", false)) {
            return Integer.parseInt(prefs.getString("memory", String.valueOf(256)));
        } else {
            return freeRamInt - 50;
        }
    }
}
