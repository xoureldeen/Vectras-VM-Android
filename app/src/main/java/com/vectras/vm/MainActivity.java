package com.vectras.vm;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;
import static android.content.Intent.ACTION_VIEW;
import static android.os.Build.VERSION.SDK_INT;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.termux.app.TermuxService;

import static com.vectras.vm.VectrasApp.getApp;
import static com.vectras.vm.utils.LibraryChecker.isPackageInstalled2;
import static com.vectras.vm.utils.UIUtils.UIAlert;

import com.vectras.vm.utils.CommandUtils;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.PermissionUtils;

import android.androidVNC.androidVNC;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
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
import com.vectras.vm.utils.AppUpdater;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.LibraryChecker;
import com.vectras.vm.utils.PackageUtils;
import com.vectras.vm.utils.UIUtils;
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
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.vectras.vm.core.ShellExecutor;

import com.vectras.vm.x11.X11Activity;


public class MainActivity extends AppCompatActivity {
    public static String curRomName;
    public static RecyclerView mRVMainRoms;
    public static LinearLayout romsLayout;
    public static AdapterMainRoms mMainAdapter;
    public static JSONArray jArray;
    public static List<DataMainRoms> data;
    public AlertDialog ad;
    public static MainActivity activity;
    private InterstitialAd mInterstitialAd;
    private AdRequest adRequest;
    public DrawerLayout mainDrawer;
    private String TAG = "MainActivity";
    public static /**/ LinearLayout extVncLayout;
    public static AppBarLayout appbar;
    public TextView totalRam;
    public TextView usedRam;
    public TextView freeRam;
    public static LinearLayout linearnothinghere;
    private final Timer _timer = new Timer();
    private AlertDialog alertDialog;
    private boolean doneonstart = false;
    public static boolean isActivate = false;
    public boolean skipIDEwithARM64DialogInStartVM = false;
    BottomAppBar bottomAppBar;

    public static Timer timer = new Timer();
    public static TimerTask timerTask;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        activity = this;
        RamInfo.activity = this;
        setContentView(R.layout.activity_main);
        isActivate = true;
        setupFolders();

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
        tvLogin.setText("LOGIN --> " + Config.defaultVNCHost + ":" + (5901)/* + "\nPASSWORD --> " + Config.defaultVNCPasswd*/);

        Button stopBtn = findViewById(R.id.stopBtn);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Stop the service
                MainService.stopService();

                Terminal vterm = new Terminal(activity);
                vterm.executeShellCommand2("killall qemu-system-*", false, activity);

