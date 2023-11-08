package com.epicstudios.vectras;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import androidx.core.app.TaskStackBuilder;
import com.epicstudios.vectras.Config;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.epicstudios.vectras.R;
import com.epicstudios.vectras.jni.VMExecutor;
import com.epicstudios.vectras.logger.VectrasStatus;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class VectrasService extends Service {

	private static final String TAG = "VectrasService";
	private static WifiLock mWifiLock;
	public static VectrasService service;
	private static WakeLock mWakeLock;
	public static final int notifID = 1000;
	private NotificationCompat.Builder builder;
	private Notification mNotification;

	@Override
	public IBinder onBind(Intent arg0) {

		return null;
	}

	public static VMExecutor executor;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String action = intent.getAction();
		final Bundle b = intent.getExtras();
		final int ui = b.getInt("ui", 0);

		if (action.equals(Config.ACTION_START)) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					//fetching notifications from server
					//if there is notifications then call this method
					setUpAsForeground("VM Running in Background");
				}
			}).start();
			setUpAsForeground("VM Running in Background");
			VectrasStatus.logInfo(String.format("VM Running in Background"));

			startLogging();

			Log.v(TAG, "Starting the VM");
			VectrasStatus.logInfo(String.format("Starting the VM"));
			executor.loadNativeLibs();

			setupLocks();

			Thread t = new Thread(new Runnable() {
				public void run() {

					String res = executor.startvm();
					Log.d(TAG, res);
					if (VectrasSDLActivity.activity != null)
						VectrasSDLActivity.activity.finish();

					releaseLocks();
					stopService();
					MainActivity.activity.cleanup();
					//                    NotificationManager notificationManager = (NotificationManager) service.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
					//                    notificationManager.cancelAll();
					//service.stopSelf();
					// VectrasActivity.sendHandlerMessage(VectrasActivity.OShandler,
					// Const.VM_STOPPED);

				}
			});
			t.start();

		}

		// Don't restart if killed
		return START_NOT_STICKY;
	}

	private void setUpAsForeground(String text) {
		
		Intent notificationIntent = new Intent(this, VectrasSDLActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
		
		Intent stopnotificationIntent = new Intent(this, VectrasService.class);
		stopnotificationIntent.setAction(Config.ACTION_STOP);
		PendingIntent Intent = PendingIntent.getService(this, 0, stopnotificationIntent, PendingIntent.FLAG_IMMUTABLE);
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Config.notificationChannelID)
		.setSmallIcon(R.mipmap.ic_launcher)
		.setContentTitle(getString(R.string.app_name))
		.setContentText(text)
		.setPriority(NotificationCompat.PRIORITY_HIGH)
		.setColor(Color.BLUE)
		.setDefaults(Notification.DEFAULT_ALL)
		.setFullScreenIntent(pendingIntent, true)
		.setAutoCancel(true)
		.setContentIntent(pendingIntent)
		.addAction(android.R.drawable.ic_media_pause, "Stop VM", Intent);
		
		
		Notification notification = builder.build();
		
		if (Build.VERSION.SDK_INT >= 26) {
			NotificationChannel channel = new NotificationChannel(Config.notificationChannelID, Config.notificationChannelID, NotificationManager.IMPORTANCE_DEFAULT);
			channel.setDescription(getString(R.string.app_name));
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.createNotificationChannel(channel);
		}
		startForeground(1000, notification);
	}
	
	private void stopForegroundService() {
		MainActivity.vmexecutor.stopvm(0);
		stopForeground(true);
		stopSelf();
	}

	public static StringBuilder log = null;

	private void startLogging() {

		Thread t = new Thread(new Runnable() {
			public void run() {

				FileOutputStream os = null;
				File logFile = new File(Config.logFilePath);
				if (logFile.exists()) {
					logFile.delete();
				}
				try {
					Runtime.getRuntime().exec("logcat -c");
					Process process = Runtime.getRuntime().exec("logcat v main");
					os = new FileOutputStream(logFile);
					BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

					log = new StringBuilder();
					String line = "";
					while ((line = bufferedReader.readLine()) != null) {
						log.append(line + "\n");
						os.write((line + "\n").getBytes("UTF-8"));
						os.flush();
					}
				} catch (IOException e) {

				} finally {
					try {
						os.flush();
						os.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

			}
		});
		t.start();
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "debug: Creating " + TAG);
		VectrasStatus.logInfo(String.format("debug: Creating " + TAG));
		service = this;

	}

	private void setupLocks() {

		mWifiLock = ((WifiManager) service.getApplicationContext().getSystemService(Context.WIFI_SERVICE))
				.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "WIFI_VECTRAS");
		mWifiLock.setReferenceCounted(false);

		PowerManager pm = (PowerManager) service.getApplicationContext().getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WAKELOCK_VECTRAS");
		mWakeLock.setReferenceCounted(false);
	}

	private static void releaseLocks() {

		if (mWifiLock != null && mWifiLock.isHeld()) {
			Log.d(TAG, "Release Wifi lock...");
			mWifiLock.release();
		}

		if (mWakeLock != null && mWakeLock.isHeld()) {
			Log.d(TAG, "Release Wake lock...");
			mWakeLock.release();
		}

	}

	public static void stopService() {
		service.stopForeground(true);
		service.stopSelf();
	}

}
