/**
 * Copyright (C) 2012-2019 Iordan Iordanov
 * Copyright (C) 2001-2004 HorizonLive.com, Inc.  All Rights Reserved.
 * Copyright (C) 2001-2006 Constantin Kaplinsky.  All Rights Reserved.
 * Copyright (C) 2000 Tridia Corporation.  All Rights Reserved.
 * Copyright (C) 1999 AT&T Laboratories Cambridge.  All Rights Reserved.
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */

package com.iiordanov.bVNC;

import android.os.Handler;
import android.util.Log;

import com.iiordanov.bVNC.input.RemoteVncKeyboard;
import com.iiordanov.bVNC.protocol.RemoteConnection;
import com.tigervnc.rdr.InStream;
import com.tigervnc.rdr.OutStream;
import com.tigervnc.rdr.RawInStream;
import com.tigervnc.rdr.RawOutStream;
import com.tigervnc.rfb.AuthFailureException;
import com.tigervnc.rfb.CSecurityRSAAES;
import com.undatech.opaque.AbstractDrawableData;
import com.undatech.opaque.RfbConnectable;
import com.undatech.opaque.Viewable;
import com.undatech.opaque.input.RemoteKeyboard;
import com.undatech.opaque.util.GeneralUtils;
import com.undatech.remoteClientUi.R;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocket;

/**
 * Access the RFB protocol through a socket.
 * <p>
 * This class has no knowledge of the android-specific UI; it sees framebuffer updates
 * and input events as defined in the RFB protocol.
 */
public class RfbProto extends RfbConnectable {

    public static final int SecTypeRA2 = 5;
    public static final int SecTypeRA2ne = 6;
    public static final int SecTypeRA256 = 129;
    public static final int SecTypeRAne256 = 130;
    /* VeNCrypt subtypes */
    public static final int secTypePlain = 256;
    public static final int secTypeTLSNone = 257;
    public static final int secTypeTLSVnc = 258;
    public static final int secTypeTLSPlain = 259;
    public static final int secTypeX509None = 260;
    public static final int secTypeX509Vnc = 261;
    public static final int secTypeX509Plain = 262;
    public static final int secTypeIdent = 265;
    public static final int secTypeTLSIdent = 266;
    public static final int secTypeX509Ident = 267;
    /* RSA-AES subtypes */
    public static final int secTypeRA2UserPass = 1;
    public static final int secTypeRA2Pass = 2;
    final static String TAG = "RfbProto";
    final static String
            versionMsg_3_3 = "RFB 003.003\n",
            versionMsg_3_7 = "RFB 003.007\n",
            versionMsg_3_8 = "RFB 003.008\n";
    // Vendor signatures: standard VNC/RealVNC, TridiaVNC, and TightVNC
    final static String
            StandardVendor = "STDV",
            TridiaVncVendor = "TRDV",
            TightVncVendor = "TGHT";
    // Security types
    final static int
            SecTypeInvalid = 0,
            SecTypeNone = 1,
            SecTypeVncAuth = 2,
            SecTypeTight = 16,
            SecTypeUltraVnc1 = 17,
            SecTypeTLS = 18,
            SecTypeVeNCrypt = 19,
            SecTypeArd = 30,
            SecTypeUltraVnc2 = 113,
            SecTypeUltraVnc3 = 114,
            SecTypeUltraVnc4 = 115,
            SecTypeUltra34 = 0xfffffffa;
    // Supported tunneling types
    final static int
            NoTunneling = 0;
    final static String
            SigNoTunneling = "NOTUNNEL";
    // Supported authentication types
    final static int
            AuthNone = 1,
            AuthVNC = 2,
            AuthUltra = 17,
            AuthUnixLogin = 129,
            AuthPlain = 256,
            AuthTLSNone = 257,
            AuthTLSVnc = 258,
            AuthTLSPlain = 259,
            AuthX509None = 260,
            AuthX509Vnc = 261,
            AuthX509Plain = 262;
    final static String
            SigAuthNone = "NOAUTH__",
            SigAuthVNC = "VNCAUTH_",
            SigAuthUnixLogin = "ULGNAUTH";
    // VNC authentication results
    final static int
            VncAuthOK = 0,
            VncAuthFailed = 1,
            VncAuthTooMany = 2,
            PlainAuthFailed = 13;
    // Server-to-client messages
    final static int
            FramebufferUpdate = 0,
            SetColourMapEntries = 1,
            Bell = 2,
            ServerCutText = 3,
            TextChat = 11;
    // Client-to-server messages
    final static int
            SetPixelFormat = 0,
            FixColourMapEntries = 1,
            SetEncodings = 2,
            FramebufferUpdateRequest = 3,
            KeyboardEvent = 4,
            PointerEvent = 5,
            ClientCutText = 6;
    // Supported encodings and pseudo-encodings
    final static int
            EncodingRaw = 0,
            EncodingCopyRect = 1,
            EncodingRRE = 2,
            EncodingCoRRE = 4,
            EncodingHextile = 5,
            EncodingZlib = 6,
            EncodingTight = 7,
    //            EncodingZlibHex = 8,
//            EncodingUltra = 9,
//            EncodingUltra2 = 10,
    EncodingZRLE = 16,
    //            EncodingZYWRLE = 17,
//            EncodingXZ = 18,
//            EncodingXZYW = 19,
//            EncodingZstd = 25,
    EncodingTightZstd = 26,
    //            EncodingZstdHex = 27,
//            EncodingZSTDRLE = 28,
//            EncodingZSTDYWRLE = 29,
    EncodingCompressLevel0 = -256,
            EncodingQualityLevel0 = -32,
            EncodingXCursor = -240,
            EncodingRichCursor = -239,
            EncodingPointerPos = -232,
            EncodingLastRect = -224,
            EncodingNewFBSize = -223,
            EncodingClientRedirect = -311,
            EncodingExtendedDesktopSize = -308;
    final static String
            SigEncodingRaw = "RAW_____",
            SigEncodingCopyRect = "COPYRECT",
            SigEncodingRRE = "RRE_____",
            SigEncodingCoRRE = "CORRE___",
            SigEncodingHextile = "HEXTILE_",
            SigEncodingZlib = "ZLIB____",
            SigEncodingTight = "TIGHT___",
            SigEncodingTightZstd = "TIGHTZSTD___",
            SigEncodingZRLE = "ZRLE____",
            SigEncodingCompressLevel0 = "COMPRLVL",
            SigEncodingQualityLevel0 = "JPEGQLVL",
            SigEncodingXCursor = "X11CURSR",
            SigEncodingRichCursor = "RCHCURSR",
            SigEncodingPointerPos = "POINTPOS",
            SigEncodingLastRect = "LASTRECT",
            SigEncodingNewFBSize = "NEWFBSIZ";
    final static int MaxNormalEncoding = 255;
    // Contstants used in the Hextile decoder
    final static int
            HextileRaw = 1,
            HextileBackgroundSpecified = 2,
            HextileForegroundSpecified = 4,
            HextileAnySubrects = 8,
            HextileSubrectsColoured = 16;
    // Contstants used in the Tight decoder
    final static int TightMinToCompress = 12;
    final static int
            TightExplicitFilter = 0x04,
            TightFill = 0x08,
            TightJpeg = 0x09,
            TightMaxSubencoding = 0x09,
            TightFilterCopy = 0x00,
            TightFilterPalette = 0x01,
            TightFilterGradient = 0x02;
    // Constants used for UltraVNC chat extension
    final static int
            CHAT_OPEN = -1,
            CHAT_CLOSE = -2,
            CHAT_FINISHED = -3;
    public static int XK_LCTRL = 0xffe3;
    public static int XK_RCTRL = 0xffe4;
    public static int XK_LSHIFT = 0xffe1;
    public static int XK_RSHIFT = 0xffe2;
    public static int XK_LALT = 0xffe9;
    public static int XK_RALT = 0xffea;
    public static int XK_LSUPER = 0xffeb;
    public static int XK_RSUPER = 0xffec;
    public static int XL_ISOL3SHIFT = 0xfe03;
    // maxStringLength protects against allocating a huge buffer.  Set it
    // higher if you need longer strings.
    public static int maxStringLength = 65535;
    String host;
    //- VncViewer viewer;
    int port;

    // This will be set to true on the first framebuffer update
    // containing Zlib-, ZRLE- or Tight-encoded data.
    //boolean wereZlibUpdates = false;
    Socket sock;
    InStream is;
    OutStream os;

    // Before starting to record each saved session, we set this field
    // to 0, and increment on each framebuffer update. We don't flush
    // the SessionRecorder data into the file before the second update.
    // This allows us to write initial framebuffer update with zero
    // timestamp, to let the player show initial desktop before
    // playback.
    //int numUpdatesInSession;
    DH dh;
    long dh_resp;
    //- SessionRecorder rec;
    boolean inNormalProtocol = false;
    // Java on UNIX does not call keyPressed() on some keys, for example
    // swedish keys To prevent our workaround to produce duplicate
    // keypresses on JVMs that actually works, keep track of if
    // keyPressed() for a "broken" key was called or not.
    boolean brokenKeyPressed = false;
    // This will be set to false if the startSession() was called after
    // we have received at least one Zlib-, ZRLE- or Tight-encoded
    // framebuffer update.
    boolean recordFromBeginning = true;
    // This fields are needed to show warnings about inefficiently saved
    // sessions only once per each saved session file.
    boolean zlibWarningShown;
    boolean tightWarningShown;
    // Measuring network throughput.
    boolean timing;
    long timeWaitedIn100us;
    long timedKbits;
    // Protocol version and TightVNC-specific protocol options.
    int serverMajor, serverMinor;

