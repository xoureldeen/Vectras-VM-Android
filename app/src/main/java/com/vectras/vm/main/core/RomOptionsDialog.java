package com.vectras.vm.main.core;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.vectras.vm.VMCreatorActivity;
import com.vectras.vm.ExportRomActivity;
import com.vectras.vm.MainRoms.DataMainRoms;
import com.vectras.vm.R;
import com.vectras.vm.VMManager;

import java.util.List;

public class RomOptionsDialog {
    public static void showNow(Activity activity, List<DataMainRoms> data, int position, String vmID, String vmName, String arch) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
        View v = activity.getLayoutInflater().inflate(R.layout.rom_options_dialog, null);
        bottomSheetDialog.setContentView(v);

        Button modifyRomBtn = v.findViewById(R.id.modifyRomBtn);
        modifyRomBtn.setOnClickListener(v3 -> {
            VMCreatorActivity.current = data.get(position);
            VMManager.setArch(arch, activity);
            activity.startActivity(new Intent(activity, VMCreatorActivity.class).putExtra("POS", position).putExtra("MODIFY", true).putExtra("VMID", vmID));
            bottomSheetDialog.cancel();
        });

        Button exportRomBtn = v.findViewById(R.id.exportRomBtn);
        exportRomBtn.setOnClickListener(v2 -> {
            ExportRomActivity.pendingPosition = position;
            Intent intent = new Intent();
            intent.setClass(activity, ExportRomActivity.class);
            activity.startActivity(intent);
            bottomSheetDialog.cancel();
        });

        Button removeRomBtn = v.findViewById(R.id.removeRomBtn);
        removeRomBtn.setOnClickListener(v1 -> {
            VMManager.deleteVMDialog(vmName, position, activity);
            bottomSheetDialog.cancel();
        });

        if (VMManager.isVMRunning(activity, vmID)) {
            removeRomBtn.setVisibility(View.GONE);
            Button deviceManagerBtn = v.findViewById(R.id.deviceManagerBtn);
            deviceManagerBtn.setOnClickListener(v1 -> {
                VMManager.showChangeRemovableDevicesDialog(activity, null);
                bottomSheetDialog.cancel();
            });
            deviceManagerBtn.setVisibility(View.VISIBLE);
        }
        bottomSheetDialog.show();
    }
}
