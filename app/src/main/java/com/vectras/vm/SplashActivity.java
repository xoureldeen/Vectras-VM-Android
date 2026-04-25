package com.vectras.vm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.PreferenceManager;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.utils.FileInstaller;
import com.vectras.vm.crashtracker.LastCrashActivity;
import com.vectras.vm.main.MainActivity;
import com.vectras.vm.manager.VmFileManager;
import com.vectras.vm.setupwizard.SetupFeatureCore;
import com.vectras.vm.setupwizard.SetupWizard2Activity;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.UIUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        UIUtils.edgeToEdge(this);
        setContentView(R.layout.activity_splash);
        UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));

        new Thread(() -> {
            setupFolders();
            MainSettingsManager.setOrientationSetting(this, 1);
            setupFiles();
            cleanUp();
            runOnUiThread(this::finishSplash);
        }).start();
    }

    public void setupFiles() {
        File tmpDir = new File(getFilesDir(), "usr/tmp");
        if (!tmpDir.isDirectory()) {
            if (tmpDir.mkdirs()) {
                FileUtils.chmod(tmpDir, 0771);
            } else {
                Log.e(TAG, "tmpDir: Directory creation failed!");
            }
        }

        File vDir = new File(com.vectras.vm.AppConfig.maindirpath);
        if (!vDir.exists()) {
            if (!vDir.mkdirs())
                Log.e(TAG, com.vectras.vm.AppConfig.maindirpath + ": Directory creation failed!");
        }

        File distroDir = new File(AppConfig.internalDataDirPath + "distro");
        if (!distroDir.exists()) {
            if (!distroDir.mkdirs()) Log.e(TAG, "distro: Directory creation failed!");
        }

        File cvbiDir = new File(FileUtils.getExternalFilesDirectory(this).getPath() + "/cvbi");
        if (!cvbiDir.exists()) {
            if (!cvbiDir.mkdirs()) Log.e(TAG, "cvbi: Directory creation failed!");
        }

        File sharedDir = new File(AppConfig.sharedFolder);
        if (!sharedDir.exists()) {
            if (!sharedDir.mkdirs())
                Log.e(TAG, AppConfig.sharedFolder + ": Directory creation failed!");
        }

        File downloadsDir = new File(AppConfig.downloadsFolder);
        if (!downloadsDir.exists()) {
            if (!downloadsDir.mkdirs())
                Log.e(TAG, AppConfig.downloadsFolder + ": Directory creation failed!");
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

        //FileInstaller.installFiles(this, true);
    }

    private void setupFolders() {
        try {
            StartVM.cache = getCacheDir().getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, getCacheDir().getAbsolutePath() + ": Directory creation failed!", e);
        }
    }

    private void cleanUp() {
        FileUtils.delete(AppConfig.vmFolder + "QuickRun");
        VmFileManager.removeTemp(this, "");
        FileUtils.delete(new File(getExternalCacheDir(), "logs"));
        FileUtils.delete(new File(getExternalCacheDir(), "cvbi"));
    }

    private void finishSplash() {
        if (MainSettingsManager.getShowLastCrashLog(this)) {
            startActivity(new Intent(this, LastCrashActivity.class));
        } else if (SetupFeatureCore.isInstalledQemu(this)) {
            if (MainSettingsManager.getStandardSetupVersion(this) != AppConfig.standardSetupVersion &&
                    !MainSettingsManager.getsetUpWithManualSetupBefore(this)) {
                Intent intent = new Intent();
                intent.putExtra("action", SetupWizard2Activity.ACTION_SYSTEM_UPDATE);
                intent.setClass(this, SetupWizard2Activity.class);
                startActivity(intent);
            } else {
                startActivity(new Intent(this, MainActivity.class));
            }
        } else {
            startActivity(new Intent(this, SetupWizard2Activity.class));
            //For Android 14+
            if (!DeviceUtils.is64bit() || Build.VERSION.SDK_INT >= 34) {
                MainSettingsManager.setVmUi(this, "VNC");
            }
        }
        finish();
    }
}
