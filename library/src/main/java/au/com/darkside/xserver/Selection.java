package au.com.darkside.xserver;

import java.io.IOException;


/**
 * This class implements a selection.
 *
 * @author Matthew KWan
 */
public class Selection {
    private final int _id;
    private Client _owner = null;
    private Window _ownerWindow = null;
    private int _lastChangeTime = 0;

    /**
     * Constructor.
     *
     * @param id The selection's ID.
     */
    public Selection(int id) {
        _id = id;
    }

    /**
     * Return the selection's atom ID.
     *
     * @return The selection's atom ID.
     */
    public int getId() {
        return _id;
    }

    /**
     * If the selection is owned by the client, clear it.
     * This occurs when a client disconnects.
     *
     * @param client The disconnecting client.
     */
    public void clearClient(Client client) {
        if (_owner == client) {
            _owner = null;
            _ownerWindow = null;
        }
    }

    /**
     * Allows transfering a selection to a (window-)property.
     * @param xServer xServer to perform this action on.
     * @param fromSelectionAtom Atom associated with the source selection. (i.e. "CLIPBOARD")
     * @param toPropertyAtom Atom associated with the target property (i.e. "CLIPBOARD" of target window).
     * @param toWindow  Window holding toPropertyAtom. (the propertyAtom can be reused, so we need to specify the window which holds the property this atom is associated with)
     * @param transferTypeAtom Atom describing the clipboard type (i.e. "UTF8_STRING").
     */
    public static void transferSelectionRequest(XServer xServer, Atom fromSelectionAtom, Atom toPropertyAtom, Window toWindow, Atom transferTypeAtom){
        Selection srcSelection = xServer.getSelection(fromSelectionAtom.getId());
        if(srcSelection == null)
            return; // selection not owned by any client
        try{
            if(srcSelection._owner != null) {
                EventCode.sendSelectionRequest(srcSelection._owner, xServer.getTimestamp(), srcSelection._ownerWindow, toWindow, fromSelectionAtom, transferTypeAtom, toPropertyAtom);
            }
        }
        catch (IOException e){
            // silently fail
        }
    }

    /**
     * Allows changing/setting the owner of a selection.
     * @param xServer xServer to perform this action on.
     * @param selectionAtom Atom associated with the selection. (i.e. "CLIPBOARD")
     * @param owner New owner window.
     */
    public static void setSelectionOwner(XServer xServer, Atom selectionAtom, Window owner) {
        Selection sel = xServer.getSelection(selectionAtom.getId());

        if (sel == null) {
            sel = new Selection(selectionAtom.getId());
            xServer.addSelection(sel);
        }

        // remove old owner
        if (sel._owner != null && sel._owner != owner.getClient()){
            try{
                EventCode.sendSelectionClear(sel._owner, sel._lastChangeTime, owner, selectionAtom);
            }
            catch (IOException e){
                // silently fail
            }
        }
        sel._lastChangeTime = xServer.getTimestamp();
        sel._ownerWindow = owner;
        sel._owner = owner.getClient();
    }

