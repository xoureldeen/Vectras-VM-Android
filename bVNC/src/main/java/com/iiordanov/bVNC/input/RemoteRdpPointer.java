package com.iiordanov.bVNC.input;

import android.content.Context;
import android.os.Handler;

import com.undatech.opaque.InputCarriable;
import com.undatech.opaque.RfbConnectable;
import com.undatech.opaque.Viewable;
import com.undatech.opaque.util.GeneralUtils;

public class RemoteRdpPointer extends RemotePointer {
    private static final String TAG = "RemoteRdpPointer";

    private final static int POINTER_FLAGS_WHEEL = 0x0200; // 512
    private final static int POINTER_FLAGS_WHEEL_NEGATIVE = 0x0100; // 256
    /*
    private final static int MOUSE_BUTTON_NONE = 0x0000;
     */
    private final static int MOUSE_BUTTON_MOVE = 0x0800; // 2048
    private final static int MOUSE_BUTTON_LEFT = 0x1000; // 4096
    private final static int MOUSE_BUTTON_RIGHT = 0x2000; // 8192

    private static final int MOUSE_BUTTON_MIDDLE = 0x4000; // 16384
    private static final int MOUSE_BUTTON_SCROLL_UP = POINTER_FLAGS_WHEEL | 0x0078;
    private static final int MOUSE_BUTTON_SCROLL_DOWN = POINTER_FLAGS_WHEEL | POINTER_FLAGS_WHEEL_NEGATIVE | 0x0088;

    public RemoteRdpPointer(
            RfbConnectable rfbConnectable, Context context, InputCarriable remoteInput,
            Viewable canvas, Handler handler, boolean debugLogging
    ) {
        super(rfbConnectable, context, remoteInput, canvas, handler, debugLogging);
    }

    private void sendButtonDownOrMoveButtonDown(int x, int y, int metaState) {
        if (prevPointerMask == pointerMask) {
            moveMouseButtonDown(x, y, metaState);
        } else {
            sendPointerEvent(x, y, metaState, false);
        }
    }

    @Override
    public void leftButtonDown(int x, int y, int metaState) {
        pointerMask = MOUSE_BUTTON_LEFT | POINTER_DOWN_MASK;
        sendButtonDownOrMoveButtonDown(x, y, metaState);
    }

    @Override
    public void middleButtonDown(int x, int y, int metaState) {
        pointerMask = MOUSE_BUTTON_MIDDLE | POINTER_DOWN_MASK;
        sendButtonDownOrMoveButtonDown(x, y, metaState);
    }

    @Override
    public void rightButtonDown(int x, int y, int metaState) {
        pointerMask = MOUSE_BUTTON_RIGHT | POINTER_DOWN_MASK;
        sendButtonDownOrMoveButtonDown(x, y, metaState);
    }

    @Override
    public void scrollUp(int x, int y, int metaState) {
        pointerMask = MOUSE_BUTTON_SCROLL_UP | POINTER_DOWN_MASK;
        sendPointerEvent(x, y, metaState, false);
    }

    @Override
    public void scrollDown(int x, int y, int metaState) {
        pointerMask = MOUSE_BUTTON_SCROLL_DOWN | POINTER_DOWN_MASK;
        sendPointerEvent(x, y, metaState, false);
    }

    @Override
    public void scrollLeft(int x, int y, int metaState) {
        // TODO: Protocol does not support scrolling left/right yet.
    }

    @Override
    public void scrollRight(int x, int y, int metaState) {
        // TODO: Protocol does not support scrolling left/right yet.
    }

    @Override
    public void moveMouse(int x, int y, int metaState) {
        pointerMask = MOUSE_BUTTON_MOVE | prevPointerMask;
        sendPointerEvent(x, y, metaState, true);
    }

    @Override
    public void moveMouseButtonDown(int x, int y, int metaState) {
        pointerMask = MOUSE_BUTTON_MOVE | POINTER_DOWN_MASK;
        sendPointerEvent(x, y, metaState, true);
    }

    @Override
    public void moveMouseButtonUp(int x, int y, int metaState) {
        pointerMask = MOUSE_BUTTON_MOVE;
        sendPointerEvent(x, y, metaState, true);
    }

    @Override
    public void releaseButton(int x, int y, int metaState) {
        pointerMask = MOUSE_BUTTON_MOVE;
        sendPointerEvent(x, y, metaState, false);
        prevPointerMask = 0;
    }

    /**
     * Sends a pointer event to the server.
     */
    private void sendPointerEvent(int x, int y, int metaState, boolean isMoving) {

        int combinedMetaState = metaState | remoteInput.getKeyboard().getMetaState();

        // Save the previous pointer mask other than action_move, so we can
        // send it with the pointer flag "not down" to clear the action.
        if (!isMoving) {
            // If this is a new mouse down event, release previous button pressed to avoid confusing the remote OS.
            if (prevPointerMask != 0 && prevPointerMask != pointerMask) {
                int upPointerMask = prevPointerMask & ~POINTER_DOWN_MASK;
                GeneralUtils.debugLog(this.debugLogging, TAG, "Sending mouse up event at: " + pointerX +
                        ", " + pointerY + " with prevPointerMask: " + prevPointerMask + ", upPointerMask: " + upPointerMask);
                protocomm.writePointerEvent(pointerX, pointerY, combinedMetaState, upPointerMask, false);
            }
            prevPointerMask = pointerMask;
        }

        canvas.invalidateMousePosition();
        pointerX = x;
        pointerY = y;

        // Do not let mouse pointer leave the bounds of the desktop.
        if (pointerX < 0) {
            pointerX = 0;
        } else if (pointerX >= canvas.getImageWidth()) {
            pointerX = canvas.getImageWidth() - 1;
        }
        if (pointerY < 0) {
            pointerY = 0;
        } else if (pointerY >= canvas.getImageHeight()) {
            pointerY = canvas.getImageHeight() - 1;
        }
        canvas.invalidateMousePosition();
        GeneralUtils.debugLog(this.debugLogging, TAG, "Sending absolute mouse event at: " + pointerX +
                ", " + pointerY + " with pointerMask: " + pointerMask);
        protocomm.writePointerEvent(pointerX, pointerY, combinedMetaState, MOUSE_BUTTON_MOVE | pointerMask, false);
        protocomm.writePointerEvent(pointerX, pointerY, combinedMetaState, pointerMask, false);
    }

}
