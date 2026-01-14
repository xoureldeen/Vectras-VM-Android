package com.vectras.vm.creator;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.vectras.vm.utils.ClipboardUltils;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.ImageUtils;
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
    boolean iseditparams = false;
    public String previousName = "";
    public boolean addromnowdone = false;
    private boolean created = false;
    boolean modify;
    private boolean isImportingCVBI = false;
    public static DataMainRoms current;
    private String thumbnailPath = "";
    private String vmID = VMManager.idGenerator();
    private boolean isShowBootMenu = false;
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
            FileUtils.createDirectory(AppConfig.vmFolder + vmID);
            FileUtils.openFolder(this, AppConfig.vmFolder + vmID);
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

        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        binding.collapsingToolbarLayout.setSubtitle(MainSettingsManager.getArch(this));

        binding.btnCreate.setOnClickListener(v -> startCreateVM());

        binding.drive.setOnClickListener(v -> diskPicker.launch("*/*"));
        binding.driveField.setOnClickListener(v -> diskPicker.launch("*/*"));

        binding.driveField.setEndIconOnClickListener(v -> {
            if (Objects.requireNonNull(binding.drive.getText()).toString().isEmpty()) {
                CreateImageDialogFragment dialogFragment = new CreateImageDialogFragment();
                dialogFragment.folder = AppConfig.vmFolder + vmID + "/";
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

        binding.lnShowbootmenu.setOnClickListener(v -> binding.cbShowbootmenu.toggle());
        binding.cbShowbootmenu.setOnCheckedChangeListener((button, isChecked) -> isShowBootMenu = isChecked);

        binding.lnBootfrom.setOnClickListener(v -> VMCreatorSelector.bootFrom(this, bootFrom, ((position, name, value) -> {
            bootFrom = position;
            binding.tvBootfrom.setText(name);
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
                    binding.qemu.setText(Objects.requireNonNull(getIntent().getStringExtra("romextra")).replaceAll("OhnoIjustrealizeditsmidnightandIstillhavetodothis", AppConfig.vmFolder + vmID + "/"));
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
                        binding.drive.setText(AppConfig.vmFolder + vmID + "/" + getIntent().getStringExtra("romfilename"));
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
                        vterm.executeShellCommand2("qemu-img create -f qcow2 " + AppConfig.vmFolder + vmID + "/disk.qcow2 128G", false, this);
                        binding.drive.setText(AppConfig.vmFolder + vmID + "/disk.qcow2");
                    }
                } else {
                    binding.driveField.setEndIconDrawable(R.drawable.add_24px);
                }

            }
        }

        if (PackageUtils.getVersionCode("com.anbui.cqcm.app", this) < 735 || !FileUtils.isFileExists(AppConfig.vmFolder + vmID + "/cqcm.json")) {
            binding.opencqcm.setVisibility(View.GONE);
        } else {
            binding.opencqcm.setOnClickListener(v -> {
                if (PackageUtils.isInstalled("com.anbui.cqcm.app", this)) {
                    Intent intentcqcm = new Intent();
                    intentcqcm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intentcqcm.setComponent(new ComponentName("com.anbui.cqcm.app", "com.anbui.cqcm.app.DownloadActivity"));
                    intentcqcm.putExtra("content", FileUtils.readAFile(AppConfig.vmFolder + vmID + "/cqcm.json"));
                    intentcqcm.putExtra("vectrasVMId", vmID);
                    startActivity(intentcqcm);
                    finish();
                } else {
                    Intent intenturl = new Intent();
                    intenturl.setAction(Intent.ACTION_VIEW);
                    intenturl.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.anbui.cqcm.app"));
                    startActivity(intenturl);
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
        if (isImportingCVBI) return;

        if (!created && !modify) {
            new Thread(() -> FileUtils.deleteDirectory(AppConfig.vmFolder + vmID)).start();
        }
        modify = false;
        finish();
    }

    public void onDestroy() {
        if (!created && !modify) {
            new Thread(() -> FileUtils.deleteDirectory(AppConfig.vmFolder + vmID)).start();
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
                    DialogProgressStyleBinding dialogProgressStyleBinding = DialogProgressStyleBinding.inflate(getLayoutInflater());
                    dialogProgressStyleBinding.progressText.setText(getString(R.string.copying_file));
                    AlertDialog progressDialog = new MaterialAlertDialogBuilder(this, R.style.CenteredDialogTheme)
                            .setView(dialogProgressStyleBinding.getRoot())
                            .setCancelable(false)
                            .create();

                    progressDialog.show();

                    executor.execute(() -> {
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
                            runOnUiThread(() -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    progressDialog.dismiss();
                                }
                            });
                        }
                    });
                } else {
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
                if (uri == null)
                    return;

                executor.execute(() -> {
                    String filePath;
                    try {
                        File selectedFilePath = new File(getPath(uri));
                        filePath = selectedFilePath.getPath();
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

                DialogProgressStyleBinding dialogProgressStyleBinding = DialogProgressStyleBinding.inflate(getLayoutInflater());
                dialogProgressStyleBinding.progressText.setText(getString(R.string.copying_file));
                AlertDialog progressDialog = new MaterialAlertDialogBuilder(this, R.style.CenteredDialogTheme)
                        .setView(dialogProgressStyleBinding.getRoot())
                        .setCancelable(false)
                        .create();

                progressDialog.show();

                executor.execute(() -> {
                    try {
                        String _filename = FileUtils.getFileNameFromUri(this, uri);
                        if (_filename == null || _filename.isEmpty()) {
                            _filename = String.valueOf(System.currentTimeMillis());
                        }

                        String filePath = AppConfig.vmFolder + vmID + "/" + _filename;

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
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                progressDialog.dismiss();
                            }
                        });
                    }
                });
            });

    private void loadConfig(DataMainRoms vmConfig) {
        if (vmConfig != null) current = vmConfig;
        if (binding != null && current != null) {
            if (current.itemArch != null) {
                MainSettingsManager.setArch(this, current.itemArch);
                binding.collapsingToolbarLayout.setSubtitle(MainSettingsManager.getArch(this));
            }

            if (current.itemName != null) {
                binding.title.setText(current.itemName);
            }

            if (current.itemPath != null && !current.itemPath.isEmpty()) {
                binding.drive.setText((current.itemPath.contains("/") ? "" : AppConfig.vmFolder + vmID + "/").concat(current.itemPath));
            }

            if (Objects.requireNonNull(binding.drive.getText()).toString().isEmpty()) {
                binding.driveField.setEndIconDrawable(R.drawable.add_24px);
            } else {
                binding.driveField.setEndIconDrawable(R.drawable.more_vert_24px);
            }

            if (current.imgCdrom != null && !current.imgCdrom.isEmpty()) {
                binding.cdrom.setText((current.imgCdrom.contains("/") ? "" : AppConfig.vmFolder + vmID + "/").concat(current.imgCdrom));
            }

            if (!Objects.requireNonNull(binding.cdrom.getText()).toString().isEmpty()) {
                binding.cdromField.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
                binding.cdromField.setEndIconDrawable(R.drawable.close_24px);
                changeOnClickCdrom();
            }

            if (current.itemIcon != null && !current.itemIcon.isEmpty()) {
                thumbnailPath = (current.itemIcon.contains("/") ? "" : AppConfig.vmFolder + vmID + "/") + current.itemIcon;
                thumbnailProcessing();
            }

            if (current.itemExtra != null) {
                binding.qemu.setText(current.itemExtra.replaceAll("OhnoIjustrealizeditsmidnightandIstillhavetodothis", AppConfig.vmFolder + vmID + "/"));
            }

            bootFrom = current.bootFrom;
            binding.tvBootfrom.setText(Objects.requireNonNull(VMCreatorSelector.getBootFrom(this, current.bootFrom).get("name")).toString());
            isShowBootMenu = current.isShowBootMenu;
            binding.cbShowbootmenu.setChecked(isShowBootMenu);
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
        File romDir = new File(AppConfig.vmFolder + vmID);
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
        vmConfigMap.put("vmID", vmID);
        vmConfigMap.put("qmpPort", 8080);
        return vmConfigMap;
    }

    private void startProcessingThumbnail(Uri uri) {
        View progressView = LayoutInflater.from(this).inflate(R.layout.dialog_progress_style, null);
        TextView progressText = progressView.findViewById(R.id.progress_text);
        progressText.setText(getString(R.string.just_a_sec));
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this, R.style.CenteredDialogTheme)
                .setView(progressView)
                .setCancelable(false)
                .create();

        progressDialog.show();

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
                    if (!isImportingCVBI && progressDialog.isShowing() && !isFinishing() && !isDestroyed())
                        progressDialog.dismiss();
                });
            }
        });
    }

    private void thumbnailProcessing() {
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
        if (FileUtils.isValidFilePath(this, FileUtils.getPath(this, _content_describer), false)) {
            new Thread(() -> {
                File selectedFilePath = new File(getPath(_content_describer));
                runOnUiThread(() -> {
                    if (VMManager.isADiskFile(selectedFilePath.getPath())) {
                        startProcessingHardDriveFile(_content_describer, _addtodrive);
                    } else {
                        DialogUtils.twoDialog(this, getString(R.string.problem_has_been_detected), getString(R.string.file_format_is_not_supported), getResources().getString(R.string.continuetext), getResources().getString(R.string.cancel), true, R.drawable.hard_drive_24px, true,
                                () -> startProcessingHardDriveFile(_content_describer, _addtodrive), null, null);
                    }
                });
            }).start();
        } else {
            startProcessingHardDriveFile(_content_describer, _addtodrive);
        }
    }

    @SuppressLint("SetTextI18n")
    private void startProcessingHardDriveFile(Uri _content_describer, boolean _addtodrive) {
        if (MainSettingsManager.copyFile(this)) {

            if (isFinishing() || isDestroyed() || !createVMFolder(true)) return;

            View progressView = LayoutInflater.from(this).inflate(R.layout.dialog_progress_style, null);
            TextView progressText = progressView.findViewById(R.id.progress_text);
            progressText.setText(getString(R.string.copying_file));
            AlertDialog progressDialog = new MaterialAlertDialogBuilder(this, R.style.CenteredDialogTheme)
                    .setView(progressView)
                    .setCancelable(false)
                    .create();

            progressDialog.show();

            executor.execute(() -> {
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
                    runOnUiThread(progressDialog::dismiss);
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

        isImportingCVBI = true;

        View progressView = LayoutInflater.from(this).inflate(R.layout.dialog_progress_style, null);
        TextView progressText = progressView.findViewById(R.id.progress_text);
        progressText.setText(getString(R.string.importing) + "\n" + getString(R.string.please_stay_here));
        ProgressBar progressBar = progressView.findViewById(R.id.progress_bar);
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this, R.style.CenteredDialogTheme)
                .setView(progressView)
                .setCancelable(false)
                .create();

        progressDialog.show();

        Log.i(TAG, "importRom: Extracting from " + filePath + " to " + AppConfig.vmFolder + vmID);

        new Thread(() -> {
            boolean result = isUseUri ? ZipUtils.extract(
                    this,
                    fileUri,
                    AppConfig.vmFolder + vmID,
                    progressText,
                    progressBar
            ) : ZipUtils.extract(
                    this,
                    filePath,
                    AppConfig.vmFolder + vmID,
                    progressText,
                    progressBar
            );

            runOnUiThread(() -> {
                if (progressDialog.isShowing() && !isFinishing() && !isDestroyed())
                    progressDialog.dismiss();

                if (isFinishing() || isDestroyed()) {
                    new Thread(() -> FileUtils.deleteDirectory(AppConfig.vmFolder + vmID)).start();
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
        isImportingCVBI = false;
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
                loadConfig(new Gson().fromJson(FileUtils.readFromFile(this, new File(AppConfig.vmFolder + vmID + "/rom-data.json")), DataMainRoms.class));
                JSONObject jObj = new JSONObject(FileUtils.readFromFile(this, new File(AppConfig.vmFolder + vmID + "/rom-data.json")));

                if (jObj.has("vmID")) {
                    if (!jObj.isNull("vmID")) {
                        if (!jObj.getString("vmID").isEmpty()) {
                            FileUtils.moveAFile(AppConfig.vmFolder + vmID, AppConfig.vmFolder + jObj.getString("vmID"));
                            vmID = jObj.getString("vmID");
                        }
                    }
                }

                FileUtils.moveAFile(AppConfig.vmFolder + _filename.replace(".cvbi", ""), AppConfig.vmFolder + vmID);

                if (!jObj.has("drive") && !jObj.has("cdrom") && !jObj.has("qemu")) {
                    UIUtils.oneDialog(getResources().getString(R.string.problem_has_been_detected), getResources().getString(R.string.this_rom_is_missing_too_much_information), true, false, this);
                }

                if (!jObj.has("versioncode")) {
                    UIUtils.oneDialog(getResources().getString(R.string.problem_has_been_detected), getResources().getString(R.string.this_rom_may_not_be_compatible), true, false, this);
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

        if (FileUtils.isFileExists(AppConfig.vmFolder + vmID + "/cqcm.json")) {
            FileUtils.writeToFile(AppConfig.vmFolder + vmID, "cqcm.json", FileUtils.readAFile(AppConfig.vmFolder + vmID + "/cqcm.json").replace("OhnoIjustrealizeditsmidnightandIstillhavetodothis", AppConfig.vmFolder + current.vmID + "/"));
        }
    }

    public String getPath(Uri uri) {
        return FileUtils.getPath(this, uri);
    }
}
