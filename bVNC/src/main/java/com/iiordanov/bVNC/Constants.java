/**
 * Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 2010 Michael A. MacDonald
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

/**
 * Keys for intent values
 */
public class Constants {

    public static final String defaultPackageName = "com.iiordanov.defaultbVNC";

    public static final int SDK_INT = android.os.Build.VERSION.SDK_INT;

    public static final int CONN_TYPE_PLAIN = 0;
    public static final int CONN_TYPE_SSH = 1;
    public static final int CONN_TYPE_ULTRAVNC = 2;
    public static final int CONN_TYPE_ANONTLS = 3;
    public static final int CONN_TYPE_VENCRYPT = 4;
    public static final int CONN_TYPE_STUNNEL = 5;

    public static final int SOCKET_CONN_TIMEOUT = 20 * 1000;

    public static final int DEFAULT_SSH_PORT = 22;
    public static final int LOGCAT_MAX_LINES = 500;
    // H_THRESH needs to be < than TOP_MARGIN to avoid pan following pointer unnecessarily.
    public static final int H_THRESH = 50;
    public static final int W_THRESH = 50;
    public static final int TOP_MARGIN = 110;
    public static final int BOTTOM_MARGIN = 300;
    public static final int DEFAULT_VNC_PORT = 5900;
    public static final int DEFAULT_RDP_PORT = 3389;
    public static final int DEFAULT_RDP_GATEWAY_PORT = 443;
    public static final int DEFAULT_RDP_COLOR_MODE = 16;
    // URI Parameters
    public static final String PARAM_CONN_NAME = "ConnectionName";
    public static final String PARAM_RDP_USER = "RdpUsername";
    public static final String PARAM_RDP_PWD = "RdpPassword";
    public static final String PARAM_SPICE_USER = "SpiceUsername";
    public static final String PARAM_SPICE_PWD = "SpicePassword";
    public static final String PARAM_VNC_USER = "VncUsername";
    public static final String PARAM_VNC_PWD = "VncPassword";
    public static final String PARAM_SECTYPE = "SecurityType";
    public static final String PARAM_SSH_HOST = "SshHost";
    public static final String PARAM_SSH_PORT = "SshPort";
    public static final String PARAM_SSH_USER = "SshUsername";
    public static final String PARAM_SSH_PWD = "SshPassword";
    public static final String PARAM_ID_HASH_ALG = "IdHashAlgorithm";
    public static final String PARAM_ID_HASH = "IdHash";
    public static final String PARAM_COLORMODEL = "ColorModel";
    public static final String PARAM_SAVE_CONN = "SaveConnection";
    public static final String PARAM_APIKEY = "ApiKey";
    public static final String PARAM_TLS_PORT = "TlsPort";
    public static final String PARAM_CACERT_PATH = "CaCertPath";
    public static final String PARAM_CERT_SUBJECT = "CertSubject";
    public static final String PARAM_VIEW_ONLY = "ViewOnly";
    public static final String PARAM_SCALE_MODE = "ScaleMode";
    public static final String PARAM_EXTRAKEYS_TOGGLE = "ExtraKeysToggle";
    public static final String PARAM_KEYBOARD_LAYOUT = "KeyboardLayout";
    public static final String PARAM_EXTERNAL_ID = "ExternalId";
    public static final String PARAM_REQUIRES_VPN = "RequiresVpn";
    public static final String PARAM_VPN_URI_SCHEME = "VpnUriScheme";
    public static final int SECTYPE_NONE = 1;
    public static final int SECTYPE_VNC = 2;
    public static final int SECTYPE_ULTRA = 17;
    public static final int SECTYPE_TLS = 18;
    public static final int SECTYPE_VENCRYPT = 19;
    public static final int SECTYPE_TUNNEL = 23;
    public static final int SECTYPE_INTEGRATED_SSH = 24;
    public static final int ID_HASH_MD5 = 1;
    public static final int ID_HASH_SHA1 = 2;
    public static final int ID_HASH_SHA256 = 4;
    public static final int COLORMODEL_BLACK_AND_WHITE = 1;
    public static final int COLORMODEL_GREYSCALE = 2;
    public static final int COLORMODEL_8_COLORS = 3;
    public static final int COLORMODEL_64_COLORS = 4;
    public static final int COLORMODEL_256_COLORS = 5;
    public static final int COLORMODEL_16BIT = 6;
    public static final int COLORMODEL_24BIT = 7;
    public static final int COLORMODEL_32BIT = 8;
    public static final int EXTRA_KEYS_OFF = 0;
    public static final int EXTRA_KEYS_ON = 1;
    public static final int EXTRA_KEYS_TIMEOUT = 2;
    public static final int ACTIVITY_GEN_KEY = 1;
    public final static int AUTOX_SELECT_DISABLED = 0;
    public final static int AUTOX_SELECT_XVFB = 1;
    public final static int AUTOX_SELECT_XVNC = 2;
    public final static int AUTOX_SELECT_XDUMMY = 3;
    public final static int AUTOX_SELECT_FIND = 4;
    public final static int AUTOX_SELECT_SUDO_FIND = 5;
    public final static int COMMAND_DISABLED = 0;
    public final static int COMMAND_LINUX_START = 100;
    public final static int COMMAND_LINUX_X11VNC = 101;
    public final static int COMMAND_LINUX_STDVNC = 102;
    public final static int COMMAND_LINUX_VINO = 103;
    public final static int COMMAND_LINUX_END = 199;
    public final static int COMMAND_WINDOWS_START = 200;
    public final static int COMMAND_WINDOWS_TIGHT = 201;
    public final static int COMMAND_WINDOWS_ULTRA = 202;
    public final static int COMMAND_WINDOWS_TIGER = 203;
    public final static int COMMAND_WINDOWS_REAL = 204;
    public final static int COMMAND_WINDOWS_END = 299;
    public final static int COMMAND_MACOSX_START = 300;
    public final static int COMMAND_MACOSX_STDVNC = 301;
    public final static int COMMAND_MACOSX_END = 399;
    public final static int COMMAND_CUSTOM = 1000;
    public final static int COMMAND_AUTO_X_DISABLED = 0;
    public final static int COMMAND_AUTO_X_CREATE_XVFB = 1;
    public final static int COMMAND_AUTO_X_CREATE_XVNC = 2;
    public final static int COMMAND_AUTO_X_CREATE_XDUMMY = 3;
    public final static int COMMAND_AUTO_X_FIND = 4;
    public final static int COMMAND_AUTO_X_SUDO_FIND = 5;
    public final static int COMMAND_AUTO_X_CUSTOM = 6;
    public final static String COMMAND_AUTO_X_CREATE_XVFB_STRING = "sh -c \"PORT= x11vnc -norc -nopw -wait_ui 2 -defer 15 -wait 15 -ncache 0 -timeout 10 -create -localhost ";
    public final static String COMMAND_AUTO_X_CREATE_XVNC_STRING = "sh -c \"PORT= x11vnc -norc -nopw -wait_ui 2 -defer 15 -wait 15 -ncache 0 -timeout 10 -create -localhost -xvnc ";
    public final static String COMMAND_AUTO_X_CREATE_XDUMMY_STRING = "sh -c \"PORT= x11vnc -norc -nopw -wait_ui 2 -defer 15 -wait 15 -ncache 0 -timeout 10 -create -localhost -xdummy ";
    public final static String COMMAND_AUTO_X_FIND_STRING = "sh -c \"PORT= x11vnc -norc -nopw -wait_ui 2 -defer 15 -wait 15 -ncache 0 -timeout 10 -find   -localhost ";
    public final static String COMMAND_AUTO_X_SUDO_FIND_STRING = "sh -c \"PORT= sudo -S x11vnc -norc -nopw -wait_ui 2 -defer 15 -wait 15 -ncache 0 -timeout 10 -find -localhost -env FD_XDM=1 ";
    public final static String AUTO_X_USERPW = "-unixpw $USER \"";
    public final static String AUTO_X_PASSWDFILE = "-passwdfile rm:";
    public final static String AUTO_X_PWFILEBASENAME = ".x11vnc_temp_pwd_";
    public final static String AUTO_X_CREATE_PASSWDFILE = "umask 0077 && cat > ";
    public final static String AUTO_X_SYNC = " ; sync";
    public final static String MV_DMRC_AWAY = "[ -O ${HOME}/.dmrc ] && mv ${HOME}/.dmrc ${HOME}/.dmrc.$$ ; ";
    public final static String MV_DMRC_BACK = " ; [ -O ${HOME}/.dmrc.$$ ] && mv ${HOME}/.dmrc.$$ ${HOME}/.dmrc";
    public final static int AUTOX_GEOM_SELECT_NATIVE = 0;
    public final static int AUTOX_GEOM_SELECT_CUSTOM = 1;
    public final static int VNC_GEOM_SELECT_DISABLED = 0;
    public final static int VNC_GEOM_SELECT_AUTOMATIC = 1;
    public final static int VNC_GEOM_SELECT_NATIVE_LANDSCAPE = 2;
    public final static int VNC_GEOM_SELECT_NATIVE_PORTRAIT = 3;
    public final static int VNC_GEOM_SELECT_CUSTOM = 4;
    public final static int AUTOX_SESS_PROG_SELECT_AUTO = 0;
    public final static int AUTOX_SESS_PROG_SELECT_CUSTOM = 1;
    public final static int AUTOX_SESS_PROG_SELECT_KDE = 2;
    public final static int AUTOX_SESS_PROG_SELECT_UNITY = 3;
    public final static int AUTOX_SESS_PROG_SELECT_UNITY2D = 4;
    public final static int AUTOX_SESS_PROG_SELECT_XFCE = 5;
    public final static int AUTOX_SESS_PROG_SELECT_GNOME = 6;
    public final static int AUTOX_SESS_PROG_SELECT_GNOMECL = 7;
    public final static int AUTOX_SESS_PROG_SELECT_TRINITY = 8;
    public final static int AUTOX_SESS_PROG_SELECT_MATE = 9;
    public final static String AUTOX_SESS_PROG_AUTO = "/etc/X11/Xsession";
    public final static String AUTOX_SESS_PROG_KDE = "/usr/bin/startkde";
    public final static String AUTOX_SESS_PROG_UNITY = "/usr/bin/gnome-session --session=ubuntu";
    public final static String AUTOX_SESS_PROG_UNITY2D = "/usr/bin/gnome-session --session=ubuntu-2d";
    public final static String AUTOX_SESS_PROG_XFCE = "/usr/bin/xfce4-session";
    public final static String AUTOX_SESS_PROG_GNOME = "/usr/bin/gnome-session --session=gnome";
    public final static String AUTOX_SESS_PROG_GNOMECL = "/usr/bin/gnome-session --session=gnome-classic";
    public final static String AUTOX_SESS_PROG_TRINITY = "/usr/bin/starttde";
    public final static String AUTOX_SESS_PROG_MATE = "/usr/bin/mate-session";
    public static final String DEFAULT_LAYOUT_MAP = "English (US)";
    public static final String passwordKey = "MasterPassword";
    public static final String testpassword = "password";
    public static final int numIterations = 10000;
    public static final int keyLength = 256;
    public static final int saltLength = 256;
    public static final String generalSettingsTag = "generalSettings";
    public static final String masterPasswordEnabledTag = "masterPasswordEnabled";
    public static final String keepScreenOnTag = "keepScreenOn";
    public static final String disableImmersiveTag = "disableImmersive";
    public static final String forceLandscapeTag = "forceLandscape";
    public static final String rAltAsIsoL3ShiftTag = "rAltAsIsoL3Shift";
    public static final String leftHandedModeTag = "leftHandedModeTag";
    public static final String defaultInputMethodTag = "defaultInputMethod";
    public static final String permissionsRequested = "permissionsRequested";
    public static final String positionToolbarLastUsed = "positionToolbarLastUsed";
    public static final String preferSendingUnicode = "preferSendingUnicode";
    public static final String doNotShowDesktopThumbnails = "doNotShowDesktopThumbnails";
    public static final String showOnlyConnectionNicknames = "showOnlyConnectionNicknames";
    public static final String softwareKeyboardType = "softwareKeyboardType";
    public static final String ACTION_USB_PERMISSION = "com.iiordanov.aSPICE.USB_PERMISSION";
    public static final int usbDeviceTimeout = 5000;
    public static final int usbDevicePermissionTimeout = 15000;
    public static final int REMOTE_SOUND_DISABLED = 2;
    public static final int REMOTE_SOUND_ON_SERVER = 1;
    public static final int REMOTE_SOUND_ON_DEVICE = 0;
    public static final int SHORT_VIBRATION = 1;
    public static final int CURSOR_AUTO = 0;
    public static final int CURSOR_FORCE_LOCAL = 1;
    public static final int CURSOR_FORCE_DISABLE = 2;
    public static volatile int DEFAULT_PROTOCOL_PORT = 5900;

