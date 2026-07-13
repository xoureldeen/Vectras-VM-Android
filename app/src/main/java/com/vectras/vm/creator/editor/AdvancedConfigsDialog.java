package com.vectras.vm.creator.editor;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.vectras.vm.creator.QemuParamsEditorActivity;
import com.vectras.vm.databinding.CreatorAdvancedDialogBinding;
import com.vectras.vm.main.vms.DataMainRoms;

import java.util.Objects;

public class AdvancedConfigsDialog extends BottomSheetDialogFragment {
    final String TAG = "AdvancedConfigsDialog";

    String vmId;
    DataMainRoms configs;

    public void setConfigs(DataMainRoms configs) {
        this.configs = configs;
        if (configs != null) {
            vmId = configs.vmID;
        }
    }

    CreatorAdvancedDialogBinding binding;

    boolean iseditparams;

    @NonNull
    @Override
    public BottomSheetDialog onCreateDialog(Bundle savedInstanceState) {
        binding = CreatorAdvancedDialogBinding.inflate(getLayoutInflater());

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

    public void onResume() {
        super.onResume();
        if (iseditparams) {
            iseditparams = false;
            binding.qemu.setText(QemuParamsEditorActivity.result);
        }
    }

    AdvancedConfigsDialogCallback callback;

    public void setOnDismiss(AdvancedConfigsDialogCallback callback) {
        this.callback = callback;
    }

    public interface AdvancedConfigsDialogCallback {
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
        binding.qemu.setOnClickListener(v -> {
            iseditparams = true;
            Intent intent = new Intent();
            intent.setClass(requireContext(), QemuParamsEditorActivity.class);
            intent.putExtra("content", Objects.requireNonNull(binding.qemu.getText()).toString());
            startActivity(intent);
        });

        binding.qemuField.setOnClickListener(v -> {
            iseditparams = true;
            Intent intent = new Intent();
            intent.setClass(requireContext(), QemuParamsEditorActivity.class);
            intent.putExtra("content", Objects.requireNonNull(binding.qemu.getText()).toString());
            startActivity(intent);
        });

        load();
    }

    private void load() {
        binding.qemu.setText(configs.itemExtra);
    }

    private void save() {
        configs.itemExtra = Objects.requireNonNull(binding.qemu.getText()).toString();
    }
}