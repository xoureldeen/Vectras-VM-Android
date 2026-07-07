package com.vectras.vm.manager;

import android.content.Context;

import com.vectras.vterm.Terminal2;

public class ProcessManager {
    public static boolean isQemuRunning(Context context) {
        return new Terminal2(context).executeOnThisThread("ps -e").contains("qemu-system-");
    }
}
