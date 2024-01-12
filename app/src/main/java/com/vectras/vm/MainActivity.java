package com.vectras.vm;

import static android.os.Build.VERSION.SDK_INT;

import android.androidVNC.RfbProto;
import android.androidVNC.VncCanvas;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.vectras.qemu.Config;
import com.vectras.qemu.MainSDLActivity;
import com.vectras.qemu.MainService;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.jni.StartVM;
import com.vectras.qemu.utils.FileUtils;
import com.vectras.qemu.utils.Machine;
import com.vectras.qemu.utils.QmpClient;
import com.vectras.vm.Fragment.HomeFragment;
import com.vectras.vm.Fragment.LoggerFragment;
import com.vectras.vm.logger.VectrasStatus;
import com.vectras.vm.utils.AppUpdater;
import com.vectras.qemu.utils.FileInstaller;
import com.vectras.qemu.utils.RamInfo;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.vectras.vm.utils.UIUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Objects;
import java.util.TimerTask;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    private static Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    public static final String TAG = "Main Activity";
    // Static
    public static final String CREDENTIAL_SHARED_PREF = "settings_prefs";

    public static MainActivity activity = null;
    //private FirebaseAuth mAuth;
    public DrawerLayout mainDrawer;
    public Toolbar mainToolbar;
    public static TextView totalRam;
    public static TextView usedRam;
    public static TextView freeRam;
    public static TextView ipTxt;
    private Timer _timer = new Timer();
    private TimerTask t;
    public ViewPager viewPager;
    MenuItem prevMenuItem;
    int pager_number = 2;
    private InterstitialAd mInterstitialAd;
    private AdRequest adRequest;
    public static AppBarLayout appbar;

    public static void UIAlert(String title, String body, Activity activity) {
        AlertDialog ad;
        ad = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
        ad.setTitle(title);
        ad.setMessage(body);
        ad.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        ad.show();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        activity = this;
        clearNotifications();
        setupFolders();
        setupStrictMode();
        execTimer();
        checkAndLoadLibs();
        Config.logFilePath = Config.cacheDir + "/vectras/vectras-log.txt";
        activity = this;
        this.setContentView(R.layout.main);
        this.setupWidgets();
        initNavigationMenu();
        FileInstaller.installFiles(activity, false);

        //updateApp(true);
        //mAuth = FirebaseAuth.getInstance();
    }

    public static PackageInfo getAppInfo(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateApp(final boolean showDialog) {
        new AppUpdater(this, new AppUpdater.OnUpdateListener() {
            @Override
            public void onUpdateListener(String result) {
                try {
                    if (!result.contains("Error on getting data")) {
                        final JSONObject obj = new JSONObject(result);
                        PackageInfo pinfo = getAppInfo(getApplicationContext());
                        int versionCode = pinfo.versionCode;
                        if (versionCode < obj.getInt("versionCode")) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(activity, R.style.MainDialogTheme);
                            alert.setTitle("Install the latest version")
                                    .setMessage(Html.fromHtml(obj.getString("Message") + "<br><br>update size:<br>" + obj.getString("size")))
                                    .setCancelable(obj.getBoolean("cancellable"))
                                    .setNegativeButton("Update", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            try {
                                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(obj.getString("url"))));
                                            } catch (JSONException e) {

                                            }
                                        }
                                    }).show();

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

    private MenuItem vectrasInfo;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home_toolbar_menu, menu);
        vectrasInfo = menu.findItem(R.id.vectrasInfo);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Menu items
        int id = item.getItemId();
        if (id == R.id.vectrasInfo) {
            appbar = findViewById(R.id.appbar);
            if (appbar.getTop() < 0)
                appbar.setExpanded(true);
            else
                appbar.setExpanded(false);

        } else if (id == R.id.installRoms) {
            startActivity(new Intent(activity, RomsManagerActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    private void initNavigationMenu() {
        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bNav);

        bottomNavigationView
                .setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        int id = item.getItemId();
                        if (id == R.id.menu_home) {
                            viewPager.setCurrentItem(0);
                        } else if (id == R.id.menu_logger) {
                            viewPager.setCurrentItem(1);
                        }
                        return false;
                    }
                });
    }

    public class MyAdapter extends FragmentPagerAdapter {

        MyAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    return new HomeFragment();
                case 1:
                    return new LoggerFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            return pager_number;
        }
    }

    public static LinearLayout extVncLayout;
    public MaterialButton stopBtn;
    FirebaseAuth mAuth;
    FirebaseUser mCurrentUser;

    // Setting up the UI
    public void setupWidgets() {
        extVncLayout = findViewById(R.id.extVnc);
        stopBtn = findViewById(R.id.stopBtn);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStopButton(true);
            }
        });
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new MyAdapter(getSupportFragmentManager()));
        viewPager.setOffscreenPageLimit(pager_number);
        final BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bNav);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (prevMenuItem != null) {
                    prevMenuItem.setChecked(false);
                } else {
                    bottomNavigationView.getMenu().getItem(0).setChecked(false);
                }
                bottomNavigationView.getMenu().getItem(position).setChecked(true);
                prevMenuItem = bottomNavigationView.getMenu().getItem(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        appbar = findViewById(R.id.appbar);
        appbar.setExpanded(false);
        mainToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mainToolbar);
        mainDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mainDrawer, mainToolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mainDrawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        View headerView = navigationView.getHeaderView(0);
        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();
        assert mCurrentUser != null;
        String name = mCurrentUser.getDisplayName();
        String email = mCurrentUser.getEmail();
        Uri picture = mCurrentUser.getPhotoUrl();
        TextView navUsername = (TextView) headerView.findViewById(R.id.usernameTxt);
        navUsername.setText(name);
        TextView navEmail = (TextView) headerView.findViewById(R.id.emailTxt);
        navEmail.setText(email);
        TextView viewProfile = (TextView) headerView.findViewById(R.id.viewProfile);
        TextView appNameTxt = (TextView) headerView.findViewById(R.id.appNameTxt);
        /*ObjectAnimator rgbAnim=ObjectAnimator.ofObject(navUsername,"textColor",new ArgbEvaluator(), Color.RED,Color.GREEN,Color.BLUE);
        rgbAnim.setDuration(500);
        rgbAnim.setRepeatMode(ValueAnimator.REVERSE);
        rgbAnim.setRepeatCount(ValueAnimator.INFINITE);
        rgbAnim.start();*/
        ImageView ivProfile = (ImageView) headerView.findViewById(R.id.profilePic2);

        viewProfile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                startActivity(new Intent(activity, ProfileActivity.class));

            }
        });

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
                } else if (id == R.id.navigation_item_website) {
                    String tw = AppConfig.vectrasWebsite;
                    Intent w = new Intent(Intent.ACTION_VIEW);
                    w.setData(Uri.parse(tw));
                    startActivity(w);
                } else if (id == R.id.navigation_item_view_logs) {
                    FileUtils.viewVectrasLog(activity);
                } else if (id == R.id.navigation_item_settings) {
                    startActivity(new Intent(activity, MainSettingsManager.class));
                } else if (id == R.id.navigation_item_store) {
                    startActivity(new Intent(activity, StoreActivity.class));
                }
                return false;
            }
        });

        ipTxt = findViewById(R.id.ipTxt);

        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);

        long freeMem = mi.availMem / 1048576L;
        long totalMem = mi.totalMem / 1048576L;
        long usedMem = totalMem - freeMem;
        int freeRamInt = safeLongToInt(freeMem);
        int totalRamInt = safeLongToInt(totalMem);
        ipTxt.setText("Local Ip Address: " + getLocalIpAddress());

        SharedPreferences credentials = activity.getSharedPreferences(CREDENTIAL_SHARED_PREF, Context.MODE_PRIVATE);

        totalRam = findViewById(R.id.totalRam);
        usedRam = findViewById(R.id.usedRam);
        freeRam = findViewById(R.id.freeRam);

        ipTxt.setVisibility(View.GONE);
        String vectrasMemory = String.valueOf(RamInfo.vectrasMemory());
        t = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ActivityManager.MemoryInfo miI = new ActivityManager.MemoryInfo();
                        ActivityManager activityManagerr = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                        activityManagerr.getMemoryInfo(miI);
                        long freeMemory = miI.availMem / 1048576L;
                        long totalMemory = miI.totalMem / 1048576L;
                        long usedMemory = totalMemory - freeMemory;

                        totalRam.setText("Total Memory: " + totalMemory + " MB");
                        usedRam.setText("Used Memory: " + usedMemory + " MB");
                        freeRam.setText("Free Memory: " + freeMemory + " MB (" + vectrasMemory + " used)");
                        ProgressBar progressBar = findViewById(R.id.progressBar);
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

        AdView mAdView = findViewById(R.id.adView);
        adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        if (MainSettingsManager.getPromptUpdateVersion(activity))
            updateApp(true);
        /*FirebaseUser user = mAuth.getCurrentUser();
        TextView usernameTxt = findViewById(R.id.usernameTxt);
        TextView emailTxt = findViewById(R.id.emailTxt);
        ImageView profilePic = findViewById(R.id.profilePic);
        if (user != null) {
            // Name, email address, and profile photo Url
            String name = user.getDisplayName();
            String email = user.getEmail();
            Uri photoUrl = user.getPhotoUrl();

            // Check if user's email is verified
            boolean emailVerified = user.isEmailVerified();

            // The user's ID, unique to the Firebase project. Do NOT use this value to
            // authenticate with your backend server, if you have one. Use
            // FirebaseUser.getIdToken() instead.
            String uid = user.getUid();

            usernameTxt.setText(name);
            emailTxt.setText(email);
            if (photoUrl != null)
                Glide.with(activity).load(photoUrl.toString()).into(profilePic);
        }*/

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (!prefs.getBoolean("tgDialog", false)) {
            AlertDialog alertDialog;
            alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
            alertDialog.setTitle("JOIN US ON TELEGRAM");
            alertDialog.setMessage("Join us on Telegram where we publish all the news and updates and receive your opinions and bugs");
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "JOIN", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String tg = "https://t.me/vectras_os";
                    Intent f = new Intent(Intent.ACTION_VIEW);
                    f.setData(Uri.parse(tg));
                    startActivity(f);
                    return;
                }
            });
            alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "CANCEL", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    return;
                }
            });
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "DONT SHOW AGAIN", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
                    SharedPreferences.Editor edit = prefs.edit();
                    edit.putBoolean("tgDialog", true);
                    edit.apply();
                    return;
                }
            });
            alertDialog.show();
        }
    }

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
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
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTimeListener();

    }

    @Override
    public void onStart() {
        super.onStart();
        if (MainSettingsManager.getVirtio(activity)) {
            Config.hd_if_type = "virtio";
        } else {
            Config.hd_if_type = "ide";
        }
        InterstitialAd.load(this, "ca-app-pub-3568137780412047/7745973511", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "onAdLoaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.d(TAG, loadAdError.toString());
                        mInterstitialAd = null;
                    }
                });
        if (mInterstitialAd != null && !MainService.isRunning) {
            mInterstitialAd.show(this);
        } else {
            Log.d("TAG", "The interstitial ad wasn't ready yet.");
        }
    }

    public void onPause() {
        super.onPause();
        stopTimeListener();
    }

    public boolean loaded = false;

    public void onResume() {
        super.onResume();
        if (MainService.isRunning && Objects.equals(Config.ui, "VNC")) {
            startvnc();
        }
        execTimer();
    }

    public static VMStatus currStatus = VMStatus.Ready;
    public static boolean vmStarted = false;
    public static StartVM vmexecutor;
    public static String vnc_passwd = "vectras";
    public static int vnc_allow_external = 1;
    public static int qmp_allow_external = 0;
    public static ProgressDialog progDialog;
    public static View parent;
    public static InstallerTask installerTaskTask;
    public static boolean timeQuit = false;
    public static Object lockTime = new Object();

    public static boolean libLoaded;


    static public void onInstall(boolean force) {
        FileInstaller.installFiles(activity, force);
    }

    public static String getVnc_passwd() {
        return vnc_passwd;
    }

    public static void setVnc_passwd(String vnc_passwd) {
        vnc_passwd = vnc_passwd;
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.getHostAddress().toString().contains(".")) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // Start calling the JNI interface
    public static void startvm(Activity activity, int UI) {
        QmpClient.allow_external = (qmp_allow_external == 1);
        vmexecutor.qmp_allow_external = qmp_allow_external;

        if (UI == Config.UI_VNC) {
            // disable sound card with VNC
            vmexecutor.enablevnc = 1;
            vmexecutor.enablespice = 0;
            vmexecutor.sound_card = null;
            vmexecutor.vnc_allow_external = vnc_allow_external;
            RfbProto.allow_external = (vnc_allow_external == 1);
            vmexecutor.vnc_passwd = vnc_passwd;
        } else if (UI == Config.UI_SDL) {
            vmexecutor.enablevnc = 0;
            vmexecutor.enablespice = 0;
        } else if (UI == Config.UI_SPICE) {
            vmexecutor.vnc_allow_external = vnc_allow_external;
            vmexecutor.vnc_passwd = vnc_passwd;
            vmexecutor.enablevnc = 0;
            vmexecutor.enablespice = 1;
        }
        vmexecutor.startvm(activity, UI);

    }


    public static void cleanup() {

        vmStarted = false;

        //XXX flush and close all file descriptors if we haven't already
        FileUtils.close_fds();

        ////XXX; we wait till fds flush and close
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //set the exit code
        MainSettingsManager.setExitCode(activity, 1);

        //XXX: SDL seems to lock the keyboard events
        // unless we finish the starting activity
        activity.finish();

        Log.v(TAG, "Exit");
        //XXX: We exit here to force unload the native libs
        System.exit(0);


    }

    public static void changeStatus(final VMStatus status_changed) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (status_changed == VMStatus.Running) {

                    vmStarted = true;
                } else if (status_changed == VMStatus.Ready || status_changed == VMStatus.Stopped) {

                } else if (status_changed == VMStatus.Saving) {

                } else if (status_changed == VMStatus.Paused) {

                }
            }
        });

    }

    public static void install(boolean force) {
        progDialog = ProgressDialog.show(activity, "Please Wait", "Installing BIOS...", true);
        installerTaskTask = new InstallerTask();
        installerTaskTask.force = force;
        installerTaskTask.execute();
    }

    public static void checkAndLoadLibs() {
        if (Config.loadNativeLibsEarly)
            if (Config.loadNativeLibsMainThread)
                setupNativeLibs();
            else
                setupNativeLibsAsync();
    }

    public static void clearNotifications() {
        NotificationManager notificationManager = (NotificationManager) activity.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    public static void setupNativeLibsAsync() {

        Thread thread = new Thread(new Runnable() {
            public void run() {
                setupNativeLibs();
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();

    }

    public static void savePendingEditText() {
        View currentView = activity.getCurrentFocus();
        if (currentView != null && currentView instanceof EditText) {
            ((EditText) currentView).setFocusable(false);
        }
    }

    public static void checkLog() {

        Thread t = new Thread(new Runnable() {
            public void run() {

                if (MainSettingsManager.getExitCode(activity) != 1) {
                    MainSettingsManager.setExitCode(activity, 1);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            UIUtils.promptShowLog(activity);
                        }
                    });
                }
            }
        });
        t.start();
    }

    public static void setupFolders() {
        Thread t = new Thread(new Runnable() {
            public void run() {

                Config.cacheDir = activity.getCacheDir().getAbsolutePath();
                Config.storagedir = Environment.getExternalStorageDirectory().toString();

                // Create Temp folder
                File folder = new File(Config.getTmpFolder());
                if (!folder.exists())
                    folder.mkdirs();


            }
        });
        t.start();
    }

    //XXX: sometimes this needs to be called from the main thread otherwise
    //  qemu crashes when it is started later
    public static void setupNativeLibs() {

        if (libLoaded)
            return;

        //Some devices need stl loaded upfront
        //System.loadLibrary("stlport_shared");

        //Compatibility lib
        System.loadLibrary("compat-vectras");

        //Glib deps
        System.loadLibrary("compat-musl");


        //Glib
        System.loadLibrary("glib-2.0");

        //Pixman for qemu
        System.loadLibrary("pixman-1");

        //Spice server
        if (Config.enable_SPICE) {
            System.loadLibrary("crypto");
            System.loadLibrary("ssl");
            System.loadLibrary("spice");
        }

        // //Load SDL library
        if (Config.enable_SDL) {
            System.loadLibrary("SDL2");
        }

        System.loadLibrary("compat-SDL2-ext");

        //Vectras needed for vmexecutor
        System.loadLibrary("vectras");

        loadQEMULib();

        libLoaded = true;
    }

    public static void loadQEMULib() {

        try {
            System.loadLibrary("qemu-system-i386");
        } catch (Error ex) {
            System.loadLibrary("qemu-system-x86_64");
        }

    }


    public static void setupStrictMode() {

        if (Config.debugStrictMode) {
            StrictMode.setThreadPolicy(
                    new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork()
                            //.penaltyDeath()
                            .penaltyLog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects().penaltyLog()
                    // .penaltyDeath()
                    .build());
        }

    }

    public static void onLicense() {
        PackageInfo pInfo = null;

        try {
            pInfo = activity.getPackageManager().getPackageInfo(activity.getClass().getPackage().getName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }

        final PackageInfo finalPInfo = pInfo;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });

    }

    // Main event function
    // Retrives values from saved preferences
    public static void onStartButton() {
        if (MainService.isRunning) {
            startvnc();
        } else {
            if (vmexecutor == null) {

                try {
                    vmexecutor = new StartVM(activity);
                } catch (Exception ex) {
                    UIUtils.toastLong(activity, "Error: " + ex);
                    return;

                }
            }
            // dns
            vmexecutor.dns_addr = Config.defaultDNSServer;

            vmexecutor.paused = 0;

            if (!vmStarted) {
                UIUtils.toastShort(activity, "Starting VM");
                //XXX: make sure that bios files are installed in case we ran out of space in the last
                //  run
                FileInstaller.installFiles(activity, false);
            } else {
                UIUtils.toastShort(activity, "Connecting to VM");
            }

            if (Config.ui.equals("VNC")) {
                vmexecutor.enableqmp = 1; // We enable qemu monitor
                startVNC();

            } else if (Config.ui.equals("SDL")) {
                vmexecutor.enableqmp = 0; // We disable qemu monitor
                startSDL();
            } else {
                vmexecutor.enableqmp = 1; // We enable qemu monitor
                startSPICE();
            }
        }
    }

    public static String getLanguageCode(int index) {
        // TODO: Add more languages from /assets/roms/keymaps
        switch (index) {
            case 0:
                return "en-us";
            case 1:
                return "es";
            case 2:
                return "fr";
        }
        return null;
    }

    public static void startSDL() {

        Thread tsdl = new Thread(new Runnable() {
            public void run() {
                startsdl();
            }
        });
        if (Config.maxPriority)
            tsdl.setPriority(Thread.MAX_PRIORITY);
        tsdl.start();
    }

    public static void startVNC() {

        VncCanvas.retries = 0;
        if (!vmStarted) {

            Thread tvm = new Thread(new Runnable() {
                public void run() {
                    startvm(activity, Config.UI_VNC);
                }
            });
            if (Config.maxPriority)
                tvm.setPriority(Thread.MAX_PRIORITY);
            tvm.start();
        } else {
            startvnc();
        }


    }

    public static void startSPICE() {

        if (!vmStarted) {

            Thread tvm = new Thread(new Runnable() {
                public void run() {
                    startvm(activity, Config.UI_SPICE);
                }
            });
            if (Config.maxPriority)
                tvm.setPriority(Thread.MAX_PRIORITY);
            tvm.start();
        }

    }

    public static void onStopButton(boolean exit) {
        stopVM(exit);
    }

    public static void onRestartButton() {

        execTimer();

        Machine.resetVM(activity);
    }

    public static void onResumeButton() {

        // TODO: This probably has no effect
        Thread t = new Thread(new Runnable() {
            public void run() {
                resumevm();
            }
        });
        t.start();
    }

    public static void toggleVisibility(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.GONE);
        } else if (view.getVisibility() == View.GONE || view.getVisibility() == View.INVISIBLE) {
            view.setVisibility(View.VISIBLE);
        }
    }

    public static void startvnc() {

        // Wait till Qemu settles
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Logger.getLogger(activity.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        if (MainSettingsManager.getVncExternal(activity)) {

        } else {
            connectLocally();
        }
    }

    public static void promptConnectLocally(final Activity activity) {

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                final AlertDialog alertDialog;
                alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
                alertDialog.setTitle("VNC Started");
                TextView stateView = new TextView(activity);
                stateView.setText("VNC Server started: " + getLocalIpAddress() + ":" + Config.defaultVNCPort + "\n"
                        + "Warning: VNC Connection is Unencrypted and not secure make sure you're on a private network!\n");

                stateView.setPadding(20, 20, 20, 20);
                alertDialog.setView(stateView);

                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                    }
                });
                alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Connect Locally", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        connectLocally();
                    }
                });
                alertDialog.show();
            }
        }, 100);


    }

    public static void connectLocally() {
        //UIUtils.toastShort(this, "Connecting to VM Display");
        Intent intent = getVNCIntent();
        activity.startActivityForResult(intent, Config.VNC_REQUEST_CODE);
    }

    public static void startsdl() {

        Intent intent = null;

        intent = new Intent(activity, MainSDLActivity.class);

        android.content.ContentValues values = new android.content.ContentValues();
        activity.startActivityForResult(intent, Config.SDL_REQUEST_CODE);
    }


    public static void resumevm() {
        if (vmexecutor != null) {
            vmexecutor.resumevm();
            UIUtils.toastShort(activity, "VM Reset");
        } else {

            UIUtils.toastShort(activity, "VM not running");
        }

    }

    public static Intent getVNCIntent() {
        return new Intent(activity, com.vectras.qemu.MainVNCActivity.class);

    }


    public static void goToSettings() {
        Intent i = new Intent(activity, MainSettingsManager.class);
        activity.startActivity(i);
    }

    public static void onViewLog() {

        Thread t = new Thread(new Runnable() {
            public void run() {
                FileUtils.viewVectrasLog(activity);
            }
        });
        t.start();
    }

    public static void goToURL(String url) {

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        activity.startActivity(i);

    }

    public static void stopVM(boolean exit) {
        execTimer();
        Machine.stopVM(activity);
    }

    public static void stopTimeListener() {

        synchronized (lockTime) {
            timeQuit = true;
            lockTime.notifyAll();
        }
    }


    public static void timer() {
        //XXX: No timers just ping a few times
        for (int i = 0; i < 3; i++) {
            checkAndUpdateStatus(false);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

    }

    public static void checkAndUpdateStatus(boolean force) {
        if (vmexecutor != null) {
            VMStatus status = checkStatus();
            if (force || status != currStatus) {
                currStatus = status;
                changeStatus(status);
            }
        }
    }

    public static void execTimer() {

        Thread t = new Thread(new Runnable() {
            public void run() {
                startTimer();
            }
        });
        t.start();
    }

    public static void startTimer() {
        stopTimeListener();

        timeQuit = false;
        try {
            timer();
        } catch (Exception ex) {
            ex.printStackTrace();

        }

    }


    public static enum VMStatus {
        Ready, Stopped, Saving, Paused, Completed, Failed, Unknown, Running
    }

    public static VMStatus checkStatus() {
        VMStatus state = VMStatus.Ready;
        if (vmexecutor != null && libLoaded && vmexecutor.get_state().toUpperCase().equals("RUNNING")) {
            state = VMStatus.Running;
        }
        return state;
    }

    public static class InstallerTask extends AsyncTask<Void, Void, Void> {
        public boolean force;

        @Override
        protected Void doInBackground(Void... arg0) {
            onInstall(force);
            if (progDialog.isShowing()) {
                progDialog.dismiss();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void test) {

        }
    }

}
