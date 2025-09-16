package com.vectras.vm.utils;

import android.app.NotificationManager;
import android.content.Context;

public class NotificationUtils {
    public static void clearAll(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }
}
