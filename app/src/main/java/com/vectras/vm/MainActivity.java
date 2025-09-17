package com.vectras.vm;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;
import static android.content.Intent.ACTION_VIEW;
import static android.os.Build.VERSION.SDK_INT;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.termux.app.TermuxService;

import static com.vectras.vm.VMManager.startFixRomsDataJson;
import static com.vectras.vm.VectrasApp.getApp;
import static com.vectras.vm.utils.LibraryChecker.isPackageInstalled2;
import static com.vectras.vm.utils.UIUtils.UIAlert;

import com.vectras.vm.settings.UpdaterActivity;
import com.vectras.vm.settings.VNCActivity;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.NetworkUtils;
import com.vectras.vm.utils.NotificationUtils;
import com.vectras.vm.utils.PermissionUtils;

import android.app.ActivityManager;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.LoadAdError;
//import com.google.android.gms.ads.MobileAds;
//import com.google.android.gms.ads.interstitial.InterstitialAd;
//import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.elevation.SurfaceColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.termux.app.TermuxActivity;
import com.vectras.qemu.Config;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.MainVNCActivity;
import com.vectras.qemu.utils.RamInfo;
import com.vectras.vm.MainRoms.AdapterMainRoms;
import com.vectras.vm.MainRoms.DataMainRoms;
import com.vectras.vm.adapter.LogsAdapter;
import com.vectras.vm.logger.VectrasStatus;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.LibraryChecker;
import com.vectras.vm.utils.PackageUtils;
import com.vectras.vm.utils.ServiceUtils;
import com.vectras.vterm.Terminal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.vectras.vm.core.ShellExecutor;

import com.vectras.vm.x11.X11Activity;


public class MainActivity extends AppCompatActivity {
    public static String curRomName;
    public static RecyclerView mRVMainRoms;
    public static LinearLayout romsLayout;
    public static AdapterMainRoms mMainAdapter;
    public static JSONArray jArray;
    public static List<DataMainRoms> data;
    public static MainActivity activity;
//    private InterstitialAd mInterstitialAd;
//    private AdRequest adRequest;
    public DrawerLayout mainDrawer;
    private final String TAG = "MainActivity";
    public static /**/ LinearLayout extVncLayout;
    public static AppBarLayout appbar;
    public TextView totalRam;
    public TextView usedRam;
    public TextView freeRam;
    public static LinearLayout linearnothinghere;
    private final Timer _timer = new Timer();
    private AlertDialog alertDialog;
    public static boolean isActivate = false;
    public boolean skipIDEwithARM64DialogInStartVM = false;
    BottomAppBar bottomAppBar;
    AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        activity = this;
        RamInfo.activity = this;
        setContentView(R.layout.activity_main);
        isActivate = true;

        NotificationManager notificationManager = (NotificationManager) activity.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        new LibraryChecker(activity).checkMissingLibraries(activity);

        romsLayout = findViewById(R.id.romsLayout);

        SwipeRefreshLayout refreshRoms = findViewById(R.id.refreshRoms);

        appbar = findViewById(R.id.appbar);
        appbar.setExpanded(false);

        extVncLayout = findViewById(R.id.extVnc);

        linearnothinghere = findViewById(R.id.linearnothinghere);

        TextView tvLogin = findViewById(R.id.tvLogin);
        tvLogin.setText(activity.getString(R.string.port_caption) + ": " + (Config.defaultVNCPort + 5900)/* + "\nPASSWORD --> " + Config.defaultVNCPasswd*/);

        Button stopBtn = findViewById(R.id.stopBtn);
        stopBtn.setOnClickListener(v -> {
            // Stop the service
            MainService.stopService();

            Terminal vterm = new Terminal(activity);
            vterm.executeShellCommand2("killall qemu-system-*", false, activity);

            extVncLayout.setVisibility(View.GONE);
            appbar.setExpanded(false);
        });

        refreshRoms.setOnRefreshListener(() -> {
            loadDataVbi();
            mMainAdapter.notifyItemRangeChanged(0, mMainAdapter.data.size());
            refreshRoms.setRefreshing(false);
        });
        /*bottomAppBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                // Menu items
                int id = item.getItemId();
                if (id == R.id.installRoms) {
                    startActivity(new Intent(activity, RomsManagerActivity.class));
                } else if (id == R.id.arch) {
                    startActivity(new Intent(activity, SetArchActivity.class));
                }

                return false;
            }
        });*/

