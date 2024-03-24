/**
 * Copyright (C) 2013- Iordan Iordanov
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */

package com.undatech.opaque;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.iiordanov.bVNC.Constants;
import com.iiordanov.util.PermissionGroups;
import com.iiordanov.util.PermissionsManager;
import com.undatech.opaque.dialogs.ManageCustomCaFragment;
import com.undatech.opaque.util.FileUtils;
import com.undatech.remoteClientUi.R;

import java.io.IOException;
import java.util.List;


public class AdvancedSettingsActivity extends FragmentActivity implements ManageCustomCaFragment.OnFragmentDismissedListener {
    private static String TAG = "AdvancedSettingsActivity";

    private ConnectionSettings currentConnection;
    private ToggleButton toggleAudioPlayback;
    private ToggleButton toggleAutoRotation;
    private ToggleButton toggleAutoRequestDisplayResolution;
    private ToggleButton toggleCustomDisplayResolution;
    private ToggleButton toggleSslStrict;
    private ToggleButton toggleUsbEnabled;
    private ToggleButton toggleUsingCustomOvirtCa;
    private ToggleButton toggleUseLastPositionToolbar;
    private Button buttonManageOvirtCa;
    private Spinner layoutMapSpinner;
    private LinearLayout layoutManageOvirtCa;
    private LinearLayout layoutToggleUsingCustomOvirtCa;
    private LinearLayout layoutCustomRemoteResolution;
    private LinearLayout layoutToggleCustomRemoteResolution;
    private LinearLayout layoutUseLastPositionToolbar;
    private TextView textUseLastPositionToolbar;
    private EditText rdpWidth;
    private EditText rdpHeight;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.advanced_settings_activity);

        Intent i = getIntent();
        currentConnection = (ConnectionSettings) i.getSerializableExtra(Constants.opaqueConnectionSettingsClassPath);

        toggleAudioPlayback = (ToggleButton) findViewById(R.id.toggleAudioPlayback);
        toggleAudioPlayback.setChecked(currentConnection.isAudioPlaybackEnabled());

        toggleUsbEnabled = (ToggleButton) findViewById(R.id.toggleUsbEnabled);
        toggleUsbEnabled.setChecked(currentConnection.isUsbEnabled());

        toggleAutoRotation = (ToggleButton) findViewById(R.id.toggleAutoRotation);
        toggleAutoRotation.setChecked(currentConnection.isRotationEnabled());

        toggleAutoRequestDisplayResolution = (ToggleButton) findViewById(R.id.toggleAutoRequestDisplayResolution);
        toggleAutoRequestDisplayResolution.setChecked(currentConnection.isRequestingNewDisplayResolution());

        toggleCustomDisplayResolution = (ToggleButton) findViewById(R.id.toggleCustomDisplayResolution);
        toggleCustomDisplayResolution.setChecked(currentConnection.getRdpResType() == RemoteClientLibConstants.RDP_GEOM_SELECT_CUSTOM);

        toggleSslStrict = (ToggleButton) findViewById(R.id.toggleSslStrict);
        toggleSslStrict.setChecked(currentConnection.isSslStrict());

        layoutManageOvirtCa = (LinearLayout) findViewById(R.id.layoutManageOvirtCa);
        layoutToggleUsingCustomOvirtCa = (LinearLayout) findViewById(R.id.layoutToggleUsingCustomOvirtCa);
        toggleUsingCustomOvirtCa = (ToggleButton) findViewById(R.id.toggleUsingCustomOvirtCa);
        toggleUsingCustomOvirtCa.setChecked(currentConnection.isUsingCustomOvirtCa());

        layoutUseLastPositionToolbar = (LinearLayout) findViewById(R.id.layoutUseLastPositionToolbar);
        textUseLastPositionToolbar = (TextView) findViewById(R.id.textUseLastPositionToolbar);
        textUseLastPositionToolbar.setText(getString(R.string.position_toolbar_last_used));
        toggleUseLastPositionToolbar = (ToggleButton) findViewById(R.id.toggleUseLastPositionToolbar);
        toggleUseLastPositionToolbar.setChecked(currentConnection.getUseLastPositionToolbar());

        buttonManageOvirtCa = (Button) findViewById(R.id.buttonManageOvirtCa);
        buttonManageOvirtCa.setEnabled(currentConnection.isUsingCustomOvirtCa());

        if (currentConnection.getConnectionTypeString()
                .equals(getResources().getString(R.string.connection_type_pve))) {
            layoutToggleUsingCustomOvirtCa.setVisibility(View.GONE);
            layoutManageOvirtCa.setVisibility(View.GONE);
        }

        layoutMapSpinner = (Spinner) findViewById(R.id.layoutMaps);

        layoutMapSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                layoutMapSpinner.setSelection(position);
                TextView selection = null;
                if (layoutMapSpinner != null) {
                    selection = (TextView) layoutMapSpinner.getSelectedView();
                }
                if (selection != null) {
                    currentConnection.setLayoutMap(selection.getText().toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        layoutToggleCustomRemoteResolution = (LinearLayout) findViewById(R.id.layoutToggleCustomRemoteResolution);
        layoutCustomRemoteResolution = findViewById(R.id.layoutCustomRemoteResolution);

        if (toggleCustomDisplayResolution.isChecked()) {
            layoutCustomRemoteResolution.setVisibility(View.VISIBLE);
        } else {
            layoutCustomRemoteResolution.setVisibility(View.GONE);
        }

        if (!toggleAutoRequestDisplayResolution.isChecked()) {
            layoutToggleCustomRemoteResolution.setVisibility(View.VISIBLE);
        } else {
            layoutToggleCustomRemoteResolution.setVisibility(View.GONE);
            layoutCustomRemoteResolution.setVisibility(View.GONE);
        }

        rdpWidth = findViewById(R.id.rdpWidth);
        rdpWidth.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                int res = 0;
                if (!s.toString().isEmpty()) {
                    try {
                        res = Integer.parseInt(s.toString());
                    } catch (NumberFormatException e) {
                        res = currentConnection.getRdpWidth();
                    }
                }
                currentConnection.setRdpWidth(res);
            }
        });
        rdpWidth.setText(Integer.toString(currentConnection.getRdpWidth()));
        rdpHeight = findViewById(R.id.rdpHeight);
        rdpHeight.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                int res = 0;
                if (!s.toString().isEmpty()) {
                    try {
                        res = Integer.parseInt(s.toString());
                    } catch (NumberFormatException e) {
                        res = currentConnection.getRdpHeight();
                    }
                }
                currentConnection.setRdpHeight(res);
            }
        });
        rdpHeight.setText(Integer.toString(currentConnection.getRdpHeight()));

        // Send the generated data back to the calling activity.
        Intent databackIntent = new Intent();
        databackIntent.putExtra(Constants.opaqueConnectionSettingsClassPath, currentConnection);
        setResult(Activity.RESULT_OK, databackIntent);
    }

    /**
     * Automatically linked with android:onClick to the toggleAudio button.
     * @param view
     */
    public void toggleAudioPlaybackSetting(View view) {
        ToggleButton s = (ToggleButton) view;
        if (s.isChecked()) {
            PermissionsManager.requestPermissions(this, PermissionGroups.RECORD_AUDIO, true);
        }
        currentConnection.setAudioPlaybackEnabled(s.isChecked());
    }

    /**
     * Automatically linked with android:onClick to the toggleUsbEnabled button.
     * @param view
     */
    public void toggleUsbEnabledSetting(View view) {
        ToggleButton s = (ToggleButton) view;
        currentConnection.setUsbEnabled(s.isChecked());
    }

    /**
     * Automatically linked with android:onClick to the toggleAutoRotation button.
     * @param view
     */
    public void toggleAutoRotation(View view) {
        ToggleButton s = (ToggleButton) view;
        currentConnection.setRotationEnabled(s.isChecked());
    }

    /**
     * Automatically linked with android:onClick to the toggleRotation button.
     * @param view
     */
    public void toggleAutoRequestDisplayResolution(View view) {
        ToggleButton s = (ToggleButton) view;
        boolean autoDisplayResolution = s.isChecked();
        currentConnection.setRequestingNewDisplayResolution(autoDisplayResolution);
        if (autoDisplayResolution) {
            layoutToggleCustomRemoteResolution.setVisibility(View.GONE);
            layoutCustomRemoteResolution.setVisibility(View.GONE);
        } else {
            layoutToggleCustomRemoteResolution.setVisibility(View.VISIBLE);
            layoutCustomRemoteResolution.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Automatically linked with android:onClick to the toggleRotation button.
     * @param view
     */
    public void toggleCustomDisplayResolution(View view) {
        ToggleButton s = (ToggleButton) view;
        boolean customDisplayResolution = s.isChecked();
        int resType = 0;
        if (customDisplayResolution) {
            resType = RemoteClientLibConstants.RDP_GEOM_SELECT_CUSTOM;
        }
        currentConnection.setRdpResType(resType);
        if (customDisplayResolution) {
            layoutCustomRemoteResolution.setVisibility(View.VISIBLE);
        } else {
            layoutCustomRemoteResolution.setVisibility(View.GONE);
        }
    }

    /**
     * Automatically linked with android:onClick to the toggleSslStrict button.
     * @param view
     */
    public void toggleSslStrict(View view) {
        ToggleButton s = (ToggleButton) view;
        currentConnection.setSslStrict(s.isChecked());
    }

    /**
     * Automatically linked with android:onClick to the toggleUsingCustomOvirtCa button.
     * @param view
     */
    public void toggleUsingCustomOvirtCa(View view) {
        ToggleButton s = (ToggleButton) view;
        boolean usingCustomOvirtCa = s.isChecked();
        currentConnection.setUsingCustomOvirtCa(usingCustomOvirtCa);
        buttonManageOvirtCa.setEnabled(usingCustomOvirtCa);
    }

    /**
     * Automatically linked with android:onClick to the positionToolbarLastUsed button.
     * @param view
     */
    public void toggleUseLastPositionToolbar(View view) {
        ToggleButton s = (ToggleButton) view;
        boolean useLastPositionToolbar = s.isChecked();
        currentConnection.setUseLastPositionToolbar(useLastPositionToolbar);
    }

    /**
     * Automatically linked with android:onClick to the buttonManageOvirtCa button.
     * @param view
     */
    public void showManageOvirtCaDialog(View view) {
        showCaDialog(ManageCustomCaFragment.TYPE_OVIRT);
    }

    private void showCaDialog(int caPurpose) {
        FragmentManager fm = this.getSupportFragmentManager();
        ManageCustomCaFragment newFragment = ManageCustomCaFragment.newInstance(caPurpose, currentConnection);
        newFragment.show(fm, "customCa");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Load list of items from asset folder and populate this:
        List<String> spinnerArray = null;
        try {
            spinnerArray = FileUtils.listFiles(this, "layouts");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinnerArray);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        layoutMapSpinner.setAdapter(adapter);

        int selection = spinnerArray.indexOf(currentConnection.getLayoutMap());
        if (selection < 0) {
            selection = spinnerArray.indexOf(RemoteClientLibConstants.DEFAULT_LAYOUT_MAP);
        }
        layoutMapSpinner.setSelection(selection);
    }

    @Override
    public void onFragmentDismissed(ConnectionSettings currentConnection) {
        this.currentConnection = currentConnection;
    }
}
