package com.vectras.vm.settings;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.databinding.ActivitySearchSettingsBinding;
import com.vectras.vm.utils.UIUtils;

import java.util.Objects;

public class SearchSettingsActivity extends AppCompatActivity {

    ActivitySearchSettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.edgeToEdge(this);
        binding = ActivitySearchSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        initialize();
    }

    private void initialize() {
        binding.swRandomSuggestion.setOnCheckedChangeListener((buttonView, isChecked) -> MainSettingsManager.setSearchRandomSuggestion(this, isChecked));
        binding.lnRandomSuggestion.setOnClickListener(v -> binding.swRandomSuggestion.toggle());

        binding.swFilters.setOnCheckedChangeListener((buttonView, isChecked) -> MainSettingsManager.setSearchFilters(this, isChecked));
        binding.lnFilters.setOnClickListener(v -> binding.swFilters.toggle());

        binding.swRandomSuggestion.setChecked(MainSettingsManager.getSearchRandomSuggestion(this));
        binding.swFilters.setChecked(MainSettingsManager.getSearchFilters(this));
    }
}