        Button gotoromstore = findViewById(R.id.gotoromstorebutton);
        gotoromstore.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), RomsManagerActivity.class);
            startActivity(intent);
        });

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd_AppBarBottomActivity);
        fabAdd.setOnClickListener(view -> startActivity(new Intent(activity, SetArchActivity.class)));

        Toolbar mainToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mainToolbar);
        mainDrawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mainDrawer, mainToolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mainDrawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);

        //Setting Navigation View Item Selected Listener to handle the item click of the navigation menu
        // This method will trigger on item Click of navigation menu
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            //Closing drawer on item click
            mainDrawer.closeDrawers();

            //Check to see which item was being clicked and perform appropriate action
            int id = menuItem.getItemId();
            if (id == R.id.navigation_item_info) {
                startActivity(new Intent(activity, AboutActivity.class));
            }
            if (id == R.id.navigation_item_help) {
                String tw = AppConfig.vectrasHelp;
                Intent w = new Intent(ACTION_VIEW);
                w.setData(Uri.parse(tw));
                startActivity(w);
            } else if (id == R.id.navigation_item_website) {
                String tw = AppConfig.vectrasWebsite;
                Intent w = new Intent(ACTION_VIEW);
                w.setData(Uri.parse(tw));
                startActivity(w);
            } else if (id == R.id.navigation_item_import_iso) {
                if (new File(AppConfig.maindirpath + "/drive.iso").exists()) {
                    AlertDialog ad;
                    ad = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
                    ad.setTitle("REPLACE ISO");
                    ad.setMessage("there is iso imported you want to replace it?");
                    ad.setButton(Dialog.BUTTON_POSITIVE, "REPLACE", (dialog, which) -> {
                        Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("*/*");

                        // Optionally, specify a URI for the file that should appear in the
                        // system file picker when it loads.
                        if (SDK_INT >= Build.VERSION_CODES.O) {
                            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                        }

                        startActivityForResult(intent, 1004);
                    });
                    ad.setButton(Dialog.BUTTON_NEGATIVE, "REMOVE", (dialog, which) -> {
                        File isoFile = new File(AppConfig.maindirpath + "/drive.iso");
                        try {
                            if(!isoFile.delete()) Log.e(TAG, "Delete drive.iso failed!");
                        } catch (Exception e) {
                            Log.e(TAG, "Delete drive.iso: ", e);
                        }
                    });
                    ad.show();
                } else {
                    Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");

                    // Optionally, specify a URI for the file that should appear in the
                    // system file picker when it loads.
                    if (SDK_INT >= Build.VERSION_CODES.O) {
                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                    }

                    startActivityForResult(intent, 1004);
                }
            } else if (id == R.id.navigation_item_hdd1) {
                if (new File(AppConfig.maindirpath + "/hdd1.qcow2").exists()) {
                    AlertDialog ad;
                    ad = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
                    ad.setTitle("REPLACE HDD1");
                    ad.setMessage("there is hdd1 imported you want to replace it?");
                    ad.setButton(Dialog.BUTTON_POSITIVE, "REPLACE", (dialog, which) -> {
                        Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("*/*");

                        // Optionally, specify a URI for the file that should appear in the
                        // system file picker when it loads.
                        if (SDK_INT >= Build.VERSION_CODES.O) {
                            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                        }

                        startActivityForResult(intent, 1006);
                    });
                    ad.setButton(Dialog.BUTTON_NEGATIVE, "REMOVE", (dialog, which) -> {
                        File isoFile = new File(AppConfig.maindirpath + "/hdd1.qcow2");
                        try {
                            if(!isoFile.delete()) Log.e(TAG, "Delete hdd1.qcow2 failed!");
                        } catch (Exception e) {
                            Log.e(TAG, "Delete hdd1.qcow2: ", e);
                        }
                    });
                    ad.setButton(Dialog.BUTTON_NEUTRAL, "SHARE", (dialog, which) -> {
                        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                        File fileWithinMyDir = new File(AppConfig.maindirpath + "/hdd1.qcow2");

                        if (fileWithinMyDir.exists()) {
                            intentShareFile.setType("*/*");
                            intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + AppConfig.maindirpath + "/hdd1.qcow2"));

                            intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                                    "Sharing File...");
                            intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File...");

                            startActivity(Intent.createChooser(intentShareFile, "Share File"));
                        }
                    });
                    ad.show();
                } else {
                    Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");

                    // Optionally, specify a URI for the file that should appear in the
                    // system file picker when it loads.
                    if (SDK_INT >= Build.VERSION_CODES.O) {
                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                    }

                    startActivityForResult(intent, 1005);
                }
            } else if (id == R.id.navigation_item_hdd2) {
                if (new File(AppConfig.maindirpath + "/hdd2.qcow2").exists()) {
                    AlertDialog ad;
                    ad = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
                    ad.setTitle("REPLACE HDD2");
                    ad.setMessage("there is hdd2 imported you want to replace it?");
                    ad.setButton(Dialog.BUTTON_POSITIVE, "REPLACE", (dialog, which) -> {
                        Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("*/*");

                        // Optionally, specify a URI for the file that should appear in the
                        // system file picker when it loads.
                        if (SDK_INT >= Build.VERSION_CODES.O) {
                            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                        }

                        startActivityForResult(intent, 1006);
                    });
                    ad.setButton(Dialog.BUTTON_NEGATIVE, "REMOVE", (dialog, which) -> {
                        File isoFile = new File(AppConfig.maindirpath + "/hdd2.qcow2");
                        try {
                            if(!isoFile.delete()) Log.e(TAG, "Delete hdd2.qcow2 failed!");
                        } catch (Exception e) {
                            Log.e(TAG, "Delete hdd2.qcow2: ", e);
                        }
                    });
                    ad.setButton(Dialog.BUTTON_NEUTRAL, "SHARE", (dialog, which) -> {
                        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                        File fileWithinMyDir = new File(AppConfig.maindirpath + "/hdd2.qcow2");

                        if (fileWithinMyDir.exists()) {
                            intentShareFile.setType("*/*");
                            intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + AppConfig.maindirpath + "/hdd2.qcow2"));

                            intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                                    "Sharing File...");
                            intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File...");

                            startActivity(Intent.createChooser(intentShareFile, "Share File"));
                        }
                    });
                    ad.show();
                } else {
                    Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");

                    // Optionally, specify a URI for the file that should appear in the
                    // system file picker when it loads.
                    if (SDK_INT >= Build.VERSION_CODES.O) {
                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                    }

                    startActivityForResult(intent, 1006);
                }
            } else if (id == R.id.navigation_item_desktop) {
                launchX11(true);
            } else if (id == R.id.navigation_item_terminal) {
                /*com.vectras.vterm.TerminalBottomSheetDialog VTERM = new com.vectras.vterm.TerminalBottomSheetDialog(activity);
                VTERM.showVterm();*/
                startActivity(new Intent(activity, TermuxActivity.class));
            } else if (id == R.id.navigation_item_view_logs) {
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
                View view = activity.getLayoutInflater().inflate(R.layout.bottomsheetdialog_logger, null);
                bottomSheetDialog.setContentView(view);
                bottomSheetDialog.show();

                final String CREDENTIAL_SHARED_PREF = "settings_prefs";
                Timer _timer = new Timer();
                TimerTask t;

                LinearLayoutManager layoutManager = new LinearLayoutManager(getApp());
                LogsAdapter mLogAdapter = new LogsAdapter(layoutManager, getApp());
                RecyclerView logList = view.findViewById(R.id.recyclerLog);
                logList.setAdapter(mLogAdapter);
                logList.setLayoutManager(layoutManager);
                mLogAdapter.scrollToLastPosition();
                try {
                    Process process = Runtime.getRuntime().exec("logcat -e");
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));
                    Process process2 = Runtime.getRuntime().exec("logcat -w");
                    BufferedReader bufferedReader2 = new BufferedReader(
                            new InputStreamReader(process2.getInputStream()));

                    t = new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(() -> {
                                try {
                                    if (bufferedReader.readLine() != null || bufferedReader2.readLine() != null) {
                                        String logLine = bufferedReader.readLine();
                                        String logLine2 = bufferedReader2.readLine();
                                        VectrasStatus.logError("<font color='red'>[E] " + logLine + "</font>");
                                        VectrasStatus.logError("<font color='#FFC107'>[W] " + logLine2 + "</font>");
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        }
                    };
                    _timer.scheduleAtFixedRate(t, (int) (0), (int) (100));
                } catch (IOException e) {
                    Log.e(TAG, "Log: ", e);
                }
            } else if (id == R.id.navigation_item_settings) {
                startActivity(new Intent(activity, MainSettingsManager.class));
            } else if (id == R.id.navigation_item_store) {
                startActivity(new Intent(activity, StoreActivity.class));
            } else if (id == R.id.navigation_data_explorer) {
                startActivity(new Intent(activity, DataExplorerActivity.class));
            } else if (id == R.id.navigation_item_donate) {
                String tw = "https://www.patreon.com/VectrasTeam";
                Intent w = new Intent(ACTION_VIEW);
                w.setData(Uri.parse(tw));
                startActivity(w);
            } else if (id == R.id.navigation_item_get_rom) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), RomsManagerActivity.class);
                startActivity(intent);
            } else if (id == R.id.mini_tools) {
                Intent intent = new Intent();
                intent.setClass(activity, Minitools.class);
                startActivity(intent);
            }
            return false;
        });

        getWindow().setNavigationBarColor(SurfaceColors.SURFACE_2.getColor(this));

