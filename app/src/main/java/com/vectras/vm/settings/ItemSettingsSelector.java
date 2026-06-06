package com.vectras.vm.settings;

import android.app.Activity;
import android.content.Context;

import com.vectras.vm.R;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.UniversalPickerDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ItemSettingsSelector {
    public static void vncRefreshRate(Activity activity, int position, UniversalPickerDialog.UniversalPickerDialogCallback callback) {
        showDialog(activity, vncRefreshRate(activity), position, callback, activity.getString(R.string.refresh_rate));
    }

    public static HashMap<String, Object> getVncRefreshRate(int position) {
        return vncRefreshRate().get(position);
    }

    public static String getVncRefreshRateValue(int position) {
        return Objects.requireNonNull(vncRefreshRate().get(position).get("value")).toString();
    }

    public static void language(Activity activity, int position, UniversalPickerDialog.UniversalPickerDialogCallback callback) {
        showDialog(activity, language(activity), position, callback, activity.getString(R.string.language));
    }

    public static void showDialog(Activity activity, ArrayList<HashMap<String, Object>> list, int position, UniversalPickerDialog.UniversalPickerDialogCallback callback, String title) {
        UniversalPickerDialog.show(activity, list, position, callback, title);
    }

    public static ArrayList<HashMap<String, Object>> vncRefreshRate(Activity activity) {
        int max = DeviceUtils.getMaxRefreshRate(activity);
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        UniversalPickerDialog.putToList(list, "20", "20");
        UniversalPickerDialog.putToList(list, "25", "25");
        UniversalPickerDialog.putToList(list, "30", "30");
        UniversalPickerDialog.putToList(list, "45", "45");
        UniversalPickerDialog.putToList(list, "60", "60");
        if (max >= 75) UniversalPickerDialog.putToList(list, "75", "75");
        if (max >= 90) UniversalPickerDialog.putToList(list, "90", "90");
        if (max >= 120) UniversalPickerDialog.putToList(list, "120", "120");
        return list;
    }

    public static ArrayList<HashMap<String, Object>> vncRefreshRate() {
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        UniversalPickerDialog.putToList(list, "20", "20");
        UniversalPickerDialog.putToList(list, "25", "25");
        UniversalPickerDialog.putToList(list, "30", "30");
        UniversalPickerDialog.putToList(list, "45", "45");
        UniversalPickerDialog.putToList(list, "60", "60");
        UniversalPickerDialog.putToList(list, "75", "75");
        UniversalPickerDialog.putToList(list, "90", "90");
        UniversalPickerDialog.putToList(list, "120", "120");
        return list;
    }

    public static ArrayList<HashMap<String, Object>> language(Context context) {
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        UniversalPickerDialog.putToList(list, context.getString(R.string.system), "");
        UniversalPickerDialog.putToList(list, "English", "en");
        UniversalPickerDialog.putToList(list, "中文", "zh");
        UniversalPickerDialog.putToList(list, "русский", "ru");
        UniversalPickerDialog.putToList(list, "Tiếng Việt", "vi");
        UniversalPickerDialog.putToList(list, "العربية", "ar");
        return list;
    }

    public static int getLanguagePosition(Context context, String languageCode) {
        ArrayList<HashMap<String, Object>> list = language(context);
        for (int i = 0; i < list.size(); i++) {
            if (Objects.requireNonNull(list.get(i).get("value")).toString().equals(languageCode)) {
                return i;
            }
        }
        return 0;
    }
}
