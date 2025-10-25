package au.com.darkside.xserver;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class implements a property.
 *
 * @author Matthew KWan
 */
public class Property {

    /**
     * Listener Object which allows listening on property change events.
     */
    static public abstract class OnPropertyChangedListener {
        /**
         * @param data New data after the ChangeProperty request
         * @param type Atom describing the property data (i.e. "UTF8_STRING")
         */
        public abstract void onPropertyChanged(byte[] data, Atom type);
    }

    private final int _id;
    private int _type;
    private byte _format;
    private byte[] _data = null;
    private OnPropertyChangedListener _onPropertyChange = null;

    /**
     * Allows setting a callback object to listen on property change events.
     * @param l Callback object to listen on property change events
     */
    public void setOnPropertyChangedListener(OnPropertyChangedListener l){
        _onPropertyChange = l;
    }

    /**
     * Constructor.
     *
     * @param id     The property's ID.
     * @param type   The ID of the property's type atom.
     * @param format Data format = 8, 16, or 32.
     */
    public Property(int id, int type, byte format) {
        _id = id;
        _type = type;
        _format = format;
    }

    /**
     * Constructor.
     *
     * @param p The property to copy.
     */
    private Property(final Property p) {
        _id = p._id;
        _type = p._type;
        _format = p._format;
        _data = p._data;
    }

    /**
     * Return the property's atom ID.
     *
     * @return The property's atom ID.
     */
    public int getId() {
        return _id;
    }

    /**
     * Allows setting the data held by this property.
     * @param d Data to set.
     */
    public void setData(byte[] d) {
        _data = d;
    }

    /**
     * @return Data held by this property
     */
    public byte[] getData() {
        return _data;
    }

    /**
     * Allows setting the type of this property.
     * @param id Atom ID of atom describing this property type.
     */
    public void setType(int id) {
        _type = id;
    }

    /**
     * Allows setting the data held by this property.
     * @param d Data to set as string, will be converted to bytes.
     */
    public void setData(String d) {
        _data = d.getBytes();
    }

    /**
     * Process an X request relating to properties.
     *
     * @param xServer        The X server.
     * @param client         The remote client.
     * @param arg            Optional first argument.
     * @param opcode         The request's opcode.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @param w              The window containing the properties.
     * @param properties     Hash table of the window's properties.
     * @throws IOException
     */
    public static void processRequest(XServer xServer, Client client, byte arg, byte opcode, int bytesRemaining, Window w, Hashtable<Integer, Property> properties) throws IOException {
        switch (opcode) {
            case RequestCode.ChangeProperty:
                processChangePropertyRequest(xServer, client, arg, bytesRemaining, w, properties);
                break;
            case RequestCode.GetProperty:
                processGetPropertyRequest(xServer, client, arg == 1, bytesRemaining, w, properties);
                break;
            case RequestCode.RotateProperties:
                processRotatePropertiesRequest(xServer, client, bytesRemaining, w, properties);
                break;
            default:
                InputOutput io = client.getInputOutput();

                io.readSkip(bytesRemaining);
                ErrorCode.write(client, ErrorCode.Implementation, opcode, 0);
                break;
        }
    }