//        adRequest = new AdRequest.Builder().build();

        //AdView mAdView = findViewById(R.id.adView);
        //adRequest = new AdRequest.Builder().build();
        //mAdView.loadAd(adRequest);

//        MobileAds.initialize(this, initializationStatus -> {
//        });

        DialogUtils.joinTelegram(activity);

        totalRam = findViewById(R.id.totalRam);
        usedRam = findViewById(R.id.usedRam);
        freeRam = findViewById(R.id.freeRam);

        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);

        long freeMem = mi.availMem / 1048576L;
        long totalMem = mi.totalMem / 1048576L;
        long usedMem = totalMem - freeMem;
        int freeRamInt = safeLongToInt(freeMem);
        int totalRamInt = safeLongToInt(totalMem);

        totalRam = findViewById(R.id.totalRam);
        usedRam = findViewById(R.id.usedRam);
        freeRam = findViewById(R.id.freeRam);


        TextView tvIsRunning = findViewById(R.id.tvIsRunning);


        String vectrasMemory = String.valueOf(RamInfo.vectrasMemory(this));
        TimerTask t =
                new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(
                                () -> {
                                    if (ServiceUtils.isServiceRunning(MainActivity.this, MainService.class))
                                        tvIsRunning.setText(R.string.running);
                                    else tvIsRunning.setText(R.string.stopped);

                                    ActivityManager.MemoryInfo miI =
                                            new ActivityManager.MemoryInfo();
                                    ActivityManager activityManagerr =
                                            (ActivityManager)
                                                    getSystemService(ACTIVITY_SERVICE);
                                    activityManagerr.getMemoryInfo(miI);
                                    long freeMemory = miI.availMem / 1048576L;
                                    long totalMemory = miI.totalMem / 1048576L;
                                    long usedMemory = totalMemory - freeMemory;

                                    totalRam.setText(
                                            activity.getResources()
                                                    .getString(R.string.total_memory)
                                                    + " "
                                                    + totalMemory
                                                    + " MB");
                                    usedRam.setText(
                                            activity.getResources()
                                                    .getString(R.string.used_memory)
                                                    + " "
                                                    + usedMemory
                                                    + " MB");
                                    freeRam.setText(
                                            activity.getResources()
                                                    .getString(R.string.free_memory)
                                                    + " "
                                                    + freeMemory
                                                    + " MB ("
                                                    + vectrasMemory
                                                    + " "
                                                    + activity.getResources()
                                                    .getString(R.string.used)
                                                    + ")");
                                    LinearProgressIndicator progressBar = findViewById(R.id.progressBar);
                                    progressBar.setMax((int) totalMemory);
                                    if (SDK_INT >= Build.VERSION_CODES.N) {
                                        progressBar.setProgress((int) usedMemory, true);
                                    } else {
                                        progressBar.setProgress((int) usedMemory);
                                    }
                                });
                    }
                };
        _timer.scheduleAtFixedRate(t, (int) (0), (int) (1000));
        ShellExecutor shellExec = new ShellExecutor();
        shellExec.exec(TermuxService.PREFIX_PATH + "/bin/termux-x11 :0");

        TextView qemuVersion = findViewById(R.id.qemuVersion);

        setupBottomAppBar();

        if (MainSettingsManager.getPromptUpdateVersion(activity))
            updateApp();

        String command = "qemu-system-x86_64 --version";
        new Terminal(activity).extractQemuVersion(command, false, activity, (output, errors) -> {
            if (errors.isEmpty()) {
                String versionStr = "Unknown";
                if (output.equals("8.2.1"))
                    versionStr = output + " - 3dfx";
                Log.d(TAG, "QEMU Version: " + versionStr);
                qemuVersion.setText(versionStr);
            } else {
                Log.e(TAG, "Errors: " + errors);
            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int spanCount = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ? 3 : 2;
        mRVMainRoms.setLayoutManager(new GridLayoutManager(this, spanCount));
    }

    @Override
    public void onDestroy() {
        isActivate = false;
        super.onDestroy();
    }

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    private void updateApp() {
        int versionCode = PackageUtils.getThisVersionCode(getApplicationContext());
        String versionName = PackageUtils.getThisVersionName(getApplicationContext());

        RequestNetwork requestNetwork = new RequestNetwork(this);
        RequestNetwork.RequestListener requestNetworkListener = new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
                View _update = findViewById(R.id.update);

                if (!response.isEmpty()) {
                    try {
                        final JSONObject obj = new JSONObject(response);
                        String versionNameonUpdate;
                        int versionCodeonUpdate;

                        if (MainSettingsManager.getcheckforupdatesfromthebetachannel(MainActivity.this)) {
                            versionNameonUpdate = obj.getString("versionNameBeta");
                            versionCodeonUpdate = obj.getInt("versionCodeBeta");
                        } else {
                            versionNameonUpdate = obj.getString("versionName");
                            versionCodeonUpdate = obj.getInt("versionCode");
                        }

                        if (versionCode < versionCodeonUpdate || !versionNameonUpdate.equals(versionName)) {
                            _update.setVisibility(View.VISIBLE);
                        } else {
                            _update.setVisibility(View.GONE);
                        }
                    } catch (JSONException e) {
                        _update.setVisibility(View.GONE);
                    }
                } else {
                    _update.setVisibility(View.GONE);
                }
            }

            @Override
            public void onErrorResponse(String tag, String message) {

            }
        };

        requestNetwork.startRequestNetwork(RequestNetworkController.GET,AppConfig.updateJson,"maincheckupdate",requestNetworkListener);
    }

    private void loadDataVbi() {

        if (FileUtils.isFileExists(AppConfig.romsdatajson)) {
            if (!VMManager.isRomsDataJsonValid(true, MainActivity.this)) {
                DialogUtils.twoDialog(this,
                        getString(R.string.problem_has_been_detected),
                        getString(R.string.vm_list_data_is_corrupted_content),
                        getString(R.string.continuetext),
                        getString(R.string.cancel),
                        true,
                        R.drawable.build_24px,
                        true,
                        () -> {
                            FileUtils.moveAFile(AppConfig.maindirpath + "roms-data.json", AppConfig.maindirpath + "roms-data.old.json");
                            FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", "[]");
                            startFixRomsDataJson();
                        },
                        null,
                        null);
            }
        } else {
            FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", "[]");
        }

        data = new ArrayList<>();

        try {

            jArray = new JSONArray(FileUtils.readFromFile(MainActivity.activity, new File(AppConfig.maindirpath
                    + "roms-data.json")));

            // Extract data from json and store into ArrayList as class objects
            for (int i = 0; i < jArray.length(); i++) {
                JSONObject json_data = jArray.getJSONObject(i);
                DataMainRoms romsMainData = new DataMainRoms();
                romsMainData.itemName = json_data.getString("imgName");
                romsMainData.itemIcon = json_data.getString("imgIcon");
                try {
                    romsMainData.itemArch = json_data.getString("imgArch");
                } catch (JSONException ignored) {
                    romsMainData.itemArch = "unknown";
                }
                romsMainData.itemPath = json_data.getString("imgPath");
                try {
                    romsMainData.imgCdrom = json_data.getString("imgCdrom");
                } catch (JSONException ignored) {
                    romsMainData.imgCdrom = "";
                }
                try {
                    romsMainData.vmID = json_data.getString("vmID");
                } catch (JSONException ignored) {
                    romsMainData.vmID = "";
                }
                try {
                    romsMainData.qmpPort = json_data.getInt("qmpPort");
                } catch (JSONException ignored) {
                    romsMainData.qmpPort = 0;
                }
                try {
                    romsMainData.itemDrv1 = json_data.getString("imgDrv1");
                } catch (JSONException ignored) {
                    romsMainData.itemDrv1 = "";
                }
                romsMainData.itemExtra = json_data.getString("imgExtra");
                //try {
                //if (json_data.getString("imgArch").equals(MainSettingsManager.getArch(MainActivity.activity)))
                data.add(romsMainData);
                //} catch (JSONException ignored) {
                //data.add(romsMainData);
                //}
            }

            // Setup and Handover data to recyclerview
            mRVMainRoms = findViewById(R.id.mRVMainRoms);
            mMainAdapter = new AdapterMainRoms(this, data);
            mRVMainRoms.setAdapter(mMainAdapter);
            int spanCount = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 3 : 2;
            mRVMainRoms.setLayoutManager(new GridLayoutManager(this, spanCount));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mdatasize();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home_toolbar_menu, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Menu items
        int id = item.getItemId();
        if (id == R.id.info) {
            appbar = findViewById(R.id.appbar);
            if (appbar.getTop() < 0)
                appbar.setExpanded(true);
            else
                appbar.setExpanded(false);

        } else if (id == R.id.shutdown) {
            alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
            alertDialog.setTitle(getResources().getString(R.string.do_you_want_to_kill_all_qemu_processes));
            alertDialog.setMessage(getResources().getString(R.string.all_running_vms_will_be_forcibly_shut_down));
            alertDialog.setCancelable(true);
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.kill_all), (dialog, which) -> {
                VMManager.killallqemuprocesses(getApplicationContext());
            });
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.cancel), (dialog, which) -> {

            });
            alertDialog.show();
        } else if (id == R.id.backtothedisplay) {
            if (VMManager.isQemuRunning(activity)) {
                if (MainSettingsManager.getVmUi(activity).equals("VNC"))
                    activity.startActivity(new Intent(activity, MainVNCActivity.class));
                else if (MainSettingsManager.getVmUi(activity).equals("X11"))
                    launchX11(false);
            } else {
                Toast.makeText(getApplicationContext(), activity.getResources().getString(R.string.there_is_nothing_here_because_there_is_no_vm_running), Toast.LENGTH_LONG).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public static void startVM(String vmName, String env, String itemExtra, String itemPath) {

//        timerTask = new TimerTask() {
//            @Override
//            public void run() {
//                ActivityManager manager = (ActivityManager) activity.getSystemService(ACTIVITY_SERVICE);
//                for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//                    if (!AudioStreamService.class.getName().equals(service.service.getClassName())) {
//                        if (SDK_INT >= Build.VERSION_CODES.O) {
//                            activity.startForegroundService(new Intent(activity, AudioStreamService.class));
//                        } else {
//                            activity.startService(new Intent(activity, AudioStreamService.class));
//                        }
//                    }
//                }
//            }
//        };
//        timer.schedule(timerTask, 5000);

        File romDir = new File(Config.getCacheDir() + "/" + Config.vmID);
        if(!romDir.mkdirs()) {
            DialogUtils.oneDialog(activity, activity.getString(R.string.problem_has_been_detected), activity.getString(R.string.vm_cache_dir_failed_to_create_content) + " " + activity.getResources().getString(R.string.reason) + ": " + VMManager.latestUnsafeCommandReason, activity.getString(R.string.ok), true, R.drawable.warning_48px, true, null, null);
            return;
        }

        if (!VMManager.isthiscommandsafe(env, activity.getApplicationContext())) {
            DialogUtils.oneDialog(activity, activity.getString(R.string.problem_has_been_detected), activity.getString(R.string.harmful_command_was_detected) + " " + activity.getResources().getString(R.string.reason) + ": " + VMManager.latestUnsafeCommandReason, activity.getString(R.string.ok), true, R.drawable.verified_user_24px, true, null, null);
            return;
        }

        VMManager.lastQemuCommand = env;

        if (VMManager.isThisVMRunning(activity, itemExtra, itemPath)) {
            Toast.makeText(activity, "This VM is already running.", Toast.LENGTH_LONG).show();
            if (MainSettingsManager.getVmUi(activity).equals("VNC"))
                activity.startActivity(new Intent(activity, MainVNCActivity.class));
            else if (MainSettingsManager.getVmUi(activity).equals("X11"))
                activity.launchX11(false);
            return;
        }

        if (AppConfig.getSetupFiles().contains("arm") && !AppConfig.getSetupFiles().contains("arm64")) {
            if (env.contains("tcg,thread=multi")) {
                DialogUtils.twoDialog(activity, activity.getResources().getString(R.string.problem_has_been_detected), activity.getResources().getString(R.string.can_not_use_mttcg), activity.getString(R.string.ok), activity.getString(R.string.cancel), true, R.drawable.warning_48px, true,
                        () -> startVM(vmName, env.replace("tcg,thread=multi", "tcg,thread=single"), itemExtra, itemPath), null, null);
                return;
            }
        }

        if (MainSettingsManager.getArch(activity).equals("ARM64") && MainSettingsManager.getIfType(activity).equals("ide") && !activity.skipIDEwithARM64DialogInStartVM) {
            DialogUtils.twoDialog(activity, activity.getString(R.string.problem_has_been_detected), activity.getString(R.string.you_cannot_use_IDE_hard_drive_type_with_ARM64), activity.getString(R.string.continuetext), activity.getString(R.string.cancel), true, R.drawable.warning_48px, true,
                    () -> {
                        activity.skipIDEwithARM64DialogInStartVM = true;
                        startVM(vmName, env, itemExtra, itemPath);
                    }, null, null);
            return;
        } else if (activity.skipIDEwithARM64DialogInStartVM) {
            activity.skipIDEwithARM64DialogInStartVM = false;
        }

        if (MainSettingsManager.getSharedFolder(activity) && MainSettingsManager.getArch(activity).equals("I386")) {
            Toast.makeText(activity, R.string.shared_folder_is_not_used_because_i386_does_not_support_it, Toast.LENGTH_LONG).show();
        }

        if (MainSettingsManager.getVncExternal(activity) &&
                NetworkUtils.isPortOpen("localhost", Config.defaultVNCPort + 5900, 500)) {
            DialogUtils.twoDialog(activity, activity.getString(R.string.problem_has_been_detected),
                    activity.getString(R.string.the_vnc_server_port_you_set_is_currently_in_use_by_other),
                    activity.getString(R.string.go_to_settings),
                    activity.getString(R.string.close),
                    true, R.drawable.warning_48px, true,
                    () -> activity.startActivity(new Intent(activity, VNCActivity.class)),
                    null,
                    null);
            return;
        }

        activity.showProgressDialog(activity.getString(R.string.booting_up));
        Handler handler = new Handler();
        handler.postDelayed(
                () -> {
                    if (ServiceUtils.isServiceRunning(activity, MainService.class)) {
                        MainService.startCommand(env, activity);
                    } else {
                        Intent serviceIntent = new Intent(activity, MainService.class);
                        MainService.env = env;
                        MainService.CHANNEL_ID = vmName;
                        if (SDK_INT >= Build.VERSION_CODES.O) {
                            activity.startForegroundService(serviceIntent);
                        } else {
                            activity.startService(serviceIntent);
                        }
                    }


                    if (MainSettingsManager.getVmUi(activity).equals("VNC")) {
                        if (MainSettingsManager.getVncExternal(MainActivity.activity)) {
                            extVncLayout.setVisibility(View.VISIBLE);
                            appbar.setExpanded(true);
                            activity.progressDialog.dismiss();
                        } else {
                            Handler handler1 = new Handler();
                            handler1.postDelayed(
                                    new Runnable() {
                                        public void run() {
                                            MainVNCActivity.started = true;
                                            activity.startActivity(
                                                    new Intent(
                                                            activity, MainVNCActivity.class));
                                            activity.progressDialog.dismiss();
                                        }
                                    },
                                    2000);
                        }
                    } else if (MainSettingsManager.getVmUi(activity).equals("SPICE")) {
                        // activity.startActivity(new Intent(activity,
                        // RemoteCanvasActivity.class));
                    } else if (MainSettingsManager.getVmUi(activity).equals("X11")) {
                        Handler handler1 = new Handler();
                        handler1.postDelayed(
                                new Runnable() {
                                    public void run() {
                                        activity.progressDialog.dismiss();
                                        activity.launchX11(false);
                                    }
                                },
                                3000);
                    }
                },
                2000);
        String[] params = env.split("\\s+");
        VectrasStatus.logInfo("Params:");
        Log.d("HomeStartVM", "Params:");
        for (int i = 0; i < params.length; i++) {
            VectrasStatus.logInfo(i + ": " + params[i]);
            Log.d("HomeStartVM", i + ": " + params[i]);
        }

    }

    public void onResume() {
        super.onResume();
        checkpermissions();
        Log.d(TAG, "onResume");
        Config.ui = MainSettingsManager.getVmUi(activity);
        Config.defaultVNCPort = Integer.parseInt(MainSettingsManager.getVncExternalDisplay(activity));

        //TEMPORARY FIX FOR VNC CLOSES
        //TODO: FIND FIX FOR CRASHING
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        if (MainSettingsManager.getVmUi(activity).equals("VNC") && FileUtils.isFileExists(Config.getLocalQMPSocketPath()) && MainVNCActivity.started)
            startActivity(new Intent(activity, MainVNCActivity.class));
    }

    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        if (!MainSettingsManager.getVncExternal(activity))
            NotificationUtils.clearAll(this);
        loadDataVbi();
        Config.ui = MainSettingsManager.getVmUi(activity);

        TextView tvQemuArch = findViewById(R.id.qemuArch);
        tvQemuArch.setText(MainSettingsManager.getArch(activity));

        //TEMPORARY FIX FOR VNC CLOSES
        //TODO: FIND FIX FOR CRASHING
        //if (MainSettingsManager.getVmUi(activity).equals("VNC") && MainVNCActivity.started)
        //startActivity(new Intent(activity, MainVNCActivity.class));

//        InterstitialAd.load(this, "ca-app-pub-3568137780412047/7745973511", adRequest,
//                new InterstitialAdLoadCallback() {
//                    @Override
//                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
//                        // The mInterstitialAd reference will be null until
//                        // an ad is loaded.
//                        mInterstitialAd = interstitialAd;
//                        Log.i("MainActivity", "onAdLoaded");
//                    }
//
//                    @Override
//                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
//                        // Handle the error
//                        Log.d("MainActivity", loadAdError.toString());
//                        mInterstitialAd = null;
//                    }
//                });
//        if (mInterstitialAd != null) {
//            mInterstitialAd.show(this);
//        } else {
//            Log.d("TAG", "The interstitial ad wasn't ready yet.");
//        }

        if (!AppConfig.pendingCommand.isEmpty()) {
            if (!VMManager.isthiscommandsafe(AppConfig.pendingCommand, getApplicationContext())) {
                AppConfig.pendingCommand = "";
                DialogUtils.oneDialog(activity, getString(R.string.problem_has_been_detected), getString(R.string.harmful_command_was_detected) + " " + activity.getResources().getString(R.string.reason) + ": " + VMManager.latestUnsafeCommandReason, getString(R.string.ok), true, R.drawable.verified_user_24px, true, null, null);
            } else {
                if (AppConfig.pendingCommand.startsWith("qemu-img")) {
                    if (!VMManager.isthiscommandsafeimg(AppConfig.pendingCommand, getApplicationContext())) {
                        DialogUtils.oneDialog(activity, getString(R.string.problem_has_been_detected), getString(R.string.size_too_large_try_qcow2_format), getString(R.string.ok), true, R.drawable.warning_48px, true, null, null);
                    } else {
                        Terminal _vterm = new Terminal(MainActivity.this);
                        _vterm.executeShellCommand2(AppConfig.pendingCommand, false, MainActivity.activity);
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.done), Toast.LENGTH_LONG).show();
                    }
                } else {
                    StartVM.cdrompath = "";
                    String env = StartVM.env(MainActivity.activity, AppConfig.pendingCommand, "", "1");
                    MainActivity.startVM("Quick run", env, AppConfig.pendingCommand, "");
                    VMManager.lastQemuCommand = AppConfig.pendingCommand;
                }
            }
            AppConfig.pendingCommand = "";
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent ReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, ReturnedIntent);
        if (requestCode == 1004 && resultCode == RESULT_OK) {
            Uri content_describer = ReturnedIntent.getData();
            File selectedFilePath = new File(getPath(content_describer));
            ProgressBar loading = findViewById(R.id.loading);
            if (selectedFilePath.toString().endsWith(".iso")) {
                loading.setVisibility(View.VISIBLE);
                new Thread(() -> {
                    FileInputStream File;
                    try {
                        assert content_describer != null;
                        File = (FileInputStream) getContentResolver().openInputStream(content_describer);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        try {
                            try (OutputStream out = new FileOutputStream(AppConfig.maindirpath + "/drive.iso")) {
                                // Transfer bytes from in to out
                                byte[] buf = new byte[1024];
                                int len;
                                while (true) {
                                    assert File != null;
                                    if (!((len = File.read(buf)) > 0)) break;
                                    out.write(buf, 0, len);
                                }
                            }
                        } finally {
                            Runnable runnable = () -> loading.setVisibility(View.GONE);
                            activity.runOnUiThread(runnable);
                            assert File != null;
                            File.close();
                        }
                    } catch (IOException e) {
                        Runnable runnable = () -> {
                            loading.setVisibility(View.GONE);
                            UIAlert(activity, e.toString(), "error");
                        };
                        activity.runOnUiThread(runnable);
                    }
                }).start();
            } else
                UIAlert(activity, "please select iso file", "INVALID FILE");
        } else if (requestCode == 1005 && resultCode == RESULT_OK) {
            Uri content_describer = ReturnedIntent.getData();
            ProgressBar loading = findViewById(R.id.loading);
            loading.setVisibility(View.VISIBLE);
            new Thread(() -> {
                FileInputStream File;
                try {
                    assert content_describer != null;
                    File = (FileInputStream) getContentResolver().openInputStream(content_describer);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                try {
                    try {
                        try (OutputStream out = new FileOutputStream(AppConfig.maindirpath + "/hdd1.qcow2")) {
                            // Transfer bytes from in to out
                            byte[] buf = new byte[1024];
                            int len;
                            while (true) {
                                assert File != null;
                                if (!((len = File.read(buf)) > 0)) break;
                                out.write(buf, 0, len);
                            }
                        }
                    } finally {
                        Runnable runnable = () -> loading.setVisibility(View.GONE);
                        activity.runOnUiThread(runnable);
                        assert File != null;
                        File.close();
                    }
                } catch (IOException e) {
                    Runnable runnable = () -> {
                        loading.setVisibility(View.GONE);
                        UIAlert(activity, e.toString(), "error");
                    };
                    activity.runOnUiThread(runnable);
                }
            }).start();
        } else if (requestCode == 1006 && resultCode == RESULT_OK) {
            Uri content_describer = ReturnedIntent.getData();
            ProgressBar loading = findViewById(R.id.loading);
            loading.setVisibility(View.VISIBLE);
            new Thread(() -> {
                FileInputStream File = null;
                try {
                    assert content_describer != null;
                    File = (FileInputStream) getContentResolver().openInputStream(content_describer);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                try {
                    try {
                        try (OutputStream out = new FileOutputStream(AppConfig.maindirpath + "/hdd2.qcow2")) {
                            // Transfer bytes from in to out
                            byte[] buf = new byte[1024];
                            int len;
                            while (true) {
                                assert File != null;
                                if (!((len = File.read(buf)) > 0)) break;
                                out.write(buf, 0, len);
                            }
                        }
                    } finally {
                        Runnable runnable = () -> loading.setVisibility(View.GONE);
                        activity.runOnUiThread(runnable);
                        assert File != null;
                        File.close();
                    }
                } catch (IOException e) {
                    Runnable runnable = () -> {
                        loading.setVisibility(View.GONE);
                        UIAlert(activity, e.toString(), "error");
                    };
                    activity.runOnUiThread(runnable);
                }
            }).start();
        } else if (requestCode == 122 && resultCode == RESULT_OK) {
            Uri content_describer = ReturnedIntent.getData();
            File selectedFilePath = new File(getPath(content_describer));
            ProgressBar loading = findViewById(R.id.loading);
            loading.setVisibility(View.VISIBLE);
            new Thread(() -> {
                FileInputStream File;
                try {
                    assert content_describer != null;
                    File = (FileInputStream) getContentResolver().openInputStream(content_describer);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                try {
                    try {
                        File romDir = new File(AppConfig.maindirpath + curRomName + "/");
                        if (!romDir.exists()) {
                            if(!romDir.mkdirs()) return;
                        }
                        try (OutputStream out = new FileOutputStream(AppConfig.maindirpath + curRomName + "/" + "drv1-" + selectedFilePath.getName())) {
                            // Transfer bytes from in to out
                            byte[] buf = new byte[1024];
                            int len;
                            while (true) {
                                assert File != null;
                                if (!((len = File.read(buf)) > 0)) break;
                                out.write(buf, 0, len);
                            }
                        }
                    } finally {
                        Runnable runnable = () -> loading.setVisibility(View.GONE);
                        activity.runOnUiThread(runnable);
                        assert File != null;
                        File.close();
                    }
                } catch (IOException e) {
                    Runnable runnable = () -> {
                        loading.setVisibility(View.GONE);
                        UIAlert(activity, e.toString(), "error");
                    };
                    activity.runOnUiThread(runnable);
                }
            }).start();
        }
    }

    public String getPath(Uri uri) {
        return FileUtils.getPath(this, uri);
    }

    private void mdatasize() {
        if (MainActivity.data.isEmpty()) {
            linearnothinghere.setVisibility(View.VISIBLE);
        } else {
            linearnothinghere.setVisibility(View.GONE);
        }
    }

    public static void mdatasize2() {
        if (MainActivity.data.isEmpty()) {
            linearnothinghere.setVisibility(View.VISIBLE);
        } else {
            linearnothinghere.setVisibility(View.GONE);
        }
    }

    private void checkpermissions() {
        if (PermissionUtils.storagepermission(activity, true)) {
            loadDataVbi();
            if (DeviceUtils.isStorageLow(this)) {
                DialogUtils.oneDialog(this,
                        getResources().getString(R.string.oops),
                        getResources().getString(R.string.very_low_available_storage_space_content),
                        getResources().getString(R.string.ok),
                        true,
                        R.drawable.warning_48px,
                        true,
                        null,
                        () -> {
                            if (DeviceUtils.isStorageLow(this)) finish();
                        });
            }
        }
    }

    private void setupBottomAppBar() {
        bottomAppBar = findViewById(R.id.bottomAppBar);
        bottomAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.update) {
                //updateApp(true);
                startActivity(new Intent(this, UpdaterActivity.class));
            } else if (item.getItemId() == R.id.shutdown) {
                VMManager.requestKillAllQemuProcess(activity, null);
            } else if (item.getItemId() == R.id.backtothedisplay) {
                if (MainSettingsManager.getVmUi(activity).equals("VNC")) {
                    startActivity(new Intent(activity, MainVNCActivity.class));
                } else if (MainSettingsManager.getVmUi(activity).equals("X11")) {
                    launchX11(false);
                }
            } else if (item.getItemId() == R.id.importrom) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), CustomRomActivity.class);
                intent.putExtra("importcvbinow", "");
                startActivity(intent);
            }
            return false;
        });
        View _update = findViewById(R.id.update);
        _update.setVisibility(View.GONE);
    }

    private void launchX11(boolean isKillXFCE) {
        if (SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            DialogUtils.oneDialog(activity, getString(R.string.x11_feature_not_supported), getString(R.string.the_x11_feature_is_currently_not_supported_on_android_14_and_above_please_use_a_device_with_android_13_or_below_for_x11_functionality), getString(R.string.ok), true, R.drawable.error_96px, true, null, null);
        } else {
            // XFCE4 meta-package
            String xfce4Package = "xfce4";

            // Check if XFCE4 is installed
            isPackageInstalled2(activity, xfce4Package, (output, errors) -> {
                boolean isInstalled = false;

                // Check if the package exists in the installed packages output
                if (output != null) {
                    Set<String> installedPackages = new HashSet<>();
                    for (String installedPackage : output.split("\n")) {
                        installedPackages.add(installedPackage.trim());
                    }

                    isInstalled = installedPackages.contains(xfce4Package.trim());
                }

                // If not installed, show a dialog to install it
                if (!isInstalled) {
                    DialogUtils.twoDialog(activity, "Install XFCE4", "XFCE4 is not installed. Would you like to install it?", getString(R.string.install), getString(R.string.cancel), true, R.drawable.desktop_24px, true,
                            () -> {
                                String installCommand = "apk add " + xfce4Package;
                                new Terminal(activity).executeShellCommand(installCommand, true, true, activity);
                            }, null, null);
                } else {
                    if (isKillXFCE)
                        new Terminal(activity).executeShellCommand2("killall xfce4-session", false, activity);
                    startActivity(new Intent(activity, X11Activity.class));
                    new Terminal(activity).executeShellCommand2("xfce4-session", false, MainActivity.activity);
                }
            });

        }
    }

    private void showProgressDialog(String _content) {
        View progressView = LayoutInflater.from(activity).inflate(R.layout.dialog_progress_style, null);
        TextView progress_text = progressView.findViewById(R.id.progress_text);
        progress_text.setText(_content);
        progressDialog = new MaterialAlertDialogBuilder(activity, R.style.CenteredDialogTheme)
                .setView(progressView)
                .setCancelable(false)
                .create();

        progressDialog.show();
    }

}
