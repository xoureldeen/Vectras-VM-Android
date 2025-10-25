package au.com.darkside.xserver;

import java.io.IOException;
import java.net.Socket;
import java.util.Vector;

import au.com.darkside.xserver.Xext.Extensions;

/**
 * This class handles communications with a client.
 *
 * @author Matthew Kwan
 */
public class Client extends Thread {
    public static final int Destroy = 0;
    public static final int RetainPermanent = 1;
    public static final int RetainTemporary = 2;

    private final XServer _xServer;
    private final Socket _socket;
    private final InputOutput _inputOutput;
    private final int _resourceIdBase;
    private final int _resourceIdMask;
    private final Vector<Resource> _resources;
    private int _sequenceNumber = 0;
    private boolean _closeConnection = false;
    private boolean _isConnected = true;
    private int _closeDownMode = Destroy;
    private boolean _imperviousToServerGrabs = false;

    /**
     * Constructor.
     *
     * @param xserver        The X Server.
     * @param socket         The communications socket.
     * @param resourceIdBase The lowest resource ID the client can use.
     * @param resourceIdMask The range of resource IDs the client can use.
     * @throws IOException
     */
    public Client(XServer xserver, Socket socket, int resourceIdBase, int resourceIdMask) throws IOException {
        _xServer = xserver;
        _socket = socket;
        _inputOutput = new InputOutput(socket);
        _resourceIdBase = resourceIdBase;
        _resourceIdMask = resourceIdMask;
        _resources = new Vector<Resource>();
    }

    /**
     * Get the client's close down mode.
     *
     * @return The client's close down mode.
     */
    public int getCloseDownMode() {
        return _closeDownMode;
    }

    /**
     * Return the input/output handle.
     *
     * @return The input/output handle.
     */
    public InputOutput getInputOutput() {
        return _inputOutput;
    }

    /**
     * Get the sequence number of the latest request sent by the client.
     *
     * @return The last-used sequence number.
     */
    public int getSequenceNumber() {
        return _sequenceNumber;
    }

    /**
     * Return whether the client is connected.
     *
     * @return True if the client is connected.
     */
    public boolean isConnected() {
        return _isConnected;
    }

    /**
     * Return whether the client is impervious to server grabs.
     *
     * @return True if impervious.
     */
    public boolean getImperviousToServerGrabs() {
        return _imperviousToServerGrabs;
    }

    /**
     * Set whether the client is impervious to server grabs.
     *
     * @param impervious If true, the client is impervious.
     */
    public void setImperviousToServerGrabs(boolean impervious) {
        _imperviousToServerGrabs = impervious;
    }

    /**
     * Add to the client's list of resources.
     *
     * @param r The resource to add.
     */
    public synchronized void addResource(Resource r) {
        _resources.add(r);
    }

    /**
     * Remove a resource from the client's list.
     *
     * @param id The resource ID.
     */
    public synchronized void freeResource(Resource r) {
        _resources.remove(r);
    }

    /**
     * Run the communications thread.
     */
    public void run() {
        try {
            doComms();
        } catch (IOException e) {
        }

        synchronized (_xServer) {
            close();
        }
    }

    /**
     * Cancel the communications thread.
     */
    public void cancel() {
        _closeConnection = true;
        close();
    }

    /**
     * Close the communications thread and free resources.
     */
    private void close() {
        if (!_isConnected) return;

        _isConnected = false;

        try {
            _inputOutput.close();
            _socket.close();
        } catch (IOException e) {
        }

        // Clear the resources associated with this client.
        if (_closeDownMode == Destroy) for (Resource r : _resources)
            r.delete();

        _resources.clear();
        _xServer.removeClient(this);
    }

