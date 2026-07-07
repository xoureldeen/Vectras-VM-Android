package com.vectras.vm.main.core;

import static android.os.Build.VERSION.SDK_INT;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.vectras.qemu.Config;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.MainVNCActivity;
import com.vectras.vm.MainService;
import com.vectras.vm.R;
import com.vectras.vm.StartVM;
import com.vectras.vm.VMManager;
import com.vectras.vm.logger.VectrasStatus;
import com.vectras.vm.main.vms.DataMainRoms;
import com.vectras.vm.manager.QmpSender;
import com.vectras.vm.manager.VmFileManager;
import com.vectras.vm.manager.VmAudioManager;
import com.vectras.vm.settings.ExternalVNCSettingsActivity;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.NetworkUtils;
import com.vectras.vm.utils.PackageUtils;
import com.vectras.vm.utils.ServiceUtils;

import java.io.File;

public class MainStartVM {
    public static final String TAG = "HomeStartVM";
    public static boolean skipIDEwithARM64DialogInStartVM = false;
    public static boolean breakNow = false;
    public static final Handler handlerForLaunch = new Handler(Looper.getMainLooper());
    public static Runnable tickForLaunch = null;

    public static boolean forceDisableMigrate = false;
    public static String lastVMName = "";
    public static String lastEnv = "";
    public static String lastVMID = "";
    public static String lastThumbnailFile = "";
    public static String pendingVMName = "";
    public static String pendingEnv = "";
    public static String pendingVMID = "";
    public static String pendingThumbnailFile = "";
    public static boolean isLaunchFromPending = false;
    public static String runCommandFormat = "%s";

    private static StartVmDialog dialog;

    public static final int PENDDING_EMPTY = -1;

    public static final int STARTED_VM = 0;
    public static final int STARTED_VM_READY_RUNNING = 1;

    public static final int ERROR_VM = 10;
    public static final int ERROR_BREAK = 11;
    public static final int ERROR_INVALID_VM_ID = 12;
    public static final int ERROR_MAKE_TEMP_FOLDER = 13;
    public static final int ERROR_UNSAFE_COMMAND = 14;
    public static final int ERROR_ACCEL = 15;
    public static final int ERROR_VNC_TCP_PORT = 16;
    public static final int ERROR_SHARED_FOLDER = 17;
    public static final int ERROR_RESUME = 18;

    public interface MainStartVMCallback {
        void onStarted(int statusCode, String message);
        void onError(int errorCode, String message);
    }

    public static void startNow(Activity activity, DataMainRoms vmConfig) {
        breakNow = false;

        Config.vmID = vmConfig.vmID;

        VMManager.setArch(vmConfig.itemArch, activity);

        if (
                MainSettingsManager.getVmUi(activity).equals("VNC") ||
                        (MainSettingsManager.getVmUi(activity).equals("X11") && !DisplaySystem.isUseBuiltInX11())
        ) {
            showDialog(activity, vmConfig.vmID, vmConfig.itemName, vmConfig.itemIcon, activity.getString(R.string.preparing));
        }

        new Thread(() -> {
            if (!VMManager.isVMRunning(activity, vmConfig.vmID)) VmFileManager.removeTemp(activity, vmConfig.vmID);

            String env = StartVM.env(activity, vmConfig);
            activity.runOnUiThread(() -> startNow(activity, vmConfig.itemName, env, vmConfig.vmID, vmConfig.itemIcon, dialog));
        }).start();
    }

    public static void startNow(
            Context context,
            String vmName,
            String env,
            String vmID,
            String thumbnailFile,
            StartVmDialog dialog
    ) {
        startNow(context, vmName, env, vmID, thumbnailFile, dialog, null);
    }

