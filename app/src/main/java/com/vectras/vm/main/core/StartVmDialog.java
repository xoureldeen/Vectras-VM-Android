package com.vectras.vm.main.core;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.vectras.qemu.Config;
import com.vectras.vm.R;
import com.vectras.vm.VMManager;
import com.vectras.vm.manager.QmpSender;
import com.vectras.vm.manager.VmFileManager;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;

import java.io.File;

public class StartVmDialog {
    Activity activity;

    AlertDialog progressDialog;
    View progressView;
    TextView tvVMName;
    TextView vmBootNote;

    String vmName;
    String vmId;
    String thumbnailFile;

    boolean isShuttingDown;

    public interface StartVmDialogCallBack {
        void onStop();
    }

    public StartVmDialog(Activity activity, String vmId, String vmName, String thumbnailFile) {
        this.activity = activity;
        this.vmName = vmName;
        this.vmId = vmId;
        this.thumbnailFile = thumbnailFile;
    }

    public void show(StartVmDialogCallBack callBack) {
        progressView = LayoutInflater.from(activity).inflate(R.layout.dialog_start_vm, null);
        tvVMName = progressView.findViewById(R.id.vm_name);
        vmBootNote = progressView.findViewById(R.id.vm_boot_note);
        tvVMName.setText(vmName);

        if (thumbnailFile != null) {
            ImageView ivThumbnail = progressView.findViewById(R.id.iv_thumbnail);

            if (!thumbnailFile.isEmpty() && FileUtils.isFileExists(thumbnailFile)) {
                Glide.with(activity.getApplicationContext())
                        .load(new File(thumbnailFile))
                        .placeholder(R.drawable.ic_computer_180dp_with_padding)
                        .error(R.drawable.ic_computer_180dp_with_padding)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(ivThumbnail);
            } else if (VmFileManager.isScreenshotPngExists(vmId)) {
                Glide.with(activity.getApplicationContext())
                        .load(new File(VmFileManager.getScreenshotPng(vmId)))
                        .placeholder(R.drawable.ic_computer_180dp_with_padding)
                        .error(R.drawable.ic_computer_180dp_with_padding)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(ivThumbnail);
            } else {
                VMManager.setIconWithName(ivThumbnail, vmName);
            }
        }

        ImageView ivStop = progressView.findViewById(R.id.ivStop);
        ivStop.setOnClickListener(v -> {
            isShuttingDown = true;
            ivStop.setVisibility(View.GONE);
            vmBootNote.setText(R.string.shutting_down);
            new Thread(() -> {
                QmpSender.shutdown();
                new Handler(Looper.getMainLooper()).post(callBack::onStop);

                final int MAX_TRY = 10;
                int triedCount = 0;

                boolean isVmRuning = VMManager.isVMRunning(activity, vmId);

                if (!isVmRuning) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        isShuttingDown = false;
                        dismiss();
                    });
                } else {
                    while (true) {
                        Config.vmID = vmId;

                        isVmRuning = VMManager.isVMRunning(activity, vmId);

                        QmpSender.shutdown();

                        if (!isVmRuning || VMManager.isQemuStopedWithError || triedCount >= MAX_TRY) {
                            if (triedCount < MAX_TRY) FileUtils.delete(Config.getLocalQMPSocketPath(vmId));

                            new Handler(Looper.getMainLooper()).post(() -> {
                                isShuttingDown = false;
                                dismiss();
                            });

                            break;
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {

                        }

                        triedCount++;
                    }
                }
            }).start();
        });

        progressDialog = new MaterialAlertDialogBuilder(activity, R.style.CenteredDialogTheme)
                .setView(progressView)
                .setCancelable(false)
                .create();

        if (!activity.isFinishing() && !activity.isDestroyed()) progressDialog.show();
    }

    public void setStatus(String status) {
        vmBootNote.setText(status);
    }

    public void setStatus(int resourceId) {
        vmBootNote.setText(resourceId);
    }

    public boolean isShowing() {
        return progressDialog != null && progressDialog.isShowing();
    }

    public void dismiss() {
        if (isShuttingDown) return;

        DialogUtils.safeDismiss(activity, progressDialog);
        progressDialog = null;
        progressView = null;
    }
}
