package com.vectras.vm.Fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.vectras.vm.R;
import com.vectras.vm.VectrasApp;
import com.vectras.vm.adapter.LogsAdapter;
import com.vectras.vm.logger.VectrasStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class LoggerDialogFragment extends DialogFragment {
    private final String TAG = "LoggerDialogFragment";
    private final Timer _timer = new Timer();
    private boolean isReading;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog alertDialog = new Dialog(requireActivity(), R.style.MainDialogTheme);
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Objects.requireNonNull(alertDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.setContentView(R.layout.fragment_logs);
        LinearLayoutManager layoutManager = new LinearLayoutManager(VectrasApp.getApp());
        LogsAdapter mLogAdapter = new LogsAdapter(layoutManager, VectrasApp.getApp());
        RecyclerView logList = alertDialog.findViewById(R.id.recyclerLog);
        logList.setAdapter(mLogAdapter);
        logList.setLayoutManager(layoutManager);
        mLogAdapter.scrollToLastPosition();
        try {
            Process process = Runtime.getRuntime().exec("logcat -e");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            Process process2 = Runtime.getRuntime().exec("logcat -w");
            BufferedReader bufferedReader2 = new BufferedReader(
                    new InputStreamReader(process2.getInputStream()));

            TimerTask t = new TimerTask() {
                @Override
                public void run() {
                    new Thread(() -> {
                        if (isReading) return;
                        String logLine = "";
                        String logLine2 = "";
                        try {
                            isReading = true;
                            if (bufferedReader.readLine() != null || bufferedReader2.readLine() != null) {
                                logLine = bufferedReader.readLine();
                                logLine2 = bufferedReader2.readLine();
                            }

                            String finalLogLine = logLine;
                            String finalLogLine1 = logLine2;
                            if (!isAdded()) {
                                _timer.cancel();
                                return;
                            }
                            requireActivity().runOnUiThread(() -> {
                                if (!finalLogLine.isEmpty())
                                    VectrasStatus.logError("<font color='red'>[E] " + finalLogLine + "</font>");
                                if (!finalLogLine1.isEmpty())
                                    VectrasStatus.logError("<font color='#FFC107'>[W] " + finalLogLine1 + "</font>");
                                isReading = false;
                            });
                        } catch (IOException e) {
                            Log.e(TAG, "onCreateDialog: ", e);
                        } finally {
                            isReading = false;
                        }
                    }).start();
                }
            };
            _timer.schedule(t, 0, 1000);
        } catch (IOException e) {
            if (isAdded())
                Toast.makeText(requireActivity(), "There was an error: " + Log.getStackTraceString(e), Toast.LENGTH_LONG).show();
            Log.e(TAG, "onCreateDialog: ", e);
        }

        alertDialog.show();
        return alertDialog;
    }

    public void onDismiss(@NonNull DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        _timer.cancel();
    }
}