package com.vectras.vm.core;

import android.content.pm.PackageInfo;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import com.vectras.vm.VectrasApp;
import com.vectras.vm.utils.FileUtils;

import dalvik.system.PathClassLoader;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class TermuxX11 {
    private static final String TAG = "TermuxX11";
    private static final String TARGET_APP_ID = "com.termux.x11";
    private static final String TARGET_CLASS_ID = "com.termux.x11.CmdEntryPoint";

    public static void main(String[] args) throws ErrnoException {
        File xkbConfigRoot = new File(VectrasApp.getContext().getFilesDir().getAbsolutePath() + "/usr/share/X11/xkb");
        if (xkbConfigRoot.exists()) {
            Os.setenv("XKB_CONFIG_ROOT", xkbConfigRoot.getAbsolutePath(), true);
            File tmpDir = new File(VectrasApp.getContext().getFilesDir().getAbsolutePath() + "/usr/tmp");
            FileUtils.deleteDirectory(tmpDir.getAbsolutePath());
            tmpDir.mkdirs();
            Os.setenv("TMPDIR", tmpDir.toString(), true);
            try {
                PackageInfo targetInfo = VectrasApp.getContext().getPackageManager().getPackageInfo(TARGET_APP_ID, 0);
                if (targetInfo.applicationInfo != null) {
                    Log.i(TAG, "Running " + targetInfo.applicationInfo.sourceDir + "::" + TARGET_CLASS_ID + "::main of " + TARGET_APP_ID + " application");
                    Class.forName(TARGET_CLASS_ID, true, new PathClassLoader(targetInfo.applicationInfo.sourceDir, (String) null, ClassLoader.getSystemClassLoader())).getMethod("main", new Class[]{String[].class}).invoke((Object) null, new Object[]{args});
                    return;
                }
                throw new RuntimeException("Termux:X11 not installed");
            } catch (AssertionError e) {
                System.err.println(e.getMessage());
            } catch (InvocationTargetException e2) {
                if (e2.getCause() != null) {
                    e2.getCause().printStackTrace(System.err);
                }
            } catch (Throwable e3) {
                Log.e(TAG, "Termux:X11 error", e3);
                e3.printStackTrace(System.err);
            }
        } else {
            throw new RuntimeException("XKB_CONFIG_ROOT not found: " + xkbConfigRoot);
        }
    }
}

