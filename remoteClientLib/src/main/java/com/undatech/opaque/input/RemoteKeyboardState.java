/**
 * Copyright (C) 2021- Iordan Iordanov
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

package com.undatech.opaque.input;

import android.view.KeyEvent;

import com.undatech.opaque.util.GeneralUtils;

public class RemoteKeyboardState {
    private static final String TAG = "RemoteKeyboardState";
    private int remoteKeyboardMetaState = 0;
    private int hardwareMetaState = 0;
    private boolean debugLogging = false;

    public RemoteKeyboardState(boolean debugLogging) {
        this.debugLogging = debugLogging;
    }

    public void detectHardwareMetaState(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int scanCode = event.getScanCode();
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN || event.getAction() == KeyEvent.ACTION_MULTIPLE;

        if (!down) {
            switch (scanCode) {
                case RemoteKeyboard.SCAN_LEFTCTRL:
                    hardwareMetaState &= ~RemoteKeyboard.CTRL_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware LCTRL scanCode, down:" + down);
                    break;
                case RemoteKeyboard.SCAN_RIGHTCTRL:
                    hardwareMetaState &= ~RemoteKeyboard.RCTRL_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware RCTRL scanCode, down: " + down);
                    break;
                case RemoteKeyboard.SCAN_LEFTALT:
                    hardwareMetaState &= ~RemoteKeyboard.ALT_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware LALT scanCode, down: " + down);
                    break;
                case RemoteKeyboard.SCAN_RIGHTALT:
                    hardwareMetaState &= ~RemoteKeyboard.RALT_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware RALT scanCode, down: " + down);
                    break;
                case RemoteKeyboard.SCAN_LEFTSHIFT:
                    hardwareMetaState &= ~RemoteKeyboard.SHIFT_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware LSHIFT scanCode, down: " + down);
                    break;
                case RemoteKeyboard.SCAN_RIGHTSHIFT:
                    hardwareMetaState &= ~RemoteKeyboard.RSHIFT_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware RSHIFT scanCode, down: " + down);
                    break;
                case RemoteKeyboard.SCAN_LEFTSUPER:
                    hardwareMetaState &= ~RemoteKeyboard.SUPER_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware LSUPER scanCode, down: " + down);
                    break;
                case RemoteKeyboard.SCAN_RIGHTSUPER:
                    hardwareMetaState &= ~RemoteKeyboard.RSUPER_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware RSUPER scanCode, down: " + down);
                    break;
            }

            switch (keyCode) {
                case KeyEvent.KEYCODE_CTRL_LEFT:
                    hardwareMetaState &= ~RemoteKeyboard.CTRL_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware LCTRL keyCode, down: " + down);
                    break;
                case KeyEvent.KEYCODE_CTRL_RIGHT:
                    hardwareMetaState &= ~RemoteKeyboard.RCTRL_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware RCTRL keyCode, down: " + down);
                    break;
                case KeyEvent.KEYCODE_ALT_LEFT:
                    hardwareMetaState &= ~RemoteKeyboard.ALT_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware LALT keyCode, down: " + down);
                    break;
                case KeyEvent.KEYCODE_ALT_RIGHT:
                    hardwareMetaState &= ~RemoteKeyboard.RALT_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware RALT keyCode, down: " + down);
                    break;
                case KeyEvent.KEYCODE_SHIFT_LEFT:
                    hardwareMetaState &= ~RemoteKeyboard.SHIFT_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware LSHIFT keyCode, down: " + down);
                    break;
                case KeyEvent.KEYCODE_SHIFT_RIGHT:
                    hardwareMetaState &= ~RemoteKeyboard.RSHIFT_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware RSHIFT keyCode, down: " + down);
                    break;
                case KeyEvent.KEYCODE_META_LEFT:
                    hardwareMetaState &= ~RemoteKeyboard.SUPER_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware LSUPER keyCode, down: " + down);
                    break;
                case KeyEvent.KEYCODE_META_RIGHT:
                    hardwareMetaState &= ~RemoteKeyboard.RSUPER_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware RSUPER keyCode, down: " + down);
                    break;

                case KeyEvent.KEYCODE_DPAD_CENTER:
                    hardwareMetaState &= ~RemoteKeyboard.CTRL_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware LCTRL via DPAD keyCode, down: " + down);
                    break;
            }
        } else {
            switch (scanCode) {
                case RemoteKeyboard.SCAN_LEFTCTRL:
                    hardwareMetaState |= RemoteKeyboard.CTRL_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware LCTRL scanCode, down: " + down);
                    break;
                case RemoteKeyboard.SCAN_RIGHTCTRL:
                    hardwareMetaState |= RemoteKeyboard.RCTRL_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware RCTRL scanCode, down: " + down);
                    break;
                case RemoteKeyboard.SCAN_LEFTALT:
                    hardwareMetaState |= RemoteKeyboard.ALT_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware LALT scanCode, down: " + down);
                    break;
                case RemoteKeyboard.SCAN_RIGHTALT:
                    hardwareMetaState |= RemoteKeyboard.RALT_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware RALT scanCode, down: " + down);
                    break;
                case RemoteKeyboard.SCAN_LEFTSHIFT:
                    hardwareMetaState |= RemoteKeyboard.SHIFT_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware LSHIFT scanCode, down: " + down);
                    break;
                case RemoteKeyboard.SCAN_RIGHTSHIFT:
                    hardwareMetaState |= RemoteKeyboard.RSHIFT_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware RSHIFT scanCode, down: " + down);
                    break;
                case RemoteKeyboard.SCAN_LEFTSUPER:
                    hardwareMetaState |= RemoteKeyboard.SUPER_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware LSUPER scanCode, down: " + down);
                    break;
                case RemoteKeyboard.SCAN_RIGHTSUPER:
                    hardwareMetaState |= RemoteKeyboard.RSUPER_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware RSUPER scanCode, down: " + down);
                    break;
            }

            switch (keyCode) {
                case KeyEvent.KEYCODE_CTRL_LEFT:
                    hardwareMetaState |= RemoteKeyboard.CTRL_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware LCTRL keyCode, down: " + down);
                    break;
                case KeyEvent.KEYCODE_CTRL_RIGHT:
                    hardwareMetaState |= RemoteKeyboard.RCTRL_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware RCTRL keyCode, down: " + down);
                    break;
                case KeyEvent.KEYCODE_ALT_LEFT:
                    hardwareMetaState |= RemoteKeyboard.ALT_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware LALT keyCode, down: " + down);
                    break;
                case KeyEvent.KEYCODE_ALT_RIGHT:
                    hardwareMetaState |= RemoteKeyboard.RALT_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware RALT keyCode, down: " + down);
                    break;
                case KeyEvent.KEYCODE_SHIFT_LEFT:
                    hardwareMetaState |= RemoteKeyboard.SHIFT_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware LSHIFT keyCode, down: " + down);
                    break;
                case KeyEvent.KEYCODE_SHIFT_RIGHT:
                    hardwareMetaState |= RemoteKeyboard.RSHIFT_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware RSHIFT keyCode, down: " + down);
                    break;
                case KeyEvent.KEYCODE_META_LEFT:
                    hardwareMetaState |= RemoteKeyboard.SUPER_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware LSUPER keyCode, down: " + down);
                    break;
                case KeyEvent.KEYCODE_META_RIGHT:
                    hardwareMetaState |= RemoteKeyboard.RSUPER_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware RSUPER keyCode, down: " + down);
                    break;

                case KeyEvent.KEYCODE_DPAD_CENTER:
                    hardwareMetaState |= RemoteKeyboard.CTRL_MASK;
                    GeneralUtils.debugLog(this.debugLogging, TAG, "detected hardware LCTRL via DPAD keyCode, down: " + down);
                    break;
            }
        }
    }

    public boolean shouldSendModifier(int softwareMetaState,
                                      int modifier, boolean keyDown) {
        boolean shouldSend = false;
        boolean wasSentAsHardwareKeyAlready = (hardwareMetaState & modifier) != 0;
        boolean softwareMetaStateContainsModifier = (softwareMetaState & modifier) != 0;
        boolean isKeyDownAndRemoteUp = keyDown && (remoteKeyboardMetaState & modifier) == 0;
        boolean isKeyUpAndRemoteDown = !keyDown && (remoteKeyboardMetaState & modifier) != 0;
        boolean hasChangedState = softwareMetaStateContainsModifier && (isKeyDownAndRemoteUp || isKeyUpAndRemoteDown);

        // Send simulated modifier only if it wasn't sent as a hardware key already
        // and if it wasn't sent already to prevent multiple down events for the same modifier
        if (hasChangedState && !wasSentAsHardwareKeyAlready) {
            GeneralUtils.debugLog(this.debugLogging, TAG, "shouldSendModifier, shouldSend: true" +
                    ", wasSentAsHardwareKeyAlready: " + wasSentAsHardwareKeyAlready +
                    ", softwareMetaStateContainsModifier: " + softwareMetaStateContainsModifier +
                    ", hasChangedState: " + hasChangedState);
            shouldSend = true;
        }

        return shouldSend;
    }

    public void updateRemoteMetaState(int modifier, boolean down) {
        if (down) {
            remoteKeyboardMetaState |= modifier;
        } else {
            remoteKeyboardMetaState &= ~modifier;
        }
    }
}
