package com.vectras.vm.settings;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.R;
import com.vectras.vm.databinding.ActivityFilePickerSettingsBinding;
import com.vectras.vm.file.FilePickerSettings;

import java.util.Objects;

public class FilePickerSettingsActivity extends AppCompatActivity {

    ActivityFilePickerSettingsBinding binding;
    FilePickerSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_external_vnc_settings);
        binding = ActivityFilePickerSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        initialize();
    }

    private void initialize() {
        settings = new FilePickerSettings(this);

        binding.swEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            MainSettingsManager.setBuiltInFilePicker(this, isChecked);
            uiController(isChecked);
        });
        binding.lnEnabled.setOnClickListener(v -> binding.swEnabled.toggle());

        binding.swResume.setOnCheckedChangeListener((buttonView, isChecked) -> settings.resume(isChecked));
        binding.lnResume.setOnClickListener(v -> binding.swResume.toggle());

        binding.swDivider.setOnCheckedChangeListener((buttonView, isChecked) -> settings.divider(isChecked));
        binding.lnDivider.setOnClickListener(v -> binding.swDivider.toggle());

        binding.swShowHiddenFiles.setOnCheckedChangeListener((buttonView, isChecked) -> settings.showHiddenFiles(isChecked));
        binding.lnShowHiddenFiles.setOnClickListener(v -> binding.swShowHiddenFiles.toggle());

        binding.swResume.setChecked(settings.resume());
        binding.swDivider.setChecked(settings.divider());
        binding.swShowHiddenFiles.setChecked(settings.showHiddenFiles());
        binding.swEnabled.setChecked(MainSettingsManager.getBuiltInFilePicker(this));

        uiController(binding.swEnabled.isChecked());
    }

    private void uiController(boolean isEnabled) {
        binding.lnAllOptions.setAlpha(isEnabled ? 1f : 0.5f);
        binding.lnResume.setEnabled(isEnabled);
        binding.lnDivider.setEnabled(isEnabled);
        binding.lnShowHiddenFiles.setEnabled(isEnabled);
    }
}