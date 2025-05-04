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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Objects;

import com.vectras.vm.utils.UIUtils;
import com.vectras.vm.utils.PermissionUtils;

public class ReceiveRomFileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PermissionUtils.storagepermission(this, false)) {
            UIUtils.edgeToEdge(this);
            setContentView(R.layout.activity_cqcm);
            UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));
            Button buttonallow;
            buttonallow = findViewById(R.id.buttonallow);
            buttonallow.setOnClickListener(v -> {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.find_and_allow_access_to_storage_in_settings), Toast.LENGTH_LONG).show();
                } else {
                    ActivityCompat.requestPermissions(ReceiveRomFileActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (PermissionUtils.storagepermission(this, false)) {
            String filesDir = getFilesDir().getAbsolutePath();
            if ((new File(filesDir, "/distro/usr/local/bin/qemu-system-x86_64").exists()) || (new File(filesDir, "/distro/usr/bin/qemu-system-x86_64").exists())) {
                Intent intent = getIntent();
                String action = intent.getAction();
                Uri uri = intent.getData();

                if (Intent.ACTION_VIEW.equals(action) && uri != null) {
                    if (Objects.requireNonNull(uri.getPath()).endsWith(".cvbi")) {
                        Intent _intent = new Intent();
                        _intent.setClass(this, CustomRomActivity.class);
                        _intent.putExtra("addromnow", "");
                        _intent.putExtra("romextra", "");
                        _intent.putExtra("romname", "");
                        _intent.putExtra("romicon", "");
                        _intent.putExtra("romfilename", ".cvbi");
                        _intent.putExtra("rompath", getFilePath(uri));
                        startActivity(_intent);
                        Log.i("ReceiveRomFileActivity", uri.toString());
                        Log.i("ReceiveRomFileActivity", Objects.requireNonNull(getFilePath(uri)));
                    } else {
                        Toast.makeText(ReceiveRomFileActivity.this, getResources().getString(R.string.format_not_supported_please_select_file_with_format_cvbi), Toast.LENGTH_LONG).show();
                    }
                } else if (Intent.ACTION_SEND.equals(action)) {
                    uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (uri != null) {
                        if (Objects.requireNonNull(uri.getPath()).endsWith(".cvbi")) {
                            Intent _intent = new Intent();
                            _intent.setClass(this, CustomRomActivity.class);
                            _intent.putExtra("addromnow", "");
                            _intent.putExtra("romextra", "");
                            _intent.putExtra("romname", "");
                            _intent.putExtra("romicon", "");
                            _intent.putExtra("romfilename", ".cvbi");
                            _intent.putExtra("rompath", getFilePath(uri));
                            startActivity(_intent);
                            Log.i("ReceiveRomFileActivity", uri.toString());
                            Log.i("ReceiveRomFileActivity", Objects.requireNonNull(getFilePath(uri)));
                        } else {
                            Toast.makeText(ReceiveRomFileActivity.this, getResources().getString(R.string.format_not_supported_please_select_file_with_format_cvbi), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            } else {
                Toast.makeText(ReceiveRomFileActivity.this, getResources().getString(R.string.you_need_to_complete_vectras_vm_setup_before_importing_this_file), Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, SplashActivity.class));
            }
            finish();
        }
    }
    private String getFilePath(Uri _uri) {
        if (_uri.toString().contains("/file%253A%252F%252F%252F")) {
            //Decrypt 2 times by FileProvider
            try {
                String decoded1 = URLDecoder.decode(_uri.getPath(), "UTF-8");
                String decoded2 = URLDecoder.decode(decoded1, "UTF-8");
                //No need to check and works perfectly with return decoded2.replace("file://", "");
                if (decoded2.startsWith("/file://")) {
                    return decoded2.replace("/file://", "");
                } else {
                    return decoded2.replace("file://", "");
                }
            } catch (UnsupportedEncodingException _e) {
                _e.printStackTrace();
            }
        } else {
            return _uri.getPath();
        }
        return null;
    }
}
