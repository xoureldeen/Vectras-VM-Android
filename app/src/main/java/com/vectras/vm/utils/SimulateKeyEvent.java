package com.vectras.vm.utils;

import android.app.Activity;
import android.view.KeyEvent;

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

    public static void qemuZoomIn(Activity activity) {
        pressAndHold(activity, KeyEvent.KEYCODE_CTRL_LEFT);
        pressAndHold(activity, KeyEvent.KEYCODE_ALT_LEFT);
        press(activity, KeyEvent.KEYCODE_PLUS);
        releaseNow(activity, KeyEvent.KEYCODE_CTRL_LEFT);
        releaseNow(activity, KeyEvent.KEYCODE_ALT_LEFT);
    }

    public static void qemuZoomOut(Activity activity) {
        pressAndHold(activity, KeyEvent.KEYCODE_CTRL_LEFT);
        pressAndHold(activity, KeyEvent.KEYCODE_ALT_LEFT);
        press(activity, KeyEvent.KEYCODE_MINUS);
        releaseNow(activity, KeyEvent.KEYCODE_CTRL_LEFT);
        releaseNow(activity, KeyEvent.KEYCODE_ALT_LEFT);
    }

    public static void qemuForcus(Activity activity) {
        pressAndHold(activity, KeyEvent.KEYCODE_CTRL_LEFT);
        pressAndHold(activity, KeyEvent.KEYCODE_ALT_LEFT);
        press(activity, KeyEvent.KEYCODE_G);
        releaseNow(activity, KeyEvent.KEYCODE_CTRL_LEFT);
        releaseNow(activity, KeyEvent.KEYCODE_ALT_LEFT);
    }

    public static void qemuSwitchToMainDisplay(Activity activity) {
        pressAndHold(activity, KeyEvent.KEYCODE_CTRL_LEFT);
        pressAndHold(activity, KeyEvent.KEYCODE_ALT_LEFT);
        press(activity, KeyEvent.KEYCODE_1);
        releaseNow(activity, KeyEvent.KEYCODE_CTRL_LEFT);
        releaseNow(activity, KeyEvent.KEYCODE_ALT_LEFT);
    }

    public static void qemuSwitchToConsole(Activity activity) {
        pressAndHold(activity, KeyEvent.KEYCODE_CTRL_LEFT);
        pressAndHold(activity, KeyEvent.KEYCODE_ALT_LEFT);
        press(activity, KeyEvent.KEYCODE_2);
        releaseNow(activity, KeyEvent.KEYCODE_CTRL_LEFT);
        releaseNow(activity, KeyEvent.KEYCODE_ALT_LEFT);
    }
}
