package com.vectras.vterm;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.vectras.vm.R;
import com.vectras.vterm.view.ZoomableTextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class TerminalBottomSheetDialog {
    private ZoomableTextView terminalOutput;
    private EditText commandInput;
    private View view;
    private Activity activity;
    private BottomSheetDialog bottomSheetDialog;

    public TerminalBottomSheetDialog(Activity activity) {
        this.activity = activity;

        bottomSheetDialog = new BottomSheetDialog(activity);
        view = activity.getLayoutInflater().inflate(R.layout.terminal_bottom_sheet, null);
        bottomSheetDialog.setContentView(view);

        terminalOutput = view.findViewById(R.id.tvTerminalOutput);
        commandInput = view.findViewById(R.id.etCommandInput);

        TextView tvPrompt = view.findViewById(R.id.tvPrompt);
        updateUserPrompt(tvPrompt);

        // Show the keyboard
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(commandInput, InputMethodManager.SHOW_IMPLICIT);

        commandInput.requestFocus();

        // Whenever you modify the text of the EditText, do the following to ensure the cursor is at the end:
        commandInput.setSelection(commandInput.getText().length());

        // when user click terminal view will open keyboard
        terminalOutput.setOnClickListener(view -> {
            // Request focus for the EditText
            commandInput.requestFocus();

            // Show the keyboard
            InputMethodManager imm1 = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm1.showSoftInput(commandInput, InputMethodManager.SHOW_IMPLICIT);
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

        commandInput.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    activity.runOnUiThread(() -> appendTextAndScroll(commandInput.getText().toString() + "\n"));
                    executeShellCommand(commandInput.getText().toString());
                    commandInput.setText("");
                    activity.runOnUiThread(() -> {
                        commandInput.requestFocus(); // Request focus again
                    });
                    return true;
                }
                return false;
            }
        });
    }

    public void showVterm() {
        bottomSheetDialog.show();
    }

    private void updateUserPrompt(TextView promptView) {
        // Run this in a separate thread to not block UI
        new Thread(() -> {
            String username = null;
            // Update the prompt on the UI thread
            String finalUsername = username != null ? username : "root";
            activity.runOnUiThread(() -> {
                promptView.setText(finalUsername + "@localhost:~$ ");
            });
        }).start();
    }

    // Function to append text and automatically scroll to bottom
    private void appendTextAndScroll(String textToAdd) {
        ScrollView scrollView = view.findViewById(R.id.scrollView);

        // Update the text
        terminalOutput.append(textToAdd);

        // Scroll to the bottom
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.getHostAddress().toString().contains(".")) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // Method to execute the shell command
    public void executeShellCommand(String userCommand) {
        if (checkInstallation())
            new Thread(() -> {
                try {
                    // Setup the process builder to start PRoot with environmental variables and commands
                    ProcessBuilder processBuilder = new ProcessBuilder();

                    // Adjust these environment variables as necessary for your app
                    String filesDir = activity.getFilesDir().getAbsolutePath();
                    String nativeLibDir = activity.getApplicationInfo().nativeLibraryDir;

                    File tmpDir = new File(activity.getFilesDir(), "tmp");

                    // Setup environment for the PRoot process
                    processBuilder.environment().put("PROOT_TMP_DIR", tmpDir.getAbsolutePath());
                    processBuilder.environment().put("PROOT_LOADER", nativeLibDir + "/libproot-loader.so");
                    processBuilder.environment().put("PROOT_LOADER_32", nativeLibDir + "/libproot-loader32.so");

                    processBuilder.environment().put("HOME", "/root");
                    processBuilder.environment().put("USER", "root");
                    processBuilder.environment().put("PATH", "/bin:/usr/bin:/sbin:/usr/sbin");
                    processBuilder.environment().put("TERM", "xterm-256color");
                    processBuilder.environment().put("TMPDIR", tmpDir.getAbsolutePath());
                    processBuilder.environment().put("SHELL", "/bin/sh");
                    processBuilder.environment().put("DISPLAY", getLocalIpAddress()+":1");
                    processBuilder.environment().put("PULSE_SERVER", "/run/pulse/native");
                    processBuilder.environment().put("XDG_RUNTIME_DIR", "/run");
                    processBuilder.environment().put("QEMU_AUDIO_DRV", "sdl");
                    processBuilder.environment().put("SDL_VIDEODRIVER", "x11");

                    String[] prootCommand = {
                            nativeLibDir + "/libproot.so", // PRoot binary path
                            "--kill-on-exit",
                            "--rootfs=" + filesDir + "/distro", // Path to the rootfs
                            "--bind=/dev",
                            "--bind=/proc",
                            "--bind=/sys",
                            "--bind=/sdcard",
                            "--bind=/storage",
                            "--bind=/data",
                            "--cwd=/root",
                            "/bin/sh",
                            "--login" // The shell to execute inside PRoot
                    };

                    processBuilder.command(prootCommand);
                    Process process = processBuilder.start();
                    // Get the input and output streams of the process
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                    // Send user command to PRoot
                    writer.write(userCommand);
                    writer.newLine();
                    writer.flush();
                    writer.close();

                    // Read the input stream for the output of the command
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String outputLine = line;
                        activity.runOnUiThread(() -> appendTextAndScroll(outputLine + "\n"));
                    }

                    // Read any errors from the error stream
                    while ((line = errorReader.readLine()) != null) {
                        final String errorLine = line;
                        activity.runOnUiThread(() -> appendTextAndScroll(errorLine + "\n"));
                    }

                    // Clean up
                    reader.close();
                    errorReader.close();

                    // Wait for the process to finish
                    process.waitFor();

                } catch (IOException | InterruptedException e) {
                    // Handle exceptions by printing the stack trace in the terminal output
                    final String errorMessage = e.getMessage();
                    activity.runOnUiThread(() -> appendTextAndScroll("Error: " + errorMessage + "n"));
                }
            }).start(); // Execute the command in a separate thread to prevent blocking the UI thread
        else
            new AlertDialog.Builder(activity, R.style.MainDialogTheme)
                    .setTitle("Error!")
                    .setMessage("Verify that \"setupFiles()\" is working properly in onCreate().")
                    .setCancelable(false)
                    .show();
    }

    private boolean checkInstallation() {
        String filesDir = activity.getFilesDir().getAbsolutePath();
        File distro = new File(filesDir, "distro");
        return distro.exists();
    }
}