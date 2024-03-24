/**
 * Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 2009 Michael A. MacDonald
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

package com.iiordanov.bVNC;

import android.graphics.Matrix;
import android.widget.ImageView.ScaleType;

import com.undatech.remoteClientUi.R;

/**
 * @author Michael A. MacDonald
 */
class ZoomScaling extends AbstractScaling {

    static final String TAG = "ZoomScaling";
    int canvasXOffset;
    int canvasYOffset;
    float scaling;
    float minimumScale;
    private Matrix matrix;

    public ZoomScaling() {
        super(R.id.itemZoomable, ScaleType.MATRIX);
        matrix = new Matrix();
        scaling = 1;
    }

    /* (non-Javadoc)
     * @see com.iiordanov.bVNC.AbstractScaling#getDefaultHandlerId()
     */
    @Override
    int getDefaultHandlerId() {
        return R.id.itemInputTouchpad;
    }

    /* (non-Javadoc)
     * @see com.iiordanov.bVNC.AbstractScaling#isAbleToPan()
     */
    @Override
    public boolean isAbleToPan() {
        return true;
    }

    /* (non-Javadoc)
     * @see com.iiordanov.bVNC.AbstractScaling#isValidInputMode(int)
     */
    @Override
    boolean isValidInputMode(int mode) {
//        return mode == R.id.itemInputTouchPanZoomMouse;
        return true;
    }

    /**
     * Call after scaling and matrix have been changed to resolve scrolling
     * @param canvas
     */
    private void resolveZoom(RemoteCanvas canvas) {
        resetMatrix();
        matrix.postScale(scaling, scaling);
        canvas.setImageMatrix(matrix);
        canvas.resetScroll();
        canvas.relativePan(0, 0);
    }

    /* (non-Javadoc)
     * @see com.iiordanov.bVNC.AbstractScaling#zoomIn(com.iiordanov.bVNC.RemoteCanvasActivity)
     */
    @Override
    void zoomIn(RemoteCanvasActivity activity) {
        resetMatrix();
        standardizeScaling();
        scaling += 0.25;
        if (scaling > 4.0f) {
            scaling = 4.0f;
        }
        matrix.postScale(scaling, scaling);
        //Log.v(TAG,String.format("before set matrix scrollx = %d scrolly = %d", activity.vncCanvas.getScrollX(), activity.vncCanvas.getScrollY()));
        activity.getCanvas().setImageMatrix(matrix);
        resolveZoom(activity.getCanvas());
    }

    /* (non-Javadoc)
     * @see com.iiordanov.bVNC.AbstractScaling#getScale()
     */
    @Override
    public float getZoomFactor() {
        return scaling;
    }

    /* (non-Javadoc)
     * @see com.iiordanov.bVNC.AbstractScaling#zoomOut(com.iiordanov.bVNC.RemoteCanvasActivity)
     */
    @Override
    void zoomOut(RemoteCanvasActivity activity) {
        resetMatrix();
        standardizeScaling();
        scaling -= 0.25;
        if (scaling < minimumScale) {
            scaling = minimumScale;
        }
        matrix.postScale(scaling, scaling);
        //Log.v(TAG,String.format("before set matrix scrollx = %d scrolly = %d", activity.vncCanvas.getScrollX(), activity.vncCanvas.getScrollY()));
        activity.getCanvas().setImageMatrix(matrix);
        //Log.v(TAG,String.format("after set matrix scrollx = %d scrolly = %d", activity.vncCanvas.getScrollX(), activity.vncCanvas.getScrollY()));
        resolveZoom(activity.getCanvas());
    }

    /* (non-Javadoc)
     * @see com.iiordanov.bVNC.AbstractScaling#adjust(com.iiordanov.bVNC.RemoteCanvasActivity, float, float, float)
     */
    @Override
    public void changeZoom(RemoteCanvasActivity activity, float scaleFactor, float fx, float fy) {

        float oldScale;
        float newScale = scaleFactor * scaling;
        if (scaleFactor < 1) {
            if (newScale < minimumScale) {
                newScale = minimumScale;
            }
        } else {
            if (newScale > 4) {
                newScale = 4;
            }
        }

        RemoteCanvas canvas = activity.getCanvas();
        // ax is the absolute x of the focus
        int xPan = canvas.absoluteXPosition;
        float ax = (fx / scaling) + xPan;
        float newXPan = (scaling * xPan - scaling * ax + newScale * ax) / newScale;
        int yPan = canvas.absoluteYPosition;
        float ay = (fy / scaling) + yPan;
        float newYPan = (scaling * yPan - scaling * ay + newScale * ay) / newScale;

        // Here we do snapping to 1:1. If we are approaching scale = 1, we snap to it.
        oldScale = scaling;
        if ((newScale > 0.90f && newScale < 1.00f) ||
                (newScale > 1.00f && newScale < 1.10f)) {
            newScale = 1.f;
            // Only if oldScale is outside the snap region, do we inform the user.
            if (oldScale < 0.90f || oldScale > 1.10f)
                canvas.displayShortToastMessage(R.string.snap_one_to_one);
        }

        resetMatrix();
        scaling = newScale;
        matrix.postScale(scaling, scaling);
        canvas.setImageMatrix(matrix);
        resolveZoom(canvas);

        // Only if we have actually scaled do we pan and potentially set mouse position.
        if (oldScale != newScale) {
            canvas.relativePan((int) (newXPan - xPan), (int) (newYPan - yPan));
        }
    }

    private void resetMatrix() {
        matrix.reset();
        matrix.preTranslate(canvasXOffset, canvasYOffset);
    }

    /**
     *  Set scaling to one of the clicks on the zoom scale
     */
    private void standardizeScaling() {
        scaling = ((float) ((int) (scaling * 4))) / 4;
    }

    /* (non-Javadoc)
     * @see com.iiordanov.bVNC.AbstractScaling#setScaleTypeForActivity(com.iiordanov.bVNC.RemoteCanvasActivity)
     */
    @Override
    void setScaleTypeForActivity(RemoteCanvasActivity activity) {
        super.setScaleTypeForActivity(activity);
        RemoteCanvas canvas = activity.getCanvas();
        if (canvas == null || canvas.myDrawable == null)
            return;
        canvasXOffset = -canvas.getCenteredXOffset();
        canvasYOffset = -canvas.getCenteredYOffset();
        canvas.computeShiftFromFullToView();
        minimumScale = canvas.getMinimumScale();
        scaling = minimumScale;
        resolveZoom(canvas);
    }

}
