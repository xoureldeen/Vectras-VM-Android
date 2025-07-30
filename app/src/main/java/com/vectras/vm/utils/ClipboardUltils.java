package com.vectras.vm.utils;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import com.vectras.vm.R;

public class ClipboardUltils {
    public static void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("From Vectras VM", text);
        assert clipboard != null;
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show();
    }
}
