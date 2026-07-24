package com.vectras.vm.creator.editor;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.utils.RamInfo;
import com.vectras.vm.R;
import com.vectras.vm.creator.configs.ListManager;
import com.vectras.vm.creator.utils.EditorUtils;
import com.vectras.vm.creator.utils.VMCreatorSelector;
import com.vectras.vm.databinding.CreatorBoardDialogBinding;
import com.vectras.vm.main.vms.DataMainRoms;
import com.vectras.vm.utils.DialogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class BoardConfigsDialog extends BottomSheetDialogFragment {
    final String TAG = "BoardConfigsDialog";

    String vmId;
    DataMainRoms configs;

    boolean isSave = true;

    int availableMemory = Integer.MAX_VALUE;

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
        // This can happen after the app is freed from memory and then reopened.
        if (configs == null) {
            isSave = false;
            if (savedInstanceState == null) DialogUtils.oopsDialog(requireActivity(), getString(R.string.something_went_wrong));
            dismiss();
            return EditorUtils.getDummyDialog(requireActivity());
        }

        availableMemory = RamInfo.vectrasMemory(requireActivity());

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
        if (callback != null && isSave) {
            save();
            callback.onDismiss(configs);
        }
    }

    // Auto save.
    public void onPause() {
        super.onPause();
        if (callback != null && isSave) {
            save();
            callback.onDismiss(configs);
        }
    }

    private void initialize() {
        if (!isAdded()) return;

        binding.sbvMachine.setOnClickListener(v -> VMCreatorSelector.machine(requireActivity(), MainSettingsManager.getArch(requireContext()), configs.machine, ((position, name, value) -> {
            configs.machine = position;
            binding.sbvMachine.setSubtitle(name);
        })));

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

        if (MainSettingsManager.getArch(requireContext()).equals(MainSettingsManager.PPC_ARCH)) {
            binding.cbvNestedVirtualization.setVisibility(View.GONE);
            binding.sbvThread.setBackground(AppCompatResources.getDrawable(requireContext(), R.drawable.object_shape_bottom_high));
        }


        binding.cpiMemory.setEndIconOnClickListener(v -> {
            // 11 = 1024
            int postion = 11;
            boolean markSelected = false;

            if (binding.tietMemory.getText() != null && !binding.tietMemory.getText().toString().isEmpty()) {
                int current = Integer.parseInt(binding.tietMemory.getText().toString());
                int nearest = Integer.MAX_VALUE;
                ArrayList<HashMap<String, Object>> list = ListManager.memoryCapacity(requireContext(), MainSettingsManager.getArch(requireContext()));

                for (int i = 0; i < list.size(); i++) {
                    int distance = Math.abs(((int) list.get(i).get("value")) - current);
                    if (distance == 0) {
                        postion = i;
                        markSelected = true;
                        break;
                    } else if (distance < nearest) {
                        nearest = distance;
                        postion = i;
                    }
                }
            }


            VMCreatorSelector.memory(requireActivity(), MainSettingsManager.getArch(requireContext()), postion, markSelected, ((position, name, value) -> {
                binding.tietMemory.setText(value);
                binding.tietMemory.setSelection(value.length());
            }));
        });

        binding.tietMemory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable editable) {
                if (!editable.toString().isEmpty() && Integer.parseInt(editable.toString()) > (availableMemory / 100 * 80)) {
                    binding.cpiMemory.setError(getString(R.string.capacity_too_large));
                } else {
                    binding.cpiMemory.setError(null);
                }
            }
        });

        load();
    }

    private void load() {
        if (!isAdded()) return;

        binding.sbvMachine.setSubtitle(Objects.requireNonNull(VMCreatorSelector.getMachine(requireContext(), MainSettingsManager.getArch(requireContext()), configs.machine).get("name")).toString());

        binding.sbvCpu.setSubtitle(Objects.requireNonNull(VMCreatorSelector.getCpu(requireContext(), MainSettingsManager.getArch(requireContext()), configs.cpu).get("name")).toString());
        binding.sbvCore.setSubtitle(Objects.requireNonNull(VMCreatorSelector.getCpuCore(MainSettingsManager.getArch(requireContext()), configs.cores).get("value")).toString());
        binding.sbvThread.setSubtitle(String.valueOf(configs.threads + 1));
        binding.cbvNestedVirtualization.setChecked(configs.nvirt);

        binding.tietMemory.setText(String.valueOf(configs.memory));

        binding.cbvBattery.setChecked(configs.battery);
    }

    private void save() {
        configs.nvirt = binding.cbvNestedVirtualization.isChecked();
        configs.memory = binding.tietMemory.getText() == null || binding.tietMemory.getText().toString().isEmpty() ? 0 : Integer.parseInt(binding.tietMemory.getText().toString());
        configs.battery = binding.cbvBattery.isChecked();
    }
}
