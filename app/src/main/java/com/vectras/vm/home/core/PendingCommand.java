package com.vectras.vm.home.core;

import android.app.Activity;
import android.widget.Toast;

import com.vectras.vm.AppConfig;
import com.vectras.vm.R;
import com.vectras.vm.StartVM;
import com.vectras.vm.VMManager;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vterm.Terminal;

public class PendingCommand {
    public static void runNow(Activity activity) {
        if (!AppConfig.pendingCommand.isEmpty()) {
            if (!VMManager.isthiscommandsafe(AppConfig.pendingCommand, activity)) {
                AppConfig.pendingCommand = "";
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
                if (AppConfig.pendingCommand.startsWith("qemu-img")) {
                    if (!VMManager.isthiscommandsafeimg(AppConfig.pendingCommand, activity)) {
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
                        _vterm.executeShellCommand2(AppConfig.pendingCommand, false, activity);
                        Toast.makeText(activity, activity.getResources().getString(R.string.done), Toast.LENGTH_LONG).show();
                    }
                } else {
                    com.vectras.vm.StartVM.cdrompath = "";
                    String env = StartVM.env(activity, AppConfig.pendingCommand, "", "1");
                    HomeStartVM.startNow(activity, "Quick run", env, AppConfig.pendingCommand, "", null, null);
                    VMManager.lastQemuCommand = AppConfig.pendingCommand;
                }
            }
            AppConfig.pendingCommand = "";
        }
    }
}
