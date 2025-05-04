package com.vectras.vm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.utils.UIUtils;

public class SetArchActivity extends AppCompatActivity implements View.OnClickListener {

    SetArchActivity activity;
    private static Handler mHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        UIUtils.edgeToEdge(this);
        setContentView(R.layout.activity_set_arch);
        UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));
        activity = this;
        Button archi386 = findViewById(R.id.archi386);
        Button archx86_64 = findViewById(R.id.archx86_64);
        Button archarm64 = findViewById(R.id.archarm64);
        Button archppc = findViewById(R.id.archppc);
        Button web = findViewById(R.id.webBtn);
        Button buttongetcm = findViewById(R.id.buttongetcm);
        CardView cdCustom = findViewById(R.id.cdCustom);
        archi386.setOnClickListener(this);
        archx86_64.setOnClickListener(this);
        archarm64.setOnClickListener(this);
        archppc.setOnClickListener(this);
        web.setOnClickListener(this);
        buttongetcm.setOnClickListener(this);
        cdCustom.setOnClickListener(this);
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
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("android-app://com.anbui.cqcm.app"));
            startActivity(intent);
        } else if (id == R.id.cdCustom) {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), CustomRomActivity.class);
            intent.putExtra("importcvbinow", "");
            startActivity(intent);
            finish();
        }
    }
}