    // VNC Encoding parameters
    int clientMajor, clientMinor;
    boolean protocolTightVNC;
    CapsContainer tunnelCaps, authCaps;
    CapsContainer serverMsgCaps, clientMsgCaps;
    CapsContainer encodingCaps;
    // The remote canvas
    Viewable canvas;
    RemoteConnection remoteConnection;
    byte[] writeIntBuffer = new byte[4];
    String desktopName;
    int framebufferWidth, framebufferHeight;
    int preferredFramebufferWidth = 0, preferredFramebufferHeight = 0;
    int bitsPerPixel, depth;
    boolean bigEndian, trueColour;
    int redMax, greenMax, blueMax, redShift, greenShift, blueShift;
    int updateNRects;
    int updateRectX, updateRectY, updateRectW, updateRectH, updateRectEncoding;
    int copyRectSrcX, copyRectSrcY;
    byte[] framebufferUpdateRequest = new byte[10];
    byte[] eventBuf = new byte[72];
    int eventBufLen;
    // If true, informs that the RFB socket was closed.
    private boolean closed;
    // The main processing loop continues while this is set to true;
    private boolean maintainConnection = true;
    // Tight encoding parameters
    private int compressLevel = 6;
    private int jpegQuality = 7;
    // Used to determine if encoding update is necessary
    private int[] encodingsSaved = null;
    private int nEncodingsSaved = 0;
    // Handle for decoder object
    private Decoder decoder;
    // Suggests to the server whether the desktop should be shared or not
    private int shareDesktop = 1;
    // Suggests to the server a preferred encoding
    private int preferredEncoding;
    // View Only mode
    private boolean viewOnly = false;
    // ExtendedDesktopSize Variables
    // ScreenId
    private int screenId;
    private int screenFlags;
    private boolean isExtendedDesktopSizeSupported = false;
    // This variable indicates whether or not the user has accepted an untrusted
    // security certificate. Used to control progress while the dialog asking the user
    // to confirm the authenticity of a certificate is displayed.
    private boolean certificateAccepted = false;
    private boolean sslTunneled = false;
    //
    // Read server's protocol version message
    //
    private int hashAlgorithm;


    //
    // Write our protocol version message
    //
    private String hash;


    //
    // Negotiate the authentication scheme.
    //
    private String cert;

    //
    // Read security type from the server (protocol version 3.3).
    //

    //
    // Constructor
    //
    public RfbProto(Decoder decoder, Viewable canvas, RemoteConnection remoteConnection, Handler handler, int preferredEncoding,
                    boolean viewOnly, boolean sslTunneled, int hashAlgorithm,
                    String hash, String cert, boolean debugLogging) {
        super(debugLogging, handler);
        this.sslTunneled = sslTunneled;
        this.decoder = decoder;
        this.viewOnly = viewOnly;
        this.canvas = canvas;
        this.remoteConnection = remoteConnection;
        this.preferredEncoding = preferredEncoding;
        this.hashAlgorithm = hashAlgorithm;
        this.hash = hash;
        this.cert = cert;
        timing = false;
        timeWaitedIn100us = 5;
        timedKbits = 0;
        modifierMap.put(RemoteKeyboard.CTRL_MASK, XK_LCTRL);
        modifierMap.put(RemoteKeyboard.RCTRL_MASK, XK_RCTRL);
        modifierMap.put(RemoteKeyboard.ALT_MASK, XK_LALT);
        modifierMap.put(RemoteKeyboard.RALT_MASK, XK_RALT);
        modifierMap.put(RemoteKeyboard.SUPER_MASK, XK_LSUPER);
        modifierMap.put(RemoteKeyboard.RSUPER_MASK, XK_RSUPER);
        modifierMap.put(RemoteKeyboard.SHIFT_MASK, XK_LSHIFT);
        modifierMap.put(RemoteKeyboard.RSHIFT_MASK, XK_RSHIFT);
        if (RemoteVncKeyboard.rAltAsIsoL3Shift)
            modifierMap.put(RemoteKeyboard.RALT_MASK, XL_ISOL3SHIFT);
    }

    //
    // Select security type from the server's list (protocol versions 3.7/3.8).
    //

