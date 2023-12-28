package com.vectras.vm.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.vectras.vm.R;
import com.vectras.vm.RomsManagerActivity;
import com.vectras.vm.MainActivity;
import com.vectras.vm.adapter.LogsAdapter;
import com.vectras.vm.utils.UIUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoggerFragment extends Fragment {

	View view;
	MainActivity activity = MainActivity.activity;
	private final String CREDENTIAL_SHARED_PREF = "settings_prefs";
	private LogsAdapter mLogAdapter;
	private RecyclerView logList;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub

		view = inflater.inflate(R.layout.fragment_logs, container, false);
		LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.activity);
		mLogAdapter = new LogsAdapter(layoutManager, MainActivity.activity);
		logList = (RecyclerView) view.findViewById(R.id.recyclerLog);
		logList.setAdapter(mLogAdapter);
		logList.setLayoutManager(layoutManager);
		mLogAdapter.scrollToLastPosition();
		return view;
	}

}
