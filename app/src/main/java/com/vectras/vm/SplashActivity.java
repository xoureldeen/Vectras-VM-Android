package com.vectras.vm;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.vectras.qemu.Config;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.crashtracker.CrashTrackerData;
import com.vectras.vm.crashtracker.CrashTrackerUtils;
import com.vectras.vm.crashtracker.LastCrashActivity;
import com.vectras.vm.main.MainActivity;
import com.vectras.vm.manager.VmFileManager;
import com.vectras.vm.setupwizard.SetupFeatureCore;
import com.vectras.vm.setupwizard.SetupWizard2Activity;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.UIUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;

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
            checkLastANR();
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
        FileUtils.delete(Config.getCacheDir());
        VmFileManager.quickCleanUp(this);
    }

    private void finishSplash() {
        if (MainSettingsManager.getShowLastCrashLog(this)) {
            startActivity(new Intent(this, LastCrashActivity.class));
        } else if (SetupFeatureCore.isInstalledQemu(this)) {
            if (MainSettingsManager.getCoreSetupVersion(this) != AppConfig.coreSetupVersion) {
                Intent intent = new Intent();
                intent.putExtra("action", SetupWizard2Activity.ACTION_CORE_SYSTEM_UPDATE);
                intent.setClass(this, SetupWizard2Activity.class);
                startActivity(intent);
            } else if (MainSettingsManager.getStandardSetupVersion(this) != AppConfig.standardSetupVersion &&
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
           /* if (!DeviceUtils.is64bit() || Build.VERSION.SDK_INT >= 34) {
                MainSettingsManager.setVmUi(this, "VNC");
            }*/
        }
        finish();
    }

    private void checkLastANR() {
        if (Build.VERSION.SDK_INT >= 30) {
            CrashTrackerData crashTrackerData = new CrashTrackerData(this);

            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ApplicationExitInfo> exitInfos = am.getHistoricalProcessExitReasons(null, 0, 1);

            if (!exitInfos.isEmpty()) {
                ApplicationExitInfo lastExit = exitInfos.get(0);

                if (lastExit.getReason() == ApplicationExitInfo.REASON_ANR) {
                    long anrTimestamp = lastExit.getTimestamp();

                    if (anrTimestamp != crashTrackerData.getLastANR()) {
                        File file = new File(AppConfig.lastCrashLogPath);

                        try (InputStream traceStream = lastExit.getTraceInputStream()) {

                            if (traceStream == null) return;

                            Scanner scanner = new Scanner(traceStream, "UTF-8").useDelimiter("\\A");
                            String traceText = scanner.hasNext() ? scanner.next() : "";

                            if (traceText.isEmpty()) return;

                            LinkedHashMap<String, String> head = CrashTrackerUtils.getClientInfo(this, anrTimestamp, true);
                            String diagnosticResults = CrashTrackerUtils.arnDiagnosis(traceText);

                            StringBuilder builder = new StringBuilder();

                            for (String key : head.keySet()) {
                                if (builder.length() != 0)
                                    builder.append("\n");
                                builder.append(key);
                                builder.append(" :    ");
                                builder.append(head.get(key));
                            }

                            builder.append("\n\n");

                            if (!diagnosticResults.isEmpty()) {
                                builder.append("Diagnostic results:\n");
                                builder.append(diagnosticResults);
                                builder.append("\n\n");
                            }

                            builder.append(traceText);

                            if (FileUtils.writeToFile(file.getParent(), file.getName(), builder.toString())) {
                                MainSettingsManager.setShowLastCrashLog(this, true);
                                crashTrackerData.setLastANR(anrTimestamp);
                            }
                        } catch (Exception ignored) {

                        }
                    }
                }
            }
        }
    }
}
