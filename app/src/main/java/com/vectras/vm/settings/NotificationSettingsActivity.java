package com.vectras.vm.settings;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.R;
import com.vectras.vm.databinding.ActivityNotificationSettingsBinding;
import com.vectras.vm.databinding.ActivitySettings2Binding;
import com.vectras.vm.fcm.FCMManager;
import com.vectras.vm.utils.UIUtils;

import java.util.Objects;

public class NotificationSettingsActivity extends AppCompatActivity {

    ActivityNotificationSettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.edgeToEdge(this);
        binding = ActivityNotificationSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        initialize();
    }

    private void initialize() {
        binding.swSuggestionsAndTips.setOnCheckedChangeListener((buttonView, isChecked) -> {
            MainSettingsManager.setSuggestionsAndTipsNotification(this, isChecked);

            if (isChecked) {
                FCMManager.subscribe();
            } else {
                FCMManager.unSubscribe();
            }
        });
        binding.lnSuggestionsAndTips.setOnClickListener(v -> binding.swSuggestionsAndTips.toggle());

        binding.lnManageInSystemSettings.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT < 26) {
                intent.putExtra("app_package", getPackageName());
                intent.putExtra("app_uid", getApplicationInfo().uid);
            } else {
                intent.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());
            }

            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), getString(R.string.unavailable), Toast.LENGTH_SHORT).show();
            }
        });

        binding.swSuggestionsAndTips.setChecked(MainSettingsManager.getSuggestionsAndTipsNotification(this));
    }
}