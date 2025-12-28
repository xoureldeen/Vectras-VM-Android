package com.vectras.vm.crashtracker;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.AppConfig;
import com.vectras.vm.R;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.FileUtils;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private final String TAG = "CrashHandler";
    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    public CrashHandler(Context ctx) {
        context = ctx.getApplicationContext();
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Log.e(TAG, "uncaughtException: ", e);

        DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss", Locale.US);
        String time = DATE_FORMAT.format(new Date());

        LinkedHashMap<String, String> head = new LinkedHashMap<>();
        head.put("Time Of Crash", time);
        head.put("Device", String.format("%s, %s", Build.MANUFACTURER, Build.MODEL));
        head.put("Android Version", String.format(Locale.US,"%s (%d)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
        head.put("App Version", String.format(Locale.US, "%s (%d)", context.getString(R.string.app_version), context.getResources().getInteger(R.integer.app_version_code)));
        head.put("Kernel", DeviceUtils.getKernel());
        head.put("Support Abis",
                Build.SUPPORTED_ABIS != null
                        ? Arrays.toString(Build.SUPPORTED_ABIS)
                        : "unknown");
        head.put("Fingerprint", Build.FINGERPRINT);

        StringBuilder builder = new StringBuilder();

        for (String key : head.keySet()) {
            if (builder.length() != 0)
                builder.append("\n");
            builder.append(key);
            builder.append(" :    ");
            builder.append(head.get(key));
        }

        builder.append("\n\n");
        builder.append(Log.getStackTraceString(e));

        File file = new File(AppConfig.lastCrashLogPath);

        FileUtils.writeToFile(file.getParent(), file.getName(), builder.toString());
        MainSettingsManager.setShowLastCrashLog(context.getApplicationContext(), true);

        defaultHandler.uncaughtException(t, e);
    }
}