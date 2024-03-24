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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.iiordanov.bVNC.Constants;
import com.iiordanov.bVNC.Utils;
import com.undatech.remoteClientUi.R;

import java.util.Arrays;
import java.util.List;

public class ConnectionSetupActivity extends Activity {
    private static String TAG = "ConnectionSetupActivity";

    private EditText hostname = null;
    private EditText vmname = null;
    private EditText user = null;
    private EditText password = null;
    private CheckBox keepPass = null;
    private Button advancedSettingsButton = null;

    private Context appContext = null;
    private ConnectionSettings currentConnection = null;
    private String currentSelectedConnection = null;
    private String connectionsList = null;
    private String[] connectionsArray = null;
    private boolean newConnection = false;
    private Spinner spinnerConnectionType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appContext = getApplicationContext();
        setContentView(R.layout.connection_setup_activity);

        hostname = (EditText) findViewById(R.id.hostname);
        vmname = (EditText) findViewById(R.id.vmname);
        user = (EditText) findViewById(R.id.user);
        password = (EditText) findViewById(R.id.password);
        keepPass = (CheckBox) findViewById(R.id.checkboxKeepPassword);

        // Define what happens when one taps the Advanced Settings button.
        advancedSettingsButton = (Button) findViewById(R.id.advancedSettingsButton);
        advancedSettingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                saveSelectedPreferences(false);

