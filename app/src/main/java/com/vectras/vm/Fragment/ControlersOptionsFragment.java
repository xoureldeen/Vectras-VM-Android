package com.vectras.vm.Fragment;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.R;
public class ControlersOptionsFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog alertDialog = new Dialog(getActivity(), R.style.MainDialogTheme);
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        alertDialog.setContentView(R.layout.fragment_controlers_options);
        alertDialog.findViewById(R.id.gamepadBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainSettingsManager.setControlMode(getActivity(), "G");
                alertDialog.cancel();
            }
        });
        alertDialog.findViewById(R.id.desktopBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainSettingsManager.setControlMode(getActivity(), "D");
                alertDialog.cancel();
            }
        });
        alertDialog.findViewById(R.id.hideBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainSettingsManager.setControlMode(getActivity(), "H");
                alertDialog.cancel();
            }
        });
        alertDialog.show();
        return alertDialog;
    }
}