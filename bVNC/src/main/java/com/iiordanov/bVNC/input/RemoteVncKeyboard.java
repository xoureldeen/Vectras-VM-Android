package com.iiordanov.bVNC.input;

import static com.undatech.opaque.util.GeneralUtils.debugLog;

import android.content.Context;
import android.os.Handler;
import android.view.KeyEvent;

import com.iiordanov.bVNC.App;
import com.iiordanov.bVNC.RfbProto;
import com.iiordanov.tigervnc.rfb.UnicodeToKeysym;
import com.undatech.opaque.InputCarriable;
import com.undatech.opaque.RfbConnectable;
import com.undatech.opaque.input.RemotePointer;

public class RemoteVncKeyboard extends RemoteKeyboard {
    private final static String TAG = "RemoteKeyboard";
    public static boolean rAltAsIsoL3Shift = false;
    protected InputCarriable remoteInput;

    public RemoteVncKeyboard(RfbConnectable r, InputCarriable i, Context c, Handler h,
                             boolean rAltAsIsoL3Shift, boolean debugLog) {
        super(r, c, h, debugLog);
        remoteInput = i;
        // Indicate we want Right Alt to be ISO L3 SHIFT if preferred.
        RemoteVncKeyboard.rAltAsIsoL3Shift = rAltAsIsoL3Shift;
    }

