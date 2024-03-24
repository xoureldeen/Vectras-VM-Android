// This class used to be generated from com.iiordanov.bVNC.IConnectionBean by an Eclipse plugin
// Since the switch to Android Studio that plugin is no longer operational / in use and this file
// is being edited by hand.
package com.iiordanov.bVNC;

import com.undatech.opaque.Connection;

public abstract class AbstractConnectionBean extends com.antlersoft.android.dbimpl.IdImplementationBase implements Connection {

    public static final String GEN_TABLE_NAME = "CONNECTION_BEAN";
    public static final int GEN_COUNT = 92;

    // Field constants
    public static final String GEN_FIELD__ID = "_id";
    public static final int GEN_ID__ID = 0;
    public static final String GEN_FIELD_NICKNAME = "NICKNAME";
    public static final int GEN_ID_NICKNAME = 1;
    public static final String GEN_FIELD_CONNECTIONTYPE = "CONNECTIONTYPE";
    public static final int GEN_ID_CONNECTIONTYPE = 2;
    public static final String GEN_FIELD_SSHSERVER = "SSHSERVER";
    public static final int GEN_ID_SSHSERVER = 3;
    public static final String GEN_FIELD_SSHPORT = "SSHPORT";
    public static final int GEN_ID_SSHPORT = 4;
    public static final String GEN_FIELD_SSHUSER = "SSHUSER";
    public static final int GEN_ID_SSHUSER = 5;
    public static final String GEN_FIELD_SSHPASSWORD = "SSHPASSWORD";
    public static final int GEN_ID_SSHPASSWORD = 6;
    public static final String GEN_FIELD_KEEPSSHPASSWORD = "KEEPSSHPASSWORD";
    public static final int GEN_ID_KEEPSSHPASSWORD = 7;
    public static final String GEN_FIELD_SSHPUBKEY = "SSHPUBKEY";
    public static final int GEN_ID_SSHPUBKEY = 8;
    public static final String GEN_FIELD_SSHPRIVKEY = "SSHPRIVKEY";
    public static final int GEN_ID_SSHPRIVKEY = 9;
    public static final String GEN_FIELD_SSHPASSPHRASE = "SSHPASSPHRASE";
    public static final int GEN_ID_SSHPASSPHRASE = 10;
    public static final String GEN_FIELD_USESSHPUBKEY = "USESSHPUBKEY";
    public static final int GEN_ID_USESSHPUBKEY = 11;
    public static final String GEN_FIELD_SSHREMOTECOMMANDOS = "SSHREMOTECOMMANDOS";
    public static final int GEN_ID_SSHREMOTECOMMANDOS = 12;
    public static final String GEN_FIELD_SSHREMOTECOMMANDTYPE = "SSHREMOTECOMMANDTYPE";
    public static final int GEN_ID_SSHREMOTECOMMANDTYPE = 13;
    public static final String GEN_FIELD_AUTOXTYPE = "AUTOXTYPE";
    public static final int GEN_ID_AUTOXTYPE = 14;
    public static final String GEN_FIELD_AUTOXCOMMAND = "AUTOXCOMMAND";
    public static final int GEN_ID_AUTOXCOMMAND = 15;
    public static final String GEN_FIELD_AUTOXENABLED = "AUTOXENABLED";
    public static final int GEN_ID_AUTOXENABLED = 16;
    public static final String GEN_FIELD_AUTOXRESTYPE = "AUTOXRESTYPE";
    public static final int GEN_ID_AUTOXRESTYPE = 17;
    public static final String GEN_FIELD_AUTOXWIDTH = "AUTOXWIDTH";
    public static final int GEN_ID_AUTOXWIDTH = 18;
    public static final String GEN_FIELD_AUTOXHEIGHT = "AUTOXHEIGHT";
    public static final int GEN_ID_AUTOXHEIGHT = 19;
    public static final String GEN_FIELD_AUTOXSESSIONPROG = "AUTOXSESSIONPROG";
    public static final int GEN_ID_AUTOXSESSIONPROG = 20;
    public static final String GEN_FIELD_AUTOXSESSIONTYPE = "AUTOXSESSIONTYPE";
    public static final int GEN_ID_AUTOXSESSIONTYPE = 21;
    public static final String GEN_FIELD_AUTOXUNIXPW = "AUTOXUNIXPW";
    public static final int GEN_ID_AUTOXUNIXPW = 22;
    public static final String GEN_FIELD_AUTOXUNIXAUTH = "AUTOXUNIXAUTH";
    public static final int GEN_ID_AUTOXUNIXAUTH = 23;
    public static final String GEN_FIELD_AUTOXRANDFILENM = "AUTOXRANDFILENM";
    public static final int GEN_ID_AUTOXRANDFILENM = 24;
    public static final String GEN_FIELD_SSHREMOTECOMMAND = "SSHREMOTECOMMAND";
    public static final int GEN_ID_SSHREMOTECOMMAND = 25;
    public static final String GEN_FIELD_SSHREMOTECOMMANDTIMEOUT = "SSHREMOTECOMMANDTIMEOUT";
    public static final int GEN_ID_SSHREMOTECOMMANDTIMEOUT = 26;
    public static final String GEN_FIELD_USESSHREMOTECOMMAND = "USESSHREMOTECOMMAND";
    public static final int GEN_ID_USESSHREMOTECOMMAND = 27;
    public static final String GEN_FIELD_SSHHOSTKEY = "SSHHOSTKEY";
    public static final int GEN_ID_SSHHOSTKEY = 28;
    public static final String GEN_FIELD_ADDRESS = "ADDRESS";
    public static final int GEN_ID_ADDRESS = 29;
    public static final String GEN_FIELD_PORT = "PORT";
    public static final int GEN_ID_PORT = 30;
    public static final String GEN_FIELD_CACERT = "CACERT";
    public static final int GEN_ID_CACERT = 31;
    public static final String GEN_FIELD_CACERTPATH = "CACERTPATH";
    public static final int GEN_ID_CACERTPATH = 32;
    public static final String GEN_FIELD_TLSPORT = "TLSPORT";
    public static final int GEN_ID_TLSPORT = 33;
    public static final String GEN_FIELD_CERTSUBJECT = "CERTSUBJECT";
    public static final int GEN_ID_CERTSUBJECT = 34;
    public static final String GEN_FIELD_PASSWORD = "PASSWORD";
    public static final int GEN_ID_PASSWORD = 35;
    public static final String GEN_FIELD_COLORMODEL = "COLORMODEL";
    public static final int GEN_ID_COLORMODEL = 36;
    public static final String GEN_FIELD_PREFENCODING = "PREFENCODING";
    public static final int GEN_ID_PREFENCODING = 37;
    public static final String GEN_FIELD_EXTRAKEYSTOGGLETYPE = "EXTRAKEYSTOGGLETYPE";
    public static final int GEN_ID_EXTRAKEYSTOGGLETYPE = 38;
    public static final String GEN_FIELD_FORCEFULL = "FORCEFULL";
    public static final int GEN_ID_FORCEFULL = 39;
    public static final String GEN_FIELD_REPEATERID = "REPEATERID";
    public static final int GEN_ID_REPEATERID = 40;
    public static final String GEN_FIELD_INPUTMODE = "INPUTMODE";
    public static final int GEN_ID_INPUTMODE = 41;
    public static final String GEN_FIELD_SCALEMODE = "SCALEMODE";
    public static final int GEN_ID_SCALEMODE = 42;
    public static final String GEN_FIELD_USEDPADASARROWS = "USEDPADASARROWS";
    public static final int GEN_ID_USEDPADASARROWS = 43;
    public static final String GEN_FIELD_ROTATEDPAD = "ROTATEDPAD";
    public static final int GEN_ID_ROTATEDPAD = 44;
    public static final String GEN_FIELD_USEPORTRAIT = "USEPORTRAIT";
    public static final int GEN_ID_USEPORTRAIT = 45;
    public static final String GEN_FIELD_USELOCALCURSOR = "USELOCALCURSOR";
    public static final int GEN_ID_USELOCALCURSOR = 46;
    public static final String GEN_FIELD_KEEPPASSWORD = "KEEPPASSWORD";
    public static final int GEN_ID_KEEPPASSWORD = 47;
    public static final String GEN_FIELD_FOLLOWMOUSE = "FOLLOWMOUSE";
    public static final int GEN_ID_FOLLOWMOUSE = 48;
    public static final String GEN_FIELD_USEREPEATER = "USEREPEATER";
    public static final int GEN_ID_USEREPEATER = 49;
    public static final String GEN_FIELD_METALISTID = "METALISTID";
    public static final int GEN_ID_METALISTID = 50;
    public static final String GEN_FIELD_LAST_META_KEY_ID = "LAST_META_KEY_ID";
    public static final int GEN_ID_LAST_META_KEY_ID = 51;
    public static final String GEN_FIELD_FOLLOWPAN = "FOLLOWPAN";
    public static final int GEN_ID_FOLLOWPAN = 52;
    public static final String GEN_FIELD_USERNAME = "USERNAME";
    public static final int GEN_ID_USERNAME = 53;
    public static final String GEN_FIELD_RDPDOMAIN = "RDPDOMAIN";
    public static final int GEN_ID_RDPDOMAIN = 54;
    public static final String GEN_FIELD_SECURECONNECTIONTYPE = "SECURECONNECTIONTYPE";
    public static final int GEN_ID_SECURECONNECTIONTYPE = 55;
    public static final String GEN_FIELD_SHOWZOOMBUTTONS = "SHOWZOOMBUTTONS";
    public static final int GEN_ID_SHOWZOOMBUTTONS = 56;
    public static final String GEN_FIELD_DOUBLE_TAP_ACTION = "DOUBLE_TAP_ACTION";
    public static final int GEN_ID_DOUBLE_TAP_ACTION = 57;
    public static final String GEN_FIELD_RDPRESTYPE = "RDPRESTYPE";
    public static final int GEN_ID_RDPRESTYPE = 58;
    public static final String GEN_FIELD_RDPWIDTH = "RDPWIDTH";
    public static final int GEN_ID_RDPWIDTH = 59;
    public static final String GEN_FIELD_RDPHEIGHT = "RDPHEIGHT";
    public static final int GEN_ID_RDPHEIGHT = 60;
    public static final String GEN_FIELD_RDPCOLOR = "RDPCOLOR";
    public static final int GEN_ID_RDPCOLOR = 61;
    public static final String GEN_FIELD_REMOTEFX = "REMOTEFX";
    public static final int GEN_ID_REMOTEFX = 62;
    public static final String GEN_FIELD_DESKTOPBACKGROUND = "DESKTOPBACKGROUND";
    public static final int GEN_ID_DESKTOPBACKGROUND = 63;
    public static final String GEN_FIELD_FONTSMOOTHING = "FONTSMOOTHING";
    public static final int GEN_ID_FONTSMOOTHING = 64;
    public static final String GEN_FIELD_DESKTOPCOMPOSITION = "DESKTOPCOMPOSITION";
    public static final int GEN_ID_DESKTOPCOMPOSITION = 65;
    public static final String GEN_FIELD_WINDOWCONTENTS = "WINDOWCONTENTS";
    public static final int GEN_ID_WINDOWCONTENTS = 66;
    public static final String GEN_FIELD_MENUANIMATION = "MENUANIMATION";
    public static final int GEN_ID_MENUANIMATION = 67;
    public static final String GEN_FIELD_VISUALSTYLES = "VISUALSTYLES";
    public static final int GEN_ID_VISUALSTYLES = 68;
    public static final String GEN_FIELD_REDIRECTSDCARD = "REDIRECTSDCARD";
    public static final int GEN_ID_REDIRECTSDCARD = 69;
    public static final String GEN_FIELD_CONSOLEMODE = "CONSOLEMODE";
    public static final int GEN_ID_CONSOLEMODE = 70;
    public static final String GEN_FIELD_ENABLESOUND = "ENABLESOUND";
    public static final int GEN_ID_ENABLESOUND = 71;
    public static final String GEN_FIELD_ENABLERECORDING = "ENABLERECORDING";
    public static final int GEN_ID_ENABLERECORDING = 72;
    public static final String GEN_FIELD_REMOTESOUNDTYPE = "REMOTESOUNDTYPE";
    public static final int GEN_ID_REMOTESOUNDTYPE = 73;
    public static final String GEN_FIELD_VIEWONLY = "VIEWONLY";
    public static final int GEN_ID_VIEWONLY = 74;
    public static final String GEN_FIELD_LAYOUTMAP = "LAYOUTMAP";
    public static final int GEN_ID_LAYOUTMAP = 75;

