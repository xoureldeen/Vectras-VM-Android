/**
 * Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 2010 Michael A. MacDonald
 * Copyright (C) 2004 Horizon Wimba.  All Rights Reserved.
 * Copyright (C) 2001-2003 HorizonLive.com, Inc.  All Rights Reserved.
 * Copyright (C) 2001,2002 Constantin Kaplinsky.  All Rights Reserved.
 * Copyright (C) 2000 Tridia Corporation.  All Rights Reserved.
 * Copyright (C) 1999 AT&T Laboratories Cambridge.  All Rights Reserved.
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
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.Log;

import com.github.luben.zstd.Zstd;
import com.undatech.opaque.AbstractDrawableData;
import com.undatech.opaque.InputCarriable;
import com.undatech.opaque.Viewable;
import com.undatech.opaque.input.RemotePointer;

import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;


public class Decoder {
    private final static String TAG = "Decoder";

    // Color Model settings
    private COLORMODEL pendingColorModel = COLORMODEL.C24bit;
    private COLORMODEL colorModel = null;
    private int bytesPerPixel = 0;
    private int[] colorPalette = null;

    // Tight decoder's data.
    private Inflater[] tightInflaters = new Inflater[4];
    private Paint handleTightRectPaint = new Paint();
    private byte[] solidColorBuf = new byte[3];
    private byte[] tightPalette8 = new byte[2];
    private int[] tightPalette24 = new int[256];
    private byte[] colorBuf = new byte[768];
    private byte[] uncompDataBuf = new byte[RfbProto.TightMinToCompress * 3];
    private byte[] zlibData = new byte[4096];
    private byte[] inflBuf = new byte[8192];
    private BitmapFactory.Options bitmapopts = new BitmapFactory.Options();
    private boolean valid, useGradient;
    private int c, dx, dy, offset, boffset, idx, stream_id, comp_ctl, numColors, rowSize, dataSize, jpegDataLen;

    // ZRLE decoder's data.
    private byte[] zrleBuf;
    private int[] zrleTilePixels;
    private ZlibInStream zrleInStream;
    private Paint handleZRLERectPaint = new Paint();
    private int[] handleZRLERectPalette = new int[128];
    private byte[] readPixelsBuffer = new byte[128];

    // Zlib decoder's data.
    private byte[] zlibBuf;
    private Inflater zlibInflater;
    private byte[] handleZlibRectBuffer = new byte[128];

    // RRE decoder's data.
    private Paint handleRREPaint = new Paint();
    private byte[] bg_buf = new byte[4];
    private byte[] rre_buf = new byte[128];

    // Raw decoder's data.
    private byte[] handleRawRectBuffer = new byte[128];

    // Hextile decoder's data.
    // These colors should be kept between handleHextileSubrect() calls.
    private int hextile_bg, hextile_fg;
    private Paint handleHextileSubrectPaint = new Paint();
    private byte[] backgroundColorBuffer = new byte[4];

    private AbstractDrawableData bitmapData;
    private Viewable vncCanvas;
    private InputCarriable remoteInput;
    private boolean discardCursorShapeUpdates;

    public Decoder(Viewable v, InputCarriable c, boolean discardCursorShapeUpdates) {
        this.discardCursorShapeUpdates = discardCursorShapeUpdates;
        handleRREPaint.setStyle(Style.FILL);
        handleTightRectPaint.setStyle(Style.FILL);
        bitmapopts.inPurgeable = false;
        bitmapopts.inDither = false;
        bitmapopts.inTempStorage = new byte[32768];
        bitmapopts.inPreferredConfig = Bitmap.Config.RGB_565;
        bitmapopts.inScaled = false;
        vncCanvas = v;
        remoteInput = c;
    }

    void setBitmapData(AbstractDrawableData b) {
        bitmapData = b;
    }

    public void setPixelFormat(RfbProto rfb) throws IOException {
        pendingColorModel.setPixelFormat(rfb);
        bytesPerPixel = pendingColorModel.bpp();
        colorPalette = pendingColorModel.palette();
        colorModel = pendingColorModel;
        pendingColorModel = null;
    }

    public COLORMODEL getColorModel() {
        return colorModel;
    }

    public void setColorModel(COLORMODEL cm) {
        // Only update if color model changes
        if (colorModel == null || !colorModel.equals(cm))
            pendingColorModel = cm;
    }

    public boolean isChangedColorModel() {
        return (pendingColorModel != null);
    }

    void handleRawRect(RfbProto rfb, int x, int y, int w, int h) throws IOException {
        handleRawRect(rfb, x, y, w, h, true);
    }

    void handleRawRect(RfbProto rfb, int x, int y, int w, int h, boolean paint) throws IOException {
        boolean valid = bitmapData.validDraw(x, y, w, h);
        int[] pixels = bitmapData.getBitmapPixels();
        if (bytesPerPixel == 1) {
            // 1 byte per pixel. Use palette lookup table.
            if (w > handleRawRectBuffer.length) {
                handleRawRectBuffer = new byte[w];
            }
            int i, offset;
            for (int dy = y; dy < y + h; dy++) {
                rfb.readFully(handleRawRectBuffer, 0, w);
                if (!valid)
                    continue;
                offset = bitmapData.offset(x, dy);
                for (i = 0; i < w; i++) {
                    pixels[offset + i] = colorPalette[0xFF & handleRawRectBuffer[i]];
                }
            }
        } else {
            // 4 bytes per pixel (argb) 24-bit color

            final int l = w * 4;
            if (l > handleRawRectBuffer.length) {
                handleRawRectBuffer = new byte[l];
            }
            int i, offset;
            for (int dy = y; dy < y + h; dy++) {
                rfb.readFully(handleRawRectBuffer, 0, l);
                if (!valid)
                    continue;
                offset = bitmapData.offset(x, dy);
                for (i = 0; i < w; i++) {
                    final int idx = i * 4;
                    pixels[offset + i] = // 0xFF << 24 |
                            (handleRawRectBuffer[idx + 2] & 0xff) << 16 | (handleRawRectBuffer[idx + 1] & 0xff) << 8 | (handleRawRectBuffer[idx] & 0xff);
                }
            }
        }

        if (!valid)
            return;

        bitmapData.updateBitmap(x, y, w, h);

        if (paint)
            vncCanvas.reDraw(x, y, w, h);
    }

    //
    // Handle a CopyRect rectangle.
    //
    void handleCopyRect(RfbProto rfb, int x, int y, int w, int h) throws IOException {
        // Read the source coordinates.
        rfb.readCopyRect();

        if (!bitmapData.validDraw(x, y, w, h))
            return;

        bitmapData.copyRect(rfb.copyRectSrcX, rfb.copyRectSrcY, x, y, w, h);
        vncCanvas.reDraw(x, y, w, h);
    }

    //
    // Handle an RRE-encoded rectangle.
    //
    void handleRRERect(RfbProto rfb, int x, int y, int w, int h) throws IOException {
        boolean valid = bitmapData.validDraw(x, y, w, h);
        int nSubrects = rfb.is.readInt();

        rfb.readFully(bg_buf, 0, bytesPerPixel);
        int pixel;
        if (bytesPerPixel == 1) {
            pixel = colorPalette[0xFF & bg_buf[0]];
        } else {
            pixel = Color.rgb(bg_buf[2] & 0xFF, bg_buf[1] & 0xFF, bg_buf[0] & 0xFF);
        }
        handleRREPaint.setColor(pixel);
        if (valid)
            bitmapData.drawRect(x, y, w, h, handleRREPaint);

        int len = nSubrects * (bytesPerPixel + 8);
        if (len > rre_buf.length)
            rre_buf = new byte[len];

        rfb.readFully(rre_buf, 0, len);
        if (!valid)
            return;

        int sx, sy, sw, sh;

        int i = 0;
        for (int j = 0; j < nSubrects; j++) {
            if (bytesPerPixel == 1) {
                pixel = colorPalette[0xFF & rre_buf[i++]];
            } else {
                pixel = Color.rgb(rre_buf[i + 2] & 0xFF, rre_buf[i + 1] & 0xFF, rre_buf[i] & 0xFF);
                i += 4;
            }
            sx = x + ((rre_buf[i] & 0xff) << 8) + (rre_buf[i + 1] & 0xff);
            i += 2;
            sy = y + ((rre_buf[i] & 0xff) << 8) + (rre_buf[i + 1] & 0xff);
            i += 2;
            sw = ((rre_buf[i] & 0xff) << 8) + (rre_buf[i + 1] & 0xff);
            i += 2;
            sh = ((rre_buf[i] & 0xff) << 8) + (rre_buf[i + 1] & 0xff);
            i += 2;

            handleRREPaint.setColor(pixel);
            bitmapData.drawRect(sx, sy, sw, sh, handleRREPaint);
        }

        vncCanvas.reDraw(x, y, w, h);
    }

    //
    // Handle a CoRRE-encoded rectangle.
    //

    void handleCoRRERect(RfbProto rfb, int x, int y, int w, int h) throws IOException {
        boolean valid = bitmapData.validDraw(x, y, w, h);
        int nSubrects = rfb.is.readInt();

        rfb.readFully(bg_buf, 0, bytesPerPixel);
        int pixel;
        if (bytesPerPixel == 1) {
            pixel = colorPalette[0xFF & bg_buf[0]];
        } else {
            pixel = Color.rgb(bg_buf[2] & 0xFF, bg_buf[1] & 0xFF, bg_buf[0] & 0xFF);
        }
        handleRREPaint.setColor(pixel);
        if (valid)
            bitmapData.drawRect(x, y, w, h, handleRREPaint);

        int len = nSubrects * (bytesPerPixel + 8);
        if (len > rre_buf.length)
            rre_buf = new byte[len];

        rfb.readFully(rre_buf, 0, len);
        if (!valid)
            return;

        int sx, sy, sw, sh;
        int i = 0;

        for (int j = 0; j < nSubrects; j++) {
            if (bytesPerPixel == 1) {
                pixel = colorPalette[0xFF & rre_buf[i++]];
            } else {
                pixel = Color.rgb(rre_buf[i + 2] & 0xFF, rre_buf[i + 1] & 0xFF, rre_buf[i] & 0xFF);
                i += 4;
            }
            sx = x + (rre_buf[i++] & 0xFF);
            sy = y + (rre_buf[i++] & 0xFF);
            sw = rre_buf[i++] & 0xFF;
            sh = rre_buf[i++] & 0xFF;

            handleRREPaint.setColor(pixel);
            bitmapData.drawRect(sx, sy, sw, sh, handleRREPaint);
        }

        vncCanvas.reDraw(x, y, w, h);
    }

    //
    // Handle a Hextile-encoded rectangle.
    //
    void handleHextileRect(RfbProto rfb, int x, int y, int w, int h) throws IOException {

        hextile_bg = Color.BLACK;
        hextile_fg = Color.BLACK;

        for (int ty = y; ty < y + h; ty += 16) {
            int th = 16;
            if (y + h - ty < 16)
                th = y + h - ty;

            for (int tx = x; tx < x + w; tx += 16) {
                int tw = 16;
                if (x + w - tx < 16)
                    tw = x + w - tx;

                handleHextileSubrect(rfb, tx, ty, tw, th);
            }

            // Finished with a row of tiles, now let's show it.
            vncCanvas.reDraw(x, y, w, h);
        }
    }

    //
    // Handle one tile in the Hextile-encoded data.
    //
    private void handleHextileSubrect(RfbProto rfb, int tx, int ty, int tw, int th) throws IOException {

        int subencoding = rfb.is.readUnsignedByte();

        // Is it a raw-encoded sub-rectangle?
        if ((subencoding & RfbProto.HextileRaw) != 0) {
            handleRawRect(rfb, tx, ty, tw, th, false);
            return;
        }

        boolean valid = bitmapData.validDraw(tx, ty, tw, th);
        // Read and draw the background if specified.
        if (bytesPerPixel > backgroundColorBuffer.length) {
            throw new RuntimeException("impossible colordepth");
        }
        if ((subencoding & RfbProto.HextileBackgroundSpecified) != 0) {
            rfb.readFully(backgroundColorBuffer, 0, bytesPerPixel);
            if (bytesPerPixel == 1) {
                hextile_bg = colorPalette[0xFF & backgroundColorBuffer[0]];
            } else {
                hextile_bg = Color.rgb(backgroundColorBuffer[2] & 0xFF, backgroundColorBuffer[1] & 0xFF, backgroundColorBuffer[0] & 0xFF);
            }
        }
        handleHextileSubrectPaint.setColor(hextile_bg);
        handleHextileSubrectPaint.setStyle(Paint.Style.FILL);
        if (valid)
            bitmapData.drawRect(tx, ty, tw, th, handleHextileSubrectPaint);

        // Read the foreground color if specified.
        if ((subencoding & RfbProto.HextileForegroundSpecified) != 0) {
            rfb.readFully(backgroundColorBuffer, 0, bytesPerPixel);
            if (bytesPerPixel == 1) {
                hextile_fg = colorPalette[0xFF & backgroundColorBuffer[0]];
            } else {
                hextile_fg = Color.rgb(backgroundColorBuffer[2] & 0xFF, backgroundColorBuffer[1] & 0xFF, backgroundColorBuffer[0] & 0xFF);
            }
        }

        // Done with this tile if there is no sub-rectangles.
        if ((subencoding & RfbProto.HextileAnySubrects) == 0)
            return;

        int nSubrects = rfb.is.readUnsignedByte();
        int bufsize = nSubrects * 2;
        if ((subencoding & RfbProto.HextileSubrectsColoured) != 0) {
            bufsize += nSubrects * bytesPerPixel;
        }
        if (rre_buf.length < bufsize)
            rre_buf = new byte[bufsize];
        rfb.readFully(rre_buf, 0, bufsize);

        int b1, b2, sx, sy, sw, sh;
        int i = 0;
        if ((subencoding & RfbProto.HextileSubrectsColoured) == 0) {

            // Sub-rectangles are all of the same color.
            handleHextileSubrectPaint.setColor(hextile_fg);
            for (int j = 0; j < nSubrects; j++) {
                b1 = rre_buf[i++] & 0xFF;
                b2 = rre_buf[i++] & 0xFF;
                sx = tx + (b1 >> 4);
                sy = ty + (b1 & 0xf);
                sw = (b2 >> 4) + 1;
                sh = (b2 & 0xf) + 1;
                if (valid)
                    bitmapData.drawRect(sx, sy, sw, sh, handleHextileSubrectPaint);
            }
        } else if (bytesPerPixel == 1) {

            // BGR233 (8-bit color) version for colored sub-rectangles.
            for (int j = 0; j < nSubrects; j++) {
                hextile_fg = colorPalette[0xFF & rre_buf[i++]];
                b1 = rre_buf[i++] & 0xFF;
                b2 = rre_buf[i++] & 0xFF;
                sx = tx + (b1 >> 4);
                sy = ty + (b1 & 0xf);
                sw = (b2 >> 4) + 1;
                sh = (b2 & 0xf) + 1;
                handleHextileSubrectPaint.setColor(hextile_fg);
                if (valid)
                    bitmapData.drawRect(sx, sy, sw, sh, handleHextileSubrectPaint);
            }

        } else {

            // Full-color (24-bit) version for colored sub-rectangles.
            for (int j = 0; j < nSubrects; j++) {
                hextile_fg = Color.rgb(rre_buf[i + 2] & 0xFF, rre_buf[i + 1] & 0xFF, rre_buf[i] & 0xFF);
                i += 4;
                b1 = rre_buf[i++] & 0xFF;
                b2 = rre_buf[i++] & 0xFF;
                sx = tx + (b1 >> 4);
                sy = ty + (b1 & 0xf);
                sw = (b2 >> 4) + 1;
                sh = (b2 & 0xf) + 1;
                handleHextileSubrectPaint.setColor(hextile_fg);
                if (valid)
                    bitmapData.drawRect(sx, sy, sw, sh, handleHextileSubrectPaint);
            }

        }
    }

    //
    // Handle a ZRLE-encoded rectangle.
    //
    void handleZRLERect(RfbProto rfb, int x, int y, int w, int h) throws Exception {

        if (zrleInStream == null)
            zrleInStream = new ZlibInStream();

        int nBytes = rfb.is.readInt();
        if (nBytes > 64 * 1024 * 1024)
            throw new Exception("ZRLE decoder: illegal compressed data size");

        if (zrleBuf == null || zrleBuf.length < nBytes) {
            zrleBuf = new byte[nBytes + 4096];
        }

        rfb.readFully(zrleBuf, 0, nBytes);

        zrleInStream.setUnderlying(new MemInStream(zrleBuf, 0, nBytes), nBytes);

        boolean valid = bitmapData.validDraw(x, y, w, h);

        for (int ty = y; ty < y + h; ty += 64) {

            int th = Math.min(y + h - ty, 64);

            for (int tx = x; tx < x + w; tx += 64) {

                int tw = Math.min(x + w - tx, 64);

                int mode = zrleInStream.readU8();
                boolean rle = (mode & 128) != 0;
                int palSize = mode & 127;

                readZrlePalette(handleZRLERectPalette, palSize);

                if (palSize == 1) {
                    int pix = handleZRLERectPalette[0];
                    int c = (bytesPerPixel == 1) ? colorPalette[0xFF & pix] : (0xFF000000 | pix);
                    handleZRLERectPaint.setColor(c);
                    handleZRLERectPaint.setStyle(Paint.Style.FILL);
                    if (valid)
                        bitmapData.drawRect(tx, ty, tw, th, handleZRLERectPaint);
                    continue;
                }

                if (!rle) {
                    if (palSize == 0) {
                        readZrleRawPixels(tw, th);
                    } else {
                        readZrlePackedPixels(tw, th, handleZRLERectPalette, palSize);
                    }
                } else {
                    if (palSize == 0) {
                        readZrlePlainRLEPixels(tw, th);
                    } else {
                        readZrlePackedRLEPixels(tw, th, handleZRLERectPalette);
                    }
                }
                if (valid)
                    handleUpdatedZrleTile(tx, ty, tw, th);
            }
        }

        zrleInStream.reset();

        vncCanvas.reDraw(x, y, w, h);
    }

    //
    // Handle a Zlib-encoded rectangle.
    //
    void handleZlibRect(RfbProto rfb, int x, int y, int w, int h) throws Exception {
        boolean valid = bitmapData.validDraw(x, y, w, h);
        int nBytes = rfb.is.readInt();

        if (zlibBuf == null || zlibBuf.length < nBytes) {
            zlibBuf = new byte[nBytes * 2];
        }

        rfb.readFully(zlibBuf, 0, nBytes);

        if (zlibInflater == null) {
            zlibInflater = new Inflater();
        }
        zlibInflater.setInput(zlibBuf, 0, nBytes);

        int[] pixels = bitmapData.getBitmapPixels();

        if (bytesPerPixel == 1) {
            // 1 byte per pixel. Use palette lookup table.
            if (w > handleZlibRectBuffer.length) {
                handleZlibRectBuffer = new byte[w];
            }
            int i, offset;
            for (int dy = y; dy < y + h; dy++) {
                zlibInflater.inflate(handleZlibRectBuffer, 0, w);
                if (!valid)
                    continue;
                offset = bitmapData.offset(x, dy);
                for (i = 0; i < w; i++) {
                    pixels[offset + i] = colorPalette[0xFF & handleZlibRectBuffer[i]];
                }
            }
        } else {
            // 24-bit color (ARGB) 4 bytes per pixel.
            final int l = w * 4;
            if (l > handleZlibRectBuffer.length) {
                handleZlibRectBuffer = new byte[l];
            }
            int i, offset;
            for (int dy = y; dy < y + h; dy++) {
                zlibInflater.inflate(handleZlibRectBuffer, 0, l);
                if (!valid)
                    continue;
                offset = bitmapData.offset(x, dy);
                for (i = 0; i < w; i++) {
                    final int idx = i * 4;
                    pixels[offset + i] = (handleZlibRectBuffer[idx + 2] & 0xFF) << 16 | (handleZlibRectBuffer[idx + 1] & 0xFF) << 8 | (handleZlibRectBuffer[idx] & 0xFF);
                }
            }
        }
        if (!valid)
            return;
        bitmapData.updateBitmap(x, y, w, h);

        vncCanvas.reDraw(x, y, w, h);
    }

    private int readPixel(InStream is) throws Exception {
        int pix;
        if (bytesPerPixel == 1) {
            pix = is.readU8();
        } else {
            int p1 = is.readU8();
            int p2 = is.readU8();
            int p3 = is.readU8();
            pix = (p3 & 0xFF) << 16 | (p2 & 0xFF) << 8 | (p1 & 0xFF);
        }
        return pix;
    }


    private void readPixels(InStream is, int[] dst, int count) throws Exception {
        if (bytesPerPixel == 1) {
            if (count > readPixelsBuffer.length) {
                readPixelsBuffer = new byte[count];
            }
            is.readBytes(readPixelsBuffer, 0, count);
            for (int i = 0; i < count; i++) {
                dst[i] = (int) readPixelsBuffer[i] & 0xFF;
            }
        } else {
            final int l = count * 3;
            if (l > readPixelsBuffer.length) {
                readPixelsBuffer = new byte[l];
            }
            is.readBytes(readPixelsBuffer, 0, l);
            for (int i = 0; i < count; i++) {
                final int idx = i * 3;
                dst[i] = ((readPixelsBuffer[idx + 2] & 0xFF) << 16 | (readPixelsBuffer[idx + 1] & 0xFF) << 8 | (readPixelsBuffer[idx] & 0xFF));
            }
        }
    }

    private void readZrlePalette(int[] palette, int palSize) throws Exception {
        readPixels(zrleInStream, palette, palSize);
    }

    private void readZrleRawPixels(int tw, int th) throws Exception {
        int len = tw * th;
        if (zrleTilePixels == null || len > zrleTilePixels.length)
            zrleTilePixels = new int[len];
        readPixels(zrleInStream, zrleTilePixels, tw * th); // /
    }

    private void readZrlePackedPixels(int tw, int th, int[] palette, int palSize) throws Exception {

        int bppp = ((palSize > 16) ? 8 : ((palSize > 4) ? 4 : ((palSize > 2) ? 2 : 1)));
        int ptr = 0;
        int len = tw * th;
        if (zrleTilePixels == null || len > zrleTilePixels.length)
            zrleTilePixels = new int[len];

        for (int i = 0; i < th; i++) {
            int eol = ptr + tw;
            int b = 0;
            int nbits = 0;

            while (ptr < eol) {
                if (nbits == 0) {
                    b = zrleInStream.readU8();
                    nbits = 8;
                }
                nbits -= bppp;
                int index = (b >> nbits) & ((1 << bppp) - 1) & 127;
                if (bytesPerPixel == 1) {
                    if (index >= colorPalette.length)
                        Log.e(TAG, "zrlePlainRLEPixels palette lookup out of bounds " + index + " (0x" + Integer.toHexString(index) + ")");
                    zrleTilePixels[ptr++] = colorPalette[0xFF & palette[index]];
                } else {
                    zrleTilePixels[ptr++] = palette[index];
                }
            }
        }
    }

    private void readZrlePlainRLEPixels(int tw, int th) throws Exception {
        int ptr = 0;
        int end = ptr + tw * th;
        if (zrleTilePixels == null || end > zrleTilePixels.length)
            zrleTilePixels = new int[end];
        while (ptr < end) {
            int pix = readPixel(zrleInStream);
            int len = 1;
            int b;
            do {
                b = zrleInStream.readU8();
                len += b;
            } while (b == 255);

            if (!(len <= end - ptr))
                throw new Exception("ZRLE decoder: assertion failed" + " (len <= end-ptr)");

            if (bytesPerPixel == 1) {
                while (len-- > 0)
                    zrleTilePixels[ptr++] = colorPalette[0xFF & pix];
            } else {
                while (len-- > 0)
                    zrleTilePixels[ptr++] = pix;
            }
        }
    }

    private void readZrlePackedRLEPixels(int tw, int th, int[] palette) throws Exception {

        int ptr = 0;
        int end = ptr + tw * th;
        if (zrleTilePixels == null || end > zrleTilePixels.length)
            zrleTilePixels = new int[end];
        while (ptr < end) {
            int index = zrleInStream.readU8();
            int len = 1;
            if ((index & 128) != 0) {
                int b;
                do {
                    b = zrleInStream.readU8();
                    len += b;
                } while (b == 255);

                if (!(len <= end - ptr))
                    throw new Exception("ZRLE decoder: assertion failed" + " (len <= end - ptr)");
            }

            index &= 127;
            int pix = palette[index];

            if (bytesPerPixel == 1) {
                while (len-- > 0)
                    zrleTilePixels[ptr++] = colorPalette[0xFF & pix];
            } else {
                while (len-- > 0)
                    zrleTilePixels[ptr++] = pix;
            }
        }
    }

    //
    // Copy pixels from zrleTilePixels8 or zrleTilePixels24, then update.
    //

    private void handleUpdatedZrleTile(int x, int y, int w, int h) {
        int offsetSrc = 0;
        int[] destPixels = bitmapData.getBitmapPixels();
        for (int j = 0; j < h; j++) {
            System.arraycopy(zrleTilePixels, offsetSrc, destPixels, bitmapData.offset(x, y + j), w);
            offsetSrc += w;
        }

        bitmapData.updateBitmap(x, y, w, h);
    }


    //
    // Handle a Tight-encoded rectangle.
    //
    void handleTightRect(RfbProto rfb, int x, int y, int w, int h, boolean zstd) throws Exception {

        int[] pixels = bitmapData.getBitmapPixels();
        valid = bitmapData.validDraw(x, y, w, h);
        comp_ctl = rfb.is.readUnsignedByte();

        rowSize = w;
        boffset = 0;
        numColors = 0;
        useGradient = false;

        // Flush zlib streams if we are told by the server to do so.
        for (stream_id = 0; stream_id < 4; stream_id++) {
            if ((comp_ctl & 1) != 0) {
                tightInflaters[stream_id] = null;
            }
            comp_ctl >>= 1;
        }

        // Check correctness of sub-encoding value.
        if (comp_ctl > RfbProto.TightMaxSubencoding) {
            throw new Exception("Incorrect tight subencoding: " + comp_ctl);
        }

        // Handle solid-color rectangles.
        if (comp_ctl == RfbProto.TightFill) {
            if (bytesPerPixel == 1) {
                idx = rfb.is.readUnsignedByte();
                handleTightRectPaint.setColor(colorPalette[0xFF & idx]);
            } else {
                rfb.readFully(solidColorBuf, 0, 3);
                handleTightRectPaint.setColor(0xFF000000 | (solidColorBuf[0] & 0xFF) << 16
                        | (solidColorBuf[1] & 0xFF) << 8 | (solidColorBuf[2] & 0xFF));
            }
            if (valid) {
                bitmapData.drawRect(x, y, w, h, handleTightRectPaint);
                vncCanvas.reDraw(x, y, w, h);
            }
            return;
        }

        if (comp_ctl == RfbProto.TightJpeg) {
            // Read JPEG data.
            jpegDataLen = rfb.readCompactLen();
            if (jpegDataLen > inflBuf.length) {
                inflBuf = new byte[2 * jpegDataLen];
            }
            rfb.readFully(inflBuf, 0, jpegDataLen);
            if (!valid)
                return;

            // Decode JPEG data
            Bitmap tightBitmap = BitmapFactory.decodeByteArray(inflBuf, 0, jpegDataLen, bitmapopts);

            // Copy decoded data into bitmapData and recycle bitmap.
            //tightBitmap.getPixels(pixels, bitmapData.offset(x, y), bitmapData.bitmapwidth, 0, 0, w, h);
            bitmapData.updateBitmap(tightBitmap, x, y, w, h);
            vncCanvas.reDraw(x, y, w, h);
            // To avoid running out of memory, recycle bitmap immediately.
            tightBitmap.recycle();
            return;
        }

        // Read filter id and parameters.
        if ((comp_ctl & RfbProto.TightExplicitFilter) != 0) {
            int filter_id = rfb.is.readUnsignedByte();

            if (filter_id == RfbProto.TightFilterPalette) {
                numColors = rfb.is.readUnsignedByte() + 1;

                if (bytesPerPixel == 1) {
                    if (numColors != 2) {
                        throw new Exception("Incorrect tight palette size: " + numColors);
                    }
                    rfb.readFully(tightPalette8, 0, 2);

                } else {
                    rfb.readFully(colorBuf, 0, numColors * 3);
                    for (c = 0; c < numColors; c++) {
                        idx = c * 3;
                        tightPalette24[c] = ((colorBuf[idx] & 0xFF) << 16 |
                                (colorBuf[idx + 1] & 0xFF) << 8 |
                                (colorBuf[idx + 2] & 0xFF));
                    }
                }

                if (numColors == 2)
                    rowSize = (w + 7) / 8;

            } else if (filter_id == RfbProto.TightFilterGradient) {
                useGradient = true;
            } else if (filter_id != RfbProto.TightFilterCopy) {
                throw new Exception("Incorrect tight filter id: " + filter_id);
            }
        }

        if (numColors == 0 && bytesPerPixel == 4)
            rowSize *= 3;

        // Read, optionally uncompress and decode data.
        dataSize = h * rowSize;

        if (dataSize < RfbProto.TightMinToCompress) {
            // Data size is small - not compressed with zlib.
            rfb.readFully(uncompDataBuf, 0, dataSize);
            if (!valid)
                return;

            if (numColors != 0) {
                // Indexed colors.
                if (numColors == 2) {
                    // Two colors.
                    if (bytesPerPixel == 1) {
                        decodeMonoData(x, y, w, h, uncompDataBuf, tightPalette8);
                    } else {
                        decodeMonoData(x, y, w, h, uncompDataBuf, tightPalette24);
                    }
                } else {
                    // 3..255 colors (assuming bytesPerPixel == 4).
                    boffset = 0;
                    for (dy = y; dy < y + h; dy++) {
                        offset = bitmapData.offset(x, dy);
                        for (dx = x; dx < x + w; dx++) {
                            pixels[offset++] = tightPalette24[uncompDataBuf[boffset++] & 0xFF];
                        }
                    }
                }
            } else if (useGradient) {
                // "Gradient"-processed data
                decodeGradientData(x, y, w, h, uncompDataBuf);
            } else {
                boffset = 0;
                // Raw true-color data.
                if (bytesPerPixel == 1) {
                    for (dy = y; dy < y + h; dy++) {
                        offset = bitmapData.offset(x, dy);
                        for (dx = 0; dx < w; dx++) {
                            pixels[offset++] = colorPalette[0xFF & uncompDataBuf[boffset++]];
                        }
                    }
                } else {
                    for (dy = y; dy < y + h; dy++) {
                        offset = bitmapData.offset(x, dy);
                        for (dx = 0; dx < w; dx++) {
                            idx = boffset * 3;
                            boffset++;
                            pixels[offset++] = (uncompDataBuf[idx] & 0xFF) << 16 |
                                    (uncompDataBuf[idx + 1] & 0xFF) << 8 |
                                    (uncompDataBuf[idx + 2] & 0xFF);
                        }
                    }
                }
            }

        } else {
            // Data was compressed with zlib or zstd
            if (zstd) {
                // Data was compressed with zstd.
                int zlibDataLen = rfb.readCompactLen();
                zlibData = new byte[zlibDataLen];
                rfb.readFully(zlibData);

                inflBuf = new byte[dataSize];

                try {
                    Zstd.decompress(inflBuf, zlibData);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            } else {
                // Data was compressed with zlib.
                int zlibDataLen = rfb.readCompactLen();
                if (zlibDataLen > zlibData.length) {
                    zlibData = new byte[zlibDataLen * 2];
                }
                rfb.readFully(zlibData, 0, zlibDataLen);

                stream_id = comp_ctl & 0x03;
                if (tightInflaters[stream_id] == null) {
                    tightInflaters[stream_id] = new Inflater();
                }

                Inflater myInflater = tightInflaters[stream_id];
                myInflater.setInput(zlibData, 0, zlibDataLen);

                if (dataSize > inflBuf.length) {
                    inflBuf = new byte[dataSize * 2];
                }

                try {
                    myInflater.inflate(inflBuf, 0, dataSize);
                } catch (DataFormatException e) {
                    e.printStackTrace();
                }
            }

            if (!valid)
                return;

            if (numColors != 0) {
                // Indexed colors.
                if (numColors == 2) {
                    // Two colors.
                    if (bytesPerPixel == 1) {
                        decodeMonoData(x, y, w, h, inflBuf, tightPalette8);
                    } else {
                        decodeMonoData(x, y, w, h, inflBuf, tightPalette24);
                    }
                } else {
                    // More than two colors (assuming bytesPerPixel == 4).
                    boffset = 0;
                    for (dy = y; dy < y + h; dy++) {
                        offset = bitmapData.offset(x, dy);
                        for (dx = x; dx < x + w; dx++) {
                            pixels[offset++] = tightPalette24[inflBuf[boffset++] & 0xFF];
                        }
                    }
                }
            } else if (useGradient) {
                // Compressed "Gradient"-filtered data (assuming bytesPerPixel == 4).
                decodeGradientData(x, y, w, h, inflBuf);
            } else {
                boffset = 0;
                // Compressed true-color data.
                if (bytesPerPixel == 1) {
                    for (dy = y; dy < y + h; dy++) {
                        offset = bitmapData.offset(x, dy);
                        for (dx = 0; dx < w; dx++) {
                            pixels[offset++] = colorPalette[0xFF & inflBuf[boffset++]];
                        }
                    }
                } else {
                    for (dy = y; dy < y + h; dy++) {
                        offset = bitmapData.offset(x, dy);
                        for (dx = 0; dx < w; dx++) {
                            idx = boffset * 3;
                            boffset++;
                            pixels[offset++] = (inflBuf[idx] & 0xFF) << 16 |
                                    (inflBuf[idx + 1] & 0xFF) << 8 |
                                    (inflBuf[idx + 2] & 0xFF);
                        }
                    }
                }
            }
        }

        bitmapData.updateBitmap(x, y, w, h);
        vncCanvas.reDraw(x, y, w, h);
    }

    //
    // Decode 1bpp-encoded bi-color rectangle (8-bit and 24-bit versions).
    //
    void decodeMonoData(int x, int y, int w, int h, byte[] src, byte[] palette) {

        int dx, dy, n;
        int i = bitmapData.offset(x, y);
        int[] pixels = bitmapData.getBitmapPixels();
        int rowBytes = (w + 7) / 8;
        byte b;

        for (dy = 0; dy < h; dy++) {
            for (dx = 0; dx < w / 8; dx++) {
                b = src[dy * rowBytes + dx];
                for (n = 7; n >= 0; n--) {
                    pixels[i++] = colorPalette[0xFF & palette[b >> n & 1]];
                }
            }
            for (n = 7; n >= 8 - w % 8; n--) {
                pixels[i++] = colorPalette[0xFF & palette[src[dy * rowBytes + dx] >> n & 1]];
            }
            i += (bitmapData.getBitmapWidth() - w);
        }
    }

    void decodeMonoData(int x, int y, int w, int h, byte[] src, int[] palette) {

        int dx, dy, n;
        int i = bitmapData.offset(x, y);
        int[] pixels = bitmapData.getBitmapPixels();
        int rowBytes = (w + 7) / 8;
        byte b;

        for (dy = 0; dy < h; dy++) {
            for (dx = 0; dx < w / 8; dx++) {
                b = src[dy * rowBytes + dx];
                for (n = 7; n >= 0; n--) {
                    pixels[i++] = palette[b >> n & 1];
                }
            }
            for (n = 7; n >= 8 - w % 8; n--) {
                pixels[i++] = palette[src[dy * rowBytes + dx] >> n & 1];
            }
            i += (bitmapData.getBitmapWidth() - w);
        }
    }

    //
    // Decode data processed with the "Gradient" filter.
    //
    void decodeGradientData(int x, int y, int w, int h, byte[] buf) {

        int dx, dy, c;
        byte[] prevRow = new byte[w * 3];
        byte[] thisRow = new byte[w * 3];
        byte[] pix = new byte[3];
        int[] est = new int[3];
        int[] pixels = bitmapData.getBitmapPixels();

        int offset = bitmapData.offset(x, y);

        for (dy = 0; dy < h; dy++) {

            /* First pixel in a row */
            for (c = 0; c < 3; c++) {
                pix[c] = (byte) (prevRow[c] + buf[dy * w * 3 + c]);
                thisRow[c] = pix[c];
            }
            pixels[offset++] = (pix[0] & 0xFF) << 16 | (pix[1] & 0xFF) << 8 | (pix[2] & 0xFF);

            /* Remaining pixels of a row */
            for (dx = 1; dx < w; dx++) {
                for (c = 0; c < 3; c++) {
                    est[c] = ((prevRow[dx * 3 + c] & 0xFF) + (pix[c] & 0xFF) -
                            (prevRow[(dx - 1) * 3 + c] & 0xFF));
                    if (est[c] > 0xFF) {
                        est[c] = 0xFF;
                    } else if (est[c] < 0x00) {
                        est[c] = 0x00;
                    }
                    pix[c] = (byte) (est[c] + buf[(dy * w + dx) * 3 + c]);
                    thisRow[dx * 3 + c] = pix[c];
                }
                pixels[offset++] = (pix[0] & 0xFF) << 16 | (pix[1] & 0xFF) << 8 | (pix[2] & 0xFF);
            }

            System.arraycopy(thisRow, 0, prevRow, 0, w * 3);
            offset += (bitmapData.getBitmapWidth() - w);
        }
    }

    /**
     * Handles cursor shape update (XCursor and RichCursor encodings).
     */
    synchronized void
    handleCursorShapeUpdate(RfbProto rfb, int encodingType, int hotX, int hotY, int w, int h) throws IOException {

        RemotePointer p = remoteInput.getPointer();
        int x = p.getX();
        int y = p.getY();

        if (w * h == 0)
            return;

        // Ignore cursor shape data if requested by user.
        /*if (ignoreCursorUpdates) {
            int bytesPerRow = (w + 7) / 8;
            int bytesMaskData = bytesPerRow * h;

            if (encodingType == RfbProto.EncodingXCursor) {
                rfb.is.skipBytes(6 + bytesMaskData * 2);
            } else {
                // RfbProto.EncodingRichCursor
                rfb.is.skipBytes(w * h * bytesPerPixel + bytesMaskData);
            }
            return;
        }*/
        int[] cursorShape = decodeCursorShape(rfb, encodingType, w, h);

        if (!discardCursorShapeUpdates) {
            // Set cursor rectangle.
            bitmapData.setCursorRect(x, y, w, h, hotX, hotY);

            // Decode cursor pixel data, and set pixel data into bitmap drawable.
            bitmapData.setSoftCursor(cursorShape);

            // Show the cursor.
            RectF r = bitmapData.getCursorRect();
            vncCanvas.reDraw(r.left, r.top, r.width(), r.height());
        }
    }

    /**
     * Decode cursor pixel data and return it in an int array.
     * @param encodingType
     * @param width
     * @param height
     * @return
     * @throws IOException
     */
    synchronized int[] decodeCursorShape(RfbProto rfb, int encodingType, int width, int height) throws IOException {

        int bytesPerRow = (width + 7) / 8;
        int bytesMaskData = bytesPerRow * height;

        int[] softCursorPixels = new int[width * height];

        if (encodingType == RfbProto.EncodingXCursor) {

            // Read foreground and background colors of the cursor.
            byte[] rgb = new byte[6];
            rfb.readFully(rgb);
            int[] colors = {(0xFF000000 | (rgb[3] & 0xFF) << 16 |
                    (rgb[4] & 0xFF) << 8 | (rgb[5] & 0xFF)),
                    (0xFF000000 | (rgb[0] & 0xFF) << 16 |
                            (rgb[1] & 0xFF) << 8 | (rgb[2] & 0xFF))};

            // Read pixel and mask data.
            byte[] pixBuf = new byte[bytesMaskData];
            rfb.readFully(pixBuf);
            byte[] maskBuf = new byte[bytesMaskData];
            rfb.readFully(maskBuf);

            // Decode pixel data into softCursorPixels[].
            byte pixByte, maskByte;
            int x, y, n, result;
            int i = 0;
            for (y = 0; y < height; y++) {
                for (x = 0; x < width / 8; x++) {
                    pixByte = pixBuf[y * bytesPerRow + x];
                    maskByte = maskBuf[y * bytesPerRow + x];
                    for (n = 7; n >= 0; n--) {
                        if ((maskByte >> n & 1) != 0) {
                            result = colors[pixByte >> n & 1];
                        } else {
                            result = 0;    // Transparent pixel
                        }
                        softCursorPixels[i++] = result;
                    }
                }
                for (n = 7; n >= 8 - width % 8; n--) {
                    if ((maskBuf[y * bytesPerRow + x] >> n & 1) != 0) {
                        result = colors[pixBuf[y * bytesPerRow + x] >> n & 1];
                    } else {
                        result = 0;        // Transparent pixel
                    }
                    softCursorPixels[i++] = result;
                }
            }

        } else {
            // encodingType == rfb.EncodingRichCursor

            // Read pixel and mask data.
            byte[] pixBuf = new byte[width * height * bytesPerPixel];
            rfb.readFully(pixBuf);
            byte[] maskBuf = new byte[bytesMaskData];
            rfb.readFully(maskBuf);

            // Decode pixel data into softCursorPixels[].
            byte pixByte, maskByte;
            int x, y, n, result;
            int i = 0;
            for (y = 0; y < height; y++) {
                for (x = 0; x < width / 8; x++) {
                    maskByte = maskBuf[y * bytesPerRow + x];
                    for (n = 7; n >= 0; n--) {
                        if ((maskByte >> n & 1) != 0) {
                            if (bytesPerPixel == 1) {
                                result = colorPalette[0xFF & pixBuf[i]];
                            } else {
                                result = 0xFF000000 |
                                        (pixBuf[i * 4 + 2] & 0xFF) << 16 |
                                        (pixBuf[i * 4 + 1] & 0xFF) << 8 |
                                        (pixBuf[i * 4] & 0xFF);
                            }
                        } else {
                            result = 0;    // Transparent pixel
                        }
                        softCursorPixels[i++] = result;
                    }
                }
                for (n = 7; n >= 8 - width % 8; n--) {
                    if ((maskBuf[y * bytesPerRow + x] >> n & 1) != 0) {
                        if (bytesPerPixel == 1) {
                            result = colorPalette[0xFF & pixBuf[i]];
                        } else {
                            result = 0xFF000000 |
                                    (pixBuf[i * 4 + 2] & 0xFF) << 16 |
                                    (pixBuf[i * 4 + 1] & 0xFF) << 8 |
                                    (pixBuf[i * 4] & 0xFF);
                        }
                    } else {
                        result = 0;        // Transparent pixel
                    }
                    softCursorPixels[i++] = result;
                }
            }

        }

        return softCursorPixels;
    }
}
