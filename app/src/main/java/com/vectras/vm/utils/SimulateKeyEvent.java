package com.vectras.vm.utils;

import android.app.Activity;

public class SimulateKeyEvent {
    public static void press(Activity activity, int keyEventCode) {
        activity.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyEventCode));
        activity.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyEventCode));
    }

    public static void pressAndHold(Activity activity, int keyEventCode) {
        activity.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyEventCode));
    }

    public static void releaseNow(Activity activity, int keyEventCode) {
        activity.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyEventCode));
    }
}
