package com.vectras.vm.core;

import android.util.Log;
import com.termux.app.TermuxService;
import com.vectras.vm.logger.VectrasStatus;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ShellExecutor {
    private static final String TAG = "ShellExecutor";
    private Process shellExecutorProcess;
    private ExecutorService executorService;
    private Future<?> processFuture;

    public ShellExecutor() {
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void exec(String command) {
        String logPath = "/sdcard/Documents/shell-executor.log";
        String shellPath = "/system/bin/sh";

        Runnable processRunnable = () -> {
            try (FileWriter logWriter = new FileWriter(logPath, true)) {
                shellExecutorProcess = new ProcessBuilder(shellPath).start();
                OutputStream outputStream = shellExecutorProcess.getOutputStream();

                logWriter.write("Running command: " + command + "\n");
                outputStream.write((command + "\n").getBytes());
                outputStream.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(shellExecutorProcess.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    logWriter.write(line + "\n");
                    logWriter.flush();
                    Log.d(TAG, line);
                    VectrasStatus.logInfo(TAG + " > " + line);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error starting ShellExecutor", e);
                VectrasStatus.logInfo(TAG + " > " + e.toString());
            }
        };

        processFuture = executorService.submit(processRunnable);
    }
}