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
                if (MainSettingsManager.getVmUi(getActivity()).equals("SDL")) {
                    //MainSDLActivity.desktop.setVisibility(View.GONE);
                    //MainSDLActivity.gamepad.setVisibility(View.VISIBLE);
                } else if (MainSettingsManager.getVmUi(getActivity()).equals("VNC")) {
                    MainVNCActivity.desktop.setVisibility(View.GONE);
                    MainVNCActivity.gamepad.setVisibility(View.VISIBLE);
                }
                alertDialog.cancel();
            }
        });
        alertDialog.findViewById(R.id.desktopBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainSettingsManager.setControlMode(getActivity(), "D");
                if (MainSettingsManager.getVmUi(getActivity()).equals("SDL")) {
                    //MainSDLActivity.desktop.setVisibility(View.VISIBLE);
                    //MainSDLActivity.gamepad.setVisibility(View.GONE);
                } else if (MainSettingsManager.getVmUi(getActivity()).equals("VNC")) {
                    MainVNCActivity.desktop.setVisibility(View.VISIBLE);
                    MainVNCActivity.gamepad.setVisibility(View.GONE);
                }
                alertDialog.cancel();
            }
        });
        alertDialog.findViewById(R.id.hideBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainSettingsManager.setControlMode(getActivity(), "H");
                if (MainSettingsManager.getVmUi(getActivity()).equals("SDL")) {
                    //MainSDLActivity.desktop.setVisibility(View.GONE);
                    //MainSDLActivity.gamepad.setVisibility(View.GONE);
                } else if (MainSettingsManager.getVmUi(getActivity()).equals("VNC")) {
                    MainVNCActivity.desktop.setVisibility(View.GONE);
                    MainVNCActivity.gamepad.setVisibility(View.GONE);
                }
                alertDialog.cancel();
            }
        });
        alertDialog.show();
        return alertDialog;
    }
}