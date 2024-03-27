package com.vectras.vm;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.widget.ImageView.ScaleType;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.utils.FileUtils;

import java.io.File;
import java.util.Hashtable;
import java.util.Objects;

/**
 * @author dev
 */
public class AppConfig {

    // App Config
    public static final String vectrasVersion = "2.8.4";
    public static final String vectrasWebsite = "https://vectras.netlify.com/";
    public static final String vectrasHelp = "https://vectras.netlify.app/how";
    public static final String vectrasRaw = "https://raw.githubusercontent.com/epicstudios856/Vectras-windows-emulator/main/";
    public static final String vectrasLicense = vectrasRaw + "LICENSE.md";
    public static final String vectrasPrivacy = vectrasRaw + "PRIVACYANDPOLICY.md";
    public static final String vectrasTerms = vectrasRaw + "TERMSOFSERVICE.md";
    public static final String vectrasInfo = vectrasRaw + "info.md";
    public static final String vectrasRepo = "https://github.com/epicstudios856/Vectras-VM-Android";
    public static final String updateJson = vectrasRaw + "UpdateConfig.json";
    public static final String blogJson = vectrasRaw + "news_list.json";
    public static final String storeJson = vectrasRaw + "store_list.json";
    public static final String vectrasPkg = vectrasWebsite + "download";

    public static final String serverIP = "https://vectrasvm.blackstorm.cc/";

    public static String getSetupFiles() {
        String abi = Build.SUPPORTED_ABIS[0];
        return serverIP + "vectras-vm/Releases/" + vectrasVersion + "/packages/" + abi + "-vectras-vm-linux.tar.gz";
    }

    public static final String romsJson(Activity activity) {
        if (Objects.equals(MainSettingsManager.getArch(activity), "X86_64")) {
            return vectrasRaw + "roms-x86_64.json";
        } else if (Objects.equals(MainSettingsManager.getArch(activity), "I386")) {
            return vectrasRaw + "roms-i386.json";
        } else if (Objects.equals(MainSettingsManager.getArch(activity), "ARM")) {
            return vectrasRaw + "roms-aarch64.json";
        } else if (Objects.equals(MainSettingsManager.getArch(activity), "PPC")) {
            return vectrasRaw + "roms-ppc.json";
        } else {
            return null;
        }
    }

    // App config
    public static final String datadirpath(Activity activity) {
        File f = new File(activity.getExternalFilesDir("data") + "/Vectras");
        return activity.getExternalFilesDir("data") + "/Vectras";
        //return FileUtils.getExternalFilesDirectory(activity).getPath();
    }

    ;
    public static final String sharedFolder = FileUtils.getExternalFilesDirectory(MainActivity.activity).getPath() + "/SharedFolder/";
    public static final String basefiledir = datadirpath(SplashActivity.activity) + "/.qemu/";
    public static final String maindirpath = datadirpath(SplashActivity.activity) + "/";

}
