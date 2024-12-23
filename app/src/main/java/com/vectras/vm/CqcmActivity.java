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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.HashMap;
import java.util.Objects;

public class CqcmActivity extends AppCompatActivity {

    private Intent gotoActivity = new Intent();
    private Intent openURL = new Intent();
    private Button buttonallow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VectrasApp.prepareDataForAppConfig(this);
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
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.find_and_allow_access_to_storage_in_settings), Toast.LENGTH_LONG).show();
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
        Log.i("CqcmActivity", "Checking access to storage...");
        if(VectrasApp.checkpermissionsgranted(this,false)) {
            if (getIntent().hasExtra("command")) {
                runCommand(getIntent().getStringExtra("command"));
            } else {
                startAdd();
            }
        }

    }
    private void startAdd() {
        HashMap<String, Object> mapForCreateNewVM = new HashMap<>();
        String _map;
        String imgName = "";
        String imgIcon = "";
        String imgPath = "";
        String imgArch = "";
        String imgCdrom = "";
        String imgExtra = "";
        String vmID = VMManager.idGenerator();

        if (!VectrasApp.isFileExists(AppConfig.romsdatajson)) {
            VectrasApp.writeToFile(AppConfig.maindirpath, "roms-data.json", "[]");
        }

        if (VectrasApp.checkJSONIsNormal(AppConfig.romsdatajson)) {
            if (getIntent().hasExtra("content")) {
                if (Objects.requireNonNull(getIntent().getStringExtra("content")).endsWith("}]")) {
                    _map = Objects.requireNonNull(getIntent().getStringExtra("content")).substring((int) 0, (int)(Objects.requireNonNull(getIntent().getStringExtra("content")).length() - 1));
                } else {
                    _map = Objects.requireNonNull(getIntent().getStringExtra("content"));
                }
                if (VectrasApp.checkJSONMapIsNormalFromString(_map)) {
                    mapForCreateNewVM = new Gson().fromJson(_map, new TypeToken<HashMap<String, Object>>(){}.getType());
                    if (mapForCreateNewVM.containsKey("imgName")) {
                        imgName = Objects.requireNonNull(mapForCreateNewVM.get("imgName")).toString();
                    }
                    if (mapForCreateNewVM.containsKey("imgIcon")) {
                        imgIcon = Objects.requireNonNull(mapForCreateNewVM.get("imgIcon")).toString();
                    }
                    if (mapForCreateNewVM.containsKey("imgPath")) {
                        imgPath = Objects.requireNonNull(mapForCreateNewVM.get("imgPath")).toString();
                    }
                    if (mapForCreateNewVM.containsKey("imgArch")) {
                        imgArch = Objects.requireNonNull(mapForCreateNewVM.get("imgArch")).toString();
                    }
                    if (mapForCreateNewVM.containsKey("imgCdrom")) {
                        imgCdrom = Objects.requireNonNull(mapForCreateNewVM.get("imgCdrom")).toString();
                    }
                    if (mapForCreateNewVM.containsKey("imgExtra")) {
                        imgExtra = Objects.requireNonNull(mapForCreateNewVM.get("imgExtra")).toString();
                    }
                    VMManager.createNewVM(imgName, imgIcon, imgPath, imgArch, imgCdrom, imgExtra, vmID);
                } else {
                    Toast.makeText(getApplicationContext(), "The data for the new virtual machine is corrupted and cannot be created.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "There is no data about the new virtual machine to create.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "The virtual machine list data is corrupted and new virtual machines cannot be added right now.", Toast.LENGTH_LONG).show();
        }
        if(!MainActivity.isActivate) {
            Log.i("CqcmActivity", "Vectras VM is not opening.");
            gotoActivity.setClass(getApplicationContext(), SplashActivity.class);
            startActivity(gotoActivity);
            Log.i("CqcmActivity", "Opened SplashActivity");
        } else {
            Log.i("CqcmActivity", "Vectras VM is opening.");
            openURL.setAction(Intent.ACTION_VIEW);
            openURL.setData(Uri.parse("android-app://com.vectras.vm"));
            startActivity(openURL);
            Log.i("CqcmActivity", "Opened Vectras VM using URL.");
        }
        finish();
    }

    private void runCommand(String _command) {
        VectrasApp.prepareDataForAppConfig(CqcmActivity.this);
        AppConfig.pendingCommand = _command;

        if(!MainActivity.isActivate) {
            Log.i("CqcmActivity", "Vectras VM is not opening.");
            gotoActivity.setClass(getApplicationContext(), SplashActivity.class);
            startActivity(gotoActivity);
            Log.i("CqcmActivity", "Opened SplashActivity");
        } else {
            Log.i("CqcmActivity", "Vectras VM is opening.");
            openURL.setAction(Intent.ACTION_VIEW);
            openURL.setData(Uri.parse("android-app://com.vectras.vm"));
            startActivity(openURL);
            Log.i("CqcmActivity", "Opened Vectras VM using URL.");
        }
        finish();
    }
}