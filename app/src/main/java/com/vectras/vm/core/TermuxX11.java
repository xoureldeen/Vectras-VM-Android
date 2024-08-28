package com.vectras.vm.core;

import android.content.pm.PackageInfo;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import com.vectras.vm.VectrasApp;

import dalvik.system.PathClassLoader;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class TermuxX11 {
    private static final String TAG = "boxvidra.TermuxX11";
    private static final String TARGET_APP_ID = "com.termux.x11";
    private static final String TARGET_CLASS_ID = "com.termux.x11.CmdEntryPoint";

    public static void main(String[] args) throws ErrnoException {
        String filesDir = VectrasApp.vectrasapp.getFilesDir().getAbsolutePath();
        File xkbConfigRoot = new File(filesDir + "/distro/usr/share/X11/xkb");
        if (!xkbConfigRoot.exists())
            throw new RuntimeException("XKB_CONFIG_ROOT not found: " + xkbConfigRoot);
        Os.setenv("XKB_CONFIG_ROOT", xkbConfigRoot.getAbsolutePath(), true);

        File tmpDir = new File(VectrasApp.vectrasapp.getFilesDir(), "tmp");
        deleteRecursively(tmpDir);
        tmpDir.mkdirs();
        Os.setenv("TMPDIR", tmpDir.toString(), true);

        try {
            PackageInfo targetInfo = VectrasApp.vectrasapp.getPackageManager().getPackageInfo(TARGET_APP_ID, 0);
            if (targetInfo == null) throw new RuntimeException("Termux:X11 not installed");
            Log.i(TAG, "Running " + targetInfo.applicationInfo.sourceDir + "::" + TARGET_CLASS_ID + "::main of " + TARGET_APP_ID + " application");
            Class<?> targetClass = Class.forName(
                    TARGET_CLASS_ID, true,
                    new PathClassLoader(targetInfo.applicationInfo.sourceDir, null, ClassLoader.getSystemClassLoader())
            );
            targetClass.getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (AssertionError e) {
            System.err.println(e.getMessage());
        } catch (InvocationTargetException e) {
            if (e.getCause() != null) e.getCause().printStackTrace(System.err);
        } catch (Throwable e) {
            Log.e(TAG, "Termux:X11 error", e);
            e.printStackTrace(System.err);
        }
    }

    // Assuming a custom method similar to Kotlin's deleteRecursively
    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
