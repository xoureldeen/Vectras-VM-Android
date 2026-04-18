package com.vectras.vm.manager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.vectras.vm.R;
import com.vectras.vm.databinding.DialogChangeADeviceBinding;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;

import java.io.File;
import java.util.Objects;

public class ChangeDeviceDialog extends DialogFragment {
    private DialogChangeADeviceBinding binding;
    private String deviceId = "";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        binding = DialogChangeADeviceBinding.inflate(getLayoutInflater());

        AlertDialog dialog = new AlertDialog.Builder(requireActivity()).create();
        dialog.setView(binding.getRoot());

        binding.btnClose.setOnClickListener(v -> dismiss());
        binding.btnChange.setOnClickListener(v -> {
            deviceId = binding.edDeviceId.getText().toString();
            filePicker.launch("*/*");
        });
        binding.btnEject.setOnClickListener(v -> startEject(binding.edDeviceId.getText().toString()));

        binding.edDeviceId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                binding.btnEject.setEnabled(!s.toString().isEmpty());
                binding.btnChange.setEnabled(!s.toString().isEmpty());
            }
        });

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }

        showKeyBoard();
    }

    private void showKeyBoard() {
        Dialog dialog = getDialog();
        if (dialog != null && isAdded()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isAdded()) return;
                binding.edDeviceId.requestFocus();
                binding.edDeviceId.setSelection(Objects.requireNonNull(binding.edDeviceId.getText()).length());
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(binding.edDeviceId, InputMethodManager.SHOW_IMPLICIT);
            }, 200);
        }
    }

    private void startChange(String id, String filePath) {
        KeyListener originalKeyListener = binding.edDeviceId.getKeyListener();

        binding.edDeviceId.setKeyListener(null);
        binding.btnEject.setEnabled(false);
        binding.btnChange.setEnabled(false);
        binding.circularProgressIndicator.setVisibility(View.VISIBLE);

        new Thread(() -> {
            boolean isChanged = QmpSender.isSuccess(QmpSender.changeDevice(id, filePath));

            new Handler(Looper.getMainLooper()).post(() -> {

                binding.edDeviceId.setKeyListener(originalKeyListener);
                binding.btnEject.setEnabled(true);
                binding.btnChange.setEnabled(true);
                binding.circularProgressIndicator.setVisibility(View.GONE);

                Toast.makeText(requireActivity().getApplicationContext(), getString(isChanged ? R.string.changed : R.string.change_failed), Toast.LENGTH_SHORT).show();

                if (isChanged) dismiss();
            });
        }).start();
    }

    private void startEject(String id) {
        KeyListener originalKeyListener = binding.edDeviceId.getKeyListener();

        binding.edDeviceId.setKeyListener(null);
        binding.btnEject.setEnabled(false);
        binding.btnChange.setEnabled(false);
        binding.circularProgressIndicator.setVisibility(View.VISIBLE);

        new Thread(() -> {
            boolean isEjected = QmpSender.isSuccess(QmpSender.ejectDevice(id));

            new Handler(Looper.getMainLooper()).post(() -> {
                binding.edDeviceId.setKeyListener(originalKeyListener);
                binding.btnEject.setEnabled(true);
                binding.btnChange.setEnabled(true);
                binding.circularProgressIndicator.setVisibility(View.GONE);

                Toast.makeText(requireActivity().getApplicationContext(), getString(isEjected ? R.string.ejected : R.string.eject_failed), Toast.LENGTH_SHORT).show();

                if (isEjected) dismiss();
            });
        }).start();
    }

    private final ActivityResultLauncher<String> filePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        File selectedFilePath = new File(Objects.requireNonNull(FileUtils.getPath(requireActivity(), uri)));
                        startChange(deviceId, selectedFilePath.getAbsolutePath());
                    } catch (Exception e) {
                        DialogUtils.oneDialog(requireActivity(),
                                getString(R.string.oops),
                                getString(R.string.invalid_file_path_content),
                                getString(R.string.ok),
                                true,
                                R.drawable.error_96px,
                                true,
                                null,
                                null
                        );

                        dismiss();
                    }
                }
            });

}
