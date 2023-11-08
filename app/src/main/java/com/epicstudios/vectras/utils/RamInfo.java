package com.epicstudios.vectras.utils;

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.ActivityManager;

import com.epicstudios.vectras.MainActivity;
import com.epicstudios.vectras.logger.VectrasStatus;

public class RamInfo {

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    public static String vectrasMemory() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) MainActivity.activity.getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long freeMem = mi.availMem / 1048576L;
        long totalMem = mi.totalMem / 1048576L;
        long usedMem = totalMem - freeMem;
        int freeRamInt = safeLongToInt(freeMem);
        int totalRamInt = safeLongToInt(totalMem);
        if (freeRamInt > 16384) {
            return "15360";
        } else if (freeRamInt > 15360) {
            return "14336";
        } else if (freeRamInt > 14336) {
            return "13312";
        } else if (freeRamInt > 13312) {
            return "12288";
        } else if (freeRamInt > 12288) {
            return "11264";
        } else if (freeRamInt > 11264) {
            return "10240";
        } else if (freeRamInt > 10240) {
            return "9216";
        } else if (freeRamInt > 9216) {
            return "8192";
        } else if (freeRamInt > 8192) {
            return "7168";
        } else if (freeRamInt > 7168) {
            return "6114";
        } else if (freeRamInt > 6114) {
            return "5120";
        } else if (freeRamInt > 5120) {
            return "4096";
        } else if (freeRamInt > 4096) {
            return "3072";
        } else if (freeRamInt > 3072) {
            return "2048";
        } else if (freeRamInt > 2048) {
            return "1024";
        } else if (freeRamInt > 1024) {
            return "786";
        } else if (freeRamInt > 786) {
            return "512";
        } else if (freeRamInt > 512) {
            return "256";
        } else {
            return "256";
        }
    }
}
