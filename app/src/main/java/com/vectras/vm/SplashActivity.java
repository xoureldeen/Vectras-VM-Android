package com.vectras.vm;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.vectras.qemu.MainSettingsManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class SplashActivity extends AppCompatActivity implements Runnable {
    public static SplashActivity activity;
    private final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        activity = this;
        setContentView(R.layout.activity_splash);
        setupFolders();
        SharedPreferences prefs = getSharedPreferences(CREDENTIAL_SHARED_PREF, Context.MODE_PRIVATE);

        try {
            if (checkConnection(activity))
                new DownloadFileAsync().execute(AppConfig.romsJson(activity));
            else
                new Handler().postDelayed(activity, 3000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }/*
        boolean isAccessed = prefs.getBoolean("isFirstLaunch", false);
        if (isAccessed && !checkConnection(activity)) {
            new Handler().postDelayed(this, 3000);
        } else {
        }
*/
        if (!checkPermission())
            requestPermission();
        MainSettingsManager.setOrientationSetting(activity, 1);

        setupFiles();
    }

    public void setupFiles() {
        String filesDir = activity.getFilesDir().getAbsolutePath();
        String nativeLibDir = activity.getApplicationInfo().nativeLibraryDir;

        File tmpDir = new File(filesDir + "/tmp/xdg");
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }

        File vDir = new File(com.vectras.vm.AppConfig.maindirpath);
        if (!vDir.exists()) {
            vDir.mkdirs();
        }

        File distroDir = new File(filesDir + "/distro");
        if (!distroDir.exists()) {
            distroDir.mkdirs();
        }
        File binDir = new File(distroDir + "/bin");
        if (!binDir.exists()) {
            String CHANNEL_ID = "vectras";
            String[] cmdline = {"tar", "xf", nativeLibDir + "/libbootstrap.so", "-C", filesDir + "/distro"};
            try {
                Runtime.getRuntime().exec(cmdline).waitFor();
            } catch (IOException | InterruptedException e) {
                // Prepare an intent that does something when the user clicks the notification.
                Intent intent = new Intent(activity, activity.getClass()); // Replace TargetActivity with your activity that you want to start.
                PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                // Build the notification to show in the notification tray.
                Notification notification = new NotificationCompat.Builder(activity, CHANNEL_ID) // Replace YOUR_CHANNEL_ID with your actual channel ID
                        .setContentTitle("Extract Bootstrap")
                        .setContentText("Error during file extraction.")
                        .setSmallIcon(com.vectras.vterm.R.drawable.cursor) // Replace this with your notification icon.
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .build();

                // Use the Notification Manager to show the notification.
                NotificationManager notificationManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);

                // It's a good practice to create a notification channel.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID,
                            "Extraction Status",
                            NotificationManager.IMPORTANCE_DEFAULT
                    );
                    notificationManager.createNotificationChannel(channel);
                }

                // Show the notification. You can use a unique ID (e.g., 0) for each notification if you want to show multiple ones.
                notificationManager.notify(0, notification);
            }

        }

        com.vectras.qemu.utils.FileInstaller.installFiles(activity, false);
    }

    public void onStart() {
        super.onStart();
        if (MainSettingsManager.getModeNight(activity)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            VectrasApp.getApp().setTheme(R.style.AppTheme);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            VectrasApp.getApp().setTheme(R.style.AppTheme);
        }
    }

    public static String[] storage_permissions = {
            WRITE_EXTERNAL_STORAGE,
            READ_EXTERNAL_STORAGE
    };

    public String getPath(Uri uri) {
        return com.vectras.vm.utils.FileUtils.getPath(this, uri);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                if (checkPermission()) {
                } else {
                    requestPermission();
                    Toast.makeText(this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    /**
     * CHECK WHETHER INTERNET CONNECTION IS AVAILABLE OR NOT
     */
    public boolean checkConnection(Context context) {
        final ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connMgr != null) {
            NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();

            if (activeNetworkInfo != null) { // connected to the internet
                // connected to the mobile provider's data plan
                if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    // connected to wifi
                    return true;
                } else
                    return activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
            }
        }
        return false;
    }

    class DownloadFileAsync extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... aurl) {
            int count;

            try {
                URL url = new URL(aurl[0]);
                URLConnection conexion = url.openConnection();
                conexion.connect();

                int lenghtOfFile = conexion.getContentLength();
                Log.d(TAG, "Lenght of file: " + lenghtOfFile);
                String fileName = "roms.json";
                InputStream input = new BufferedInputStream(url.openStream());
                OutputStream output = new FileOutputStream(getExternalFilesDir("data") + fileName);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();
            } catch (Exception e) {
            }
            return null;

        }

        protected void onProgressUpdate(String... progress) {
            Log.d(TAG, progress[0]);
        }

        @Override
        protected void onPostExecute(String unused) {
            new Handler().postDelayed(activity, 3000);
        }
    }

    private boolean checkPermission() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int result = ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE);
            int result1 = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                permissions(),
                1);
    }

    public static String[] permissions() {
        String[] p;
        p = storage_permissions;
        return p;
    }

    private void copyAssetFile(String assetFileName, String destinationDirectory) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                AssetManager assetManager = getAssets();
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = assetManager.open(assetFileName);
                    File outFile = new File(destinationDirectory);
                    out = new FileOutputStream(outFile);
                    copyFile(in, out);
                } catch (IOException e) {
                    Log.e("tag", "Failed to copy asset file: " + assetFileName, e);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            // NOOP
                        }
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            // NOOP
                        }
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new Handler().postDelayed(this, 3000);
                        }
                    });
                }
            }
        }).start();
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static void setupFolders() {
        try {
            StartVM.cache = activity.getCacheDir().getAbsolutePath();
        } catch (Exception ignored) {

        }
    }

    public static final String CREDENTIAL_SHARED_PREF = "settings_prefs";

    @Override
    public void run() {
        String filesDir = activity.getFilesDir().getAbsolutePath();
        SharedPreferences prefs = getSharedPreferences(CREDENTIAL_SHARED_PREF, Context.MODE_PRIVATE);
        if (new File(filesDir, "/distro/usr/local/bin/qemu-system-x86_64").exists())
            startActivity(new Intent(this, MainActivity.class));
        else
            startActivity(new Intent(this, SetupQemuActivity.class));
        finish();
    }
}