                Intent intent = new Intent(ConnectionSetupActivity.this, AdvancedSettingsActivity.class);
                intent.putExtra(Constants.opaqueConnectionSettingsClassPath, currentConnection);
                startActivityForResult(intent, RemoteClientLibConstants.ADVANCED_SETTINGS);
            }
        });

        // Load any existing list of connection preferences.
        loadConnections();

        Intent i = getIntent();
        currentSelectedConnection = (String) i.getStringExtra("com.undatech.opaque.connectionToEdit");
        android.util.Log.e(TAG, "currentSelectedConnection SET TO: " + currentSelectedConnection);

        // If no currentSelectedConnection was passed in, then generate one.
        if (currentSelectedConnection == null) {
            currentSelectedConnection = nextLargestNumber(connectionsArray);
            newConnection = true;
        }

        spinnerConnectionType = (Spinner) findViewById(R.id.spinnerConnectionType);
        spinnerConnectionType.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view != null) {
                    android.util.Log.e(TAG, "Selected connection type: " +
                            Integer.toString(position) + " " + ((TextView) view).getText());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        currentConnection = new ConnectionSettings(currentSelectedConnection);
        if (newConnection) {
            // Load advanced settings defaults from the saved default settings
            currentConnection.loadAdvancedSettings(this, RemoteClientLibConstants.DEFAULT_SETTINGS_FILE);
            // Save the empty connection preferences to override any values of a previously
            // deleted connection.
            saveSelectedPreferences(false);
        }

        // Finally, load the preferences for the currentSelectedConnection.
        loadSelectedPreferences();
    }

    /**
     * Returns the string representation of N+1 where N is the largest value
     * in the array "numbers" when converted to an integer.
     * @return
     */
    private String nextLargestNumber(String[] numbers) {
        int maxValue = 0;
        if (numbers != null) {
            for (String num : numbers) {
                int currValue = 0;
                try {
                    currValue = Integer.parseInt(num);
                    if (currValue >= maxValue) {
                        maxValue = currValue + 1;
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        android.util.Log.e(TAG, "nextLargestNumber determined: " + maxValue);
        return Integer.toString(maxValue);
    }

    /**
     * Loads the space-separated string representing the saved connections, splits them,
     * also setting the appropriate member variables.
     * @return
     */
    private void loadConnections() {
        SharedPreferences sp = appContext.getSharedPreferences("generalSettings", Context.MODE_PRIVATE);
        connectionsList = sp.getString("connections", null);
        if (connectionsList != null && !connectionsList.trim().equals("")) {
            connectionsArray = connectionsList.split(" ");
        }
    }

    /**
     * Saves the space-separated string representing the saved connections,
     * and reloads the list to ensure the related member variables are consistent.
     */
    private void saveConnections() {
        // Only if this is a new connection do we need to add it to the list
        if (newConnection) {
            newConnection = false;

            String newListOfConnections = new String(currentSelectedConnection);
            if (connectionsArray != null) {
                for (int i = 0; i < connectionsArray.length; i++) {
                    newListOfConnections += " " + connectionsArray[i];
                }
            }

            android.util.Log.d(TAG, "Saving list of connections: " + newListOfConnections);
            SharedPreferences sp = appContext.getSharedPreferences("generalSettings", Context.MODE_PRIVATE);
            Editor editor = sp.edit();
            editor.putString("connections", newListOfConnections.trim());
            editor.apply();

            // Reload the list of connections from preferences for consistency.
            loadConnections();
        }
    }

    /**
     * This function is used to retrieve data returned by activities started with startActivityForResult.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        android.util.Log.i(TAG, "onActivityResult");

        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (RemoteClientLibConstants.ADVANCED_SETTINGS):
                if (resultCode == Activity.RESULT_OK) {
                    Bundle b = data.getExtras();
                    currentConnection = (ConnectionSettings) b.get(Constants.opaqueConnectionSettingsClassPath);
                    saveSelectedPreferences(false);
                } else {
                    android.util.Log.i(TAG, "Error during AdvancedSettingsActivity.");
                }
                break;
        }
    }

    /**
     * Loads the preferences from shared preferences and populates the on-screen Views.
     */
    private void loadSelectedPreferences() {
        // We use the index as the file name to which to save the connection.
        android.util.Log.i(TAG, "Loading current settings from file: " + currentSelectedConnection);
        currentConnection.loadFromSharedPreferences(appContext);
    }

    private void updateViewsFromPreferences() {
        List<String> connectionTypes = Arrays.asList(getResources().getStringArray(R.array.connection_types));
        spinnerConnectionType.setSelection(connectionTypes.indexOf(currentConnection.getConnectionTypeString()));
        hostname.setText(currentConnection.getHostname());
        vmname.setText(currentConnection.getVmname());
        user.setText(currentConnection.getUser());
        password.setText(currentConnection.getPassword());
        keepPass.setChecked(currentConnection.getKeepPassword());
    }

    /**
     * Saves the preferences which are selected on-screen by the user into shared preferences.
     */
    private void saveSelectedPreferences(boolean saveInList) {
        android.util.Log.i(TAG, "Saving current settings to file: " + currentSelectedConnection);

        String u = user.getText().toString();
        String h = hostname.getText().toString();

        // Only if a username and a hostname were entered, save the connection to list of connections.
        if (saveInList && !(u.equals("") || h.equals(""))) {
            saveConnections();
        }

        // Then, save the connection to a separate SharedPreferences file.
        currentConnection.setConnectionTypeString(spinnerConnectionType.getSelectedItem().toString());
        currentConnection.setUser(u);
        currentConnection.setHostname(h);
        currentConnection.setVmname(vmname.getText().toString());
        currentConnection.setPassword(password.getText().toString());
        currentConnection.setKeepPassword(keepPass.isChecked());
        currentConnection.saveToSharedPreferences(appContext);
    }

    @Override
    public void onStop() {
        super.onStop();
        android.util.Log.e(TAG, "onStop");
        //saveSelectedPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.e(TAG, "onResume");
        loadSelectedPreferences();
        updateViewsFromPreferences();
    }

    /**
     * Automatically linked with android:onClick to the toggleSslStrict button.
     * @param view
     */
    public void toggleConnectionType(View view) {
        view.cancelLongPress();
        //ToggleButton s = (ToggleButton) view;
        //currentConnection.setSslStrict(s.isChecked());  
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.connection_setup_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemID = menuItem.getItemId();
        return true;
    }

    public void showConnectionScreenHelp(MenuItem item) {
        Log.d(TAG, "Showing connection screen help.");
        Utils.createConnectionScreenDialog(this);
    }

    public void save(MenuItem item) {
        String u = user.getText().toString();
        String h = hostname.getText().toString();

        // Only if a username and a hostname were entered, save the connection.
        if (!(u.equals("") || h.equals(""))) {
            saveSelectedPreferences(true);
            finish();
            // Otherwise, let the user know that at least a user and hostname are required.
        } else {
            Toast toast = Toast.makeText(appContext, R.string.error_no_user_hostname, Toast.LENGTH_LONG);
            toast.show();
        }
    }
}