    public static final String GEN_FIELD_FILENAME = "FILENAME";
    public static final int GEN_ID_FILENAME = 76;
    public static final String GEN_FIELD_X509KEYSIGNATURE = "X509KEYSIGNATURE";
    public static final int GEN_ID_X509KEYSIGNATURE = 77;
    public static final String GEN_FIELD_SCREENSHOTFILENAME = "SCREENSHOTFILENAME";
    public static final int GEN_ID_SCREENSHOTFILENAME = 78;

    public static final String GEN_FIELD_ENABLEGFX = "ENABLEGFX";
    public static final int GEN_ID_ENABLEGFX = 79;
    public static final String GEN_FIELD_ENABLEGFXH264 = "ENABLEGFXH264";
    public static final int GEN_ID_ENABLEGFXH264 = 80;

    public static final String GEN_FIELD_PREFERSENDINGUNICODE = "PREFERSENDINGUNICODE";
    public static final int GEN_ID_PREFERSENDINGUNICODE = 81;

    public static final String GEN_FIELD_EXTERNALID = "EXTERNALID";
    public static final int GEN_ID_EXTERNALID = 82;
    public static final String GEN_FIELD_REQUIRESVPN = "REQUIRESVPN";
    public static final int GEN_ID_REQUIRESVPN = 83;
    public static final String GEN_FIELD_VPNURISCHEME = "VPNURISCHEME";
    public static final int GEN_ID_VPNURISCHEME = 84;

    public static final String GEN_FIELD_RDPGATEWAYENABLED = "RDPGATEWAYENABLED";
    public static final int GEN_ID_RDPGATEWAYENABLED = 85;
    public static final String GEN_FIELD_RDPGATEWAYHOSTNAME = "RDPGATEWAYHOSTNAME";
    public static final int GEN_ID_RDPGATEWAYHOSTNAME = 86;
    public static final String GEN_FIELD_RDPGATEWAYPORT = "RDPGATEWAYPORT";
    public static final int GEN_ID_RDPGATEWAYPORT = 87;
    public static final String GEN_FIELD_RDPGATEWAYUSERNAME = "RDPGATEWAYUSERNAME";
    public static final int GEN_ID_RDPGATEWAYUSERNAME = 88;
    public static final String GEN_FIELD_RDPGATEWAYDOMAIN = "RDPGATEWAYDOMAIN";
    public static final int GEN_ID_RDPGATEWAYDOMAIN = 89;
    public static final String GEN_FIELD_RDPGATEWAYPASSWORD = "RDPGATEWAYPASSWORD";
    public static final int GEN_ID_RDPGATEWAYPASSWORD = 90;
    public static final String GEN_FIELD_KEEPRDPGATEWAYPASSWORD = "KEEPRDPGATEWAYPASSWORD";
    public static final int GEN_ID_KEEPRDPGATEWAYPASSWORD = 91;

    // SQL Command for creating the table
    public static String GEN_CREATE = "CREATE TABLE CONNECTION_BEAN (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "NICKNAME TEXT," +
            "CONNECTIONTYPE INTEGER," +
            "SSHSERVER TEXT," +
            "SSHPORT INTEGER," +
            "SSHUSER TEXT," +
            "SSHPASSWORD TEXT," +
            "KEEPSSHPASSWORD INTEGER," +
            "SSHPUBKEY TEXT," +
            "SSHPRIVKEY TEXT," +
            "SSHPASSPHRASE TEXT," +
            "USESSHPUBKEY INTEGER," +
            "SSHREMOTECOMMANDOS INTEGER," +
            "SSHREMOTECOMMANDTYPE INTEGER," +
            "AUTOXTYPE INTEGER," +
            "AUTOXCOMMAND TEXT," +
            "AUTOXENABLED INTEGER," +
            "AUTOXRESTYPE INTEGER," +
            "AUTOXWIDTH INTEGER," +
            "AUTOXHEIGHT INTEGER," +
            "AUTOXSESSIONPROG TEXT," +
            "AUTOXSESSIONTYPE INTEGER," +
            "AUTOXUNIXPW INTEGER," +
            "AUTOXUNIXAUTH INTEGER," +
            "AUTOXRANDFILENM TEXT," +
            "SSHREMOTECOMMAND TEXT," +
            "SSHREMOTECOMMANDTIMEOUT INTEGER," +
            "USESSHREMOTECOMMAND INTEGER," +
            "SSHHOSTKEY TEXT," +
            "ADDRESS TEXT," +
            "PORT INTEGER," +
            "CACERT TEXT," +
            "CACERTPATH TEXT," +
            "TLSPORT INTEGER," +
            "CERTSUBJECT TEXT," +
            "PASSWORD TEXT," +
            "COLORMODEL TEXT," +
            "PREFENCODING INTEGER," +
            "EXTRAKEYSTOGGLETYPE INTEGER," +
            "FORCEFULL INTEGER," +
            "REPEATERID TEXT," +
            "INPUTMODE TEXT," +
            "SCALEMODE TEXT," +
            "USEDPADASARROWS INTEGER," +
            "ROTATEDPAD INTEGER," +
            "USEPORTRAIT INTEGER," +
            "USELOCALCURSOR INTEGER," +
            "KEEPPASSWORD INTEGER," +
            "FOLLOWMOUSE INTEGER," +
            "USEREPEATER INTEGER," +
            "METALISTID INTEGER," +
            "LAST_META_KEY_ID INTEGER," +
            "FOLLOWPAN INTEGER DEFAULT 0," +
            "USERNAME TEXT," +
            "RDPDOMAIN TEXT," +
            "SECURECONNECTIONTYPE TEXT," +
            "SHOWZOOMBUTTONS INTEGER DEFAULT 1," +
            "DOUBLE_TAP_ACTION TEXT," +
            "RDPRESTYPE INTEGER," +
            "RDPWIDTH INTEGER," +
            "RDPHEIGHT INTEGER," +
            "RDPCOLOR INTEGER," +
            "REMOTEFX INTEGER," +
            "DESKTOPBACKGROUND INTEGER," +
            "FONTSMOOTHING INTEGER," +
            "DESKTOPCOMPOSITION INTEGER," +
            "WINDOWCONTENTS INTEGER," +
            "MENUANIMATION INTEGER," +
            "VISUALSTYLES INTEGER," +
            "REDIRECTSDCARD INTEGER," +
            "CONSOLEMODE INTEGER," +
            "ENABLESOUND INTEGER," +
            "ENABLERECORDING INTEGER," +
            "REMOTESOUNDTYPE INTEGER," +
            "VIEWONLY INTEGER," +
            "LAYOUTMAP TEXT," +
            "FILENAME TEXT," +
            "X509KEYSIGNATURE TEXT," +
            "SCREENSHOTFILENAME TEXT," +
            "ENABLEGFX INTEGER," +
            "ENABLEGFXH264 INTEGER," +
            "PREFERSENDINGUNICODE INTEGER," +
            "EXTERNALID TEXT," +
            "REQUIRESVPN INTEGER," +
            "VPNURISCHEME TEXT," +
            "RDPGATEWAYENABLED INTEGER," +
            "RDPGATEWAYHOSTNAME TEXT," +
            "RDPGATEWAYPORT INTEGER," +
            "RDPGATEWAYUSERNAME TEXT," +
            "RDPGATEWAYDOMAIN TEXT," +
            "RDPGATEWAYPASSWORD TEXT," +
            "KEEPRDPGATEWAYPASSWORD INTEGER" +
            ")";

    // Members corresponding to defined fields
    private long gen__Id;
    private java.lang.String gen_nickname;
    private int gen_connectionType;
    private java.lang.String gen_sshServer;
    private int gen_sshPort;
    private java.lang.String gen_sshUser;
    private java.lang.String gen_sshPassword;
    private boolean gen_keepSshPassword;
    private java.lang.String gen_sshPubKey;
    private java.lang.String gen_sshPrivKey;
    private java.lang.String gen_sshPassPhrase;
    private boolean gen_useSshPubKey;
    private int gen_sshRemoteCommandOS;
    private int gen_sshRemoteCommandType;
    private int gen_autoXType;
    private java.lang.String gen_autoXCommand;
    private boolean gen_autoXEnabled;
    private int gen_autoXResType;
    private int gen_autoXWidth;
    private int gen_autoXHeight;
    private java.lang.String gen_autoXSessionProg;
    private int gen_autoXSessionType;
    private boolean gen_autoXUnixpw;
    private boolean gen_autoXUnixAuth;
    private java.lang.String gen_autoXRandFileNm;
    private java.lang.String gen_sshRemoteCommand;
    private int gen_sshRemoteCommandTimeout;
    private boolean gen_useSshRemoteCommand;
    private java.lang.String gen_sshHostKey;
    private java.lang.String gen_address;
    private int gen_port;
    private java.lang.String gen_caCert;
    private java.lang.String gen_caCertPath;
    private int gen_tlsPort;
    private java.lang.String gen_certSubject;
    private java.lang.String gen_password;
    private java.lang.String gen_colorModel;
    private int gen_prefEncoding;
    private int gen_extraKeysToggleType;
    private long gen_forceFull;
    private java.lang.String gen_repeaterId;
    private java.lang.String gen_inputMode;
    private java.lang.String gen_SCALEMODE;
    private boolean gen_useDpadAsArrows;
    private boolean gen_rotateDpad;
    private boolean gen_usePortrait;
    private int gen_useLocalCursor;
    private boolean gen_keepPassword;
    private boolean gen_followMouse;
    private boolean gen_useRepeater;
    private long gen_metaListId;
    private long gen_LAST_META_KEY_ID;
    private boolean gen_followPan;
    private java.lang.String gen_userName;
    private java.lang.String gen_rdpDomain;
    private java.lang.String gen_secureConnectionType;
    private boolean gen_showZoomButtons;
    private java.lang.String gen_DOUBLE_TAP_ACTION;
    private int gen_rdpResType;
    private int gen_rdpWidth;
    private int gen_rdpHeight;
    private int gen_rdpColor;
    private boolean gen_remoteFx;
    private boolean gen_desktopBackground;
    private boolean gen_fontSmoothing;
    private boolean gen_desktopComposition;
    private boolean gen_windowContents;
    private boolean gen_menuAnimation;
    private boolean gen_visualStyles;
    private boolean gen_redirectSdCard;
    private boolean gen_consoleMode;
    private boolean gen_enableSound;
    private boolean gen_enableRecording;
    private int gen_remoteSoundType;
    private boolean gen_viewOnly;
    private java.lang.String gen_layoutMap;

    private java.lang.String gen_filename;
    private java.lang.String gen_x509KeySignature;
    private java.lang.String gen_screenshotFilename;

    private boolean gen_enableGfx;
    private boolean gen_enableGfxH264;
    private boolean gen_preferSendingUnicode;

    private String gen_externalId;
    private boolean gen_requiresVpn;
    private String gen_vpnUriScheme;

    private boolean gen_rdpGatewayEnabled;
    private String gen_rdpGatewayHostname;
    private int gen_rdpGatewayPort;
    private String gen_rdpGatewayUsername;
    private String gen_rdpGatewayDomain;
    private String gen_rdpGatewayPassword;
    private boolean gen_keepRdpGatewayPassword;

    public String Gen_tableName() {
        return GEN_TABLE_NAME;
    }

    // Field accessors
    public long get_Id() {
        return gen__Id;
    }

    public void set_Id(long arg__Id) {
        gen__Id = arg__Id;
    }

    public java.lang.String getNickname() {
        return gen_nickname;
    }

    public void setNickname(java.lang.String arg_nickname) {
        gen_nickname = arg_nickname;
    }

    public int getConnectionType() {
        return gen_connectionType;
    }

    public void setConnectionType(int arg_connectionType) {
        gen_connectionType = arg_connectionType;
    }

    public java.lang.String getSshServer() {
        return gen_sshServer;
    }

    public void setSshServer(java.lang.String arg_sshServer) {
        gen_sshServer = arg_sshServer;
    }

    public int getSshPort() {
        return gen_sshPort;
    }

    public void setSshPort(int arg_sshPort) {
        gen_sshPort = arg_sshPort;
    }

    public java.lang.String getSshUser() {
        return gen_sshUser;
    }

    public void setSshUser(java.lang.String arg_sshUser) {
        gen_sshUser = arg_sshUser;
    }

