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
import java.util.Objects;

public class CqcmActivity extends AppCompatActivity {

    private Intent gotoActivity = new Intent();
    private Intent openURL = new Intent();
    private String contentJson = "";
    private String contentJsonNow = "";
    private Button buttonallow;
    private String vmID = VMManager.idGenerator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VectrasApp.prepareDataForAppConfig(this);
        if(!VectrasApp.checkpermissionsgranted(this,false)) {
            Log.i("CqcmActivity", "Creating layout...");
            setContentView(R.layout.activity_cqcm);
            buttonallow = findViewById(R.id.buttonallow);
            buttonallow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Log.i("CqcmActivity", "Start granting access to storage in Settings.");
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.find_and_allow_access_to_storage_in_settings), Toast.LENGTH_LONG).show();
                    } else {
                        Log.i("CqcmActivity", "Start granting access to storage.");
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
            Log.i("CqcmActivity", "Access to storage has been granted.");
            File vDir = new File(AppConfig.maindirpath);
            if (!vDir.exists()) {
                Log.w("CqcmActivity", "The main directory has not been created.");
                boolean wasSuccessful = vDir.mkdirs();
                if (!wasSuccessful) {
                    Log.e("CqcmActivity", "Main directory was created unsuccessfully.");
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.cannot_create_VM_at_this_time), Toast.LENGTH_LONG).show();
                    return;
                } else {
                    Log.i("CqcmActivity", "Main directory created successfully");
                }
            }
            if (!VectrasApp.isFileExists(AppConfig.romsdatajson)) {
                Log.w("CqcmActivity", "roms-data.json file does not exist.");
                VectrasApp.writeToFile(AppConfig.maindirpath, "roms-data.json", "[]");
                Log.i("CqcmActivity", "Successfully created roms-data.json file.");
            }
            if (getIntent().hasExtra("content")) {
                Log.i("CqcmActivity", "Read roms-data.json file.");
                contentJson = VectrasApp.readFile(AppConfig.romsdatajson);
                Log.i("CqcmActivity", "Successfully read roms-data.json file.");
                if (contentJson.isEmpty()) {
                    Log.w("CqcmActivity", "The roms-data.json file is empty.");
                    contentJson = "[]";
                    Log.i("CqcmActivity", "Added raw data to roms-data.json file.");
                }
                if (VectrasApp.checkJSONIsNormalFromString(contentJson)) {
                    Log.i("CqcmActivity", "Checked roms-data.json and no errors.");
                    if (contentJson.contains("}")) {
                        Log.i("CqcmActivity", "The roms-data.json file contains data about the VMs.");
                        contentJsonNow = contentJson.replaceAll("]", "," + getIntent().getStringExtra("content").replaceAll("\\}\\]", ",\"vmID\":\"" + vmID + "\"}]"));
                    } else {
                        Log.i("CqcmActivity", "The roms-data.json file does not contain data about the VMs.");
                        contentJsonNow = contentJson.replaceAll("]", Objects.requireNonNull(getIntent().getStringExtra("content")).replaceAll("\\}\\]", ",\"vmID\":\"" + vmID + "\"}]"));
                    }
                    Log.i("CqcmActivity", "Double check the data has been edited before adding.");
                    if (VectrasApp.checkJSONIsNormalFromString(contentJsonNow)) {
                        VectrasApp.writeToFile(AppConfig.maindirpath, "roms-data.json", contentJsonNow);
                        Log.i("CqcmActivity", "Successfully added new VM to roms-data.json file.");
                        // "\}\]" = Fix java.util.regex.PatternSyntaxException: Syntax error in regexp pattern near index 1
                        VectrasApp.writeToFile(AppConfig.maindirpath + "roms/" + vmID, "rom-data.json", Objects.requireNonNull(getIntent().getStringExtra("content")).replaceAll("\\}\\]", ",\"vmID\":\"" + vmID + "\"}"));
                        Log.i("CqcmActivity", "Successfully created rom-data.json file.");

                        VectrasApp.writeToFile(AppConfig.maindirpath + "roms/" + vmID, "vmID.txt", vmID);
                        Log.i("CqcmActivity", "Successfully created ID for new VM.");
                    } else {
                        Log.e("CqcmActivity", "Cannot add VM to roms-data.json as it will corrupt the roms-data.json file after adding.");
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.cannot_create_VM_at_this_time), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e("CqcmActivity", "Unable to add VM to rom-data.json because roms-data.json file has corrupted data from before and needs action before adding VM.");
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.cannot_create_VM_at_this_time), Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e("CqcmActivity", "Unable to add VM to rom-data.json because the required data was not provided. This happens because this activity was not opened properly.");
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_data), Toast.LENGTH_LONG).show();
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
            Log.i("CqcmActivity", "Finished.");
            finish();
        } else {
            Log.w("CqcmActivity", "Access to storage not granted.");
        }
    }
}