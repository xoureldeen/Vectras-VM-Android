package com.epicstudios.vectras;

import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.animation.ValueAnimator;
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
import com.epicstudios.vectras.R;
import com.epicstudios.vectras.MainActivity;
import com.epicstudios.vectras.utils.UIUtils;

import java.io.File;

public class SplashActivity extends AppCompatActivity implements Runnable {
	public AlertDialog ad;
	public static SplashActivity activity;
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		activity = this;
		File baseDir = new File(Config.basefiledir);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_splash);
		if (!checkPermission()) {
			ad = new AlertDialog.Builder(this, R.style.MainDialogTheme).create();
			ad.setTitle("permissions");
			ad.setMessage("Vectras needs some permissions:\n-full storage access(shared folder - vectras bootable image '.vbi')");
			ad.setCanceledOnTouchOutside(false);
			ad.setButton(Dialog.BUTTON_POSITIVE, "Allow", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					try {
						ActivityCompat.requestPermissions(SplashActivity.this,
								new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
					} catch (Exception e) {
						UIUtils.toastLong(activity, e.toString());
						throw new RuntimeException(e);
					}
					return;
				}
			});

			ad.setButton(Dialog.BUTTON_NEGATIVE, "Learn More", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String gt = Config.vectrasRepo;
					Intent g = new Intent(Intent.ACTION_VIEW);
					g.setData(Uri.parse(gt));
					startActivity(g);
					finish();
					return;
				}
			});
			ad.show();
		} else {
			File sharedDir = new File(Config.sharedFolder);
			if (!sharedDir.exists()) {
				sharedDir.mkdirs();
			}
		    new Handler().postDelayed(this, 2000);
		}

		File sharedDir = new File(Config.sharedFolder);
		if (!sharedDir.exists()) {
			sharedDir.mkdirs();
		}
		File mainDir = new File(Config.maindirpath);
		if (!mainDir.exists()) {
			mainDir.mkdirs();
		}

	}
	private boolean checkPermission() {
		if (SDK_INT >= Build.VERSION_CODES.R) {
			return Environment.isExternalStorageManager();
		} else {
			int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
			int result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
			if (android.os.Build.VERSION.SDK_INT >= 30) {
    
    			int result2 = ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE);
    			return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED && result2 == PackageManager.PERMISSION_GRANTED;
    			
    		} else {
    		
    		    return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    			
    		}
		}
	}
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (android.os.Build.VERSION.SDK_INT >= 30) {

    			Intent intent = new Intent();
    			intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
    			Uri uri = Uri.fromParts("package", this.getPackageName(), null);
    			intent.setData(uri);
    			startActivity(intent);
    			ad.cancel();
				finish();
    		} else {
				new Handler().postDelayed(this, 2000);

			}
		} else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
			ActivityCompat.requestPermissions(SplashActivity.this,
					new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
		}
	}
	public static final String CREDENTIAL_SHARED_PREF = "settings_prefs";

	@Override
	public void run() {
		SharedPreferences prefs = getSharedPreferences(CREDENTIAL_SHARED_PREF, Context.MODE_PRIVATE);

		boolean isAccessed = prefs.getBoolean("isFirstLaunch", false);
		if (!isAccessed) {
			startActivity(new Intent(this, FirstActivity.class));
		} else {
			startActivity(new Intent(this, MainActivity.class));
		}
		finish();
	}
}
