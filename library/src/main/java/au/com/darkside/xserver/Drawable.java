package au.com.darkside.xserver;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;

import java.io.IOException;


/**
 * This class implements an X drawable.
 *
 * @author Matthew Kwan
 */
public class Drawable {
    private final Bitmap _bitmap;
    private final Canvas _canvas;
    private final int _depth;
    private Bitmap _backgroundBitmap;
    private int _backgroundColor;
    private boolean[] _shapeMask = null;

    private static final byte BITMAP_FORMAT = 0;
    private static final byte XY_PIXMAP_FORMAT = 1;
    private static final byte Z_PIXMAP_FORMAT = 2;

    /**
     * Constructor.
     *
     * @param width    The drawable width.
     * @param height   The drawable height.
     * @param depth    The drawable depth.
     * @param bgbitmap Background bitmap. Can be null.
     * @param bgcolor  Background color.
     */
    public Drawable(int width, int height, int depth, Bitmap bgbitmap, int bgcolor) {
        _bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        _canvas = new Canvas(_bitmap);
        _depth = depth;
        _backgroundBitmap = bgbitmap;
        _backgroundColor = bgcolor;
    }

    /**
     * Return the drawable's width.
     *
     * @return The drawable's width.
     */
    public int getWidth() {
        return _bitmap.getWidth();
    }

    /**
     * Return the drawable's height.
     *
     * @return The drawable's height.
     */
    public int getHeight() {
        return _bitmap.getHeight();
    }

    /**
     * Return the drawable's depth.
     *
     * @return The drawable's depth.
     */
    public int getDepth() {
        return _depth;
    }

    /**
     * Return the drawable's bitmap.
     *
     * @return The drawable's bitmap.
     */
    public Bitmap getBitmap() {
        return _bitmap;
    }

    /**
     * Set the drawable's background color.
     *
     * @param color The background color.
     */
    public void setBackgroundColor(int color) {
        _backgroundColor = color;
    }

    /**
     * Set the drawable's background bitmap.
     *
     * @param bitmap The background bitmap.
     */
    public void setBackgroundBitmap(Bitmap bitmap) {
        _backgroundBitmap = bitmap;
    }

