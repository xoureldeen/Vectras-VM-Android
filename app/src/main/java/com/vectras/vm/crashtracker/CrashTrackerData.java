package com.vectras.vm.crashtracker;

import android.content.Context;
import android.content.SharedPreferences;

public class CrashTrackerData {
    Context context;
    SharedPreferences sharedPreferences;

    public CrashTrackerData(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences("crash_tracker_data", Context.MODE_PRIVATE);
    }

    public void setLastANR(long value) {
        sharedPreferences.edit().putLong("lastANR", value).apply();
    }

    public long getLastANR() {
        return sharedPreferences.getLong("lastANR", 0);
    }
}
