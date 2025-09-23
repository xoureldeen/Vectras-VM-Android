package com.vectras.vm.utils;

import static android.content.Intent.ACTION_VIEW;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.vectras.vm.R;

public class DialogUtils {

    public static void oneDialog(Activity _context, String _title, String _message, String _textPositiveButton, boolean _isicon, int _iconid, boolean _cancel, Runnable _onPositive, Runnable _onDismiss) {
        View buttonsView = LayoutInflater.from(_context).inflate(R.layout.dialog_layout, null);

        AlertDialog dialog = new AlertDialog.Builder(_context).create();
        dialog.setCancelable(_cancel);
        dialog.setView(buttonsView);

        ImageView icon = buttonsView.findViewById(R.id.icon);
        TextView title = buttonsView.findViewById(R.id.tv_title);
        TextView content = buttonsView.findViewById(R.id.tv_content);
        TextView positiveButton = buttonsView.findViewById(R.id.positiveButton);
        TextView negativeButton = buttonsView.findViewById(R.id.negativeButton);
        TextView neutralButton = buttonsView.findViewById(R.id.neutralButton);

        if (_isicon) {
            icon.setImageResource(_iconid);
        } else {
            icon.setVisibility(View.GONE);
        }

        title.setText(_title);
        content.setText(_message);

        positiveButton.setText(_textPositiveButton);
        positiveButton.setBackgroundResource(R.drawable.dialog_shape_single_button);
        negativeButton.setVisibility(View.GONE);
        neutralButton.setVisibility(View.GONE);

        positiveButton.setOnClickListener(v -> {
            if (_onPositive != null) _onPositive.run();
            dialog.dismiss();
        });

//        dialog.setPositiveButton(_textPositiveButton, (dialog2, which) -> {
//            if (_onPositive != null) _onPositive.run();
//            dialog2.dismiss();
//        });
        dialog.setOnDismissListener(dialog1 -> {
            if (_onDismiss != null) _onDismiss.run();
        });
        dialog.show();
    }
    public static void twoDialog(Activity _context, String _title, String _message, String _textPositiveButton, String _textNegativeButton, boolean _isicon, int _iconid, boolean _cancel, Runnable _onPositive, Runnable _onNegative, Runnable _onDismiss) {
        View buttonsView = LayoutInflater.from(_context).inflate(R.layout.dialog_layout, null);

        AlertDialog dialog = new AlertDialog.Builder(_context).create();
        dialog.setCancelable(_cancel);
        dialog.setView(buttonsView);

        ImageView icon = buttonsView.findViewById(R.id.icon);
        TextView title = buttonsView.findViewById(R.id.tv_title);
        TextView content = buttonsView.findViewById(R.id.tv_content);
        TextView positiveButton = buttonsView.findViewById(R.id.positiveButton);
        TextView negativeButton = buttonsView.findViewById(R.id.negativeButton);
        TextView neutralButton = buttonsView.findViewById(R.id.neutralButton);

        if (_isicon) {
            icon.setImageResource(_iconid);
        } else {
            icon.setVisibility(View.GONE);
        }

        title.setText(_title);
        content.setText(_message);

        positiveButton.setText(_textPositiveButton);
        negativeButton.setText(_textNegativeButton);
        negativeButton.setBackgroundResource(R.drawable.dialog_shape_bottom_button);
        neutralButton.setVisibility(View.GONE);

        positiveButton.setOnClickListener(v -> {
            if (_onPositive != null) _onPositive.run();
            dialog.dismiss();
        });

        negativeButton.setOnClickListener(v -> {
            if (_onNegative != null) _onNegative.run();
            dialog.dismiss();
        });
//        dialog.setPositiveButton(_textPositiveButton, (dialog2, which) -> {
//            if (_onPositive != null) _onPositive.run();
//            dialog2.dismiss();
//        });
//        dialog.setNegativeButton(_textNegativeButton, (dialog3, which) -> {
//            if (_onNegative != null) _onNegative.run();
//            dialog3.dismiss();
//        });
        dialog.setOnDismissListener(dialog1 -> {
            if (_onDismiss != null) _onDismiss.run();
        });
        dialog.show();
    }

    public static void threeDialog(Activity _context, String _title, String _message, String _textPositiveButton, String _textNegativeButton, String _textNeutralButton ,boolean _isicon, int _iconid, boolean _cancel, Runnable _onPositive, Runnable _onNegative, Runnable _onNeutral, Runnable _onDismiss) {
        View buttonsView = LayoutInflater.from(_context).inflate(R.layout.dialog_layout, null);

        AlertDialog dialog = new AlertDialog.Builder(_context).create();
        dialog.setCancelable(_cancel);
        dialog.setView(buttonsView);

        ImageView icon = buttonsView.findViewById(R.id.icon);
        TextView title = buttonsView.findViewById(R.id.tv_title);
        TextView content = buttonsView.findViewById(R.id.tv_content);
        TextView positiveButton = buttonsView.findViewById(R.id.positiveButton);
        TextView negativeButton = buttonsView.findViewById(R.id.negativeButton);
        TextView neutralButton = buttonsView.findViewById(R.id.neutralButton);

        if (_isicon) {
            icon.setImageResource(_iconid);
        } else {
            icon.setVisibility(View.GONE);
        }

        title.setText(_title);
        content.setText(_message);

        positiveButton.setText(_textPositiveButton);
        negativeButton.setText(_textNegativeButton);
        neutralButton.setText(_textNeutralButton);

        positiveButton.setOnClickListener(v -> {
            if (_onPositive != null) _onPositive.run();
            dialog.dismiss();
        });

        negativeButton.setOnClickListener(v -> {
            if (_onNegative != null) _onNegative.run();
            dialog.dismiss();
        });

        neutralButton.setOnClickListener(v -> {
            if (_onNeutral != null) _onNeutral.run();
            dialog.dismiss();
        });

//        dialog.setPositiveButton(_textPositiveButton, (dialog2, which) -> {
//            if (_onPositive != null) _onPositive.run();
//            dialog2.dismiss();
//        });
//        dialog.setNegativeButton(_textNegativeButton, (dialog3, which) -> {
//            if (_onNegative != null) _onNegative.run();
//            dialog3.dismiss();
//        });
//        dialog.setNeutralButton(_textNeutralButton, (dialog4, which) -> {
//            if (_onNeutral != null) _onNeutral.run();
//            dialog4.dismiss();
//        });
        dialog.setOnDismissListener(dialog1 -> {
            if (_onDismiss != null) _onDismiss.run();
        });
        dialog.show();
    }

    public static void joinTelegram(Activity _activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_activity);
        if (!prefs.getBoolean("tgDialog", false)) {
            threeDialog(_activity, _activity.getResources().getString(R.string.join_us_on_telegram),
                    _activity.getResources().getString(R.string.join_us_on_telegram_where_we_publish_all_the_news_and_updates_and_receive_your_opinions_and_bugs),
                    _activity.getResources().getString(R.string.join), _activity.getResources().getString(R.string.cancel), _activity.getResources().getString(R.string.dont_show_again),
                    true, R.drawable.send_24px, true,
                    () -> {
                        String tg = "https://t.me/vectras_os";
                        Intent f = new Intent(ACTION_VIEW);
                        f.setData(Uri.parse(tg));
                        _activity.startActivity(f);
                    }, null,
                    () -> {
                        SharedPreferences.Editor edit = prefs.edit();
                        edit.putBoolean("tgDialog", true);
                        edit.apply();
                    }, null);
        }
    }
}
