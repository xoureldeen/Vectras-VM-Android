package au.com.darkside.xserver;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;


/**
 * This class implements an X Windows cursor.
 *
 * @author Matthew Kwan
 */
public class Cursor extends Resource {
    private final int _hotspotX;
    private final int _hotspotY;
    private Bitmap _bitmap;
    private int _foregroundColor;
    private int _backgroundColor;

    private static final int _glyphs[][] = {{R.drawable.xc_x_cursor, 7, 7}, {R.drawable.xc_arrow, 14, 1}, {R.drawable.xc_based_arrow_down, 4, 10}, {R.drawable.xc_based_arrow_up, 4, 10}, {R.drawable.xc_boat, 14, 4}, {R.drawable.xc_bogosity, 7, 7}, {R.drawable.xc_bottom_left_corner, 1, 14}, {R.drawable.xc_bottom_right_corner, 14, 14}, {R.drawable.xc_bottom_side, 7, 14}, {R.drawable.xc_bottom_tee, 8, 10}, {R.drawable.xc_box_spiral, 8, 8}, {R.drawable.xc_center_ptr, 5, 1}, {R.drawable.xc_circle, 8, 8}, {R.drawable.xc_clock, 6, 3}, {R.drawable.xc_coffee_mug, 7, 9}, {R.drawable.xc_cross, 7, 7}, {R.drawable.xc_cross_reverse, 7, 7}, {R.drawable.xc_crosshair, 7, 7}, {R.drawable.xc_diamond_cross, 7, 7}, {R.drawable.xc_dot, 6, 6}, {R.drawable.xc_dotbox, 7, 6}, {R.drawable.xc_double_arrow, 6, 8}, {R.drawable.xc_draft_large, 14, 0}, {R.drawable.xc_draft_small, 14, 0}, {R.drawable.xc_draped_box, 7, 6}, {R.drawable.xc_exchange, 7, 7}, {R.drawable.xc_fleur, 8, 8}, {R.drawable.xc_gobbler, 14, 3}, {R.drawable.xc_gumby, 2, 0}, {R.drawable.xc_hand1, 12, 0}, {R.drawable.xc_hand2, 0, 1}, {R.drawable.xc_heart, 6, 8}, {R.drawable.xc_icon, 8, 8}, {R.drawable.xc_iron_cross, 8, 7}, {R.drawable.xc_left_ptr, 1, 1}, {R.drawable.xc_left_side, 1, 7}, {R.drawable.xc_left_tee, 1, 8}, {R.drawable.xc_leftbutton, 8, 8}, {R.drawable.xc_ll_angle, 1, 10}, {R.drawable.xc_lr_angle, 10, 10}, {R.drawable.xc_man, 14, 5}, {R.drawable.xc_middlebutton, 8, 8}, {R.drawable.xc_mouse, 4, 1}, {R.drawable.xc_pencil, 11, 15}, {R.drawable.xc_pirate, 7, 12}, {R.drawable.xc_plus, 5, 6}, {R.drawable.xc_question_arrow, 5, 8}, {R.drawable.xc_right_ptr, 8, 1}, {R.drawable.xc_right_side, 14, 7}, {R.drawable.xc_right_tee, 10, 8}, {R.drawable.xc_rightbutton, 8, 8}, {R.drawable.xc_rtl_logo, 7, 7}, {R.drawable.xc_sailboat, 8, 0}, {R.drawable.xc_sb_down_arrow, 4, 15}, {R.drawable.xc_sb_h_double_arrow, 7, 4}, {R.drawable.xc_sb_left_arrow, 0, 4}, {R.drawable.xc_sb_right_arrow, 15, 4}, {R.drawable.xc_sb_up_arrow, 4, 0}, {R.drawable.xc_sb_v_double_arrow, 4, 7}, {R.drawable.xc_shuttle, 11, 0}, {R.drawable.xc_sizing, 8, 8}, {R.drawable.xc_spider, 6, 7}, {R.drawable.xc_spraycan, 10, 2}, {R.drawable.xc_star, 7, 7}, {R.drawable.xc_target, 7, 7}, {R.drawable.xc_tcross, 7, 7}, {R.drawable.xc_top_left_arrow, 1, 1}, {R.drawable.xc_top_left_corner, 1, 1}, {R.drawable.xc_top_right_corner, 14, 1}, {R.drawable.xc_top_side, 7, 1}, {R.drawable.xc_top_tee, 8, 1}, {R.drawable.xc_trek, 4, 0}, {R.drawable.xc_ul_angle, 1, 1}, {R.drawable.xc_umbrella, 8, 2}, {R.drawable.xc_ur_angle, 10, 1}, {R.drawable.xc_watch, 15, 9}, {R.drawable.xc_xterm, 4, 8}};

