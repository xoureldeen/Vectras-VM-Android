package au.com.darkside.xserver;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;

import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import au.com.darkside.xserver.Xext.XShape;
import au.com.darkside.xserver.Xext.Extensions;

/**
 * This class implements an X window.
 *
 * @author Matthew Kwan
 */
public class Window extends Resource {
    private final ScreenView _screen;
    private Window _parent;
    private Rect _orect;
    private Rect _irect;
    private Region _boundingShapeRegion = null;
    private Region _clipShapeRegion = null;
    private Region _inputShapeRegion = null;
    private Vector<Client> _shapeSelectInput;
    private Drawable _drawable;
    private Colormap _colormap;
    private Cursor _cursor = null;
    private int[] _attributes;
    private int _borderWidth;
    private final boolean _inputOnly;
    private boolean _overrideRedirect;
    private boolean _isServerWindow = false;
    private boolean _hardwareAccelerated = false;
    private final Vector<Window> _children;
    private final Hashtable<Integer, Property> _properties;
    private final Set<PassiveButtonGrab> _passiveButtonGrabs;
    private final Set<PassiveKeyGrab> _passiveKeyGrabs;
    private boolean _isMapped = false;
    private boolean _exposed = false;
    private int _visibility = NotViewable;
    private Bitmap _backgroundBitmap = null;
    private int _eventMask = 0;
    private final Hashtable<Client, Integer> _clientMasks;

    private static final int Unobscured = 0;
    private static final int PartiallyObscured = 1;
    private static final int FullyObscured = 2;
    private static final int NotViewable = 3;

    private static final int AttrBackgroundPixmap = 0;
    private static final int AttrBackgroundPixel = 1;
    private static final int AttrBorderPixmap = 2;
    private static final int AttrBorderPixel = 3;
    private static final int AttrBitGravity = 4;
    private static final int AttrWinGravity = 5;
    private static final int AttrBackingStore = 6;
    private static final int AttrBackingPlanes = 7;
    private static final int AttrBackingPixel = 8;
    private static final int AttrOverrideRedirect = 9;
    private static final int AttrSaveUnder = 10;
    private static final int AttrEventMask = 11;
    private static final int AttrDoNotPropagateMask = 12;
    private static final int AttrColormap = 13;
    private static final int AttrCursor = 14;

    private static final int WinGravityUnmap = 0;
    private static final int WinGravityNorthWest = 1;
    private static final int WinGravityNorth = 2;
    private static final int WinGravityNorthEast = 3;
    private static final int WinGravityWest = 4;
    private static final int WinGravityCenter = 5;
    private static final int WinGravityEast = 6;
    private static final int WinGravitySouthWest = 7;
    private static final int WinGravitySouth = 8;
    private static final int WinGravitySouthEast = 9;
    private static final int WinGravityStatic = 10;

    /**
     * Constructor.
     *
     * @param id          The window's ID.
     * @param xServer     The X server.
     * @param client      The client issuing the request.
     * @param screen      The window's screen.
     * @param parent      The window's parent.
     * @param x           X position relative to parent.
     * @param y           Y position relative to parent.
     * @param width       Width of the window.
     * @param height      Height of the window.
     * @param borderWidth Width of the window's border.
     * @param inputOnly   Is this an InputOnly window?
     * @param isRoot      Is this the root window?
     */
    public Window(int id, XServer xServer, Client client, ScreenView screen, Window parent, int x, int y, int width, int height, int borderWidth, boolean inputOnly, boolean isRoot) {
        super(WINDOW, id, xServer, client);

        _screen = screen;
        _parent = parent;
        _borderWidth = borderWidth;
        _colormap = _screen.getDefaultColormap();
        _inputOnly = inputOnly;

        if (isRoot) {
            _orect = new Rect(0, 0, width, height);
            _irect = new Rect(0, 0, width, height);
        } else {
            final int left = _parent._orect.left + _parent._borderWidth + x;
            final int top = _parent._orect.top + _parent._borderWidth + y;
            final int border = 2 * borderWidth;

            _orect = new Rect(left, top, left + width + border, top + height + border);
            if (_borderWidth == 0) _irect = new Rect(_orect);
            else
                _irect = new Rect(left + borderWidth, top + borderWidth, _orect.right - borderWidth, _orect.bottom - borderWidth);
        }

        _attributes = new int[]{0,    // background-pixmap = None
                0,    // background-pixel = zero
                0,    // border-pixmap = CopyFromParent
                0,    // border-pixel = zero
                0,    // bit-gravity = Forget
                WinGravityNorthWest,    // win-gravity = NorthWest
                0,    // backing-store = NotUseful
                0xffffffff,    // backing-planes = all ones
                0,    // backing-pixel = zero
                0,    // override-redirect = False
                0,    // save-under = False
                0,    // event-mask = empty set
                0,    // do-not-propogate-mask = empty set
                0,    // colormap = CopyFromParent
                0    // cursor = None
        };

        if (isRoot) {
            _attributes[AttrBackgroundPixel] = 0xffc0c0c0;
            _isMapped = true;
            _cursor = (Cursor) _xServer.getResource(2);    // X cursor.
            _drawable = new Drawable(width, height, 32, null, _attributes[AttrBackgroundPixel]);
            _drawable.clear();
        } else {
            _attributes[AttrBackgroundPixel] = 0xff000000;
            _drawable = new Drawable(width, height, 32, null, _attributes[AttrBackgroundPixel]);
        }

        _children = new Vector<Window>();
        _properties = new Hashtable<Integer, Property>();
        _passiveButtonGrabs = new HashSet<PassiveButtonGrab>();
        _passiveKeyGrabs = new HashSet<PassiveKeyGrab>();
        _clientMasks = new Hashtable<Client, Integer>();
        _shapeSelectInput = new Vector<Client>();
    }

    /**
     * Is this a functional server only window?
     *
     * @return True if this is a hidden server only window.
     */
    public boolean isServerWindow() {
        return _isServerWindow;
    }

    /**
     * Flag this window as server only.
     * 
     * @param b True if this is a hidden server only window.
     */
    public void setIsServerWindow(boolean b) {
        _isServerWindow = b;
    }

    /**
     * Is the clip region shaped?
     *
     * @return True if the clip region is shaped.
     */
    public boolean isClipShaped() {
        return _clipShapeRegion != null;
    }

    /**
     * Is the bounding region shaped?
     *
     * @return True if the bounding region is shaped.
     */
    public boolean isBoundingShaped() {
        return _boundingShapeRegion != null;
    }

