/**
 * Copyright (C) 2012 Iordan Iordanov
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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

package com.iiordanov.bVNC;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.iiordanov.bVNC.dialogs.IntroTextDialog;
import com.iiordanov.util.PermissionGroups;
import com.iiordanov.util.PermissionsManager;
import com.morpheusly.common.Utilities;
import com.undatech.opaque.RemoteClientLibConstants;
import com.undatech.remoteClientUi.R;

import java.util.List;

/**
 * aRDP is the Activity for setting up RDP connections.
 */
public class aRDP extends MainConfiguration {
    private final static String TAG = "aRDP";

    private Spinner spinnerRdpGeometry;
    private EditText rdpDomain;
    private ToggleButton rdpGatewayEnabled;
    private LinearLayout layoutRdpGatewaySettings;
    private EditText rdpGatewayHostname;
    private EditText rdpGatewayPort;
    private EditText rdpGatewayUsername;
    private EditText rdpGatewayDomain;
    private EditText rdpGatewayPassword;
    private CheckBox checkboxKeepRdpGatewayPassword;
    private RadioGroup groupRemoteSoundType;
    private CheckBox checkboxEnableRecording;
    private CheckBox checkboxConsoleMode;
    private CheckBox checkboxRedirectSdCard;
    private CheckBox checkboxRemoteFx;
    private CheckBox checkboxDesktopBackground;
    private CheckBox checkboxFontSmoothing;
    private CheckBox checkboxDesktopComposition;
    private CheckBox checkboxWindowContents;
    private CheckBox checkboxMenuAnimation;
    private CheckBox checkboxVisualStyles;
    private CheckBox checkboxEnableGfx;
    private CheckBox checkboxEnableGfxH264;
    private CheckBox checkboxPreferSendingUnicode;
    private Spinner spinnerRdpColor;
    private List<String> rdpColorArray;

    @Override
    public void onCreate(Bundle icicle) {
        layoutID = R.layout.main_rdp;
        super.onCreate(icicle);
        setConnectionTypeSpinnerAdapter(R.array.rdp_connection_type);
        initializeRdpSpecificConnectionParameters();
        initializeRdpGatewaySettings();
        initializeRdpColorSpinner();
        initializeRdpResolutionSpinner();
        initializeAdvancedSettings();
    }

    private void initializeRdpSpecificConnectionParameters() {
        textUsername = findViewById(R.id.textUsername);
        rdpDomain = findViewById(R.id.rdpDomain);
    }

    private void initializeAdvancedSettings() {
        groupRemoteSoundType = findViewById(R.id.groupRemoteSoundType);
        groupRemoteSoundType.setOnCheckedChangeListener((radioGroup, selection) -> {
            if (Utils.isFree(aRDP.this) && selection != R.id.radioRemoteSoundDisabled) {
                setRemoteSoundTypeFromSelected(Constants.REMOTE_SOUND_DISABLED);
                IntroTextDialog.showIntroTextIfNecessary(aRDP.this, database, true);
            }
        });
        checkboxEnableRecording = findViewById(R.id.checkboxEnableRecording);
        checkboxConsoleMode = findViewById(R.id.checkboxConsoleMode);
        checkboxRedirectSdCard = findViewById(R.id.checkboxRedirectSdCard);
        checkboxRemoteFx = findViewById(R.id.checkboxRemoteFx);
        checkboxDesktopBackground = findViewById(R.id.checkboxDesktopBackground);
        checkboxFontSmoothing = findViewById(R.id.checkboxFontSmoothing);
        checkboxDesktopComposition = findViewById(R.id.checkboxDesktopComposition);
        checkboxWindowContents = findViewById(R.id.checkboxWindowContents);
        checkboxMenuAnimation = findViewById(R.id.checkboxMenuAnimation);
        checkboxVisualStyles = findViewById(R.id.checkboxVisualStyles);
        checkboxEnableGfx = findViewById(R.id.checkboxEnableGfx);
        checkboxEnableGfxH264 = findViewById(R.id.checkboxEnableGfxH264);
        checkboxPreferSendingUnicode = findViewById(R.id.checkboxPreferSendingUnicode);
    }

