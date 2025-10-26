package com.vectras.vm.home.core;

import static android.os.Build.VERSION.SDK_INT;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.vectras.qemu.Config;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.MainVNCActivity;
import com.vectras.vm.AppConfig;
import com.vectras.vm.MainService;
import com.vectras.vm.R;
import com.vectras.vm.VMManager;
import com.vectras.vm.logger.VectrasStatus;
import com.vectras.vm.settings.ExternalVNCSettingsActivity;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.NetworkUtils;
import com.vectras.vm.utils.ServiceUtils;

import java.io.File;

import au.com.darkside.xdemo.XServerActivity;

public class HomeStartVM {
    public static final String TAG = "HomeStartVM";
    public static AlertDialog progressDialog;
    public static boolean skipIDEwithARM64DialogInStartVM = false;
    public static boolean isStopNow = false;
    public static final Handler handlerForLaunch = new Handler(Looper.getMainLooper());
    public static Runnable tickForLaunch = null;
    public static String pendingVMName = "";
    public static String pendingEnv = "";
    public static String pendingVMID = "";
    public static String pendingThumbnailFile = "";
    public static boolean isLaunchFromPending = false;

    public static void startNow(
            Activity activity,
            String vmName,
            String env,
            String vmID,
            String thumbnailFile
    ) {

        if (isLaunchFromPending) {
            isLaunchFromPending = false;
            if (pendingVMID.isEmpty()) return;
        } else {
            if (MainSettingsManager.getVmUi(activity).equals("X11") && SDK_INT >= 34) {
                pendingVMName = vmName;
                pendingEnv = "xterm -e bash -c '" + env + "'";
                pendingVMID = vmID;
                pendingThumbnailFile = thumbnailFile;
                activity.startActivity(new Intent(activity, XServerActivity.class));
                return;
            }
        }

        isStopNow = false;

        String finalvmID;
        if (vmID == null || vmID.isEmpty()) {
            finalvmID = VMManager.startRamdomVMID();
        } else {
            finalvmID = vmID;
        }

        Config.vmID = finalvmID;

        File romDir = new File(Config.getCacheDir() + "/" + finalvmID);
        if (!romDir.exists()) {
            if (!romDir.mkdirs()) {
                DialogUtils.oneDialog(activity, activity.getString(R.string.problem_has_been_detected), activity.getString(R.string.vm_cache_dir_failed_to_create_content), activity.getString(R.string.ok), true, R.drawable.warning_48px, true, null, null);
                return;
            }
        }

        if (!VMManager.isthiscommandsafe(env, activity.getApplicationContext())) {
            DialogUtils.oneDialog(activity, activity.getString(R.string.problem_has_been_detected), activity.getString(R.string.harmful_command_was_detected) + " " + activity.getResources().getString(R.string.reason) + ": " + VMManager.latestUnsafeCommandReason, activity.getString(R.string.ok), true, R.drawable.verified_user_24px, true, null, null);
            return;
        }

        if (MainSettingsManager.getSharedFolder(activity)
                && !MainSettingsManager.getArch(activity).equals("I386")
                && FileUtils.getFolderSize(FileUtils.getExternalFilesDirectory(activity).getPath() + "/SharedFolder") * Math.pow(10, -6) > 516) {
            DialogUtils.twoDialog(
                    activity,
                    activity.getString(R.string.problem_has_been_detected),
                    activity.getString(R.string.shared_folder_is_too_large_content),
                    activity.getString(R.string.open_shared_folder),
                    activity.getString(R.string.close),
                    true,
                    R.drawable.warning_48px,
                    true,
                    () -> FileUtils.openFolder(activity, FileUtils.getExternalFilesDirectory(activity).getPath() + "/SharedFolder"),
                    null,
                    null
            );
            return;
        }

        VMManager.lastQemuCommand = env;

        if (VMManager.isVMRunning(activity, finalvmID)) {
            Toast.makeText(activity, "This VM is already running.", Toast.LENGTH_LONG).show();
            if (MainSettingsManager.getVmUi(activity).equals("VNC"))
                activity.startActivity(new Intent(activity, MainVNCActivity.class));
            else if (MainSettingsManager.getVmUi(activity).equals("X11"))
                DisplaySystem.launchX11(activity, false);
            return;
        }

        if (AppConfig.getSetupFiles().contains("arm") && !AppConfig.getSetupFiles().contains("arm64")) {
            if (env.contains("tcg,thread=multi")) {
                DialogUtils.twoDialog(activity, activity.getResources().getString(R.string.problem_has_been_detected), activity.getResources().getString(R.string.can_not_use_mttcg), activity.getString(R.string.ok), activity.getString(R.string.cancel), true, R.drawable.warning_48px, true,
                        () -> startNow(activity, vmName, env.replace("tcg,thread=multi", "tcg,thread=single"), finalvmID, thumbnailFile), null, null);
                return;
            }
        }

        if (MainSettingsManager.getArch(activity).equals("ARM64") && MainSettingsManager.getIfType(activity).equals("ide") && skipIDEwithARM64DialogInStartVM) {
            DialogUtils.twoDialog(activity, activity.getString(R.string.problem_has_been_detected), activity.getString(R.string.you_cannot_use_IDE_hard_drive_type_with_ARM64), activity.getString(R.string.continuetext), activity.getString(R.string.cancel), true, R.drawable.warning_48px, true,
                    () -> {
                        skipIDEwithARM64DialogInStartVM = true;
                        startNow(activity, vmName, env, finalvmID, thumbnailFile);
                    }, null, null);
            return;
        } else if (skipIDEwithARM64DialogInStartVM) {
            skipIDEwithARM64DialogInStartVM = false;
        }

        if (MainSettingsManager.getSharedFolder(activity) && MainSettingsManager.getArch(activity).equals("I386")) {
            Toast.makeText(activity, R.string.shared_folder_is_not_used_because_i386_does_not_support_it, Toast.LENGTH_LONG).show();
        }

        if (MainSettingsManager.getVncExternal(activity) &&
                NetworkUtils.isPortOpen("localhost", Config.defaultVNCPort + Config.defaultVNCPort, 500)) {
            DialogUtils.twoDialog(activity, activity.getString(R.string.problem_has_been_detected),
                    activity.getString(R.string.the_vnc_server_port_you_set_is_currently_in_use_by_other),
                    activity.getString(R.string.go_to_settings),
                    activity.getString(R.string.close),
                    true, R.drawable.warning_48px, true,
                    () -> activity.startActivity(new Intent(activity, ExternalVNCSettingsActivity.class)),
                    null,
                    null);
            return;
        }

        showProgressDialog(activity, vmName, thumbnailFile);

        VMManager.isQemuStopedWithError = false;

        if (ServiceUtils.isServiceRunning(activity, MainService.class)) {
            MainService.startCommand(env, activity);
        } else {
            Intent serviceIntent = new Intent(activity, MainService.class);
            MainService.env = env;
            MainService.CHANNEL_ID = vmName;
            MainService.activity = activity;
            if (SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(serviceIntent);
            } else {
                activity.startService(serviceIntent);
            }
        }

        tickForLaunch = new Runnable() {
            @Override
            public void run() {
                if (isStopNow || VMManager.isQemuStopedWithError || FileUtils.isFileExists(Config.getLocalQMPSocketPath())) {
                    handlerForLaunch.removeCallbacks(this);

                    progressDialog.dismiss();

                    if (!isStopNow && !VMManager.isQemuStopedWithError) {
                        if (MainSettingsManager.getVmUi(activity).equals("VNC")) {
                            if (MainSettingsManager.getVncExternal(activity)) {
                                Config.currentVNCServervmID = finalvmID;
                                DialogUtils.oneDialog(activity, activity.getString(R.string.vnc_server), activity.getString(R.string.running_vm_with_vnc_server_content) + " " + (Integer.parseInt(MainSettingsManager.getVncExternalDisplay(activity)) + 5900) + ".", activity.getString(R.string.ok), true, R.drawable.cast_24px, true, null, null);
                            } else {
                                MainVNCActivity.started = true;
                                activity.startActivity(new Intent(activity, MainVNCActivity.class));
                            }
//                    } else if (MainSettingsManager.getVmUi(activity).equals("SPICE")) {
//                        //This feature is not available yet.
                        } else if (MainSettingsManager.getVmUi(activity).equals("X11") && SDK_INT < 34) {
                            DisplaySystem.launchX11(activity, false);
                        }

                        Log.i(TAG, "Virtual machine running.");
                    }

                    skipIDEwithARM64DialogInStartVM = false;
                    tickForLaunch = null;
                    return;
                }

                handlerForLaunch.postDelayed(this, 500);
            }
        };
        handlerForLaunch.postDelayed(tickForLaunch, 1000);

        String[] params = env.split("\\s+");
        VectrasStatus.logInfo("Params:");
        Log.d("HomeStartVM", "Params:");
        for (int i = 0; i < params.length; i++) {
            VectrasStatus.logInfo(i + ": " + params[i]);
            Log.d("HomeStartVM", i + ": " + params[i]);
        }

    }