    /**
     * Send a shape notify to all interested clients.
     *
     * @param shapeKind The kind of shape.
     */
    public void sendShapeNotify(byte shapeKind) {
        Region r = getShapeRegion(shapeKind);
        boolean shaped = (r != null);
        Rect rect;

        if (r != null) rect = r.getBounds();
        else if (shapeKind == XShape.KindClip) rect = _irect;
        else rect = _orect;

        for (Client client : _shapeSelectInput) {
            if (client == null) continue;
            try {
                InputOutput io = client.getInputOutput();

                synchronized (io) {
                    io.writeByte(XShape.EventBase);
                    io.writeByte((byte) shapeKind);
                    io.writeShort((short) (client.getSequenceNumber() & 0xffff));
                    io.writeInt(_id);
                    io.writeShort((short) (rect.left - _irect.left));
                    io.writeShort((short) (rect.top - _irect.left));
                    io.writeShort((short) rect.width());
                    io.writeShort((short) rect.height());
                    io.writeInt(1);
                    io.writeByte((byte) (shaped ? 1 : 0));
                    io.writePadBytes(11);
                }
                io.flush();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Add a client to the shape select input.
     *
     * @param client The client to add.
     */
    public void addShapeSelectInput(Client client) {
        _shapeSelectInput.add(client);
    }

    /**
     * Remove a client from the shape select input.
     *
     * @param client The client to remove.
     */
    public void removeShapeSelectInput(Client client) {
        _shapeSelectInput.remove(client);
    }

    /**
     * Does the client is the shape select input list?
     *
     * @param client The client to check.
     * @return True if the client is in the shape select input list.
     */
    public boolean shapeSelectInputEnabled(Client client) {
        return _shapeSelectInput.contains(client);
    }

    /**
     * Return a shape region.
     *
     * @param shapeKind The kind of shape to return.
     * @return The shape region.
     */
    public Region getShapeRegion(byte shapeKind) {
        switch (shapeKind) {
            case XShape.KindBounding:
                return _boundingShapeRegion;
            case XShape.KindClip:
                return _clipShapeRegion;
            case XShape.KindInput:
                return _inputShapeRegion;
        }

        return null;
    }

    /**
     * Set a shape region.
     *
     * @param shapeKind The kind of shape to set.
     * @param r         The shape region.
     */
    public void setShapeRegion(byte shapeKind, Region r) {
        switch (shapeKind) {
            case XShape.KindBounding:
                _boundingShapeRegion = r;
                break;
            case XShape.KindClip:
                _clipShapeRegion = r;
                break;
            case XShape.KindInput:
                _inputShapeRegion = r;
                break;
        }
    }

    /**
     * Return the window's parent.
     *
     * @return The window's parent.
     */
    public Window getParent() {
        return _parent;
    }

    /**
     * Return the window's screen.
     *
     * @return The window's screen.
     */
    public ScreenView getScreen() {
        return _screen;
    }

    /**
     * Return the window's drawable.
     *
     * @return The window's drawable.
     */
    public Drawable getDrawable() {
        return _drawable;
    }

    /**
     * Return the window's cursor.
     *
     * @return The window's cursor.
     */
    public Cursor getCursor() {
        if (_cursor == null) return _parent.getCursor();
        else return _cursor;
    }

    /**
     * Return the window's inner rectangle.
     *
     * @return The window's inner rectangle.
     */
    public Rect getIRect() {
        return _irect;
    }

    /**
     * Return the window's outer rectangle.
     *
     * @return The window's outer rectangle.
     */
    public Rect getORect() {
        return _orect;
    }

    /**
     * Return the window's cumulative event mask.
     *
     * @return The window's event mask.
     */
    public int getEventMask() {
        return _eventMask;
    }

    /**
     * Return the list of clients selecting on the events.
     *
     * @param mask The event mask.
     * @return List of clients, or null if none selecting.
     */
    public Vector<Client> getSelectingClients(int mask) {
        if ((mask & _eventMask) == 0) return null;

        Vector<Client> rc = new Vector<Client>();
        Set<Client> sc = _clientMasks.keySet();

        for (Client c : sc) {
            if (c == null) continue;
            if ((_clientMasks.get(c) & mask) != 0) rc.add(c);
        }

        return rc;
    }

    /**
     * Remove a client from the event selection list.
     * Usually occurs after an I/O error on the client.
     *
     * @param client The client to remove.
     */
    private void removeSelectingClient(Client client) {
        _clientMasks.remove(client);

        Set<Client> sc = _clientMasks.keySet();

        _eventMask = 0;
        for (Client c : sc) {
            if (c == null) continue;
            _eventMask |= _clientMasks.get(c);
        }
    }

    /**
     * Return the event mask that the client is selecting on.
     *
     * @param client The client selecting on the events.
     * @return The event mask, or zero if the client is selecting.
     */
    public int getClientEventMask(Client client) {
        if (_clientMasks.containsKey(client)) return _clientMasks.get(client);
        else return 0;
    }

    /**
     * Return the window's do-not-propagate mask.
     *
     * @return The window's do-not-propagate mask.
     */
    public int getDoNotPropagateMask() {
        return _attributes[AttrDoNotPropagateMask];
    }

    /**
     * Is the window an inferior of this window?
     *
     * @param w The window being tested.
     * @return True if the window is a inferior of this window.
     */
    public boolean isInferior(Window w) {
        for (; ; ) {
            if (w._parent == this) return true;
            else if (w._parent == null) return false;
            else w = w._parent;
        }
    }

    /**
     * Is the window an ancestor of this window?
     *
     * @param w The window being tested.
     * @return True if the window is an ancestor of this window.
     */
    public boolean isAncestor(Window w) {
        return w.isInferior(this);
    }

    /**
     * Is the window viewable? It and all its ancestors must be mapped.
     *
     * @return True if the window is viewable.
     */
    public boolean isViewable() {
        for (Window w = this; w != null; w = w._parent)
            if (!w._isMapped) return false;

        return true;
    }

    /**
     * Draw the window and its mapped children.
     *
     * @param canvas The canvas to draw to.
     * @param paint  A paint to draw with.
     */
    public void draw(Canvas canvas, Paint paint) {
        if (!_isMapped) return;

        if (_boundingShapeRegion != null) {
            canvas.save();

            if (!_hardwareAccelerated) {
                try {
                    canvas.clipRect(_boundingShapeRegion.getBounds());
                } catch (UnsupportedOperationException e) {
                    _hardwareAccelerated = true;
                }
            }

            paint.setColor(_attributes[AttrBorderPixel] | 0xff000000);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(_orect, paint);
        } else if (_borderWidth != 0) {
            if (!Rect.intersects(_orect, canvas.getClipBounds())) return;

            float hbw = 0.5f * _borderWidth;

            paint.setColor(_attributes[AttrBorderPixel] | 0xff000000);
            paint.setStrokeWidth(_borderWidth);
            paint.setStyle(Paint.Style.STROKE);

            canvas.drawRect(_orect.left + hbw, _orect.top + hbw, _orect.right - hbw, _orect.bottom - hbw, paint);
        }

        canvas.save();

        boolean clipIntersect;

        if (_clipShapeRegion != null && !_hardwareAccelerated) {
            try {
                clipIntersect = canvas.clipRect(_clipShapeRegion.getBounds());
            } catch (UnsupportedOperationException e) {
                _hardwareAccelerated = true;
                clipIntersect = canvas.clipRect(_irect);
            }
        } else {
            clipIntersect = canvas.clipRect(_irect);
        }

        if (clipIntersect) {
            if (!_inputOnly)
                canvas.drawBitmap(_drawable.getBitmap(), _irect.left, _irect.top, paint);
            for (Window w : _children)
                w.draw(canvas, paint);
        }

        canvas.restore();
        if (_boundingShapeRegion != null) canvas.restore();
    }

    /**
     * Return the mapped window whose input area contains the specified point.
     *
     * @param x X coordinate of the point.
     * @param y Y coordinate of the point.
     * @return The mapped window containing the point.
     */
    public Window windowAtPoint(int x, int y) {
        for (int i = _children.size() - 1; i >= 0; i--) {
            Window w = _children.elementAt(i);

            if (!w._isMapped) continue;

            if (w._inputShapeRegion != null) {
                if (w._inputShapeRegion.contains(x, y)) return w.windowAtPoint(x, y);
            } else if (w._orect.contains(x, y)) {
                return w.windowAtPoint(x, y);
            }
        }

        return this;
    }

    /**
     * Find a passive button grab on this window or its ancestors.
     *
     * @param buttons    The pointer buttons and modifiers currently pressed.
     * @param highestPbg Highest pointer grab found so far.
     * @return The passive pointer grab from the highest ancestor.
     */
    public PassiveButtonGrab findPassiveButtonGrab(int buttons, PassiveButtonGrab highestPbg) {
        for (PassiveButtonGrab pbg : _passiveButtonGrabs) {
            if (pbg.matchesEvent(buttons)) {
                highestPbg = pbg;
                break;
            }
        }

        if (_parent == null) return highestPbg;
        else return _parent.findPassiveButtonGrab(buttons, highestPbg);
    }

    /**
     * Add a passive button grab.
     *
     * @param pbg The passive button grab.
     */
    public void addPassiveButtonGrab(PassiveButtonGrab pbg) {
        removePassiveButtonGrab(pbg.getButton(), pbg.getModifiers());
        _passiveButtonGrabs.add(pbg);
    }

    /**
     * Remove all passive button grabs that match the button and modifiers
     * combination.
     *
     * @param button    The button, or 0 for any button.
     * @param modifiers The modifier mask, or 0x8000 for any.
     */
    public void removePassiveButtonGrab(byte button, int modifiers) {
        Iterator<PassiveButtonGrab> it = _passiveButtonGrabs.iterator();

        while (it.hasNext()) {
            PassiveButtonGrab pbg = it.next();

            if (pbg.matchesGrab(button, modifiers)) it.remove();
        }
    }

    /**
     * Find a passive key grab on this window or its ancestors.
     *
     * @param key        The key that was pressed.
     * @param modifiers  The modifiers currently pressed.
     * @param highestPkg Highest key grab found so far.
     * @return The passive key grab from the highest ancestor.
     */
    public PassiveKeyGrab findPassiveKeyGrab(int key, int modifiers, PassiveKeyGrab highestPkg) {
        for (PassiveKeyGrab pkg : _passiveKeyGrabs) {
            if (pkg.matchesEvent(key, modifiers)) {
                highestPkg = pkg;
                break;
            }
        }

        if (_parent == null) return highestPkg;
        else return _parent.findPassiveKeyGrab(key, modifiers, highestPkg);
    }

    /**
     * Add a passive key grab.
     *
     * @param pkg The passive key grab.
     */
    public void addPassiveKeyGrab(PassiveKeyGrab pkg) {
        removePassiveKeyGrab(pkg.getKey(), pkg.getModifiers());
        _passiveKeyGrabs.add(pkg);
    }

    /**
     * Remove all passive key grabs that match the key and modifiers
     * combination.
     *
     * @param key       The key, or 0 for any key.
     * @param modifiers The modifier mask, or 0x8000 for any.
     */
    public void removePassiveKeyGrab(byte key, int modifiers) {
        Iterator<PassiveKeyGrab> it = _passiveKeyGrabs.iterator();

        while (it.hasNext()) {
            PassiveKeyGrab pkg = it.next();

            if (pkg.matchesGrab(key, modifiers)) it.remove();
        }
    }

    /**
     * Process a CreateWindow.
     * Create a window with the specified ID, with this window as parent.
     *
     * @param io             The input/output stream.
     * @param client         The client issuing the request.
     * @param sequenceNumber The request sequence number.
     * @param id             The ID of the window to create.
     * @param depth          The window depth.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @return True if the window was created successfully.
     * @throws IOException
     */
    public boolean processCreateWindowRequest(InputOutput io, Client client, int sequenceNumber, int id, int depth, int bytesRemaining) throws IOException {
        int x = (short) io.readShort();    // X position.
        int y = (short) io.readShort();    // Y position.
        int width = io.readShort();    // Window width.
        int height = io.readShort();    // Window height.
        int borderWidth = io.readShort();    // Border width.
        int wclass = io.readShort();    // Window class.
        Window w;
        boolean inputOnly;

        io.readInt();    // Visual.
        bytesRemaining -= 16;

        if (wclass == 0)    // Copy from parent.
            inputOnly = _inputOnly;
        else if (wclass == 1)    // Input/output.
            inputOnly = false;
        else inputOnly = true;

        try {
            w = new Window(id, _xServer, client, _screen, this, x, y, width, height, borderWidth, inputOnly, false);
        } catch (OutOfMemoryError e) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Alloc, RequestCode.CreateWindow, 0);
            return false;
        }

        if (!w.processWindowAttributes(client, RequestCode.CreateWindow, bytesRemaining))
            return false;

        w._drawable.clear();

        _xServer.addResource(w);
        client.addResource(w);
        _children.add(w);

        Vector<Client> sc;

        if ((sc = getSelectingClients(EventCode.MaskSubstructureNotify)) != null) {
            for (Client c : sc) {
                if (c == null) continue;
                EventCode.sendCreateNotify(c, this, w, x, y, width, height, borderWidth, _overrideRedirect);
            }
        }

        return true;
    }

    /**
     * Request a redraw of the window.
     */
    public void invalidate() {
        _screen.postInvalidate(_orect.left, _orect.top, _orect.right, _orect.bottom);
    }

    /**
     * Request a redraw of a region of the window.
     *
     * @param x      X coordinate of the region.
     * @param y      Y coordinate of the region.
     * @param width  Width of the region.
     * @param height Height of the region.
     */
    public void invalidate(int x, int y, int width, int height) {
        _screen.postInvalidate(_irect.left + x, _irect.top + y, _irect.left + x + width, _irect.top + y + height);
    }

    /**
     * Delete this window from its parent.
     * Used when a client disconnects.
     */
    @Override
    public void delete() {
        Vector<Client> psc, sc;

        // Send unmap and destroy notification to any other clients that
        // are listening.
        removeSelectingClient(_client);
        _parent.removeSelectingClient(_client);

        sc = getSelectingClients(EventCode.MaskStructureNotify);
        psc = _parent.getSelectingClients(EventCode.MaskSubstructureNotify);

        if (_isMapped) {
            _screen.revertFocus(this);
            _isMapped = false;

            if (sc != null) {
                for (Client c : sc) {
                    if (c == null) continue;
                    try {
                        EventCode.sendUnmapNotify(c, this, this, false);
                    } catch (IOException e) {
                        removeSelectingClient(c);
                    }
                }
            }

            if (psc != null) {
                for (Client c : psc) {
                    if (c == null) continue;
                    try {
                        EventCode.sendUnmapNotify(c, _parent, this, false);
                    } catch (IOException e) {
                        removeSelectingClient(c);
                    }
                }
            }

            updateAffectedVisibility();
            invalidate();
        }

        if (sc != null) {
            for (Client c : sc) {
                if (c == null) continue;
                try {
                    EventCode.sendDestroyNotify(c, this, this);
                } catch (IOException e) {
                    removeSelectingClient(c);
                }
            }
        }

        if (psc != null) {
            for (Client c : psc) {
                if (c == null) continue;
                try {
                    EventCode.sendDestroyNotify(c, _parent, this);
                } catch (IOException e) {
                    removeSelectingClient(c);
                }
            }
        }

        _screen.deleteWindow(this);

        if (_parent != null) _parent._children.remove(this);

        super.delete();
    }

    /**
     * Process a list of window attributes.
     *
     * @param client         The remote client.
     * @param opcode         The opcode being processed.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @return True if the window is successfully created.
     * @throws IOException
     */
    private boolean processWindowAttributes(Client client, byte opcode, int bytesRemaining) throws IOException {
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

        for (int i = 0; i < 15; i++)
            if ((valueMask & (1 << i)) != 0) processValue(io, i);

        if (opcode == RequestCode.CreateWindow)    // Apply all values on create.
            valueMask = 0xffffffff;

        return applyValues(client, opcode, valueMask);
    }

    /**
     * Process a single window attribute value.
     *
     * @param io      The input/output stream.
     * @param maskBit The mask bit of the attribute.
     * @throws IOException
     */
    private void processValue(InputOutput io, int maskBit) throws IOException {
        switch (maskBit) {
            case AttrBackgroundPixmap:
            case AttrBackgroundPixel:
            case AttrBorderPixmap:
            case AttrBorderPixel:
            case AttrBackingPlanes:
            case AttrBackingPixel:
            case AttrEventMask:
            case AttrDoNotPropagateMask:
            case AttrColormap:
            case AttrCursor:
                _attributes[maskBit] = io.readInt();
                break;
            case AttrBitGravity:
            case AttrWinGravity:
            case AttrBackingStore:
            case AttrOverrideRedirect:
            case AttrSaveUnder:
                _attributes[maskBit] = io.readByte();
                io.readSkip(3);
                break;
        }
    }

    /**
     * Apply the attribute values to the window.
     *
     * @param client The remote client.
     * @param opcode The opcode being processed.
     * @param mask   Bit mask of the attributes that have changed.
     * @return True if the values are all valid.
     * @throws IOException
     */
    private boolean applyValues(Client client, byte opcode, int mask) throws IOException {
        boolean ok = true;

        if ((mask & (1 << AttrBackgroundPixmap)) != 0) {
            int pmid = _attributes[AttrBackgroundPixmap];

            if (pmid == 0) {    // None.
                _backgroundBitmap = null;
                _drawable.setBackgroundBitmap(null);
            } else if (pmid == 1) {    // ParentRelative.
                _backgroundBitmap = _parent._backgroundBitmap;
                _attributes[AttrBackgroundPixel] = _parent._attributes[AttrBackgroundPixel];
                _drawable.setBackgroundBitmap(_backgroundBitmap);
                _drawable.setBackgroundColor(_attributes[AttrBackgroundPixel] | 0xff000000);
            } else {
                Resource r = _xServer.getResource(pmid);

                if (r != null && r.getType() == PIXMAP) {
                    Pixmap p = (Pixmap) r;
                    Drawable d = p.getDrawable();

                    _backgroundBitmap = d.getBitmap();
                    _drawable.setBackgroundBitmap(_backgroundBitmap);
                } else {
                    ErrorCode.write(client, ErrorCode.Colormap, opcode, pmid);
                    ok = false;
                }
            }
        }

        if ((mask & (1 << AttrBackgroundPixel)) != 0)
            _drawable.setBackgroundColor(_attributes[AttrBackgroundPixel] | 0xff000000);

        if ((mask & (1 << AttrColormap)) != 0) {
            int cid = _attributes[AttrColormap];

            if (cid != 0) {
                Resource r = _xServer.getResource(cid);

                if (r != null && r.getType() == COLORMAP) {
                    _colormap = (Colormap) r;
                } else {
                    ErrorCode.write(client, ErrorCode.Colormap, opcode, cid);
                    ok = false;
                }
            } else if (_parent != null) {
                _colormap = _parent._colormap;
            }
        }

        if ((mask & (1 << AttrEventMask)) != 0) {
            _clientMasks.put(client, _attributes[AttrEventMask]);

            Set<Client> sc = _clientMasks.keySet();

            _eventMask = 0;
            for (Client c : sc){
                if (c == null) continue;
                _eventMask |= _clientMasks.get(c);
            }
        }

        if ((mask & (1 << AttrOverrideRedirect)) != 0)
            _overrideRedirect = (_attributes[AttrOverrideRedirect] == 1);

        if ((mask & (1 << AttrCursor)) != 0) {
            int cid = _attributes[AttrCursor];

            if (cid != 0) {
                Resource r = _xServer.getResource(cid);

                if (r != null && r.getType() == CURSOR) {
                    _cursor = (Cursor) r;
                } else {
                    ErrorCode.write(client, ErrorCode.Cursor, opcode, cid);
                    ok = false;
                }
            } else {
                _cursor = null;
            }
        }

        return ok;
    }

    /**
     * Notify that the pointer has entered this window.
     *
     * @param x        Pointer X coordinate.
     * @param y        Pointer Y coordinate.
     * @param detail   0=Ancestor, 1=Virtual, 2=Inferior, 3=Nonlinear,
     *                 4=NonlinearVirtual.
     * @param toWindow Window containing pointer.
     * @param mode     0=Normal, 1=Grab, 2=Ungrab.
     */
    private void enterNotify(int x, int y, int detail, Window toWindow, int mode) {
        if (!_isMapped) return;

        Vector<Client> sc;

        if ((sc = getSelectingClients(EventCode.MaskEnterWindow)) == null) return;

        Window child = (toWindow._parent == this) ? toWindow : null;
        Window fw = _screen.getFocusWindow();
        boolean focus = false;

        if (fw != null) focus = (fw == this) || isAncestor(fw);

        for (Client c : sc) {
            if (c == null) continue;
            try {
                EventCode.sendEnterNotify(c, _xServer.getTimestamp(), detail, _screen.getRootWindow(), this, child, x, y, x - _irect.left, y - _irect.top, _screen.getButtons(), mode, focus);
            } catch (IOException e) {
                removeSelectingClient(c);
            }
        }

        sc = getSelectingClients(EventCode.MaskKeymapState);
        if (sc != null) {
            Keyboard kb = _xServer.getKeyboard();

            for (Client c : sc) {
                if (c == null) continue;
                try {
                    EventCode.sendKeymapNotify(c, kb.getKeymap());
                } catch (IOException e) {
                    removeSelectingClient(c);
                }
            }
        }
    }

    /**
     * Notify that the pointer has left this window.
     *
     * @param x          Pointer X coordinate.
     * @param y          Pointer Y coordinate.
     * @param detail     0=Ancestor, 1=Virtual, 2=Inferior, 3=Nonlinear, 4=NonlinearVirtual.
     * @param fromWindow Window previously containing pointer.
     * @param mode       0=Normal, 1=Grab, 2=Ungrab.
     */
    private void leaveNotify(int x, int y, int detail, Window fromWindow, int mode) {
        if (!_isMapped) return;

        Vector<Client> sc;

        if ((sc = getSelectingClients(EventCode.MaskLeaveWindow)) == null) return;

        Window child = (fromWindow._parent == this) ? fromWindow : null;
        Window fw = _screen.getFocusWindow();
        boolean focus = false;

        if (fw != null) focus = (fw == this) || isAncestor(fw);

        for (Client c : sc) {
            if (c == null) continue;
            try {
                EventCode.sendLeaveNotify(c, _xServer.getTimestamp(), detail, _screen.getRootWindow(), this, child, x, y, x - _irect.left, y - _irect.top, _screen.getButtons(), mode, focus);
            } catch (IOException e) {
                removeSelectingClient(c);
            }
        }
    }

    /**
     * Called when the pointer leaves this window and enters another.
     *
     * @param x    Pointer X coordinate.
     * @param y    Pointer Y coordinate.
     * @param ew   The window being entered.
     * @param mode 0=Normal, 1=Grab, 2=Ungrab.
     */
    public void leaveEnterNotify(int x, int y, Window ew, int mode) {
        if (ew.isInferior(this)) {
            leaveNotify(x, y, 0, this, mode);

            for (Window w = _parent; w != ew; w = w._parent)
                w.leaveNotify(x, y, 1, this, 0);

            ew.enterNotify(x, y, 2, ew, mode);
        } else if (isInferior(ew)) {
            leaveNotify(x, y, 2, this, mode);

            Stack<Window> stack = new Stack<Window>();

            for (Window w = ew._parent; w != this; w = w._parent)
                stack.push(w);

            while (!stack.empty()) {
                Window w = stack.pop();

                w.enterNotify(x, y, 1, ew, mode);
            }

            ew.enterNotify(x, y, 0, ew, mode);
        } else {
            leaveNotify(x, y, 3, this, 0);

            Window lca = null;
            Stack<Window> stack = new Stack<Window>();

            for (Window w = _parent; w != ew; w = w._parent) {
                if (w.isInferior(ew)) {
                    lca = w;
                    break;
                } else {
                    w.leaveNotify(x, y, 4, this, mode);
                }
            }

            for (Window w = ew._parent; w != lca; w = w._parent)
                stack.push(w);

            while (!stack.empty()) {
                Window w = stack.pop();

                w.enterNotify(x, y, 4, ew, mode);
            }

            ew.enterNotify(x, y, 3, ew, mode);
        }
    }

    /**
     * Notify that this window has gained keyboard focus.
     *
     * @param detail 0=Ancestor, 1=Virtual, 2=Inferior, 3=Nonlinear,
     *               4=NonlinearVirtual, 5=Pointer, 6=PointerRoot, 7=None.
     * @param mode   0=Normal, 1=Grab, 2=Ungrab, 3=WhileGrabbed.
     */
    private void focusInNotify(int detail, int mode) {
        if (!_isMapped) return;

        Vector<Client> sc;

        if ((sc = getSelectingClients(EventCode.MaskFocusChange)) == null) return;

        for (Client c : sc) {
            if (c == null) continue;
            try {
                EventCode.sendFocusIn(c, _xServer.getTimestamp(), detail, this, mode);
            } catch (IOException e) {
                removeSelectingClient(c);
            }
        }

        sc = getSelectingClients(EventCode.MaskKeymapState);
        if (sc != null) {
            Keyboard kb = _xServer.getKeyboard();

            for (Client c : sc) {
                if (c == null) continue;
                try {
                    EventCode.sendKeymapNotify(c, kb.getKeymap());
                } catch (IOException e) {
                    removeSelectingClient(c);
                }
            }
        }
    }

    /**
     * Notify that this window has lost keyboard focus.
     *
     * @param detail 0=Ancestor, 1=Virtual, 2=Inferior, 3=Nonlinear,
     *               4=NonlinearVirtual, 5=Pointer, 6=PointerRoot, 7=None.
     * @param mode   0=Normal, 1=Grab, 2=Ungrab, 3=WhileGrabbed.
     */
    private void focusOutNotify(int detail, int mode) {
        if (!_isMapped) return;

        Vector<Client> sc;

        if ((sc = getSelectingClients(EventCode.MaskFocusChange)) == null) return;

        for (Client c : sc) {
            if (c == null) continue;
            try {
                EventCode.sendFocusOut(c, _xServer.getTimestamp(), detail, this, mode);
            } catch (IOException e) {
                removeSelectingClient(c);
            }
        }
    }

    /**
     * Called when keyboard focus changes from one window to another.
     * Handles the FocusIn and FocusOut events.
     *
     * @param wlose The window that is losing focus.
     * @param wgain The window that is gaining focus.
     * @param wp    The window containing the pointer.
     * @param wroot The root window.
     * @param mode  0=Normal, 1=Grab, 2=Ungrab, 3=WhileGrabbed.
     */
    public static void focusInOutNotify(Window wlose, Window wgain, Window wp, Window wroot, int mode) {
        if (wlose == wgain) return;

        if (wlose == null) {
            wroot.focusOutNotify(7, mode);

            if (wgain == wroot) {
                wroot.focusInNotify(6, mode);

                Stack<Window> stack = new Stack<Window>();

                for (Window w = wp; w != null; w = w._parent)
                    stack.push(w);

                while (!stack.empty()) {
                    Window w = stack.pop();

                    w.focusInNotify(5, mode);
                }
            } else {
                Stack<Window> stack = new Stack<Window>();

                for (Window w = wgain._parent; w != null; w = w._parent)
                    stack.push(w);

                while (!stack.empty()) {
                    Window w = stack.pop();

                    w.focusInNotify(4, mode);
                }

                wgain.focusInNotify(3, mode);

                if (wgain.isInferior(wp)) {
                    for (Window w = wp; w != wgain; w = w._parent)
                        stack.push(w);

                    while (!stack.empty()) {
                        Window w = stack.pop();

                        w.focusInNotify(5, mode);
                    }
                }
            }
        } else if (wlose == wroot) {
            for (Window w = wp; w != null; w = w._parent)
                w.focusOutNotify(5, mode);

            wroot.focusOutNotify(6, mode);

            if (wgain == null) {
                wroot.focusInNotify(7, mode);
            } else {
                Stack<Window> stack = new Stack<Window>();

                for (Window w = wgain._parent; w != null; w = w._parent)
                    stack.push(w);

                while (!stack.empty()) {
                    Window w = stack.pop();

                    w.focusInNotify(4, mode);
                }

                wgain.focusInNotify(3, mode);

                if (wgain.isInferior(wp)) {
                    for (Window w = wp; w != wgain; w = w._parent)
                        stack.push(w);

                    while (!stack.empty()) {
                        Window w = stack.pop();

                        w.focusInNotify(5, mode);
                    }
                }
            }
        } else if (wgain == null) {
            if (wlose.isInferior(wp)) for (Window w = wp; w != wlose; w = w._parent)
                w.focusOutNotify(5, mode);

            wlose.focusOutNotify(3, mode);
            for (Window w = wlose._parent; w != null; w = w._parent)
                w.focusOutNotify(4, mode);
            wroot.focusInNotify(7, mode);
        } else if (wgain == wroot) {
            if (wlose.isInferior(wp)) for (Window w = wp; w != wlose; w = w._parent)
                w.focusOutNotify(5, mode);

            wlose.focusOutNotify(3, mode);
            for (Window w = wlose._parent; w != null; w = w._parent)
                w.focusOutNotify(4, mode);
            wroot.focusInNotify(6, mode);

            Stack<Window> stack = new Stack<Window>();

            for (Window w = wp; w != null; w = w._parent)
                stack.push(w);

            while (!stack.empty()) {
                Window w = stack.pop();

                w.focusInNotify(5, mode);
            }
        } else if (wgain.isInferior(wlose)) {
            wlose.focusOutNotify(0, mode);

            for (Window w = wlose._parent; w != wgain; w = w._parent)
                w.focusOutNotify(1, mode);

            wgain.focusInNotify(2, mode);

            if (wgain.isInferior(wp) && (wp != wlose && !wp.isInferior(wlose) && !wp.isAncestor(wlose))) {
                Stack<Window> stack = new Stack<Window>();

                for (Window w = wp; w != wgain; w = w._parent)
                    stack.push(w);

                while (!stack.empty()) {
                    Window w = stack.pop();

                    w.focusInNotify(5, mode);
                }
            }
        } else if (wlose.isInferior(wgain)) {
            if (wlose.isInferior(wp) && (wp != wgain && !wp.isInferior(wgain) && !wp.isAncestor(wgain))) {
                for (Window w = wp; w != wlose; w = w._parent)
                    w.focusOutNotify(5, mode);
            }

            wlose.focusOutNotify(2, mode);

            Stack<Window> stack = new Stack<Window>();

            for (Window w = wgain._parent; w != wlose; w = w._parent)
                stack.push(w);

            while (!stack.empty()) {
                Window w = stack.pop();

                w.focusInNotify(1, mode);
            }

            wgain.focusInNotify(0, mode);
        } else {
            if (wlose.isInferior(wp)) for (Window w = wp; w != wlose; w = w._parent)
                w.focusOutNotify(5, mode);

            wlose.focusOutNotify(3, 0);

            Window lca = null;
            Stack<Window> stack = new Stack<Window>();

            for (Window w = wlose._parent; w != wgain; w = w._parent) {
                if (w.isInferior(wgain)) {
                    lca = w;
                    break;
                } else {
                    w.focusOutNotify(4, mode);
                }
            }

            for (Window w = wgain._parent; w != lca; w = w._parent)
                stack.push(w);

            while (!stack.empty()) {
                Window w = stack.pop();

                w.focusInNotify(4, mode);
            }

            wgain.focusInNotify(3, mode);

            if (wgain.isInferior(wp)) {
                for (Window w = wp; w != wgain; w = w._parent)
                    stack.push(w);

                while (!stack.empty()) {
                    Window w = stack.pop();

                    w.focusInNotify(5, mode);
                }
            }
        }
    }

    /**
     * Called when a button is pressed or released in this window.
     *
     * @param pressed    Whether the button was pressed or released.
     * @param x          Pointer X coordinate.
     * @param y          Pointer Y coordinate.
     * @param button     Button that was pressed/released.
     * @param grabClient Only send to this client if it isn't null.
     * @return The window it was sent to, or null if not sent.
     */
    public Window buttonNotify(boolean pressed, int x, int y, int button, int timestamp, Client grabClient) {
        Window evw = this;
        Window child = null;
        int mask = pressed ? EventCode.MaskButtonPress : EventCode.MaskButtonRelease;
        Vector<Client> sc;

        for (; ; ) {
            if (evw._isMapped) {
                sc = evw.getSelectingClients(mask);
                if (sc != null) break;
            }

            if (evw._parent == null) return null;

            if ((evw._attributes[AttrDoNotPropagateMask] & mask) != 0) return null;

            child = evw;
            evw = evw._parent;
        }

        Window sentWindow = null;

        for (Client c : sc) {
            if (c == null) continue;
            if (grabClient != null && grabClient != c) continue;

            try {
                if (pressed)
                    EventCode.sendButtonPress(c, timestamp, button, _screen.getRootWindow(), evw, child, x, y, x - evw._irect.left, y - evw._irect.top, _screen.getButtons());
                else
                    EventCode.sendButtonRelease(c, timestamp, button, _screen.getRootWindow(), evw, child, x, y, x - evw._irect.left, y - evw._irect.top, _screen.getButtons());
                sentWindow = evw;
            } catch (IOException e) {
                evw.removeSelectingClient(c);
            }
        }

        return sentWindow;
    }

    /**
     * Called when a button is pressed or released while grabbed by this
     * window.
     *
     * @param pressed     Whether the button was pressed or released.
     * @param x           Pointer X coordinate.
     * @param y           Pointer Y coordinate.
     * @param button      Button that was pressed/released.
     * @param eventMask   The events the window is interested in.
     * @param grabClient  The grabbing client.
     * @param ownerEvents Owner-events flag.
     */
    public void grabButtonNotify(boolean pressed, int x, int y, int button, int eventMask, Client grabClient, boolean ownerEvents) {
        if (ownerEvents) {
            Window w = _screen.getRootWindow().windowAtPoint(x, y);

            if (w.buttonNotify(pressed, x, y, button, _xServer.getTimestamp(), grabClient) != null)
                return;
        }

        int mask = pressed ? EventCode.MaskButtonPress : EventCode.MaskButtonRelease;

        if ((eventMask & mask) == 0) return;

        try {
            if (pressed)
                EventCode.sendButtonPress(grabClient, _xServer.getTimestamp(), button, _screen.getRootWindow(), this, null, x, y, x - _irect.left, y - _irect.top, _screen.getButtons());
            else
                EventCode.sendButtonRelease(grabClient, _xServer.getTimestamp(), button, _screen.getRootWindow(), this, null, x, y, x - _irect.left, y - _irect.top, _screen.getButtons());
        } catch (IOException e) {
            removeSelectingClient(grabClient);
        }
    }

    /**
     * Called when a key is pressed or released in this window.
     *
     * @param pressed    Whether the key was pressed or released.
     * @param x          Pointer X coordinate.
     * @param y          Pointer Y coordinate.
     * @param keycode    Keycode of the key.
     * @param grabClient Only notify this client if it isn't null.
     * @return True if an event is sent.
     */
    public boolean keyNotify(boolean pressed, int x, int y, int keycode, Client grabClient) {
        Window evw = this;
        Window child = null;
        int mask = pressed ? EventCode.MaskKeyPress : EventCode.MaskKeyRelease;
        Vector<Client> sc;

        for (; ; ) {
            if (evw._isMapped) {
                sc = evw.getSelectingClients(mask);
                if (sc != null) break;
            }

            if (evw._parent == null) return false;

            if ((evw._attributes[AttrDoNotPropagateMask] & mask) != 0) return false;

            child = evw;
            evw = evw._parent;
        }

        boolean sent = false;

        for (Client c : sc) {
            if (c == null) continue;
            if (grabClient != null && grabClient != c) continue;

            try {
                if (pressed)
                    EventCode.sendKeyPress(c, _xServer.getTimestamp(), keycode, _screen.getRootWindow(), evw, child, x, y, x - evw._irect.left, y - evw._irect.top, _screen.getButtons());
                else
                    EventCode.sendKeyRelease(c, _xServer.getTimestamp(), keycode, _screen.getRootWindow(), evw, child, x, y, x - evw._irect.left, y - evw._irect.top, _screen.getButtons());
                sent = true;
            } catch (IOException e) {
                evw.removeSelectingClient(c);
            }
        }

        return sent;
    }

    /**
     * Called when a key is pressed or released while grabbed by this window.
     *
     * @param pressed     Whether the key was pressed or released.
     * @param x           Pointer X coordinate.
     * @param y           Pointer Y coordinate.
     * @param keycode     Keycode of the key.
     * @param grabClient  The grabbing client.
     * @param ownerEvents Owner-events flag.
     */
    public void grabKeyNotify(boolean pressed, int x, int y, int keycode, Client grabClient, boolean ownerEvents) {
        if (ownerEvents) {
            Window w = _screen.getRootWindow().windowAtPoint(x, y);

            if (w.keyNotify(pressed, x, y, keycode, grabClient)) return;
        }

        try {
            if (pressed)
                EventCode.sendKeyPress(grabClient, _xServer.getTimestamp(), keycode, _screen.getRootWindow(), this, null, x, y, x - _irect.left, y - _irect.top, _screen.getButtons());
            else
                EventCode.sendKeyRelease(grabClient, _xServer.getTimestamp(), keycode, _screen.getRootWindow(), this, null, x, y, x - _irect.left, y - _irect.top, _screen.getButtons());
        } catch (IOException e) {
            removeSelectingClient(grabClient);
        }
    }

    /**
     * Return the event mask that would select on the buttons.
     *
     * @param buttonMask Currently pressed pointer buttons.
     * @return The event mask that would select on the buttons.
     */
    private int buttonEventMask(int buttonMask) {
        int mask = EventCode.MaskPointerMotion | EventCode.MaskPointerMotionHint;

        if ((buttonMask & 0x700) == 0) return mask;

        mask |= EventCode.MaskButtonMotion;
        if ((buttonMask & 0x100) != 0) mask |= EventCode.MaskButton1Motion;
        if ((buttonMask & 0x200) != 0) mask |= EventCode.MaskButton2Motion;
        if ((buttonMask & 0x400) != 0) mask |= EventCode.MaskButton3Motion;

        return mask;
    }

    /**
     * Called when the pointer moves within this window.
     *
     * @param x          Pointer X coordinate.
     * @param y          Pointer Y coordinate.
     * @param buttonMask Currently pressed pointer buttons.
     * @param grabClient Only send notify this client if it isn't null.
     * @return True if an event is sent.
     */
    public boolean motionNotify(int x, int y, int buttonMask, Client grabClient) {
        Window evw = this;
        Window child = null;
        int mask = buttonEventMask(buttonMask);
        Vector<Client> sc;

        for (; ; ) {
            if (evw._isMapped) {
                sc = evw.getSelectingClients(mask);
                if (sc != null) break;
            }

            if (evw._parent == null) return false;

            if ((evw._attributes[AttrDoNotPropagateMask] & EventCode.MaskPointerMotion) != 0)
                return false;

            child = evw;
            evw = evw._parent;
        }

        boolean sent = false;

        for (Client c : sc) {
            if (c == null) continue;
            if (grabClient != null && grabClient != c) continue;

            int detail = 0;    // Normal.
            int em = evw.getClientEventMask(c);

            if ((em & EventCode.MaskPointerMotionHint) != 0 && (em & EventCode.MaskPointerMotion) == 0)
                detail = 1;        // Hint.

            try {
                EventCode.sendMotionNotify(c, _xServer.getTimestamp(), detail, _screen.getRootWindow(), evw, child, x, y, x - evw._irect.left, y - evw._irect.top, buttonMask);
            } catch (IOException e) {
                evw.removeSelectingClient(c);
            }
        }

        return sent;
    }

    /**
     * Called when the pointer moves while grabbed by this window.
     *
     * @param x           Pointer X coordinate.
     * @param y           Pointer Y coordinate.
     * @param buttonMask  Currently pressed pointer buttons.
     * @param eventMask   The events the window is interested in.
     * @param grabClient  The grabbing client.
     * @param ownerEvents Owner-events flag.
     */
    public void grabMotionNotify(int x, int y, int buttonMask, int eventMask, Client grabClient, boolean ownerEvents) {
        if (ownerEvents) {
            Window w = _screen.getRootWindow().windowAtPoint(x, y);

            if (w.motionNotify(x, y, buttonMask, grabClient)) return;
        }

        int em = buttonEventMask(buttonMask) & eventMask;

        if (em != 0) {
            int detail = 0;    // Normal.

            if ((em & EventCode.MaskPointerMotionHint) != 0 && (em & EventCode.MaskPointerMotion) == 0)
                detail = 1;        // Hint.

            try {
                EventCode.sendMotionNotify(grabClient, _xServer.getTimestamp(), detail, _screen.getRootWindow(), this, null, x, y, x - _irect.left, y - _irect.top, buttonMask);
            } catch (IOException e) {
                removeSelectingClient(grabClient);
            }
        }
    }

    /**
     * Map the window.
     *
     * @param client The remote client.
     * @throws IOException
     */
    private void map(Client client) throws IOException {
        if (_isMapped) return;

        Vector<Client> sc;

        if (!_overrideRedirect) {
            sc = _parent.getSelectingClients(EventCode.MaskSubstructureRedirect);
            if (sc != null) {
                for (Client c : sc) {
                    if (c == null) continue;
                    if (c != client) {
                        EventCode.sendMapRequest(c, _parent, this);
                        return;
                    }
                }
            }
        }

        _isMapped = true;

        sc = getSelectingClients(EventCode.MaskStructureNotify);
        if (sc != null) {
            for (Client c : sc) {
                if (c == null) continue;
                try {
                    EventCode.sendMapNotify(c, this, this, _overrideRedirect);
                } catch (IOException e) {
                    removeSelectingClient(c);
                }
            }
        }

        sc = _parent.getSelectingClients(EventCode.MaskSubstructureNotify);
        if (sc != null) {
            for (Client c : sc) {
                if (c == null) continue;
                try {
                    EventCode.sendMapNotify(c, _parent, this, _overrideRedirect);
                } catch (IOException e) {
                    removeSelectingClient(c);
                }
            }
        }

        updateAffectedVisibility();

        if (!_exposed) {
            sc = getSelectingClients(EventCode.MaskExposure);
            if (sc != null) {
                for (Client c : sc) {
                    if (c == null) continue;
                    try {
                        EventCode.sendExpose(c, this, 0, 0, _drawable.getWidth(), _drawable.getHeight(), 0);
                    } catch (IOException e) {
                        removeSelectingClient(c);
                    }
                }
            }
            _exposed = true;
        }
    }

    /**
     * Map the children of this window.
     *
     * @param client The remote client.
     * @throws IOException
     */
    private void mapSubwindows(Client client) throws IOException {
        for (Window w : _children) {
            w.map(client);
            w.mapSubwindows(client);
        }
    }

    /**
     * Unmap the window.
     *
     * @throws IOException
     */
    private void unmap() throws IOException {
        if (!_isMapped) return;

        _isMapped = false;

        Vector<Client> sc;

        sc = getSelectingClients(EventCode.MaskStructureNotify);
        if (sc != null) {
            for (Client c : sc) {
                if (c == null) continue;
                try {
                    EventCode.sendUnmapNotify(c, this, this, false);
                } catch (IOException e) {
                    removeSelectingClient(c);
                }
            }
        }

        sc = _parent.getSelectingClients(EventCode.MaskSubstructureNotify);
        if (sc != null) {
            for (Client c : sc) {
                if (c == null) continue;
                try {
                    EventCode.sendUnmapNotify(c, _parent, this, false);
                } catch (IOException e) {
                    removeSelectingClient(c);
                }
            }
        }

        updateAffectedVisibility();
        _screen.revertFocus(this);
    }

    /**
     * Unmap the children of this window.
     *
     * @throws IOException
     */
    private void unmapSubwindows() throws IOException {
        for (Window w : _children) {
            w.unmap();
            w.unmapSubwindows();
        }
    }

    /**
     * Destroy the window and all its children.
     *
     * @param removeFromParent If true, remove it from its parent.
     * @throws IOException
     */
    private void destroy(boolean removeFromParent) throws IOException {
        if (_parent == null)    // No effect on root window.
            return;

        _xServer.freeResource(_id);
        if (_isMapped) unmap();

        for (Window w : _children)
            w.destroy(false);

        _children.clear();

        if (removeFromParent) _parent._children.remove(this);

        Vector<Client> sc;

        sc = getSelectingClients(EventCode.MaskStructureNotify);
        if (sc != null) {
            for (Client c : sc) {
                if (c == null) continue;
                try {
                    EventCode.sendDestroyNotify(c, this, this);
                } catch (IOException e) {
                    removeSelectingClient(c);
                }
            }
        }

        sc = _parent.getSelectingClients(EventCode.MaskSubstructureNotify);
        if (sc != null) {
            for (Client c : sc) {
                if (c == null) continue;
                try {
                    EventCode.sendDestroyNotify(c, _parent, this);
                } catch (IOException e) {
                    removeSelectingClient(c);
                }
            }
        }

        _drawable.getBitmap().recycle();
    }

    /**
     * Change the window's parent.
     *
     * @param client The remote client.
     * @param parent New parent.
     * @param x      New X position relative to new parent.
     * @param y      New Y position relative to new parent.
     * @throws IOException
     */
    private void reparent(Client client, Window parent, int x, int y) throws IOException {
        boolean mapped = _isMapped;

        if (mapped) unmap();

        Rect orig = new Rect(_orect);
        int dx = parent._irect.left + x - _orect.left;
        int dy = parent._irect.top + y - _orect.top;

        _orect.left += dx;
        _orect.top += dy;
        _orect.right += dx;
        _orect.bottom += dy;
        _irect.left += dx;
        _irect.top += dy;
        _irect.right += dx;
        _irect.bottom += dy;

        _parent._children.remove(this);
        parent._children.add(this);

        if (dx != 0 || dy != 0) for (Window w : _children)
            w.move(dx, dy, 0, 0);

        Vector<Client> sc;

        sc = getSelectingClients(EventCode.MaskStructureNotify);
        if (sc != null) {
            for (Client c : sc) {
                if (c == null) continue;
                try {
                    EventCode.sendReparentNotify(c, this, this, parent, x, y, _overrideRedirect);
                } catch (IOException e) {
                    removeSelectingClient(c);
                }
            }
        }

        sc = _parent.getSelectingClients(EventCode.MaskSubstructureNotify);
        if (sc != null) {
            for (Client c : sc) {
                if (c == null) continue;
                try {
                    EventCode.sendReparentNotify(c, _parent, this, parent, x, y, _overrideRedirect);
                } catch (IOException e) {
                    removeSelectingClient(c);
                }
            }
        }

        sc = parent.getSelectingClients(EventCode.MaskSubstructureNotify);
        if (sc != null) {
            for (Client c : sc) {
                if (c == null) continue;
                try {
                    EventCode.sendReparentNotify(c, parent, this, parent, x, y, _overrideRedirect);
                } catch (IOException e) {
                    removeSelectingClient(c);
                }
            }
        }

        _parent = parent;
        if (mapped) {
            map(client);
            if (!_inputOnly) _screen.postInvalidate(orig.left, orig.top, orig.right, orig.bottom);
        }
    }

    /**
     * Circulate occluded windows.
     *
     * @param client    The remote client.
     * @param direction 0=RaiseLowest, 1=LowerHighest.
     * @return True if a window is restacked.
     * @throws IOException
     */
    private boolean circulate(Client client, int direction) throws IOException {
        Window sw = null;

        if (direction == 0) {    // Raise lowest occluded.
            for (Window w : _children) {
                if (occludes(null, w)) {
                    sw = w;
                    break;
                }
            }
        } else {    // Lower highest occluding.
            for (int i = _children.size() - 1; i >= 0; i--) {
                Window w = _children.elementAt(i);

                if (occludes(w, null)) {
                    sw = w;
                    break;
                }
            }
        }

        if (sw == null) return false;

        Vector<Client> sc;

        sc = getSelectingClients(EventCode.MaskSubstructureRedirect);
        if (sc != null) {
            for (Client c : sc) {
                if (c == null) continue;
                if (c != client) {
                    try {
                        EventCode.sendCirculateRequest(c, this, sw, direction);
                        return false;
                    } catch (IOException e) {
                        removeSelectingClient(c);
                    }
                }
            }
        }

        if (direction == 0) {
            _children.remove(sw);
            _children.add(sw);
        } else {
            _children.remove(sw);
            _children.add(0, sw);
        }

        sc = getSelectingClients(EventCode.MaskStructureNotify);
        if (sc != null) {
            for (Client c : sc) {
                if (c == null) continue;
                try {
                    EventCode.sendCirculateNotify(c, this, sw, direction);
                } catch (IOException e) {
                    removeSelectingClient(c);
                }
            }
        }

        sc = _parent.getSelectingClients(EventCode.MaskSubstructureNotify);
        if (sc != null) {
            for (Client c : sc) {
                if (c == null) continue;
                try {
                    EventCode.sendCirculateNotify(c, _parent, sw, direction);
                } catch (IOException e) {
                    removeSelectingClient(c);
                }
            }
        }

        updateAffectedVisibility();

        return true;
    }

    /**
     * Does the first window occlude the second?
     * If the first window is null, does any window occlude w2?
     * If the second window is null, is any window occluded by w1?
     *
     * @param w1 First window.
     * @param w2 Second window.
     * @return True if occlusion occurs.
     */
    private boolean occludes(Window w1, Window w2) {
        if (w1 == null) {
            if (w2 == null || !w2._isMapped) return false;

            // Does anything occlude w2?
            Rect r = w2._orect;
            boolean above = false;

            for (Window w : _children) {
                if (above) {
                    if (w._isMapped && Rect.intersects(w._orect, r)) return true;
                } else {
                    if (w == w2) above = true;
                }
            }
        } else {
            if (w2 == null) {    // Does w1 occlude anything?
                if (!w1._isMapped) return false;

                Rect r = w1._orect;

                for (Window w : _children) {
                    if (w == w1) return false;
                    else if (w._isMapped && Rect.intersects(w._orect, r)) return true;
                }
            } else {    // Does w1 occlude w2?
                if (!w1._isMapped || !w2._isMapped) return false;
                if (!Rect.intersects(w1._orect, w2._orect)) return false;

                return _children.indexOf(w1) > _children.indexOf(w2);
            }
        }

        return false;
    }

    /**
     * Move the window and its children.
     *
     * @param dx X distance to move.
     * @param dy Y distance to move.
     * @param dw The change in the parent's width.
     * @param dh The change in the parent's height.
     * @throws IOException
     */
    private void move(int dx, int dy, int dw, int dh) throws IOException {
        if (dw != 0 || dh != 0) {
            switch (_attributes[AttrWinGravity]) {
                case WinGravityUnmap:
                    unmap();
                    break;
                case WinGravityNorthWest:
                    break;    // No change.
                case WinGravityNorth:
                    dx += dw / 2;
                    break;
                case WinGravityNorthEast:
                    dx += dw;
                    break;
                case WinGravityWest:
                    dy += dh / 2;
                    break;
                case WinGravityCenter:
                    dx += dw / 2;
                    dy += dh / 2;
                    break;
                case WinGravityEast:
                    dx += dw;
                    dy += dh / 2;
                    break;
                case WinGravitySouthWest:
                    dy += dh;
                    break;
                case WinGravitySouth:
                    dx += dw / 2;
                    dy += dh;
                    break;
                case WinGravitySouthEast:
                    dx += dw;
                    dy += dh;
                    break;
                case WinGravityStatic:
                    dx = 0;
                    dy = 0;
                    break;
            }

            Vector<Client> sc;

            sc = getSelectingClients(EventCode.MaskStructureNotify);
            if (sc != null) {
                for (Client c : sc) {
                    if (c == null) continue;
                    try {
                        EventCode.sendGravityNotify(c, this, this, _orect.left + dx - _parent._irect.left, _orect.top + dy - _parent._irect.top);
                    } catch (IOException e) {
                        removeSelectingClient(c);
                    }
                }
            }

            sc = _parent.getSelectingClients(EventCode.MaskSubstructureNotify);
            if (sc != null) {
                for (Client c : sc) {
                    if (c == null) continue;
                    try {
                        EventCode.sendGravityNotify(c, _parent, this, _orect.left + dx - _parent._irect.left, _orect.top + dy - _parent._irect.top);
                    } catch (IOException e) {
                        removeSelectingClient(c);
                    }
                }
            }
        }

        if (dx == 0 && dy == 0) return;

        _irect.left += dx;
        _irect.right += dx;
        _irect.top += dy;
        _irect.bottom += dy;
        _orect.left += dx;
        _orect.right += dx;
        _orect.top += dy;
        _orect.bottom += dy;

        for (Window w : _children)
            w.move(dx, dy, 0, 0);
    }

    /**
     * Process a ConfigureWindow request.
     *
     * @param client         The remote client.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @return True if the window needs to be redrawn.
     * @throws IOException
     */
    private boolean processConfigureWindow(Client client, int bytesRemaining) throws IOException {
        InputOutput io = client.getInputOutput();

        if (bytesRemaining < 4) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, RequestCode.ConfigureWindow, 0);
            return false;
        }

