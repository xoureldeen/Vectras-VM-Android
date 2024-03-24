/**
 * Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 2010 Michael A. MacDonald
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
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.iiordanov.bVNC.Database;
import com.iiordanov.bVNC.Utils;
import com.undatech.opaque.RemoteClientLibConstants;
import com.undatech.remoteClientUi.R;

/**
 * @author Michael A. MacDonald
 *
 */
public class ImportExportDialog extends Dialog {
    public static final String TAG = "ImportExportDialog";
    private Activity activity;
    private Database database;
    private boolean connectionsInSharedPrefs;

    /**
     * @param context
     */
    public ImportExportDialog(Activity context, Database database, boolean connectionsInSharedPrefs) {
        super((Context) context);
        setOwnerActivity((Activity) context);
        activity = context;
        this.database = database;
        this.connectionsInSharedPrefs = connectionsInSharedPrefs;
    }

    /* (non-Javadoc)
     * @see android.app.Dialog#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.importexport);
        setTitle(R.string.import_export_settings);

        Button export = (Button) findViewById(R.id.buttonExport);
        export.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                        "*/*"
                });
                activity.startActivityForResult(intent, RemoteClientLibConstants.EXPORT_SETTINGS_REQUEST_CODE);
                dismiss();
            }

        });

        ((Button) findViewById(R.id.buttonImport)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                        "*/*"
                });
                activity.startActivityForResult(intent, RemoteClientLibConstants.IMPORT_SETTINGS_REQUEST_CODE);
                dismiss();
            }

        });
    }

    private void errorNotify(String msg, Throwable t) {
        Log.i(TAG, msg, t);
        Utils.showErrorMessage(this.getContext(), msg + ":" + t.getMessage());
    }

}
