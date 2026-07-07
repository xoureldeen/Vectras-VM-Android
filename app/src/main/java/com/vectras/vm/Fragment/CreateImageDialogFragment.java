package com.vectras.vm.Fragment;

import android.annotation.SuppressLint;
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
import com.google.android.material.textfield.TextInputLayout;
import com.vectras.vm.AppConfig;
import com.vectras.vm.R;
import com.vectras.vm.manager.VmFileManager;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.ProgressDialog;
import com.vectras.vterm.Terminal2;

import java.io.File;
import java.util.Objects;

public class CreateImageDialogFragment extends DialogFragment {

    public boolean customRom = false;

    public String folder = AppConfig.maindirpath;
    public String filename = "disk";
    public TextInputEditText drive;
    public TextInputLayout driveLayout;
    public boolean isMarkPendingAdd;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.create_vhd, container, false);
    }

    // If you want to style the dialog to have no title or to adjust the width, etc., override onCreateDialog.
    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity(), R.style.MainDialogTheme);

        View view = requireActivity().getLayoutInflater().inflate(R.layout.create_vhd, null);

        TextInputEditText imageSize = view.findViewById(R.id.size);

        TextInputEditText imageName = view.findViewById(R.id.name);

        if (customRom)
            imageName.setText(filename);

        TextView createPath = view.findViewById(R.id.createPath);

        createPath.setText(requireContext().getString(R.string.it_will_be_created_in) + ": " + folder);

        if (customRom)
            createPath.append(filename + ".qcow2");

        Button createQcow2Btn = view.findViewById(R.id.createQcow2Btn);

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                createQcow2Btn.setEnabled(isFilled(imageName) && isFilled(imageSize));
                createPath.setText(requireContext().getString(R.string.it_will_be_created_in) + ": " + folder + Objects.requireNonNull(imageName.getText()) + ".qcow2");
            }

            @Override
            public void afterTextChanged(Editable s) {
                createQcow2Btn.setEnabled(isFilled(imageName) && isFilled(imageSize));
            }
        };
        imageName.addTextChangedListener(afterTextChangedListener);
        imageSize.addTextChangedListener(afterTextChangedListener);

        createQcow2Btn.setOnClickListener(v -> {
            File vDir = new File(folder);
            if (!vDir.exists()) {
                vDir.mkdirs();
            }

            ProgressDialog progressDialog = new ProgressDialog(requireActivity());
            progressDialog.setText(getString(R.string.just_a_sec));
            progressDialog.show();

            new Thread(() -> {
                String result = new Terminal2(requireContext()).executeOnThisThread("qemu-img create -f qcow2 \"" + folder + Objects.requireNonNull(imageName.getText()) + ".qcow2\" " +
                        Objects.requireNonNull(imageSize.getText()) + "G").trim();

                if (isMarkPendingAdd) VmFileManager.markPendingAdd(folder + Objects.requireNonNull(imageName.getText()) + ".qcow2");
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (result.endsWith("_bits=16")) {
                        if (customRom) {
                            if(drive != null)
                                drive.setText(folder + imageName.getText().toString() + ".qcow2");

                            if(driveLayout != null)
                                driveLayout.setEndIconDrawable(R.drawable.more_vert_24px);
                        }

                        dismiss();
                    } else {
                        DialogUtils.oopsDialog(requireActivity(), getString(R.string.something_went_wrong) + "\n\n" + result);
                    }
                });
            }).start();
        });

        builder.setView(view);
        return builder.create();
    }

    private boolean isFilled(TextInputEditText TXT) {
        return !Objects.requireNonNull(TXT.getText()).toString().trim().isEmpty();
    }
}
