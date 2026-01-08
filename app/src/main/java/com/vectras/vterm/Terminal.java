package com.vectras.vterm;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.termux.app.TermuxService;

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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.vectras.vm.R;
import com.vectras.vm.VMManager;
import com.vectras.vm.AppConfig;
import com.vectras.vm.VectrasApp;
import com.vectras.vm.utils.ClipboardUltils;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.NotificationUtils;

public class Terminal {
    private static final String TAG = "Vterm";
    private final Context context;
    private static final String user = "root";

    public static Process qemuProcess;
    public static String DISPLAY = ":0";

    public Terminal(Context context) {
        this.context = context;
    }

    private void showDialog(String message, Context context, String usercommand) {
        if (VMManager.isExecutedCommandError(usercommand, message, context))
            return;

        DialogUtils.twoDialog(context, "Execution Result", message, context.getString(R.string.copy), context.getString(R.string.close), true, R.drawable.round_terminal_24, true,
                () -> ClipboardUltils.copyToClipboard(context, message), null, null);
    }

    // Method to execute the shell command
    public void executeShellCommand(String userCommand, boolean showResultDialog, boolean showProgressDialog, Context dialogActivity) {
        executeShellCommand(userCommand, showResultDialog, showProgressDialog, dialogActivity.getString(R.string.executing_command_please_wait), dialogActivity);
    }

