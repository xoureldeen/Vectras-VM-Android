package com.vectras.vm;

import static android.content.Intent.ACTION_VIEW;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.BaseAdapter;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.databinding.ActivitySetupWizardBinding;
import com.vectras.vm.databinding.SetupQemuAdvancedBinding;
import com.vectras.vm.databinding.SetupQemuDoneBinding;
import com.vectras.vm.databinding.SimpleLayoutForSpinerBinding;
import com.vectras.vm.home.HomeActivity;
import com.vectras.vm.utils.ClipboardUltils;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.JSONUtils;
import com.vectras.vm.utils.ListUtils;
import com.vectras.vm.utils.UIUtils;
import com.vectras.vm.utils.PermissionUtils;
import com.vectras.vterm.TerminalBottomSheetDialog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class SetupWizardActivity extends AppCompatActivity {
    ActivitySetupWizardBinding binding;
    SetupQemuAdvancedBinding bindingAdvancedSetup;
    SetupQemuDoneBinding bindingFinalSteps;
    private final String TAG = "SetupWizardActivity";
    private boolean isFirstLaunch = false;
    private boolean libprooterror = false;
    private RequestNetwork net;
    private RequestNetwork.RequestListener _net_request_listener;
    private String contentJSON = "";
    private HashMap<String, Object> mmap = new HashMap<>();
    private String bootstrapfilelink = "";
    private final ArrayList<HashMap<String, String>> listmapForSelectMirrors = new ArrayList<>();
    private String selectedMirrorCommand = "echo ";
    private String selectedMirrorLocation = "";
    private String downloadBootstrapsCommand = "";
    private boolean aria2Error = false;
    private boolean isexecutingCommand = false;
    private boolean isEdgeServerError = false;
    private boolean isManualMode = false;
    private boolean isAllowCheckPermissions = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.edgeToEdge(this);
        binding = ActivitySetupWizardBinding.inflate(getLayoutInflater());
        bindingAdvancedSetup = binding.advancedsetup;
        bindingFinalSteps = binding.finalsteps;
        setContentView(binding.getRoot());
        UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));

        setupSpiner();

        tarPath = getExternalFilesDir("data") + "/data.tar.gz";

        binding.spinnerselectmirror.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMirrorCommand = Objects.requireNonNull(listmapForSelectMirrors.get(position).get("mirror"));
                selectedMirrorLocation = Objects.requireNonNull(listmapForSelectMirrors.get(position).get("location"));
                MainSettingsManager.setSelectedMirror(SetupWizardActivity.this, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        net = new RequestNetwork(this);
        _net_request_listener = new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
                binding.linearload.setVisibility(View.GONE);
                contentJSON = response;
                if (JSONUtils.isValidFromString(contentJSON)) {
                    mmap.clear();
                    mmap = new Gson().fromJson(contentJSON, new TypeToken<HashMap<String, Object>>() {
                    }.getType());
                    if (mmap.containsKey("arm64-v8a") && mmap.containsKey("amd64")) {
                        if (Build.SUPPORTED_ABIS[0].contains("arm64")) {
                            bootstrapfilelink = Objects.requireNonNull(mmap.get("arm64-v8a")).toString();
                        } else {
                            bootstrapfilelink = Objects.requireNonNull(mmap.get("amd64")).toString();
                        }
                        downloadBootstrapsCommand = " aria2c -x 4 --async-dns=false --disable-ipv6 --check-certificate=false -o setup.tar.gz " + bootstrapfilelink;
                        if (!bootstrapfilelink.isEmpty()) {
                            binding.linearcannotconnecttoserver.setVisibility(View.GONE);
                        }
                    }
                }
            }

            @Override
            public void onErrorResponse(String tag, String message) {
                binding.linearload.setVisibility(View.GONE);
            }
        };

        setupOnClick();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isexecutingCommand) {
                    if (binding.linearsimplesetupui.getVisibility() == View.GONE)
                        binding.linearsimplesetupui.setVisibility(View.VISIBLE);
                } else if (bindingAdvancedSetup.lnAdvancedsetup.getVisibility() == View.GONE) {
                    onBackInFinalSteps();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        startExtractSystemFiles();
    }

    private void startExtractSystemFiles() {
        String filesDir = getFilesDir().getAbsolutePath();
        File distroDir = new File(filesDir + "/distro");
        File binDir = new File(distroDir + "/bin");
        if (!binDir.exists()) {
            extractSystemFiles("bootstrap", "");
            if (Build.SUPPORTED_ABIS[0].contains("64")) {
                extractSystemFiles("alpine22", "distro");
            } else {
                extractSystemFiles("alpine21", "distro");
            }
        }
    }

    public void extractSystemFiles(String fromAsset, String extractTo) {
        String randomFileName = VMManager.startRamdomVMID();
        String filesDir = getFilesDir().getAbsolutePath();
        String abi = getDeviceAbi();
        String assetPath = fromAsset + "/" + abi + ".tar";
        String extractedFilePath = filesDir + "/" + randomFileName + ".tar";
        File destDir = new File(filesDir + "/" + extractTo);
        if (!destDir.exists()) if (!destDir.mkdir()) Log.e(TAG, "extractSystemFiles: Unable to create folder " + filesDir + "/" + extractTo);

        View progressView = LayoutInflater.from(this).inflate(R.layout.dialog_progress_style, null);
        TextView progress_text = progressView.findViewById(R.id.progress_text);
        progress_text.setText(getString(R.string.installing));
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this, R.style.CenteredDialogTheme)
                .setView(progressView)
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            boolean isCompleted;
            String errorMessage = "";

            // Step 1: Copy asset to filesDir
            if (!copyAssetToFile(assetPath, extractedFilePath)) {
                errorMessage = "Failed to copy asset file.";
                isCompleted = false;
            } else {
                isCompleted = true;
            }

            // Step 2: Run tar extraction
            if (isCompleted) {
                String[] cmdline = {"tar", "xf", extractedFilePath, "-C", filesDir + "/" + extractTo};
                Process process = null;
                try {
                    process = Runtime.getRuntime().exec(cmdline);

                    // Capture standard error output (stderr)
                    BufferedReader errorReader =
                            new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    StringBuilder errorOutput = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                    }
                    errorReader.close();

                    // Wait for the process to complete
                    int exitCode = process.waitFor();

                    // If there was any output in stderr, treat it as an error
                    if (exitCode != 0 || errorOutput.length() > 0) {
                        errorMessage =
                                errorOutput.toString().isEmpty()
                                        ? "Unknown error"
                                        : errorOutput.toString();
                        isCompleted = false;
                    }
                } catch (IOException | InterruptedException e) {
                    errorMessage = e.getMessage();
                    isCompleted = false;
                } finally {
                    if (process != null) {
                        process.destroy();
                    }
                }
            }

            boolean finalIsCompleted = isCompleted;
            String finalErrorMessage = errorMessage;
            runOnUiThread(() -> {
                progressDialog.dismiss();
                if (finalIsCompleted) {
                    Toast.makeText(
                                    getApplicationContext(),
                                    R.string.extraction_complete,
                                    Toast.LENGTH_SHORT)
                            .show();

                    if (fromAsset.contains("alpine")) {
                        File rootDir = new File(filesDir + "/distro/root");
                        if (!rootDir.exists()) if(!rootDir.mkdir()) Log.e(TAG, "extractSystemFiles: Unable to create folder " + filesDir + "/distro/root");

                        File resolv = new File(filesDir + "/distro/etc/resolv.conf");
                        if(!Objects.requireNonNull(resolv.getParentFile()).mkdirs()) Log.e(TAG, "extractSystemFiles: Unable to add DNS.");
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(resolv))) {
                            writer.write("nameserver 1.1.1.1\n");
                            writer.write("nameserver 1.0.0.1\n");
                            writer.write("nameserver 8.8.8.8\n");
                            writer.write("nameserver 8.8.4.4\n");
                        } catch (IOException e) {
                            Log.e(TAG, "extractSystemFiles: resolv: ", e);
                        }

                    }
                } else {
                    DialogUtils.twoDialog(
                            this,
                            getString(R.string.failed),
                            finalErrorMessage,
                            getString(R.string.try_again),
                            getString(R.string.close),
                            true,
                            R.drawable.error_96px,
                            false,
                            this::startExtractSystemFiles,
                            this::finish,
                            null);
                }

                File extractedTarFile = new File(extractedFilePath);
                if (extractedTarFile.exists()) if(!extractedTarFile.delete()) Log.e(TAG, "extractSystemFiles: Unable to delete " + extractedFilePath);
            });
        }).start();
    }

    /**
     * Copies the specified asset to the given file path.
     */
    private boolean copyAssetToFile(String assetPath, String outputPath) {
        try (InputStream in = getAssets().open(assetPath);
             OutputStream out = new FileOutputStream(outputPath)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "copyAssetToFile: ", e);
            return false;
        }
    }

    /**
     * Determines the ABI of the device.
     */
    private String getDeviceAbi() {
        return Build.SUPPORTED_ABIS[0];
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkpermissions();
    }

    String tarPath;

    // Function to append text and automatically scroll to bottom
    @SuppressLint("SetTextI18n")
    private void appendTextAndScroll(String textToAdd) {
        // Update the text
        bindingAdvancedSetup.tvTerminalOutput.append(textToAdd);

        if (textToAdd.contains("xssFjnj58Id")) {
            isexecutingCommand = false;
            binding.linearsimplesetupui.setVisibility(View.GONE);
            bindingAdvancedSetup.lnAdvancedsetup.setVisibility(View.GONE);
        } else if (textToAdd.contains("libproot.so --help")) {
            libprooterror = true;
        } else if (textToAdd.contains("not complete: /root/setup.tar.gz")) {
            aria2Error = true;
        } else if (textToAdd.contains("edge") && textToAdd.contains("temporary error")) {
            isEdgeServerError = true;
        }

        if (textToAdd.contains("Starting setup...")) {
            bindingAdvancedSetup.title.setText("Getting ready for you...");
            binding.textviewsettingup.setText(R.string.getting_ready_for_you_please_don_t_disconnect_the_network);
        } else if (textToAdd.contains("fetch http")) {
            bindingAdvancedSetup.title.setText(getString(R.string.connecting_to_mirror_in) + "\n" + selectedMirrorLocation + "...");
            binding.textviewsettingup.setText(getString(R.string.connecting_to_mirror_in) + "\n" + selectedMirrorLocation + "...");
        } else if (textToAdd.contains("Installing packages...")) {
            bindingAdvancedSetup.title.setText(R.string.it_won_t_take_long);
            binding.textviewsettingup.setText(R.string.completed_10_it_won_t_take_long);
        } else if (textToAdd.contains("(50/")) {
            binding.textviewsettingup.setText(R.string.completed_20_it_won_t_take_long);
        } else if (textToAdd.contains("100/")) {
            binding.textviewsettingup.setText(R.string.completed_30_it_won_t_take_long);
        } else if (textToAdd.contains("150/")) {
            binding.textviewsettingup.setText(R.string.completed_40_it_won_t_take_long);
        } else if (textToAdd.contains("200/")) {
            binding.textviewsettingup.setText(R.string.completed_50_it_won_t_take_long);
        } else if (textToAdd.contains("Downloading Qemu...")) {
            bindingAdvancedSetup.title.setText(R.string.don_t_disconnect);
            binding.textviewsettingup.setText(R.string.completed_75_don_t_disconnect);
        } else if (textToAdd.contains("Installing Qemu...")) {
            bindingAdvancedSetup.title.setText(R.string.keep_it_up);
            binding.textviewsettingup.setText(R.string.completed_80_keep_it_up);
        } else if (textToAdd.contains("qemu-system")) {
            binding.textviewsettingup.setText(R.string.completed_95_keep_it_up);
        } else if (textToAdd.contains("Just a sec...")) {
            bindingAdvancedSetup.title.setText(R.string.almost_there);
            binding.textviewsettingup.setText(getString(R.string.almost_there));
        }

        // Scroll to the bottom
        bindingAdvancedSetup.scrollView.post(() -> bindingAdvancedSetup.scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void startSetup() {
        aria2Error = false;
        isEdgeServerError = false;
        simpleSetupUIControler(1);
        uiControllerAdvancedSetup(true);
        String cmd = selectedMirrorCommand + ";" +
                " set -e;" +
                " echo \"Starting setup...\";" +
                " apk update;" +
                " echo \"Installing packages...\";" +
                " apk add " + (DeviceUtils.is64bit() ? AppConfig.neededPkgs : AppConfig.neededPkgs32bit) + ";" +
                " echo \"Downloading Qemu...\";";

        if (isManualMode) {
            cmd += " tar -xzvf " + tarPath + " -C /;" +
                    " rm " + tarPath + ";" +
                    " chmod 775 /usr/local/bin/*;";
        } else if (DeviceUtils.is64bit()) {
            cmd += downloadBootstrapsCommand + ";" +
                    " echo \"Installing Qemu...\";" +
                    " tar -xzvf setup.tar.gz -C /;" +
                    " rm setup.tar.gz;" +
                    " chmod 775 /usr/local/bin/*;";
        } else {
            cmd += " apk add qemu-system-x86_64 qemu-system-ppc qemu-system-i386 qemu-system-aarch64" +
                    " qemu-pr-helper qemu-img qemu-audio-sdl pulseaudio mesa-dri-gallium;";
        }

        cmd += " echo \"Just a sec...\";" +
                " apk add qemu-audio-sdl pulseaudio;" +
                " echo export PULSE_SERVER=127.0.0.1 >> /etc/profile;" +
                " mkdir -p ~/.vnc && echo -e \"555555\\n555555\" | vncpasswd -f > ~/.vnc/passwd && chmod 0600 ~/.vnc/passwd;" +
                " echo \"installation successful! xssFjnj58Id\"";

        bindingAdvancedSetup.tvCommandsetup.setText(cmd);
        executeShellCommand(cmd);
    }

    private void checkabi() {
        if (!DeviceUtils.is64bit())
            DialogUtils.oneDialog(this,
                    getResources().getString(R.string.warning),
                    getResources().getString(R.string.cpu_not_support_64),
                    getString(R.string.ok),
                    true,
                    R.drawable.error_96px,
                    true,
                    null,
                    null);
    }

    private void checkpermissions() {
        if (!isAllowCheckPermissions) return;

        if (PermissionUtils.storagepermission(this, true)) {
            if (!isFirstLaunch) {
                isFirstLaunch = true;
                checkabi();

                File tarGZ = new File(tarPath);
                if (tarGZ.exists()) {
                    isManualMode = true;
                    startSetup();
                } else {
                    if (binding.linearsimplesetupui.getVisibility() == View.GONE) {
                        showAdvancedSetupDialog();
                    }
                }
            }

            if (DeviceUtils.isStorageLow(this, false)) {
                DialogUtils.oneDialog(this,
                        getResources().getString(R.string.oops),
                        getResources().getString(R.string.not_enough_storage_to_set_up_content),
                        getResources().getString(R.string.ok),
                        true,
                        R.drawable.warning_48px,
                        true,
                        null,
                        () -> {
                            if (DeviceUtils.isStorageLow(this, false)) finish();
                        });
            }
        }
    }

    private void simpleSetupUIControler(int status) {
        if (status == 0) {
            //Before setup.
            binding.linearstartsetup.setVisibility(View.VISIBLE);
            binding.linearsettingup.setVisibility(View.GONE);
            binding.linearsetupfailed.setVisibility(View.GONE);

            File tarGZ = new File(tarPath);
            if (tarGZ.exists()) {
                if (!tarGZ.delete()) Log.e(TAG, "simpleSetupUIControler: Unable to delete " + tarPath);
            }
        } else if (status == 1) {
            //Setting up.
            binding.linearstartsetup.setVisibility(View.GONE);
            binding.linearsettingup.setVisibility(View.VISIBLE);
            binding.linearsetupfailed.setVisibility(View.GONE);
        } else if (status == 2) {
            //Failed.
            binding.linearstartsetup.setVisibility(View.GONE);
            binding.linearsettingup.setVisibility(View.GONE);
            binding.linearsetupfailed.setVisibility(View.VISIBLE);
        }
    }

    private final ActivityResultLauncher<String> bootstrapFilePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    File selectedFilePath = new File(getPath(uri));
                    String abi = Build.SUPPORTED_ABIS[0];
                    if (selectedFilePath.toString().endsWith(abi + ".tar.gz")) {
                        simpleSetupUIControler(1);
                        new Thread(() -> {
                            FileInputStream File = null;
                            try {
                                File = (FileInputStream) getContentResolver().openInputStream(uri);
                            } catch (FileNotFoundException e) {
                                runOnUiThread(() -> {
                                    DialogUtils.oneDialog(this,
                                            getString(R.string.oops),
                                            getString(R.string.the_file_could_not_be_processed_content),
                                            getResources().getString(R.string.ok),
                                            true,
                                            R.drawable.warning_48px,
                                            true,
                                            null,
                                            () -> {
                                                if (binding.linearsimplesetupui.getVisibility() == View.GONE) {
                                                    showAdvancedSetupDialog();
                                                }
                                            });
                                    simpleSetupUIControler(0);
                                });
                            }
                            try {
                                try {
                                    try (OutputStream out = new FileOutputStream(tarPath)) {
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
                                    runOnUiThread(() -> {
                                        isManualMode = true;
                                        startSetup();
                                        MainSettingsManager.setsetUpWithManualSetupBefore(SetupWizardActivity.this, true);
                                    });
                                    assert File != null;
                                    File.close();
                                }
                            } catch (IOException e) {
                                runOnUiThread(() -> {
                                    DialogUtils.oneDialog(this,
                                            getString(R.string.oops),
                                            getString(R.string.the_file_could_not_be_processed_content),
                                            getResources().getString(R.string.ok),
                                            true,
                                            R.drawable.warning_48px,
                                            true,
                                            null,
                                            () -> {
                                                if (binding.linearsimplesetupui.getVisibility() == View.GONE) {
                                                    showAdvancedSetupDialog();
                                                }
                                            });
                                    simpleSetupUIControler(0);
                                });
                            }
                        }).start();
                    } else {
                        DialogUtils.oneDialog(this,
                                "Invalid file",
                                "Please select vectras-vm-" + abi + ".tar.gz file.",
                                getResources().getString(R.string.ok),
                                true,
                                R.drawable.warning_48px,
                                true,
                                null,
                                () -> {
                                    if (binding.linearsimplesetupui.getVisibility() == View.GONE) {
                                        showAdvancedSetupDialog();
                                    }
                                });
                    }
                } else {
                    if (binding.linearsimplesetupui.getVisibility() == View.GONE) {
                        showAdvancedSetupDialog();
                    }
                }
            });

    private void onBackInFinalSteps() {
        if (bindingFinalSteps.lineardonate.getVisibility() == View.GONE) {
            bindingFinalSteps.lineardonate.setVisibility(View.VISIBLE);
        } else if (bindingFinalSteps.linearcommunity.getVisibility() == View.GONE) {
            bindingFinalSteps.linearcommunity.setVisibility(View.VISIBLE);
        }

        if (bindingFinalSteps.tvLater.getVisibility() == View.GONE) {
            bindingFinalSteps.tvLater.setVisibility(View.VISIBLE);
            bindingFinalSteps.btnContinue.setText(getString(R.string.join));
        }
    }

    private void setupOnClick() {
        //Simple setup
        binding.buttonletstart.setOnClickListener(v -> {
            binding.linearwelcome.setVisibility(View.GONE);
            isAllowCheckPermissions = true;
            checkpermissions();
            net.startRequestNetwork(RequestNetworkController.GET, AppConfig.bootstrapfileslink, "", _net_request_listener);
        });

        binding.buttontryconnectagain.setOnClickListener(v -> {
            binding.linearload.setVisibility(View.VISIBLE);
            net.startRequestNetwork(RequestNetworkController.GET, AppConfig.bootstrapfileslink, "", _net_request_listener);
        });

        binding.buttonautosetup.setOnClickListener(v -> {
            isManualMode = false;
            startSetup();
            simpleSetupUIControler(1);
        });

        binding.buttonmanualsetup.setOnClickListener(v -> bootstrapFilePicker.launch("*/*"));

        binding.buttonsetuptryagain.setOnClickListener(v -> simpleSetupUIControler(0));

        binding.buttonsetupshowlog.setOnClickListener(v -> binding.linearsimplesetupui.setVisibility(View.GONE));

        binding.textviewshowadvancedsetup.setOnClickListener(v -> {
            binding.linearsimplesetupui.setVisibility(View.GONE);
            if (binding.linearstartsetup.getVisibility() == View.VISIBLE) {
                showAdvancedSetupDialog();
            }
        });


        //Advanced setup
        bindingAdvancedSetup.ivClose.setOnClickListener(v -> binding.linearsimplesetupui.setVisibility(View.VISIBLE));

        bindingAdvancedSetup.ivOpenterminal.setOnClickListener(v -> {
//            if (DeviceUtils.is64bit()) {
                startActivity(new Intent(this, TermuxActivity.class));
//            } else {
//                TerminalBottomSheetDialog VTERM = new TerminalBottomSheetDialog(this);
//                VTERM.showVterm();
//            }
        });

        bindingAdvancedSetup.btnInstall.setOnClickListener(v -> {
            File tarGZ = new File(tarPath);
            if (tarGZ.exists()) {
                if (!tarGZ.delete()) Log.e(TAG, "btnInstall: Unable to delete " + tarPath);
            }
            showAdvancedSetupDialog();
        });

        bindingAdvancedSetup.ivCopycommandsetup.setOnClickListener(v -> ClipboardUltils.copyToClipboard(SetupWizardActivity.this, bindingAdvancedSetup.tvCommandsetup.getText().toString()));


        //Final steps
        bindingFinalSteps.tvLater.setOnClickListener(v -> {
            if (bindingFinalSteps.linearcommunity.getVisibility() == View.VISIBLE) {
                bindingFinalSteps.linearcommunity.setVisibility(View.GONE);
            } else if (bindingFinalSteps.lineardonate.getVisibility() == View.VISIBLE) {
                bindingFinalSteps.tvLater.setVisibility(View.GONE);
                bindingFinalSteps.lineardonate.setVisibility(View.GONE);
                bindingFinalSteps.btnContinue.setText(getString(R.string.done));
            }
        });

        bindingFinalSteps.btnContinue.setOnClickListener(v -> {
            if (bindingFinalSteps.linearcommunity.getVisibility() == View.VISIBLE) {
                bindingFinalSteps.linearcommunity.setVisibility(View.GONE);
                Intent intent = new Intent(ACTION_VIEW, Uri.parse(AppConfig.telegramLink));
                startActivity(intent);
                //Don't show join Telegram dialog again
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean("tgDialog", true);
                edit.apply();
            } else if (bindingFinalSteps.lineardonate.getVisibility() == View.VISIBLE) {
                bindingFinalSteps.tvLater.setVisibility(View.GONE);
                bindingFinalSteps.lineardonate.setVisibility(View.GONE);
                bindingFinalSteps.btnContinue.setText(getString(R.string.done));
                Intent intent = new Intent(ACTION_VIEW, Uri.parse(AppConfig.patreonLink));
                startActivity(intent);
            } else {
                startActivity(new Intent(SetupWizardActivity.this, HomeActivity.class));
                finish();
            }
        });
    }

    private void uiControllerAdvancedSetup(boolean isStartSetup) {
        bindingAdvancedSetup.lnBntinstall.setVisibility(isStartSetup ? View.GONE : View.VISIBLE);
        bindingAdvancedSetup.progressBar.setVisibility(isStartSetup ? View.VISIBLE : View.GONE);
    }

    private void showAdvancedSetupDialog() {
        DialogUtils.twoDialog(
                this,
                getResources().getString(R.string.bootstrap_required),
                getResources().getString(R.string.you_can_choose_between_auto_download_and_setup_or_manual_setup_by_choosing_bootstrap_file),
                getString(R.string.auto_setup),
                getString(R.string.manual_setup),
                true, R.drawable.system_update_24px,
                false,
                () -> {
                    //startDownload();
                    isManualMode = false;
                    startSetup();
                    simpleSetupUIControler(1);
                },
                () -> bootstrapFilePicker.launch("*/*"),
                null);
    }

    private void setupSpiner() {
        ListUtils.setupMirrorListForListmap(listmapForSelectMirrors);

        binding.spinnerselectmirror.setAdapter(new SpinnerSelectMirrorAdapter(getApplicationContext(), listmapForSelectMirrors));
        binding.spinnerselectmirror.setSelection(MainSettingsManager.getSelectedMirror(this));
    }

    public static class SpinnerSelectMirrorAdapter extends BaseAdapter {

        private final ArrayList<HashMap<String, String>> data;
        private final LayoutInflater inflater;

        public SpinnerSelectMirrorAdapter(Context context, ArrayList<HashMap<String, String>> arr) {
            this.data = arr;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public HashMap<String, String> getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                // Inflate binding only once for each new item
                SimpleLayoutForSpinerBinding binding =
                        SimpleLayoutForSpinerBinding.inflate(inflater, parent, false);

                // Create ViewHolder to hold binding
                holder = new ViewHolder(binding);
                convertView = binding.getRoot();
                convertView.setTag(holder);
            } else {
                // Get back the saved ViewHolder
                holder = (ViewHolder) convertView.getTag();
            }

            // Assign data
            HashMap<String, String> item = data.get(position);
            holder.binding.textViewLocation.setText(item.get("location"));

            return convertView;
        }

        // ViewHolder holds binding for reuse
        private static class ViewHolder {
            final SimpleLayoutForSpinerBinding binding;

            ViewHolder(SimpleLayoutForSpinerBinding binding) {
                this.binding = binding;
            }
        }
    }

    // Method to execute the shell command
    public void executeShellCommand(String userCommand) {
        isexecutingCommand = true;
        new Thread(() -> {
            try {
                // Setup the process builder to start PRoot with environmental variables and commands
                ProcessBuilder processBuilder = new ProcessBuilder();

                // Adjust these environment variables as necessary for your app
                String filesDir = getFilesDir().getAbsolutePath();

                File tmpDir = new File(getFilesDir(), "usr/tmp");

                // Setup environment for the PRoot process
                processBuilder.environment().put("PROOT_TMP_DIR", tmpDir.getAbsolutePath());

                processBuilder.environment().put("HOME", "/root");
                processBuilder.environment().put("USER", "root");
                processBuilder.environment().put("PATH", "/bin:/usr/bin:/sbin:/usr/sbin");
                processBuilder.environment().put("TERM", "xterm-256color");
                processBuilder.environment().put("TMPDIR", tmpDir.getAbsolutePath());
                processBuilder.environment().put("SHELL", "/bin/sh");

                String[] prootCommand = {
                        TermuxService.PREFIX_PATH + "/bin/proot", // PRoot binary path
                        "--kill-on-exit",
                        "--link2symlink",
                        "-0",
                        "-r", filesDir + "/distro", // Path to the rootfs
                        "-b", "/dev",
                        "-b", "/proc",
                        "-b", "/sys",
                        "-b", "/sdcard",
                        "-b", "/storage",
                        "-b", "/data",
                        "-w", "/root",
                        "/bin/sh",
                        "--login"// The shell to execute inside PRoot
                };

                processBuilder.command(prootCommand);
                Process process = processBuilder.start();
                // Get the input and output streams of the process
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                // Send user command to PRoot
                writer.write(userCommand);
                writer.newLine();
                writer.flush();
                writer.close();

                // Read the input stream for the output of the command
                String line;
                while ((line = reader.readLine()) != null) {
                    final String outputLine = line;
                    runOnUiThread(() -> appendTextAndScroll(outputLine + "\n"));
                }

                // Read any errors from the error stream
                while ((line = errorReader.readLine()) != null) {
                    final String errorLine = line;
                    runOnUiThread(() -> appendTextAndScroll(errorLine + "\n"));
                }

                // Clean up
                reader.close();
                errorReader.close();

                // Wait for the process to finish
                process.waitFor();

                // Wait for the process to finish
                int exitValue = process.waitFor();

                // Check if the exit value indicates an error
                if (exitValue != 0) {
                    isexecutingCommand = false;
                    // If exit value is not zero, display a toast message
                    if (!aria2Error && !isEdgeServerError) {
                        String toastMessage = "Command failed with exit code: " + exitValue;
                        runOnUiThread(() -> {
                            appendTextAndScroll("Error: " + toastMessage + "\n");
                            Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
                            uiControllerAdvancedSetup(false);
                            bindingAdvancedSetup.title.setText(getString(R.string.failed));
                            simpleSetupUIControler(2);
                        });
                    }
                    if (libprooterror) {
                        DialogUtils.twoDialog(
                                this,
                                getResources().getString(R.string.oops),
                                getResources().getString(R.string.a_serious_problem_has_occurred),
                                getString(R.string.join_our_community),
                                getString(R.string.close),
                                true, R.drawable.system_update_24px,
                                true,
                                () -> {
                                    Intent intent = new Intent();
                                    intent.setAction(ACTION_VIEW);
                                    intent.setData(Uri.parse(AppConfig.community));
                                    startActivity(intent);
                                },
                                null,
                                null);
                    } else if (aria2Error && downloadBootstrapsCommand.contains("aria2c")) {
                        runOnUiThread(() -> {
                            downloadBootstrapsCommand = " curl -o setup.tar.gz -L " + bootstrapfilelink;
                            startSetup();
                        });
                    } else if (isEdgeServerError) {
                        isEdgeServerError = false;
                        runOnUiThread(() -> {
                            selectedMirrorCommand += " && sed '/edge/d' /etc/apk/repositories";
                            startSetup();
                        });
                    }
                }
            } catch (IOException | InterruptedException e) {
                isexecutingCommand = false;
                // Handle exceptions by printing the stack trace in the terminal output
                final String errorMessage = e.getMessage();
                runOnUiThread(() -> {
                    appendTextAndScroll("Error: " + errorMessage + "\n");
                    Toast.makeText(this, "Error executing command: " + errorMessage, Toast.LENGTH_LONG).show();
                    uiControllerAdvancedSetup(false);
                    bindingAdvancedSetup.title.setText(getString(R.string.failed));
                    simpleSetupUIControler(2);
                });
            }
        }).start(); // Execute the command in a separate thread to prevent blocking the UI thread
    }

    public String getPath(Uri uri) {
        return FileUtils.getPath(this, uri);
    }
}
