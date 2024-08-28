package com.vectras.vm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.vectras.qemu.MainVNCActivity;
import com.vectras.vterm.Terminal;

import java.io.File;
import java.util.Objects;

public class MainService extends Service {
    public static String CHANNEL_ID = "Vectras VM Service";
    private static final int NOTIFICATION_ID = 1;
    private static final String MACHINE_NAME = "Vectras VM";
    public static String env = null;
    private String TAG = "MainService";
    public static MainService service;

    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

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
                .setContentTitle("Vectras VM")
                .setContentText(MACHINE_NAME + " running in background.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .addAction(R.drawable.round_logout_24, "Stop", pStopSelf)
                .build();


        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Vectras::MyWakeLockTag");
        wakeLock.acquire();

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "Vectras::MyWifiLockTag");
        wifiLock.acquire();

        if (env != null) {
            if (service != null) {
                Terminal vterm = new Terminal(this);
                vterm.executeShellCommand("awesome &", false, MainActivity.activity);
                //vterm.executeShellCommand("pulseaudio --system --disallow-exit --disallow-module-loading --daemonize --log-level=debug --log-time=1");
                vterm.executeShellCommand(env, true, MainActivity.activity);
            }
        } else
            Log.e(TAG, "env is null");

        startForeground(NOTIFICATION_ID, notification);
    }

    public static void stopService() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                if (service != null) {
                    service.stopForeground(true);
                    service.stopSelf();

                    //TODO: Not Work
                    Terminal.killQemuProcess();
                }

            }
        });
        t.setName("StartVM");
        t.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();

            //TODO: Not Work
            Terminal.killQemuProcess();

            return START_NOT_STICKY;
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Release the WakeLock if it is held
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        // Release the WifiLock if it is held
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_ID,
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}