package com.vectras.vm.setupwizard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.anbui.elephant.retrofit2utils.Retrofit2Utils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.termux.app.TermuxActivity;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.AppConfig;
import com.vectras.vm.R;
import com.vectras.vm.VMManager;
import com.vectras.vm.creator.utils.VMCreatorSelector;
import com.vectras.vm.databinding.ActivitySetupWizard2Binding;
import com.vectras.vm.databinding.SetupQemuDoneBinding;
import com.vectras.vm.main.MainActivity;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.IntentUtils;
import com.vectras.vm.utils.ListUtils;
import com.vectras.vm.utils.PermissionUtils;
import com.vectras.vm.utils.TarUtils;
import com.vectras.vm.utils.UIUtils;
import com.vectras.vterm.Terminal2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SetupWizard2Activity extends AppCompatActivity {
    ActivitySetupWizard2Binding binding;
    SetupQemuDoneBinding bindingFinalSteps;
    public static final int ACTION_SYSTEM_UPDATE = 1;
    public static final int ACTION_CORE_SYSTEM_UPDATE = 2;
    int ACTION;
    final int STEP_REQUEST_PERMISSION = 1;
    final int STEP_EXTRACTING_SYSTEM_FILES = 2;
    final int STEP_GETTING_DATA = 3;
    final int STEP_SETUP_OPTIONS = 4;
    final int STEP_INSTALLING_PACKAGES = 5;
    final int STEP_ERROR = 6;
    final int STEP_JOIN_COMMUNITY = 7;
    final int STEP_PATERON = 8;
    final int STEP_FINISH = 9;
    final int STEP_SYSTEM_UPDATE = -1;
    int currentStep = 0;
    String logs = "";
    String selectedMirrorCommand = "echo ";
    String selectedMirrorLocation = "";
    String tarPath = "";
    String progressText ="0%";
    String qemuDownloadUrl = "";
    String qemuDownloadSha256 = "";
    boolean isExecutingCommand = false;
    boolean isLibProotError = false;
    boolean aria2Error = false;
    boolean isServerError = false;
    boolean isNotEnoughStorageSpace = false;
    boolean isCustomSetupMode = false;
    final ArrayList<HashMap<String, Object>> mirrorList = new ArrayList<>();
    ExecutorService executor = Executors.newSingleThreadExecutor();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.edgeToEdge(this);
        binding = ActivitySetupWizard2Binding.inflate(getLayoutInflater());
        bindingFinalSteps = binding.layoutFinalSteps;
        setContentView(binding.getRoot());
        UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentStep > STEP_JOIN_COMMUNITY) {
                    uiControllerFinalSteps(currentStep - 1);
                } else if (!isExecutingCommand) {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        initialize();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentStep == 1 && PermissionUtils.storagepermission(this, false)) {
            extractSystemFiles();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        loadingIndicatorController(currentStep);
    }

    private void initialize() {
        tarPath = getExternalFilesDir("data") + "/data.tar.gz";

        ListUtils.setupMirrorListForListmap(mirrorList);

        HashMap<String, Object> item = mirrorList.get(MainSettingsManager.getSelectedMirror(this));
        selectedMirrorCommand = Objects.requireNonNull(item.get("value")).toString();
        selectedMirrorLocation = Objects.requireNonNull(item.get("name")).toString();

        bindingFinalSteps.main.setVisibility(View.GONE);

        if (!DeviceUtils.is64bit()) binding.ln32BitWarning.setVisibility(View.VISIBLE);

        binding.btnLetStart.setOnClickListener(v -> {
            if (PermissionUtils.storagepermission(this, false)) {
                extractSystemFiles();
            } else {
                uiController(STEP_REQUEST_PERMISSION);
            }
        });

        binding.btnAllowPermission.setOnClickListener(v -> PermissionUtils.requestStoragePermission(this));

        binding.standardSetupOption.setOnClickListener(v -> {
            getDataForStandardSetup();
        });

        binding.customSetupOption.setOnClickListener(v -> bootstrapFilePicker.launch("*/*"));

        binding.selectMirrorOption.setOnClickListener(v -> selectMirror());

        binding.ivOpenTerminal.setOnClickListener(v -> startActivity(new Intent(this, TermuxActivity.class)));

        binding.btnTryAgain.setOnClickListener(v -> {
            if (ACTION == ACTION_SYSTEM_UPDATE) {
                uiController(STEP_SYSTEM_UPDATE);
                binding.btnSkipSystemUpdate.setVisibility(View.GONE);
            } else if (isLibProotError) {
                IntentUtils.openTelegramLink(this);
            } else {
                extractSystemFiles();
            }
        });

        //Final steps
        bindingFinalSteps.tvLater.setOnClickListener(v -> uiControllerFinalSteps(currentStep + 1));

        bindingFinalSteps.btnContinue.setOnClickListener(v -> {
            if (currentStep == STEP_JOIN_COMMUNITY) {
                uiControllerFinalSteps(currentStep + 1);
                IntentUtils.openTelegramLink(this);
                //Don't show join Telegram dialog again
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean("tgDialog", true);
                edit.apply();
            } else if (currentStep == STEP_PATERON) {
                uiControllerFinalSteps(currentStep + 1);
                IntentUtils.openUrl(this, AppConfig.patreonLink);
            } else {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });


        //System update
        binding.btnSkipSystemUpdate.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        binding.btnSystemUpdate.setOnClickListener(v -> {
            uiController(STEP_EXTRACTING_SYSTEM_FILES);
            new Thread(() -> {
                VMManager.killallqemuprocesses(this);
                if (ACTION == ACTION_CORE_SYSTEM_UPDATE) {
                    FileUtils.delete(getFilesDir().getAbsolutePath() + "/data");
                    FileUtils.delete(getFilesDir().getAbsolutePath() + "/distro");
                    FileUtils.delete(getFilesDir().getAbsolutePath() + "/usr");
                }
                runOnUiThread(this::extractSystemFiles);
            }).start();
        });

        if (getIntent().hasExtra("action")) {
            ACTION = getIntent().getIntExtra("action", -1);

            if (ACTION == ACTION_CORE_SYSTEM_UPDATE) {
                uiController(STEP_SYSTEM_UPDATE);
                binding.btnSkipSystemUpdate.setVisibility(View.GONE);
            } else if (ACTION == ACTION_SYSTEM_UPDATE) {
                uiController(STEP_SYSTEM_UPDATE);
            }
        }
    }

    private void uiController(int step) {
        uiController(step, "");
    }

    private void uiController(int step, String log) {
        TransitionManager.beginDelayedTransition(binding.main);

        binding.lnWelcome.setVisibility(View.GONE);
        binding.lnAllowPermission.setVisibility(View.GONE);
        binding.lnExtractingSystemFiles.setVisibility(View.GONE);
        binding.lnGettingData.setVisibility(View.GONE);
        binding.lnSetupOptions.setVisibility(View.GONE);
        binding.lnInstallingPackages.setVisibility(View.GONE);
        binding.lnSystemUpdate.setVisibility(View.GONE);
        binding.lnInstallingPackagesFailed.setVisibility(View.GONE);

        TransitionManager.beginDelayedTransition(binding.main);

        if (step == STEP_REQUEST_PERMISSION) {
            binding.lnAllowPermission.setVisibility(View.VISIBLE);
        } else if (step == STEP_EXTRACTING_SYSTEM_FILES) {
            binding.lnExtractingSystemFiles.setVisibility(View.VISIBLE);
        } else if (step == STEP_GETTING_DATA) {
            binding.lnGettingData.setVisibility(View.VISIBLE);
        } else if (step == STEP_SETUP_OPTIONS) {
            binding.lnSetupOptions.setVisibility(View.VISIBLE);
        } else if (step == STEP_INSTALLING_PACKAGES) {
            binding.lnInstallingPackages.setVisibility(View.VISIBLE);
        } else if (step == STEP_SYSTEM_UPDATE) {
            binding.lnSystemUpdate.setVisibility(View.VISIBLE);
        } else if (step == STEP_ERROR) {
            binding.lnInstallingPackagesFailed.setVisibility(View.VISIBLE);
            binding.tvErrorLogContent.setText(log.isEmpty() ? getString(R.string.there_are_no_logs) : log);

            if (isNotEnoughStorageSpace) {
                binding.ivErrorLarge.setImageResource(R.drawable.disc_full_100px);
                binding.tvErrorTitle.setText(getString(R.string.not_enough_storage_space));
                binding.tvErrorSubtitle.setText(getString(R.string.not_enough_storage_to_set_up_content));
                binding.btnTryAgain.setText(getString(R.string.join_our_community));
            } else if (isLibProotError) {
                binding.ivErrorLarge.setImageResource(R.drawable.error_96px);
                binding.tvErrorTitle.setText(getString(R.string.vectras_vm_cannot_run_on_this_device));
                binding.tvErrorSubtitle.setText(getString(R.string.a_serious_problem_has_occurred));
                binding.btnTryAgain.setText(getString(R.string.join_our_community));
            } else if (isServerError || aria2Error) {
                binding.ivErrorLarge.setImageResource(R.drawable.android_wifi_3_bar_alert_100px);
                binding.tvErrorTitle.setText(getString(R.string.unable_to_connect_to_server));
                binding.tvErrorSubtitle.setText(getString(R.string.check_your_internet_connection));
            } else {
                binding.ivErrorLarge.setImageResource(R.drawable.error_96px);
                binding.tvErrorTitle.setText(getString(R.string.something_went_wrong));
                binding.tvErrorSubtitle.setText(getString(R.string.the_setup_could_not_be_completed_and_below_is_the_log));
            }
        } else if (step == STEP_JOIN_COMMUNITY) {
            bindingFinalSteps.main.setVisibility(View.VISIBLE);
        }

        loadingIndicatorController(step);

        currentStep = step;
    }

    private void loadingIndicatorController(int step) {
        float dp = 200f;
        float px = dp * getResources().getDisplayMetrics().density;

        if (step == STEP_EXTRACTING_SYSTEM_FILES) {
            binding.lnExtractingSystemFilesCpiContainer.post(() -> {
                int heightPx = binding.lnExtractingSystemFilesCpiContainer.getHeight();

                if (heightPx < px) {
                    binding.cpiExtractingSystemFiles.setVisibility(View.GONE);
                    binding.lpiExtractingSystemFiles.setVisibility(View.VISIBLE);
                } else {
                    binding.cpiExtractingSystemFiles.setVisibility(View.VISIBLE);
                    binding.lpiExtractingSystemFiles.setVisibility(View.GONE);
                }
            });
        } else if (step == STEP_GETTING_DATA) {
            binding.lnGettingDataCpiContainer.post(() -> {
                int heightPx = binding.lnGettingDataCpiContainer.getHeight();

                if (heightPx < px) {
                    binding.cpiGettingData.setVisibility(View.GONE);
                    binding.lpiGettingData.setVisibility(View.VISIBLE);
                } else {
                    binding.cpiGettingData.setVisibility(View.VISIBLE);
                    binding.lpiGettingData.setVisibility(View.GONE);
                }
            });
        } else if (step == STEP_INSTALLING_PACKAGES) {
            binding.lnInstallingPackagesCpiContainer.post(() -> {
                int heightPx = binding.lnInstallingPackagesCpiContainer.getHeight();

                if (heightPx < px) {
                    binding.cpiInstallingPackages.setVisibility(View.GONE);
                    binding.lpiInstallingPackages.setVisibility(View.VISIBLE);
                } else {
                    binding.cpiInstallingPackages.setVisibility(View.VISIBLE);
                    binding.lpiInstallingPackages.setVisibility(View.GONE);
                }
            });
        }
    }

    private void uiControllerFinalSteps(int step) {
        TransitionManager.beginDelayedTransition(bindingFinalSteps.mainContent);

        bindingFinalSteps.linearcommunity.setVisibility(View.GONE);
        bindingFinalSteps.lineardonate.setVisibility(View.GONE);
        bindingFinalSteps.linearwelcomehome.setVisibility(View.GONE);

        TransitionManager.beginDelayedTransition(bindingFinalSteps.mainContent);

        if (step == STEP_JOIN_COMMUNITY) {
            bindingFinalSteps.linearcommunity.setVisibility(View.VISIBLE);
            bindingFinalSteps.tvLater.setVisibility(View.VISIBLE);
            bindingFinalSteps.btnContinue.setText(getString(R.string.join));
        } else if (step == STEP_PATERON) {
            bindingFinalSteps.lineardonate.setVisibility(View.VISIBLE);
            bindingFinalSteps.tvLater.setVisibility(View.VISIBLE);
            bindingFinalSteps.btnContinue.setText(getString(R.string.join));
        } else if (step == STEP_FINISH) {
            bindingFinalSteps.linearwelcomehome.setVisibility(View.VISIBLE);
            bindingFinalSteps.tvLater.setVisibility(View.GONE);
            bindingFinalSteps.btnContinue.setText(getString(R.string.done));
        }

        currentStep = step;
    }

    private void extractSystemFiles() {
        uiController(STEP_EXTRACTING_SYSTEM_FILES);

        executor.execute(() -> {
            isNotEnoughStorageSpace = DeviceUtils.isStorageLow(this, false);
            runOnUiThread(() -> {
                if (isNotEnoughStorageSpace) {
                    uiController(STEP_ERROR);
                    return;
                }

                new Thread(() -> {
                    boolean result = SetupFeatureCore.startExtractSystemFiles(this);

                    runOnUiThread(() -> new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (result) {
                            requestQemuArchive();
                        } else {
                            uiController(STEP_ERROR, getString(R.string.system_files_installation_failed_content) + (!SetupFeatureCore.lastErrorLog.isEmpty() ? "\n\n" + SetupFeatureCore.lastErrorLog : ""));
                        }
                    }, 1000));
                }).start();
            });
        });
    }

    private void requestQemuArchive() {
        uiController(STEP_SETUP_OPTIONS);
        binding.standardSetupOption.setVisibility(View.VISIBLE);
        DialogUtils.twoDialog(this,
                getString(R.string.qemu_archive_required),
                getString(R.string.download_qemu_archive_content),
                getString(R.string.download_qemu_from_github),
                getString(R.string.select_qemu_archive),
                true, R.drawable.folder_24px, true,
                this::getDataForStandardSetup,
                () -> bootstrapFilePicker.launch("*/*"),
                null);
    }

    private void getDataForStandardSetup() {
        if (isExecutingCommand) return;
        isExecutingCommand = true;
        uiController(STEP_GETTING_DATA);
        qemuDownloadUrl = "";
        qemuDownloadSha256 = "";
        Retrofit2Utils.get(AppConfig.bootstrapfileslink, (isSuccess, body, status, error) -> {
            isExecutingCommand = false;
            if (isFinishing() || isDestroyed()) return;
            if (isSuccess) {
                try {
                    JsonObject manifest = JsonParser.parseString(body).getAsJsonObject();
                    String key = DeviceUtils.isArm()
                            ? (DeviceUtils.is64bit() ? "aarch64" : "armhf")
                            : (DeviceUtils.is64bit() ? "amd64" : "x86");
                    if (manifest.has(key) && !manifest.get(key).isJsonNull()) {
                        String url = manifest.get(key).getAsString();
                        // No release URL is hard-coded: the repository JSON selects the archive.
                        if (url.startsWith("https://") && Uri.parse(url).getHost() != null) {
                            qemuDownloadUrl = url;
                            String digestKey = key + "_sha256";
                            if (manifest.has(digestKey) && !manifest.get(digestKey).isJsonNull()) {
                                String digest = manifest.get(digestKey).getAsString();
                                if (!digest.matches("[0-9a-fA-F]{64}")) {
                                    throw new IllegalArgumentException("Invalid QEMU SHA-256 in setup JSON");
                                }
                                qemuDownloadSha256 = digest;
                            }
                        }
                    }
                } catch (Exception e) {
                    qemuDownloadUrl = "";
                    qemuDownloadSha256 = "";
                    Log.e("SetupWizard2Activity", "Invalid QEMU setup JSON", e);
                }
            } else {
                Log.e("SetupWizard2Activity", "Could not load QEMU setup JSON: " + status, error);
            }
            if (!qemuDownloadUrl.isEmpty()) {
                isCustomSetupMode = false;
                startSetup();
            } else {
                uiController(STEP_ERROR, getString(R.string.qemu_online_unavailable_content,
                        AppConfig.bootstrapfileslink, Build.SUPPORTED_ABIS[0]));
            }
        });
    }

    private void startSetup() {
        if (isExecutingCommand) return;
        if (!isCustomSetupMode && qemuDownloadUrl.isEmpty()) {
            requestQemuArchive();
            return;
        }
        isExecutingCommand = true;
        uiController(STEP_INSTALLING_PACKAGES);

        new Thread(() -> {
            final String checkQemuCommand;
            final String downloadQemuCommand;
            try {
                checkQemuCommand = SetupFeatureCore.readTextFromAssets(this, "setup/check-qemu.sh");
                downloadQemuCommand = isCustomSetupMode ? "" :
                        SetupFeatureCore.readTextFromAssets(this, "setup/download-qemu.sh");
            } catch (Exception e) {
                isExecutingCommand = false;
                runOnUiThread(() -> uiController(STEP_ERROR, e.toString()));
                return;
            }
            if (isCustomSetupMode) {
                runOnUiThread(() -> appendTextAndScroll(" | " + getString(R.string.checking)));

                try {
                    if (!TarUtils.isAllowExtract(tarPath)) {
                        isExecutingCommand = false;
                        runOnUiThread(() -> uiController(STEP_ERROR, getString(R.string.this_bootstrap_file_is_invalid)));
                        return;
                    }
                } catch (Exception e) {
                    isExecutingCommand = false;
                    runOnUiThread(() -> uiController(STEP_ERROR, e.toString()));
                    return;
                }
            }

            runOnUiThread(() -> {
                logs = "";
                progressText = "";
                aria2Error = false;
                isServerError = false;

                String cmd = selectedMirrorCommand + ";" +
                        " set -e;" +
                        " echo \"Starting setup...\";" +
                        " apk update;" +
                        " echo \"Installing packages...\";" +
                        " apk add " + (DeviceUtils.is64bit() ? AppConfig.neededPkgs()
                        : AppConfig.neededPkgs32bit()) + ";";

                String qemuArchivePath = isCustomSetupMode ? tarPath : "/root/setup.tar.gz";
                if (!isCustomSetupMode) {
                    // Verify the complete download before removing an installed QEMU during updates.
                    cmd += " set -- " + shellQuote(qemuDownloadUrl) + " " +
                            shellQuote(qemuDownloadSha256) + ";\n" + downloadQemuCommand + "\n";
                }

                if (ACTION == ACTION_SYSTEM_UPDATE) {
                    cmd += "echo \"Uninstalling...\";" +
                            "set +e;" +
                            " rm -f /usr/local/bin/elf2dmp;" +
                            " rm -f /usr/local/bin/qemu-*;" +
                            " rm -f /usr/local/include/qemu-*;" +
                            " rm -f /usr/local/libexec/qemu-*;" +
                            " rm -f /usr/local/libexec/vhost-user-gpu;" +
                            " rm -f /usr/local/libexec/virtfs-proxy-helper;" +
                            " rm -f /usr/local/libexec/virtiofsd;" +
                            " rm -rf /usr/local/share/doc/qemu;" +
                            " rm -f /usr/local/share/applications/qemu.desktop;" +
                            " rm -f /usr/local/share/icons/hicolor/*/qemu.png;" +
                            " rm -f /usr/local/share/locale/*/qemu.mo;" +
                            " rm -f /usr/local/share/man/man1/qemu*;" +
                            " rm -f /usr/local/share/man/man1/virtfs-proxy-helper.1;" +
                            " rm -f /usr/local/share/man/man1/virtiofsd.1;" +
                            " rm -f /usr/local/share/man/man7/qemu*;" +
                            " rm -f /usr/local/share/man/man8/qemu*;" +
                            " rm -rf /usr/local/share/qemu;" +
                            " set -e;";
                }

                cmd += " echo \"Installing Qemu...\";" +
                        " tar -xzvf " + shellQuote(qemuArchivePath) + " -C /;" +
                        " rm " + shellQuote(qemuArchivePath) + ";" +
                        " chmod 755 /usr/local/bin/*;";
                if (ACTION != ACTION_SYSTEM_UPDATE) {
                    cmd += " echo \"Just a sec...\";" +
                            " mkdir -p ~/.vnc && echo -e \"555555\\n555555\" | vncpasswd -f > ~/.vnc/passwd && chmod 0600 ~/.vnc/passwd;";
                }

                cmd += "\n" + checkQemuCommand + "\necho \"Installation successful! xssFjnj58Id\"";

                execute(cmd);
            });
        }).start();
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private final ActivityResultLauncher<String> bootstrapFilePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    String abi = Build.SUPPORTED_ABIS[0];
                    if (FileUtils.getFileNameFromUri(this, uri).endsWith(abi + ".tar.gz")) {
                        uiController(STEP_INSTALLING_PACKAGES);
                        new Thread(() -> {
                            try {
                                FileUtils.copyFileFromUri(this, uri, tarPath);
                                runOnUiThread(() -> {
                                    isCustomSetupMode = true;
                                    startSetup();
                                });
                            } catch (Exception e) {
                                runOnUiThread(() -> uiController(STEP_ERROR, getString(R.string.the_file_could_not_be_processed_content)));
                            }
                        }).start();
                    } else {
                        DialogUtils.oneDialog(this,
                                getString(R.string.invalid_file),
                                getString(R.string.please_select) + " vectras-vm-" + abi + ".tar.gz.",
                                getResources().getString(R.string.ok),
                                true,
                                R.drawable.warning_48px,
                                true,
                                null,
                                null);
                    }
                }
            });

    private void execute(String command) {
        Terminal2 terminal2 = new Terminal2(this);
        terminal2.execute(command, new Terminal2.Terminal2Callback() {
            @Override
            public void onRunning(String command, String newLine) {
                runOnUiThread(() -> appendTextAndScroll(newLine + "\n"));
            }

            @Override
            public void onFinished(String command, String log, int status) {
                isExecutingCommand = false;
                if (status == terminal2.SUCCESS && log.contains("Installation successful! xssFjnj58Id")) {
                    runOnUiThread(() -> {
                        MainSettingsManager.setStandardSetupVersion(SetupWizard2Activity.this, AppConfig.standardSetupVersion);
                        MainSettingsManager.setCoreSetupVersion(SetupWizard2Activity.this, AppConfig.coreSetupVersion);
                        MainSettingsManager.setsetUpWithManualSetupBefore(SetupWizard2Activity.this, isCustomSetupMode);
                        uiController(STEP_JOIN_COMMUNITY);
                        if (ACTION == ACTION_SYSTEM_UPDATE) uiControllerFinalSteps(STEP_FINISH);
                    });
                } else {
                    runOnUiThread(() -> {
                        String toastMessage = "Command failed with exit code: " + status;
                        appendTextAndScroll("Error: " + toastMessage + "\n");
                        uiController(STEP_ERROR, logs);
                    });
                }
            }

            @Override
            public void onError(String command, Exception exception) {
                isExecutingCommand = false;
                runOnUiThread(() -> {
                    appendTextAndScroll("Error: " + exception.getMessage() + "\n");
                    uiController(STEP_ERROR, logs);
                });
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void appendTextAndScroll(String newLog) {
        logs += newLog;

        if (newLog.contains("libproot.so --help") || newLog.contains("/bin/sh: can't fork:")) {
            isLibProotError = true;
        } else if (newLog.contains("not complete: /root/setup.tar.gz")) {
            aria2Error = true;
        } else if (newLog.contains("temporary error")) {
            isServerError = true;
        } else if (newLog.contains("QEMU download failed")) {
            isServerError = true;
        }

        if (newLog.contains("Starting setup...")) {
            progressText = "5% | ";
        } else if (newLog.contains("fetch http")) {
            progressText = "10% | ";
        } else if (newLog.contains("Installing packages...")) {
            progressText = "20% | ";
        } else if (newLog.contains("(50/")) {
            progressText = "25% | ";
        } else if (newLog.contains("100/")) {
            progressText = "30% | ";
        } else if (newLog.contains("150/")) {
            progressText = "35% | ";
        } else if (newLog.contains("200/")) {
            progressText = "40% | ";
        } else if (newLog.contains("250/")) {
            progressText = "50% | ";
        } else if (newLog.contains("300/")) {
            progressText = "60% | ";
        } else if (newLog.contains("325/")) {
            progressText = "65% | ";
        } else if (newLog.contains("350/")) {
            progressText = "68% | ";
        } else if (newLog.contains("375/")) {
            progressText = "69% | ";
        } else if (newLog.contains("Downloading Qemu...") || newLog.contains("tar -xzvf ")) {
            progressText = "70% | ";
        } else if (newLog.contains("Installing Qemu...")) {
            progressText = "75% | ";
        } else if (newLog.contains("qemu-system")) {
            progressText = "80% | ";
        } else if (newLog.contains("Just a sec...")) {
            progressText = "95% | ";
        }

        binding.tvLastestCommandResult.setText(progressText + newLog);
    }

    private void selectMirror() {
        VMCreatorSelector.showDialog(this, mirrorList, MainSettingsManager.getSelectedMirror(this), ((position, name, value) -> {
            selectedMirrorCommand = value;
            selectedMirrorLocation = name;
            MainSettingsManager.setSelectedMirror(SetupWizard2Activity.this, position);
        }), getString(R.string.mirrors));
    }

    public String getPath(Uri uri) {
        return FileUtils.getPath(this, uri);
    }
}
