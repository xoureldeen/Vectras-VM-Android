package com.vectras.vm.main.core;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.FragmentActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.vectras.vm.creator.VMCreatorActivity;
import com.vectras.vm.ExportRomActivity;
import com.vectras.vm.R;
import com.vectras.vm.VMManager;
import com.vectras.vm.manager.VmAudioManager;
import com.vectras.vm.manager.VmControllerDialog;

public class RomOptionsDialog {
    public static void showNow(Activity activity, int position, String vmID, String vmName) {
        if (VMManager.isVMRunning(activity, vmID)) {
            VmControllerDialog vmControllerDialog = new VmControllerDialog();
            vmControllerDialog.streamAudio = VmAudioManager.streamAudio;
            vmControllerDialog.position = position;
            vmControllerDialog.show(((FragmentActivity) activity).getSupportFragmentManager(), "VmControllerDialog");
        } else {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
            View v = activity.getLayoutInflater().inflate(R.layout.rom_options_dialog, null);
            bottomSheetDialog.setContentView(v);

            Button modifyRomBtn = v.findViewById(R.id.modifyRomBtn);
            modifyRomBtn.setOnClickListener(v3 -> {
                activity.startActivity(new Intent(activity, VMCreatorActivity.class).putExtra("POS", position).putExtra("MODIFY", true).putExtra("VMID", vmID));
                bottomSheetDialog.cancel();
            });

            Button exportRomBtn = v.findViewById(R.id.exportRomBtn);
            exportRomBtn.setOnClickListener(v2 -> {
                Intent intent = new Intent();
                intent.setClass(activity, ExportRomActivity.class);
                intent.putExtra("POS", position);
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
}
