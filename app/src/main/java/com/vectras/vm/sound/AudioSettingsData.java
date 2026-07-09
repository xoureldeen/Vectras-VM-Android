package com.vectras.vm.sound;

import android.content.Context;
import android.content.SharedPreferences;

public class AudioSettingsData {
    Context context;
    SharedPreferences sharedPreferences;

    public AudioSettingsData(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences("audio_settings", Context.MODE_PRIVATE);
    }

    public void setEqualizerEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean("isEnabled", enabled).apply();
    }

    public boolean isEqualizerEnabled() {
        return sharedPreferences.getBoolean("isEnabled", false);
    }

    public void setUpperTreble(float value) {
        sharedPreferences.edit().putFloat("upperTreble", value).apply();
    }

    public float getUpperTreble() {
        return sharedPreferences.getFloat("upperTreble", 0);
    }

    public void setTreble(float value) {
        sharedPreferences.edit().putFloat("treble", value).apply();
    }

    public float getTreble() {
        return sharedPreferences.getFloat("treble", 0);
    }

    public void setMid(float value) {
        sharedPreferences.edit().putFloat("mid", value).apply();
    }

    public float getMid() {
        return sharedPreferences.getFloat("mid", 0);
    }

    public void setBass(float value) {
        sharedPreferences.edit().putFloat("bass", value).apply();
    }

    public float getBass() {
        return sharedPreferences.getFloat("bass", 0);
    }

    public void setLowBass(float value) {
        sharedPreferences.edit().putFloat("lowBass", value).apply();
    }

    public float getLowBass() {
        return sharedPreferences.getFloat("lowBass", 0);
    }
}
