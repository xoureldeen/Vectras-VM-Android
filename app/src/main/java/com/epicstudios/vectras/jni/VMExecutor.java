package com.epicstudios.vectras.jni;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.epicstudios.vectras.Config;
import com.epicstudios.vectras.MainActivity;
import com.epicstudios.vectras.VectrasService;
import com.epicstudios.vectras.logger.VectrasStatus;
import com.epicstudios.vectras.utils.FileUtils;
import com.epicstudios.vectras.utils.RamInfo;
import com.epicstudios.vectras.utils.UIUtils;
import java.io.File;
import android.os.Environment;

public class VMExecutor {

	private static Context context;
	private final String TAG = "VMExecutor";
	public int paused;
	public String base_dir = Config.basefiledir;
	public boolean busy = false;
	public boolean libLoaded = false;

	// Default Settings
	private String libqemu = Config.libqemupath;
	private int restart = 0;
	private int width;
	private int height;

	public static final File fileExtra = new File(Config.basefiledir + "config_extra.txt");
	public static final String extraParams = FileUtils.readFromFile(MainActivity.activity, fileExtra);

	/**
	 * @throws Exception
	 */
	public VMExecutor(Context context) throws Exception {

	}

	public void loadNativeLibs() {
		libLoaded = true;
	}

	// Load the shared lib
	private void loadNativeLib(String lib, String destDir) {
		if (true) {
			String libLocation = destDir + "/" + lib;
			try {
				System.load(libLocation);
			} catch (Exception ex) {
				VectrasStatus.logInfo(String.format("failed to load native library: "+ex));
				Log.e("JNIExample", "failed to load native library: " + ex);
			}
		}

	}

	public String startvm() {

		String res = null;

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread thread, Throwable e) {
				e.printStackTrace();
				VectrasStatus.logInfo(String.format("Vectras Uncaught Exception: "+e.toString()));
				Log.e(TAG, "Vectras Uncaught Exception: " + e.toString());
			}
		});

		try {
			String Extras = null;
			if (extraParams == "null") {
				Extras = null;
			} else {
				Extras = extraParams;
			}
			if (extraParams == "error") {
				VectrasStatus.logInfo(String.format(" QEMU Params Error: "+extraParams));
				return "error";
			}
			VectrasStatus.logInfo(String.format("QEMU PARAMS: "+extraParams+" -m "+ RamInfo.vectrasMemory()));
			res = start(this.libqemu, MainActivity.params, extraParams+" -m "+ RamInfo.vectrasMemory(), paused, "VECTRAS");
		} catch (Exception ex) {
			ex.printStackTrace();
			VectrasStatus.logInfo(String.format("Vectras Exception: " + ex.toString()));
			Log.e(TAG, "Vectras Exception: " + ex.toString());
		}
		return res;
	}

	//JNI Methods
	public native String start(String lib_path, Object[] params, String params_extra, int paused, String save_state);

	protected native String stop(int restart);

	protected native void scale();

	protected native String getpausestate();

	public native String pausevm(String uri);

	protected native void resize();

	protected native void togglefullscreen();

	protected native String getstate();

	public String startvm(Context context) {
		VectrasService.executor = this;
		Intent i = new Intent(Config.ACTION_START, null, context, VectrasService.class);
		Bundle b = new Bundle();
		// b.putString("machine_type", this.machine_type);
		b.putInt("ui", 1);
		i.putExtras(b);
		context.startService(i);
		
		VectrasStatus.logInfo(String.format("VMStarted"));
		Log.v(TAG, "startVMService");
		return "startVMService";

	}

	public String stopvm(int restart) {
		Log.v(TAG, "Stopping the VM");
		VectrasStatus.logInfo(String.format("Stopping the VM"));
		this.restart = restart;
		return this.stop(this.restart);
	}

	public String resumevm() {
		// Set to delete previous snapshots after vm resumed
		Log.v(TAG, "Resume the VM");
		VectrasStatus.logInfo(String.format("Resume the VM"));
		String res = startvm();
		Log.d(TAG, res);
		return res;
	}

	public String get_pause_state() {
		if (this.libLoaded)
			return this.getpausestate();
		return "";
	}

	public String get_state() {
		return this.getstate();
	}

	public void resizeScreen() {

		this.resize();

	}

	public void toggleFullScreen() {

		this.togglefullscreen();

	}

	public void screenScale(int width, int height) {

		this.width = width;
		this.height = height;

		this.scale();

	}

	public int get_fd(String path) {
		int fd = FileUtils.get_fd(context, path);
		return fd;

	}

	public int close_fd(int fd) {
		int res = FileUtils.close_fd(fd);
		return res;

	}

}