    /**
     * Process a ChangeProperty request.
     * Change the owner of the specified selection.
     *
     * @param xServer        The X server.
     * @param client         The remote client.
     * @param mode           0=Replace 1=Prepend 2=Append.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @param w              The window containing the properties.
     * @param properties     Hash table of the window's properties.
     * @throws IOException
     */
    public static void processChangePropertyRequest(XServer xServer, Client client, byte mode, int bytesRemaining, Window w, Hashtable<Integer, Property> properties) throws IOException {
        InputOutput io = client.getInputOutput();

        if (bytesRemaining < 16) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, RequestCode.ChangeProperty, 0);
            return;
        }

        int pid = io.readInt();    // Property atom.
        int tid = io.readInt();    // Type atom.
        byte format = (byte) io.readByte();    // Format.

        io.readSkip(3);    // Unused.

        int length = io.readInt();    // Length of data.
        int n, pad;

        if (format == 8) n = length;
        else if (format == 16) n = length * 2;
        else n = length * 4;

        pad = -n & 3;

        bytesRemaining -= 16;
        if (bytesRemaining != n + pad) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, RequestCode.ChangeProperty, 0);
            return;
        }

        byte[] data = new byte[n];

        io.readBytes(data, 0, n);
        io.readSkip(pad);    // Unused.

        Atom property = xServer.getAtom(pid);

        if (property == null) {
            ErrorCode.write(client, ErrorCode.Atom, RequestCode.ChangeProperty, pid);
            return;
        }

        if (!xServer.atomExists(tid)) {
            ErrorCode.write(client, ErrorCode.Atom, RequestCode.ChangeProperty, tid);
            return;
        }

        Property p;

        if (properties.containsKey(pid)) {
            p = properties.get(pid);
        } else {
            p = new Property(pid, tid, format);
            properties.put(pid, p);
        }

        if (mode == 0) {    // Replace.
            p._type = tid;
            p._format = format;
            p._data = data;
        } else {
            if (tid != p._type || format != p._format) {
                ErrorCode.write(client, ErrorCode.Match, RequestCode.ChangeProperty, 0);
                return;
            }

            if (p._data == null) {
                p._data = data;
            } else {
                byte[] d1, d2;

                if (mode == 1) {    // Prepend.
                    d1 = data;
                    d2 = p._data;
                } else {    // Append.
                    d1 = p._data;
                    d2 = data;
                }

                p._data = new byte[d1.length + d2.length];
                System.arraycopy(d1, 0, p._data, 0, d1.length);
                System.arraycopy(d2, 0, p._data, d1.length, d2.length);
            }
        }

        Vector<Client> sc;

        if ((sc = w.getSelectingClients(EventCode.MaskPropertyChange)) != null) {
            for (Client c : sc) {
                if (c == null) continue;
                EventCode.sendPropertyNotify(c, w, property, xServer.getTimestamp(), 0);
            }
        }

        // trigger callback for event change if existent
        if(p._onPropertyChange != null) p._onPropertyChange.onPropertyChanged(p._data, xServer.getAtom(tid));
    }

    /**
     * Process a GetProperty request.
     *
     * @param xServer        The X server.
     * @param client         The remote client.
     * @param delete         Delete flag.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @param w              The window containing the properties.
     * @param properties     Hash table of the window's properties.
     * @throws IOException
     */
    public static void processGetPropertyRequest(XServer xServer, Client client, boolean delete, int bytesRemaining, Window w, Hashtable<Integer, Property> properties) throws IOException {
        InputOutput io = client.getInputOutput();

        if (bytesRemaining != 16) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, RequestCode.GetProperty, 0);
            return;
        }

        int pid = io.readInt();    // Property.
        int tid = io.readInt();    // Type.
        int longOffset = io.readInt();    // Long offset.
        int longLength = io.readInt();    // Long length.
        Atom property = xServer.getAtom(pid);

        if (property == null) {
            ErrorCode.write(client, ErrorCode.Atom, RequestCode.GetProperty, pid);
            return;
        } else if (tid != 0 && !xServer.atomExists(tid)) {
            ErrorCode.write(client, ErrorCode.Atom, RequestCode.GetProperty, tid);
            return;
        }

        byte format = 0;
        int bytesAfter = 0;
        byte[] value = null;
        boolean generateNotify = false;

        if (properties.containsKey(pid)) {
            Property p = properties.get(pid);

            tid = p._type;
            format = p._format;

            if (tid != 0 && tid != p._type) {
                bytesAfter = (p._data == null) ? 0 : p._data.length;
            } else {
                int n, i, t, l;

                n = (p._data == null) ? 0 : p._data.length;
                i = 4 * longOffset;
                t = n - i;

                if (longLength < 0 || longLength > 536870911)
                    longLength = 536870911;    // Prevent overflow.

                if (t < longLength * 4) l = t;
                else l = longLength * 4;

                bytesAfter = n - (i + l);

                if (l < 0) {
                    ErrorCode.write(client, ErrorCode.Value, RequestCode.GetProperty, 0);
                    return;
                }

                if (l > 0) {
                    value = new byte[l];
                    System.arraycopy(p._data, i, value, 0, l);
                }

                if (delete && bytesAfter == 0) {
                    properties.remove(pid);
                    generateNotify = true;
                }
            }
        } else {
            tid = 0;
        }

        int length = (value == null) ? 0 : value.length;
        int pad = -length & 3;
        int valueLength;

        if (format == 8) valueLength = length;
        else if (format == 16) valueLength = length / 2;
        else if (format == 32) valueLength = length / 4;
        else valueLength = 0;

        synchronized (io) {
            Util.writeReplyHeader(client, format);
            io.writeInt((length + pad) / 4);    // Reply length.
            io.writeInt(tid);    // Type.
            io.writeInt(bytesAfter);    // Bytes after.
            io.writeInt(valueLength);    // Value length.
            io.writePadBytes(12);    // Unused.

            if (value != null) {
                io.writeBytes(value, 0, value.length);    // Value.
                io.writePadBytes(pad);    // Unused.
            }
        }
        io.flush();

        if (generateNotify) {
            Vector<Client> sc;

            if ((sc = w.getSelectingClients(EventCode.MaskPropertyChange)) != null) {
                for (Client c : sc) {
                    if (c == null) continue;
                    EventCode.sendPropertyNotify(c, w, property, xServer.getTimestamp(), 1);
                }
            }
        }
    }

    /**
     * Process a RotateProperties request.
     *
     * @param xServer        The X server.
     * @param client         The remote client.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @param w              The window containing the properties.
     * @param properties     Hash table of the window's properties.
     * @throws IOException
     */
    public static void processRotatePropertiesRequest(XServer xServer, Client client, int bytesRemaining, Window w, Hashtable<Integer, Property> properties) throws IOException {
        InputOutput io = client.getInputOutput();

        if (bytesRemaining < 4) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, RequestCode.RotateProperties, 0);
            return;
        }

        int n = io.readShort();    // Num properties.
        int delta = io.readShort();    // Delta.

        bytesRemaining -= 4;
        if (bytesRemaining != n * 4) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, RequestCode.RotateProperties, 0);
            return;
        }

        if (n == 0 || (delta % n) == 0) return;

        int[] aids = new int[n];
        Property[] props = new Property[n];
        Property[] pcopy = new Property[n];

        for (int i = 0; i < n; i++)
            aids[i] = io.readInt();

        for (int i = 0; i < n; i++) {
            if (!xServer.atomExists(aids[i])) {
                ErrorCode.write(client, ErrorCode.Atom, RequestCode.RotateProperties, aids[i]);
                return;
            } else if (!properties.containsKey(aids[i])) {
                ErrorCode.write(client, ErrorCode.Match, RequestCode.RotateProperties, aids[i]);
                return;
            } else {
                props[i] = properties.get(aids[i]);
                pcopy[i] = new Property(props[i]);
            }
        }

        for (int i = 0; i < n; i++) {
            Property p = props[i];
            Property pc = pcopy[(i + delta) % n];

            p._type = pc._type;
            p._format = pc._format;
            p._data = pc._data;
        }

        Vector<Client> sc;

        if ((sc = w.getSelectingClients(EventCode.MaskPropertyChange)) != null) {
            for (int i = 0; i < n; i++) {
                for (Client c : sc) {
                    if (c == null) continue;
                    EventCode.sendPropertyNotify(c, w, xServer.getAtom(aids[i]), xServer.getTimestamp(), 0);
                }
            }
        }
    }
}