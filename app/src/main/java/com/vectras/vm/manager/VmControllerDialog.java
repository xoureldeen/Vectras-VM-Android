package com.vectras.vm.manager;

import android.androidVNC.ConnectionBean;
import android.androidVNC.VncCanvas;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
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
import com.vectras.vm.main.core.DisplaySystem;
import com.vectras.vm.main.vms.DataMainRoms;
import com.vectras.vm.settings.VNCSettingsActivity;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.ProgressDialog;
import com.vectras.vm.utils.StreamAudio;
import com.vectras.vm.x11.X11Activity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class VmControllerDialog extends DialogFragment {

    private DialogChangeRemovableDevicesBinding binding;
    private String infoBlock = "";
    public int position = -1;
    public StreamAudio streamAudio;
    public VncCanvas vncCanvas;
    public View screenshotFrame;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        binding = DialogChangeRemovableDevicesBinding.inflate(getLayoutInflater());

        AlertDialog dialog = new AlertDialog.Builder(requireActivity()).create();
        dialog.setView(binding.getRoot());

        AtomicBoolean isGotInfo = new AtomicBoolean(false);

        ProgressDialog progressDialog = new ProgressDialog(requireActivity());
        progressDialog.setText(getString(R.string.just_a_sec));
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isGotInfo.get()) {
                progressDialog.reset();
            } else {
                progressDialog.show();
            }
        }, 1000);


        new Thread(() -> {
            try {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<String> getInfoBlock = executor.submit(QmpSender::getAllDevice);
                infoBlock = getInfoBlock.get(3, TimeUnit.SECONDS);
            } catch (Exception e) {
                infoBlock = "";
            }

            if (!isAdded()) {
                progressDialog.reset();
                return;
            }

            long audioFileSize = FileUtils.getFileSize(VmFileManager.findAudioRaw(requireContext(), Config.vmID));

            isGotInfo.set(true);
            new Handler(Looper.getMainLooper()).post(() -> {
                progressDialog.reset();

                if (position > -1) {
                    binding.lnConnect.setOnClickListener(v -> {
                        if (isAdded()) DisplaySystem.launch(requireActivity());
                        dismiss();
                    });

//                    binding.lnEdit.setOnClickListener(v -> {
//                        if (isAdded()) {
//                            DataMainRoms vmConfig = VMManager.getVMConfig(position);
//                            startActivity(new Intent(requireActivity(), VMCreatorActivity.class).putExtra("POS", position).putExtra("MODIFY", true).putExtra("VMID", vmConfig.vmID));
//                        }
//                        dismiss();
//                    });

                    binding.lnRemove.setOnClickListener(v -> {
                        if (isAdded()) {
                            DataMainRoms vmConfig = VMManager.getVMConfig(position);
                            VMManager.deleteVMDialog(vmConfig.itemName, position, requireActivity());
                        }
                        dismiss();
                    });

                    binding.lnSwitch.setVisibility(View.GONE);
                } else if (requireActivity() instanceof MainVNCActivity) {
                    ArrayList<HashMap<String, Object>> list = VmListManager.getAllVmForPickRunningVncSocketOnly(requireActivity());

                    if (list.isEmpty() || (list.size() == 1 && Objects.requireNonNull(list.get(0).get("value")).toString().equals(Config.vmID))) {
                        binding.lnManage.setVisibility(View.GONE);
                    } else {
                        binding.lnSwitch.setOnClickListener(v -> {
                            if (isAdded()) {
                                VmPicker vmPicker = new VmPicker(requireActivity());
                                vmPicker.currentVmId = Config.vmID;
                                vmPicker.title = getString(R.string.switch_to);
                                vmPicker.listVm = list;
                                vmPicker.pick((position, name, value) -> {
                                    if (Config.vmID.equals(value)) return;

                                    if (position < 0) {
                                        DialogUtils.oopsDialog(requireActivity(), getString(R.string.no_vms_are_available));
                                        return;
                                    }

                                    Config.vmID = value;
                                    streamAudio.setCross(null);
                                    streamAudio.stop();
                                    requireActivity().recreate();

                                    dismiss();
                                });
                            }
                        });

                        binding.lnConnect.setVisibility(View.GONE);
//                        binding.lnEdit.setVisibility(View.GONE);
                        binding.lnRemove.setVisibility(View.GONE);
                    }
                } else {
                    binding.lnManage.setVisibility(View.GONE);
                }

                if (isAdded() && requireActivity() instanceof MainVNCActivity) {
                    binding.lnPower.setVisibility(View.GONE);
                } else {
                    binding.lnPower.setOnClickListener(v -> {
                        if (isAdded()) VMManager.showPowerDialogOptions(requireActivity());
                        dismiss();
                    });
                }

                if (screenshotFrame != null && screenshotFrame.getVisibility() == View.VISIBLE) {
                    binding.lnTakeScreenshot.setVisibility(View.GONE);
                } else {
                    binding.lnTakeScreenshot.setOnClickListener(v -> takeScreenshot());
                }


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

                if (isAdded() && (!(requireActivity() instanceof MainVNCActivity)) && (!(requireActivity() instanceof X11Activity))) {
                    if (streamAudio == null)
                        streamAudio = new StreamAudio(requireActivity().getApplicationContext());
                    if (!streamAudio.isPlaying()) VmAudioManager.set(Config.vmID);
                }

                if (streamAudio == null ||
                        !isAdded() ||
                        audioFileSize == 0 ||
                        (isAdded() && (!(requireActivity() instanceof MainVNCActivity)) && !(requireActivity() instanceof X11Activity) && !VmAudioManager.currentVmId.equals(Config.vmID))
                ) {
                    binding.lnMute.setVisibility(View.GONE);
                } else {
                    if (isAdded() && requireActivity() instanceof X11Activity) {
                        if (!streamAudio.getFile().equals(VmFileManager.findAudioRaw(getContext(), Config.vmID)) || !streamAudio.isPlaying()) {
                            binding.ivMute.setImageResource(R.drawable.volume_up_24px);
                            binding.tvMute.setText(R.string.unmute);
                        }
                    } else if (!streamAudio.isPlaying()) {
                        binding.ivMute.setImageResource(R.drawable.volume_up_24px);
                        binding.tvMute.setText(R.string.unmute);
                    }

                    binding.lnMute.setOnClickListener(v -> mute());
                }

                if (infoBlock != null && (infoBlock.contains(QmpSender.DEFAULT_OPTICAL_DISC_1_ID)
                        || infoBlock.contains(QmpSender.DEFAULT_OPTICAL_DISC_2_ID)
                        || infoBlock.contains(QmpSender.DEFAULT_SECONDARY_OPTICAL_DISC_1_ID)
                        || infoBlock.contains(QmpSender.DEFAULT_SECONDARY_OPTICAL_DISC_2_ID)
                        || infoBlock.contains(QmpSender.DEFAULT_FLOPPY_DISK_0_ID)
                        || infoBlock.contains(QmpSender.DEFAULT_FLOPPY_DISK_1_ID)
                        || infoBlock.contains(QmpSender.DEFAULT_MEMORY_CARD_ID))) {

                    if (isHavingOpticalDisc() || isHavingSecondaryOpticalDisc()) {

                        if (isHavingOpticalDisc()) {
                            binding.lnCdrom.setOnClickListener(v -> isoPicker.launch("*/*"));

                            binding.ivEjectcdrom.setOnClickListener(v -> {
                                QmpSender.ejectOpticalDisc(requireActivity(), infoBlock);
                                dismiss();
                            });

                            if (!isHavingSecondaryOpticalDisc())
                                binding.tvCdrom.setText(R.string.cdrom);
                        } else {
                            binding.lnCdrom.setVisibility(View.GONE);
                        }

                        if (isHavingSecondaryOpticalDisc()) {
                            binding.lnSecondaryCdrom.setOnClickListener(v -> isoPickerSecondary.launch("*/*"));

                            binding.ivSecondaryEjectcdrom.setOnClickListener(v -> {
                                QmpSender.ejectSecondaryOpticalDisc(requireActivity(), infoBlock);
                                dismiss();
                            });

                            if (!isHavingOpticalDisc())
                                binding.tvSecondaryCdrom.setText(R.string.cdrom);
                        } else {
                            binding.lnSecondaryCdrom.setVisibility(View.GONE);
                        }

                        if (!infoBlock.contains(AppConfig.basefiledir + "3dfx-wrappers.iso"))
                            binding.ivEject3dfx.setVisibility(View.GONE);

                        binding.ln3dfx.setOnClickListener(v -> {
                            if (infoBlock.contains(AppConfig.basefiledir + "3dfx-wrappers.iso")) {
                                QmpSender.ejectDynamicSecondaryOpticalDisc(requireActivity(), infoBlock);
                            } else {
                                ToolsManager.mount3dfxWrappers(requireActivity());
                            }
                            dismiss();
                        });

                        if (!infoBlock.contains(AppConfig.basefiledir + "virtio-win.iso"))
                            binding.ivEjectvirtio.setVisibility(View.GONE);

                        binding.lnVirtio.setOnClickListener(v -> {
                            if (infoBlock.contains(AppConfig.basefiledir + "virtio-win.iso")) {
                                QmpSender.ejectDynamicSecondaryOpticalDisc(requireActivity(), infoBlock);
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


                if (isAdded() && requireActivity() instanceof MainVNCActivity mainVNCActivity) {
                    binding.lnRefresh.setOnClickListener(v -> {
                        requireActivity().startActivity(new Intent(requireActivity(), MainVNCActivity.class));
                        requireActivity().overridePendingTransition(0, 0);
                        requireActivity().finish();
                        dismiss();
                    });

                    binding.swVirtualmouse.setChecked(ConnectionBean.useLocalCursor);

                    binding.swVirtualmouse.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        MainSettingsManager.setShowVirtualMouse(requireActivity(), isChecked);
                        ConnectionBean.useLocalCursor = isChecked;
                    });

                    binding.lnVirtualmouse.setOnClickListener(v -> binding.swVirtualmouse.toggle());

//                    binding.lnMouse.setOnClickListener(v -> {
//                        MainVNCActivity.getContext.onMouseMode();
//                        dismiss();
//                    });

                    binding.lnSettings.setOnClickListener(v -> {
                        requireActivity().startActivity(new Intent(requireActivity(), VNCSettingsActivity.class));
                        dismiss();
                    });

                    TypedValue typedValue = new TypedValue();
                    requireActivity().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
                    int colorPrimary = typedValue.data;
                    int colorControlNormal = binding.tvMute.getCurrentTextColor();

                    if (MainSettingsManager.getVNCScaleMode(requireActivity()) == VNCConfig.oneToOne) {
                        binding.ivScreenOneToOne.setBackgroundResource(R.drawable.dialog_shape_single_button);
                        binding.ivScreenOneToOne.setColorFilter(colorPrimary);
                    } else if (MainSettingsManager.getVNCScaleMode(requireActivity()) == VNCConfig.scaleToFitScreen) {
                        binding.ivScreenScale.setBackgroundResource(R.drawable.dialog_shape_single_button);
                        binding.ivScreenScale.setColorFilter(colorPrimary);
                    } else {
                        binding.ivScreenFit.setBackgroundResource(R.drawable.dialog_shape_single_button);
                        binding.ivScreenFit.setColorFilter(colorPrimary);
                    }

                    binding.ivScreenOneToOne.setOnClickListener(v -> {
                        if (MainSettingsManager.getVNCScaleMode(requireActivity()) == VNCConfig.oneToOne)
                            return;

                        MainSettingsManager.setVNCScaleMode(requireActivity(), VNCConfig.oneToOne);
                        streamAudio.setCross(null);
                        streamAudio.stop();
                        requireActivity().recreate();
                        dismiss();
                    });

                    binding.ivScreenFit.setOnClickListener(v -> {
                        if (MainSettingsManager.getVNCScaleMode(requireActivity()) == VNCConfig.fitToScreen)
                            return;

                        MainSettingsManager.setVNCScaleMode(requireActivity(), VNCConfig.fitToScreen);
                        streamAudio.setCross(null);
                        streamAudio.stop();
                        requireActivity().recreate();
                        dismiss();
                    });

                    binding.ivScreenScale.setOnClickListener(v -> {
                        if (MainSettingsManager.getVNCScaleMode(requireActivity()) == VNCConfig.scaleToFitScreen)
                            return;

                        MainSettingsManager.setVNCScaleMode(requireActivity(), VNCConfig.scaleToFitScreen);
                        streamAudio.setCross(null);
                        streamAudio.stop();
                        requireActivity().recreate();
                        dismiss();
                    });

                    boolean isEdgeToEdge = MainSettingsManager.getEdgeToEdgeVnc(getContext());

                    binding.swEdgeToEdge.setChecked(isEdgeToEdge);

                    binding.lnEdgeToEdge.setOnClickListener(v -> {
                        MainSettingsManager.setEdgeToEdgeVnc(requireActivity(), !isEdgeToEdge);
                        streamAudio.setCross(null);
                        streamAudio.stop();
                        requireActivity().recreate();
                        dismiss();
                    });

                    binding.swPinchToZoom.setChecked(MainSettingsManager.getVncPinchToZoom(getContext()));

                    binding.swPinchToZoom.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        MainSettingsManager.setVncPinchToZoom(requireActivity(), isChecked);

                        if (!isChecked) {
                            vncCanvas.setScaleY(vncCanvas.scalingY);
                            vncCanvas.setScaleX(vncCanvas.scalingX);

                            vncCanvas.setTranslationY(0);
                            vncCanvas.setTranslationX(0);
                        }
                    });

                    binding.lnPinchToZoom.setOnClickListener(v -> binding.swPinchToZoom.toggle());

                    if (Config.mouseMode == Config.MouseMode.Trackpad) {
                        binding.ivTrackpadMode.setBackgroundResource(R.drawable.dialog_shape_single_button);
                        binding.ivTrackpadMode.setColorFilter(colorPrimary);
                    } else {
                        binding.ivExternalMouseMode.setBackgroundResource(R.drawable.dialog_shape_single_button);
                        binding.ivExternalMouseMode.setColorFilter(colorPrimary);
                    }

                    binding.ivTrackpadMode.setOnClickListener(v -> {
                        if (Config.mouseMode == Config.MouseMode.Trackpad) return;
                        mainVNCActivity.setUIModeMobile(false);
                        binding.ivTrackpadMode.setBackgroundResource(R.drawable.dialog_shape_single_button);
                        binding.ivTrackpadMode.setColorFilter(colorPrimary);
                        binding.ivExternalMouseMode.setBackgroundResource(R.drawable.dialog_shape_click_effect_button);
                        binding.ivExternalMouseMode.setColorFilter(colorControlNormal);
                    });

                    binding.ivExternalMouseMode.setOnClickListener(v -> {
                        if (Config.mouseMode == Config.MouseMode.External) return;
                        mainVNCActivity.setUIModeDesktop();
                        binding.ivTrackpadMode.setBackgroundResource(R.drawable.dialog_shape_click_effect_button);
                        binding.ivTrackpadMode.setColorFilter(colorControlNormal);
                        binding.ivExternalMouseMode.setBackgroundResource(R.drawable.dialog_shape_single_button);
                        binding.ivExternalMouseMode.setColorFilter(colorPrimary);
                    });
                } else {
                    binding.lnUserInterface.setVisibility(View.GONE);
                }
            });
        }).start();

        return dialog;
    }

    private Runnable onDismissCallback;

    public void setOnDismissCallback(Runnable callback) {
        this.onDismissCallback = callback;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (onDismissCallback != null) {
            onDismissCallback.run();
        }
    }

    private void takeScreenshot() {
        ProgressDialog progressDialog;

        if (screenshotFrame == null) {
            progressDialog = new ProgressDialog(requireActivity());
            progressDialog.setText(getString(R.string.taking_a_screenshot));
            progressDialog.setFixTextColor(true);
            progressDialog.show();
        } else {
            progressDialog = null;

            screenshotFrame.setVisibility(View.VISIBLE);
            screenshotFrame.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        }

        new Thread(() -> {
            boolean isSaved = VmActions.takeScreenshot(requireActivity(), true);

            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded()) return;

                if (progressDialog != null) progressDialog.reset();

                if (screenshotFrame != null) {
                    screenshotFrame.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .start();

                    new Handler(Looper.getMainLooper()).postDelayed(() -> screenshotFrame.setVisibility(View.GONE), 300);
                }

                Toast.makeText(requireActivity().getApplicationContext(), getString(isSaved ? R.string.saved_to_the_gallery : R.string.unable_to_take_a_screenshot), Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }).start();
    }

    private void mute() {
        if (isAdded() && requireActivity() instanceof X11Activity) {
            if (streamAudio.isPlaying() && streamAudio.getFile().equals(VmFileManager.findAudioRaw(getContext(), Config.vmID))) {
                streamAudio.stop();
                binding.ivMute.setImageResource(R.drawable.volume_up_24px);
                binding.tvMute.setText(R.string.unmute);
            } else {
                if (!streamAudio.getFile().equals(VmFileManager.findAudioRaw(getContext(), Config.vmID))) {
                    streamAudio.stop();
                    streamAudio.setFile(VmFileManager.findAudioRaw(getContext(), Config.vmID));
                }
                streamAudio.play();
                binding.ivMute.setImageResource(R.drawable.volume_off_24px);
                binding.tvMute.setText(R.string.mute);
            }
        } else if (streamAudio.isPlaying()) {
            streamAudio.stop();
            binding.ivMute.setImageResource(R.drawable.volume_up_24px);
            binding.tvMute.setText(R.string.unmute);
        } else {
            if (isAdded() && requireActivity() instanceof MainVNCActivity) {
                streamAudio.play();
            } else {
                VmAudioManager.stream(Config.vmID);
            }
            binding.ivMute.setImageResource(R.drawable.volume_off_24px);
            binding.tvMute.setText(R.string.mute);
        }
    }

    private boolean isHavingOpticalDisc() {
        return infoBlock.contains(QmpSender.DEFAULT_OPTICAL_DISC_1_ID) ||
                infoBlock.contains(QmpSender.DEFAULT_OPTICAL_DISC_2_ID);
    }

    private boolean isHavingSecondaryOpticalDisc() {
        return infoBlock.contains(QmpSender.DEFAULT_SECONDARY_OPTICAL_DISC_1_ID) ||
                (infoBlock.contains(QmpSender.DEFAULT_OPTICAL_DISC_2_ID) && infoBlock.contains(QmpSender.DEFAULT_SECONDARY_OPTICAL_DISC_2_ID));
    }

    private final ActivityResultLauncher<String> isoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        new Thread(() -> {
                            String path = FileUtils.getPath(getContext(), uri);

                            if (path == null) {
                                showErrorSelectedFileDialog();
                                dismiss();
                                return;
                            }

                            File selectedFilePath = new File(path);

                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    QmpSender.changeOpticalDisc(requireActivity(), selectedFilePath.getAbsolutePath(), infoBlock);
                                    dismiss();
                                });
                            }
                        }).start();
                    } catch (Exception e) {
                        showErrorSelectedFileDialog();
                        dismiss();
                    }
                }
            });

    private final ActivityResultLauncher<String> isoPickerSecondary =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        new Thread(() -> {
                            File selectedFilePath = new File(Objects.requireNonNull(FileUtils.getPath(getContext(), uri)));

                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    QmpSender.changeSecondaryOpticalDisc(requireActivity(), selectedFilePath.getAbsolutePath(), infoBlock);
                                    dismiss();
                                });
                            }
                        }).start();
                    } catch (Exception e) {
                        showErrorSelectedFileDialog();
                        dismiss();
                    }
                }
            });

    private final ActivityResultLauncher<String> floppyAPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        new Thread(() -> {
                            File selectedFilePath = new File(Objects.requireNonNull(FileUtils.getPath(getContext(), uri)));

                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    QmpSender.changeFloppyDiskA(requireActivity(), selectedFilePath.getAbsolutePath());
                                    dismiss();
                                });
                            }
                        }).start();
                    } catch (Exception e) {
                        showErrorSelectedFileDialog();
                        dismiss();
                    }
                }
            });

    private final ActivityResultLauncher<String> floppyBPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        new Thread(() -> {
                            File selectedFilePath = new File(Objects.requireNonNull(FileUtils.getPath(getContext(), uri)));

                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    QmpSender.changeFloppyDiskB(requireActivity(), selectedFilePath.getAbsolutePath());
                                    dismiss();
                                });
                            }
                        }).start();
                    } catch (Exception e) {
                        showErrorSelectedFileDialog();
                        dismiss();
                    }
                }
            });

    private final ActivityResultLauncher<String> memoryCardPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        new Thread(() -> {
                            File selectedFilePath = new File(Objects.requireNonNull(FileUtils.getPath(getContext(), uri)));

                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    QmpSender.changeMemoryCard(requireActivity(), selectedFilePath.getAbsolutePath());
                                    dismiss();
                                });
                            }
                        }).start();
                    } catch (Exception e) {
                        showErrorSelectedFileDialog();
                        dismiss();
                    }
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
