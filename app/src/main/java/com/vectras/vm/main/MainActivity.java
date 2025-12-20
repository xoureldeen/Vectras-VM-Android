package com.vectras.vm.main;

import static android.content.Intent.ACTION_VIEW;
import static com.vectras.vm.VectrasApp.getApp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.color.MaterialColors;
import com.termux.app.TermuxActivity;
import com.vectras.qemu.Config;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.AboutActivity;
import com.vectras.vm.AppConfig;
import com.vectras.vm.VMCreatorActivity;
import com.vectras.vm.Minitools;
import com.vectras.vm.R;
import com.vectras.vm.databinding.ActivityMainBinding;
import com.vectras.vm.databinding.ActivityMainContentBinding;
import com.vectras.vm.main.softwarestore.SoftwareStoreFragment;
import com.vectras.vm.main.softwarestore.SoftwareStoreHomeAdapterSearch;
import com.vectras.vm.network.RequestNetwork;
import com.vectras.vm.network.RequestNetworkController;
import com.vectras.vm.databinding.BottomsheetdialogLoggerBinding;
import com.vectras.vm.databinding.UpdateBottomDialogLayoutBinding;
import com.vectras.vm.main.romstore.RomStoreHomeAdapterSearch;
import com.vectras.vm.Roms.DataRoms;
import com.vectras.vm.SetArchActivity;
import com.vectras.vm.VMManager;
import com.vectras.vm.adapter.LogsAdapter;
import com.vectras.vm.main.core.CallbackInterface;
import com.vectras.vm.main.core.DisplaySystem;
import com.vectras.vm.main.core.PendingCommand;
import com.vectras.vm.main.core.SharedData;
import com.vectras.vm.main.monitor.SystemMonitorFragment;
import com.vectras.vm.main.romstore.RomStoreFragment;
import com.vectras.vm.main.vms.VmsFragment;
import com.vectras.vm.logger.VectrasStatus;
import com.vectras.vm.settings.UpdaterActivity;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.LibraryChecker;
import com.vectras.vm.utils.NotificationUtils;
import com.vectras.vm.utils.PackageUtils;
import com.vectras.vm.utils.UIUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements RomStoreFragment.RomStoreCallToHomeListener, VmsFragment.VmsCallToHomeListener, SoftwareStoreFragment.SoftwareStoreCallToHomeListener {
    private final String TAG = "HomeActivity";
    private final int SEARCH_ROM_STORE = 0;
    private final int SEARCH_SOFTWARE_STORE = 1;
    private int currentSearchMode = 0;
    public static boolean isActivate = false;
    public static boolean isNeedRecreate = false;
    public static boolean isOpenHome = false;
    public static boolean isOpenRomStore = false;
    ActivityMainBinding binding;
    ActivityMainContentBinding bindingContent;
    private RomStoreHomeAdapterSearch adapterRomStoreSearch;
    private SoftwareStoreHomeAdapterSearch adapterSoftwareStoreSearch;
    private final List<DataRoms> dataRomStoreSearch = new ArrayList<>();

    public static CallbackInterface.HomeCallToVmsListener homeCallToVmsListener;

    public static void refeshVMListNow() {
        homeCallToVmsListener.refeshVMList();
    }

    @Override
    public void updateSearchStatus(boolean isReady) {
        bindingContent.searchbar.setEnabled(isReady);
    }

    @Override
    public void openRomStore() {
        bindingContent.bottomNavigation.setSelectedItemId(R.id.item_romstore);
    }

    Handler handlerUpdateLog = new Handler(Looper.getMainLooper());
    Runnable updateLogTask = () -> {

    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        VmsFragment.vmsCallToHomeListener = this;
        RomStoreFragment.romStoreCallToHomeListener = this;
        SoftwareStoreFragment.softwareStoreCallToHomeListener = this;

//        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        bindingContent = binding.maincontent;
        setContentView(binding.getRoot());
        isActivate = true;

//        UIUtils.setOnApplyWindowInsetsListenerTop(bindingContent.main);
//        UIUtils.setOnApplyWindowInsetsListenerLeftOnly(binding.navView);
        UIUtils.setOnApplyWindowInsetsListenerBottomOnly(binding.rvRomstoresearch);
        UIUtils.setOnApplyWindowInsetsListenerBottomOnly(binding.lnSearchempty);

        initialize(bundle);
    }

    private void initialize(Bundle savedInstanceState) {
        //Any view
        getWindow().setNavigationBarColor(MaterialColors.getColor(binding.drawerLayout, com.google.android.material.R.attr.colorSurfaceContainer));

        bindingContent.efabCreate.setOnClickListener(view -> startActivity(new Intent(this, SetArchActivity.class)));

        setSupportActionBar(bindingContent.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, binding.drawerLayout, bindingContent.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(bindingContent.containerView.getId(), new VmsFragment())
                    .commit();
        }

        binding.searchview.setupWithSearchBar(bindingContent.searchbar);

        bindingContent.searchbar.inflateMenu(R.menu.searchbar_menu);
        bindingContent.searchbar.setOnMenuItemClickListener(
                menuItem -> {
                    if (menuItem.getItemId() == R.id.importrom) {
                        Intent intent = new Intent();
                        intent.setClass(getApplicationContext(), VMCreatorActivity.class);
                        intent.putExtra("importcvbinow", "");
                        startActivity(intent);
                    } else if (menuItem.getItemId() == R.id.backtothedisplay) {
                        DisplaySystem.launch(this);
                    }
                    return true;
                });

        bindingContent.searchbar.setEnabled(false);

        bindingContent.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;

            int id = item.getItemId();
            if (id == R.id.item_home) {
                selectedFragment = new VmsFragment();
                bindingContent.efabCreate.setVisibility(View.VISIBLE);
                bindingContent.searchbar.setHint(getText(R.string.home));
                bindingContent.searchbar.setEnabled(false);
            } else if (id == R.id.item_romstore) {
                selectedFragment = new RomStoreFragment();
                bindingContent.efabCreate.setVisibility(View.GONE);
                bindingContent.searchbar.setEnabled(true);
                bindingContent.searchbar.setHint(getText(R.string.search));
                currentSearchMode = SEARCH_ROM_STORE;
                adapterRomStoreSearch = new RomStoreHomeAdapterSearch(this, dataRomStoreSearch);
                binding.rvRomstoresearch.setAdapter(adapterRomStoreSearch);
            } else if (id == R.id.item_softwarestore) {
                selectedFragment = new SoftwareStoreFragment();
                bindingContent.efabCreate.setVisibility(View.GONE);
                bindingContent.searchbar.setEnabled(true);
                bindingContent.searchbar.setHint(getText(R.string.search));
                currentSearchMode = SEARCH_SOFTWARE_STORE;
                adapterSoftwareStoreSearch = new SoftwareStoreHomeAdapterSearch(this, dataRomStoreSearch);
                binding.rvRomstoresearch.setAdapter(adapterSoftwareStoreSearch);
            } else if (id == R.id.item_monitor) {
                selectedFragment = new SystemMonitorFragment();
                bindingContent.efabCreate.setVisibility(View.GONE);
                bindingContent.searchbar.setHint(getText(R.string.system_monitor));
                bindingContent.searchbar.setEnabled(false);
            } else {
                selectedFragment = new VmsFragment();
                bindingContent.efabCreate.setVisibility(View.VISIBLE);
                bindingContent.searchbar.setHint(getText(R.string.home));
                bindingContent.searchbar.setEnabled(false);
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(bindingContent.containerView.getId(), selectedFragment)
                    .commit();
            return true;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                    //Prevent apps from exiting after the drawer is closed.
                    return;
                }
                if (binding.searchview.isShowing()) {
                    binding.searchview.hide();
                } else if (bindingContent.bottomNavigation.getSelectedItemId() != R.id.item_home) {
                    bindingContent.bottomNavigation.setSelectedItemId(R.id.item_home);
                } else if (MainSettingsManager.getQuickStart(MainActivity.this)) {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    finish();
                }
            }
        });

        binding.rvRomstoresearch.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        binding.searchview.getEditText().

                addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        search(s.toString());
                    }

                    @Override
                    public void onTextChanged(CharSequence newText, int start, int before, int count) {
                    }
                });

        new LibraryChecker(this).
        checkMissingLibraries(this);

        setupDrawer();
        DialogUtils.joinTelegram(this);
        NotificationUtils.clearAll(this);

        if (MainSettingsManager.getPromptUpdateVersion(this))
            updateApp();

        NotificationUtils.requestPermission(this);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        homeCallToVmsListener.configurationChanged(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onDestroy() {
        isActivate = false;
        super.onDestroy();
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
        if (id == R.id.shutdown) {
            VMManager.requestKillAllQemuProcess(this, null);
        }

        return super.onOptionsItemSelected(item);
    }

    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        if (isNeedRecreate) {
            isNeedRecreate = false;
            recreate();
            return;
        }

        Config.ui = MainSettingsManager.getVmUi(this);
        Config.defaultVNCPort = Integer.parseInt(MainSettingsManager.getVncExternalDisplay(this));
        Config.forceRefeshVNCDisplay = MainSettingsManager.getForceRefeshVNCDisplay(this);

        if (!MainSettingsManager.getVncExternal(this))
            NotificationUtils.clearAll(this);
        Config.ui = MainSettingsManager.getVmUi(this);

        DisplaySystem.reLaunchVNC(this);
        PendingCommand.runNow(this);

        if (isOpenRomStore) {
            isOpenRomStore = false;
            bindingContent.bottomNavigation.setSelectedItemId(R.id.item_romstore);
        } else if (isOpenHome) {
            isOpenHome = false;
            if (binding.searchview.isShowing()) binding.searchview.hide();
            bindingContent.bottomNavigation.setSelectedItemId(R.id.item_home);
        }

        new Handler(Looper.getMainLooper()).post(() -> DisplaySystem.startTermuxX11(this));
    }

    private void setupDrawer() {
        binding.drawerLayout.setScrimColor(Color.parseColor("#40000000")); //25%

        //Setting Navigation View Item Selected Listener to handle the item click of the navigation menu
        // This method will trigger on item Click of navigation menu
        binding.navView.setNavigationItemSelectedListener(menuItem -> {
            //Closing drawer on item click
            binding.drawerLayout.closeDrawers();

            //Check to see which item was being clicked and perform appropriate action
            int id = menuItem.getItemId();
            if (id == R.id.navigation_item_info) {
                startActivity(new Intent(this, AboutActivity.class));
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
            } else if (id == R.id.navigation_item_desktop) {
                DisplaySystem.launchX11(this, true);
            } else if (id == R.id.navigation_item_terminal) {
                if (DeviceUtils.is64bit()) {
                    startActivity(new Intent(this, TermuxActivity.class));
                } else {
                    com.vectras.vterm.TerminalBottomSheetDialog VTERM = new com.vectras.vterm.TerminalBottomSheetDialog(this);
                    VTERM.showVterm();
                }
            } else if (id == R.id.navigation_item_view_logs) {
                showLogsDialog();
            } else if (id == R.id.navigation_item_settings) {
                startActivity(new Intent(this, MainSettingsManager.class));
            } else if (id == R.id.navigation_data_explorer) {
//                startActivity(new Intent(this, DataExplorerActivity.class));
                FileUtils.openFolder(this, AppConfig.maindirpath);
            } else if (id == R.id.navigation_item_donate) {
                String tw = "https://www.patreon.com/VectrasTeam";
                Intent w = new Intent(ACTION_VIEW);
                w.setData(Uri.parse(tw));
                startActivity(w);
            } else if (id == R.id.mini_tools) {
                Intent intent = new Intent();
                intent.setClass(this, Minitools.class);
                startActivity(intent);
            }
            return false;
        });
    }

    private void updateApp() {
        int versionCode = PackageUtils.getThisVersionCode(getApplicationContext());
//        String versionName = PackageUtils.getThisVersionName(getApplicationContext());

        RequestNetwork requestNetwork = new RequestNetwork(this);
        RequestNetwork.RequestListener requestNetworkListener = new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
                if (!response.isEmpty()) {
                    try {
                        final JSONObject obj = new JSONObject(response);
                        String versionNameonUpdate;
                        int versionCodeonUpdate;
//                        String message;
//                        String size;

                        if (MainSettingsManager.getcheckforupdatesfromthebetachannel(MainActivity.this)) {
                            versionNameonUpdate = obj.getString("versionNameBeta");
                            versionCodeonUpdate = obj.getInt("versionCodeBeta");
//                            message = obj.getString("MessageBeta");
//                            size = obj.getString("sizeBeta");
                        } else {
                            versionNameonUpdate = obj.getString("versionName");
                            versionCodeonUpdate = obj.getInt("versionCode");
//                            message = obj.getString("Message");
//                            size = obj.getString("size");
                        }

                        if ((versionCode < versionCodeonUpdate &&
                                !MainSettingsManager.getSkipVersion(MainActivity.this).equals(versionNameonUpdate))) {

                            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this);
                            UpdateBottomDialogLayoutBinding updateBottomDialogLayoutBinding = UpdateBottomDialogLayoutBinding.inflate(getLayoutInflater());
                            bottomSheetDialog.setContentView(updateBottomDialogLayoutBinding.getRoot());

//                            TextView tvContent = v.findViewById(R.id.tv_content);

//                            tvContent.setMovementMethod(LinkMovementMethod.getInstance());
//                            tvContent.setText(Html.fromHtml(message + "<br><br>Update size:<br>" + size));

                            updateBottomDialogLayoutBinding.bnSkip.setOnClickListener(view -> {
                                MainSettingsManager.setSkipVersion(MainActivity.this, versionNameonUpdate);
                                bottomSheetDialog.dismiss();
                            });

                            updateBottomDialogLayoutBinding.bnLater.setOnClickListener(view -> bottomSheetDialog.dismiss());

                            updateBottomDialogLayoutBinding.bnUpdate.setOnClickListener(view -> {
                                startActivity(new Intent(MainActivity.this, UpdaterActivity.class));
                                bottomSheetDialog.dismiss();
                            });

                            bottomSheetDialog.show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "updateApp: ", e);
                    }
                }
            }

            @Override
            public void onErrorResponse(String tag, String message) {

            }
        };

        requestNetwork.startRequestNetwork(RequestNetworkController.GET, AppConfig.updateJson, "maincheckupdate", requestNetworkListener);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void search(String keyword) {
        try {
            // Extract data from json and store into ArrayList as class objects
            List<DataRoms> filteredData = new ArrayList<>();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                filteredData = (currentSearchMode == SEARCH_ROM_STORE ? SharedData.dataRomStore.stream() : SharedData.dataSoftwareStore.stream())
                        .filter(rom -> {
                            String romName = (rom.romName != null) ? rom.romName : "";
                            String romKernel = (rom.romKernel != null) ? rom.romKernel : "";

                            return romName.toLowerCase().contains(keyword.toLowerCase())
                                    || romKernel.toLowerCase().contains(keyword.toLowerCase());
                        })
                        .collect(Collectors.toList());
            } else {
                for (DataRoms rom : (currentSearchMode == SEARCH_ROM_STORE ? SharedData.dataRomStore : SharedData.dataSoftwareStore)) {
                    if (rom.romName.toLowerCase().contains(keyword.toLowerCase()) ||
                            rom.romKernel.toLowerCase().contains(keyword.toLowerCase())) {
                        filteredData.add(rom);
                    }
                }
            }

            dataRomStoreSearch.clear();
            dataRomStoreSearch.addAll(filteredData);
        } catch (Exception e) {
            Log.e("RomManagerActivity", "Json parsing error: " + e.getMessage());
        }

        if (dataRomStoreSearch.isEmpty())
            binding.rvRomstoresearch.setVisibility(View.GONE);
        else
            binding.rvRomstoresearch.setVisibility(View.VISIBLE);

        adapterRomStoreSearch.notifyDataSetChanged();
    }

    private void showLogsDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        BottomsheetdialogLoggerBinding bottomsheetdialogLoggerBinding = BottomsheetdialogLoggerBinding.inflate(getLayoutInflater());
        bottomSheetDialog.setContentView(bottomsheetdialogLoggerBinding.getRoot());
        bottomSheetDialog.show();

        LinearLayoutManager layoutManager = new LinearLayoutManager(getApp());
        LogsAdapter mLogAdapter = new LogsAdapter(layoutManager, getApp());
        bottomsheetdialogLoggerBinding.recyclerLog.setAdapter(mLogAdapter);
        bottomsheetdialogLoggerBinding.recyclerLog.setLayoutManager(layoutManager);
        mLogAdapter.scrollToLastPosition();

        AtomicBoolean isStop = new AtomicBoolean(false);

        try {
            Process process = Runtime.getRuntime().exec("logcat -e");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            Process process2 = Runtime.getRuntime().exec("logcat -w");
            BufferedReader bufferedReader2 = new BufferedReader(
                    new InputStreamReader(process2.getInputStream()));


            updateLogTask = new Runnable() {
                @Override
                public void run() {
                    if (isStop.get()) return;

                    try {
                        if (bufferedReader.readLine() != null || bufferedReader2.readLine() != null) {
                            String logLine = bufferedReader.readLine();
                            String logLine2 = bufferedReader2.readLine();
                            VectrasStatus.logError("<font color='red'>[E] " + logLine + "</font>");
                            VectrasStatus.logError("<font color='#FFC107'>[W] " + logLine2 + "</font>");
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Log: ", e);
                    }
                    handlerUpdateLog.postDelayed(this, 1000);
                }
            };

            handlerUpdateLog.post(updateLogTask);
        } catch (IOException e) {
            Log.e(TAG, "Log: ", e);
        }

        bottomSheetDialog.setOnDismissListener(menuItem1 -> {
            isStop.set(true);
            handlerUpdateLog.removeCallbacks(updateLogTask);
        });
    }
}