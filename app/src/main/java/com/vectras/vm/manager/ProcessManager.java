package com.vectras.vm.manager;

import android.content.Context;

import com.vectras.vterm.Terminal;

public class ProcessManager {
    public static boolean isQemuRunning(Context context) {
        return Terminal.executeShellCommandWithResult("ps -e", context).contains("qemu-system-");
    }
}
