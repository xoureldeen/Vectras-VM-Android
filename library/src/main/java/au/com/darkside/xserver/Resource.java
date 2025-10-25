package au.com.darkside.xserver;

import java.io.IOException;

/**
 * This class handles details of a server resource.
 *
 * @author Matthew Kwan
 */
public class Resource {
    public static final int WINDOW = 1;
    public static final int PIXMAP = 2;
    public static final int CURSOR = 3;
    public static final int FONT = 4;
    public static final int GCONTEXT = 5;
    public static final int COLORMAP = 6;

    private final int _type;
    protected final int _id;
    protected final XServer _xServer;
    protected Client _client;
    private int _closeDownMode = Client.Destroy;

    /**
     * Constructor.
     *
     * @param type    The resource type.
     * @param id      The resource ID.
     * @param xServer The X server.
     * @param client  The client issuing the request.
     */
    protected Resource(int type, int id, XServer xServer, Client client) {
        _type = type;
        _id = id;
        _xServer = xServer;
        _client = client;
    }

    /**
     * Return the resource type.
     *
     * @return The resource type.
     */
    public int getType() {
        return _type;
    }

    /**
     * Return the resource ID.
     *
     * @return The resource ID.
     */
    public int getId() {
        return _id;
    }

    /**
     * Return the client that created the resource.
     *
     * @return The client that created the resource.
     */
    public Client getClient() {
        return _client;
    }

    /**
     * Return the resource's close down mode.
     *
     * @return The resource's close down mode.
     */
    public int getCloseDownMode() {
        return _closeDownMode;
    }

    /**
     * Set the close down mode of the resource.
     *
     * @param mode The mode used to destroy the resource.
     */
    public void setCloseDownMode(int mode) {
        _closeDownMode = mode;
    }

    /**
     * Is the resource a drawable? (Window or Pixmap)
     *
     * @return Whether the resource is a drawable.
     */
    public boolean isDrawable() {
        return (_type == WINDOW || _type == PIXMAP);
    }

    /**
     * Is the resource a fontable? (Font or GContext)
     *
     * @return Whether the resource is a fontable.
     */
    public boolean isFontable() {
        return (_type == FONT || _type == GCONTEXT);
    }

    /**
     * Destroy the resource.
     * Remove it from the X server's resources, and override this function
     * to handle object-specific removals.
     */
    public void delete() {
        _xServer.freeResource(_id);
    }

    /**
     * Process an X request relating to this resource.
     * This is a fallback function that does nothing.
     *
     * @param client         The remote client.
     * @param opcode         The request's opcode.
     * @param arg            Optional first argument.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    public void processRequest(Client client, byte opcode, byte arg, int bytesRemaining) throws IOException {
    }
}