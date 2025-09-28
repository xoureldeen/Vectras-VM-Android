/*
Copyright (C) Max Kastanas 2012

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package com.vectras.qemu;

import android.androidVNC.COLORMODEL;
import android.androidVNC.VncCanvasActivity;
import android.graphics.Bitmap;
import android.os.Environment;
import android.widget.ImageView.ScaleType;

import com.vectras.vm.AppConfig;
import com.vectras.vm.SplashActivity;
import com.vectras.vm.VectrasApp;

import java.util.Hashtable;
import java.util.LinkedHashMap;

/**
 *
 * @author dev
 */
public class Config {

    // Constants
    public static final int UI_VNC = 0;
    public static final int UI_SDL = 1;
    public static final int UI_SPICE = 2;
    public static final int SDL_MOUSE_LEFT = 1;
    public static final int SDL_MOUSE_MIDDLE = 2;
    public static final int SDL_MOUSE_RIGHT = 3;
    public static final int VNC_REQUEST_CODE = 1004;
    public static final int VNC_RESET_RESULT_CODE = 1006;
    public static final int SDL_REQUEST_CODE = 1007;
    public static final String ACTION_START = "com.vectras.qemu.action.STARTVM";

    // GUI Options
    public static final boolean enable_SDL = true;
    public static final boolean enable_SPICE = false;

    public static final boolean enable_qemu_fullScreen = true;

    // App config
    public static final String APP_NAME = "Vectras Emulator";
    public static String storagedir = null;

    //Some OSes don't like emulated multi cores for QEMU 2.9.1 you can disable here
    /// thought there is also the Disable TSC feature so you don't have to do it here
    public static boolean enableSMPOnlyOnKVM = false;

    //set to true if you need to debug native library loading
    public static boolean loadNativeLibsEarly = false; 

    //XXX: QEMU 3.1.0 needs the libraries to be loaded from the main thread
    public static boolean loadNativeLibsMainThread = true;

    public static String wakeLockTag = "vectras:wakelock";
    public static String wifiLockTag = "vectras:wifilock";

    //this will be populated later
    public static String cacheDir = null;

    //we disable mouse modes for now
    public static boolean disableMouseModes = true;

    //double tap an hold is still buggy so we keep using the old-way trackpad
    public static boolean enableDragOnLongPress = true;

    //we need to define the configuration for the VNC client since we replaced some deprecated
    //  functions
    public static Bitmap.Config bitmapConfig = Bitmap.Config.RGB_565;

    //XXX set scaling to linear it's a tad slower but it's worth it
    public static int SDLHintScale=1;
    public static boolean viewLogInternally = true;


    //XXX some archs don't support floppy or sd card
    public static boolean enableEmulatedFloppy = true;
    public static boolean enableEmulatedSDCard;
    public static String destLogFilename = "vectraslog.txt";

    public static String notificationChannelID = "vectras";
    public static String notificationChannelName = "vectras";
    public static boolean showToast = false;
    public static boolean closeFileDescriptors = true;
    public static String hda_path;
    public static String extra_params;


    public static final String getCacheDir(){
        return cacheDir.toString();
    }
    public static final String getBasefileDir() {
        return AppConfig.basefiledir;
    }

    public static String machineFolder = "machines/";
    public static String getMachineDir(){
        return getBasefileDir() + machineFolder;
    }
    public static String logFilePath = cacheDir + "/vectras/vectras-log.txt";


    public static final String defaultDNSServer = "8.8.8.8";
    public static String state_filename = "vm.state";

    //QMP
    public static String QMPServer = "127.0.0.1";
    public static int QMPPort = 4444;

    public static int MAX_DISPLAY_REFRESH_RATE = 100; //Hz

    // VNC Defaults
    public static String defaultVNCHost = "0.0.0.0";
    public static String defaultVNCUsername = "vectras";
    public static String defaultVNCPasswd = "555555";

    //It seems that for new version of qemu it expects a relative number
    //  so we stop using absolute port numbers
    public static int defaultVNCPort = 5901;

    //Keyboard Layout
    public static String defaultKeyboardLayout = "en-us";


    public static boolean enableToggleKeyboard = false;
    // Debug
    public static final boolean debug = false;
    public static boolean debugQmp = false;

    //remove in production
    public static boolean debugStrictMode = false;

    public static boolean processMouseHistoricalEvents = false;

    public static String getLocalQMPSocketPath() {
        return Config.getCacheDir() + "/" + vmID + "/qmpsocket";
    }

    public static String getLocalVNCSocketPath() {
        return Config.getCacheDir()+ "/" + vmID + "/vncsocket";
    }

    public static enum MouseMode {
        Trackpad, External
    }
    public static MouseMode mouseMode = MouseMode.Trackpad;

    //specify hd interface, alternative we don't need it right now
    public static boolean enable_hd_if = true;
    public static String hd_if_type = "ide";

    //Change to true in prod if you want to be notified by default for new versions
    public static boolean defaultCheckNewVersion = true;

    // App config
    public static final String datadirpath = VectrasApp.getApp().getExternalFilesDir("data")+"/";

	public static String machinename = "VECTRAS";
	public static int paused = 0;
	public static String ui = "VNC";
	public static boolean maxPriority = false;
    public static final String defaultVNCColorMode = COLORMODEL.C24bit.nameString();
    public static final ScaleType defaultFullscreenScaleMode = ScaleType.FIT_CENTER;
    public static final ScaleType defaultScaleModeCenter = ScaleType.CENTER;
    public static final String defaultInputMode = VncCanvasActivity.TOUCH_ZOOM_MODE;
    public static String vmID = "";
    public static String currentVNCServervmID = "";
    public static boolean forceRefeshVNCDisplay = false;

    public static void setDefault () {
        defaultVNCHost = "0.0.0.0";
        defaultVNCUsername = "vectras";
        defaultVNCPasswd = "555555";
        defaultVNCPort = 5901;
        QMPPort = 4444;
    }
}
