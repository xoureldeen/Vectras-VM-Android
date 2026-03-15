package com.vectras.vm.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.vectras.vm.R;
import com.vectras.vm.databinding.DialogProgressStyleBinding;

public class ProgressDialog {

    private final Context context;
    private AlertDialog dialog;
    private DialogProgressStyleBinding binding;
    private String text;
    private Integer progress;
    private Integer max;
    private Boolean isIndeterminate;

    public ProgressDialog(Context context) {
        this.context = context;
    }

    public void show() {
        if (DialogUtils.isAllowShow(context)) {
            if (dialog != null && dialog.isShowing()) return;

            binding = DialogProgressStyleBinding.inflate(LayoutInflater.from(context));

            if (text != null) binding.progressText.setText(text);

            if (progress != null) {
                binding.progressBar.setIndeterminate(false);

                if (Build.VERSION.SDK_INT >= 24) {
                    binding.progressBar.setProgress(progress, true);
                } else {
                    binding.progressBar.setProgress(progress);
                }
            }

            if (max != null) binding.progressBar.setMax(max);

            if (isIndeterminate != null) binding.progressBar.setIndeterminate(isIndeterminate);

            dialog = new MaterialAlertDialogBuilder(context, R.style.CenteredDialogTheme)
                    .setView(binding.getRoot())
                    .setCancelable(false)
                    .create();
            dialog.show();
        }
    }

    public void setText(String text) {
        this.text = text;
        if (binding != null) binding.progressText.setText(text);
    }

    public void setProgress(int progress) {
        this.progress = Math.max(progress, 0);
        if (binding != null) {
            binding.progressBar.setIndeterminate(false);

            if (Build.VERSION.SDK_INT >= 24) {
                binding.progressBar.setProgress(this.progress, true);
            } else {
                binding.progressBar.setProgress(this.progress);
            }
        }
    }

    public void setMax(int max) {
        this.max = Math.max(max, 0);
        if (binding != null) binding.progressBar.setMax(this.max);
    }

    public void setIndeterminate(boolean isIndeterminate) {
        this.isIndeterminate = isIndeterminate;
        if (binding != null) binding.progressBar.setIndeterminate(isIndeterminate);
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    public void dismiss() {
        DialogUtils.safeDismiss((Activity) context, dialog);
        binding = null;
        dialog = null;
    }

    public void reset() {
        dismiss();
        text = null;
        progress = null;
        max = null;
        isIndeterminate = null;
    }
}
