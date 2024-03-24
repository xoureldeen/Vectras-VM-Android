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

package com.iiordanov.bVNC.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.iiordanov.bVNC.ConnectionBean;
import com.iiordanov.bVNC.Constants;
import com.iiordanov.bVNC.Database;
import com.iiordanov.bVNC.bVNC;
import com.iiordanov.util.RandomString;
import com.undatech.remoteClientUi.R;

/**
 * @author Iordan K Iordanov
 *
 */
public class AutoXCustomizeDialog extends AlertDialog {
    private static final Intent docIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://iiordanov.blogspot.ca/2012/10/looking-for-nx-client-for-android-or.html"));
    private bVNC mainConfigDialog;
    private ConnectionBean selected;
    private int commandIndex;
    private int origCommandIndex;
    private String command;
    private Spinner spinnerAutoXType;
    private Button autoXConfirm;
    private Button autoXCancel;
    private String geometry = "";
    private String sessionProg = "";
    private String pw = "";
    private ToggleButton toggleAutoXAdvanced;
    private LinearLayout layoutAdvancedSettings;
    private Spinner spinnerAutoXGeometry;
    private EditText autoXWidth;
    private EditText autoXHeight;
    private Spinner spinnerAutoXSession;
    private EditText autoXSessionProg;
    private int nativeWidth;
    private int nativeHeight;
    private CheckBox checkboxAutoXUnixpw;
    private CheckBox checkboxAutoXUnixAuth;
    private RandomString rnd;
    private Button buttonAutoXHelp;
    private Database database;

    /**
     * @param context
     */
    public AutoXCustomizeDialog(Context context, Database database) {
        super(context);
        setOwnerActivity((Activity) context);
        mainConfigDialog = (bVNC) context;
        selected = mainConfigDialog.getCurrentConnection();
        rnd = new RandomString();
        this.database = database;
    }

    public static void showDocumentation(Context c) {
        c.startActivity(docIntent);
    }

    /* Don't do anything if back is pressed.
     *
     * (non-Javadoc)
     * @see android.app.Dialog#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        retainAutoXInfo();
        dismiss();
    }

    /* This function needs to be overloaded because the dialog does not get
     * destroyed and recreated every time the button on the main screen is
     * pressed, and so the widgets' state wasn't set correctly. This makes
     * sure the widgets' state is set when the dialog gets displayed.
     * (non-Javadoc)
     * @see android.app.Dialog#onAttachedToWindow()
     */
    @Override
    public void onAttachedToWindow() {
        setWidgetStateAppropriately();
    }

    private void setWidgetStateAppropriately() {
        selected = mainConfigDialog.getCurrentConnection();
        if (selected == null) {
            dismiss();
            return;
        }
        commandIndex = selected.getAutoXType();
        origCommandIndex = selected.getAutoXType();
        // Set current selection to the one corresponding to saved setting.
        spinnerAutoXType.setSelection(commandIndex);
        // Set the widgets controlling the remote resolution.
        setRemoteWidthAndHeight();
        // Set the widgets controlling the remote session program.
        setSessionProg();
        // Sets the password option.
        setPwOption();
        // Sets the state of the auto unixpw authentication checkbox.
        checkboxAutoXUnixAuth.setChecked(selected.getAutoXUnixAuth());
        // Set the toggle state of the advanced button.
        setAdvancedToggleState();
    }

    /**
     * Sets the state of the advanced button.
     */
    private void setAdvancedToggleState() {
        boolean adv = ((commandIndex != Constants.COMMAND_AUTO_X_DISABLED) &&
                ((selected.getAutoXResType() != Constants.AUTOX_GEOM_SELECT_NATIVE) ||
                        (selected.getAutoXSessionType() != Constants.AUTOX_SESS_PROG_SELECT_AUTO) ||
                        selected.getAutoXUnixpw() || selected.getAutoXUnixAuth()));
        toggleAutoXAdvanced.setChecked(adv);
    }

    /**
     * Make sure commandIndex and command match the state of the checkbox which
     * specifies whether .dmrc should be moved away.
     */
    private void setCommandIndexAndCommand(int itemIndex) {
        commandIndex = itemIndex;
        if (commandIndex != Constants.COMMAND_AUTO_X_DISABLED)
            command = Constants.getCommandString(commandIndex, geometry + sessionProg + pw);
        else
            command = new String("");
    }

