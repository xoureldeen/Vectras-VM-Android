/**
 * Copyright (C) 2013- Iordan Iordanov
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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


package com.undatech.opaque;

public class RemoteClientLibConstants {

    public static final int SDK_INT = android.os.Build.VERSION.SDK_INT;

    public final static int RDP_GEOM_SELECT_NATIVE_LANDSCAPE = 0;
    public final static int RDP_GEOM_SELECT_NATIVE_PORTRAIT = 1;
    public final static int RDP_GEOM_SELECT_CUSTOM = 2;
    public final static int RDP_GEOM_SELECT_AUTO = 3;

    public static final int DIALOG_X509_CERT = 1;
    public static final int DIALOG_SSH_CERT = 2;
    public static final int DIALOG_RDP_CERT = 3;
    public static final int SPICE_CONNECT_SUCCESS = 4;
    public static final int SPICE_CONNECT_FAILURE = 5;
    public static final int RDP_CONNECT_FAILURE = 7;
    public static final int RDP_UNABLE_TO_CONNECT = 8;
    public static final int RDP_AUTH_FAILED = 9;
    public static final int GET_PASSWORD = 10;
    public static final int GET_VERIFICATIONCODE = 11;
    public static final int GET_SSH_CREDENTIALS = 12;
    public static final int GET_SSH_PASSPHRASE = 13;
    public static final int GET_VNC_CREDENTIALS = 14;
    public static final int GET_VNC_PASSWORD = 15;
    public static final int REINIT_SESSION = 16;
    public static final int DISCONNECT_NO_MESSAGE = 17;
    public static final int DISCONNECT_WITH_MESSAGE = 18;
    public static final int GET_RDP_CREDENTIALS = 19;
    public static final int GET_SPICE_PASSWORD = 20;
    public static final int REPORT_TOOLBAR_POSITION = 21;

    public static final int LAUNCH_VNC_VIEWER = 23;
    public static final int VM_LAUNCHED = 24;
    public static final int OVIRT_AUTH_FAILURE = 25;
    public static final int OVIRT_SSL_HANDSHAKE_FAILURE = 26;
    public static final int VM_LOOKUP_FAILED = 27;
    public static final int NO_VM_FOUND_FOR_USER = 28;
    public static final int VV_FILE_ERROR = 29;
    public static final int OVIRT_TIMEOUT = 30;
    public static final int VV_OVER_HTTP_FAILURE = 31;
    public static final int VV_OVER_HTTPS_FAILURE = 32;
    public static final int PVE_FAILED_TO_AUTHENTICATE = 33;
    public static final int PVE_FAILED_TO_CONNECT = 34;
    public static final int PVE_FAILED_TO_PARSE_JSON = 35;
    public static final int PVE_VMID_NOT_NUMERIC = 36;
    public static final int PVE_API_UNEXPECTED_CODE = 37;
    public static final int PVE_API_IO_ERROR = 38;
    public static final int PVE_TIMEOUT_COMMUNICATING = 39;
    public static final int PVE_NULL_DATA = 40;
    public static final int VV_DOWNLOAD_TIMEOUT = 41;
    public static final int GET_OTP_CODE = 42;
    public static final int SERVER_CUT_TEXT = 43;
    public static final int DIALOG_DISPLAY_VMS = 44;
    public static final int SPICE_TLS_ERROR = 45;
    public static final int SPICE_CONNECT_FAILURE_IF_MAINTAINING_CONNECTION = 46;
    public static final int SHOW_TOAST = 47;
    public static final int GET_RDP_GATEWAY_CREDENTIALS = 48;

    public static final int PRO_FEATURE = 99;
    public static final int ADVANCED_SETTINGS = 1;
    public static final int DEFAULT_SETTINGS = 2;
    public static final int IMPORT_SETTINGS_REQUEST_CODE = 3;
    public static final int EXPORT_SETTINGS_REQUEST_CODE = 4;

    public static final String DEFAULT_LAYOUT_MAP = "English (US)";

    public static final String ACTION_USB_PERMISSION = "com.undatech.opaque.USB_PERMISSION";

    public static final String DEFAULT_SETTINGS_FILE = "defaultSettings";

    public static final int URL_BUFFER_SIZE = 3000;
    public static final int VV_GET_FILE_TIMEOUT = 17000;

    public static final String PVE_DEFAULT_REALM = "pam";

    public static final int LOGCAT_MAX_LINES = 500;
}
