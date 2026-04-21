package com.vectras.vm.manager;

import android.androidVNC.ConnectionBean;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.vectras.qemu.Config;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.MainVNCActivity;
import com.vectras.qemu.VNCConfig;
import com.vectras.vm.AppConfig;
import com.vectras.vm.R;
import com.vectras.vm.VMManager;
import com.vectras.vm.databinding.DialogChangeRemovableDevicesBinding;
import com.vectras.vm.settings.VNCSettingsActivity;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.ImageUtils;
import com.vectras.vm.utils.ProgressDialog;

import java.io.File;
import java.util.Objects;

public class VmControllerDialog extends DialogFragment {

    private DialogChangeRemovableDevicesBinding binding;
    private String infoBlock = "";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        binding = DialogChangeRemovableDevicesBinding.inflate(getLayoutInflater());

        AlertDialog dialog = new AlertDialog.Builder(requireActivity()).create();
        dialog.setView(binding.getRoot());

        new Thread(() -> {
            infoBlock = QmpSender.getAllDevice();

            new Handler(Looper.getMainLooper()).post(() -> {
                binding.lnTakeScreenshot.setOnClickListener(v -> takeScreenshot());

                binding.lnConsole.setOnClickListener(v -> {
                    QemuConsoleDialog qemuConsoleDialog = new QemuConsoleDialog();
                    qemuConsoleDialog.show(requireActivity().getSupportFragmentManager(), "QemuConsoleDialogFragment");
                    dismiss();
                });


                binding.lnPause.setOnClickListener(v -> {
                    VMManager.showPauseDialog(requireActivity());
                    dismiss();
                });

                binding.lnFolderSharingServer.setOnClickListener(v -> {
                    HostSharedFolder.start(requireActivity());
                    dismiss();
                });

                if (infoBlock != null && (infoBlock.contains(QmpSender.DEFAULT_OPTICAL_DISC_1_ID)
                        || infoBlock.contains(QmpSender.DEFAULT_OPTICAL_DISC_2_ID)
                        || infoBlock.contains(QmpSender.DEFAULT_FLOPPY_DISK_0_ID)
                        || infoBlock.contains(QmpSender.DEFAULT_FLOPPY_DISK_1_ID)
                        || infoBlock.contains(QmpSender.DEFAULT_MEMORY_CARD_ID))) {

                    if (infoBlock.contains(QmpSender.DEFAULT_OPTICAL_DISC_1_ID)
                            || infoBlock.contains(QmpSender.DEFAULT_OPTICAL_DISC_2_ID)) {

                        binding.lnCdrom.setOnClickListener(v -> isoPicker.launch("*/*"));

                        binding.ivEjectcdrom.setOnClickListener(v -> {
                            QmpSender.ejectOpticalDisc(requireActivity(), infoBlock);
                            dismiss();
                        });

                        if (!infoBlock.contains(AppConfig.basefiledir + "3dfx-wrappers.iso"))
                            binding.ivEject3dfx.setVisibility(View.GONE);

                        binding.ln3dfx.setOnClickListener(v -> {
                            if (infoBlock.contains(AppConfig.basefiledir + "3dfx-wrappers.iso")) {
                                QmpSender.ejectOpticalDisc(requireActivity(), infoBlock);
                            } else {
                                ToolsManager.mount3dfxWrappers(requireActivity());
                            }
                            dismiss();
                        });

                        if (!infoBlock.contains(AppConfig.basefiledir + "virtio-win.iso"))
                            binding.ivEjectvirtio.setVisibility(View.GONE);

                        binding.lnVirtio.setOnClickListener(v -> {
                            if (infoBlock.contains(AppConfig.basefiledir + "virtio-win.iso")) {
                                QmpSender.ejectOpticalDisc(requireActivity(), infoBlock);
                            } else {
                                ToolsManager.mountVirtIOWin(requireActivity());
                            }
                            dismiss();
                        });
                    } else {
                        binding.lnCdrom.setVisibility(View.GONE);
                        binding.lnTools.setVisibility(View.GONE);
                    }

                    if (infoBlock.contains(QmpSender.DEFAULT_FLOPPY_DISK_0_ID)) {
                        binding.lnFda.setOnClickListener(v -> floppyAPicker.launch("*/*"));

                        binding.ivEjectfda.setOnClickListener(v -> {
                            QmpSender.ejectFloppyDiskA(requireActivity());
                            dismiss();
                        });

                        if (!infoBlock.contains(QmpSender.DEFAULT_FLOPPY_DISK_1_ID)) {
                            binding.tvFda.setText(R.string.floppy_drive);
                        }
                    } else {
                        binding.lnFda.setVisibility(View.GONE);
                    }

                    if (infoBlock.contains(QmpSender.DEFAULT_FLOPPY_DISK_1_ID)) {
                        binding.lnFdb.setOnClickListener(v -> floppyBPicker.launch("*/*"));

                        binding.ivEjectfdb.setOnClickListener(v -> {
                            QmpSender.ejectFloppyDiskB(requireActivity());
                            dismiss();
                        });

                        if (!infoBlock.contains(QmpSender.DEFAULT_FLOPPY_DISK_0_ID)) {
                            binding.tvFdb.setText(R.string.floppy_drive);
                        }
                    } else {
                        binding.lnFdb.setVisibility(View.GONE);
                    }

                    if (infoBlock.contains(QmpSender.DEFAULT_MEMORY_CARD_ID)) {
                        binding.lnSd.setOnClickListener(v -> memoryCardPicker.launch("*/*"));

                        binding.ivEjectsd.setOnClickListener(v -> {
                            QmpSender.ejectMemoryCard(requireActivity());
                            dismiss();
                        });
                    } else {
                        binding.lnSd.setVisibility(View.GONE);
                    }
                } else {
                    binding.tvOtherdevice.setText(R.string.change_or_eject_a_device);

                    binding.lnCdrom.setVisibility(View.GONE);
                    binding.lnFda.setVisibility(View.GONE);
                    binding.lnFdb.setVisibility(View.GONE);
                    binding.lnSd.setVisibility(View.GONE);
                }

                binding.lnOtherdevice.setOnClickListener(v -> {
                    ChangeDeviceDialog changeDeviceDialog = new ChangeDeviceDialog();
                    changeDeviceDialog.show(requireActivity().getSupportFragmentManager(), "ChangeDeviceDialog");
                    dismiss();
                });


                if (isAdded() && requireActivity() instanceof MainVNCActivity) {
                    binding.lnRefresh.setOnClickListener(v -> {
                        requireActivity().startActivity(new Intent(requireActivity(), MainVNCActivity.class));
                        requireActivity().overridePendingTransition(0, 0);
                        requireActivity().finish();
                        dismiss();
                    });

                    if (ConnectionBean.useLocalCursor) {
                        binding.tvVirtualmouse.setText(getString(R.string.hide_virtual_mouse));
                    }

                    binding.lnVirtualmouse.setOnClickListener(v -> {
                        MainSettingsManager.setShowVirtualMouse(requireActivity(), !ConnectionBean.useLocalCursor);
                        ConnectionBean.useLocalCursor = !ConnectionBean.useLocalCursor;
                        dismiss();
                    });

                    binding.lnMouse.setOnClickListener(v -> {
                        MainVNCActivity.getContext.onMouseMode();
                        dismiss();
                    });

                    binding.lnSettings.setOnClickListener(v -> {
                        requireActivity().startActivity(new Intent(requireActivity(), VNCSettingsActivity.class));
                        dismiss();
                    });

                    if (MainSettingsManager.getVNCScaleMode(requireActivity()) == VNCConfig.oneToOne) {
                        binding.ivScreenOneToOne.setBackgroundResource(R.drawable.dialog_shape_single_button);
                    } else {
                        binding.ivScreenFit.setBackgroundResource(R.drawable.dialog_shape_single_button);
                    }

                    binding.ivScreenOneToOne.setOnClickListener(v -> {
                        MainSettingsManager.setVNCScaleMode(requireActivity(), VNCConfig.oneToOne);
                        requireActivity().startActivity(new Intent(requireActivity(), MainVNCActivity.class));
                        requireActivity().overridePendingTransition(0, 0);
                        requireActivity().finish();
                        dismiss();
                    });

                    binding.ivScreenFit.setOnClickListener(v -> {
                        MainSettingsManager.setVNCScaleMode(requireActivity(), VNCConfig.fitToScreen);
                        requireActivity().startActivity(new Intent(requireActivity(), MainVNCActivity.class));
                        requireActivity().overridePendingTransition(0, 0);
                        requireActivity().finish();
                        dismiss();
                    });
                } else {
                    binding.lnUserInterface.setVisibility(View.GONE);
                }
            });
        }).start();

