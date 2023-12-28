package com.vectras.vm;

import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.*;
import android.graphics.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.vectras.qemu.MainActivityCommon;
import com.vectras.qemu.utils.RamInfo;
import com.vectras.vm.R;
import com.vectras.vm.MainActivity;
import com.vectras.vm.ui.login.LoginActivity;
import com.vectras.vm.utils.UIUtils;

import java.io.File;

public class SplashActivity extends AppCompatActivity implements Runnable {
    public AlertDialog ad;
    public static SplashActivity activity;
    private FirebaseAuth mAuth;

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

        new Handler().postDelayed(this, 3000);
        File sharedDir = new File(AppConfig.sharedFolder);
        if (!sharedDir.exists()) {
            sharedDir.mkdirs();
        }
        File mainDir = new File(AppConfig.maindirpath);
        if (!mainDir.exists()) {
            mainDir.mkdirs();
        }
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        RamInfo.activity = activity;
    }

    public static final String CREDENTIAL_SHARED_PREF = "settings_prefs";

    @Override
    public void run() {
        SharedPreferences prefs = getSharedPreferences(CREDENTIAL_SHARED_PREF, Context.MODE_PRIVATE);

        boolean isAccessed = prefs.getBoolean("isFirstLaunch", false);
        if (!isAccessed) {
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }
}
