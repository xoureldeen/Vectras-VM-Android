package com.vectras.vm.settings;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.databinding.ActivitySettings2Binding;
import com.vectras.vm.utils.UIUtils;

import java.util.Objects;

public class Settings2Activity extends AppCompatActivity {

    ActivitySettings2Binding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.edgeToEdge(this);
        binding = ActivitySettings2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        initialize();
    }

    private void initialize() {
        binding.lnTheme.setOnClickListener(v -> startActivity(new Intent(this, ThemeActivity.class)));
        binding.lnNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationSettingsActivity.class)));
        binding.lnSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchSettingsActivity.class)));
        binding.lnSystem.setOnClickListener(v -> startActivity(new Intent(this, SystemSettingsActivity.class)));

        binding.lnQemu.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainSettingsManager.class);
            intent.putExtra("goto", "qemu");
            startActivity(intent);
        });
        binding.lnImportExport.setOnClickListener(v -> startActivity(new Intent(this, ImportExportSettingsActivity.class)));

        binding.lnVnc.setOnClickListener(v -> startActivity(new Intent(this, VNCSettingsActivity.class)));
        binding.lnX11.setOnClickListener(v -> startActivity(new Intent(this, X11DisplaySettingsActivity.class)));
    }
}