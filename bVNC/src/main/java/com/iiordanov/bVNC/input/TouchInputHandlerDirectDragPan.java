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

public class TouchInputHandlerDirectDragPan extends TouchInputHandlerGeneric {
    public static final String ID = "TOUCH_ZOOM_MODE_DRAG_PAN";
    static final String TAG = "InputHandlerDirectDragPan";

    public TouchInputHandlerDirectDragPan(RemoteCanvasActivity activity, RemoteCanvas canvas,
                                          InputCarriable remoteInput, boolean debugLogging) {
        super(activity, canvas, remoteInput, debugLogging);
    }

    /*
     * (non-Javadoc)
     * @see com.iiordanov.bVNC.input.InputHandler#getDescription()
     */
    @Override
    public String getDescription() {
        return canvas.getResources().getString(R.string.input_method_direct_drag_pan_description);
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
     * @see com.iiordanov.bVNC.input.InputHandlerGeneric#onLongPress(android.view.MotionEvent)
     */
    @Override
    public void onLongPress(MotionEvent e) {
        GeneralUtils.debugLog(debugLogging, TAG, "onLongPress, e: " + e);

        // If we've performed a right/middle-click and the gesture is not over yet, do not start drag mode.
        if (secondPointerWasDown || thirdPointerWasDown)
            return;

        endDragModesAndScrolling();
        if (canvas.canvasZoomer != null && canvas.canvasZoomer.isAbleToPan()) {
            activity.sendShortVibration();
            canvas.displayShortToastMessage(activity.getString(R.string.panning));
            panMode = true;
        } else {
            startDragAndDropMode(e);
        }
    }

    private void startDragAndDropMode(MotionEvent e) {
        GeneralUtils.debugLog(debugLogging, TAG, "startDragAndDropMode, e: " + e);
        dragMode = true;
        remoteInput.getPointer().leftButtonDown(getX(e), getY(e), e.getMetaState());
    }

    /*
     * (non-Javadoc)
     * @see android.view.GestureDetector.SimpleOnGestureListener#onScroll(android.view.MotionEvent, android.view.MotionEvent, float, float)
     */
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        GeneralUtils.debugLog(debugLogging, TAG, "onScroll, e1: " + e1 + ", e2:" + e2);

        // If we are scaling, allow panning around by moving two fingers around the screen
        if (inScaling) {
            float scale = canvas.getZoomFactor();
            activity.showActionBar();
            canvas.relativePan((int) (distanceX * scale), (int) (distanceY * scale));
        } else {
            // onScroll called while scaling/swiping gesture is in effect. We ignore the event and pretend it was
            // consumed. This prevents the mouse pointer from flailing around while we are scaling.
            // Also, if one releases one finger slightly earlier than the other when scaling, it causes Android
            // to stick a spiteful onScroll with a MASSIVE delta here.
            // This would cause the mouse pointer to jump to another place suddenly.
            // Hence, we ignore onScroll after scaling until we lift all pointers up.
            boolean twoFingers = false;
            if (e1 != null)
                twoFingers = (e1.getPointerCount() > 1);
            if (e2 != null)
                twoFingers = twoFingers || (e2.getPointerCount() > 1);

            if (twoFingers || inSwiping)
                return true;

            activity.showActionBar();

            if (!dragMode) {
                startDragAndDropMode(e1);
            } else {
                remoteInput.getPointer().moveMouseButtonDown(getX(e2), getY(e2), e2.getMetaState());
            }
        }
        canvas.movePanToMakePointerVisible();
        return true;
    }
}
