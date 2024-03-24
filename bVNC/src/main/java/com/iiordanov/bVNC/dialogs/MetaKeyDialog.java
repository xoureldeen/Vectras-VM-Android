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
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;

import com.iiordanov.bVNC.ConnectionSettable;
import com.iiordanov.bVNC.Database;
import com.iiordanov.bVNC.MetaList;
import com.iiordanov.bVNC.RemoteCanvasActivity;
import com.iiordanov.bVNC.Utils;
import com.iiordanov.bVNC.input.MetaKeyBase;
import com.iiordanov.bVNC.input.MetaKeyBean;
import com.undatech.opaque.Connection;
import com.undatech.opaque.input.RemoteKeyboard;
import com.undatech.remoteClientUi.R;

import net.sqlcipher.database.SQLiteDatabase;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;

/**
 * @author Michael A. MacDonald
 *
 */
public class MetaKeyDialog extends Dialog implements ConnectionSettable {

    public static final String[] EMPTY_ARGS = new String[0];
    static ArrayList<MetaList> _lists;
    private static String copyListString;
    CheckBox _checkShift;
    CheckBox _checkCtrl;
    CheckBox _checkAlt;
    CheckBox _checkSuper;
    TextView _textKeyDesc;
    EditText _textListName;
    Spinner _spinnerKeySelect;
    Spinner _spinnerKeysInList;
    Spinner _spinnerLists;
    Database _database;
    ArrayList<MetaKeyBean> _keysInList = new ArrayList<MetaKeyBean>();
    long _listId;
    RemoteCanvasActivity _canvasActivity;
    MetaKeyBean _currentKeyBean = new MetaKeyBean(0, 0, MetaKeyBean.allKeys.get(0));
    Connection _connection;
    private boolean _justStarted;

    /**
     * @param context
     */
    public MetaKeyDialog(Context context) {
        super(context);
        setOwnerActivity((Activity) context);
        _canvasActivity = (RemoteCanvasActivity) context;
    }

    @SuppressWarnings("unchecked")
    private static ArrayAdapter<String> getSpinnerAdapter(Spinner spinner) {
        return (ArrayAdapter<String>) spinner.getAdapter();
    }

