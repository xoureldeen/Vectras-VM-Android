package com.vectras.vterm;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

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

import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.MainVNCActivity;
import com.vectras.vm.MainActivity;
import com.vectras.vm.MainService;
import com.vectras.vm.R;
import com.vectras.vm.VectrasApp;

public class Terminal {
    private static final String TAG = "Vterm";
    private Context context;
    private String user = "root";

    public static Process qemuProcess;
    public static String DISPLAY = ":0";

    public Terminal(Context context) {
        this.context = context;
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

    private void showDialog(String message, Activity activity, String usercommand) {
        if (!usercommand.contains("qemu-system") || message.contains("Killed"))
            return;
        AlertDialog dialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
        dialog.setTitle("Execution Result");
        dialog.setMessage(message);
                //.setPositiveButton("OK", (dialogInterface, i) -> dialogInterface.dismiss())
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //killQemuProcess();
                MainService.stopService();
                dialog.dismiss();
            }
        });
        dialog.create();

        dialog.show();
    }

    // Method to execute the shell command
    public void executeShellCommand(String userCommand, boolean showResultDialog, Activity dialogActivity) {
        StringBuilder output = new StringBuilder();
        StringBuilder errors = new StringBuilder();
        Log.d(TAG, userCommand);
        com.vectras.vm.logger.VectrasStatus.logError("<font color='yellow'>VTERM: >" + userCommand + "</font>");
        new Thread(() -> {
            try {
                // Setup the qemuProcess builder to start PRoot with environmental variables and commands
                ProcessBuilder processBuilder = new ProcessBuilder();

                // Adjust these environment variables as necessary for your app
                String filesDir = context.getFilesDir().getAbsolutePath();

                File tmpDir = new File(context.getFilesDir(), "usr/tmp");

                // Setup environment for the PRoot qemuProcess
                processBuilder.environment().put("PROOT_TMP_DIR", tmpDir.getAbsolutePath());

                processBuilder.environment().put("HOME", "/root");
                processBuilder.environment().put("USER", user);
                //processBuilder.environment().put("PATH", "/bin:/usr/bin:/sbin:/usr/sbin");
                //processBuilder.environment().put("LD_LIBRARY_PATH", TermuxService.PREFIX_PATH + "/lib");
                processBuilder.environment().put("TERM", "xterm-256color");
                processBuilder.environment().put("TMPDIR", tmpDir.getAbsolutePath());
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
                        "-b", "/data/data/com.vectras.vm/files/distro/root:/dev/shm",
                        "-b", "/sdcard",
                        "-b", "/storage",
                        "-b", "/data",
                        "-b", "/data/data/com.vectras.vm/files/usr/tmp:/tmp",
                        "-w", "/root",
                        "/bin/sh",
                        "--login"// The shell to execute inside PRoot
                };

                processBuilder.command(prootCommand);
                qemuProcess = processBuilder.start();
                // Get the input and output streams of the qemuProcess
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(qemuProcess.getOutputStream()));
                BufferedReader reader = new BufferedReader(new InputStreamReader(qemuProcess.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(qemuProcess.getErrorStream()));

                // Send user command to PRoot
                writer.write(userCommand);
                writer.newLine();
                writer.flush();
                writer.close();

                // Read the input stream for the output of the command
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.d(TAG, line);
                    com.vectras.vm.logger.VectrasStatus.logError("<font color='yellow'>VTERM: >" + line + "</font>");
                    output.append(line).append("\n");
                }

                // Read any errors from the error stream
                while ((line = errorReader.readLine()) != null) {
                    Log.w(TAG, line);
                    com.vectras.vm.logger.VectrasStatus.logError("<font color='red'>VTERM ERROR: >" + line + "</font>");
                    output.append(line).append("\n");
                }

                // Clean up
                reader.close();
                errorReader.close();

                int exitCode = qemuProcess.waitFor(); // Wait for the process to finish
                if (exitCode == 0) {
                    output.append("Execution finished successfully.\n");
                    output.append(reader.readLine()).append("\n");
                    Log.i(TAG, reader.readLine());
                } else {
                    output.append("Execution finished with exit code: ").append(exitCode).append("\n");
                    output.append(reader.readLine()).append("\n");
                    Log.i(TAG, reader.readLine());
                }
            } catch (IOException | InterruptedException e) {
                output.append(e.getMessage());
                errors.append(Log.getStackTraceString(e));
                MainActivity.clearNotifications();
            } finally {
                // Switch to main thread after execution
                new Handler(Looper.getMainLooper()).post(() -> {
                    VectrasApp.TerminalOutput = output.toString();
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

    private boolean checkInstallation() {
        String filesDir = context.getFilesDir().getAbsolutePath();
        File distro = new File(filesDir, "distro");
        return distro.exists();
    }

    public static void killQemuProcess() {
        if (qemuProcess != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                qemuProcess.destroyForcibly();
            else
                qemuProcess.destroy();
            //MainVNCActivity.activity.finish();
            MainVNCActivity.started = false;
            qemuProcess = null; // Set it to null after destroying it
            Log.d(TAG, "QEMU process destroyed.");
        } else {
            Log.d(TAG, "QEMU process was null.");
        }
    }
}
