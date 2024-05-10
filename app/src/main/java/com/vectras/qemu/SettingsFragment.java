package com.vectras.qemu;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.vectras.vm.R;
import com.vectras.vm.SplashActivity;

public class SettingsFragment extends PreferenceFragmentCompat {

    private Handler mHandler;
    public SharedPreferences mPref;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();
        SharedPreferences.OnSharedPreferenceChangeListener listener;
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                switch (key) {

                    case "modeNight":
                        getActivity().finish();
                        startActivity(new Intent(getActivity(), SplashActivity.class));
                        break;
                    case "customMemory":
                        if (prefs.getBoolean("customMemory", false))
                            findPreference("memory").setEnabled(true);
                        else
                            findPreference("memory").setEnabled(false);
                        break;
                    case "MTTCG":
                        if (prefs.getBoolean("MTTCG", false)) {
                            findPreference("cpuNum").setEnabled(false);
                            MainSettingsManager.setCpuCores(getContext(), 1);
                        } else {
                            findPreference("cpuNum").setEnabled(true);
                        }
                        break;
                }
            }
        };

        mPref = getPreferenceManager().getDefaultSharedPreferences(getContext());
        if (mPref != null) {
            mPref.registerOnSharedPreferenceChangeListener(listener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPref.getBoolean("customMemory", false))
            findPreference("memory").setEnabled(true);
        else
            findPreference("memory").setEnabled(false);

        if (mPref.getBoolean("MTTCG", false)) {
            findPreference("cpuNum").setEnabled(false);
        } else {
            findPreference("cpuNum").setEnabled(true);
        }
    }
}
