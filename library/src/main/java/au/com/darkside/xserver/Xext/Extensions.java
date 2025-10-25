package au.com.darkside.xserver.Xext;

import java.io.IOException;

import au.com.darkside.xserver.Client;
import au.com.darkside.xserver.ErrorCode;
import au.com.darkside.xserver.InputOutput;
import au.com.darkside.xserver.Util;
import au.com.darkside.xserver.XServer;

/**
 * This class handles requests relating to extensions.
 *
 * @author mkwan
 */
public class Extensions {
    public static final byte XGE = -128;
    public static final byte XTEST = -124;
    public static final byte Sync = -127;
    public static final byte BigRequests = -126;
    public static final byte Shape = -125;

    static public void Initialize(){
        XSync.Initialize();
    }

    /**
     * Process a request relating to an X extension.
     *
     * @param xServer        The X server.
     * @param client         The remote client.
     * @param opcode         The request's opcode.
     * @param arg            Optional first argument.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    public static void processRequest(XServer xServer, Client client, byte opcode, byte arg, int bytesRemaining) throws IOException {
        InputOutput io = client.getInputOutput();

        switch (opcode) {
            case XGE:
                if (bytesRemaining != 4) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {    // Assume arg == 0 (GEQueryVersion).
                    short xgeMajor = (short) io.readShort();
                    short xgeMinor = (short) io.readShort();

                    synchronized (io) {
                        Util.writeReplyHeader(client, arg);
                        io.writeInt(0);    // Reply length.
                        io.writeShort(xgeMajor);
                        io.writeShort(xgeMinor);
                        io.writePadBytes(20);
                    }
                    io.flush();
                }
                break;
            case BigRequests:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {    // Assume arg == 0 (BigReqEnable).
                    synchronized (io) {
                        Util.writeReplyHeader(client, arg);
                        io.writeInt(0);
                        io.writeInt(Integer.MAX_VALUE);
                        io.writePadBytes(20);
                    }
                    io.flush();
                }
                break;
            case Shape:
                XShape.processRequest(xServer, client, opcode, arg, bytesRemaining);
                break;
            case XTEST:
                XTest.processRequest(xServer, client, opcode, arg, bytesRemaining);
                break;
            case Sync:
            //    XSync.processRequest(xServer, client, opcode, arg, bytesRemaining);
            //    break;
            default:
                io.readSkip(bytesRemaining);    // Not implemented.
                ErrorCode.write(client, ErrorCode.Implementation, opcode, 0);
                break;
        }
    }
}