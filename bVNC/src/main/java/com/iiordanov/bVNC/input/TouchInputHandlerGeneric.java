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

import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.core.view.InputDeviceCompat;

import com.iiordanov.bVNC.Constants;
import com.iiordanov.bVNC.RemoteCanvas;
import com.iiordanov.bVNC.RemoteCanvasActivity;
import com.undatech.opaque.InputCarriable;
import com.undatech.opaque.util.GeneralUtils;

import java.util.LinkedList;
import java.util.Queue;

abstract class TouchInputHandlerGeneric extends GestureDetector.SimpleOnGestureListener
        implements TouchInputHandler, ScaleGestureDetector.OnScaleGestureListener {
    private static final String TAG = "InputHandlerGeneric";
    protected final boolean debugLogging;
    final int maxSwipeSpeed = 7;
    // If swipe events are registered once every baseSwipeTime miliseconds, then
    // swipeSpeed will be one. If more often, swipe-speed goes up, if less, down.
    final long baseSwipeTime = 400;
    // The minimum distance a scale event has to traverse the FIRST time before scaling starts.
    final double minScaleFactor = 0.1;
    protected GestureDetector gestureDetector;
    protected MyScaleGestureDetector scalingGestureDetector;
    // Handles to the RemoteCanvas view and RemoteCanvasActivity activity.
    protected RemoteCanvas canvas;
    protected RemoteCanvasActivity activity;
    protected PanRepeater panRepeater;
    // Various drag modes in which we don't detect gestures.
    protected boolean panMode = false;
    protected boolean dragMode = false;
    protected boolean rightDragMode = false;
    protected boolean middleDragMode = false;
    protected float dragX, dragY;
    protected boolean singleHandedGesture = false;
    protected boolean singleHandedJustEnded = false;
    // These variables keep track of which pointers have seen ACTION_DOWN events.
    protected boolean secondPointerWasDown = false;
    protected boolean thirdPointerWasDown = false;
    protected InputCarriable remoteInput;
    // This is the initial "focal point" of the gesture (between the two fingers).
    float xInitialFocus;
    float yInitialFocus;
    // This is the final "focal point" of the gesture (between the two fingers).
    float xCurrentFocus;
    float yCurrentFocus;
    float xPreviousFocus;
    float yPreviousFocus;
    // These variables record whether there was a two-finger swipe performed up or down.
    boolean inSwiping = false;
    boolean scrollUp = false;
    boolean scrollDown = false;
    boolean scrollLeft = false;
    boolean scrollRight = false;
    // These variables indicate whether the dpad should be used as arrow keys
    // and whether it should be rotated.
    boolean useDpadAsArrows = false;
    boolean rotateDpad = false;
    // The variables which indicates how many scroll events to send per swipe
    // event and the maximum number to send at one time.
    long swipeSpeed = 1;
    // This is how far the swipe has to travel before a swipe event is generated.
    float startSwipeDist = 15.f;
    float baseSwipeDist = 10.f;
    // This is how far from the top and bottom edge to detect immersive swipe.
    float immersiveSwipeDistance = 10.f;
    boolean immersiveSwipe = false;
    // Some variables indicating what kind of a gesture we're currently in or just finished.
    boolean inScrolling = false;
    boolean inScaling = false;
    boolean scalingJustFinished = false;
    // What action was previously performed by a mouse or stylus.
    int prevMouseOrStylusAction = 0;
    // What the display density is.
    float displayDensity = 0;
    // Indicates that the next onFling will be disregarded.
    boolean disregardNextOnFling = false;
    // Queue which holds the last two MotionEvents which triggered onScroll
    Queue<Float> distXQueue;
    Queue<Float> distYQueue;

    TouchInputHandlerGeneric(RemoteCanvasActivity activity, RemoteCanvas canvas, InputCarriable remoteInput,
                             boolean debugLogging) {
        this.activity = activity;
        this.canvas = canvas;
        this.remoteInput = remoteInput;
        this.debugLogging = debugLogging;

        // TODO: Implement this
        useDpadAsArrows = true; //activity.getUseDpadAsArrows();
        rotateDpad = false; //activity.getRotateDpad();

        gestureDetector = new GestureDetector(activity, this);
        scalingGestureDetector = new MyScaleGestureDetector(activity, this);

        gestureDetector.setOnDoubleTapListener(this);

        this.panRepeater = new PanRepeater(canvas, canvas.handler);

        displayDensity = canvas.getDisplayDensity();

        distXQueue = new LinkedList<Float>();
        distYQueue = new LinkedList<Float>();

        baseSwipeDist = baseSwipeDist * displayDensity;
        startSwipeDist = startSwipeDist * displayDensity;
        immersiveSwipeDistance = immersiveSwipeDistance * displayDensity;
        GeneralUtils.debugLog(debugLogging, TAG, "displayDensity, baseSwipeDist, immersiveSwipeDistance: "
                + displayDensity + " " + baseSwipeDist + " " + immersiveSwipeDistance);
    }

    /**
     * Function to get appropriate X coordinate from motion event for this input handler.
     * @return the appropriate X coordinate.
     */
    protected int getX(MotionEvent e) {
        float scale = canvas.getZoomFactor();
        return (int) (canvas.getAbsX() + e.getX() / scale);
    }

    /**
     * Function to get appropriate Y coordinate from motion event for this input handler.
     * @return the appropriate Y coordinate.
     */
    protected int getY(MotionEvent e) {
        float scale = canvas.getZoomFactor();
        return (int) (canvas.getAbsY() + (e.getY() - 1.f * canvas.getTop()) / scale);
    }

    /**
     * Handles actions performed by a mouse-like device.
     * @param e touch or generic motion event
     * @return
     */
    protected boolean handleMouseActions(MotionEvent e) {
        boolean used = false;
        final int action = e.getActionMasked();
        final int meta = e.getMetaState();
        final int bstate = e.getButtonState();
        float scale = canvas.getZoomFactor();
        int x = (int) (canvas.getAbsX() + e.getX() / scale);
        int y = (int) (canvas.getAbsY() + (e.getY() - 1.f * canvas.getTop()) / scale);

        switch (action) {
            // If a mouse button was pressed or mouse was moved.
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                switch (bstate) {
                    case MotionEvent.BUTTON_PRIMARY:
                        canvas.movePanToMakePointerVisible();
                        remoteInput.getPointer().leftButtonDown(x, y, meta);
                        used = true;
                        break;
                    case MotionEvent.BUTTON_SECONDARY:
                    case MotionEvent.BUTTON_STYLUS_PRIMARY:
                        canvas.movePanToMakePointerVisible();
                        remoteInput.getPointer().rightButtonDown(x, y, meta);
                        used = true;
                        break;
                    case MotionEvent.BUTTON_TERTIARY:
                    case MotionEvent.BUTTON_STYLUS_SECONDARY:
                        canvas.movePanToMakePointerVisible();
                        remoteInput.getPointer().middleButtonDown(x, y, meta);
                        used = true;
                        break;
                }
                break;
            // If a mouse button was released.
            case MotionEvent.ACTION_UP:
                switch (bstate) {
                    case 0:
                        if (e.getToolType(0) != MotionEvent.TOOL_TYPE_MOUSE) {
                            break;
                        }
                    case MotionEvent.BUTTON_PRIMARY:
                    case MotionEvent.BUTTON_SECONDARY:
                    case MotionEvent.BUTTON_TERTIARY:
                    case MotionEvent.BUTTON_STYLUS_PRIMARY:
                    case MotionEvent.BUTTON_STYLUS_SECONDARY:
                        canvas.movePanToMakePointerVisible();
                        remoteInput.getPointer().releaseButton(x, y, meta);
                        used = true;
                        break;
                }
                break;
            // If the mouse wheel was scrolled.
            case MotionEvent.ACTION_SCROLL:
                float vscroll = e.getAxisValue(MotionEvent.AXIS_VSCROLL);
                float hscroll = e.getAxisValue(MotionEvent.AXIS_HSCROLL);
                scrollDown = false;
                scrollUp = false;
                scrollRight = false;
                scrollLeft = false;
                // Determine direction and speed of scrolling.
                if (vscroll < 0) {
                    swipeSpeed = (int) (-1 * vscroll);
                    scrollDown = true;
                } else if (vscroll > 0) {
                    swipeSpeed = (int) vscroll;
                    scrollUp = true;
                } else if (hscroll < 0) {
                    swipeSpeed = (int) (-1 * hscroll);
                    scrollRight = true;
                } else if (hscroll > 0) {
                    swipeSpeed = (int) hscroll;
                    scrollLeft = true;
                } else
                    break;

                sendScrollEvents(x, y, meta);
                used = true;
                break;
            // If the mouse was moved OR as reported, some external mice trigger this when a
            // mouse button is pressed as well, so we check bstate here too.
            case MotionEvent.ACTION_HOVER_MOVE:
                activity.showActionBar();
                canvas.movePanToMakePointerVisible();
                switch (bstate) {
                    case MotionEvent.BUTTON_PRIMARY:
                        remoteInput.getPointer().leftButtonDown(x, y, meta);
                        break;
                    case MotionEvent.BUTTON_SECONDARY:
                    case MotionEvent.BUTTON_STYLUS_PRIMARY:
                        remoteInput.getPointer().rightButtonDown(x, y, meta);
                        break;
                    case MotionEvent.BUTTON_TERTIARY:
                    case MotionEvent.BUTTON_STYLUS_SECONDARY:
                        remoteInput.getPointer().middleButtonDown(x, y, meta);
                        break;
                    default:
                        remoteInput.getPointer().moveMouseButtonUp(x, y, meta);
                        break;
                }
                used = true;
        }

        prevMouseOrStylusAction = action;
        return used;
    }

    /**
     * Sends scroll events with previously set direction and speed.
     * @param x
     * @param y
     * @param meta
     */
    protected void sendScrollEvents(int x, int y, int meta) {
        GeneralUtils.debugLog(debugLogging, TAG, "sendScrollEvents");

        int numEvents = 0;
        while (numEvents < swipeSpeed && numEvents < maxSwipeSpeed) {
            if (scrollDown) {
                remoteInput.getPointer().scrollDown(x, y, meta);
                remoteInput.getPointer().moveMouseButtonUp(x, y, meta);
            } else if (scrollUp) {
                remoteInput.getPointer().scrollUp(x, y, meta);
                remoteInput.getPointer().moveMouseButtonUp(x, y, meta);
            }
            if (scrollRight) {
                remoteInput.getPointer().scrollRight(x, y, meta);
                remoteInput.getPointer().moveMouseButtonUp(x, y, meta);
            } else if (scrollLeft) {
                remoteInput.getPointer().scrollLeft(x, y, meta);
                remoteInput.getPointer().moveMouseButtonUp(x, y, meta);
            }
            numEvents++;
        }
        remoteInput.getPointer().releaseButton(x, y, meta);
    }

    /*
     * @see android.view.GestureDetector.SimpleOnGestureListener#onSingleTapConfirmed(android.view.MotionEvent)
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        GeneralUtils.debugLog(debugLogging, TAG, "onSingleTapConfirmed, e: " + e);

        int metaState = e.getMetaState();
        activity.showActionBar();
        remoteInput.getPointer().leftButtonDown(getX(e), getY(e), metaState);
        SystemClock.sleep(50);
        remoteInput.getPointer().releaseButton(getX(e), getY(e), metaState);
        canvas.movePanToMakePointerVisible();
        return true;
    }

    /*
     * @see android.view.GestureDetector.SimpleOnGestureListener#onDoubleTap(android.view.MotionEvent)
     */
    @Override
    public boolean onDoubleTap(MotionEvent e) {
        GeneralUtils.debugLog(debugLogging, TAG, "onDoubleTap, e: " + e);

        int metaState = e.getMetaState();
        remoteInput.getPointer().leftButtonDown(getX(e), getY(e), metaState);
        SystemClock.sleep(50);
        remoteInput.getPointer().releaseButton(getX(e), getY(e), metaState);
        SystemClock.sleep(50);
        remoteInput.getPointer().leftButtonDown(getX(e), getY(e), metaState);
        SystemClock.sleep(50);
        remoteInput.getPointer().releaseButton(getX(e), getY(e), metaState);
        canvas.movePanToMakePointerVisible();
        return true;
    }

    /*
     * @see android.view.GestureDetector.SimpleOnGestureListener#onLongPress(android.view.MotionEvent)
     */
    @Override
    public void onLongPress(MotionEvent e) {
        GeneralUtils.debugLog(debugLogging, TAG, "onLongPress, e: " + e);

        int metaState = e.getMetaState();

        if (secondPointerWasDown || thirdPointerWasDown) {
            GeneralUtils.debugLog(debugLogging, TAG,
                    "onLongPress: right/middle-click gesture in progress, not starting drag mode");
            return;
        }

        activity.sendShortVibration();

        dragMode = true;
        remoteInput.getPointer().leftButtonDown(getX(e), getY(e), metaState);
    }

    /**
     * Indicates that drag modes and scrolling have ended.
     * @return whether any mode other than the drag modes was enabled
     */
    protected boolean endDragModesAndScrolling() {
        GeneralUtils.debugLog(debugLogging, TAG, "endDragModesAndScrolling");
        boolean nonDragGesture = panMode || inScaling || inSwiping || inScrolling || immersiveSwipe;
        canvas.cursorBeingMoved = false;
        panMode = false;
        inScaling = false;
        inSwiping = false;
        inScrolling = false;
        immersiveSwipe = false;
        if (dragMode || rightDragMode || middleDragMode) {
            nonDragGesture = false;
            dragMode = false;
            rightDragMode = false;
            middleDragMode = false;
        }
        return nonDragGesture;
    }

    /**
     * Modify the event so that the mouse goes where we specify.
     * @param e event to be modified.
     * @param x new x coordinate.
     * @param y new y coordinate.
     */
    protected void setEventCoordinates(MotionEvent e, float x, float y) {
        GeneralUtils.debugLog(debugLogging, TAG, "setEventCoordinates");
        e.setLocation(x, y);
    }

    private void detectImmersiveSwipe(float y) {
        GeneralUtils.debugLog(debugLogging, TAG, "detectImmersiveSwipe");
        if (Constants.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT &&
                (y <= immersiveSwipeDistance || canvas.getHeight() - y <= immersiveSwipeDistance)) {
            inSwiping = true;
            immersiveSwipe = true;
        } else if (!singleHandedGesture) {
            inSwiping = false;
            immersiveSwipe = false;
        }
    }

    /*
     * @see com.iiordanov.bVNC.input.InputHandler#0yonTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        GeneralUtils.debugLog(debugLogging, TAG, "onTouchEvent, e: " + e);

        final int action = e.getActionMasked();
        final int index = e.getActionIndex();
        final int pointerID = e.getPointerId(index);
        final int meta = e.getMetaState();

        float f = e.getPressure();
        if (f > 2.f)
            f = f / 50.f;
        if (f > .92f) {
            disregardNextOnFling = true;
        }

        if (android.os.Build.VERSION.SDK_INT >= 14) {
            // Handle and consume actions performed by a (e.g. USB or bluetooth) mouse.
            if (handleMouseActions(e))
                return true;
        }

        if (action == MotionEvent.ACTION_UP) {
            canvas.invalidate();
        }

        GeneralUtils.debugLog(debugLogging, TAG, "onTouchEvent: pointerID: " + pointerID);
        switch (pointerID) {
            case 0:
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        disregardNextOnFling = false;
                        singleHandedJustEnded = false;
                        // We have put down first pointer on the screen, so we can reset the state of all click-state variables.
                        // Permit sending mouse-down event on long-tap again.
                        secondPointerWasDown = false;
                        // Permit right-clicking again.
                        thirdPointerWasDown = false;
                        // Cancel any effect of scaling having "just finished" (e.g. ignoring scrolling).
                        scalingJustFinished = false;
                        // Cancel drag modes and scrolling.
                        if (!singleHandedGesture)
                            endDragModesAndScrolling();
                        canvas.cursorBeingMoved = true;
                        // Indicate where we start dragging from.
                        dragX = e.getX();
                        dragY = e.getY();

                        // Detect whether this is potentially the start of a gesture to show the nav bar.
                        detectImmersiveSwipe(dragY);
                        break;
                    case MotionEvent.ACTION_UP:
                        singleHandedGesture = false;
                        singleHandedJustEnded = true;

                        // If this is the end of a swipe that showed the nav bar, consume.
                        if (immersiveSwipe && Math.abs(dragY - e.getY()) > immersiveSwipeDistance) {
                            endDragModesAndScrolling();
                            return true;
                        }

                        if (!endDragModesAndScrolling()) {
                            // If no non-drag gestures were going on, send a mouse up event.
                            GeneralUtils.debugLog(debugLogging, TAG,
                                    "onTouchEvent: No non-drag gestures detected, sending mouse up event");
                            remoteInput.getPointer().releaseButton(getX(e), getY(e), meta);
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        GeneralUtils.debugLog(debugLogging, TAG, "onTouchEvent: ACTION_MOVE");
                        // Send scroll up/down events if swiping is happening.
                        if (panMode) {
                            float scale = canvas.getZoomFactor();
                            canvas.relativePan(-(int) ((e.getX() - dragX) * scale), -(int) ((e.getY() - dragY) * scale));
                            dragX = e.getX();
                            dragY = e.getY();
                            GeneralUtils.debugLog(debugLogging, TAG, "onTouchEvent: ACTION_MOVE panMode");
                            return true;
                        } else if (dragMode || rightDragMode || middleDragMode) {
                            canvas.movePanToMakePointerVisible();
                            remoteInput.getPointer().moveMouseButtonDown(getX(e), getY(e), meta);
                            GeneralUtils.debugLog(debugLogging, TAG, "onTouchEvent: ACTION_MOVE in a drag mode, moving mouse with button down");
                            return true;
                        } else if (inSwiping) {
                            // Save the coordinates and restore them afterward.
                            float x = e.getX();
                            float y = e.getY();
                            // Set the coordinates to where the swipe began (i.e. where scaling started).
                            setEventCoordinates(e, xInitialFocus, yInitialFocus);
                            sendScrollEvents(getX(e), getY(e), meta);
                            // Restore the coordinates so that onScale doesn't get all muddled up.
                            setEventCoordinates(e, x, y);
                            GeneralUtils.debugLog(debugLogging, TAG, "onTouchEvent: ACTION_MOVE inSwiping, saving coordinates");
                        } else if (immersiveSwipe) {
                            // If this is part of swipe that shows the nav bar, consume.
                            GeneralUtils.debugLog(debugLogging, TAG, "onTouchEvent: ACTION_MOVE Gesture showing nav bar, no action");
                            return true;
                        }
                }
                break;
            case 1:
                switch (action) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        // We re-calculate the initial focal point to be between the 1st and 2nd pointer index.
                        xInitialFocus = 0.5f * (dragX + e.getX(pointerID));
                        yInitialFocus = 0.5f * (dragY + e.getY(pointerID));
                        // Here we only prepare for the second click, which we perform on ACTION_POINTER_UP for pointerID==1.
                        endDragModesAndScrolling();
                        // Permit sending mouse-down event on long-tap again.
                        secondPointerWasDown = true;
                        // Permit right-clicking again.
                        thirdPointerWasDown = false;
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        if (!inSwiping && !inScaling && !thirdPointerWasDown) {
                            // If user taps with a second finger while first finger is down, then we treat this as
                            // a right mouse click, but we only effect the click when the second pointer goes up.
                            // If the user taps with a second and third finger while the first
                            // finger is down, we treat it as a middle mouse click. We ignore the lifting of the
                            // second index when the third index has gone down (using the thirdPointerWasDown variable)
                            // to prevent inadvertent right-clicks when a middle click has been performed.
                            remoteInput.getPointer().rightButtonDown(getX(e), getY(e), meta);
                            // Enter right-drag mode.
                            rightDragMode = true;
                            // Now the event must be passed on to the parent class in order to
                            // end scaling as it was certainly started when the second pointer went down.
                        }
                        break;
                }
                break;

            case 2:
                switch (action) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        if (!inScaling) {
                            // This boolean prevents the right-click from firing simultaneously as a middle button click.
                            thirdPointerWasDown = true;
                            remoteInput.getPointer().middleButtonDown(getX(e), getY(e), meta);
                            // Enter middle-drag mode.
                            middleDragMode = true;
                        }
                }
                break;
        }

        scalingGestureDetector.onTouchEvent(e);
        return gestureDetector.onTouchEvent(e);
    }

    /*
     * @see android.view.ScaleGestureDetector.OnScaleGestureListener#onScale(android.view.ScaleGestureDetector)
     */
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        GeneralUtils.debugLog(debugLogging, TAG, "onScale");
        boolean eventConsumed = true;

        // Get the current focus.
        xCurrentFocus = detector.getFocusX();
        yCurrentFocus = detector.getFocusY();

        // If we haven't started scaling yet, we check whether a swipe is being performed.
        // The arbitrary fudge factor may not be the best way to set a tolerance...
        if (!inScaling) {
            // Start swiping mode only after we've moved away from the initial focal point some distance.
            if (!inSwiping) {
                if ((yCurrentFocus < (yInitialFocus - startSwipeDist)) ||
                        (yCurrentFocus > (yInitialFocus + startSwipeDist)) ||
                        (xCurrentFocus < (xInitialFocus - startSwipeDist)) ||
                        (xCurrentFocus > (xInitialFocus + startSwipeDist))) {
                    inSwiping = true;
                    xPreviousFocus = xCurrentFocus;
                    yPreviousFocus = yCurrentFocus;
                }
            }

            // If in swiping mode, indicate a swipe at regular intervals.
            if (inSwiping) {
                scrollDown = false;
                scrollUp = false;
                scrollRight = false;
                scrollLeft = false;
                if (yCurrentFocus < (yPreviousFocus - baseSwipeDist)) {
                    scrollDown = true;
                    xPreviousFocus = xCurrentFocus;
                    yPreviousFocus = yCurrentFocus;
                } else if (yCurrentFocus > (yPreviousFocus + baseSwipeDist)) {
                    scrollUp = true;
                    xPreviousFocus = xCurrentFocus;
                    yPreviousFocus = yCurrentFocus;
                } else if (xCurrentFocus < (xPreviousFocus - baseSwipeDist)) {
                    scrollRight = true;
                    xPreviousFocus = xCurrentFocus;
                    yPreviousFocus = yCurrentFocus;
                } else if (xCurrentFocus > (xPreviousFocus + baseSwipeDist)) {
                    scrollLeft = true;
                    xPreviousFocus = xCurrentFocus;
                    yPreviousFocus = yCurrentFocus;
                } else {
                    eventConsumed = false;
                }
                // The faster we swipe, the faster we traverse the screen, and hence, the
                // smaller the time-delta between consumed events. We take the reciprocal
                // obtain swipeSpeed. If it goes to zero, we set it to at least one.
                long elapsedTime = detector.getTimeDelta();
                if (elapsedTime < 10) elapsedTime = 10;

                swipeSpeed = baseSwipeTime / elapsedTime;
                if (swipeSpeed == 0) swipeSpeed = 1;
                GeneralUtils.debugLog(debugLogging, TAG, "Current swipe speed: " + swipeSpeed);
            }
        }

        if (!inSwiping) {
            if (!inScaling && Math.abs(1.0 - detector.getScaleFactor()) < minScaleFactor) {
                GeneralUtils.debugLog(debugLogging, TAG, "Not scaling due to small scale factor");
                eventConsumed = false;
            }

            if (eventConsumed && canvas != null && canvas.canvasZoomer != null) {
                if (inScaling == false) {
                    inScaling = true;
                }
                GeneralUtils.debugLog(debugLogging, TAG, "Changing zoom level: " + detector.getScaleFactor());
                canvas.canvasZoomer.changeZoom(activity, detector.getScaleFactor(), xCurrentFocus, yCurrentFocus);
            }
        }
        return eventConsumed;
    }

    /*
     * @see android.view.ScaleGestureDetector.OnScaleGestureListener#onScaleBegin(android.view.ScaleGestureDetector)
     */
    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        GeneralUtils.debugLog(debugLogging, TAG, "onScaleBegin (" + xInitialFocus + "," + yInitialFocus + ")");
        inScaling = false;
        scalingJustFinished = false;
        // Cancel any swipes that may have been registered last time.
        inSwiping = false;
        scrollDown = false;
        scrollUp = false;
        scrollRight = false;
        scrollLeft = false;
        return true;
    }

    /*
     * @see android.view.ScaleGestureDetector.OnScaleGestureListener#onScaleEnd(android.view.ScaleGestureDetector)
     */
    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        GeneralUtils.debugLog(debugLogging, TAG, "onScaleEnd");
        inScaling = false;
        inSwiping = false;
        scalingJustFinished = true;
    }

    /**
     * Returns the sign of the given number.
     * @param number
     * @return
     */
    protected float getSign(float number) {
        float sign;
        if (number >= 0) {
            sign = 1.f;
        } else {
            sign = -1.f;
        }
        return sign;
    }

    boolean consumeAsMouseWheel(MotionEvent e1, MotionEvent e2) {
        boolean useEvent = false;
        if (!canvas.isAbleToPan()) {
            GeneralUtils.debugLog(debugLogging, TAG, "consumeAsMouseWheel, fit-to-screen");
            useEvent = true;
        }

        if (e1.getSource() == InputDeviceCompat.SOURCE_MOUSE ||
                e1.getSource() == InputDeviceCompat.SOURCE_CLASS_POINTER ||
                e1.getSource() == InputDeviceCompat.SOURCE_CLASS_TRACKBALL ||
                e1.getSource() == InputDeviceCompat.SOURCE_TOUCHPAD ||
                e1.getSource() == InputDeviceCompat.SOURCE_DPAD
        ) {
            GeneralUtils.debugLog(debugLogging, TAG, "consumeAsMouseWheel, mouse-like source");
            useEvent = true;
        }

        if (!useEvent) {
            return false;
        }

        int meta = e1.getMetaState();
        scrollUp = false;
        scrollDown = false;
        scrollLeft = false;
        scrollRight = false;
        swipeSpeed = 1;

        if (e1.getX() < e2.getX()) {
            scrollLeft = true;
        } else if (e1.getX() > e2.getX()) {
            scrollRight = true;
        }

        if (e1.getY() < e2.getY()) {
            scrollUp = true;
        } else if (e1.getY() > e2.getY()) {
            scrollDown = true;
        }
        sendScrollEvents(getX(e1), getY(e1), meta);
        return true;
    }
}
