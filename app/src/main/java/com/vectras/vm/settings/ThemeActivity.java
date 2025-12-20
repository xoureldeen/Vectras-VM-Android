package com.vectras.vm.settings;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.DynamicColors;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.R;
import com.vectras.vm.databinding.ActivityThemeBinding;
import com.vectras.vm.main.MainActivity;
import com.vectras.vm.utils.UIUtils;

import java.util.Objects;

public class ThemeActivity extends AppCompatActivity {
    ActivityThemeBinding binding;
    int oldThemeData;
    int newThemeData;
    boolean oldDynamicColorData;
    boolean newDynamicColorData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_external_vnc_settings);
        binding = ActivityThemeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> onBack());
        initialize();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBack();
            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!(oldDynamicColorData == newDynamicColorData))
            MainActivity.isNeedRecreate = true;
    }

    private void initialize() {
        oldThemeData = MainSettingsManager.getTheme(this);
        newThemeData = oldThemeData;
        if (oldThemeData == MainSettingsManager.THEME_DEFAULT) {
            binding.dnSelector.check(binding.selectDaynight.getId());
        } else if (oldThemeData == MainSettingsManager.THEME_LIGHT) {
            binding.dnSelector.check(binding.selectDay.getId());
        } else if (oldThemeData == MainSettingsManager.THEME_DARK) {
            binding.dnSelector.check(binding.selectNight.getId());
        }

        binding.dnSelector.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == binding.selectDaynight.getId()) {
                    changeTheme(MainSettingsManager.THEME_DEFAULT);
                } else if (checkedId == binding.selectDay.getId()) {
                    changeTheme(MainSettingsManager.THEME_LIGHT);
                } else if (checkedId == binding.selectNight.getId()) {
                    changeTheme(MainSettingsManager.THEME_DARK);
                }
            }
        });

        oldDynamicColorData = MainSettingsManager.getDynamicColor(this);
        newDynamicColorData = oldDynamicColorData;

        if (DynamicColors.isDynamicColorAvailable()) {
            binding.swDynamiccolor.setChecked(oldDynamicColorData);
            binding.lnDynamiccolor.setOnClickListener(v -> binding.swDynamiccolor.toggle());
            binding.swDynamiccolor.setOnCheckedChangeListener((buttonView, isChecked) -> changeDynamicColor(isChecked));
        } else {
            binding.lnDynamiccolor.setEnabled(false);
            binding.lnDynamiccolor.setAlpha(0.5f);
        }
    }

    private void onBack() {
        startActivity(new Intent(this, MainSettingsManager.class));
        finish();
    }

    private void changeTheme(int mode) {
        newThemeData = mode;
        MainSettingsManager.setTheme(this, mode);
        UIUtils.setDarkOrLight(mode);
        recreate();
    }

    private void changeDynamicColor(boolean isEnable) {
        newDynamicColorData = isEnable;
        MainSettingsManager.setDynamicColor(this, isEnable);
        recreate();
    }
}