package com.vectras.vm.creator;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.AppConfig;
import com.vectras.vm.Fragment.CreateImageDialogFragment;
import com.vectras.vm.R;
import com.vectras.vm.RomInfo;
import com.vectras.vm.SplashActivity;
import com.vectras.vm.VMManager;
import com.vectras.vm.main.vms.DataMainRoms;
import com.vectras.vm.databinding.ActivityVmCreatorBinding;
import com.vectras.vm.databinding.DialogProgressStyleBinding;
import com.vectras.vm.main.MainActivity;
import com.vectras.vm.manager.VmFileManager;
import com.vectras.vm.utils.ClipboardUltils;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.ImageUtils;
import com.vectras.vm.utils.IntentUtils;
import com.vectras.vm.utils.JSONUtils;
import com.vectras.vm.utils.PackageUtils;
import com.vectras.vm.utils.UIUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.vectras.vm.utils.ZipUtils;
import com.vectras.vterm.Terminal;
import com.vectras.vm.utils.PermissionUtils;

public class VMCreatorActivity extends AppCompatActivity {

    private final String TAG = "VMCreatorActivity";
    private ActivityVmCreatorBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private DialogProgressStyleBinding dialogProgressStyleBinding;
    private AlertDialog progressDialog;
    boolean iseditparams = false;
    public String previousName = "";
    public boolean addromnowdone = false;
    private boolean created = false;
    boolean modify;
    private boolean isProcessingFile = false;
    public static DataMainRoms current;
    private String thumbnailPath = "";
    private String vmID = VMManager.idGenerator();
    private boolean isShowBootMenu = false;
    private boolean isUseUefi = false;
    private boolean isUseLocalTime = true;
    private int bootFrom = 0;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.add_file) {
            filePicker.launch("*/*");
            return true;
        } else if (id == R.id.show_in_folder) {
            FileUtils.openFolder(this, VmFileManager.getPath(vmID));
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.vm_creator_toolbar_menu, menu);
        return true;
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.edgeToEdge(this);
        binding = ActivityVmCreatorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dialogProgressStyleBinding = DialogProgressStyleBinding.inflate(getLayoutInflater());
        progressDialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogProgressStyleBinding.getRoot())
                .setCancelable(false)
                .create();

        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        UIUtils.setOnApplyWindowInsetsListener(binding.main);

        binding.collapsingToolbarLayout.setSubtitle(MainSettingsManager.getArch(this));

        binding.btnCreate.setOnClickListener(v -> startCreateVM());

        binding.drive.setOnClickListener(v -> diskPicker.launch("*/*"));
        binding.driveField.setOnClickListener(v -> diskPicker.launch("*/*"));

        binding.driveField.setEndIconOnClickListener(v -> {
            if (Objects.requireNonNull(binding.drive.getText()).toString().isEmpty()) {
                CreateImageDialogFragment dialogFragment = new CreateImageDialogFragment();
                dialogFragment.folder = VmFileManager.getPath(vmID);
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
                            if (binding.drive.getText().toString().contains(VmFileManager.quickGetPath(vmID))) {
                                FileUtils.delete(new File(Objects.requireNonNull(binding.drive.getText()).toString()));
                            }
                            binding.drive.setText("");
                            binding.driveField.setEndIconDrawable(R.drawable.add_24px);
                        },
                        () -> {
                            if (createVMFolder(true)) {
                                CreateImageDialogFragment dialogFragment = new CreateImageDialogFragment();
                                dialogFragment.customRom = true;
                                dialogFragment.filename = Objects.requireNonNull(binding.title.getText()).toString();
                                dialogFragment.drive = binding.drive;
                                dialogFragment.driveLayout = binding.driveField;
                                dialogFragment.show(getSupportFragmentManager(), "CreateImageDialogFragment");
                            }
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

        binding.qemu.setOnClickListener(v -> {
            iseditparams = true;
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), QemuParamsEditorActivity.class);
            intent.putExtra("content", Objects.requireNonNull(binding.qemu.getText()).toString());
            startActivity(intent);
        });

        binding.qemuField.setOnClickListener(v -> {
            iseditparams = true;
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), QemuParamsEditorActivity.class);
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
                            binding.ivAddThubnail.setImageResource(R.drawable.add_24px);
                            VMManager.setIconWithName(binding.ivIcon, Objects.requireNonNull(binding.title.getText()).toString());
                        }, null);
            }
        });

        binding.lineardisclaimer.setOnClickListener(v -> DialogUtils.oneDialog(this, getResources().getString(R.string.dont_miss_out), getResources().getString(R.string.disclaimer_when_using_rom), getResources().getString(R.string.i_agree), true, R.drawable.verified_user_24px, true, null, null));

        binding.cbvShowbootmenu.setOnCheckedChangeListener((v, isChecked) -> isShowBootMenu = isChecked);

        binding.cbvUseuefi.setOnCheckedChangeListener((v, isChecked) -> isUseUefi = isChecked);

        binding.cbvUselocaltime.setOnCheckedChangeListener((v, isChecked) -> isUseLocalTime = isChecked);

        if (!MainSettingsManager.getArch(this).equals("X86_64"))
            binding.cbvUseuefi.setVisibility(View.GONE);

        binding.sbvBootfrom.setOnClickListener(v -> VMCreatorSelector.bootFrom(this, bootFrom, ((position, name, value) -> {
            bootFrom = position;
            binding.sbvBootfrom.setSubtitle(name);
        })));

        modify = getIntent().getBooleanExtra("MODIFY", false);
        if (modify) {
            binding.collapsingToolbarLayout.setTitle(getString(R.string.edit));
            created = true;

            loadConfig(VMManager.getVMConfig(getIntent().getIntExtra("POS", 0)));

            vmID = getIntent().getStringExtra("VMID");

            if (vmID == null || vmID.isEmpty()) {
                vmID = VMManager.idGenerator();
            }

            previousName = current.itemName;
        } else {
            checkVMID();
            if (getIntent().hasExtra("addromnow")) {
                if (Objects.requireNonNull(getIntent().getStringExtra("romextra")).isEmpty()) {
                    setDefault();
                } else {
                    binding.qemu.setText(VmFileManager.textMarkToPath(vmID, Objects.requireNonNull(getIntent().getStringExtra("romextra"))));
                }

                binding.title.setText(getIntent().getStringExtra("romname"));

                if (getIntent().hasExtra("romicon") && !Objects.requireNonNull(getIntent().getStringExtra("romicon")).isEmpty()) {
                    startProcessingThumbnail(Uri.parse(getIntent().getStringExtra("romicon")));
                }

                if (Objects.requireNonNull(getIntent().getStringExtra("romfilename")).endsWith(".cvbi")) {
                    importRom(
                            getIntent().hasExtra("romuri") ?
                                    Uri.parse(getIntent().getStringExtra("romuri")) :
                                    null, Objects.requireNonNull(getIntent().getStringExtra("rompath")),
                            Objects.requireNonNull(getIntent().getStringExtra("romfilename")));
                } else {
                    addromnowdone = true;
                    if (!Objects.requireNonNull(getIntent().getStringExtra("rompath")).isEmpty()) {
                        selectedDiskFile(Uri.fromFile(new File((Objects.requireNonNull(getIntent().getStringExtra("rompath"))))), false);
                    }
                    if (!Objects.requireNonNull(getIntent().getStringExtra("addtodrive")).isEmpty()) {
                        binding.drive.setText(VmFileManager.getPath(vmID, getIntent().getStringExtra("romfilename")));
                        if (Objects.requireNonNull(binding.drive.getText()).toString().isEmpty()) {
                            binding.driveField.setEndIconDrawable(R.drawable.add_24px);
                        } else {
                            binding.driveField.setEndIconDrawable(R.drawable.more_vert_24px);
                        }
                    } else {
                        binding.driveField.setEndIconDrawable(R.drawable.add_24px);
                    }
                }

            } else if (getIntent().hasExtra("importcvbinow")) {
                setDefault();
                cvbiPicker.launch("*/*");
            } else {
                setDefault();
                if (MainSettingsManager.autoCreateDisk(this)) {
                    if (createVMFolder(true)) {
                        Terminal vterm = new Terminal(this);
                        vterm.executeShellCommand2("qemu-img create -f qcow2 " + VmFileManager.getPath(vmID, "disk.qcow2") + " 128G", false, this);
                        binding.drive.setText(VmFileManager.getPath(vmID, "disk.qcow2"));
                    }
                } else {
                    binding.driveField.setEndIconDrawable(R.drawable.add_24px);
                }

            }
        }

        if (PackageUtils.getVersionCode("com.anbui.cqcm.app", this) < 735 || !FileUtils.isFileExists(VmFileManager.getCreateCommandConfigFile(vmID))) {
            binding.opencqcm.setVisibility(View.GONE);
        } else {
            binding.opencqcm.setOnClickListener(v -> {
                if (PackageUtils.isInstalled("com.anbui.cqcm.app", this)) {
                    Intent intentcqcm = new Intent();
                    intentcqcm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intentcqcm.setComponent(new ComponentName("com.anbui.cqcm.app", "com.anbui.cqcm.app.DownloadActivity"));
                    intentcqcm.putExtra("content", FileUtils.readAFile(VmFileManager.getCreateCommandConfigFile(vmID)));
                    intentcqcm.putExtra("vectrasVMId", vmID);
                    startActivity(intentcqcm);
                    finish();
                } else {
                    IntentUtils.openUrl(this, "https://play.google.com/store/apps/details?id=com.anbui.cqcm.app", true);
                }
            });
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBack();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        PermissionUtils.storagepermission(this, true);
        if (iseditparams) {
            iseditparams = false;
            binding.qemu.setText(QemuParamsEditorActivity.result);
        }
    }

    private void onBack() {
        if (isProcessingFile) return;

        if (!created && !modify) {
            new Thread(() -> VmFileManager.delete(this, vmID)).start();
        }
        modify = false;
        finish();
    }

    public void onDestroy() {
        if (!created && !modify) {
            new Thread(() -> VmFileManager.delete(this, vmID)).start();
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
                    showProgressDialog(getString(R.string.copying_file));

                    executor.execute(() -> {
                        try {
                            isProcessingFile = true;
                            String _filename = FileUtils.getFileNameFromUri(this, uri);
                            if (_filename == null || _filename.isEmpty()) {
                                _filename = String.valueOf(System.currentTimeMillis());
                            }

                            FileUtils.copyFileFromUri(this, uri, VmFileManager.getPath(vmID, _filename));

                            String final_filename = _filename;
                            runOnUiThread(() -> {
                                binding.cdrom.setText(VmFileManager.getPath(vmID, final_filename));
                                binding.cdromField.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
                                binding.cdromField.setEndIconDrawable(R.drawable.close_24px);
                                changeOnClickCdrom();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> DialogUtils.oneDialog(this,
                                    getString(R.string.oops),
                                    getString(R.string.unable_to_copy_iso_file_content),
                                    getString(R.string.ok),
                                    true,
                                    R.drawable.warning_48px,
                                    true,
                                    null,
                                    null));
                            Log.e(TAG, "isoPicker: " + e.getMessage());
                        } finally {
                            isProcessingFile = false;
                            runOnUiThread(() -> DialogUtils.safeDismiss(this, progressDialog));
                        }
                    });
                } else {
                    executor.execute(() -> {
                        if (!FileUtils.isValidFilePath(this, FileUtils.getPath(this, uri), false)) {
                            runOnUiThread(() -> DialogUtils.oneDialog(this,
                                    getString(R.string.problem_has_been_detected),
                                    getString(R.string.invalid_file_path_content),
                                    getString(R.string.ok),
                                    true,
                                    R.drawable.warning_48px,
                                    true,
                                    null,
                                    null)
                            );
                            return;
                        }
                        File selectedFilePath = new File(getPath(uri));
                        if (selectedFilePath.getName().endsWith(".iso")) {
                            runOnUiThread(() -> binding.cdrom.setText(selectedFilePath.getPath()));
                        } else {
                            runOnUiThread(() -> DialogUtils.oneDialog(this,
                                    getString(R.string.problem_has_been_detected),
                                    getString(R.string.invalid_iso_file_path_content),
                                    getString(R.string.ok),
                                    true,
                                    R.drawable.warning_48px,
                                    true,
                                    null,
                                    null)
                            );
                        }
                    });
                }
            });

    private final ActivityResultLauncher<String> cvbiPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null)
                    return;

                executor.execute(() -> {
                    String filePath;
                    try {
                        File selectedFilePath = new File(getPath(uri));
                        filePath = selectedFilePath.getAbsolutePath();
                    } catch (Exception e) {
                        filePath = "";
                    }

                    String finalFilePath = filePath;
                    runOnUiThread(() -> importRom(uri, finalFilePath, FileUtils.getFileNameFromUri(this, uri)));
                });
            });

    @SuppressLint("SetTextI18n")
    private final ActivityResultLauncher<String> filePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                showProgressDialog(getString(R.string.copying_file));

                executor.execute(() -> {
                    try {
                        isProcessingFile = true;
                        String _filename = FileUtils.getFileNameFromUri(this, uri);
                        if (_filename == null || _filename.isEmpty()) {
                            _filename = String.valueOf(System.currentTimeMillis());
                        }

                        String filePath = VmFileManager.getPath(vmID, _filename);

                        FileUtils.copyFileFromUri(this, uri, filePath);

                        runOnUiThread(() -> DialogUtils.twoDialog(this,
                                getString(R.string.file_added),
                                filePath,
                                getString(R.string.copy_full_path),
                                getString(R.string.close),
                                true,
                                R.drawable.check_24px,
                                true,
                                () -> ClipboardUltils.copyToClipboard(this, filePath),
                                null,
                                null));
                    } catch (Exception e) {
                        runOnUiThread(() -> DialogUtils.oneDialog(this,
                                getString(R.string.oops),
                                getString(R.string.adding_file_failed_content),
                                getString(R.string.ok),
                                true,
                                R.drawable.error_96px,
                                true,
                                null,
                                null));
                        Log.e(TAG, "filePicker: " + e.getMessage());
                    } finally {
                        isProcessingFile = false;
                        runOnUiThread(() -> DialogUtils.safeDismiss(this, progressDialog));
                    }
                });
            });

    private void loadConfig(DataMainRoms vmConfig) {
        if (vmConfig != null) current = vmConfig;
        if (binding != null && current != null) {
            if (current.itemArch != null) {
                VMManager.setArch(current.itemArch, this);
                binding.collapsingToolbarLayout.setSubtitle(MainSettingsManager.getArch(this));
            }

            if (current.itemName != null) {
                binding.title.setText(current.itemName);
            }

            if (current.itemPath != null && !current.itemPath.isEmpty()) {
                binding.drive.setText((current.itemPath.contains("/") ? "" : VmFileManager.getPath(vmID)).concat(current.itemPath));
            }

            if (Objects.requireNonNull(binding.drive.getText()).toString().isEmpty()) {
                binding.driveField.setEndIconDrawable(R.drawable.add_24px);
            } else {
                binding.driveField.setEndIconDrawable(R.drawable.more_vert_24px);
            }

            if (current.imgCdrom != null && !current.imgCdrom.isEmpty()) {
                binding.cdrom.setText((current.imgCdrom.contains("/") ? "" : VmFileManager.getPath(vmID)).concat(current.imgCdrom));
            }

            if (!Objects.requireNonNull(binding.cdrom.getText()).toString().isEmpty()) {
                binding.cdromField.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
                binding.cdromField.setEndIconDrawable(R.drawable.close_24px);
                changeOnClickCdrom();
            }

            if (current.itemIcon != null && !current.itemIcon.isEmpty()) {
                thumbnailPath = (current.itemIcon.contains("/") ? "" : VmFileManager.getPath(vmID)) + current.itemIcon;
                thumbnailProcessing();
            }

            if (current.itemExtra != null) {
                binding.qemu.setText(VmFileManager.textMarkToPath(vmID, current.itemExtra));
            }

            bootFrom = current.bootFrom;
            binding.sbvBootfrom.setSubtitle(Objects.requireNonNull(VMCreatorSelector.getBootFrom(this, current.bootFrom).get("name")).toString());
            isShowBootMenu = current.isShowBootMenu;
            binding.cbvShowbootmenu.setChecked(isShowBootMenu);

            if (MainSettingsManager.getArch(this).equals("X86_64")) {
                isUseUefi = current.isUseUefi;
                binding.cbvUseuefi.setChecked(isUseUefi);
            } else {
                binding.cbvUseuefi.setVisibility(View.GONE);
            }

            isUseLocalTime = current.isUseLocalTime;
            binding.cbvUselocaltime.setChecked(isUseLocalTime);
        }
    }

    private void setDefault() {
        String defQemuParams;
        if (DeviceUtils.is64bit()) {
            defQemuParams = switch (MainSettingsManager.getArch(this)) {
                case "ARM64" ->
                        "-M virt,virtualization=true -cpu cortex-a76 -accel tcg,thread=multi -net nic,model=e1000 -net user -device nec-usb-xhci -device usb-kbd -device usb-mouse -device VGA";
                case "PPC" -> "-M mac99 -cpu g4 -accel tcg,thread=multi -smp 1";
                case "I386" ->
                        "-M pc -cpu coreduo,+popcnt -accel tcg,thread=multi -smp 4 -vga std -netdev user,id=usernet -device e1000,netdev=usernet  -usb -device usb-tablet";
                default ->
                        "-M pc -cpu core2duo,+popcnt -accel tcg,thread=multi -smp 4 -vga std -netdev user,id=usernet -device e1000,netdev=usernet  -usb -device usb-tablet";
            };
        } else {
            defQemuParams = switch (MainSettingsManager.getArch(this)) {
                case "ARM64" ->
                        "-M virt -cpu cortex-a76 -net nic,model=e1000 -net user -device nec-usb-xhci -device usb-kbd -device usb-mouse -device VGA";
                case "PPC" -> "-M mac99 -cpu g4 -smp 1";
                case "I386" ->
                        "-M pc -cpu coreduo,+popcnt -smp 4 -vga std -netdev user,id=usernet -device e1000,netdev=usernet  -usb -device usb-tablet";
                default ->
                        "-M pc -cpu core2duo,+popcnt -smp 4 -vga std -netdev user,id=usernet -device e1000,netdev=usernet -usb -device usb-tablet";
            };
        }
        binding.title.setText(getString(R.string.new_vm));
        binding.qemu.setText(defQemuParams);
    }

    private void checkVMID() {
        if (FileUtils.isFileExists(AppConfig.maindirpath + "/roms/" + vmID) || vmID.isEmpty()) {
            vmID = VMManager.idGenerator();
        }
    }

    private boolean createVMFolder(boolean isShowDialog) {
        File romDir = new File(VmFileManager.getPath(vmID));
        if (!romDir.exists()) {
            if (!romDir.mkdirs()) {
                if (isShowDialog) DialogUtils.oneDialog(this,
                        getResources().getString(R.string.oops),
                        getResources().getString(R.string.unable_to_create_the_directory_to_create_the_vm),
                        getResources().getString(R.string.ok),
                        true,
                        R.drawable.error_96px,
                        true,
                        getIntent().hasExtra("addromnow") ? this::finish : null,
                        getIntent().hasExtra("addromnow") ? this::finish : null
                );
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
                        this::createNewVM, null, null);
            }
        }
    }

    private void createNewVM() {
        if (FileUtils.isFileExists(AppConfig.romsdatajson)) {
            if (!VMManager.isRomsDataJsonValid(true, this)) {
                return;
            }
        } else {
            FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", "[]");
        }

        if (!VMManager.addVM(finalDataConfig(), modify ? getIntent().getIntExtra("POS", 0) : -1)) {
            DialogUtils.oneDialog(
                    this,
                    getString(R.string.oops),
                    getString(R.string.unable_to_save_please_try_again_later),
                    R.drawable.error_96px);
            return;
        }

        created = true;

        if (getIntent().hasExtra("addromnow")) {
            RomInfo.isFinishNow = true;
            MainActivity.isOpenHome = true;
        }

        modify = false;
        if (!MainActivity.isActivate) {
            startActivity(new Intent(this, SplashActivity.class));
        } else {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.setClass(this, MainActivity.class);
            startActivity(intent);
        }
        finish();
    }

    private HashMap<String, Object> finalDataConfig() {
        HashMap<String, Object> vmConfigMap = new HashMap<>();
        vmConfigMap.put("imgName", Objects.requireNonNull(binding.title.getText()).toString());
        vmConfigMap.put("imgIcon", thumbnailPath);
        vmConfigMap.put("imgPath", Objects.requireNonNull(binding.drive.getText()).toString());
        vmConfigMap.put("imgCdrom", Objects.requireNonNull(binding.cdrom.getText()).toString());
        vmConfigMap.put("imgExtra", Objects.requireNonNull(binding.qemu.getText()).toString());
        vmConfigMap.put("imgArch", MainSettingsManager.getArch(this));
        vmConfigMap.put("bootFrom", bootFrom);
        vmConfigMap.put("isShowBootMenu", isShowBootMenu);
        vmConfigMap.put("isUseUefi", isUseUefi);
        vmConfigMap.put("isUseLocalTime", isUseLocalTime);
        vmConfigMap.put("vmID", vmID);
        vmConfigMap.put("qmpPort", 8080);
        return vmConfigMap;
    }

    private void startProcessingThumbnail(Uri uri) {
        showProgressDialog(getString(R.string.just_a_sec));

        executor.execute(() -> {
            try {
                isProcessingFile = true;
                ImageUtils.convertToPng(this, uri, VmFileManager.getThumbnail(vmID));

                thumbnailPath = VmFileManager.getThumbnail(vmID);
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
                isProcessingFile = false;
                runOnUiThread(() -> DialogUtils.safeDismiss(this, progressDialog));
            }
        });
    }

    private void thumbnailProcessing() {
        if (isFinishing() || isDestroyed()) return;
        if (!thumbnailPath.isEmpty()) {
            binding.ivAddThubnail.setImageResource(R.drawable.edit_24px);
            File imgFile = new File(thumbnailPath);

            if (imgFile.exists()) {
                Glide.with(this)
                        .load(new File(thumbnailPath))
                        .placeholder(R.drawable.ic_computer_180dp_with_padding)
                        .error(R.drawable.ic_computer_180dp_with_padding)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(binding.ivIcon);
            } else {
                binding.ivAddThubnail.setImageResource(R.drawable.add_24px);
                VMManager.setIconWithName(binding.ivIcon, Objects.requireNonNull(binding.title.getText()).toString());
            }
        } else {
            binding.ivAddThubnail.setImageResource(R.drawable.add_24px);
        }
    }

    private void selectedDiskFile(Uri _content_describer, boolean _addtodrive) {
        new Thread(() -> {
            if (FileUtils.isValidFilePath(this, FileUtils.getPath(this, _content_describer), false)) {
                File selectedFilePath = new File(getPath(_content_describer));
                runOnUiThread(() -> {
                    if (VMManager.isADiskFile(selectedFilePath.getPath())) {
                        startProcessingHardDriveFile(_content_describer, _addtodrive);
                    } else {
                        DialogUtils.twoDialog(this, getString(R.string.problem_has_been_detected), getString(R.string.file_format_is_not_supported), getResources().getString(R.string.continuetext), getResources().getString(R.string.cancel), true, R.drawable.hard_drive_24px, true,
                                () -> startProcessingHardDriveFile(_content_describer, _addtodrive), null, null);
                    }
                });
            } else {
                runOnUiThread(() -> startProcessingHardDriveFile(_content_describer, _addtodrive));
            }
        }).start();
    }

    @SuppressLint("SetTextI18n")
    private void startProcessingHardDriveFile(Uri _content_describer, boolean _addtodrive) {
        if (MainSettingsManager.copyFile(this)) {

            if (isFinishing() || isDestroyed() || !createVMFolder(true)) return;
            showProgressDialog(getString(R.string.copying_file));

            executor.execute(() -> {
                try {
                    String _filename = FileUtils.getFileNameFromUri(this, _content_describer);
                    if (_filename == null || _filename.isEmpty()) {
                        _filename = String.valueOf(System.currentTimeMillis());
                    }

                    FileUtils.copyFileFromUri(this, _content_describer, VmFileManager.getPath(vmID, _filename));

                    String final_filename = _filename;
                    runOnUiThread(() -> {
                        if ((isFinishing() || isDestroyed())) {
                            if (!vmID.isEmpty())
                                FileUtils.delete(new File(VmFileManager.getPath(vmID, final_filename)));
                            return;
                        }
                        if (_addtodrive) {
                            binding.drive.setText(VmFileManager.getPath(vmID, final_filename));
                            binding.driveField.setEndIconDrawable(R.drawable.more_vert_24px);
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> DialogUtils.oneDialog(this,
                            getString(R.string.oops),
                            getString(R.string.unable_to_copy_hard_drive_file_content),
                            getString(R.string.ok),
                            true,
                            R.drawable.warning_48px,
                            true,
                            null,
                            null));
                } finally {
                    runOnUiThread(() -> DialogUtils.safeDismiss(this, progressDialog));
                }
            });
        } else {
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

    @SuppressLint("SetTextI18n")
    private void importRom(Uri fileUri, String filePath, String fileName) {
        if (isFinishing() || isDestroyed()) return;

        if (!(fileName.endsWith(".cvbi") || filePath.endsWith(".cvbi.zip"))) {
            DialogUtils.oneDialog(this,
                    getResources().getString(R.string.problem_has_been_detected),
                    getResources().getString(R.string.format_not_supported_please_select_file_with_format_cvbi),
                    getResources().getString(R.string.ok),
                    true,
                    R.drawable.error_96px,
                    true,
                    getIntent().hasExtra("addromnow") ? this::finish : null,
                    getIntent().hasExtra("addromnow") ? this::finish : null
            );
            return;
        }

        boolean isUseUri;

        if (filePath.isEmpty() || !FileUtils.isFileExists(filePath)) {
            if (fileUri != null && !fileUri.toString().isEmpty()) {
                isUseUri = true;
            } else {
                DialogUtils.oneDialog(this,
                        getResources().getString(R.string.oops),
                        getResources().getString(R.string.error_CR_CVBI1),
                        getResources().getString(R.string.ok),
                        true,
                        R.drawable.error_96px,
                        true,
                        getIntent().hasExtra("addromnow") ? this::finish : null,
                        getIntent().hasExtra("addromnow") ? this::finish : null
                );
                return;
            }
        } else {
            isUseUri = false;
        }

        if (!createVMFolder(false)) {
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

        isProcessingFile = true;

        showProgressDialog(getString(R.string.importing) + "\n" + getString(R.string.please_stay_here));

        Log.i(TAG, "importRom: Extracting with " + (isUseUri ? "uri" : "path") + " from " + filePath + " to " + VmFileManager.getPath(vmID));

        new Thread(() -> {
            boolean result = isUseUri ? ZipUtils.extract(
                    this,
                    fileUri,
                    VmFileManager.getPath(vmID),
                    dialogProgressStyleBinding.progressText,
                    dialogProgressStyleBinding.progressBar
            ) : ZipUtils.extract(
                    this,
                    filePath,
                    VmFileManager.getPath(vmID),
                    dialogProgressStyleBinding.progressText,
                    dialogProgressStyleBinding.progressBar
            );

            runOnUiThread(() -> {
                DialogUtils.safeDismiss(this, progressDialog);

                if (isFinishing() || isDestroyed()) {
                    new Thread(() -> VmFileManager.delete(this, vmID)).start();
                    return;
                }

                if (result) {
                    afterExtractCVBIFile(fileName);
                } else {
                    runOnUiThread(() -> DialogUtils.oneDialog(VMCreatorActivity.this,
                            getString(R.string.oops),
                            getString(R.string.could_not_process_cvbi_file_content),
                            getString(R.string.ok),
                            true,
                            R.drawable.warning_48px,
                            true,
                            null,
                            null));
                }
            });
        }).start();
    }

    @SuppressLint("SetTextI18n")
    private void afterExtractCVBIFile(String _filename) {
        isProcessingFile = false;
        binding.ivIcon.setEnabled(true);
        try {
            if (!VmFileManager.isConfigFileExists(vmID)) {
                String _getDiskFile = VMManager.quickScanDiskFileInFolder(VmFileManager.getPath(vmID));
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
                                binding.qemu.setText(VmFileManager.textMarkToPath(vmID, Objects.requireNonNull(getIntent().getStringExtra("romextra"))));
                            }
                        }

                        binding.title.setText(getIntent().getStringExtra("romname"));

                        if (getIntent().hasExtra("romicon") && !Objects.requireNonNull(getIntent().getStringExtra("romicon")).isEmpty()) {
                            startProcessingThumbnail(Uri.parse(getIntent().getStringExtra("romicon")));
                        }
                    } else {
                        if (Objects.requireNonNull(binding.qemu.getText()).toString().isEmpty()) {
                            setDefault();
                        }
                        if (Objects.requireNonNull(binding.title.getText()).toString().isEmpty() || binding.title.getText().toString().equals("New VM")) {
                            binding.title.setText(_filename.replace(".cvbi", ""));
                        }
                        binding.drive.setText(_getDiskFile);
                        VMManager.setArch("", this);
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
                if (!JSONUtils.isValidFromFile(VmFileManager.getConfigFile(vmID))) {
                    DialogUtils.oneDialog(this, getResources().getString(R.string.oops), getResources().getString(R.string.error_CR_CVBI4), getResources().getString(R.string.ok), true, R.drawable.warning_48px, true, null, null);
                    return;
                }

                try {
                    loadConfig(new Gson().fromJson(FileUtils.readFromFile(this, new File(VmFileManager.getConfigFile(vmID))), DataMainRoms.class));
                } catch (JsonSyntaxException e) {
                    DialogUtils.oneDialog(this, getResources().getString(R.string.oops), getResources().getString(R.string.error_CR_CVBI4), getResources().getString(R.string.ok), true, R.drawable.warning_48px, true, null, null);
                    return;
                }

                JSONObject jObj = new JSONObject(FileUtils.readFromFile(this, new File(VmFileManager.getConfigFile(vmID))));

                if (jObj.has("vmID")) {
                    if (!jObj.isNull("vmID")) {
                        if (!jObj.getString("vmID").isEmpty()) {
                            FileUtils.move(VmFileManager.getConfigFile(vmID), VmFileManager.getConfigFile( jObj.getString("vmID")));
                            vmID = jObj.getString("vmID");
                        }
                    }
                }

                //It can be deleted because there are few users of the old version.
                if (!_filename.replace(".cvbi", "").isEmpty())
                    FileUtils.move(AppConfig.vmFolder + _filename.replace(".cvbi", ""), VmFileManager.getPath(vmID));

                if (!jObj.has("drive") && !jObj.has("cdrom") && !jObj.has("qemu")) {
                    DialogUtils.oneDialog(this, getResources().getString(R.string.problem_has_been_detected), getResources().getString(R.string.this_rom_is_missing_too_much_information), R.drawable.warning_24px);
                }

                if (!jObj.has("versioncode")) {
                    DialogUtils.oneDialog(this, getResources().getString(R.string.problem_has_been_detected), getResources().getString(R.string.this_rom_may_not_be_compatible), R.drawable.warning_24px);
                }

                if (jObj.has("author") && !jObj.isNull("author") && jObj.has("desc") && !jObj.isNull("desc")) {
                    DialogUtils.oneDialog(this,
                            getString(R.string.from) + " " + jObj.getString("author"),
                            jObj.getString("desc"),
                            getString(R.string.ok),
                            true,
                            R.drawable.info_24px,
                            false,
                            null,
                            null);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "afterExtractCVBIFile: " + e.getMessage());
        }

        if (VmFileManager.isCreateCommandConfigFileExists(vmID)) {
            FileUtils.writeToFile(VmFileManager.getPath(vmID), VmFileManager.CREATE_COMMAND_CONFIG_FILE_NAME, VmFileManager.textMarkToPath(vmID, FileUtils.readAFile(VmFileManager.getCreateCommandConfigFile(vmID))));
        }

        if (VmFileManager.isSnapshotShExists(vmID)) {
            FileUtils.writeToFile(VmFileManager.getPath(vmID), VmFileManager.SNAPSHOT_SH_FILE_NAME, VmFileManager.textMarkToPath(vmID, FileUtils.readAFile(VmFileManager.getSnapshotSh(vmID))));
        }
    }

    private void showProgressDialog(String message) {
        dialogProgressStyleBinding.progressText.setText(message);
        dialogProgressStyleBinding.progressBar.setIndeterminate(true);
        progressDialog.show();
    }

    public String getPath(Uri uri) {
        return FileUtils.getPath(this, uri);
    }
}
