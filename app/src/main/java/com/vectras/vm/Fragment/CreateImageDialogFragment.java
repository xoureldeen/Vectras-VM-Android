package com.vectras.vm.Fragment;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.vectras.qemu.Config;
import com.vectras.vm.AppConfig;
import com.vectras.vm.CustomRomActivity;
import com.vectras.vm.R;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vterm.Terminal;

public class CreateImageDialogFragment extends DialogFragment {

    public boolean customRom = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_vhd, container, false);

        return view;
    }

    // If you want to style the dialog to have no title or to adjust the width, etc., override onCreateDialog.
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.MainDialogTheme);

        builder.setTitle("Create Image");

        View view = getActivity().getLayoutInflater().inflate(R.layout.create_vhd, null);

        TextInputEditText imageSize = view.findViewById(R.id.size);

        TextInputEditText imageName = view.findViewById(R.id.name);

        if (customRom)
            imageName.setText(CustomRomActivity.title.getText().toString());

        TextView createPath = view.findViewById(R.id.createPath);

        createPath.setText(FileUtils.getExternalFilesDirectory(getActivity()).getPath() + "/QCOW2/");

        if (customRom)
            createPath.append(CustomRomActivity.title.getText().toString() + ".qcow2");

        Button createQcow2Btn = view.findViewById(R.id.createQcow2Btn);

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isFilled(imageName) && isFilled(imageSize)) {
                    createQcow2Btn.setEnabled(true);
                } else {
                    createQcow2Btn.setEnabled(false);
                }
                createPath.setText(FileUtils.getExternalFilesDirectory(getActivity()).getPath() + "/QCOW2/" + imageName.getText().toString() + ".qcow2");
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFilled(imageName) && isFilled(imageSize))
                    createQcow2Btn.setEnabled(true);
                else
                    createQcow2Btn.setEnabled(false);
            }
        };
        imageName.addTextChangedListener(afterTextChangedListener);
        imageSize.addTextChangedListener(afterTextChangedListener);

        createQcow2Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Terminal vterm = new Terminal(getActivity());
                vterm.executeShellCommand("qemu-img create -f qcow2 " + FileUtils.getExternalFilesDirectory(getActivity()).getPath() + "/QCOW2/" + imageName.getText().toString() + ".qcow2 " +
                        imageSize.getText().toString() + "G", true, getActivity());
                if (customRom)
                    CustomRomActivity.drive.setText(FileUtils.getExternalFilesDirectory(getActivity()).getPath() + "/QCOW2/" + imageName.getText().toString() + ".qcow2");
            }
        });

        builder.setView(view);
        return builder.create();
    }

    private boolean isFilled(TextInputEditText TXT) {
        if (TXT.getText().toString().trim().length() > 0)
            return true;
        else
            return false;
    }
}
