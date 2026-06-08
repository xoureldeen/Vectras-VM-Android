package com.vectras.vm.main.core;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.vectras.vm.creator.VMCreatorActivity;
import com.vectras.vm.ExportRomActivity;
import com.vectras.vm.R;
import com.vectras.vm.VMManager;
import com.vectras.vm.main.vms.DataMainRoms;
import com.vectras.vm.manager.VmAudioManager;
import com.vectras.vm.manager.VmControllerDialog;
import com.vectras.vm.manager.VmFileManager;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.ProgressDialog;

import java.io.File;

public class RomOptionsDialog {
    public static void show(Activity activity, int position, DataMainRoms vmConfig) {
        if (VMManager.isVMRunning(activity, vmConfig.vmID)) {
            VmControllerDialog vmControllerDialog = new VmControllerDialog();
            vmControllerDialog.streamAudio = VmAudioManager.streamAudio;
            vmControllerDialog.position = position;
            vmControllerDialog.show(((FragmentActivity) activity).getSupportFragmentManager(), "VmControllerDialog");
        } else {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);

            bottomSheetDialog.setOnShowListener(d -> {
                BottomSheetBehavior<FrameLayout> behavior = bottomSheetDialog.getBehavior();
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            });

            View v = activity.getLayoutInflater().inflate(R.layout.rom_options_dialog, null);
            bottomSheetDialog.setContentView(v);

            ImageView thumbnail = v.findViewById(R.id.ivIcon);
            TextView name = v.findViewById(R.id.textName);
            TextView arch = v.findViewById(R.id.textArch);

            name.setText(vmConfig.itemName);
            arch.setText(vmConfig.itemArch);


            if (!vmConfig.itemIcon.isEmpty() && FileUtils.isFileExists(vmConfig.itemIcon)){
                Glide.with(activity.getApplicationContext())
                        .load(new File(vmConfig.itemIcon))
                        .placeholder(R.drawable.ic_computer_180dp_with_padding)
                        .error(R.drawable.ic_computer_180dp_with_padding)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(thumbnail);
            } else if (VmFileManager.isScreenshotPngExists(vmConfig.vmID)) {
                Glide.with(activity.getApplicationContext())
                        .load(new File(VmFileManager.getScreenshotPng(vmConfig.vmID )))
                        .placeholder(R.drawable.ic_computer_180dp_with_padding)
                        .error(R.drawable.ic_computer_180dp_with_padding)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(thumbnail);
            } else {
                VMManager.setIconWithName(thumbnail, vmConfig.itemName);
            }

            v.findViewById(R.id.btn_start).setOnClickListener(v4 -> {
                MainStartVM.startNow(activity, vmConfig);
                bottomSheetDialog.cancel();
            });

            v.findViewById(R.id.ln_edit).setOnClickListener(v3 -> {
                activity.startActivity(new Intent(activity, VMCreatorActivity.class).putExtra("POS", position).putExtra("MODIFY", true).putExtra("VMID", vmConfig.vmID));
                bottomSheetDialog.cancel();
            });

            v.findViewById(R.id.ln_export).setOnClickListener(v2 -> {
                Intent intent = new Intent();
                intent.setClass(activity, ExportRomActivity.class);
                intent.putExtra("POS", position);
                activity.startActivity(intent);
                bottomSheetDialog.cancel();
            });

            v.findViewById(R.id.ln_cleanup).setOnClickListener(v5 -> {
                ProgressDialog progressDialog = new ProgressDialog(activity);
                progressDialog.setText(activity.getString(R.string.just_a_sec));
                progressDialog.show();

                new Thread(() -> {
                    VmFileManager.removeTemp(activity, vmConfig.vmID);
                    activity.runOnUiThread(() -> {
                        progressDialog.reset();
                        Toast.makeText(activity, activity.getString(R.string.done), Toast.LENGTH_SHORT).show();
                        bottomSheetDialog.cancel();
                    });
                }).start();
            });

            v.findViewById(R.id.ln_remove).setOnClickListener(v1 -> {
                VMManager.deleteVMDialog(vmConfig.itemName, position, activity);
                bottomSheetDialog.cancel();
            });

            bottomSheetDialog.show();
        }
    }
}
