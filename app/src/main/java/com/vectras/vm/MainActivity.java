package com.vectras.vm;

import static android.os.Build.VERSION.SDK_INT;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
import androidx.viewpager.widget.ViewPager;

import com.google.firebase.auth.FirebaseAuth;
import com.vectras.qemu.Config;
import com.vectras.qemu.MainActivityCommon;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.jni.StartVM;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.TimerTask;
import java.util.Timer;

public class MainActivity extends AppCompatActivity {

    private static Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    public static final String TAG = "Main Activity";
    // Static
    public static final String CREDENTIAL_SHARED_PREF = "settings_prefs";

    public static MainActivity activity = null;
    private FirebaseAuth mAuth;
    public DrawerLayout mainDrawer;
    public Toolbar mainToolbar;
    public static TextView totalRam;
    public static TextView usedRam;
    public static TextView freeRam;
    public static TextView ipTxt;
    public FloatingActionButton mStart;
    private Timer _timer = new Timer();
    private TimerTask t;
    public ViewPager viewPager;
    MenuItem prevMenuItem;
    int pager_number = 2;

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

    public void FAB_Click(View v) {
		/*Thread thread = new Thread(new Runnable() {
			public void run() {
				MainActivity.onStartButton();
			}
		});
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();*/
        if (HomeFragment.romsLayout.getVisibility() == View.GONE) {
            Animation animation;
            animation = AnimationUtils.loadAnimation(MainActivity.activity, R.anim.slide_from_left);
            animation.setDuration(300);
            HomeFragment.romsLayout.startAnimation(animation);
            Animation animation2;
            animation2 = AnimationUtils.loadAnimation(MainActivity.activity, R.anim.slide_from_top);
            animation2.setDuration(300);
            HomeFragment.mRVBlog.startAnimation(animation2);
            HomeFragment.romsLayout.setVisibility(View.VISIBLE);
        } else if (HomeFragment.romsLayout.getVisibility() == View.VISIBLE) {
            Animation animation;
            animation = AnimationUtils.loadAnimation(MainActivity.activity, R.anim.slide_to_left);
            animation.setDuration(300);
            HomeFragment.romsLayout.startAnimation(animation);
            Animation animation2;
            animation2 = AnimationUtils.loadAnimation(MainActivity.activity, R.anim.slide_from_top);
            animation2.setDuration(300);
            HomeFragment.mRVBlog.startAnimation(animation2);
            HomeFragment.romsLayout.setVisibility(View.GONE);
        }
    }

    // This is easier: traverse the interfaces and get the local IPs
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

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        MainActivityCommon.activity = this;
        MainActivityCommon.clearNotifications();
        MainActivityCommon.setupFolders();
        MainActivityCommon.setupStrictMode();
        MainActivityCommon.execTimer();
        MainActivityCommon.checkAndLoadLibs();
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
            AppBarLayout nnl_appbar = findViewById(R.id.nnl_appbar);
            if (nnl_appbar.getTop() < 0)
                nnl_appbar.setExpanded(true);
            else
                nnl_appbar.setExpanded(false);

        }

        return super.onOptionsItemSelected(item);
    }

    private void initNavigationMenu() {
        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation_view);

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

    // Setting up the UI
    public void setupWidgets() {
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new MyAdapter(getSupportFragmentManager()));
        viewPager.setOffscreenPageLimit(pager_number);
        final BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation_view);
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

        AppBarLayout nnl_appbar = findViewById(R.id.nnl_appbar);
        nnl_appbar.setExpanded(false);
        mainToolbar = (Toolbar) findViewById(R.id.nnl_toolbar);
        setSupportActionBar(mainToolbar);
        mainDrawer = (DrawerLayout) findViewById(R.id.nnl_drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mainDrawer, mainToolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mainDrawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        View headerView = navigationView.getHeaderView(0);
        TextView navUsername = (TextView) headerView.findViewById(R.id.usernameTxt);
        navUsername.setText(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        TextView navEmail = (TextView) headerView.findViewById(R.id.emailTxt);
        navEmail.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());

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
                        freeRam.setText("Free Memory: " + freeMemory + " MB (" + RamInfo.vectrasMemory() + ")");
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

        mStart = (FloatingActionButton) findViewById(R.id.nnl_fab);
        mStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                FAB_Click(view);

            }
        });
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
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
    }

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.nnl_drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void goToURL(String url) {

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        activity.startActivity(i);

    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MainActivityCommon.stopTimeListener();

    }

    public void onPause() {
        super.onPause();
        MainActivityCommon.stopTimeListener();
    }

    public void onResume() {

        super.onResume();
        MainActivityCommon.execTimer();
    }


}
