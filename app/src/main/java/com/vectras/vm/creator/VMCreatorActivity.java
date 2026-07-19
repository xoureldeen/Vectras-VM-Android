package com.vectras.vm.creator;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.AppConfig;
import com.vectras.vm.R;
import com.vectras.vm.creator.editor.AdvancedConfigsDialog;
import com.vectras.vm.creator.editor.BoardConfigsDialog;
import com.vectras.vm.creator.editor.NetworkConfigsDialog;
import com.vectras.vm.creator.utils.CreatorUtils;
import com.vectras.vm.creator.editor.FirmwareConfigsDialog;
import com.vectras.vm.creator.editor.StorageConfigsDialog;
import com.vectras.vm.creator.utils.VMCreatorSelector;
import com.vectras.vm.file.FilePickerDialog;
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
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.vectras.vm.utils.ZipUtils;
import com.vectras.vm.utils.PermissionUtils;
import com.vectras.vterm.Terminal2;

public class VMCreatorActivity extends AppCompatActivity {

    private final String TAG = "VMCreatorActivity";
    private ActivityVmCreatorBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private DialogProgressStyleBinding dialogProgressStyleBinding;
    private AlertDialog progressDialog;
    public String previousName = "";
    public boolean addromnowdone = false;
    private boolean created = false;
    boolean modify;
    private boolean isProcessingFile = false;
    public static DataMainRoms current = new DataMainRoms();
    private String thumbnailPath = "";
    private String vmID = VMManager.idGenerator();

