package au.com.darkside.xserver.Xext;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import au.com.darkside.xserver.Client;
import au.com.darkside.xserver.Drawable;
import au.com.darkside.xserver.ErrorCode;
import au.com.darkside.xserver.InputOutput;
import au.com.darkside.xserver.Pixmap;
import au.com.darkside.xserver.Util;
import au.com.darkside.xserver.Window;
import au.com.darkside.xserver.XServer;

/**
 * Handles requests related to the X SHAPE extension.
 *
 * @author mkwan
 */
public class XShape {
    public static final byte EventBase = 76;
    public static final byte KindBounding = 0;
    public static final byte KindClip = 1;
    public static final byte KindInput = 2;

    private static final byte ShapeQueryVersion = 0;
    private static final byte ShapeRectangles = 1;
    private static final byte ShapeMask = 2;
    private static final byte ShapeCombine = 3;
    private static final byte ShapeOffset = 4;
    private static final byte ShapeQueryExtents = 5;
    private static final byte ShapeSelectInput = 6;
    private static final byte ShapeInputSelected = 7;
    private static final byte ShapeGetRectangles = 8;

    private static final byte OpSet = 0;
    private static final byte OpUnion = 1;
    private static final byte OpIntersect = 2;
    private static final byte OpSubtract = 3;
    private static final byte OpInvert = 4;

