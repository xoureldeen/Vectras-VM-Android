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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.DrawableContainer;

/**
 * @author Michael A. MacDonald
 *
 */
public class AbstractBitmapDrawable extends DrawableContainer {
    public Paint _defaultPaint;
    RectF cursorRect;
    int hotX, hotY;
    Bitmap softCursor;
    boolean softCursorInit;
    Rect clipRect;
    Rect toDraw;
    boolean drawing = false;
    AbstractBitmapData data;
    Paint _whitePaint;
    Paint _blackPaint;

    AbstractBitmapDrawable(AbstractBitmapData data) {
        this.data = data;
        cursorRect = new RectF();
        clipRect = new Rect();
        // Try to free up some memory.
        System.gc();
        softCursor = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        softCursorInit = false;

        _defaultPaint = new Paint();
        _defaultPaint.setFilterBitmap(true);
        _whitePaint = new Paint();
        _whitePaint.setColor(0xffffffff);
        _blackPaint = new Paint();
        _blackPaint.setColor(0xff000000);
    }

    void draw(Canvas canvas, int xoff, int yoff) {

        try {
            synchronized (this) {
                canvas.drawBitmap(data.mbitmap, xoff, yoff, _defaultPaint);
                canvas.drawBitmap(softCursor, cursorRect.left, cursorRect.top, _defaultPaint);
            }
        } catch (Throwable e) {
        }
    }

    void setCursorRect(int x, int y, float w, float h, int hX, int hY) {
        hotX = hX;
        hotY = hY;
        cursorRect.left = x - hotX;
        cursorRect.right = cursorRect.left + w;
        cursorRect.top = y - hotY;
        cursorRect.bottom = cursorRect.top + h;
    }

    void moveCursorRect(int x, int y) {
        setCursorRect(x, y, cursorRect.width(), cursorRect.height(), hotX, hotY);
    }

    void setSoftCursor(int[] newSoftCursorPixels) {
        Bitmap oldSoftCursor = softCursor;
        softCursor = Bitmap.createBitmap(newSoftCursorPixels, (int) cursorRect.width(),
                (int) cursorRect.height(), Bitmap.Config.ARGB_8888);
        softCursorInit = true;
        oldSoftCursor.recycle();
    }

    /* (non-Javadoc)
     * @see android.graphics.drawable.DrawableContainer#getIntrinsicHeight()
     */
    @Override
    public int getIntrinsicHeight() {
        return data.framebufferheight;
    }

    /* (non-Javadoc)
     * @see android.graphics.drawable.DrawableContainer#getIntrinsicWidth()
     */
    @Override
    public int getIntrinsicWidth() {
        return data.framebufferwidth;
    }

    /* (non-Javadoc)
     * @see android.graphics.drawable.DrawableContainer#getOpacity()
     */
    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    /* (non-Javadoc)
     * @see android.graphics.drawable.DrawableContainer#isStateful()
     */
    @Override
    public boolean isStateful() {
        return false;
    }

    public void dispose() {
        drawing = false;
        if (softCursor != null)
            softCursor.recycle();
        softCursor = null;
        cursorRect = null;
        clipRect = null;
        toDraw = null;
    }

    protected void startDrawing() {
        drawing = true;
    }
}
