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


package com.iiordanov.bVNC.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.undatech.remoteClientUi.R;

public class GetTextFragment extends DialogFragment {
    public static final int Plaintext = 1;
    public static final int Password = 2;
    public static final int MatchingPasswordTwice = 3;
    public static final int Credentials = 4;
    public static final int CredentialsWithDomain = 5;
    public static final int CredentialsWithReadOnlyUser = 6;
    public static final int PasswordNoKeep = 7;
    public static final String DIALOG_ID_GET_MASTER_PASSWORD = "DIALOG_ID_GET_MASTER_PASSWORD";
    public static final String DIALOG_ID_GET_MATCHING_MASTER_PASSWORDS = "DIALOG_ID_GET_MATCHING_MASTER_PASSWORDS";
    public static final String DIALOG_ID_GET_VERIFICATION_CODE = "DIALOG_ID_GET_VERIFICATION_CODE";
    public static final String DIALOG_ID_GET_SSH_CREDENTIALS = "DIALOG_ID_GET_SSH_CREDENTIALS";
    public static final String DIALOG_ID_GET_SSH_PASSPHRASE = "DIALOG_ID_GET_SSH_PASSPHRASE";
    public static final String DIALOG_ID_GET_VNC_CREDENTIALS = "DIALOG_ID_GET_VNC_CREDENTIALS";
    public static final String DIALOG_ID_GET_VNC_PASSWORD = "DIALOG_ID_GET_VNC_PASSWORD";
    public static final String DIALOG_ID_GET_RDP_CREDENTIALS = "DIALOG_ID_GET_RDP_CREDENTIALS";
    public static final String DIALOG_ID_GET_RDP_GATEWAY_CREDENTIALS = "DIALOG_ID_GET_RDP_GATEWAY_CREDENTIALS";
    public static final String DIALOG_ID_GET_SPICE_PASSWORD = "DIALOG_ID_GET_SPICE_PASSWORD";
    public static final String DIALOG_ID_GET_OPAQUE_CREDENTIALS = "DIALOG_ID_GET_OPAQUE_CREDENTIALS";
    public static final String DIALOG_ID_GET_OPAQUE_PASSWORD = "DIALOG_ID_GET_OPAQUE_PASSWORD";
    public static final String DIALOG_ID_GET_OPAQUE_OTP_CODE = "DIALOG_ID_GET_OPAQUE_OTP_CODE";
    public static String TAG = "GetTextFragment";
    private boolean wasCancelled = false;
    private boolean wasConfirmed = false;
    private TextView error;
    private TextView textViewBox;
    private EditText textBox;
    private EditText textBox2;
    private EditText textBox3;
    private CheckBox checkboxKeepPassword;
    private OnFragmentDismissedListener dismissalListener;
    private String title;
    private String dialogId;
    private String t1;
    private String t2;
    private String t3;
    private boolean keepPassword;
    private int dialogType = 0;
    private int messageNum = 0;
    private int errorNum = 0;

    public GetTextFragment() {
    }