    /**
     * Process a request relating to the X SHAPE extension.
     *
     * @param xServer        The X server.
     * @param client         The remote client.
     * @param opcode         The request's opcode.
     * @param arg            Optional first argument.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    public static void processRequest(XServer xServer, Client client, byte opcode, byte arg, int bytesRemaining) throws java.io.IOException {
        InputOutput io = client.getInputOutput();

        switch (arg) {
            case ShapeQueryVersion:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.writeWithMinorOpcode(client, ErrorCode.Length, arg, opcode, 0);
                } else {
                    synchronized (io) {
                        Util.writeReplyHeader(client, arg);
                        io.writeInt(0);    // Reply length.
                        io.writeShort((short) 1);    // Shape major.
                        io.writeShort((short) 1);    // Shape minor.
                        io.writePadBytes(20);
                    }
                    io.flush();
                }
                break;
            case ShapeRectangles:
                if (bytesRemaining < 12) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.writeWithMinorOpcode(client, ErrorCode.Length, arg, opcode, 0);
                } else {
                    byte shapeOp = (byte) io.readByte();
                    byte shapeKind = (byte) io.readByte();

                    io.readByte();    // Ordering.
                    io.readSkip(1);

                    int wid = io.readInt();
                    int x = io.readShort();
                    int y = io.readShort();
                    Window w = (Window) xServer.getResource(wid);

                    bytesRemaining -= 12;

                    int nr = bytesRemaining / 8;
                    Region r = (nr == 0) ? null : new Region();

                    for (int i = 0; i < nr; i++) {
                        int rx = io.readShort();
                        int ry = io.readShort();
                        int rw = io.readShort();
                        int rh = io.readShort();

                        r.op(new Rect(rx, ry, rx + rw, ry + rh), Region.Op.UNION);
                        bytesRemaining -= 8;
                    }

                    if (bytesRemaining != 0)    // Oops!
                        io.readSkip(bytesRemaining);

                    regionOperate(w, shapeKind, r, shapeOp, x, y);
                    if (shapeKind != KindInput && w.isViewable()) w.invalidate();
                }
                break;
            case ShapeMask:
                if (bytesRemaining != 16) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.writeWithMinorOpcode(client, ErrorCode.Length, arg, opcode, 0);
                } else {
                    byte shapeOp = (byte) io.readByte();
                    byte shapeKind = (byte) io.readByte();

                    io.readSkip(2);

                    int wid = io.readInt();
                    int x = io.readShort();
                    int y = io.readShort();
                    int pid = io.readInt();    // Pixmap ID.
                    Window w = (Window) xServer.getResource(wid);
                    Pixmap p = (pid == 0) ? null : (Pixmap) xServer.getResource(pid);
                    Region r = (p == null) ? null : createRegion(p);

                    regionOperate(w, shapeKind, r, shapeOp, x, y);
                    if (shapeKind != KindInput && w.isViewable()) w.invalidate();
                }
                break;
            case ShapeCombine:
                if (bytesRemaining != 16) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.writeWithMinorOpcode(client, ErrorCode.Length, arg, opcode, 0);
                } else {
                    byte shapeOp = (byte) io.readByte();
                    byte dstKind = (byte) io.readByte();
                    byte srcKind = (byte) io.readByte();

                    io.readSkip(1);

                    int dwid = io.readInt();
                    int x = io.readShort();
                    int y = io.readShort();
                    int swid = io.readInt();
                    Window sw = (Window) xServer.getResource(swid);
                    Window dw = (Window) xServer.getResource(dwid);
                    Region sr = sw.getShapeRegion(srcKind);
                    Rect irect = sw.getIRect();

                    x -= irect.left;    // Make region coordinates relative.
                    y -= irect.top;

                    regionOperate(dw, dstKind, sr, shapeOp, x, y);
                    if (dstKind != KindInput && dw.isViewable()) dw.invalidate();
                }
                break;
            case ShapeOffset:
                if (bytesRemaining != 12) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.writeWithMinorOpcode(client, ErrorCode.Length, arg, opcode, 0);
                } else {
                    byte shapeKind = (byte) io.readByte();

                    io.readSkip(3);

                    int wid = io.readInt();
                    int x = io.readShort();
                    int y = io.readShort();
                    Window w = (Window) xServer.getResource(wid);
                    Region r = w.getShapeRegion(shapeKind);

                    if (r != null && (x != 0 || y != 0)) {
                        r.translate(x, y);
                        w.sendShapeNotify(shapeKind);
                        if (shapeKind != KindInput && w.isViewable()) w.invalidate();
                    }
                }
                break;
            case ShapeQueryExtents:
                if (bytesRemaining != 4) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.writeWithMinorOpcode(client, ErrorCode.Length, arg, opcode, 0);
                } else {
                    int wid = io.readInt();
                    Window w = (Window) xServer.getResource(wid);
                    boolean bs = w.isBoundingShaped();
                    boolean cs = w.isClipShaped();
                    Rect orect;
                    Rect irect;

                    if (bs) orect = w.getShapeRegion(KindBounding).getBounds();
                    else orect = w.getORect();

                    if (cs) irect = w.getShapeRegion(KindClip).getBounds();
                    else irect = w.getIRect();

                    synchronized (io) {
                        Util.writeReplyHeader(client, arg);
                        io.writeInt(0);
                        io.writeByte((byte) (bs ? 1 : 0));    // Bounding shaped?
                        io.writeByte((byte) (cs ? 1 : 0));    // Clip shaped?
                        io.writePadBytes(2);
                        io.writeShort((short) orect.left);
                        io.writeShort((short) orect.top);
                        io.writeShort((short) orect.width());
                        io.writeShort((short) orect.height());
                        io.writeShort((short) irect.left);
                        io.writeShort((short) irect.top);
                        io.writeShort((short) irect.width());
                        io.writeShort((short) irect.height());
                        io.writePadBytes(4);
                    }
                    io.flush();
                }
                break;
            case ShapeSelectInput:
                if (bytesRemaining != 8) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.writeWithMinorOpcode(client, ErrorCode.Length, arg, opcode, 0);
                } else {
                    int wid = io.readInt();
                    boolean enable = (io.readByte() == 1);

                    io.readSkip(3);

                    Window w = (Window) xServer.getResource(wid);

                    if (enable) w.addShapeSelectInput(client);
                    else w.removeShapeSelectInput(client);
                }
                break;
            case ShapeInputSelected:
                if (bytesRemaining != 4) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.writeWithMinorOpcode(client, ErrorCode.Length, arg, opcode, 0);
                } else {
                    int wid = io.readInt();
                    Window w = (Window) xServer.getResource(wid);
                    boolean enabled = w.shapeSelectInputEnabled(client);

                    synchronized (io) {
                        Util.writeReplyHeader(client, (byte) (enabled ? 1 : 0));
                        io.writeInt(0);    // Reply length.
                        io.writePadBytes(24);
                    }
                    io.flush();
                }
                break;
            case ShapeGetRectangles:
                if (bytesRemaining != 8) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.writeWithMinorOpcode(client, ErrorCode.Length, arg, opcode, 0);
                } else {
                    int wid = io.readInt();
                    byte shapeKind = (byte) io.readByte();

                    io.readSkip(3);

                    Window w = (Window) xServer.getResource(wid);
                    Region r = w.getShapeRegion(shapeKind);
                    Rect irect = w.getIRect();
                    byte ordering = 0;    // Unsorted.
                    List<Rect> rectangles = rectanglesFromRegion(r);
                    int nr = rectangles.size();

                    synchronized (io) {
                        Util.writeReplyHeader(client, ordering);
                        io.writeInt(2 * nr);    // Reply length.
                        io.writeInt(nr);
                        io.writePadBytes(20);

                        for (Rect rect : rectangles) {
                            io.writeShort((short) (rect.left - irect.left));
                            io.writeShort((short) (rect.top - irect.top));
                            io.writeShort((short) rect.width());
                            io.writeShort((short) rect.height());
                        }
                    }

                    io.flush();
                }
                break;
            default:
                io.readSkip(bytesRemaining);
                ErrorCode.write(client, ErrorCode.Implementation, opcode, 0);
                break;
        }
    }

    /**
     * Carry out a shape operation on a region.
     *
     * @param w         The destination window to operate on.
     * @param shapeKind The type of shape in the destination window.
     * @param sr        Source region.
     * @param shapeOp   Operation to carry out on the regions.
     * @param x         X offset to apply to the source region.
     * @param y         Y offset to apply to the source region.
     */
    private static void regionOperate(Window w, byte shapeKind, Region sr, byte shapeOp, int x, int y) {
        if (sr != null) {    // Apply (x, y) offset.
            Region r = new Region();
            Rect irect = w.getIRect();

            sr.translate(x + irect.left, y + irect.top, r);
            sr = r;
        }

        Region dr = w.getShapeRegion(shapeKind);

        switch (shapeOp) {
            case OpSet:
                break;
            case OpUnion:
                if (sr == null || dr == null) sr = null;
                else sr.op(dr, Region.Op.UNION);
                break;
            case OpIntersect:
                if (sr == null) sr = dr;
                else if (dr != null) sr.op(dr, Region.Op.INTERSECT);
                break;
            case OpSubtract:    // Subtract source region from dest region.
                if (sr == null) sr = new Region();    // Empty region.
                else if (dr == null) sr.op(w.getORect(), Region.Op.DIFFERENCE);
                else sr.op(dr, Region.Op.DIFFERENCE);
                break;
            case OpInvert:    // Subtract dest region from source region.
                if (dr == null) {
                    sr = new Region();    // Empty region.
                } else if (sr == null) {
                    sr = new Region(w.getORect());
                    sr.op(dr, Region.Op.REVERSE_DIFFERENCE);
                } else {
                    sr.op(dr, Region.Op.REVERSE_DIFFERENCE);
                }
                break;
            default:
                return;
        }

        w.setShapeRegion(shapeKind, sr);
        w.sendShapeNotify(shapeKind);
    }