    // Make TCP connection to RFB server.
    private void initSocket() throws Exception {
        Socket sock = null;

        if (sslTunneled) {
            // If this is a tunneled connection, set up the tunnel and get its socket.
            Log.i(TAG, "Creating secure tunnel.");
            SecureTunnel tunnel = new SecureTunnel(host, port, hashAlgorithm, hash, cert, handler);
            tunnel.setup();
            synchronized (this) {
                while (!certificateAccepted) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            sock = tunnel.getSocket();
        }


        if (sock == null) {
            sock = new Socket();
            sock.setSoTimeout(Constants.SOCKET_CONN_TIMEOUT);
            sock.setKeepAlive(true);
            sock.connect(new InetSocketAddress(host, port), Constants.SOCKET_CONN_TIMEOUT);
            sock.setSoTimeout(0);
            sock.setTcpNoDelay(true);
        }

        this.sock = sock;
        setStreams(new RawInStream(sock.getInputStream()), new RawOutStream(sock.getOutputStream()));
    }

    public synchronized void closeSocket() {
        inNormalProtocol = false;
        try {
            if (sock != null) {
                sock.close();
            }
            closed = true;
            Log.v(TAG, "RFB socket closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //
    // Perform "no authentication".
    //

    public void close() {
        inNormalProtocol = false;
        maintainConnection = false;
        closeSocket();
    }

    //
    // Perform standard VNC Authentication.
    //

    @Override
    public boolean isCertificateAccepted() {
        return certificateAccepted;
    }

    @Override
    public void setCertificateAccepted(boolean certificateAccepted) {
        this.certificateAccepted = certificateAccepted;
    }

    synchronized boolean closed() {
        return closed;
    }

    public void initializeAndAuthenticate(String host, int port, String us, String pw,
                                          boolean useRepeater, String repeaterID, int connType,
                                          String cert) throws Exception, AuthFailureException {
        this.host = host;
        this.port = port;
        Log.v(TAG, "Connecting to server: " + this.host + " at port: " + this.port);
        initSocket();

        // <RepeaterMagic>
        if (useRepeater && repeaterID != null && repeaterID.length() > 0) {
            Log.i(TAG, "Negotiating repeater/proxy connection");
            byte[] protocolMsg = new byte[12];
            is.readBytes(protocolMsg, 0, 12);
            byte[] buffer = new byte[250];
            System.arraycopy(repeaterID.getBytes(), 0, buffer, 0, repeaterID.length());
            os.write(buffer);
        }
        // </RepeaterMagic>

        readVersionMsg();
        Log.i(TAG, "RFB server supports protocol version " + serverMajor + "." + serverMinor);

        writeVersionMsg();
        Log.i(TAG, "Using RFB protocol version " + clientMajor + "." + clientMinor);

        boolean userNameSupplied = us.length() > 0;
        Log.d(TAG, "userNameSupplied: " + userNameSupplied);
        int secType = negotiateSecurity(userNameSupplied, connType);
        int authType;
        if (secType == RfbProto.SecTypeTight) {
            Log.i(TAG, "secType == RfbProto.SecTypeTight");
            initCapabilities();
            setupTunneling();
            authType = negotiateAuthenticationTight();
        } else if (secType == RfbProto.SecTypeVeNCrypt) {
            Log.i(TAG, "secType == RfbProto.SecTypeVeNCrypt");
            authType = authenticateVeNCrypt(userNameSupplied);
        } else if (secType == RfbProto.SecTypeTLS) {
            Log.i(TAG, "secType == RfbProto.SecTypeTLS");
            authenticateTLS();
            authType = negotiateSecurity(userNameSupplied, 0);
        } else if (secType == RfbProto.SecTypeUltra34 ||
                secType == RfbProto.SecTypeUltraVnc2) {
            Log.i(TAG, "secType == RfbProto.SecTypeUltra34 or SecTypeUltraVnc2");
            authType = RfbProto.AuthUltra;
        } else if (secType == RfbProto.SecTypeArd) {
            Log.i(TAG, "secType == RfbProto.SecTypeArd");
            RFBSecurityARD ardAuth = new RFBSecurityARD(us, pw);
            ardAuth.perform(this);
            readSecurityResult(true, "ARD Authentication");
            return;
        } else if (secType == RfbProto.SecTypeRA2) {
            Log.i(TAG, "secType == RfbProto.SecTypeRA2");
            CSecurityRSAAES x = new CSecurityRSAAES(this, secType, 128, true);
            x.processMsg(us, pw);
            readSecurityResult(true, "SecTypeRA2 Authentication");
            return;
        } else if (secType == RfbProto.SecTypeRA2ne) {
            Log.i(TAG, "secType == RfbProto.secTypeRA2ne");
            CSecurityRSAAES x = new CSecurityRSAAES(this, secType, 128, false);
            x.processMsg(us, pw);
            readSecurityResult(true, "SecTypeRA2ne Authentication");
            return;
        } else if (secType == RfbProto.SecTypeRA256) {
            Log.i(TAG, "secType == RfbProto.SecTypeRA2");
            CSecurityRSAAES x = new CSecurityRSAAES(this, secType, 256, true);
            x.processMsg(us, pw);
            readSecurityResult(true, "SecTypeRA256 Authentication");
            return;
        } else if (secType == RfbProto.SecTypeRAne256) {
            Log.i(TAG, "secType == RfbProto.SecTypeRA2");
            CSecurityRSAAES x = new CSecurityRSAAES(this, secType, 256, false);
            x.processMsg(us, pw);
            readSecurityResult(true, "SecTypeRAne256 Authentication");
            return;
        } else {
            authType = secType;
        }

        switch (authType) {
            case RfbProto.AuthNone:
                Log.i(TAG, "authType == RfbProto.AuthNone, No authentication needed");
                authenticateNone();
                break;
            case RfbProto.AuthPlain:
                Log.i(TAG, "authType == RfbProto.AuthPlain, Plain authentication needed ");
                authenticatePlain(us, pw);
                break;
            case RfbProto.AuthVNC:
                Log.i(TAG, "authType == RfbProto.AuthVNC, VNC authentication needed");
                authenticateVNC(pw);
                break;
            case RfbProto.AuthUltra:
                Log.i(TAG, "authType == RfbProto.AuthUltra, UltraVNC authentication needed");
                prepareDH();
                authenticateDH(us, pw);
                break;
            case RfbProto.AuthTLSNone:
                Log.i(TAG, "authType == RfbProto.AuthTLSNone, No authentication needed");
                authenticateTLS();
                authenticateNone();
                break;
            case RfbProto.AuthTLSPlain:
                Log.i(TAG, "authType == RfbProto.AuthTLSPlain, Plain authentication needed");
                authenticateTLS();
                authenticatePlain(us, pw);
                break;
            case RfbProto.AuthTLSVnc:
                Log.i(TAG, "authType == RfbProto.AuthTLSVnc, VNC authentication needed");
                authenticateTLS();
                authenticateVNC(pw);
                break;
            case RfbProto.AuthX509None:
                Log.i(TAG, "authType == RfbProto.AuthX509None, No authentication needed");
                authenticateX509(cert);
                authenticateNone();
                break;
            case RfbProto.AuthX509Plain:
                Log.i(TAG, "authType == RfbProto.AuthX509Plain, Plain authentication needed");
                authenticateX509(cert);
                authenticatePlain(us, pw);
                break;
            case RfbProto.AuthX509Vnc:
                Log.i(TAG, "authType == RfbProto.AuthX509Vnc,VNC authentication needed");
                authenticateX509(cert);
                authenticateVNC(pw);
                break;
            default:
                throw new Exception("Unknown authentication scheme " + authType);
        }
    }

    //
    // Read security result.
    // Throws an exception on authentication failure.
    //

    void readVersionMsg() throws Exception {

        byte[] b = new byte[12];

        readFully(b);

        if ((b[0] != 'R') || (b[1] != 'F') || (b[2] != 'B') || (b[3] != ' ')
                || (b[4] < '0') || (b[4] > '9') || (b[5] < '0') || (b[5] > '9')
                || (b[6] < '0') || (b[6] > '9') || (b[7] != '.')
                || (b[8] < '0') || (b[8] > '9') || (b[9] < '0') || (b[9] > '9')
                || (b[10] < '0') || (b[10] > '9') || (b[11] != '\n')) {
            Log.i(TAG, new String(b));
            throw new Exception("Host " + host + " port " + port +
                    " is not an RFB server");
        }

        serverMajor = (b[4] - '0') * 100 + (b[5] - '0') * 10 + (b[6] - '0');
        serverMinor = (b[8] - '0') * 100 + (b[9] - '0') * 10 + (b[10] - '0');

        if (serverMajor < 3) {
            throw new Exception("RFB server does not support protocol version 3");
        }
    }

    //
    // Read the string describing the reason for a connection failure,
    // and throw an exception.
    //

    synchronized void writeVersionMsg() throws IOException {
        clientMajor = 3;
        if (serverMajor > 3 || serverMinor >= 8) {
            clientMinor = 8;
            os.write(versionMsg_3_8.getBytes());
        } else if (serverMinor >= 7) {
            clientMinor = 7;
            os.write(versionMsg_3_7.getBytes());
        } else {
            clientMinor = 3;
            os.write(versionMsg_3_3.getBytes());
        }
        protocolTightVNC = false;
    }

    int negotiateSecurity(boolean userNameSupplied, int connType) throws Exception {
        if (clientMinor >= 7) {
            return selectSecurityType(userNameSupplied, connType);
        } else {
            return readSecurityType(userNameSupplied);
        }
    }

    int readSecurityType(boolean userNameSupplied) throws Exception {
        int secType = is.readInt();

        switch (secType) {
            case SecTypeInvalid:
                readConnFailedReason();
                return SecTypeInvalid;    // should never be executed
            case SecTypeNone:
            case SecTypeVncAuth:
                return secType;
            case SecTypeUltra34:
            case SecTypeUltraVnc2:
                return returnSecTypeIfUsernameSuppliedOrThrowError(userNameSupplied, secType);
            default:
                throw new Exception("Unknown security type from RFB server: " + secType);
        }
    }

    private int returnSecTypeIfUsernameSuppliedOrThrowError(
            boolean userNameSupplied,
            int secType
    ) throws RfbUserPassAuthFailedOrUsernameRequiredException {
        if (userNameSupplied) {
            return secType;
        }
        throw new RfbUserPassAuthFailedOrUsernameRequiredException("Username required.");
    }

    int selectSecurityType(boolean userNameSupplied, int connType) throws Exception {
        android.util.Log.i(TAG, "(Re)Selecting security type.");

        int secType = SecTypeInvalid;

        // Read the list of security types.
        int nSecTypes = is.readUnsignedByte();
        if (nSecTypes == 0) {
            readConnFailedReason();
            return SecTypeInvalid;    // should never be executed
        }
        byte[] secTypes = new byte[nSecTypes];
        readFully(secTypes);

        // Find out if the server supports TightVNC protocol extensions
        for (int i = 0; i < nSecTypes; i++) {
            if (secTypes[i] == SecTypeTight) {
                protocolTightVNC = true;
                os.write(SecTypeTight);
                return SecTypeTight;
            }
        }

        // Find first supported security type.
        for (int i = 0; i < nSecTypes; i++) {
            android.util.Log.i(TAG, "Received security type: " + secTypes[i]);
            int currentSecType = secTypes[i] & 0xff;
            // If AnonTLS or VeNCrypt modes are enforced, then only accept them. Otherwise, accept it and all others.
            if (connType == Constants.CONN_TYPE_ANONTLS) {
                if (currentSecType == SecTypeTLS) {
                    secType = currentSecType;
                    break;
                }
            } else if (connType == Constants.CONN_TYPE_VENCRYPT) {
                if (currentSecType == SecTypeVeNCrypt) {
                    secType = currentSecType;
                    break;
                }
            } else if (connType == Constants.CONN_TYPE_ULTRAVNC) {
                if (currentSecType == SecTypeNone || currentSecType == SecTypeVncAuth ||
                        currentSecType == SecTypeUltraVnc2 || currentSecType == SecTypeUltra34) {
                    secType = currentSecType;
                    break;
                }
            } else {
                if (currentSecType == SecTypeRA256 || currentSecType == SecTypeRA2) {
                    secType = currentSecType;
                    break;
                }

                if (currentSecType == SecTypeRAne256 || currentSecType == SecTypeRA2ne) {
                    secType = currentSecType;
                    break;
                }

                if (currentSecType == SecTypeNone || currentSecType == SecTypeVncAuth ||
                        currentSecType == SecTypeVeNCrypt) {
                    secType = currentSecType;
                    break;
                }

                if (currentSecType == SecTypeTLS) {
                    secType = currentSecType;
                    break;
                }

                if (currentSecType == SecTypeArd) {
                    if (userNameSupplied) {
                        secType = currentSecType;
                        break;
                    }
                    throw new RfbUserPassAuthFailedOrUsernameRequiredException("Username required.");
                }
            }
        }

        if (secType == SecTypeInvalid) {
            String message;
            // If the server tried to negotiate SecTypeTLS and this is an SDK >= Marshmallow, report
            // the appropriate error to the user.
            message = canvas.getContext().getString(R.string.error_security_type)
                    + " " + canvas.getContext().getString(R.string.error_pick_correct_item);
            throw new Exception(message);
        } else {
            os.write(secType);
        }

        return secType;
    }
    //
    // Initialize capability lists (TightVNC protocol extensions).
    //

    int authenticateVeNCrypt(boolean userNameSupplied) throws Exception {
        int majorVersion = is.readUnsignedByte();
        int minorVersion = is.readUnsignedByte();
        int Version = (majorVersion << 8) | minorVersion;
        if (Version < 0x0002) {
            os.write(0);
            os.write(0);
            throw new Exception("Server reported an unsupported VeNCrypt version");
        }
        os.write(0);
        os.write(2);
        if (is.readUnsignedByte() != 0)
            throw new Exception("Server reported it could not support the VeNCrypt version");
        int nSecTypes = is.readUnsignedByte();
        int[] secTypes = new int[nSecTypes];
        for (int i = 0; i < nSecTypes; i++) {
            secTypes[i] = is.readInt();
        }

        for (int i = 0; i < nSecTypes; i++) {
            switch (secTypes[i]) {
                case AuthNone:
                case AuthVNC:
                case AuthPlain:
                    writeInt(secTypes[i]);
                    Log.i(TAG, "Selecting AuthPlain VeNCrypt subtype: " + AuthPlain);
                    return secTypes[i];
                case AuthTLSNone:
                case AuthTLSVnc:
                case AuthTLSPlain:
                case AuthX509None:
                case AuthX509Vnc:
                case AuthX509Plain:
                    writeInt(secTypes[i]);
                    Log.i(TAG, "Selecting AuthX509Plain VeNCrypt subtype: " + AuthX509Plain);
                    if (readU8() == 0) {
                        throw new Exception("VeNCrypt setup on the server failed. Please check your certificate if applicable.");
                    }
                    return secTypes[i];
            }
        }

        throw new Exception("No valid VeNCrypt sub-type");
    }

    //
    // Setup tunneling (TightVNC protocol extensions)
    //

    void authenticateNone() throws Exception {
        if (clientMinor >= 8)
            readSecurityResult(false, "No authentication");
    }

    //
    // Negotiate authentication scheme (TightVNC protocol extensions)
    //

    void authenticateVNC(String pw) throws Exception {
        byte[] challenge = new byte[16];
        readFully(challenge);

        if (pw.length() > 8)
            pw = pw.substring(0, 8);    // Truncate to 8 chars

        // Truncate password on the first zero byte.
        int firstZero = pw.indexOf(0);
        if (firstZero != -1)
            pw = pw.substring(0, firstZero);

        byte[] key = {0, 0, 0, 0, 0, 0, 0, 0};
        System.arraycopy(pw.getBytes(), 0, key, 0, pw.length());

        DesCipher des = new DesCipher(key);

        des.encrypt(challenge, 0, challenge, 0);
        des.encrypt(challenge, 8, challenge, 8);

        os.write(challenge);

        readSecurityResult(false, "VNC authentication");
    }

    //
    // Read a capability list (TightVNC protocol extensions)
    //

    void authenticateTLS() throws Exception {
        TLSTunnel tunnel = new TLSTunnel(sock);
        SSLSocket sslsock = tunnel.setup();
        setStreams(new RawInStream(sslsock.getInputStream()), new RawOutStream(sslsock.getOutputStream()));
    }

    //
    // Write a 32-bit integer into the output stream.
    //

    void authenticateX509(String certstr) throws Exception {
        X509Tunnel tunnel = new X509Tunnel(sock, certstr, handler, this);
        SSLSocket sslsock = tunnel.setup();
        setStreams(new RawInStream(sslsock.getInputStream()), new RawOutStream(sslsock.getOutputStream()));
    }

    void authenticatePlain(String user, String password) throws Exception {
        // Workaround for certain servers simply closing the connection when they detect empty username
        String username = "".equals(user) ? " " : user;
        byte[] userBytes = username.getBytes();
        byte[] passwordBytes = password.getBytes();
        writeInt(userBytes.length);
        writeInt(passwordBytes.length);
        os.write(userBytes);
        os.write(passwordBytes);

        readSecurityResult(true, "Plain authentication");
    }

    //
    // Write the client initialisation message
    //

    void readSecurityResult(boolean userNameRequired, String authType) throws Exception {
        Log.d(TAG, "readSecurityResult: authType: " + authType + ", userNameRequired: " + userNameRequired);
        int securityResult = is.readInt();

        switch (securityResult) {
            case VncAuthOK:
                System.out.println(authType + ": success");
                break;
            case VncAuthFailed:
                if (clientMinor >= 8)
                    readConnFailedReason(false);
                throwAppropriateAuthException(userNameRequired, authType);
            case VncAuthTooMany:
                throwAppropriateAuthException(userNameRequired, authType);
            case PlainAuthFailed:
                throw new RfbUserPassAuthFailedOrUsernameRequiredException(authType + ": failed");
            default:
                throw new Exception(authType + ": unknown result " + securityResult);
        }
    }

    private void throwAppropriateAuthException(
            boolean userNameSupplied,
            String authType
    ) throws RfbUserPassAuthFailedOrUsernameRequiredException, RfbPasswordAuthenticationException {
        if (userNameSupplied) {
            throw new RfbUserPassAuthFailedOrUsernameRequiredException(authType + ": failed");
        } else {
            throw new RfbPasswordAuthenticationException(authType + ": failed");
        }
    }


    //
    // Read the server initialisation message
    //

    void readConnFailedReason() throws Exception {
        readConnFailedReason(true);
    }

    void readConnFailedReason(boolean throwException) throws Exception {
        int reasonLen = is.readInt();
        byte[] reason = new byte[reasonLen];
        readFully(reason);
        String reasonString = new String(reason);
        Log.d(TAG, "readConnFailedReason: " + reasonString);
        if (throwException) {
            throw new Exception(reasonString);
        }
    }

    void prepareDH() throws Exception {
        long gen = is.readLong();
        long mod = is.readLong();
        dh_resp = is.readLong();

        dh = new DH(gen, mod);
        long pub = dh.createInterKey();

        os.write(DH.longToBytes(pub));
    }

    void authenticateDH(String us, String pw) throws Exception {
        long key = dh.createEncryptionKey(dh_resp);

        DesCipher des = new DesCipher(DH.longToBytes(key));

        byte user[] = new byte[256];
        byte passwd[] = new byte[64];
        int i;
        System.arraycopy(us.getBytes(), 0, user, 0, us.length());
        if (us.length() < 256) {
            for (i = us.length(); i < 256; i++) {
                user[i] = 0;
            }
        }
        System.arraycopy(pw.getBytes(), 0, passwd, 0, pw.length());
        if (pw.length() < 64) {
            for (i = pw.length(); i < 64; i++) {
                passwd[i] = 0;
            }
        }

        des.encryptText(user, user, DH.longToBytes(key));
        des.encryptText(passwd, passwd, DH.longToBytes(key));

        os.write(user);
        os.write(passwd);

        readSecurityResult(true, "VNC authentication");
    }

    void initCapabilities() {
        tunnelCaps = new CapsContainer();
        authCaps = new CapsContainer();
        serverMsgCaps = new CapsContainer();
        clientMsgCaps = new CapsContainer();
        encodingCaps = new CapsContainer();

        // Supported authentication methods
        authCaps.add(AuthNone, StandardVendor, SigAuthNone,
                "No authentication");
        authCaps.add(AuthVNC, StandardVendor, SigAuthVNC,
                "Standard VNC password authentication");

        // Supported encoding types
        encodingCaps.add(EncodingCopyRect, StandardVendor,
                SigEncodingCopyRect, "Standard CopyRect encoding");
        encodingCaps.add(EncodingRRE, StandardVendor,
                SigEncodingRRE, "Standard RRE encoding");
        encodingCaps.add(EncodingCoRRE, StandardVendor,
                SigEncodingCoRRE, "Standard CoRRE encoding");
        encodingCaps.add(EncodingHextile, StandardVendor,
                SigEncodingHextile, "Standard Hextile encoding");
        encodingCaps.add(EncodingZRLE, StandardVendor,
                SigEncodingZRLE, "Standard ZRLE encoding");
        encodingCaps.add(EncodingZlib, TridiaVncVendor,
                SigEncodingZlib, "Zlib encoding");
        encodingCaps.add(EncodingTight, TightVncVendor,
                SigEncodingTight, "Tight encoding");
        encodingCaps.add(EncodingTightZstd, TightVncVendor,
                SigEncodingTightZstd, "Tight Zstd encoding");

        // Supported pseudo-encoding types
        encodingCaps.add(EncodingCompressLevel0, TightVncVendor,
                SigEncodingCompressLevel0, "Compression level");
        encodingCaps.add(EncodingQualityLevel0, TightVncVendor,
                SigEncodingQualityLevel0, "JPEG quality level");
        encodingCaps.add(EncodingXCursor, TightVncVendor,
                SigEncodingXCursor, "X-style cursor shape update");
        encodingCaps.add(EncodingRichCursor, TightVncVendor,
                SigEncodingRichCursor, "Rich-color cursor shape update");
        encodingCaps.add(EncodingPointerPos, TightVncVendor,
                SigEncodingPointerPos, "Pointer position update");
        encodingCaps.add(EncodingLastRect, TightVncVendor,
                SigEncodingLastRect, "LastRect protocol extension");
        encodingCaps.add(EncodingNewFBSize, TightVncVendor,
                SigEncodingNewFBSize, "Framebuffer size change");
    }

    void setupTunneling() throws IOException {
        int nTunnelTypes = is.readInt();
        if (nTunnelTypes != 0) {
            readCapabilityList(tunnelCaps, nTunnelTypes);

            // We don't support tunneling yet.
            writeInt(NoTunneling);
        }
    }

    int negotiateAuthenticationTight() throws Exception {
        int nAuthTypes = is.readInt();
        if (nAuthTypes == 0)
            return AuthNone;

        readCapabilityList(authCaps, nAuthTypes);
        for (int i = 0; i < authCaps.numEnabled(); i++) {
            int authType = authCaps.getByOrder(i);
            if (authType == AuthNone || authType == AuthVNC) {
                writeInt(authType);
                return authType;
            }
        }
        throw new Exception("No suitable authentication scheme found");
    }


    //
    // Create session file and write initial protocol messages into it.
    //
  /*-
  void startSession(String fname) throws IOException {
    rec = new SessionRecorder(fname);
    rec.writeHeader();
    rec.write(versionMsg_3_3.getBytes());
    rec.writeIntBE(SecTypeNone);
    rec.writeShortBE(framebufferWidth);
    rec.writeShortBE(framebufferHeight);
    byte[] fbsServerInitMsg =    {
      32, 24, 0, 1, 0,
      (byte)0xFF, 0, (byte)0xFF, 0, (byte)0xFF,
      16, 8, 0, 0, 0, 0
    };
    rec.write(fbsServerInitMsg);
    rec.writeIntBE(desktopName.length());
    rec.write(desktopName.getBytes());
    numUpdatesInSession = 0;

    // FIXME: If there were e.g. ZRLE updates only, that should not
    //        affect recording of Zlib and Tight updates. So, actually
    //        we should maintain separate flags for Zlib, ZRLE and
    //        Tight, instead of one ``wereZlibUpdates'' variable.
    //
    if (wereZlibUpdates)
      recordFromBeginning = false;

    zlibWarningShown = false;
    tightWarningShown = false;
  }

  //
  // Close session file.
  //

  void closeSession() throws IOException {
    if (rec != null) {
      rec.close();
      rec = null;
    }
  }
  */

    //
    // Set new framebuffer size
    //

    void readCapabilityList(CapsContainer caps, int count) throws IOException {
        int code;
        byte[] vendor = new byte[4];
        byte[] name = new byte[8];
        for (int i = 0; i < count; i++) {
            code = is.readInt();
            readFully(vendor);
            readFully(name);
            caps.enable(new CapabilityInfo(code, vendor, name));
        }
    }

    void writeInt(int value) throws IOException {
        writeIntBuffer[0] = (byte) ((value >> 24) & 0xff);
        writeIntBuffer[1] = (byte) ((value >> 16) & 0xff);
        writeIntBuffer[2] = (byte) ((value >> 8) & 0xff);
        writeIntBuffer[3] = (byte) (value & 0xff);
        os.write(writeIntBuffer);
    }


    //
    // Read the server message type
    //

    public void writeClientInit() throws IOException {
    /*- if (viewer.options.shareDesktop) {
      os.write(1);
    } else {
      os.write(0);
    }
    viewer.options.disableShareDesktop();
    */
        os.writeU8(shareDesktop);
    }


    //
    // Read a FramebufferUpdate message
    //

    public void readServerInit() throws IOException {
        android.util.Log.i(TAG, "Reading server init.");
        int framebufferWidth = is.readUnsignedShort();
        int framebufferHeight = is.readUnsignedShort();

        android.util.Log.i(TAG, "Read framebuffer size: " + framebufferWidth + "x" + framebufferHeight);
        this.setFramebufferSize(framebufferWidth, framebufferHeight);
        bitsPerPixel = is.readUnsignedByte();
        depth = is.readUnsignedByte();
        bigEndian = (is.readUnsignedByte() != 0);
        trueColour = (is.readUnsignedByte() != 0);
        redMax = is.readUnsignedShort();
        greenMax = is.readUnsignedShort();
        blueMax = is.readUnsignedShort();
        redShift = is.readUnsignedByte();
        greenShift = is.readUnsignedByte();
        blueShift = is.readUnsignedByte();
        byte[] pad = new byte[3];
        readFully(pad);
        int nameLength = is.readInt();
        byte[] name = new byte[nameLength];
        readFully(name);
        desktopName = new String(name);

        // Read interaction capabilities (TightVNC protocol extensions)
        if (protocolTightVNC) {
            int nServerMessageTypes = is.readUnsignedShort();
            int nClientMessageTypes = is.readUnsignedShort();
            int nEncodingTypes = is.readUnsignedShort();
            is.readUnsignedShort();
            readCapabilityList(serverMsgCaps, nServerMessageTypes);
            readCapabilityList(clientMsgCaps, nClientMessageTypes);
            readCapabilityList(encodingCaps, nEncodingTypes);
        }

        inNormalProtocol = true;
    }

    void setFramebufferSize(int width, int height) {
        Log.d(TAG, "setFramebufferSize, wxh: " + width + "x" + height);
        framebufferWidth = width;
        framebufferHeight = height;
    }

    // Read a FramebufferUpdate rectangle header

    /**
     * Sets the desired framebuffer size if we want to request a custom resolution from the server.
     *
     * @param width
     * @param height
     */
    public void setPreferredFramebufferSize(int width, int height) {
        Log.d(TAG, "setPreferredFramebufferSize, wxh: " + width + "x" + height);
        preferredFramebufferWidth = width;
        preferredFramebufferHeight = height;
    }

    int readServerMessageType() throws IOException {
        return is.readUnsignedByte();

        // If the session is being recorded:
    /*-
    if (rec != null) {
      if (msgType == Bell) {    // Save Bell messages in session files.
    rec.writeByte(msgType);
    if (numUpdatesInSession > 0)
      rec.flush();
      }
    }
    */
    }

    // Read CopyRect source X and Y.

    void readFramebufferUpdate() throws IOException {
        is.readByte();
        updateNRects = is.readUnsignedShort();

        // If the session is being recorded:
    /*-
    if (rec != null) {
      rec.writeByte(FramebufferUpdate);
      rec.writeByte(0);
      rec.writeShortBE(updateNRects);
    }


    numUpdatesInSession++;*/
    }

    void readFramebufferUpdateRectHdr() throws Exception {
        updateRectX = is.readUnsignedShort();
        updateRectY = is.readUnsignedShort();
        updateRectW = is.readUnsignedShort();
        updateRectH = is.readUnsignedShort();
        updateRectEncoding = is.readInt();

    /*
    if (updateRectEncoding == EncodingZlib ||
        updateRectEncoding == EncodingZRLE ||
        updateRectEncoding == EncodingTight)
        wereZlibUpdates = true;

    // If the session is being recorded:
    if (rec != null) {
      if (numUpdatesInSession > 1)
    rec.flush();        // Flush the output on each rectangle.
      rec.writeShortBE(updateRectX);
      rec.writeShortBE(updateRectY);
      rec.writeShortBE(updateRectW);
      rec.writeShortBE(updateRectH);
      if (updateRectEncoding == EncodingZlib && !recordFromBeginning) {
    // Here we cannot write Zlib-encoded rectangles because the
    // decoder won't be able to reproduce zlib stream state.
    if (!zlibWarningShown) {
      System.out.println("Warning: Raw encoding will be used " +
                 "instead of Zlib in recorded session.");
      zlibWarningShown = true;
    }
    rec.writeIntBE(EncodingRaw);
      } else {
    rec.writeIntBE(updateRectEncoding);
    if (updateRectEncoding == EncodingTight && !recordFromBeginning &&
        !tightWarningShown) {
      System.out.println("Warning: Re-compressing Tight-encoded " +
                 "updates for session recording.");
      tightWarningShown = true;
    }
      }
    }
    */

    /*
    if (updateRectEncoding != RfbProto.EncodingPointerPos &&
        ( updateRectEncoding < 0 || updateRectEncoding > MaxNormalEncoding ))
        return;

    if (updateRectX + updateRectW > framebufferWidth ||
        updateRectY + updateRectH > framebufferHeight) {
        throw new Exception("Framebuffer update rectangle too large: " +
                            updateRectW + "x" + updateRectH + " at (" +
                            updateRectX + "," + updateRectY + ")");
    }
    */
    }


    //
    // Read a ServerCutText message
    //

    void readCopyRect() throws IOException {
        copyRectSrcX = is.readUnsignedShort();
        copyRectSrcY = is.readUnsignedShort();

        // If the session is being recorded:
    /*-
    if (rec != null) {
      rec.writeShortBE(copyRectSrcX);
      rec.writeShortBE(copyRectSrcY);
    }
    */
    }


    //
    // Read an integer in compact representation (1..3 bytes).
    // Such format is used as a part of the Tight encoding.
    // Also, this method records data if session recording is active and
    // the viewer's recordFromBeginning variable is set to true.
    //

    String readServerCutText() throws IOException {
        byte[] pad = new byte[3];
        readFully(pad);
        int len = is.readInt();
        byte[] text = new byte[len];
        readFully(text);
        return new String(text);
    }


    //
    // Write a FramebufferUpdateRequest message
    //

    int readCompactLen() throws IOException {
        int[] portion = new int[3];
        portion[0] = is.readUnsignedByte();
        //int byteCount = 1;
        int len = portion[0] & 0x7F;
        if ((portion[0] & 0x80) != 0) {
            portion[1] = is.readUnsignedByte();
            //byteCount++;
            len |= (portion[1] & 0x7F) << 7;
            if ((portion[1] & 0x80) != 0) {
                portion[2] = is.readUnsignedByte();
                //byteCount++;
                len |= (portion[2] & 0xFF) << 14;
            }
        }
    /*-
    if (rec != null && recordFromBeginning)
      for (int i = 0; i < byteCount; i++)
    rec.writeByte(portion[i]);
    */
        return len;
    }

    public synchronized void writeFramebufferUpdateRequest(int x, int y, int w, int h,
                                                           boolean incremental) {
        framebufferUpdateRequest[0] = (byte) FramebufferUpdateRequest;
        framebufferUpdateRequest[1] = (byte) (incremental ? 1 : 0);
        framebufferUpdateRequest[2] = (byte) ((x >> 8) & 0xff);
        framebufferUpdateRequest[3] = (byte) (x & 0xff);
        framebufferUpdateRequest[4] = (byte) ((y >> 8) & 0xff);
        framebufferUpdateRequest[5] = (byte) (y & 0xff);
        framebufferUpdateRequest[6] = (byte) ((w >> 8) & 0xff);
        framebufferUpdateRequest[7] = (byte) (w & 0xff);
        framebufferUpdateRequest[8] = (byte) ((h >> 8) & 0xff);
        framebufferUpdateRequest[9] = (byte) (h & 0xff);

        try {
            os.write(framebufferUpdateRequest);
        } catch (IOException e) {
            Log.e(TAG, "Could not write framebuffer update request.");
            e.printStackTrace();
        }
    }


    //
    // Write a SetPixelFormat message
    //

    public synchronized void writeSetPixelFormat(int bitsPerPixel, int depth, boolean bigEndian,
                                                 boolean trueColour, int redMax, int greenMax, int blueMax,
                                                 int redShift, int greenShift, int blueShift, boolean fGreyScale) // sf@2005)
    {
        byte[] b = new byte[20];

        b[0] = (byte) SetPixelFormat;
        b[4] = (byte) bitsPerPixel;
        b[5] = (byte) depth;
        b[6] = (byte) (bigEndian ? 1 : 0);
        b[7] = (byte) (trueColour ? 1 : 0);
        b[8] = (byte) ((redMax >> 8) & 0xff);
        b[9] = (byte) (redMax & 0xff);
        b[10] = (byte) ((greenMax >> 8) & 0xff);
        b[11] = (byte) (greenMax & 0xff);
        b[12] = (byte) ((blueMax >> 8) & 0xff);
        b[13] = (byte) (blueMax & 0xff);
        b[14] = (byte) redShift;
        b[15] = (byte) greenShift;
        b[16] = (byte) blueShift;
        b[17] = (byte) (fGreyScale ? 1 : 0); // sf@2005

        try {
            os.write(b);
        } catch (IOException e) {
            Log.e(TAG, "Could not write setPixelFormat message to VNC server.");
            e.printStackTrace();
        }
    }


    //
    // Write a FixColourMapEntries message.  The values in the red, green and
    // blue arrays are from 0 to 65535.
    //

    synchronized void writeFixColourMapEntries(int firstColour, int nColours,
                                               int[] red, int[] green, int[] blue)
            throws IOException {
        byte[] b = new byte[6 + nColours * 6];

        b[0] = (byte) FixColourMapEntries;
        b[2] = (byte) ((firstColour >> 8) & 0xff);
        b[3] = (byte) (firstColour & 0xff);
        b[4] = (byte) ((nColours >> 8) & 0xff);
        b[5] = (byte) (nColours & 0xff);

        for (int i = 0; i < nColours; i++) {
            b[6 + i * 6] = (byte) ((red[i] >> 8) & 0xff);
            b[6 + i * 6 + 1] = (byte) (red[i] & 0xff);
            b[6 + i * 6 + 2] = (byte) ((green[i] >> 8) & 0xff);
            b[6 + i * 6 + 3] = (byte) (green[i] & 0xff);
            b[6 + i * 6 + 4] = (byte) ((blue[i] >> 8) & 0xff);
            b[6 + i * 6 + 5] = (byte) (blue[i] & 0xff);
        }

        os.write(b);
    }


    //
    // Write a SetEncodings message
    //

    synchronized void writeSetEncodings(int[] encs, int len) throws IOException {
        byte[] b = new byte[4 + 4 * len];

        b[0] = (byte) SetEncodings;
        b[2] = (byte) ((len >> 8) & 0xff);
        b[3] = (byte) (len & 0xff);

        for (int i = 0; i < len; i++) {
            b[4 + 4 * i] = (byte) ((encs[i] >> 24) & 0xff);
            b[5 + 4 * i] = (byte) ((encs[i] >> 16) & 0xff);
            b[6 + 4 * i] = (byte) ((encs[i] >> 8) & 0xff);
            b[7 + 4 * i] = (byte) (encs[i] & 0xff);
        }

        os.write(b);
    }


    //
    // Write a ClientCutText message
    //

    synchronized void writeClientCutText(String text, int length) throws IOException {
        if (viewOnly)
            return;

        byte[] b = new byte[8 + length];

        b[0] = (byte) ClientCutText;
        b[4] = (byte) ((text.length() >> 24) & 0xff);
        b[5] = (byte) ((text.length() >> 16) & 0xff);
        b[6] = (byte) ((text.length() >> 8) & 0xff);
        b[7] = (byte) (text.length() & 0xff);

        System.arraycopy(text.getBytes(), 0, b, 8, length);

        os.write(b);
    }


    //
    // A buffer for putting pointer and keyboard events before being sent.  This
    // is to ensure that multiple RFB events generated from a single Java Event
    // will all be sent in a single network packet.  The maximum possible
    // length is 4 modifier down events, a single key event followed by 4
    // modifier up events i.e. 9 key events or 72 bytes.
    //

    /**
     * Write a pointer event message.  We may need to send modifier key events
     * around it to set the correct modifier state.
     *
     * @param x
     * @param y
     * @param modifiers
     * @param pointerMask
     * @throws IOException
     */
    public synchronized void writePointerEvent(int x, int y, int modifiers, int pointerMask,
                                               boolean rel) {
        if (viewOnly)
            return;

        eventBufLen = 0;
        writeModifierKeyEvents(modifiers, true);

        eventBuf[eventBufLen++] = (byte) PointerEvent;
        eventBuf[eventBufLen++] = (byte) pointerMask;
        eventBuf[eventBufLen++] = (byte) ((x >> 8) & 0xff);
        eventBuf[eventBufLen++] = (byte) (x & 0xff);
        eventBuf[eventBufLen++] = (byte) ((y >> 8) & 0xff);
        eventBuf[eventBufLen++] = (byte) (y & 0xff);

        //
        // Always release all modifiers after an "up" event
        //

        if (pointerMask == 0) {
            writeModifierKeyEvents(modifiers, false);
        }

        try {
            os.write(eventBuf, 0, eventBufLen);
        } catch (IOException e) {
            Log.e(TAG, "Failed to write pointer event to VNC server.");
            e.printStackTrace();
        }
    }

    void writeCtrlAltDel() throws IOException {
        final int DELETE = 0xffff;
        final int CTRLALT = RemoteKeyboard.CTRL_MASK | RemoteKeyboard.ALT_MASK;
        try {
            // Press
            eventBufLen = 0;
            writeModifierKeyEvents(CTRLALT, true);
            writeKeyEvent(DELETE, true);
            os.write(eventBuf, 0, eventBufLen);

            // Release
            eventBufLen = 0;
            writeKeyEvent(DELETE, false);
            writeModifierKeyEvents(CTRLALT, false);

            // Reset VNC server modifiers state
            //writeModifierKeyEvents(0, false);
            os.write(eventBuf, 0, eventBufLen);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //
    // Write a key event message.  We may need to send modifier key events
    // around it to set the correct modifier state.  Also we need to translate
    // from the Java key values to the X keysym values used by the RFB protocol.
    //
    public synchronized void writeKeyEvent(int keySym, int metaState, boolean down) {
        if (viewOnly)
            return;

        eventBufLen = 0;
        if (down) {
            writeModifierKeyEvents(metaState, down);
        }

        if (keySym > 0)
            writeKeyEvent(keySym, down);

        // Always release all modifiers after an "up" event
        if (!down) {
            writeModifierKeyEvents(metaState, down);
        }

        try {
            os.write(eventBuf, 0, eventBufLen);
        } catch (IOException e) {
            Log.e(TAG, "Failed to write key event to VNC server.");
            e.printStackTrace();
        }
    }

    private synchronized void writeKeyEvent(int keysym, boolean down) {

        if (viewOnly)
            return;

        GeneralUtils.debugLog(this.debugLogging, TAG, "writeKeyEvent, sending keysym:" +
                keysym + ", down: " + down);

        eventBuf[eventBufLen++] = (byte) KeyboardEvent;
        eventBuf[eventBufLen++] = (byte) (down ? 1 : 0);
        eventBuf[eventBufLen++] = (byte) 0;
        eventBuf[eventBufLen++] = (byte) 0;
        eventBuf[eventBufLen++] = (byte) ((keysym >> 24) & 0xff);
        eventBuf[eventBufLen++] = (byte) ((keysym >> 16) & 0xff);
        eventBuf[eventBufLen++] = (byte) ((keysym >> 8) & 0xff);
        eventBuf[eventBufLen++] = (byte) (keysym & 0xff);
    }

    void readClientRedirect(int x, int y, int w, int h) throws Exception {
        int port = readU16();
        String host = readString();
        String x509subject = readString();

        if (x != 0 || y != 0 || w != 0 || h != 0) {
            android.util.Log.e(TAG, "Ignoring ClientRedirect rect with non-zero position/size");
        } else {
            clientRedirect(port, host, x509subject);
        }
    }


    //
    // Add a raw key event with the given X keysym to eventBuf.
    //

    // clientRedirect() migrates the client to another host/port
    public void clientRedirect(int port, String host, String x509subject) {
        Log.d(TAG, "clientRedirect");
        try {
            closeSocket();
            this.host = host;
            this.port = port;
            initSocket();
            writeClientInit();
            readServerInit();
            processProtocol();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void writeModifierKeyEvents(int metaState, boolean down) {
        for (int modifierMask : modifierMap.keySet()) {
            if (remoteKeyboardState.shouldSendModifier(metaState, modifierMask, down)) {
                int modifier = modifierMap.get(modifierMask);
                GeneralUtils.debugLog(this.debugLogging, TAG, "sendModifierKeys, modifierMask:" +
                        modifierMask + ", sending: " + modifier + ", down: " + down);
                writeKeyEvent(modifier, down);
                remoteKeyboardState.updateRemoteMetaState(modifierMask, down);
            }
        }
    }

    public void startTiming() {
        timing = true;

        // Carry over up to 1s worth of previous rate for smoothing.

        if (timeWaitedIn100us > 10000) {
            timedKbits = timedKbits * 10000 / timeWaitedIn100us;
            timeWaitedIn100us = 10000;
        }
    }

    //
    // Write key events to set the correct modifier state.
    //

    public void stopTiming() {
        timing = false;
        if (timeWaitedIn100us < timedKbits / 2)
            timeWaitedIn100us = timedKbits / 2; // upper limit 20Mbit/s
    }
    //
    // Compress and write the data into the recorded session file. This
    // method assumes the recording is on (rec != null).
    //

    public long kbitsPerSecond() {
        return timedKbits * 10000 / timeWaitedIn100us;
    }

    public long timeWaited() {
        return timeWaitedIn100us;
    }

    public void readFully(byte b[]) throws IOException {
        readFully(b, 0, b.length);
    }

    public void readFully(byte b[], int off, int len) throws IOException {
        // TODO: Try reenabling timing and set color according to bandwidth
    /*
    long before = 0;
    timing = false; // for test

    if (timing)
      before = System.currentTimeMillis();
     */

        is.readBytes(b, off, len);

    /*
    if (timing) {
      long after = System.currentTimeMillis();
      long newTimeWaited = (after - before) * 10;
      int newKbits = len * 8 / 1000;

      // limit rate to between 10kbit/s and 40Mbit/s

      if (newTimeWaited > newKbits*1000) newTimeWaited = newKbits*1000;
      if (newTimeWaited < newKbits/4)    newTimeWaited = newKbits/4;

      timeWaitedIn100us += newTimeWaited;
      timedKbits += newKbits;
    }
    */
    }

    final int readU8() throws IOException {
        return is.readUnsignedByte();
    }

    final int readU16() throws IOException {
        return is.readUnsignedShort();
    }

    final int readU32() throws IOException {
        return is.readInt();
    }

    // readString() reads a string - a U32 length followed by the data.
    public final String readString() throws Exception {
        int len = readU32();
        if (len > maxStringLength)
            throw new Exception("Max string length exceeded");

        byte[] str = new byte[len];
        readFully(str, 0, len);
        String utf8string = new String();
        try {
            utf8string = new String(str, "UTF8");
        } catch (java.io.UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return utf8string;
    }

    public InStream getInStream() {
        return is;
    }

    public OutStream getOutStream() {
        return os;
    }

    public void setStreams(InStream is_, OutStream os_) {
        // After much testing, 8192 does seem like the best compromize between
        // responsiveness and throughput.
        Log.d(TAG, "setStreams");
        is = is_;
        os = os_;
    }

    synchronized void writeOpenChat() throws Exception {
        os.write(TextChat); // byte type
        os.write(0); // byte pad 1
        os.write(0); // byte pad 2
        os.write(0); // byte pad 2
        writeInt(CHAT_OPEN); // int message length
    }

    synchronized void writeCloseChat() throws Exception {
        os.write(TextChat); // byte type
        os.write(0); // byte pad 1
        os.write(0); // byte pad 2
        os.write(0); // byte pad 2
        writeInt(CHAT_CLOSE); // int message length
    }

    synchronized void writeFinishedChat() throws Exception {
        os.write(TextChat); // byte type
        os.write(0); // byte pad 1
        os.write(0); // byte pad 2
        os.write(0); // byte pad 2
        writeInt(CHAT_FINISHED); // int message length
    }

    String readTextChatMsg() throws Exception {
        byte[] pad = new byte[3];
        readFully(pad);
        int len = is.readInt();
        if (len == CHAT_OPEN) {
            // Remote user requests chat
            ///viewer.openChat();
            // Respond to chat request
            writeOpenChat();
            return null;
        } else if (len == CHAT_CLOSE) {
            // Remote user ends chat
            ///viewer.closeChat();
            return null;
        } else if (len == CHAT_FINISHED) {
            // Remote user says chat finished.
            // Not sure why I should care about this state.
            return null;
        } else {
            // Remote user sends message!!
            if (len > 0) {
                byte[] msg = new byte[len];
                readFully(msg);
                return new String(msg);
            }
        }
        return null;
    }

    public synchronized void writeChatMessage(String msg) throws Exception {
        os.write(TextChat); // byte type
        os.write(0); // byte pad 1
        os.write(0); // byte pad 2
        os.write(0); // byte pad 2
        byte[] bytes = msg.getBytes("8859_1");
        byte[] outgoing = bytes;
        if (bytes.length > 4096) {
            outgoing = new byte[4096];
            System.arraycopy(bytes, 0, outgoing, 0, 4096);
        }
        writeInt(outgoing.length); // int message length
        os.write(outgoing); // message
    }

    // The following methods are implementations of the RfbConnectable interface
    @Override
    public int framebufferWidth() {
        return framebufferWidth;
    }

    @Override
    public int framebufferHeight() {
        return framebufferHeight;
    }

    @Override
    public String desktopName() {
        return desktopName;
    }

    @Override
    public void requestUpdate(boolean incremental) {
        writeFramebufferUpdateRequest(0, 0, framebufferWidth, framebufferHeight, incremental);
    }

    @Override
    public void writeClientCutText(String text) {
        try {
            writeClientCutText(text, text.length());
        } catch (IOException e) {
            Log.e(TAG, "Could not write text to VNC server clipboard.");
            e.printStackTrace();
        }
    }

    @Override
    public void setIsInNormalProtocol(boolean state) {
        this.inNormalProtocol = state;
    }

    @Override
    public boolean isInNormalProtocol() {
        return this.inNormalProtocol;
    }

    public String getEncoding() {
        switch (preferredEncoding) {
            case RfbProto.EncodingRaw:
                return "RAW";
            case RfbProto.EncodingTight:
                return "TIGHT";
            case RfbProto.EncodingTightZstd:
                return "TIGHTZSTD";
            case RfbProto.EncodingCoRRE:
                return "CoRRE";
            case RfbProto.EncodingHextile:
                return "HEXTILE";
            case RfbProto.EncodingRRE:
                return "RRE";
            case RfbProto.EncodingZlib:
                return "ZLIB";
            case RfbProto.EncodingZRLE:
                return "ZRLE";
        }
        return "";
    }

    private void setEncodings() {
        if (!inNormalProtocol)
            return;

        int[] encodings = new int[20];
        int nEncodings = 0;

        encodings[nEncodings++] = preferredEncoding;
        encodings[nEncodings++] = RfbProto.EncodingTight;
        encodings[nEncodings++] = RfbProto.EncodingZRLE;
        encodings[nEncodings++] = RfbProto.EncodingHextile;
        encodings[nEncodings++] = RfbProto.EncodingZlib;
        encodings[nEncodings++] = RfbProto.EncodingCoRRE;
        encodings[nEncodings++] = RfbProto.EncodingRRE;

        encodings[nEncodings++] = RfbProto.EncodingCopyRect;

        encodings[nEncodings++] = RfbProto.EncodingCompressLevel0 + compressLevel;
        encodings[nEncodings++] = RfbProto.EncodingQualityLevel0 + jpegQuality;

        encodings[nEncodings++] = RfbProto.EncodingXCursor;
        encodings[nEncodings++] = RfbProto.EncodingRichCursor;

        encodings[nEncodings++] = RfbProto.EncodingPointerPos;
        encodings[nEncodings++] = RfbProto.EncodingLastRect;
        encodings[nEncodings++] = RfbProto.EncodingNewFBSize;
        encodings[nEncodings++] = RfbProto.EncodingExtendedDesktopSize;

        // TODO: Disabling ClientRedirect encoding for now because of
        // it being reserved for CursorWithAlpha by RealVNC and for
        // ClientRedirect by IANA. This can be reenabled once the
        // problem has been resolved.
        //encodings[nEncodings++] = RfbProto.EncodingClientRedirect;

        boolean encodingsWereChanged = false;
        if (nEncodings != nEncodingsSaved) {
            encodingsWereChanged = true;
        } else {
            for (int i = 0; i < nEncodings; i++) {
                if (encodings[i] != encodingsSaved[i]) {
                    encodingsWereChanged = true;
                    break;
                }
            }
        }

        if (encodingsWereChanged) {
            try {
                writeSetEncodings(encodings, nEncodings);
            } catch (Exception e) {
                e.printStackTrace();
            }
            encodingsSaved = encodings;
            nEncodingsSaved = nEncodings;
        }
    }

    public void processProtocol() throws Exception {
        boolean exitforloop = false;
        int msgType = 0;

        try {
            setEncodings();
            remoteConnection.writeFullUpdateRequest(false);

            //
            // main dispatch loop
            //
            while (maintainConnection) {
                exitforloop = false;
                if (!canvas.isUseFull()) {
                    canvas.syncScroll();
                    // Read message type from the server.
                    msgType = readServerMessageType();
                    canvas.doneWaiting();
                } else {
                    msgType = readServerMessageType();
                }

                // Process the message depending on its type.
                switch (msgType) {
                    case RfbProto.FramebufferUpdate:
                        readFramebufferUpdate();

                        for (int i = 0; i < updateNRects; i++) {
                            readFramebufferUpdateRectHdr();

                            switch (updateRectEncoding) {
                                case RfbProto.EncodingTight:
                                    decoder.handleTightRect(this, updateRectX, updateRectY, updateRectW, updateRectH, false);
                                    break;
                                case RfbProto.EncodingTightZstd:
                                    decoder.handleTightRect(this, updateRectX, updateRectY, updateRectW, updateRectH, true);
                                    break;
                                case RfbProto.EncodingPointerPos:
                                    canvas.softCursorMove(updateRectX, updateRectY);
                                    break;
                                case RfbProto.EncodingXCursor:
                                case RfbProto.EncodingRichCursor:
                                    decoder.handleCursorShapeUpdate(this, updateRectEncoding, updateRectX, updateRectY,
                                            updateRectW, updateRectH);
                                    break;
                                case RfbProto.EncodingLastRect:
                                    exitforloop = true;
                                    break;
                                case RfbProto.EncodingCopyRect:
                                    decoder.handleCopyRect(this, updateRectX, updateRectY, updateRectW, updateRectH);
                                    break;
                                case RfbProto.EncodingNewFBSize:
                                    setFramebufferSize(updateRectW, updateRectH);
                                    canvas.reallocateDrawable(updateRectW, updateRectH);
                                    exitforloop = true;
                                    break;
                                case RfbProto.EncodingRaw:
                                    decoder.handleRawRect(this, updateRectX, updateRectY, updateRectW, updateRectH);
                                    break;
                                case RfbProto.EncodingRRE:
                                    decoder.handleRRERect(this, updateRectX, updateRectY, updateRectW, updateRectH);
                                    break;
                                case RfbProto.EncodingCoRRE:
                                    decoder.handleCoRRERect(this, updateRectX, updateRectY, updateRectW, updateRectH);
                                    break;
                                case RfbProto.EncodingHextile:
                                    decoder.handleHextileRect(this, updateRectX, updateRectY, updateRectW, updateRectH);
                                    break;
                                case RfbProto.EncodingZRLE:
                                    decoder.handleZRLERect(this, updateRectX, updateRectY, updateRectW, updateRectH);
                                    break;
                                case RfbProto.EncodingZlib:
                                    decoder.handleZlibRect(this, updateRectX, updateRectY, updateRectW, updateRectH);
                                    break;
                                case RfbProto.EncodingClientRedirect:
                                    readClientRedirect(updateRectX, updateRectY, updateRectW, updateRectH);
                                    break;
                                case RfbProto.EncodingExtendedDesktopSize:
                                    Log.d(TAG, "EncodingExtendedDesktopSize, wxh: " + updateRectW + "x" + updateRectH);
                                    handleExtendedDesktopSize();
                                    break;
                                default:
                                    Log.e(TAG, "Unknown RFB rectangle encoding " + updateRectEncoding +
                                            " (0x" + Integer.toHexString(updateRectEncoding) + ")");
                            }

                            if (exitforloop) {
                                exitforloop = false;
                                break;
                            }
                        }

                        if (decoder.isChangedColorModel()) {
                            decoder.setPixelFormat(this);
                            //setEncodings();
                            remoteConnection.writeFullUpdateRequest(false);
                        } else {
                            //setEncodings();
                            remoteConnection.writeFullUpdateRequest(true);
                        }
                        break;

                    case RfbProto.SetColourMapEntries:
                        throw new Exception("Can't handle SetColourMapEntries message");

                    case RfbProto.Bell:
                        canvas.displayShortToastMessage("VNC Beep");
                        break;

                    case RfbProto.ServerCutText:
                        remoteClipboardChanged(readServerCutText());
                        break;

                    case RfbProto.TextChat:
                        // UltraVNC extension
                        String msg = readTextChatMsg();
                        if (msg != null && msg.length() > 0) {
                            // TODO implement chat interface
                        }
                        break;

                    case 14:
                        // This message is sent by UltraVNC when color < 24bit is requested by the client
                        throw new RfbUltraVncColorMapException("Only 24bpp color supported with UltraVNC");

                    default:
                        throw new Exception("Unknown RFB message type " + msgType);
                }
            }
        } catch (Exception e) {
            String m = e.getMessage();
            Log.e(TAG, "Exception caught while processing protocol: " + m);
            e.printStackTrace();
            closeSocket();
            throw e;
        } finally {
            Log.v(TAG, "Closing VNC Connection");
            closeSocket();
        }
        closeSocket();
    }

    /**
     * This method handles the pseudo encoding ExtendedDesktopSize enabling a remote resizing of the vnc session
     * Protocol: https://github.com/rfbproto/rfbproto/blob/master/rfbproto.rst#extendeddesktopsize-pseudo-encoding
     */
    private void handleExtendedDesktopSize() throws Exception {
        Log.d(TAG, "handleExtendedDesktopSize");
        // Is Supported:
        boolean firstUpdate = !this.isExtendedDesktopSizeSupported;

        this.isExtendedDesktopSizeSupported = true;

        // Read the number of screens
        int screenCount = is.readUnsignedByte();

        // Read an empty padding of 3 bytes and ignoring them
        byte[] padding = new byte[3];
        readFully(padding);

        int width = 0;
        int height = 0;
        // Read the SCREEN structure, each of them needs 16 bytes
        for (int i = 0; i < screenCount; i++) {
            if (i == 0) {
                this.screenId = is.readInt();

                // Read the xPosition -> In this protocol it indicates the reason for the change
                int xPos = is.readUnsignedShort();

                // Read the yPosition -> In this protocol it indicates the status code
                int yPos = is.readUnsignedShort();

                // Read the width
                width = is.readUnsignedShort();

                // Read the height
                height = is.readUnsignedShort();

                // Read the screen flags
                this.screenFlags = is.readInt();
            } else {
                // Ignore all other screens
                byte[] screenBuffer = new byte[16];
                readFully(screenBuffer);
            }
        }

        Log.d(TAG, "handleExtendedDesktopSize, wxh: " + width + "x" + height);
        if (width != 0 && height != 0) {
            setFramebufferSize(width, height);
            canvas.reallocateDrawable(width, height);
        }
        if (preferredFramebufferWidth != 0 && preferredFramebufferHeight != 0) {
            // Notifiy Listeners on first update
            if (firstUpdate) {
                requestResolution(this.preferredFramebufferWidth, this.preferredFramebufferHeight);
            }
        }
    }

    @Override
    public void requestResolution(int x, int y) throws Exception {
        Log.d(TAG, "requestResolution, wxh: " + x + "x" + y);
        this.setPreferredFramebufferSize(x, y);

        // Is this encoding supported by the server?
        if (!this.isExtendedDesktopSizeSupported) {
            Log.d(TAG, "requestResolution: ExtendedDesktopSize not supported, not continuing");
            return;
        }
        Log.d(TAG, "requestResolution: ExtendedDesktopSize supported, continuing");

        byte[] setDesktopSizeBuff = new byte[24];

        // MSG-Byte -> 251 -> Framebuffer update
        setDesktopSizeBuff[0] = (byte) 251;

        // Padding
        setDesktopSizeBuff[1] = 0;

        // Width
        setDesktopSizeBuff[2] = (byte) (x >> 8);
        setDesktopSizeBuff[3] = (byte) x;

        // Height
        setDesktopSizeBuff[4] = (byte) (y >> 8);
        setDesktopSizeBuff[5] = (byte) y;

        // Number of screens
        setDesktopSizeBuff[6] = 1;

        // Padding
        setDesktopSizeBuff[7] = 0;

        // Screen array
        setDesktopSizeBuff[8] = (byte) (this.screenId >> 24);
        setDesktopSizeBuff[9] = (byte) (this.screenId >> 16);
        setDesktopSizeBuff[10] = (byte) (this.screenId >> 8);
        setDesktopSizeBuff[11] = (byte) this.screenId;

        // X-Y Position -> Fixed values
        setDesktopSizeBuff[12] = 0;
        setDesktopSizeBuff[13] = 0;
        setDesktopSizeBuff[14] = 0;
        setDesktopSizeBuff[15] = 0;

        // Width
        setDesktopSizeBuff[16] = (byte) (x >> 8);
        setDesktopSizeBuff[17] = (byte) x;

        // Height
        setDesktopSizeBuff[18] = (byte) (y >> 8);
        setDesktopSizeBuff[19] = (byte) y;

        // Flags
        setDesktopSizeBuff[20] = (byte) (this.screenFlags >> 24);
        setDesktopSizeBuff[21] = (byte) (this.screenFlags >> 16);
        setDesktopSizeBuff[22] = (byte) (this.screenFlags >> 8);
        setDesktopSizeBuff[23] = (byte) this.screenFlags;

        try {
            os.write(setDesktopSizeBuff);
        } catch (IOException e) {
            Log.e(TAG, "Sending the ExtendedDesktopSize Frame failed");
            throw new Exception("Sending the ExtendedDesktopSize Frame failed");
        }
    }

    public class RfbPasswordAuthenticationException extends Exception {
        public RfbPasswordAuthenticationException(String errorMessage) {
            super(errorMessage);
        }
    }

    public class RfbUserPassAuthFailedOrUsernameRequiredException extends Exception {
        public RfbUserPassAuthFailedOrUsernameRequiredException(String errorMessage) {
            super(errorMessage);
        }
    }

    public class RfbUltraVncColorMapException extends Exception {
        public RfbUltraVncColorMapException(String errorMessage) {
            super(errorMessage);
        }
    }

    @Override
    public void setBitmapData(AbstractDrawableData drawable) {
        decoder.setBitmapData(drawable);
    }

    public int getFramebufferWidth() {
        return framebufferWidth;
    }

    public int getFramebufferHeight() {
        return framebufferHeight;
    }
}