    public static void startPending(Activity activity) {
        isLaunchFromPending = true;
        startNow(activity, pendingVMName, pendingEnv, pendingVMID, pendingThumbnailFile);
    }

    public static void showProgressDialog(Activity activity, String _content, String thumbnailFile) {
        View progressView = LayoutInflater.from(activity).inflate(R.layout.dialog_start_vm, null);
        TextView tvVMName = progressView.findViewById(R.id.vm_name);
        tvVMName.setText(_content);

        if (thumbnailFile != null) {
            ImageView ivThumbnail = progressView.findViewById(R.id.iv_thumbnail);

            if (thumbnailFile.isEmpty()) {
                VMManager.setIconWithName(ivThumbnail, _content);
            } else {
                if (FileUtils.isFileExists(thumbnailFile)) {
                    Glide.with(activity.getApplicationContext())
                            .load(new File(thumbnailFile))
                            .placeholder(R.drawable.ic_computer_180dp_with_padding)
                            .error(R.drawable.ic_computer_180dp_with_padding)
                            .into(ivThumbnail);
                } else {
                    VMManager.setIconWithName(ivThumbnail, _content);
                }
            }
        }

        ImageView ivStop = progressView.findViewById(R.id.ivStop);
        ivStop.setOnClickListener(v -> {
            isStopNow = true;
            VMManager.shutdownCurrentVM();
        });

        progressDialog = new MaterialAlertDialogBuilder(activity, R.style.CenteredDialogTheme)
                .setView(progressView)
                .setCancelable(false)
                .create();

        progressDialog.show();
    }
}