    /**
     * Return a list of rectangles that when combined make up the region.
     *
     * @param r The region.
     * @return
     */
    private static List<Rect> rectanglesFromRegion(Region r) {
        ArrayList<Rect> rl = new ArrayList<Rect>();

        if (r != null && !r.isEmpty()) {
            if (r.isRect()) rl.add(r.getBounds());
            else extractRectangles(r, r.getBounds(), rl);
        }

        return rl;
    }

    /**
     * Recursively break the region contained in the rectangle into
     * rectangles entirely contained in the rectangle.
     *
     * @param r    Region being broken into rectangles.
     * @param rect Part of the region being analyzed..
     * @param rl   Return list of rectangles.
     */
    private static void extractRectangles(Region r, Rect rect, ArrayList<Rect> rl) {
        int rs = regionRectIntersection(r, rect);

        if (rs == 0)    // No intersection with rect.
            return;

        if (rs == 1) {    // Full intersection with rect.
            rl.add(rect);
            return;
        }

        int rw = rect.width();
        int rh = rect.height();

        if (rw > rh) {    // Split the rectangle horizontally.
            int cx = rect.left + rw / 2;

            extractRectangles(r, new Rect(rect.left, rect.top, cx, rect.bottom), rl);
            extractRectangles(r, new Rect(cx, rect.top, rect.right, rect.bottom), rl);
        } else {    // Split it vertically.
            int cy = rect.top + rh / 2;

            extractRectangles(r, new Rect(rect.left, rect.top, rect.right, cy), rl);
            extractRectangles(r, new Rect(rect.left, cy, rect.right, rect.bottom), rl);
        }
    }

