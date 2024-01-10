/*
Copyright (C) Max Kastanas 2012

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package com.vectras.qemu;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceFragmentCompat;
import com.vectras.vm.R;

import java.util.List;

public class MainSettingsManager extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    public static MainSettingsManager activity;
    public static SharedPreferences sp;

    public static int fragment = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        activity = this;

        fragment = 0;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setTitle("Settings");
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()== android.R.id.home){
            if (fragment == 0) {
                finish();
            } else {
                MainFragment.mainFragment();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public static class MainFragment extends PreferenceFragmentCompat {

        public static MainFragment fr;
        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.headers_preference, rootKey);
            fr = MainFragment.this;
            fragment = 0;
            findPreference("app").setOnPreferenceClickListener(preference -> {
                getFragmentManager().beginTransaction().replace(R.id.settingz,
                        new AppPreferencesFragment()).commit();
                return false;
            });
            findPreference("userinterface").setOnPreferenceClickListener(preference -> {
                getFragmentManager().beginTransaction().replace(R.id.settingz,
                        new UserInterfacePreferencesFragment()).commit();
                return false;
            });
            findPreference("qemu").setOnPreferenceClickListener(preference -> {
                getFragmentManager().beginTransaction().replace(R.id.settingz,
                        new QemuPreferencesFragment()).commit();
                return false;
            });
            findPreference("vnc").setOnPreferenceClickListener(preference -> {
                getFragmentManager().beginTransaction().replace(R.id.settingz,
                        new VncPreferencesFragment()).commit();
                return false;
            });
        }
        public static void mainFragment(){
            fr.getFragmentManager().beginTransaction().replace(R.id.settingz,
                    new MainFragment()).commit();
        }

        @Override
        public void onResume() {
            super.onResume();
            fragment = 0;
        }

        @Override
        public void onPause() {
            super.onPause();
            fragment = 0;
        }

    }

    public static class AppPreferencesFragment extends PreferenceFragmentCompat
            implements OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            sp = getPreferenceScreen().getSharedPreferences();

            Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar);
            activity.setSupportActionBar(toolbar);
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
            toolbar.setTitle("APP SETTINGS");
            fragment = 1;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.settings, rootKey);
        }

        @Override
        public void onResume() {
            super.onResume();
            fragment = 1;
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            sp = sharedPreferences;
        }
    }

    public static class UserInterfacePreferencesFragment extends PreferenceFragmentCompat
            implements OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.userinterface);
            sp = getPreferenceScreen().getSharedPreferences();

            Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar);
            activity.setSupportActionBar(toolbar);
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
            toolbar.setTitle("USER INTERFACE");
            fragment = 2;
        }

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {

        }

        @Override
        public void onResume() {
            super.onResume();
            fragment = 2;
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            sp = sharedPreferences;
        }

    }
    public static class QemuPreferencesFragment extends PreferenceFragmentCompat
            implements Preference.OnPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.qemu);
            sp = getPreferenceScreen().getSharedPreferences();

            Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar);
            activity.setSupportActionBar(toolbar);
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
            toolbar.setTitle("QEMU");
            fragment = 3;
        }

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {

        }

        @Override
        public void onResume() {
            super.onResume();
            onMemory();
            fragment = 3;
        }

        private void onMemory() {
            //findPreference("memory").setEnabled(getCusRam(activity));
        }

        @Override
        public void onPause() {
            super.onPause();
            onMemory();
        }

        @Override
        public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
            onMemory();
            return true;
        }
    }
    public static class VncPreferencesFragment extends PreferenceFragmentCompat
            implements OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.vnc);
            sp = getPreferenceScreen().getSharedPreferences();

            Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar);
            activity.setSupportActionBar(toolbar);
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
            toolbar.setTitle("VNC");
            fragment = 4;
        }

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {

        }

        @Override
        public void onResume() {
            super.onResume();
            fragment = 4;
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            sp = sharedPreferences;
        }

    }
    static String getDNSServer(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getString("dnsServer", Config.defaultDNSServer);
    }

    public static void setDNSServer(Activity activity, String dnsServer) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("dnsServer", dnsServer);
        edit.apply();
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


    public static Boolean getMTTCG(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean MTTCG = prefs.getBoolean("MTTCG", true);
        return MTTCG;
    }

    public static void setMTTCG(Context context, Boolean MTTCG) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("MTTCG", MTTCG);
        edit.commit();
    }

    public static int getCpuCores(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int cpuCores = prefs.getInt("cpuCores", 1);
        return cpuCores;
    }

    public static void setCpuCores(Context context, int cpuCores) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt("cpuCores", cpuCores);
        edit.commit();
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

    public static int getCpuNum(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int cpuNum = Integer.parseInt(prefs.getString("cpuNum", "1"));
        return cpuNum;
    }

    public static void setCpuNum(Context context, String cpuNum) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("cpuNum", cpuNum);
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

    @Override
    public void onBackPressed() {
        if (fragment == 0) {
            finish();
            super.onBackPressed();
        } else {
            MainFragment.mainFragment();
        }
    }

}
