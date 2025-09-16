package com.vectras.vm.utils;

import android.app.ActivityManager;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DeviceUtils {

    public static String TAG = "DeviceUtils";

    public static double totalMemoryCapacity(Context context) {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.totalMem;
    }

    public static boolean isStorageLow(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            try {
                File path = Environment.getDataDirectory();
                StatFs stat = new StatFs(path.getPath());

                long blockSize = stat.getBlockSizeLong();
                long availableBlocks = stat.getAvailableBlocksLong();

                long availableBytes = availableBlocks * blockSize;
                long availableMB = availableBytes / (1024 * 1024);

                return availableMB < 2048;
            } catch (Exception e) {
                Log.e(TAG, "Error getting storage stats", e);
            }
        } else {
            StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            StorageStatsManager statsManager = (StorageStatsManager) context.getSystemService(Context.STORAGE_STATS_SERVICE);

            try {
                UUID uuid = storageManager.getUuidForPath(Environment.getDataDirectory());
                long availableBytes = statsManager.getFreeBytes(uuid);
                long availableMB = availableBytes / (1024 * 1024);

                return availableMB < 2048;
            } catch (IOException e) {
                Log.e(TAG, "Error getting storage stats", e);
            }
        }

        return false;
    }

    public static boolean is64bit() {
        return Build.SUPPORTED_ABIS[0].contains("arm64");
    }
}