    public boolean processLocalKeyEvent(int keyCode, KeyEvent evt, int additionalMetaState) {
        debugLog(App.debugLog, TAG, "processLocalKeyEvent: " + evt.toString() + " " + keyCode);
        // Drop repeated modifiers
        if (shouldDropModifierKeys(evt))
            return true;
        rfb.remoteKeyboardState.detectHardwareMetaState(evt);

        if (rfb != null && rfb.isInNormalProtocol()) {
            RemotePointer pointer = remoteInput.getPointer();
            boolean down = (evt.getAction() == KeyEvent.ACTION_DOWN) ||
                    (evt.getAction() == KeyEvent.ACTION_MULTIPLE);
            boolean unicode = false;
            int numchars = 1;
            int metaState = additionalMetaState | convertEventMetaState(evt);

            if (keyCode == KeyEvent.KEYCODE_MENU)
                return true;                           // Ignore menu key

            if (pointer.hardwareButtonsAsMouseEvents(keyCode, evt, metaState | onScreenMetaState | hardwareMetaState))
                return true;

            int key = 0, keysym = 0;

            // Detect whether this event is coming from a default hardware keyboard.
            boolean defaultHardwareKbd = (evt.getDeviceId() == 0);
            if (!down) {
                switch (evt.getScanCode()) {
                    case SCAN_ESC:
                        key = 0xff1b;
                        break;
                    case SCAN_LEFTCTRL:
                        hardwareMetaState &= ~CTRL_MASK;
                        break;
                    case SCAN_RIGHTCTRL:
                        hardwareMetaState &= ~RCTRL_MASK;
                        break;
                    case SCAN_F1:
                        keysym = 0xffbe;
                        break;
                    case SCAN_F2:
                        keysym = 0xffbf;
                        break;
                    case SCAN_F3:
                        keysym = 0xffc0;
                        break;
                    case SCAN_F4:
                        keysym = 0xffc1;
                        break;
                    case SCAN_F5:
                        keysym = 0xffc2;
                        break;
                    case SCAN_F6:
                        keysym = 0xffc3;
                        break;
                    case SCAN_F7:
                        keysym = 0xffc4;
                        break;
                    case SCAN_F8:
                        keysym = 0xffc5;
                        break;
                    case SCAN_F9:
                        keysym = 0xffc6;
                        break;
                    case SCAN_F10:
                        keysym = 0xffc7;
                        break;
                }

                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                        hardwareMetaState &= ~CTRL_MASK;
                        break;
                    case KeyEvent.KEYCODE_ALT_LEFT:
                        // Leaving KeyEvent.KEYCODE_ALT_LEFT for symbol input on hardware keyboards.
                        if (!defaultHardwareKbd)
                            hardwareMetaState &= ~ALT_MASK;
                        break;
                    case KeyEvent.KEYCODE_ALT_RIGHT:
                        hardwareMetaState &= ~RALT_MASK;
                        break;
                }
            }

            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    keysym = 0xff1b;
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    keysym = 0xff51;
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    keysym = 0xff52;
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    keysym = 0xff53;
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    keysym = 0xff54;
                    break;
                case KeyEvent.KEYCODE_DEL:
                    keysym = 0xff08;
                    break;
                case KeyEvent.KEYCODE_ENTER:
                    keysym = 0xff0d;
                    break;
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    keysym = 0xff0d;
                    break;
                case KeyEvent.KEYCODE_TAB:
                    keysym = 0xff09;
                    break;
                case 92 /* KEYCODE_PAGE_UP */:
                    keysym = 0xff55;
                    break;
                case 93 /* KEYCODE_PAGE_DOWN */:
                    keysym = 0xff56;
                    break;
                case 111 /* KEYCODE_ESCAPE */:
                    keysym = 0xff1b;
                    break;
                case 112 /* KEYCODE_FORWARD_DEL */:
                    keysym = 0xffff;
                    break;
                case 113 /* KEYCODE_CTRL_LEFT */:
                    keysym = 0xffe3;
                    break;
                case 114 /* KEYCODE_CTRL_RIGHT */:
                    keysym = 0xffe4;
                    break;
                case 115 /* KEYCODE_CAPS_LOCK */:
                    keysym = 0xffe5;
                    break;
                case 116 /* KEYCODE_SCROLL_LOCK */:
                    keysym = 0xff14;
                    break;
                case 117 /* KEYCODE_META_LEFT */:
                    keysym = 0xffeb;
                    break;
                case 118 /* KEYCODE_META_RIGHT */:
                    keysym = 0xffec;
                    break;
                case 120 /* KEYCODE_SYSRQ */:
                    keysym = 0xff61;
                    break;
                case 121 /* KEYCODE_BREAK */:
                    keysym = 0xff6b;
                    break;
                case 122 /* KEYCODE_MOVE_HOME */:
                    keysym = 0xff50;
                    break;
                case 123 /* KEYCODE_MOVE_END */:
                    keysym = 0xff57;
                    break;
                case 124 /* KEYCODE_INSERT */:
                    keysym = 0xff63;
                    break;
                case 131 /* KEYCODE_F1 */:
                    keysym = 0xffbe;
                    break;
                case 132 /* KEYCODE_F2 */:
                    keysym = 0xffbf;
                    break;
                case 133 /* KEYCODE_F3 */:
                    keysym = 0xffc0;
                    break;
                case 134 /* KEYCODE_F4 */:
                    keysym = 0xffc1;
                    break;
                case 135 /* KEYCODE_F5 */:
                    keysym = 0xffc2;
                    break;
                case 136 /* KEYCODE_F6 */:
                    keysym = 0xffc3;
                    break;
                case 137 /* KEYCODE_F7 */:
                    keysym = 0xffc4;
                    break;
                case 138 /* KEYCODE_F8 */:
                    keysym = 0xffc5;
                    break;
                case 139 /* KEYCODE_F9 */:
                    keysym = 0xffc6;
                    break;
                case 140 /* KEYCODE_F10 */:
                    keysym = 0xffc7;
                    break;
                case 141 /* KEYCODE_F11 */:
                    keysym = 0xffc8;
                    break;
                case 142 /* KEYCODE_F12 */:
                    keysym = 0xffc9;
                    break;
                case 143 /* KEYCODE_NUM_LOCK */:
                    keysym = 0xff7f;
                    break;
                case KeyEvent.KEYCODE_ALT_LEFT:
                    keysym = RfbProto.XK_LALT;
                    break;
                case KeyEvent.KEYCODE_ALT_RIGHT:
                    keysym = RfbProto.XK_RALT;
                    break;
                case KeyEvent.KEYCODE_SHIFT_LEFT:
                    keysym = RfbProto.XK_LSHIFT;
                    break;
                case KeyEvent.KEYCODE_SHIFT_RIGHT:
                    keysym = RfbProto.XK_RSHIFT;
                    break;

                case 0   /* KEYCODE_UNKNOWN */:
                    if (evt.getCharacters() != null) {
                        key = evt.getCharacters().charAt(0);
                        keysym = UnicodeToKeysym.translate(key);
                        numchars = evt.getCharacters().length();
                        unicode = true;
                        debugLog(App.debugLog, TAG, "processLocalKeyEvent: KEYCODE_UNKNOWN, " +
                                "unicode key: " + key);
                    }
                    break;
                default:
                    // Modifier handling is a bit tricky. Alt, Ctrl, and Super should be passed
                    // through to the VNC server so that they get handled there, but strip
                    // them from the character before retrieving the Unicode char from it.
                    // Don't clear Shift, we still want uppercase characters.
                    int metaMask = (KeyEvent.META_CTRL_MASK | KeyEvent.META_META_MASK);
                    // When events come from a default hardware keyboard, we still want alt-key combinations to
                    // give us symbols, so we only strip out KeyEvent.META_ALT_MASK if we've decided to send
                    // over ALT as a separate key modifier in convertEventMetaState().
                    if ((metaState & ALT_MASK) != 0 || (metaState & RALT_MASK) != 0) {
                        metaMask |= KeyEvent.META_ALT_MASK;
                    }
                    // Add SHIFT to the metastate to get upper-case characters if onscreen shift is pressed
                    int addShift = 0;
                    if ((onScreenMetaState & SHIFT_MASK) != 0) {
                        addShift |= KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON;
                    }
                    KeyEvent copy = new KeyEvent(evt.getDownTime(), evt.getEventTime(), evt.getAction(),
                            evt.getKeyCode(), evt.getRepeatCount(), evt.getMetaState() & ~metaMask | addShift,
                            evt.getDeviceId(), evt.getScanCode());
                    key = copy.getUnicodeChar();
                    keysym = UnicodeToKeysym.translate(key);
                    debugLog(App.debugLog, TAG, "processLocalKeyEvent: extracted unicode key: " +
                            key + ", keysym: " + keysym);
                    break;
            }

