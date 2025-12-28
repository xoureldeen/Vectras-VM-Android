package com.vectras.vm.crashtracker;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.AppConfig;
import com.vectras.vm.R;
import com.vectras.vm.SplashActivity;
import com.vectras.vm.databinding.ActivityLastCrashBinding;
import com.vectras.vm.utils.ClipboardUltils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.UIUtils;

import java.io.File;

public class LastCrashActivity extends AppCompatActivity {
    ActivityLastCrashBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.edgeToEdge(this);
        binding = ActivityLastCrashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));

        binding.tvContent.setText(FileUtils.isFileExists(AppConfig.lastCrashLogPath) ? FileUtils.readAFile(AppConfig.lastCrashLogPath) : getString(R.string.there_are_no_logs));
        binding.btnCopy.setOnClickListener(v -> ClipboardUltils.copyToClipboard(this, binding.tvContent.getText().toString()));

        MainSettingsManager.setShowLastCrashLog(this, false);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(LastCrashActivity.this, SplashActivity.class));
                finish();
            }
        });
    }
}