    /**
     * Constructor for a pixmap cursor.
     *
     * @param id              The server cursor ID.
     * @param xServer         The X server.
     * @param client          The client issuing the request.
     * @param p               Cursor pixmap.
     * @param mp              Mask pixmap. May be null.
     * @param x               Hotspot X coordinate.
     * @param y               Hotspot Y coordinate.
     * @param foregroundColor Foreground color of the cursor.
     * @param backgroundColor Foreground color of the cursor.
     */
    public Cursor(int id, XServer xServer, Client client, Pixmap p, Pixmap mp, int x, int y, int foregroundColor, int backgroundColor) {
        super(CURSOR, id, xServer, client);

        _hotspotX = x;
        _hotspotY = y;
        _foregroundColor = foregroundColor;
        _backgroundColor = backgroundColor;

        Bitmap bm = p.getDrawable().getBitmap();
        int width = bm.getWidth();
        int height = bm.getHeight();
        int[] pixels = new int[width * height];

        bm.getPixels(pixels, 0, width, 0, 0, width, height);
        if (mp == null) {
            for (int i = 0; i < pixels.length; i++) {
                if (pixels[i] == 0xffffffff) pixels[i] = foregroundColor;
                else pixels[i] = backgroundColor;
            }
        } else {
            Bitmap mbm = mp.getDrawable().getBitmap();
            int[] mask = new int[width * height];

            mbm.getPixels(mask, 0, width, 0, 0, width, height);
            for (int i = 0; i < pixels.length; i++) {
                if (mask[i] != 0xffffffff) pixels[i] = 0;
                else if (pixels[i] == 0xffffffff) pixels[i] = foregroundColor;
                else pixels[i] = backgroundColor;
            }
        }

        _bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    /**
     * Constructor for a glyph cursor.
     * This functions just assumes the caller wants one of the 77 predefined
     * cursors from the "cursor" font, so the sourceFont, maskFont, and
     * maskChar are all ignored.
     *
     * @param id              The server cursor ID.
     * @param xServer         The X server.
     * @param client          The client issuing the request.
     * @param sourceFont      Font to use for the cursor character.
     * @param maskFont        Font for the mask character. May be null.
     * @param sourceChar      Character to use as the cursor.
     * @param maskChar        Character to use as the mask.
     * @param foregroundColor Foreground color of the cursor.
     * @param backgroundColor Foreground color of the cursor.
     */
    public Cursor(int id, XServer xServer, Client client, Font sourceFont, Font maskFont, int sourceChar, int maskChar, int foregroundColor, int backgroundColor) {
        super(CURSOR, id, xServer, client);

        sourceChar /= 2;
        if (sourceChar < 0 || sourceChar >= _glyphs.length) sourceChar = 0;

        if (maskChar == 32) {
            _bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
            _bitmap.eraseColor(0);
        } else {
            _bitmap = BitmapFactory.decodeResource(xServer.getContext().getResources(), _glyphs[sourceChar][0]);
        }

        _foregroundColor = 0xff000000;
        _backgroundColor = 0xffffffff;
        setColor(foregroundColor, backgroundColor);

        _hotspotX = _glyphs[sourceChar][1];
        _hotspotY = _glyphs[sourceChar][2];
    }

    /**
     * Return the cursor's bitmap.
     *
     * @return The cursor's bitmap.
     */
    public Bitmap getBitmap() {
        return _bitmap;
    }

    /**
     * Return the X coordinate of the cursor's hotspot.
     *
     * @return The X coordinate of the cursor's hotspot.
     */
    public int getHotspotX() {
        return _hotspotX;
    }

    /**
     * Return the Y coordinate of the cursor's hotspot.
     *
     * @return The Y coordinate of the cursor's hotspot.
     */
    public int getHotspotY() {
        return _hotspotY;
    }

    /**
     * Set the foreground and background colors of the cursor.
     *
     * @param fg Foreground color.
     * @param bg Background color.
     */
    private void setColor(int fg, int bg) {
        if (fg == _foregroundColor && bg == _backgroundColor) return;

        int width = _bitmap.getWidth();
        int height = _bitmap.getHeight();
        int[] pixels = new int[width * height];

        _bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < pixels.length; i++) {
            int pix = pixels[i];

            if (pix == _foregroundColor) pixels[i] = fg;
            else if (pix == _backgroundColor) pixels[i] = bg;
        }

        _bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        _foregroundColor = fg;
        _backgroundColor = bg;
    }