    public java.lang.String getSshPassword() {
        return gen_sshPassword;
    }

    public void setSshPassword(java.lang.String arg_sshPassword) {
        gen_sshPassword = arg_sshPassword;
    }

    public boolean getKeepSshPassword() {
        return gen_keepSshPassword;
    }

    public void setKeepSshPassword(boolean arg_keepSshPassword) {
        gen_keepSshPassword = arg_keepSshPassword;
    }

    public java.lang.String getSshPubKey() {
        return gen_sshPubKey;
    }

    public void setSshPubKey(java.lang.String arg_sshPubKey) {
        gen_sshPubKey = arg_sshPubKey;
    }

    public java.lang.String getSshPrivKey() {
        return gen_sshPrivKey;
    }

    public void setSshPrivKey(java.lang.String arg_sshPrivKey) {
        gen_sshPrivKey = arg_sshPrivKey;
    }

    public java.lang.String getSshPassPhrase() {
        return gen_sshPassPhrase;
    }

    public void setSshPassPhrase(java.lang.String arg_sshPassPhrase) {
        gen_sshPassPhrase = arg_sshPassPhrase;
    }

    public boolean getUseSshPubKey() {
        return gen_useSshPubKey;
    }

    public void setUseSshPubKey(boolean arg_useSshPubKey) {
        gen_useSshPubKey = arg_useSshPubKey;
    }

    public int getSshRemoteCommandOS() {
        return gen_sshRemoteCommandOS;
    }

    public void setSshRemoteCommandOS(int arg_sshRemoteCommandOS) {
        gen_sshRemoteCommandOS = arg_sshRemoteCommandOS;
    }

    public int getSshRemoteCommandType() {
        return gen_sshRemoteCommandType;
    }

    public void setSshRemoteCommandType(int arg_sshRemoteCommandType) {
        gen_sshRemoteCommandType = arg_sshRemoteCommandType;
    }

    public int getAutoXType() {
        return gen_autoXType;
    }

    public void setAutoXType(int arg_autoXType) {
        gen_autoXType = arg_autoXType;
    }

    public java.lang.String getAutoXCommand() {
        return gen_autoXCommand;
    }

    public void setAutoXCommand(java.lang.String arg_autoXCommand) {
        gen_autoXCommand = arg_autoXCommand;
    }

    public boolean getAutoXEnabled() {
        return gen_autoXEnabled;
    }

    public void setAutoXEnabled(boolean arg_autoXEnabled) {
        gen_autoXEnabled = arg_autoXEnabled;
    }

    public int getAutoXResType() {
        return gen_autoXResType;
    }

    public void setAutoXResType(int arg_autoXResType) {
        gen_autoXResType = arg_autoXResType;
    }

    public int getAutoXWidth() {
        return gen_autoXWidth;
    }

    public void setAutoXWidth(int arg_autoXWidth) {
        gen_autoXWidth = arg_autoXWidth;
    }

    public int getAutoXHeight() {
        return gen_autoXHeight;
    }

    public void setAutoXHeight(int arg_autoXHeight) {
        gen_autoXHeight = arg_autoXHeight;
    }

    public java.lang.String getAutoXSessionProg() {
        return gen_autoXSessionProg;
    }

    public void setAutoXSessionProg(java.lang.String arg_autoXSessionProg) {
        gen_autoXSessionProg = arg_autoXSessionProg;
    }

    public int getAutoXSessionType() {
        return gen_autoXSessionType;
    }

    public void setAutoXSessionType(int arg_autoXSessionType) {
        gen_autoXSessionType = arg_autoXSessionType;
    }

    public boolean getAutoXUnixpw() {
        return gen_autoXUnixpw;
    }

    public void setAutoXUnixpw(boolean arg_autoXUnixpw) {
        gen_autoXUnixpw = arg_autoXUnixpw;
    }

    public boolean getAutoXUnixAuth() {
        return gen_autoXUnixAuth;
    }

    public void setAutoXUnixAuth(boolean arg_autoXUnixAuth) {
        gen_autoXUnixAuth = arg_autoXUnixAuth;
    }

    public java.lang.String getAutoXRandFileNm() {
        return gen_autoXRandFileNm;
    }

    public void setAutoXRandFileNm(java.lang.String arg_autoXRandFileNm) {
        gen_autoXRandFileNm = arg_autoXRandFileNm;
    }

    public java.lang.String getSshRemoteCommand() {
        return gen_sshRemoteCommand;
    }

    public void setSshRemoteCommand(java.lang.String arg_sshRemoteCommand) {
        gen_sshRemoteCommand = arg_sshRemoteCommand;
    }

    public int getSshRemoteCommandTimeout() {
        return gen_sshRemoteCommandTimeout;
    }

    public void setSshRemoteCommandTimeout(int arg_sshRemoteCommandTimeout) {
        gen_sshRemoteCommandTimeout = arg_sshRemoteCommandTimeout;
    }

    public boolean getUseSshRemoteCommand() {
        return gen_useSshRemoteCommand;
    }

    public void setUseSshRemoteCommand(boolean arg_useSshRemoteCommand) {
        gen_useSshRemoteCommand = arg_useSshRemoteCommand;
    }

    public java.lang.String getSshHostKey() {
        return gen_sshHostKey;
    }

    public void setSshHostKey(java.lang.String arg_sshHostKey) {
        gen_sshHostKey = arg_sshHostKey;
    }

    public java.lang.String getAddress() {
        return gen_address;
    }

    public void setAddress(java.lang.String arg_address) {
        gen_address = arg_address;
    }

    public int getPort() {
        return gen_port;
    }

    public void setPort(int arg_port) {
        gen_port = arg_port;
    }

    public java.lang.String getCaCert() {
        return gen_caCert;
    }

    public void setCaCert(java.lang.String arg_caCert) {
        gen_caCert = arg_caCert;
    }

    public java.lang.String getCaCertPath() {
        return gen_caCertPath;
    }

    public void setCaCertPath(java.lang.String arg_caCertPath) {
        gen_caCertPath = arg_caCertPath;
    }

    public int getTlsPort() {
        return gen_tlsPort;
    }

    public void setTlsPort(int arg_tlsPort) {
        gen_tlsPort = arg_tlsPort;
    }

    public java.lang.String getCertSubject() {
        return gen_certSubject;
    }

    public void setCertSubject(java.lang.String arg_certSubject) {
        gen_certSubject = arg_certSubject;
    }

    public java.lang.String getPassword() {
        return gen_password;
    }

    public void setPassword(java.lang.String arg_password) {
        gen_password = arg_password;
    }

    public java.lang.String getColorModel() {
        return gen_colorModel;
    }

    public void setColorModel(java.lang.String arg_colorModel) {
        gen_colorModel = arg_colorModel;
    }

    public int getPrefEncoding() {
        return gen_prefEncoding;
    }

    public void setPrefEncoding(int arg_prefEncoding) {
        gen_prefEncoding = arg_prefEncoding;
    }

    public int getExtraKeysToggleType() {
        return gen_extraKeysToggleType;
    }

    public void setExtraKeysToggleType(int arg_extraKeysToggleType) {
        gen_extraKeysToggleType = arg_extraKeysToggleType;
    }

    public long getForceFull() {
        return gen_forceFull;
    }

    public void setForceFull(long arg_forceFull) {
        gen_forceFull = arg_forceFull;
    }

    public java.lang.String getRepeaterId() {
        return gen_repeaterId;
    }

    public void setRepeaterId(java.lang.String arg_repeaterId) {
        gen_repeaterId = arg_repeaterId;
    }

    public java.lang.String getInputMode() {
        return gen_inputMode;
    }

    public void setInputMode(java.lang.String arg_inputMode) {
        gen_inputMode = arg_inputMode;
    }

    public java.lang.String getScaleModeAsString() {
        return gen_SCALEMODE;
    }

    public void setScaleModeAsString(java.lang.String arg_SCALEMODE) {
        gen_SCALEMODE = arg_SCALEMODE;
    }

    public boolean getUseDpadAsArrows() {
        return gen_useDpadAsArrows;
    }

    public void setUseDpadAsArrows(boolean arg_useDpadAsArrows) {
        gen_useDpadAsArrows = arg_useDpadAsArrows;
    }

    public boolean getRotateDpad() {
        return gen_rotateDpad;
    }

    public void setRotateDpad(boolean arg_rotateDpad) {
        gen_rotateDpad = arg_rotateDpad;
    }

    public boolean getUsePortrait() {
        return gen_usePortrait;
    }

    public void setUsePortrait(boolean arg_usePortrait) {
        gen_usePortrait = arg_usePortrait;
    }

    public int getUseLocalCursor() {
        return gen_useLocalCursor;
    }

    public void setUseLocalCursor(int arg_useLocalCursor) {
        gen_useLocalCursor = arg_useLocalCursor;
    }

    public boolean getKeepPassword() {
        return gen_keepPassword;
    }

    public void setKeepPassword(boolean arg_keepPassword) {
        gen_keepPassword = arg_keepPassword;
    }

    public boolean getFollowMouse() {
        return gen_followMouse;
    }

    public void setFollowMouse(boolean arg_followMouse) {
        gen_followMouse = arg_followMouse;
    }

    public boolean getUseRepeater() {
        return gen_useRepeater;
    }

    public void setUseRepeater(boolean arg_useRepeater) {
        gen_useRepeater = arg_useRepeater;
    }

    public long getMetaListId() {
        return gen_metaListId;
    }

    public void setMetaListId(long arg_metaListId) {
        gen_metaListId = arg_metaListId;
    }

    public long getLastMetaKeyId() {
        return gen_LAST_META_KEY_ID;
    }

    public void setLastMetaKeyId(long arg_LAST_META_KEY_ID) {
        gen_LAST_META_KEY_ID = arg_LAST_META_KEY_ID;
    }

    public boolean getFollowPan() {
        return gen_followPan;
    }

    public void setFollowPan(boolean arg_followPan) {
        gen_followPan = arg_followPan;
    }

    public java.lang.String getUserName() {
        return gen_userName;
    }

    public void setUserName(java.lang.String arg_userName) {
        gen_userName = arg_userName;
    }

    public java.lang.String getRdpDomain() {
        return gen_rdpDomain;
    }

    public void setRdpDomain(java.lang.String arg_rdpDomain) {
        gen_rdpDomain = arg_rdpDomain;
    }

    public java.lang.String getSecureConnectionType() {
        return gen_secureConnectionType;
    }

    public void setSecureConnectionType(java.lang.String arg_secureConnectionType) {
        gen_secureConnectionType = arg_secureConnectionType;
    }

    public boolean getShowZoomButtons() {
        return gen_showZoomButtons;
    }

    public void setShowZoomButtons(boolean arg_showZoomButtons) {
        gen_showZoomButtons = arg_showZoomButtons;
    }

    public java.lang.String getDoubleTapActionAsString() {
        return gen_DOUBLE_TAP_ACTION;
    }

    public void setDoubleTapActionAsString(java.lang.String arg_DOUBLE_TAP_ACTION) {
        gen_DOUBLE_TAP_ACTION = arg_DOUBLE_TAP_ACTION;
    }

    public int getRdpResType() {
        return gen_rdpResType;
    }

    public void setRdpResType(int arg_rdpResType) {
        gen_rdpResType = arg_rdpResType;
    }

    public int getRdpWidth() {
        return gen_rdpWidth;
    }

    public void setRdpWidth(int arg_rdpWidth) {
        gen_rdpWidth = arg_rdpWidth;
    }

    public int getRdpHeight() {
        return gen_rdpHeight;
    }

    public void setRdpHeight(int arg_rdpHeight) {
        gen_rdpHeight = arg_rdpHeight;
    }

    public int getRdpColor() {
        if (gen_rdpColor == 0)
            return Constants.DEFAULT_RDP_COLOR_MODE;
        return gen_rdpColor;
    }

    public void setRdpColor(int arg_rdpColor) {
        gen_rdpColor = arg_rdpColor;
    }

    public boolean getRemoteFx() {
        return gen_remoteFx;
    }

    public void setRemoteFx(boolean arg_remoteFx) {
        gen_remoteFx = arg_remoteFx;
    }

    public boolean getDesktopBackground() {
        return gen_desktopBackground;
    }

    public void setDesktopBackground(boolean arg_desktopBackground) {
        gen_desktopBackground = arg_desktopBackground;
    }

