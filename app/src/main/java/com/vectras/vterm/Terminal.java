package com.vectras.vterm;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
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

public class Terminal {
    private static final String TAG = "Vterm";
    private Context context;
    private String user = "root";

    public static Process qemuProcess;
    
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

    // Method to execute the shell command
    public void executeShellCommand(String userCommand) {
        new Thread(() -> {
            try {
                // Setup the qemuProcess builder to start PRoot with environmental variables and commands
                ProcessBuilder processBuilder = new ProcessBuilder();

                // Adjust these environment variables as necessary for your app
                String filesDir = context.getFilesDir().getAbsolutePath();
                String nativeLibDir = context.getApplicationInfo().nativeLibraryDir;

                // Setup environment for the PRoot qemuProcess
                processBuilder.environment().put("PROOT_TMP_DIR", filesDir + "/tmp");
                processBuilder.environment().put("PROOT_LOADER", nativeLibDir + "/libproot-loader.so");
                processBuilder.environment().put("PROOT_LOADER_32", nativeLibDir + "/libproot-loader32.so");

                processBuilder.environment().put("HOME", "/root");
                processBuilder.environment().put("USER", user);
                processBuilder.environment().put("PATH", "/bin:/usr/bin:/sbin:/usr/sbin");
                processBuilder.environment().put("TERM", "xterm-256color");
                processBuilder.environment().put("TMPDIR", filesDir + "/tmp");
                processBuilder.environment().put("SHELL", "/bin/sh");
                processBuilder.environment().put("PULSE_SERVER", "/run/pulse/native");
                processBuilder.environment().put("XDG_RUNTIME_DIR", "/run");

                // Example PRoot command; replace 'libproot.so' and other paths as needed
                String[] prootCommand = {
                        nativeLibDir + "/libproot.so", // PRoot binary path
                        "--kill-on-exit",
                        "--link2symlink",
                        "-0",
                        "-r", filesDir + "/distro", // Path to the rootfs
                        "-b", "/dev",
                        "-b", "/proc",
                        "-b", "/sys",
                        "-b", "/sdcard",
                        "-b", "/storage",
                        "-b", "/data",
                        "-w", "/root",
                        "/bin/sh",
                        "--login" // The shell to execute inside PRoot
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
                    com.vectras.vm.logger.VectrasStatus.logError("<font color='red'>Vterm ERROR: >"+ line+"</font>");
                }

                // Read any errors from the error stream
                while ((line = errorReader.readLine()) != null) {
                    Log.w(TAG, line);
                    com.vectras.vm.logger.VectrasStatus.logError("<font color='red'>Vterm ERROR: >"+ line+"</font>");
                }

                // Clean up
                reader.close();
                errorReader.close();

                // Wait for the qemuProcess to finish
                qemuProcess.waitFor();

            } catch (IOException | InterruptedException e) {
                // Handle exceptions by printing the stack trace in the terminal output
                final String errorMessage = e.getMessage();
                Log.e("Vterm ERROR:", errorMessage);
                //com.vectras.vm.logger.VectrasStatus.logError("<font color='red'>Vterm ERROR: >"+ errorMessage+"</font>");
            }
        }).start(); // Execute the command in a separate thread to prevent blocking the UI thread
    }

    private boolean checkInstallation() {
        String filesDir = context.getFilesDir().getAbsolutePath();
        File distro = new File(filesDir, "distro");
        return distro.exists();
    }

    public static void killQemuProcess() {
        if (qemuProcess != null) {
            qemuProcess.destroy();
            qemuProcess = null;
        }
    }
}