    /* (non-Javadoc)
     * @see android.app.Dialog#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        _canvasActivity.getMenuInflater().inflate(R.menu.metakeymenu, menu);
        menu.findItem(R.id.itemDeleteKeyList).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    /* (non-Javadoc)
                     * @see android.view.MenuItem.OnMenuItemClickListener#onMenuItemClick(android.view.MenuItem)
                     */
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Utils.showYesNoPrompt(_canvasActivity, getContext().getString(R.string.delete_key_list),
                                getContext().getString(R.string.delete_key_list) + " " + _textListName.getText().toString(),
                                new OnClickListener() {

                                    /* (non-Javadoc)
                                     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                                     */
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        int position = _spinnerLists.getSelectedItemPosition();
                                        if (position == Spinner.INVALID_POSITION)
                                            return;
                                        _listId = _lists.get(position).get_Id();
                                        if (_listId > 1) {
                                            _lists.remove(position);
                                            ArrayAdapter<String> adapter = getSpinnerAdapter(_spinnerLists);
                                            adapter.remove(adapter.getItem(position));
                                            SQLiteDatabase db = _database.getWritableDatabase();
                                            db.execSQL(MessageFormat.format("DELETE FROM {0} WHERE {1} = {2}",
                                                    MetaKeyBean.GEN_TABLE_NAME, MetaKeyBean.GEN_FIELD_METALISTID,
                                                    _listId));
                                            db.execSQL(MessageFormat.format("DELETE FROM {0} WHERE {1} = {2}",
                                                    MetaList.GEN_TABLE_NAME, MetaList.GEN_FIELD__ID,
                                                    _listId));
                                            db.close();
                                            _connection.setMetaListId(1);
                                            _connection.save(getContext());
                                            setMetaKeyList();
                                        }
                                    }
                                },
                                null);
                        return true;
                    }

                });
        menu.findItem(R.id.itemDeleteKey).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    /* (non-Javadoc)
                     * @see android.view.MenuItem.OnMenuItemClickListener#onMenuItemClick(android.view.MenuItem)
                     */
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        final int position = _spinnerKeysInList.getSelectedItemPosition();
                        if (position != Spinner.INVALID_POSITION) {
                            final MetaKeyBean toDelete = _keysInList.get(position);
                            Utils.showYesNoPrompt(_canvasActivity, getContext().getString(R.string.delete_key),
                                    getContext().getString(R.string.delete_key) + " " + toDelete.getKeyDesc(),
                                    new OnClickListener() {

                                        /* (non-Javadoc)
                                         * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                                         */
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            getSpinnerAdapter(_spinnerKeysInList).remove(toDelete.getKeyDesc());
                                            _keysInList.remove(position);
                                            SQLiteDatabase db = _database.getWritableDatabase();
                                            db.execSQL(
                                                    MessageFormat.format("DELETE FROM {0} WHERE {1} = {2}",
                                                            MetaKeyBean.GEN_TABLE_NAME, MetaKeyBean.GEN_FIELD_METALISTID,
                                                            toDelete.get_Id())
                                            );
                                            if (_connection.getLastMetaKeyId() == toDelete.get_Id()) {
                                                _connection.setLastMetaKeyId(0);
                                                _connection.save(getContext());
                                            }
                                            int newPos = _spinnerKeysInList.getSelectedItemPosition();
                                            if (newPos != Spinner.INVALID_POSITION && newPos < _keysInList.size()) {
                                                _currentKeyBean = new MetaKeyBean(_keysInList.get(newPos));
                                                updateDialogForCurrentKey();
                                            }
                                        }
                                    },
                                    null);
                        }
                        return true;
                    }

                });
        return true;
    }

    /* (non-Javadoc)
     * @see android.app.Dialog#onMenuOpened(int, android.view.Menu)
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.itemDeleteKeyList).setEnabled(_currentKeyBean.getMetaListId() > 1);
        menu.findItem(R.id.itemDeleteKey).setEnabled(_spinnerKeysInList.getSelectedItemPosition() != Spinner.INVALID_POSITION);
        return true;
    }

    /* (non-Javadoc)
     * @see android.app.Dialog#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.metakey);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.dimAmount = 1.0f;
        lp.width = LayoutParams.FILL_PARENT;
        lp.height = LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(lp);

        setTitle(R.string.meta_key_title);
        _checkShift = (CheckBox) findViewById(R.id.checkboxShift);
        _checkCtrl = (CheckBox) findViewById(R.id.checkboxCtrl);
        _checkAlt = (CheckBox) findViewById(R.id.checkboxAlt);
        _checkSuper = (CheckBox) findViewById(R.id.checkboxSuper);
        _textKeyDesc = (TextView) findViewById(R.id.textKeyDesc);
        _textListName = (EditText) findViewById(R.id.textListName);
        _spinnerKeySelect = (Spinner) findViewById(R.id.spinnerKeySelect);
        _spinnerKeysInList = (Spinner) findViewById(R.id.spinnerKeysInList);
        _spinnerLists = (Spinner) findViewById(R.id.spinnerLists);

        _database = new Database(getContext());
        if (_lists == null) {
            _lists = new ArrayList<MetaList>();
            MetaList.getAll(_database.getReadableDatabase(), MetaList.GEN_TABLE_NAME, _lists, MetaList.GEN_NEW);
        }
        _spinnerKeySelect.setAdapter(new ArrayAdapter<String>(getOwnerActivity(), R.layout.key_list_entry, MetaKeyBean.allKeysNames));
        _spinnerKeySelect.setSelection(0);

        setListSpinner();

        _checkShift.setOnCheckedChangeListener(new MetaCheckListener(RemoteKeyboard.SHIFT_MASK));
        _checkAlt.setOnCheckedChangeListener(new MetaCheckListener(RemoteKeyboard.ALT_MASK));
        _checkCtrl.setOnCheckedChangeListener(new MetaCheckListener(RemoteKeyboard.CTRL_MASK));
        _checkSuper.setOnCheckedChangeListener(new MetaCheckListener(RemoteKeyboard.SUPER_MASK));

        _spinnerLists.setOnItemSelectedListener(new OnItemSelectedListener() {

            /* (non-Javadoc)
             * @see android.widget.AdapterView.OnItemSelectedListener#onItemSelected(android.widget.AdapterView, android.view.View, int, long)
             */
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                _connection.setMetaListId(_lists.get(position).get_Id());
                _connection.save(getContext());
                setMetaKeyList();
            }

            /* (non-Javadoc)
             * @see android.widget.AdapterView.OnItemSelectedListener#onNothingSelected(android.widget.AdapterView)
             */
            public void onNothingSelected(AdapterView<?> parent) {
            }

        });

        _spinnerKeysInList.setOnItemSelectedListener(new OnItemSelectedListener() {

            /* (non-Javadoc)
             * @see android.widget.AdapterView.OnItemSelectedListener#onItemSelected(android.widget.AdapterView, android.view.View, int, long)
             */
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                _currentKeyBean = new MetaKeyBean(_keysInList.get(position));
                updateDialogForCurrentKey();
            }

            /* (non-Javadoc)
             * @see android.widget.AdapterView.OnItemSelectedListener#onNothingSelected(android.widget.AdapterView)
             */
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        _spinnerKeySelect.setOnItemSelectedListener(new OnItemSelectedListener() {

            /* (non-Javadoc)
             * @see android.widget.AdapterView.OnItemSelectedListener#onItemSelected(android.widget.AdapterView, android.view.View, int, long)
             */
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (_currentKeyBean == null) {
                    _currentKeyBean = new MetaKeyBean(0, 0, MetaKeyBean.allKeys.get(position));
                } else {
                    _currentKeyBean.setKeyBase(MetaKeyBean.allKeys.get(position));
                }
                updateDialogForCurrentKey();
            }

            /* (non-Javadoc)
             * @see android.widget.AdapterView.OnItemSelectedListener#onNothingSelected(android.widget.AdapterView)
             */
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ((Button) findViewById(R.id.buttonSend)).setOnClickListener(new View.OnClickListener() {

            /* (non-Javadoc)
             * @see android.view.View.OnClickListener#onClick(android.view.View)
             */
            public void onClick(View v) {
                sendCurrentKey();
                dismiss();
            }

        });

        ((Button) findViewById(R.id.buttonNewList)).setOnClickListener(new View.OnClickListener() {

            /* (non-Javadoc)
             * @see android.view.View.OnClickListener#onClick(android.view.View)
             */
            @Override
            public void onClick(View v) {
                MetaList newList = new MetaList();
                newList.setName(getContext().getString(R.string.new_list_button));
                SQLiteDatabase db = _database.getWritableDatabase();
                newList.Gen_insert(db);
                _connection.setMetaListId(newList.get_Id());
                _connection.save(getContext());
                _lists.add(newList);
                getSpinnerAdapter(_spinnerLists).add(newList.getName());
                setMetaKeyList();
            }

        });
        ((Button) findViewById(R.id.buttonCopyList)).setOnClickListener(new View.OnClickListener() {

            /* (non-Javadoc)
             * @see android.view.View.OnClickListener#onClick(android.view.View)
             */
            @Override
            public void onClick(View v) {
                MetaList newList = new MetaList();
                newList.setName(getContext().getString(R.string.copy_of) + " " + _textListName.getText().toString());
                SQLiteDatabase db = _database.getWritableDatabase();
                newList.Gen_insert(db);
                db.execSQL(MessageFormat.format(getCopyListString(), newList.get_Id(), _listId));
                _connection.setMetaListId(newList.get_Id());
                _connection.save(getContext());
                _lists.add(newList);
                getSpinnerAdapter(_spinnerLists).add(newList.getName());
                setMetaKeyList();
            }

        });
    }

    private String getCopyListString() {
        if (copyListString == null) {
            StringBuilder sb = new StringBuilder("INSERT INTO ");
            sb.append(MetaKeyBean.GEN_TABLE_NAME);
            sb.append(" ( ");
            sb.append(MetaKeyBean.GEN_FIELD_METALISTID);
            StringBuilder fieldList = new StringBuilder();
            for (Entry<String, Object> s : _currentKeyBean.Gen_getValues().valueSet()) {
                if (!s.getKey().equals(MetaKeyBean.GEN_FIELD__ID) && !s.getKey().equals(MetaKeyBean.GEN_FIELD_METALISTID)) {
                    fieldList.append(',');
                    fieldList.append(s.getKey());
                }
            }
            String fl = fieldList.toString();
            sb.append(fl);
            sb.append(" ) SELECT {0} ");
            sb.append(fl);
            sb.append(" FROM ");
            sb.append(MetaKeyBean.GEN_TABLE_NAME);
            sb.append(" WHERE ");
            sb.append(MetaKeyBean.GEN_FIELD_METALISTID);
            sb.append(" = {1}");
            copyListString = sb.toString();
        }
        return copyListString;
    }

    /* (non-Javadoc)
     * @see android.app.Dialog#onStart()
     */
    @Override
    protected void onStart() {
        takeKeyEvents(true);
        _justStarted = true;
        super.onStart();
        View v = getCurrentFocus();
        if (v != null)
            v.clearFocus();
    }

    /* (non-Javadoc)
     * @see android.app.Dialog#onStop()
     */
    @Override
    protected void onStop() {
        int i = 0;
        for (MetaList l : _lists) {
            if (l.get_Id() == _listId) {
                String s = _textListName.getText().toString();
                if (!s.equals(l.getName())) {
                    l.setName(s);
                    l.Gen_update(_database.getWritableDatabase());
                    ArrayAdapter<String> adapter = getSpinnerAdapter(_spinnerLists);
                    adapter.remove(adapter.getItem(i));
                    adapter.insert(s, i);
                }
                break;
            }
            i++;
        }
        takeKeyEvents(false);
        super.onStop();
    }

    /* (non-Javadoc)
     * @see android.app.Dialog#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        _justStarted = false;
        if (keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_MENU && getCurrentFocus() == null) {
            int flags = event.getMetaState();
            int currentFlags = _currentKeyBean.getMetaFlags();
            MetaKeyBase base = MetaKeyBean.keysByKeyCode.get(keyCode);
            if (base != null) {
                if (0 != (flags & KeyEvent.META_SHIFT_ON)) {
                    currentFlags |= RemoteKeyboard.SHIFT_MASK;
                }
                if (0 != (flags & KeyEvent.META_ALT_ON)) {
                    currentFlags |= RemoteKeyboard.ALT_MASK;
                }
                _currentKeyBean.setKeyBase(base);
            } else {
                // Toggle flags according to meta keys
                if (0 != (flags & KeyEvent.META_SHIFT_ON)) {
                    currentFlags ^= RemoteKeyboard.SHIFT_MASK;
                }
                if (0 != (flags & KeyEvent.META_ALT_ON)) {
                    currentFlags ^= RemoteKeyboard.ALT_MASK;
                }
                if (keyCode == KeyEvent.KEYCODE_SEARCH) {
                    currentFlags ^= RemoteKeyboard.CTRL_MASK;
                }
            }
            _currentKeyBean.setMetaFlags(currentFlags);
            updateDialogForCurrentKey();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /* (non-Javadoc)
     * @see android.app.Dialog#onKeyUp(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!_justStarted && keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_MENU && getCurrentFocus() == null) {
            if (MetaKeyBean.keysByKeyCode.get(keyCode) != null) {
                sendCurrentKey();
                dismiss();
            }
            return true;
        }
        _justStarted = false;
        return super.onKeyUp(keyCode, event);
    }

    void sendCurrentKey() {
        int index = Collections.binarySearch(_keysInList, _currentKeyBean);
        SQLiteDatabase db = _database.getWritableDatabase();
        if (index < 0) {
            int insertionPoint = -(index + 1);
            _currentKeyBean.Gen_insert(db);
            _keysInList.add(insertionPoint, _currentKeyBean);
            ArrayAdapter<String> adapter = getSpinnerAdapter(_spinnerKeysInList);
            if (adapter != null) {
                adapter.insert(_currentKeyBean.getKeyDesc(), insertionPoint);
            }
            _spinnerKeysInList.setSelection(insertionPoint);
            _connection.setLastMetaKeyId(_currentKeyBean.get_Id());
        } else {
            MetaKeyBean bean = _keysInList.get(index);
            _connection.setLastMetaKeyId(bean.get_Id());
            _spinnerKeysInList.setSelection(index);
        }
        _connection.save(getContext());
        _canvasActivity.getRemoteConnection().getKeyboard().sendMetaKey(_currentKeyBean);
    }

    void setMetaKeyList() {
        long listId = _connection.getMetaListId();
        if (listId != _listId) {
            for (int i = 0; i < _lists.size(); ++i) {
                MetaList list = _lists.get(i);
                if (list.get_Id() == listId) {
                    _spinnerLists.setSelection(i);
                    _keysInList = new ArrayList<MetaKeyBean>();
                    Cursor c = _database.getReadableDatabase().rawQuery(
                            MessageFormat.format("SELECT * FROM {0} WHERE {1} = {2} ORDER BY KEYDESC",
                                    MetaKeyBean.GEN_TABLE_NAME,
                                    MetaKeyBean.GEN_FIELD_METALISTID,
                                    listId),
                            EMPTY_ARGS);
                    MetaKeyBean.Gen_populateFromCursor(
                            c,
                            _keysInList,
                            MetaKeyBean.NEW);
                    c.close();
                    ArrayList<String> keys = new ArrayList<String>(_keysInList.size());
                    int selectedOffset = 0;
                    long lastSelectedKeyId = _canvasActivity.getConnection().getLastMetaKeyId();
                    for (int j = 0; j < _keysInList.size(); j++) {
                        MetaKeyBean key = _keysInList.get(j);
                        keys.add(key.getKeyDesc());
                        if (lastSelectedKeyId == key.get_Id()) {
                            selectedOffset = j;
                        }
                    }
                    _spinnerKeysInList.setAdapter(new ArrayAdapter<String>(getOwnerActivity(), R.layout.key_list_entry, keys));
                    if (keys.size() > 0) {
                        _spinnerKeysInList.setSelection(selectedOffset);
                        _currentKeyBean = new MetaKeyBean(_keysInList.get(selectedOffset));
                    } else {
                        _currentKeyBean = new MetaKeyBean(listId, 0, MetaKeyBean.allKeys.get(0));
                    }
                    updateDialogForCurrentKey();
                    _textListName.setText(list.getName());
                    break;
                }
            }
            _listId = listId;
        }
    }

    private void updateDialogForCurrentKey() {
        int flags = _currentKeyBean.getMetaFlags();
        _checkAlt.setChecked(0 != (flags & (RemoteKeyboard.ALT_MASK | RemoteKeyboard.RALT_MASK)));
        _checkShift.setChecked(0 != (flags & (RemoteKeyboard.SHIFT_MASK | RemoteKeyboard.RSHIFT_MASK)));
        _checkCtrl.setChecked(0 != (flags & (RemoteKeyboard.CTRL_MASK | RemoteKeyboard.RCTRL_MASK)));
        _checkSuper.setChecked(0 != (flags & (RemoteKeyboard.SUPER_MASK | RemoteKeyboard.RSUPER_MASK)));
        MetaKeyBase base = null;
        if (_currentKeyBean.isMouseClick()) {
            base = MetaKeyBean.keysByMouseButton.get(_currentKeyBean.getMouseButtons());
        } else {
            base = MetaKeyBean.keysByKeySym.get(_currentKeyBean.getKeySym());
        }
        if (base != null) {
            int index = Collections.binarySearch(MetaKeyBean.allKeys, base);
            if (index >= 0) {
                _spinnerKeySelect.setSelection(index);
            }
        }
        _textKeyDesc.setText(_currentKeyBean.getKeyDesc());
    }

    public void setConnection(Connection conn) {
        if (_connection != conn) {
            _connection = conn;
            setMetaKeyList();
        }
    }

    void setListSpinner() {
        ArrayList<String> listNames = new ArrayList<String>(_lists.size());
        for (int i = 0; i < _lists.size(); ++i) {
            MetaList l = _lists.get(i);
            listNames.add(l.getName());
        }
        _spinnerLists.setAdapter(new ArrayAdapter<String>(getOwnerActivity(), R.layout.key_list_entry, listNames));
    }

    /**
     * @author Michael A. MacDonald
     *
     */
    class MetaCheckListener implements OnCheckedChangeListener {

        private int _mask;

        MetaCheckListener(int mask) {
            _mask = mask;
        }

        /* (non-Javadoc)
         * @see android.widget.CompoundButton.OnCheckedChangeListener#onCheckedChanged(android.widget.CompoundButton, boolean)
         */
        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                                     boolean isChecked) {
            if (isChecked) {
                _currentKeyBean.setMetaFlags(_currentKeyBean.getMetaFlags() | _mask);
            } else {
                _currentKeyBean.setMetaFlags(_currentKeyBean.getMetaFlags() & ~_mask);
            }
            _textKeyDesc.setText(_currentKeyBean.getKeyDesc());
        }
    }

}