    public boolean getFontSmoothing() {
        return gen_fontSmoothing;
    }

    public void setFontSmoothing(boolean arg_fontSmoothing) {
        gen_fontSmoothing = arg_fontSmoothing;
    }

    public boolean getDesktopComposition() {
        return gen_desktopComposition;
    }

    public void setDesktopComposition(boolean arg_desktopComposition) {
        gen_desktopComposition = arg_desktopComposition;
    }

    public boolean getWindowContents() {
        return gen_windowContents;
    }

    public void setWindowContents(boolean arg_windowContents) {
        gen_windowContents = arg_windowContents;
    }

    public boolean getMenuAnimation() {
        return gen_menuAnimation;
    }

    public void setMenuAnimation(boolean arg_menuAnimation) {
        gen_menuAnimation = arg_menuAnimation;
    }

    public boolean getVisualStyles() {
        return gen_visualStyles;
    }

    public void setVisualStyles(boolean arg_visualStyles) {
        gen_visualStyles = arg_visualStyles;
    }

    public boolean getRedirectSdCard() {
        return gen_redirectSdCard;
    }

    public void setRedirectSdCard(boolean arg_redirectSdCard) {
        gen_redirectSdCard = arg_redirectSdCard;
    }

    public boolean getConsoleMode() {
        return gen_consoleMode;
    }

    public void setConsoleMode(boolean arg_consoleMode) {
        gen_consoleMode = arg_consoleMode;
    }

    public boolean getEnableSound() {
        return gen_enableSound;
    }

    public void setEnableSound(boolean arg_enableSound) {
        gen_enableSound = arg_enableSound;
    }

    public boolean getEnableRecording() {
        return gen_enableRecording;
    }

    public void setEnableRecording(boolean arg_enableRecording) {
        gen_enableRecording = arg_enableRecording;
    }

    public int getRemoteSoundType() {
        return gen_remoteSoundType;
    }

    public void setRemoteSoundType(int arg_remoteSoundType) {
        gen_remoteSoundType = arg_remoteSoundType;
    }

    public boolean getViewOnly() {
        return gen_viewOnly;
    }

    public void setViewOnly(boolean arg_viewOnly) {
        gen_viewOnly = arg_viewOnly;
    }

    public java.lang.String getLayoutMap() {
        return gen_layoutMap;
    }

    public void setLayoutMap(java.lang.String arg_layoutMap) {
        gen_layoutMap = arg_layoutMap;
    }

    public java.lang.String getFilename() {
        return gen_filename;
    }

    public void setFilename(java.lang.String arg_filename) {
        gen_filename = arg_filename;
    }

    public java.lang.String getX509KeySignature() {
        return gen_x509KeySignature;
    }

    public void setX509KeySignature(java.lang.String arg_x509KeySignature) {
        gen_x509KeySignature = arg_x509KeySignature;
    }

    public java.lang.String getScreenshotFilename() {
        return gen_screenshotFilename;
    }

    public void setScreenshotFilename(java.lang.String arg_screenshotFilename) {
        gen_screenshotFilename = arg_screenshotFilename;
    }

    public boolean getEnableGfx() {
        return gen_enableGfx;
    }

    public void setEnableGfx(boolean arg_enableGfx) {
        gen_enableGfx = arg_enableGfx;
    }

    public boolean getEnableGfxH264() {
        return gen_enableGfxH264;
    }

    public void setEnableGfxH264(boolean arg_enableGfxH264) {
        gen_enableGfxH264 = arg_enableGfxH264;
    }

    public boolean getPreferSendingUnicode() {
        return gen_preferSendingUnicode;
    }

    public void setPreferSendingUnicode(boolean arg_preferSendingUnicode) {
        gen_preferSendingUnicode = arg_preferSendingUnicode;
    }

    public String getExternalId() {
        return gen_externalId;
    }

    public void setExternalId(String gen_externalId) {
        this.gen_externalId = gen_externalId;
    }

    public boolean getRequiresVpn() {
        return gen_requiresVpn;
    }

    public void setRequiresVpn(boolean gen_requiresVpn) {
        this.gen_requiresVpn = gen_requiresVpn;
    }

    public String getVpnUriScheme() {
        return gen_vpnUriScheme;
    }

    public void setVpnUriScheme(String gen_vpnUriScheme) {
        this.gen_vpnUriScheme = gen_vpnUriScheme;
    }

    public boolean getRdpGatewayEnabled() {
        return gen_rdpGatewayEnabled;
    }

    public void setRdpGatewayEnabled(boolean gen_rdpGatewayEnabled) {
        this.gen_rdpGatewayEnabled = gen_rdpGatewayEnabled;
    }

    public String getRdpGatewayHostname() {
        return gen_rdpGatewayHostname;
    }

    public void setRdpGatewayHostname(String rdpGatewayHostname) {
        this.gen_rdpGatewayHostname = rdpGatewayHostname;
    }

    public int getRdpGatewayPort() {
        return gen_rdpGatewayPort;
    }

    public void setRdpGatewayPort(int rdpGatewayPort) {
        this.gen_rdpGatewayPort = rdpGatewayPort;
    }

    public String getRdpGatewayUsername() {
        return gen_rdpGatewayUsername;
    }

    public void setRdpGatewayUsername(String rdpGatewayUsername) {
        this.gen_rdpGatewayUsername = rdpGatewayUsername;
    }

    public String getRdpGatewayDomain() {
        return gen_rdpGatewayDomain;
    }

    public void setRdpGatewayDomain(String rdpGatewayDomain) {
        this.gen_rdpGatewayDomain = rdpGatewayDomain;
    }

    public String getRdpGatewayPassword() {
        return gen_rdpGatewayPassword;
    }

    public void setRdpGatewayPassword(String rdpGatewayPassword) {
        this.gen_rdpGatewayPassword = rdpGatewayPassword;
    }

    public boolean getKeepRdpGatewayPassword() {
        return gen_keepRdpGatewayPassword;
    }

    public void setKeepRdpGatewayPassword(boolean keepRdpGatewayPassword) {
        this.gen_keepRdpGatewayPassword = keepRdpGatewayPassword;
    }

    public android.content.ContentValues Gen_getValues() {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(GEN_FIELD__ID, Long.toString(this.gen__Id));
        values.put(GEN_FIELD_NICKNAME, this.gen_nickname);
        values.put(GEN_FIELD_CONNECTIONTYPE, Integer.toString(this.gen_connectionType));
        values.put(GEN_FIELD_SSHSERVER, this.gen_sshServer);
        values.put(GEN_FIELD_SSHPORT, Integer.toString(this.gen_sshPort));
        values.put(GEN_FIELD_SSHUSER, this.gen_sshUser);
        values.put(GEN_FIELD_SSHPASSWORD, this.gen_sshPassword);
        values.put(GEN_FIELD_KEEPSSHPASSWORD, (this.gen_keepSshPassword ? "1" : "0"));
        values.put(GEN_FIELD_SSHPUBKEY, this.gen_sshPubKey);
        values.put(GEN_FIELD_SSHPRIVKEY, this.gen_sshPrivKey);
        values.put(GEN_FIELD_SSHPASSPHRASE, this.gen_sshPassPhrase);
        values.put(GEN_FIELD_USESSHPUBKEY, (this.gen_useSshPubKey ? "1" : "0"));
        values.put(GEN_FIELD_SSHREMOTECOMMANDOS, Integer.toString(this.gen_sshRemoteCommandOS));
        values.put(GEN_FIELD_SSHREMOTECOMMANDTYPE, Integer.toString(this.gen_sshRemoteCommandType));
        values.put(GEN_FIELD_AUTOXTYPE, Integer.toString(this.gen_autoXType));
        values.put(GEN_FIELD_AUTOXCOMMAND, this.gen_autoXCommand);
        values.put(GEN_FIELD_AUTOXENABLED, (this.gen_autoXEnabled ? "1" : "0"));
        values.put(GEN_FIELD_AUTOXRESTYPE, Integer.toString(this.gen_autoXResType));
        values.put(GEN_FIELD_AUTOXWIDTH, Integer.toString(this.gen_autoXWidth));
        values.put(GEN_FIELD_AUTOXHEIGHT, Integer.toString(this.gen_autoXHeight));
        values.put(GEN_FIELD_AUTOXSESSIONPROG, this.gen_autoXSessionProg);
        values.put(GEN_FIELD_AUTOXSESSIONTYPE, Integer.toString(this.gen_autoXSessionType));
        values.put(GEN_FIELD_AUTOXUNIXPW, (this.gen_autoXUnixpw ? "1" : "0"));
        values.put(GEN_FIELD_AUTOXUNIXAUTH, (this.gen_autoXUnixAuth ? "1" : "0"));
        values.put(GEN_FIELD_AUTOXRANDFILENM, this.gen_autoXRandFileNm);
        values.put(GEN_FIELD_SSHREMOTECOMMAND, this.gen_sshRemoteCommand);
        values.put(GEN_FIELD_SSHREMOTECOMMANDTIMEOUT, Integer.toString(this.gen_sshRemoteCommandTimeout));
        values.put(GEN_FIELD_USESSHREMOTECOMMAND, (this.gen_useSshRemoteCommand ? "1" : "0"));
        values.put(GEN_FIELD_SSHHOSTKEY, this.gen_sshHostKey);
        values.put(GEN_FIELD_ADDRESS, this.gen_address);
        values.put(GEN_FIELD_PORT, Integer.toString(this.gen_port));
        values.put(GEN_FIELD_CACERT, this.gen_caCert);
        values.put(GEN_FIELD_CACERTPATH, this.gen_caCertPath);
        values.put(GEN_FIELD_TLSPORT, Integer.toString(this.gen_tlsPort));
        values.put(GEN_FIELD_CERTSUBJECT, this.gen_certSubject);
        values.put(GEN_FIELD_PASSWORD, this.gen_password);
        values.put(GEN_FIELD_COLORMODEL, this.gen_colorModel);
        values.put(GEN_FIELD_PREFENCODING, Integer.toString(this.gen_prefEncoding));
        values.put(GEN_FIELD_EXTRAKEYSTOGGLETYPE, Integer.toString(this.gen_extraKeysToggleType));
        values.put(GEN_FIELD_FORCEFULL, Long.toString(this.gen_forceFull));
        values.put(GEN_FIELD_REPEATERID, this.gen_repeaterId);
        values.put(GEN_FIELD_INPUTMODE, this.gen_inputMode);
        values.put(GEN_FIELD_SCALEMODE, this.gen_SCALEMODE);
        values.put(GEN_FIELD_USEDPADASARROWS, (this.gen_useDpadAsArrows ? "1" : "0"));
        values.put(GEN_FIELD_ROTATEDPAD, (this.gen_rotateDpad ? "1" : "0"));
        values.put(GEN_FIELD_USEPORTRAIT, (this.gen_usePortrait ? "1" : "0"));
        values.put(GEN_FIELD_USELOCALCURSOR, (Integer.toString(this.gen_useLocalCursor)));
        values.put(GEN_FIELD_KEEPPASSWORD, (this.gen_keepPassword ? "1" : "0"));
        values.put(GEN_FIELD_FOLLOWMOUSE, (this.gen_followMouse ? "1" : "0"));
        values.put(GEN_FIELD_USEREPEATER, (this.gen_useRepeater ? "1" : "0"));
        values.put(GEN_FIELD_METALISTID, Long.toString(this.gen_metaListId));
        values.put(GEN_FIELD_LAST_META_KEY_ID, Long.toString(this.gen_LAST_META_KEY_ID));
        values.put(GEN_FIELD_FOLLOWPAN, (this.gen_followPan ? "1" : "0"));
        values.put(GEN_FIELD_USERNAME, this.gen_userName);
        values.put(GEN_FIELD_RDPDOMAIN, this.gen_rdpDomain);
        values.put(GEN_FIELD_SECURECONNECTIONTYPE, this.gen_secureConnectionType);
        values.put(GEN_FIELD_SHOWZOOMBUTTONS, (this.gen_showZoomButtons ? "1" : "0"));
        values.put(GEN_FIELD_DOUBLE_TAP_ACTION, this.gen_DOUBLE_TAP_ACTION);
        values.put(GEN_FIELD_RDPRESTYPE, Integer.toString(this.gen_rdpResType));
        values.put(GEN_FIELD_RDPWIDTH, Integer.toString(this.gen_rdpWidth));
        values.put(GEN_FIELD_RDPHEIGHT, Integer.toString(this.gen_rdpHeight));
        values.put(GEN_FIELD_RDPCOLOR, Integer.toString(this.gen_rdpColor));
        values.put(GEN_FIELD_REMOTEFX, (this.gen_remoteFx ? "1" : "0"));
        values.put(GEN_FIELD_DESKTOPBACKGROUND, (this.gen_desktopBackground ? "1" : "0"));
        values.put(GEN_FIELD_FONTSMOOTHING, (this.gen_fontSmoothing ? "1" : "0"));
        values.put(GEN_FIELD_DESKTOPCOMPOSITION, (this.gen_desktopComposition ? "1" : "0"));
        values.put(GEN_FIELD_WINDOWCONTENTS, (this.gen_windowContents ? "1" : "0"));
        values.put(GEN_FIELD_MENUANIMATION, (this.gen_menuAnimation ? "1" : "0"));
        values.put(GEN_FIELD_VISUALSTYLES, (this.gen_visualStyles ? "1" : "0"));
        values.put(GEN_FIELD_REDIRECTSDCARD, (this.gen_redirectSdCard ? "1" : "0"));
        values.put(GEN_FIELD_CONSOLEMODE, (this.gen_consoleMode ? "1" : "0"));
        values.put(GEN_FIELD_ENABLESOUND, (this.gen_enableSound ? "1" : "0"));
        values.put(GEN_FIELD_ENABLERECORDING, (this.gen_enableRecording ? "1" : "0"));
        values.put(GEN_FIELD_REMOTESOUNDTYPE, Integer.toString(this.gen_remoteSoundType));
        values.put(GEN_FIELD_VIEWONLY, (this.gen_viewOnly ? "1" : "0"));
        values.put(GEN_FIELD_LAYOUTMAP, this.gen_layoutMap);

        values.put(GEN_FIELD_FILENAME, this.gen_filename);
        values.put(GEN_FIELD_X509KEYSIGNATURE, this.gen_x509KeySignature);
        values.put(GEN_FIELD_SCREENSHOTFILENAME, this.gen_screenshotFilename);

        values.put(GEN_FIELD_ENABLEGFX, (this.gen_enableGfx ? "1" : "0"));
        values.put(GEN_FIELD_ENABLEGFXH264, (this.gen_enableGfxH264 ? "1" : "0"));

        values.put(GEN_FIELD_PREFERSENDINGUNICODE, (this.gen_preferSendingUnicode ? "1" : "0"));

        values.put(GEN_FIELD_EXTERNALID, (this.gen_externalId));
        values.put(GEN_FIELD_REQUIRESVPN, (this.gen_requiresVpn ? "1" : "0"));
        values.put(GEN_FIELD_VPNURISCHEME, (this.gen_vpnUriScheme));

        values.put(GEN_FIELD_RDPGATEWAYENABLED, (this.gen_rdpGatewayEnabled ? "1" : "0"));
        values.put(GEN_FIELD_RDPGATEWAYHOSTNAME, (this.gen_rdpGatewayHostname));
        values.put(GEN_FIELD_RDPGATEWAYPORT, (this.gen_rdpGatewayPort));
        values.put(GEN_FIELD_RDPGATEWAYUSERNAME, (this.gen_rdpGatewayUsername));
        values.put(GEN_FIELD_RDPGATEWAYDOMAIN, (this.gen_rdpGatewayDomain));
        values.put(GEN_FIELD_RDPGATEWAYPASSWORD, (this.gen_rdpGatewayPassword));
        values.put(GEN_FIELD_KEEPRDPGATEWAYPASSWORD, (this.gen_keepRdpGatewayPassword));

        return values;
    }

