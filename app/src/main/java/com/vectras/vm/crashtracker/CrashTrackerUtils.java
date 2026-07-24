package com.vectras.vm.crashtracker;

import android.content.Context;
import android.os.Build;

import com.vectras.vm.R;
import com.vectras.vm.utils.DeviceUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.SplittableRandom;

public class CrashTrackerUtils {
    public static LinkedHashMap<String, String> getClientInfo(Context context, long timestamp, boolean isANR) {
        DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss", Locale.US);
        String time = DATE_FORMAT.format(new Date(timestamp));

        LinkedHashMap<String, String> info = new LinkedHashMap<>();
        info.put("Time Of " + (isANR ? "ANR" : "Crash"), time);
        info.put("Device", String.format("%s, %s", Build.MANUFACTURER, Build.MODEL));
        info.put("Android Version", String.format(Locale.US,"%s (%d)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
        info.put("App Version", String.format(Locale.US, "%s (%d)", context.getString(R.string.app_version), context.getResources().getInteger(R.integer.app_version_code)));
        info.put("Kernel", DeviceUtils.getKernel());
        info.put("Support Abis",
                Build.SUPPORTED_ABIS != null
                        ? Arrays.toString(Build.SUPPORTED_ABIS)
                        : "unknown");
        info.put("Fingerprint", Build.FINGERPRINT);

        return info;
    }

    public static String arnDiagnosis(String log) {
        StringBuilder predictions = new StringBuilder();

        if (
                log.contains("android.app.QueuedWork.processPendingWork") &&
                        log.contains("- waiting to lock <") &&
                        log.contains("android.app.QueuedWork.waitToFinish")
        ) {
            predictions.append("I/O congestion\n");
        }

        if (log.contains("android.os.BinderProxy.transactNative")) {
            predictions.append("system_server slow\n");
        }

        return predictions.toString().trim();
    }
}
