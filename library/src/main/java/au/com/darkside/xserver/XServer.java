package au.com.darkside.xserver;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import au.com.darkside.xserver.Xext.Extensions;
import au.com.darkside.xserver.Xext.XShape;
import au.com.darkside.xserver.Xext.XSync;

/**
 * This class implements an X Windows server.
 *
 * @author Matthew Kwan
 */
public class XServer {

    static public abstract class OnXSeverStartListener {
        public abstract void onStart();
    }

    public final short ProtocolMajorVersion = 11;
    public final short ProtocolMinorVersion = 0;
    public final String vendor = "Open source";
    public final int ReleaseNumber = 0;

    private final int _port;
    private final Context _context;
    private final String _windowManagerClass;
    private final Vector<Format> _formats;
    private final Hashtable<Integer, Resource> _resources;

    private final Vector<Client> _clients;
    private final int _clientIdBits = 20;
    private final int _clientIdStep = (1 << _clientIdBits);
    private int _clientIdBase = _clientIdStep;

    private final Hashtable<Integer, Atom> _atoms;
    private final Hashtable<String, Atom> _atomNames;
    private int _maxAtomId = 0;
    private final Hashtable<Integer, Selection> _selections;

    private final Keyboard _keyboard;
    private final Pointer _pointer;
    private final Font _defaultFont;
    private final Visual _rootVisual;
    private ScreenView _screen = null;
    private String[] _fontPath = null;
    private AcceptThread _acceptThread = null;
    private long _timestamp;
    private Client _grabClient;

    private int _screenSaverTimeout = 0;
    private int _screenSaverInterval = 0;
    private int _preferBlanking = 1;
    private int _allowExposures = 0;
    private long _screenSaverTime = 0;
    private CountDownTimer _screenSaverCountDownTimer = null;

    private boolean _accessControlEnabled = false;
    private final HashSet<Integer> _accessControlHosts;

    private final Hashtable<String, Extension> _extensions;
    private OnXSeverStartListener _onStartListener = null;

    /**
     * Constructor.
     *
     * @param c                  The application context.
     * @param port               The port to listen on. Usually 6000.
     * @param windowManagerClass Window manager class name. Can be null.
     */
    public XServer(Context c, int port, String windowManagerClass) {
        _context = c;
        _port = port;
        _windowManagerClass = windowManagerClass;
        _formats = new Vector<Format>();
        _resources = new Hashtable<Integer, Resource>();
        _clients = new Vector<Client>();
        _atoms = new Hashtable<Integer, Atom>();
        _atomNames = new Hashtable<String, Atom>();
        _selections = new Hashtable<Integer, Selection>();
        _accessControlHosts = new HashSet<Integer>();

        Extensions.Initialize();
        _extensions = new Hashtable<String, Extension>();
        _extensions.put("Generic Event Extension", new Extension(Extensions.XGE, (byte) 0, (byte) 0));
        _extensions.put("BIG-REQUESTS", new Extension(Extensions.BigRequests, (byte) 0, (byte) 0));
        _extensions.put("SHAPE", new Extension(Extensions.Shape, XShape.EventBase, (byte) 0));
        //_extensions.put("SYNC", new Extension(Extensions.Sync, XSync.EventBase, XSync.ErrorBase));
        _extensions.put("XTEST", new Extension(Extensions.XTEST, (byte) 0, (byte) 0));

        _formats.add(new Format((byte) 32, (byte) 24, (byte) 8));

        _keyboard = new Keyboard();
        _pointer = new Pointer();

        _defaultFont = new Font(1, this, null, null);
        addResource(_defaultFont);
        addResource(new Cursor(2, this, null, (Font) null, (Font) null, 0, 1, 0xff000000, 0xffffffff));

        _screen = new ScreenView(_context, this, 3, pixelsPerMillimeter());

        Colormap cmap = new Colormap(4, this, null, _screen);

        cmap.setInstalled(true);
        addResource(cmap);

        _rootVisual = new Visual(1);
        Atom.registerPredefinedAtoms(this);

        _timestamp = System.currentTimeMillis();
    }