            if (down) {
                // Look for standard scan-codes from hardware keyboards
                switch (evt.getScanCode()) {
                    case SCAN_ESC:
                        keysym = 0xff1b;
                        break;
                    case SCAN_LEFTCTRL:
                        hardwareMetaState |= CTRL_MASK;
                        break;
                    case SCAN_RIGHTCTRL:
                        hardwareMetaState |= RCTRL_MASK;
                        break;
                    case SCAN_F1:
                        keysym = 0xffbe;
                        break;
                    case SCAN_F2:
                        keysym = 0xffbf;
                        break;
                    case SCAN_F3:
                        keysym = 0xffc0;
                        break;
                    case SCAN_F4:
                        keysym = 0xffc1;
                        break;
                    case SCAN_F5:
                        keysym = 0xffc2;
                        break;
                    case SCAN_F6:
                        keysym = 0xffc3;
                        break;
                    case SCAN_F7:
                        keysym = 0xffc4;
                        break;
                    case SCAN_F8:
                        keysym = 0xffc5;
                        break;
                    case SCAN_F9:
                        keysym = 0xffc6;
                        break;
                    case SCAN_F10:
                        keysym = 0xffc7;
                        break;
                }

                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                        hardwareMetaState |= CTRL_MASK;
                        break;
                    case KeyEvent.KEYCODE_ALT_LEFT:
                        // Leaving KeyEvent.KEYCODE_ALT_LEFT for symbol input on hardware keyboards.
                        if (!defaultHardwareKbd)
                            hardwareMetaState |= ALT_MASK;
                        break;
                    case KeyEvent.KEYCODE_ALT_RIGHT:
                        hardwareMetaState |= RALT_MASK;
                        break;
                }
            }

            try {
                if (afterMenu) {
                    afterMenu = false;
                    if (!down && keysym != lastKeyDown)
                        return true;
                }
                if (down)
                    lastKeyDown = keysym;

                metaState = metaState | onScreenMetaState | hardwareMetaState;
                if (down) {
                    lastDownMetaState = metaState;
                } else {
                    metaState = lastDownMetaState;
                    lastDownMetaState = 0;
                }

                if (numchars == 1) {
                    debugLog(App.debugLog, TAG, "processLocalKeyEvent: Sending key. Down: " + down +
                            ", key: " + key + ", keysym:" + keysym + ", metaState: " + metaState);
                    rfb.writeKeyEvent(keysym, metaState, down);
                    // If this is a unicode key, the up event will never come, so we artificially insert it.
                    if (unicode) {
                        debugLog(App.debugLog, TAG, "processLocalKeyEvent: Unicode key. Down: false" +
                                ", key: " + key + ", keysym:" + keysym + ", metaState: " + metaState);
                        rfb.writeKeyEvent(keysym, metaState, false);
                    }

                } else if (numchars > 1) {
                    for (int i = 0; i < numchars; i++) {
                        key = evt.getCharacters().charAt(i);
                        keysym = UnicodeToKeysym.translate(key);
                        debugLog(App.debugLog, TAG, "processLocalKeyEvent: Sending multiple keys. Key: " +
                                key + " keysym: " + keysym + ", metaState: " + metaState);
                        rfb.writeKeyEvent(keysym, metaState, true);
                        rfb.writeKeyEvent(keysym, metaState, false);
                        lastDownMetaState = 0;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public void sendMetaKey(MetaKeyBean meta) {
        RemotePointer pointer = remoteInput.getPointer();
        int x = pointer.getX();
        int y = pointer.getY();

        if (meta.isMouseClick()) {
            rfb.writePointerEvent(x, y, meta.getMetaFlags() | onScreenMetaState | hardwareMetaState, meta.getMouseButtons(), false);
            rfb.writePointerEvent(x, y, meta.getMetaFlags() | onScreenMetaState | hardwareMetaState, 0, false);
        } else {
            rfb.writeKeyEvent(meta.getKeySym(), meta.getMetaFlags() | onScreenMetaState | hardwareMetaState, true);
            rfb.writeKeyEvent(meta.getKeySym(), meta.getMetaFlags() | onScreenMetaState | hardwareMetaState, false);
        }
    }
}
