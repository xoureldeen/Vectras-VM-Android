package com.vectras.vm.creator.editor;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.R;
import com.vectras.vm.creator.VMCreatorSelector;
import com.vectras.vm.databinding.CreatorFirmwareDialogBinding;
import com.vectras.vm.main.vms.DataMainRoms;

import java.util.Objects;

public class FirmwareConfigsDialog extends BottomSheetDialogFragment {
    final String TAG = "FirmwareConfigsDialog";

    String vmId;
    DataMainRoms configs;

    public void setConfigs(DataMainRoms configs) {
        this.configs = configs;
        if (configs != null) {
            vmId = configs.vmID;
        }
    }

    CreatorFirmwareDialogBinding binding;

    @NonNull
    @Override
    public BottomSheetDialog onCreateDialog(Bundle savedInstanceState) {
        binding = CreatorFirmwareDialogBinding.inflate(getLayoutInflater());

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

    FirmwareConfigsDialogCallback callback;

    public void setOnDismiss(FirmwareConfigsDialogCallback callback) {
        this.callback = callback;
    }

    public interface FirmwareConfigsDialogCallback {
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
        binding.sbvBootfrom.setOnClickListener(v -> VMCreatorSelector.bootFrom(requireActivity(), configs.bootFrom, ((position, name, value) -> {
            configs.bootFrom = position;
            binding.sbvBootfrom.setSubtitle(name);
        })));

        binding.cbvShowbootmenu.setOnCheckedChangeListener((v, isChecked) -> configs.isShowBootMenu = isChecked);

        binding.cbvUselocaltime.setOnCheckedChangeListener((v, isChecked) -> configs.isUseLocalTime = isChecked);

        if (!MainSettingsManager.getArch(requireContext()).equals("X86_64")) {
            binding.cbvUseuefi.setVisibility(View.GONE);
            binding.cbvUseDefaultBios.setBackground(AppCompatResources.getDrawable(requireContext(), R.drawable.object_shape_single_high));
        } else {
            binding.cbvUseuefi.setOnCheckedChangeListener((v, isChecked) -> configs.isUseUefi = isChecked);
        }

        binding.cbvUseDefaultBios.setOnCheckedChangeListener((v, isChecked) -> {
            configs.isUseDefaultBios = isChecked;
            binding.cbvUseuefi.setEnabled(isChecked);
        });

        load();
    }

    private void load() {
        binding.sbvBootfrom.setSubtitle(Objects.requireNonNull(VMCreatorSelector.getBootFrom(requireActivity(), configs.bootFrom).get("name")).toString());

        binding.cbvShowbootmenu.setChecked(configs.isShowBootMenu);

        if (MainSettingsManager.getArch(requireContext()).equals(MainSettingsManager.X86_64_ARCH)) {
            binding.cbvUseuefi.setChecked(configs.isUseUefi);
        } else {
            binding.cbvUseuefi.setVisibility(View.GONE);
        }

        binding.cbvUseDefaultBios.setChecked(configs.isUseDefaultBios);

        binding.cbvUseuefi.setEnabled(configs.isUseDefaultBios);

        binding.cbvUselocaltime.setChecked(configs.isUseLocalTime);
    }

    private void save() {
        configs.isShowBootMenu = binding.cbvShowbootmenu.isChecked();

        configs.isUseUefi = binding.cbvUseuefi.isChecked();
        configs.isUseDefaultBios = binding.cbvUseDefaultBios.isChecked();
        configs.isUseLocalTime = binding.cbvUselocaltime.isChecked();
    }
}