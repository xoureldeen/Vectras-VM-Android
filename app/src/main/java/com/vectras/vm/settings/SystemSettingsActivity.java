package com.vectras.vm.settings;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.databinding.ActivitySystemSettingsBinding;
import com.vectras.vm.utils.UIUtils;

import java.util.Objects;

public class SystemSettingsActivity extends AppCompatActivity {

    ActivitySystemSettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.edgeToEdge(this);
        binding = ActivitySystemSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        initialize();
    }

    private void initialize() {
        binding.swCopyFile.setOnCheckedChangeListener((buttonView, isChecked) -> MainSettingsManager.setcopyFile(this, isChecked));
        binding.lnCopyFile.setOnClickListener(v -> binding.swCopyFile.toggle());

        binding.swQuickStart.setOnCheckedChangeListener((buttonView, isChecked) -> MainSettingsManager.setQuickStart(this, isChecked));
        binding.lnQuickStart.setOnClickListener(v -> binding.swQuickStart.toggle());

        binding.swCopyFile.setChecked(MainSettingsManager.copyFile(this));
        binding.swQuickStart.setChecked(MainSettingsManager.getQuickStart(this));

        binding.lnLanguage.setOnClickListener(v -> {
            ItemSettingsSelector.language(this, ItemSettingsSelector.getLanguagePosition(this, MainSettingsManager.getLang(this)), (position, name, value) -> {
                if (!value.isEmpty()) {
                    AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.forLanguageTags(value)
                    );
                } else {
                    AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.getEmptyLocaleList()
                    );
                }

                MainSettingsManager.setLang(this, value);
            });
        });

        binding.lnUpdate.setOnClickListener(v -> startActivity(new Intent(this, UpdaterActivity.class)));
    }
}