    private CreatorUtils utils;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.add_file) {
            try {
                if (MainSettingsManager.getBuiltInFilePicker(this)) {
                    FilePickerDialog filePickerDialog = new FilePickerDialog();
                    filePickerDialog.pick(this, FilePickerDialog.TYPE_FILE, (path -> handleAddFile(Uri.fromFile(new File (path)))));
                } else {
                    filePicker.launch("*/*");
                }
            } catch (Exception e) {
                IntentUtils.showErrorDialog(this);
            }
            return true;
        } else if (id == R.id.show_in_folder) {
            if (MainSettingsManager.getBuiltInFilePicker(this)) {
                FilePickerDialog filePickerDialog = new FilePickerDialog();
                filePickerDialog.setHomeName(current.itemName);
                filePickerDialog.setLockHome(true);
                filePickerDialog.browse(this, VmFileManager.getPath(vmID));
            } else {
                FileUtils.openFolder(this, VmFileManager.getPath(vmID));
            }
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

                            if (FileUtils.isFileExists(VmFileManager.getThumbnail(vmID))) utils.markDelete(VmFileManager.getThumbnail(vmID));
                        }, null);
            }
        });

        binding.lineardisclaimer.setOnClickListener(v -> DialogUtils.oneDialog(this, getResources().getString(R.string.dont_miss_out), getResources().getString(R.string.disclaimer_when_using_rom), getResources().getString(R.string.i_agree), true, R.drawable.verified_user_24px, true, null, null));

        binding.lnBoard.setOnClickListener(v -> {
            save();

            BoardConfigsDialog dialog = new BoardConfigsDialog();
            dialog.setConfigs(current);
            dialog.setOnDismiss(this::loadConfig);
            dialog.show(getSupportFragmentManager(), "board_configs_dialog");
        });

        binding.lnStorage.setOnClickListener(v -> {
            save();

            StorageConfigsDialog dialog = new StorageConfigsDialog();
            dialog.setConfigs(current);
            dialog.setOnDismiss(this::loadConfig);
            dialog.show(getSupportFragmentManager(), "storage_configs_dialog");
        });

        binding.lnFirmware.setOnClickListener(v -> {
            save();

            FirmwareConfigsDialog dialog = new FirmwareConfigsDialog();
            dialog.setConfigs(current);
            dialog.setOnDismiss(this::loadConfig);
            dialog.show(getSupportFragmentManager(), "firmware_configs_dialog");
        });

        binding.lnNetwork.setOnClickListener(v -> {
            save();

            NetworkConfigsDialog dialog = new NetworkConfigsDialog();
            dialog.setConfigs(current);
            dialog.setOnDismiss(this::loadConfig);
            dialog.show(getSupportFragmentManager(), "network_configs_dialog");
        });

        binding.lnAdvanced.setOnClickListener(v -> {
            save();

            AdvancedConfigsDialog dialog = new AdvancedConfigsDialog();
            dialog.setConfigs(current);
            dialog.setOnDismiss(this::loadConfig);
            dialog.show(getSupportFragmentManager(), "advanced_configs_dialog");
        });

        modify = getIntent().getBooleanExtra("MODIFY", false);
        if (modify) {
            binding.collapsingToolbarLayout.setTitle(getString(R.string.edit));
            created = true;

            loadConfig(VMManager.getVMConfig(getIntent().getIntExtra("POS", 0)));

//            vmID = getIntent().getStringExtra("VMID");
//
//            if (vmID == null || vmID.isEmpty()) {
//                vmID = VMManager.idGenerator();
//            }

            previousName = current.itemName;
        } else {
            checkVMID();

            utils = new CreatorUtils(this, vmID);

            if (getIntent().hasExtra("addromnow")) {
                if (Objects.requireNonNull(getIntent().getStringExtra("romextra")).isEmpty()) {
                    setDefault();
                } else {
                    current.itemExtra = VmFileManager.textMarkToPath(this, vmID, Objects.requireNonNull(getIntent().getStringExtra("romextra")));
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
                        current.itemPath =  VmFileManager.getPath(vmID, getIntent().getStringExtra("romfilename"));
                    } else {
                        current.itemPath = "";
                    }
                }

            } else if (getIntent().hasExtra("importcvbinow")) {
                setDefault();
                try {
                    if (MainSettingsManager.getBuiltInFilePicker(this)) {
                        FilePickerDialog filePickerDialog = new FilePickerDialog();
                        filePickerDialog.pick(this, FilePickerDialog.ROM_FILE, (path -> importRom(null, path, new File(path).getName())));
                    } else {
                        cvbiPicker.launch("*/*");
                    }
                } catch (Exception e) {
                    IntentUtils.showErrorDialog(this);
                }
            } else {
                setDefault();
                if (MainSettingsManager.autoCreateDisk(this)) {
                    if (createVMFolder(true)) {
                        Terminal2 terminal2 = new Terminal2(this);
                        terminal2.setShowProgressDialog(true);
                        terminal2.execute("qemu-img create -f qcow2 " + VmFileManager.getPath(vmID, "disk.qcow2") + " 128G", new Terminal2.Terminal2Callback() {
                            @Override
                            public void onRunning(String command, String newLine) {
                                // Nothing to do.
                            }

                            @Override
                            public void onFinished(String command, String log, int status) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    if (status == terminal2.SUCCESS)
                                        current.itemPath = VmFileManager.getPath(vmID, "disk.qcow2");
                                });
                            }

                            @Override
                            public void onError(String command, Exception exception) {
                                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), getString(R.string.an_error_occurred_while_creating_the_virtual_drive), Toast.LENGTH_SHORT).show());

                            }
                        });
                    }
                } else {
                    current.itemPath = "";
                }

            }
        }

        utils.removeTemp();

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
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::handleAddFile);

    public void handleAddFile(Uri uri) {
        if (uri == null) return;
        showProgressDialog(getString(R.string.copying_file));

        executor.execute(() -> {
            try {
                isProcessingFile = true;

                String filePath = utils.copyToTemp(uri);

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
    }

    private void loadConfig(DataMainRoms vmConfig) {
        if (vmConfig != null) current = vmConfig;
        if (binding != null && current != null) {
            if (!current.vmID.isEmpty()) {
                vmID = current.vmID;
                checkVMID();
            }

            utils = new CreatorUtils(this, vmID);

            if (current.itemArch != null) {
                VMManager.setArch(current.itemArch, this);
                binding.collapsingToolbarLayout.setSubtitle(MainSettingsManager.getArch(this));
            }

            if (current.itemName != null) {
                binding.title.setText(current.itemName);
            }

            if (current.itemIcon != null && !current.itemIcon.isEmpty()) {
                thumbnailPath = (current.itemIcon.contains("/") ? current.itemIcon : VmFileManager.getPath(vmID, current.itemIcon));
                updateThumbnailViewer("");
            }

            if (current.itemPath != null && !current.itemPath.isEmpty())
                current.itemPath = (current.itemPath.contains("/") ? current.itemPath : VmFileManager.getPath(vmID, current.itemPath));

            if (current.hd1 != null && !current.hd1.isEmpty())
                current.hd1 = (current.hd1.contains("/") ? current.hd1 : VmFileManager.getPath(vmID, current.hd1));


            if (current.imgCdrom != null && !current.imgCdrom.isEmpty())
                current.imgCdrom = (current.imgCdrom.contains("/") ? current.imgCdrom : VmFileManager.getPath(vmID, current.imgCdrom));

            if (current.cdrom1 != null && !current.cdrom1.isEmpty())
                current.cdrom1 = (current.cdrom1.contains("/") ? current.cdrom1 : VmFileManager.getPath(vmID, current.cdrom1));


            if (current.fda != null && !current.fda.isEmpty())
                current.fda = (current.fda.contains("/") ? current.fda : VmFileManager.getPath(vmID, current.fda));

            if (current.fdb != null && !current.fdb.isEmpty())
                current.fdb = (current.fdb.contains("/") ? current.fdb : VmFileManager.getPath(vmID, current.fdb));
        }
    }

    private void setDefault() {
        String defQemuParams;
        if (DeviceUtils.is64bit()) {
            defQemuParams = switch (MainSettingsManager.getArch(this)) {
                case "ARM64" ->
                        "-accel tcg,thread=multi -device nec-usb-xhci -device usb-kbd -device usb-mouse -device VGA";
                case "PPC" -> "-M mac99 -accel tcg,thread=multi";
                default ->
                        "-accel tcg,thread=multi -vga std -usb -device usb-tablet";
            };
        } else {
            defQemuParams = switch (MainSettingsManager.getArch(this)) {
                case "ARM64" ->
                        "-device nec-usb-xhci -device usb-kbd -device usb-mouse -device VGA";
                case "PPC" -> "-M mac99";
                default ->
                        "-vga std -usb -device usb-tablet";
            };
        }
        binding.title.setText(getString(R.string.new_vm));
        current.itemExtra = defQemuParams;

        String currentArch = MainSettingsManager.getArch(this);

        if (currentArch.equals(MainSettingsManager.X86_64_ARCH)) {
            current.cores = Math.min(1, VMCreatorSelector.getCpuCorePosition(new CpuHelper().getCpuCores() - 1));
        } else if (currentArch.equals(MainSettingsManager.ARM64_ARCH)) {
            current.cores = Math.min(2, VMCreatorSelector.getCpuCorePosition(new CpuHelper().getCpuCores() - 1));
            current.nvirt = true;
        }

        current.networkCard = 3; // Intel E1000 (82540EM)
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
        save();

        if (current.itemName.isEmpty()) {
            DialogUtils.oneDialog(this, getString(R.string.oops), getString(R.string.need_set_name), getString(R.string.ok), true, R.drawable.error_96px, true, null, null);
        } else {
            String _contentDialog = "";
            if (current.itemExtra.isEmpty()) {
                _contentDialog = getResources().getString(R.string.qemu_params_is_empty);
            }

            if (isAllDriveEmpty() && !VMManager.isHaveADisk(current.itemExtra)) {
                if (!_contentDialog.isEmpty()) {
                    _contentDialog += "\n\n";
                }
                _contentDialog += getResources().getString(R.string.you_have_not_added_any_storage_devices);
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

        if (!VMManager.addVM(current, modify ? getIntent().getIntExtra("POS", 0) : -1)) {
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
            utils.moveAllFromTemp();
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

    private void save() {
        current.vmID = vmID;
        current.itemArch = MainSettingsManager.getArch(this);

        current.itemName = Objects.requireNonNull(binding.title.getText()).toString();
        current.itemIcon = thumbnailPath;


        current.qmpPort = 8080;
    }

    private void handleThumbnail(Uri uri) {
        showProgressDialog(getString(R.string.just_a_sec));

        executor.execute(() -> {
            try {
                isProcessingFile = true;

                if (FileUtils.isFileExists(VmFileManager.getThumbnail(vmID)))
                    VmFileManager.markPendingDelete(VmFileManager.getThumbnail(vmID));

                String tempPath = utils.getTempPath(VmFileManager.THUMBNAIL_FILE_NAME);

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

                    String path = utils.copyToTemp(_content_describer);

                    String final_filename = _filename;
                    runOnUiThread(() -> {
                        if ((isFinishing() || isDestroyed())) {
                            if (!vmID.isEmpty())
                                FileUtils.delete(new File(VmFileManager.getPath(vmID, final_filename)));
                            return;
                        }
                        if (_addtodrive) {
                            current.itemPath = path;
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
                        current.itemPath = path;
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
                            current.itemPath = _getDiskFile;
                        } else {
                            if (Objects.requireNonNull(getIntent().getStringExtra("romextra")).contains(Objects.requireNonNull(getIntent().getStringExtra("finalromfilename")))) {
                                current.itemExtra = Objects.requireNonNull(getIntent().getStringExtra("romextra")).replaceAll(Objects.requireNonNull(getIntent().getStringExtra("finalromfilename")), "\"" + _getDiskFile + "\"");
                            } else {
                                current.itemPath = _getDiskFile;
                                current.itemExtra = VmFileManager.textMarkToPath(this, vmID, Objects.requireNonNull(getIntent().getStringExtra("romextra")));
                            }
                        }

                        binding.title.setText(getIntent().getStringExtra("romname"));

                        if (getIntent().hasExtra("romicon") && !Objects.requireNonNull(getIntent().getStringExtra("romicon")).isEmpty()) {
                            handleThumbnail(Uri.parse(getIntent().getStringExtra("romicon")));
                        }
                    } else {
                        if (current.itemExtra.isEmpty()) setDefault();
                        if (Objects.requireNonNull(binding.title.getText()).toString().isEmpty() || binding.title.getText().toString().equals("New VM")) {
                            binding.title.setText(_filename.replace(".cvbi", ""));
                        }
                        current.itemPath = _getDiskFile;
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
                    DataMainRoms newConfigs = new Gson().fromJson(FileUtils.readFromFile(this, new File(VmFileManager.getConfigFile(vmID))), DataMainRoms.class);
                    newConfigs.itemExtra = VmFileManager.textMarkToPath(this, vmID, newConfigs.itemExtra);
                    loadConfig(newConfigs);
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
        if (!isFinishing() && !isDestroyed()) progressDialog.show();
    }

    public String getPath(Uri uri) {
        return FileUtils.getPath(this, uri);
    }

    private boolean isAllDriveEmpty() {
        return (current.itemPath.isEmpty() &&
                current.hd1.isEmpty() &&
                current.imgCdrom.isEmpty() &&
                current.cdrom1.isEmpty() &&
                current.fda.isEmpty() &&
                current.fdb.isEmpty());
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
