package com.vectras.vm.settings;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.databinding.ActivityUpdateSettingsBinding;

import com.vectras.vm.utils.UIUtils;

import java.util.Objects;

public class UpdateSettingsActivity extends AppCompatActivity {

    ActivityUpdateSettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.edgeToEdge(this);
        binding = ActivityUpdateSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        initialize();
    }

    private void initialize() {
        binding.swAutoCheck.setOnCheckedChangeListener((buttonView, isChecked) -> MainSettingsManager.setPromptUpdateVersion(this, isChecked));
        binding.lnAutoCheck.setOnClickListener(v -> binding.swAutoCheck.toggle());

        binding.swJoinBeta.setOnCheckedChangeListener((buttonView, isChecked) -> MainSettingsManager.setcheckforupdatesfromthebetachannel(this, isChecked));
        binding.lnJoinBeta.setOnClickListener(v -> binding.swJoinBeta.toggle());

        binding.swAutoCheck.setChecked(MainSettingsManager.getPromptUpdateVersion(this));
        binding.swJoinBeta.setChecked(MainSettingsManager.getcheckforupdatesfromthebetachannel(this));
    }
}