    public void executeShellCommand(String userCommand, boolean showResultDialog, boolean showProgressDialog, String progressDialogMessage, Context dialogActivity) {
        AtomicReference<StringBuilder> output = new AtomicReference<>(new StringBuilder());
        StringBuilder errors = new StringBuilder();
        Log.d(TAG, userCommand);
        com.vectras.vm.logger.VectrasStatus.logError("<font color='#4db6ac'>VTERM: >" + userCommand + "</font>");

        // Show ProgressDialog
        View progressView = LayoutInflater.from(dialogActivity).inflate(R.layout.dialog_progress_style, null);
        TextView progress_text = progressView.findViewById(R.id.progress_text);
        progress_text.setText(progressDialogMessage);
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(dialogActivity, R.style.CenteredDialogTheme)
                .setView(progressView)
                .setCancelable(false)
                .create();

        if (showProgressDialog) progressDialog.show();

        new Thread(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder();

                String filesDir = Objects.requireNonNull(context.getFilesDir().getAbsolutePath());
                File tmpDir = new File(Objects.requireNonNull(context.getFilesDir()), "usr/tmp");

                processBuilder.environment().put("PROOT_TMP_DIR", tmpDir.getAbsolutePath());
                processBuilder.environment().put("HOME", "/root");
                processBuilder.environment().put("USER", user);
                processBuilder.environment().put("TERM", "xterm-256color");
                processBuilder.environment().put("TMPDIR", "/tmp");
                processBuilder.environment().put("SHELL", "/bin/sh");
                processBuilder.environment().put("DISPLAY", DISPLAY);
                processBuilder.environment().put("PULSE_SERVER", "127.0.0.1");

                String[] prootCommand = {
                        TermuxService.PREFIX_PATH + "/bin/proot",
                        "--kill-on-exit",
                        "--link2symlink",
                        "-0",
                        "-r", filesDir + "/distro",
                        "-b", "/dev",
                        "-b", "/proc",
                        "-b", "/sys",
                        "-b", AppConfig.internalDataDirPath + "distro/root:/dev/shm",
                        "-b", "/sdcard",
                        "-b", "/storage",
                        "-b", "/data",
                        "-b", AppConfig.internalDataDirPath + "usr/tmp:/tmp",
                        "-w", "/root",
                        "/bin/sh",
                        "--login"
                };

                processBuilder.command(prootCommand);
                qemuProcess = processBuilder.start();

                output.set(streamLog(userCommand, qemuProcess, false));
            } catch (IOException e) {
                progressDialog.dismiss(); // Dismiss ProgressDialog
                output.get().append(e.getMessage());
                errors.append(Log.getStackTraceString(e));
            } finally {
                new Handler(Looper.getMainLooper()).post(() -> {
                    progressDialog.dismiss(); // Dismiss ProgressDialog
                    AppConfig.temporaryLastedTerminalOutput = output.toString();
                    if (showResultDialog) {
                        String finalOutput = output.toString();
                        String finalErrors = errors.toString();
                        showDialog(finalOutput.isEmpty() ? finalErrors : finalOutput.replace("read interrupted", "Done!"), dialogActivity, userCommand);
                    }
                });
            }
        }).start();
    }

    public void executeShellCommand2(String userCommand, boolean showResultDialog, Context dialogActivity) {
        AtomicReference<StringBuilder> output = new AtomicReference<>(new StringBuilder());
        StringBuilder errors = new StringBuilder();
        Log.d(TAG, userCommand);
        com.vectras.vm.logger.VectrasStatus.logError("<font color='#4db6ac'>VTERM: >" + userCommand + "</font>");
        new Thread(() -> {
            try {
                // Set up the qemuProcess builder to start PRoot with environmental variables and commands
                ProcessBuilder processBuilder = new ProcessBuilder();

                // Adjust these environment variables as necessary for your app
                String filesDir = getContext().getFilesDir().getAbsolutePath();

                File tmpDir = new File(getContext().getFilesDir(), "usr/tmp");

                // Setup environment for the PRoot qemuProcess
                processBuilder.environment().put("PROOT_TMP_DIR", tmpDir.getAbsolutePath());

                processBuilder.environment().put("HOME", "/root");
                processBuilder.environment().put("USER", user);
                //processBuilder.environment().put("PATH", "/bin:/usr/bin:/sbin:/usr/sbin");
                //processBuilder.environment().put("LD_LIBRARY_PATH", TermuxService.PREFIX_PATH + "/lib");
                processBuilder.environment().put("TERM", "xterm-256color");
                processBuilder.environment().put("TMPDIR", "/tmp");
                processBuilder.environment().put("SHELL", "/bin/sh");
                processBuilder.environment().put("DISPLAY", DISPLAY);
                processBuilder.environment().put("PULSE_SERVER", "127.0.0.1");
                processBuilder.environment().put("XDG_RUNTIME_DIR", "${TMPDIR}");
                processBuilder.environment().put("SDL_VIDEODRIVER", "x11");

                String[] prootCommand = {
                        TermuxService.PREFIX_PATH + "/bin/proot", // PRoot binary path
                        "--kill-on-exit",
                        "--link2symlink",
                        "-0",
                        "-r", filesDir + "/distro", // Path to the rootfs
                        "-b", "/dev",
                        "-b", "/proc",
                        "-b", "/sys",
                        "-b", AppConfig.internalDataDirPath + "distro/root:/dev/shm",
                        "-b", "/sdcard",
                        "-b", "/storage",
                        "-b", "/data",
                        "-b", AppConfig.internalDataDirPath + "usr/tmp:/tmp",
                        "-w", "/root",
                        "/bin/sh",
                        "--login"// The shell to execute inside PRoot
                };

                processBuilder.command(prootCommand);
                qemuProcess = processBuilder.start();

                output.set(streamLog(userCommand, qemuProcess, false));
            } catch (IOException e) {
                output.get().append(e.getMessage());
                errors.append(Log.getStackTraceString(e));
                NotificationUtils.clearAll(VectrasApp.getContext());
            } finally {
                // Switch to main thread after execution
                new Handler(Looper.getMainLooper()).post(() -> {
                    AppConfig.temporaryLastedTerminalOutput = output.toString();
                    // If showResultDialog is enabled, show the dialog with the result or errors
                    if (showResultDialog) {
                        String finalOutput = output.toString();
                        String finalErrors = errors.toString();
                        // bcuz there is dumb users bruh
                        showDialog(finalOutput.isEmpty() ? finalErrors : finalOutput.replace("read interrupted", "Done!"), dialogActivity, userCommand);
                    }
                });
            }
        }).start();
    }

    public static String executeShellCommandWithResult(String userCommand, Context context) {
        StringBuilder output = new StringBuilder();
        StringBuilder errors = new StringBuilder();
        Log.d(TAG, userCommand);
        com.vectras.vm.logger.VectrasStatus.logError("<font color='#4db6ac'>VTERM: >" + userCommand + "</font>");

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();

            String filesDir = Objects.requireNonNull(context.getFilesDir().getAbsolutePath());
            File tmpDir = new File(Objects.requireNonNull(context.getFilesDir()), "usr/tmp");

            processBuilder.environment().put("PROOT_TMP_DIR", tmpDir.getAbsolutePath());
            processBuilder.environment().put("HOME", "/root");
            processBuilder.environment().put("USER", user);
            processBuilder.environment().put("TERM", "xterm-256color");
            processBuilder.environment().put("TMPDIR", "/tmp");
            processBuilder.environment().put("SHELL", "/bin/sh");
            processBuilder.environment().put("DISPLAY", DISPLAY);
            processBuilder.environment().put("PULSE_SERVER", "127.0.0.1");

            String[] prootCommand = {
                    TermuxService.PREFIX_PATH + "/bin/proot",
                    "--kill-on-exit",
                    "--link2symlink",
                    "-0",
                    "-r", filesDir + "/distro",
                    "-b", "/dev",
                    "-b", "/proc",
                    "-b", "/sys",
                    "-b", AppConfig.internalDataDirPath + "distro/root:/dev/shm",
                    "-b", "/sdcard",
                    "-b", "/storage",
                    "-b", "/data",
                    "-b", AppConfig.internalDataDirPath + "usr/tmp:/tmp",
                    "-w", "/root",
                    "/bin/sh",
                    "--login"
            };

            processBuilder.command(prootCommand);
            qemuProcess = processBuilder.start();

            output = streamLog(userCommand, qemuProcess, false);
        } catch (IOException e) {
            output.append(e.getMessage());
            errors.append(Log.getStackTraceString(e));
        }
        return output.toString();
    }

    public interface CommandCallback {
        void onCommandCompleted(String output, String errors);
    }

    public String executeShellCommand(String userCommand, Context dialogActivity, boolean isShowProgressDialog, CommandCallback callback) {
        AtomicReference<StringBuilder> output = new AtomicReference<>(new StringBuilder());
        StringBuilder errors = new StringBuilder();
        Log.d(TAG, userCommand);
        com.vectras.vm.logger.VectrasStatus.logError("<font color='#4db6ac'>VTERM: >" + userCommand + "</font>");

        // Show ProgressDialog on the main thread
        View progressView = LayoutInflater.from(dialogActivity).inflate(R.layout.dialog_progress_style, null);
        TextView progress_text = progressView.findViewById(R.id.progress_text);
        progress_text.setText(dialogActivity.getString(R.string.executing_command_please_wait));
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(dialogActivity, R.style.CenteredDialogTheme)
                .setView(progressView)
                .setCancelable(false)
                .create();

        // Make sure to show the dialog on the main thread
        if (isShowProgressDialog) new Handler(Looper.getMainLooper()).post(progressDialog::show);

        new Thread(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder();

                String filesDir = Objects.requireNonNull(context.getFilesDir().getAbsolutePath());
                File tmpDir = new File(Objects.requireNonNull(context.getFilesDir()), "usr/tmp");

                processBuilder.environment().put("PROOT_TMP_DIR", tmpDir.getAbsolutePath());
                processBuilder.environment().put("HOME", "/root");
                processBuilder.environment().put("USER", user);
                processBuilder.environment().put("TERM", "xterm-256color");
                processBuilder.environment().put("TMPDIR", "/tmp");
                processBuilder.environment().put("SHELL", "/bin/sh");
                processBuilder.environment().put("DISPLAY", DISPLAY);
                processBuilder.environment().put("PULSE_SERVER", "127.0.0.1");

                String[] prootCommand = {
                        TermuxService.PREFIX_PATH + "/bin/proot",
                        "--kill-on-exit",
                        "--link2symlink",
                        "-0",
                        "-r", filesDir + "/distro",
                        "-b", "/dev",
                        "-b", "/proc",
                        "-b", "/sys",
                        "-b", AppConfig.internalDataDirPath + "distro/root:/dev/shm",
                        "-b", "/sdcard",
                        "-b", "/storage",
                        "-b", "/data",
                        "-b", AppConfig.internalDataDirPath + "usr/tmp:/tmp",
                        "-w", "/root",
                        "/bin/sh",
                        "--login"
                };

                processBuilder.command(prootCommand);
                qemuProcess = processBuilder.start();

                output.set(streamLog(userCommand, qemuProcess, true));

            } catch (IOException e) {
                output.get().append(e.getMessage());
                errors.append(Log.getStackTraceString(e));
            } finally {
                // Dismiss ProgressDialog on the main thread
                new Handler(Looper.getMainLooper()).post(progressDialog::dismiss);

                // Use callback to return both output and errors
                new Handler(Looper.getMainLooper()).post(() -> callback.onCommandCompleted(output.toString(), errors.toString()));
            }
        }).start();

        return "Execution is in progress..."; // Returning a message indicating the command execution is ongoing
    }

    /**
     * Checks if a package is installed using `apk info`.
     *
     * @param packageName The name of the package to check.
     * @return True if the package is installed, otherwise false.
     */
    private static boolean isPackageInstalled(String packageName) {
        try {
            Process process = new ProcessBuilder("apk", "info", packageName).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(packageName)) {
                    return true;
                }
            }
            process.waitFor();
        } catch (Exception e) {
            Log.e(TAG, "Error checking package: " + packageName, e);
        }
        return false;
    }

    public static StringBuilder streamLog(String command, Process process, boolean isShortProcess) {
        StringBuilder output = new StringBuilder();
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            writer.write(command);
            writer.newLine();
            writer.flush();
            writer.close();

            String line;
            while ((line = reader.readLine()) != null) {
                com.vectras.vm.logger.VectrasStatus.logError("<font color='#4db6ac'>VTERM: >" + line + "</font>");
                output.append(line).append("\n");
            }

            while ((line = errorReader.readLine()) != null) {
                Log.w(TAG, line);
                com.vectras.vm.logger.VectrasStatus.logError("<font color='red'>VTERM ERROR: >" + line + "</font>");
                output.append(line).append("\n");
            }

            if (isShortProcess) {
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    output.append("Execution finished successfully.\n");
                } else {
                    output.append("Execution finished with exit code: ").append(exitCode).append("\n");
                }
            }

            reader.close();
            errorReader.close();
        } catch (Exception e) {
            output.append(e.getMessage());
            Log.e(TAG, "streamLog: ", e);
        }
        return output;
    }

    private Context getContext() {
        if (context == null) {
            return VectrasApp.getContext();
        }
        return context;
    }
}
