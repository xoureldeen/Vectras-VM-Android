package com.vectras.vm;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.color.DynamicColors;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vterm.Terminal;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class VectrasApp extends Application {
	public static VectrasApp vectrasapp;
	public static boolean debugLog = false;
	private static WeakReference<Context> context;

	public static Context getContext() {
		return context.get();
	}

	private static Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

	public static String TerminalOutput ="";

	@Override
	public void onCreate() {
		super.onCreate();
		vectrasapp = this;
		CrashHandler.getInstance().registerGlobal(this);
		CrashHandler.getInstance().registerPart(this);
		try {
			Class.forName("android.os.AsyncTask");
		} catch (Throwable ignore) {
			// ignored
		}
		setModeNight(this);
		DynamicColors.applyToActivitiesIfAvailable(this);

		Locale locale = Locale.getDefault();
		String language = locale.getLanguage();

		if (language.contains("ar")) {
			overrideFont("DEFAULT", R.font.cairo_regular);
		} else {
			overrideFont("DEFAULT", R.font.gilroy);
		}

	}

	public void overrideFont(String defaultFontNameToOverride, int customFontResourceId) {
		try {
			Typeface customFontTypeface = ResourcesCompat.getFont(getApplicationContext(), customFontResourceId);

			final Field defaultFontTypefaceField = Typeface.class.getDeclaredField(defaultFontNameToOverride);
			defaultFontTypefaceField.setAccessible(true);
			defaultFontTypefaceField.set(null, customFontTypeface);
		} catch (Exception e) {
			Log.e("overrideFont", "Failed to override font", e);
		}
	}

	private void setModeNight(Context context) {
		if (MainSettingsManager.getModeNight(context)) {
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
			setTheme(R.style.AppTheme);
		} else {
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
			setTheme(R.style.AppTheme);
		}

	}

	public static Context getApp() {
		return vectrasapp;
	}


	public static void write(InputStream input, OutputStream output) throws IOException {
		byte[] buf = new byte[1024 * 8];
		int len;
		while ((len = input.read(buf)) != -1) {
			output.write(buf, 0, len);
		}
	}

	public static void write(File file, byte[] data) throws IOException {
		File parent = file.getParentFile();
		if (parent != null && !parent.exists())
			parent.mkdirs();

		ByteArrayInputStream input = new ByteArrayInputStream(data);
		FileOutputStream output = new FileOutputStream(file);
		try {
			write(input, output);
		} finally {
			closeIO(input, output);
		}
	}

	public static String toString(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		write(input, output);
		try {
			return output.toString("UTF-8");
		} finally {
			closeIO(input, output);
		}
	}

	public static void closeIO(Closeable... closeables) {
		for (Closeable closeable : closeables) {
			try {
				if (closeable != null)
					closeable.close();
			} catch (IOException ignored) {
			}
		}
	}

	public static class CrashHandler {

		public static final UncaughtExceptionHandler DEFAULT_UNCAUGHT_EXCEPTION_HANDLER = Thread
				.getDefaultUncaughtExceptionHandler();

		private static CrashHandler sInstance;

		private PartCrashHandler mPartCrashHandler;

		public static CrashHandler getInstance() {
			if (sInstance == null) {
				sInstance = new CrashHandler();
			}
			return sInstance;
		}

		public void registerGlobal(Context context) {
			registerGlobal(context, null);
		}

		public void registerGlobal(Context context, String crashDir) {
			Thread.setDefaultUncaughtExceptionHandler(
					new UncaughtExceptionHandlerImpl(context.getApplicationContext(), crashDir));
		}

		public void unregister() {
			Thread.setDefaultUncaughtExceptionHandler(DEFAULT_UNCAUGHT_EXCEPTION_HANDLER);
		}

		public void registerPart(Context context) {
			unregisterPart(context);
			mPartCrashHandler = new PartCrashHandler(context.getApplicationContext());
			MAIN_HANDLER.postAtFrontOfQueue(mPartCrashHandler);
		}

		public void unregisterPart(Context context) {
			if (mPartCrashHandler != null) {
				mPartCrashHandler.isRunning.set(false);
				mPartCrashHandler = null;
			}
		}

		private static class PartCrashHandler implements Runnable {

			private final Context mContext;

			public AtomicBoolean isRunning = new AtomicBoolean(true);

			public PartCrashHandler(Context context) {
				this.mContext = context;
			}

			@Override
			public void run() {
				while (isRunning.get()) {
					try {
						Looper.loop();
					} catch (final Throwable e) {
						e.printStackTrace();
						if (isRunning.get()) {
							MAIN_HANDLER.post(new Runnable() {

								@Override
								public void run() {
									//VectrasStatus.logError("<font color='red'>[E] >"+ mContext.getApplicationContext().toString() +e.getMessage()+"</font>");
								}
							});
						} else {
							if (e instanceof RuntimeException) {
								throw (RuntimeException) e;
							} else {
								throw new RuntimeException(e);
							}
						}
					}
				}
			}
		}

		private static class UncaughtExceptionHandlerImpl implements UncaughtExceptionHandler {

			private static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");

			private final Context mContext;

			private final File mCrashDir;

			public UncaughtExceptionHandlerImpl(Context context, String crashDir) {
				this.mContext = context;
				this.mCrashDir = TextUtils.isEmpty(crashDir) ? new File(mContext.getExternalCacheDir(), "crash")
						: new File(crashDir);
			}

			@Override
			public void uncaughtException(Thread thread, Throwable throwable) {
				try {

					String log = buildLog(throwable);
					writeLog(log);

					try {
						Intent intent = new Intent(mContext, CrashActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.putExtra(Intent.EXTRA_TEXT, log);
						mContext.startActivity(intent);
					} catch (Throwable e) {
						e.printStackTrace();
						writeLog(e.toString());
					}

					throwable.printStackTrace();
					android.os.Process.killProcess(android.os.Process.myPid());
					System.exit(0);

				} catch (Throwable e) {
					if (DEFAULT_UNCAUGHT_EXCEPTION_HANDLER != null)
						DEFAULT_UNCAUGHT_EXCEPTION_HANDLER.uncaughtException(thread, throwable);
				}
			}

			private String buildLog(Throwable throwable) {
				String time = DATE_FORMAT.format(new Date());

				String versionName = "unknown";
				long versionCode = 0;
				try {
					PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
					versionName = packageInfo.versionName;
					versionCode = Build.VERSION.SDK_INT >= 28 ? packageInfo.getLongVersionCode()
							: packageInfo.versionCode;
				} catch (Throwable ignored) {
				}

				LinkedHashMap<String, String> head = new LinkedHashMap<String, String>();
				head.put("Time Of Crash", time);
				head.put("Device", String.format("%s, %s", Build.MANUFACTURER, Build.MODEL));
				head.put("Android Version", String.format("%s (%d)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
				head.put("App Version", String.format("%s (%d)", versionName, versionCode));
				head.put("Kernel", getKernel());
				head.put("Support Abis",
						Build.VERSION.SDK_INT >= 21 && Build.SUPPORTED_ABIS != null
								? Arrays.toString(Build.SUPPORTED_ABIS)
								: "unknown");
				head.put("Fingerprint", Build.FINGERPRINT);

				StringBuilder builder = new StringBuilder();

				for (String key : head.keySet()) {
					if (builder.length() != 0)
						builder.append("\n");
					builder.append(key);
					builder.append(" :    ");
					builder.append(head.get(key));
				}

				builder.append("\n\n");
				builder.append(Log.getStackTraceString(throwable));

				return builder.toString();
			}

			private void writeLog(String log) {
				String time = DATE_FORMAT.format(new Date());
				File file = new File(mCrashDir, "crash_" + time + ".txt");
				try {
					write(file, log.getBytes("UTF-8"));
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}

			private static String getKernel() {
				try {
					return VectrasApp.toString(new FileInputStream("/proc/version")).trim();
				} catch (Throwable e) {
					return e.getMessage();
				}
			}
		}
	}

	public static final class CrashActivity extends Activity {

		private String mLog;

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			setTheme(android.R.style.Theme_DeviceDefault);
			setTitle("App Crash");

			mLog = getIntent().getStringExtra(Intent.EXTRA_TEXT);

			ScrollView contentView = new ScrollView(this);
			contentView.setFillViewport(true);

			HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);

			TextView textView = new TextView(this);
			int padding = dp2px(16);
			textView.setPadding(padding, padding, padding, padding);
			textView.setText(mLog);
			textView.setTextIsSelectable(true);
			textView.setTypeface(Typeface.DEFAULT);
			textView.setLinksClickable(true);

			horizontalScrollView.addView(textView);
			contentView.addView(horizontalScrollView, ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT);

			setContentView(contentView);
		}

		private void restart() {
			Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
			if (intent != null) {
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
			finish();
			android.os.Process.killProcess(android.os.Process.myPid());
			System.exit(0);
		}

		private static int dp2px(float dpValue) {
			final float scale = Resources.getSystem().getDisplayMetrics().density;
			return (int) (dpValue * scale + 0.5f);
		}

		@Override
		public boolean onCreateOptionsMenu(Menu menu) {
			menu.add(0, android.R.id.copy, 0, android.R.string.copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
			return super.onCreateOptionsMenu(menu);
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
			case android.R.id.copy:
				ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				cm.setPrimaryClip(ClipData.newPlainText(getPackageName(), mLog));
				return true;
			}
			return super.onOptionsItemSelected(item);
		}

		@Override
		public void onBackPressed() {
			restart();
		}
	}

	public static boolean checkpermissionsgranted(Activity activity, boolean request) {
		if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
			return true;
		} else {
			if (request) {
				AlertDialog alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
				alertDialog.setTitle(activity.getResources().getString(R.string.allow_permissions));
				alertDialog.setMessage(activity.getResources().getString(R.string.you_need_to_grant_permission_to_access_the_storage_before_use));
				alertDialog.setCancelable(false);
				alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, activity.getResources().getString(R.string.allow), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (activity.shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
							Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
							intent.setData(Uri.parse("package:" + activity.getPackageName()));
							activity.startActivity(intent);
							Toast.makeText(activity, activity.getResources().getString(R.string.find_and_allow_access_to_storage_in_settings), Toast.LENGTH_LONG).show();
						} else {
							ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
						}
						alertDialog.dismiss();
					}
				});
				alertDialog.show();
			}
			return false;
		}
	}

	public static boolean checkJSONIsNormal(String _filepath) {
		ArrayList<HashMap<String, Objects>> mmap = new ArrayList<>();
		String contentjson = "";
		if (VectrasApp.isFileExists(_filepath)) {
			contentjson = readFile(_filepath);
			try {
				mmap.clear();
				mmap = new Gson().fromJson(contentjson, new TypeToken<ArrayList<HashMap<String, Object>>>() {
				}.getType());
				return true;
			} catch (Exception e) {
				return false;
			}
		} else {
			return false;
		}
	}

	public static boolean checkJSONIsNormalFromString(String _content) {
		ArrayList<HashMap<String, Objects>> mmap = new ArrayList<>();
		try {
			mmap.clear();
			mmap = new Gson().fromJson(_content, new TypeToken<ArrayList<HashMap<String, Object>>>() {
			}.getType());
				return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean checkJSONMapIsNormalFromString(String _content) {
		HashMap<String, Object> mmap = new HashMap<>();
		try {
			mmap.clear();
			mmap= new Gson().fromJson(_content, new TypeToken<HashMap<String, Object>>(){}.getType());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isFileExists(String filePath) {
		File file = new File(filePath.replaceAll("\n", ""));
		return file.exists();
	}

	public static String readFile(String filePath) {
		StringBuilder content = new StringBuilder();
		try (FileInputStream inputStream = new FileInputStream(filePath);
			 BufferedReader reader = new BufferedReader(new
					 InputStreamReader(inputStream))) {
			String line;
			while ((line = reader.readLine()) != null) {
				content.append(line).append("\n");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return content.toString();

	}

	public static void writeToFile(String folderPath, String fileName, String content) {
		File vDir = new File(folderPath);
		if (!vDir.exists()) {
			vDir.mkdirs();
		}
		File file = new File(folderPath, fileName);
		FileOutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(file);
			outputStream.write(content.getBytes());
			outputStream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void deleteDirectory(String _pathToDelete) {
		File _dir = new File(_pathToDelete);
		if (_dir.isDirectory()) {
			String[] children = _dir.list();
			for (int i = 0; i < children.length; i++) {
				File temp = new File(_dir, children[i]);
				deleteDirectory(String.valueOf(temp));
			}
		}
		boolean success = _dir.delete();
		if (!success) {
			Log.e("ERROR", "Deletion failed. " + _dir);
		}
	}

	public static void copyAFile(String _sourceFile, String _destFile) {
		File vDir = new File(_destFile.substring((int)0, (int)(_destFile.lastIndexOf("/"))));
		if (!vDir.exists()) {
			vDir.mkdirs();
		}
		try {
			File source = new File(_sourceFile);
			File dest = new File(_destFile);

			if (!source.exists())
			{
				throw new IOException("Source file not found");
			}

			FileInputStream inStream = new FileInputStream(source);
			FileOutputStream outStream = new FileOutputStream(dest);

			byte[] buffer = new byte[1024];
			int length;
			while ((length = inStream.read(buffer))
					> 0) {
				outStream.write(buffer, 0, length);
			}

			inStream.close();
			outStream.close();
		} catch (IOException e) {

		}

	}

	public static void moveAFile(String _from, String _to) {
		File oldFile = new File(_from);
		File newFile = new File(_to);

		boolean success = oldFile.renameTo(newFile);
		if (success) {
			Log.d("File", "Done!");
		} else {
			Log.e("File", "Failed!");
		}

	}

	public static void listDir(String path, ArrayList<String> list) {
		File dir = new File(path);
		if (!dir.exists() || dir.isFile()) return;

		File[] listFiles = dir.listFiles();
		if (listFiles == null || listFiles.length <= 0) return;

		if (list == null) return;
		list.clear();
		for (File file : listFiles) {
			list.add(file.getAbsolutePath());
		}
	}

	public static void killallqemuprocesses(Context context) {
		Terminal vterm = new Terminal(context);
		vterm.executeShellCommand2("killall -9 qemu-system-i386", false, MainActivity.activity);
		vterm.executeShellCommand2("killall -9 qemu-system-x86_64", false, MainActivity.activity);
		vterm.executeShellCommand2("killall -9 qemu-system-aarch64", false, MainActivity.activity);
		vterm.executeShellCommand2("killall -9 qemu-system-ppc", false, MainActivity.activity);
	}

	public static void killcurrentqemuprocess(Context context) {
		Terminal vterm = new Terminal(context);
		String env = "killall -9 ";
		switch (MainSettingsManager.getArch(MainActivity.activity)) {
			case "ARM64":
				env += "qemu-system-aarch64";
				break;
			case "PPC":
				env += "qemu-system-ppc";
				break;
			case "I386":
				env += "qemu-system-i386";
				break;
			default:
				env += "qemu-system-x86_64";
				break;
		}
		vterm.executeShellCommand2(env, false, MainActivity.activity);
	}

	public static boolean isAppInstalled(String packagename, Context context) {
		PackageManager pm = context.getPackageManager();
		try {
			pm.getPackageInfo(packagename,PackageManager.GET_ACTIVITIES);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return  false;
		}
	}

	public static void setIconWithName(ImageView imageview, String name) {
		String itemName = name.toLowerCase();
		if (itemName.contains("linux") || itemName.contains("ubuntu")  || itemName.contains("debian") || itemName.contains("arch") || itemName.contains("kali")) {
			imageview.setImageResource(R.drawable.linux);
		} else if (itemName.contains("windows")) {
			imageview.setImageResource(R.drawable.windows);
		} else if (itemName.contains("macos") || itemName.contains("mac os")) {
			imageview.setImageResource(R.drawable.macos);
		} else if (itemName.contains("android")) {
			imageview.setImageResource(R.drawable.android);
		} else {
			imageview.setImageResource(R.drawable.no_machine_image);
		}
	}

	public static boolean isHaveADisk(String env) {
		if (env.contains("-drive") || env.contains("-hda")  || env.contains("-hdb") || env.contains("-cdrom") || env.contains("-fda") || env.contains("-fdb"))
			return true;
		return false;
	}

	public static boolean isQemuRunning() {
		Terminal vterm = new Terminal(MainActivity.activity);
		vterm.executeShellCommand2("ps -e", false, MainActivity.activity);
		if (TerminalOutput.contains("qemu-system")) {
			Log.d("VectrasApp.isQemuRunning", "Yes");
			return true;
		} else {
			Log.d("VectrasApp.isQemuRunning", "No");
			return false;
		}
	}

	public static boolean isThisVMRunning(String intemExtra, String itemPath) {
		Terminal vterm = new Terminal(MainActivity.activity);
		vterm.executeShellCommand2("ps -e", false, MainActivity.activity);
		if (TerminalOutput.contains(intemExtra) && TerminalOutput.contains(itemPath)) {
			Log.d("VectrasApp.isThisVMRunning", "Yes");
			return true;
		} else {
			Log.d("VectrasApp.isThisVMRunning", "No");
			return false;
		}
	}

	public static void oneDialog(String _title, String _message, boolean _cancel, boolean _finish, Activity _activity) {
		AlertDialog alertDialog = new AlertDialog.Builder(_activity, R.style.MainDialogTheme).create();
		alertDialog.setTitle(_title);
		alertDialog.setMessage(_message);
		if (!_cancel) {
			alertDialog.setCancelable(false);
		}
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (_finish) {
					_activity.finish();
				}
			}
		});
		alertDialog.show();
	}

	public static void oneDialogWithContext(String _title, String _message, boolean _cancel, Context _context) {
		AlertDialog alertDialog = new AlertDialog.Builder(_context, R.style.MainDialogTheme).create();
		alertDialog.setTitle(_title);
		alertDialog.setMessage(_message);
		if (!_cancel) {
			alertDialog.setCancelable(false);
		}
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {

			}
		});
		alertDialog.show();
	}

	public static void prepareDataForAppConfig(Activity _activity) {
		AppConfig.vectrasVersion = "2.9.4";
		AppConfig.vectrasWebsite = "https://vectras.vercel.app/";
		AppConfig.vectrasWebsiteRaw = "https://raw.githubusercontent.com/xoureldeen/Vectras-VM-Android/refs/heads/master/web/";
		AppConfig.bootstrapfileslink = AppConfig.vectrasWebsiteRaw + "/data/setupfiles.json";
		AppConfig.vectrasHelp = AppConfig.vectrasWebsite + "how.html";
		AppConfig.community = AppConfig.vectrasWebsite + "community.html";
		AppConfig.vectrasRaw = AppConfig.vectrasWebsiteRaw + "data/";
		AppConfig.vectrasLicense = AppConfig.vectrasRaw + "LICENSE.md";
		AppConfig.vectrasPrivacy = AppConfig.vectrasRaw + "PRIVACYANDPOLICY.md";
		AppConfig.vectrasTerms = AppConfig.vectrasRaw + "TERMSOFSERVICE.md";
		AppConfig.vectrasInfo = AppConfig.vectrasRaw + "info.md";
		AppConfig.vectrasRepo = "https://github.com/xoureldeen/Vectras-VM-Android";
		AppConfig.updateJson = AppConfig.vectrasRaw + "UpdateConfig.json";
		AppConfig.blogJson = AppConfig.vectrasRaw + "news_list.json";
		AppConfig.storeJson = AppConfig.vectrasWebsiteRaw + "store_list.json";
		AppConfig.releaseUrl = AppConfig.vectrasWebsite;
		AppConfig.basefiledir = AppConfig.datadirpath(_activity) + "/.qemu/";
		AppConfig.maindirpath = FileUtils.getExternalFilesDirectory(_activity).getPath() + "/";
		AppConfig.sharedFolder = AppConfig.maindirpath + "SharedFolder/";
		AppConfig.downloadsFolder = AppConfig.maindirpath + "Downloads/";
		AppConfig.romsdatajson = AppConfig.maindirpath + "roms-data.json";
		AppConfig.vmFolder = AppConfig.maindirpath + "roms/";
		AppConfig.recyclebin = AppConfig.maindirpath + "recyclebin/";
	}

	public static PackageInfo getAppInfo(Context context) {
		try {
			return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
		} catch (PackageManager.NameNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