                extVncLayout.setVisibility(View.GONE);
                appbar.setExpanded(false);
            }
        });

        refreshRoms.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                errorjsondialog();
                loadDataVbi();
                mMainAdapter.notifyItemRangeChanged(0, mMainAdapter.data.size());
                refreshRoms.setRefreshing(false);
            }
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
        gotoromstore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), RomsManagerActivity.class);
                startActivity(intent);
            }
        });

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd_AppBarBottomActivity);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(activity, SetArchActivity.class));
            }
        });
        Toolbar mainToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mainToolbar);
        mainDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mainDrawer, mainToolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mainDrawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        //Setting Navigation View Item Selected Listener to handle the item click of the navigation menu
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            // This method will trigger on item Click of navigation menu
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
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
                        ad.setButton(Dialog.BUTTON_POSITIVE, "REPLACE", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                intent.setType("*/*");

                                // Optionally, specify a URI for the file that should appear in the
                                // system file picker when it loads.
                                if (SDK_INT >= Build.VERSION_CODES.O) {
                                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                                }

                                startActivityForResult(intent, 1004);
                                return;
                            }
                        });
                        ad.setButton(Dialog.BUTTON_NEGATIVE, "REMOVE", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                File isoFile = new File(AppConfig.maindirpath + "/drive.iso");
                                try {
                                    isoFile.delete();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                return;
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
                        ad.setButton(Dialog.BUTTON_POSITIVE, "REPLACE", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                intent.setType("*/*");

                                // Optionally, specify a URI for the file that should appear in the
                                // system file picker when it loads.
                                if (SDK_INT >= Build.VERSION_CODES.O) {
                                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                                }

                                startActivityForResult(intent, 1006);
                                return;
                            }
                        });
                        ad.setButton(Dialog.BUTTON_NEGATIVE, "REMOVE", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                File isoFile = new File(AppConfig.maindirpath + "/hdd1.qcow2");
                                try {
                                    isoFile.delete();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                return;
                            }
                        });
                        ad.setButton(Dialog.BUTTON_NEUTRAL, "SHARE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
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
                        ad.setButton(Dialog.BUTTON_POSITIVE, "REPLACE", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                intent.setType("*/*");

                                // Optionally, specify a URI for the file that should appear in the
                                // system file picker when it loads.
                                if (SDK_INT >= Build.VERSION_CODES.O) {
                                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                                }

                                startActivityForResult(intent, 1006);
                                return;
                            }
                        });
                        ad.setButton(Dialog.BUTTON_NEGATIVE, "REMOVE", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                File isoFile = new File(AppConfig.maindirpath + "/hdd2.qcow2");
                                try {
                                    isoFile.delete();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                return;
                            }
                        });
                        ad.setButton(Dialog.BUTTON_NEUTRAL, "SHARE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
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
                    if (SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle(R.string.x11_feature_not_supported)
                                .setMessage(R.string.the_x11_feature_is_currently_not_supported_on_android_14_and_above_please_use_a_device_with_android_13_or_below_for_x11_functionality)
                                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                .create()
                                .show();
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
                                new AlertDialog.Builder(activity, R.style.MainDialogTheme)
                                        .setTitle("Install XFCE4")
                                        .setMessage("XFCE4 is not installed. Would you like to install it?")
                                        .setCancelable(false)
                                        .setPositiveButton("Install", (dialog, which) -> {
                                            String installCommand = "apk add " + xfce4Package;
                                            new Terminal(activity).executeShellCommand(installCommand, true, activity);
                                        })
                                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                                        .show();
                            } else {
                                new Terminal(activity).executeShellCommand2("killall xfce4-session", false, activity);
                                startActivity(new Intent(activity, X11Activity.class));
                                new Terminal(activity).executeShellCommand2("xfce4-session", false, MainActivity.activity);
                            }
                        });

                    }
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
                    RecyclerView logList = (RecyclerView) view.findViewById(R.id.recyclerLog);
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
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
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
                                    }
                                });
                            }
                        };
                        _timer.scheduleAtFixedRate(t, (int) (0), (int) (100));
                    } catch (IOException e) {
                        Toast.makeText(activity, "There was an error: " + Log.getStackTraceString(e), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
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
            }
        });

        getWindow().setNavigationBarColor(SurfaceColors.SURFACE_2.getColor(this));

        adRequest = new AdRequest.Builder().build();

        //AdView mAdView = findViewById(R.id.adView);
        //adRequest = new AdRequest.Builder().build();
        //mAdView.loadAd(adRequest);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

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


        String vectrasMemory = String.valueOf(RamInfo.vectrasMemory());
        TimerTask t =
                new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        boolean isMainServiceRunning =
                                                isServiceRunning(MainService.class, activity);
                                        if (isMainServiceRunning)
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
            updateApp(false);

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
    public void onDestroy() {
        isActivate = false;
        super.onDestroy();
    }

    public static void clearNotifications() {
        NotificationManager notificationManager = (NotificationManager) activity.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    public void updateApp(final boolean showDialog) {
        new AppUpdater(this, new AppUpdater.OnUpdateListener() {
            @Override
            public void onUpdateListener(String result) {
                try {
                    if (!result.contains("Error on getting data")) {
                        final JSONObject obj = new JSONObject(result);
                        int versionCode = PackageUtils.getThisVersionCode(getApplicationContext());
                        String versionName = PackageUtils.getThisVersionName(getApplicationContext());
                        String versionNameonUpdate;

                        if (obj.getString("versionNameBetas").equals(versionName) && !MainSettingsManager.getcheckforupdatesfromthebetachannel(activity) && !MainSettingsManager.getDontShowAgainJoinBetaUpdateChannelDialog(activity)) {
                            AlertDialog _alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
                            _alertDialog.setTitle(getResources().getString(R.string.you_are_using_beta_version));
                            _alertDialog.setMessage(getResources().getString(R.string.switch_to_check_for_updates_on_the_Beta_channel_now));
                            _alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    MainSettingsManager.setcheckforupdatesfromthebetachannel(activity, true);
                                }
                            });
                            _alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    _alertDialog.dismiss();
                                }
                            });
                            _alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, getResources().getString(R.string.dont_show_again), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    MainSettingsManager.setDontShowAgainJoinBetaUpdateChannelDialog(activity, true);
                                }
                            });
                            _alertDialog.show();
                        }

                        if (MainSettingsManager.getcheckforupdatesfromthebetachannel(activity)) {
                            versionNameonUpdate = obj.getString("versionNameBeta");

                            if (versionCode < obj.getInt("versionCode") || !versionNameonUpdate.equals(versionName)) {
                                if (showDialog) {
                                AlertDialog.Builder alert = new AlertDialog.Builder(activity, R.style.MainDialogTheme);
                                alert.setTitle("Install the latest version")
                                        .setMessage(Html.fromHtml(obj.getString("MessageBeta") + "<br><br>Update size:<br>" + obj.getString("sizeBeta")))
                                        .setCancelable(obj.getBoolean("cancellableBeta"))
                                        .setNegativeButton("Update", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                try {
                                                    startActivity(new Intent(ACTION_VIEW, Uri.parse(obj.getString("urlBeta"))));
                                                } catch (JSONException e) {

                                                }
                                            }
                                        }).show();
                                } else {
                                    View _update = findViewById(R.id.update);
                                    _update.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            versionNameonUpdate = obj.getString("versionName");

                            if (versionCode < obj.getInt("versionCode") || !versionNameonUpdate.contains(versionName)) {
                                if (showDialog) {
                                AlertDialog.Builder alert = new AlertDialog.Builder(activity, R.style.MainDialogTheme);
                                alert.setTitle("Install the latest version")
                                        .setMessage(Html.fromHtml(obj.getString("Message") + "<br><br>Update size:<br>" + obj.getString("size")))
                                        .setCancelable(obj.getBoolean("cancellable"))
                                        .setNegativeButton("Update", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                try {
                                                    startActivity(new Intent(ACTION_VIEW, Uri.parse(obj.getString("url"))));
                                                } catch (JSONException e) {

                                                }
                                            }
                                        }).show();
                                } else {
                                    View _update = findViewById(R.id.update);
                                    _update.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    } else if (result.contains("Error on getting data") && showDialog) {
                        errorUpdateDialog(result);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start(showDialog);
    }

    private void errorUpdateDialog(String error) {
        VectrasStatus.logInfo(String.format(error));
    }

    public static void loadDataVbi() {
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
                    romsMainData.qmpPort= 0;
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
            mRVMainRoms = (RecyclerView) activity.findViewById(R.id.mRVMainRoms);
            mMainAdapter = new AdapterMainRoms(MainActivity.activity, data);
            mRVMainRoms.setAdapter(mMainAdapter);
            int spanCount = activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 3 : 2;
            mRVMainRoms.setLayoutManager(new GridLayoutManager(MainActivity.activity, spanCount));
        } catch (JSONException e) {
        }
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
            if (VMManager.isQemuRunning()) {
                if (MainSettingsManager.getVmUi(activity).equals("VNC"))
                    activity.startActivity(new Intent(activity, MainVNCActivity.class));
                else if (MainSettingsManager.getVmUi(activity).equals("X11"))
                    activity.startActivity(new Intent(activity, X11Activity.class));
            } else {
                Toast.makeText(getApplicationContext(), activity.getResources().getString(R.string.there_is_nothing_here_because_there_is_no_vm_running), Toast.LENGTH_LONG).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * CHECK WHETHER INTERNET CONNECTION IS AVAILABLE OR NOT
     */
    public boolean checkConnection(Context context) {
        final ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connMgr != null) {
            NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();

            if (activeNetworkInfo != null) { // connected to the internet
                // connected to the mobile provider's data plan
                if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    // connected to wifi
                    return true;
                } else
                    return activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
            }
        }
        return false;
    }

    private static boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean checkSharedFolder() { //TODO: not work idk why
        File folder = new File(AppConfig.sharedFolder);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile() && file.length() > 500 * 1024 * 1024) { // 500MB
                    return true;
                }
            }
        }
        return false;
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

        File romDir = new File(Config.getCacheDir()+ "/" + Config.vmID);
        romDir.mkdirs();

        if (!VMManager.isthiscommandsafe(env, activity.getApplicationContext())) {
            UIUtils.oneDialog(activity.getResources().getString(R.string.problem_has_been_detected), activity.getResources().getString(R.string.harmful_command_was_detected) + " " + activity.getResources().getString(R.string.reason) + ": " + VMManager.latestUnsafeCommandReason, true, false, activity);
            return;
        }

        if (VMManager.isThisVMRunning(itemExtra, itemPath)) {
            Toast.makeText(activity, "This VM is already running.", Toast.LENGTH_LONG).show();
            if (MainSettingsManager.getVmUi(activity).equals("VNC"))
                activity.startActivity(new Intent(activity, MainVNCActivity.class));
            else if (MainSettingsManager.getVmUi(activity).equals("X11"))
                activity.startActivity(new Intent(activity, X11Activity.class));
            return;
        }

        if (AppConfig.getSetupFiles().contains("arm") && !AppConfig.getSetupFiles().contains("arm64")) {
            if (env.contains("tcg,thread=multi")) {
                AlertDialog abiAlertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
                abiAlertDialog.setTitle(activity.getResources().getString(R.string.oops));
                abiAlertDialog.setMessage(activity.getResources().getString(R.string.can_not_use_mttcg));
                abiAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, activity.getResources().getString(R.string.continuetext), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startVM(vmName, env.replace("tcg,thread=multi", "tcg,thread=single"), itemExtra, itemPath);
                    }
                });
                abiAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                abiAlertDialog.show();
                return;
            }
        }

        if (MainSettingsManager.getArch(activity).equals("ARM64") && MainSettingsManager.getIfType(activity).equals("ide") && !activity.skipIDEwithARM64DialogInStartVM) {
            AlertDialog abiAlertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
            abiAlertDialog.setTitle(activity.getResources().getString(R.string.problem_has_been_detected));
            abiAlertDialog.setMessage(activity.getResources().getString(R.string.you_cannot_use_IDE_hard_drive_type_with_ARM64));
            abiAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, activity.getResources().getString(R.string.continuetext), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    activity.skipIDEwithARM64DialogInStartVM = true;
                    startVM(vmName, env, itemExtra, itemPath);
                }
            });
            abiAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            abiAlertDialog.show();
            return;
        } else if (activity.skipIDEwithARM64DialogInStartVM) {
            activity.skipIDEwithARM64DialogInStartVM = false;
        }

        if (MainSettingsManager.getSharedFolder(activity) && MainSettingsManager.getArch(activity).equals("I386")) {
            Toast.makeText(activity, R.string.shared_folder_is_not_used_because_i386_does_not_support_it, Toast.LENGTH_LONG).show();
        }

        boolean isRunning = isMyServiceRunning(MainService.class);

        ProgressDialog progressDialog = new ProgressDialog(activity, R.style.MainDialogTheme);
        progressDialog.setMessage(activity.getString(R.string.booting_up));
        progressDialog.setCancelable(false);
        progressDialog.show();
        Handler handler = new Handler();
        handler.postDelayed(
                new Runnable() {
                    public void run() {
                        if (isRunning) {
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
                                progressDialog.dismiss();
                            } else {
                                progressDialog.show();
                                Handler handler = new Handler();
                                handler.postDelayed(
                                        new Runnable() {
                                            public void run() {
                                                MainVNCActivity.started = true;
                                                activity.startActivity(
                                                        new Intent(
                                                                activity, MainVNCActivity.class));
                                                progressDialog.dismiss();
                                            }
                                        },
                                        2000);
                            }
                        } else if (MainSettingsManager.getVmUi(activity).equals("SPICE")) {
                            // activity.startActivity(new Intent(activity,
                            // RemoteCanvasActivity.class));
                        } else if (MainSettingsManager.getVmUi(activity).equals("X11")) {
                            progressDialog.show();
                            Handler handler = new Handler();
                            handler.postDelayed(
                                    new Runnable() {
                                        public void run() {
                                            activity.startActivity(
                                                    new Intent(activity, X11Activity.class));
                                            progressDialog.dismiss();
                                        }
                                    },
                                    3000);
                        }
                    }
                },
                2000);
        String[] params = env.split("\\s+");
        VectrasStatus.logInfo("Params:");
        Log.d("StartVM", "Params:");
        for (int i = 0; i < params.length; i++) {
            VectrasStatus.logInfo(i + ": " + params[i]);
            Log.d("StartVM", i + ": " + params[i]);
        }

    }

    private void setupFolders() {
        Thread t = new Thread(new Runnable() {
            public void run() {

                Config.cacheDir = getCacheDir().getAbsolutePath();
                Config.storagedir = Environment.getExternalStorageDirectory().toString();

            }
        });
        t.start();
    }

    public void onResume() {
        super.onResume();
        checkpermissions();
        Log.d(TAG, "onResume");
        Config.ui = MainSettingsManager.getVmUi(activity);

        //TEMPORARY FIX FOR VNC CLOSES
        //TODO: FIND FIX FOR CRASHING
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        //if (MainSettingsManager.getVmUi(activity).equals("VNC") && IOApplication.isPortOpen("127.0.0.1", Config.QMPPort, 100) && MainVNCActivity.started)
            //startActivity(new Intent(activity, MainVNCActivity.class));
    }

    public static boolean isServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        if (!MainSettingsManager.getVncExternal(activity))
            clearNotifications();
        loadDataVbi();
        Config.ui = MainSettingsManager.getVmUi(activity);

        TextView tvQemuArch = findViewById(R.id.qemuArch);
        tvQemuArch.setText(MainSettingsManager.getArch(activity));

        //TEMPORARY FIX FOR VNC CLOSES
        //TODO: FIND FIX FOR CRASHING
        //if (MainSettingsManager.getVmUi(activity).equals("VNC") && MainVNCActivity.started)
            //startActivity(new Intent(activity, MainVNCActivity.class));

        InterstitialAd.load(this, "ca-app-pub-3568137780412047/7745973511", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        Log.i("MainActivity", "onAdLoaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.d("MainActivity", loadAdError.toString());
                        mInterstitialAd = null;
                    }
                });
        if (mInterstitialAd != null) {
            mInterstitialAd.show(this);
        } else {
            Log.d("TAG", "The interstitial ad wasn't ready yet.");
        }
        doneonstart = true;

        if (!AppConfig.pendingCommand.isEmpty()) {
            if (!VMManager.isthiscommandsafe(AppConfig.pendingCommand, getApplicationContext())) {
                AppConfig.pendingCommand = "";
                UIUtils.oneDialog(activity.getResources().getString(R.string.problem_has_been_detected), activity.getResources().getString(R.string.harmful_command_was_detected) + " " + activity.getResources().getString(R.string.reason) + ": " + VMManager.latestUnsafeCommandReason, true, false, activity);
            } else {
                if (AppConfig.pendingCommand.startsWith("qemu-img")) {
                    if (!VMManager.isthiscommandsafeimg(AppConfig.pendingCommand, getApplicationContext())) {
                        UIUtils.oneDialog(activity.getResources().getString(R.string.problem_has_been_detected), activity.getResources().getString(R.string.size_too_large_try_qcow2_format), true, false, activity);
                    } else {
                        Terminal _vterm = new Terminal(MainActivity.this);
                        _vterm.executeShellCommand2(AppConfig.pendingCommand, false, MainActivity.activity);
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.done), Toast.LENGTH_LONG).show();
                    }
                } else {
                    StartVM.cdrompath = "";
                    String env = StartVM.env(MainActivity.activity, AppConfig.pendingCommand, "", "1");
                    MainActivity.startVM("Quick run", env, AppConfig.pendingCommand, "");
                }
            }
            AppConfig.pendingCommand = "";
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
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
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FileInputStream File = null;
                        try {
                            File = (FileInputStream) getContentResolver().openInputStream(content_describer);
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                        try {
                            try {
                                OutputStream out = new FileOutputStream(new File(AppConfig.maindirpath + "/drive.iso"));
                                try {
                                    // Transfer bytes from in to out
                                    byte[] buf = new byte[1024];
                                    int len;
                                    while ((len = File.read(buf)) > 0) {
                                        out.write(buf, 0, len);
                                    }
                                } finally {
                                    out.close();
                                }
                            } finally {
                                Runnable runnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        loading.setVisibility(View.GONE);
                                    }
                                };
                                activity.runOnUiThread(runnable);
                                File.close();
                            }
                        } catch (IOException e) {
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    loading.setVisibility(View.GONE);
                                    UIAlert(activity, e.toString(), "error");
                                }
                            };
                            activity.runOnUiThread(runnable);
                        }
                    }
                }).start();
            } else
                UIAlert(activity, "please select iso file", "INVALID FILE");
        } else if (requestCode == 1005 && resultCode == RESULT_OK) {
            Uri content_describer = ReturnedIntent.getData();
            File selectedFilePath = new File(getPath(content_describer));
            ProgressBar loading = findViewById(R.id.loading);
            loading.setVisibility(View.VISIBLE);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FileInputStream File = null;
                    try {
                        File = (FileInputStream) getContentResolver().openInputStream(content_describer);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        try {
                            OutputStream out = new FileOutputStream(new File(AppConfig.maindirpath + "/hdd1.qcow2"));
                            try {
                                // Transfer bytes from in to out
                                byte[] buf = new byte[1024];
                                int len;
                                while ((len = File.read(buf)) > 0) {
                                    out.write(buf, 0, len);
                                }
                            } finally {
                                out.close();
                            }
                        } finally {
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    loading.setVisibility(View.GONE);
                                }
                            };
                            activity.runOnUiThread(runnable);
                            File.close();
                        }
                    } catch (IOException e) {
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                loading.setVisibility(View.GONE);
                                UIAlert(activity, e.toString(), "error");
                            }
                        };
                        activity.runOnUiThread(runnable);
                    }
                }
            }).start();
        } else if (requestCode == 1006 && resultCode == RESULT_OK) {
            Uri content_describer = ReturnedIntent.getData();
            File selectedFilePath = new File(getPath(content_describer));
            ProgressBar loading = findViewById(R.id.loading);
            loading.setVisibility(View.VISIBLE);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FileInputStream File = null;
                    try {
                        File = (FileInputStream) getContentResolver().openInputStream(content_describer);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        try {
                            OutputStream out = new FileOutputStream(new File(AppConfig.maindirpath + "/hdd2.qcow2"));
                            try {
                                // Transfer bytes from in to out
                                byte[] buf = new byte[1024];
                                int len;
                                while ((len = File.read(buf)) > 0) {
                                    out.write(buf, 0, len);
                                }
                            } finally {
                                out.close();
                            }
                        } finally {
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    loading.setVisibility(View.GONE);
                                }
                            };
                            activity.runOnUiThread(runnable);
                            File.close();
                        }
                    } catch (IOException e) {
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                loading.setVisibility(View.GONE);
                                UIAlert(activity, e.toString(), "error");
                            }
                        };
                        activity.runOnUiThread(runnable);
                    }
                }
            }).start();
        } else if (requestCode == 122 && resultCode == RESULT_OK) {
            Uri content_describer = ReturnedIntent.getData();
            File selectedFilePath = new File(getPath(content_describer));
            ProgressBar loading = findViewById(R.id.loading);
            loading.setVisibility(View.VISIBLE);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FileInputStream File = null;
                    try {
                        File = (FileInputStream) getContentResolver().openInputStream(content_describer);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        try {
                            File romDir = new File(AppConfig.maindirpath + curRomName + "/");
                            if (!romDir.exists()) {
                                romDir.mkdirs();
                            }
                            OutputStream out = new FileOutputStream(new File(AppConfig.maindirpath + curRomName + "/" + "drv1-" + selectedFilePath.getName()));
                            try {
                                // Transfer bytes from in to out
                                byte[] buf = new byte[1024];
                                int len;
                                while ((len = File.read(buf)) > 0) {
                                    out.write(buf, 0, len);
                                }
                            } finally {
                                out.close();
                            }
                        } finally {
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    loading.setVisibility(View.GONE);
                                }
                            };
                            activity.runOnUiThread(runnable);
                            File.close();
                        }
                    } catch (IOException e) {
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                loading.setVisibility(View.GONE);
                                UIAlert(activity, e.toString(), "error");
                            }
                        };
                        activity.runOnUiThread(runnable);
                    }
                }
            }).start();
        }
    }

    public String getPath(Uri uri) {
        return FileUtils.getPath(this, uri);
    }

    private void errorjsondialog() {
        if (FileUtils.isFileExists(AppConfig.romsdatajson)) {
            if (VMManager.isRomsDataJsonNormal(true, MainActivity.this)) {
                loadDataVbi();
                mdatasize();
            }
        } else {
            FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", "[]");
            loadDataVbi();
            mdatasize();
        }
    }

    private String readFile(String filePath) {
        StringBuilder content = new StringBuilder();
        try (FileInputStream inputStream = new FileInputStream(filePath);
             BufferedReader reader = new BufferedReader(new
                     InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return content.toString();

    }

    private void mdatasize() {
        if (MainActivity.data.isEmpty()) {
            if (!doneonstart) {
                alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
                alertDialog.setTitle(getResources().getString(R.string.nothing_here));
                alertDialog.setMessage("Do you want to create a new virtual machine now?");
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.create), (dialog, which) -> {
                    startActivity(new Intent(activity, SetArchActivity.class));
                });
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.cancel), (dialog, which) -> {

                });
                alertDialog.show();
            }
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
            errorjsondialog();
        }
    }

    private void setupBottomAppBar() {
        bottomAppBar = findViewById(R.id.bottomAppBar);
        bottomAppBar.setOnMenuItemClickListener(new BottomAppBar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.update) {
                    updateApp(true);
                } else if (item.getItemId() == R.id.shutdown) {
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
                } else if (item.getItemId() == R.id.backtothedisplay) {
                    if (MainSettingsManager.getVmUi(activity).equals("VNC")) {
                        startActivity(new Intent(activity, MainVNCActivity.class));
                    } else if (MainSettingsManager.getVmUi(activity).equals("X11")) {
                        startActivity(new Intent(activity, X11Activity.class));
                    }
                } else if (item.getItemId() == R.id.importrom) {
                    Intent intent = new Intent();
                    intent.setClass(getApplicationContext(), CustomRomActivity.class);
                    intent.putExtra("importcvbinow", "");
                    startActivity(intent);
                }
                return false;
            }
        });
        View _update = findViewById(R.id.update);
        _update.setVisibility(View.GONE);
    }

}
