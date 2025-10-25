package au.com.darkside.xserver;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Region;

import java.io.IOException;


/**
 * This class implements an X graphics context.
 *
 * @author Matthew Kwan
 */
public class GContext extends Resource {
    private Paint _paint;
    private Font _font = null;
    private Path.FillType _fillType;
    private int[] _attributes;
    private Rect[] _clipRectangles = null;
    private int _foregroundColor = 0xff000000;
    private int _backgroundColor = 0xffffffff;

    private static final int AttrFunction = 0;
    private static final int AttrPlaneMask = 1;
    private static final int AttrForeground = 2;
    private static final int AttrBackground = 3;
    private static final int AttrLineWidth = 4;
    private static final int AttrLineStyle = 5;
    private static final int AttrCapStyle = 6;
    private static final int AttrJoinStyle = 7;
    private static final int AttrFillStyle = 8;
    private static final int AttrFillRule = 9;
    private static final int AttrTile = 10;
    private static final int AttrStipple = 11;
    private static final int AttrTileStippleXOrigin = 12;
    private static final int AttrTileStippleYOrigin = 13;
    private static final int AttrFont = 14;
    private static final int AttrSubwindowMode = 15;
    private static final int AttrGraphicsExposures = 16;
    private static final int AttrClipXOrigin = 17;
    private static final int AttrClipYOrigin = 18;
    private static final int AttrClipMask = 19;
    private static final int AttrDashOffset = 20;
    private static final int AttrDashes = 21;
    private static final int AttrArcMode = 22;

    /**
     * Constructor.
     *
     * @param id      The graphics context's ID.
     * @param xServer The X server.
     * @param client  The client issuing the request.
     */
    public GContext(int id, XServer xServer, Client client) {
        super(GCONTEXT, id, xServer, client);

        _paint = new Paint();
        _attributes = new int[]{3,    // function = Copy
                0xffffffff,    // plane-mask = all ones
                0,    // foreground = 0
                1,    // background = 1
                0,    // line-width = 0
                0,    // line-style = Solid
                1,    // cap-style = Butt
                0,    // join-style = Miter
                0,    // fill-style = Solid
                0,    // fill-rule = EvenOdd
                0,    // tile = foreground-filled pixmap
                0,    // stipple = pixmap filled with ones
                0,    // tile-stipple-x-origin = 0
                0,    // tile-stipple-y-origin = 0
                0,    // font = server-dependent
                0,    // subwindow-mode = ClipByChildren
                1,    // graphics-exposures = True
                0,    // clip-x-origin = 0
                0,    // clip-y-origin = 0
                0,    // clip-mask = None
                0,    // dash-offset = 0
                4,    // dashes = 4 (i.e. the list [4,4])
                1    // arc-mode = PieSlice
        };
    }

    /**
     * Return the GContext's Paint handle.
     *
     * @return The GContext's Paint handle.
     */
    public Paint getPaint() {
        return _paint;
    }

    /**
     * Return the GContext's background color.
     *
     * @return The GContext's background color.
     */
    public int getBackgroundColor() {
        return _backgroundColor;
    }

    /**
     * Return the GContext's foreground color.
     *
     * @return The GContext's foreground color.
     */
    public int getForegroundColor() {
        return _foregroundColor;
    }

    /**
     * Return the fill type.
     *
     * @return The fill type.
     */
    public Path.FillType getFillType() {
        return _fillType;
    }

    /**
     * Return the arc mode.
     * 0 = chord, 1 = pie slice.
     *
     * @return The arc mode.
     */
    public int getArcMode() {
        return _attributes[AttrArcMode];
    }

    /**
     * Return whether to generate graphics exposure events.
     *
     * @return Whether to generate graphics exposure events.
     */
    public boolean getGraphicsExposure() {
        return (_attributes[AttrGraphicsExposures] != 0);
    }

    /**
     * Return the GContext's font.
     *
     * @return The GContext's font.
     */
    public Font getFont() {
        return _font;
    }

