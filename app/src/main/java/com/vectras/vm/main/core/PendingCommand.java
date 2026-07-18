package com.vectras.vm.main.core;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.vectras.qemu.Config;
import com.vectras.vm.AppConfig;
import com.vectras.vm.R;
import com.vectras.vm.StartVM;
import com.vectras.vm.VMManager;
import com.vectras.vm.manager.VmFileManager;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vterm.Terminal2;

public class PendingCommand {
    private static final String TAG = "PendingCommand";
    public static String command = "";

    public static void runNow(Activity activity) {
        Log.i(TAG, command);

        if (!command.isEmpty()) {
            if (!VMManager.isthiscommandsafe(command, activity)) {
                command = "";
                DialogUtils.oneDialog(
                        activity,
                        activity.getString(R.string.problem_has_been_detected),
                        activity.getString(R.string.harmful_command_was_detected) + " " + activity.getResources().getString(R.string.reason) + ": " + VMManager.latestUnsafeCommandReason,
                        activity.getString(R.string.ok),
                        true,
                        R.drawable.verified_user_24px,
                        true,
                        null,
                        null
                );
            } else {
                if (command.startsWith("qemu-img")) {
                    if (!VMManager.isthiscommandsafeimg(command, activity)) {
                        DialogUtils.oneDialog(activity,
                                activity.getString(R.string.problem_has_been_detected),
                                activity.getString(R.string.size_too_large_try_qcow2_format),
                                activity.getString(R.string.ok),
                                true,
                                R.drawable.warning_48px,
                                true,
                                null,
                                null
                        );
                    } else {
                        Terminal2 terminal2 = new Terminal2(activity);
                        terminal2.setShowProgressDialog(true);
                        terminal2.execute(command, new Terminal2.Terminal2Callback() {
                            @Override
                            public void onRunning(String command, String newLine) {
                                // Nothing to do.
                            }

                            @Override
                            public void onFinished(String command, String log, int status) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    if (status == terminal2.SUCCESS) {
                                        Toast.makeText(activity, activity.getResources().getString(R.string.done), Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(activity, activity.getResources().getString(R.string.an_error_occurred_while_creating_the_virtual_drive), Toast.LENGTH_LONG).show();
                                    }
                                });
                            }

                            @Override
                            public void onError(String command, Exception exception) {
                                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(activity, activity.getResources().getString(R.string.an_error_occurred_while_creating_the_virtual_drive), Toast.LENGTH_LONG).show());
                            }
                        });
                    }

                    command = "";
                } else {
                    Log.i(TAG, "Run VM...");

                    Config.vmID = "quick_run_" + VMManager.idGenerator();
                    new Thread(() -> {
                        VmFileManager.removeTemp(activity, Config.vmID);

                        String env = StartVM.env(activity, command, "", true);
                        FileUtils.createDirectory(AppConfig.vmFolder + Config.vmID);
                        activity.runOnUiThread(() -> {
                            MainStartVM.startNow(activity, "Quick run", env, Config.vmID, null, null);
                            VMManager.lastQemuCommand = command;
                            command = "";
                        });
                    }).start();
                }
            }
        }
    }

    public static String vmId;
    public static String vmConfig;
    public static String paramsNotebookConfig;
    public static boolean forceCreate;
}
