package com.vectras.vm.utils;

import static android.content.Intent.ACTION_VIEW;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.vectras.vm.R;

public class DialogUtils {

    public static void oneDialog(Activity _context, String _title, String _message, String _textPositiveButton, boolean _isicon, int _iconid, boolean _cancel, Runnable _onPositive, Runnable _onDismiss) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(_context, R.style.CenteredDialogTheme);
        dialog.setTitle(_title);
        dialog.setMessage(_message);
        if (_isicon) {
            dialog.setIcon(_iconid);
        }
        if (!_cancel) {
            dialog.setCancelable(false);
        }
        dialog.setPositiveButton(_textPositiveButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (_onPositive != null) _onPositive.run();
                dialog.dismiss();
            }
        });
        dialog.setOnDismissListener(dialog1 -> {
            if (_onDismiss != null) _onDismiss.run();
        });
        dialog.show();
    }
    public static void twoDialog(Activity _context, String _title, String _message, String _textPositiveButton, String _textNegativeButton, boolean _isicon, int _iconid, boolean _cancel, Runnable _onPositive, Runnable _onNegative, Runnable _onDismiss) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(_context, R.style.CenteredDialogTheme);
        dialog.setTitle(_title);
        dialog.setMessage(_message);
        if (_isicon) {
            dialog.setIcon(_iconid);
        }
        if (!_cancel) {
            dialog.setCancelable(false);
        }
        dialog.setPositiveButton(_textPositiveButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (_onPositive != null) _onPositive.run();
                dialog.dismiss();
            }
        });
        dialog.setNegativeButton(_textNegativeButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (_onNegative != null) _onNegative.run();
                dialog.dismiss();
            }
        });
        dialog.setOnDismissListener(dialog1 -> {
            if (_onDismiss != null) _onDismiss.run();
        });
        dialog.show();
    }

    public static void threeDialog(Activity _context, String _title, String _message, String _textPositiveButton, String _textNegativeButton, String _textNeutralButton ,boolean _isicon, int _iconid, boolean _cancel, Runnable _onPositive, Runnable _onNegative, Runnable _onNeutral, Runnable _onDismiss) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(_context, R.style.CenteredDialogTheme);
        dialog.setTitle(_title);
        dialog.setMessage(_message);
        if (_isicon) {
            dialog.setIcon(_iconid);
        }
        if (!_cancel) {
            dialog.setCancelable(false);
        }
        dialog.setPositiveButton(_textPositiveButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (_onPositive != null) _onPositive.run();
                dialog.dismiss();
            }
        });
        dialog.setNegativeButton(_textNegativeButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (_onNegative != null) _onNegative.run();
                dialog.dismiss();
            }
        });
        dialog.setNeutralButton(_textNeutralButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (_onNeutral != null) _onNeutral.run();
                dialog.dismiss();
            }
        });
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