    /**
     * Check how a region intersects with a rectangle.
     *
     * @param r    The region.
     * @param rect The rectangle.
     * @return 0 = no overlap; 1 = full overlap; -1 = partial overlap.
     */
    private static int regionRectIntersection(final Region r, final Rect rect) {
        if (r.quickReject(rect)) return 0;

        int icount = 0;
        int ocount = 0;

        for (int y = rect.top; y < rect.bottom; y++) {
            for (int x = rect.left; x < rect.right; x++)
                if (r.contains(x, y)) icount++;
                else ocount++;

            if (icount > 0 && ocount > 0) return -1;
        }

        if (icount == 0) return 0;
        else if (ocount == 0) return 1;

        return -1;
    }

    /**
     * Create a region using the non-zero pixels in the pixmap.
     *
     * @param p The pixmap.
     * @return A region equivalent to the non-zero pixels.
     */
    private static Region createRegion(Pixmap p) {
        Drawable d = p.getDrawable();
        Region r = new Region();

        extractRegion(r, d.getBitmap(), new Rect(0, 0, d.getWidth(), d.getHeight()));

        return r;
    }

    /**
     * Recursively break the image contained in the rectangle into
     * rectangles containing non-zero pixels.
     *
     * @param region Returned region.
     * @param bitmap Bitmap where the pixels appear.
     * @param rect   Rectangle containing the pixels.
     */
    private static void extractRegion(Region region, Bitmap bitmap, Rect rect) {
        int nzp = checkNonZeroPixels(bitmap, rect);

        if (nzp == 1)    // Empty.
            return;

        int rw = rect.width();
        int rh = rect.height();

        if (nzp == 2) {    // All non-zero. We have a rectangle.
            region.op(rect, Region.Op.UNION);
            return;
        }

        if (rw > rh) {    // Split the rectangle horizontally.
            int cx = rect.left + rw / 2;

            extractRegion(region, bitmap, new Rect(rect.left, rect.top, cx, rect.bottom));
            extractRegion(region, bitmap, new Rect(cx, rect.top, rect.right, rect.bottom));
        } else {    // Split it vertically.
            int cy = rect.top + rh / 2;

            extractRegion(region, bitmap, new Rect(rect.left, rect.top, rect.right, cy));
            extractRegion(region, bitmap, new Rect(rect.left, cy, rect.right, rect.bottom));
        }
    }

    /**
     * Check the number of non-zero pixels contained in the rectangle.
     * Return a bit mask indicating whether all the pixels are non-zero,
     * none of them, or a mix.
     *
     * @param bitmap The bitmap containing the pixels.
     * @param rect   The rectangle.
     * @return 1 = no pixels set; 2 = all pixels set; 0 = some pixels set
     */
    private static int checkNonZeroPixels(Bitmap bitmap, Rect rect) {
        final int width = rect.width();
        final int height = rect.height();
        int[] pixels = new int[width];
        int mask = 3;

        for (int i = 0; i < height; i++) {
            bitmap.getPixels(pixels, 0, width, rect.left, rect.top + i, width, 1);

            for (int p : pixels) {
                mask &= (p != 0xff000000) ? 2 : 1;
                if (mask == 0) return 0;
            }
        }

        return mask;
    }
}