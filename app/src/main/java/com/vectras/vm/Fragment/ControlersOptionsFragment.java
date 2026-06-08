package com.vectras.vm.Fragment;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.view.View;
import android.view.Window;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.MainVNCActivity;
import com.vectras.vm.R;
import com.vectras.vm.databinding.ControlsFragmentBinding;
import com.vectras.vm.x11.X11Activity;

public class ControlersOptionsFragment extends DialogFragment {
    public ControlsFragmentBinding binding;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog alertDialog = new Dialog(getActivity(), R.style.MainDialogTheme);
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        alertDialog.setContentView(R.layout.fragment_controlers_options);
        alertDialog.findViewById(R.id.gamepadBtn).setOnClickListener(v -> {
            MainSettingsManager.setControlMode(getActivity(), "G");
            binding.desktop.setVisibility(View.GONE);
            binding.gamepad.setVisibility(View.VISIBLE);
            alertDialog.cancel();
        });
        alertDialog.findViewById(R.id.desktopBtn).setOnClickListener(v -> {
            MainSettingsManager.setControlMode(getActivity(), "D");
            binding.desktop.setVisibility(View.VISIBLE);
            binding.gamepad.setVisibility(View.GONE);
            alertDialog.cancel();
        });
        alertDialog.findViewById(R.id.hideBtn).setOnClickListener(v -> {
            MainSettingsManager.setControlMode(getActivity(), "H");
            binding.desktop.setVisibility(View.GONE);
            binding.gamepad.setVisibility(View.GONE);
            alertDialog.cancel();
        });

        if (binding != null) {
            alertDialog.findViewById(R.id.hide_all).setVisibility(View.VISIBLE);

            alertDialog.findViewById(R.id.hide_all).setOnClickListener(v -> {
                binding.mainControl.setVisibility(View.GONE);
                alertDialog.cancel();
            });
        }
        alertDialog.show();
        return alertDialog;
    }
}