    /**
     * Return an array that gives the column index in the cursor for each field defined
     *
     * @param cursor Database cursor over some columns, possibly including this table
     * @return array of column indices; -1 if the column with that id is not in cursor
     */
    public int[] Gen_columnIndices(android.database.Cursor cursor) {
        int[] result = new int[GEN_COUNT];
        result[0] = cursor.getColumnIndex(GEN_FIELD__ID);
        // Make compatible with database generated by older version of plugin with uppercase column name
        if (result[0] == -1) {
            result[0] = cursor.getColumnIndex("_ID");
        }
        result[1] = cursor.getColumnIndex(GEN_FIELD_NICKNAME);
        result[2] = cursor.getColumnIndex(GEN_FIELD_CONNECTIONTYPE);
        result[3] = cursor.getColumnIndex(GEN_FIELD_SSHSERVER);
        result[4] = cursor.getColumnIndex(GEN_FIELD_SSHPORT);
        result[5] = cursor.getColumnIndex(GEN_FIELD_SSHUSER);
        result[6] = cursor.getColumnIndex(GEN_FIELD_SSHPASSWORD);
        result[7] = cursor.getColumnIndex(GEN_FIELD_KEEPSSHPASSWORD);
        result[8] = cursor.getColumnIndex(GEN_FIELD_SSHPUBKEY);
        result[9] = cursor.getColumnIndex(GEN_FIELD_SSHPRIVKEY);
        result[10] = cursor.getColumnIndex(GEN_FIELD_SSHPASSPHRASE);
        result[11] = cursor.getColumnIndex(GEN_FIELD_USESSHPUBKEY);
        result[12] = cursor.getColumnIndex(GEN_FIELD_SSHREMOTECOMMANDOS);
        result[13] = cursor.getColumnIndex(GEN_FIELD_SSHREMOTECOMMANDTYPE);
        result[14] = cursor.getColumnIndex(GEN_FIELD_AUTOXTYPE);
        result[15] = cursor.getColumnIndex(GEN_FIELD_AUTOXCOMMAND);
        result[16] = cursor.getColumnIndex(GEN_FIELD_AUTOXENABLED);
        result[17] = cursor.getColumnIndex(GEN_FIELD_AUTOXRESTYPE);
        result[18] = cursor.getColumnIndex(GEN_FIELD_AUTOXWIDTH);
        result[19] = cursor.getColumnIndex(GEN_FIELD_AUTOXHEIGHT);
        result[20] = cursor.getColumnIndex(GEN_FIELD_AUTOXSESSIONPROG);
        result[21] = cursor.getColumnIndex(GEN_FIELD_AUTOXSESSIONTYPE);
        result[22] = cursor.getColumnIndex(GEN_FIELD_AUTOXUNIXPW);
        result[23] = cursor.getColumnIndex(GEN_FIELD_AUTOXUNIXAUTH);
        result[24] = cursor.getColumnIndex(GEN_FIELD_AUTOXRANDFILENM);
        result[25] = cursor.getColumnIndex(GEN_FIELD_SSHREMOTECOMMAND);
        result[26] = cursor.getColumnIndex(GEN_FIELD_SSHREMOTECOMMANDTIMEOUT);
        result[27] = cursor.getColumnIndex(GEN_FIELD_USESSHREMOTECOMMAND);
        result[28] = cursor.getColumnIndex(GEN_FIELD_SSHHOSTKEY);
        result[29] = cursor.getColumnIndex(GEN_FIELD_ADDRESS);
        result[30] = cursor.getColumnIndex(GEN_FIELD_PORT);
        result[31] = cursor.getColumnIndex(GEN_FIELD_CACERT);
        result[32] = cursor.getColumnIndex(GEN_FIELD_CACERTPATH);
        result[33] = cursor.getColumnIndex(GEN_FIELD_TLSPORT);
        result[34] = cursor.getColumnIndex(GEN_FIELD_CERTSUBJECT);
        result[35] = cursor.getColumnIndex(GEN_FIELD_PASSWORD);
        result[36] = cursor.getColumnIndex(GEN_FIELD_COLORMODEL);
        result[37] = cursor.getColumnIndex(GEN_FIELD_PREFENCODING);
        result[38] = cursor.getColumnIndex(GEN_FIELD_EXTRAKEYSTOGGLETYPE);
        result[39] = cursor.getColumnIndex(GEN_FIELD_FORCEFULL);
        result[40] = cursor.getColumnIndex(GEN_FIELD_REPEATERID);
        result[41] = cursor.getColumnIndex(GEN_FIELD_INPUTMODE);
        result[42] = cursor.getColumnIndex(GEN_FIELD_SCALEMODE);
        result[43] = cursor.getColumnIndex(GEN_FIELD_USEDPADASARROWS);
        result[44] = cursor.getColumnIndex(GEN_FIELD_ROTATEDPAD);
        result[45] = cursor.getColumnIndex(GEN_FIELD_USEPORTRAIT);
        result[46] = cursor.getColumnIndex(GEN_FIELD_USELOCALCURSOR);
        result[47] = cursor.getColumnIndex(GEN_FIELD_KEEPPASSWORD);
        result[48] = cursor.getColumnIndex(GEN_FIELD_FOLLOWMOUSE);
        result[49] = cursor.getColumnIndex(GEN_FIELD_USEREPEATER);
        result[50] = cursor.getColumnIndex(GEN_FIELD_METALISTID);
        result[51] = cursor.getColumnIndex(GEN_FIELD_LAST_META_KEY_ID);
        result[52] = cursor.getColumnIndex(GEN_FIELD_FOLLOWPAN);
        result[53] = cursor.getColumnIndex(GEN_FIELD_USERNAME);
        result[54] = cursor.getColumnIndex(GEN_FIELD_RDPDOMAIN);
        result[55] = cursor.getColumnIndex(GEN_FIELD_SECURECONNECTIONTYPE);
        result[56] = cursor.getColumnIndex(GEN_FIELD_SHOWZOOMBUTTONS);
        result[57] = cursor.getColumnIndex(GEN_FIELD_DOUBLE_TAP_ACTION);
        result[58] = cursor.getColumnIndex(GEN_FIELD_RDPRESTYPE);
        result[59] = cursor.getColumnIndex(GEN_FIELD_RDPWIDTH);
        result[60] = cursor.getColumnIndex(GEN_FIELD_RDPHEIGHT);
        result[61] = cursor.getColumnIndex(GEN_FIELD_RDPCOLOR);
        result[62] = cursor.getColumnIndex(GEN_FIELD_REMOTEFX);
        result[63] = cursor.getColumnIndex(GEN_FIELD_DESKTOPBACKGROUND);
        result[64] = cursor.getColumnIndex(GEN_FIELD_FONTSMOOTHING);
        result[65] = cursor.getColumnIndex(GEN_FIELD_DESKTOPCOMPOSITION);
        result[66] = cursor.getColumnIndex(GEN_FIELD_WINDOWCONTENTS);
        result[67] = cursor.getColumnIndex(GEN_FIELD_MENUANIMATION);
        result[68] = cursor.getColumnIndex(GEN_FIELD_VISUALSTYLES);
        result[69] = cursor.getColumnIndex(GEN_FIELD_REDIRECTSDCARD);
        result[70] = cursor.getColumnIndex(GEN_FIELD_CONSOLEMODE);
        result[71] = cursor.getColumnIndex(GEN_FIELD_ENABLESOUND);
        result[72] = cursor.getColumnIndex(GEN_FIELD_ENABLERECORDING);
        result[73] = cursor.getColumnIndex(GEN_FIELD_REMOTESOUNDTYPE);
        result[74] = cursor.getColumnIndex(GEN_FIELD_VIEWONLY);
        result[75] = cursor.getColumnIndex(GEN_FIELD_LAYOUTMAP);

        result[76] = cursor.getColumnIndex(GEN_FIELD_FILENAME);
        result[77] = cursor.getColumnIndex(GEN_FIELD_X509KEYSIGNATURE);
        result[78] = cursor.getColumnIndex(GEN_FIELD_SCREENSHOTFILENAME);

        result[79] = cursor.getColumnIndex(GEN_FIELD_ENABLEGFX);
        result[80] = cursor.getColumnIndex(GEN_FIELD_ENABLEGFXH264);

        result[81] = cursor.getColumnIndex(GEN_FIELD_PREFERSENDINGUNICODE);

        result[82] = cursor.getColumnIndex(GEN_FIELD_EXTERNALID);
        result[83] = cursor.getColumnIndex(GEN_FIELD_REQUIRESVPN);
        result[84] = cursor.getColumnIndex(GEN_FIELD_VPNURISCHEME);

        result[85] = cursor.getColumnIndex(GEN_FIELD_RDPGATEWAYENABLED);
        result[86] = cursor.getColumnIndex(GEN_FIELD_RDPGATEWAYHOSTNAME);
        result[87] = cursor.getColumnIndex(GEN_FIELD_RDPGATEWAYPORT);
        result[88] = cursor.getColumnIndex(GEN_FIELD_RDPGATEWAYUSERNAME);
        result[89] = cursor.getColumnIndex(GEN_FIELD_RDPGATEWAYDOMAIN);
        result[90] = cursor.getColumnIndex(GEN_FIELD_RDPGATEWAYPASSWORD);
        result[91] = cursor.getColumnIndex(GEN_FIELD_KEEPRDPGATEWAYPASSWORD);

        return result;
    }

