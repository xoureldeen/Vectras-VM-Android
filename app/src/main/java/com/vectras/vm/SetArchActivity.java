package com.vectras.vm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.appbar.MaterialToolbar;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.databinding.ActivitySetArchBinding;
import com.vectras.vm.utils.PackageUtils;
import com.vectras.vm.utils.UIUtils;

import java.util.Objects;

public class SetArchActivity extends AppCompatActivity implements View.OnClickListener {

    SetArchActivity activity;
    ActivitySetArchBinding binding;
    private static Handler mHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        UIUtils.edgeToEdge(this);
        binding = ActivitySetArchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));
        activity = this;
        binding.archi386.setOnClickListener(this);
        binding.archx8664.setOnClickListener(this);
        binding.archarm64.setOnClickListener(this);
        binding.archppc.setOnClickListener(this);
        binding.webBtn.setOnClickListener(this);
        binding.buttongetcm.setOnClickListener(this);
        binding.bntimport.setOnClickListener(this);

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> {
            onBackPressed();
        });

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.roms_store) {

                Intent intent = new Intent(this, RomsManagerActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });

        if (PackageUtils.isInstalled("com.anbui.cqcm.app", this)) {
            binding.buttongetcm.setText(getResources().getString(R.string.open));
        }
    }

    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.archi386) {
            MainSettingsManager.setArch(this, "I386");

            startActivity(new Intent(activity, CustomRomActivity.class));
            finish();
        } else if (id == R.id.archx86_64) {
            MainSettingsManager.setArch(this, "X86_64");

            startActivity(new Intent(activity, CustomRomActivity.class));
            finish();
        } else if (id == R.id.archarm64) {
            MainSettingsManager.setArch(this, "ARM64");

            startActivity(new Intent(activity, CustomRomActivity.class));
            finish();
        } else if (id == R.id.archppc) {
            MainSettingsManager.setArch(this, "PPC");

            startActivity(new Intent(activity, CustomRomActivity.class));
            finish();
        } else if (id == R.id.webBtn) {
            String qe = "https://www.qemu.org/";
            Intent q = new Intent(Intent.ACTION_VIEW);
            q.setData(Uri.parse(qe));
            startActivity(q);
        } else if (id == R.id.buttongetcm) {
            PackageManager pm = getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage("com.anbui.cqcm.app");

            if (intent != null) {
                startActivity(intent);
            } else {
                Intent intenturl = new Intent();
                intenturl.setAction(Intent.ACTION_VIEW);
                intenturl.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.anbui.cqcm.app"));
                startActivity(intenturl);
            }
        } else if (id == R.id.bntimport) {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), CustomRomActivity.class);
            intent.putExtra("importcvbinow", "");
            startActivity(intent);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.set_arch_toolbar_menu, menu);
        return true;
    }
}