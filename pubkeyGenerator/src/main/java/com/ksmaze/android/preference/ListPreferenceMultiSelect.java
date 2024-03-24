/* proxydroid - Global / Individual Proxy App for Android
 * Copyright (C) 2011 K's Maze <kafkasmaze@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.ksmaze.android.preference;


import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 * A {@link Preference} that displays a list of entries as
 * a dialog and allows multiple selections
 * <p>
 * This preference will store a string into the SharedPreferences. This string will be the values selected
 * from the {@link #setEntryValues(CharSequence[])} array.
 * </p>
 */
public class ListPreferenceMultiSelect extends ListPreference {
    //Need to make sure the SEPARATOR is unique and weird enough that it doesn't match one of the entries.
    //Not using any fancy symbols because this is interpreted as a regex for splitting strings.
    private static final String SEPARATOR = " , ";
    private boolean[] mClickedDialogEntryIndices;

    public ListPreferenceMultiSelect(Context context) {
        this(context, null);
    }

    public ListPreferenceMultiSelect(Context context, AttributeSet attrs) {
        super(context, attrs);

        mClickedDialogEntryIndices = new boolean[getEntries().length];
    }

    public static String[] parseStoredValue(CharSequence val) {
        if (val == null)
            return null;
        if ("".equals(val))
            return null;
        else
            return ((String) val).split(SEPARATOR);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
//        super.onDialogClosed(positiveResult);

        CharSequence[] entryValues = getEntryValues();
        if (positiveResult && entryValues != null) {
            StringBuffer value = new StringBuffer();
            for (int i = 0; i < entryValues.length; i++) {
                if (mClickedDialogEntryIndices[i]) {
                    value.append(entryValues[i]).append(SEPARATOR);
                }
            }

            if (callChangeListener(value)) {
                String val = value.toString();
                if (val.length() > 0)
                    val = val.substring(0, val.length() - SEPARATOR.length());
                setValue(val);
            }
        }
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        CharSequence[] entries = getEntries();
        CharSequence[] entryValues = getEntryValues();

        if (entries == null || entryValues == null || entries.length != entryValues.length) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array which are both the same length");
        }

        restoreCheckedEntries();
        builder.setMultiChoiceItems(entries, mClickedDialogEntryIndices,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean val) {
                        mClickedDialogEntryIndices[which] = val;
                    }
                });
    }

    private void restoreCheckedEntries() {
        CharSequence[] entryValues = getEntryValues();

        String[] vals = parseStoredValue(getValue());
        if (vals != null) {
            for (int j = 0; j < vals.length; j++) {
                String val = vals[j].trim();
                for (int i = 0; i < entryValues.length; i++) {
                    CharSequence entry = entryValues[i];
                    if (entry.equals(val)) {
                        mClickedDialogEntryIndices[i] = true;
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void setEntries(CharSequence[] entries) {
        super.setEntries(entries);
        mClickedDialogEntryIndices = new boolean[entries.length];
    }
}
