package au.com.darkside.xserver;

import java.io.IOException;


/**
 * This class implements an X atom.
 *
 * @author Matthew KWan
 */
public class Atom {
    private static final String[] _predefinedAtoms = {"PRIMARY", "SECONDARY", "CLIPBOARD", "ARC", "ATOM", "BITMAP", "CARDINAL", "COLORMAP", "CURSOR", "CUT_BUFFER0", "CUT_BUFFER1", "CUT_BUFFER2", "CUT_BUFFER3", "CUT_BUFFER4", "CUT_BUFFER5", "CUT_BUFFER6", "CUT_BUFFER7", "DRAWABLE", "FONT", "INTEGER", "PIXMAP", "POINT", "RECTANGLE", "RESOURCE_MANAGER", "RGB_COLOR_MAP", "RGB_BEST_MAP", "RGB_BLUE_MAP", "RGB_DEFAULT_MAP", "RGB_GRAY_MAP", "RGB_GREEN_MAP", "RGB_RED_MAP", "STRING", "VISUALID", "WINDOW", "WM_COMMAND", "WM_HINTS", "WM_CLIENT_MACHINE", "WM_ICON_NAME", "WM_ICON_SIZE", "WM_NAME", "WM_NORMAL_HINTS", "WM_SIZE_HINTS", "WM_ZOOM_HINTS", "MIN_SPACE", "NORM_SPACE", "MAX_SPACE", "END_SPACE", "SUPERSCRIPT_X", "SUPERSCRIPT_Y", "SUBSCRIPT_X", "SUBSCRIPT_Y", "UNDERLINE_POSITION", "UNDERLINE_THICKNESS", "STRIKEOUT_ASCENT", "STRIKEOUT_DESCENT", "ITALIC_ANGLE", "X_HEIGHT", "QUAD_WIDTH", "WEIGHT", "POINT_SIZE", "RESOLUTION", "COPYRIGHT", "NOTICE", "FONT_NAME", "FAMILY_NAME", "FULL_NAME", "CAP_HEIGHT", "WM_CLASS", "WM_TRANSIENT_FOR", "UTF8_STRING" };

    private final int _id;
    private final String _name;

    /**
     * Constructor.
     *
     * @param id The atom's ID.
     */
    public Atom(int id, String name) {
        _id = id;
        _name = name;
    }

    /**
     * Register the predefined atoms with the X server.
     *
     * @param xServer
     */
    public static void registerPredefinedAtoms(XServer xServer) {
        for (int i = 0; i < _predefinedAtoms.length; i++)
            xServer.addAtom(new Atom(i + 1, _predefinedAtoms[i]));
    }

    /**
     * Return the number of predefined atoms.
     *
     * @return The number of predefined atoms.
     */
    public static int numPredefinedAtoms() {
        return _predefinedAtoms.length;
    }

    /**
     * Return the atom's ID.
     *
     * @return The atom's ID.
     */
    public int getId() {
        return _id;
    }

    /**
     * Return the atom's name.
     *
     * @return The atom's name.
     */
    public String getName() {
        return _name;
    }

    /**
     * Process a GetAtomName request.
     *
     * @param xServer        The X server.
     * @param client         The remote client.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    public static void processGetAtomNameRequest(XServer xServer, Client client, int bytesRemaining) throws IOException {
        InputOutput io = client.getInputOutput();

        if (bytesRemaining != 4) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, RequestCode.GetAtomName, 0);
            return;
        }

        int id = io.readInt();
        Atom a = xServer.getAtom(id);

        if (a == null) {
            ErrorCode.write(client, ErrorCode.Atom, RequestCode.GetAtomName, id);
            return;
        }

        byte[] bytes = a._name.getBytes();
        int length = bytes.length;
        int pad = -length & 3;

        synchronized (io) {
            Util.writeReplyHeader(client, (byte) 0);
            io.writeInt((length + pad) / 4);    // Reply length.
            io.writeShort((short) length);    // Name length.
            io.writePadBytes(22);    // Unused.
            io.writeBytes(bytes, 0, length);    // Name.
            io.writePadBytes(pad);    // Unused.
        }
        io.flush();
    }

    /**
     * Process an InternAtom request.
     * Return or create an atom with the specified name.
     *
     * @param xServer        The X server.
     * @param client         The remote client.
     * @param arg            Optional first argument.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    public static void processInternAtomRequest(XServer xServer, Client client, byte arg, int bytesRemaining) throws IOException {
        InputOutput io = client.getInputOutput();

        if (bytesRemaining < 4) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, RequestCode.InternAtom, 0);
            return;
        }

        boolean onlyIfExists = (arg != 0);
        int n = io.readShort();    // Length of name.
        int pad = -n & 3;

        io.readSkip(2);    // Unused.
        bytesRemaining -= 4;

        if (bytesRemaining != n + pad) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, RequestCode.InternAtom, 0);
            return;
        }

        byte[] name = new byte[n];

        io.readBytes(name, 0, n);    // The atom name.
        io.readSkip(pad);    // Unused.

        int id = 0;
        String s = new String(name);
        Atom a = xServer.findAtom(s);

        if (a != null) {
            id = a.getId();
        } else if (!onlyIfExists) {
            a = new Atom(xServer.nextFreeAtomId(), s);
            xServer.addAtom(a);
            id = a.getId();
        }

        synchronized (io) {
            Util.writeReplyHeader(client, (byte) 0);
            io.writeInt(0);    // Reply length.
            io.writeInt(id);    // The atom ID.
            io.writePadBytes(20);    // Unused.
        }
        io.flush();
    }
}
