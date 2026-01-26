package com.vectras.vm;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vectras.vm.main.MainActivity;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.JSONUtils;
import com.vectras.vm.utils.PermissionUtils;
import com.vectras.vm.utils.UIUtils;

import java.util.HashMap;
import java.util.Objects;

public class CqcmActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!PermissionUtils.storagepermission(this,false)) {
            UIUtils.edgeToEdge(this);
            setContentView(R.layout.activity_cqcm);
            UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));

            Button buttonallow = findViewById(R.id.buttonallow);
            buttonallow.setOnClickListener(v -> {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.find_and_allow_access_to_storage_in_settings), Toast.LENGTH_LONG).show();
                } else {
                    ActivityCompat.requestPermissions(CqcmActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("CqcmActivity", "Checking access to storage...");
        if(PermissionUtils.storagepermission(this,false)) {
            if (getIntent().hasExtra("command")) {
                runCommand(getIntent().getStringExtra("command"));
            } else {
                startAdd();
            }
        }

    }
    private void startAdd() {
        HashMap<String, Object> mapForCreateNewVM;
        String _map;
        String imgName = "";
        String imgIcon = "";
        String imgPath = "";
        String imgArch = "";
        String imgCdrom = "";
        String imgExtra = "";
        String vmID = VMManager.idGenerator();

        if (!FileUtils.isFileExists(AppConfig.romsdatajson)) {
            FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", "[]");
        }

        if (JSONUtils.isValidFromFile(AppConfig.romsdatajson)) {
            if (getIntent().hasExtra("content")) {
                if (getIntent().hasExtra("cqcmcontent")) {
                    if (JSONUtils.isValidFromString(getIntent().getStringExtra("content"))) {
                        String vmId;
                        boolean isForceCreateNew = getIntent().hasExtra("forceCreateNew") && getIntent().getBooleanExtra("forceCreateNew", false);
                        if (!isForceCreateNew && VMManager.isVMExist(getIntent().getStringExtra("vmId"))) {
                            vmId = getIntent().getStringExtra("vmId");
                            if (VMManager.replaceToVMList(-1, getIntent().getStringExtra("vmId"), getIntent().getStringExtra("content"))) {
                                Toast.makeText(getApplicationContext(), getString(R.string.vm_has_been_edited), Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getApplicationContext(), getString(R.string.an_error_occurred_and_vm_was_not_modified), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            vmId = VMManager.isVMExist(getIntent().getStringExtra("vmId")) ? VMManager.idGenerator() : getIntent().getStringExtra("vmId");
                            if (VMManager.addToVMList(getIntent().getStringExtra("content"), vmId)) {
                                Toast.makeText(getApplicationContext(), getString(R.string.vm_has_been_created), Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getApplicationContext(), getString(R.string.an_error_occurred_and_vm_was_not_created), Toast.LENGTH_LONG).show();
                            }
                        }
                        FileUtils.writeToFile(AppConfig.vmFolder + vmId, "cqcm.json", getIntent().getStringExtra("cqcmcontent"));
                    } else {
                        Toast.makeText(getApplicationContext(), "An error occurred and it was not possible to create or edit a virtual machine.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    if (Objects.requireNonNull(getIntent().getStringExtra("content")).endsWith("}]")) {
                        _map = Objects.requireNonNull(getIntent().getStringExtra("content")).substring(0, Objects.requireNonNull(getIntent().getStringExtra("content")).length() - 1);
                    } else {
                        _map = Objects.requireNonNull(getIntent().getStringExtra("content"));
                    }
                    if (JSONUtils.isValidFromString(_map)) {
                        mapForCreateNewVM = new Gson().fromJson(_map, new TypeToken<HashMap<String, Object>>() {
                        }.getType());
                        mapForCreateNewVM.put("vmID", VMManager.startRamdomVMID());
                        VMManager.addVM(mapForCreateNewVM, -1);
                    } else {
                        Toast.makeText(getApplicationContext(), "The data for the new virtual machine is corrupted and cannot be created.", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Toast.makeText(getApplicationContext(), "There is no data about the new virtual machine to create.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "The virtual machine list data is corrupted and new virtual machines cannot be added right now.", Toast.LENGTH_LONG).show();
        }

        if (!MainActivity.isActivate) {
            startActivity(new Intent(this, SplashActivity.class));
        } else {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.setClass(this, MainActivity.class);
            startActivity(intent);
        }
        finish();
    }

    private void runCommand(String _command) {
        AppConfig.pendingCommand = _command;

        if (!MainActivity.isActivate) {
            startActivity(new Intent(this, SplashActivity.class));
        } else {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.setClass(this, MainActivity.class);
            startActivity(intent);
        }
        finish();
    }
}