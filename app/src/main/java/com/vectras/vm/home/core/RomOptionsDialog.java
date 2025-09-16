package com.vectras.vm.home.core;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.vectras.vm.CustomRomActivity;
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
            CustomRomActivity.current = data.get(position);
            VMManager.setArch(arch, activity);
            activity.startActivity(new Intent(activity, CustomRomActivity.class).putExtra("POS", position).putExtra("MODIFY", true).putExtra("VMID", vmID));
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
        bottomSheetDialog.show();
    }
}
