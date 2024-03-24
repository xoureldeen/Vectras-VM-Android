package com.vectras.vm.Fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.MainVNCActivity;
import com.vectras.vm.R;

public class UiSettingsFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_ui_settings, container, false);
        TextView btnMouseMode = v.findViewById(R.id.btnMouseMode);
        btnMouseMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainSettingsManager.getVmUi(getActivity()).equals("SDL")) {
                    //MainSDLActivity.activity.onMouseMode();
                } else if (MainSettingsManager.getVmUi(getActivity()).equals("VNC")) {
                    MainVNCActivity.activity.onMouseMode();
                }
            }
        });
        // Inflate the layout for this fragment
        return v;
    }
}