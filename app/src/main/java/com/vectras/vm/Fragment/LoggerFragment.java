package com.vectras.vm.Fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vectras.vm.R;
import com.vectras.vm.RomsManagerActivity;
import com.vectras.vm.MainActivity;
import com.vectras.vm.VectrasApp;
import com.vectras.vm.adapter.LogsAdapter;
import com.vectras.vm.logger.VectrasStatus;
import com.vectras.vm.utils.UIUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class LoggerFragment extends Fragment {

    View view;
    private final String CREDENTIAL_SHARED_PREF = "settings_prefs";
    private LogsAdapter mLogAdapter;
    private RecyclerView logList;
    private Timer _timer = new Timer();
    private TimerTask t;
    Activity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO Auto-generated method stub

        view = inflater.inflate(R.layout.fragment_logs, container, false);
        activity = getActivity();
        LinearLayoutManager layoutManager = new LinearLayoutManager(VectrasApp.getApp());
        mLogAdapter = new LogsAdapter(layoutManager, VectrasApp.getApp());
        logList = (RecyclerView) view.findViewById(R.id.recyclerLog);
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

        return view;
    }

}