    /**
     * Handle communications with the client.
     *
     * @throws IOException
     */
    private void doComms() throws IOException {
        // Read the connection setup.
        int byteOrder = _inputOutput.readByte();

        if (byteOrder == 0x42) _inputOutput.setMSB(true);
        else if (byteOrder == 0x6c) _inputOutput.setMSB(false);
        else return;

        _inputOutput.readByte();    // Unused.
        _inputOutput.readShort();    // Protocol major version.
        _inputOutput.readShort();    // Protocol minor version.

        int nameLength = _inputOutput.readShort();
        int dataLength = _inputOutput.readShort();

        _inputOutput.readShort();    // Unused.

        if (nameLength > 0) {
            _inputOutput.readSkip(nameLength);    // Authorization protocol name.
            _inputOutput.readSkip(-nameLength & 3);    // Padding.
        }

        if (dataLength > 0) {
            _inputOutput.readSkip(dataLength);    // Authorization protocol data.
            _inputOutput.readSkip(-dataLength & 3);    // Padding.
        }

        // Complete the setup.
        final byte[] vendor = _xServer.vendor.getBytes();
        int pad = -vendor.length & 3;
        int extra = 26 + 2 * _xServer.getNumFormats() + (vendor.length + pad) / 4;
        Keyboard kb = _xServer.getKeyboard();

        synchronized (_inputOutput) {
            _inputOutput.writeByte((byte) 1);        // Success.
            _inputOutput.writeByte((byte) 0);        // Unused.
            _inputOutput.writeShort(_xServer.ProtocolMajorVersion);
            _inputOutput.writeShort(_xServer.ProtocolMinorVersion);
            _inputOutput.writeShort((short) extra);    // Length of data.
            _inputOutput.writeInt(_xServer.ReleaseNumber);    // Release number.
            _inputOutput.writeInt(_resourceIdBase);
            _inputOutput.writeInt(_resourceIdMask);
            _inputOutput.writeInt(0);        // Motion buffer size.
            _inputOutput.writeShort((short) vendor.length);    // Vendor length.
            _inputOutput.writeShort((short) 0x7fff);    // Max request length.
            _inputOutput.writeByte((byte) 1);    // Number of screens.
            _inputOutput.writeByte((byte) _xServer.getNumFormats());
            _inputOutput.writeByte((byte) 0);    // Image byte order (0=LSB, 1=MSB).
            _inputOutput.writeByte((byte) 1);    // Bitmap bit order (0=LSB, 1=MSB).
            _inputOutput.writeByte((byte) 8);    // Bitmap format scanline unit.
            _inputOutput.writeByte((byte) 8);    // Bitmap format scanline pad.
            _inputOutput.writeByte((byte) kb.getMinimumKeycode());
            _inputOutput.writeByte((byte) kb.getMaximumKeycode());
            _inputOutput.writePadBytes(4);    // Unused.

            if (vendor.length > 0) {    // Write padded vendor string.
                _inputOutput.writeBytes(vendor, 0, vendor.length);
                _inputOutput.writePadBytes(pad);
            }

            _xServer.writeFormats(_inputOutput);
            _xServer.getScreen().write(_inputOutput);
        }
        _inputOutput.flush();

        while (!_closeConnection) {
            byte opcode = (byte) _inputOutput.readByte();
            byte arg = (byte) _inputOutput.readByte();
            int requestLength = _inputOutput.readShort();
            int bytesRemaining;

            if (requestLength == 0) {    // Handle big requests.
                requestLength = _inputOutput.readInt();
                if (requestLength > 2) bytesRemaining = requestLength * 4 - 8;
                else bytesRemaining = 0;
            } else {
                bytesRemaining = requestLength * 4 - 4;
            }

            // Deal with server grabs.
            while (!_xServer.processingAllowed(this)) {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                }
            }

            synchronized (_xServer) {
                processRequest(opcode, arg, bytesRemaining);
            }
        }
    }

    /**
     * Is it OK to create a resource with the specified ID?
     *
     * @param id The resource ID.
     * @return True if it is OK to create a resource with the ID.
     */
    private boolean validResourceId(int id) {
        return ((id & ~_resourceIdMask) == _resourceIdBase && !_xServer.resourceExists(id));
    }

    /**
     * Process a single request from the client.
     *
     * @param opcode         The request's opcode.
     * @param arg            Optional first argument.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    private void processRequest(byte opcode, byte arg, int bytesRemaining) throws IOException {
        _sequenceNumber++;
        switch (opcode) {
            case RequestCode.CreateWindow:
                if (bytesRemaining < 28) {
                    _inputOutput.readSkip(bytesRemaining);
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    int id = _inputOutput.readInt();    // Window ID.
                    int parent = _inputOutput.readInt();    // Parent.
                    Resource r = _xServer.getResource(parent);

                    bytesRemaining -= 8;
                    if (!validResourceId(id)) {
                        _inputOutput.readSkip(bytesRemaining);
                        ErrorCode.write(this, ErrorCode.IDChoice, opcode, id);
                    } else if (r == null || r.getType() != Resource.WINDOW) {
                        _inputOutput.readSkip(bytesRemaining);
                        ErrorCode.write(this, ErrorCode.Window, opcode, parent);
                    } else {
                        Window w = (Window) r;

                        w.processCreateWindowRequest(_inputOutput, this, _sequenceNumber, id, arg, bytesRemaining);
                    }
                }
                break;
            case RequestCode.ChangeWindowAttributes:
            case RequestCode.GetWindowAttributes:
            case RequestCode.DestroyWindow:
            case RequestCode.DestroySubwindows:
            case RequestCode.ChangeSaveSet:
            case RequestCode.ReparentWindow:
            case RequestCode.MapWindow:
            case RequestCode.MapSubwindows:
            case RequestCode.UnmapWindow:
            case RequestCode.UnmapSubwindows:
            case RequestCode.ConfigureWindow:
            case RequestCode.CirculateWindow:
            case RequestCode.QueryTree:
            case RequestCode.ChangeProperty:
            case RequestCode.DeleteProperty:
            case RequestCode.GetProperty:
            case RequestCode.ListProperties:
            case RequestCode.QueryPointer:
            case RequestCode.GetMotionEvents:
            case RequestCode.TranslateCoordinates:
            case RequestCode.ClearArea:
            case RequestCode.ListInstalledColormaps:
            case RequestCode.RotateProperties:
                if (bytesRemaining < 4) {
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    int id = _inputOutput.readInt();
                    Resource r = _xServer.getResource(id);

                    bytesRemaining -= 4;
                    if (r == null || r.getType() != Resource.WINDOW) {
                        _inputOutput.readSkip(bytesRemaining);
                        ErrorCode.write(this, ErrorCode.Window, opcode, id);
                    } else {
                        r.processRequest(this, opcode, arg, bytesRemaining);
                    }
                }
                break;
            case RequestCode.GetGeometry:
            case RequestCode.CopyArea:
            case RequestCode.CopyPlane:
            case RequestCode.PolyPoint:
            case RequestCode.PolyLine:
            case RequestCode.PolySegment:
            case RequestCode.PolyRectangle:
            case RequestCode.PolyArc:
            case RequestCode.FillPoly:
            case RequestCode.PolyFillRectangle:
            case RequestCode.PolyFillArc:
            case RequestCode.PutImage:
            case RequestCode.GetImage:
            case RequestCode.PolyText8:
            case RequestCode.PolyText16:
            case RequestCode.ImageText8:
            case RequestCode.ImageText16:
            case RequestCode.QueryBestSize:
                if (bytesRemaining < 4) {
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    int id = _inputOutput.readInt();
                    Resource r = _xServer.getResource(id);

                    bytesRemaining -= 4;
                    if (r == null || !r.isDrawable()) {
                        _inputOutput.readSkip(bytesRemaining);
                        ErrorCode.write(this, ErrorCode.Drawable, opcode, id);
                    } else {
                        r.processRequest(this, opcode, arg, bytesRemaining);
                    }
                }
                break;
            case RequestCode.InternAtom:
                Atom.processInternAtomRequest(_xServer, this, arg, bytesRemaining);
                break;
            case RequestCode.GetAtomName:
                Atom.processGetAtomNameRequest(_xServer, this, bytesRemaining);
                break;
            case RequestCode.GetSelectionOwner:
            case RequestCode.SetSelectionOwner:
            case RequestCode.ConvertSelection:
                Selection.processRequest(_xServer, this, opcode, bytesRemaining);
                break;
            case RequestCode.SendEvent:
            case RequestCode.GrabPointer:
            case RequestCode.UngrabPointer:
            case RequestCode.GrabButton:
            case RequestCode.UngrabButton:
            case RequestCode.ChangeActivePointerGrab:
            case RequestCode.GrabKeyboard:
            case RequestCode.UngrabKeyboard:
            case RequestCode.GrabKey:
            case RequestCode.UngrabKey:
            case RequestCode.AllowEvents:
            case RequestCode.SetInputFocus:
            case RequestCode.GetInputFocus:
                _xServer.getScreen().processRequest(_xServer, this, opcode, arg, bytesRemaining);
                break;
            case RequestCode.GrabServer:
                if (bytesRemaining != 0) {
                    _inputOutput.readSkip(bytesRemaining);
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    _xServer.grabServer(this);
                }
                break;
            case RequestCode.UngrabServer:
                if (bytesRemaining != 0) {
                    _inputOutput.readSkip(bytesRemaining);
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    _xServer.ungrabServer(this);
                }
                break;
            case RequestCode.WarpPointer:
            case RequestCode.ChangePointerControl:
            case RequestCode.GetPointerControl:
            case RequestCode.SetPointerMapping:
            case RequestCode.GetPointerMapping:
                _xServer.getPointer().processRequest(_xServer, this, opcode, arg, bytesRemaining);
                break;
            case RequestCode.OpenFont:
                if (bytesRemaining < 8) {
                    _inputOutput.readSkip(bytesRemaining);
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    int id = _inputOutput.readInt();    // Font ID.

                    bytesRemaining -= 4;
                    if (!validResourceId(id)) {
                        _inputOutput.readSkip(bytesRemaining);
                        ErrorCode.write(this, ErrorCode.IDChoice, opcode, id);
                    } else {
                        Font.processOpenFontRequest(_xServer, this, id, bytesRemaining);
                    }
                }
                break;
            case RequestCode.CloseFont:
                if (bytesRemaining != 4) {
                    _inputOutput.readSkip(bytesRemaining);
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    int id = _inputOutput.readInt();
                    Resource r = _xServer.getResource(id);

                    bytesRemaining -= 4;
                    if (r == null || r.getType() != Resource.FONT)
                        ErrorCode.write(this, ErrorCode.Font, opcode, id);
                    else r.processRequest(this, opcode, arg, bytesRemaining);
                }
                break;
            case RequestCode.QueryFont:
            case RequestCode.QueryTextExtents:
                if (bytesRemaining != 4) {
                    _inputOutput.readSkip(bytesRemaining);
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    int id = _inputOutput.readInt();
                    Resource r = _xServer.getResource(id);

                    bytesRemaining -= 4;
                    if (r == null || !r.isFontable()) {
                        _inputOutput.readSkip(bytesRemaining);
                        ErrorCode.write(this, ErrorCode.Font, opcode, id);
                    } else {
                        r.processRequest(this, opcode, arg, bytesRemaining);
                    }
                }
                break;
            case RequestCode.ListFonts:
            case RequestCode.ListFontsWithInfo:
                Font.processListFonts(this, opcode, bytesRemaining);
                break;
            case RequestCode.SetFontPath:
                Font.processSetFontPath(_xServer, this, bytesRemaining);
                break;
            case RequestCode.GetFontPath:
                if (bytesRemaining != 0) {
                    _inputOutput.readSkip(bytesRemaining);
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    Font.processGetFontPath(_xServer, this);
                }
                break;
            case RequestCode.CreatePixmap:
                if (bytesRemaining != 12) {
                    _inputOutput.readSkip(bytesRemaining);
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    int id = _inputOutput.readInt();    // Pixmap ID.
                    int did = _inputOutput.readInt();    // Drawable ID.
                    int width = _inputOutput.readShort();    // Width.
                    int height = _inputOutput.readShort();    // Height.
                    Resource r = _xServer.getResource(did);

                    if (!validResourceId(id)) {
                        ErrorCode.write(this, ErrorCode.IDChoice, opcode, id);
                    } else if (r == null || !r.isDrawable()) {
                        ErrorCode.write(this, ErrorCode.Drawable, opcode, did);
                    } else {
                        try {
                            Pixmap.processCreatePixmapRequest(_xServer, this, id, width, height, arg, r);
                        } catch (OutOfMemoryError e) {
                            ErrorCode.write(this, ErrorCode.Alloc, opcode, 0);
                        }
                    }
                }
                break;
            case RequestCode.FreePixmap:
                if (bytesRemaining != 4) {
                    _inputOutput.readSkip(bytesRemaining);
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    int id = _inputOutput.readInt();
                    Resource r = _xServer.getResource(id);

                    bytesRemaining -= 4;
                    if (r == null || r.getType() != Resource.PIXMAP)
                        ErrorCode.write(this, ErrorCode.Pixmap, opcode, id);
                    else r.processRequest(this, opcode, arg, bytesRemaining);
                }
                break;
            case RequestCode.CreateGC:
                if (bytesRemaining < 12) {
                    _inputOutput.readSkip(bytesRemaining);
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    int id = _inputOutput.readInt();    // GContext ID.
                    int d = _inputOutput.readInt();    // Drawable ID.
                    Resource r = _xServer.getResource(d);

                    bytesRemaining -= 8;
                    if (!validResourceId(id)) {
                        _inputOutput.readSkip(bytesRemaining);
                        ErrorCode.write(this, ErrorCode.IDChoice, opcode, id);
                    } else if (r == null || !r.isDrawable()) {
                        _inputOutput.readSkip(bytesRemaining);
                        ErrorCode.write(this, ErrorCode.Drawable, opcode, d);
                    } else {
                        GContext.processCreateGCRequest(_xServer, this, id, bytesRemaining);
                    }
                }
                break;
            case RequestCode.ChangeGC:
            case RequestCode.CopyGC:
            case RequestCode.SetDashes:
            case RequestCode.SetClipRectangles:
            case RequestCode.FreeGC:
                if (bytesRemaining < 4) {
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    int id = _inputOutput.readInt();
                    Resource r = _xServer.getResource(id);

                    bytesRemaining -= 4;
                    if (r == null || r.getType() != Resource.GCONTEXT) {
                        _inputOutput.readSkip(bytesRemaining);
                        ErrorCode.write(this, ErrorCode.GContext, opcode, id);
                    } else {
                        r.processRequest(this, opcode, arg, bytesRemaining);
                    }
                }
                break;
            case RequestCode.CreateColormap:
                if (bytesRemaining != 12) {
                    _inputOutput.readSkip(bytesRemaining);
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    int id = _inputOutput.readInt();    // Colormap ID.

                    bytesRemaining -= 4;
                    if (!validResourceId(id)) {
                        _inputOutput.readSkip(bytesRemaining);
                        ErrorCode.write(this, ErrorCode.IDChoice, opcode, id);
                    } else {
                        Colormap.processCreateColormapRequest(_xServer, this, id, arg);
                    }
                }
                break;
            case RequestCode.CopyColormapAndFree:
                if (bytesRemaining != 8) {
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    int id1 = _inputOutput.readInt();
                    int id2 = _inputOutput.readInt();
                    Resource r = _xServer.getResource(id2);

                    if (r == null || r.getType() != Resource.COLORMAP)
                        ErrorCode.write(this, ErrorCode.Colormap, opcode, id2);
                    else if (!validResourceId(id1))
                        ErrorCode.write(this, ErrorCode.IDChoice, opcode, id1);
                    else ((Colormap) r).processCopyColormapAndFree(this, id1);
                }
                break;
            case RequestCode.FreeColormap:
            case RequestCode.InstallColormap:
            case RequestCode.UninstallColormap:
            case RequestCode.AllocColor:
            case RequestCode.AllocNamedColor:
            case RequestCode.AllocColorCells:
            case RequestCode.AllocColorPlanes:
            case RequestCode.FreeColors:
            case RequestCode.StoreColors:
            case RequestCode.StoreNamedColor:
            case RequestCode.QueryColors:
            case RequestCode.LookupColor:
                if (bytesRemaining < 4) {
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    int id = _inputOutput.readInt();
                    Resource r = _xServer.getResource(id);

                    bytesRemaining -= 4;
                    if (r == null || r.getType() != Resource.COLORMAP) {
                        _inputOutput.readSkip(bytesRemaining);
                        ErrorCode.write(this, ErrorCode.Colormap, opcode, id);
                    } else {
                        r.processRequest(this, opcode, arg, bytesRemaining);
                    }
                }
                break;
            case RequestCode.CreateCursor:
            case RequestCode.CreateGlyphCursor:
                if (bytesRemaining != 28) {
                    _inputOutput.readSkip(bytesRemaining);
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    int id = _inputOutput.readInt();    // Cursor ID.

                    bytesRemaining -= 4;
                    if (!validResourceId(id)) {
                        _inputOutput.readSkip(bytesRemaining);
                        ErrorCode.write(this, ErrorCode.IDChoice, opcode, id);
                    } else {
                        Cursor.processCreateRequest(_xServer, this, opcode, id, bytesRemaining);
                    }
                }
                break;
            case RequestCode.FreeCursor:
            case RequestCode.RecolorCursor:
                if (bytesRemaining < 4) {
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    int id = _inputOutput.readInt();
                    Resource r = _xServer.getResource(id);

                    bytesRemaining -= 4;
                    if (r == null || r.getType() != Resource.CURSOR) {
                        _inputOutput.readSkip(bytesRemaining);
                        ErrorCode.write(this, ErrorCode.Colormap, opcode, id);
                    } else {
                        r.processRequest(this, opcode, arg, bytesRemaining);
                    }
                }
                break;
            case RequestCode.QueryExtension:
                _xServer.processQueryExtensionRequest(this, bytesRemaining);
                break;
            case RequestCode.ListExtensions:
                if (bytesRemaining != 0) {
                    _inputOutput.readSkip(bytesRemaining);
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    _xServer.writeListExtensions(this);
                }
                break;
            case RequestCode.QueryKeymap:
            case RequestCode.ChangeKeyboardMapping:
            case RequestCode.GetKeyboardMapping:
            case RequestCode.ChangeKeyboardControl:
            case RequestCode.SetModifierMapping:
            case RequestCode.GetModifierMapping:
            case RequestCode.GetKeyboardControl:
            case RequestCode.Bell:
                _xServer.getKeyboard().processRequest(_xServer, this, opcode, arg, bytesRemaining);
                break;
            case RequestCode.SetScreenSaver:
                if (bytesRemaining != 8) {
                    _inputOutput.readSkip(bytesRemaining);
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    int timeout = _inputOutput.readShort();    // Timeout.
                    int interval = _inputOutput.readShort();    // Interval
                    int pb = _inputOutput.readByte();    // Prefer-blanking.
                    int ae = _inputOutput.readByte();    // Allow-exposures.

                    _inputOutput.readSkip(2);    // Unused.
                    _xServer.setScreenSaver(timeout, interval, pb, ae);
                }
                break;
            case RequestCode.GetScreenSaver:
                if (bytesRemaining != 0) {
                    _inputOutput.readSkip(bytesRemaining);
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    _xServer.writeScreenSaver(this);
                }
                break;
            case RequestCode.ChangeHosts:
                _xServer.processChangeHostsRequest(this, arg, bytesRemaining);
                break;
            case RequestCode.ListHosts:
                if (bytesRemaining != 0) {
                    _inputOutput.readSkip(bytesRemaining);
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    _xServer.writeListHosts(this);
                }
                break;
            case RequestCode.SetAccessControl:
                if (bytesRemaining != 0) {
                    _inputOutput.readSkip(bytesRemaining);
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    _xServer.setAccessControl(arg == 1);
                }
                break;
            case RequestCode.SetCloseDownMode:
                processSetCloseDownModeRequest(arg, bytesRemaining);
                break;
            case RequestCode.KillClient:
                processKillClientRequest(bytesRemaining);
                break;
            case RequestCode.ForceScreenSaver:
                if (bytesRemaining != 0) {
                    _inputOutput.readSkip(bytesRemaining);
                    ErrorCode.write(this, ErrorCode.Length, opcode, 0);
                } else {
                    _xServer.getScreen().blank(arg == 1);
                }
                break;
            case RequestCode.NoOperation:
                _inputOutput.readSkip(bytesRemaining);
                break;
            default:    // Opcode not implemented.
                if (opcode < 0) {
                    Extensions.processRequest(_xServer, this, opcode, arg, bytesRemaining);
                } else {
                    _inputOutput.readSkip(bytesRemaining);
                    ErrorCode.write(this, ErrorCode.Implementation, opcode, 0);
                }
                break;
        }
    }

    /**
     * Process a SetCloseDownMode request.
     *
     * @param mode           The close down mode.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    public void processSetCloseDownModeRequest(int mode, int bytesRemaining) throws IOException {
        if (bytesRemaining != 0) {
            _inputOutput.readSkip(bytesRemaining);
            ErrorCode.write(this, ErrorCode.Length, RequestCode.SetCloseDownMode, 0);
            return;
        }

        _closeDownMode = mode;
        for (Resource r : _resources)
            r.setCloseDownMode(mode);
    }

    /**
     * Process a KillClient request.
     *
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    public void processKillClientRequest(int bytesRemaining) throws IOException {
        if (bytesRemaining != 4) {
            _inputOutput.readSkip(bytesRemaining);
            ErrorCode.write(this, ErrorCode.Length, RequestCode.KillClient, 0);
            return;
        }

        int id = _inputOutput.readInt();
        Client client = null;

        if (id != 0) {
            Resource r = _xServer.getResource(id);

            if (r == null) {
                ErrorCode.write(this, ErrorCode.Length, RequestCode.KillClient, 0);
                return;
            }

            client = r.getClient();
        }

        if (client != null && client._isConnected) client._closeConnection = true;
        else if (client == null || client._closeDownMode != Destroy)
            _xServer.destroyClientResources(client);
    }
}
