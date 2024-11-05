package com.vectras.qemu;

import static android.os.Build.VERSION.SDK_INT;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

import com.vectras.vm.MainActivity;
import com.vectras.vm.R;
import com.vectras.vm.SplashActivity;
import com.vectras.vm.VectrasApp;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainSettingsManager extends AppCompatActivity
        implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    public static MainSettingsManager activity;

    private static Handler mHandler;
    public static SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        activity = this;

        sp = PreferenceManager.getDefaultSharedPreferences(activity);

        PreferenceFragmentCompat preference = new MainPreferencesFragment();
        Intent intent = getIntent();

        // add preference settings
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settingz, preference)
                .commit();

        // toolbar
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle bundle = pref.getExtras();
        final Fragment fragment = Fragment.instantiate(this, pref.getFragment(), bundle);

        fragment.setTargetFragment(caller, 0);

        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settingz, fragment)
                .addToBackStack(null)
                .commit();

        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static class MainPreferencesFragment extends PreferenceFragmentCompat
            implements Preference.OnPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

        }

        @Override
        public void onResume() {
            super.onResume();

        }

        @Override
        public void onPause() {
            super.onPause();

        }


        @Override
        public void onCreatePreferences(Bundle bundle, String root_key) {
            // Load the Preferences from the XML file
            setPreferencesFromResource(R.xml.headers_preference, root_key);

        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            if (pref.getKey().equals("app")) {

            }
            return true;
        }

    }


    public static class AppPreferencesFragment extends PreferenceFragmentCompat
            implements Preference.OnPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onResume() {
            super.onResume();

        }

        @Override
        public void onPause() {
            super.onPause();

        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load the Preferences from the XML file
            setPreferencesFromResource(R.xml.settings, rootKey);

            // Find the ListPreference and set the change listener
            ListPreference languagePref = findPreference("language");
            if (languagePref != null) {
                languagePref.setOnPreferenceChangeListener(this);
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference.getKey().equals("language")) {
                String newLocale = (String) newValue;
                updateLocale(newLocale);
                return true;
            }
            return false;
        }

        private void updateLocale(String languageCode) {
            Locale locale = new Locale(languageCode);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.setLocale(locale);

            getResources().updateConfiguration(config, getResources().getDisplayMetrics());

            MainActivity.activity.finish();
            activity.finish();
            startActivity(new Intent(activity, SplashActivity.class));
        }

    }

    public static class UserInterfacePreferencesFragment extends PreferenceFragmentCompat
            implements Preference.OnPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mHandler = new Handler();
            Preference pref = findPreference("modeNight");
            if (pref != null) {
                pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(@NonNull Preference preference,
                                                      Object newValue) {
                        onNightMode();
                        return true;
                    }

                });
            }
        }

        private void onNightMode() {
            if (MainSettingsManager.getModeNight(activity)) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                VectrasApp.getApp().setTheme(R.style.AppTheme);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                VectrasApp.getApp().setTheme(R.style.AppTheme);
            }

            activity.finish();
            startActivity(new Intent(activity, SplashActivity.class));
        }

        @Override
        public void onResume() {
            super.onResume();

        }

        @Override
        public void onPause() {
            super.onPause();

        }


        @Override
        public void onCreatePreferences(Bundle bundle, String root_key) {
            // Load the Preferences from the XML file
            setPreferencesFromResource(R.xml.userinterface, root_key);

        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            return true;
        }

    }

    public static class QemuPreferencesFragment extends PreferenceFragmentCompat
            implements Preference.OnPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mHandler = new Handler();

            //Preference prefIfType = findPreference("ifType");
            //if (getArch(activity).equals("ARM64"))
                //if (prefIfType != null) {
                    //prefIfType.setVisible(false);
                //}

            Preference pref = findPreference("vmArch");
            if (pref != null) {
                pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(@NonNull Preference preference,
                                                      Object newValue) {
                        onArch();
                        return true;
                    }

                });
            }

            Preference prefAVX = findPreference("AVX");
            if (!getArch(activity).equals("X86_64"))
                if (prefAVX != null) {
                    prefAVX.setVisible(false);
                }

            if (Objects.equals(getArch(activity), "I386")) { // I386 DOES NOT SUPPORT SHARED FOLDER
                SwitchPreferenceCompat sharedPref = findPreference("sharedFolder");
                sharedPref.setEnabled(false);
                sharedPref.setChecked(false);
                setSharedFolder(activity, false);

            }
        }

        private void onMemory() {
            //findPreference("memory").setEnabled(sp.getBoolean("customMemory", false));
        }

        @Override
        public void onResume() {
            super.onResume();
            onMemory();
        }

        @Override
        public void onPause() {
            super.onPause();
            onMemory();
        }

        private void onArch() {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    activity.finish();
                    startActivity(new Intent(activity, SplashActivity.class));
                }
            }, 300);
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String root_key) {
            // Load the Preferences from the XML file
            setPreferencesFromResource(R.xml.qemu, root_key);
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            return true;
        }

    }

    public static class VncPreferencesFragment extends PreferenceFragmentCompat
            implements Preference.OnPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

        }

        @Override
        public void onResume() {
            super.onResume();

        }

        @Override
        public void onPause() {
            super.onPause();

        }


        @Override
        public void onCreatePreferences(Bundle bundle, String root_key) {
            // Load the Preferences from the XML file
            setPreferencesFromResource(R.xml.vnc, root_key);

        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            if (pref.getKey().equals("app")) {

            }
            return true;
        }

    }

    public static boolean getVncExternal(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getBoolean("vncExternal", false);
    }

    public static void setVncExternal(Activity activity, boolean vncExternal) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("vncExternal", vncExternal);
        edit.apply();
    }

    public static int getOrientationSetting(Activity activity) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        int orientation = prefs.getInt("orientation", 0);
        // UIUtils.log("Getting First time: " + firstTime);
        return orientation;
    }

    public static void setOrientationSetting(Activity activity, int orientation) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt("orientation", orientation);
        edit.apply();
    }

    static boolean getPrio(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getBoolean("HighPrio", false);
    }

    public static void setPrio(Activity activity, boolean flag) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("HighPrio", flag);
        edit.apply();
        // UIUtils.log("Setting First time: ");
    }

    public static boolean getAlwaysShowMenuToolbar(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getBoolean("AlwaysShowMenuToolbar", false);
    }

    public static void setAlwaysShowMenuToolbar(Activity activity, boolean flag) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("AlwaysShowMenuToolbar", flag);
        edit.apply();
        // UIUtils.log("Setting First time: ");
    }

    public static boolean getFullscreen(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getBoolean("ShowFullscreen", true);
    }

    public static void setFullscreen(Activity activity, boolean flag) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("ShowFullscreen", flag);
        edit.apply();
        // UIUtils.log("Setting First time: ");
    }

    public static boolean getDesktopMode(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getBoolean("DesktopMode", false);
    }

    public static void setDesktopMode(Activity activity, boolean flag) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("DesktopMode", flag);
        edit.apply();
        // UIUtils.log("Setting First time: ");
    }

    public static boolean getEnableLegacyFileManager(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getBoolean("EnableLegacyFileManager", false);
    }


    public static void setEnableLegacyFileManager(Activity activity, boolean flag) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("EnableLegacyFileManager", flag);
        edit.apply();
        // UIUtils.log("Setting First time: ");
    }

    public static String getLastDir(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String imagesDir = prefs.getString("lastDir", null);
        return imagesDir;
    }

    public static void setLastDir(Context context, String imagesPath) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("lastDir", imagesPath);
        edit.commit();
    }

    public static String getImagesDir(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String imagesDir = prefs.getString("imagesDir", null);
        return imagesDir;
    }

    public static void setImagesDir(Context context, String imagesPath) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("imagesDir", imagesPath);
        edit.commit();
    }


    public static String getExportDir(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String imagesDir = prefs.getString("exportDir", null);
        return imagesDir;
    }

    public static void setExportDir(Context context, String imagesPath) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("exportDir", imagesPath);
        edit.commit();
    }


    public static String getSharedDir(Context context) {
        String lastDir = Environment.getExternalStorageDirectory().getPath();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("sharedDir", lastDir);
    }

    public static void setSharedDir(Context context, String lastDir) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("sharedDir", lastDir);
        edit.apply();
        // UIUtils.log("Setting First time: ");
    }

    public static int getExitCode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int exitCode = prefs.getInt("exitCode", 1);
        return exitCode;
    }

    public static void setExitCode(Context context, int exitCode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt("exitCode", exitCode);
        edit.commit();
    }

    public static String getControlMode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String controlMode = prefs.getString("controlMode", "D");
        return controlMode;
    }

    public static void setControlMode(Context context, String controlMode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("controlMode", controlMode);
        edit.commit();
    }


    public static void setModeNight(Context context, Boolean nightMode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("modeNight", nightMode);
        edit.commit();
    }

    public static Boolean getModeNight(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("modeNight", false);
    }

    public static void setCusRam(Activity activity, Boolean cusRam) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("customMemory", cusRam);
        edit.apply();
    }

    public static boolean getCusRam(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getBoolean("customMemory", false);
    }

    public static void setIfType(Activity activity, String type) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("ifType", type);
        edit.apply();
    }

    public static String getIfType(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getString("ifType", "");
    }

    public static void setBoot(Activity activity, String boot) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("boot", boot);
        edit.apply();
    }

    public static String getBoot(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getString("boot", "c");
    }

    public static void setVmUi(Activity activity, String vmUi) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("vmUi", vmUi);
        edit.apply();
    }

    public static String getVmUi(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getString("vmUi", "X11");
    }

    public static void setResolution(Activity activity, String RESOLUTION) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("RESOLUTION", RESOLUTION);
        edit.apply();
    }

    public static String getResolution(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getString("RESOLUTION", "800x600x32");
    }

    public static void setSoundCard(Activity activity, String soundCard) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("soundCard", soundCard);
        edit.apply();
    }

    public static String getSoundCard(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getString("soundCard", "None");
    }

    public static void setUsbTablet(Activity activity, boolean UsbTablet) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("UsbTablet", UsbTablet);
        edit.apply();
    }

    public static boolean getUsbTablet(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getBoolean("UsbTablet", false);
    }

    public static void setSharedFolder(Activity activity, boolean enable) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("sharedFolder", enable);
        edit.apply();
    }

    public static boolean getSharedFolder(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getBoolean("sharedFolder", false);
    }

    public static void setArch(Activity activity, String vmArch) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("vmArch", vmArch);
        edit.apply();
    }

    public static String getArch(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getString("vmArch", "X86_64");
    }

    public static void setLang(Activity activity, String language) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("language", language);
        edit.apply();
    }

    public static String getLang(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getString("language", "en");
    }

    public static boolean isFirstLaunch(Activity activity) {
        PackageInfo pInfo = null;

        try {
            pInfo = activity.getPackageManager().getPackageInfo(activity.getClass().getPackage().getName(),
                    PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean firstTime = prefs.getBoolean("firstTime" + pInfo.versionName, true);
        return firstTime;
    }

    public static void setFirstLaunch(Activity activity) {
        PackageInfo pInfo = null;

        try {
            pInfo = activity.getPackageManager().getPackageInfo(activity.getClass().getPackage().getName(),
                    PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("firstTime" + pInfo.versionName, false);
        edit.commit();
    }


    public static boolean getPromptUpdateVersion(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getBoolean("updateVersionPrompt", Config.defaultCheckNewVersion);
    }


    public static void setPromptUpdateVersion(Activity activity, boolean flag) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("updateVersionPrompt", flag);
        edit.apply();
        // UIUtils.log("Setting First time: ");
    }

}
