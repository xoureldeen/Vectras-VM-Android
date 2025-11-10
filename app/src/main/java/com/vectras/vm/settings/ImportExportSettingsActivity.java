package com.vectras.vm.settings;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.R;
import com.vectras.vm.databinding.ActivityExternalVncSettingsBinding;
import com.vectras.vm.databinding.ActivityImportExportSettingsBinding;

import java.util.Objects;

public class ImportExportSettingsActivity extends AppCompatActivity {

    ActivityImportExportSettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityImportExportSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        initialize();
    }

    private void initialize() {
        binding.swSmartSizeCalculation.setChecked(MainSettingsManager.getSmartSizeCalculation(this));
        binding.swCyclicRedundancyCheck.setChecked(MainSettingsManager.getCyclicRedundancyCheck(this));

        binding.swSmartSizeCalculation.setOnCheckedChangeListener((buttonView, isChecked) -> MainSettingsManager.setSmartSizeCalculation(this, isChecked));
        binding.lnSmartSizeCalculation.setOnClickListener(v -> binding.swSmartSizeCalculation.toggle());

        binding.swCyclicRedundancyCheck.setOnCheckedChangeListener((buttonView, isChecked) -> MainSettingsManager.setCyclicRedundancyCheck(this, isChecked));
        binding.lnCyclicRedundancyCheck.setOnClickListener(v -> binding.swCyclicRedundancyCheck.toggle());
    }
}