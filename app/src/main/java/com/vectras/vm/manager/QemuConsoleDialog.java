package com.vectras.vm.manager;

import android.annotation.SuppressLint;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.vectras.vm.databinding.DialogQemuConsoleBinding;
import com.vectras.vm.utils.DialogUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QemuConsoleDialog extends DialogFragment {

    private DialogQemuConsoleBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        binding = DialogQemuConsoleBinding.inflate(getLayoutInflater());

        AlertDialog dialog = new AlertDialog.Builder(requireActivity()).create();
        dialog.setView(binding.getRoot());

        binding.edEnterCommand.addTextChangedListener(new TextWatcher() {
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
                binding.btnSend.setVisibility(s.toString().isEmpty() ? View.GONE : View.VISIBLE);
            }
        });

        binding.btnClean.setOnClickListener(v -> binding.tvConsole.setText("(qemu)"));
        binding.btnClose.setOnClickListener(v -> dismiss());
        binding.btnSend.setOnClickListener(v -> send(binding.edEnterCommand.getText().toString()));

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }


    private void showKeyBoard() {
        Dialog dialog = getDialog();
        if (dialog != null && isAdded()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isAdded()) return;
                binding.edEnterCommand.requestFocus();
                binding.edEnterCommand.setSelection(binding.edEnterCommand.getText().length());
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(binding.edEnterCommand, InputMethodManager.SHOW_IMPLICIT);
            }, 200);
        }
    }

    private void send(String command) {
        KeyListener originalKeyListener = binding.edEnterCommand.getKeyListener();

        binding.btnSend.setVisibility(View.GONE);
        binding.circularProgressIndicator.setVisibility(View.VISIBLE);
        binding.edEnterCommand.setKeyListener(null);
        binding.edEnterCommand.setAlpha(0.5f);
        updateConsole(true, command);

        executor.execute(() -> {
            String response = QmpSender.send(command);
            requireActivity().runOnUiThread(() -> {

                if (command.trim().equals("quit")) {
                    if (DialogUtils.isSafeDismiss(requireActivity(), (AlertDialog) getDialog())) dismiss();
                    return;
                }

                binding.btnSend.setVisibility(View.VISIBLE);
                binding.circularProgressIndicator.setVisibility(View.GONE);
                binding.edEnterCommand.setKeyListener(originalKeyListener);
                binding.edEnterCommand.setAlpha(1f);
                if (!response.equals(QmpSender.SEND_FAILED_MESSAGE)) binding.edEnterCommand.setText("");
                updateConsole(false, response);
            });
        });
    }

    private void updateConsole(boolean isUserCommand, String newText) {
        String newHistory = binding.tvConsole.getText().toString();

        if (isUserCommand) {
            newHistory += " " + newText + "\n";
        } else {
            newHistory += newText + "\n(qemu)";
        }

        binding.tvConsole.setText(newHistory);

        binding.nsvConsole.post(() -> {
                binding.nsvConsole.fullScroll(View.FOCUS_DOWN);
                if (!isUserCommand) binding.edEnterCommand.requestFocus();
        });
    }
}
