package au.com.darkside.xserver.Xext;

import java.util.Hashtable;

import au.com.darkside.xserver.XServer;
import au.com.darkside.xserver.Client;
import au.com.darkside.xserver.InputOutput;
import au.com.darkside.xserver.Util;
import au.com.darkside.xserver.ErrorCode;

/**
 * Handles requests related to the X SYNC extension.
 */
public class XSync {
    private static int syncMajor = -1;
    private static int syncMinor;
    private static final Hashtable<String, SystemCounter> _systemCounters = new Hashtable<String, SystemCounter>();
    private static final Hashtable<Integer, Counter> _counters = new Hashtable<Integer, Counter>();

    public static final byte SYNC_INIT = 0;
    public static final byte SYNC_LIST = 1;
    public static final byte SYNC_CREATE = 2;
    public static final byte SYNC_DESTROY = 3;
    public static final byte EventBase = 95;
    public static final byte ErrorBase = (byte)154;

    static public void Initialize() {
        _systemCounters.put("dummy", new SystemCounter (1,4, 0,"dummy"));
        _systemCounters.put("SERVERTIME", new SystemCounter (3,6,7,"SERVERTIME"));
    }

    /**
     * Process a request relating to the X SYNC extension.
     *
     * @param xServer        The X server.
     * @param client         The remote client.
     * @param opcode         The request's opcode.
     * @param arg            Optional first argument.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    static public void processRequest(XServer xServer, Client client, byte opcode, byte arg, int bytesRemaining) throws java.io.IOException {
        InputOutput io=client.getInputOutput();
        switch(arg) {
            case SYNC_INIT:
                if (bytesRemaining < 2) {
                    io.readSkip (bytesRemaining);
                    ErrorCode.write (client, ErrorCode.Length, opcode, 0);
                } else {
                    syncMajor=io.readByte();
                    syncMinor=io.readByte();
                    bytesRemaining-=2;
                    io.readSkip (bytesRemaining);
                    synchronized(io) {
                        Util.writeReplyHeader(client, arg);
                        io.writeInt (0);	// Reply length.
                        io.writeByte((byte)syncMajor);
                        io.writeByte((byte)syncMinor);
                        io.writePadBytes(22);
                    }
                    io.flush ();
                }
                break;
            case SYNC_LIST:
                int n = _systemCounters.size();//number of counters
                int length = 0;
                for(String key : _systemCounters.keySet()) {
                    int temp=(key.length()+2)%4;
                    if(temp==0)
                        temp=4;
                    length+=14+key.length()+(4-temp);
                }
                int len=length%32;
                length/=32;
                if(len>0)
                    length++;
                else
                    len=32;
                length*=8;
                synchronized(io) {
                    Util.writeReplyHeader(client, arg);
                    io.writeInt (length);	// Reply length.
                    io.writeInt (n);	// List length.
                    io.writePadBytes(20); //unused
                    for(SystemCounter count : _systemCounters.values()) {
                        /* 4 COUNTER counter
                           8 INT64   resolution
                           2 n       length of name in bytes
                           n STRING8 name
                           p         pad,p=pad(n+2) */
                        io.writeInt(count.COUNTER);
                        io.writeInt(count.resLo);
                        io.writeInt(count.resHi);
                        io.writeShort(count.nameLength);
                        io.writeBytes(count.name,0,count.name.length);
                        int pad=(count.name.length+2)%4;
                        if(pad==0)
                            pad=4;
                        io.writePadBytes(4-pad);
                    }
                    io.writePadBytes(32-(len));
                }
                io.flush();
                break;
            case SYNC_CREATE:
                if (bytesRemaining != 12) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int ID=io.readInt();
                    long initVal=io.readLong();
                    bytesRemaining-=12;
                    _counters.put(ID, new Counter(ID,initVal));
                }
                break;
            case SYNC_DESTROY:
                if (bytesRemaining != 4) {
                    io.readSkip (bytesRemaining);
                    ErrorCode.write (client, ErrorCode.Length, opcode, 0);
                } else {
                    int ID=io.readInt();
                    bytesRemaining-=4;
                    Counter count=_counters.remove(ID);
                    if(count!=null) {
                        synchronized(io) {
                            Util.writeReplyHeader(client, arg);
                            io.writeInt(0);
                            io.writeLong(count.getValue());
                            io.writePadBytes(16);
                        }
                        io.flush();
                    }
                    else {
                        ErrorCode.writeWithMinorOpcode(client, ErrorBase, arg, opcode, ID);
                    }
                }
                break;
            default:
                io.readSkip(bytesRemaining);    // Not implemented.
                ErrorCode.write(client, ErrorCode.Implementation, opcode, 0);
                break;
        }
    }}
