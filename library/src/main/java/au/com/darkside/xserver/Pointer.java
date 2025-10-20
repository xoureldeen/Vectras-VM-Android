package au.com.darkside.xserver;

import android.graphics.Rect;

import java.io.IOException;


/**
 * This class handles an X pointer.
 *
 * @author Matthew Kwan
 */
public class Pointer {
    private byte[] _buttonMap = {1, 2, 3};

    /**
     * Return the virtual button that a physical button has been mapped to.
     * Zero indicates the button has been disabled.
     *
     * @param button The physical button: 1, 2, or 3.
     * @return The virtual button, or 0 for disabled.
     */
    public int mapButton(int button) {
        if (button < 1 || button > _buttonMap.length) return 0;

        return _buttonMap[button - 1];
    }

    /**
     * Process a WarpPointer request.
     *
     * @param xServer The X server.
     * @param client  The remote client.
     * @throws IOException
     */
    public void processWarpPointer(XServer xServer, Client client) throws IOException {
        InputOutput io = client.getInputOutput();
        int swin = io.readInt();    // Source window.
        int dwin = io.readInt();    // Destination window.
        int sx = io.readShort();    // Source X.
        int sy = io.readShort();    // Source Y.
        int width = io.readShort();    // Source width.
        int height = io.readShort();    // Source height.
        int dx = io.readShort();    // Destination X.
        int dy = io.readShort();    // Destination Y.
        ScreenView screen = xServer.getScreen();
        boolean ok = true;
        int x, y;

        if (dwin == 0) {
            x = screen.getPointerX() + dx;
            y = screen.getPointerY() + dy;
        } else {
            Resource r = xServer.getResource(dwin);

            if (r == null || r.getType() != Resource.WINDOW) {
                ErrorCode.write(client, ErrorCode.Window, RequestCode.WarpPointer, dwin);
                ok = false;
            }

            Rect rect = ((Window) r).getIRect();

            x = rect.left + dx;
            y = rect.top + dy;
        }

        if (swin != 0) {
            Resource r = xServer.getResource(swin);

            if (r == null || r.getType() != Resource.WINDOW) {
                ErrorCode.write(client, ErrorCode.Window, RequestCode.WarpPointer, swin);
                ok = false;
            } else {
                Window w = (Window) r;
                Rect rect = w.getIRect();

                sx += rect.left;
                sy += rect.top;

                if (width == 0) width = rect.right - sx;
                if (height == 0) height = rect.bottom - sy;

                if (x < sx || x >= sx + width || y < sy || y >= sy + height) ok = false;
            }
        }

        if (ok) screen.updatePointerPosition(x, y, 0);
    }

    /**
     * Process an X request relating to the pointers.
     *
     * @param xServer        The X server.
     * @param client         The remote client.
     * @param opcode         The request's opcode.
     * @param arg            Optional first argument.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    public void processRequest(XServer xServer, Client client, byte opcode, byte arg, int bytesRemaining) throws IOException {
        InputOutput io = client.getInputOutput();

        switch (opcode) {
            case RequestCode.WarpPointer:
                if (bytesRemaining != 20) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    processWarpPointer(xServer, client);
                }
                break;
            case RequestCode.ChangePointerControl:
                if (bytesRemaining != 8) ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                io.readSkip(bytesRemaining);
                break;    // Do nothing.
            case RequestCode.GetPointerControl:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    synchronized (io) {
                        Util.writeReplyHeader(client, (byte) 0);
                        io.writeInt(0);    // Reply length.
                        io.writeShort((short) 1);    // Acceleration numerator.
                        io.writeShort((short) 1);    // Acceleration denom.
                        io.writeShort((short) 1);    // Threshold.
                        io.writePadBytes(18);    // Unused.
                    }
                    io.flush();
                }
                break;
            case RequestCode.SetPointerMapping:
                if (bytesRemaining != arg + (-arg & 3)) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else if (arg != _buttonMap.length) {
                    ErrorCode.write(client, ErrorCode.Value, opcode, 0);
                } else {
                    io.readBytes(_buttonMap, 0, arg);
                    io.readSkip(-arg & 3);    // Unused.

                    synchronized (io) {
                        Util.writeReplyHeader(client, (byte) 0);
                        io.writeInt(0);    // Reply length.
                        io.writePadBytes(24);    // Unused.
                    }
                    io.flush();

                    xServer.sendMappingNotify(2, 0, 0);
                }
                break;
            case RequestCode.GetPointerMapping:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    byte n = (byte) _buttonMap.length;
                    int pad = -n & 3;

                    synchronized (io) {
                        Util.writeReplyHeader(client, n);
                        io.writeInt((n + pad) / 4);    // Reply length.
                        io.writePadBytes(24);    // Unused.

                        io.writeBytes(_buttonMap, 0, n);    // Map.
                        io.writePadBytes(pad);    // Unused.
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
}