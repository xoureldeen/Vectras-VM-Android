package com.anbui.elephant.interaction;

import java.util.Locale;

public class InteractionUtils {
    public static String formatCount(int number) {
        if (number >= 1_000_000) {
            return String.format(Locale.US, "%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format(Locale.US, "%.1fK", number / 1_000.0);
        } else {
            return String.valueOf(number);
        }
    }
}
