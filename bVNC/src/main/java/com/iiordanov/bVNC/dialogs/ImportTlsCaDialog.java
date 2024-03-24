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
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout.LayoutParams;

import com.iiordanov.bVNC.ConnectionBean;
import com.iiordanov.bVNC.Database;
import com.iiordanov.bVNC.aSPICE;
import com.morpheusly.common.Utilities;
import com.undatech.remoteClientUi.R;

/**
 * @author Iordan K Iordanov
 *
 */
public class ImportTlsCaDialog extends AlertDialog {
    public static final int IMPORT_CA_REQUEST = 0;
    private static final Intent docIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://spice-space.org/page/SSLConnection"));
    private aSPICE mainConfigPage;
    private ConnectionBean selected;
    private EditText certSubject;
    private EditText caCert;
    private Button importButton;
    private Button helpButton;
    private Database database;

    /**
     * @param context
     */
    public ImportTlsCaDialog(Context context, Database database) {
        super(context);
        setOwnerActivity((Activity) context);
        mainConfigPage = (aSPICE) context;
        selected = mainConfigPage.getCurrentConnection();
        this.database = database;
    }

    public static void showDocumentation(Context c) {
        c.startActivity(docIntent);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Dialog#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        selected.setCaCert(caCert.getText().toString());
        selected.setCertSubject(certSubject.getText().toString());
        mainConfigPage.updateViewFromSelected();
        selected.saveAndWriteRecent(false, getContext());
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
        selected = mainConfigPage.getCurrentConnection();
        certSubject.setText(selected.getCertSubject());
        caCert.setText(selected.getCaCert());
    }

    /* (non-Javadoc)
     * @see android.app.Dialog#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.import_tls_ca_dialog);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.dimAmount = 1.0f;
        lp.width = LayoutParams.FILL_PARENT;
        lp.height = LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(lp);

        certSubject = (EditText) findViewById(R.id.certSubject);
        caCert = (EditText) findViewById(R.id.caCert);

        // Set up the import button.
        importButton = (Button) findViewById(R.id.importButton);
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
                Utilities.Companion.importCaCertFromFile(getOwnerActivity(), IMPORT_CA_REQUEST);
            }
        });

        // Set up the help button.
        helpButton = (Button) findViewById(R.id.helpButton);
        helpButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showDocumentation(ImportTlsCaDialog.this.mainConfigPage);
            }
        });

        // Set the widgets' state appropriately.
        setWidgetStateAppropriately();
    }
}
