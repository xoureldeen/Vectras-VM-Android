package com.vectras.vm.creator.editor;

import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.vectras.vm.R;
import com.vectras.vm.creator.utils.EditorUtils;
import com.vectras.vm.creator.utils.VMCreatorSelector;
import com.vectras.vm.databinding.CreatorNetworkDialogBinding;
import com.vectras.vm.main.vms.DataMainRoms;
import com.vectras.vm.utils.DialogUtils;

import java.util.Objects;

public class NetworkConfigsDialog extends BottomSheetDialogFragment {
    final String TAG = "NetworkConfigsDialog";

    String vmId;
    DataMainRoms configs;

    boolean isSave = true;

    public void setConfigs(DataMainRoms configs) {
        this.configs = configs;
        if (configs != null) {
            vmId = configs.vmID;
        }
    }

    CreatorNetworkDialogBinding binding;

    @NonNull
    @Override
    public BottomSheetDialog onCreateDialog(Bundle savedInstanceState) {
        // This can happen after the app is freed from memory and then reopened.
        if (configs == null) {
            isSave = false;
            DialogUtils.oopsDialog(requireActivity(), getString(R.string.something_went_wrong));
            dismiss();
            return EditorUtils.getDummyDialog(requireActivity());
        }

        binding = CreatorNetworkDialogBinding.inflate(getLayoutInflater());

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

    NetworkConfigsDialogCallback callback;

    public void setOnDismiss(NetworkConfigsDialogCallback callback) {
        this.callback = callback;
    }

    public interface NetworkConfigsDialogCallback {
        void onDismiss(DataMainRoms configs);
    }

    public void onDismiss(@NonNull DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        if (callback != null && isSave) {
            save();
            callback.onDismiss(configs);
        }
    }

    private void initialize() {
        if (!isAdded()) return;

        binding.sbvCardType.setOnClickListener(v -> VMCreatorSelector.networkCard(requireActivity(), configs.networkCard, ((position, name, value) -> {
            configs.networkCard = position;
            binding.sbvCardType.setSubtitle(name);
        })));

        load();
    }

    private void load() {
        if (!isAdded()) return;

        binding.sbvCardType.setSubtitle(Objects.requireNonNull(VMCreatorSelector.getNetworkCard(requireActivity(), configs.networkCard).get("name")).toString());
    }

    private void save() {
        // It may be used in subsequent versions.
    }
}