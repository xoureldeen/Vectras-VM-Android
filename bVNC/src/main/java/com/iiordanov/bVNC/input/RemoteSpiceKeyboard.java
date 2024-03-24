/**
 * Copyright (C) 2013- Iordan Iordanov
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


package com.iiordanov.bVNC.input;

import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;

import com.iiordanov.bVNC.Constants;
import com.undatech.opaque.InputCarriable;
import com.undatech.opaque.RfbConnectable;
import com.undatech.opaque.Viewable;
import com.undatech.opaque.input.RemotePointer;
import com.undatech.opaque.util.GeneralUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class RemoteSpiceKeyboard extends RemoteKeyboard {
    final static int SCANCODE_SHIFT_MASK = 0x10000;
    final static int SCANCODE_ALTGR_MASK = 0x20000;
    final static int SCANCODE_CIRCUMFLEX_MASK = 0x40000;
    final static int SCANCODE_DIAERESIS_MASK = 0x80000;
    final static int UNICODE_MASK = 0x100000;
    final static int UNICODE_META_MASK = KeyEvent.META_CTRL_MASK | KeyEvent.META_META_MASK | KeyEvent.META_CAPS_LOCK_ON;
    private final static String TAG = "RemoteSpiceKeyboard";
    protected Viewable canvas;
    protected InputCarriable remoteInput;
    private HashMap<Integer, Integer[]> table;

    public RemoteSpiceKeyboard(
            Resources resources,
            RfbConnectable r,
            Viewable v,
            InputCarriable i,
            Handler h,
            String layoutMapFile,
            boolean debugLog
    ) throws IOException {
        super(r, v.getContext(), h, debugLog);
        canvas = v;
        remoteInput = i;
        this.table = loadKeyMap(resources, "layouts/" + layoutMapFile);
    }

    private HashMap<Integer, Integer[]> loadKeyMap(Resources r, String file) throws IOException {
        InputStream is;
        try {
            is = r.getAssets().open(file);
        } catch (IOException e) {
            // If layout map file was not found, load the default one.
            is = r.getAssets().open("layouts/" + Constants.DEFAULT_LAYOUT_MAP);
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line = in.readLine();
        HashMap<Integer, Integer[]> table = new HashMap<Integer, Integer[]>(500);
        while (line != null) {
            GeneralUtils.debugLog(debugLogging, TAG, "Layout " + file + " " + line);
            String[] tokens = line.split(" ");
            Integer[] scanCodes = new Integer[tokens.length - 1];
            for (int i = 1; i < tokens.length; i++) {
                scanCodes[i - 1] = Integer.parseInt(tokens[i]);
            }
            table.put(Integer.parseInt(tokens[0]), scanCodes);
            line = in.readLine();
        }
        return table;
    }

    public boolean processLocalKeyEvent(int keyCode, KeyEvent event, int additionalMetaState) {
        return keyEvent(keyCode, event, additionalMetaState);
    }

    public boolean keyEvent(int keyCode, KeyEvent event, int additionalMetaState) {
        GeneralUtils.debugLog(debugLogging, TAG, event.toString());
        // Drop repeated modifiers
        if (shouldDropModifierKeys(event))
            return true;
        rfb.remoteKeyboardState.detectHardwareMetaState(event);

        int action = event.getAction();
        boolean down = (action == KeyEvent.ACTION_DOWN);
        // Combine current event meta state with any meta state passed in.
        int metaState = additionalMetaState | convertEventMetaState(event, event.getMetaState());
        
        /* TODO: Consider whether this is a good idea. At least some scan codes between
           my bluetooth keyboard and what the VM expects do not match. For example, d-pad does not send arrows.
        // If the event has a scan code, just send that along!
        if (event.getScanCode() != 0) {
            GeneralUtils.debugLog(debugLog, TAG, "Event has a scancode, sending that: " + event.getScanCode());
            spicecomm.writeKeyEvent(event.getScanCode(), 0, down);
            return true;
        }*/

        // Ignore menu key and handle other hardware buttons here.
        if (keyCode == KeyEvent.KEYCODE_MENU ||
                remoteInput.getPointer().hardwareButtonsAsMouseEvents(keyCode,
                        event,
                        metaState | onScreenMetaState)) {
        } else if (rfb != null && rfb.isInNormalProtocol()) {
            // Combine metaState
            metaState = onScreenMetaState | metaState;

            // If the event consists of multiple unicode characters, send them one by one.
            if (action == KeyEvent.ACTION_MULTIPLE) {
                String s = event.getCharacters();
                if (s != null) {
                    int numchars = s.length();
                    for (int i = 0; i < numchars; i++) {
                        GeneralUtils.debugLog(debugLogging, TAG, "Trying to convert unicode to KeyEvent: " + (int) s.charAt(i));
                        if (!sendUnicode(s.charAt(i), additionalMetaState)) {
                            writeKeyEvent(true, (int) s.charAt(i), metaState, true, true);
                        }
                    }
                }
            } else {
                // Get unicode character that would result from this event, masking out Ctrl and Super keys.
                int unicode = event.getUnicodeChar(event.getMetaState() & ~UNICODE_META_MASK);
                Integer[] scanCodes = null;
                int unicodeMetaState = 0;

                if (unicode > 0) {
                    // We managed to get a unicode value, if Alt is present in the metaState, check whether scan codes can be determined
                    // for this unicode character with this keymap
                    if ((event.getMetaState() & KeyEvent.META_ALT_MASK) != 0) {
                        scanCodes = table.get(unicode |= UNICODE_MASK);
                    }

                    // If scan codes cannot be determined, try to get a unicode value without Alt and send Alt
                    // as meta-state.
                    if (scanCodes == null || scanCodes.length == 0) {
                        unicode = -1;
                    } else {
                        // We managed to get a unicode value with ALT potentially enabled, and valid scancodes.
                        // So convert and send that over without sending ALT as meta-state.
                        unicodeMetaState = additionalMetaState | onScreenMetaState |
                                convertEventMetaState(event, event.getMetaState() & ~KeyEvent.META_SHIFT_MASK & ~KeyEvent.META_ALT_MASK);
                    }
                }

                if (unicode <= 0) {
                    // Try to get a unicode value without ALT mask and if successful do not mask ALT out of the meta-state.
                    unicode = event.getUnicodeChar(event.getMetaState() & ~UNICODE_META_MASK & ~KeyEvent.META_ALT_MASK);
                    unicodeMetaState = additionalMetaState | onScreenMetaState |
                            convertEventMetaState(event, event.getMetaState() & ~KeyEvent.META_SHIFT_MASK);
                }

                if (unicode > 0) {
                    GeneralUtils.debugLog(debugLogging, TAG, String.format("Got unicode value: %d from event", unicode));
                    writeKeyEvent(true, unicode, unicodeMetaState, down, false);
                } else {
                    // We were unable to obtain a unicode, or the list of scancodes was empty, so we have to try converting a keyCode.
                    GeneralUtils.debugLog(debugLogging, TAG, "Could not get unicode or determine scancodes for event. Keycode: " + event.getKeyCode());
                    writeKeyEvent(false, event.getKeyCode(), metaState, down, false);
                }
            }
        }
        return true;
    }

    private void writeKeyEvent(boolean isUnicode, int code, int metaState, boolean down, boolean sendUpEvents) {
        if (down) {
            lastDownMetaState = metaState;
        } else {
            metaState = lastDownMetaState;
            lastDownMetaState = 0;
        }

        if (isUnicode) {
            code |= UNICODE_MASK;
            Log.d(TAG, String.format("isUnicode true, adding UNICODE_MASK to code %d", code));
        } else {
            Log.d(TAG, String.format("isUnicode false, not adding UNICODE_MASK to code %d", code));
        }
        //GeneralUtils.debugLog(debugLog, TAG, "Trying to convert keycode or masked unicode: " + code);
        Integer[] scanCode = null;
        try {
            scanCode = table.get(code);
            if (scanCode == null) {
                Log.d(TAG, "Could not convert KeyCode to scan codes. Not sending key.");
                return;
            }
            for (int i = 0; i < scanCode.length; i++) {
                int scode = scanCode[i];
                int meta = metaState;
                //GeneralUtils.debugLog(debugLog, TAG, "Got back possibly masked scanCode: " + scode);
                if ((scode & SCANCODE_SHIFT_MASK) != 0) {
                    GeneralUtils.debugLog(debugLogging, TAG, "Found Shift mask.");
                    meta |= SHIFT_MASK;
                    scode &= ~SCANCODE_SHIFT_MASK;
                }
                if ((scode & SCANCODE_ALTGR_MASK) != 0) {
                    GeneralUtils.debugLog(debugLogging, TAG, "Found AltGr mask.");
                    meta |= RALT_MASK;
                    scode &= ~SCANCODE_ALTGR_MASK;
                }
                GeneralUtils.debugLog(debugLogging, TAG, "Will send scanCode: " + scode + " with meta: " + meta);
                rfb.writeKeyEvent(scode, meta, down);
                if (sendUpEvents) {
                    rfb.writeKeyEvent(scode, meta, false);
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void sendMetaKey(MetaKeyBean meta) {
        RemotePointer pointer = remoteInput.getPointer();
        int x = pointer.getX();
        int y = pointer.getY();

        if (meta.isMouseClick()) {
            GeneralUtils.debugLog(debugLogging, TAG, "event is a mouse click");
            int button = meta.getMouseButtons();
            switch (button) {
                case RemoteVncPointer.MOUSE_BUTTON_LEFT:
                    pointer.leftButtonDown(x, y, meta.getMetaFlags() | onScreenMetaState);
                    break;
                case RemoteVncPointer.MOUSE_BUTTON_RIGHT:
                    pointer.rightButtonDown(x, y, meta.getMetaFlags() | onScreenMetaState);
                    break;
                case RemoteVncPointer.MOUSE_BUTTON_MIDDLE:
                    pointer.middleButtonDown(x, y, meta.getMetaFlags() | onScreenMetaState);
                    break;
                case RemoteVncPointer.MOUSE_BUTTON_SCROLL_UP:
                    pointer.scrollUp(x, y, meta.getMetaFlags() | onScreenMetaState);
                    break;
                case RemoteVncPointer.MOUSE_BUTTON_SCROLL_DOWN:
                    pointer.scrollDown(x, y, meta.getMetaFlags() | onScreenMetaState);
                    break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            pointer.releaseButton(x, y, meta.getMetaFlags() | onScreenMetaState);

            //rfb.writePointerEvent(x, y, meta.getMetaFlags()|onScreenMetaState, button);
            //rfb.writePointerEvent(x, y, meta.getMetaFlags()|onScreenMetaState, 0);
        } else if (meta.equals(MetaKeyBean.keyCtrlAltDel)) {
            writeKeyEvent(false, KeyEvent.KEYCODE_FORWARD_DEL, RemoteKeyboard.CTRL_MASK | RemoteKeyboard.ALT_MASK, true, true);
        } else {
            sendKeySym(meta.getKeySym(), meta.getMetaFlags());
        }
    }
}
