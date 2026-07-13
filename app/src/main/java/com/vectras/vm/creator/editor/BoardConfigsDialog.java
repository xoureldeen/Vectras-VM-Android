package com.vectras.vm.creator.editor;

import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.creator.VMCreatorSelector;
import com.vectras.vm.databinding.CreatorBoardDialogBinding;
import com.vectras.vm.main.vms.DataMainRoms;

import java.util.Objects;

public class BoardConfigsDialog extends BottomSheetDialogFragment {
    final String TAG = "BoardConfigsDialog";

    String vmId;
    DataMainRoms configs;
    public void setConfigs(DataMainRoms configs) {
        this.configs = configs;
        if (configs != null) {
            vmId = configs.vmID;
        }
    }

    CreatorBoardDialogBinding binding;

    @NonNull
    @Override
    public BottomSheetDialog onCreateDialog(Bundle savedInstanceState) {
        binding = CreatorBoardDialogBinding.inflate(getLayoutInflater());

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

    BoardConfigsDialogCallback callback;

    public void setOnDismiss(BoardConfigsDialogCallback callback) {
        this.callback = callback;
    }

    public interface BoardConfigsDialogCallback {
        void onDismiss(DataMainRoms configs);
    }

    public void onDismiss(@NonNull DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        if (callback != null) {
            save();
            callback.onDismiss(configs);
        }
    }

    private void initialize() {
        binding.sbvCpu.setOnClickListener(v -> VMCreatorSelector.cpu(requireActivity(), MainSettingsManager.getArch(requireContext()), configs.cpu, ((position, name, value) -> {
            configs.cpu = position;
            binding.sbvCpu.setSubtitle(name);
        })));

        binding.sbvCore.setOnClickListener(v -> VMCreatorSelector.cpuCore(requireActivity(), MainSettingsManager.getArch(requireContext()), configs.cores, ((position, name, value) -> {
            configs.cores = position;
            binding.sbvCore.setSubtitle(name);
        })));

        binding.sbvThread.setOnClickListener(v -> VMCreatorSelector.cpuThread(requireActivity(), MainSettingsManager.getArch(requireContext()), configs.threads, ((position, name, value) -> {
            configs.threads = position;
            binding.sbvThread.setSubtitle(name);
        })));

        load();
    }

    private void load() {
        binding.sbvCpu.setSubtitle(Objects.requireNonNull(VMCreatorSelector.getCpu(requireContext(), MainSettingsManager.getArch(requireContext()), configs.cpu).get("name")).toString());

        binding.sbvCore.setSubtitle(Objects.requireNonNull(VMCreatorSelector.getCpuCore(MainSettingsManager.getArch(requireContext()), configs.cores).get("value")).toString());

        binding.sbvThread.setSubtitle(String.valueOf(configs.threads + 1));

        binding.cbvBattery.setChecked(configs.battery);
    }

    private void save() {
        configs.battery = binding.cbvBattery.isChecked();
    }
}
