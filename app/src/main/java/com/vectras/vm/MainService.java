package com.vectras.vm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.vectras.vterm.Terminal;

public class MainService extends Service {
    public static String CHANNEL_ID = "Vectras VM Service";
    private static final int NOTIFICATION_ID = 1;
    private static final String MACHINE_NAME = "Vectras VM";
    public static String env = null;
    private String TAG = "MainService";
    public static MainService service;
    public static Context activityContext;

    @Override
    public void onCreate() {
        super.onCreate();
        service = this;
        createNotificationChannel();

        Intent stopSelf = new Intent(this, MainService.class);
        stopSelf.setAction("STOP");
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
                Terminal vterm = new Terminal(activityContext);
                vterm.executeShellCommand2(env, true, activityContext);
            }
        } else {
            Log.e(TAG, "env is null");
        }
    }

    public static void stopService() {
        Thread t = new Thread(() -> {
            if (service != null) {
                service.stopForeground(true);
                service.stopSelf();
                VMManager.killallqemuprocesses(activityContext);
            }
        });
        t.setName("HomeStartVM");
        t.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
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

    public static void startCommand(String _env, Context _context) {
        Terminal vterm = new Terminal(_context);
        vterm.executeShellCommand2(_env, true, _context);
    }
}