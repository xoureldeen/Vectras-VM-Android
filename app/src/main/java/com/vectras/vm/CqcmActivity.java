package com.vectras.vm;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;

public class CqcmActivity extends AppCompatActivity {

    private Intent gotoActivity = new Intent();
    private Intent openURL = new Intent();
    private String contentJson = "";
    private String contentJsonNow = "";
    private Button buttonallow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VectrasApp.prepareDataForAppConfig(this);
        Log.d("CqcmActivity", "Start creating...");
        if(!VectrasApp.checkpermissionsgranted(this,false)) {
            setContentView(R.layout.activity_cqcm);
            buttonallow = findViewById(R.id.buttonallow);
            buttonallow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                        Toast.makeText(getApplicationContext(), "Find and allow access to storage in Settings.", Toast.LENGTH_LONG).show();
                    } else {
                        ActivityCompat.requestPermissions(CqcmActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
                    }
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(VectrasApp.checkpermissionsgranted(this,false)) {
            File vDir = new File(AppConfig.maindirpath);
            if (!vDir.exists()) {
                vDir.mkdirs();
            }
            if (!VectrasApp.isFileExists(AppConfig.romsdatajson)) {
                VectrasApp.writeToFile(AppConfig.maindirpath, "roms-data.json", "[]");
            }
            if (getIntent().hasExtra("content")) {
                contentJson = VectrasApp.readFile(AppConfig.romsdatajson);
                if (contentJson.isEmpty()) {
                    contentJson = "[]";
                }
                if (VectrasApp.checkJSONIsNormalFromString(contentJson)) {
                    if (contentJson.contains("}")) {
                        contentJsonNow = contentJson.replaceAll("]", "," + getIntent().getStringExtra("content"));
                    } else {
                        contentJsonNow = contentJson.replaceAll("]", getIntent().getStringExtra("content"));
                    }

                    if (VectrasApp.checkJSONIsNormalFromString(contentJsonNow)) {
                        VectrasApp.writeToFile(AppConfig.maindirpath, "roms-data.json", contentJsonNow);
                    } else {
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.cannot_create_VM_at_this_time), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.cannot_create_VM_at_this_time), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_data), Toast.LENGTH_LONG).show();
            }
            if(!MainActivity.isActivate) {
                gotoActivity.setClass(getApplicationContext(), SplashActivity.class);
                startActivity(gotoActivity);
            } else {
                openURL.setAction(Intent.ACTION_VIEW);
                openURL.setData(Uri.parse("android-app://com.vectras.vm"));
                startActivity(openURL);
            }
            finish();
        }
    }
}