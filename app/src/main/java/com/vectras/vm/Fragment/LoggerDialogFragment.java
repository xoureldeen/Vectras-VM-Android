package com.vectras.vm.Fragment;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.R;
import com.vectras.vm.VectrasApp;
import com.vectras.vm.adapter.LogsAdapter;
import com.vectras.vm.logger.VectrasStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

public class LoggerDialogFragment extends DialogFragment {

    private final String CREDENTIAL_SHARED_PREF = "settings_prefs";
    private LogsAdapter mLogAdapter;
    private RecyclerView logList;
    private Timer _timer = new Timer();
    private TimerTask t;
    Activity activity;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        activity = getActivity();
        final Dialog alertDialog = new Dialog(getActivity(), R.style.MainDialogTheme);
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        alertDialog.setContentView(R.layout.fragment_logs);
        LinearLayoutManager layoutManager = new LinearLayoutManager(VectrasApp.getApp());
        mLogAdapter = new LogsAdapter(layoutManager, VectrasApp.getApp());
        logList = (RecyclerView) alertDialog.findViewById(R.id.recyclerLog);
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

            t = new TimerTask() {
                @Override
                public void run() {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (bufferedReader.readLine() != null || bufferedReader2.readLine() != null) {
                                    String logLine = bufferedReader.readLine();
                                    String logLine2 = bufferedReader2.readLine();
                                    VectrasStatus.logError("<font color='red'>[E] "+logLine+"</font>");
                                    VectrasStatus.logError("<font color='#FFC107'>[W] "+logLine2+"</font>");
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                }
            };
            _timer.scheduleAtFixedRate(t, (int) (0), (int) (100));
        } catch (IOException e) {
            Toast.makeText(activity, "There was an error: " + Log.getStackTraceString(e), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        alertDialog.show();
        return alertDialog;
    }
}