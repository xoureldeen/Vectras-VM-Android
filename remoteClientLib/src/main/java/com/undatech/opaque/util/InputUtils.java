package com.undatech.opaque.util;

import android.content.Context;
import android.content.res.Configuration;

public class InputUtils {
    public static boolean isNoQwertyKbd(Context context) {
        return context.getResources().getConfiguration().keyboard != Configuration.KEYBOARD_QWERTY;
    }
}
