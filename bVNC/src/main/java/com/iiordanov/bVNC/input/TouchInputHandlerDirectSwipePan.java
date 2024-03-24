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

public class TouchInputHandlerDirectSwipePan extends TouchInputHandlerGeneric {
    public static final String ID = "TOUCH_ZOOM_MODE";
    static final String TAG = "InputHandlerDirectSwipePan";

    public TouchInputHandlerDirectSwipePan(RemoteCanvasActivity activity, RemoteCanvas canvas,
                                           InputCarriable remoteInput, boolean debugLogging) {
        super(activity, canvas, remoteInput, debugLogging);
    }

    /*
     * (non-Javadoc)
     * @see com.iiordanov.bVNC.input.InputHandler#getDescription()
     */
    @Override
    public String getDescription() {
        return canvas.getResources().getString(R.string.input_method_direct_swipe_pan_description);
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
     * @see android.view.GestureDetector.SimpleOnGestureListener#onFling(android.view.MotionEvent, android.view.MotionEvent, float, float)
     */
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        GeneralUtils.debugLog(debugLogging, TAG, "onFling, e1: " + e1 + ", e2: " + e2);

        if (consumeAsMouseWheel(e1, e2)) {
            return true;
        }

        // TODO: Workaround for Android 4.2.
        boolean twoFingers = false;
        if (e1 != null)
            twoFingers = (e1.getPointerCount() > 1);
        if (e2 != null)
            twoFingers = twoFingers || (e2.getPointerCount() > 1);

        // onFling called while scaling/swiping gesture is in effect. We ignore the event and pretend it was
        // consumed. This prevents the mouse pointer from flailing around while we are scaling.
        // Also, if one releases one finger slightly earlier than the other when scaling, it causes Android
        // to stick a spiteful onFling with a MASSIVE delta here.
        // This would cause the mouse pointer to jump to another place suddenly.
        // Hence, we ignore onFing after scaling until we lift all pointers up.
        if (twoFingers || disregardNextOnFling || inSwiping || inScaling || scalingJustFinished) {
            return true;
        }

        activity.showActionBar();
        panRepeater.start(-velocityX, -velocityY);
        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.view.GestureDetector.SimpleOnGestureListener#onScroll(android.view.MotionEvent, android.view.MotionEvent, float, float)
     */
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        GeneralUtils.debugLog(debugLogging, TAG, "onScroll, e1: " + e1 + ", e2: " + e2);

        if (consumeAsMouseWheel(e1, e2)) {
            return true;
        }

        if (!inScaling) {
            // onScroll called while swiping gesture is in effect. We ignore the event and pretend it was
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
        }

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

        float scale = canvas.getZoomFactor();
        activity.showActionBar();
        canvas.relativePan((int) (distanceX * scale), (int) (distanceY * scale));
        return true;
    }
}