    public static GetTextFragment newInstance(String dialogId, String title,
                                              OnFragmentDismissedListener dismissalListener,
                                              int dialogType, int messageNum, int errorNum,
                                              String t1, String t2, String t3, boolean keepPassword) {
        android.util.Log.i(TAG, "newInstance called");
        GetTextFragment f = new GetTextFragment();
        f.setDismissalListener(dismissalListener);

        Bundle args = new Bundle();
        args.putString("dialogId", dialogId);
        args.putString("title", title);
        args.putInt("dialogType", dialogType);
        args.putInt("messageNum", messageNum);
        args.putInt("errorNum", errorNum);
        args.putString("t1", t1);
        args.putString("t2", t2);
        args.putString("t3", t3);
        args.putBoolean("keepPassword", keepPassword);
        f.setArguments(args);
        f.setRetainInstance(false);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.i(TAG, "onCreate called");
        Bundle b = getArguments();
        if (b != null) {
            dialogId = b.getString("dialogId");
            title = b.getString("title");
            dialogType = b.getInt("dialogType");
            messageNum = b.getInt("messageNum");
            errorNum = b.getInt("errorNum");
            t1 = b.getString("t1");
            t2 = b.getString("t2");
            t3 = b.getString("t3");
            keepPassword = b.getBoolean("keepPassword");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        android.util.Log.i(TAG, "onCreateView called");
        wasCancelled = false;
        wasConfirmed = false;

        Dialog d = getDialog();
        if (d != null) {
            Window w = d.getWindow();
            if (w != null) {
                w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN |
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }
            d.setTitle(title);
        }
        View v = null;
        Button buttonConfirm;
        Button buttonCancel;

        switch (dialogType) {
            case Plaintext:
                v = inflater.inflate(R.layout.get_text, container, false);
                textBox = (EditText) v.findViewById(R.id.textBox);
                checkboxKeepPassword = v.findViewById(R.id.checkboxKeepPassword);
                if (DIALOG_ID_GET_OPAQUE_OTP_CODE.equals(dialogId) ||
                        DIALOG_ID_GET_VERIFICATION_CODE.equals(dialogId)
                ) {
                    textBox.setHint("");
                    checkboxKeepPassword.setVisibility(View.INVISIBLE);
                }
                buttonConfirm = (Button) v.findViewById(R.id.buttonConfirm);
                buttonCancel = (Button) v.findViewById(R.id.buttonCancel);
                dismissOnCancel(buttonCancel);
                dismissOnConfirm(buttonConfirm);
                break;
            case PasswordNoKeep:
            case Password:
                v = inflater.inflate(R.layout.get_text, container, false);
                textBox = (EditText) v.findViewById(R.id.textBox);
                hideText(textBox);
                checkboxKeepPassword = v.findViewById(R.id.checkboxKeepPassword);
                buttonConfirm = (Button) v.findViewById(R.id.buttonConfirm);
                buttonCancel = (Button) v.findViewById(R.id.buttonCancel);
                dismissOnCancel(buttonCancel);
                dismissOnConfirm(buttonConfirm);
                if (dialogType == PasswordNoKeep) {
                    checkboxKeepPassword.setVisibility(View.GONE);
                }
                break;
            case MatchingPasswordTwice:
                v = inflater.inflate(R.layout.get_text_twice, container, false);
                error = (TextView) v.findViewById(R.id.error);
                textBox = (EditText) v.findViewById(R.id.textBox);
                textBox2 = (EditText) v.findViewById(R.id.textBox2);
                hideText(textBox);
                hideText(textBox2);
                buttonConfirm = (Button) v.findViewById(R.id.buttonConfirm);
                buttonCancel = (Button) v.findViewById(R.id.buttonCancel);
                dismissOnCancel(buttonCancel);
                ensureMatchingDismissOnConfirm(buttonConfirm, textBox, textBox2, error);
                break;
            case CredentialsWithDomain:
                v = inflater.inflate(R.layout.get_credentials_with_domain, container, false);
                error = (TextView) v.findViewById(R.id.error);
                textBox = (EditText) v.findViewById(R.id.textBox);
                textBox2 = (EditText) v.findViewById(R.id.textBox2);
                textBox3 = (EditText) v.findViewById(R.id.textBox3);
                hideText(textBox3);
                textBox3.requestFocus();
                checkboxKeepPassword = v.findViewById(R.id.checkboxKeepPassword);
                buttonConfirm = (Button) v.findViewById(R.id.buttonConfirm);
                buttonCancel = (Button) v.findViewById(R.id.buttonCancel);
                dismissOnCancel(buttonCancel);
                dismissOnConfirm(buttonConfirm);
                break;
            case Credentials:
                v = inflater.inflate(R.layout.get_credentials, container, false);
                error = (TextView) v.findViewById(R.id.error);
                textBox = (EditText) v.findViewById(R.id.textBox);
                textBox2 = (EditText) v.findViewById(R.id.textBox2);
                hideText(textBox2);
                textBox2.requestFocus();
                checkboxKeepPassword = v.findViewById(R.id.checkboxKeepPassword);
                buttonConfirm = (Button) v.findViewById(R.id.buttonConfirm);
                buttonCancel = (Button) v.findViewById(R.id.buttonCancel);
                dismissOnCancel(buttonCancel);
                dismissOnConfirm(buttonConfirm);
                break;
            case CredentialsWithReadOnlyUser:
                v = inflater.inflate(R.layout.get_credentials_with_read_only_user, container, false);
                error = (TextView) v.findViewById(R.id.error);
                textViewBox = v.findViewById(R.id.textViewBox);
                textBox2 = (EditText) v.findViewById(R.id.textBox2);
                hideText(textBox2);
                textBox2.requestFocus();
                checkboxKeepPassword = v.findViewById(R.id.checkboxKeepPassword);
                buttonConfirm = (Button) v.findViewById(R.id.buttonConfirm);
                buttonCancel = (Button) v.findViewById(R.id.buttonCancel);
                dismissOnCancel(buttonCancel);
                dismissOnConfirm(buttonConfirm);
                break;
            default:
                getDialog().dismiss();
                break;
        }

        if (textViewBox != null)
            textViewBox.setText(t1);
        if (textBox != null && t1 != null)
            textBox.setText(t1);
        if (textBox2 != null && t2 != null)
            textBox2.setText(t2);
        if (textBox3 != null && t3 != null)
            textBox3.setText(t3);

        if (checkboxKeepPassword != null) {
            checkboxKeepPassword.setChecked(keepPassword);
        }

        TextView message = null;
        if (v != null) {
            message = (TextView) v.findViewById(R.id.message);
        }
        if (message != null) {
            message.setText(messageNum);
        }

        this.setRetainInstance(true);

        return v;
    }

    public void setDismissalListener(OnFragmentDismissedListener dismissalListener) {
        this.dismissalListener = dismissalListener;
    }

    private void hideText(EditText textBox) {
        textBox.setTransformationMethod(new PasswordTransformationMethod());
    }

    private void dismissOnCancel(Button cancelButton) {
        cancelButton.setOnClickListener(v -> {
            wasCancelled = true;
            dismissSafely();
        });
    }

    private void dismissSafely() {
        Dialog d = getDialog();
        if (d != null) {
            d.dismiss();
        }
    }

    private void dismissOnConfirm(Button buttonConfirm) {
        buttonConfirm.setOnClickListener(v -> {
            wasConfirmed = true;
            dismissSafely();
        });
    }

    private void ensureMatchingDismissOnConfirm(Button buttonConfirm, final EditText textBox1, final EditText textBox2, final TextView error) {
        buttonConfirm.setOnClickListener(v -> {
            if (textBox1.getText().toString().equals(textBox2.getText().toString())) {
                wasConfirmed = true;
                dismissSafely();
            } else {
                error.setText(errorNum);
                error.setVisibility(View.VISIBLE);
                error.invalidate();
            }
        });

        textBox1.addTextChangedListener(new TextMatcher());
        textBox2.addTextChangedListener(new TextMatcher());
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        android.util.Log.i(TAG, "onDismiss called: Sending data back to Activity");
        String[] results = new String[3];
        if (textViewBox != null) {
            results[0] = textViewBox.getText().toString();
            textViewBox.setText("");
        }
        if (textBox != null) {
            results[0] = textBox.getText().toString();
            textBox.setText("");
        }
        if (textBox2 != null) {
            results[1] = textBox2.getText().toString();
            textBox2.setText("");
        }
        if (textBox3 != null) {
            results[2] = textBox3.getText().toString();
            textBox3.setText("");
        }
        if (checkboxKeepPassword != null) {
            keepPassword = checkboxKeepPassword.isChecked();
        }
        if (dismissalListener != null && (wasConfirmed || wasCancelled)) {
            boolean cancelled = wasCancelled;
            wasCancelled = false;
            wasConfirmed = false;
            dismissalListener.onTextObtained(dialogId, results, cancelled, keepPassword);
        }
        super.onDismiss(dialog);
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setOnDismissListener(null);
        super.onDestroyView();
    }

    @Override
    public void show(FragmentManager fm, String tag) {
        try {
            FragmentTransaction ft = fm.beginTransaction();
            for (Fragment fragment : fm.getFragments()) {
                fm.beginTransaction().remove(fragment).commit();
            }
            ft.add(this, tag); //.addToBackStack(null);
            ft.commitAllowingStateLoss();
        } catch (IllegalStateException e) {
            Log.e("IllegalStateException", "Exception", e);
            e.printStackTrace();
        }
    }

    public interface OnFragmentDismissedListener {
        void onTextObtained(String dialogId, String[] obtainedStrings, boolean dialogCancelled, boolean save);
    }

    private class TextMatcher implements TextWatcher {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            error.setVisibility(View.GONE);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable arg0) {
        }
    }

}
