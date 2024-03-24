/**
 * Copyright (C) 2012 Iordan Iordanov
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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

package com.undatech.opaque;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.undatech.opaque.input.RemoteKeyboardState;

import java.util.HashMap;
import java.util.Map;

public abstract class RfbConnectable implements DrawableReallocatedListener {
    private final static String TAG = "RfbConnectable";
    public RemoteKeyboardState remoteKeyboardState = null;
    protected Map<Integer, Integer> modifierMap = new HashMap<>();
    protected boolean debugLogging = false;
    protected int metaState = 0;
    protected Handler handler = null;
    public boolean serverJustCutText;

    public RfbConnectable(boolean debugLogging, Handler handler) {
        this.handler = handler;
        this.debugLogging = debugLogging;
        this.remoteKeyboardState = new RemoteKeyboardState(debugLogging);
    }

    public Handler getHandler() {
        return handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public abstract int framebufferWidth();

    public abstract int framebufferHeight();

    public abstract String desktopName();

    public abstract void requestUpdate(boolean incremental);

    public abstract void requestResolution(int x, int y) throws Exception;

    public abstract void writeClientCutText(String text);

    public abstract void setIsInNormalProtocol(boolean state);

    public abstract boolean isInNormalProtocol();

    public abstract String getEncoding();

    public abstract void writePointerEvent(int x, int y, int metaState, int pointerMask, boolean relative);

    public abstract void writeKeyEvent(int key, int metaState, boolean down);

    public abstract void writeSetPixelFormat(int bitsPerPixel, int depth, boolean bigEndian,
                                             boolean trueColour, int redMax, int greenMax, int blueMax,
                                             int redShift, int greenShift, int blueShift, boolean fGreyScale);

    public abstract void writeFramebufferUpdateRequest(int x, int y, int w, int h, boolean b);

    public abstract void close();

    public abstract boolean isCertificateAccepted();

    public abstract void setCertificateAccepted(boolean certificateAccepted);

    protected void remoteClipboardChanged(String data) {
        android.util.Log.d(TAG, "remoteClipboardChanged called.");
        // Send a message containing the text to our handler.
        Message m = new Message();
        m.setTarget(handler);
        m.what = RemoteClientLibConstants.SERVER_CUT_TEXT;
        Bundle strings = new Bundle();
        strings.putString("text", data);
        m.obj = strings;
        handler.sendMessage(m);
    }

    @Override
    public void setBitmapData(AbstractDrawableData drawable) {
        Log.d(TAG, "Stub setBitmapData called");
    }
}
