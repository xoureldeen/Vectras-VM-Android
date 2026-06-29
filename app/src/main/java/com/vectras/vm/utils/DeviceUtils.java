package com.vectras.vm.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.input.InputManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.InputDevice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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

    public static boolean isStorageLow(Context context, boolean isCheckVeryLow) {
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

                return availableMB < (isCheckVeryLow ? 256 : 2048);
            } catch (IOException e) {
                Log.e(TAG, "Error getting storage stats", e);
            }
        }

        return false;
    }

    public static boolean is64bit() {
        return new CpuHelper().is64Bit();
    }
    public static boolean isArm() {
        return Build.SUPPORTED_ABIS[0].contains("arm");
    }

    public static String getKernel() {
        String v = System.getProperty("os.version");
        if (v != null && !v.isEmpty()) return v;

        try (BufferedReader br = new BufferedReader(
                new FileReader("/proc/version"))) {
            return br.readLine();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static boolean isLargeScreen(Context context) {
        Configuration config = context.getResources().getConfiguration();
        return config.smallestScreenWidthDp >= 600;
    }

    public static boolean isHighDpi(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int currentDpi = metrics.densityDpi;
        Log.i(TAG, "isHighDpi: " + currentDpi);
        return currentDpi >= 600;
    }

    public static int getMaxRefreshRate(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Display.Mode[] modes = display.getSupportedModes();

        float maxHz = display.getRefreshRate();
        for (Display.Mode mode : modes) {
            if (mode.getRefreshRate() > maxHz) {
                maxHz = mode.getRefreshRate();
            }
        }
        return Math.round(maxHz);
    }

    public static int getBatteryPropertyCapacity(Context context) {
        BatteryManager bm= (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    public static int getBatteryCycleCount(Context context) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, filter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (batteryStatus == null) return -1;

            return  batteryStatus.getIntExtra(
                    BatteryManager.EXTRA_CYCLE_COUNT, -1
            );
        }

        return -1;
    }

    public static boolean isMouseConnected(Context context) {
        InputManager inputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
        int[] deviceIds = inputManager.getInputDeviceIds();
        for (int id : deviceIds) {
            InputDevice device = inputManager.getInputDevice(id);
            if (device != null) {
                int sources = device.getSources();
                if ((sources & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE
                        || (sources & InputDevice.SOURCE_MOUSE_RELATIVE) == InputDevice.SOURCE_MOUSE_RELATIVE) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isMouseSource(int sources) {
        return (sources & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE
                || (sources & InputDevice.SOURCE_MOUSE_RELATIVE) == InputDevice.SOURCE_MOUSE_RELATIVE;
    }

    public static boolean isColorOS(Context context) {
        return PackageUtils.isInstalled("com.nearme.themespace", context);
    }
}
