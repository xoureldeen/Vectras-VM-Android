package com.vectras.vm.settings;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.R;
import com.vectras.vm.databinding.ActivityX11DisplaySettingsBinding;

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

        binding.swEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            MainSettingsManager.setVmUi(this, isChecked ? "X11" : "VNC");
            uiController(isChecked);
        });
        binding.lnEnabled.setOnClickListener(v -> binding.swEnabled.toggle());

        binding.swRunQemuWithXterm.setOnCheckedChangeListener((buttonView, isChecked) -> MainSettingsManager.setRunQemuWithXterm(this, isChecked));
        binding.lnRunQemuWithXtermh.setOnClickListener(v -> binding.swRunQemuWithXterm.toggle());

        isInitialized = true;

        uiController(binding.swEnabled.isChecked());
    }

    private void uiController(boolean isEnabled) {
        binding.lnAllOptions.setAlpha(isEnabled ? 1f : 0.5f);
        binding.lnRunQemuWithXtermh.setEnabled(isEnabled);
    }
}