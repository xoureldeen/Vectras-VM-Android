package com.vectras.vm.main.core;

import static android.os.Build.VERSION.SDK_INT;

import android.content.Context;
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
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.NetworkUtils;
import com.vectras.vm.utils.PackageUtils;
import com.vectras.vm.utils.ServiceUtils;
import com.vectras.vterm.Terminal;

import java.io.File;

public class MainStartVM {
    public static final String TAG = "HomeStartVM";
    public static AlertDialog progressDialog;
    public static boolean skipIDEwithARM64DialogInStartVM = false;
    public static boolean isStopNow = false;
    public static final Handler handlerForLaunch = new Handler(Looper.getMainLooper());
    public static Runnable tickForLaunch = null;

    public static String lastVMName = "";
    public static String lastEnv = "";
    public static String lastVMID = "";
    public static String lastThumbnailFile = "";
    public static String pendingVMName = "";
    public static String pendingEnv = "";
    public static String pendingVMID = "";
    public static String pendingThumbnailFile = "";
    public static boolean isLaunchFromPending = false;
    public static String runCommandFormat = "export TMPDIR=/tmp && mkdir -p $TMPDIR/pulse && export XDG_RUNTIME_DIR=/tmp && chmod -R 775 $TMPDIR/pulse && pulseaudio --start --exit-idle-time=-1 > /dev/null 2>&1 && %s";

