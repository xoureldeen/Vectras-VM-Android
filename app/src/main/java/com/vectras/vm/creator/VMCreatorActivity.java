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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.AppConfig;
import com.vectras.vm.Fragment.CreateImageDialogFragment;
import com.vectras.vm.R;
import com.vectras.vm.store.RomInfo;
import com.vectras.vm.SplashActivity;
import com.vectras.vm.VMManager;
import com.vectras.vm.main.vms.DataMainRoms;
import com.vectras.vm.databinding.ActivityVmCreatorBinding;
import com.vectras.vm.databinding.DialogProgressStyleBinding;
import com.vectras.vm.main.MainActivity;
import com.vectras.vm.manager.FormatManager;
import com.vectras.vm.manager.VmFileManager;
import com.vectras.vm.utils.ClipboardUltils;
import com.vectras.vm.utils.CpuHelper;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.ImageUtils;
import com.vectras.vm.utils.IntentUtils;
import com.vectras.vm.utils.JSONUtils;
import com.vectras.vm.utils.PackageUtils;
import com.vectras.vm.utils.ProgressDialog;
import com.vectras.vm.utils.UIUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    public static DataMainRoms current = new DataMainRoms();
    private String thumbnailPath = "";
    private String vmID = VMManager.idGenerator();
    private int cpu = 0;
    private int cores = 0;
    private int threads = 0;
    private boolean isUseBattery = false;
    private boolean isShowBootMenu = false;
    private boolean isUseLocalTime = true;
    private boolean isUseUefi = false;
    private boolean isUseDefaultBios = true;
    private boolean sharedFolder = false;
    private int bootFrom = 0;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.add_file) {
            try {
                filePicker.launch("*/*");
            } catch (Exception e) {
                IntentUtils.showErrorDialog(this);
            }
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

        setupStorageUi();

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

                            if (FileUtils.isFileExists(VmFileManager.getThumbnail(vmID))) markDelete(VmFileManager.getThumbnail(vmID));
                        }, null);
            }
        });

        binding.lineardisclaimer.setOnClickListener(v -> DialogUtils.oneDialog(this, getResources().getString(R.string.dont_miss_out), getResources().getString(R.string.disclaimer_when_using_rom), getResources().getString(R.string.i_agree), true, R.drawable.verified_user_24px, true, null, null));


        binding.sbvCpu.setOnClickListener(v -> VMCreatorSelector.cpu(this, MainSettingsManager.getArch(this), cpu, ((position, name, value) -> {
            cpu = position;
            binding.sbvCpu.setSubtitle(name);
        })));

        binding.sbvCore.setOnClickListener(v -> VMCreatorSelector.cpuCore(this, MainSettingsManager.getArch(this), cores, ((position, name, value) -> {
            cores = position;
            binding.sbvCore.setSubtitle(name);
        })));

        binding.sbvThread.setOnClickListener(v -> VMCreatorSelector.cpuThread(this, MainSettingsManager.getArch(this), threads, ((position, name, value) -> {
            threads = position;
            binding.sbvThread.setSubtitle(name);
        })));

        binding.cbvBattery.setOnCheckedChangeListener((v, isChecked) -> isUseBattery = isChecked);



        binding.sbvBootfrom.setOnClickListener(v -> VMCreatorSelector.bootFrom(this, bootFrom, ((position, name, value) -> {
            bootFrom = position;
            binding.sbvBootfrom.setSubtitle(name);
        })));

        binding.cbvShowbootmenu.setOnCheckedChangeListener((v, isChecked) -> isShowBootMenu = isChecked);

        binding.cbvUselocaltime.setOnCheckedChangeListener((v, isChecked) -> isUseLocalTime = isChecked);

        if (!MainSettingsManager.getArch(this).equals("X86_64")) {
            binding.cbvUseuefi.setVisibility(View.GONE);
        } else {
            binding.cbvUseuefi.setOnCheckedChangeListener((v, isChecked) -> isUseUefi = isChecked);
        }

        binding.cbvUseDefaultBios.setOnCheckedChangeListener((v, isChecked) -> {
            isUseDefaultBios = isChecked;
            binding.cbvUseuefi.setEnabled(isChecked);
        });



        VmFileManager.removeTemp(this, vmID, peddingTempFolder);

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
                    binding.qemu.setText(VmFileManager.textMarkToPath(this, vmID, Objects.requireNonNull(getIntent().getStringExtra("romextra"))));
                }

                binding.title.setText(getIntent().getStringExtra("romname"));

                if (getIntent().hasExtra("romicon") && !Objects.requireNonNull(getIntent().getStringExtra("romicon")).isEmpty()) {
                    handleThumbnail(Uri.parse(getIntent().getStringExtra("romicon")));
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
                        handleDiskFile(Uri.fromFile(new File((Objects.requireNonNull(getIntent().getStringExtra("rompath"))))), false);
                    }
                    if (!Objects.requireNonNull(getIntent().getStringExtra("addtodrive")).isEmpty()) {
                        setDrive(SELECT_DISK_0_FILE_MODE, VmFileManager.getPath(vmID, getIntent().getStringExtra("romfilename")));
                    } else {
                        setDrive(SELECT_DISK_0_FILE_MODE, null);
                    }
                }

            } else if (getIntent().hasExtra("importcvbinow")) {
                setDefault();
                try {
                    cvbiPicker.launch("*/*");
                } catch (Exception e) {
                    IntentUtils.showErrorDialog(this);
                }
            } else {
                setDefault();
                if (MainSettingsManager.autoCreateDisk(this)) {
                    if (createVMFolder(true)) {
                        Terminal vterm = new Terminal(this);
                        vterm.executeShellCommand2("qemu-img create -f qcow2 " + VmFileManager.getPath(vmID, "disk.qcow2") + " 128G", false, this);
                        binding.drive.setText(VmFileManager.getPath(vmID, "disk.qcow2"));
                    }
                } else {
                    setDrive(SELECT_DISK_0_FILE_MODE, null);
                }

            }
        }

        checkCreateCommandConfig();

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
        } else {
            new Thread(() -> {
                VmFileManager.unMarkAllPendingDelete(vmID);
                VmFileManager.removeAllPendingAddMarkFiles(vmID);
            }).start();
        }

        modify = false;
        new Thread(() -> VmFileManager.removeTemp(getApplicationContext(), vmID)).start();

        super.onDestroy();
    }

    private final ActivityResultLauncher<String> thumbnailPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                handleThumbnail(uri);
            });

    private final ActivityResultLauncher<String> diskPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                handleDiskFile(uri, true);
            });

    @SuppressLint("SetTextI18n")
    private final ActivityResultLauncher<String> isoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;

                String fileName = FileUtils.getFileNameFromUri(this, uri);
                if (fileName != null && !fileName.isEmpty() && !FormatManager.isOpticalFileFormat(fileName)) {
                    DialogUtils.twoDialog(this, getString(R.string.problem_has_been_detected), getString(R.string.file_format_is_not_supported_optical_drive), getResources().getString(R.string.continuetext), getResources().getString(R.string.cancel), true, R.drawable.album_24px, true,
                            () -> handleFile(uri), null, null);
                } else {
                    handleFile(uri);
                }
            });


    @SuppressLint("SetTextI18n")
    private final ActivityResultLauncher<String> floppyPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;

                String fileName = FileUtils.getFileNameFromUri(this, uri);
                if (fileName != null && !fileName.isEmpty() && !FormatManager.isFloppyFileFormat(fileName)) {
                    DialogUtils.twoDialog(this, getString(R.string.problem_has_been_detected), getString(R.string.file_format_is_not_supported_floppy_drive), getResources().getString(R.string.continuetext), getResources().getString(R.string.cancel), true, R.drawable.save_24px, true,
                            () -> handleFile(uri), null, null);
                } else {
                    handleFile(uri);
                }
            });


    private void handleFile(Uri uri) {
        if (MainSettingsManager.copyFile(this)) {
            showProgressDialog(getString(R.string.copying_file));

            executor.execute(() -> {
                try {
                    isProcessingFile = true;

                    String path = copyToTemp(uri);

                    runOnUiThread(() -> setDrive(this.PENDING_SELECT_FILE_MODE, path));
                } catch (Exception e) {
                    runOnUiThread(() -> DialogUtils.oneDialog(this,
                            getString(R.string.oops),
                            getString(R.string.unable_to_copy_file_content),
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
                } else {
                    runOnUiThread(() -> setDrive(this.PENDING_SELECT_FILE_MODE, new File(getPath(uri)).getAbsolutePath()));
                }
            });
        }
    }


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

                        String filePath = copyToTemp(uri);

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
            if (!current.vmID.isEmpty()) {
                vmID = current.vmID;
                checkVMID();
            }

            if (current.itemArch != null) {
                VMManager.setArch(current.itemArch, this);
                binding.collapsingToolbarLayout.setSubtitle(MainSettingsManager.getArch(this));
            }

            if (current.itemName != null) {
                binding.title.setText(current.itemName);
            }

            if (current.itemPath != null && !current.itemPath.isEmpty()) {
                setDrive(SELECT_DISK_0_FILE_MODE, (current.itemPath.contains("/") ? "" : VmFileManager.getPath(vmID)).concat(current.itemPath));
            }

            if (current.hd1 != null && !current.hd1.isEmpty()) {
                setDrive(SELECT_DISK_1_FILE_MODE, (current.hd1.contains("/") ? "" : VmFileManager.getPath(vmID)).concat(current.hd1));
            }

            if (MainSettingsManager.getArch(this).equals(MainSettingsManager.ARM64_ARCH)) {
                binding.lnFloppyContainer.setVisibility(View.GONE);
            } else {
                if (current.fda!= null && !current.fda.isEmpty()) {
                    setDrive(SELECT_FLOPPY_A_FILE_MODE, (current.fda.contains("/") ? "" : VmFileManager.getPath(vmID)).concat(current.fda));
                }

                if (current.fdb!= null && !current.fdb.isEmpty()) {
                    setDrive(SELECT_FLOPPY_B_FILE_MODE, (current.fdb.contains("/") ? "" : VmFileManager.getPath(vmID)).concat(current.fdb));
                }
            }

            if (current.imgCdrom != null && !current.imgCdrom.isEmpty()) {
                setDrive(SELECT_CDROM_0_FILE_MODE, (current.imgCdrom.contains("/") ? "" : VmFileManager.getPath(vmID)).concat(current.imgCdrom));
            }

            if (current.cdrom1 != null && !current.cdrom1.isEmpty()) {
                setDrive(SELECT_CDROM_1_FILE_MODE, (current.cdrom1.contains("/") ? "" : VmFileManager.getPath(vmID)).concat(current.cdrom1));
            }

            if (current.itemIcon != null && !current.itemIcon.isEmpty()) {
                thumbnailPath = (current.itemIcon.contains("/") ? current.itemIcon : VmFileManager.getPath(vmID, current.itemIcon));
                updateThumbnailViewer("");
            }

            if (current.itemExtra != null) {
                binding.qemu.setText(VmFileManager.textMarkToPath(this, vmID, current.itemExtra));
            }

            cpu = current.cpu;
            binding.sbvCpu.setSubtitle(Objects.requireNonNull(VMCreatorSelector.getCpu(this, MainSettingsManager.getArch(this), current.cpu).get("name")).toString());

            cores = current.cores;
            binding.sbvCore.setSubtitle(Objects.requireNonNull(VMCreatorSelector.getCpuCore(MainSettingsManager.getArch(this), cores).get("value")).toString());

            threads = current.threads;
            binding.sbvThread.setSubtitle(String.valueOf(threads + 1));

            isUseBattery = current.battery;
            binding.cbvBattery.setChecked(isUseBattery);

            bootFrom = current.bootFrom;
            binding.sbvBootfrom.setSubtitle(Objects.requireNonNull(VMCreatorSelector.getBootFrom(this, current.bootFrom).get("name")).toString());
            isShowBootMenu = current.isShowBootMenu;
            binding.cbvShowbootmenu.setChecked(isShowBootMenu);

            sharedFolder = current.sharedFolder;
            binding.svSharedFolder.setChecked(sharedFolder);

            if (MainSettingsManager.getArch(this).equals(MainSettingsManager.X86_64_ARCH)) {
                isUseUefi = current.isUseUefi;
                binding.cbvUseuefi.setChecked(isUseUefi);
            } else {
                binding.cbvUseuefi.setVisibility(View.GONE);
            }

            isUseDefaultBios = current.isUseDefaultBios;
            binding.cbvUseDefaultBios.setChecked(isUseDefaultBios);

            binding.cbvUseuefi.setEnabled(isUseDefaultBios);

            isUseLocalTime = current.isUseLocalTime;
            binding.cbvUselocaltime.setChecked(isUseLocalTime);
        }
    }

    private void setDefault() {
        String defQemuParams;
        if (DeviceUtils.is64bit()) {
            defQemuParams = switch (MainSettingsManager.getArch(this)) {
                case "ARM64" ->
                        "-M virt,virtualization=true -accel tcg,thread=multi -net nic,model=e1000 -net user -device nec-usb-xhci -device usb-kbd -device usb-mouse -device VGA";
                case "PPC" -> "-M mac99 -accel tcg,thread=multi";
                default ->
                        "-M pc -accel tcg,thread=multi -vga std -netdev user,id=usernet -device e1000,netdev=usernet  -usb -device usb-tablet";
            };
        } else {
            defQemuParams = switch (MainSettingsManager.getArch(this)) {
                case "ARM64" ->
                        "-M virt -net nic,model=e1000 -net user -device nec-usb-xhci -device usb-kbd -device usb-mouse -device VGA";
                case "PPC" -> "-M mac99 1";
                default ->
                        "-M pc -vga std -netdev user,id=usernet -device e1000,netdev=usernet -usb -device usb-tablet";
            };
        }
        binding.title.setText(getString(R.string.new_vm));
        binding.qemu.setText(defQemuParams);

        String currentArch = MainSettingsManager.getArch(this);

        if (currentArch.equals(MainSettingsManager.X86_64_ARCH)) {
            cores = Math.min(1, VMCreatorSelector.getCpuCorePosition(new CpuHelper().getCpuCores() - 1));
            binding.sbvCore.setSubtitle(Objects.requireNonNull(VMCreatorSelector.getCpuCore(currentArch, cores).get("value")).toString());
        } else if (currentArch.equals(MainSettingsManager.ARM64_ARCH)) {
            cores = Math.min(2, VMCreatorSelector.getCpuCorePosition(new CpuHelper().getCpuCores() - 1));
            binding.sbvCore.setSubtitle(Objects.requireNonNull(VMCreatorSelector.getCpuCore(currentArch, cores).get("value")).toString());
        } else {
            binding.sbvCore.setSubtitle("1");
        }

        binding.sbvThread.setSubtitle("1");
    }

    private void checkVMID() {
        if (vmID.isEmpty() || (!modify && VmFileManager.isInUse(vmID))) {
            vmID = VMManager.idGenerator();
            Log.d(TAG, "Changed to ID:" + vmID);
        }
    }

    private boolean createVMFolder(boolean isShowDialog) {
        File romDir = new File(VmFileManager.quickGetPath(vmID));
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

            if (isAllDriveEmpty()) {
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

        if (!VMManager.addVM(finalVmConfig(), modify ? getIntent().getIntExtra("POS", 0) : -1)) {
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

        showProgressDialog(getString(R.string.just_a_sec));
        new Thread(() -> {
            moveAllFromTemp();
            VmFileManager.unMarkAllPendingAdd(vmID);
            VmFileManager.removeAllPendingDeleteMarkFiles(vmID);

            runOnUiThread(() -> {
                DialogUtils.safeDismiss(this, progressDialog);
                if (!MainActivity.isActivate) {
                    startActivity(new Intent(this, SplashActivity.class));
                } else {
                    Intent intent = new Intent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    intent.setClass(this, MainActivity.class);
                    startActivity(intent);
                }

                finish();
            });
        }).start();
    }

    private DataMainRoms finalVmConfig() {
        current.vmID = vmID;
        current.itemArch = MainSettingsManager.getArch(this);

        current.itemName = Objects.requireNonNull(binding.title.getText()).toString();
        current.itemIcon = thumbnailPath;


        current.cpu = cpu;
        current.cores = cores;
        current.threads = threads;
        current.battery = isUseBattery;


        current.itemPath = Objects.requireNonNull(binding.drive.getText()).toString();
        current.hd1 = Objects.requireNonNull(binding.tieHd1.getText()).toString();

        current.fda = Objects.requireNonNull(binding.tieFda.getText()).toString();
        current.fdb = Objects.requireNonNull(binding.tieFdb.getText()).toString();

        current.imgCdrom = Objects.requireNonNull(binding.cdrom.getText()).toString();
        current.cdrom1 = Objects.requireNonNull(binding.tieCdrom1.getText()).toString();

        current.sharedFolder = sharedFolder;


        current.bootFrom = bootFrom;
        current.isShowBootMenu = isShowBootMenu;
        current.isUseLocalTime = isUseLocalTime;

        current.isUseUefi = isUseUefi;
        current.isUseDefaultBios = isUseDefaultBios;


        current.itemExtra = Objects.requireNonNull(binding.qemu.getText()).toString();


        current.qmpPort = 8080;

        return current;
    }

    private void handleThumbnail(Uri uri) {
        showProgressDialog(getString(R.string.just_a_sec));

        executor.execute(() -> {
            try {
                isProcessingFile = true;

                if (FileUtils.isFileExists(VmFileManager.getThumbnail(vmID)))
                    VmFileManager.markPendingDelete(VmFileManager.getThumbnail(vmID));

                String tempPath = VmFileManager.getTempPath(this, vmID, peddingTempFolder + VmFileManager.THUMBNAIL_FILE_NAME);

                ImageUtils.convertToPng(this, uri, tempPath);

                thumbnailPath = VmFileManager.getThumbnail(vmID);
                runOnUiThread(() -> updateThumbnailViewer(tempPath));
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

    private void updateThumbnailViewer(String path) {
        if (isFinishing() || isDestroyed()) return;

        if (!thumbnailPath.isEmpty() || !path.isEmpty()) {
            binding.ivAddThubnail.setImageResource(R.drawable.edit_24px);
            File imgFile = new File(path.isEmpty() ? thumbnailPath : path);

            if (imgFile.exists()) {
                Glide.with(this)
                        .load(imgFile)
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

    private void handleDiskFile(Uri _content_describer, boolean _addtodrive) {
        ProgressDialog progressDialog1 = new ProgressDialog(this);
        progressDialog1.show();
        new Thread(() -> {
            if (FileUtils.isValidFilePath(this, FileUtils.getPath(this, _content_describer), false)) {
                File selectedFilePath = new File(getPath(_content_describer));
                runOnUiThread(() -> {
                    progressDialog1.reset();

                    if (FormatManager.isHardDriveFileFormat(selectedFilePath.getName())) {
                        startHandleHardDriveFile(_content_describer, _addtodrive);
                    } else {
                        DialogUtils.twoDialog(this, getString(R.string.problem_has_been_detected), getString(R.string.file_format_is_not_supported_hard_drive), getResources().getString(R.string.continuetext), getResources().getString(R.string.cancel), true, R.drawable.hard_drive_24px, true,
                                () -> startHandleHardDriveFile(_content_describer, _addtodrive), null, null);
                    }
                });
            } else {
                runOnUiThread(() -> startHandleHardDriveFile(_content_describer, _addtodrive));
            }
        }).start();
    }

    @SuppressLint("SetTextI18n")
    private void startHandleHardDriveFile(Uri _content_describer, boolean _addtodrive) {
        if (MainSettingsManager.copyFile(this)) {

            if (isFinishing() || isDestroyed() || !createVMFolder(true)) return;
            showProgressDialog(getString(R.string.copying_file));

            executor.execute(() -> {
                try {
                    String _filename = FileUtils.getFileNameFromUri(this, _content_describer);
                    if (_filename == null || _filename.isEmpty()) {
                        _filename = String.valueOf(System.currentTimeMillis());
                    }

                    String path = copyToTemp(_content_describer);

                    String final_filename = _filename;
                    runOnUiThread(() -> {
                        if ((isFinishing() || isDestroyed())) {
                            if (!vmID.isEmpty())
                                FileUtils.delete(new File(VmFileManager.getPath(vmID, final_filename)));
                            return;
                        }
                        if (_addtodrive) {
                            setDrive(PENDING_SELECT_FILE_MODE, path);
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
            ProgressDialog progressDialog1 = new ProgressDialog(this);
            progressDialog1.show();
            new Thread(() -> {
                String path = getPath(_content_describer);
                runOnUiThread(() -> {
                    progressDialog1.reset();

                    if (!FileUtils.isValidFilePath(this, path, false)) {
                        DialogUtils.oneDialog(this,
                                getString(R.string.problem_has_been_detected),
                                getString(R.string.invalid_file_path_content),
                                getString(R.string.ok),
                                true,
                                R.drawable.warning_48px,
                                true,
                                null,
                                null);
                    } else {
                        setDrive(PENDING_SELECT_FILE_MODE, path);
                    }
                });
            }).start();
        }
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

        Log.i(TAG, "importRom: Extracting with " + (isUseUri ? "uri" : "path") + " from " + filePath + " to " + VmFileManager.quickGetPath(vmID));

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
                                binding.qemu.setText(VmFileManager.textMarkToPath(this, vmID, Objects.requireNonNull(getIntent().getStringExtra("romextra"))));
                            }
                        }

                        binding.title.setText(getIntent().getStringExtra("romname"));

                        if (getIntent().hasExtra("romicon") && !Objects.requireNonNull(getIntent().getStringExtra("romicon")).isEmpty()) {
                            handleThumbnail(Uri.parse(getIntent().getStringExtra("romicon")));
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
                            FileUtils.move(VmFileManager.getConfigFile(vmID), VmFileManager.getConfigFile(jObj.getString("vmID")));
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
            FileUtils.writeToFile(VmFileManager.getPath(vmID), VmFileManager.CREATE_COMMAND_CONFIG_FILE_NAME, VmFileManager.textMarkToPath(this, vmID, FileUtils.readAFile(VmFileManager.getCreateCommandConfigFile(vmID))));
            checkCreateCommandConfig();
        }

        if (VmFileManager.isSnapshotShExists(vmID)) {
            String snapshotParams = FileUtils.readAFile(VmFileManager.getSnapshotSh(vmID));
            if (snapshotParams.isEmpty()) return;
            FileUtils.writeToFile(VmFileManager.getPath(vmID), VmFileManager.SNAPSHOT_SH_FILE_NAME, VmFileManager.textMarkToPath(this, vmID, snapshotParams));
        }
    }

    private void showProgressDialog(String message) {
        dialogProgressStyleBinding.progressText.setText(message);
        dialogProgressStyleBinding.progressBar.setIndeterminate(true);
        progressDialog.show();
    }

    private String copyToTemp(Uri uri) throws IOException {
        String fileName = FileUtils.getFileNameFromUri(this, uri);

        if (fileName == null || fileName.isEmpty()) {
            fileName = String.valueOf(System.currentTimeMillis());
        }

        FileUtils.copyFileFromUri(this, uri, VmFileManager.getTempPath(this, vmID, peddingTempFolder + fileName));
        return VmFileManager.getPath(vmID, fileName);
    }

    public String getPath(Uri uri) {
        return FileUtils.getPath(this, uri);
    }

    String peddingTempFolder = "pedding/";

    private void onFileChange(String fileName) {
        markDelete(VmFileManager.getPath(vmID, fileName));
    }

    private void markDelete(String path) {
        VmFileManager.markPendingDelete(path);
        deleteTemp(new File(path).getName());
    }

    private void deleteTemp(String fileName) {
        VmFileManager.removeTemp(this, vmID, peddingTempFolder + fileName);
    }

    public void moveAllFromTemp() {
        ArrayList<String> fileList = new ArrayList<>();
        FileUtils.getAListOfAllFilesAndFoldersInADirectory(VmFileManager.getTempPath(this, vmID, peddingTempFolder), fileList);
        for (int position = 0; position < fileList.size(); position++) {
            FileUtils.moveToFolder(fileList.get(position), VmFileManager.getPath(vmID));
        }

        ArrayList<String> fileListInternal = new ArrayList<>();
        FileUtils.getAListOfAllFilesAndFoldersInADirectory(VmFileManager.getInternalTempPath(this, vmID, peddingTempFolder), fileListInternal);
        for (int position = 0; position < fileListInternal.size(); position++) {
            FileUtils.moveToFolder(fileListInternal.get(position), VmFileManager.getPath(vmID));
        }
    }

    // STORAGE LOGIC CORE

    private void setupStorageUi() {
        binding.drive.setOnClickListener(v -> pickStorageFile(SELECT_DISK_0_FILE_MODE));
        binding.driveField.setOnClickListener(v -> pickStorageFile(SELECT_DISK_0_FILE_MODE));
        binding.driveField.setEndIconOnClickListener(v -> {
            if (Objects.requireNonNull(binding.drive.getText()).toString().isEmpty()) {
                callQcow2CreatorDialog(SELECT_DISK_0_FILE_MODE);
            } else {
                storageFileOptionDialog(SELECT_DISK_0_FILE_MODE);
            }
        });



        binding.tieHd1.setOnClickListener(v -> pickStorageFile(SELECT_DISK_1_FILE_MODE));
        binding.tilHd1.setOnClickListener(v -> pickStorageFile(SELECT_DISK_1_FILE_MODE));
        binding.tilHd1.setEndIconOnClickListener(v -> {
            if (Objects.requireNonNull(binding.tieHd1.getText()).toString().isEmpty()) {
                callQcow2CreatorDialog(SELECT_DISK_1_FILE_MODE);
            } else {
                storageFileOptionDialog(SELECT_DISK_1_FILE_MODE);
            }
        });



        if (MainSettingsManager.getArch(this).equals(MainSettingsManager.ARM64_ARCH)) {
            binding.lnFloppyContainer.setVisibility(View.GONE);
        }

        binding.tieFda.setOnClickListener(v -> pickStorageFile(SELECT_FLOPPY_A_FILE_MODE));
        binding.tilFda.setOnClickListener(v -> pickStorageFile(SELECT_FLOPPY_A_FILE_MODE));
        binding.tilFda.setEndIconOnClickListener(v -> setDrive(SELECT_FLOPPY_A_FILE_MODE, null));

        binding.tieFdb.setOnClickListener(v -> pickStorageFile(SELECT_FLOPPY_B_FILE_MODE));
        binding.tilFdb.setOnClickListener(v -> pickStorageFile(SELECT_FLOPPY_B_FILE_MODE));
        binding.tilFdb.setEndIconOnClickListener(v -> setDrive(SELECT_FLOPPY_B_FILE_MODE, null));


        binding.cdrom.setOnClickListener(v -> pickStorageFile(SELECT_CDROM_0_FILE_MODE));
        binding.cdromField.setOnClickListener(v -> pickStorageFile(SELECT_CDROM_0_FILE_MODE));
        binding.cdromField.setEndIconOnClickListener(v -> setDrive(SELECT_CDROM_0_FILE_MODE, null));

        binding.tieCdrom1.setOnClickListener(v -> pickStorageFile(SELECT_CDROM_1_FILE_MODE));
        binding.tilCdrom1.setOnClickListener(v -> pickStorageFile(SELECT_CDROM_1_FILE_MODE));
        binding.tilCdrom1.setEndIconOnClickListener(v -> setDrive(SELECT_CDROM_1_FILE_MODE, null));



        binding.svSharedFolder.setSubTitle(AppConfig.sharedFolder);
        binding.svSharedFolder.setOnCheckedChangeListener((v, isChecked) -> sharedFolder = isChecked);
    }

    private final int SELECT_DISK_0_FILE_MODE = 0;
    private final int SELECT_DISK_1_FILE_MODE = 1;
    public final int SELECT_CDROM_0_FILE_MODE = 2;
    public final int SELECT_CDROM_1_FILE_MODE = 3;
    public final int SELECT_FLOPPY_A_FILE_MODE = 4;
    public final int SELECT_FLOPPY_B_FILE_MODE = 5;
    private int PENDING_SELECT_FILE_MODE = 0;
    private String PENDING_OLD_FILE_AFTER_SELECTED_NEW_FILE = "";

    private void pickStorageFile(int mode) {
        PENDING_SELECT_FILE_MODE = mode;
        PENDING_OLD_FILE_AFTER_SELECTED_NEW_FILE = Objects.requireNonNull(getPeddingStorageEditText().getText()).toString();

        try {
            if (PENDING_SELECT_FILE_MODE == SELECT_CDROM_0_FILE_MODE || PENDING_SELECT_FILE_MODE == SELECT_CDROM_1_FILE_MODE) {
                isoPicker.launch("*/*");
            } else if (PENDING_SELECT_FILE_MODE == SELECT_FLOPPY_A_FILE_MODE || PENDING_SELECT_FILE_MODE == SELECT_FLOPPY_B_FILE_MODE) {
                    floppyPicker.launch("*/*");
            } else {
                diskPicker.launch("*/*");
            }
        } catch (Exception e) {
            IntentUtils.showErrorDialog(this);
        }
    }

    private void storageFileOptionDialog(int mode) {
        PENDING_SELECT_FILE_MODE = mode;

        //TextInputLayout peddingTextInputLayout = getPeddingStorageInputLayout();
        TextInputEditText peddingTextInputEditText = getPeddingStorageEditText();

        DialogUtils.threeDialog(this,
                getString(R.string.change_hard_drive),
                getString(R.string.do_you_want_to_change_create_or_remove),
                getString(R.string.change), getString(R.string.remove),
                getString(R.string.create),
                true,
                R.drawable.hard_drive_24px,
                true,
                () -> pickStorageFile(mode),
                () -> {
                    String path = new File(Objects.requireNonNull(peddingTextInputEditText.getText()).toString()).getAbsolutePath();
                    if (path.startsWith(VmFileManager.quickGetPath(vmID)))  {
                        ProgressDialog progressDialog1 = new ProgressDialog(this);
                        progressDialog1.show();
                        new Thread(() -> {
                            markDelete(path);
                            runOnUiThread(progressDialog1::reset);
                        }).start();
                    }

                    setDrive(mode, "");
                },
                () -> {
                    if (createVMFolder(true)) {
                        callQcow2CreatorDialog(PENDING_SELECT_FILE_MODE);
                    }
                }, null);
    }

    private void callQcow2CreatorDialog(int mode) {
        PENDING_SELECT_FILE_MODE = mode;

        TextInputLayout peddingTextInputLayout = getPeddingStorageInputLayout();
        TextInputEditText peddingTextInputEditText = getPeddingStorageEditText();

        CreateImageDialogFragment dialogFragment = new CreateImageDialogFragment();
        dialogFragment.folder = VmFileManager.getPath(vmID);
        dialogFragment.customRom = true;
        dialogFragment.filename = Objects.requireNonNull(binding.title.getText()).toString();
        dialogFragment.drive = peddingTextInputEditText;
        dialogFragment.driveLayout = peddingTextInputLayout;
        dialogFragment.isMarkPendingAdd = true;
        dialogFragment.show(getSupportFragmentManager(), "CreateImageDialogFragment");
    }

    private void setRemovableDriveEndIconOnClickListener(int mode, TextInputLayout textInputLayout) {
        textInputLayout.setEndIconOnClickListener(v -> {
            PENDING_SELECT_FILE_MODE = mode;
            PENDING_OLD_FILE_AFTER_SELECTED_NEW_FILE = Objects.requireNonNull(getPeddingStorageEditText().getText()).toString();
            setDrive(mode, null);
        });
    }

    private void setDrive(Integer mode, String path) {
        if (isFileReadyinUse(path)) {
            DialogUtils.oopsDialog(this, getString(R.string.vm_creator_this_file_is_already_in_use_content));
            deleteTemp(new File(path).getName());
            return;
        }

        if (mode != null) PENDING_SELECT_FILE_MODE = mode;

        TextInputLayout peddingTextInputLayout = getPeddingStorageInputLayout();
        TextInputEditText peddingTextInputEditText = getPeddingStorageEditText();

        peddingTextInputEditText.setText(path != null ? path : "");

        if (path == null || path.isEmpty()) {
            if (PENDING_SELECT_FILE_MODE == SELECT_CDROM_0_FILE_MODE || PENDING_SELECT_FILE_MODE == SELECT_CDROM_1_FILE_MODE || PENDING_SELECT_FILE_MODE == SELECT_FLOPPY_A_FILE_MODE || PENDING_SELECT_FILE_MODE == SELECT_FLOPPY_B_FILE_MODE) {
                peddingTextInputLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            } else {
                peddingTextInputLayout.setEndIconDrawable(R.drawable.add_24px);
            }
        } else {
            if (PENDING_SELECT_FILE_MODE == SELECT_CDROM_0_FILE_MODE || PENDING_SELECT_FILE_MODE == SELECT_CDROM_1_FILE_MODE || PENDING_SELECT_FILE_MODE == SELECT_FLOPPY_A_FILE_MODE || PENDING_SELECT_FILE_MODE == SELECT_FLOPPY_B_FILE_MODE) {
                peddingTextInputLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
                peddingTextInputLayout.setEndIconDrawable(R.drawable.close_24px);
                setRemovableDriveEndIconOnClickListener(PENDING_SELECT_FILE_MODE, peddingTextInputLayout);
            } else {
                peddingTextInputLayout.setEndIconDrawable(R.drawable.more_vert_24px);
            }
        }

        if (!PENDING_OLD_FILE_AFTER_SELECTED_NEW_FILE.isEmpty()) {
            onFileChange(new File(PENDING_OLD_FILE_AFTER_SELECTED_NEW_FILE).getName());
            PENDING_OLD_FILE_AFTER_SELECTED_NEW_FILE = "";
        }
    }

    private TextInputLayout getPeddingStorageInputLayout() {
        if (PENDING_SELECT_FILE_MODE == SELECT_DISK_1_FILE_MODE) {
            return binding.tilHd1;
        } else if (PENDING_SELECT_FILE_MODE == SELECT_CDROM_0_FILE_MODE) {
            return binding.cdromField;
        } else if (PENDING_SELECT_FILE_MODE == SELECT_CDROM_1_FILE_MODE) {
            return binding.tilCdrom1;
        } else if (PENDING_SELECT_FILE_MODE == SELECT_FLOPPY_A_FILE_MODE) {
            return binding.tilFda;
        } else if (PENDING_SELECT_FILE_MODE == SELECT_FLOPPY_B_FILE_MODE) {
            return binding.tilFdb;
        } else {
            return binding.driveField;
        }
    }

    private TextInputEditText getPeddingStorageEditText() {
        if (PENDING_SELECT_FILE_MODE == SELECT_DISK_1_FILE_MODE) {
            return binding.tieHd1;
        } else if (PENDING_SELECT_FILE_MODE == SELECT_CDROM_0_FILE_MODE) {
            return binding.cdrom;
        } else if (PENDING_SELECT_FILE_MODE == SELECT_CDROM_1_FILE_MODE) {
            return binding.tieCdrom1;
        } else if (PENDING_SELECT_FILE_MODE == SELECT_FLOPPY_A_FILE_MODE) {
            return binding.tieFda;
        } else if (PENDING_SELECT_FILE_MODE == SELECT_FLOPPY_B_FILE_MODE) {
            return binding.tieFdb;
        } else {
            return binding.drive;
        }
    }

    private boolean isAllDriveEmpty() {
        return (Objects.requireNonNull(binding.drive.getText()).toString().isEmpty()) &&
                (Objects.requireNonNull(binding.tieHd1.getText()).toString().isEmpty()) &&
                (Objects.requireNonNull(binding.cdrom.getText()).toString().isEmpty()) &&
                (Objects.requireNonNull(binding.tieCdrom1.getText()).toString().isEmpty()) &&
                (Objects.requireNonNull(binding.tieFda.getText()).toString().isEmpty()) &&
                (Objects.requireNonNull(binding.tieFdb.getText()).toString().isEmpty());
    }

    private boolean isFileReadyinUse(String path) {
        if (path == null || path.trim().isEmpty()) return false;

        String paramCollection = Objects.requireNonNull(binding.drive.getText()).toString();
        paramCollection += "\n" + Objects.requireNonNull(binding.tieHd1.getText());
        paramCollection += "\n" + Objects.requireNonNull(binding.cdrom.getText());
        paramCollection += "\n" + Objects.requireNonNull(binding.tieCdrom1.getText());
        paramCollection += "\n" + Objects.requireNonNull(binding.tieFda.getText());
        paramCollection += "\n" + Objects.requireNonNull(binding.tieFdb.getText());
        paramCollection += "\n" + Objects.requireNonNull(binding.qemu.getText());

        return paramCollection.contains(path);
    }

    private void checkCreateCommandConfig() {
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
    }
}
