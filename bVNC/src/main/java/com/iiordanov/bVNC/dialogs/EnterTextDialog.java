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
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.iiordanov.bVNC.Database;
import com.iiordanov.bVNC.RemoteCanvasActivity;
import com.iiordanov.bVNC.SentTextBean;
import com.undatech.remoteClientUi.R;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;

/**
 * @author Michael A. MacDonald
 *
 */
public class EnterTextDialog extends Dialog {
    static final int NUMBER_SENT_SAVED = 100;
    static final int DELETED_ID = -10;

    private RemoteCanvasActivity _canvasActivity;

    private EditText _textEnterText;

    private ArrayList<SentTextBean> _history;

    private int _historyIndex;

    private ImageButton _buttonNextEntry;
    private ImageButton _buttonPreviousEntry;

    public EnterTextDialog(Context context) {
        super(context);
        setOwnerActivity((Activity) context);
        _canvasActivity = (RemoteCanvasActivity) context;
        _history = new ArrayList<SentTextBean>();
    }

    private String saveText(boolean wasSent) {
        CharSequence cs = _textEnterText.getText();
        if (cs.length() == 0)
            return "";
        String s = cs.toString();
        if (wasSent || _historyIndex >= _history.size() || !s.equals(_history.get(_historyIndex).getSentText())) {
            SentTextBean added = new SentTextBean();
            added.setSentText(s);
            SQLiteDatabase db = new Database(getContext()).getWritableDatabase();
            added.Gen_insert(db);
            _history.add(added);
            for (int i = 0; i < _historyIndex - NUMBER_SENT_SAVED; i++) {
                SentTextBean deleteCandidate = _history.get(i);
                if (deleteCandidate.get_Id() != DELETED_ID) {
                    deleteCandidate.Gen_delete(db);
                    deleteCandidate.set_Id(DELETED_ID);
                }
            }
        }
        return s;
    }

    private void sendText(String s) {
        _canvasActivity.inputListener.sendText(s);
    }

    /* (non-Javadoc)
     * @see android.app.Dialog#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entertext);
        setTitle(R.string.enter_text_title);
        _textEnterText = (EditText) findViewById(R.id.textEnterText);
        _buttonNextEntry = (ImageButton) findViewById(R.id.buttonNextEntry);
        _buttonNextEntry.setOnClickListener(new View.OnClickListener() {

            /* (non-Javadoc)
             * @see android.view.View.OnClickListener#onClick(android.view.View)
             */
            @Override
            public void onClick(View v) {
                int oldSize = _history.size();
                if (_historyIndex < oldSize) {
                    saveText(false);
                    _historyIndex++;
                    if (_history.size() > oldSize && _historyIndex == oldSize)
                        _historyIndex++;
                    if (_historyIndex < _history.size()) {
                        _textEnterText.setText(_history.get(_historyIndex).getSentText());
                    } else {
                        _textEnterText.setText("");
                    }
                }
                updateButtons();
            }

        });
        _buttonPreviousEntry = (ImageButton) findViewById(R.id.buttonPreviousEntry);
        _buttonPreviousEntry.setOnClickListener(new View.OnClickListener() {

            /* (non-Javadoc)
             * @see android.view.View.OnClickListener#onClick(android.view.View)
             */
            @Override
            public void onClick(View v) {
                if (_historyIndex > 0) {
                    saveText(false);
                    _historyIndex--;
                    _textEnterText.setText(_history.get(_historyIndex).getSentText());
                }
                updateButtons();
            }

        });
        ((Button) findViewById(R.id.buttonSendText)).setOnClickListener(new View.OnClickListener() {

            /* (non-Javadoc)
             * @see android.view.View.OnClickListener#onClick(android.view.View)
             */
            @Override
            public void onClick(View v) {
                String s = saveText(true);
                sendText(s);
                _textEnterText.setText("");
                _historyIndex = _history.size();
                updateButtons();
                dismiss();
            }

        });

        ((Button) findViewById(R.id.buttonSendWithoutSaving)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String s = _textEnterText.getText().toString();
                sendText(s);
                _textEnterText.setText("");
                _historyIndex = _history.size();
                updateButtons();
                dismiss();
            }
        });

        ((ImageButton) findViewById(R.id.buttonTextDelete)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (_historyIndex < _history.size()) {
                    String s = _textEnterText.getText().toString();
                    SentTextBean bean = _history.get(_historyIndex);
                    if (s.equals(bean.getSentText())) {

                        bean.Gen_delete(new Database(getContext()).getWritableDatabase());
                        _history.remove(_historyIndex);
                        if (_historyIndex > 0) {
                            _historyIndex = _historyIndex - 1;
                        }
                    }
                }
                String s = "";
                if (_historyIndex < _history.size()) {
                    s = _history.get(_historyIndex).getSentText();
                }
                _textEnterText.setText(s);
                updateButtons();
            }

        });
        Cursor readInOrder = new Database(getContext()).getReadableDatabase().rawQuery(
                "select * from " + SentTextBean.GEN_TABLE_NAME + " ORDER BY _id", null);
        try {
            SentTextBean.Gen_populateFromCursor(readInOrder, _history, SentTextBean.GEN_NEW);
        } finally {
            readInOrder.close();
        }
        _historyIndex = _history.size();

        updateButtons();
    }

    private void updateButtons() {
        _buttonPreviousEntry.setEnabled(_historyIndex > 0);
        _buttonNextEntry.setEnabled(_historyIndex < _history.size());
    }

    /* (non-Javadoc)
     * @see android.app.Dialog#onStart()
     */
    @Override
    protected void onStart() {
        super.onStart();
        _textEnterText.requestFocus();
    }
}