    public static void startNow(
            Context context,
            String vmName,
            String env,
            String vmID,
            String thumbnailFile,
            StartVmDialog dialog,
            MainStartVMCallback callback
    ) {

        if (breakNow) {
            breakNow = false;
            if (callback != null) callback.onError(PENDDING_EMPTY, "");
            return;
        }

        setDefault();

        if (isLaunchFromPending) {
            isLaunchFromPending = false;
            if (pendingVMID.isEmpty()) {
                if (callback != null) callback.onError(ERROR_INVALID_VM_ID, "");
                return;
            }
            pendingVMID = "";
        } else {
            if (MainSettingsManager.getVmUi(context).equals("X11") && !DisplaySystem.isUseBuiltInX11()) {
                if (!PackageUtils.isInstalled("com.termux.x11", context)) {
                    DialogUtils.needInstallTermuxX11(context);
                    return;
                }
            }
            
            lastVMName = vmName;
            lastEnv = env;
            lastVMID = vmID;
            lastThumbnailFile = thumbnailFile;

            if (MainSettingsManager.getVmUi(context).equals("X11") && !VMManager.isVMRunning(context, vmID)) {
                if (MainSettingsManager.getRunQemuWithXterm(context)) {
                    String logFilePath = VmFileManager.getLog(context, vmID);
                    runCommandFormat = String.format(runCommandFormat, "mkdir -p \"" + new File(logFilePath).getParent() + "\"; xterm -e bash -c \"%s 2>&1 | tee " + logFilePath + "\"; cat " + logFilePath + "; rm " + logFilePath);
                } else {
                    runCommandFormat = String.format(runCommandFormat, "bash -c \"%s\"");
                }

                if (DisplaySystem.isUseBuiltInX11()) {
                    pendingVMName = vmName;
                    pendingEnv = env;
                    pendingVMID = vmID;
                    pendingThumbnailFile = thumbnailFile;
                    DisplaySystem.launch(context);
                    return;
                }
            }
        }

        breakNow = false;

        String finalvmID;
        if (vmID == null || vmID.isEmpty()) {
            finalvmID = VMManager.startRamdomVMID();
        } else {
            finalvmID = vmID;
        }

        Config.vmID = finalvmID;

        if (VMManager.isVMRunning(context, finalvmID)) {
            dismissDialog();

            Toast.makeText(context, context.getString(R.string.this_vm_is_already_running), Toast.LENGTH_LONG).show();
            DisplaySystem.launch(context);
            if (
                    !MainSettingsManager.getVmUi(context).equals("VNC") &&
                            !DisplaySystem.isUseBuiltInX11()
            )
                VmAudioManager.stream(vmID);

            if (callback != null) callback.onStarted(STARTED_VM_READY_RUNNING, "");
            return;
        }

        // Place it here to avoid freezing when the dialog box appears upon returning to the virtual machine.
        if (dialog == null || !dialog.isShowing()) showDialog((Activity) context, vmID, vmName, thumbnailFile, null);

        File romDir = new File(Config.getCacheDir() + "/" + finalvmID);
        if (!romDir.exists()) {
            if (!romDir.mkdirs()) {
                DialogUtils.oneDialog(
                        context,
                        context.getString(R.string.problem_has_been_detected),
                        context.getString(R.string.vm_cache_dir_failed_to_create_content),
                        R.drawable.warning_48px
                );

                dismissDialog();
                if (callback != null) callback.onError(ERROR_MAKE_TEMP_FOLDER, "");
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

            dismissDialog();
            if (callback != null) callback.onError(ERROR_UNSAFE_COMMAND, "");
            return;
        }

        if (!DeviceUtils.is64bit()) {
            if (env.contains("tcg,thread=multi")) {
                StartVmDialog finalDialog1 = dialog;
                DialogUtils.twoDialog(context, context.getResources().getString(R.string.problem_has_been_detected), context.getResources().getString(R.string.can_not_use_mttcg), context.getString(R.string.ok), context.getString(R.string.cancel), true, R.drawable.warning_48px, true,
                        () -> startNow(context, vmName, env.replace("tcg,thread=multi", "tcg,thread=single"), finalvmID, thumbnailFile, finalDialog1), null, null);

                dismissDialog();
                if (callback != null) callback.onError(ERROR_ACCEL, "");
                return;
            }
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

            dismissDialog();
            if (callback != null) callback.onError(ERROR_VNC_TCP_PORT, "");
            return;
        }

        new Thread(() -> {
            boolean isExceeded = env.contains(FileUtils.getExternalFilesDirectory(context).getPath() + "/SharedFolder") && FileUtils.getFolderSize(FileUtils.getExternalFilesDirectory(context).getPath() + "/SharedFolder") * Math.pow(10, -6) > 516;

            new Handler(Looper.getMainLooper()).post(() -> {
                if (isExceeded) {
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

                    dismissDialog();
                    if (callback != null) callback.onError(ERROR_SHARED_FOLDER, "");
                    return;
                }

                VMManager.lastQemuCommand = env;

                if (breakNow) {
                    dismissDialog();
                    if (callback != null) callback.onError(ERROR_BREAK, "");
                    return;
                }

                startVm(context, vmName, env, finalvmID, thumbnailFile, callback);
            });
        }).start();
    }

    public static void startVm(
            Context context,
            String vmName,
            String env,
            String vmID,
            String thumbnailFile,
            MainStartVMCallback callback
    ) {
        VMManager.isQemuStopedWithError = false;

        String cleanUpCommand = "; rm -r " + Config.getCacheVMPath(vmID);

        String finalCommand = VMManager.addAudioDevWav(vmID, String.format(runCommandFormat, env + cleanUpCommand));

        if (MainSettingsManager.getVmUi(context).equals("X11")) {
            finalCommand = "export DISPLAY=:0 && " + finalCommand;
            DisplaySystem.startDesktop(context);
        }
        Log.i(TAG, finalCommand);

        if (breakNow) {
            dismissDialog();
            if (callback != null) callback.onError(ERROR_BREAK, "");
            return;
        }

        if (ServiceUtils.isServiceRunning(context, MainService.class)) {
            MainService.startCommand(finalCommand, context);
        } else {
            Intent serviceIntent = new Intent(context, MainService.class);
            MainService.activityContext = context;
            MainService.env = finalCommand;
            MainService.CHANNEL_ID = vmName;
            if (SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }

        tickForLaunch = new Runnable() {
            @Override
            public void run() {
                if (breakNow || VMManager.isQemuStopedWithError || FileUtils.isFileExists(Config.getLocalQMPSocketPath())) {
                    handlerForLaunch.removeCallbacks(this);

                    Activity activity = context instanceof Activity ? (Activity) context : null;

                    if (!breakNow && !VMManager.isQemuStopedWithError) {
                        new Thread(() -> {
                            if (!forceDisableMigrate && VMManager.isNeedLoadMigrate()) {
                                if (activity != null) {
                                    activity.runOnUiThread(() -> dialog.setStatus(R.string.resuming));
                                }
                                String loadMigrateResponse = VMManager.loadMigrate();
                                Log.d(TAG, "loadMigrateResponse: " + loadMigrateResponse);

                                Boolean[] result = VMManager.getMigrateStatus();

                                while (!result[0] && !result[1]) {
                                    try {
                                        Thread.sleep(1000);
                                    } catch (Exception ignored) {

                                    }

                                    result = VMManager.getMigrateStatus();
                                }

                                if (result[0]) {
                                    VMManager.deleteMigrate();
                                } else {
                                    QmpSender.quickShutdown();
                                    assert activity != null;
                                    activity.runOnUiThread(() -> {
                                        activity.runOnUiThread(MainStartVM::dismissDialog);

                                        DialogUtils.threeDialog(
                                                activity,
                                                activity.getString(R.string.oops),
                                                activity.getString(R.string.vm_resume_error_note),
                                                activity.getString(R.string.keep_and_continue),
                                                activity.getString(R.string.remove_and_continue),
                                                activity.getString(R.string.close),
                                                true,
                                                R.drawable.error_96px,
                                                true,
                                                () -> {
                                                    forceDisableMigrate = true;
                                                    startNow(context, vmName, env.replace("-incoming defer", ""), vmID, thumbnailFile, dialog);
                                                },
                                                () -> {
                                                    VMManager.deleteMigrate();
                                                    startNow(context, vmName, env.replace("-incoming defer", ""), vmID, thumbnailFile, dialog);
                                                },
                                                null,
                                                null
                                        );

                                        if (callback != null) callback.onError(ERROR_RESUME, "");
                                    });

                                    return;
                                }
                            } else {
                                forceDisableMigrate = false;
                                if ( activity != null) activity.runOnUiThread(() -> dialog.setStatus(R.string.booting_up));
                            }

                            QmpSender.resume();

                            if (MainSettingsManager.getVmUi(context).equals("VNC")) {
                                if (MainSettingsManager.getVncExternal(context)) {
                                    Config.currentVNCServervmID = vmID;
                                    if (activity != null) {
                                        activity.runOnUiThread(() -> DialogUtils.oneDialog(context,
                                                context.getString(R.string.vnc_server),
                                                context.getString(R.string.running_vm_with_vnc_server_content) + " " + (Integer.parseInt(MainSettingsManager.getVncExternalDisplay(context)) + 5900) + ".",
                                                R.drawable.cast_24px
                                        ));
                                    }
                                } else {
                                    MainVNCActivity.started = true;
                                    context.startActivity(new Intent(context, MainVNCActivity.class));
                                }
//                    } else if (MainSettingsManager.getVmUi(activity).equals("SPICE")) {
//                        //This feature is not available yet.
                            } else if (MainSettingsManager.getVmUi(context).equals("X11") && !DisplaySystem.isUseBuiltInX11()) {
                                assert activity != null;
                                activity.runOnUiThread(() -> {
                                    DisplaySystem.launch(context);
                                    VmAudioManager.stream(vmID);
                                });
                            }

                            FileUtils.writeToFile(VmFileManager.getPath(vmID), VmFileManager.SNAPSHOT_SH_FILE_NAME, env);

                            Log.i(TAG, "Virtual machine running.");

                            if (activity != null) {
                                activity.runOnUiThread(() -> {
                                    MainStartVM.dismissDialog();
                                    if (callback != null) callback.onStarted(STARTED_VM, "");
                                });
                            }
                        }).start();
                    } else {
                        assert activity != null;
                        activity.runOnUiThread(() -> {
                            MainStartVM.dismissDialog();
                            if (callback != null) callback.onError(ERROR_VM, "");
                        });
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

    public static void startTryAgain(Context context) {
        startNow(context, lastVMName, lastEnv, lastVMID, lastThumbnailFile, dialog);
        VMManager.isTryAgain = false;
    }

    public static void startPending(Context context, MainStartVMCallback callback) {
        isLaunchFromPending = true;
        startNow(context, pendingVMName, pendingEnv, pendingVMID, pendingThumbnailFile, null, callback);
    }

    private static void showDialog(Activity activity, String vmId, String vmName, String thumbnailPath, String status) {
        if (dialog != null) {
            dialog.dismiss();
        }

        dialog = new StartVmDialog(activity, vmId, vmName, thumbnailPath);
        dialog.show(() -> breakNow = true);
        if (status != null) dialog.setStatus(status);
    }

    public static void dismissDialog() {
        if (dialog != null) dialog.dismiss();
        if (breakNow) {
            new Thread(QmpSender::shutdown).start();
        }
    }

    public static void setDefault() {
        runCommandFormat = "%s";
    }
}
