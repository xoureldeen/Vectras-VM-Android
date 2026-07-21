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
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.databinding.ActivityExportRomBinding;
import com.vectras.vm.file.FilePickerDialog;
import com.vectras.vm.main.vms.DataMainRoms;
import com.vectras.vm.manager.VmFileManager;
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

            if (MainSettingsManager.getBuiltInFilePicker(this)) {
                FilePickerDialog filePickerDialog = new FilePickerDialog();
                filePickerDialog.pick(this, FilePickerDialog.TYPE_FOLDER, (path -> new Thread(() -> {
                    String exportFolder = path + "/";
                    String outputPath;
                    String outputFileName = current.itemName + ".cvbi";

                    if (!FileUtils.isFileExists(exportFolder + current.itemName + ".cvbi")) {
                        outputPath = exportFolder + outputFileName;
                    } else {
                        int prefix = 0;
                        while (true) {
                            if (!FileUtils.isFileExists(exportFolder + current.itemName + " (" + prefix + ").cvbi")) {
                                outputFileName = current.itemName + " (" + prefix + ").cvbi";
                                outputPath = exportFolder + outputFileName;
                                break;
                            } else {
                                prefix++;
                            }
                        }
                    }

                    runOnUiThread(() -> startCreate(Uri.fromFile(new File(outputPath))));
                }).start()));
            } else {
                folderPicker.launch(current.itemName + ".cvbi");
            }
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
        /*String cvbiFolder = Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath() + "/cvbi/";
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
        }*/

        String getRomPath = VmFileManager.getPath(current.vmID);
        HashMap<String, Object> vmConfigMap = new HashMap<>();

        vmConfigMap.put("title", current.itemName);

        if (FileUtils.isFileExists(current.itemIcon)) {
            vmConfigMap.put("icon", new File(Objects.requireNonNull(Uri.parse(current.itemIcon).getPath())).getName());
        } else {
            vmConfigMap.put("icon", current.itemIcon);
        }

        vmConfigMap.put("machine", current.machine);

        vmConfigMap.put("cpu", current.cpu);
        vmConfigMap.put("cores", current.cores);
        vmConfigMap.put("threads", current.threads);
        vmConfigMap.put("nvirt", current.nvirt);

        vmConfigMap.put("battery", current.battery);

        boolean isUsingDiskInQemuExtraParams = VMManager.isHaveADisk(current.itemExtra);

        if (FileUtils.isFileExists(current.itemPath)) {
            vmConfigMap.put("drive", new File(Objects.requireNonNull(Uri.parse(current.itemPath).getPath())).getName());
        } else {
            vmConfigMap.put("drive", isUsingDiskInQemuExtraParams ? "" : VMManager.quickScanDiskFileInFolder(getRomPath));
        }

        if (FileUtils.isFileExists(current.hd1)) {
            vmConfigMap.put("hd1", new File(Objects.requireNonNull(Uri.parse(current.hd1).getPath())).getName());
        }

        if (FileUtils.isFileExists(current.imgCdrom)) {
            vmConfigMap.put("cdrom", new File(Objects.requireNonNull(Uri.parse(current.imgCdrom).getPath())).getName());
        } else {
            vmConfigMap.put("cdrom", isUsingDiskInQemuExtraParams || FileUtils.isFileExists(Objects.requireNonNull(vmConfigMap.get("drive")).toString()) ? "" : VMManager.quickScanISOFileInFolder(getRomPath));
        }

        if (FileUtils.isFileExists(current.cdrom1)) {
            vmConfigMap.put("cdrom1", new File(Objects.requireNonNull(Uri.parse(current.cdrom1).getPath())).getName());
        } else {
            vmConfigMap.put("cdrom1", isUsingDiskInQemuExtraParams || FileUtils.isFileExists(Objects.requireNonNull(vmConfigMap.get("drive")).toString()) ? "" : VMManager.quickScanISOFileInFolder(getRomPath));
        }

        if (FileUtils.isFileExists(current.fda)) {
            vmConfigMap.put("fda", new File(Objects.requireNonNull(Uri.parse(current.fda).getPath())).getName());
        } else {
            vmConfigMap.put("fda", "");
        }

        if (FileUtils.isFileExists(current.fdb)) {
            vmConfigMap.put("fdb", new File(Objects.requireNonNull(Uri.parse(current.fdb).getPath())).getName());
        } else {
            vmConfigMap.put("fdb", "");
        }

        vmConfigMap.put("sharedFolder", current.sharedFolder);

        vmConfigMap.put("networkCard", current.networkCard);

        vmConfigMap.put("wifi", current.wifi);

        vmConfigMap.put("bootFrom", current.bootFrom);
        vmConfigMap.put("isShowBootMenu", current.isShowBootMenu);
        vmConfigMap.put("isUseLocalTime", current.isUseLocalTime);

        vmConfigMap.put("isUseUefi", current.isUseUefi);
        vmConfigMap.put("isUseDefaultBios", current.isUseDefaultBios);

        vmConfigMap.put("qemu", VmFileManager.pathToTextMark(this, current.vmID, current.itemExtra));
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

        String tempFolder = VmFileManager.getTempPath(this, current.vmID + "/export");

        FileUtils.writeToFile(tempFolder, "rom-data.json", new Gson().toJson(vmConfigMap));

        String[] filePaths = new String[0];

        ArrayList<String> _filelist = new ArrayList<>();
        FileUtils.getAListOfAllFilesAndFoldersInADirectory(VmFileManager.getPath(current.vmID), _filelist);
        if (!_filelist.isEmpty()) {
            ArrayList<String> pathList = new ArrayList<>();

            for (int _repeat = 0; _repeat < _filelist.size(); _repeat++) {
                if (!_filelist.get(_repeat).endsWith("vmID.txt") &&
                        !_filelist.get(_repeat).endsWith("vmID.old.txt")) {

                    if (_filelist.get(_repeat).endsWith("rom-data.json")) {
                        pathList.add(tempFolder + "rom-data.json");
                    } else if (_filelist.get(_repeat).endsWith(VmFileManager.SNAPSHOT_SH_FILE_NAME)) {
                        if (VmFileManager.isSnapshotBinExists(current.vmID)) {
                            String snapshotParams = FileUtils.readAFile(_filelist.get(_repeat));
                            snapshotParams = StartVM.removeQmpParams(snapshotParams);
                            snapshotParams = StartVM.removeDisplayParams(snapshotParams);
                            FileUtils.writeToFile(tempFolder, VmFileManager.SNAPSHOT_SH_FILE_NAME, VmFileManager.pathToTextMark(this, current.vmID, snapshotParams));
                            pathList.add(tempFolder + VmFileManager.SNAPSHOT_SH_FILE_NAME);
                        }
                    } else if (_filelist.get(_repeat).endsWith(VmFileManager.CREATE_COMMAND_CONFIG_FILE_NAME)) {
                        FileUtils.writeToFile(tempFolder, VmFileManager.CREATE_COMMAND_CONFIG_FILE_NAME, FileUtils.readAFile(_filelist.get(_repeat)).replace(getRomPath, VmFileManager.TEXT_MARK_VM_PATH));
                        pathList.add(tempFolder + VmFileManager.CREATE_COMMAND_CONFIG_FILE_NAME);
                    } else if (_filelist.get(_repeat).endsWith(VmFileManager.SCREENSHOT_PNG_FILE_NAME) || _filelist.get(_repeat).endsWith(VmFileManager.AUDIO_STREAM_FILE_NAME)) {
                        //ignore
                    } else {
                        pathList.add(_filelist.get(_repeat));
                    }
                }
            }

            filePaths = pathList.toArray(new String[0]);
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

            /*String outputPath;
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
            }*/

            String outputPath = FileUtils.getPath(this, uri);

            if (outputPath != null) {
                if (outputPath.startsWith(VmFileManager.quickGetPath(current.vmID))) {
                    if (!new File(outputPath).isDirectory()) FileUtils.delete(outputPath);
                    runOnUiThread(() -> {
                        DialogUtils.oopsDialog(this, getString(R.string.cannot_save_here_please_choose_another_location));
                        DialogUtils.safeDismiss(this, progressDialog);
                    });
                    return;
                }
            } else {
                outputPath = "";
            }

            final boolean[] result = {ZipUtils.compress(
                    this,
                    finalFilePaths,
                    uri,
                    progressText,
                    progressBar
            )};
            String finalOutputPath = outputPath;
            runOnUiThread(() -> {
                isExporting = false;
                DialogUtils.safeDismiss(this, progressDialog);

                try {
                    //FileUtils.delete(new File(outputPath));
                    VmFileManager.removeTemp(this, current.vmID);
                } catch (Exception e) {
                    Log.e(TAG, "startCreate: ", e);
                }

                String title;
                String content;
                if (result[0]) {
                    title = getString(R.string.done);
                    content = finalOutputPath.isEmpty() ? getString(R.string.rom_successfully_exported) : getString(R.string.saved_in) + ": " + finalOutputPath + ".";
                } else {
                    title = getString(R.string.oops);
                    content = getString(R.string.something_went_wrong) + ":\n\n" + ZipUtils.lastErrorContent;
                }

                File file = new File(finalOutputPath);
                boolean isShowInFolder = !finalOutputPath.isEmpty() && result[0] && file.getParent() != null && FileUtils.isFileExists(file.getParent());

                DialogUtils.twoDialog(this,
                        title,
                        content,
                        getString(isShowInFolder ? R.string.show_in_folder : R.string.ok),
                        getString(isShowInFolder ? R.string.close : R.string.exit),
                        true,
                        result[0] ? R.drawable.check_24px : R.drawable.error_96px,
                        true,
                        () -> {
                            if (isShowInFolder) {
                                if (MainSettingsManager.getBuiltInFilePicker(this)) {
                                    FilePickerDialog filePickerDialog = new FilePickerDialog();
                                    filePickerDialog.setLockHome(true);
                                    filePickerDialog.browse(this, file.getParent());
                                } else {
                                    FileUtils.openFolder(this, file.getParent());
                                }
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