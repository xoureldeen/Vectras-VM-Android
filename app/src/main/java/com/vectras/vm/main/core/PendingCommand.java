package com.vectras.vm.main.core;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.vectras.qemu.Config;
import com.vectras.vm.AppConfig;
import com.vectras.vm.R;
import com.vectras.vm.StartVM;
import com.vectras.vm.VMManager;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vterm.Terminal;

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
                        Terminal _vterm = new Terminal(activity);
                        _vterm.executeShellCommand2(command, false, activity);
                        Toast.makeText(activity, activity.getResources().getString(R.string.done), Toast.LENGTH_LONG).show();
                    }

                    command = "";
                } else {
                    Log.i(TAG, "Run VM...");

                    com.vectras.vm.StartVM.cdrompath = "";
                    Config.vmID = VMManager.idGenerator();
                    new Thread(() -> {
                        String env = StartVM.env(activity, command, "", true);
                        activity.runOnUiThread(() -> {
                            MainStartVM.startNow(activity, "Quick run", env, Config.vmID, null);
                            VMManager.lastQemuCommand = command;
                            command = "";
                        });
                    }).start();
                }
            }
        }
    }
}
