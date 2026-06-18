package com.vectras.vm.manager;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.vectras.vm.MainService;
import com.vectras.vterm.Terminal;

public class VmServiceManager {
    public static void stopService(Context context) {
        new Thread(() -> {
            if (!ProcessManager.isQemuRunning(context)) {
                new Handler(Looper.getMainLooper()).post(MainService::stopService);
            }
        }).start();
    }
}
