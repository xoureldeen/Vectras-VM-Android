package com.anbui.elephant.log;

import android.util.Log;

import com.vectras.vm.BuildConfig;

public class LogPrinter {
    public static void print(String TAG, String text, Throwable throwable) {
        if (isPrint()) Log.e(TAG, text, throwable);
    }

    public static void print(String TAG, Throwable throwable) {
        if (isPrint()) Log.e(TAG, "", throwable);
    }

    public static void print(String TAG, String text) {
        if (isPrint()) Log.d(TAG, text);
    }

    private static boolean isPrint() {
        return BuildConfig.DEBUG;
    }
}
