package com.epicstudios.vectras;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import static android.os.Build.VERSION.SDK_INT;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.text.Html;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.epicstudios.vectras.R;
import com.epicstudios.vectras.jni.VMExecutor;
import com.epicstudios.vectras.Fragment.HomeFragment;
import com.epicstudios.vectras.Fragment.LoggerFragment;
import com.epicstudios.vectras.logger.VectrasStatus;
import com.epicstudios.vectras.utils.AppUpdater;
import com.epicstudios.vectras.utils.FileInstaller;
import com.epicstudios.vectras.utils.FileUtils;
import com.epicstudios.vectras.utils.RamInfo;
import com.epicstudios.vectras.utils.UIUtils;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.TimerTask;
import java.util.Timer;

//import com.epicstudios.vectras.R;

public class MainActivity extends AppCompatActivity {
	private static Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
	public static final String TAG = "Vectras";
	// Static
	public static final String CREDENTIAL_SHARED_PREF = "settings_prefs";

	public static String[] params = null;
	public static boolean vmStarted = false;
	public static MainActivity activity = null;
	public static VMExecutor vmexecutor;
	public static String currStatus = "READY";
	static public ProgressDialog progDialog;
	public static Handler OShandler;
	private Handler mHandler;
	private static Installer a;
	private static String output;
	public View parent;
	public TextView mOutput;
	public AutoScrollView mLyricsScroll;
	public DrawerLayout mainDrawer;
	public CoordinatorLayout mainCoordinatorLayout;
	public AppBarLayout mainAppBar;
	public CollapsingToolbarLayout mainCtl;
	public FloatingActionButton mainFab;
	public Toolbar mainToolbar;
	public static String strFree, strUsed, strTotal;
	public static TextView totalRam;
	public static TextView usedRam;
	public static TextView freeRam;
	public static TextView ipTxt;
	public FloatingActionButton mStart;
	private Timer _timer = new Timer();
	private TimerTask t;
	public ViewPager viewPager;
	MenuItem prevMenuItem;
	int pager_number = 2;

	//Widgets
	private boolean timeQuit = false;
	private Object lockTime = new Object();

