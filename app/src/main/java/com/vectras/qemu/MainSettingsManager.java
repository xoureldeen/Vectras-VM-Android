package com.vectras.qemu;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import androidx.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.vectras.vm.R;
import com.vectras.vm.SplashActivity;
import com.vectras.vm.VectrasApp;
import com.vectras.vm.settings.ThemeActivity;

import java.util.Locale;
import java.util.Objects;

public class MainSettingsManager extends AppCompatActivity
        implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    public static final String TAG = "MainSettingsManager";

    public static final int THEME_DEFAULT = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    public static MainSettingsManager activity;

    private static Handler mHandler;
    public static SharedPreferences sp;
    public static boolean isAllowFirstChangeSubtitle = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        activity = this;

        sp = PreferenceManager.getDefaultSharedPreferences(activity);

        if (getIntent().hasExtra("goto")) {
            if (Objects.equals(getIntent().getStringExtra("goto"), "termuxx11")) {
                Fragment fragment = new com.vectras.vm.x11.LoriePreferences.LoriePreferenceFragment();
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.settingz, fragment)
                        .commit();
            } else if (Objects.equals(getIntent().getStringExtra("goto"), "qemu")) {
                PreferenceFragmentCompat preference = new QemuPreferencesFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.settingz, preference)
                        .commit();
            }
        } else {
            PreferenceFragmentCompat preference = new MainPreferencesFragment();

            // add preference settings
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settingz, preference)
                    .commit();
        }


        // toolbar
        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle bundle = pref.getExtras();

        assert pref.getFragment() != null;
        Fragment fragment = getSupportFragmentManager()
                .getFragmentFactory()
                .instantiate(getClassLoader(), pref.getFragment());
        fragment.setArguments(bundle);

