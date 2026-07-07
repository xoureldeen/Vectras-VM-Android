package com.vectras.vterm;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.vectras.vm.AppConfig;
import com.vectras.vm.R;
import com.vectras.vm.logger.VectrasStatus;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.ProgressDialog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Terminal2 {
    final String TAG = "Terminal2";
    public final int SUCCESS = 0;
    public final int ERROR = 1;
    Context context;
    String user = "root";
    String home = "/root";
    String display = ":0";
    String shell = "/bin/sh";
    String startup = "";
    String logs = "";
    String lastLog = "";
    boolean isShowProgressDialog;

    ProgressDialog progressDialog = null;

    public interface Terminal2Callback {
        void onRunning(String command, String newLine);
        void onFinished(String command, String log, int status);
        void onError(String command, Exception exception);
    }

    public Terminal2(Context context) {
        this.context = context;
    }

    public void setUserName(String user) {
        this.user = user;
        home = user.equals("root") ? "/root" : "/home/" + user;
    }

    public void setRootMode() {
        this.user = "root";
        home = "/root";
    }

    public void setDefaultShell() {
        this.shell = "/bin/sh";
    }

    public void setDefaultShellBash() {
        this.shell = "/bin/bash";
    }

    public void setStartup(String command) {
        this.startup = command;
    }

    public String getLogs() {
        return logs;
    }

    public void clearLog() {
        logs = "";
    }

    public void setShowProgressDialog(boolean isShow) {
        this.isShowProgressDialog = isShow;
    }

    public String executeOnThisThread(String command) {
        execute(command, null, false);
        return lastLog;
    }

    public void execute(String command) {
        execute(command, null);
    }

    public void execute(String command, Terminal2Callback callback) {
        execute(command, callback, true);
    }

    public void execute(String command, Terminal2Callback callback, boolean isNewThread) {
        AtomicReference<Process> process = new AtomicReference<>();

        Runnable runnable = () -> {

            if (isShowProgressDialog) {
                progressDialog = new ProgressDialog(context);
                progressDialog.setText(context.getString(R.string.executing_command_please_wait));
                new Handler(Looper.getMainLooper()).post((progressDialog::show));
            }

            try {
                ProcessBuilder processBuilder = new ProcessBuilder();


                String filesDir = Objects.requireNonNull(context.getFilesDir().getAbsolutePath());
                String prootPath = new File(filesDir, "usr/bin/proot").getAbsolutePath();
                String tmpDir = new File(filesDir, "usr/tmp").getAbsolutePath();

                if (!FileUtils.isFileExists(filesDir + "/distro" + shell)) {
                    setDefaultShell();
                }

                if (!FileUtils.isFileExists(filesDir + "/distro" + home)) {
                    FileUtils.createDirectory(filesDir + "/distro" + home);
                }

                processBuilder.environment().put("PROOT_TMP_DIR", tmpDir);
                processBuilder.environment().put("HOME", home);
                processBuilder.environment().put("USER", user);
                processBuilder.environment().put("TERM", "xterm-256color");
                processBuilder.environment().put("TMPDIR", "/tmp");
                processBuilder.environment().put("SHELL", shell);
                processBuilder.environment().put("DISPLAY", display);
                processBuilder.environment().put("XDG_RUNTIME_DIR", "/tmp");
                processBuilder.environment().put("SDL_VIDEODRIVER", "x11");

                String[] prootCommand = {
                        prootPath,
                        "--kill-on-exit",
                        "--link2symlink",
                        "-0",
                        "-r", filesDir + "/distro",
                        "-b", "/dev",
                        "-b", "/proc",
                        "-b", "/sys",
                        "-b", AppConfig.internalDataDirPath + "distro" + home + ":/dev/shm",
                        "-b", "/sdcard",
                        "-b", "/storage",
                        "-b", "/data",
                        "-b", AppConfig.internalDataDirPath + "usr/tmp:/tmp",
                        "-w", home,
                        shell,
                        "--login"
                };

                processBuilder.command(prootCommand);
                process.set(processBuilder.start());

                resultData data = startProcess((startup.isEmpty() ? "" : startup + " && ") + command, process.get(), callback);

                if (callback != null) callback.onFinished(command, data.log.toString(), data.status);
            } catch (IOException e) {
                if (callback != null) callback.onError(command, e);

                addToLogs(command, e.getMessage());
            } finally {
                if (progressDialog != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        progressDialog.reset();
                        progressDialog = null;
                    });
                }
            }
        };

        if (isNewThread) {
            new Thread(runnable).start();
        } else {
            runnable.run();
        }
    }

    private resultData startProcess(String command, Process process, Terminal2Callback callback) {
        resultData data = new resultData();
        AtomicInteger exitCode = new AtomicInteger();

        int MAX_LOG_SIZE = 200_000; // ~200KB text
        StringBuilder output = new StringBuilder();
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            writer.write(command);
            writer.newLine();
            writer.flush();
            writer.close();

            String line;
            while ((line = reader.readLine()) != null) {
                Log.d(TAG, line);
                VectrasStatus.logError(line);
                output.append(line).append("\n");
                if (callback != null) callback.onRunning(command, line);

                if (output.length() > MAX_LOG_SIZE) {
                    output.delete(0, output.length() - MAX_LOG_SIZE);
                }

                if (progressDialog != null) {
                    String finalLine = line;
                    new Handler(Looper.getMainLooper()).post(() -> progressDialog.setText(finalLine));
                }
            }

            try {
                exitCode.set(process.waitFor());
            } catch (InterruptedException e) {
                exitCode.set(ERROR);
            }
            data.status = exitCode.get();

            reader.close();
        } catch (Exception e) {
            output.append(e.getMessage());
            Log.e(TAG, "streamLog: ", e);
        }

        data.status = exitCode.get();
        data.log = output;

        addToLogs(command, output.toString());

        return data;
    }

    private void addToLogs(String command,String log) {
        lastLog = log;

        if (!logs.isEmpty()) {
            logs += "\n\n----------\n\n";
        }

        logs += "$ " + command + "\n\n";

        logs += log;

        if (logs.contains("\n") && logs.lastIndexOf("\n") > 1000)
            logs = logs.substring(logs.lastIndexOf("\n") + 1);
    }

    private class resultData {
        StringBuilder log;
        int status;
    }
}
