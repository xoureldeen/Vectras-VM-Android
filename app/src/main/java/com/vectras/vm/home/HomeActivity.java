package com.vectras.vm.home;

import static android.content.Intent.ACTION_VIEW;
import static com.vectras.vm.VectrasApp.getApp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.termux.app.TermuxActivity;
import com.vectras.qemu.Config;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.AboutActivity;
import com.vectras.vm.AppConfig;
import com.vectras.vm.CustomRomActivity;
import com.vectras.vm.Minitools;
import com.vectras.vm.R;
import com.vectras.vm.RequestNetwork;
import com.vectras.vm.RequestNetworkController;
import com.vectras.vm.home.romstore.RomStoreHomeAdapterSearch;
import com.vectras.vm.Roms.DataRoms;
import com.vectras.vm.RomsManagerActivity;
import com.vectras.vm.SetArchActivity;
import com.vectras.vm.StoreActivity;
import com.vectras.vm.VMManager;
import com.vectras.vm.adapter.LogsAdapter;
import com.vectras.vm.databinding.ActivityHomeBinding;
import com.vectras.vm.databinding.ActivityHomeContentBinding;
import com.vectras.vm.home.core.CallbackInterface;
import com.vectras.vm.home.core.DisplaySystem;
import com.vectras.vm.home.core.PendingCommand;
import com.vectras.vm.home.core.SharedData;
import com.vectras.vm.home.monitor.SystemMonitorFragment;
import com.vectras.vm.home.romstore.RomStoreFragment;
import com.vectras.vm.home.vms.VmsFragment;
import com.vectras.vm.logger.VectrasStatus;
import com.vectras.vm.settings.UpdaterActivity;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.LibraryChecker;
import com.vectras.vm.utils.NotificationUtils;
import com.vectras.vm.utils.PackageUtils;
import com.vectras.vm.utils.UIUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class HomeActivity extends AppCompatActivity implements RomStoreFragment.RomStoreCallToHomeListener, VmsFragment.VmsCallToHomeListener {
    private final String TAG = "HomeActivity";
    public static boolean isActivate = false;
    public static boolean isOpenRomStore = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    ActivityHomeBinding binding;
    ActivityHomeContentBinding bindingContent;
    private RomStoreHomeAdapterSearch adapterRomStoreSearch;
    private final List<DataRoms> dataRomStoreSearch = new ArrayList<>();

    public static CallbackInterface.HomeCallToVmsListener homeCallToVmsListener;

    public static void refeshVMListNow() {
        homeCallToVmsListener.refeshVMList();
    }

    @Override
    public void updateDataStatus(boolean isReady) {
        bindingContent.searchbar.setEnabled(isReady);
    }

    @Override
    public void openRomStore() {
        bindingContent.bottomNavigation.setSelectedItemId(R.id.item_romstore);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        VmsFragment.vmsCallToHomeListener = this;
        RomStoreFragment.romStoreCallToHomeListener = this;

//        EdgeToEdge.enable(this);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        bindingContent = binding.maincontent;
        setContentView(binding.getRoot());
        isActivate = true;

//        UIUtils.setOnApplyWindowInsetsListenerTop(bindingContent.main);
//        UIUtils.setOnApplyWindowInsetsListenerLeftOnly(binding.navView);
        UIUtils.setOnApplyWindowInsetsListenerBottomOnly(binding.rvRomstoresearch);
//        UIUtils.setOnApplyWindowInsetsListenerBottomOnly(binding.lnSearchempty);

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
                        intent.setClass(getApplicationContext(), CustomRomActivity.class);
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
                } else if (MainSettingsManager.getQuickStart(HomeActivity.this)){
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    finish();
                }
            }
        });

        adapterRomStoreSearch = new RomStoreHomeAdapterSearch(this, dataRomStoreSearch);
        binding.rvRomstoresearch.setAdapter(adapterRomStoreSearch);
        binding.rvRomstoresearch.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        binding.searchview.getEditText().addTextChangedListener(new TextWatcher() {
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

        new LibraryChecker(this).checkMissingLibraries(this);

        setupDrawer();
        DisplaySystem.startTermuxX11();
        DialogUtils.joinTelegram(this);
        NotificationUtils.clearAll(this);

        if (MainSettingsManager.getPromptUpdateVersion(this))
            updateApp();
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

        }
    }

    private final ActivityResultLauncher<String> isoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;

                if (VMManager.isAISOFile(FileUtils.getFileNameFromUri(this, uri))) {
                    importFile(uri, AppConfig.importedDriveFolder + "/drive.iso");
                } else {
                    DialogUtils.twoDialog(this,
                            getString(R.string.problem_has_been_detected),
                            getString(R.string.file_format_is_not_supported),
                            getResources().getString(R.string.continuetext),
                            getResources().getString(R.string.cancel),
                            true,
                            R.drawable.album_24px,
                            true,
                            () -> importFile(uri, AppConfig.importedDriveFolder + "/drive.iso"),
                            null,
                            null);
                }
            });

    private final ActivityResultLauncher<String> hdd1Picker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;

                if (VMManager.isADiskFile(FileUtils.getFileNameFromUri(this, uri))) {
                    importFile(uri, AppConfig.importedDriveFolder + "/hdd1.qcow2");
                } else {
                    DialogUtils.twoDialog(this,
                            getString(R.string.problem_has_been_detected),
                            getString(R.string.file_format_is_not_supported),
                            getResources().getString(R.string.continuetext),
                            getResources().getString(R.string.cancel),
                            true,
                            R.drawable.hard_drive_24px,
                            true,
                            () -> importFile(uri, AppConfig.importedDriveFolder + "/hdd1.qcow2"),
                            null,
                            null);
                }
            });

    private final ActivityResultLauncher<String> hdd2Picker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;

                if (VMManager.isADiskFile(FileUtils.getFileNameFromUri(this, uri))) {
                    importFile(uri, AppConfig.importedDriveFolder + "/hdd2.qcow2");
                } else {
                    DialogUtils.twoDialog(this,
                            getString(R.string.problem_has_been_detected),
                            getString(R.string.file_format_is_not_supported),
                            getResources().getString(R.string.continuetext),
                            getResources().getString(R.string.cancel),
                            true,
                            R.drawable.hard_drive_24px,
                            true,
                            () -> importFile(uri, AppConfig.importedDriveFolder + "/hdd2.qcow2"),
                            null,
                            null);
                }
            });

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
            } else if (id == R.id.navigation_item_import_iso) {
                if (new File(AppConfig.importedDriveFolder + "/drive.iso").exists()) {
                    DialogUtils.threeDialog(
                            this,
                            "Replace iso",
                            "There is iso imported you want to replace it?",
                            getString(R.string.replace),
                            getString(R.string.cancel),
                            getString(R.string.remove),
                            true,
                            R.drawable.album_24px,
                            true,
                            () -> isoPicker.launch("*/*"),
                            null,
                            () -> {
                                try {
                                    File isoFile = new File(AppConfig.importedDriveFolder + "/drive.iso");
                                    DialogUtils.fileDeletionResult(this, isoFile.delete());
                                } catch (Exception e) {
                                    DialogUtils.fileDeletionResult(this, false);
                                }
                            },
                            null);
                } else {
                    isoPicker.launch("*/*");
                }
            } else if (id == R.id.navigation_item_hdd1) {
                if (new File(AppConfig.importedDriveFolder + "/hdd1.qcow2").exists()) {
                    DialogUtils.threeDialog(
                            this,
                            "Replace HDD1",
                            "There is HDD1 imported you want to replace it?",
                            getString(R.string.replace),
                            getString(R.string.cancel),
                            getString(R.string.remove),
                            true,
                            R.drawable.hard_drive_24px,
                            true,
                            () -> hdd1Picker.launch("*/*"),
                            null,
                            () -> {
                                try {
                                    File hdd1File = new File(AppConfig.importedDriveFolder + "/hdd1.qcow2");
                                    DialogUtils.fileDeletionResult(this, hdd1File.delete());
                                } catch (Exception e) {
                                    DialogUtils.fileDeletionResult(this, false);
                                }
                            },
                            null);
                } else {
                    hdd1Picker.launch("*/*");
                }
            } else if (id == R.id.navigation_item_hdd2) {
                if (new File(AppConfig.importedDriveFolder + "/hdd2.qcow2").exists()) {
                    DialogUtils.threeDialog(
                            this,
                            "Replace HDD2",
                            "There is HDD2 imported you want to replace it?",
                            getString(R.string.replace),
                            getString(R.string.cancel),
                            getString(R.string.remove),
                            true,
                            R.drawable.hard_drive_24px,
                            true,
                            () -> hdd2Picker.launch("*/*"),
                            null,
                            () -> {
                                try {
                                    File hdd1File = new File(AppConfig.importedDriveFolder + "/hdd2.qcow2");
                                    DialogUtils.fileDeletionResult(this, hdd1File.delete());
                                } catch (Exception e) {
                                    DialogUtils.fileDeletionResult(this, false);
                                }
                            },
                            null);
                } else {
                    hdd2Picker.launch("*/*");
                }
            } else if (id == R.id.navigation_item_desktop) {
                DisplaySystem.launchX11(this, true);
            } else if (id == R.id.navigation_item_terminal) {
                /*com.vectras.vterm.TerminalBottomSheetDialog VTERM = new com.vectras.vterm.TerminalBottomSheetDialog(activity);
                VTERM.showVterm();*/
                startActivity(new Intent(this, TermuxActivity.class));
            } else if (id == R.id.navigation_item_view_logs) {
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
                View view = getLayoutInflater().inflate(R.layout.bottomsheetdialog_logger, null);
                bottomSheetDialog.setContentView(view);
                bottomSheetDialog.show();

//                final String CREDENTIAL_SHARED_PREF = "settings_prefs";
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
                    _timer.scheduleAtFixedRate(t, 0, 100);
                } catch (IOException e) {
                    Log.e(TAG, "Log: ", e);
                }
            } else if (id == R.id.navigation_item_settings) {
                startActivity(new Intent(this, MainSettingsManager.class));
            } else if (id == R.id.navigation_item_store) {
                startActivity(new Intent(this, StoreActivity.class));
            } else if (id == R.id.navigation_data_explorer) {
//                startActivity(new Intent(this, DataExplorerActivity.class));
                FileUtils.openFolder(this, AppConfig.maindirpath);
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
                intent.setClass(this, Minitools.class);
                startActivity(intent);
            }
            return false;
        });
    }

    private void updateApp() {
        int versionCode = PackageUtils.getThisVersionCode(getApplicationContext());
        String versionName = PackageUtils.getThisVersionName(getApplicationContext());

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

                        if (MainSettingsManager.getcheckforupdatesfromthebetachannel(HomeActivity.this)) {
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

                        if ((versionCode < versionCodeonUpdate ||
                                !versionNameonUpdate.equals(versionName)) &&
                                !MainSettingsManager.getSkipVersion(HomeActivity.this).equals(versionNameonUpdate)) {

                            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(HomeActivity.this);
                            View v = getLayoutInflater().inflate(R.layout.update_bottom_dialog_layout, null);
                            bottomSheetDialog.setContentView(v);

//                            TextView tvContent = v.findViewById(R.id.tv_content);
                            MaterialButton skipButton = v.findViewById(R.id.bn_skip);
                            MaterialButton laterButton = v.findViewById(R.id.bn_later);
                            MaterialButton updateButton = v.findViewById(R.id.bn_update);

//                            tvContent.setMovementMethod(LinkMovementMethod.getInstance());
//                            tvContent.setText(Html.fromHtml(message + "<br><br>Update size:<br>" + size));

                            skipButton.setOnClickListener(view -> {
                                MainSettingsManager.setSkipVersion(HomeActivity.this, versionNameonUpdate);
                                bottomSheetDialog.dismiss();
                            });

                            laterButton.setOnClickListener(view -> bottomSheetDialog.dismiss());

                            updateButton.setOnClickListener(view -> {
                                startActivity(new Intent(HomeActivity.this, UpdaterActivity.class));
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
            Gson gson = new Gson();
            List<DataRoms> filteredData = new ArrayList<>();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                filteredData = SharedData.dataRomStore.stream()
                        .filter(rom -> {
                            String romName = (rom.romName != null) ? rom.romName : "";
                            String romKernel = (rom.romKernel != null) ? rom.romKernel : "";

                            return romName.toLowerCase().contains(keyword.toLowerCase())
                                    || romKernel.toLowerCase().contains(keyword.toLowerCase());
                        })
                        .collect(Collectors.toList());
            } else {
                for (DataRoms rom : SharedData.dataRomStore) {
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

    private void importFile(Uri uri, String copyTo) {
        if (uri == null) return;

        View progressView = LayoutInflater.from(this).inflate(R.layout.dialog_progress_style, null);
        TextView progress_text = progressView.findViewById(R.id.progress_text);
        progress_text.setText(getString(R.string.importing_file));
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this, R.style.CenteredDialogTheme)
                .setView(progressView)
                .setCancelable(false)
                .create();
        progressDialog.show();

        AtomicBoolean isCompleted = new AtomicBoolean(false);
        executor.execute(() -> {
            try {
                FileUtils.copyFileFromUri(this, uri, copyTo);
                isCompleted.set(true);
            } catch (Exception e) {
                isCompleted.set(false);
            } finally {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    DialogUtils.oneDialog(
                            this,
                            isCompleted.get() ? getString(R.string.imported) : getString(R.string.oops),
                            isCompleted.get() ? getString(R.string.file_imported_successfully) : getString(R.string.file_import_failed),
                            getString(R.string.ok),
                            true,
                            isCompleted.get() ? R.drawable.check_24px : R.drawable.error_96px,
                            true,
                            null,
                            null
                    );
                });
            }
        });
    }
}