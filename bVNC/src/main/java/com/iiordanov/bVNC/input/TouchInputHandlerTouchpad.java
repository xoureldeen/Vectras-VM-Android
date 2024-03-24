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

import android.view.MotionEvent;

import com.iiordanov.bVNC.RemoteCanvas;
import com.iiordanov.bVNC.RemoteCanvasActivity;
import com.undatech.opaque.InputCarriable;
import com.undatech.opaque.util.GeneralUtils;
import com.undatech.remoteClientUi.R;

public class TouchInputHandlerTouchpad extends TouchInputHandlerGeneric {
    public static final String ID = "TOUCHPAD_MODE";
    static final String TAG = "InputHandlerTouchpad";

    public TouchInputHandlerTouchpad(RemoteCanvasActivity activity, RemoteCanvas canvas,
                                     InputCarriable remoteInput, boolean debugLogging) {
        super(activity, canvas, remoteInput, debugLogging);
    }

    /*
     * (non-Javadoc)
     * @see com.iiordanov.bVNC.input.InputHandler#getDescription()
     */
    @Override
    public String getDescription() {
        return canvas.getResources().getString(R.string.input_method_touchpad_description);
    }

    /*
     * (non-Javadoc)
     * @see com.iiordanov.bVNC.input.InputHandler#getId()
     */
    @Override
    public String getId() {
        return ID;
    }

    /*
     * (non-Javadoc)
     * @see android.view.GestureDetector.SimpleOnGestureListener#onScroll(android.view.MotionEvent, android.view.MotionEvent, float, float)
     */
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        GeneralUtils.debugLog(debugLogging, TAG, "onScroll, e1: " + e1 + ", e2:" + e2);

        final int meta = e2.getMetaState();

        // If we are scaling, allow panning around by moving two fingers around the screen
        if (inScaling) {
            float scale = canvas.getZoomFactor();
            activity.showActionBar();
            canvas.relativePan(Math.round(distanceX * scale), Math.round(distanceY * scale));
        } else {
            // TODO: This is a workaround for Android 4.2
            boolean twoFingers = false;
            if (e1 != null)
                twoFingers = (e1.getPointerCount() > 1);
            if (e2 != null)
                twoFingers = twoFingers || (e2.getPointerCount() > 1);

            // onScroll called while scaling/swiping gesture is in effect. We ignore the event and pretend it was
            // consumed. This prevents the mouse pointer from flailing around while we are scaling.
            // Also, if one releases one finger slightly earlier than the other when scaling, it causes Android
            // to stick a spiteful onScroll with a MASSIVE delta here.
            // This would cause the mouse pointer to jump to another place suddenly.
            // Hence, we ignore onScroll after scaling until we lift all pointers up.
            if (twoFingers || inSwiping) {
                return true;
            }

            activity.showActionBar();

            // If the gesture has just began, then don't allow a big delta to prevent
            // pointer jumps at the start of scrolling.
            if (!inScrolling) {
                inScrolling = true;
                distanceX = getSign(distanceX);
                distanceY = getSign(distanceY);
                distXQueue.clear();
                distYQueue.clear();
            }

            distXQueue.add(distanceX);
            distYQueue.add(distanceY);

            // Only after the first two events have arrived do we start using distanceX and Y
            // In order to effectively discard the last two events (which are typically unreliable
            // because of the finger lifting off).
            if (distXQueue.size() > 2) {
                distanceX = distXQueue.poll();
                distanceY = distYQueue.poll();
            } else {
                return true;
            }

            // Make distanceX/Y display density independent.
            float sensitivity = remoteInput.getPointer().getSensitivity();
            distanceX = sensitivity * distanceX / displayDensity;
            distanceY = sensitivity * distanceY / displayDensity;

            // Compute the absolute new mouse position.
            int newX = Math.round(remoteInput.getPointer().getX() + getDelta(-distanceX));
            int newY = Math.round(remoteInput.getPointer().getY() + getDelta(-distanceY));

            remoteInput.getPointer().moveMouse(newX, newY, meta);
        }
        canvas.movePanToMakePointerVisible();
        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.view.GestureDetector.SimpleOnGestureListener#onDown(android.view.MotionEvent)
     */
    @Override
    public boolean onDown(MotionEvent e) {
        GeneralUtils.debugLog(debugLogging, TAG, "onDown, e: " + e);
        panRepeater.stop();
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.iiordanov.bVNC.input.InputHandlerGeneric#getX(android.view.MotionEvent)
     */
    protected int getX(MotionEvent e) {
        if (dragMode || rightDragMode || middleDragMode) {
            float distanceX = e.getX() - dragX;
            dragX = e.getX();
            // Compute the absolute new X coordinate.
            return Math.round(remoteInput.getPointer().getX() + getDelta(distanceX));
        }
        dragX = e.getX();
        return remoteInput.getPointer().getX();
    }

    /*
     * (non-Javadoc)
     * @see com.iiordanov.bVNC.input.InputHandlerGeneric#getY(android.view.MotionEvent)
     */
    protected int getY(MotionEvent e) {
        if (dragMode || rightDragMode || middleDragMode) {
            float distanceY = e.getY() - dragY;
            dragY = e.getY();
            // Compute the absolute new Y coordinate.
            return Math.round(remoteInput.getPointer().getY() + getDelta(distanceY));
        }
        dragY = e.getY();
        return remoteInput.getPointer().getY();
    }

    /**
     * Computes how far the pointer will move.
     * @param distance
     * @return
     */
    private float getDelta(float distance) {
        float delta = (float) (distance * Math.cbrt(canvas.getZoomFactor()));
        return computeAcceleration(delta);
    }

    /**
     * Computes the acceleration depending on the size of the supplied delta.
     * @param delta
     * @return
     */
    private float computeAcceleration(float delta) {
        float origSign = getSign(delta);
        delta = Math.abs(delta);
        boolean accelerated = remoteInput.getPointer().isAccelerated();
        if (delta <= 15) {
            delta = delta * 0.75f;
        } else if (accelerated && delta <= 70.0f) {
            delta = delta * delta / 20.0f;
        } else if (accelerated) {
            delta = delta * 4.5f;
        }
        return origSign * delta;
    }
}