    /**
     * Populate one instance from a cursor
     */
    public void Gen_populate(android.database.Cursor cursor, int[] columnIndices) {
        if (columnIndices[GEN_ID__ID] >= 0 && !cursor.isNull(columnIndices[GEN_ID__ID])) {
            gen__Id = cursor.getLong(columnIndices[GEN_ID__ID]);
        }
        if (columnIndices[GEN_ID_NICKNAME] >= 0 && !cursor.isNull(columnIndices[GEN_ID_NICKNAME])) {
            gen_nickname = cursor.getString(columnIndices[GEN_ID_NICKNAME]);
        }
        if (columnIndices[GEN_ID_CONNECTIONTYPE] >= 0 && !cursor.isNull(columnIndices[GEN_ID_CONNECTIONTYPE])) {
            gen_connectionType = cursor.getInt(columnIndices[GEN_ID_CONNECTIONTYPE]);
        }
        if (columnIndices[GEN_ID_SSHSERVER] >= 0 && !cursor.isNull(columnIndices[GEN_ID_SSHSERVER])) {
            gen_sshServer = cursor.getString(columnIndices[GEN_ID_SSHSERVER]);
        }
        if (columnIndices[GEN_ID_SSHPORT] >= 0 && !cursor.isNull(columnIndices[GEN_ID_SSHPORT])) {
            gen_sshPort = cursor.getInt(columnIndices[GEN_ID_SSHPORT]);
        }
        if (columnIndices[GEN_ID_SSHUSER] >= 0 && !cursor.isNull(columnIndices[GEN_ID_SSHUSER])) {
            gen_sshUser = cursor.getString(columnIndices[GEN_ID_SSHUSER]);
        }
        if (columnIndices[GEN_ID_SSHPASSWORD] >= 0 && !cursor.isNull(columnIndices[GEN_ID_SSHPASSWORD])) {
            gen_sshPassword = cursor.getString(columnIndices[GEN_ID_SSHPASSWORD]);
        }
        if (columnIndices[GEN_ID_KEEPSSHPASSWORD] >= 0 && !cursor.isNull(columnIndices[GEN_ID_KEEPSSHPASSWORD])) {
            gen_keepSshPassword = (cursor.getInt(columnIndices[GEN_ID_KEEPSSHPASSWORD]) != 0);
        }
        if (columnIndices[GEN_ID_SSHPUBKEY] >= 0 && !cursor.isNull(columnIndices[GEN_ID_SSHPUBKEY])) {
            gen_sshPubKey = cursor.getString(columnIndices[GEN_ID_SSHPUBKEY]);
        }
        if (columnIndices[GEN_ID_SSHPRIVKEY] >= 0 && !cursor.isNull(columnIndices[GEN_ID_SSHPRIVKEY])) {
            gen_sshPrivKey = cursor.getString(columnIndices[GEN_ID_SSHPRIVKEY]);
        }
        if (columnIndices[GEN_ID_SSHPASSPHRASE] >= 0 && !cursor.isNull(columnIndices[GEN_ID_SSHPASSPHRASE])) {
            gen_sshPassPhrase = cursor.getString(columnIndices[GEN_ID_SSHPASSPHRASE]);
        }
        if (columnIndices[GEN_ID_USESSHPUBKEY] >= 0 && !cursor.isNull(columnIndices[GEN_ID_USESSHPUBKEY])) {
            gen_useSshPubKey = (cursor.getInt(columnIndices[GEN_ID_USESSHPUBKEY]) != 0);
        }
        if (columnIndices[GEN_ID_SSHREMOTECOMMANDOS] >= 0 && !cursor.isNull(columnIndices[GEN_ID_SSHREMOTECOMMANDOS])) {
            gen_sshRemoteCommandOS = cursor.getInt(columnIndices[GEN_ID_SSHREMOTECOMMANDOS]);
        }
        if (columnIndices[GEN_ID_SSHREMOTECOMMANDTYPE] >= 0 && !cursor.isNull(columnIndices[GEN_ID_SSHREMOTECOMMANDTYPE])) {
            gen_sshRemoteCommandType = cursor.getInt(columnIndices[GEN_ID_SSHREMOTECOMMANDTYPE]);
        }
        if (columnIndices[GEN_ID_AUTOXTYPE] >= 0 && !cursor.isNull(columnIndices[GEN_ID_AUTOXTYPE])) {
            gen_autoXType = cursor.getInt(columnIndices[GEN_ID_AUTOXTYPE]);
        }
        if (columnIndices[GEN_ID_AUTOXCOMMAND] >= 0 && !cursor.isNull(columnIndices[GEN_ID_AUTOXCOMMAND])) {
            gen_autoXCommand = cursor.getString(columnIndices[GEN_ID_AUTOXCOMMAND]);
        }
        if (columnIndices[GEN_ID_AUTOXENABLED] >= 0 && !cursor.isNull(columnIndices[GEN_ID_AUTOXENABLED])) {
            gen_autoXEnabled = (cursor.getInt(columnIndices[GEN_ID_AUTOXENABLED]) != 0);
        }
        if (columnIndices[GEN_ID_AUTOXRESTYPE] >= 0 && !cursor.isNull(columnIndices[GEN_ID_AUTOXRESTYPE])) {
            gen_autoXResType = cursor.getInt(columnIndices[GEN_ID_AUTOXRESTYPE]);
        }
        if (columnIndices[GEN_ID_AUTOXWIDTH] >= 0 && !cursor.isNull(columnIndices[GEN_ID_AUTOXWIDTH])) {
            gen_autoXWidth = cursor.getInt(columnIndices[GEN_ID_AUTOXWIDTH]);
        }
        if (columnIndices[GEN_ID_AUTOXHEIGHT] >= 0 && !cursor.isNull(columnIndices[GEN_ID_AUTOXHEIGHT])) {
            gen_autoXHeight = cursor.getInt(columnIndices[GEN_ID_AUTOXHEIGHT]);
        }
        if (columnIndices[GEN_ID_AUTOXSESSIONPROG] >= 0 && !cursor.isNull(columnIndices[GEN_ID_AUTOXSESSIONPROG])) {
            gen_autoXSessionProg = cursor.getString(columnIndices[GEN_ID_AUTOXSESSIONPROG]);
        }
        if (columnIndices[GEN_ID_AUTOXSESSIONTYPE] >= 0 && !cursor.isNull(columnIndices[GEN_ID_AUTOXSESSIONTYPE])) {
            gen_autoXSessionType = cursor.getInt(columnIndices[GEN_ID_AUTOXSESSIONTYPE]);
        }
        if (columnIndices[GEN_ID_AUTOXUNIXPW] >= 0 && !cursor.isNull(columnIndices[GEN_ID_AUTOXUNIXPW])) {
            gen_autoXUnixpw = (cursor.getInt(columnIndices[GEN_ID_AUTOXUNIXPW]) != 0);
        }
        if (columnIndices[GEN_ID_AUTOXUNIXAUTH] >= 0 && !cursor.isNull(columnIndices[GEN_ID_AUTOXUNIXAUTH])) {
            gen_autoXUnixAuth = (cursor.getInt(columnIndices[GEN_ID_AUTOXUNIXAUTH]) != 0);
        }
        if (columnIndices[GEN_ID_AUTOXRANDFILENM] >= 0 && !cursor.isNull(columnIndices[GEN_ID_AUTOXRANDFILENM])) {
            gen_autoXRandFileNm = cursor.getString(columnIndices[GEN_ID_AUTOXRANDFILENM]);
        }
        if (columnIndices[GEN_ID_SSHREMOTECOMMAND] >= 0 && !cursor.isNull(columnIndices[GEN_ID_SSHREMOTECOMMAND])) {
            gen_sshRemoteCommand = cursor.getString(columnIndices[GEN_ID_SSHREMOTECOMMAND]);
        }
        if (columnIndices[GEN_ID_SSHREMOTECOMMANDTIMEOUT] >= 0 && !cursor.isNull(columnIndices[GEN_ID_SSHREMOTECOMMANDTIMEOUT])) {
            gen_sshRemoteCommandTimeout = cursor.getInt(columnIndices[GEN_ID_SSHREMOTECOMMANDTIMEOUT]);
        }
        if (columnIndices[GEN_ID_USESSHREMOTECOMMAND] >= 0 && !cursor.isNull(columnIndices[GEN_ID_USESSHREMOTECOMMAND])) {
            gen_useSshRemoteCommand = (cursor.getInt(columnIndices[GEN_ID_USESSHREMOTECOMMAND]) != 0);
        }
        if (columnIndices[GEN_ID_SSHHOSTKEY] >= 0 && !cursor.isNull(columnIndices[GEN_ID_SSHHOSTKEY])) {
            gen_sshHostKey = cursor.getString(columnIndices[GEN_ID_SSHHOSTKEY]);
        }
        if (columnIndices[GEN_ID_ADDRESS] >= 0 && !cursor.isNull(columnIndices[GEN_ID_ADDRESS])) {
            gen_address = cursor.getString(columnIndices[GEN_ID_ADDRESS]);
        }
        if (columnIndices[GEN_ID_PORT] >= 0 && !cursor.isNull(columnIndices[GEN_ID_PORT])) {
            gen_port = cursor.getInt(columnIndices[GEN_ID_PORT]);
        }
        if (columnIndices[GEN_ID_CACERT] >= 0 && !cursor.isNull(columnIndices[GEN_ID_CACERT])) {
            gen_caCert = cursor.getString(columnIndices[GEN_ID_CACERT]);
        }
        if (columnIndices[GEN_ID_CACERTPATH] >= 0 && !cursor.isNull(columnIndices[GEN_ID_CACERTPATH])) {
            gen_caCertPath = cursor.getString(columnIndices[GEN_ID_CACERTPATH]);
        }
        if (columnIndices[GEN_ID_TLSPORT] >= 0 && !cursor.isNull(columnIndices[GEN_ID_TLSPORT])) {
            gen_tlsPort = cursor.getInt(columnIndices[GEN_ID_TLSPORT]);
        }
        if (columnIndices[GEN_ID_CERTSUBJECT] >= 0 && !cursor.isNull(columnIndices[GEN_ID_CERTSUBJECT])) {
            gen_certSubject = cursor.getString(columnIndices[GEN_ID_CERTSUBJECT]);
        }
        if (columnIndices[GEN_ID_PASSWORD] >= 0 && !cursor.isNull(columnIndices[GEN_ID_PASSWORD])) {
            gen_password = cursor.getString(columnIndices[GEN_ID_PASSWORD]);
        }
        if (columnIndices[GEN_ID_COLORMODEL] >= 0 && !cursor.isNull(columnIndices[GEN_ID_COLORMODEL])) {
            gen_colorModel = cursor.getString(columnIndices[GEN_ID_COLORMODEL]);
        }
        if (columnIndices[GEN_ID_PREFENCODING] >= 0 && !cursor.isNull(columnIndices[GEN_ID_PREFENCODING])) {
            gen_prefEncoding = cursor.getInt(columnIndices[GEN_ID_PREFENCODING]);
        }
        if (columnIndices[GEN_ID_EXTRAKEYSTOGGLETYPE] >= 0 && !cursor.isNull(columnIndices[GEN_ID_EXTRAKEYSTOGGLETYPE])) {
            gen_extraKeysToggleType = cursor.getInt(columnIndices[GEN_ID_EXTRAKEYSTOGGLETYPE]);
        }
        if (columnIndices[GEN_ID_FORCEFULL] >= 0 && !cursor.isNull(columnIndices[GEN_ID_FORCEFULL])) {
            gen_forceFull = cursor.getLong(columnIndices[GEN_ID_FORCEFULL]);
        }
        if (columnIndices[GEN_ID_REPEATERID] >= 0 && !cursor.isNull(columnIndices[GEN_ID_REPEATERID])) {
            gen_repeaterId = cursor.getString(columnIndices[GEN_ID_REPEATERID]);
        }
        if (columnIndices[GEN_ID_INPUTMODE] >= 0 && !cursor.isNull(columnIndices[GEN_ID_INPUTMODE])) {
            gen_inputMode = cursor.getString(columnIndices[GEN_ID_INPUTMODE]);
        }
        if (columnIndices[GEN_ID_SCALEMODE] >= 0 && !cursor.isNull(columnIndices[GEN_ID_SCALEMODE])) {
            gen_SCALEMODE = cursor.getString(columnIndices[GEN_ID_SCALEMODE]);
        }
        if (columnIndices[GEN_ID_USEDPADASARROWS] >= 0 && !cursor.isNull(columnIndices[GEN_ID_USEDPADASARROWS])) {
            gen_useDpadAsArrows = (cursor.getInt(columnIndices[GEN_ID_USEDPADASARROWS]) != 0);
        }
        if (columnIndices[GEN_ID_ROTATEDPAD] >= 0 && !cursor.isNull(columnIndices[GEN_ID_ROTATEDPAD])) {
            gen_rotateDpad = (cursor.getInt(columnIndices[GEN_ID_ROTATEDPAD]) != 0);
        }
        if (columnIndices[GEN_ID_USEPORTRAIT] >= 0 && !cursor.isNull(columnIndices[GEN_ID_USEPORTRAIT])) {
            gen_usePortrait = (cursor.getInt(columnIndices[GEN_ID_USEPORTRAIT]) != 0);
        }
        if (columnIndices[GEN_ID_USELOCALCURSOR] >= 0 && !cursor.isNull(columnIndices[GEN_ID_USELOCALCURSOR])) {
            gen_useLocalCursor = cursor.getInt(columnIndices[GEN_ID_USELOCALCURSOR]);
        }
        if (columnIndices[GEN_ID_KEEPPASSWORD] >= 0 && !cursor.isNull(columnIndices[GEN_ID_KEEPPASSWORD])) {
            gen_keepPassword = (cursor.getInt(columnIndices[GEN_ID_KEEPPASSWORD]) != 0);
        }
        if (columnIndices[GEN_ID_FOLLOWMOUSE] >= 0 && !cursor.isNull(columnIndices[GEN_ID_FOLLOWMOUSE])) {
            gen_followMouse = (cursor.getInt(columnIndices[GEN_ID_FOLLOWMOUSE]) != 0);
        }
        if (columnIndices[GEN_ID_USEREPEATER] >= 0 && !cursor.isNull(columnIndices[GEN_ID_USEREPEATER])) {
            gen_useRepeater = (cursor.getInt(columnIndices[GEN_ID_USEREPEATER]) != 0);
        }
        if (columnIndices[GEN_ID_METALISTID] >= 0 && !cursor.isNull(columnIndices[GEN_ID_METALISTID])) {
            gen_metaListId = cursor.getLong(columnIndices[GEN_ID_METALISTID]);
        }
        if (columnIndices[GEN_ID_LAST_META_KEY_ID] >= 0 && !cursor.isNull(columnIndices[GEN_ID_LAST_META_KEY_ID])) {
            gen_LAST_META_KEY_ID = cursor.getLong(columnIndices[GEN_ID_LAST_META_KEY_ID]);
        }
        if (columnIndices[GEN_ID_FOLLOWPAN] >= 0 && !cursor.isNull(columnIndices[GEN_ID_FOLLOWPAN])) {
            gen_followPan = (cursor.getInt(columnIndices[GEN_ID_FOLLOWPAN]) != 0);
        }
        if (columnIndices[GEN_ID_USERNAME] >= 0 && !cursor.isNull(columnIndices[GEN_ID_USERNAME])) {
            gen_userName = cursor.getString(columnIndices[GEN_ID_USERNAME]);
        }
        if (columnIndices[GEN_ID_RDPDOMAIN] >= 0 && !cursor.isNull(columnIndices[GEN_ID_RDPDOMAIN])) {
            gen_rdpDomain = cursor.getString(columnIndices[GEN_ID_RDPDOMAIN]);
        }
        if (columnIndices[GEN_ID_SECURECONNECTIONTYPE] >= 0 && !cursor.isNull(columnIndices[GEN_ID_SECURECONNECTIONTYPE])) {
            gen_secureConnectionType = cursor.getString(columnIndices[GEN_ID_SECURECONNECTIONTYPE]);
        }
        if (columnIndices[GEN_ID_SHOWZOOMBUTTONS] >= 0 && !cursor.isNull(columnIndices[GEN_ID_SHOWZOOMBUTTONS])) {
            gen_showZoomButtons = (cursor.getInt(columnIndices[GEN_ID_SHOWZOOMBUTTONS]) != 0);
        }
        if (columnIndices[GEN_ID_DOUBLE_TAP_ACTION] >= 0 && !cursor.isNull(columnIndices[GEN_ID_DOUBLE_TAP_ACTION])) {
            gen_DOUBLE_TAP_ACTION = cursor.getString(columnIndices[GEN_ID_DOUBLE_TAP_ACTION]);
        }
        if (columnIndices[GEN_ID_RDPRESTYPE] >= 0 && !cursor.isNull(columnIndices[GEN_ID_RDPRESTYPE])) {
            gen_rdpResType = cursor.getInt(columnIndices[GEN_ID_RDPRESTYPE]);
        }
        if (columnIndices[GEN_ID_RDPWIDTH] >= 0 && !cursor.isNull(columnIndices[GEN_ID_RDPWIDTH])) {
            gen_rdpWidth = cursor.getInt(columnIndices[GEN_ID_RDPWIDTH]);
        }
        if (columnIndices[GEN_ID_RDPHEIGHT] >= 0 && !cursor.isNull(columnIndices[GEN_ID_RDPHEIGHT])) {
            gen_rdpHeight = cursor.getInt(columnIndices[GEN_ID_RDPHEIGHT]);
        }
        if (columnIndices[GEN_ID_RDPCOLOR] >= 0 && !cursor.isNull(columnIndices[GEN_ID_RDPCOLOR])) {
            gen_rdpColor = cursor.getInt(columnIndices[GEN_ID_RDPCOLOR]);
        }
        if (columnIndices[GEN_ID_REMOTEFX] >= 0 && !cursor.isNull(columnIndices[GEN_ID_REMOTEFX])) {
            gen_remoteFx = (cursor.getInt(columnIndices[GEN_ID_REMOTEFX]) != 0);
        }
        if (columnIndices[GEN_ID_DESKTOPBACKGROUND] >= 0 && !cursor.isNull(columnIndices[GEN_ID_DESKTOPBACKGROUND])) {
            gen_desktopBackground = (cursor.getInt(columnIndices[GEN_ID_DESKTOPBACKGROUND]) != 0);
        }
        if (columnIndices[GEN_ID_FONTSMOOTHING] >= 0 && !cursor.isNull(columnIndices[GEN_ID_FONTSMOOTHING])) {
            gen_fontSmoothing = (cursor.getInt(columnIndices[GEN_ID_FONTSMOOTHING]) != 0);
        }
        if (columnIndices[GEN_ID_DESKTOPCOMPOSITION] >= 0 && !cursor.isNull(columnIndices[GEN_ID_DESKTOPCOMPOSITION])) {
            gen_desktopComposition = (cursor.getInt(columnIndices[GEN_ID_DESKTOPCOMPOSITION]) != 0);
        }
        if (columnIndices[GEN_ID_WINDOWCONTENTS] >= 0 && !cursor.isNull(columnIndices[GEN_ID_WINDOWCONTENTS])) {
            gen_windowContents = (cursor.getInt(columnIndices[GEN_ID_WINDOWCONTENTS]) != 0);
        }
        if (columnIndices[GEN_ID_MENUANIMATION] >= 0 && !cursor.isNull(columnIndices[GEN_ID_MENUANIMATION])) {
            gen_menuAnimation = (cursor.getInt(columnIndices[GEN_ID_MENUANIMATION]) != 0);
        }
        if (columnIndices[GEN_ID_VISUALSTYLES] >= 0 && !cursor.isNull(columnIndices[GEN_ID_VISUALSTYLES])) {
            gen_visualStyles = (cursor.getInt(columnIndices[GEN_ID_VISUALSTYLES]) != 0);
        }
        if (columnIndices[GEN_ID_REDIRECTSDCARD] >= 0 && !cursor.isNull(columnIndices[GEN_ID_REDIRECTSDCARD])) {
            gen_redirectSdCard = (cursor.getInt(columnIndices[GEN_ID_REDIRECTSDCARD]) != 0);
        }
        if (columnIndices[GEN_ID_CONSOLEMODE] >= 0 && !cursor.isNull(columnIndices[GEN_ID_CONSOLEMODE])) {
            gen_consoleMode = (cursor.getInt(columnIndices[GEN_ID_CONSOLEMODE]) != 0);
        }
        if (columnIndices[GEN_ID_ENABLESOUND] >= 0 && !cursor.isNull(columnIndices[GEN_ID_ENABLESOUND])) {
            gen_enableSound = (cursor.getInt(columnIndices[GEN_ID_ENABLESOUND]) != 0);
        }
        if (columnIndices[GEN_ID_ENABLERECORDING] >= 0 && !cursor.isNull(columnIndices[GEN_ID_ENABLERECORDING])) {
            gen_enableRecording = (cursor.getInt(columnIndices[GEN_ID_ENABLERECORDING]) != 0);
        }
        if (columnIndices[GEN_ID_REMOTESOUNDTYPE] >= 0 && !cursor.isNull(columnIndices[GEN_ID_REMOTESOUNDTYPE])) {
            gen_remoteSoundType = cursor.getInt(columnIndices[GEN_ID_REMOTESOUNDTYPE]);
        }
        if (columnIndices[GEN_ID_VIEWONLY] >= 0 && !cursor.isNull(columnIndices[GEN_ID_VIEWONLY])) {
            gen_viewOnly = (cursor.getInt(columnIndices[GEN_ID_VIEWONLY]) != 0);
        }
        if (columnIndices[GEN_ID_LAYOUTMAP] >= 0 && !cursor.isNull(columnIndices[GEN_ID_LAYOUTMAP])) {
            gen_layoutMap = cursor.getString(columnIndices[GEN_ID_LAYOUTMAP]);
        }

        if (columnIndices[GEN_ID_FILENAME] >= 0 && !cursor.isNull(columnIndices[GEN_ID_FILENAME])) {
            gen_filename = cursor.getString(columnIndices[GEN_ID_FILENAME]);
        }
        if (columnIndices[GEN_ID_X509KEYSIGNATURE] >= 0 && !cursor.isNull(columnIndices[GEN_ID_X509KEYSIGNATURE])) {
            gen_x509KeySignature = cursor.getString(columnIndices[GEN_ID_X509KEYSIGNATURE]);
        }
        if (columnIndices[GEN_ID_SCREENSHOTFILENAME] >= 0 && !cursor.isNull(columnIndices[GEN_ID_SCREENSHOTFILENAME])) {
            gen_screenshotFilename = cursor.getString(columnIndices[GEN_ID_SCREENSHOTFILENAME]);
        }

        if (columnIndices[GEN_ID_ENABLEGFX] >= 0 && !cursor.isNull(columnIndices[GEN_ID_ENABLEGFX])) {
            gen_enableGfx = (cursor.getInt(columnIndices[GEN_ID_ENABLEGFX]) != 0);
        }
        if (columnIndices[GEN_ID_ENABLEGFXH264] >= 0 && !cursor.isNull(columnIndices[GEN_ID_ENABLEGFXH264])) {
            gen_enableGfxH264 = (cursor.getInt(columnIndices[GEN_ID_ENABLEGFXH264]) != 0);
        }
        if (columnIndices[GEN_ID_PREFERSENDINGUNICODE] >= 0 && !cursor.isNull(columnIndices[GEN_ID_PREFERSENDINGUNICODE])) {
            gen_preferSendingUnicode = (cursor.getInt(columnIndices[GEN_ID_PREFERSENDINGUNICODE]) != 0);
        }

        if (columnIndices[GEN_ID_EXTERNALID] >= 0 && !cursor.isNull(columnIndices[GEN_ID_EXTERNALID])) {
            gen_externalId = cursor.getString(columnIndices[GEN_ID_EXTERNALID]);
        }
        if (columnIndices[GEN_ID_REQUIRESVPN] >= 0 && !cursor.isNull(columnIndices[GEN_ID_REQUIRESVPN])) {
            gen_requiresVpn = (cursor.getInt(columnIndices[GEN_ID_REQUIRESVPN]) != 0);
        }
        if (columnIndices[GEN_ID_VPNURISCHEME] >= 0 && !cursor.isNull(columnIndices[GEN_ID_VPNURISCHEME])) {
            gen_vpnUriScheme = cursor.getString(columnIndices[GEN_ID_VPNURISCHEME]);
        }

        if (columnIndices[GEN_ID_RDPGATEWAYENABLED] >= 0 && !cursor.isNull(columnIndices[GEN_ID_RDPGATEWAYENABLED])) {
            gen_rdpGatewayEnabled = cursor.getInt(columnIndices[GEN_ID_RDPGATEWAYENABLED]) != 0;
        }
        if (columnIndices[GEN_ID_RDPGATEWAYHOSTNAME] >= 0 && !cursor.isNull(columnIndices[GEN_ID_RDPGATEWAYHOSTNAME])) {
            gen_rdpGatewayHostname = cursor.getString(columnIndices[GEN_ID_RDPGATEWAYHOSTNAME]);
        }
        if (columnIndices[GEN_ID_RDPGATEWAYPORT] >= 0 && !cursor.isNull(columnIndices[GEN_ID_RDPGATEWAYPORT])) {
            gen_rdpGatewayPort = cursor.getInt(columnIndices[GEN_ID_RDPGATEWAYPORT]);
        }
        if (columnIndices[GEN_ID_RDPGATEWAYUSERNAME] >= 0 && !cursor.isNull(columnIndices[GEN_ID_RDPGATEWAYUSERNAME])) {
            gen_rdpGatewayUsername = cursor.getString(columnIndices[GEN_ID_RDPGATEWAYUSERNAME]);
        }
        if (columnIndices[GEN_ID_RDPGATEWAYDOMAIN] >= 0 && !cursor.isNull(columnIndices[GEN_ID_RDPGATEWAYDOMAIN])) {
            gen_rdpGatewayDomain = cursor.getString(columnIndices[GEN_ID_RDPGATEWAYDOMAIN]);
        }
        if (columnIndices[GEN_ID_RDPGATEWAYPASSWORD] >= 0 && !cursor.isNull(columnIndices[GEN_ID_RDPGATEWAYPASSWORD])) {
            gen_rdpGatewayPassword = cursor.getString(columnIndices[GEN_ID_RDPGATEWAYPASSWORD]);
        }
        if (columnIndices[GEN_ID_KEEPRDPGATEWAYPASSWORD] >= 0 && !cursor.isNull(columnIndices[GEN_ID_KEEPRDPGATEWAYPASSWORD])) {
            gen_keepRdpGatewayPassword = (cursor.getInt(columnIndices[GEN_ID_KEEPRDPGATEWAYPASSWORD]) != 0);
        }
    }