    public void setOnStartListener(OnXSeverStartListener l){
        _onStartListener = l;
    }

    /**
     * Start the thread that listens on the socket.
     * Also start the window manager if one is specified.
     *
     * @return True if the thread is started successfully.
     */
    public synchronized boolean start() {
        if (_acceptThread != null) return true;    // Already running.

        try {
            _acceptThread = new AcceptThread(_port);
            _acceptThread.start();
        } catch (IOException e) {
            return false;
        }

        if (_windowManagerClass != null) {
            int idx = _windowManagerClass.lastIndexOf('.');

            if (idx > 0) {
                String pkg = _windowManagerClass.substring(0, idx);
                Intent intent = new Intent(Intent.ACTION_MAIN);

                intent.setComponent(new ComponentName(pkg, _windowManagerClass));

                try {
                    if (_context.startService(intent) == null)
                        Log.e("XServer", "Could not start " + _windowManagerClass);
                } catch (SecurityException e) {
                    Log.e("XServer", "Could not start " + _windowManagerClass + ": " + e.getMessage());
                }
            }
        }

        resetScreenSaver();

        if(_onStartListener != null) _onStartListener.onStart();

        return true;
    }

    /**
     * Stop listening on the socket and terminate all clients.
     */
    public synchronized void stop() {
        if (_acceptThread != null) {
            _acceptThread.cancel();
            _acceptThread = null;
        }

        _grabClient = null;
        while (!_clients.isEmpty()) _clients.get(0).cancel();
    }

    /**
     * Reset the server.
     * This should be called when the last client disconnects with a
     * close-down mode of Destroy.
     */
    private void reset() {
        Iterator<Integer> it = _resources.keySet().iterator();
        while (it.hasNext()) {
            Integer entry = it.next();
            if (entry > _clientIdStep) _resources.remove(entry);
        }

        _screen.removeNonDefaultColormaps();

        if (_atoms.size() != Atom.numPredefinedAtoms()) {
            _atoms.clear();
            _atomNames.clear();
            Atom.registerPredefinedAtoms(this);
        }

        _selections.clear();
        _timestamp = System.currentTimeMillis();
    }

    /**
     * Return the server's application context.
     *
     * @return The server's application context.
     */
    public Context getContext() {
        return _context;
    }

    /**
     * Return the internet address the server is listening on.
     *
     * @return The internet address the server is listening on.
     */
    public InetAddress getInetAddress() {
        if (_acceptThread == null) return null;

        return _acceptThread.getInetAddress();
    }

    /**
     * Return the number of milliseconds since the last reset.
     *
     * @return The number of milliseconds since the last reset.
     */
    public int getTimestamp() {
        long diff = System.currentTimeMillis() - _timestamp;

        if (diff <= 0) return 1;

        return (int) diff;
    }

    /**
     * Remove a client from the list of active clients.
     *
     * @param client The client to remove.
     */
    public void removeClient(Client client) {
        for (Selection sel : _selections.values())
            sel.clearClient(client);

        _clients.remove(client);
        if (_grabClient == client) _grabClient = null;

        if (client.getCloseDownMode() == Client.Destroy && _clients.size() == 0) reset();
    }

    /**
     * Disable all clients except this one.
     *
     * @param client The client issuing the grab.
     */
    public void grabServer(Client client) {
        _grabClient = client;
    }

    /**
     * End the server grab.
     *
     * @param client The client issuing the grab.
     */
    public void ungrabServer(Client client) {
        if (_grabClient == client) _grabClient = null;
    }

    /**
     * Return true if processing is allowed. This is only false if the
     * server has been grabbed by another client and the checking client
     * is not impervious to server grabs.
     *
     * @param client The client checking if processing is allowed.
     * @return True if processing is allowed for the client.
     */
    public boolean processingAllowed(Client client) {
        if (_grabClient == null || client.getImperviousToServerGrabs()) return true;

        return _grabClient == client;
    }

    /**
     * Get the X server's keyboard.
     *
     * @return The keyboard used by the X server.
     */
    public Keyboard getKeyboard() {
        return _keyboard;
    }