    /**
     * Enables and disables the EditText boxes for width and height of remote desktop.
     */
    private void setRemoteWidthAndHeight() {

        // Android devices with SDK newer than KITKAT use immersive mode and therefore
        // we get the resolution of the whole display.
        if (Constants.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            nativeWidth = Math.max(mainConfigDialog.getWidth(), mainConfigDialog.getHeight());
            nativeHeight = Math.min(mainConfigDialog.getWidth(), mainConfigDialog.getHeight());
        } else {
            Point dS = new Point();
            mainConfigDialog.getWindowManager().getDefaultDisplay().getRealSize(dS);
            nativeWidth = Math.max(dS.x, dS.y);
            nativeHeight = Math.min(dS.x, dS.y);
        }

        spinnerAutoXGeometry.setSelection(selected.getAutoXResType());
        if (selected.getAutoXResType() == Constants.AUTOX_GEOM_SELECT_NATIVE) {
            autoXWidth.setEnabled(false);
            autoXHeight.setEnabled(false);
            autoXWidth.setText(Integer.toString(nativeWidth));
            autoXHeight.setText(Integer.toString(nativeHeight));
            selected.setAutoXWidth(nativeWidth);
            selected.setAutoXHeight(nativeHeight);
        } else {
            autoXWidth.setEnabled(true);
            autoXHeight.setEnabled(true);
            autoXWidth.setText(Integer.toString(selected.getAutoXWidth()));
            autoXHeight.setText(Integer.toString(selected.getAutoXHeight()));
        }

        geometry = new String(" -env FD_GEOM=" + selected.getAutoXWidth() + "x" + selected.getAutoXHeight());
    }

    /**
     * Sets the sessionProg variable according to selection.
     */
    private void setSessionProg() {

        spinnerAutoXSession.setSelection(selected.getAutoXSessionType());

        if (selected.getAutoXSessionType() != Constants.AUTOX_SESS_PROG_SELECT_CUSTOM) {
            autoXSessionProg.setEnabled(false);
            autoXSessionProg.setText(Constants.getSessionProgString(selected.getAutoXSessionType()));
            sessionProg = new String(" -env FD_PROG=\""
                    + Constants.getSessionProgString(selected.getAutoXSessionType()) + "\" ");
        } else {
            autoXSessionProg.setEnabled(true);
            autoXSessionProg.setText(selected.getAutoXSessionProg().toString());
            sessionProg = new String(" -env FD_PROG=\"" + autoXSessionProg.getText().toString() + "\" ");
        }
    }

    /**
     * Sets the UNIXsessionProg variable according to selection.
     */
    private void setPwOption() {
        checkboxAutoXUnixpw.setChecked(selected.getAutoXUnixpw());
        if (selected.getAutoXUnixpw()) {
            pw = Constants.AUTO_X_USERPW;
        } else {
            // Generate, save, and use random file extension.
            selected.setAutoXRandFileNm(rnd.randomLowerCaseString(20));
            pw = Constants.AUTO_X_PASSWDFILE + Constants.AUTO_X_PWFILEBASENAME + selected.getAutoXRandFileNm() + " \"";
        }
    }

