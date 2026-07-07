package com.vectras.vm;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.vectras.vm.utils.ClipboardUltils;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vterm.Terminal;
import com.vectras.vterm.Terminal2;

import java.util.Objects;

public class MainService extends Service {
    public static String CHANNEL_ID = "Vectras VM Service";
    private final int NOTIFICATION_ID = 1;
    private final String MACHINE_NAME = "Vectras VM";
    public static String env = null;
    private static String TAG = "MainService";
    public static MainService service;
    public static Context activityContext;

    private final String STOP_ACTION = "STOP";

    @Override
    public void onCreate() {
        super.onCreate();
        service = this;
        createNotificationChannel();

        Intent stopSelf = new Intent(this, MainService.class);
        stopSelf.setAction(STOP_ACTION);
        PendingIntent pStopSelf = PendingIntent.getService(
                this, 0, stopSelf, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(MACHINE_NAME + " running in background.")
                .setSmallIcon(R.drawable.ic_vectras_vm_48)
                .addAction(R.drawable.close_24px, getString(R.string.stop), pStopSelf)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        if (env != null) {
            if (service != null) {
                startCommand(env, activityContext);
            }
        } else {
            Log.e(TAG, "env is null");
        }
    }

    public static void stopService() {
        new Thread(() -> {
            if (service != null) {
                service.stopForeground(true);
                service.stopSelf();
                VMManager.killallqemuprocesses(activityContext);
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && Objects.equals(intent.getAction(), STOP_ACTION)) {
            VMManager.killallqemuprocesses(this);
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_ID,
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public static void startCommand(String env, Context context) {
        Terminal2 terminal2 = new Terminal2(activityContext);
        terminal2.setDefaultShellBash();
        terminal2.setStartup("export XDG_RUNTIME_DIR=/tmp && unset PULSE_SERVER");
        terminal2.execute(env, new Terminal2.Terminal2Callback() {
            @Override
            public void onRunning(String command, String newLine) {
                // Nothing to do.
            }

            @Override
            public void onFinished(String command, String log, int status) {
                if (context instanceof Activity activity) {
                    if (activity.isFinishing() || activity.isDestroyed()) {
                        return;
                    }
                } else {
                    Log.e(TAG, "context is not an Activity");
                    return;
                }

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (VMManager.isExecutedCommandError(command, log, context))
                        return;

                    DialogUtils.twoDialog(context, "Execution Result", log, context.getString(R.string.copy), context.getString(R.string.close), true, R.drawable.round_terminal_24, true,
                            () -> ClipboardUltils.copyToClipboard(context, log), null, null);
                });
            }

            @Override
            public void onError(String command, Exception exception) {
                if (context instanceof Activity activity) {
                    if (activity.isFinishing() || activity.isDestroyed()) {
                        return;
                    }
                } else {
                    Log.e(TAG, "context is not an Activity");
                    return;
                }

                new Handler(Looper.getMainLooper()).post(() -> DialogUtils.twoDialog(context, "Execution Result", exception.getMessage(), context.getString(R.string.copy), context.getString(R.string.close), true, R.drawable.round_terminal_24, true,
                        () -> ClipboardUltils.copyToClipboard(context, exception.getMessage()), null, null));
            }
        });
    }
}