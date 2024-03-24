/**
 * Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 2009 Michael A. MacDonald
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
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.iiordanov.bVNC.bVNC;
import com.undatech.remoteClientUi.R;

/**
 * @author Michael A. MacDonald
 *
 */
public class RepeaterDialog extends Dialog {
    bVNC _configurationDialog;
    private EditText _repeaterId;

    public RepeaterDialog(bVNC context) {
        super(context);
        setOwnerActivity((Activity) context);
        _configurationDialog = context;
    }

    /* (non-Javadoc)
     * @see android.app.Dialog#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.repeater_dialog_title);

        setContentView(R.layout.repeater_dialog);
        _repeaterId = (EditText) findViewById(R.id.textRepeaterInfo);
        ((TextView) findViewById(R.id.textRepeaterCaption)).setText(Html.fromHtml(getContext().getString(R.string.repeater_caption)));
        ((Button) findViewById(R.id.buttonSaveRepeater)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                _configurationDialog.updateRepeaterInfo(true, _repeaterId.getText().toString());
                dismiss();
            }
        });
        ((Button) findViewById(R.id.buttonClearRepeater)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                _configurationDialog.updateRepeaterInfo(false, "");
                dismiss();
            }
        });
    }
}
