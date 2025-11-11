package com.vectras.vm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.home.HomeActivity;
import com.vectras.vm.setupwizard.SetupWizardActivity;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.UIUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class SplashActivity extends AppCompatActivity implements Runnable {
    public static SplashActivity activity;
    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        activity = this;
        UIUtils.edgeToEdge(this);
        setContentView(R.layout.activity_splash);
        UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));

        setupFolders();

        try {
            new Handler().postDelayed(activity, 1000);
        } catch (Exception e) {
            Log.e(TAG, "Handler().postDelayed: ", e);
        }
        MainSettingsManager.setOrientationSetting(activity, 1);

        setupFiles();

        updateLocale();
    }

    private void updateLocale() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String languageCode = sharedPreferences.getString("language", "en");

        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    public void setupFiles() {
        File tmpDir = new File(activity.getFilesDir(), "usr/tmp");
        if (!tmpDir.isDirectory()) {
            if (tmpDir.mkdirs()) {
                FileUtils.chmod(tmpDir, 0771);
            } else {
                Log.e(TAG, "tmpDir: Directory creation failed!");
            }
        }

        File vDir = new File(com.vectras.vm.AppConfig.maindirpath);
        if (!vDir.exists()) {
            if (!vDir.mkdirs()) Log.e(TAG, com.vectras.vm.AppConfig.maindirpath + ": Directory creation failed!");
        }

        File distroDir = new File(AppConfig.internalDataDirPath + "distro");
        if (!distroDir.exists()) {
            if(!distroDir.mkdirs()) Log.e(TAG, "distro: Directory creation failed!");
        }

        File cvbiDir = new File(FileUtils.getExternalFilesDirectory(activity).getPath() + "/cvbi");
        if (!cvbiDir.exists()) {
            if(!cvbiDir.mkdirs()) Log.e(TAG, "cvbi: Directory creation failed!");
        }

        File sharedDir = new File(AppConfig.sharedFolder);
        if (!sharedDir.exists()) {
            if(!sharedDir.mkdirs()) Log.e(TAG, AppConfig.sharedFolder + ": Directory creation failed!");
        }

        File downloadsDir = new File(AppConfig.downloadsFolder);
        if (!downloadsDir.exists()) {
            if(!downloadsDir.mkdirs()) Log.e(TAG, AppConfig.downloadsFolder+ ": Directory creation failed!");
        }

        File jsonFile = new File(AppConfig.maindirpath
                + "roms-data.json");
        if (!jsonFile.exists())
            try {

                if (!jsonFile.exists()) {
                    jsonFile.createNewFile();
                }

                FileWriter writer = new FileWriter(jsonFile);
                writer.write("[]");
                writer.flush();
                writer.close();
            } catch (IOException e) {
                Log.e(TAG, "Create roms-data.json file failed: ", e);
            }

        com.vectras.qemu.utils.FileInstaller.installFiles(activity, true);
    }

    public static void setupFolders() {
        try {
            StartVM.cache = activity.getCacheDir().getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, activity.getCacheDir().getAbsolutePath() + ": Directory creation failed!", e);
        }
    }

    @Override
    public void run() {
        if ((new File(AppConfig.internalDataDirPath, "distro/usr/local/bin/qemu-system-x86_64").exists()) || (new File(AppConfig.internalDataDirPath, "distro/usr/bin/qemu-system-x86_64").exists())) {
            startActivity(new Intent(this, HomeActivity.class));
        } else {
            startActivity(new Intent(this, SetupWizardActivity.class));
            //For Android 14+
            if (!DeviceUtils.is64bit() || Build.VERSION.SDK_INT >= 34) {
                MainSettingsManager.setVmUi(this, "VNC");
            }
        }
        finish();
    }
}