    /**
     * Process an X request relating to this cursor.
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
            case RequestCode.FreeCursor:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    _xServer.freeResource(_id);
                    if (_client != null) _client.freeResource(this);
                }
                break;
            case RequestCode.RecolorCursor:
                if (bytesRemaining != 12) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int fgRed = io.readShort();
                    int fgGreen = io.readShort();
                    int fgBlue = io.readShort();
                    int bgRed = io.readShort();
                    int bgGreen = io.readShort();
                    int bgBlue = io.readShort();

                    setColor(Colormap.fromParts16(fgRed, fgGreen, fgBlue), Colormap.fromParts16(bgRed, bgGreen, bgBlue));
                }
                break;
            default:
                io.readSkip(bytesRemaining);
                ErrorCode.write(client, ErrorCode.Implementation, opcode, _id);
                break;
        }
    }

    /**
     * Process a create request.
     *
     * @param xServer        The X server.
     * @param client         The client issuing the request.
     * @param opcode         The request opcode.
     * @param id             The ID of the cursor to create.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    public static void processCreateRequest(XServer xServer, Client client, byte opcode, int id, int bytesRemaining) throws IOException {
        InputOutput io = client.getInputOutput();

        if (opcode == RequestCode.CreateCursor) {
            int sid = io.readInt();    // Source pixmap ID.
            int mid = io.readInt();    // Mask pixmap ID.
            int fgRed = io.readShort();
            int fgGreen = io.readShort();
            int fgBlue = io.readShort();
            int bgRed = io.readShort();
            int bgGreen = io.readShort();
            int bgBlue = io.readShort();
            short x = (short) io.readShort();
            short y = (short) io.readShort();
            Resource r = xServer.getResource(sid);
            Resource mr = null;

            if (r == null || r.getType() != PIXMAP) {
                ErrorCode.write(client, ErrorCode.Pixmap, opcode, sid);
                return;
            } else if (mid != 0) {
                mr = xServer.getResource(mid);
                if (mr == null || mr.getType() != PIXMAP) {
                    ErrorCode.write(client, ErrorCode.Pixmap, opcode, mid);
                    return;
                }
            }

            Pixmap p = (Pixmap) r;
            Pixmap mp = (Pixmap) mr;

            if (p.getDepth() != 1) {
                ErrorCode.write(client, ErrorCode.Match, opcode, sid);
                return;
            } else if (mp != null) {
                if (mp.getDepth() != 1) {
                    ErrorCode.write(client, ErrorCode.Match, opcode, mid);
                    return;
                }

                Bitmap bm1 = p.getDrawable().getBitmap();
                Bitmap bm2 = mp.getDrawable().getBitmap();

                if (bm1.getWidth() != bm2.getWidth() || bm1.getHeight() != bm2.getHeight()) {
                    ErrorCode.write(client, ErrorCode.Match, opcode, mid);
                    return;
                }
            }

            int fg = Colormap.fromParts16(fgRed, fgGreen, fgBlue);
            int bg = Colormap.fromParts16(bgRed, bgGreen, bgBlue);
            Cursor c = new Cursor(id, xServer, client, p, mp, x, y, fg, bg);

            xServer.addResource(c);
            client.addResource(c);
        } else if (opcode == RequestCode.CreateGlyphCursor) {
            int sid = io.readInt();    // Source font ID.
            int mid = io.readInt();    // Mask font ID.
            int sourceChar = io.readShort();    // Source char.
            int maskChar = io.readShort();    // Mask char.
            int fgRed = io.readShort();
            int fgGreen = io.readShort();
            int fgBlue = io.readShort();
            int bgRed = io.readShort();
            int bgGreen = io.readShort();
            int bgBlue = io.readShort();
            Resource r = xServer.getResource(sid);
            Resource mr = null;

            if (r == null || r.getType() != FONT) {
                ErrorCode.write(client, ErrorCode.Font, opcode, sid);
                return;
            } else if (mid != 0) {
                mr = xServer.getResource(mid);
                if (mr == null || mr.getType() != FONT) {
                    ErrorCode.write(client, ErrorCode.Font, opcode, mid);
                    return;
                }
            }

            int fg = Colormap.fromParts16(fgRed, fgGreen, fgBlue);
            int bg = Colormap.fromParts16(bgRed, bgGreen, bgBlue);
            Cursor c = new Cursor(id, xServer, client, (Font) r, (Font) mr, sourceChar, maskChar, fg, bg);

            xServer.addResource(c);
            client.addResource(c);
        }
    }
}
