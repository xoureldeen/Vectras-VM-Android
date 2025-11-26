package com.vectras.vm.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.R;
import com.vectras.vm.databinding.ActivityVncSettingsBinding;

import java.util.Objects;

public class VNCSettingsActivity extends AppCompatActivity {

    ActivityVncSettingsBinding binding;
    boolean isInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_external_vnc_settings);
        binding = ActivityVncSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        initialize();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isInitialized) {
            binding.swExternal.setEnabled(true);
            binding.swExternal.setChecked(MainSettingsManager.getVncExternal(this));
            binding.swEnabled.setChecked(MainSettingsManager.getVmUi(this).equals("VNC"));
            uiController(binding.swEnabled.isChecked());
        }
    }

    private void initialize() {
        binding.swEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            MainSettingsManager.setVmUi(this, isChecked ? "VNC" : "X11");
            uiController(isChecked);
        });
        binding.lnEnabled.setOnClickListener(v -> binding.swEnabled.toggle());

        binding.swForcerefesh.setOnCheckedChangeListener((buttonView, isChecked) -> MainSettingsManager.setForceRefeshVNCDisplay(this, isChecked));
        binding.lnForcerefesh.setOnClickListener(v -> binding.swForcerefesh.toggle());

        binding.swExternal.setOnCheckedChangeListener((buttonView, isChecked) -> MainSettingsManager.setVncExternal(this, isChecked));
        binding.lnExternal.setOnClickListener( v -> startActivity(new Intent(this, ExternalVNCSettingsActivity.class)));

        binding.swForcerefesh.setChecked(MainSettingsManager.getForceRefeshVNCDisplay(this));
        binding.swExternal.setChecked(MainSettingsManager.getVncExternal(this));

        isInitialized = true;

        uiController(binding.swEnabled.isChecked());
    }

    private void uiController(boolean isEnabled) {
        binding.lnAllOptions.setAlpha(isEnabled ? 1f : 0.5f);
        binding.lnForcerefesh.setEnabled(isEnabled);
        binding.lnExternal.setEnabled(isEnabled);
        binding.swExternal.setEnabled(isEnabled);
    }
}