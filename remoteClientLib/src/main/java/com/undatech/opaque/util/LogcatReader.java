package com.undatech.opaque.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by iordan on 24/06/18.
 */

public class LogcatReader {
    private static final int LOGCAT_MAX_LINES = 1000;
    public static String TAG = "LogcatReader";
    private List<String> logcatCommand = new ArrayList<String>();

    public LogcatReader() {
        this(false);
    }

    public LogcatReader(boolean onlyThisPid) {
        logcatCommand.add("logcat");
        logcatCommand.add("-d");
        if (onlyThisPid) {
            int id = android.os.Process.myPid();
            logcatCommand.add("--pid");
            logcatCommand.add(Integer.toString(id));
        }
    }

    public String getMyLogcat(int lines) {
        ArrayList<String> logCatLines = new ArrayList<>(LOGCAT_MAX_LINES);
        StringBuilder logCatOutput = new StringBuilder(LOGCAT_MAX_LINES);
        String line;

        try {
            Process p = new ProcessBuilder().command(logcatCommand).start();
            BufferedReader buffReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = buffReader.readLine()) != null) {
                logCatLines.add(line + "\n");
            }
        } catch (IOException e) {
            android.util.Log.e(TAG, "Error obtaining output from logcat");
            e.printStackTrace();
        }

        for (int i = Math.max(logCatLines.size() - LOGCAT_MAX_LINES, 0); i < logCatLines.size(); i++) {
            logCatOutput.append(logCatLines.get(i));
        }

        return logCatOutput.toString();
    }
}