    /**
     * Get the X server's pointer.
     *
     * @return The pointer used by the X server.
     */
    public Pointer getPointer() {
        return _pointer;
    }

    /**
     * Get the server's font path.
     *
     * @return The server's font path.
     */
    public String[] getFontPath() {
        return _fontPath;
    }

    /**
     * Set the server's font path.
     *
     * @param path The new font path.
     */
    public void setFontPath(String[] path) {
        _fontPath = path;
    }

    /**
     * Return the screen attached to the display.
     *
     * @return The screen attached to the display.
     */
    public ScreenView getScreen() {
        return _screen;
    }

    /**
     * Return the number of pixels per millimeter on the display.
     *
     * @return The number of pixels per millimeter on the display.
     */
    private float pixelsPerMillimeter() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) _context.getSystemService(Context.WINDOW_SERVICE);

        wm.getDefaultDisplay().getMetrics(metrics);
        Font.setDpi((int) metrics.ydpi);    // Use the value since we have it.

        return metrics.xdpi / 25.4f;
    }

    /**
     * Get the number of pixmap formats.
     *
     * @return The number of pixmap formats.
     */
    public int getNumFormats() {
        return _formats.size();
    }

    /**
     * Write details of all the pixmap formats.
     *
     * @param io The input/output stream.
     * @throws IOException
     */
    public void writeFormats(InputOutput io) throws IOException {
        for (Format f : _formats)
            f.write(io);
    }

    /**
     * Return the default font.
     *
     * @return The default font.
     */
    public Font getDefaultFont() {
        return _defaultFont;
    }

    /**
     * Return the root visual.
     *
     * @return The root visual.
     */
    public Visual getRootVisual() {
        return _rootVisual;
    }

    /**
     * Add an atom.
     *
     * @param a The atom to add.
     */
    public void addAtom(Atom a) {
        _atoms.put(a.getId(), a);
        _atomNames.put(a.getName(), a);

        if (a.getId() > _maxAtomId) _maxAtomId = a.getId();
    }

    /**
     * Return the atom with the specified ID.
     *
     * @param id The atom ID.
     * @return The specified atom, or null if it doesn't exist.
     */
    public Atom getAtom(int id) {
        if (!_atoms.containsKey(id))    // No such atom.
            return null;

        return _atoms.get(id);
    }

    /**
     * Return the atom with the specified name.
     *
     * @param name The atom's name.
     * @return The specified atom, or null if it doesn't exist.
     */
    public Atom findAtom(final String name) {
        if (!_atomNames.containsKey(name)) return null;

        return _atomNames.get(name);
    }

    /**
     * Does the atom with specified ID exist?
     *
     * @param id The atom ID.
     * @return True if an atom with the ID exists.
     */
    public boolean atomExists(int id) {
        return _atoms.containsKey(id);
    }

    /**
     * Get the ID of the next free atom.
     *
     * @return The ID of the next free atom.
     */
    public int nextFreeAtomId() {
        return ++_maxAtomId;
    }

    /**
     * Return the selection with the specified ID.
     *
     * @param id The selection ID.
     * @return The specified selection, or null if it doesn't exist.
     */
    public Selection getSelection(int id) {
        if (!_selections.containsKey(id))    // No such selection.
            return null;

        return _selections.get(id);
    }

    /**
     * Add a selection.
     *
     * @param sel The selection to add.
     */
    public void addSelection(Selection sel) {
        _selections.put(sel.getId(), sel);
    }

    /**
     * Add a resource.
     *
     * @param r The resource to add.
     */
    public void addResource(Resource r) {
        _resources.put(r.getId(), r);
    }

    /**
     * Return the resource with the specified ID.
     *
     * @param id The resource ID.
     * @return The specified resource, or null if it doesn't exist.
     */
    public Resource getResource(int id) {
        if (!resourceExists(id)) return null;

        return _resources.get(id);
    }

    /**
     * Does the resource with specified ID exist?
     *
     * @param id The resource ID.
     * @return True if a resource with the ID exists.
     */
    public boolean resourceExists(int id) {
        return _resources.containsKey(id);
    }

    /**
     * Free the resource with the specified ID.
     *
     * @param id The resource ID.
     */
    public void freeResource(int id) {
        _resources.remove(id);
    }

    /**
     * Get next free resource id.
     *
     * @param id The resource ID.
     */
    public int nextFreeResourceId() {
        int maxKey = 0;  
        for (int cur : _resources.keySet())
            if (cur > maxKey)
                maxKey = cur;  
        return maxKey++; 
    }

    /**
     * If client is null, destroy the resources of all clients that have
     * terminated in RetainTemporary mode. Otherwise destroy all resources
     * associated with the client, which has terminated with mode
     * RetainPermanant or RetainTemporary.
     *
     * @param client The terminated client, or null.
     */
    public synchronized void destroyClientResources(Client client) {
        Collection<Resource> rc = _resources.values();
        Vector<Resource> dl = new Vector<Resource>();

        if (client == null) {
            for (Resource r : rc) {
                Client c = r.getClient();
                boolean disconnected = (c == null || !c.isConnected());

                if (disconnected && r.getCloseDownMode() == Client.RetainTemporary) dl.add(r);
            }
        } else {
            for (Resource r : rc)
                if (r.getClient() == client) dl.add(r);
        }

        for (Resource r : dl)
            r.delete();
    }

    /**
     * Send a MappingNotify to all clients.
     *
     * @param request      0=Modifier, 1=Keyboard, 2=Pointer
     * @param firstKeycode First keycode in new keyboard map.
     * @param keycodeCount Number of keycodes in new keyboard map.
     */
    public void sendMappingNotify(int request, int firstKeycode, int keycodeCount) {
        for (Client c : _clients) {
            if (c == null) continue;
            try {
                EventCode.sendMappingNotify(c, request, firstKeycode, keycodeCount);
            } catch (IOException e) {
            }
        }
    }

    /**
     * Process a QueryExtension request.
     *
     * @param client         The remote client.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    public void processQueryExtensionRequest(Client client, int bytesRemaining) throws IOException {
        InputOutput io = client.getInputOutput();

        if (bytesRemaining < 4) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, RequestCode.QueryExtension, 0);
            return;
        }

        int length = io.readShort();    // Length of name.
        int pad = -length & 3;

        io.readSkip(2);    // Unused.
        bytesRemaining -= 4;

        if (bytesRemaining != length + pad) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, RequestCode.QueryExtension, 0);
            return;
        }

        byte[] bytes = new byte[length];

        io.readBytes(bytes, 0, length);
        io.readSkip(pad);    // Unused.

        String s = new String(bytes);
        Extension e;

        if (_extensions.containsKey(s)) e = _extensions.get(s);
        else e = null;

        synchronized (io) {
            Util.writeReplyHeader(client, (byte) 0);
            io.writeInt(0);    // Reply length.

            if (e == null) {
                io.writeByte((byte) 0);    // Present. 0 = false.
                io.writeByte((byte) 0);    // Major opcode.
                io.writeByte((byte) 0);    // First event.
                io.writeByte((byte) 0);    // First error.
            } else {
                io.writeByte((byte) 1);    // Present. 1 = true.
                io.writeByte(e.majorOpcode);    // Major opcode.
                io.writeByte(e.firstEvent);    // First event.
                io.writeByte(e.firstError);    // First error.
            }

            io.writePadBytes(20);    // Unused.
        }
        io.flush();
    }

    /**
     * Write the list of extensions supported by the server.
     *
     * @param client The remote client.
     * @throws IOException
     */
    public void writeListExtensions(Client client) throws IOException {
        Set<String> ss = _extensions.keySet();
        int length = 0;

        for (String s : ss)
            length += s.length() + 1;

        int pad = -length & 3;
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            Util.writeReplyHeader(client, (byte) ss.size());
            io.writeInt((length + pad) / 4);    // Reply length.
            io.writePadBytes(24);    // Unused.

            for (String s : ss) {
                byte[] ba = s.getBytes();

                io.writeByte((byte) ba.length);
                io.writeBytes(ba, 0, ba.length);
            }

            io.writePadBytes(pad);    // Unused.
        }
        io.flush();
    }

    /**
     * Process a ChangeHosts request.
     *
     * @param client         The remote client.
     * @param mode           Change mode. 0=Insert, 1=Delete.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    public void processChangeHostsRequest(Client client, int mode, int bytesRemaining) throws IOException {
        InputOutput io = client.getInputOutput();

        if (bytesRemaining < 4) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, RequestCode.ChangeHosts, 0);
            return;
        }

        int family = io.readByte();    // 0=Inet, 1=DECnet, 2=Chaos.

        io.readSkip(1);    // Unused.

        int length = io.readShort();    // Length of address.
        int pad = -length & 3;

        bytesRemaining -= 4;
        if (bytesRemaining != length + pad) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, RequestCode.ChangeHosts, 0);
            return;
        }

        if (family != 0 || length != 4) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Value, RequestCode.ChangeHosts, 0);
            return;
        }

        int address = 0;

        for (int i = 0; i < length; i++)
            address = (address << 8) | io.readByte();

        io.readSkip(pad);    // Unused.

        if (mode == 0) _accessControlHosts.add(address);
        else _accessControlHosts.remove(address);
    }

    /**
     * Reply to a ListHosts request.
     *
     * @param client The remote client.
     * @throws IOException
     */
    public void writeListHosts(Client client) throws IOException {
        InputOutput io = client.getInputOutput();
        int n = _accessControlHosts.size();

        synchronized (io) {
            Util.writeReplyHeader(client, (byte) (_accessControlEnabled ? 1 : 0));
            io.writeInt(n * 2);    // Reply length.
            io.writeShort((short) n);    // Number of hosts.
            io.writePadBytes(22);    // Unused.

            for (int addr : _accessControlHosts) {
                io.writeByte((byte) 0);    // Family = Internet.
                io.writePadBytes(1);    // Unused.
                io.writeShort((short) 4);    // Length of address.
                io.writeByte((byte) ((addr >> 24) & 0xff));
                io.writeByte((byte) ((addr >> 16) & 0xff));
                io.writeByte((byte) ((addr >> 8) & 0xff));
                io.writeByte((byte) (addr & 0xff));
            }
        }
        io.flush();
    }

    /**
     * Enable/disable access control.
     *
     * @param enabled If true, enable access control.
     */
    public void setAccessControl(boolean enabled) {
        _accessControlEnabled = enabled;
    }

    /**
     * Get the list of hosts that are allowed to connect.
     * This can be modified.
     *
     * @return The set of addresses that are allowed to connect.
     */
    public HashSet<Integer> getAccessControlHosts() {
        return _accessControlHosts;
    }

    /**
     * Is a client from the specified address allowed to connect?
     *
     * @param address The client's IP address, MSB format.
     * @return True if the client is allowed to exist.
     */
    private boolean isAccessAllowed(int address) {
        if (!_accessControlEnabled) return true;

        return _accessControlHosts.contains(address);
    }

    /**
     * Set the screen saver parameters.
     *
     * @param timeout        Timeout period, in seconds. 0=disabled, -1=default.
     * @param interval       Interval in seconds. 0=disabled, -1=default.
     * @param preferBlanking 0=No, 1=Yes, 2=Default.
     * @param allowExposures 0=No, 1=Yes, 2=Default.
     */
    public void setScreenSaver(int timeout, int interval, int preferBlanking, int allowExposures) {
        if (timeout == -1) _screenSaverTimeout = 0;    // Default timeout.
        else _screenSaverTimeout = timeout;

        if (interval == -1) _screenSaverInterval = 0;    // Default interval.
        else _screenSaverInterval = interval;

        _preferBlanking = preferBlanking;
        _allowExposures = allowExposures;

        resetScreenSaver();
    }

    /**
     * Called when we'd potentially want to blank the screen.
     */
    private void checkScreenBlank() {
        if (_screenSaverTimeout == 0)    // Disabled.
            return;

        long offset = (_screenSaverTime + _screenSaverTimeout) * 1000 - System.currentTimeMillis() / 1000;

        if (offset < 1000) {
            _screen.blank(true);
            return;
        }

        _screenSaverCountDownTimer = new CountDownTimer(offset, offset + 1) {
            public void onTick(long millis) {
            }

            public void onFinish() {
                _screenSaverCountDownTimer = null;
                checkScreenBlank();
            }
        };
        _screenSaverCountDownTimer.start();
    }

    /**
     * Reset the screen saver timer.
     */
    public void resetScreenSaver() {
        long now = System.currentTimeMillis() / 1000;

        if (now == _screenSaverTime) return;

        _screenSaverTime = now;
        if (_screenSaverCountDownTimer == null) checkScreenBlank();
    }

    /**
     * Reply to GetScreenSaver request.
     *
     * @param client The remote client.
     * @throws IOException
     */
    public void writeScreenSaver(Client client) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            Util.writeReplyHeader(client, (byte) 0);
            io.writeInt(0);    // Reply length.
            io.writeShort((short) _screenSaverTimeout);    // Timeout.
            io.writeShort((short) _screenSaverInterval);    // Interval.
            io.writeByte((byte) _preferBlanking);    // Prefer blanking.
            io.writeByte((byte) _allowExposures);    // Allow exposures.
            io.writePadBytes(18);    // Unused.
        }
        io.flush();
    }

    /**
     * This class holds details of an extension.
     *
     * @author Matthew Kwan
     */
    private class Extension {
        private final byte majorOpcode;
        private final byte firstEvent;
        private final byte firstError;

        /**
         * Constructor.
         *
         * @param pmajorOpcode Major opcode of the extension, or zero.
         * @param pfirstEvent  Base event type code, or zero.
         * @param pfirstError  Base error code, or zero.
         */
        public Extension(byte pmajorOpcode, byte pfirstEvent, byte pfirstError) {
            majorOpcode = pmajorOpcode;
            firstEvent = pfirstEvent;
            firstError = pfirstError;
        }
    }

    /**
     * This thread runs while listening for incoming connections.
     * It runs until it is cancelled.
     *
     * @author Matthew Kwan
     */
    private class AcceptThread extends Thread {
        private final ServerSocket _serverSocket;

        /**
         * Constructor.
         *
         * @param port The port to listen on.
         * @throws IOException
         */
        AcceptThread(int port) throws IOException {
            _serverSocket = new ServerSocket(port);
        }

        /**
         * Return the internet address that is accepting connections.
         * May be null.
         *
         * @return The internet address that is accepting connections.
         */
        public InetAddress getInetAddress() {
            return _serverSocket.getInetAddress();
        }

        /**
         * Run the thread.
         */
        public void run() {
            while (true) {
                Socket socket;

                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception.
                    socket = _serverSocket.accept();
                } catch (IOException e) {
                    break;
                }

                int addr = 0;
                InetSocketAddress isa;

                isa = (InetSocketAddress) socket.getRemoteSocketAddress();
                if (isa != null) {
                    InetAddress ia = isa.getAddress();
                    byte[] ba = ia.getAddress();

                    addr = ((ba[0] & 0xff) << 24) | ((ba[1] & 0xff) << 16) | ((ba[2] & 0xff) << 8) | (ba[3] & 0xff);
                }

                if (addr != 0 && !isAccessAllowed(addr)) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                    }
                    continue;
                }

                synchronized (this) {
                    Client c;

                    try {
                        c = new Client(XServer.this, socket, _clientIdBase, _clientIdStep - 1);
                        _clients.add(c);
                        c.start();
                        _clientIdBase += _clientIdStep;
                    } catch (IOException e) {
                        try {
                            socket.close();
                        } catch (IOException e2) {
                        }
                    }
                }
            }
        }

        /**
         * Cancel the thread.
         */
        public void cancel() {
            try {
                _serverSocket.close();
            } catch (IOException e) {
            }
        }
    }
}