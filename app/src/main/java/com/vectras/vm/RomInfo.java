package com.vectras.vm;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.databinding.ActivityRomInfoBinding;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.UIUtils;

import java.io.File;
import java.util.Objects;

public class RomInfo extends AppCompatActivity {

    ActivityRomInfoBinding binding;
    public static boolean isFinishNow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityRomInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));

        ImageView ivIcon;
        TextView textName;
        TextView textSize;
        Button btn_download;
        Button btn_pick;
        TextView descTxt;
        Toolbar toolbar;

        ivIcon = findViewById(R.id.ivIcon);
        textName = findViewById(R.id.textName);
        textSize = findViewById(R.id.textSize);
        btn_download = findViewById(R.id.btn_download);
        btn_pick = findViewById(R.id.btn_pick);
        descTxt = findViewById(R.id.descTxt);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            onBackPressed();
        });

        btn_download.setOnClickListener(v -> {
            Intent openurl = new Intent();
            openurl.setAction(Intent.ACTION_VIEW);
            openurl.setData(Uri.parse(getIntent().getStringExtra("getrom")));
            startActivity(openurl);
        });

        btn_pick.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                startActivityForResult(intent, 0);
            }
        });

        if (getIntent().hasExtra("title")) {
            textName.setText(getIntent().getStringExtra("title"));
        }
        if (getIntent().hasExtra("shortdesc")) {
            textSize.setText(getIntent().getStringExtra("shortdesc"));
        }
        if (getIntent().hasExtra("desc")) {
            descTxt.setText(getIntent().getStringExtra("desc"));
        }

        if (getIntent().hasExtra("icon")) {
            Glide.with(this).load(getIntent().getStringExtra("icon")).placeholder(R.drawable.ic_computer_180dp_with_padding).error(R.drawable.ic_computer_180dp_with_padding).into(ivIcon);
        }

//        btn_pick.setText(getString(R.string.select) + " " + getIntent().getStringExtra("filename"));
        initialize();
    }

    public void onResume() {
        super.onResume();
        if (isFinishNow)
            finish();
        isFinishNow = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_OK) {
            Uri content_describer = data.getData();
            File selectedFilePath = new File(getPath(content_describer));
            if (selectedFilePath.getName().equals(getIntent().getStringExtra("filename")) || (selectedFilePath.getName().endsWith(".cvbi.zip") && selectedFilePath.getName().equals(getIntent().getStringExtra("filename") + ".zip"))) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), CustomRomActivity.class);
                intent.putExtra("addromnow", "");
                intent.putExtra("romname", getIntent().getStringExtra("title"));
                intent.putExtra("romfilename", getIntent().getStringExtra("filename"));
                intent.putExtra("finalromfilename", getIntent().getStringExtra("finalromfilename"));
                intent.putExtra("rompath", selectedFilePath.getPath());
                if (Objects.requireNonNull(getIntent().getStringExtra("extra")).contains(selectedFilePath.getName())) {
                    intent.putExtra("addtodrive", "");
                    intent.putExtra("romextra", getIntent().getStringExtra("extra"));
                } else {
                    intent.putExtra("addtodrive", "1");
                    intent.putExtra("romextra", getIntent().getStringExtra("extra"));
                }
                intent.putExtra("romicon", AppConfig.maindirpath + "icons/" + getIntent().getStringExtra("filename") + ".png");
                switch (Objects.requireNonNull(getIntent().getStringExtra("arch"))) {
                    case "X86_64":
                        MainSettingsManager.setArch(this, "X86_64");
                        break;
                    case "i386":
                        MainSettingsManager.setArch(this, "I386");
                        break;
                    case "ARM64":
                        MainSettingsManager.setArch(this, "ARM64");
                        break;
                    case "PowerPC":
                        MainSettingsManager.setArch(this, "PPC");
                        break;
                }
                startActivity(intent);
            } else {
                UIUtils.oneDialog(getString(R.string.problem_has_been_detected), getString(R.string.please_select) + " " + getIntent().getStringExtra("filename"), true, false, this);
            }

        }
    }

    public String getPath(Uri uri) {
        return FileUtils.getPath(RomInfo.this, uri);
    }

    private void initialize() {
        int currentVerifyIcon = R.drawable.verified_user_24px;
        String currentVerifyText = getString(R.string.verified);
        String currentVerifyContent = getString(R.string.verified_content);

        if (getIntent().hasExtra("verified")) {
            if (!getIntent().getBooleanExtra("verified", false)) {
                binding.ivVerified.setImageResource(R.drawable.gpp_maybe_24px);
                binding.tvVerified.setText(getString(R.string.not_verified));

                currentVerifyIcon = R.drawable.gpp_maybe_24px;
                currentVerifyText = getString(R.string.not_verified);
                currentVerifyContent = getString(R.string.not_verified_content);
            }
        }

        if (getIntent().hasExtra("creator")) {
            binding.tvCreator.setText(getIntent().getStringExtra("creator"));
        }

        binding.tvArch.setText(getArchName(Objects.requireNonNull(getIntent().getStringExtra("arch"))));

        if (getIntent().hasExtra("size")) {
            binding.tvSize.setText(getIntent().getStringExtra("size"));
        }

        if (getIntent().hasExtra("filename")) {
            binding.tvFilename.setText(getIntent().getStringExtra("filename"));
        }

        String finalCurrentVerifyText = currentVerifyText;
        String finalCurrentVerifyContent = currentVerifyContent;
        int finalCurrentVerifyIcon = currentVerifyIcon;
        binding.lnVerified.setOnClickListener((v -> DialogUtils.oneDialog(
                RomInfo.this,
                finalCurrentVerifyText,
                finalCurrentVerifyContent,
                getString(R.string.ok),
                true,
                finalCurrentVerifyIcon,
                true,
                null,
                null)));

        binding.lnCreator.setOnClickListener((v -> DialogUtils.oneDialog(
                RomInfo.this,
                getString(R.string.creator),
                getIntent().getStringExtra("creator"),
                getString(R.string.ok),
                true,
                R.drawable.account_circle_24px,
                true,
                null,
                null)));

        binding.lnArch.setOnClickListener((v -> DialogUtils.oneDialog(
                RomInfo.this,
                getString(R.string.architecture),
                getArchName(Objects.requireNonNull(getIntent().getStringExtra("arch"))),
                getString(R.string.ok),
                true,
                R.drawable.devices_other_24px,
                true,
                null,
                null)));

        binding.lnSize.setOnClickListener((v -> DialogUtils.oneDialog(
                RomInfo.this,
                getString(R.string.sizetext),
                getIntent().getStringExtra("size"),
                getString(R.string.ok),
                true,
                R.drawable.hard_drive_24px,
                true,
                null,
                null)));

        binding.lnFilename.setOnClickListener((v -> DialogUtils.oneDialog(
                RomInfo.this,
                getString(R.string.file_name),
                getIntent().getStringExtra("filename"),
                getString(R.string.ok),
                true,
                R.drawable.file_copy_24px,
                true,
                null,
                null)));
    }

    private String getArchName(String arch) {
        return switch (arch) {
            case "X86_64" -> getString(R.string.x86_64);
            case "i386" -> getString(R.string.i386_qemu);
            case "ARM64" -> getString(R.string.arm64_qemu);
            case "PowerPC" -> getString(R.string.powerpc_qemu);
            default -> getString(R.string.unknow);
        };
    }
}