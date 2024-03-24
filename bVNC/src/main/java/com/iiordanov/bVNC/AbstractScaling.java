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

import android.widget.ImageView;

import com.undatech.remoteClientUi.R;

/**
 * @author Michael A. MacDonald
 *
 * A scaling mode for the VncCanvas; based on ImageView.ScaleType
 */
public abstract class AbstractScaling {
    private static final int scaleModeIds[] = {R.id.itemFitToScreen, R.id.itemOneToOne, R.id.itemZoomable};

    private static AbstractScaling[] scalings;
    protected ImageView.ScaleType scaleType;
    private int id;

    protected AbstractScaling(int id, ImageView.ScaleType scaleType) {
        this.id = id;
        this.scaleType = scaleType;
    }

    static AbstractScaling getById(int id) {
        if (scalings == null) {
            scalings = new AbstractScaling[scaleModeIds.length];
        }
        for (int i = 0; i < scaleModeIds.length; ++i) {
            if (scaleModeIds[i] == id) {
                if (scalings[i] == null) {
                    if (id == R.id.itemFitToScreen) {
                        scalings[i] = new FitToScreenScaling();
                    } else if (id == R.id.itemOneToOne) {
                        scalings[i] = new OneToOneScaling();
                    } else if (id == R.id.itemZoomable) {
                        scalings[i] = new ZoomScaling();
                    }
                }
                return scalings[i];
            }
        }
        throw new IllegalArgumentException("Unknown scaling id " + id);
    }

    static AbstractScaling getByScaleType(ImageView.ScaleType scaleType) {
        for (int i : scaleModeIds) {
            AbstractScaling s = getById(i);
            if (s.scaleType == scaleType)
                return s;
        }
        throw new IllegalArgumentException("Unsupported scale type: " + scaleType.toString());
    }

    /**
     * Returns the scale factor of this scaling mode.
     * @return
     */
    public float getZoomFactor() {
        return 1.f;
    }

    void zoomIn(RemoteCanvasActivity activity) {
    }

    void zoomOut(RemoteCanvasActivity activity) {
    }

    /**
     *
     * @return Id corresponding to menu item that sets this scale type
     */
    int getId() {
        return id;
    }

    /**
     * Sets the activity's scale type to the scaling
     * @param activity
     */
    void setScaleTypeForActivity(RemoteCanvasActivity activity) {
        RemoteCanvas canvas = activity.getCanvas();
        canvas.canvasZoomer = this;
        // This is a bit of a hack because Scaletype.FIT_CENTER is now obsolete, since fit-to-screen scaling is now
        // essentially zoom-scaling with minimumScale and zoom disabled. However, we still use
        // it to identify Fit-to-screen scale mode. Instead of setting scaleType here, we hard-code MATRIX.
        canvas.setScaleType(ImageView.ScaleType.MATRIX);
        activity.getConnection().setScaleMode(scaleType);
        if (activity.touchInputHandler == null || !isValidInputMode(activity.getModeIdFromHandler(activity.touchInputHandler))) {
            activity.touchInputHandler = activity.getInputHandlerById(getDefaultHandlerId());
            activity.getConnection().setInputMode(activity.touchInputHandler.getId());
        }
        activity.getConnection().save(activity);
        activity.updateInputMenu();
    }

    abstract int getDefaultHandlerId();

    /**
     * True if this scale type allows panning of the image
     * @return
     */
    public abstract boolean isAbleToPan();

    /**
     * True if the listed input mode is valid for this scaling mode
     * @param mode Id of the input mode
     * @return True if the input mode is compatible with the scaling mode
     */
    abstract boolean isValidInputMode(int mode);

    /**
     * Change the scaling and focus dynamically, as from a detected scale gesture
     * @param activity Activity containing to canvas to scale
     * @param scaleFactor Factor by which to adjust scaling
     * @param fx Focus X of center of scale change
     * @param fy Focus Y of center of scale change
     */
    public void changeZoom(RemoteCanvasActivity activity, float scaleFactor, float fx, float fy) {
    }

    public ImageView.ScaleType getScaleType() {
        return scaleType;
    }
}