        int mask = io.readShort();    // Value mask.
        int n = Util.bitcount(mask);

        io.readSkip(2);    // Unused.
        bytesRemaining -= 4;
        if (bytesRemaining != 4 * n) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, RequestCode.ConfigureWindow, 0);
            return false;
        } else if (_parent == null) {    // No effect on root window.
            io.readSkip(bytesRemaining);
            return false;
        }

        int oldLeft = _irect.left;
        int oldTop = _irect.top;
        int oldWidth = _irect.right - _irect.left;
        int oldHeight = _irect.bottom - _irect.top;
        int oldX = _orect.left - _parent._irect.left;
        int oldY = _orect.top - _parent._irect.top;
        int width = oldWidth;
        int height = oldHeight;
        int x = oldX;
        int y = oldY;
        int borderWidth = _borderWidth;
        int stackMode = 0;
        boolean changed = false;
        Window sibling = null;
        Rect dirty = null;

        if ((mask & 0x01) != 0) {
            x = (short) io.readShort();    // X.
            io.readSkip(2);    // Unused.
        }
        if ((mask & 0x02) != 0) {
            y = (short) io.readShort();    // Y.
            io.readSkip(2);    // Unused.
        }
        if ((mask & 0x04) != 0) {
            width = (short) io.readShort();    // Width.
            io.readSkip(2);    // Unused.
        }
        if ((mask & 0x08) != 0) {
            height = (short) io.readShort();    // Height.
            io.readSkip(2);    // Unused.
        }
        if ((mask & 0x10) != 0) {
            borderWidth = (short) io.readShort();    // Border width.
            io.readSkip(2);    // Unused.
        }
        if ((mask & 0x20) != 0) {
            int id = io.readInt();    // Sibling.
            Resource r = _xServer.getResource(id);

            if (r == null || r.getType() != WINDOW) {
                ErrorCode.write(client, ErrorCode.Window, RequestCode.ConfigureWindow, id);
                io.readSkip(bytesRemaining);
                return false;
            } else {
                sibling = (Window) r;
            }
        }
        if ((mask & 0x40) != 0) {
            stackMode = io.readByte();    // Stack mode.
            io.readSkip(3);    // Unused.
        }

        if (!_overrideRedirect) {
            Vector<Client> sc;

            sc = _parent.getSelectingClients(EventCode.MaskSubstructureRedirect);
            if (sc != null) {
                for (Client c : sc) {
                    if (c == null) continue;
                    if (c != client) {
                        EventCode.sendConfigureRequest(c, stackMode, _parent, this, sibling, x, y, width, height, borderWidth, mask);
                        return false;
                    }
                }
            }
        }

        if (width != oldWidth || height != oldHeight) {
            if (width <= 0 || height <= 0) {
                ErrorCode.write(client, ErrorCode.Value, RequestCode.ConfigureWindow, 0);
                return false;
            }

            Vector<Client> sc;

            sc = getSelectingClients(EventCode.MaskResizeRedirect);
            if (sc != null) {
                for (Client c : sc) {
                    if (c == null) continue;
                    if (c != client) {
                        EventCode.sendResizeRequest(c, this, width, height);
                        width = oldWidth;
                        height = oldHeight;
                        break;
                    }
                }
            }
        }

        if (x != oldX || y != oldY || width != oldWidth || height != oldHeight || borderWidth != _borderWidth) {
            if (width != oldWidth || height != oldHeight) {
                try {
                    _drawable = new Drawable(width, height, 32, _backgroundBitmap, _attributes[AttrBackgroundPixel] | 0xff000000);
                } catch (OutOfMemoryError e) {
                    ErrorCode.write(client, ErrorCode.Alloc, RequestCode.ConfigureWindow, 0);
                    return false;
                }

                _drawable.clear();
                _exposed = false;
            }

            dirty = new Rect(_orect);
            _borderWidth = borderWidth;
            _orect.left = _parent._irect.left + x;
            _orect.top = _parent._irect.top + y;
            _orect.right = _orect.left + width + 2 * borderWidth;
            _orect.bottom = _orect.top + height + 2 * borderWidth;
            _irect.left = _orect.left + borderWidth;
            _irect.top = _orect.top + borderWidth;
            _irect.right = _orect.right - borderWidth;
            _irect.bottom = _orect.bottom - borderWidth;
            changed = true;
        }

        if ((mask & 0x60) != 0) {
            if (sibling != null && sibling._parent != _parent) {
                ErrorCode.write(client, ErrorCode.Match, RequestCode.ConfigureWindow, 0);
                return false;
            }

            if (sibling == null) {
                switch (stackMode) {
                    case 0:    // Above.
                        _parent._children.remove(this);
                        _parent._children.add(this);
                        changed = true;
                        break;
                    case 1:    // Below.
                        _parent._children.remove(this);
                        _parent._children.add(0, this);
                        changed = true;
                        break;
                    case 2:    // TopIf.
                        if (_parent.occludes(null, this)) {
                            _parent._children.remove(this);
                            _parent._children.add(this);
                            changed = true;
                        }
                        break;
                    case 3:    // BottomIf.
                        if (_parent.occludes(this, null)) {
                            _parent._children.remove(this);
                            _parent._children.add(0, this);
                            changed = true;
                        }
                        break;
                    case 4:    // Opposite.
                        if (_parent.occludes(null, this)) {
                            _parent._children.remove(this);
                            _parent._children.add(this);
                            changed = true;
                        } else if (_parent.occludes(this, null)) {
                            _parent._children.remove(this);
                            _parent._children.add(0, this);
                            changed = true;
                        }
                        break;
                }
            } else {
                int pos;

                switch (stackMode) {
                    case 0:    // Above.
                        _parent._children.remove(this);
                        pos = _parent._children.indexOf(sibling);
                        _parent._children.add(pos + 1, this);
                        changed = true;
                        break;
                    case 1:    // Below.
                        _parent._children.remove(this);
                        pos = _parent._children.indexOf(sibling);
                        _parent._children.add(pos, this);
                        changed = true;
                        break;
                    case 2:    // TopIf.
                        if (_parent.occludes(sibling, this)) {
                            _parent._children.remove(this);
                            _parent._children.add(this);
                            changed = true;
                        }
                        break;
                    case 3:    // BottomIf.
                        if (_parent.occludes(this, sibling)) {
                            _parent._children.remove(this);
                            _parent._children.add(0, this);
                            changed = true;
                        }
                        break;
                    case 4:    // Opposite.
                        if (_parent.occludes(sibling, this)) {
                            _parent._children.remove(this);
                            _parent._children.add(this);
                            changed = true;
                        } else if (_parent.occludes(this, sibling)) {
                            _parent._children.remove(this);
                            _parent._children.add(0, this);
                            changed = true;
                        }
                        break;
                }
            }
        }

        if (changed) {
            Vector<Client> sc;

            sc = getSelectingClients(EventCode.MaskStructureNotify);
            if (sc != null) {
                for (Client c : sc) {
                    if (c == null) continue;
                    EventCode.sendConfigureNotify(c, this, this, null, x, y, width, height, _borderWidth, _overrideRedirect);
                }
            }

            sc = _parent.getSelectingClients(EventCode.MaskSubstructureNotify);
            if (sc != null) {
                for (Client c : sc) {
                    if (c == null) continue;
                    EventCode.sendConfigureNotify(c, _parent, this, null, x, y, width, height, _borderWidth, _overrideRedirect);
                }
            }

            if (_irect.left != oldLeft || _irect.top != oldTop || width != oldWidth || height != oldHeight)
                for (Window w : _children)
                    w.move(_irect.left - oldLeft, _irect.top - oldTop, width - oldWidth, height - oldHeight);

            updateAffectedVisibility();
        }

        if (!_exposed) {
            Vector<Client> sc;

            if ((sc = getSelectingClients(EventCode.MaskExposure)) != null) {
                for (Client c : sc) {
                    if (c == null) continue;
                    EventCode.sendExpose(c, this, 0, 0, _drawable.getWidth(), _drawable.getHeight(), 0);
                }
            }
            _exposed = true;
        }

        if (dirty != null && _isMapped && !_inputOnly)
            _screen.postInvalidate(dirty.left, dirty.top, dirty.right, dirty.bottom);

        return changed;
    }

    /**
     * Process an X request relating to this window.
     *
     * @param client         The remote client.
     * @param opcode         The request's opcode.
     * @param arg            Optional first argument.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    @Override
    public void processRequest(Client client, byte opcode, byte arg, int bytesRemaining) throws IOException {
        boolean redraw = false;
        boolean updatePointer = false;
        InputOutput io = client.getInputOutput();

        switch (opcode) {
            case RequestCode.ChangeWindowAttributes:
                redraw = processWindowAttributes(client, RequestCode.ChangeWindowAttributes, bytesRemaining);
                updatePointer = true;
                break;
            case RequestCode.GetWindowAttributes:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int vid = _xServer.getRootVisual().getId();
                    byte mapState = (byte) (_isMapped ? 0 : 2);

                    synchronized (io) {
                        Util.writeReplyHeader(client, (byte) 2);
                        io.writeInt(3);    // Reply length.
                        io.writeInt(vid);    // Visual.
                        io.writeShort((short) (_inputOnly ? 2 : 1));    // Class.
                        io.writeByte((byte) _attributes[AttrBitGravity]);
                        io.writeByte((byte) _attributes[AttrWinGravity]);
                        io.writeInt(_attributes[AttrBackingPlanes]);
                        io.writeInt(_attributes[AttrBackingPixel]);
                        io.writeByte((byte) _attributes[AttrSaveUnder]);
                        io.writeByte((byte) 1);    // Map is installed.
                        io.writeByte(mapState);    // Map-state.
                        io.writeByte((byte) (_overrideRedirect ? 1 : 0));
                        io.writeInt(_colormap.getId());    // Colormap.
                        io.writeInt(_attributes[AttrEventMask]);
                        io.writeInt(_attributes[AttrEventMask]);
                        io.writeShort((short) _attributes[AttrDoNotPropagateMask]);
                        io.writePadBytes(2);    // Unused.
                    }
                    io.flush();
                }
                break;
            case RequestCode.DestroyWindow:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    destroy(true);
                    redraw = true;
                    updatePointer = true;
                }
                break;
            case RequestCode.DestroySubwindows:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    for (Window w : _children)
                        w.destroy(false);
                    _children.clear();
                    redraw = true;
                    updatePointer = true;
                }
                break;
            case RequestCode.ChangeSaveSet:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    // Do nothing.
                }
                break;
            case RequestCode.ReparentWindow:
                if (bytesRemaining != 8) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int id = io.readInt();    // Parent.
                    int x = (short) io.readShort();    // X.
                    int y = (short) io.readShort();    // Y.
                    Resource r = _xServer.getResource(id);

                    if (r == null || r.getType() != WINDOW) {
                        ErrorCode.write(client, ErrorCode.Window, opcode, id);
                    } else {
                        reparent(client, (Window) r, x, y);
                        redraw = true;
                        updatePointer = true;
                    }
                }
                break;
            case RequestCode.MapWindow:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    map(client);
                    redraw = true;
                    updatePointer = true;
                }
                break;
            case RequestCode.MapSubwindows:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    mapSubwindows(client);
                    redraw = true;
                    updatePointer = true;
                }
                break;
            case RequestCode.UnmapWindow:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    unmap();
                    redraw = true;
                    updatePointer = true;
                }
                break;
            case RequestCode.UnmapSubwindows:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    unmapSubwindows();
                    redraw = true;
                    updatePointer = true;
                }
                break;
            case RequestCode.ConfigureWindow:
                redraw = processConfigureWindow(client, bytesRemaining);
                updatePointer = true;
                break;
            case RequestCode.CirculateWindow:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    redraw = circulate(client, arg);
                    updatePointer = true;
                }
                break;
            case RequestCode.GetGeometry:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int rid = _screen.getRootWindow().getId();
                    byte depth = _xServer.getRootVisual().getDepth();
                    int x, y;
                    int width = _irect.right - _irect.left;
                    int height = _irect.bottom - _irect.top;

                    if (_parent == null) {
                        x = _orect.left;
                        y = _orect.top;
                    } else {
                        x = _orect.left - _parent._irect.left;
                        y = _orect.top - _parent._irect.top;
                    }

                    synchronized (io) {
                        Util.writeReplyHeader(client, depth);
                        io.writeInt(0);    // Reply length.
                        io.writeInt(rid);    // Root.
                        io.writeShort((short) x);    // X.
                        io.writeShort((short) y);    // Y.
                        io.writeShort((short) width);    // Width.
                        io.writeShort((short) height);    // Height.
                        io.writeShort((short) _borderWidth);    // Border wid.
                        io.writePadBytes(10);    // Unused.
                    }
                    io.flush();
                }
                break;
            case RequestCode.QueryTree:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int rid = _screen.getRootWindow().getId();
                    int pid = (_parent == null) ? 0 : _parent.getId();

                    synchronized (io) {
                        Util.writeReplyHeader(client, (byte) 0);
                        io.writeInt(_children.size());    // Reply length.
                        io.writeInt(rid);    // Root.
                        io.writeInt(pid);    // Parent.
                        // Number of children.
                        io.writeShort((short) _children.size());
                        io.writePadBytes(14);    // Unused.

                        for (Window w : _children)
                            io.writeInt(w.getId());
                    }
                    io.flush();
                }
                break;
            case RequestCode.ChangeProperty:
            case RequestCode.GetProperty:
            case RequestCode.RotateProperties:
                Property.processRequest(_xServer, client, arg, opcode, bytesRemaining, this, _properties);
                break;
            case RequestCode.DeleteProperty:
                if (bytesRemaining != 4) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int id = io.readInt();    // Property.
                    Atom a = _xServer.getAtom(id);

                    if (a == null) {
                        ErrorCode.write(client, ErrorCode.Atom, opcode, id);
                    } else if (_properties.containsKey(id)) {
                        Vector<Client> sc = getSelectingClients(EventCode.MaskPropertyChange);

                        _properties.remove(id);
                        if (sc != null) {
                            for (Client c : sc) {
                                if (c == null) continue;
                                try {
                                    EventCode.sendPropertyNotify(c, this, a, _xServer.getTimestamp(), 1);
                                } catch (IOException e) {
                                    removeSelectingClient(c);
                                }
                            }
                        }
                    }
                }
                break;
            case RequestCode.ListProperties:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int n = _properties.size();

                    synchronized (io) {
                        Util.writeReplyHeader(client, (byte) 0);
                        io.writeInt(n);    // Reply length.
                        io.writeShort((short) n);    // Num atoms.
                        io.writePadBytes(22);    // Unused.

                        for (Property p : _properties.values())
                            io.writeInt(p.getId());
                    }
                    io.flush();
                }
                break;
            case RequestCode.QueryPointer:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int rid = _screen.getRootWindow().getId();
                    int rx = _screen.getPointerX();
                    int ry = _screen.getPointerY();
                    int mask = _screen.getButtons();
                    int wx = rx - _irect.left;
                    int wy = ry - _irect.top;
                    Window w = windowAtPoint(rx, ry);
                    int cid = 0;

                    if (w._parent == this) cid = w.getId();

                    synchronized (io) {
                        Util.writeReplyHeader(client, (byte) 1);
                        io.writeInt(0);    // Reply length.
                        io.writeInt(rid);    // Root.
                        io.writeInt(cid);    // Child.
                        io.writeShort((short) rx);    // Root X.
                        io.writeShort((short) ry);    // Root Y.
                        io.writeShort((short) wx);    // Win X.
                        io.writeShort((short) wy);    // Win Y.
                        io.writeShort((short) mask);    // Mask.
                        io.writePadBytes(6);    // Unused.
                    }
                    io.flush();
                }
                break;
            case RequestCode.GetMotionEvents:
                if (bytesRemaining != 8) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int numEvents = 0;    // Do nothing.

                    io.readInt();    // Start time.
                    io.readInt();    // Stop time.

                    synchronized (io) {
                        Util.writeReplyHeader(client, (byte) 0);
                        io.writeInt(numEvents * 2);    // Reply length.
                        io.writeInt(numEvents);    // Number of events.
                        io.writePadBytes(20);    // Unused.
                    }
                    io.flush();
                }
                break;
            case RequestCode.TranslateCoordinates:
                if (bytesRemaining != 8) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int id = io.readInt();    // Destination window.
                    int x = (short) io.readShort();    // Source X.
                    int y = (short) io.readShort();    // Source Y.
                    Resource r = _xServer.getResource(id);

                    if (r == null || r.getType() != WINDOW) {
                        ErrorCode.write(client, ErrorCode.Window, opcode, id);
                    } else {
                        Window w = (Window) r;
                        int dx = _irect.left + x - w._irect.left;
                        int dy = _irect.top + y - w._irect.top;
                        int child = 0;

                        for (Window c : w._children)
                            if (c._isMapped && c._irect.contains(x, y)) child = c._id;

                        synchronized (io) {
                            Util.writeReplyHeader(client, (byte) 1);
                            io.writeInt(0);    // Reply length.
                            io.writeInt(child);    // Child.
                            io.writeShort((short) dx);    // Dest X.
                            io.writeShort((short) dy);    // Dest Y.
                            io.writePadBytes(16);    // Unused.
                        }
                        io.flush();
                    }
                }
                break;
            case RequestCode.ClearArea:
                if (bytesRemaining != 8) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int x = (short) io.readShort();    // Source X.
                    int y = (short) io.readShort();    // Source Y.
                    int width = io.readShort();    // Width.
                    int height = io.readShort();    // Height.

                    if (width == 0) width = _drawable.getWidth() - x;
                    if (height == 0) height = _drawable.getHeight() - y;
                    _drawable.clearArea(x, y, width, height);
                    invalidate(x, y, width, height);

                    if (arg == 1) {
                        Vector<Client> sc;

                        sc = getSelectingClients(EventCode.MaskExposure);
                        if (sc != null) for (Client c : sc){
                            if (c == null) continue;
                            EventCode.sendExpose(c, this, x, y, width, height, 0);
                        }
                    }
                }
                break;
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
                redraw = _drawable.processRequest(_xServer, client, _id, opcode, arg, bytesRemaining);
                break;
            case RequestCode.ListInstalledColormaps:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    _screen.writeInstalledColormaps(client);
                }
                break;
            default:
                if(opcode<0) {
                    redraw = false;
                    Extensions.processRequest(_xServer, client, opcode, arg, bytesRemaining);
                }
                else{
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Implementation, opcode, 0);
                }
                break;
        }

        if (redraw) {
            invalidate();
            if (updatePointer) _screen.updatePointer(0);
        }
    }

    /**
     * Calculate a window's visibility.
     *
     * @return The window's visibility.
     */
    private int calculateVisibility() {
        if (_inputOnly) return NotViewable;

        int result = Unobscured;

        for (Window aw = this; aw._parent != null; aw = aw._parent) {
            if (!_isMapped) return NotViewable;    // All ancestors must be mapped.

            if (result == FullyObscured) continue;    // Keep checking in case ancestor is unmapped.

            boolean above = false;

            for (Window w : aw._parent._children) {
                if (!w._isMapped) continue;

                if (above) {
                    if (Rect.intersects(w._orect, _orect)) {
                        if (w._orect.contains(_orect)) {
                            result = FullyObscured;
                            break;
                        }

                        result = PartiallyObscured;
                    }
                } else if (w == aw) {
                    above = true;
                }
            }
        }

        return result;
    }

    /**
     * Update the visibility of this window and its children.
     */
    private void updateVisibility() {
        Vector<Client> sc = getSelectingClients(EventCode.MaskVisibilityChange);

        if (sc != null) {
            int visibility = calculateVisibility();

            if (visibility != _visibility) {
                _visibility = visibility;
                if (visibility != NotViewable) {
                    for (Client c : sc) {
                        if (c == null) continue;
                        try {
                            EventCode.sendVisibilityNotify(c, this, visibility);
                        } catch (IOException e) {
                            removeSelectingClient(c);
                        }
                    }
                }
            }
        }

        for (Window w : _children)
            w.updateVisibility();
    }

    /**
     * Update the visibility of all the windows that might have been
     * affected by changes to this window.
     */
    private void updateAffectedVisibility() {
        if (_parent == null) {
            updateVisibility();
            return;
        }

        for (Window w : _parent._children) {
            w.updateVisibility();
            if (w == this) break;
        }
    }

    /**
     * Allows adding a new property to this window.
     * @param p property to add.
     */
    public void addProperty(Property p){
        _properties.put(p.getId(),p);
    }

    /**
     * Returns a property assigned to this window.
     * @param id Id of the property.
     * @return Property or null if not found.
     */
    public Property getProperty(int id) {
        return _properties.get(id);
    }
}