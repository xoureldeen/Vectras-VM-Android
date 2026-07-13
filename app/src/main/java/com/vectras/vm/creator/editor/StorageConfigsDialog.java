package com.vectras.vm.creator.editor;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.AppConfig;
import com.vectras.vm.Fragment.CreateImageDialogFragment;
import com.vectras.vm.R;
import com.vectras.vm.creator.utils.CreatorUtils;
import com.vectras.vm.databinding.CreatorStorageDialogBinding;
import com.vectras.vm.file.FilePickerDialog;
import com.vectras.vm.main.vms.DataMainRoms;
import com.vectras.vm.manager.FormatManager;
import com.vectras.vm.manager.VmFileManager;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.IntentUtils;
import com.vectras.vm.utils.ProgressDialog;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StorageConfigsDialog extends BottomSheetDialogFragment {
    final String TAG = "StorageConfigsDialog";

    String vmId;
    DataMainRoms configs;
    public void setConfigs(DataMainRoms configs) {
        this.configs = configs;
        if (configs != null) {
            vmId = configs.vmID;
            if (isAdded()) utils = new CreatorUtils(requireActivity(), vmId);
        }
    }

    CreatorStorageDialogBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private CreatorUtils utils;

    @NonNull
    @Override
    public BottomSheetDialog onCreateDialog(Bundle savedInstanceState) {
        binding = CreatorStorageDialogBinding.inflate(getLayoutInflater());

        utils = new CreatorUtils(requireActivity(), vmId);

        BottomSheetDialog dialog = new BottomSheetDialog(requireActivity());
        dialog.setContentView(binding.getRoot());

        initialize();

        dialog.setOnShowListener(d -> {
            BottomSheetBehavior<FrameLayout> behavior = dialog.getBehavior();
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        });

        return dialog;
    }

    StorageConfigsDialogCallback callback;

    public void setOnDismiss(StorageConfigsDialogCallback callback) {
        this.callback = callback;
    }

    public interface StorageConfigsDialogCallback {
        void onDismiss(DataMainRoms configs);
    }

    public void onDismiss(@NonNull DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        if (callback != null) {
            save();
            callback.onDismiss(configs);
        }
    }

    private final ActivityResultLauncher<String> diskPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                handleDiskFile(uri);
            });

    @SuppressLint("SetTextI18n")
    private final ActivityResultLauncher<String> isoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;

                String fileName = FileUtils.getFileNameFromUri(requireActivity(), uri);
                if (fileName != null && !fileName.isEmpty() && !FormatManager.isOpticalFileFormat(fileName)) {
                    DialogUtils.twoDialog(requireActivity(), getString(R.string.problem_has_been_detected), getString(R.string.file_format_is_not_supported_optical_drive), getResources().getString(R.string.continuetext), getResources().getString(R.string.cancel), true, R.drawable.album_24px, true,
                            () -> handleFile(uri), null, null);
                } else {
                    handleFile(uri);
                }
            });


    @SuppressLint("SetTextI18n")
    private final ActivityResultLauncher<String> floppyPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;

                String fileName = FileUtils.getFileNameFromUri(requireActivity(), uri);
                if (fileName != null && !fileName.isEmpty() && !FormatManager.isFloppyFileFormat(fileName)) {
                    DialogUtils.twoDialog(requireActivity(), getString(R.string.problem_has_been_detected), getString(R.string.file_format_is_not_supported_floppy_drive), getResources().getString(R.string.continuetext), getResources().getString(R.string.cancel), true, R.drawable.save_24px, true,
                            () -> handleFile(uri), null, null);
                } else {
                    handleFile(uri);
                }
            });


    private void handleFile(Uri uri) {
        if (MainSettingsManager.copyFile(requireActivity())) {
            utils.showProgressDialog(getString(R.string.copying_file));

            executor.execute(() -> {
                try {

                    String path = utils.copyToTemp(uri);

                    requireActivity().runOnUiThread(() -> setDrive(PENDING_SELECT_FILE_MODE, path));
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> DialogUtils.oneDialog(requireActivity(),
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
                    requireActivity().runOnUiThread(() -> utils.dissmissProgressDialog());
                }
            });
        } else {
            executor.execute(() -> {
                CreatorUtils.FilePathData filePathData = utils.getFilePath(uri);

                if (!filePathData.isValid) {
                    requireActivity().runOnUiThread(() -> DialogUtils.oneDialog(requireActivity(),
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
                    requireActivity().runOnUiThread(() -> utils.showProgressDialog(getString(R.string.just_a_moment)));
                    requireActivity().runOnUiThread(() -> {
                        setDrive(PENDING_SELECT_FILE_MODE, new File(filePathData.path).getAbsolutePath());
                        utils.dissmissProgressDialog();
                    });
                }
            });
        }
    }

    private void handleDiskFile(Uri uri) {
        ProgressDialog progressDialog1 = new ProgressDialog(requireActivity());
        progressDialog1.show();
        new Thread(() -> {
            CreatorUtils.FilePathData filePathData = utils.getFilePath(uri);

            if (filePathData.isValid) {
                requireActivity().runOnUiThread(() -> {
                    progressDialog1.reset();

                    if (FormatManager.isHardDriveFileFormat(filePathData.name)) {
                        startHandleHardDriveFile(uri);
                    } else {
                        DialogUtils.twoDialog(requireActivity(), getString(R.string.problem_has_been_detected), getString(R.string.file_format_is_not_supported_hard_drive), getResources().getString(R.string.continuetext), getResources().getString(R.string.cancel), true, R.drawable.hard_drive_24px, true,
                                () -> startHandleHardDriveFile(uri), null, null);
                    }
                });
            } else {
                requireActivity().runOnUiThread(() -> startHandleHardDriveFile(uri));
            }
        }).start();
    }

    @SuppressLint("SetTextI18n")
    private void startHandleHardDriveFile(Uri _content_describer) {
        if (MainSettingsManager.copyFile(requireActivity())) {

            if (requireActivity().isFinishing() || requireActivity().isDestroyed()) return;
            utils.showProgressDialog(getString(R.string.copying_file));

            executor.execute(() -> {
                try {
                    String _filename = FileUtils.getFileNameFromUri(requireActivity(), _content_describer);
                    if (_filename == null || _filename.isEmpty()) {
                        _filename = String.valueOf(System.currentTimeMillis());
                    }

                    String path = utils.copyToTemp(_content_describer);

                    String final_filename = _filename;
                    requireActivity().runOnUiThread(() -> {
                        if (requireActivity().isFinishing() || requireActivity().isDestroyed()) {
                            if (!vmId.isEmpty())
                                FileUtils.delete(new File(VmFileManager.getPath(vmId, final_filename)));
                            return;
                        }

                        setDrive(PENDING_SELECT_FILE_MODE, path);
                    });
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> DialogUtils.oneDialog(requireActivity(),
                            getString(R.string.oops),
                            getString(R.string.unable_to_copy_hard_drive_file_content),
                            getString(R.string.ok),
                            true,
                            R.drawable.warning_48px,
                            true,
                            null,
                            null));
                } finally {
                    requireActivity().runOnUiThread(() -> utils.dissmissProgressDialog());
                }
            });
        } else {
            ProgressDialog progressDialog1 = new ProgressDialog(requireActivity());
            progressDialog1.show();
            new Thread(() -> {
                String path = FileUtils.getPath(requireContext(), _content_describer);
                requireActivity().runOnUiThread(() -> {
                    progressDialog1.reset();

                    if (!FileUtils.isValidFilePath(requireActivity(), path, false)) {
                        DialogUtils.oneDialog(requireActivity(),
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

    private void onFileChange(String fileName) {
        utils.markDelete(VmFileManager.getPath(vmId, fileName));
    }

    // STORAGE LOGIC CORE

    private void initialize() {
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



        if (MainSettingsManager.getArch(requireActivity()).equals(MainSettingsManager.ARM64_ARCH)) {
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

        load();
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
                if (MainSettingsManager.getBuiltInFilePicker(requireActivity())) {
                    builtInFilePicker(PICK_OPTICAL_FILE_MODE);
                } else {
                    isoPicker.launch("*/*");
                }
            } else if (PENDING_SELECT_FILE_MODE == SELECT_FLOPPY_A_FILE_MODE || PENDING_SELECT_FILE_MODE == SELECT_FLOPPY_B_FILE_MODE) {
                if (MainSettingsManager.getBuiltInFilePicker(requireActivity())) {
                    builtInFilePicker(PICK_FLOPPY_FILE_MODE);
                } else {
                    floppyPicker.launch("*/*");
                }
            } else {
                if (MainSettingsManager.getBuiltInFilePicker(requireActivity())) {
                    builtInFilePicker(PICK_DRIVE_FILE_MODE);
                } else {
                    diskPicker.launch("*/*");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "pickStorageFile: ", e);
            IntentUtils.showErrorDialog(requireActivity());
        }
    }

    private void storageFileOptionDialog(int mode) {
        PENDING_SELECT_FILE_MODE = mode;

        //TextInputLayout peddingTextInputLayout = getPeddingStorageInputLayout();
        TextInputEditText peddingTextInputEditText = getPeddingStorageEditText();

        DialogUtils.threeDialog(requireActivity(),
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
                    if (path.startsWith(VmFileManager.quickGetPath(vmId)))  {
                        ProgressDialog progressDialog1 = new ProgressDialog(requireActivity());
                        progressDialog1.show();
                        new Thread(() -> {
                            utils.markDelete(path);
                            requireActivity().runOnUiThread(progressDialog1::reset);
                        }).start();
                    }

                    setDrive(mode, "");
                },
                () -> callQcow2CreatorDialog(PENDING_SELECT_FILE_MODE), null);
    }

    private void callQcow2CreatorDialog(int mode) {
        PENDING_SELECT_FILE_MODE = mode;

        TextInputLayout peddingTextInputLayout = getPeddingStorageInputLayout();
        TextInputEditText peddingTextInputEditText = getPeddingStorageEditText();

        CreateImageDialogFragment dialogFragment = new CreateImageDialogFragment();
        dialogFragment.folder = VmFileManager.getPath(vmId);
        dialogFragment.customRom = true;
        dialogFragment.filename = configs.itemName;
        dialogFragment.drive = peddingTextInputEditText;
        dialogFragment.driveLayout = peddingTextInputLayout;
        dialogFragment.isMarkPendingAdd = true;
        dialogFragment.show(requireActivity().getSupportFragmentManager(), "CreateImageDialogFragment");
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
            DialogUtils.oopsDialog(requireActivity(), getString(R.string.vm_creator_this_file_is_already_in_use_content));
            utils.deleteTemp(new File(path).getName());
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

    private boolean isFileReadyinUse(String path) {
        if (path == null || path.trim().isEmpty()) return false;

        String paramCollection = Objects.requireNonNull(binding.drive.getText()).toString();
        paramCollection += "\n" + Objects.requireNonNull(binding.tieHd1.getText());
        paramCollection += "\n" + Objects.requireNonNull(binding.cdrom.getText());
        paramCollection += "\n" + Objects.requireNonNull(binding.tieCdrom1.getText());
        paramCollection += "\n" + Objects.requireNonNull(binding.tieFda.getText());
        paramCollection += "\n" + Objects.requireNonNull(binding.tieFdb.getText());
        paramCollection += "\n" + Objects.requireNonNull(configs.itemExtra);

        return paramCollection.contains(path);
    }

    final int PICK_DRIVE_FILE_MODE = 0;
    final int PICK_OPTICAL_FILE_MODE = 1;
    final int PICK_FLOPPY_FILE_MODE = 2;

    private void builtInFilePicker(int mode) {
        FilePickerDialog filePickerDialog = new FilePickerDialog();

        int finalMode;
        if (mode == PICK_DRIVE_FILE_MODE) {
            finalMode = FilePickerDialog.FIT_PICK_DISK_FILE_MODE;
        } else if (mode == PICK_OPTICAL_FILE_MODE) {
            finalMode = FilePickerDialog.FIT_PICK_OPTICAL_FILE_MODE;
        } else {
            finalMode = FilePickerDialog.FIT_PICK_FLOPPY_FILE_MODE;
        }

        filePickerDialog.pick(requireActivity(), finalMode, (path -> handleFile(Uri.fromFile(new File(path)))));
    }

    private void load() {
        if (configs.itemPath != null && !configs.itemPath.isEmpty()) {
            setDrive(SELECT_DISK_0_FILE_MODE, (configs.itemPath.contains("/") ? "" : VmFileManager.getPath(vmId)).concat(configs.itemPath));
        }

        if (configs.hd1 != null && !configs.hd1.isEmpty()) {
            setDrive(SELECT_DISK_1_FILE_MODE, (configs.hd1.contains("/") ? "" : VmFileManager.getPath(vmId)).concat(configs.hd1));
        }

        if (MainSettingsManager.getArch(getContext()).equals(MainSettingsManager.ARM64_ARCH)) {
            binding.lnFloppyContainer.setVisibility(View.GONE);
        } else {
            if (configs.fda!= null && !configs.fda.isEmpty()) {
                setDrive(SELECT_FLOPPY_A_FILE_MODE, (configs.fda.contains("/") ? "" : VmFileManager.getPath(vmId)).concat(configs.fda));
            }

            if (configs.fdb!= null && !configs.fdb.isEmpty()) {
                setDrive(SELECT_FLOPPY_B_FILE_MODE, (configs.fdb.contains("/") ? "" : VmFileManager.getPath(vmId)).concat(configs.fdb));
            }
        }

        if (configs.imgCdrom != null && !configs.imgCdrom.isEmpty()) {
            setDrive(SELECT_CDROM_0_FILE_MODE, (configs.imgCdrom.contains("/") ? "" : VmFileManager.getPath(vmId)).concat(configs.imgCdrom));
        }

        if (configs.cdrom1 != null && !configs.cdrom1.isEmpty()) {
            setDrive(SELECT_CDROM_1_FILE_MODE, (configs.cdrom1.contains("/") ? "" : VmFileManager.getPath(vmId)).concat(configs.cdrom1));
        }

        binding.svSharedFolder.setChecked(configs.sharedFolder);
    }

    private void save() {
        configs.itemPath = Objects.requireNonNull(binding.drive.getText()).toString();
        configs.hd1 = Objects.requireNonNull(binding.tieHd1.getText()).toString();

        configs.fda = Objects.requireNonNull(binding.tieFda.getText()).toString();
        configs.fdb = Objects.requireNonNull(binding.tieFdb.getText()).toString();

        configs.imgCdrom = Objects.requireNonNull(binding.cdrom.getText()).toString();
        configs.cdrom1 = Objects.requireNonNull(binding.tieCdrom1.getText()).toString();

        configs.sharedFolder = binding.svSharedFolder.isChecked();
    }
}