    /**
     * Populate one instance from a ContentValues
     */
    public void Gen_populate(android.content.ContentValues values) {
        gen__Id = values.getAsLong(GEN_FIELD__ID);
        gen_nickname = values.getAsString(GEN_FIELD_NICKNAME);
        gen_connectionType = values.getAsInteger(GEN_FIELD_CONNECTIONTYPE);
        gen_sshServer = values.getAsString(GEN_FIELD_SSHSERVER);
        gen_sshPort = values.getAsInteger(GEN_FIELD_SSHPORT);
        gen_sshUser = values.getAsString(GEN_FIELD_SSHUSER);
        gen_sshPassword = values.getAsString(GEN_FIELD_SSHPASSWORD);
        gen_keepSshPassword = (values.getAsInteger(GEN_FIELD_KEEPSSHPASSWORD) != 0);
        gen_sshPubKey = values.getAsString(GEN_FIELD_SSHPUBKEY);
        gen_sshPrivKey = values.getAsString(GEN_FIELD_SSHPRIVKEY);
        gen_sshPassPhrase = values.getAsString(GEN_FIELD_SSHPASSPHRASE);
        gen_useSshPubKey = (values.getAsInteger(GEN_FIELD_USESSHPUBKEY) != 0);
        gen_sshRemoteCommandOS = values.getAsInteger(GEN_FIELD_SSHREMOTECOMMANDOS);
        gen_sshRemoteCommandType = values.getAsInteger(GEN_FIELD_SSHREMOTECOMMANDTYPE);
        gen_autoXType = values.getAsInteger(GEN_FIELD_AUTOXTYPE);
        gen_autoXCommand = values.getAsString(GEN_FIELD_AUTOXCOMMAND);
        gen_autoXEnabled = (values.getAsInteger(GEN_FIELD_AUTOXENABLED) != 0);
        gen_autoXResType = values.getAsInteger(GEN_FIELD_AUTOXRESTYPE);
        gen_autoXWidth = values.getAsInteger(GEN_FIELD_AUTOXWIDTH);
        gen_autoXHeight = values.getAsInteger(GEN_FIELD_AUTOXHEIGHT);
        gen_autoXSessionProg = values.getAsString(GEN_FIELD_AUTOXSESSIONPROG);
        gen_autoXSessionType = values.getAsInteger(GEN_FIELD_AUTOXSESSIONTYPE);
        gen_autoXUnixpw = (values.getAsInteger(GEN_FIELD_AUTOXUNIXPW) != 0);
        gen_autoXUnixAuth = (values.getAsInteger(GEN_FIELD_AUTOXUNIXAUTH) != 0);
        gen_autoXRandFileNm = values.getAsString(GEN_FIELD_AUTOXRANDFILENM);
        gen_sshRemoteCommand = values.getAsString(GEN_FIELD_SSHREMOTECOMMAND);
        gen_sshRemoteCommandTimeout = values.getAsInteger(GEN_FIELD_SSHREMOTECOMMANDTIMEOUT);
        gen_useSshRemoteCommand = (values.getAsInteger(GEN_FIELD_USESSHREMOTECOMMAND) != 0);
        gen_sshHostKey = values.getAsString(GEN_FIELD_SSHHOSTKEY);
        gen_address = values.getAsString(GEN_FIELD_ADDRESS);
        gen_port = values.getAsInteger(GEN_FIELD_PORT);
        gen_caCert = values.getAsString(GEN_FIELD_CACERT);
        gen_caCertPath = values.getAsString(GEN_FIELD_CACERTPATH);
        gen_tlsPort = values.getAsInteger(GEN_FIELD_TLSPORT);
        gen_certSubject = values.getAsString(GEN_FIELD_CERTSUBJECT);
        gen_password = values.getAsString(GEN_FIELD_PASSWORD);
        gen_colorModel = values.getAsString(GEN_FIELD_COLORMODEL);
        gen_prefEncoding = values.getAsInteger(GEN_FIELD_PREFENCODING);
        gen_extraKeysToggleType = values.getAsInteger(GEN_FIELD_EXTRAKEYSTOGGLETYPE);
        gen_forceFull = values.getAsLong(GEN_FIELD_FORCEFULL);
        gen_repeaterId = values.getAsString(GEN_FIELD_REPEATERID);
        gen_inputMode = values.getAsString(GEN_FIELD_INPUTMODE);
        gen_SCALEMODE = values.getAsString(GEN_FIELD_SCALEMODE);
        gen_useDpadAsArrows = (values.getAsInteger(GEN_FIELD_USEDPADASARROWS) != 0);
        gen_rotateDpad = (values.getAsInteger(GEN_FIELD_ROTATEDPAD) != 0);
        gen_usePortrait = (values.getAsInteger(GEN_FIELD_USEPORTRAIT) != 0);
        gen_useLocalCursor = values.getAsInteger(GEN_FIELD_USELOCALCURSOR);
        gen_keepPassword = (values.getAsInteger(GEN_FIELD_KEEPPASSWORD) != 0);
        gen_followMouse = (values.getAsInteger(GEN_FIELD_FOLLOWMOUSE) != 0);
        gen_useRepeater = (values.getAsInteger(GEN_FIELD_USEREPEATER) != 0);
        gen_metaListId = values.getAsLong(GEN_FIELD_METALISTID);
        gen_LAST_META_KEY_ID = values.getAsLong(GEN_FIELD_LAST_META_KEY_ID);
        gen_followPan = (values.getAsInteger(GEN_FIELD_FOLLOWPAN) != 0);
        gen_userName = values.getAsString(GEN_FIELD_USERNAME);
        gen_rdpDomain = values.getAsString(GEN_FIELD_RDPDOMAIN);
        gen_secureConnectionType = values.getAsString(GEN_FIELD_SECURECONNECTIONTYPE);
        gen_showZoomButtons = (values.getAsInteger(GEN_FIELD_SHOWZOOMBUTTONS) != 0);
        gen_DOUBLE_TAP_ACTION = values.getAsString(GEN_FIELD_DOUBLE_TAP_ACTION);
        gen_rdpResType = values.getAsInteger(GEN_FIELD_RDPRESTYPE);
        gen_rdpWidth = values.getAsInteger(GEN_FIELD_RDPWIDTH);
        gen_rdpHeight = values.getAsInteger(GEN_FIELD_RDPHEIGHT);
        gen_rdpColor = values.getAsInteger(GEN_FIELD_RDPCOLOR);
        gen_remoteFx = (values.getAsInteger(GEN_FIELD_REMOTEFX) != 0);
        gen_desktopBackground = (values.getAsInteger(GEN_FIELD_DESKTOPBACKGROUND) != 0);
        gen_fontSmoothing = (values.getAsInteger(GEN_FIELD_FONTSMOOTHING) != 0);
        gen_desktopComposition = (values.getAsInteger(GEN_FIELD_DESKTOPCOMPOSITION) != 0);
        gen_windowContents = (values.getAsInteger(GEN_FIELD_WINDOWCONTENTS) != 0);
        gen_menuAnimation = (values.getAsInteger(GEN_FIELD_MENUANIMATION) != 0);
        gen_visualStyles = (values.getAsInteger(GEN_FIELD_VISUALSTYLES) != 0);
        gen_redirectSdCard = (values.getAsInteger(GEN_FIELD_REDIRECTSDCARD) != 0);
        gen_consoleMode = (values.getAsInteger(GEN_FIELD_CONSOLEMODE) != 0);
        gen_enableSound = (values.getAsInteger(GEN_FIELD_ENABLESOUND) != 0);
        gen_enableRecording = (values.getAsInteger(GEN_FIELD_ENABLERECORDING) != 0);
        gen_remoteSoundType = values.getAsInteger(GEN_FIELD_REMOTESOUNDTYPE);
        gen_viewOnly = (values.getAsInteger(GEN_FIELD_VIEWONLY) != 0);
        gen_layoutMap = values.getAsString(GEN_FIELD_LAYOUTMAP);

        gen_filename = values.getAsString(GEN_FIELD_FILENAME);
        gen_x509KeySignature = values.getAsString(GEN_FIELD_X509KEYSIGNATURE);
        gen_screenshotFilename = values.getAsString(GEN_FIELD_SCREENSHOTFILENAME);

        gen_enableGfx = (values.getAsInteger(GEN_FIELD_ENABLEGFX) != 0);
        gen_enableGfxH264 = (values.getAsInteger(GEN_FIELD_ENABLEGFXH264) != 0);

        gen_preferSendingUnicode = (values.getAsInteger(GEN_FIELD_PREFERSENDINGUNICODE) != 0);

        gen_externalId = values.getAsString(GEN_FIELD_EXTERNALID);
        gen_requiresVpn = (values.getAsInteger(GEN_FIELD_REQUIRESVPN) != 0);
        gen_vpnUriScheme = values.getAsString(GEN_FIELD_VPNURISCHEME);

        gen_rdpGatewayEnabled = values.getAsInteger(GEN_FIELD_RDPGATEWAYENABLED) != 0;
        gen_rdpGatewayHostname = values.getAsString(GEN_FIELD_RDPGATEWAYHOSTNAME);
        gen_rdpGatewayPort = values.getAsInteger(GEN_FIELD_RDPGATEWAYPORT);
        gen_rdpGatewayUsername = values.getAsString(GEN_FIELD_RDPGATEWAYUSERNAME);
        gen_rdpGatewayDomain = values.getAsString(GEN_FIELD_RDPGATEWAYDOMAIN);
        gen_rdpGatewayPassword = values.getAsString(GEN_FIELD_RDPGATEWAYPASSWORD);
        gen_keepRdpGatewayPassword = values.getAsBoolean(GEN_FIELD_KEEPRDPGATEWAYPASSWORD);
    }
}
