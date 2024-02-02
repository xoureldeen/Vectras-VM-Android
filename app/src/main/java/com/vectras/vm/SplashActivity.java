package com.vectras.vm;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.*;
import android.content.pm.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.webkit.URLUtil;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.utils.RamInfo;
import com.vectras.vm.Fragment.HomeFragment;
import com.vectras.vm.MainRoms.AdapterMainRoms;
import com.vectras.vm.MainRoms.DataMainRoms;
import com.vectras.vm.ui.login.LoginActivity;
import com.vectras.vm.utils.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class SplashActivity extends AppCompatActivity implements Runnable {
    public AlertDialog ad;
    public static SplashActivity activity;
    private FirebaseAuth mAuth;
    private final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        activity = this;
        File baseDir = new File(AppConfig.basefiledir);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash);

        SharedPreferences prefs = getSharedPreferences(CREDENTIAL_SHARED_PREF, Context.MODE_PRIVATE);

        boolean isAccessed = prefs.getBoolean("isFirstLaunch", false);
        if (isAccessed && !checkConnection(activity)) {
            new Handler().postDelayed(this, 3000);
        } else {
            try {
                new DownloadFileAsync().execute(AppConfig.romsJson(activity));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        File sharedDir = new File(AppConfig.sharedFolder);
        if (!sharedDir.exists()) {
            sharedDir.mkdirs();
        }
        File mainDir = new File(AppConfig.maindirpath);
        if (!mainDir.exists()) {
            mainDir.mkdirs();
        }
        File iconsDir = new File(AppConfig.maindirpath + "/icons/");
        if (!iconsDir.exists()) {
            iconsDir.mkdirs();
        }
        File cvbiDir = new File(AppConfig.maindirpath + "/cvbi/");
        if (!cvbiDir.exists()) {
            cvbiDir.mkdirs();
        }
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        RamInfo.activity = activity;

        MainSettingsManager.setOrientationSetting(activity, 1);
    }

    public static final String CREDENTIAL_SHARED_PREF = "settings_prefs";

    @Override
    public void run() {
        SharedPreferences prefs = getSharedPreferences(CREDENTIAL_SHARED_PREF, Context.MODE_PRIVATE);

        boolean isAccessed = prefs.getBoolean("isFirstLaunch", false);
        if (!isAccessed) {
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            if (FirebaseAuth.getInstance().getCurrentUser() != null)
                startActivity(new Intent(this, MainActivity.class));
            else
                startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
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


    public static final int DIALOG_DOWNLOAD_PROGRESS = 0;
    private ProgressDialog mProgressDialog;

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_DOWNLOAD_PROGRESS:
                mProgressDialog = new ProgressDialog(this, R.style.MainDialogTheme);
                mProgressDialog.setMessage("loading app data..");
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
                return mProgressDialog;
            default:
                return null;
        }
    }

    class DownloadFileAsync extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(DIALOG_DOWNLOAD_PROGRESS);
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
                OutputStream output = new FileOutputStream(getExternalFilesDir("data")+fileName);

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
            mProgressDialog.setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected void onPostExecute(String unused) {
            dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
            new Handler().postDelayed(activity, 3000);
        }
    }
}
