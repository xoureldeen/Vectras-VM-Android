package com.vectras.vm;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.widget.ImageView.ScaleType;

import com.vectras.qemu.MainSettingsManager;

import java.io.File;
import java.util.Hashtable;
import java.util.Objects;

/**
 * @author dev
 */
public class AppConfig {

    // App Config
    public static final String vectrasWebsite = "https://vectras.netlify.com/";
    public static final String vectrasRaw = "https://raw.githubusercontent.com/epicstudios856/Vectras-windows-emulator/main/";
    public static final String vectrasLicense = vectrasRaw + "LICENSE.md";
    public static final String vectrasPrivacy = vectrasRaw + "PRIVACYANDPOLICY.md";
    public static final String vectrasTerms = vectrasRaw + "TERMSOFSERVICE.md";
    public static final String vectrasInfo = vectrasRaw + "info.md";
    public static final String vectrasRepo = "https://github.com/epicstudios856/Vectras-windows-emulator/tree/main/";
    public static final String updateJson = vectrasRaw + "UpdateConfig.json";
    public static final String blogJson = vectrasRaw + "news_list.json";
    public static final String storeJson = vectrasRaw + "store_list.json";

    public static final String romsJson(Activity activity) {
        if (Objects.equals(MainSettingsManager.getArch(activity), "X86_64")) {
            return vectrasRaw + "roms.json";
        } else if (Objects.equals(MainSettingsManager.getArch(activity), "ARM")) {
            return vectrasRaw + "roms-arm.json";
        } else {
            return null;
        }
    }

    // App config
    public static final String datadirpath = SplashActivity.activity.getExternalFilesDir("data") + "/";
    public static final String sharedFolder = datadirpath + "Vectras/ProgramFiles/";
    public static final String basefiledir = datadirpath + "Vectras/.qemu/";
    public static final String maindirpath = datadirpath + "Vectras/";

}
