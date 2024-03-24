package com.iiordanov.bVNC;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.View;

import com.iiordanov.util.CustomClientConfigFileReader;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class CustomVnc extends bVNC {
    private final static String TAG = "CustomVnc";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        try {
            View view;
            String packageName = Utils.pName(this);
            CustomClientConfigFileReader configFileReader = new CustomClientConfigFileReader(
                    getAssets().open(packageName + ".yaml"));
            Map<String, Map> configData = configFileReader.getConfigData();
            Map<String, Integer> visibility = (Map<String, Integer>) configData.get("mainConfiguration").get("visibility");

            for (String s : visibility.keySet()) {
                android.util.Log.d(TAG, s);
                int resID = getResources().getIdentifier(s, "id", packageName);
                view = findViewById(resID);
                view.setVisibility(visibility.get(s));
            }

            Locale current = getResources().getConfiguration().locale;
            String currentLanguage = current.getLanguage();
            Map mainConfiguration = configData.get("mainConfiguration");

            String title = (String) ((Map) configData.get("mainConfiguration").get("title")).get("default");
            String subtitle = (String) ((Map) configData.get("mainConfiguration").get("subtitle")).get("default");
            String currentLanguageTitle = (String) ((Map) configData.get("mainConfiguration").get("title")).get(currentLanguage);
            if (currentLanguageTitle != null) {
                title = currentLanguageTitle;
            }
            String currentLanguageSubTitle = (String) ((Map) configData.get("mainConfiguration").get("subtitle")).get(currentLanguage);
            if (currentLanguageSubTitle != null) {
                subtitle = currentLanguageSubTitle;
            }
            ActionBar ab = getActionBar();
            ab.setTitle(title);
            ab.setSubtitle(subtitle);
        } catch (IOException e) {
            android.util.Log.e(TAG, "Error opening config file from assets.");
            e.printStackTrace();
        }
    }
}