//        fragment.setTargetFragment(caller, 0);

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
            if (MainSettingsManager.isAllowFirstChangeSubtitle) {
                CollapsingToolbarLayout collapsingToolbarLayout =
                        requireActivity().findViewById(R.id.collapsingToolbarLayout);

                if (collapsingToolbarLayout != null) {
                    collapsingToolbarLayout.setSubtitle(getString(R.string.general));
                }
            }
        }

        @Override
        public void onPause() {
            super.onPause();

        }


        @Override
        public void onCreatePreferences(Bundle bundle, String root_key) {
            // Load the Preferences from the XML file
            if (!requireActivity().getIntent().hasExtra("goto"))
                setPreferencesFromResource(R.xml.headers_preference, root_key);
        }

        @Override
        public boolean onPreferenceChange(@NonNull Preference pref, Object newValue) {
            return true;
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if ("userinterface".equals(preference.getKey())) {
                Intent intent = new Intent(getContext(), ThemeActivity.class);
                startActivity(intent);
                //Need to close to avoid lag when this activity recreate.
                requireActivity().finish();
                return true;
            }
            return super.onPreferenceTreeClick(preference);
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

            CollapsingToolbarLayout collapsingToolbarLayout =
                    requireActivity().findViewById(R.id.collapsingToolbarLayout);

            if (collapsingToolbarLayout != null) {
                collapsingToolbarLayout.setSubtitle(getString(R.string.system));
            }

            // Find the ListPreference and set the change listener
            ListPreference languagePref = findPreference("language");
            if (languagePref != null) {
                languagePref.setOnPreferenceChangeListener(this);
            }

//            SwitchPreferenceCompat switchPreferenceCompat = findPreference("updateVersionPrompt");
//            assert switchPreferenceCompat != null;
//            SwitchPreferenceCompat switchJoinBetaChannel = findPreference("checkforupdatesfromthebetachannel");
//            assert switchJoinBetaChannel != null;
//
//            if (!switchPreferenceCompat.isChecked()) {
//                switchJoinBetaChannel.setEnabled(false);
//            }
//
//            switchPreferenceCompat.setOnPreferenceChangeListener((preference, newValue) -> {
//                if (!(Boolean) newValue) {
//                    if (switchJoinBetaChannel.isEnabled())
//                        switchJoinBetaChannel.setEnabled(false);
//                } else {
//                    if (!switchJoinBetaChannel.isEnabled())
//                        switchJoinBetaChannel.setEnabled(true);
//
//                }
//                return true;
//            });

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

            Intent intent = new Intent(requireActivity().getApplicationContext(), SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finishAffinity();
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
                pref.setOnPreferenceChangeListener((preference, newValue) -> {
                    //onNightMode();
                    return true;
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

            Intent intent = new Intent(requireActivity().getApplicationContext(), SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finishAffinity();
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
            CollapsingToolbarLayout collapsingToolbarLayout =
                    requireActivity().findViewById(R.id.collapsingToolbarLayout);

            if (collapsingToolbarLayout != null) {
                collapsingToolbarLayout.setSubtitle(getString(R.string.personalization));
            }
        }

        @Override
        public boolean onPreferenceChange(@NonNull Preference pref, Object newValue) {
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
                pref.setOnPreferenceChangeListener((preference, newValue) -> {
                    onArch();
                    return true;
                });
            }

            Preference prefAVX = findPreference("AVX");
            if (!getArch(activity).equals("X86_64"))
                if (prefAVX != null) {
                    prefAVX.setVisible(false);
                }

//            if (Objects.equals(getArch(activity), "I386")) { // I386 DOES NOT SUPPORT SHARED FOLDER
//                SwitchPreferenceCompat sharedPref = findPreference("sharedFolder");
//                sharedPref.setEnabled(false);
//                sharedPref.setChecked(false);
//                setSharedFolder(activity, false);
//
//            }

            if (!getuseDefaultBios(getActivity())) {
                SwitchPreferenceCompat useUEFIPref = findPreference("useUEFI");
                assert useUEFIPref != null;
                if (!getuseDefaultBios(getActivity())) {
                    useUEFIPref.setChecked(false);
                    setuseUEFI(getActivity(), false);
                }
                useUEFIPref.setEnabled(false);
            }

            SwitchPreferenceCompat useDefaultBiosPref = findPreference("useDefaultBios");
            assert useDefaultBiosPref != null;
            useDefaultBiosPref.setOnPreferenceChangeListener((preference, newValue) -> {
                SwitchPreferenceCompat useUEFIPref = findPreference("useUEFI");
                assert useUEFIPref != null;
                if (!(Boolean) newValue) {
                    if (getuseUEFI(getActivity())) {
                        useUEFIPref.setChecked(false);
                        setuseUEFI(getActivity(), false);
                    }
                    if (useUEFIPref.isEnabled()) {
                        useUEFIPref.setEnabled(false);
                    }
                } else {
                    if (!useUEFIPref.isEnabled()) {
                        useUEFIPref.setEnabled(true);
                    }
                }
                return true;
            });
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
            mHandler.postDelayed(() -> {
                activity.finish();
                startActivity(new Intent(activity, SplashActivity.class));
            }, 300);
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String root_key) {
            // Load the Preferences from the XML file
            setPreferencesFromResource(R.xml.qemu, root_key);
            CollapsingToolbarLayout collapsingToolbarLayout =
                    requireActivity().findViewById(R.id.collapsingToolbarLayout);

            if (collapsingToolbarLayout != null) {
                collapsingToolbarLayout.setSubtitle(getString(R.string.qemu));
            }

            SwitchPreferenceCompat customMemory = findPreference("customMemory");
            assert customMemory != null;
            EditTextPreference memory = findPreference("memory");
            assert memory != null;

            if (!customMemory.isChecked()) {
                memory.setEnabled(false);
            }
            customMemory.setOnPreferenceChangeListener((preference, newValue) -> {
                if (!(Boolean) newValue) {
                    if (memory.isEnabled())
                        memory.setEnabled(false);
                } else {
                    if (!memory.isEnabled())
                        memory.setEnabled(true);

                }
                return true;
            });
        }

        @Override
        public boolean onPreferenceChange(@NonNull Preference pref, Object newValue) {
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
            CollapsingToolbarLayout collapsingToolbarLayout =
                    requireActivity().findViewById(R.id.collapsingToolbarLayout);

            if (collapsingToolbarLayout != null) {
                collapsingToolbarLayout.setSubtitle(getString(R.string.vnc_server));
            }

        }

        @Override
        public boolean onPreferenceChange(@NonNull Preference pref, Object newValue) {
            return true;
        }

    }

    public static boolean getVncExternal(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("vncExternal", false);
    }

    public static void setVncExternal(Context context, boolean vncExternal) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("vncExternal", vncExternal);
        edit.apply();
    }

    public static String getVncExternalDisplay(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("vncExternalDisplay", "1");
    }

    public static void setVncExternalDisplay(Context context, String vncExternalDisplay) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("vncExternalDisplay", vncExternalDisplay);
        edit.apply();
    }

    public static String getVncExternalPassword(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("vncExternalPassword", "");
    }

    public static void setVncExternalPassword(Context context, String vncExternalPassword) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("vncExternalPassword", vncExternalPassword);
        edit.apply();
    }

    public static int getOrientationSetting(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // UIUtils.log("Getting First time: " + firstTime);
        return prefs.getInt("orientation", 0);
    }

    public static void setOrientationSetting(Context context, int orientation) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt("orientation", orientation);
        edit.apply();
    }

    static boolean getPrio(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("HighPrio", false);
    }

    public static void setPrio(Context context, boolean flag) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("HighPrio", flag);
        edit.apply();
        // UIUtils.log("Setting First time: ");
    }

    public static boolean getAlwaysShowMenuToolbar(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("AlwaysShowMenuToolbar", false);
    }

    public static void setAlwaysShowMenuToolbar(Context context, boolean flag) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("AlwaysShowMenuToolbar", flag);
        edit.apply();
        // UIUtils.log("Setting First time: ");
    }

    public static boolean getFullscreen(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("ShowFullscreen", true);
    }

    public static void setFullscreen(Context context, boolean flag) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("ShowFullscreen", flag);
        edit.apply();
        // UIUtils.log("Setting First time: ");
    }

    public static boolean getDesktopMode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("DesktopMode", false);
    }

    public static void setDesktopMode(Context context, boolean flag) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("DesktopMode", flag);
        edit.apply();
        // UIUtils.log("Setting First time: ");
    }

    public static boolean getEnableLegacyFileManager(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("EnableLegacyFileManager", false);
    }


    public static void setEnableLegacyFileManager(Context context, boolean flag) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("EnableLegacyFileManager", flag);
        edit.apply();
        // UIUtils.log("Setting First time: ");
    }

    public static String getLastDir(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("lastDir", null);
    }

    public static void setLastDir(Context context, String imagesPath) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("lastDir", imagesPath);
        edit.apply();
    }

    public static String getImagesDir(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("imagesDir", null);
    }

    public static void setImagesDir(Context context, String imagesPath) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("imagesDir", imagesPath);
        edit.apply();
    }


    public static String getExportDir(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("exportDir", null);
    }

    public static void setExportDir(Context context, String imagesPath) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("exportDir", imagesPath);
        edit.apply();
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
        return prefs.getInt("exitCode", 1);
    }

    public static void setExitCode(Context context, int exitCode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt("exitCode", exitCode);
        edit.apply();
    }

    public static String getControlMode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("controlMode", "D");
    }

    public static void setControlMode(Context context, String controlMode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("controlMode", controlMode);
        edit.apply();
    }


    public static void setModeNight(Context context, Boolean nightMode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("modeNight", nightMode);
        edit.apply();
    }

    public static Boolean getModeNight(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("modeNight", false);
    }

    public static void setCusRam(Context context, Boolean cusRam) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("customMemory", cusRam);
        edit.apply();
    }

    public static boolean getCusRam(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("customMemory", false);
    }

    public static boolean autoCreateDisk(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("autoCreateDisk", false);
    }

    public static boolean useDefaultBios(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("useDefaultBios", true);
    }

    public static boolean useMemoryOvercommit(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("useMemoryOvercommit", true);
    }

    public static boolean useLocalTime(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("useLocalTime", true);
    }

    public static boolean copyFile(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("copyFile", true);
    }

    public static void setIfType(Context context, String type) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("ifType", type);
        edit.apply();
    }

    public static String getIfType(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("ifType", "");
    }

    public static void setBoot(Context context, String boot) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("boot", boot);
        edit.apply();
    }

    public static String getBoot(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("boot", "c");
    }

    public static void setVmUi(Context context, String vmUi) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("vmUi", vmUi);
        edit.apply();
    }

    public static String getVmUi(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("vmUi", "X11");
    }

    public static void setResolution(Context context, String RESOLUTION) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("RESOLUTION", RESOLUTION);
        edit.apply();
    }

    public static String getResolution(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("RESOLUTION", "800x600x32");
    }

    public static void setSoundCard(Context context, String soundCard) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("soundCard", soundCard);
        edit.apply();
    }

    public static String getSoundCard(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("soundCard", "None");
    }

    public static void setUsbTablet(Context context, boolean UsbTablet) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("UsbTablet", UsbTablet);
        edit.apply();
    }

    public static boolean getUsbTablet(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("UsbTablet", false);
    }

    public static void setSharedFolder(Context context, boolean enable) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("sharedFolder", enable);
        edit.apply();
    }

    public static boolean getSharedFolder(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("sharedFolder", false);
    }

    public static void setArch(Context context, String vmArch) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("vmArch", vmArch);
        edit.apply();
    }

    public static String getArch(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("vmArch", "X86_64");
    }

    public static void setLang(Context context, String language) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("language", language);
        edit.apply();
    }

    public static String getLang(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("language", "en");
    }

    public static boolean isFirstLaunch(Context context) {
        PackageInfo pInfo = null;

        try {
            pInfo = context.getPackageManager().getPackageInfo(Objects.requireNonNull(context.getClass().getPackage()).getName(),
                    PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "isFirstLaunch", e);
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        assert pInfo != null;
        return prefs.getBoolean("firstTime" + pInfo.versionName, true);
    }

    public static void setFirstLaunch(Context context) {
        PackageInfo pInfo = null;

        try {
            pInfo = context.getPackageManager().getPackageInfo(Objects.requireNonNull(context.getClass().getPackage()).getName(),
                    PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "setFirstLaunch", e);
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        assert pInfo != null;
        edit.putBoolean("firstTime" + pInfo.versionName, false);
        edit.apply();
    }


    public static boolean getPromptUpdateVersion(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("updateVersionPrompt", Config.defaultCheckNewVersion);
    }


    public static void setPromptUpdateVersion(Context context, boolean flag) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("updateVersionPrompt", flag);
        edit.apply();
        // UIUtils.log("Setting First time: ");
    }

    public static boolean getcheckforupdatesfromthebetachannel(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("checkforupdatesfromthebetachannel", false);
    }


    public static void setcheckforupdatesfromthebetachannel(Context context, boolean flag) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("checkforupdatesfromthebetachannel", flag);
        edit.apply();
        // UIUtils.log("Setting First time: ");
    }

    public static void setsetUpWithManualSetupBefore(Context context, Boolean _boolean) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("setUpWithManualSetupBefore", _boolean);
        edit.apply();
    }

    public static Boolean getsetUpWithManualSetupBefore(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("setUpWithManualSetupBefore", false);
    }

    public static void setSelectedMirror(Context context, int _int) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt("SelectedMirror", _int);
        edit.apply();
    }

    public static int getSelectedMirror(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt("SelectedMirror", 0);
    }

    public static void setDontShowAgainJoinBetaUpdateChannelDialog(Context context, Boolean _boolean) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("dontShowAgainJoinBetaUpdateChannelDialog", _boolean);
        edit.apply();
    }

    public static Boolean getDontShowAgainJoinBetaUpdateChannelDialog(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("dontShowAgainJoinBetaUpdateChannelDialog", false);
    }

    public static void setuseDefaultBios(Context context, Boolean _boolean) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("useDefaultBios", _boolean);
        edit.apply();
    }

    public static Boolean getuseDefaultBios(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("useDefaultBios", true);
    }

    public static void setuseUEFI(Context context, Boolean _boolean) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("useUEFI", _boolean);
        edit.apply();
    }

    public static Boolean getuseUEFI(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("useUEFI", false);
    }

    public static void setSkipVersion(Context context, String version) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("skipVersion", version);
        edit.apply();
    }

    public static String getSkipVersion(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("skipVersion", "");
    }

    public static void setVNCScaleMode(Context context, int mode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt("vncScaleMode", mode);
        edit.apply();
    }

    public static int getVNCScaleMode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt("vncScaleMode", 0);
    }

    public static void setForceRefeshVNCDisplay(Context context, Boolean _boolean) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("forceRefeshVNCDisplay", _boolean);
        edit.apply();
    }

    public static Boolean getForceRefeshVNCDisplay(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("forceRefeshVNCDisplay", true);
    }

    public static void setQuickStart(Context context, Boolean _boolean) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("quickStart", _boolean);
        edit.apply();
    }

    public static Boolean getQuickStart(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("quickStart", true);
    }

    public static void setTheme(Context context, int value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt("theme", value);
        edit.apply();
    }

    public static int getTheme(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt("theme", 0);
    }

    public static void setDynamicColor(Context context, Boolean _boolean) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("dynamicColor", _boolean);
        edit.apply();
    }

    public static Boolean getDynamicColor(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("dynamicColor", true);
    }

    public static void setLikes(Context context, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("likes", value);
        edit.apply();
    }

    public static String getLikes(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("likes", "");
    }

    public static void setViews(Context context, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("views", value);
        edit.apply();
    }

    public static String getViews(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("views", "");
    }

    public static void setSmartSizeCalculation(Context context, Boolean _boolean) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("smartSizeCalculation", _boolean);
        edit.apply();
    }

    public static Boolean getSmartSizeCalculation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("smartSizeCalculation", true);
    }

    public static void setCyclicRedundancyCheck(Context context, Boolean _boolean) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("cyclicRedundancyCheck", _boolean);
        edit.apply();
    }

    public static Boolean getCyclicRedundancyCheck(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("cyclicRedundancyCheck", true);
    }

    public static void setCheckBeforeExtract(Context context, Boolean _boolean) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("checkBeforeExtract", _boolean);
        edit.apply();
    }

    public static Boolean getCheckBeforeExtract(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("checkBeforeExtract", false);
    }

    public static void setRunQemuWithXterm(Context context, Boolean _boolean) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("runQemuWithXterm", _boolean);
        edit.apply();
    }

    public static Boolean getRunQemuWithXterm(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("runQemuWithXterm", true);
    }

    public static void setStandardSetupVersion(Context context, int value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt("standardSetupVersion", value);
        edit.apply();
    }

    public static int getStandardSetupVersion(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt("standardSetupVersion", 0);
    }
}