    /* (non-Javadoc)
     * @see android.app.Dialog#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.auto_x_customize);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.dimAmount = 1.0f;
        lp.width = LayoutParams.FILL_PARENT;
        lp.height = LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(lp);

        // Define the spinner which allows one to choose the X-server type
        spinnerAutoXType = (Spinner) findViewById(R.id.spinnerAutoXType);
        spinnerAutoXType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> ad, View view, int itemIndex, long id) {
                // Set our preferred commandIndex, and command accordingly.
                setCommandIndexAndCommand(itemIndex);
                // Set the toggle state of the advanced button.
                setAdvancedToggleState();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        // Set up the help button.
        buttonAutoXHelp = (Button) findViewById(R.id.buttonAutoXHelp);
        buttonAutoXHelp.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showDocumentation(AutoXCustomizeDialog.this.mainConfigDialog);
            }
        });

        // The advanced settings button.
        toggleAutoXAdvanced = (ToggleButton) findViewById(R.id.toggleAutoXAdvanced);
        layoutAdvancedSettings = (LinearLayout) findViewById(R.id.layoutAdvancedSettings);
        toggleAutoXAdvanced.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean checked) {
                if (checked)
                    layoutAdvancedSettings.setVisibility(View.VISIBLE);
                else
                    layoutAdvancedSettings.setVisibility(View.GONE);
            }

        });

        // The geometry type and dimensions boxes.
        spinnerAutoXGeometry = (Spinner) findViewById(R.id.spinnerAutoXGeometry);
        autoXWidth = (EditText) findViewById(R.id.autoXWidth);
        autoXHeight = (EditText) findViewById(R.id.autoXHeight);
        spinnerAutoXGeometry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int itemIndex, long id) {
                selected.setAutoXResType(itemIndex);
                setRemoteWidthAndHeight();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }

        });

        // Define the type of session to start.
        spinnerAutoXSession = (Spinner) findViewById(R.id.spinnerAutoXSession);
        autoXSessionProg = (EditText) findViewById(R.id.autoXSessionProg);
        spinnerAutoXSession.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int itemIndex, long id) {
                selected.setAutoXSessionType(itemIndex);
                setSessionProg();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        // Define the unixpw checkbox.
        checkboxAutoXUnixpw = (CheckBox) findViewById(R.id.checkboxAutoXUnixpw);
        checkboxAutoXUnixpw.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean checked) {
                selected.setAutoXUnixpw(checked);
                setPwOption();
            }
        });

        // Define the auto unix authentication checkbox.
        checkboxAutoXUnixAuth = (CheckBox) findViewById(R.id.checkboxAutoXUnixAuth);
        checkboxAutoXUnixAuth.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean checked) {
                selected.setAutoXUnixAuth(checked);
            }
        });


        // Define Confirm button
        autoXConfirm = (Button) findViewById(R.id.autoXConfirm);
        autoXConfirm.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                updateAutoXInfo();
                dismiss();
            }
        });

        // Define Cancel button
        autoXCancel = (Button) findViewById(R.id.autoXCancel);
        autoXCancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                retainAutoXInfo();
                // If the user cancels, exit without changes.
                dismiss();
            }
        });

        // Set the widgets' state appropriately.
        setWidgetStateAppropriately();
    }

    /**
     * Called from Auto X Session dialog to update the Auto X config options.
     */
    public void updateAutoXInfo() {
        // Save any user modified resolution.
        selected.setAutoXResType(spinnerAutoXGeometry.getSelectedItemPosition());
        try {
            selected.setAutoXWidth(Integer.parseInt(autoXWidth.getText().toString()));
            selected.setAutoXHeight(Integer.parseInt(autoXHeight.getText().toString()));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        // Save any remote session command the user set.
        selected.setAutoXSessionType(spinnerAutoXSession.getSelectedItemPosition());
        selected.setAutoXSessionProg(autoXSessionProg.getText().toString());

        // Ensure the values of command and commandIndex match the widgets.
        setRemoteWidthAndHeight();
        setSessionProg();
        setCommandIndexAndCommand(commandIndex);

        boolean autoXenabled = commandIndex != Constants.COMMAND_AUTO_X_DISABLED;
        selected.setAutoXEnabled(autoXenabled);
        if (autoXenabled) {
            selected.setAddress("localhost");
        }
        selected.setAutoXType(commandIndex);
        selected.setAutoXCommand(command);
        selected.setAutoXUnixpw(checkboxAutoXUnixpw.isChecked());
        selected.setAutoXUnixAuth(checkboxAutoXUnixAuth.isChecked());
        // Set a random VNC password (for the built-in security mechanism).
        selected.setPassword(rnd.randomString(8));

        // Update and save.
        mainConfigDialog.updateViewFromSelected();
        selected.saveAndWriteRecent(false, getContext());
    }

    public void retainAutoXInfo() {
        setCommandIndexAndCommand(origCommandIndex);
        selected.setAutoXType(origCommandIndex);
        selected.setAutoXCommand(command);

        // Update and save.
        mainConfigDialog.updateViewFromSelected();
        selected.saveAndWriteRecent(false, getContext());
    }
}
