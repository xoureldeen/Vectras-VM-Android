package com.epicstudios.vectras;

import android.os.Environment;
import android.widget.ImageView.ScaleType;

import java.io.File;
import java.util.Hashtable;

/**
 *
 * @author dev
 */
public class Config {

	// App Config
	public static final String vectrasWebsite = "https://vectras.netlify.com/";
	public static final String vectrasRaw = "https://raw.githubusercontent.com/epicstudios856/Vectras-windows-emulator/main/";
	public static final String vectrasLicense = vectrasRaw + "LICENSE.md";
	public static final String vectrasPrivacy = vectrasRaw + "PRIVACYANDPOLICY.md";
	public static final String vectrasTerms = vectrasRaw + "TERMSOFSERVICE.md";
	public static final String vectrasInfo = vectrasRaw + "info.md";
	public static final String vectrasRepo = "https://github.com/epicstudios856/Vectras-windows-emulator/tree/main/";
	public static final String updateJson = vectrasRaw + "UpdateConfig.json";
	public static final String blogJson = vectrasRaw + "news_list.json";
	public static final String storeJson = vectrasRaw + "store_list.json";
	public static final String romsJson = vectrasRaw + "roms.json";

	// Constants
	public static final int SDL_MOUSE_LEFT = 1;
	public static final int SDL_MOUSE_RIGHT = 3;
	public static final int SETTINGS_RETURN_CODE = 1000;
	public static final int SDL_REQUEST_CODE = 1007;
	public static final String ACTION_START = "com.epicstudios.vectras.action.STARTVM";
	public static final String ACTION_STOP = "com.epicstudios.vectras.action.STOPVM";

	//Backend libs
	public static final boolean enable_iconv = false; //not needed for now

	public static final boolean enable_qemu_fullScreen = true;
	public static boolean enableSDLAlwaysFullscreen = true;

	// App config
	public static String packageName = "com.epicstudios.vectras";
	public static final String datadirpath = SplashActivity.activity.getExternalFilesDir("data")+"/";
	public static final String basefiledir = datadirpath + "Vectras/.qemu/";
	public static final String maindirpath = datadirpath + "/Vectras/";
	public static final String libqemupath = "libqemu-system-x86_64.so";
	public static final String sharedFolder = datadirpath + "Vectras/ProgramFiles/";
	public static final String tmpFolder = basefiledir + "tmp"; // Do not modify
	public static final String defaultDNSServer = "8.8.8.8";
	public static String state_filename = "vm.state";
	public static String notificationChannelID = "Vectras";
    public static String notificationChannelName = "Vectras";
	public static String QMPServer = "localhost";
	public static int QMPPort = 4444;

	//Keyboard Layout
	public static String defaultKeyboardLayout = "en-us";
	public static String logFilePath = maindirpath + "Vectras-logs.txt";

	// Debug
	public static final boolean debug = true;

    // Class that starts when user presses notification
    public static Class<?> clientClass = VectrasSDLActivity.class;
}