    /**
     * Process an X request relating to this drawable.
     *
     * @param xServer        The X server.
     * @param client         The remote client.
     * @param id             The ID of the pixmap or window using this drawable.
     * @param opcode         The request's opcode.
     * @param arg            Optional first argument.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @return True if the drawable has been changed.
     * @throws IOException
     */
    public boolean processRequest(XServer xServer, Client client, int id, byte opcode, byte arg, int bytesRemaining) throws IOException {
        boolean changed = false;
        InputOutput io = client.getInputOutput();

        switch (opcode) {
            case RequestCode.CopyArea:
                if (bytesRemaining != 20) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int did = io.readInt();    // Dest drawable.
                    int gcid = io.readInt();    // GC.
                    short sx = (short) io.readShort();    // Src X.
                    short sy = (short) io.readShort();    // Src Y.
                    short dx = (short) io.readShort();    // Dst X.
                    short dy = (short) io.readShort();    // Dst Y.
                    int width = io.readShort();    // Width.
                    int height = io.readShort();    // Height.
                    Resource r1 = xServer.getResource(did);
                    Resource r2 = xServer.getResource(gcid);

                    if (r1 == null || !r1.isDrawable()) {
                        ErrorCode.write(client, ErrorCode.Drawable, opcode, did);
                    } else if (r2 == null || r2.getType() != Resource.GCONTEXT) {
                        ErrorCode.write(client, ErrorCode.GContext, opcode, gcid);
                    } else if (width > 0 && height > 0) {
                        copyArea(sx, sy, width, height, r1, dx, dy, (GContext) r2);
                    }
                }
                break;
            case RequestCode.CopyPlane:
                if (bytesRemaining != 24) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, id);
                } else {
                    int did = io.readInt();    // Dest drawable.
                    int gcid = io.readInt();    // GC.
                    short sx = (short) io.readShort();    // Src X.
                    short sy = (short) io.readShort();    // Src Y.
                    short dx = (short) io.readShort();    // Dst X.
                    short dy = (short) io.readShort();    // Dst Y.
                    int width = io.readShort();    // Width.
                    int height = io.readShort();    // Height.
                    int bitPlane = io.readInt();    // Bit plane.
                    Resource r1 = xServer.getResource(did);
                    Resource r2 = xServer.getResource(gcid);

                    if (r1 == null || !r1.isDrawable()) {
                        ErrorCode.write(client, ErrorCode.Drawable, opcode, did);
                    } else if (r2 == null || r2.getType() != Resource.GCONTEXT) {
                        ErrorCode.write(client, ErrorCode.GContext, opcode, gcid);
                    } else {
                        if (_depth != 32)
                            copyPlane(sx, sy, width, height, bitPlane, r1, dx, dy, (GContext) r2);
                        else copyArea(sx, sy, width, height, r1, dx, dy, (GContext) r2);
                    }
                }
                break;
            case RequestCode.GetImage:
                if (bytesRemaining != 12) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    processGetImageRequest(client, arg);
                }
                break;
            case RequestCode.QueryBestSize:
                if (bytesRemaining != 4) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int width = io.readShort();    // Width.
                    int height = io.readShort();    // Height.

                    synchronized (io) {
                        Util.writeReplyHeader(client, (byte) 0);
                        io.writeInt(0);    // Reply length.
                        io.writeShort((short) width);    // Width.
                        io.writeShort((short) height);    // Height.
                        io.writePadBytes(20);    // Unused.
                    }
                    io.flush();
                }
                break;
            case RequestCode.PolyPoint:
            case RequestCode.PolyLine:
            case RequestCode.PolySegment:
            case RequestCode.PolyRectangle:
            case RequestCode.PolyArc:
            case RequestCode.FillPoly:
            case RequestCode.PolyFillRectangle:
            case RequestCode.PolyFillArc:
            case RequestCode.PutImage:
            case RequestCode.PolyText8:
            case RequestCode.PolyText16:
            case RequestCode.ImageText8:
            case RequestCode.ImageText16:
                if (bytesRemaining < 4) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int gcid = io.readInt();    // GContext.
                    Resource r = xServer.getResource(gcid);

                    bytesRemaining -= 4;
                    if (r == null || r.getType() != Resource.GCONTEXT) {
                        io.readSkip(bytesRemaining);
                        ErrorCode.write(client, ErrorCode.GContext, opcode, 0);

                    } else {
                        changed = processGCRequest(xServer, client, id, (GContext) r, opcode, arg, bytesRemaining);
                    }
                }
                break;
            default:
                io.readSkip(bytesRemaining);
                ErrorCode.write(client, ErrorCode.Implementation, opcode, 0);
                break;
        }

        return changed;
    }

    /**
     * Process a GetImage request.
     *
     * @param client The remote client.
     * @param format 1=XYPixmap, 2=ZPixmap.
     * @throws IOException
     */
    private void processGetImageRequest(Client client, byte format) throws IOException {
        InputOutput io = client.getInputOutput();
        short x = (short) io.readShort();    // X.
        short y = (short) io.readShort();    // Y.
        int width = io.readShort();    // Width.
        int height = io.readShort();    // Height.
        int planeMask = io.readInt();    // Plane mask.
        int wh = width * height;
        int n, pad;
        int[] pixels;
        byte[] bytes = null;

        if (x < 0 || y < 0 || x + width > _bitmap.getWidth() || y + height > _bitmap.getHeight()) {
            ErrorCode.write(client, ErrorCode.Match, RequestCode.GetImage, 0);
            return;
        }

        try {
            pixels = new int[wh];
        } catch (OutOfMemoryError e) {
            ErrorCode.write(client, ErrorCode.Alloc, RequestCode.GetImage, 0);
            return;
        }

        _bitmap.getPixels(pixels, 0, width, x, y, width, height);

        if (format == Z_PIXMAP_FORMAT) {
            n = wh * 3;
        } else {    // XY_PIXMAP_FORMAT is the only other valid value.
            int planes = Util.bitcount(planeMask);
            int rightPad = -width & 7;
            int xmax = width + rightPad;
            int offset = 0;

            n = planes * height * (width + rightPad) / 8;

            try {
                bytes = new byte[n];
            } catch (OutOfMemoryError e) {
                ErrorCode.write(client, ErrorCode.Alloc, RequestCode.GetImage, 0);
                return;
            }

            for (int plane = 31; plane >= 0; plane--) {
                int bit = 1 << plane;

                if ((planeMask & bit) == 0) continue;

                byte b = 0;

                for (int yi = 0; yi < height; yi++) {
                    for (int xi = 0; xi < xmax; xi++) {
                        b <<= 1;
                        if (xi < width && (pixels[yi * width + xi] & bit) != 0) b |= 1;

                        if ((xi & 7) == 7) {
                            bytes[offset++] = b;
                            b = 0;
                        }
                    }
                }
            }
        }

        pad = -n & 3;

        synchronized (io) {
            Util.writeReplyHeader(client, (byte) 32);
            io.writeInt((n + pad) / 4);    // Reply length.
            io.writeInt(0);    // Visual ID.
            io.writePadBytes(20);    // Unused.

            if (format == 2) {
                for (int i = 0; i < wh; i++) {
                    n = pixels[i] & planeMask;
                    io.writeByte((byte) (n & 0xff));
                    io.writeByte((byte) ((n >> 8) & 0xff));
                    io.writeByte((byte) ((n >> 16) & 0xff));
                }
            } else {
                io.writeBytes(bytes, 0, n);
            }

            io.writePadBytes(pad);    // Unused.
        }
        io.flush();
    }

    /**
     * Clear the entire drawable.
     */
    public void clear() {
        if (_backgroundBitmap == null || _backgroundBitmap.isRecycled()) {
            _bitmap.eraseColor(_backgroundColor);
        } else {
            int width = _bitmap.getWidth();
            int height = _bitmap.getHeight();
            int dx = _backgroundBitmap.getWidth();
            int dy = _backgroundBitmap.getHeight();

            for (int y = 0; y < height; y += dy)
                for (int x = 0; x < width; x += dx)
                    _canvas.drawBitmap(_backgroundBitmap, x, y, null);
        }
    }

    /**
     * Clear a rectangular region of the drawable.
     *
     * @param x      X coordinate of the rectangle.
     * @param y      Y coordinate of the rectangle.
     * @param width  Width of the rectangle.
     * @param height Height of the rectangle.
     */
    public void clearArea(int x, int y, int width, int height) {
        Rect r = new Rect(x, y, x + width, y + height);
        Paint paint = new Paint();

        if (_backgroundBitmap == null || _backgroundBitmap.isRecycled()) {
            paint.setColor(_backgroundColor);
            paint.setStyle(Paint.Style.FILL);
            _canvas.drawRect(r, paint);
        } else {
            int bw = _bitmap.getWidth();
            int bh = _bitmap.getHeight();
            int dx = _backgroundBitmap.getWidth();
            int dy = _backgroundBitmap.getHeight();

            _canvas.save();
            _canvas.clipRect(r);

            for (int iy = 0; iy < bh; iy += dy) {
                if (iy >= r.bottom) break;
                if (iy + dy < r.top) continue;

                for (int ix = 0; ix < bw; ix += dx) {
                    if (ix >= r.right) break;
                    if (iy + dy < r.left) continue;

                    _canvas.drawBitmap(_backgroundBitmap, ix, iy, null);
                }
            }

            _canvas.restore();
        }
    }

    /**
     * Copy a rectangle from this drawable to another.
     *
     * @param sx     X coordinate of this rectangle.
     * @param sy     Y coordinate of this rectangle.
     * @param width  Width of the rectangle.
     * @param height Height of the rectangle.
     * @param dr     The pixmap or window to draw the rectangle in.
     * @param dx     The destination X coordinate.
     * @param dy     The destination Y coordinate.
     * @param gc     The GContext.
     * @throws IOException
     */
    private void copyArea(int sx, int sy, int width, int height, Resource dr, int dx, int dy, GContext gc) throws IOException {
        Drawable dst;

        if (dr.getType() == Resource.PIXMAP) dst = ((Pixmap) dr).getDrawable();
        else dst = ((Window) dr).getDrawable();

        if (sx < 0) {
            width += sx;
            dx -= sx;
            sx = 0;
        }

        if (sy < 0) {
            height += sy;
            dy -= sy;
            sy = 0;
        }

        if (sx + width > _bitmap.getWidth()) width = _bitmap.getWidth() - sx;

        if (sy + height > _bitmap.getHeight()) height = _bitmap.getHeight() - sy;

        if (width <= 0 || height <= 0) return;

        Bitmap bm = Bitmap.createBitmap(_bitmap, sx, sy, width, height);

        dst._canvas.drawBitmap(bm, dx, dy, gc.getPaint());

        if (dr.getType() == Resource.WINDOW) ((Window) dr).invalidate(dx, dy, width, height);

        if (gc.getGraphicsExposure())
            EventCode.sendNoExposure(gc.getClient(), dr, RequestCode.CopyArea);
    }

    /**
     * Copy a rectangle from a plane of this drawable to another rectangle.
     *
     * @param sx       X coordinate of this rectangle.
     * @param sy       Y coordinate of this rectangle.
     * @param width    Width of the rectangle.
     * @param height   Height of the rectangle.
     * @param bitPlane The bit plane being copied.
     * @param dr       The pixmap or window to draw the rectangle in.
     * @param dx       The destination X coordinate.
     * @param dy       The destination Y coordinate.
     * @param gc       The GContext.
     * @throws IOException
     */
    private void copyPlane(int sx, int sy, int width, int height, int bitPlane, Resource dr, int dx, int dy, GContext gc) throws IOException {
        Drawable dst;

        if (dr.getType() == Resource.PIXMAP) dst = ((Pixmap) dr).getDrawable();
        else dst = ((Window) dr).getDrawable();

        int fg = (_depth == 1) ? 0xffffffff : gc.getForegroundColor();
        int bg = (_depth == 1) ? 0 : gc.getBackgroundColor();
        int[] pixels = new int[width * height];

        _bitmap.getPixels(pixels, 0, width, sx, sy, width, height);
        for (int i = 0; i < pixels.length; i++)
            pixels[i] = ((pixels[i] & bitPlane) != 0) ? fg : bg;

        Bitmap pixelsBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        pixelsBmp.setPixels(pixels, 0, width, 0, 0, width, height);
        dst._canvas.drawBitmap(pixelsBmp, dx, dy, gc.getPaint());

        if (dr.getType() == Resource.WINDOW) ((Window) dr).invalidate(dx, dy, width, height);

        if (gc.getGraphicsExposure())
            EventCode.sendNoExposure(gc.getClient(), dr, RequestCode.CopyPlane);
    }

    /**
     * Draw text at the specified location, on top of a bounding rectangle
     * drawn in the background color.
     *
     * @param s  The string to write.
     * @param x  X coordinate.
     * @param y  Y coordinate.
     * @param gc Graphics context for drawing the text.
     */
    private void drawImageText(String s, int x, int y, GContext gc) {
        Paint paint = gc.getPaint();
        Font font = gc.getFont();
        Rect rect = new Rect();

        font.getTextBounds(s, x, y, rect);
        paint.setColor(gc.getBackgroundColor());
        paint.setStyle(Paint.Style.FILL);
        _canvas.drawRect(rect, paint);

        paint.setColor(gc.getForegroundColor());
        _canvas.drawText(s, x, y, paint);
    }

    /**
     * Process an X request relating to this drawable using the
     * GContext provided.
     *
     * @param xServer        The X server.
     * @param client         The remote client.
     * @param id             The ID of the pixmap or window using this drawable.
     * @param gc             The GContext to use for drawing.
     * @param opcode         The request's opcode.
     * @param arg            Optional first argument.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @return True if the drawable is modified.
     * @throws IOException
     */
    public boolean processGCRequest(XServer xServer, Client client, int id, GContext gc, byte opcode, byte arg, int bytesRemaining) throws IOException {
        InputOutput io = client.getInputOutput();
        Paint paint = gc.getPaint();
        boolean changed = false;
        int originalColor = paint.getColor();

        _canvas.save();
        gc.applyClipRectangles(_canvas);

        switch (opcode) {
            case RequestCode.PolyPoint:
                if ((bytesRemaining & 3) != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    float[] points = new float[bytesRemaining / 2];
                    int i = 0;

                    while (bytesRemaining > 0) {
                        float p = (short) io.readShort();

                        bytesRemaining -= 2;
                        if (arg == 0 || i < 2)    // Relative to origin.
                            points[i] = p;
                        else points[i] = points[i - 2] + p;    // Rel to previous.
                        i++;
                    }

                    try {
                        _canvas.drawPoints(points, paint);
                    } catch (UnsupportedOperationException e) {
                        for (i = 0; i < points.length; i += 2)
                            _canvas.drawPoint(points[i], points[i + 1], paint);
                    }
                    changed = true;
                }
                break;
            case RequestCode.PolyLine:
                if ((bytesRemaining & 3) != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    Path path = new Path();
                    int i = 0;

                    while (bytesRemaining > 0) {
                        float x = (short) io.readShort();
                        float y = (short) io.readShort();

                        bytesRemaining -= 4;
                        if (i == 0) path.moveTo(x, y);
                        else if (arg == 0)    // Relative to origin.
                            path.lineTo(x, y);
                        else    // Relative to previous.
                            path.rLineTo(x, y);
                        i++;
                    }
                    paint.setStyle(Paint.Style.STROKE);
                    _canvas.drawPath(path, paint);
                    changed = true;
                }
                break;
            case RequestCode.PolySegment:
                if ((bytesRemaining & 7) != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    float[] points = new float[bytesRemaining / 2];
                    int i = 0;

                    while (bytesRemaining > 0) {
                        points[i++] = (short) io.readShort();
                        bytesRemaining -= 2;
                    }

                    _canvas.drawLines(points, paint);
                    changed = true;
                }
                break;
            case RequestCode.PolyRectangle:
            case RequestCode.PolyFillRectangle:
                if ((bytesRemaining & 7) != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    if (opcode == RequestCode.PolyRectangle) paint.setStyle(Paint.Style.STROKE);
                    else paint.setStyle(Paint.Style.FILL);

                    while (bytesRemaining > 0) {
                        float x = (short) io.readShort();
                        float y = (short) io.readShort();
                        float width = io.readShort();
                        float height = io.readShort();

                        bytesRemaining -= 8;
                        _canvas.drawRect(x, y, x + width, y + height, paint);
                        changed = true;
                    }
                }
                break;
            case RequestCode.FillPoly:
                if (bytesRemaining < 4 || (bytesRemaining & 3) != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    io.readByte();        // Shape.

                    int mode = io.readByte();    // Coordinate mode.
                    Path path = new Path();
                    int i = 0;

                    io.readSkip(2);    // Unused.
                    bytesRemaining -= 4;

                    while (bytesRemaining > 0) {
                        float x = (short) io.readShort();
                        float y = (short) io.readShort();

                        bytesRemaining -= 4;
                        if (i == 0) path.moveTo(x, y);
                        else if (mode == 0)    // Relative to origin.
                            path.lineTo(x, y);
                        else    // Relative to previous.
                            path.rLineTo(x, y);
                        i++;
                    }

                    path.close();
                    path.setFillType(gc.getFillType());
                    paint.setStyle(Paint.Style.FILL);
                    _canvas.drawPath(path, paint);
                    changed = true;
                }
                break;
            case RequestCode.PolyArc:
            case RequestCode.PolyFillArc:
                if ((bytesRemaining % 12) != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    boolean useCenter = false;

                    if (opcode == RequestCode.PolyArc) {
                        paint.setStyle(Paint.Style.STROKE);
                    } else {
                        paint.setStyle(Paint.Style.FILL);
                        if (gc.getArcMode() == 1)        // Pie slice.
                            useCenter = true;
                    }

                    while (bytesRemaining > 0) {
                        float x = (short) io.readShort();
                        float y = (short) io.readShort();
                        float width = io.readShort();
                        float height = io.readShort();
                        float angle1 = (short) io.readShort();
                        float angle2 = (short) io.readShort();
                        RectF r = new RectF(x, y, x + width, y + height);

                        bytesRemaining -= 12;
                        _canvas.drawArc(r, angle1 / -64.0f, angle2 / -64.0f, useCenter, paint);
                        changed = true;
                    }
                }
                break;
            case RequestCode.PutImage:
                changed = processPutImage(client, gc, arg, bytesRemaining);
                break;
            case RequestCode.PolyText8:
            case RequestCode.PolyText16:
                changed = processPolyText(client, gc, opcode, bytesRemaining);
                break;
            case RequestCode.ImageText8:
                if (bytesRemaining != 4 + arg + (-arg & 3)) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int x = (short) io.readShort();
                    int y = (short) io.readShort();
                    int pad = -arg & 3;
                    byte[] bytes = new byte[arg];

                    io.readBytes(bytes, 0, arg);
                    io.readSkip(pad);
                    drawImageText(new String(bytes), x, y, gc);
                    changed = true;
                }
                break;
            case RequestCode.ImageText16:
                if (bytesRemaining != 4 + 2 * arg + (-(2 * arg) & 3)) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int x = (short) io.readShort();
                    int y = (short) io.readShort();
                    int pad = (-2 * arg) & 3;
                    char[] chars = new char[arg];

                    for (int i = 0; i < arg; i++) {
                        int b1 = io.readByte();
                        int b2 = io.readByte();

                        chars[i] = (char) ((b1 << 8) | b2);
                    }

                    io.readSkip(pad);
                    drawImageText(new String(chars), x, y, gc);
                    changed = true;
                }
                break;
            default:
                io.readSkip(bytesRemaining);
                ErrorCode.write(client, ErrorCode.Implementation, opcode, 0);
                break;
        }

        if (_depth == 1) paint.setColor(originalColor);

        _canvas.restore();        // Undo any clip rectangles.

        return changed;
    }

    /**
     * Process a PutImage request.
     *
     * @param client         The remote client.
     * @param gc             The GContext to use for drawing.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @return True if the drawable is modified.
     * @throws IOException
     */
    private boolean processPutImage(Client client, GContext gc, byte format, int bytesRemaining) throws IOException {
        InputOutput io = client.getInputOutput();

        if (bytesRemaining < 12) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, RequestCode.PutImage, 0);
            return false;
        }

        int width = io.readShort();
        int height = io.readShort();
        float dstX = (short) io.readShort();
        float dstY = (short) io.readShort();
        int leftPad = io.readByte();
        int depth = io.readByte();
        int n, pad, rightPad;

        io.readSkip(2);        // Unused.
        bytesRemaining -= 12;

        boolean badMatch = false;

        if (format == BITMAP_FORMAT) {
            if (depth != 1) badMatch = true;
        } else if (format == XY_PIXMAP_FORMAT) {
            if (depth != _depth) badMatch = true;
        } else if (format == Z_PIXMAP_FORMAT) {
            if (depth != _depth || leftPad != 0) badMatch = true;
        } else {    // Invalid format.
            badMatch = true;
        }

        if (badMatch) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Match, RequestCode.PutImage, 0);
            return false;
        }

        boolean isShapeMask = false;

        if (format == Z_PIXMAP_FORMAT) {
            rightPad = 0;
            if (depth == 32) {
                n = 3 * width * height;
            } else {
                n = (width * height + 7) / 8;
                if (bytesRemaining != n + (-n & 3)) {
                    isShapeMask = true;
                    n = (width + 1) / 2 * height;
                }
            }
        } else {    // XYPixmap or Bitmap.
            rightPad = -(width + leftPad) & 7;
            n = ((width + leftPad + rightPad) * height * depth + 7) / 8;
        }
        pad = -n & 3;

        if (bytesRemaining != n + pad) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, RequestCode.PutImage, 0);
            return false;
        }

        int[] colors;

        try {
            colors = new int[width * height];
        } catch (OutOfMemoryError e) {
            ErrorCode.write(client, ErrorCode.Alloc, RequestCode.PutImage, 0);
            return false;
        }

        if (format == BITMAP_FORMAT) {
            int fg = gc.getForegroundColor();
            int bg = gc.getBackgroundColor();
            int offset = 0;
            int count = 0;
            int x = 0;
            int y = 0;
            int mask = 128;
            int val = 0;

            for (; ; ) {
                if ((count++ & 7) == 0) {
                    val = io.readByte();
                    mask = 128;
                }

                if (x >= leftPad && x < leftPad + width)
                    colors[offset++] = ((val & mask) == 0) ? bg : fg;

                mask >>= 1;
                if (++x == leftPad + width + rightPad) {
                    x = 0;
                    if (++y == height) break;
                }
            }
        } else if (format == XY_PIXMAP_FORMAT) {
            int planeBit = 1 << (depth - 1);

            for (int i = 0; i < depth; i++) {
                int offset = 0;
                int count = 0;
                int x = 0;
                int y = 0;
                int mask = 128;
                int val = 0;

                for (; ; ) {
                    if ((count++ & 7) == 0) {
                        val = io.readByte();
                        mask = 128;
                    }

                    if (x >= leftPad && x < leftPad + width)
                        colors[offset++] |= ((val & mask) == 0) ? 0 : planeBit;

                    mask >>= 1;
                    if (++x == leftPad + width + rightPad) {
                        x = 0;
                        if (++y == height) break;
                    }
                }

                planeBit >>= 1;
            }
        } else if (depth == 32) {    // 32-bit ZPixmap.
            boolean useShapeMask = (_shapeMask != null && colors.length == _shapeMask.length);

            for (int i = 0; i < colors.length; i++) {
                int b = io.readByte();
                int g = io.readByte();
                int r = io.readByte();
                int alpha = (useShapeMask && !_shapeMask[i]) ? 0 : 0xff000000;

                colors[i] = alpha | (r << 16) | (g << 8) | b;
            }

            if (useShapeMask) _shapeMask = null;
        } else if (isShapeMask) {    // ZPixmap, depth = 1, shape mask.
            _shapeMask = new boolean[colors.length];
            io.readShapeMask(_shapeMask, width, height);
            io.readSkip(pad);

            return false;    // Don't redraw.
        } else {    // ZPixmap with depth = 1.
            int fg = gc.getForegroundColor();
            int bg = gc.getBackgroundColor();
            boolean[] bits = new boolean[colors.length];

            io.readBits(bits, 0, colors.length);

            for (int i = 0; i < colors.length; i++)
                colors[i] = bits[i] ? fg : bg;
        }

        io.readSkip(pad);
        Bitmap colorsBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        colorsBmp.setPixels(colors, 0, width, 0, 0, width, height);
        _canvas.drawBitmap(colorsBmp, dstX, dstY, gc.getPaint());

        return true;
    }

    /**
     * Process a PolyText8 or PolyText16 request.
     *
     * @param client         The remote client.
     * @param gc             The GContext to use for drawing.
     * @param opcode         The request's opcode.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @return True if the drawable is modified.
     * @throws IOException
     */
    private boolean processPolyText(Client client, GContext gc, byte opcode, int bytesRemaining) throws IOException {
        InputOutput io = client.getInputOutput();

        if (bytesRemaining < 4) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, opcode, 0);
            return false;
        }

        float x = (short) io.readShort();
        float y = (short) io.readShort();

        bytesRemaining -= 4;
        while (bytesRemaining > 1) {
            int length = io.readByte();
            int minBytes;

            bytesRemaining--;
            if (length == 255)        // Font change indicator.
                minBytes = 4;
            else if (opcode == RequestCode.PolyText8) minBytes = 1 + length;
            else minBytes = 1 + length * 2;

            if (bytesRemaining < minBytes) {
                io.readSkip(bytesRemaining);
                ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                return false;
            }

            if (length == 255) {    // Font change indicator.
                int fid = 0;

                for (int i = 0; i < 4; i++)
                    fid = (fid << 8) | io.readByte();

                bytesRemaining -= 4;
                if (!gc.setFont(fid)) ErrorCode.write(client, ErrorCode.Font, opcode, fid);
            } else {    // It's a string.
                int delta = io.readByte();
                String s;

                bytesRemaining--;
                if (opcode == RequestCode.PolyText8) {
                    byte[] bytes = new byte[length];

                    io.readBytes(bytes, 0, length);
                    bytesRemaining -= length;
                    s = new String(bytes);
                } else {
                    char[] chars = new char[length];

                    for (int i = 0; i < length; i++) {
                        int b1 = io.readByte();
                        int b2 = io.readByte();

                        chars[i] = (char) ((b1 << 8) | b2);
                    }

                    bytesRemaining -= length * 2;
                    s = new String(chars);
                }

                Paint paint = gc.getPaint();

                x += delta;
                _canvas.drawText(s, x, y, paint);
                x += paint.measureText(s);
            }
        }
        io.readSkip(bytesRemaining);

        return true;
    }
}