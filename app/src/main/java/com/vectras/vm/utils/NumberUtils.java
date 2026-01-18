package com.vectras.vm.utils;

import java.util.Locale;

public class NumberUtils {
    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            return 0;
        }
        return (int) l;
    }

    public static String getFormatSizeFromMB(long mb) {
        if (mb < 1024) {
            return mb + " MB";
        } else {
            double gb = mb / 1024.0;
            return String.format(Locale.US, "%.2f GB", gb);
        }
    }

}
