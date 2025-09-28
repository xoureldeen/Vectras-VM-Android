package com.vectras.vm.settings;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.R;
import com.vectras.vm.databinding.ActivityExternalVncSettingsBinding;
import com.vectras.vm.utils.ClipboardUltils;

import java.util.Objects;

public class ExternalVNCSettingsActivity extends AppCompatActivity {

    ActivityExternalVncSettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_external_vnc_settings);
        binding = ActivityExternalVncSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> onBackPressed());
        initialize();
    }

    @Override
    public void onStop() {
        super.onStop();
        MainSettingsManager.setVncExternal(this, binding.swEnabled.isChecked());
        MainSettingsManager.setVncExternalDisplay(this, Objects.requireNonNull(binding.etDisplay.getText()).toString());
        MainSettingsManager.setVncExternalPassword(this, Objects.requireNonNull(binding.etPassword.getText()).toString());
    }

    private void initialize() {
        binding.swEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            MainSettingsManager.setVncExternal(this, isChecked);
            uiController(isChecked);
        });
        binding.lnEnabled.setOnClickListener(v -> binding.swEnabled.toggle());

        binding.etDisplay.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                VNCPort(s.toString());
            }
        });

        binding.tilPassword.setEndIconOnClickListener(v -> {
            if (binding.etPassword.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                binding.etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                binding.tilPassword.setEndIconDrawable(R.drawable.visibility_off);
            } else {
                binding.etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                binding.tilPassword.setEndIconDrawable(R.drawable.visibility_24px);
            }        });

        binding.ivCopy.setOnClickListener(v -> {
            ClipboardUltils.copyToClipboard(this, binding.tvPort.getText().toString());
        });

        binding.swEnabled.setChecked(MainSettingsManager.getVncExternal(this));
        binding.etDisplay.setText(MainSettingsManager.getVncExternalDisplay(this));
        binding.etPassword.setText(MainSettingsManager.getVncExternalPassword(this));
        uiController(binding.swEnabled.isChecked());
    }

    private void uiController(boolean isEnabled) {
        MainSettingsManager.setVncExternal(this, isEnabled);

        binding.lnAllOptions.setAlpha(isEnabled ? 1 : 0.5f);
        binding.etDisplay.setEnabled(isEnabled);
        binding.tilPassword.setEnabled(isEnabled);
        binding.etPassword.setEnabled(isEnabled);
        binding.ivCopy.setEnabled(isEnabled);
    }

    private void VNCPort(String display) {

        if (display.isEmpty()) {
            binding.tilDisplay.setError(getString(R.string.you_need_to_set_a_number_for_the_display));
            binding.cvCopyport.setVisibility(View.GONE);
            return;
        } else {
            if (binding.cvCopyport.getVisibility() == View.GONE)
                binding.cvCopyport.setVisibility(View.VISIBLE);
        }

        int result = Integer.parseInt(display) + 5900;

        if (result > 65535) {
            binding.tilDisplay.setError(getString(R.string.need_to_set_smaller_screen_number));
            if (binding.cvCopyport.getVisibility() == View.VISIBLE)
                binding.cvCopyport.setVisibility(View.GONE);
        } else {
            binding.tilDisplay.setError(null);
            binding.tvPort.setText(String.valueOf(result));
            if (binding.cvCopyport.getVisibility() == View.GONE)
                binding.cvCopyport.setVisibility(View.VISIBLE);
        }
    }
}