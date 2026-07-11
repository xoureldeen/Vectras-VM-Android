package com.vectras.vm.file;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

public class FilePickerSettings {
    Context context;
    SharedPreferences sharedPreferences;

    public FilePickerSettings(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences("built_in_file_picker_settings", Context.MODE_PRIVATE);
    }

    public void lastPath(String path) {
        sharedPreferences.edit().putString("lastPath", path).apply();
    }

    public String lastPath() {
        return sharedPreferences.getString("lastPath", Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    public void resume(boolean enabled) {
        sharedPreferences.edit().putBoolean("resume", enabled).apply();
    }

    public boolean resume() {
        return sharedPreferences.getBoolean("resume", true);
    }

    public void showHiddenFiles(boolean enabled) {
        sharedPreferences.edit().putBoolean("showHiddenFiles", enabled).apply();
    }

    public boolean showHiddenFiles() {
        return sharedPreferences.getBoolean("showHiddenFiles", true);
    }

    public void divider(boolean enabled) {
        sharedPreferences.edit().putBoolean("divider", enabled).apply();
    }

    public boolean divider() {
        return sharedPreferences.getBoolean("divider", false);
    }
}