    /**
     * Process an X request relating to selections.
     *
     * @param xServer        The X server.
     * @param client         The client issuing the request.
     * @param opcode         The request's opcode.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    public static void processRequest(XServer xServer, Client client, byte opcode, int bytesRemaining) throws IOException {
        InputOutput io = client.getInputOutput();

        switch (opcode) {
            case RequestCode.SetSelectionOwner:
                processSetSelectionOwnerRequest(xServer, client, bytesRemaining);
                break;
            case RequestCode.GetSelectionOwner:
                if (bytesRemaining != 4) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int aid = io.readInt();    // Selection atom.

                    if (!xServer.atomExists(aid)) {
                        ErrorCode.write(client, ErrorCode.Atom, RequestCode.SetSelectionOwner, aid);
                    } else {
                        int wid = 0;
                        Selection sel = xServer.getSelection(aid);

                        if (sel != null && sel._ownerWindow != null) wid = sel._ownerWindow.getId();

                        synchronized (io) {
                            Util.writeReplyHeader(client, (byte) 0);
                            io.writeInt(0);    // Reply length.
                            io.writeInt(wid);    // Owner.
                            io.writePadBytes(20);    // Unused.
                        }
                        io.flush();
                    }
                }
                break;
            case RequestCode.ConvertSelection:
                processConvertSelectionRequest(xServer, client, bytesRemaining);
                break;
            default:
                io.readSkip(bytesRemaining);
                ErrorCode.write(client, ErrorCode.Implementation, opcode, 0);
                break;
        }
    }

    /**
     * Process a SetSelectionOwner request.
     * Change the owner of the specified selection.
     *
     * @param xServer        The X server.
     * @param client         The client issuing the request.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    public static void processSetSelectionOwnerRequest(XServer xServer, Client client, int bytesRemaining) throws IOException {
        InputOutput io = client.getInputOutput();

        if (bytesRemaining != 12) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, RequestCode.SetSelectionOwner, 0);
            return;
        }

        int wid = io.readInt();    // Owner window.
        int aid = io.readInt();    // Selection atom.
        int time = io.readInt();    // Timestamp.
        Window w = null;

        if (wid != 0) {
            Resource r = xServer.getResource(wid);

            if (r == null || r.getType() != Resource.WINDOW) {
                ErrorCode.write(client, ErrorCode.Window, RequestCode.SetSelectionOwner, wid);
                return;
            }

            w = (Window) r;
        }

        Atom a = xServer.getAtom(aid);

        if (a == null) {
            ErrorCode.write(client, ErrorCode.Atom, RequestCode.SetSelectionOwner, aid);
            return;
        }

        Selection sel = xServer.getSelection(aid);

        if (sel == null) {
            sel = new Selection(aid);
            xServer.addSelection(sel);
        }

        int now = xServer.getTimestamp();

        if (time != 0) {
            if (time < sel._lastChangeTime || time >= now) return;
        } else {
            time = now;
        }

        sel._lastChangeTime = time;
        sel._ownerWindow = w;

        if (sel._owner != null && sel._owner != client)
            EventCode.sendSelectionClear(sel._owner, time, w, a);

        sel._owner = (w != null) ? client : null;
        if (xServer.getScreen().hasSharedClipboard() && aid == xServer.findAtom("CLIPBOARD").getId())
            transferSelectionRequest(xServer, xServer.findAtom("CLIPBOARD"), xServer.findAtom("CLIPBOARD"), xServer.getScreen().getSharedClipboardWindow() , xServer.findAtom("UTF8_STRING"));
        else if (xServer.getScreen().hasSharedClipboard() && aid == xServer.findAtom("PRIMARY").getId())
            transferSelectionRequest(xServer, xServer.findAtom("PRIMARY"), xServer.findAtom("PRIMARY"), xServer.getScreen().getSharedClipboardWindow() , xServer.findAtom("UTF8_STRING"));
        

    }

    /**
     * Process a ConvertSelection request.
     *
     * @param xServer        The X server.
     * @param client         The client issuing the request.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    public static void processConvertSelectionRequest(XServer xServer, Client client, int bytesRemaining) throws IOException {
        InputOutput io = client.getInputOutput();

        if (bytesRemaining != 20) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, RequestCode.ConvertSelection, 0);
            return;
        }

        int wid = io.readInt();    // Requestor.
        int sid = io.readInt();    // Selection.
        int tid = io.readInt();    // Target.
        int pid = io.readInt();    // Property.
        int time = io.readInt();    // Time.
        Resource r = xServer.getResource(wid);
        Window w;
        Atom selectionAtom, targetAtom, propertyAtom;

        if (r == null || r.getType() != Resource.WINDOW) {
            ErrorCode.write(client, ErrorCode.Window, RequestCode.ConvertSelection, wid);
            return;
        } else {
            w = (Window) r;
        }

        selectionAtom = xServer.getAtom(sid);
        if (selectionAtom == null) {
            ErrorCode.write(client, ErrorCode.Atom, RequestCode.ConvertSelection, sid);
            return;
        }

        targetAtom = xServer.getAtom(tid);
        if (targetAtom == null) {
            ErrorCode.write(client, ErrorCode.Atom, RequestCode.ConvertSelection, tid);
            return;
        }

        propertyAtom = null;
        if (pid != 0 && (propertyAtom = xServer.getAtom(pid)) == null) {
            ErrorCode.write(client, ErrorCode.Atom, RequestCode.ConvertSelection, pid);
            return;
        }

        Client owner = null;
        Selection sel = xServer.getSelection(sid);

        if (sel != null) owner = sel._owner;

        Window ownerWindow = sel != null ? sel._ownerWindow : null;
        // move the property to the client the dirty way,... some interfaces would make sense here
        if(ownerWindow != null && ownerWindow.isServerWindow() && owner == null){
            Property target = w.getProperty(pid);
            if(target == null){
                target = new Property(pid, tid, (byte)8);
                w.addProperty(target);
            }
            target.setData(sel._ownerWindow.getProperty(sid).getData());
            target.setType(tid);
        }
        
        if (owner != null) {
            try {
                EventCode.sendSelectionRequest(owner, time, sel._ownerWindow, w, selectionAtom, targetAtom, propertyAtom);
            } catch (IOException e) {
            }
        } else {
            EventCode.sendSelectionNotify(client, time, w, selectionAtom, targetAtom, propertyAtom);
        }
    }
}