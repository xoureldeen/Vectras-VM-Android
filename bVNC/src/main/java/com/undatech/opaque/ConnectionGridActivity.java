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

import static com.iiordanov.bVNC.Utils.createMainScreenDialog;
import static com.iiordanov.bVNC.Utils.setClipboard;
import static com.iiordanov.bVNC.Utils.startUriIntent;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.iiordanov.bVNC.App;
import com.iiordanov.bVNC.ConnectionBean;
import com.iiordanov.bVNC.Constants;
import com.iiordanov.bVNC.Database;
import com.iiordanov.bVNC.RemoteCanvasActivity;
import com.iiordanov.bVNC.Utils;
import com.iiordanov.bVNC.dialogs.GetTextFragment;
import com.iiordanov.bVNC.dialogs.ImportExportDialog;
import com.iiordanov.bVNC.dialogs.IntroTextDialog;
import com.iiordanov.bVNC.dialogs.RateOrShareFragment;
import com.iiordanov.bVNC.input.TouchInputHandlerDirectSwipePan;
import com.iiordanov.util.MasterPasswordDelegate;
import com.undatech.opaque.util.ConnectionLoader;
import com.undatech.opaque.util.FileUtils;
import com.undatech.opaque.util.GeneralUtils;
import com.undatech.opaque.util.LogcatReader;
import com.undatech.remoteClientUi.R;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectionGridActivity extends FragmentActivity implements GetTextFragment.OnFragmentDismissedListener {
    private static String TAG = "ConnectionGridActivity";
    protected Database database;
    protected boolean isStarting = true;
    FragmentManager fragmentManager = getSupportFragmentManager();
    GetTextFragment getPassword = null;
    GetTextFragment getNewPassword = null;
    private Context appContext;
    private GridView gridView;
    private EditText search;
    private boolean togglingMasterPassword = false;
    private AppCompatImageButton addNewConnection = null;

    private RateOrShareFragment rateOrShareFragment = new RateOrShareFragment();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appContext = getApplicationContext();
        setContentView(R.layout.grid_view_activity);

        gridView = (GridView) findViewById(R.id.gridView);
        gridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                launchConnection(v);
            }
        });
        gridView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ConnectionGridActivity.this);
                String gridItemText = (String) ((TextView) v.findViewById(R.id.grid_item_text)).getText();
                alertDialogBuilder.setTitle(getString(R.string.connection_edit_delete_prompt) + " " + gridItemText + " ?");
                CharSequence[] cs = {getString(R.string.connection_edit), getString(R.string.connection_delete)};
                alertDialogBuilder.setItems(cs, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        if (cs[item].toString() == getString(R.string.connection_edit)) {
                            editConnection(v);
                        } else if (cs[item].toString() == getString(R.string.connection_delete)) {
                            deleteConnection(v);
                        }
                    }
                });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                return true;
            }
        });

        search = findViewById(R.id.search);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                createAndSetLabeledImageAdapterAndNumberOfColumns();
            }
        });
        database = ((App) getApplication()).getDatabase();
        if (getPassword == null) {
            getPassword = GetTextFragment.newInstance(GetTextFragment.DIALOG_ID_GET_MASTER_PASSWORD,
                    getString(R.string.master_password_verify), this,
                    GetTextFragment.PasswordNoKeep, R.string.master_password_verify_message,
                    R.string.master_password_set_error, null, null, null, false);
        }
        if (getNewPassword == null) {
            getNewPassword = GetTextFragment.newInstance(GetTextFragment.DIALOG_ID_GET_MATCHING_MASTER_PASSWORDS,
                    getString(R.string.master_password_set), this,
                    GetTextFragment.MatchingPasswordTwice, R.string.master_password_set_message,
                    R.string.master_password_set_error, null, null, null, false);
        }
        FileUtils.logFilesInPrivateStorage(this);
        FileUtils.deletePrivateFileIfExisting(this, ".config/freerdp/licenses");
        addNewConnection = findViewById(R.id.addNewConnection);
        addNewConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewConnection();
            }
        });
    }

    private ConnectionLoader getConnectionLoader(Context context) {
        boolean connectionsInSharedPrefs = Utils.isOpaque(context);
        ConnectionLoader connectionLoader = new ConnectionLoader(appContext, this, connectionsInSharedPrefs);
        return connectionLoader;
    }

    private void createAndSetLabeledImageAdapterAndNumberOfColumns() {
        LabeledImageApapter labeledImageApapter = new LabeledImageApapter(
                ConnectionGridActivity.this,
                getConnectionLoader(this).loadConnectionsById(),
                search.getText().toString().toLowerCase().split(" "),
                2);
        gridView.setAdapter(labeledImageApapter);
        gridView.setNumColumns(labeledImageApapter.getNumCols());
    }

    private void launchConnection(View v) {
        Utils.hideKeyboard(this, getCurrentFocus());
        android.util.Log.i(TAG, "Launch Connection");

        ActivityManager.MemoryInfo info = Utils.getMemoryInfo(this);
        if (info.lowMemory)
            System.gc();

        String runtimeId = (String) ((TextView) v.findViewById(R.id.grid_item_id)).getText();
        Intent intent = new IntentHelper().getIntent(
                getConnectionLoader(this),
                runtimeId,
                appContext,
                this
        );
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Error launching connection: " + e);
            Toast.makeText(this, R.string.no_application_to_handle_vpn, Toast.LENGTH_LONG).show();
            startUriIntent(this, "market://search?q=pub:\"Morpheusly\"");
        }
    }

    private void editConnection(View v) {
        android.util.Log.d(TAG, "Modify Connection");
        String runtimeId = (String) ((TextView) v.findViewById(R.id.grid_item_id)).getText();
        ConnectionLoader connectionLoader = getConnectionLoader(this);
        Connection conn = connectionLoader.getConnectionsById().get(runtimeId);
        Intent intent = new Intent(ConnectionGridActivity.this, Utils.getConnectionSetupClass(this));
        if (Utils.isOpaque(this)) {
            ConnectionSettings cs = (ConnectionSettings) connectionLoader.getConnectionsById().get(runtimeId);
            intent.putExtra("com.undatech.opaque.connectionToEdit", cs.getFilename());

        } else {
            intent.putExtra("isNewConnection", false);
            intent.putExtra("connID", conn.getId());
        }
        startActivity(intent);
    }

    private void deleteConnection(View v) {
        android.util.Log.d(TAG, "Delete Connection");
        String runtimeId = (String) ((TextView) v.findViewById(R.id.grid_item_id)).getText();
        String gridItemText = (String) ((TextView) v.findViewById(R.id.grid_item_text)).getText();
        Utils.showYesNoPrompt(this, getString(R.string.delete_connection) + "?", getString(R.string.delete_connection) + " " + gridItemText + " ?",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        ConnectionLoader connectionLoader = getConnectionLoader(ConnectionGridActivity.this);
                        if (Utils.isOpaque(ConnectionGridActivity.this)) {

                            String newListOfConnections = new String();

                            SharedPreferences sp = appContext.getSharedPreferences("generalSettings", Context.MODE_PRIVATE);
                            String currentConnectionsStr = sp.getString("connections", null);

                            ConnectionSettings cs = (ConnectionSettings) connectionLoader.getConnectionsById().get(runtimeId);
                            if (sp != null) {
                                String[] currentConnections = currentConnectionsStr.split(" ");
                                for (String connection : currentConnections) {
                                    if (!connection.equals(cs.getFilename())) {
                                        newListOfConnections += " " + connection;
                                    }
                                }
                                android.util.Log.d(TAG, "Deleted connection, current list: " + newListOfConnections);
                                Editor editor = sp.edit();
                                editor.putString("connections", newListOfConnections.trim());
                                editor.apply();
                                File toDelete = new File(getFilesDir() + "/" + cs.getFilename() + ".png");
                                toDelete.delete();
                            }
                        } else {
                            ConnectionBean conn = (ConnectionBean) connectionLoader.getConnectionsById().get(runtimeId);
                            conn.Gen_delete(database.getWritableDatabase());
                            database.close();
                        }
                        onResume();
                    }
                }, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume of version " + Utils.getVersionAndCode(this));
        if (Utils.querySharedPreferenceBoolean(this, Constants.masterPasswordEnabledTag)) {
            showGetTextFragment(getPassword);
        } else {
            loadSavedConnections();
            IntroTextDialog.showIntroTextIfNecessary(this, database, Utils.isFree(this) && isStarting);
        }
        isStarting = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        if (database != null)
            database.close();
    }

    @Override
    protected void onResumeFragments() {
        Log.i(TAG, "onResumeFragments called");
        super.onResumeFragments();
        System.gc();
        if (Utils.querySharedPreferenceBoolean(this, Constants.masterPasswordEnabledTag)) {
            showGetTextFragment(getPassword);
        } else {
            loadSavedConnections();
        }
    }

    private void loadSavedConnections() {
        createAndSetLabeledImageAdapterAndNumberOfColumns();
    }

    /**
     * Starts a new connection.
     */
    public void addNewConnection() {
        Intent intent = new Intent(ConnectionGridActivity.this,
                Utils.getConnectionSetupClass(this));
        intent.putExtra("isNewConnection", true);
        startActivity(intent);
    }

    /**
     * Linked with android:onClick to the add new connection action bar item.
     */
    public void addNewConnection(MenuItem menuItem) {
        addNewConnection();
    }

    /**
     * Linked with android:onClick to the add new connection item in the activity.
     */
    public void addNewConnection(View view) {
        addNewConnection();
    }

    /**
     * Linked with android:onClick to the copyLogcat action bar item.
     *
     * @param menuItem
     */
    public void copyLogcat(MenuItem menuItem) {
        LogcatReader logcatReader = new LogcatReader();
        setClipboard(this, logcatReader.getMyLogcat(RemoteClientLibConstants.LOGCAT_MAX_LINES));
        Toast.makeText(getBaseContext(), getResources().getString(R.string.log_copied),
                Toast.LENGTH_LONG).show();
    }

    /**
     * Linked with android:onClick to the edit default settings action bar item.
     *
     * @param menuItem
     */
    public void editDefaultSettings(MenuItem menuItem) {
        android.util.Log.d(TAG, "editDefaultSettings selected.");
        if (Utils.isOpaque(this)) {
            Intent intent = new Intent(ConnectionGridActivity.this, GeneralUtils.getClassByName("com.undatech.opaque.AdvancedSettingsActivity"));
            ConnectionSettings defaultConnection = new ConnectionSettings(RemoteClientLibConstants.DEFAULT_SETTINGS_FILE);
            defaultConnection.loadFromSharedPreferences(getApplicationContext());
            intent.putExtra(Constants.opaqueConnectionSettingsClassPath, defaultConnection);
            startActivityForResult(intent, RemoteClientLibConstants.DEFAULT_SETTINGS);
        } else {
            Intent intent = new Intent();
            intent.setClassName(this, "com.iiordanov.bVNC.GlobalPreferencesActivity");
            startActivity(intent);
        }
    }

    /**
     * Linked with android:onClick to share or rate action bar item.
     *
     * @param menuItem
     */
    public void rateOrShare(MenuItem menuItem) {
        android.util.Log.d(TAG, "rateOrShare selected.");
        if (!rateOrShareFragment.isVisible()) {
            rateOrShareFragment.show(fragmentManager, "");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.grid_view_activity_actions, menu);
        inflater.inflate(R.menu.input_mode_menu_item, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * This function is used to retrieve data returned by activities started with startActivityForResult.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        android.util.Log.i(TAG, "onActivityResult");

        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RemoteClientLibConstants.DEFAULT_SETTINGS:
                if (resultCode == Activity.RESULT_OK) {
                    Bundle b = data.getExtras();
                    ConnectionSettings defaultSettings = (ConnectionSettings) b.get(Constants.opaqueConnectionSettingsClassPath);
                    defaultSettings.saveToSharedPreferences(this);
                } else {
                    android.util.Log.i(TAG, "Error during AdvancedSettingsActivity.");
                }
                break;
            case RemoteClientLibConstants.IMPORT_SETTINGS_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null && data.getData() != null) {
                        Uri uri = data.getData();
                        ContentResolver resolver = getContentResolver();

                        boolean connectionsInSharedPrefs = Utils.isOpaque(this);
                        InputStream in = FileUtils.getInputStreamFromUri(resolver, uri);
                        if (in == null) {
                            CharSequence error = getString(R.string.error) + ": " + uri;
                            Toast.makeText(appContext, error, Toast.LENGTH_LONG).show();
                            break;
                        }
                        if (connectionsInSharedPrefs) {
                            ConnectionSettings.importSettingsFromJsonToSharedPrefs(in, this);
                        } else {
                            Utils.importSettingsFromXml(in, database.getWritableDatabase());
                        }
                        recreate();
                    } else {
                        android.util.Log.e(TAG, "File uri not found, not importing settings");
                    }
                } else {
                    android.util.Log.e(TAG, "Error while selecting file to import settings from");
                }
                break;
            case RemoteClientLibConstants.EXPORT_SETTINGS_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null && data.getData() != null) {
                        ContentResolver resolver = getContentResolver();

                        boolean connectionsInSharedPrefs = Utils.isOpaque(this);
                        OutputStream out = FileUtils.getOutputStreamFromUri(resolver, data.getData());
                        if (connectionsInSharedPrefs) {
                            ConnectionSettings.exportSettingsFromSharedPrefsToJson(out, this);
                        } else {
                            Utils.exportSettingsToXml(out, database.getReadableDatabase());
                        }
                    } else {
                        android.util.Log.e(TAG, "File uri not found, not exporting settings");
                    }
                } else {
                    android.util.Log.e(TAG, "Error while selecting file to export settings to");
                }
                break;
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onMenuOpened(int, android.view.Menu)
     */
    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        android.util.Log.d(TAG, "onMenuOpened");
        try {
            updateInputMenu(menu.findItem(R.id.itemInputMode).getSubMenu());
            MenuItem itemMasterPassword = menu.findItem(R.id.itemMasterPassword);
            itemMasterPassword.setChecked(Utils.querySharedPreferenceBoolean(this, Constants.masterPasswordEnabledTag));
        } catch (NullPointerException e) {
        }
        return true;
    }

    /**
     * Check the right item in the input mode sub-menu
     */
    void updateInputMenu(Menu inputMenu) {
        MenuItem[] inputModeMenuItems = new MenuItem[RemoteCanvasActivity.inputModeIds.length];
        for (int i = 0; i < RemoteCanvasActivity.inputModeIds.length; i++) {
            inputModeMenuItems[i] = inputMenu.findItem(RemoteCanvasActivity.inputModeIds[i]);
        }
        String defaultInputHandlerId = Utils.querySharedPreferenceString(
                this, Constants.defaultInputMethodTag, TouchInputHandlerDirectSwipePan.ID);
        android.util.Log.d(TAG, "Default Input Mode Item: " + defaultInputHandlerId);

        try {
            for (MenuItem item : inputModeMenuItems) {
                android.util.Log.d(TAG, "Input Mode Item: " +
                        RemoteCanvasActivity.inputModeMap.get(item.getItemId()));

                if (defaultInputHandlerId.equals(RemoteCanvasActivity.inputModeMap.get(item.getItemId()))) {
                    item.setChecked(true);
                }
            }
        } catch (NullPointerException e) {
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == R.layout.importexport) {
            boolean connectionsInSharedPrefs = Utils.isOpaque(this);
            return new ImportExportDialog(this, database, connectionsInSharedPrefs);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.itemExportImport) {
            showDialog(R.layout.importexport);
        } else if (itemId == R.id.itemMasterPassword) {
            if (Utils.isFree(this)) {
                IntroTextDialog.showIntroTextIfNecessary(this, database, true);
            } else {
                togglingMasterPassword = true;
                if (Utils.querySharedPreferenceBoolean(this, Constants.masterPasswordEnabledTag)) {
                    showGetTextFragment(getPassword);
                } else {
                    showGetTextFragment(getNewPassword);
                }
            }
        } else if (item.getGroupId() == R.id.itemInputModeGroup) {
            Log.d(TAG, RemoteCanvasActivity.inputModeMap.get(item.getItemId()));
            Utils.setSharedPreferenceString(this, Constants.defaultInputMethodTag,
                    RemoteCanvasActivity.inputModeMap.get(item.getItemId()));
        }
        return true;
    }

    public void onTextObtained(String dialogId, String[] obtainedStrings, boolean wasCancelled, boolean keep) {
        Log.i(TAG, "onTextObtained");
        handlePassword(obtainedStrings[0], wasCancelled);
    }

    public void handlePassword(String providedPassword, boolean dialogWasCancelled) {
        Log.i(TAG, "handlePassword");
        boolean loadConnections;
        MasterPasswordDelegate passwordDelegate = new MasterPasswordDelegate(this, database);
        if (togglingMasterPassword) {
            loadConnections = passwordDelegate.toggleMasterPassword(providedPassword, dialogWasCancelled);
            togglingMasterPassword = false;
        } else {
            loadConnections = passwordDelegate.checkMasterPasswordAndQuitIfWrong(providedPassword, dialogWasCancelled);
        }
        if (loadConnections) {
            removeGetPasswordFragments();
            loadSavedConnections();
        }
    }

    private void showGetTextFragment(GetTextFragment f) {
        if (!f.isVisible()) {
            removeGetPasswordFragments();
            f.setCancelable(false);
            f.show(fragmentManager, "");
        }
    }

    private void removeGetPasswordFragments() {
        if (getPassword.isAdded()) {
            FragmentTransaction tx = this.getSupportFragmentManager().beginTransaction();
            tx.remove(getPassword);
            tx.commit();
            fragmentManager.executePendingTransactions();
        }
        if (getNewPassword.isAdded()) {
            FragmentTransaction tx = this.getSupportFragmentManager().beginTransaction();
            tx.remove(getNewPassword);
            tx.commit();
            fragmentManager.executePendingTransactions();
        }
    }

    public void showMainScreenHelp(View item) {
        Log.d(TAG, "showMainScreenHelp: Showing main screen help.");
        createMainScreenDialog(this);
    }

    public void showSupportForum(View item) {
        startUriIntent(this, "https://groups.google.com/forum/#!forum/bvnc-ardp-aspice-opaque-remote-desktop-clients");
    }

    public void emailUs(View item) {
        startUriIntent(this, "mailto:support@morpheusly.com");
    }

    public void reportBug(View item) {
        startUriIntent(this, "https://github.com/iiordanov/remote-desktop-clients/issues");
    }

    public void rateApp(View item) {
        Log.d(TAG, "rateApp: Showing rate app functionality");
        Utils.showRateAppDialog(this);
    }

    public void shareApp(View item) {
        Log.d(TAG, "shareApp: Copying app link to clipboard");
        String url = Utils.getDonationPackageUrl(this);
        setClipboard(this, url);
        Toast.makeText(appContext, R.string.share_app_toast, Toast.LENGTH_LONG).show();
    }

    public void donateToProject(View item) {
        startUriIntent(this, Utils.getDonationPackageLink(this));
    }

    public void moreApps(View item) {
        startUriIntent(this, "market://search?q=pub:\"Iordan Iordanov (Undatech)\"");
    }

    public void previousVersions(View item) {
        startUriIntent(this, "https://github.com/iiordanov/remote-desktop-clients/releases");
    }
}
