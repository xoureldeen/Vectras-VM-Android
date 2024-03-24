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


package com.undatech.opaque.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.undatech.remoteClientUi.R;

public class ChoiceFragment extends DialogFragment {
    public static String TAG = "ChoiceFragment";
    private Button noButton;
    private Button yesButton;
    private TextView message;
    private boolean result;
    private OnFragmentDismissedListener dismissalListener;
    private String title;
    private String messageText;
    private String positiveButtonText;
    private String negativeButtonText;
    public ChoiceFragment() {
    }

    public static ChoiceFragment newInstance(String title, String messageText, String positiveButtonText,
                                             String negativeButtonText, OnFragmentDismissedListener dismissalListener) {
        android.util.Log.e(TAG, "newInstance called");
        ChoiceFragment f = new ChoiceFragment();
        f.setOnFragmentDismissedListener(dismissalListener);

        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("messageText", messageText);
        args.putString("positiveButtonText", positiveButtonText);
        args.putString("negativeButtonText", negativeButtonText);
        f.setArguments(args);
        f.setRetainInstance(true);

        return f;
    }

    /**
     * We make sure that if this dialog is produced automatically as part of an activity rebuild
     * and its dismissalListener is null, it closes itself upon being attached to the window.
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (dismissalListener == null) {
            this.dismiss();
        }
    }

    public void setOnFragmentDismissedListener(OnFragmentDismissedListener dismissalListener) {
        this.dismissalListener = dismissalListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.e(TAG, "onCreate called");
        title = getArguments().getString("title");
        messageText = getArguments().getString("messageText");
        positiveButtonText = getArguments().getString("positiveButtonText");
        negativeButtonText = getArguments().getString("negativeButtonText");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        android.util.Log.e(TAG, "onCreateView called");

        // Set title for this dialog
        getDialog().setTitle(title);

        View v = inflater.inflate(R.layout.choice, container, false);
        message = (TextView) v.findViewById(R.id.message);
        message.setText(messageText);

        yesButton = (Button) v.findViewById(R.id.yesButton);
        yesButton.setText(positiveButtonText);
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChoiceFragment.this.result = true;
                ChoiceFragment.this.dismiss();
            }
        });

        noButton = (Button) v.findViewById(R.id.noButton);
        noButton.setText(negativeButtonText);
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChoiceFragment.this.result = false;
                ChoiceFragment.this.dismiss();
            }
        });
        return v;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        android.util.Log.e(TAG, "dismiss: sending back data to Activity");
        if (dismissalListener != null) {
            dismissalListener.onResponseObtained(result);
        }
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setOnDismissListener(null);
        super.onDestroyView();
    }

    public interface OnFragmentDismissedListener {
        public void onResponseObtained(boolean result);
    }
}