    private void initializeRdpResolutionSpinner() {
        // The geometry type and dimensions boxes.
        spinnerRdpGeometry = findViewById(R.id.spinnerRdpGeometry);
        spinnerRdpGeometry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int itemIndex, long id) {
                selected.setRdpResType(itemIndex);
                setRemoteWidthAndHeight(RemoteClientLibConstants.RDP_GEOM_SELECT_CUSTOM);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private void initializeRdpColorSpinner() {
        rdpColorArray = Utilities.Companion.toList(getResources().getStringArray(R.array.rdp_colors));
        spinnerRdpColor = findViewById(R.id.spinnerRdpColor);
        spinnerRdpColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int itemIndex, long id) {
                selected.setRdpColor(Integer.parseInt(rdpColorArray.get(itemIndex)));
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private void initializeRdpGatewaySettings() {
        layoutRdpGatewaySettings = findViewById(R.id.layoutRdpGatewaySettings);
        rdpGatewayEnabled = findViewById(R.id.rdpGatewayEnabled);
        rdpGatewayEnabled.setOnClickListener(v -> {
            layoutRdpGatewaySettings.setVisibility(((ToggleButton)v).isChecked() ? View.VISIBLE : View.GONE);
        });
        rdpGatewayHostname = findViewById(R.id.rdpGatewayHostname);
        rdpGatewayPort = findViewById(R.id.rdpGatewayPort);
        rdpGatewayUsername = findViewById(R.id.rdpGatewayUsername);
        rdpGatewayDomain = findViewById(R.id.rdpGatewayDomain);
        rdpGatewayPassword = findViewById(R.id.rdpGatewayPassword);
        checkboxKeepRdpGatewayPassword = findViewById(R.id.checkboxKeepRdpGatewayPassword);
    }

    @SuppressLint("SetTextI18n")
    public void updateViewFromSelected() {
        if (selected == null)
            return;
        super.updateViewFromSelected();
        setRdpSpecificSettingsFromSelected();
        setRdpColorSpinnerPositionFromSelected();
        setRdpGeometrySpinnerPositionFromSelected();
        setRemoteWidthAndHeight(RemoteClientLibConstants.RDP_GEOM_SELECT_CUSTOM);
        setRemoteSoundTypeFromSelected(selected.getRemoteSoundType());
        updateAdvancedSettingsViewsFromSelected();
    }

    @SuppressLint("SetTextI18n")
    private void setRdpSpecificSettingsFromSelected() {
        textUsername.setText(selected.getUserName());
        rdpDomain.setText(selected.getRdpDomain());
        boolean isRdpGatewayEnabled = selected.getRdpGatewayEnabled();
        rdpGatewayEnabled.setChecked(isRdpGatewayEnabled);
        layoutRdpGatewaySettings.setVisibility(isRdpGatewayEnabled ? View.VISIBLE : View.GONE);
        rdpGatewayHostname.setText(selected.getRdpGatewayHostname());
        rdpGatewayPort.setText(Integer.toString(selected.getRdpGatewayPort()));
        rdpGatewayUsername.setText(selected.getRdpGatewayUsername());
        rdpGatewayDomain.setText(selected.getRdpGatewayDomain());
        rdpGatewayPassword.setText(selected.getRdpGatewayPassword());
        checkboxKeepRdpGatewayPassword.setChecked(selected.getKeepRdpGatewayPassword());
    }

    private void setRdpGeometrySpinnerPositionFromSelected() {
        spinnerRdpGeometry.setSelection(selected.getRdpResType());
    }

    private void setRdpColorSpinnerPositionFromSelected() {
        spinnerRdpColor.setSelection(rdpColorArray.indexOf(String.valueOf(selected.getRdpColor())));
    }

    private void updateAdvancedSettingsViewsFromSelected() {
        checkboxEnableRecording.setChecked(selected.getEnableRecording());
        checkboxConsoleMode.setChecked(selected.getConsoleMode());
        checkboxRedirectSdCard.setChecked(selected.getRedirectSdCard());
        checkboxRemoteFx.setChecked(selected.getRemoteFx());
        checkboxDesktopBackground.setChecked(selected.getDesktopBackground());
        checkboxFontSmoothing.setChecked(selected.getFontSmoothing());
        checkboxDesktopComposition.setChecked(selected.getDesktopComposition());
        checkboxWindowContents.setChecked(selected.getWindowContents());
        checkboxMenuAnimation.setChecked(selected.getMenuAnimation());
        checkboxVisualStyles.setChecked(selected.getVisualStyles());
        checkboxEnableGfx.setChecked(selected.getEnableGfx());
        checkboxEnableGfxH264.setChecked(selected.getEnableGfxH264());
        checkboxPreferSendingUnicode.setChecked(selected.getPreferSendingUnicode());
    }

    protected void updateSelectedFromView() {
        if (selected == null) {
            return;
        }
        super.updateSelectedFromView();
        updateSelectedRdpSpecificSettingsFromViews();
        updateSelectedRdpResolutionTypeFromRdpGeometrySpinnerPosition();
        updateSelectedAdvancedSettingsFromViews();
    }

    private void updateSelectedRdpSpecificSettingsFromViews() {
        selected.setUserName(textUsername.getText().toString());
        selected.setRdpDomain(rdpDomain.getText().toString());
        selected.setRdpGatewayEnabled(rdpGatewayEnabled.isChecked());
        selected.setRdpGatewayHostname(rdpGatewayHostname.getText().toString());
        try {
            selected.setRdpGatewayPort(Integer.parseInt(rdpGatewayPort.getText().toString()));
        } catch (NumberFormatException nfe) {
            logAndPrintStacktrace(nfe);
        }
        selected.setRdpGatewayUsername(rdpGatewayUsername.getText().toString());
        selected.setRdpGatewayDomain(rdpGatewayDomain.getText().toString());
        selected.setRdpGatewayPassword(rdpGatewayPassword.getText().toString());
        selected.setKeepRdpGatewayPassword(checkboxKeepRdpGatewayPassword.isChecked());
    }

    private void updateSelectedRdpResolutionTypeFromRdpGeometrySpinnerPosition() {
        selected.setRdpResType(spinnerRdpGeometry.getSelectedItemPosition());
    }

    private void updateSelectedAdvancedSettingsFromViews() {
        setRemoteSoundTypeFromView(groupRemoteSoundType);
        selected.setEnableRecording(checkboxEnableRecording.isChecked());
        selected.setConsoleMode(checkboxConsoleMode.isChecked());
        selected.setRedirectSdCard(checkboxRedirectSdCard.isChecked());
        selected.setRemoteFx(checkboxRemoteFx.isChecked());
        selected.setDesktopBackground(checkboxDesktopBackground.isChecked());
        selected.setFontSmoothing(checkboxFontSmoothing.isChecked());
        selected.setDesktopComposition(checkboxDesktopComposition.isChecked());
        selected.setWindowContents(checkboxWindowContents.isChecked());
        selected.setMenuAnimation(checkboxMenuAnimation.isChecked());
        selected.setVisualStyles(checkboxVisualStyles.isChecked());
        selected.setEnableGfx(checkboxEnableGfx.isChecked());
        selected.setEnableGfxH264(checkboxEnableGfxH264.isChecked());
        selected.setPreferSendingUnicode(checkboxPreferSendingUnicode.isChecked());
    }

    /**
     * Automatically linked with android:onClick in the layout.
     */
    public void toggleEnableRecording(View view) {
        CheckBox b = (CheckBox) view;
        if (Utils.isFree(this)) {
            IntroTextDialog.showIntroTextIfNecessary(this, database, true);
            b.setChecked(false);
        } else {
            PermissionsManager.requestPermissions(this, PermissionGroups.RECORD_AND_MODIFY_AUDIO, true);
        }
        selected.setEnableRecording(b.isChecked());
    }

    /**
     * Automatically linked with android:onClick in the layout.
     */
    public void remoteSoundTypeToggled(View view) {
        if (Utils.isFree(this)) {
            IntroTextDialog.showIntroTextIfNecessary(this, database, true);
        }
    }

    /**
     * Sets the remote sound type in the settings from the specified parameter.
     */
    public void setRemoteSoundTypeFromView(View view) {
        RadioGroup g = (RadioGroup) view;
        int id = g.getCheckedRadioButtonId();
        int soundType = Constants.REMOTE_SOUND_DISABLED;
        if (id == R.id.radioRemoteSoundOnServer) {
            soundType = Constants.REMOTE_SOUND_ON_SERVER;
        } else if (id == R.id.radioRemoteSoundOnDevice) {
            soundType = Constants.REMOTE_SOUND_ON_DEVICE;
        }
        selected.setRemoteSoundType(soundType);
    }

    public void setRemoteSoundTypeFromSelected(int type) {
        if (Utils.isFree(this)) {
            type = Constants.REMOTE_SOUND_DISABLED;
        }

        int id = 0;
        switch (type) {
            case Constants.REMOTE_SOUND_DISABLED:
                id = R.id.radioRemoteSoundDisabled;
                break;
            case Constants.REMOTE_SOUND_ON_DEVICE:
                id = R.id.radioRemoteSoundOnDevice;
                break;
            case Constants.REMOTE_SOUND_ON_SERVER:
                id = R.id.radioRemoteSoundOnServer;
                break;
        }
        groupRemoteSoundType.check(id);
    }

    public void save(MenuItem item) {
        Log.d(TAG, "save called");
        if (ipText.getText().length() != 0 && portText.getText().length() != 0) {
            saveConnectionAndCloseLayout();
        } else {
            Toast.makeText(this, R.string.rdp_server_empty, Toast.LENGTH_LONG).show();
        }
    }
}
