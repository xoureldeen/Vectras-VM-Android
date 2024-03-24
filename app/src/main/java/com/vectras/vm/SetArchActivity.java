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

import com.vectras.qemu.MainSettingsManager;

public class SetArchActivity extends AppCompatActivity implements View.OnClickListener {

    SetArchActivity activity;
    private static Handler mHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        setContentView(R.layout.activity_set_arch);
        activity = this;
        Button arch86 = findViewById(R.id.archx86);
        Button archarm = findViewById(R.id.archarm);
        Button web = findViewById(R.id.webBtn);
        arch86.setOnClickListener(this);
        archarm.setOnClickListener(this);
        web.setOnClickListener(this);
    }

    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.archx86) {
            MainSettingsManager.setArch(this, "X86_64");

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent startActivity = new Intent(activity, SplashActivity.class);
                    int pendingIntentId = 123456;
                    PendingIntent pendingIntent = PendingIntent.getActivity(activity, pendingIntentId, startActivity, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    AlarmManager mgr = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
                    mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 500, pendingIntent);

                    System.exit(0);
                }
            }, 300);
        } else if (id == R.id.archarm) {
            MainSettingsManager.setArch(this, "ARM");

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent startActivity = new Intent(activity, SplashActivity.class);
                    int pendingIntentId = 123456;
                    PendingIntent pendingIntent = PendingIntent.getActivity(activity, pendingIntentId, startActivity, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    AlarmManager mgr = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
                    mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 500, pendingIntent);

                    System.exit(0);
                }
            }, 300);
        } else if (id == R.id.webBtn) {
            String qe = "https://www.qemu.org/";
            Intent q = new Intent(Intent.ACTION_VIEW);
            q.setData(Uri.parse(qe));
            startActivity(q);
        }
    }
}