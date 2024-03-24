/**
 * Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 2010 Michael A. MacDonald
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

import com.undatech.opaque.RfbConnectable;
import com.undatech.opaque.Viewable;

import java.util.Arrays;

class FullBufferBitmapData extends AbstractBitmapData {
    /**
     * Multiply this times total number of pixels to get estimate of process size with all buffers plus
     * safety factor
     */
    static final int CAPACITY_MULTIPLIER = 6;

    int xoffset;
    int yoffset;
    int dataWidth;
    int dataHeight;

    /**
     * @param p
     * @param c
     */
    public FullBufferBitmapData(int width, int height, Viewable c, int capacity) {
        super(width, height, c);
        framebufferwidth = width;
        framebufferheight = height;
        bitmapwidth = framebufferwidth;
        bitmapheight = framebufferheight;
        dataWidth = framebufferwidth;
        dataHeight = framebufferheight;
        android.util.Log.i("FBBM", "bitmapsize = (" + bitmapwidth + "," + bitmapheight + ")");
        bitmapPixels = new int[framebufferwidth * framebufferheight];
        drawable.startDrawing();
    }

    /* (non-Javadoc)
     * @see com.iiordanov.bVNC.AbstractBitmapData#copyRect(android.graphics.Rect, android.graphics.Rect, android.graphics.Paint)
     */
    @Override
    public void copyRect(int sx, int sy, int dx, int dy, int w, int h) {
        int srcOffset, dstOffset;
        int dstH = h;
        int dstW = w;

        int startSrcY, endSrcY, dstY, deltaY;
        if (sy > dy) {
            startSrcY = sy;
            endSrcY = sy + dstH;
            dstY = dy;
            deltaY = +1;
        } else {
            startSrcY = sy + dstH - 1;
            endSrcY = sy - 1;
            dstY = dy + dstH - 1;
            deltaY = -1;
        }
        for (int y = startSrcY; y != endSrcY; y += deltaY) {
            srcOffset = offset(sx, y);
            dstOffset = offset(dx, dstY);
            try {
                System.arraycopy(bitmapPixels, srcOffset, bitmapPixels, dstOffset, dstW);
            } catch (Exception e) {
                // There was an index out of bounds exception, but we continue copying what we can.
                e.printStackTrace();
            }
            dstY += deltaY;
        }
    }

    /* (non-Javadoc)
     * @see com.iiordanov.bVNC.AbstractBitmapData#createDrawable()
     */
    @Override
    AbstractBitmapDrawable createDrawable() {
        return new Drawable(this);
    }

    /* (non-Javadoc)
     * @see com.iiordanov.bVNC.AbstractBitmapData#drawRect(int, int, int, int, android.graphics.Paint)
     */
    @Override
    public void drawRect(int x, int y, int w, int h, Paint paint) {
        int color = paint.getColor();
        int offset = offset(x, y);
        if (w > 10) {
            for (int j = 0; j < h; j++, offset += framebufferwidth) {
                Arrays.fill(bitmapPixels, offset, offset + w, color);
            }
        } else {
            for (int j = 0; j < h; j++, offset += framebufferwidth - w) {
                for (int k = 0; k < w; k++, offset++) {
                    bitmapPixels[offset] = color;
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see com.iiordanov.bVNC.AbstractBitmapData#offset(int, int)
     */
    @Override
    public int offset(int x, int y) {
        return x + y * framebufferwidth;
    }

    /* (non-Javadoc)
     * @see com.iiordanov.bVNC.AbstractBitmapData#scrollChanged(int, int)
     */
    @Override
    public void scrollChanged(int newx, int newy) {
        xoffset = newx;
        yoffset = newy;
    }

    /* (non-Javadoc)
     * @see com.iiordanov.bVNC.AbstractBitmapData#frameBufferSizeChanged(RfbProto)
     */
    @Override
    public void frameBufferSizeChanged(int width, int height) {
        framebufferwidth = width;
        framebufferheight = height;
        bitmapwidth = framebufferwidth;
        bitmapheight = framebufferheight;
        android.util.Log.i("FBBM", "bitmapsize changed = (" + bitmapwidth + "," + bitmapheight + ")");
        if (dataWidth < framebufferwidth || dataHeight < framebufferheight) {
            dispose();
            // Try to free up some memory.
            System.gc();
            dataWidth = framebufferwidth;
            dataHeight = framebufferheight;
            bitmapPixels = new int[framebufferwidth * framebufferheight];
            drawable = createDrawable();
            drawable.startDrawing();
        }
    }

    /* (non-Javadoc)
     * @see com.iiordanov.bVNC.AbstractBitmapData#syncScroll()
     */
    @Override
    public void syncScroll() {
        // Don't need to do anything here
    }

    /* (non-Javadoc)
     * @see com.iiordanov.bVNC.AbstractBitmapData#updateBitmap(int, int, int, int)
     */
    @Override
    public void updateBitmap(int x, int y, int w, int h) {
        // Don't need to do anything here
    }

    /* (non-Javadoc)
     * @see com.iiordanov.bVNC.AbstractBitmapData#updateBitmap(Bitmap, int, int, int, int)
     */
    @Override
    public void updateBitmap(Bitmap b, int x, int y, int w, int h) {
        b.getPixels(bitmapPixels, offset(x, y), bitmapwidth, 0, 0, w, h);
    }

    /* (non-Javadoc)
     * @see com.iiordanov.bVNC.AbstractBitmapData#validDraw(int, int, int, int)
     */
    @Override
    public boolean validDraw(int x, int y, int w, int h) {
        if (x + w > bitmapwidth || y + h > bitmapheight)
            return false;
        return true;
    }

    class Drawable extends AbstractBitmapDrawable {
        private final static String TAG = "Drawable";
        int drawWidth;
        int drawHeight;
        int xo, yo;

        /**
         * @param data
         */
        public Drawable(AbstractBitmapData data) {
            super(data);
        }

        /* (non-Javadoc)
         * @see android.graphics.drawable.DrawableContainer#draw(android.graphics.Canvas)
         */
        @Override
        public void draw(Canvas canvas) {
            toDraw = canvas.getClipBounds();

            // To avoid artifacts, we need to enlarge the box by one pixel in all directions.
            toDraw.set(toDraw.left - 1, toDraw.top - 1, toDraw.right + 1, toDraw.bottom + 1);
            drawWidth = toDraw.width();
            drawHeight = toDraw.height();

            if (toDraw.left < 0)
                xo = 0;
            else if (toDraw.left >= data.framebufferwidth)
                return;
            else
                xo = toDraw.left;

            if (toDraw.top < 0)
                yo = 0;
            else if (toDraw.top >= data.framebufferheight)
                return;
            else
                yo = toDraw.top;

            if (xo + drawWidth >= data.framebufferwidth)
                drawWidth = data.framebufferwidth - xo;
            if (yo + drawHeight >= data.framebufferheight)
                drawHeight = data.framebufferheight - yo;

            try {
                synchronized (this) {
                    canvas.drawBitmap(data.bitmapPixels, offset(xo, yo), data.framebufferwidth,
                            xo, yo, drawWidth, drawHeight, false, _defaultPaint);
                    canvas.drawBitmap(softCursor, cursorRect.left, cursorRect.top, _defaultPaint);
                }
            } catch (Throwable e) {
            }
        }
    }
}
