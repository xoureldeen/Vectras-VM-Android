package com.vectras.vm.home.core;

import static android.os.Build.VERSION.SDK_INT;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.appbar.AppBarLayout;
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
import com.vectras.vm.utils.NetworkUtils;
import com.vectras.vm.utils.ServiceUtils;

import java.io.File;

public class HomeStartVM {
    public static AlertDialog progressDialog;

    public static boolean skipIDEwithARM64DialogInStartVM = false;

    public static void startNow(
            Activity activity,
            String vmName,
            String env,
            String itemExtra,
            String itemPath,
            AppBarLayout appbar,
            LinearLayout extVncLayout
    ) {
        File romDir = new File(Config.getCacheDir() + "/" + Config.vmID);
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

        VMManager.lastQemuCommand = env;

        if (VMManager.isThisVMRunning(activity, itemExtra, itemPath)) {
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
                        () -> startNow(activity, vmName, env.replace("tcg,thread=multi", "tcg,thread=single"), itemExtra, itemPath, appbar, extVncLayout), null, null);
                return;
            }
        }

        if (MainSettingsManager.getArch(activity).equals("ARM64") && MainSettingsManager.getIfType(activity).equals("ide") && skipIDEwithARM64DialogInStartVM) {
            DialogUtils.twoDialog(activity, activity.getString(R.string.problem_has_been_detected), activity.getString(R.string.you_cannot_use_IDE_hard_drive_type_with_ARM64), activity.getString(R.string.continuetext), activity.getString(R.string.cancel), true, R.drawable.warning_48px, true,
                    () -> {
                        skipIDEwithARM64DialogInStartVM = true;
                        startNow(activity, vmName, env, itemExtra, itemPath, appbar, extVncLayout);
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

        showProgressDialog(activity, activity.getString(R.string.booting_up));
        Handler handler = new Handler();
        handler.postDelayed(
                () -> {
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


                    if (MainSettingsManager.getVmUi(activity).equals("VNC")) {
                        if (MainSettingsManager.getVncExternal(activity)) {
                            if (extVncLayout != null) extVncLayout.setVisibility(View.VISIBLE);
                            if (appbar != null) appbar.setExpanded(true);
                            progressDialog.dismiss();
                        } else {
                            Handler handler1 = new Handler();
                            handler1.postDelayed(
                                    () -> {
                                        MainVNCActivity.started = true;
                                        activity.startActivity(
                                                new Intent(
                                                        activity, MainVNCActivity.class));
                                        progressDialog.dismiss();
                                    },
                                    2000);
                        }
//                    } else if (MainSettingsManager.getVmUi(activity).equals("SPICE")) {
//                        //This feature is not available yet.
                    } else if (MainSettingsManager.getVmUi(activity).equals("X11")) {
                        Handler handler1 = new Handler();
                        handler1.postDelayed(
                                () -> {
                                    progressDialog.dismiss();
                                    DisplaySystem.launchX11(activity, false);
                                },
                                3000);
                    }

                    if (MainSettingsManager.getVncExternal(activity)) {
                        Config.currentVNCServervmID = Config.vmID;
                        DialogUtils.oneDialog(activity, activity.getString(R.string.vnc_server), activity.getString(R.string.running_vm_with_vnc_server_content) + " " + (Integer.parseInt(MainSettingsManager.getVncExternalDisplay(activity)) + 5900) + ".", activity.getString(R.string.ok), true, R.drawable.cast_24px, true, null, null);
                    }
                },
                2000);
        String[] params = env.split("\\s+");
        VectrasStatus.logInfo("Params:");
        Log.d("HomeStartVM", "Params:");
        for (int i = 0; i < params.length; i++) {
            VectrasStatus.logInfo(i + ": " + params[i]);
            Log.d("HomeStartVM", i + ": " + params[i]);
        }

    }

    public static void showProgressDialog(Activity activity, String _content) {
        View progressView = LayoutInflater.from(activity).inflate(R.layout.dialog_progress_style, null);
        TextView progress_text = progressView.findViewById(R.id.progress_text);
        progress_text.setText(_content);
        progressDialog = new MaterialAlertDialogBuilder(activity, R.style.CenteredDialogTheme)
                .setView(progressView)
                .setCancelable(false)
                .create();

        progressDialog.show();
    }
}
