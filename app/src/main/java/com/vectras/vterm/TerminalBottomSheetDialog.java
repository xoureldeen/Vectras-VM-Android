package com.vectras.vterm;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.vectras.vm.R;
import com.vectras.vterm.view.ZoomableTextView;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Objects;

public class TerminalBottomSheetDialog {
    final String TAG = "TerminalBottomSheetDialog";

    private final ZoomableTextView terminalOutput;
    private final EditText commandInput;
    private final View view;
    private final Activity activity;
    private final BottomSheetDialog bottomSheetDialog;
    LinearLayout inputContainer;
    boolean isAllowAddToResultCommand = true;

    Terminal2 terminal2;

    public TerminalBottomSheetDialog(Activity activity) {
        this.activity = activity;

        bottomSheetDialog = new BottomSheetDialog(activity);
        view = activity.getLayoutInflater().inflate(R.layout.terminal_bottom_sheet, null);
        bottomSheetDialog.setContentView(view);

        terminalOutput = view.findViewById(R.id.tvTerminalOutput);
        commandInput = view.findViewById(R.id.etCommandInput);
        inputContainer = view.findViewById(R.id.ln_input);

        if (!checkInstallation()) {
            inputContainer.setVisibility(View.GONE);
            appendTextAndScroll(activity.getString(R.string.the_system_has_not_been_installed));
            return;
        }

        terminal2 = new Terminal2(activity);

        TextView tvPrompt = view.findViewById(R.id.tvPrompt);
        updateUserPrompt(tvPrompt);

        // Show the keyboard
        forcusCommandInput();

        // Whenever you modify the text of the EditText, do the following to ensure the cursor is at the end:
        commandInput.setSelection(commandInput.getText().length());

        // when user click terminal view will open keyboard
        terminalOutput.setOnClickListener(view -> {
            forcusCommandInput();
        });
        // Configure the editor to handle the "Done" action on the soft keyboard
        commandInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                executeShellCommand(commandInput.getText().toString());
                commandInput.setText("");
                commandInput.requestFocus();
                return true;
            }
            return false;
        });

        commandInput.setOnKeyListener((v, keyCode, event) -> {
            // If the event is a key-down event on the "enter" button
            if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                if (activity.isFinishing() || activity.isDestroyed()) return true;

                activity.runOnUiThread(() -> appendTextAndScroll(commandInput.getText().toString() + "\n"));
                executeShellCommand(commandInput.getText().toString());
                commandInput.setText("");
                // Request focus again
                activity.runOnUiThread(commandInput::requestFocus);
                return true;
            }
            return false;
        });
    }

    public void showVterm() {
        bottomSheetDialog.show();
    }

    private void updateUserPrompt(TextView promptView) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        // Run this in a separate thread to not block UI
        new Thread(() -> {
            String username = terminal2.getUsername();
            // Update the prompt on the UI thread
            String finalUsername = username != null ? username : "root";
            activity.runOnUiThread(() -> promptView.setText(finalUsername.concat("@localhost:~$ ")));
        }).start();
    }

    // Function to append text and automatically scroll to bottom
    private void appendTextAndScroll(String textToAdd) {
        ScrollView scrollView = view.findViewById(R.id.scrollView);

        // Update the text
        if (textToAdd.contains("@localhost:~$ exit")) {
            bottomSheetDialog.dismiss();
        } else if (textToAdd.contains("@localhost:~$ clear")) {
            isAllowAddToResultCommand = false;
            terminalOutput.setText("");
            terminalOutput.setVisibility(View.GONE);
        } else {
            if (isAllowAddToResultCommand) {
                terminalOutput.append(textToAdd);
            } else {
                isAllowAddToResultCommand = true;
            }
        }

        // Scroll to the bottom
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && Objects.requireNonNull(inetAddress.getHostAddress()).contains(".")) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, "getLocalIpAddress:", ex);
        }
        return null;
    }

    // Method to execute the shell command
    public void executeShellCommand(String userCommand) {
        inputContainer.setVisibility(View.GONE);
        terminal2.execute(userCommand, new Terminal2.Terminal2Callback() {
            @Override
            public void onRunning(String command, String newLine) {
                if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

                activity.runOnUiThread(() -> {
                    appendTextAndScroll(newLine);
                    inputContainer.setVisibility(View.GONE);
                });
            }

            @Override
            public void onFinished(String command, String log, int status) {
                if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

                activity.runOnUiThread(() -> {
                    inputContainer.setVisibility(View.VISIBLE);
                    commandInput.requestFocus();
                });
            }

            @Override
            public void onError(String command, Exception exception) {
                if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

                activity.runOnUiThread(() -> appendTextAndScroll(exception.toString()));
            }
        });
    }

    private boolean checkInstallation() {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return false;

        String filesDir = activity.getFilesDir().getAbsolutePath();
        File distro = new File(filesDir, "distro");
        return distro.exists();
    }

    private void forcusCommandInput() {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        commandInput.post(() -> {
            commandInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(commandInput, InputMethodManager.SHOW_IMPLICIT);
        });
    }
}