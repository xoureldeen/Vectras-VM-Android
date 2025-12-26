package com.vectras.vm.settings;

import static android.os.Build.VERSION.SDK_INT;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.R;
import com.vectras.vm.databinding.ActivityX11DisplaySettingsBinding;
import com.vectras.vm.utils.DeviceUtils;

import java.util.Objects;

public class X11DisplaySettingsActivity extends AppCompatActivity {

    ActivityX11DisplaySettingsBinding binding;
    boolean isInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_external_vnc_settings);
        binding = ActivityX11DisplaySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        initialize();
    }

    private void initialize() {
        binding.swEnabled.setChecked(MainSettingsManager.getVmUi(this).equals("X11"));
        binding.swRunQemuWithXterm.setChecked(MainSettingsManager.getRunQemuWithXterm(this));
        binding.swUseSdl.setChecked(MainSettingsManager.getUseSdl(this));

        binding.swEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            MainSettingsManager.setVmUi(this, isChecked ? "X11" : "VNC");
            uiController(isChecked);
        });
        binding.lnEnabled.setOnClickListener(v -> binding.swEnabled.toggle());

        binding.lnPreferences.setOnClickListener(v -> {
            Intent intent = new Intent();
            if (SDK_INT >= 34 || !DeviceUtils.isArm()) {
                intent.setClassName("com.termux.x11", "com.termux.x11.MainActivity");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            } else {
                intent.setClass(this, MainSettingsManager.class);
                intent.putExtra("goto", "termuxx11");
            }
            startActivity(intent);
        });

        binding.swRunQemuWithXterm.setOnCheckedChangeListener((buttonView, isChecked) -> MainSettingsManager.setRunQemuWithXterm(this, isChecked));
        binding.lnRunQemuWithXterm.setOnClickListener(v -> binding.swRunQemuWithXterm.toggle());

        binding.swUseSdl.setOnCheckedChangeListener((buttonView, isChecked) -> MainSettingsManager.setUseSdl(this, isChecked));
        binding.lnUseSdl.setOnClickListener(v -> binding.swUseSdl.toggle());

        isInitialized = true;

        uiController(binding.swEnabled.isChecked());
    }

    private void uiController(boolean isEnabled) {
        binding.lnAllOptions.setAlpha(isEnabled ? 1f : 0.5f);
        binding.lnPreferences.setEnabled(isEnabled);
        binding.lnRunQemuWithXterm.setEnabled(isEnabled);
        binding.lnUseSdl.setEnabled(isEnabled);
    }
}