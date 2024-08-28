package com.vectras.vterm;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

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
import com.vectras.vm.R;
import com.vectras.vm.VectrasApp;

public class Terminal {
    private static final String TAG = "Vterm";
    private Context context;
    private String user = "root";

    private Process qemuProcess;
    private BufferedWriter commandWriter;
    public static String DISPLAY = ":0";

    public Terminal(Context context) {
        this.context = context;
        startQemuProcess();
    }

    private void startQemuProcess() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            String filesDir = context.getFilesDir().getAbsolutePath();
            String nativeLibDir = context.getApplicationInfo().nativeLibraryDir;

            File tmpDir = new File(context.getFilesDir(), "tmp");

            processBuilder.environment().put("PROOT_TMP_DIR", tmpDir.getAbsolutePath());
            processBuilder.environment().put("PROOT_LOADER", nativeLibDir + "/libproot-loader.so");
            processBuilder.environment().put("PROOT_LOADER_32", nativeLibDir + "/libproot-loader32.so");

            processBuilder.environment().put("HOME", "/root");
            processBuilder.environment().put("USER", user);
            processBuilder.environment().put("PATH", "/bin:/usr/bin:/sbin:/usr/sbin");
            processBuilder.environment().put("TERM", "xterm-256color");
            processBuilder.environment().put("TMPDIR", tmpDir.getAbsolutePath());
            processBuilder.environment().put("SHELL", "/bin/sh");
            processBuilder.environment().put("DISPLAY", DISPLAY);

            String[] prootCommand = {
                    nativeLibDir + "/libproot.so",
                    "--kill-on-exit",
                    "--link2symlink",
                    "-0",
                    "-r", filesDir + "/distro",
                    "-b", "/dev",
                    "-b", "/proc",
                    "-b", "/sys",
                    "-b", "/sdcard",
                    "-b", "/storage",
                    "-b", "/data",
                    "-b", filesDir + "/distro/root:/dev/shm",
                    "-b", tmpDir.getAbsolutePath() + ":/tmp",
                    "-w", "/root",
                    "/usr/bin/env", "-i",
                    "HOME=/root",
                    "DISPLAY=" + DISPLAY,
                    "/bin/sh",
                    "--login"
            };

            processBuilder.command(prootCommand);
            qemuProcess = processBuilder.start();
            commandWriter = new BufferedWriter(new OutputStreamWriter(qemuProcess.getOutputStream()));

            // Thread to read the output from the process
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(qemuProcess.getInputStream()));
                     BufferedReader errorReader = new BufferedReader(new InputStreamReader(qemuProcess.getErrorStream()))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        Log.d(TAG, line);
                        com.vectras.vm.logger.VectrasStatus.logError("<font color='yellow'>VTERM: >" + line + "</font>");
                    }
                    while ((line = errorReader.readLine()) != null) {
                        Log.w(TAG, line);
                        com.vectras.vm.logger.VectrasStatus.logError("<font color='red'>VTERM ERROR: >" + line + "</font>");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading from qemuProcess", e);
                }
            }).start();

        } catch (IOException e) {
            Log.e(TAG, "Failed to start qemuProcess", e);
        }
    }

    public void executeShellCommand(String userCommand, boolean showResultDialog, Activity dialogActivity) {
        new Thread(() -> {
            try {
                if (commandWriter != null) {
                    commandWriter.write(userCommand);
                    commandWriter.newLine();
                    commandWriter.flush();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error writing to qemuProcess", e);
            }
        }).start();
    }

    public static void killQemuProcess() {

    }
}