    /**
     * Set the GContext's font.
     *
     * @param id The ID of the font.
     * @return True if the ID refers to a valid font.
     */
    public boolean setFont(int id) {
        Resource r = _xServer.getResource(id);

        if (r == null || r.getType() != Resource.FONT) return false;

        _font = (Font) r;
        _paint.setTypeface(_font.getTypeface());
        _paint.setTextSize(_font.getSize());

        return true;
    }

    /**
     * Apply the GContext's clip rectangles to the canvas.
     *
     * @param canvas The canvas to apply the rectangles to.
     */
    public void applyClipRectangles(Canvas canvas) {
        if (_clipRectangles == null) return;

        if (_clipRectangles.length == 0) canvas.clipRect(0, 0, 0, 0);
        else for (Rect r : _clipRectangles){
            canvas.save();
            canvas.clipRect(r);
            canvas.restore();
        }
    }

    /**
     * Process an X request relating to this graphics context.
     *
     * @param client         The remote client.
     * @param opcode         The request's opcode.
     * @param arg            Optional first argument.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    @Override
    public void processRequest(Client client, byte opcode, byte arg, int bytesRemaining) throws IOException {
        InputOutput io = client.getInputOutput();

        switch (opcode) {
            case RequestCode.QueryFont:
            case RequestCode.QueryTextExtents:
                _font.processRequest(client, opcode, arg, bytesRemaining);
                return;
            case RequestCode.ChangeGC:
                processValues(client, opcode, bytesRemaining);
                break;
            case RequestCode.CopyGC:
                if (bytesRemaining != 8) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int id = io.readInt();    // Destination GContext.
                    int mask = io.readInt();    // Value mask.
                    Resource r = _xServer.getResource(id);

                    if (r == null || r.getType() != Resource.GCONTEXT) {
                        ErrorCode.write(client, ErrorCode.GContext, opcode, id);
                    } else {
                        GContext gc = (GContext) r;

                        for (int i = 0; i < 23; i++)
                            if ((mask & (1 << i)) != 0) gc._attributes[i] = _attributes[i];

                        gc.applyValues(null, opcode);
                    }
                }
                break;
            case RequestCode.SetDashes:
                if (bytesRemaining < 4) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    io.readShort();    // Dash offset.

                    int n = io.readShort();    // Length of dashes.
                    int pad = -n & 3;

                    bytesRemaining -= 4;
                    if (bytesRemaining != n + pad)
                        ErrorCode.write(client, ErrorCode.Length, opcode, 0);

                    io.readSkip(n + pad);    // Ignore the dash information.
                }
                break;
            case RequestCode.SetClipRectangles:
                if (bytesRemaining < 4) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int clipXOrigin = (short) io.readShort();
                    int clipYOrigin = (short) io.readShort();

                    bytesRemaining -= 4;
                    if ((bytesRemaining & 7) != 0) {
                        io.readSkip(bytesRemaining);
                        ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                    } else {
                        int i = 0;

                        _clipRectangles = new Rect[bytesRemaining / 8];
                        while (bytesRemaining > 0) {
                            int x = (short) io.readShort();
                            int y = (short) io.readShort();
                            int width = io.readShort();
                            int height = io.readShort();

                            bytesRemaining -= 8;
                            _clipRectangles[i++] = new Rect(x + clipXOrigin, y + clipYOrigin, x + clipXOrigin + width, y + clipYOrigin + height);
                        }
                    }
                }
                break;
            case RequestCode.FreeGC:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    _xServer.freeResource(_id);
                    if (_client != null) _client.freeResource(this);
                }
                break;
            default:
                io.readSkip(bytesRemaining);
                ErrorCode.write(client, ErrorCode.Implementation, opcode, 0);
                break;
        }
    }

    /**
     * Process a CreateGC request.
     *
     * @param xServer        The X server.
     * @param client         The client issuing the request.
     * @param id             The ID of the GContext to create.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    public static void processCreateGCRequest(XServer xServer, Client client, int id, int bytesRemaining) throws IOException {
        GContext gc = new GContext(id, xServer, client);

        if (gc.processValues(client, RequestCode.CreateGC, bytesRemaining)) {
            xServer.addResource(gc);
            client.addResource(gc);
        }
    }

    /**
     * Process a list of GContext attribute values.
     *
     * @param client         The remote client.
     * @param opcode         The opcode being processed.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @return True if the values are all valid.
     * @throws IOException
     */
    private boolean processValues(Client client, byte opcode, int bytesRemaining) throws IOException {
        InputOutput io = client.getInputOutput();

        if (bytesRemaining < 4) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, opcode, 0);
            return false;
        }

        int valueMask = io.readInt();    // Value mask.
        int n = Util.bitcount(valueMask);

        bytesRemaining -= 4;
        if (bytesRemaining != n * 4) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, opcode, 0);
            return false;
        }

        for (int i = 0; i < 23; i++)
            if ((valueMask & (1 << i)) != 0) processValue(io, i);

        return applyValues(client, opcode);
    }

    /**
     * Process a single GContext attribute value.
     *
     * @param io      The input/output stream.
     * @param maskBit The mask bit of the attribute.
     * @throws IOException
     */
    private void processValue(InputOutput io, int maskBit) throws IOException {
        switch (maskBit) {
            case AttrFunction:
            case AttrLineStyle:
            case AttrCapStyle:
            case AttrJoinStyle:
            case AttrFillStyle:
            case AttrFillRule:
            case AttrSubwindowMode:
            case AttrGraphicsExposures:
            case AttrDashes:
            case AttrArcMode:
                _attributes[maskBit] = io.readByte();
                io.readSkip(3);
                break;
            case AttrPlaneMask:
            case AttrForeground:
            case AttrBackground:
            case AttrTile:
            case AttrStipple:
            case AttrFont:
            case AttrClipMask:
                _attributes[maskBit] = io.readInt();
                break;
            case AttrLineWidth:
            case AttrDashOffset:
                _attributes[maskBit] = io.readShort();
                io.readSkip(2);
                break;
            case AttrTileStippleXOrigin:
            case AttrTileStippleYOrigin:
            case AttrClipXOrigin:
            case AttrClipYOrigin:
                _attributes[maskBit] = (short) io.readShort();
                io.readSkip(2);
                break;
        }
    }

    /**
     * Apply the attribute values to the Paint.
     *
     * @param client The remote client.
     * @param opcode The opcode being processed.
     * @return True if the values are all valid.
     * @throws IOException
     */
    private boolean applyValues(Client client, byte opcode) throws IOException {
        boolean ok = true;

        _foregroundColor = _attributes[AttrForeground] | 0xff000000;
        _backgroundColor = _attributes[AttrBackground] | 0xff000000;

        _paint.setColor(_foregroundColor);
        _paint.setStrokeWidth(_attributes[AttrLineWidth]);

        if (_attributes[AttrFunction] == 6)        // XOR.
            _paint.setXfermode(new PorterDuffXfermode(Mode.XOR));
        else _paint.setXfermode(null);

        switch (_attributes[AttrCapStyle]) {
            case 0:    // NotLast
            case 1:    // Butt
                _paint.setStrokeCap(Paint.Cap.BUTT);
                break;
            case 2:    // Round
                _paint.setStrokeCap(Paint.Cap.ROUND);
                break;
            case 3:    // Projecting
                _paint.setStrokeCap(Paint.Cap.SQUARE);
                break;
        }

        switch (_attributes[AttrJoinStyle]) {
            case 0:    // Miter
                _paint.setStrokeJoin(Paint.Join.MITER);
                break;
            case 1:    // Round
                _paint.setStrokeJoin(Paint.Join.ROUND);
                break;
            case 2:    // Bevel
                _paint.setStrokeJoin(Paint.Join.BEVEL);
                break;
        }

        if (_attributes[AttrFillRule] == 1)        // Winding.
            _fillType = Path.FillType.WINDING;
        else    // Defaults to even-odd.
            _fillType = Path.FillType.EVEN_ODD;

        int fid = _attributes[AttrFont];

        if (_font == null || fid == 0) _font = _xServer.getDefaultFont();

        if (fid != 0 && !setFont(fid)) {
            ok = false;
            ErrorCode.write(client, ErrorCode.Font, opcode, fid);
        }

        return ok;
    }
}