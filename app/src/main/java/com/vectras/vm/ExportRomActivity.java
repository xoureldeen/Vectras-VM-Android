package com.vectras.vm;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.vectras.vm.databinding.ActivityExportRomBinding;
import com.vectras.vm.main.vms.DataMainRoms;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.PackageUtils;
import com.vectras.vm.utils.UIUtils;
import com.vectras.vm.utils.ZipUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ExportRomActivity extends AppCompatActivity {

    private final String TAG = "ExportRomActivity";
    ActivityExportRomBinding binding;
    private SharedPreferences data;
    private boolean isExporting = false;
    private ActivityResultLauncher<String> folderPicker;
    private DataMainRoms current;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.edgeToEdge(this);
        binding = ActivityExportRomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));
        binding.appbar.post(() -> binding.appbar.setExpanded(false, false));
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.btnDone.setOnClickListener(v -> {
            binding.edAuthor.setEnabled(false);
            binding.edContent.setEnabled(false);
            binding.edAuthor.setEnabled(true);
            binding.edContent.setEnabled(true);
            folderPicker.launch(current.itemName + ".cvbi");
        });

        data = getSharedPreferences("data", Activity.MODE_PRIVATE);

        binding.edAuthor.setText(data.getString("author", ""));
        binding.edContent.setText(data.getString("desc", ""));

        current = VMManager.getVMConfig(getIntent().getIntExtra("POS", 0));

        folderPicker = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/octet-stream"),
                uri -> {
                    if (uri != null) {
                        startCreate(uri);
                    }
                });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBack();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        data.edit().putString("author", Objects.requireNonNull(binding.edAuthor.getText()).toString()).apply();
        data.edit().putString("desc", Objects.requireNonNull(binding.edContent.getText()).toString()).apply();
    }

    private void onBack() {
        if (!isExporting) finish();
    }

    @SuppressLint("SetTextI18n")
    private void startCreate(Uri uri) {
        String cvbiFolder = Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath() + "/cvbi/";
        File vDir = new File(cvbiFolder);
        if (!vDir.exists()) {
            if (!vDir.mkdirs()) {
                DialogUtils.oneDialog(this,
                        getString(R.string.oops),
                        getString(R.string.could_not_create_dir_to_save_cvbi_content),
                        getString(R.string.ok),
                        true,
                        R.drawable.error_96px,
                        true,
                        null,
                        this::finish
                );
            }
        }

        String getRomPath = AppConfig.vmFolder + current.vmID + "/";
        HashMap<String, Object> vmConfigMap = new HashMap<>();

        vmConfigMap.put("title", current.itemName);

        if (FileUtils.isFileExists(current.itemIcon)) {
            vmConfigMap.put("icon", new File(Objects.requireNonNull(Uri.parse(current.itemIcon).getPath())).getName());
        } else {
            vmConfigMap.put("icon", current.itemIcon);
        }

        if (FileUtils.isFileExists(current.itemPath)) {
            vmConfigMap.put("drive", new File(Objects.requireNonNull(Uri.parse(current.itemPath).getPath())).getName());
        } else {
            vmConfigMap.put("drive", VMManager.quickScanDiskFileInFolder(getRomPath));
        }

        if (FileUtils.isFileExists(current.imgCdrom)) {
            vmConfigMap.put("cdrom", new File(Objects.requireNonNull(Uri.parse(current.imgCdrom).getPath())).getName());
        } else {
            vmConfigMap.put("cdrom", VMManager.quickScanISOFileInFolder(getRomPath));
        }

        vmConfigMap.put("bootFrom", current.bootFrom);
        vmConfigMap.put("isShowBootMenu", current.isShowBootMenu);
        vmConfigMap.put("qemu", current.itemExtra.replace(AppConfig.vmFolder + current.vmID + "/", "OhnoIjustrealizeditsmidnightandIstillhavetodothis"));
        vmConfigMap.put("arch", current.itemArch);

        if (Objects.requireNonNull(binding.edAuthor.getText()).toString().isEmpty()) {
            vmConfigMap.put("author", "Unknow");
        } else {
            vmConfigMap.put("author", binding.edAuthor.getText().toString());
        }
        if (Objects.requireNonNull(binding.edContent.getText()).toString().isEmpty()) {
            vmConfigMap.put("desc", "Empty.");
        } else {
            vmConfigMap.put("desc", binding.edContent.getText().toString());
        }

        vmConfigMap.put("versioncode", PackageUtils.getThisVersionCode(getApplicationContext()));

        String tempFolder = Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath() + "/temp/";

        FileUtils.writeToFile(tempFolder, "rom-data.json", new Gson().toJson(vmConfigMap));

        String[] filePaths = new String[0];

        ArrayList<String> _filelist = new ArrayList<>();
        FileUtils.getAListOfAllFilesAndFoldersInADirectory(AppConfig.vmFolder + current.vmID, _filelist);
        if (!_filelist.isEmpty()) {
            for (int _repeat = 0; _repeat < _filelist.size(); _repeat++) {
                if (!_filelist.get(_repeat).endsWith("vmID.txt") &&
                        !_filelist.get(_repeat).endsWith("vmID.old.txt")) {
                    filePaths = java.util.Arrays.copyOf(filePaths, filePaths.length + 1);

                    if (_filelist.get(_repeat).endsWith("rom-data.json")) {
                        filePaths[filePaths.length - 1] = tempFolder + "rom-data.json";
                    } else if (_filelist.get(_repeat).endsWith("cqcm.json")) {
                        FileUtils.writeToFile(tempFolder, "cqcm.json", FileUtils.readAFile(_filelist.get(_repeat)).replace(AppConfig.vmFolder + current.vmID + "/", "OhnoIjustrealizeditsmidnightandIstillhavetodothis"));
                        filePaths[filePaths.length - 1] = tempFolder + "cqcm.json";
                    } else {
                        filePaths[filePaths.length - 1] = _filelist.get(_repeat);
                    }
                }
            }
        }

        View progressView = LayoutInflater.from(this).inflate(R.layout.dialog_progress_style, null);
        TextView progressText = progressView.findViewById(R.id.progress_text);
        progressText.setText(getString(R.string.exporting) + "\n" + getString(R.string.please_stay_here));
        ProgressBar progressBar = progressView.findViewById(R.id.progress_bar);
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this, R.style.CenteredDialogTheme)
                .setView(progressView)
                .setCancelable(false)
                .create();

        progressDialog.show();

        String[] finalFilePaths = filePaths;
        new Thread(() -> {
            isExporting = true;

            String outputPath;
            String outputFileName = current.itemName + ".cvbi";
            if (!FileUtils.isFileExists(cvbiFolder + current.itemName + ".cvbi")) {
                outputPath = cvbiFolder + outputFileName;
            } else {
                int prefix = 0;
                while (true) {
                    if (!FileUtils.isFileExists(cvbiFolder + current.itemName + "_" + prefix + ".cvbi")) {
                        outputFileName = current.itemName + "_" + prefix + ".cvbi";
                        outputPath = cvbiFolder + outputFileName;
                        break;
                    } else {
                        prefix++;
                    }
                }
            }

            final boolean[] result = {ZipUtils.compress(
                    this,
                    finalFilePaths,
                    uri,
                    progressText,
                    progressBar
            )};
            runOnUiThread(() -> {
                isExporting = false;
                DialogUtils.safeDismiss(this, progressDialog);

                String finalOutputPath = "";
                try {
                    FileUtils.delete(new File(outputPath));
                    FileUtils.delete(new File(tempFolder));
                    finalOutputPath = FileUtils.getPath(this, uri);
                } catch (Exception e) {
                    Log.e(TAG, "startCreate: ", e);
                }

                String finalOutputPath1 = finalOutputPath;
                String title;
                String content;
                if (result[0]) {
                    title = getString(R.string.done);
                    content = finalOutputPath1 == null || finalOutputPath1.isEmpty() ? getString(R.string.rom_successfully_exported) : getString(R.string.saved_in) + ": " + finalOutputPath1 + ".";
                } else {
                    title = getString(R.string.oops);
                    content = getString(R.string.something_went_wrong) + ":\n\n" + ZipUtils.lastErrorContent;
                }

                DialogUtils.twoDialog(this,
                        title,
                        content,
                        getString(result[0] ? R.string.show_in_folder : R.string.ok),
                        getString(result[0] ? R.string.close : R.string.exit),
                        true,
                        result[0] ? R.drawable.check_24px : R.drawable.error_96px,
                        true,
                        () -> {
                            if (result[0]) {
                                assert finalOutputPath1 != null;
                                File file = new File(finalOutputPath1.isEmpty() ? outputPath : finalOutputPath1);
                                FileUtils.openFolder(this, file.getParent());
                            }
                        },
                        () -> {
                            if (!result[0]) {
                                finish();
                            }
                        },
                        null);
            });
        }).start();
    }
}