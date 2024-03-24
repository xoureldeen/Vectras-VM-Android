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


package com.undatech.opaque;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.DrawableContainer;

public class CanvasDrawableContainer extends DrawableContainer {
    static final int CAPACITY_FACTOR = 7;
    public Paint paint;
    // Bitmap related variables
    protected Bitmap bitmap;
    // Soft cursor related variables
    private RectF cursorRect;
    private Bitmap softCursor;
    private boolean softCursorInit = false;
    private Bitmap.Config cfg = Bitmap.Config.ARGB_8888;
    private int bitmapW;
    private int bitmapH;

    CanvasDrawableContainer(int width, int height) {
        bitmapW = width;
        bitmapH = height;

        // To please createBitmap, we ensure the size it at least 1x1.
        if (bitmapW == 0) bitmapW = 1;
        if (bitmapH == 0) bitmapH = 1;

        bitmap = Bitmap.createBitmap(bitmapW, bitmapH, cfg);
        bitmap.setHasAlpha(false);

        cursorRect = new RectF();
        // Try to free up some memory.
        System.gc();
        softCursor = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);

        paint = new Paint();
        paint.setFilterBitmap(true);
    }

    @Override
    public void draw(Canvas canvas) {
        try {
            synchronized (this) {
                canvas.drawBitmap(bitmap, 0.f, 0.f, paint);
                canvas.drawBitmap(softCursor, cursorRect.left, cursorRect.top, paint);
            }
        } catch (Throwable e) {
        }
    }

    void setCursorRect(int x, int y, float w, float h) {
        cursorRect.left = x;
        cursorRect.right = cursorRect.left + w;
        cursorRect.top = y;
        cursorRect.bottom = cursorRect.top + h;
    }

    void moveCursorRect(int x, int y) {
        cursorRect.offsetTo(x, y);
    }

    void setSoftCursor(int[] newSoftCursorPixels) {
        Bitmap oldSoftCursor = softCursor;
        softCursor = Bitmap.createBitmap(newSoftCursorPixels, (int) cursorRect.width(),
                (int) cursorRect.height(), Bitmap.Config.ARGB_8888);
        oldSoftCursor.recycle();
        softCursorInit = true;
    }

    RectF getCursorRect() {
        return cursorRect;
    }

    boolean isNotInitSoftCursor() {
        return softCursorInit;
    }

    /**
     *
     * @return The smallest scale supported by the implementation; the scale at which
     * the bitmap would be smaller than the screen
     */
    float getMinimumScale(int canvaswidth, int canvasheight) {
        return Math.min((float) canvaswidth / bitmapW, (float) canvasheight / bitmapH);
    }

    public void destroy() {
        if (bitmap != null)
            bitmap.recycle();
        bitmap = null;
        if (softCursor != null)
            softCursor.recycle();
        softCursor = null;
        cursorRect = null;
    }

    public void frameBufferSizeChanged(int width, int height) {
        android.util.Log.i("CanvasDrawableContainer", "bitmapsize changed = (" + bitmapW + "," + bitmapH + ")");
        if (bitmapW < width || bitmapH < width) {
            destroy();
            // Try to free up some memory.
            System.gc();
            bitmapW = width;
            bitmapH = height;
            bitmap = Bitmap.createBitmap(bitmapW, bitmapH, cfg);
            bitmap.setHasAlpha(false);
        }
    }

    @Override
    public int getIntrinsicHeight() {
        return bitmapH;
    }

    @Override
    public int getIntrinsicWidth() {
        return bitmapW;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    public boolean isStateful() {
        return false;
    }
}