	public static void UIAlert(String title, String body, Activity activity) {
		AlertDialog ad;
		ad = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
		ad.setTitle(title);
		ad.setMessage(body);
		ad.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				return;
			}
		});
		ad.show();
	}

	public void FAB_Click(View v) {
		/*Thread thread = new Thread(new Runnable() {
			public void run() {
				MainActivity.onStartButton();
			}
		});
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();*/
		if (HomeFragment.romsLayout.getVisibility() == View.GONE) {
			Animation animation;
			animation = AnimationUtils.loadAnimation(MainActivity.activity, R.anim.slide_from_left);
			animation.setDuration(300);
			HomeFragment.romsLayout.startAnimation(animation);
			Animation animation2;
			animation2 = AnimationUtils.loadAnimation(MainActivity.activity, R.anim.slide_from_top);
			animation2.setDuration(300);
			HomeFragment.mRVBlog.startAnimation(animation2);
			HomeFragment.romsLayout.setVisibility(View.VISIBLE);
		} else if (HomeFragment.romsLayout.getVisibility() == View.VISIBLE) {
			Animation animation;
			animation = AnimationUtils.loadAnimation(MainActivity.activity, R.anim.slide_to_left);
			animation.setDuration(300);
			HomeFragment.romsLayout.startAnimation(animation);
			Animation animation2;
			animation2 = AnimationUtils.loadAnimation(MainActivity.activity, R.anim.slide_from_top);
			animation2.setDuration(300);
			HomeFragment.mRVBlog.startAnimation(animation2);
			HomeFragment.romsLayout.setVisibility(View.GONE);
		}
	}

	public static void quit() {
		activity.finish();
	}

	public static void install() {
		progDialog = ProgressDialog.show(activity, "Please Wait", "Installing Files...", true);
		a = new Installer();
		a.execute();
	}

	public static void onInstall() {
		FileInstaller.installFiles(activity);
	}

	// This is easier: traverse the interfaces and get the local IPs
	public static String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && inetAddress.getHostAddress().toString().contains(".")) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	// Start calling the JNI interface
	public static void startvm(Activity activity) {
		vmexecutor.startvm(activity);

	}

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		NotificationManager notificationManager = (NotificationManager) getApplicationContext()
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancelAll();
		setupStrictMode();
		activity = this;
		this.setContentView(R.layout.main);
		this.setupWidgets();
		execTimeListener();
		initNavigationMenu();
		updateApp(true);
		
		mHandler = new Handler();

		setupNativeLibs();
	}
	public static PackageInfo getAppInfo(Context context){
		try {
			return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
		} catch (PackageManager.NameNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	public void updateApp(final boolean showDialog) {
		new AppUpdater(this, new AppUpdater.OnUpdateListener() {
			@Override
			public void onUpdateListener(String result) {
				try {
					if (!result.contains("Error on getting data")) {
						final JSONObject obj = new JSONObject(result);
						PackageInfo pinfo = getAppInfo(getApplicationContext());
						int versionCode = pinfo.versionCode;
						if (versionCode < obj.getInt("versionCode")) {
							AlertDialog.Builder alert = new AlertDialog.Builder(activity, R.style.MainDialogTheme);
							alert.setTitle("Install the latest version")
									.setMessage(Html.fromHtml(obj.getString("Message") + "<br><br>update size:<br>" + obj.getString("size")))
									.setCancelable(obj.getBoolean("cancellable"))
									.setNegativeButton("Update", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int id) {
											try  {
												startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(obj.getString("url"))));
											} catch (JSONException e) {

											}
										}
									}).show();

						}
					} else if(result.contains("Error on getting data") && showDialog){
						errorUpdateDialog(result);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start(showDialog);
	}

	private void errorUpdateDialog(String error) {
		VectrasStatus.logInfo(String.format(error));
	}

	private MenuItem vectrasInfo;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.home_toolbar_menu, menu);
		vectrasInfo = menu.findItem(R.id.vectrasInfo);
		return true;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Menu items
		int id = item.getItemId();
		if (id == R.id.vectrasInfo) {
			AppBarLayout nnl_appbar = findViewById(R.id.nnl_appbar);
			if (nnl_appbar.getTop() < 0)
				nnl_appbar.setExpanded(true);
			else
				nnl_appbar.setExpanded(false);

		}

		return super.onOptionsItemSelected(item);
	}

	private void initNavigationMenu() {
		BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation_view);

		bottomNavigationView
				.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
					@Override
					public boolean onNavigationItemSelected(@NonNull MenuItem item) {
						int id = item.getItemId();
						if (id == R.id.menu_home) {
							viewPager.setCurrentItem(0);
						} else if (id == R.id.menu_logger) {
							viewPager.setCurrentItem(1);
						}
						return false;
					}
				});
	}

	public class MyAdapter extends FragmentPagerAdapter {

		MyAdapter(FragmentManager fm) {
       	    super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
		}

		@Override
		public Fragment getItem(int position) {

			switch (position) {
			case 0:
				return new HomeFragment();
			case 1:
				return new LoggerFragment();
			}
			return null;
		}

		@Override
		public int getCount() {
			return pager_number;
		}
	}

	public void setupNativeLibs() {
        //Glib
		System.loadLibrary("glib-2.0");
		System.loadLibrary("gthread-2.0");
		System.loadLibrary("gobject-2.0");
		System.loadLibrary("gmodule-2.0");

        //Pixman for qemu
        System.loadLibrary("pixman");

		// //Load SDL libraries
		System.loadLibrary("SDL2");
		System.loadLibrary("SDL2_image");

		// System.loadLibrary("mikmod");
		System.loadLibrary("SDL2_mixer");
		// System.loadLibrary("SDL_ttf");

		//main for SDL
		System.loadLibrary("main");

		//Limbo needed for vmexecutor
		System.loadLibrary("vectras");

		System.loadLibrary("qemu-system-x86_64");
	}

	public void checkUpdate() {

	}

	private void setupStrictMode() {

		if (Config.debug) {
			StrictMode.setThreadPolicy(
					new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork()
							//.penaltyDeath()
							.penaltyLog().build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects()
					.detectLeakedClosableObjects().penaltyLog()
					// .penaltyDeath()
					.build());
		}

	}

	public void cleanup() {
		MainActivity.vmexecutor = null;

	}

	public void exit() {
		onStopButton(true);
	}

	// Main event function
	// Retrives values from saved preferences
	public static void onStartButton() {

		if (vmexecutor == null) {

			try {
				vmexecutor = new VMExecutor(activity);
			} catch (Exception ex) {
				UIUtils.toastLong(activity, "Error: " + ex);
				return;

			}
		}

		output = "Starting VM...";
		VectrasStatus.logInfo(String.format("Starting VM..."));
		vmexecutor.paused = 0;
		startSDL();

		if (vmStarted) {
			//do nothing
		} else if (vmexecutor.paused == 1) {
			vmStarted = true;
		}

	}

	public static void startSDL() {

		Thread tsdl = new Thread(new Runnable() {
			public void run() {
				startsdl();
			}
		});
		tsdl.setPriority(Thread.MAX_PRIORITY);
		tsdl.start();
	}

	public static void onStopButton(boolean exit) {
		stopVM(exit);
	}

	public static void onRestartButton() {

		new AlertDialog.Builder(activity).setTitle("Reset VM")
				.setMessage("VM will be reset and you may lose data. Continue?")
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (MainActivity.vmexecutor != null) {
							Thread t = new Thread(new Runnable() {
								public void run() {
									restartvm();
								}
							});
							t.start();
						} else if (activity.getParent() != null) {
							activity.getParent().finish();
						} else {
							activity.finish();
						}
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();

	}

	public static void onResumeButton() {

		// TODO: This probably has no effect
		Thread t = new Thread(new Runnable() {
			public void run() {
				resumevm();
			}
		});
		t.start();
	}

	public static void prepareParams(String hda) throws FileNotFoundException {
		String libqemu = null;
		libqemu = FileUtils.getDataDir() + "/lib/libqemu-system-x86_64.so";
		
		SharedPreferences credentials = activity.getSharedPreferences(CREDENTIAL_SHARED_PREF, Context.MODE_PRIVATE);
		
		params = null;
		ArrayList<String> paramsList = new ArrayList<String>();

		paramsList.add(libqemu);

		paramsList.add("-L");
		paramsList.add(Config.basefiledir);

		paramsList.add("-hda");
		paramsList.add(hda);

		paramsList.add("-hdb");
		paramsList.add(Config.basefiledir+"harddisk/hdb.qcow2");

		paramsList.add("-hdc");
		paramsList.add(Config.basefiledir+"harddisk/hdc.qcow2");

		paramsList.add("-hdd");
		paramsList.add(Config.basefiledir+"harddisk/hdd.qcow2");
/*
		paramsList.add("-cdrom");
		paramsList.add(Config.basefiledir+"iso/cdrom.iso");
*/
		paramsList.add("-boot");
		paramsList.add("c");

		paramsList.add("-k");
		paramsList.add("en-us");
/*
		paramsList.add("-drive");
		String driveParams = "index=3";
		driveParams += ",media=disk";
		driveParams += ",if=ide";
		driveParams += ",format=raw";
		driveParams += ",file=fat:";
		driveParams += "rw:";
		driveParams += Config.sharedFolder;
		paramsList.add(driveParams);*/

		params = (String[]) paramsList.toArray(new String[paramsList.size()]);

	}

	// Setting up the UI
	public void setupWidgets() {
		viewPager = findViewById(R.id.viewPager);
		viewPager.setAdapter(new MyAdapter(getSupportFragmentManager()));
		viewPager.setOffscreenPageLimit(pager_number);
		final BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation_view);
		viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			}

			@Override
			public void onPageSelected(int position) {
				if (prevMenuItem != null) {
					prevMenuItem.setChecked(false);
				} else {
					bottomNavigationView.getMenu().getItem(0).setChecked(false);
				}
				bottomNavigationView.getMenu().getItem(position).setChecked(true);
				prevMenuItem = bottomNavigationView.getMenu().getItem(position);
			}

			@Override
			public void onPageScrollStateChanged(int state) {

			}
		});

		AppBarLayout nnl_appbar = findViewById(R.id.nnl_appbar);
		nnl_appbar.setExpanded(false);
		mainToolbar = (Toolbar) findViewById(R.id.nnl_toolbar);
		setSupportActionBar(mainToolbar);
		mainDrawer = (DrawerLayout) findViewById(R.id.nnl_drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mainDrawer, mainToolbar,
				R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		mainDrawer.setDrawerListener(toggle);
		toggle.syncState();

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

		//Setting Navigation View Item Selected Listener to handle the item click of the navigation menu
		navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

			// This method will trigger on item Click of navigation menu
			@Override
			public boolean onNavigationItemSelected(MenuItem menuItem) {
				//Closing drawer on item click
				mainDrawer.closeDrawers();

				//Check to see which item was being clicked and perform appropriate action
				int id = menuItem.getItemId();
				if (id == R.id.navigation_item_info){
					startActivity(new Intent(activity, AboutActivity.class));
				} else if (id == R.id.navigation_item_website){
					String tw = Config.vectrasWebsite;
					Intent w = new Intent(Intent.ACTION_VIEW);
					w.setData(Uri.parse(tw));
					startActivity(w);
				} else if (id == R.id.navigation_item_store){
					startActivity(new Intent(activity, StoreActivity.class));
				}
				return false;
			}
		});

		ipTxt = findViewById(R.id.ipTxt);

		ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
		ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(mi);

		long freeMem = mi.availMem / 1048576L;
		long totalMem = mi.totalMem / 1048576L;
		long usedMem = totalMem - freeMem;
		int freeRamInt = safeLongToInt(freeMem);
		int totalRamInt = safeLongToInt(totalMem);
		ipTxt.setText("Local Ip Address: " + getLocalIpAddress());

		SharedPreferences credentials = activity.getSharedPreferences(CREDENTIAL_SHARED_PREF, Context.MODE_PRIVATE);

		totalRam = findViewById(R.id.totalRam);
		usedRam = findViewById(R.id.usedRam);
		freeRam = findViewById(R.id.freeRam);
		t = new TimerTask() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						//update
						ActivityManager.MemoryInfo miI = new ActivityManager.MemoryInfo();
						ActivityManager activityManagerr = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
						activityManagerr.getMemoryInfo(miI);
						//update textview here
						long freeMemory = miI.availMem / 1048576L;
						long totalMemory = miI.totalMem / 1048576L;
						long usedMemory = totalMemory - freeMemory;

						totalRam.setText("Total Memory: " + totalMemory + " MB");
						usedRam.setText("Used Memory: " + usedMemory + " MB");
						freeRam.setText("Free Memory: " + freeMemory + " MB ("+RamInfo.vectrasMemory()+")");
					}
				});
			}
		};
		_timer.scheduleAtFixedRate(t, (int) (0), (int) (1000));

		mStart = (FloatingActionButton) findViewById(R.id.nnl_fab);
		mStart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {

				FAB_Click(view);

			}
		});
		AdView mAdView = findViewById(R.id.adView);
		AdRequest adRequest = new AdRequest.Builder().build();
		mAdView.loadAd(adRequest);
		UIAlert(getString(R.string.app_version), "This is only the first beta version of Vectras. Please note that the application is still in the experimental stage so that it is available in the best condition. Do not be lazy and support the application with your opinion.", activity);



		//File extra = new File(Config.basefiledir+"config_extra.txt");
		//String extraParams = FileUtils.readFromFile(MainActivity.activity, extra);
	
		//UIAlert("Args", imgPath+"\n\n"+vectrasMem, activity);
	}

	public static int safeLongToInt(long l) {
		if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(l + " cannot be cast to int without changing its value.");
		}
		return (int) l;
	}

	@Override
	public void onBackPressed() {
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.nnl_drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			moveTaskToBack(true);
			return true; // return
		}

		return false;
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.stopTimeListener();

	}

	public static void startsdl() {

		Intent intent = null;

		intent = new Intent(activity, VectrasSDLActivity.class);

		android.content.ContentValues values = new android.content.ContentValues();
		activity.startActivityForResult(intent, Config.SDL_REQUEST_CODE);
	}

	public static void restartvm() {
		if (vmexecutor != null) {

			output = vmexecutor.stopvm(1);
			vmStarted = true;

		} else {

		}

	}

	public void savevm(String name) {

	}

	public static void resumevm() {
		if (vmexecutor != null) {
			output = vmexecutor.resumevm();

		} else {

		}

	}

	public void onViewLog() {
		
	}

	private void goToURL(String url) {

		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		activity.startActivity(i);

	}

	public static void stopVM(boolean exit) {

		new AlertDialog.Builder(activity).setTitle("Shutdown VM")
				.setMessage("To avoid any corrupt data make sure you "
						+ "have already shutdown the Operating system from within the VM. Continue?")
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						if (MainActivity.vmexecutor != null) {
							MainActivity.vmexecutor.stopvm(0);
							VectrasStatus.logInfo(String.format("VMStopped"));
						} else if (activity.getParent() != null) {
							activity.getParent().finish();
						} else {
							activity.finish();
						}
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
	}

	public void saveSnapshotDB(String snapshot_name) {

	}

	public void saveStateVMDB() {

	}

	public void stopTimeListener() {

		synchronized (this.lockTime) {
			this.timeQuit = true;
			this.lockTime.notifyAll();
		}
	}

	public void onPause() {
		super.onPause();
	}

	public void onResume() {

		super.onResume();

	}

	public void timeListener() {
		while (timeQuit != true) {
			if (vmexecutor != null) {
				String status = checkStatus();
				if (!status.equals(currStatus)) {
					currStatus = status;

				}
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}

	}

	void execTimeListener() {

		Thread t = new Thread(new Runnable() {
			public void run() {
				startTimeListener();
			}
		});
		t.start();
	}

	public void startTimeListener() {
		this.stopTimeListener();

		timeQuit = false;
		try {

			timeListener();
			synchronized (lockTime) {
				while (timeQuit == false) {
					lockTime.wait();
				}
				lockTime.notifyAll();
			}
		} catch (Exception ex) {
			ex.printStackTrace();

		}

	}

	private String checkStatus() {
		String state = "READY";
		if (vmexecutor != null && vmexecutor.libLoaded && vmexecutor.get_state().equals("RUNNING")) {
			state = "RUNNING";
		} else if (vmexecutor != null) {
			//String save_state = vmexecutor.get_save_state();
			String pause_state = vmexecutor.get_pause_state();

			// Shutdown if paused done
			if (pause_state.equals("SAVING")) {
				return pause_state;
			} else if (pause_state.equals("DONE")) {
				if (MainActivity.vmexecutor != null) {
					MainActivity.vmexecutor.stopvm(0);
				}

			} else {
				state = "READY";
			}
		} else {
			state = "READY";
		}

		return state;
	}

	public static class Installer extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {
			onInstall();
			if (progDialog.isShowing()) {
				progDialog.dismiss();
				//activity.setupWidgets();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void test) {

		}
	}

	public class AutoScrollView extends ScrollView {

		public AutoScrollView(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		public AutoScrollView(Context context) {
			super(context);
		}
	}
}
