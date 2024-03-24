package com.iiordanov.bVNC;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.iiordanov.pubkeygenerator.GeneratePubkeyActivity;
import com.undatech.opaque.util.LogcatReader;
import com.undatech.remoteClientUi.R;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public abstract class MainConfiguration extends FragmentActivity {
    private final static String TAG = "MainConfiguration";
    protected ConnectionBean selected;
    protected Database database;
    protected EditText textNickname;
    protected int layoutID;
    protected Spinner connectionType;
    protected int selectedConnType;
    protected EditText ipText;
    protected boolean isNewConnection;
    private RadioGroup radioCursor;
    private TextView sshCaption;
    private LinearLayout sshCredentials;
    private LinearLayout layoutUseSshPubkey;
    private LinearLayout sshServerEntry;
    private EditText sshPassword;
    private EditText sshPassphrase;
    private CheckBox checkboxKeepSshPass;
    private long connID = 0;
    protected EditText sshServer;
    protected EditText sshPort;
    protected EditText sshUser;
    protected EditText portText;
    protected EditText passwordText;
    protected ToggleButton toggleAdvancedSettings;
    protected EditText textUsername;
    protected EditText resWidth;
    protected EditText resHeight;
    protected CheckBox checkboxKeepPassword;
    protected CheckBox checkboxUseDpadAsArrows;
    protected CheckBox checkboxRotateDpad;
    protected CheckBox checkboxUseLastPositionToolbar;
    protected CheckBox checkboxUseSshPubkey;
    protected LinearLayout layoutAdvancedSettings;

    @SuppressLint("SetTextI18n")
    public void updateViewFromSelected() {
        Log.d(TAG, "UpdateViewFromSelected called");
        selected.loadFromSharedPreferences(this);
        textNickname.setText(selected.getNickname());
        selectedConnType = selected.getConnectionType();
        connectionType.setSelection(selectedConnType);
        sshServer.setText(selected.getSshServer());
        sshPort.setText(Integer.toString(selected.getSshPort()));
        sshUser.setText(selected.getSshUser());
        checkboxUseSshPubkey.setChecked(selected.getUseSshPubKey());
        checkboxKeepSshPass.setChecked(selected.getKeepSshPassword());
        if (selected.getKeepSshPassword() || selected.getSshPassword().length() > 0) {
            sshPassword.setText(selected.getSshPassword());
        } else {
            sshPassword.setText("");
        }
        if (selected.getKeepSshPassword() || selected.getSshPassPhrase().length() > 0) {
            sshPassphrase.setText(selected.getSshPassPhrase());
        } else {
            sshPassphrase.setText("");
        }
        if (selectedConnType == Constants.CONN_TYPE_SSH && selected.getAddress().equals("")) {
            ipText.setText("localhost");
        } else {
            ipText.setText(selected.getAddress());
        }
        portText.setText(Integer.toString(selected.getPort()));
        if (selected.getKeepPassword() || selected.getPassword().length() > 0) {
            passwordText.setText(selected.getPassword());
        }
        checkboxKeepPassword.setChecked(selected.getKeepPassword());
        checkboxUseDpadAsArrows.setChecked(selected.getUseDpadAsArrows());
        checkboxRotateDpad.setChecked(selected.getRotateDpad());
        checkboxUseLastPositionToolbar.setChecked(
                (!isNewConnection) ? selected.getUseLastPositionToolbar() : this.useLastPositionToolbarDefault()
        );
        if (selected.getUseLocalCursor() == Constants.CURSOR_AUTO) {
            radioCursor.check(R.id.radioCursorAuto);
        } else if (selected.getUseLocalCursor() == Constants.CURSOR_FORCE_LOCAL) {
            radioCursor.check(R.id.radioCursorForceLocal);
        } else if (selected.getUseLocalCursor() == Constants.CURSOR_FORCE_DISABLE) {
            radioCursor.check(R.id.radioCursorForceDisable);
        }
        resWidth.setText(Integer.toString(selected.getRdpWidth()));
        resHeight.setText(Integer.toString(selected.getRdpHeight()));
    }

    protected void updateSelectedFromView() {
        Log.d(TAG, "updateSelectedFromView called");
        selected.setConnectionType(selectedConnType);

        selected.setNickname(textNickname.getText().toString());
        selected.setSshServer(sshServer.getText().toString());
        try {
            selected.setSshPort(Integer.parseInt(sshPort.getText().toString()));
        } catch (NumberFormatException nfe) {
            logAndPrintStacktrace(nfe);
        }
        selected.setSshUser(sshUser.getText().toString());
        selected.setSshPassPhrase(sshPassphrase.getText().toString());
        selected.setSshPassword(sshPassword.getText().toString());
        selected.setKeepSshPassword(checkboxKeepSshPass.isChecked());
        // If we are using an SSH key, then the ssh password box is used
        // for the key pass-phrase instead.
        selected.setUseSshPubKey(checkboxUseSshPubkey.isChecked());

        selected.setAddress(ipText.getText().toString());

        try {
            selected.setPort(Integer.parseInt(portText.getText().toString()));
        } catch (NumberFormatException nfe) {
            logAndPrintStacktrace(nfe);
        }

        if (radioCursor.getCheckedRadioButtonId() == R.id.radioCursorAuto) {
            selected.setUseLocalCursor(Constants.CURSOR_AUTO);
        } else if (radioCursor.getCheckedRadioButtonId() == R.id.radioCursorForceLocal) {
            selected.setUseLocalCursor(Constants.CURSOR_FORCE_LOCAL);
        } else if (radioCursor.getCheckedRadioButtonId() == R.id.radioCursorForceDisable) {
            selected.setUseLocalCursor(Constants.CURSOR_FORCE_DISABLE);
        }

        selected.setPassword(passwordText.getText().toString());
        selected.setKeepPassword(checkboxKeepPassword.isChecked());
        selected.setUseDpadAsArrows(checkboxUseDpadAsArrows.isChecked());
        selected.setRotateDpad(checkboxRotateDpad.isChecked());
        selected.setUseLastPositionToolbar(checkboxUseLastPositionToolbar.isChecked());
        if (!checkboxUseLastPositionToolbar.isChecked()) {
            selected.setUseLastPositionToolbarMoved(false);
        }
        try {
            selected.setRdpWidth(Integer.parseInt(resWidth.getText().toString()));
            selected.setRdpHeight(Integer.parseInt(resHeight.getText().toString()));
        } catch (NumberFormatException nfe) {
            logAndPrintStacktrace(nfe);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        Log.d(TAG, "onCreate called");
        Intent intent = getIntent();
        isNewConnection = intent.getBooleanExtra("isNewConnection", false);
        initializeConnId(intent.getStringExtra("connID"));
        super.onCreate(icicle);
        Utils.showMenu(this);
        setContentView(layoutID);
        System.gc();
        textNickname = findViewById(R.id.textNickname);
        sshServer = findViewById(R.id.sshServer);
        sshPort = findViewById(R.id.sshPort);
        sshUser = findViewById(R.id.sshUser);
        sshPassword = findViewById(R.id.sshPassword);
        checkboxKeepSshPass = findViewById(R.id.checkboxKeepSshPass);
        sshPassphrase = findViewById(R.id.sshPassphrase);
        ipText = findViewById(R.id.textIP);
        portText = findViewById(R.id.textPORT);
        passwordText = findViewById(R.id.textPASSWORD);
        resWidth = findViewById(R.id.rdpWidth);
        resHeight = findViewById(R.id.rdpHeight);
        checkboxKeepPassword = findViewById(R.id.checkboxKeepPassword);
        checkboxUseSshPubkey = findViewById(R.id.checkboxUseSshPubkey);
        checkboxUseDpadAsArrows = findViewById(R.id.checkboxUseDpadAsArrows);
        checkboxRotateDpad = findViewById(R.id.checkboxRotateDpad);
        checkboxUseLastPositionToolbar = findViewById(R.id.checkboxUseLastPositionToolbar);
        // Here we say what happens when the Pubkey Generate button is pressed.
        Button buttonGeneratePubkey = findViewById(R.id.buttonGeneratePubkey);
        buttonGeneratePubkey.setOnClickListener(view -> generatePubkey());
        TextView versionAndCode = findViewById(R.id.versionAndCode);
        versionAndCode.setText(Utils.getVersionAndCode(this));

        database = ((App) getApplication()).getDatabase();

        (findViewById(R.id.copyLogcat)).setOnClickListener(v -> {
            LogcatReader logcatReader = new LogcatReader();
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setText(logcatReader.getMyLogcat(Constants.LOGCAT_MAX_LINES));
            Toast.makeText(getBaseContext(), getResources().getString(R.string.log_copied),
                    Toast.LENGTH_LONG).show();
        });

        radioCursor = findViewById(R.id.radioCursor);

        sshCredentials = findViewById(R.id.sshCredentials);
        sshCaption = findViewById(R.id.sshCaption);
        layoutUseSshPubkey = findViewById(R.id.layoutUseSshPubkey);
        sshServerEntry = findViewById(R.id.sshServerEntry);

        // Define what happens when somebody selects different connection types.
        connectionType = findViewById(R.id.connectionType);
        connectionType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onItemSelected(AdapterView<?> ad, View view, int itemIndex, long id) {
                selectedConnType = itemIndex;
                selected.setConnectionType(selectedConnType);
                selected.save(MainConfiguration.this);
                if (selectedConnType == Constants.CONN_TYPE_PLAIN) {
                    setVisibilityOfSshWidgets(View.GONE);
                } else if (selectedConnType == Constants.CONN_TYPE_SSH) {
                    setVisibilityOfSshWidgets(View.VISIBLE);
                    if (ipText.getText().toString().equals(""))
                        ipText.setText("localhost");
                }
                updateViewFromSelected();
            }

            @Override
            public void onNothingSelected(AdapterView<?> ad) {
            }
        });

        // The advanced settings button.
        toggleAdvancedSettings = findViewById(R.id.toggleAdvancedSettings);
        layoutAdvancedSettings = findViewById(R.id.layoutAdvancedSettings);
        toggleAdvancedSettings.setOnCheckedChangeListener((arg0, checked) -> {
            if (checked)
                layoutAdvancedSettings.setVisibility(View.VISIBLE);
            else
                layoutAdvancedSettings.setVisibility(View.GONE);
        });
    }

    private void initializeConnId(String connIdStr) {
        if (!isNewConnection && connIdStr != null) {
            try {
                connID = Long.parseLong(connIdStr);
            } catch (NumberFormatException e) {
                connID = 0;
                Log.e(TAG, "Could not parse connection to edit from connID!");
                e.printStackTrace();
            }
        }
    }

    /**
     * Enables and disables the EditText boxes for width and height of remote desktop.
     */
    protected void setRemoteWidthAndHeight(int customResType) {
        Log.d(TAG, "setRemoteWidthAndHeight called");
        if (selected.getRdpResType() != customResType) {
            resWidth.setEnabled(false);
            resHeight.setEnabled(false);
        } else {
            resWidth.setEnabled(true);
            resHeight.setEnabled(true);
        }
    }


    void setConnectionTypeSpinnerAdapter(int arrayId) {
        Log.d(TAG, "setConnectionTypeSpinnerAdapter called");
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                arrayId, R.layout.connection_list_entry);
        adapter.setDropDownViewResource(R.layout.connection_list_entry);
        connectionType.setAdapter(adapter);
    }

    /**
     * Makes the ssh-related widgets visible/invisible.
     */
    protected void setVisibilityOfSshWidgets(int visibility) {
        Log.d(TAG, "setVisibilityOfSshWidgets called");
        sshCredentials.setVisibility(visibility);
        sshCaption.setVisibility(visibility);
        layoutUseSshPubkey.setVisibility(visibility);
        sshServerEntry.setVisibility(visibility);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart called");
        super.onStart();
        System.gc();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume called");
        super.onResume();
        System.gc();
    }

    @Override
    protected void onResumeFragments() {
        Log.d(TAG, "onResumeFragments called");
        super.onResumeFragments();
        arriveOnPage();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged called");
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
        if (database != null)
            database.close();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
        if (database != null)
            database.close();
        if (selected != null) {
            updateSelectedFromView();
            selected.saveAndWriteRecent(false, this);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy called");
        if (database != null)
            database.close();
        System.gc();
        super.onDestroy();
    }

    protected void saveConnectionAndCloseLayout() {
        Log.d(TAG, "saveConnectionAndCloseLayout called");
        if (selected != null) {
            updateSelectedFromView();
            selected.saveAndWriteRecent(false, this);
        }
        finish();
    }

    public void arriveOnPage() {
        Log.d(TAG, "arriveOnPage called");
        if (!isNewConnection) {
            SQLiteDatabase db = database.getReadableDatabase();
            ArrayList<ConnectionBean> connections = new ArrayList<>();
            ConnectionBean.getAll(db,
                    ConnectionBean.GEN_TABLE_NAME, connections,
                    ConnectionBean.newInstance);
            Collections.sort(connections);
            connections.add(0, new ConnectionBean(this));
            for (int i = 1; i < connections.size(); ++i) {

                if (connections.get(i).get_Id() == connID) {
                    selected = connections.get(i);
                    break;
                }
            }
            database.close();
        }
        if (selected == null) {
            selected = new ConnectionBean(this);
        }
        updateViewFromSelected();
    }

    /**
     * Starts the activity which manages keys.
     */
    protected void generatePubkey() {
        Log.d(TAG, "generatePubkey called");
        updateSelectedFromView();
        selected.saveAndWriteRecent(true, this);
        Intent intent = new Intent(this, GeneratePubkeyActivity.class);
        intent.putExtra("PrivateKey", selected.getSshPrivKey());
        startActivityForResult(intent, Constants.ACTIVITY_GEN_KEY);
    }

    /**
     * Returns the display height, or if the device has software
     * buttons, the 'bottom' of the view (in order to take into account the
     * software buttons.
     *
     * @return the height in pixels.
     */
    public int getHeight() {
        Log.d(TAG, "getHeight called");
        View v = getWindow().getDecorView().findViewById(android.R.id.content);
        Display d = getWindowManager().getDefaultDisplay();
        int bottom = v.getBottom();
        Point outSize = new Point();
        d.getSize(outSize);
        int height = outSize.y;
        if (android.os.Build.VERSION.SDK_INT >= 14) {
            android.view.ViewConfiguration vc = ViewConfiguration.get(this);
            if (vc.hasPermanentMenuKey())
                height = bottom;
        }
        if (Utils.isBlackBerry()) {
            height = bottom;
        }
        return height;
    }

    /**
     * Returns the display width, or if the device has software
     * buttons, the 'right' of the view (in order to take into account the
     * software buttons.
     *
     * @return the width in pixels.
     */
    public int getWidth() {
        Log.d(TAG, "getWidth called");
        View v = getWindow().getDecorView().findViewById(android.R.id.content);
        Display d = getWindowManager().getDefaultDisplay();
        int right = v.getRight();
        Point outSize = new Point();
        d.getSize(outSize);
        int width = outSize.x;
        if (android.os.Build.VERSION.SDK_INT >= 14) {
            android.view.ViewConfiguration vc = ViewConfiguration.get(this);
            if (vc.hasPermanentMenuKey())
                return right;
        }
        return width;
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu called");
        getMenuInflater().inflate(R.menu.connectionsetupmenu, menu);
        return true;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onMenuOpened(int, android.view.Menu)
     */
    @Override
    public boolean onMenuOpened(int featureId, @NonNull Menu menu) {
        Log.d(TAG, "onMenuOpened called");
        try {
            menu.findItem(R.id.itemSaveAsCopy).setEnabled(selected != null && !selected.isNew());
        } catch (NullPointerException e) {
            logAndPrintStacktrace(e);
        }
        return true;
    }


    /**
     * This function is used to retrieve data returned by activities started with startActivityForResult.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult called");
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.ACTIVITY_GEN_KEY) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                Bundle b = data.getExtras();
                String privateKey = (String) b.get("PrivateKey");
                if (privateKey != null &&
                        !privateKey.equals(selected.getSshPrivKey()) && privateKey.length() != 0)
                    Toast.makeText(getBaseContext(), getString(R.string.ssh_key_generated), Toast.LENGTH_LONG).show();
                selected.setSshPrivKey(privateKey);
                selected.setSshPubKey((String) b.get("PublicKey"));
                selected.saveAndWriteRecent(true, this);
            } else
                Log.i(TAG, "The user cancelled SSH key generation.");
        }
    }

    /**
     * Returns the current ConnectionBean.
     */
    public ConnectionBean getCurrentConnection() {
        Log.d(TAG, "getCurrentConnection called");
        return selected;
    }

    public void saveAsCopy(MenuItem item) {
        Log.d(TAG, "saveAsCopy called");
        if (selected.getNickname().equals(textNickname.getText().toString()))
            textNickname.setText(
                    String.format(
                            Locale.getDefault(), "%s %s",
                            getString(R.string.copy_of),
                            selected.getNickname()
                    )
            );
        selected.setScreenshotFilename(Utils.newScreenshotFileName());
        updateSelectedFromView();
        selected.set_Id(0);
        selected.saveAndWriteRecent(false, this);
        arriveOnPage();
        finish();
    }

    public void showConnectionScreenHelp(MenuItem item) {
        Log.d(TAG, "showConnectionScreenHelp called");
        Log.d(TAG, "Showing connection screen help.");
        Utils.createConnectionScreenDialog(this);
    }

    protected boolean useLastPositionToolbarDefault() {
        android.util.Log.d(TAG, "UseLastPositionToolbarDefault called");
        SharedPreferences sp = getSharedPreferences(Constants.generalSettingsTag, Context.MODE_PRIVATE);
        return sp.getBoolean(Constants.positionToolbarLastUsed, true);
    }

    protected static void logAndPrintStacktrace(Exception e) {
        e.printStackTrace();
        Log.d(TAG, "Ignoring Exception: " + e);
    }
}