    public static final String DEFAULT_VPN_URI_SCHEME = "vpn";
    /**
     * Returns a string matching a session selection index
     * @param index - index to convert
     * @return string matching prog.
     */
    public static String getSessionProgString(int index) {
        switch (index) {
            case AUTOX_SESS_PROG_SELECT_AUTO:
                return AUTOX_SESS_PROG_AUTO;
            case AUTOX_SESS_PROG_SELECT_KDE:
                return AUTOX_SESS_PROG_KDE;
            case AUTOX_SESS_PROG_SELECT_UNITY:
                return AUTOX_SESS_PROG_UNITY;
            case AUTOX_SESS_PROG_SELECT_UNITY2D:
                return AUTOX_SESS_PROG_UNITY2D;
            case AUTOX_SESS_PROG_SELECT_XFCE:
                return AUTOX_SESS_PROG_XFCE;
            case AUTOX_SESS_PROG_SELECT_GNOME:
                return AUTOX_SESS_PROG_GNOME;
            case AUTOX_SESS_PROG_SELECT_GNOMECL:
                return AUTOX_SESS_PROG_GNOMECL;
            case AUTOX_SESS_PROG_SELECT_TRINITY:
                return AUTOX_SESS_PROG_TRINITY;
            case AUTOX_SESS_PROG_SELECT_MATE:
                return AUTOX_SESS_PROG_MATE;
        }
        return "";
    }

    /**
     * Returns a string matching a command index.
     * @param command - command to convert
     * @param opts - options to add to command
     * @return string matching command.
     */
    public static String getCommandString(int command, String opts) {
        switch (command) {
            case COMMAND_AUTO_X_CREATE_XVFB:
                return COMMAND_AUTO_X_CREATE_XVFB_STRING + opts;

            case COMMAND_AUTO_X_CREATE_XVNC:
                return COMMAND_AUTO_X_CREATE_XVNC_STRING + opts;

            case COMMAND_AUTO_X_CREATE_XDUMMY:
                return COMMAND_AUTO_X_CREATE_XDUMMY_STRING + opts;

            case COMMAND_AUTO_X_FIND:
                return COMMAND_AUTO_X_FIND_STRING + opts;

            case COMMAND_AUTO_X_SUDO_FIND:
                return COMMAND_AUTO_X_SUDO_FIND_STRING + opts;
        }
        return "";
    }
    public static final String opaqueConnectionSettingsClassPath = "com.undatech.opaque.ConnectionSettings";
    public static final String remoteCanvasActivityClassPath = "com.iiordanov.bVNC.RemoteCanvasActivity";

}