    public static void startNow(
            Context context,
            String vmName,
            String env,
            String vmID,
            String thumbnailFile
    ) {

        if (isLaunchFromPending) {
            isLaunchFromPending = false;
            if (pendingVMID.isEmpty()) return;
            pendingVMID = "";
        } else {
            lastVMName = vmName;
            lastEnv = env;
            lastVMID = vmID;
            lastThumbnailFile = thumbnailFile;

            if (MainSettingsManager.getVmUi(context).equals("X11")) {
                if (MainSettingsManager.getRunQemuWithXterm(context)) {
                    runCommandFormat = String.format(runCommandFormat, "xterm -e bash -c \"%s\"");
                } else {
                    runCommandFormat = String.format(runCommandFormat, "bash -c \"%s\"");
                }

                if (SDK_INT < 34) {
                    pendingVMName = vmName;
                    pendingEnv = env;
                    pendingVMID = vmID;
                    pendingThumbnailFile = thumbnailFile;
                    DisplaySystem.launch(context);
                    return;
                }
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
                DialogUtils.oneDialog(
                        context,
                        context.getString(R.string.problem_has_been_detected),
                        context.getString(R.string.vm_cache_dir_failed_to_create_content),
                        R.drawable.warning_48px
                );
                return;
            }
        }

        if (!VMManager.isthiscommandsafe(env, context.getApplicationContext())) {
            DialogUtils.oneDialog(
                    context,
                    context.getString(R.string.problem_has_been_detected),
                    context.getString(R.string.harmful_command_was_detected) + " " + context.getResources().getString(R.string.reason) + ": " + VMManager.latestUnsafeCommandReason,
                    R.drawable.verified_user_24px
            );
            return;
        }

        if (MainSettingsManager.getSharedFolder(context)
                && !MainSettingsManager.getArch(context).equals("I386")
                && FileUtils.getFolderSize(FileUtils.getExternalFilesDirectory(context).getPath() + "/SharedFolder") * Math.pow(10, -6) > 516) {
            DialogUtils.twoDialog(
                    context,
                    context.getString(R.string.problem_has_been_detected),
                    context.getString(R.string.shared_folder_is_too_large_content),
                    context.getString(R.string.open_shared_folder),
                    context.getString(R.string.close),
                    true,
                    R.drawable.warning_48px,
                    true,
                    () -> FileUtils.openFolder(context, FileUtils.getExternalFilesDirectory(context).getPath() + "/SharedFolder"),
                    null,
                    null
            );
            return;
        }

        VMManager.lastQemuCommand = env;

        if (VMManager.isVMRunning(context, finalvmID)) {
            Toast.makeText(context, "This VM is already running.", Toast.LENGTH_LONG).show();
            if (MainSettingsManager.getVmUi(context).equals("VNC"))
                context.startActivity(new Intent(context, MainVNCActivity.class));
            else if (MainSettingsManager.getVmUi(context).equals("X11"))
                DisplaySystem.launchX11(context, false);
            return;
        }

        if (AppConfig.getSetupFiles().contains("arm") && !AppConfig.getSetupFiles().contains("arm64")) {
            if (env.contains("tcg,thread=multi")) {
                DialogUtils.twoDialog(context, context.getResources().getString(R.string.problem_has_been_detected), context.getResources().getString(R.string.can_not_use_mttcg), context.getString(R.string.ok), context.getString(R.string.cancel), true, R.drawable.warning_48px, true,
                        () -> startNow(context, vmName, env.replace("tcg,thread=multi", "tcg,thread=single"), finalvmID, thumbnailFile), null, null);
                return;
            }
        }

        if (MainSettingsManager.getArch(context).equals("ARM64") && MainSettingsManager.getIfType(context).equals("ide") && skipIDEwithARM64DialogInStartVM) {
            DialogUtils.twoDialog(context, context.getString(R.string.problem_has_been_detected), context.getString(R.string.you_cannot_use_IDE_hard_drive_type_with_ARM64), context.getString(R.string.continuetext), context.getString(R.string.cancel), true, R.drawable.warning_48px, true,
                    () -> {
                        skipIDEwithARM64DialogInStartVM = true;
                        startNow(context, vmName, env, finalvmID, thumbnailFile);
                    }, null, null);
            return;
        } else if (skipIDEwithARM64DialogInStartVM) {
            skipIDEwithARM64DialogInStartVM = false;
        }

        if (MainSettingsManager.getSharedFolder(context) && MainSettingsManager.getArch(context).equals("I386")) {
            Toast.makeText(context, R.string.shared_folder_is_not_used_because_i386_does_not_support_it, Toast.LENGTH_LONG).show();
        }

        if (MainSettingsManager.getVncExternal(context) &&
                NetworkUtils.isPortOpen("localhost", Config.defaultVNCPort + Config.defaultVNCPort, 500)) {
            DialogUtils.twoDialog(context, context.getString(R.string.problem_has_been_detected),
                    context.getString(R.string.the_vnc_server_port_you_set_is_currently_in_use_by_other),
                    context.getString(R.string.go_to_settings),
                    context.getString(R.string.close),
                    true, R.drawable.warning_48px, true,
                    () -> context.startActivity(new Intent(context, ExternalVNCSettingsActivity.class)),
                    null,
                    null);
            return;
        }

        showProgressDialog(context, vmName, thumbnailFile);

        VMManager.isQemuStopedWithError = false;

        String finalCommand = VMManager.addAudioDevSdl(String.format(runCommandFormat, env));

        if (MainSettingsManager.getVmUi(context).equals("X11") && SDK_INT >= 34) {
            finalCommand = "export DISPLAY=:0 &&" + finalCommand;
        }
        Log.i(TAG, finalCommand);

        Terminal vterm = new Terminal(context);
        vterm.executeShellCommand2("export DISPLAY=:0 && fluxbox > /dev/null", false, context);

        if (ServiceUtils.isServiceRunning(context, MainService.class)) {
            MainService.startCommand(finalCommand, context);
        } else {
            Intent serviceIntent = new Intent(context, MainService.class);
            MainService.env = finalCommand;
            MainService.CHANNEL_ID = vmName;
            if (SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }

        if (MainSettingsManager.getVmUi(context).equals("X11") && SDK_INT >= 34) {
            if (!PackageUtils.isInstalled("com.termux.x11", context)) {
                DialogUtils.needInstallTermuxX11(context);
                return;
            }
            DisplaySystem.launchX11(context, false);
        }

        tickForLaunch = new Runnable() {
            @Override
            public void run() {
                if (isStopNow || VMManager.isQemuStopedWithError || FileUtils.isFileExists(Config.getLocalQMPSocketPath())) {
                    handlerForLaunch.removeCallbacks(this);

                    progressDialog.dismiss();

                    if (!isStopNow && !VMManager.isQemuStopedWithError) {
                        if (MainSettingsManager.getVmUi(context).equals("VNC")) {
                            if (MainSettingsManager.getVncExternal(context)) {
                                Config.currentVNCServervmID = finalvmID;
                                DialogUtils.oneDialog(context,
                                        context.getString(R.string.vnc_server),
                                        context.getString(R.string.running_vm_with_vnc_server_content) + " " + (Integer.parseInt(MainSettingsManager.getVncExternalDisplay(context)) + 5900) + ".",
                                        R.drawable.cast_24px
                                );
                            } else {
                                MainVNCActivity.started = true;
                                context.startActivity(new Intent(context, MainVNCActivity.class));
                            }
//                    } else if (MainSettingsManager.getVmUi(activity).equals("SPICE")) {
//                        //This feature is not available yet.
                        }

                        Log.i(TAG, "Virtual machine running.");
                    } if (MainSettingsManager.getVmUi(context).equals("X11") && SDK_INT >= 34) {
                        Intent intent = new Intent();
                        intent.setClassName("com.termux.x11", "com.termux.x11.MainActivity");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        context.startActivity(intent);
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

        setDefault();
    }

    public static void startTryAgain(Context context) {
        startNow(context, lastVMName, lastEnv, lastVMID, lastThumbnailFile);
        VMManager.isTryAgain = false;
    }

    public static void startPending(Context context) {
        isLaunchFromPending = true;
        startNow(context, pendingVMName, pendingEnv, pendingVMID, pendingThumbnailFile);
        setDefault();
    }

    public static void showProgressDialog(Context context, String _content, String thumbnailFile) {
        View progressView = LayoutInflater.from(context).inflate(R.layout.dialog_start_vm, null);
        TextView tvVMName = progressView.findViewById(R.id.vm_name);
        tvVMName.setText(_content);

        if (thumbnailFile != null) {
            ImageView ivThumbnail = progressView.findViewById(R.id.iv_thumbnail);

            if (thumbnailFile.isEmpty()) {
                VMManager.setIconWithName(ivThumbnail, _content);
            } else {
                if (FileUtils.isFileExists(thumbnailFile)) {
                    Glide.with(context.getApplicationContext())
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

        progressDialog = new MaterialAlertDialogBuilder(context, R.style.CenteredDialogTheme)
                .setView(progressView)
                .setCancelable(false)
                .create();

        progressDialog.show();
    }

    public static void setDefault() {
        runCommandFormat = "export TMPDIR=/tmp && mkdir -p $TMPDIR/pulse && export XDG_RUNTIME_DIR=/tmp && chmod -R 775 $TMPDIR/pulse && pulseaudio --start --exit-idle-time=-1 > /dev/null 2>&1 && %s";
    }
}