        return dialog;
    }

    private void takeScreenshot() {
        ProgressDialog progressDialog = new ProgressDialog(requireActivity());
        progressDialog.setText(getString(R.string.taking_a_screenshot));
        progressDialog.setFixTextColor(true);
        progressDialog.show();

        new Thread(() -> {
            boolean isSaved = VmActions.takeScreenshot(requireActivity(), true);

            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded()) return;
                progressDialog.reset();
                Toast.makeText(requireActivity().getApplicationContext(), getString(isSaved ? R.string.saved_to_the_gallery : R.string.unable_to_take_a_screenshot), Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }).start();
    }

    private final ActivityResultLauncher<String> isoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        File selectedFilePath = new File(Objects.requireNonNull(FileUtils.getPath(requireActivity(), uri)));
                        QmpSender.changeOpticalDisc(requireActivity(), selectedFilePath.getAbsolutePath(), infoBlock);
                    } catch (Exception e) {
                        showErrorSelectedFileDialog();
                    }

                    dismiss();
                }
            });

    private final ActivityResultLauncher<String> floppyAPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        File selectedFilePath = new File(Objects.requireNonNull(FileUtils.getPath(requireActivity(), uri)));
                        QmpSender.changeFloppyDiskA(requireActivity(), selectedFilePath.getAbsolutePath());
                    } catch (Exception e) {
                        showErrorSelectedFileDialog();
                    }

                    dismiss();
                }
            });

    private final ActivityResultLauncher<String> floppyBPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        File selectedFilePath = new File(Objects.requireNonNull(FileUtils.getPath(requireActivity(), uri)));
                        QmpSender.changeFloppyDiskB(requireActivity(), selectedFilePath.getAbsolutePath());
                    } catch (Exception e) {
                        showErrorSelectedFileDialog();
                    }

                    dismiss();
                }
            });

    private final ActivityResultLauncher<String> memoryCardPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        File selectedFilePath = new File(Objects.requireNonNull(FileUtils.getPath(requireActivity(), uri)));
                        QmpSender.changeMemoryCard(requireActivity(), selectedFilePath.getAbsolutePath());
                    } catch (Exception e) {
                        showErrorSelectedFileDialog();
                    }

                    dismiss();
                }
            });

    private void showErrorSelectedFileDialog() {
        DialogUtils.oneDialog(requireActivity(),
                getString(R.string.oops),
                getString(R.string.invalid_file_path_content),
                getString(R.string.ok),
                true,
                R.drawable.error_96px,
                true,
                null,
                null
        );
    }
}
