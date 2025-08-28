package com.vectras.vm;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputLayout;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.Fragment.CreateImageDialogFragment;
import com.vectras.vm.MainRoms.DataMainRoms;
import com.vectras.vm.databinding.ActivityCustomRomBinding;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.ImageUtils;
import com.vectras.vm.utils.UIUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.vectras.vm.utils.ZipUtils;
import com.vectras.vterm.Terminal;
import com.vectras.vm.utils.PermissionUtils;

public class CustomRomActivity extends AppCompatActivity {

    private final String TAG = "CustomRomActivity";
    private ActivityCustomRomBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    boolean iseditparams = false;
    public String previousName = "";
    public boolean addromnowdone = false;
    public static String vmID = VMManager.idGenerator();
    public int port = VMManager.startRandomPort();
    private boolean created = false;
    private String thumbnailPath = "";
    private final Timer _timer = new Timer();
    private TimerTask timerTask;
    double zipFileSize = 0;
    double folderSize = 0;
    int decompressionProgress = 0;
    boolean modify;
    public static DataMainRoms current;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(1, 1, 1, getString(R.string.create)).setShortcut('3', 'c').setIcon(R.drawable.check_24px).setShowAsAction(1);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return switch (item.getItemId()) {
            case 1 -> {
                startCreateVM();
                yield true;
            }
            case android.R.id.home -> {
                finish();
                yield true;
            }
            default -> super.onOptionsItemSelected(item);
        };
    }


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.edgeToEdge(this);
        binding = ActivityCustomRomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        binding.collapsingToolbarLayout.setSubtitle(MainSettingsManager.getArch(this));

        binding.drive.setOnClickListener(v -> diskPicker.launch("*/*"));
        binding.driveField.setOnClickListener(v -> diskPicker.launch("*/*"));

        binding.driveField.setEndIconOnClickListener(v -> {
            if (Objects.requireNonNull(binding.drive.getText()).toString().isEmpty()) {
                CreateImageDialogFragment dialogFragment = new CreateImageDialogFragment();
                dialogFragment.customRom = true;
                dialogFragment.filename = Objects.requireNonNull(binding.title.getText()).toString();
                dialogFragment.drive = binding.drive;
                dialogFragment.driveLayout = binding.driveField;
                dialogFragment.show(getSupportFragmentManager(), "CreateImageDialogFragment");
            } else {
                DialogUtils.threeDialog(this,
                        getString(R.string.change_hard_drive),
                        getString(R.string.do_you_want_to_change_create_or_remove),
                        getString(R.string.change), getString(R.string.remove),
                        getString(R.string.create),
                        true,
                        R.drawable.hard_drive_24px,
                        true,
                        () -> diskPicker.launch("*/*"),
                        () -> {
                            if (binding.drive.getText().toString().contains(AppConfig.vmFolder + vmID)) {
                                FileUtils.deleteDirectory(Objects.requireNonNull(binding.drive.getText()).toString());
                            }
                            binding.drive.setText("");
                            binding.driveField.setEndIconDrawable(R.drawable.round_add_24);
                        },
                        () -> {
                            if (!createVMFolder()) {
                                return;
                            }
                            CreateImageDialogFragment dialogFragment = new CreateImageDialogFragment();
                            dialogFragment.customRom = true;
                            dialogFragment.filename = Objects.requireNonNull(binding.title.getText()).toString();
                            dialogFragment.drive = binding.drive;
                            dialogFragment.driveLayout = binding.driveField;
                            dialogFragment.show(getSupportFragmentManager(), "CreateImageDialogFragment");
                        }, null);
            }
        });

        View.OnClickListener cdromClickListener = v -> isoPicker.launch("*/*");

        binding.cdrom.setOnClickListener(cdromClickListener);
        binding.cdromField.setOnClickListener(cdromClickListener);

        binding.cdromField.setEndIconOnClickListener(v -> {
            if (!Objects.requireNonNull(binding.cdrom.getText()).toString().isEmpty()) {
                binding.cdrom.setText("");
                binding.cdromField.setEndIconMode(TextInputLayout.END_ICON_NONE);
            }
        });

        binding.addRomBtn.setOnClickListener(v -> startCreateVM());

        binding.qemu.setOnClickListener(v -> {
            iseditparams = true;
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), EditActivity.class);
            intent.putExtra("content", Objects.requireNonNull(binding.qemu.getText()).toString());
            startActivity(intent);
        });

        binding.qemuField.setOnClickListener(v -> {
            iseditparams = true;
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), EditActivity.class);
            intent.putExtra("content", Objects.requireNonNull(binding.qemu.getText()).toString());
            startActivity(intent);
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (!thumbnailPath.isEmpty())
                    return;

                VMManager.setIconWithName(binding.ivIcon, Objects.requireNonNull(binding.title.getText()).toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
        binding.title.addTextChangedListener(afterTextChangedListener);
        binding.drive.addTextChangedListener(afterTextChangedListener);
        binding.qemu.addTextChangedListener(afterTextChangedListener);


        binding.qemuField.setEndIconOnClickListener(v -> {
            String qcc = "android-app://com.anbui.cqcm.app";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(qcc));
            startActivity(i);
        });

        binding.ivIcon.setOnClickListener(v -> {
            if (thumbnailPath.isEmpty()) {
                thumbnailPicker.launch("image/*");
            } else {
                DialogUtils.twoDialog(this,
                        getString(R.string.change_thumbnail),
                        getString(R.string.do_you_want_to_change_or_remove),
                        getString(R.string.change), getString(R.string.remove),
                        true,
                        R.drawable.photo_24px,
                        true,
                        () -> thumbnailPicker.launch("image/*"),
                        () -> {
                            thumbnailPath = "";
                            binding.ivAddThubnail.setImageResource(R.drawable.round_add_24);
                            VMManager.setIconWithName(binding.ivIcon, Objects.requireNonNull(binding.title.getText()).toString());
                        }, null);
            }
        });

        binding.lineardisclaimer.setOnClickListener(v -> DialogUtils.oneDialog(this, getResources().getString(R.string.dont_miss_out), getResources().getString(R.string.disclaimer_when_using_rom), getResources().getString(R.string.i_agree), true, R.drawable.verified_user_24px, true, null, null));

        modify = getIntent().getBooleanExtra("MODIFY", false);
        if (modify) {
            created = true;
            binding.addRomBtn.setText(R.string.save_changes);
            binding.title.setText(current.itemName);
            binding.drive.setText(current.itemPath);
            binding.cdrom.setText(current.imgCdrom);
            thumbnailPath = current.itemIcon;
            vmID = getIntent().getStringExtra("VMID");

            if (vmID != null && vmID.isEmpty()) {
                vmID = VMManager.idGenerator();
            }

            binding.qemu.setText(current.itemExtra);

            thumbnailProcessing();

            if (Objects.requireNonNull(binding.drive.getText()).toString().isEmpty()) {
                binding.driveField.setEndIconDrawable(R.drawable.round_add_24);
            }

            if (!Objects.requireNonNull(binding.cdrom.getText()).toString().isEmpty()) {
                binding.cdromField.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
                binding.cdromField.setEndIconDrawable(R.drawable.close_24px);
                changeOnClickCdrom();
            }

            previousName = current.itemName;
        } else {
            checkVMID();
            if (getIntent().hasExtra("addromnow")) {
                if (Objects.requireNonNull(getIntent().getStringExtra("romextra")).isEmpty()) {
                    setDefault();
                } else {
                    binding.qemu.setText(Objects.requireNonNull(getIntent().getStringExtra("romextra")).replaceAll("OhnoIjustrealizeditsmidnightandIstillhavetodothis", AppConfig.vmFolder + vmID + "/"));
                }

                binding.title.setText(getIntent().getStringExtra("romname"));

                if (getIntent().hasExtra("romicon")) {
                    startProcessingThumbnail(Uri.parse(getIntent().getStringExtra("romicon")));
                }

                if (Objects.requireNonNull(getIntent().getStringExtra("romfilename")).endsWith(".cvbi")) {
                    importCVBI(Objects.requireNonNull(getIntent().getStringExtra("rompath")), getIntent().getStringExtra("romfilename"));
                } else {
                    addromnowdone = true;
                    if (!Objects.requireNonNull(getIntent().getStringExtra("rompath")).isEmpty()) {
                        selectedDiskFile(Uri.fromFile(new File((Objects.requireNonNull(getIntent().getStringExtra("rompath"))))), false);
                    }
                    if (!Objects.requireNonNull(getIntent().getStringExtra("addtodrive")).isEmpty()) {
                        binding.drive.setText(AppConfig.vmFolder + vmID + "/" + getIntent().getStringExtra("romfilename"));
                        if (Objects.requireNonNull(binding.drive.getText()).toString().isEmpty()) {
                            binding.driveField.setEndIconDrawable(R.drawable.round_add_24);
                        } else {
                            binding.driveField.setEndIconDrawable(R.drawable.more_vert_24px);
                        }
                    } else {
                        binding.driveField.setEndIconDrawable(R.drawable.round_add_24);
                    }
                }

            } else if (getIntent().hasExtra("importcvbinow")) {
                setDefault();
                cvbiPicker.launch("*/*");
            } else {
                setDefault();
                if (MainSettingsManager.autoCreateDisk(this)) {
                    if (!createVMFolder()) {
                        return;
                    }
                    Terminal vterm = new Terminal(this);
                    vterm.executeShellCommand2("qemu-img create -f qcow2 " + AppConfig.vmFolder + vmID + "/disk.qcow2 128G", false, this);
                    binding.drive.setText(AppConfig.vmFolder + vmID + "/disk.qcow2");
                } else {
                    binding.driveField.setEndIconDrawable(R.drawable.round_add_24);
                }

            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        PermissionUtils.storagepermission(this, true);
        if (iseditparams) {
            iseditparams = false;
            binding.qemu.setText(EditActivity.result);
        }
    }

    @Override
    public void onBackPressed() {
        if (!created && !modify) {
            FileUtils.deleteDirectory(AppConfig.vmFolder + vmID);
        }
        modify = false;
        super.onBackPressed();
    }

    public void onDestroy() {
        if (!created && !modify) {
            FileUtils.deleteDirectory(AppConfig.vmFolder + vmID);
        }
        modify = false;
        super.onDestroy();
    }

    private final ActivityResultLauncher<String> thumbnailPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                startProcessingThumbnail(uri);
            });

    private final ActivityResultLauncher<String> diskPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                selectedDiskFile(uri, true);
            });

    @SuppressLint("SetTextI18n")
    private final ActivityResultLauncher<String> isoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;

                if (MainSettingsManager.copyFile(this)) {
                    whenProcessing(true);
                    binding.custom.setVisibility(View.GONE);
                    executor.execute(() -> {
                        try {
                            try {
                                String _filename = FileUtils.getFileNameFromUri(this, uri);
                                if (_filename == null || _filename.isEmpty()) {
                                    _filename = String.valueOf(System.currentTimeMillis());
                                }

                                FileUtils.copyFileFromUri(this, uri, AppConfig.vmFolder + vmID + "/" + _filename);

                                String final_filename = _filename;
                                runOnUiThread(() -> {
                                    binding.cdrom.setText(AppConfig.vmFolder + vmID + "/" + final_filename);
                                    binding.cdromField.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
                                    binding.cdromField.setEndIconDrawable(R.drawable.close_24px);
                                    changeOnClickCdrom();
                                });

                            } finally {
                                runOnUiThread(() -> {
                                    whenProcessing(false);
                                    binding.custom.setVisibility(View.VISIBLE);
                                });
                            }
                        } catch (IOException e) {
                            runOnUiThread(() -> {
                                whenProcessing(false);
                                binding.custom.setVisibility(View.VISIBLE);
                                DialogUtils.oneDialog(this,
                                        getString(R.string.oops),
                                        getString(R.string.unable_to_copy_iso_file_content),
                                        getString(R.string.ok),
                                        true,
                                        R.drawable.warning_48px,
                                        true,
                                        null,
                                        null);
                            });
                            Log.e(TAG, "isoPicker: " + e.getMessage());
                        }
                    });
                } else {
                    whenProcessing(false);
                    if (!FileUtils.isValidFilePath(this, FileUtils.getPath(this, uri), false)) {
                        DialogUtils.oneDialog(this,
                                getString(R.string.problem_has_been_detected),
                                getString(R.string.invalid_file_path_content),
                                getString(R.string.ok),
                                true,
                                R.drawable.warning_48px,
                                true,
                                null,
                                null);
                        return;
                    }
                    File selectedFilePath = new File(getPath(uri));
                    if (selectedFilePath.getName().endsWith(".iso")) {
                        binding.cdrom.setText(selectedFilePath.getPath());
                    } else {
                        DialogUtils.oneDialog(this,
                                getString(R.string.problem_has_been_detected),
                                getString(R.string.invalid_iso_file_path_content),
                                getString(R.string.ok),
                                true,
                                R.drawable.warning_48px,
                                true,
                                null,
                                null);
                    }
                }
            });

    private final ActivityResultLauncher<String> cvbiPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null || !FileUtils.isValidFilePath(this, FileUtils.getPath(this, uri), true))
                    return;
                File selectedFilePath = new File(getPath(uri));
                importCVBI(selectedFilePath.getPath(), selectedFilePath.getName());
            });

    private void setDefault() {
        String defQemuParams;
        if (AppConfig.getSetupFiles().contains("arm64-v8a") || AppConfig.getSetupFiles().contains("x86_64")) {
            defQemuParams = switch (MainSettingsManager.getArch(this)) {
                case "ARM64" ->
                        "-M virt,virtualization=true -cpu cortex-a76 -accel tcg,thread=multi -net nic,model=e1000 -net user -device nec-usb-xhci -device usb-kbd -device usb-mouse -device VGA";
                case "PPC" -> "-M mac99 -cpu g4 -accel tcg,thread=multi -smp 1";
                case "I386" ->
                        "-M pc -cpu qemu32,+avx -accel tcg,thread=multi -smp 4 -vga std -netdev user,id=usernet -device e1000,netdev=usernet";
                default ->
                        "-M pc -cpu qemu64,+avx -accel tcg,thread=multi -smp 4 -vga std -netdev user,id=usernet -device e1000,netdev=usernet";
            };
        } else {
            defQemuParams = switch (MainSettingsManager.getArch(this)) {
                case "ARM64" ->
                        "-M virt -cpu cortex-a76 -net nic,model=e1000 -net user -device nec-usb-xhci -device usb-kbd -device usb-mouse -device VGA";
                case "PPC" -> "-M mac99 -cpu g4 -smp 1";
                case "I386" ->
                        "-M pc -cpu qemu32,+avx -smp 4 -vga std -netdev user,id=usernet -device e1000,netdev=usernet";
                default ->
                        "-M pc -cpu qemu64,+avx -smp 4 -vga std -netdev user,id=usernet -device e1000,netdev=usernet";
            };
        }
        binding.title.setText(getString(R.string.new_vm));
        binding.qemu.setText(defQemuParams);
    }

    private void whenProcessing(boolean _isProcessing) {
        if (_isProcessing) {
            binding.mainlayout.setVisibility(View.GONE);
            binding.appbar.setVisibility(View.GONE);
            binding.linearprogress.setIndeterminate(true);
            binding.textviewprogress.setText(getResources().getString(R.string.processing_this_may_take_a_few_minutes));
        } else {
            binding.mainlayout.setVisibility(View.VISIBLE);
            binding.appbar.setVisibility(View.VISIBLE);
        }
    }

    private void checkVMID() {
        if (FileUtils.isFileExists(AppConfig.maindirpath + "/roms/" + vmID) || vmID.isEmpty()) {
            vmID = VMManager.idGenerator();
            port = VMManager.startRandomPort();
        }
    }

    private boolean createVMFolder() {
        File romDir = new File(AppConfig.vmFolder + vmID);
        if (!romDir.exists()) {
            if (!romDir.mkdirs()) {
                if (getIntent().hasExtra("addromnow")) {
                    DialogUtils.oneDialog(this, getResources().getString(R.string.oops), getResources().getString(R.string.unable_to_create_the_directory_to_create_the_vm), getResources().getString(R.string.ok), true, R.drawable.error_96px, true,
                            this::finish, this::finish);
                } else {
                    DialogUtils.oneDialog(this, getResources().getString(R.string.oops), getResources().getString(R.string.unable_to_create_the_directory_to_create_the_vm), getResources().getString(R.string.ok), true, R.drawable.error_96px, true, null, null);
                }
                return false;
            }
        }
        return true;
    }

    private void startCreateVM() {
        if (Objects.requireNonNull(binding.title.getText()).toString().isEmpty()) {
            DialogUtils.oneDialog(this, getString(R.string.oops), getString(R.string.need_set_name), getString(R.string.ok), true, R.drawable.error_96px, true, null, null);
        } else {
            String _contentDialog = "";
            if (Objects.requireNonNull(binding.qemu.getText()).toString().isEmpty()) {
                _contentDialog = getResources().getString(R.string.qemu_params_is_empty);
            }

            if ((Objects.requireNonNull(binding.drive.getText()).toString().isEmpty()) && (Objects.requireNonNull(binding.cdrom.getText()).toString().isEmpty())) {
                if (!VMManager.isHaveADisk(Objects.requireNonNull(binding.qemu.getText()).toString())) {
                    if (!_contentDialog.isEmpty()) {
                        _contentDialog += "\n\n";
                    }
                    _contentDialog += getResources().getString(R.string.you_have_not_added_any_storage_devices);
                }

            }

            if (_contentDialog.isEmpty()) {
                createNewVM();
            } else {
                DialogUtils.twoDialog(this, getString(R.string.problem_has_been_detected), _contentDialog, getString(R.string.continuetext), getString(R.string.cancel), true, R.drawable.warning_48px, true,
                        () -> {
                            createNewVM();
                            finish();
                        }, null, null);
            }
        }
    }

    private void createNewVM() {
        if (FileUtils.isFileExists(AppConfig.romsdatajson)) {
            if (!VMManager.isRomsDataJsonNormal(true, this)) {
                return;
            }
        } else {
            FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", "[]");
        }

        if (modify) {
            VMManager.editVM(Objects.requireNonNull(binding.title.getText()).toString(),
                    thumbnailPath,
                    Objects.requireNonNull(binding.drive.getText()).toString(),
                    MainSettingsManager.getArch(this),
                    Objects.requireNonNull(binding.cdrom.getText()).toString(),
                    Objects.requireNonNull(binding.qemu.getText()).toString(),
                    getIntent().getIntExtra("POS", 0));
        } else {
            VMManager.createNewVM(Objects.requireNonNull(binding.title.getText()).toString(),
                    thumbnailPath,
                    Objects.requireNonNull(binding.drive.getText()).toString(),
                    MainSettingsManager.getArch(this),
                    Objects.requireNonNull(binding.cdrom.getText()).toString(),
                    Objects.requireNonNull(binding.qemu.getText()).toString(), vmID, port);
        }

        created = true;

        if (getIntent().hasExtra("addromnow")) {
            RomsManagerActivity.isFinishNow = true;
            RomInfo.isFinishNow = true;
        }

        modify = false;
        if (!MainActivity.isActivate) {
            startActivity(new Intent(this, SplashActivity.class));
        } else {
            Intent openURL = new Intent();
            openURL.setAction(Intent.ACTION_VIEW);
            openURL.setData(Uri.parse("android-app://com.vectras.vm"));
            startActivity(openURL);
        }
        finish();
    }

    private void startProcessingThumbnail(Uri uri) {
        whenProcessing(true);
        executor.execute(() -> {
            try {
                ImageUtils.convertToPng(this, uri, AppConfig.vmFolder + vmID + "/thumbnail.png");

                thumbnailPath = AppConfig.vmFolder + vmID + "/thumbnail.png";
                runOnUiThread(this::thumbnailProcessing);
            } catch (Exception e) {
                runOnUiThread(() -> DialogUtils.oneDialog(this,
                        getString(R.string.oops),
                        getString(R.string.unable_to_process_thumbnail_content),
                        getString(R.string.ok),
                        true,
                        R.drawable.warning_48px,
                        true,
                        null,
                        null));
            } finally {
                runOnUiThread(() -> {
                    whenProcessing(false);
                });
            }
        });
    }

    private void thumbnailProcessing() {
        if (!thumbnailPath.isEmpty()) {
            binding.ivAddThubnail.setImageResource(R.drawable.round_edit_24);
            File imgFile = new File(thumbnailPath);

            if (imgFile.exists()) {
                Glide.with(this)
                        .load(new File(AppConfig.vmFolder + vmID + "/thumbnail.png"))
                        .placeholder(R.drawable.ic_computer_180dp_with_padding)
                        .error(R.drawable.ic_computer_180dp_with_padding)
                        .into(binding.ivIcon);
            } else {
                binding.ivAddThubnail.setImageResource(R.drawable.round_add_24);
                VMManager.setIconWithName(binding.ivIcon, current.itemName);
            }
        } else {
            binding.ivAddThubnail.setImageResource(R.drawable.round_add_24);
        }
    }

    private void selectedDiskFile(Uri _content_describer, boolean _addtodrive) {
        if (FileUtils.isValidFilePath(this, FileUtils.getPath(this, _content_describer), false)) {
            File selectedFilePath = new File(getPath(_content_describer));
            if (VMManager.isADiskFile(selectedFilePath.getPath())) {
                startProcessingHardDriveFile(_content_describer, _addtodrive);
            } else {
                DialogUtils.twoDialog(this, getString(R.string.problem_has_been_detected), getString(R.string.file_format_is_not_supported), getResources().getString(R.string.continuetext), getResources().getString(R.string.cancel), true, R.drawable.hard_drive_24px, true,
                        () -> startProcessingHardDriveFile(_content_describer, _addtodrive), null, null);
            }
        } else {
            startProcessingHardDriveFile(_content_describer, _addtodrive);
        }
    }

    @SuppressLint("SetTextI18n")
    private void startProcessingHardDriveFile(Uri _content_describer, boolean _addtodrive) {
        if (MainSettingsManager.copyFile(this)) {
            whenProcessing(true);
            binding.custom.setVisibility(View.GONE);
            if (!createVMFolder()) {
                return;
            }
            executor.execute(() -> {
                try {
                    try {
                        String _filename = FileUtils.getFileNameFromUri(this, _content_describer);
                        if (_filename == null || _filename.isEmpty()) {
                            _filename = String.valueOf(System.currentTimeMillis());
                        }

                        FileUtils.copyFileFromUri(this, _content_describer, AppConfig.vmFolder + vmID + "/" + _filename);

                        String final_filename = _filename;
                        runOnUiThread(() -> {
                            if (_addtodrive) {
                                binding.drive.setText(AppConfig.vmFolder + vmID + "/" + final_filename);
                                binding.driveField.setEndIconDrawable(R.drawable.more_vert_24px);
                            }
                        });
                    } finally {
                        runOnUiThread(() -> {
                            whenProcessing(false);
                            binding.custom.setVisibility(View.VISIBLE);
                        });
                    }
                } catch (IOException e) {
                    runOnUiThread(() -> {
                        whenProcessing(false);
                        binding.custom.setVisibility(View.VISIBLE);
                        DialogUtils.oneDialog(this,
                                getString(R.string.oops),
                                getString(R.string.unable_to_copy_hard_drive_file_content),
                                getString(R.string.ok),
                                true,
                                R.drawable.warning_48px,
                                true,
                                null,
                                null);
                    });
                }
            });
        } else {
            whenProcessing(false);
            if (!FileUtils.isValidFilePath(this, FileUtils.getPath(this, _content_describer), false)) {
                DialogUtils.oneDialog(this,
                        getString(R.string.problem_has_been_detected),
                        getString(R.string.invalid_file_path_content),
                        getString(R.string.ok),
                        true,
                        R.drawable.warning_48px,
                        true,
                        null,
                        null);
                return;
            }
            File selectedFilePath = new File(getPath(_content_describer));
            binding.drive.setText(selectedFilePath.getPath());
            binding.driveField.setEndIconDrawable(R.drawable.more_vert_24px);
        }
    }

    private void changeOnClickCdrom() {
        binding.cdromField.setEndIconOnClickListener(v -> {
            if (!Objects.requireNonNull(binding.cdrom.getText()).toString().isEmpty()) {
                binding.cdrom.setText("");
                binding.cdromField.setEndIconMode(TextInputLayout.END_ICON_NONE);
            }
        });
    }

    private void importCVBI(String _filepath, String _filename) {
        if (_filepath.endsWith(".cvbi") || _filepath.endsWith(".cvbi.zip")) {
            //Error code: CR_CVBI1
            if (!FileUtils.isFileExists(_filepath)) {
                if (getIntent().hasExtra("addromnow")) {
                    DialogUtils.oneDialog(this, getResources().getString(R.string.oops), getResources().getString(R.string.error_CR_CVBI1), getResources().getString(R.string.ok), true, R.drawable.error_96px, true,
                            this::finish, this::finish);
                } else {
                    DialogUtils.oneDialog(this, getResources().getString(R.string.oops), getResources().getString(R.string.error_CR_CVBI1), getResources().getString(R.string.ok), true, R.drawable.error_96px, true, null, null);
                }
                return;
            }

            if (!createVMFolder()) {
                DialogUtils.oneDialog(this,
                        getString(R.string.oops),
                        getString(R.string.unable_to_cvbi_file_vm_dir_content),
                        getString(R.string.ok),
                        true,
                        R.drawable.warning_48px,
                        true,
                        null,
                        null);
                return;
            }

            whenProcessing(true);
            binding.custom.setVisibility(View.GONE);
            binding.ivIcon.setEnabled(false);

            zipFileSize = FileUtils.getFileSize(_filepath);
            ZipUtils.reset();

            timerTask = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run() {
                            folderSize = FileUtils.getFolderSize(AppConfig.vmFolder + vmID);
                            decompressionProgress = ZipUtils.getDecompressionProgress(folderSize, zipFileSize);
                            if (decompressionProgress > 0) {
                                if (decompressionProgress > 98) {
                                    binding.linearprogress.setIndeterminate(true);
                                } else {
                                    binding.linearprogress.setProgressCompat(ZipUtils.getDecompressionProgress(folderSize, zipFileSize), true);
                                }

                                binding.textviewprogress.setText(getResources().getString(R.string.about) + " " + ZipUtils.getRemainingDecompressionTime(folderSize, zipFileSize) + " " + getResources().getString(R.string.seconds_left));
                            }
                        }
                    });
                }
            };
            _timer.schedule(timerTask, 0, 1000);

            executor.execute(() -> {
                FileInputStream zipFile;
                try {
                    zipFile = (FileInputStream) getContentResolver().openInputStream((Uri.fromFile(new File(_filepath))));
                    File targetDirectory = new File(AppConfig.vmFolder + vmID);
                    ZipInputStream zis;
                    zis = new ZipInputStream(
                            new BufferedInputStream(zipFile));
                    try {
                        ZipEntry ze;
                        int count;
                        byte[] buffer = new byte[128 * 1024];
                        if (DeviceUtils.totalMemoryCapacity(getApplicationContext()) < 4L * 1024 * 1024 * 1024) {
                            buffer = new byte[64 * 1024];
                        }
                        while ((ze = zis.getNextEntry()) != null) {
                            File file = new File(targetDirectory, ze.getName());
                            File dir = ze.isDirectory() ? file : file.getParentFile();
                            assert dir != null;
                            if (!dir.isDirectory() && !dir.mkdirs())
                                Log.e(TAG, "importCVBI: Failed to ensure directory: " +
                                        dir.getAbsolutePath());
                            if (ze.isDirectory())
                                continue;
                            try (FileOutputStream fout = new FileOutputStream(file)) {
                                while ((count = zis.read(buffer)) != -1)
                                    fout.write(buffer, 0, count);
                            }
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> DialogUtils.oneDialog(CustomRomActivity.this,
                                getString(R.string.oops),
                                getString(R.string.could_not_process_cvbi_file_content),
                                getString(R.string.ok),
                                true,
                                R.drawable.warning_48px,
                                true,
                                null,
                                null));
                        Log.e(TAG, "importCVBI" + Objects.requireNonNull(e.getMessage()));
                    } finally {
                        Runnable runnable = () -> afterExtractCVBIFile(_filename);
                        runOnUiThread(runnable);
                        try {
                            zis.close();
                        } catch (IOException e) {
                            Log.e(TAG, "importCVBI" + Objects.requireNonNull(e.getMessage()));
                        }
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> DialogUtils.oneDialog(CustomRomActivity.this,
                            getString(R.string.oops),
                            getString(R.string.could_not_process_cvbi_file_content),
                            getString(R.string.ok),
                            true,
                            R.drawable.warning_48px,
                            true,
                            null,
                            null));
                    Log.e(TAG, "importCVBI" + Objects.requireNonNull(e.getMessage()));
                }
            });
        } else {
            if (getIntent().hasExtra("addromnow")) {
                DialogUtils.oneDialog(this, getResources().getString(R.string.problem_has_been_detected), getResources().getString(R.string.format_not_supported_please_select_file_with_format_cvbi), getResources().getString(R.string.ok), true, R.drawable.error_96px, true,
                        this::finish, this::finish);
            } else {
                DialogUtils.oneDialog(this, getResources().getString(R.string.problem_has_been_detected), getResources().getString(R.string.format_not_supported_please_select_file_with_format_cvbi), getResources().getString(R.string.ok), true, R.drawable.error_96px, true, null, null);
            }
        }

        if (Objects.requireNonNull(binding.drive.getText()).toString().isEmpty()) {
            binding.driveField.setEndIconDrawable(R.drawable.round_add_24);
        } else {
            binding.driveField.setEndIconDrawable(R.drawable.more_vert_24px);
        }
    }

    @SuppressLint("SetTextI18n")
    private void afterExtractCVBIFile(String _filename) {
        if (timerTask != null) {
            timerTask.cancel();
        }

        whenProcessing(false);
        binding.custom.setVisibility(View.VISIBLE);
        binding.ivIcon.setEnabled(true);
        try {
            if (!FileUtils.isFileExists(AppConfig.vmFolder + vmID + "/rom-data.json")) {
                String _getDiskFile = VMManager.quickScanDiskFileInFolder(AppConfig.vmFolder + vmID);
                if (!_getDiskFile.isEmpty()) {
                    //Error code: CR_CVBI2
                    if (getIntent().hasExtra("addromnow") && !addromnowdone) {
                        addromnowdone = true;
                        if (Objects.requireNonNull(getIntent().getStringExtra("romextra")).isEmpty()) {
                            setDefault();
                            binding.drive.setText(_getDiskFile);
                        } else {
                            if (Objects.requireNonNull(getIntent().getStringExtra("romextra")).contains(Objects.requireNonNull(getIntent().getStringExtra("finalromfilename")))) {
                                binding.qemu.setText(Objects.requireNonNull(getIntent().getStringExtra("romextra")).replaceAll(Objects.requireNonNull(getIntent().getStringExtra("finalromfilename")), "\"" + _getDiskFile + "\""));
                            } else {
                                binding.drive.setText(_getDiskFile);
                                binding.qemu.setText(Objects.requireNonNull(getIntent().getStringExtra("romextra")).replaceAll("OhnoIjustrealizeditsmidnightandIstillhavetodothis", AppConfig.vmFolder + vmID + "/"));
                            }
                        }

                        binding.title.setText(getIntent().getStringExtra("romname"));

                        if (!Objects.requireNonNull(getIntent().getStringExtra("romicon")).isEmpty()) {
                            File imgFile = new File(Objects.requireNonNull(getIntent().getStringExtra("romicon")));
                            if (imgFile.exists()) {
                                thumbnailPath = getIntent().getStringExtra("romicon");
                                thumbnailProcessing();
                            }
                        }
                    } else {
                        if (Objects.requireNonNull(binding.qemu.getText()).toString().isEmpty()) {
                            setDefault();
                        }
                        if (Objects.requireNonNull(binding.title.getText()).toString().isEmpty() || binding.title.getText().toString().equals("New VM")) {
                            binding.title.setText(_filename.replace(".cvbi", ""));
                        }
                        binding.drive.setText(_getDiskFile);
                        VMManager.setArch("X86_64", this);
                    }

                    DialogUtils.oneDialog(this, getResources().getString(R.string.oops), getResources().getString(R.string.error_CR_CVBI2), getResources().getString(R.string.ok), true, R.drawable.warning_48px, true, null, null);
                } else {
                    //Error code: CR_CVBI3
                    if (getIntent().hasExtra("addromnow")) {
                        DialogUtils.oneDialog(this, getResources().getString(R.string.oops), getResources().getString(R.string.error_CR_CVBI3), getResources().getString(R.string.ok), true, R.drawable.error_96px, true,
                                this::finish, this::finish);
                    } else {
                        DialogUtils.oneDialog(this, getResources().getString(R.string.oops), getResources().getString(R.string.error_CR_CVBI3), getResources().getString(R.string.ok), true, R.drawable.error_96px, true, null, null);
                    }
                }
            } else {
                JSONObject jObj = new JSONObject(FileUtils.readFromFile(this, new File(AppConfig.vmFolder + vmID + "/rom-data.json")));

                if (jObj.has("vmID")) {
                    if (!jObj.isNull("vmID")) {
                        if (!jObj.getString("vmID").isEmpty()) {
                            FileUtils.moveAFile(AppConfig.vmFolder + vmID, AppConfig.vmFolder + jObj.getString("vmID"));
                            vmID = jObj.getString("vmID");
                        }
                    }
                }

                if (jObj.has("title") && !jObj.isNull("title")) {
                    binding.title.setText(jObj.getString("title"));
                }

                if (jObj.has("drive") && !jObj.isNull("drive")) {
                    if (!jObj.getString("drive").isEmpty()) {
                        binding.drive.setText(AppConfig.vmFolder + vmID + "/" + jObj.getString("drive"));
                    }

                }

                if (jObj.has("qemu") && !jObj.isNull("qemu")) {
                    if (!jObj.getString("qemu").isEmpty()) {
                        binding.qemu.setText(jObj.getString("qemu").replaceAll("OhnoIjustrealizeditsmidnightandIstillhavetodothis", AppConfig.vmFolder + vmID + "/"));
                    }
                }

                if (jObj.has("icon") && !jObj.isNull("icon")) {
                    binding.ivAddThubnail.setImageResource(R.drawable.round_edit_24);
                    thumbnailPath = AppConfig.vmFolder + vmID + "/" + jObj.getString("icon");
                    thumbnailProcessing();
                } else {
                    binding.ivAddThubnail.setImageResource(R.drawable.round_add_24);
                    VMManager.setIconWithName(binding.ivIcon, Objects.requireNonNull(binding.title.getText()).toString());
                }

                if (jObj.has("cdrom") && !jObj.isNull("cdrom")) {
                    if (!jObj.getString("cdrom").isEmpty()) {
                        binding.cdrom.setText(AppConfig.vmFolder + vmID + "/" + jObj.getString("cdrom"));
                        binding.cdromField.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
                        binding.cdromField.setEndIconDrawable(R.drawable.close_24px);
                        changeOnClickCdrom();
                    } else {
                        binding.cdromField.setEndIconMode(TextInputLayout.END_ICON_NONE);
                    }
                } else {
                    binding.cdromField.setEndIconMode(TextInputLayout.END_ICON_NONE);
                }

                if (jObj.has("arch") && !jObj.isNull("arch")) {
                    VMManager.setArch(jObj.getString("arch"), this);
                } else {
                    VMManager.setArch("x86_64", this);
                }

                FileUtils.moveAFile(AppConfig.vmFolder + _filename.replace(".cvbi", ""), AppConfig.vmFolder + vmID);

                if (!jObj.has("drive") && !jObj.has("cdrom") && !jObj.has("qemu")) {
                    UIUtils.oneDialog(getResources().getString(R.string.problem_has_been_detected), getResources().getString(R.string.this_rom_is_missing_too_much_information), true, false, this);
                }

                if (!jObj.has("versioncode")) {
                    UIUtils.oneDialog(getResources().getString(R.string.problem_has_been_detected), getResources().getString(R.string.this_rom_may_not_be_compatible), true, false, this);
                }

                if (jObj.has("author") && !jObj.isNull("author") && jObj.has("desc") && !jObj.isNull("desc")) {
                    if (jObj.getString("desc").contains("<") && jObj.getString("desc").contains(">")) {
                        UIUtils.UIAlert(this, getResources().getString(R.string.from) + ": " + jObj.getString("author"), jObj.getString("desc"));
                    } else {
                        UIUtils.oneDialog(getResources().getString(R.string.from) + ": " + jObj.getString("author"), jObj.getString("desc"), true, false, this);
                    }
                }
            }
            binding.collapsingToolbarLayout.setSubtitle(MainSettingsManager.getArch(this));
        } catch (JSONException e) {
            Log.e(TAG, "afterExtractCVBIFile: " + e.getMessage());
        }
    }

    public String getPath(Uri uri) {
        return FileUtils.getPath(this, uri);
    }
}
