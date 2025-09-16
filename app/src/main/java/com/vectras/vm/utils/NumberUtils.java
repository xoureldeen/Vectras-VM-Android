package com.vectras.vm.utils;

public class NumberUtils {
    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            return 0;
        }
        return (int) l;
    }
}
