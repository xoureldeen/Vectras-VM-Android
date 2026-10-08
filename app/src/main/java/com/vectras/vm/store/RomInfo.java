package com.vectras.vm.store;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.ImagePrvActivity;
import com.vectras.vm.R;
import com.vectras.vm.VMManager;
import com.vectras.vm.creator.VMCreatorActivity;
import com.vectras.vm.databinding.ActivityRomInfoBinding;
import com.vectras.vm.file.FilePickerDialog;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.ImageUtils;
import com.vectras.vm.utils.IntentUtils;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RomInfo extends AppCompatActivity {
    ActivityRomInfoBinding binding;
    public static boolean isFinishNow = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityRomInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        initialize();
    }

    public void onResume() {
        super.onResume();
        if (isFinishNow)
            finish();
        isFinishNow = false;
    }

    public void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }

    private final ActivityResultLauncher<String> romPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::handlePickedRom);

    public void handlePickedRom(Uri uri) {
        if (uri == null) return;

        executor.execute(() -> {
            String filePath;
            try {
                File selectedFilePath = new File(getPath(uri));
                filePath = selectedFilePath.getPath();
            } catch (Exception e) {
                filePath = "";
            }

            String selectedFileName = FileUtils.getFileNameFromUri(this, uri);

            if (isFinishing() || isDestroyed()) return;

            String finalFilePath = filePath;
            runOnUiThread(() -> {
                if (selectedFileName.equals(getIntent().getStringExtra("filename")) ||
                        (selectedFileName.endsWith(".cvbi.zip") && selectedFileName.equals(getIntent().getStringExtra("filename") + ".zip"))) {
                    Intent intent = new Intent();

                    if (getIntent().hasExtra("icon") &&
                            !Objects.requireNonNull(getIntent().getStringExtra("icon")).isEmpty() &&
                            (!selectedFileName.endsWith(".cvbi")
                                    || !selectedFileName.endsWith(".cvbi.zip"))) {

                        Glide.with(this)
                                .asBitmap()
                                .load(getIntent().getStringExtra("icon"))
                                .into(new CustomTarget<Bitmap>() {
                                    @Override
                                    public void onResourceReady(@NonNull Bitmap resource,
                                                                @Nullable Transition<? super Bitmap> transition) {
                                        File cacheDir = getExternalCacheDir();

                                        if (cacheDir != null)
                                            ImageUtils.saveBitmapToPNGFile(resource, cacheDir.getAbsolutePath(), "thumbnail.png");

                                        intent.putExtra("romicon", cacheDir != null ? cacheDir.getAbsolutePath() + "/thumbnail.png" : "");
                                    }

                                    @Override
                                    public void onLoadCleared(@Nullable Drawable placeholder) {
                                        intent.putExtra("romicon", "");
                                    }
                                });
                    } else {
                        intent.putExtra("romicon", "");
                    }

                    intent.setClass(getApplicationContext(), VMCreatorActivity.class);
                    intent.putExtra("addromnow", "");
                    intent.putExtra("romname", getIntent().getStringExtra("title"));
                    intent.putExtra("romfilename", getIntent().getStringExtra("filename"));
                    intent.putExtra("finalromfilename", getIntent().getStringExtra("finalromfilename"));
                    intent.putExtra("rompath", finalFilePath);
                    intent.putExtra("romuri", uri.toString());
                    if (Objects.requireNonNull(getIntent().getStringExtra("extra")).contains(selectedFileName)) {
                        intent.putExtra("addtodrive", "");
                        intent.putExtra("romextra", getIntent().getStringExtra("extra"));
                    } else {
                        intent.putExtra("addtodrive", "1");
                        intent.putExtra("romextra", getIntent().getStringExtra("extra"));
                    }

                    VMManager.setArch(getIntent().hasExtra("arch") && getIntent().getStringExtra("arch") != null ? Objects.requireNonNull(getIntent().getStringExtra("arch")) : "", this);

                    startActivity(intent);
                } else {
                    DialogUtils.oneDialog(RomInfo.this,
                            getString(R.string.problem_has_been_detected),
                            getString(R.string.please_select) + " " + getIntent().getStringExtra("filename"),
                            getString(R.string.ok),
                            true, R.drawable.warning_48px,
                            true,
                            null,
                            null);
                }
            });
        });
    }

    public String getPath(Uri uri) {
        return FileUtils.getPath(RomInfo.this, uri);
    }

    private void initialize() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.btnDownload.setOnClickListener(v -> download());

        if (getIntent().hasExtra("isRomInfo") && getIntent().getBooleanExtra("isRomInfo", false)) {
            binding.btnPick.setOnClickListener(v -> {
                try {
                    if (MainSettingsManager.getBuiltInFilePicker(this)) {
                        FilePickerDialog filePickerDialog = new FilePickerDialog();
                        filePickerDialog.pick(this, filePickerDialog.ROM_FILE, (path -> handlePickedRom(Uri.fromFile(new File(path)))));
                    } else {
                        romPicker.launch("*/*");
                    }
                } catch (Exception e) {
                    IntentUtils.showErrorDialog(this);
                }
            });
        } else {
            binding.btnPick.setVisibility(View.GONE);
        }

        if (getIntent().hasExtra("title")) {
            binding.textName.setText(getIntent().getStringExtra("title"));
        }

        if (getIntent().hasExtra("shortdesc")) {
            binding.textSize.setText(getIntent().getStringExtra("shortdesc"));
        }

        if (getIntent().hasExtra("desc")) {
            binding.descTxt.setText(getIntent().getStringExtra("desc"));
        }

        if (getIntent().hasExtra("icon")) {
            Glide.with(this).load(getIntent().getStringExtra("icon")).placeholder(R.drawable.ic_computer_180dp_with_padding).error(R.drawable.ic_computer_180dp_with_padding).into(binding.ivIcon);
        }

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

        binding.ivIcon.setOnClickListener(v -> {
            if (getIntent().hasExtra("icon")) {
                Intent intent = new Intent();
                intent.putExtra("uri", getIntent().getStringExtra("icon"));
                intent.setClass(getApplicationContext(), ImagePrvActivity.class);
                startActivity(intent);
            }
        });

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
                getString(getIntent().hasExtra("isRomInfo") && getIntent().getBooleanExtra("isRomInfo", false) ? R.string.who_created_this_rom : R.string.shared_by),
                getIntent().getStringExtra("creator") + ".",
                getString(R.string.ok),
                true,
                R.drawable.account_circle_24px,
                true,
                null,
                null)));

        binding.lnArch.setOnClickListener((v -> DialogUtils.oneDialog(
                RomInfo.this,
                getString(R.string.architecture),
                getArchName(Objects.requireNonNull(getIntent().getStringExtra("arch"))) + ".",
                getString(R.string.ok),
                true,
                R.drawable.devices_other_24px,
                true,
                null,
                null)));

        binding.lnSize.setOnClickListener((v -> DialogUtils.oneDialog(
                RomInfo.this,
                getString(R.string.sizetext),
                getIntent().getStringExtra("size") + ".",
                getString(R.string.ok),
                true,
                R.drawable.hard_drive_24px,
                true,
                null,
                null)));

        binding.lnFilename.setOnClickListener((v -> DialogUtils.oneDialog(
                RomInfo.this,
                getString(R.string.file_name),
                getIntent().getStringExtra("filename") + ".",
                getString(R.string.ok),
                true,
                R.drawable.file_copy_24px,
                true,
                null,
                null)));

    }

    private void download() {
        IntentUtils.openUrl(this, getIntent().getStringExtra("getrom"));
    }

    @NonNull
    private String getArchName(String arch) {
        return switch (arch) {
            case "X86_64" -> getString(R.string.x86_64);
            case "i386" -> getString(R.string.i386_qemu);
            case "ARM64" -> getString(R.string.arm64_qemu);
            case "PPC" -> getString(R.string.powerpc_qemu);
            default -> getString(R.string.unknow);
        };
    }
}
