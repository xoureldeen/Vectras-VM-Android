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
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.iiordanov.bVNC.dialogs.ImportTlsCaDialog;
import com.iiordanov.util.PermissionGroups;
import com.iiordanov.util.PermissionsManager;
import com.morpheusly.common.Utilities;
import com.undatech.opaque.RemoteClientLibConstants;
import com.undatech.opaque.util.FileUtils;
import com.undatech.remoteClientUi.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * aSPICE is the Activity for setting up SPICE connections.
 */
public class aSPICE extends MainConfiguration {
    private final static String TAG = "aSPICE";

    private EditText tlsPort;
    private Spinner spinnerGeometry;
    private CheckBox checkboxEnableSound;
    private Spinner layoutMapSpinner = null;
    private List<String> spinnerArray = null;

    @Override
    public void onCreate(Bundle icicle) {
        layoutID = R.layout.main_spice;
        super.onCreate(icicle);

        tlsPort = findViewById(R.id.tlsPort);

        Button buttonImportCa = findViewById(R.id.buttonImportCa);
        buttonImportCa.setOnClickListener(view -> {
            aSPICE.this.updateSelectedFromView();
            showDialog(R.layout.import_tls_ca_dialog);
        });

        checkboxEnableSound = findViewById(R.id.checkboxEnableSound);

        // The geometry type and dimensions boxes.
        spinnerGeometry = findViewById(R.id.spinnerRdpGeometry);
        spinnerGeometry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int itemIndex, long id) {
                selected.setRdpResType(itemIndex);
                setRemoteWidthAndHeight(RemoteClientLibConstants.RDP_GEOM_SELECT_CUSTOM);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        // Load list of items from asset folder and populate
        try {
            spinnerArray = FileUtils.listFiles(this, "layouts");
        } catch (IOException e) {
            e.printStackTrace();
        }
        layoutMapSpinner = findViewById(R.id.layoutMaps);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerArray);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        layoutMapSpinner.setAdapter(adapter);
        setConnectionTypeSpinnerAdapter(R.array.spice_connection_type);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == R.layout.import_tls_ca_dialog) {
            return new ImportTlsCaDialog(this, database);
        }
        return null;
    }

    @SuppressLint("SetTextI18n")
    public void updateViewFromSelected() {
        if (selected == null)
            return;
        super.updateViewFromSelected();

        if (selected.getPort() < 0) {
            portText.setText("");
        } else {
            portText.setText(Integer.toString(selected.getPort()));
        }

        if (selected.getTlsPort() < 0) {
            tlsPort.setText("");
        } else {
            tlsPort.setText(Integer.toString(selected.getTlsPort()));
        }

        if (selected.getEnableSound()) {
            PermissionsManager.requestPermissions(this, PermissionGroups.RECORD_AUDIO, true);
        }
        checkboxEnableSound.setChecked(selected.getEnableSound());
        spinnerGeometry.setSelection(selected.getRdpResType());

        setRemoteWidthAndHeight(RemoteClientLibConstants.RDP_GEOM_SELECT_CUSTOM);

        // Write out CA to file if it doesn't exist.
        writeCaToFileIfNotThere(selected.getCaCert());

        int selection = spinnerArray.indexOf(selected.getLayoutMap());
        if (selection < 0) {
            selection = spinnerArray.indexOf(Constants.DEFAULT_LAYOUT_MAP);
        }
        layoutMapSpinner.setSelection(selection);
    }

    private void writeCaToFileIfNotThere(String caCertData) {
        try {
            // If a cert has been set, write out a unique file containing the cert and save the path to that file to give to libspice.
            String filename = getFilesDir() + "/ca" + selected.getCaCert().hashCode() + ".pem";
            selected.setCaCertPath(filename);
            File file = new File(filename);
            if (!file.exists() && !caCertData.equals("")) {
                Log.e(TAG, filename);
                PrintWriter fOut = new PrintWriter(filename);
                fOut.println(selected.getCaCert());
                fOut.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    protected void updateSelectedFromView() {
        if (selected == null) {
            return;
        }
        super.updateSelectedFromView();

        String port = portText.getText().toString();
        if (port.equals("")) {
            selected.setPort(-1);
        }
        try {
            selected.setTlsPort(Integer.parseInt(tlsPort.getText().toString()));
        } catch (NumberFormatException nfe) {
            logAndPrintStacktrace(nfe);
        }
        String tlsPort = this.tlsPort.getText().toString();
        if (tlsPort.equals("")) {
            selected.setTlsPort(-1);
        }
        selected.setRdpResType(spinnerGeometry.getSelectedItemPosition());
        selected.setEnableSound(checkboxEnableSound.isChecked());

        if (layoutMapSpinner != null) {
            TextView layoutMapSelection = (TextView) layoutMapSpinner.getSelectedView();
            if (layoutMapSelection != null) {
                selected.setLayoutMap(layoutMapSelection.getText().toString());
            }
        }
    }

    /**
     * Automatically linked with android:onClick in the layout.
     */
    public void toggleEnableSound(View view) {
        CheckBox b = (CheckBox) view;
        PermissionsManager.requestPermissions(this, PermissionGroups.RECORD_AND_MODIFY_AUDIO, true);
        selected.setEnableSound(b.isChecked());
    }

    public void save(MenuItem item) {
        if (ipText.getText().length() != 0
                && (portText.getText().length() != 0 || tlsPort.getText().length() != 0)) {
            saveConnectionAndCloseLayout();
        } else {
            Toast.makeText(this, R.string.spice_server_empty, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        android.util.Log.i(TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ImportTlsCaDialog.IMPORT_CA_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null && data.getData() != null) {
                    String keyData = Utilities.Companion.getStringDataFromIntent(data, this);
                    Log.i(TAG, "onActivityResult, keyData: " + keyData);
                    selected.setCaCert(keyData);
                    updateViewFromSelected();
                    selected.saveAndWriteRecent(false, this);
                    showDialog(R.layout.import_tls_ca_dialog);
                } else {
                    Toast.makeText(this, R.string.ca_file_error_reading, Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
