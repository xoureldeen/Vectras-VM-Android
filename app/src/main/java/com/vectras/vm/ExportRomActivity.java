package com.vectras.vm;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vectras.vm.databinding.ActivityExportRomBinding;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.PackageUtils;
import com.vectras.vm.utils.UIUtils;
import com.vectras.vm.utils.ZipUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ExportRomActivity extends AppCompatActivity {

    ActivityExportRomBinding binding;
    public static int pendingPosition = 0;
    public static HashMap<String, Object> mapForGetData = new HashMap<>();
    public static ArrayList<HashMap<String, Object>> listmapForGetData = new ArrayList<>();
    private SharedPreferences data;
    public String getRomPath = "";
    public String iconfile = "";
    public String diskfile = "";
    public String cdromfile = "";
    private boolean isExporting = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.edgeToEdge(this);
        binding = ActivityExportRomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));

        binding.materialbutton1.setOnClickListener(v -> {
            binding.edittext1.setEnabled(false);
            binding.edittext2.setEnabled(false);
            binding.edittext1.setEnabled(true);
            binding.edittext2.setEnabled(true);
            startCreate();
        });

        binding.buttonexit.setOnClickListener(v -> finish());

        binding.buttonexit2.setOnClickListener(v -> finish());
        data= getSharedPreferences("data", Activity.MODE_PRIVATE);

        binding.edittext1.setText(data.getString("author", ""));
        binding.edittext2.setText(data.getString("desc", ""));

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
        data.edit().putString("author", Objects.requireNonNull(binding.edittext1.getText()).toString()).apply();
        data.edit().putString("desc", Objects.requireNonNull(binding.edittext2.getText()).toString()).apply();
    }

    private void onBack() {
        if (!isExporting) finish();
    }

    @SuppressLint("SetTextI18n")
    private void UIControler(int _status, String _content) {
        if (_status == 0) {
            binding.linearall.setVisibility(View.GONE);
            binding.linearload.setVisibility(View.VISIBLE);
        } else if (_status == 1) {
            binding.linearall.setVisibility(View.GONE);
            binding.linearload.setVisibility(View.GONE);
            binding.lineardone.setVisibility(View.VISIBLE);
            binding.textviewfilename.setText(getString(R.string.saved_in) + " " + _content);
        } else if (_status == 2) {
            binding.linearall.setVisibility(View.GONE);
            binding.linearload.setVisibility(View.GONE);
            binding.lineardone.setVisibility(View.GONE);
            binding.linearerror.setVisibility(View.VISIBLE);
            binding.textviewerrorcontent.setText(_content);
        }
    }

    @SuppressLint("SetTextI18n")
    private void startCreate() {
        File vDir = new File(AppConfig.cvbiFolder);
        if (!vDir.exists()) {
            if(!vDir.mkdirs()) {
                UIControler(2, getString(R.string.could_not_create_dir_to_save_cvbi_content));
            }
        }

        listmapForGetData.clear();
        mapForGetData.clear();

        listmapForGetData = new Gson().fromJson(FileUtils.readAFile(AppConfig.romsdatajson), new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());

        getRomPath = AppConfig.vmFolder + Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("vmID")) + "/";

        if (listmapForGetData.get(pendingPosition).containsKey("imgName")) {
            mapForGetData.put("title", Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("imgName")).toString());
        } else {
            mapForGetData.put("title", "");
        }
        if (listmapForGetData.get(pendingPosition).containsKey("imgIcon")) {
            iconfile = Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("imgIcon")).toString();
            try {
                mapForGetData.put("icon", Uri.parse(Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("imgIcon")).toString()).getLastPathSegment());
            } catch (Exception _e){
                mapForGetData.put("icon", Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("imgIcon")).toString());
            }
        } else {
            mapForGetData.put("icon", "");
        }
        if (listmapForGetData.get(pendingPosition).containsKey("imgPath")) {
            if (Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("imgPath")).toString().isEmpty()) {
                diskfile = VMManager.quickScanDiskFileInFolder(getRomPath);
            } else {
                diskfile = Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("imgPath")).toString();
            }
            mapForGetData.put("drive", Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("imgPath")).toString().replaceAll(getRomPath, ""));
        } else {
            diskfile = VMManager.quickScanDiskFileInFolder(getRomPath);
            mapForGetData.put("drive", "");
        }
        if (listmapForGetData.get(pendingPosition).containsKey("imgCdrom")) {
            if (Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("imgCdrom")).toString().isEmpty()) {
                cdromfile = VMManager.quickScanISOFileInFolder(getRomPath);
            } else {
                cdromfile = Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("imgCdrom")).toString();
            }
            mapForGetData.put("cdrom", Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("imgCdrom")).toString().replaceAll(getRomPath, ""));
        } else {
            cdromfile = VMManager.quickScanISOFileInFolder(getRomPath);
            mapForGetData.put("cdrom", "");
        }
        if (listmapForGetData.get(pendingPosition).containsKey("imgExtra")) {
            mapForGetData.put("qemu", Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("imgExtra")).toString().replaceAll(getRomPath, "OhnoIjustrealizeditsmidnightandIstillhavetodothis"));
        } else {
            mapForGetData.put("qemu", "");
        }
        if (listmapForGetData.get(pendingPosition).containsKey("imgArch")) {
            mapForGetData.put("arch", Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("imgArch")).toString());
        } else {
            mapForGetData.put("arch", "");
        }
        if (Objects.requireNonNull(binding.edittext1.getText()).toString().isEmpty()) {
            mapForGetData.put("author", "Unknow");
        } else {
            mapForGetData.put("author", binding.edittext1.getText().toString());
        }
        if (Objects.requireNonNull(binding.edittext2.getText()).toString().isEmpty()) {
            mapForGetData.put("desc", "Empty.");
        } else {
            mapForGetData.put("desc", binding.edittext2.getText().toString());
        }

        mapForGetData.put("versioncode", PackageUtils.getThisVersionCode(getApplicationContext()));

        FileUtils.writeToFile(new File(String.valueOf(getExternalFilesDir("data"))).getPath(), "rom-data.json", new Gson().toJson(mapForGetData));

        String[] filePaths = new String[0];

        ArrayList<String> _filelist = new ArrayList<>();
        FileUtils.getAListOfAllFilesAndFoldersInADirectory(AppConfig.vmFolder + Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("vmID")), _filelist);
        if (!_filelist.isEmpty()) {
            for (int _repeat = 0; _repeat < _filelist.size(); _repeat++) {
                if (_filelist.get(_repeat).endsWith("vmID.txt") ||
                        _filelist.get(_repeat).endsWith("vmID.old.txt")) return;

                filePaths = java.util.Arrays.copyOf(filePaths, filePaths.length + 1);
                filePaths[filePaths.length - 1] = !_filelist.get(_repeat).endsWith("rom-data.json") ? _filelist.get(_repeat) : getExternalFilesDir("data") + "/rom-data.json";
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
            if (!FileUtils.isFileExists(AppConfig.cvbiFolder + Objects.requireNonNull(mapForGetData.get("title")) + ".cvbi")) {
                outputPath = AppConfig.cvbiFolder + Objects.requireNonNull(mapForGetData.get("title")) + ".cvbi";
            } else {
                String outputFileName = Objects.requireNonNull(mapForGetData.get("title")).toString();
                int prefix = 0;
                while (true) {
                    if (!FileUtils.isFileExists(AppConfig.cvbiFolder + outputFileName + "_" + prefix + ".cvbi")) {
                        outputPath = AppConfig.cvbiFolder + outputFileName + "_" + prefix + ".cvbi";
                        break;
                    } else {
                        prefix++;
                    }
                }
            }

            boolean result = ZipUtils.compress(
                    this,
                    finalFilePaths,
                    outputPath,
                    progressText,
                    progressBar
            );

            runOnUiThread(() -> {
                isExporting = false;
                progressDialog.dismiss();

                if (result) {
                    UIControler(1, outputPath);
                } else {
                    UIControler(2, ZipUtils.lastErrorContent);
                }
            });
        }).start();
    }
}