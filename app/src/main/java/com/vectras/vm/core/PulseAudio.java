package com.vectras.vm.core;

import android.content.Context;
import android.util.Log;
import com.termux.app.TermuxService;
import com.vectras.vm.logger.VectrasStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class PulseAudio {
    private static final String TAG = "PulseAudio";
    private Process pulseAudioProcess;
    private Context context;
    private ExecutorService executorService;
    private Future<?> processFuture;

    public PulseAudio(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void start() {
        String tmpDir = TermuxService.PREFIX_PATH + "/tmp";

        Runnable processRunnable = () -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(
                    "/system/bin/sh", "-c",
                    "XDG_RUNTIME_DIR=" + tmpDir + " TMPDIR=" + tmpDir + " " +
                    TermuxService.PREFIX_PATH + "/bin/pulseaudio --start " +
                    "--load=\"module-native-protocol-tcp auth-ip-acl=127.0.0.1 auth-anonymous=1\" " +
                    "--exit-idle-time=-1"
                );

                processBuilder.redirectErrorStream(true);
                pulseAudioProcess = processBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(pulseAudioProcess.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.d(TAG, line);
                    VectrasStatus.logInfo(TAG + " > " + line);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error starting PulseAudio", e);
                VectrasStatus.logInfo(TAG + " > " + e.toString());
            }
        };

        processFuture = executorService.submit(processRunnable);
    }

    public void stop() {
        if (pulseAudioProcess != null) {
            pulseAudioProcess.destroy();
            try {
                pulseAudioProcess.waitFor();
                Log.d(TAG, "PulseAudio stopped");
                VectrasStatus.logInfo(TAG + " > PulseAudio stopped");
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping PulseAudio", e);
                VectrasStatus.logInfo(TAG + " > Error stopping PulseAudio: " + e.toString());
            }
        }

        if (processFuture != null) {
            processFuture.cancel(true);
        }

        executorService.shutdown();
    }
}