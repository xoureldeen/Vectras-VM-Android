/**
 * Copyright (C) 2019 Iordan Iordanov
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

class UltraCompactBitmapData extends AbstractBitmapData {
    /**
     * Multiply this times total number of pixels to get estimate of process size with all buffers plus
     * safety factor
     */
    static final int CAPACITY_MULTIPLIER = 4;
    private final static String TAG = "UltraCompactBitmapData";
    Bitmap.Config cfg = Bitmap.Config.RGB_565;

    UltraCompactBitmapData(int width, int height, Viewable c, boolean trueColor) {
        super(width, height, c);
        bitmapwidth = framebufferwidth;
        bitmapheight = framebufferheight;

        // To please createBitmap, we ensure the size it at least 1x1.
        if (bitmapwidth == 0) bitmapwidth = 1;
        if (bitmapheight == 0) bitmapheight = 1;

        if (trueColor) {
            cfg = Bitmap.Config.ARGB_8888;
        }

        mbitmap = Bitmap.createBitmap(bitmapwidth, bitmapheight, cfg);
        android.util.Log.i(TAG, "bitmapsize = (" + bitmapwidth + "," + bitmapheight + ")");

        if (Constants.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
            mbitmap.setHasAlpha(false);
        }

        memGraphics = new Canvas(mbitmap);
        drawable.startDrawing();
    }

    @Override
    public boolean validDraw(int x, int y, int w, int h) {
        return true;
    }

    @Override
    public int offset(int x, int y) {
        return y * bitmapwidth + x;
    }

    @Override
    AbstractBitmapDrawable createDrawable() {
        return new UltraCompactBitmapDrawable();
    }

    @Override
    public void updateBitmap(int x, int y, int w, int h) {
        // Not used
    }

    @Override
    public void updateBitmap(Bitmap b, int x, int y, int w, int h) {
        synchronized (mbitmap) {
            memGraphics.drawBitmap(b, x, y, null);
        }
    }

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
                int[] bitmapPixels = new int[w * h];
                synchronized (mbitmap) {
                    mbitmap.getPixels(bitmapPixels, srcOffset, bitmapwidth, sx - xoffset, y - yoffset, dstW, 1);
                    mbitmap.setPixels(bitmapPixels, offset(dx, dy), bitmapwidth, dx, dy, dstW, dstH);
                }
            } catch (Exception e) {
                // There was an index out of bounds exception, but we continue copying what we can.
                e.printStackTrace();
            }
            dstY += deltaY;
        }
    }

    @Override
    public void drawRect(int x, int y, int w, int h, Paint paint) {
        synchronized (mbitmap) {
            memGraphics.drawRect(x, y, x + w, y + h, paint);
        }
    }

    @Override
    public void scrollChanged(int newx, int newy) {
        // Don't need to do anything here
    }

    @Override
    public void frameBufferSizeChanged(int width, int height) {
        framebufferwidth = width;
        framebufferheight = height;
        if (bitmapwidth < framebufferwidth || bitmapheight < framebufferheight) {
            android.util.Log.i(TAG, "One or more bitmap dimensions increased, realloc = ("
                    + framebufferwidth + "," + framebufferheight + ")");
            dispose();
            // Try to free up some memory.
            System.gc();
            bitmapwidth = framebufferwidth;
            bitmapheight = framebufferheight;
            mbitmap = Bitmap.createBitmap(bitmapwidth, bitmapheight, cfg);
            memGraphics = new Canvas(mbitmap);
            drawable = createDrawable();
            drawable.startDrawing();
        } else {
            android.util.Log.i(TAG, "Both bitmap dimensions same or smaller, no realloc = ("
                    + framebufferwidth + "," + framebufferheight + ")");
        }
    }

    @Override
    public void syncScroll() {
        // Don't need anything here either
    }

    class UltraCompactBitmapDrawable extends AbstractBitmapDrawable {

        UltraCompactBitmapDrawable() {
            super(UltraCompactBitmapData.this);
        }

        @Override
        public void draw(Canvas canvas) {
            try {
                synchronized (this) {
                    canvas.drawBitmap(data.mbitmap, 0.0f, 0.0f, _defaultPaint);
                    canvas.drawBitmap(softCursor, cursorRect.left, cursorRect.top, _defaultPaint);
                }
            } catch (Throwable e) {
            }
        }
    }
}
