package com.epicstudios.vectras;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.epicstudios.vectras.R;
import com.epicstudios.vectras.Fragment.ControlsFragment;
import com.epicstudios.vectras.logger.VectrasStatus;
import com.epicstudios.vectras.utils.FileUtils;
import com.epicstudios.vectras.utils.QmpClient;
import com.epicstudios.vectras.utils.UIUtils;
import java.io.File;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import org.json.JSONObject;
import org.libsdl.app.ClearRenderer;
import org.libsdl.app.SDLActivity;
import org.libsdl.app.SDLSurface;

/**
 * SDL Activity
 */
public class VectrasSDLActivity extends SDLActivity {

	public static final int KEYBOARD = 10000;
	public static final int QUIT = 10001;
	public static final int HELP = 10002;
	public static boolean monitorMode = false;
	private boolean mouseOn = false;
	private Object lockTime = new Object();
	private boolean timeQuit = false;
	private Thread timeListenerThread;
	private ProgressDialog progDialog;
	public static Activity activity ;

	public String cd_iso_path = null;

	// HDD
	public String hda_img_path = null;
	public String hdb_img_path = null;
	public String hdc_img_path = null;
	public String hdd_img_path = null;

	public String fda_img_path = null;
	public String fdb_img_path = null;
	public String cpu = null;
	public String TAG = "VMExecutor";

	public int aiomaxthreads = 1;
	// Default Settings
	public int memory = 128;
	public String bootdevice = null;
	// net
	public String net_cfg = "None";
	public int nic_num = 1;
	public String vga_type = "std";
	public String hd_cache = "default";
	public String nic_driver = null;
    public String soundcard = null;
	public String lib = "libvectras.so";
	public String lib_path = null;
	public int restart = 0;
	public String snapshot_name = "Vectras";
	public int disableacpi = 0;
	public int disablehpet = 0;
	public int disabletsc = 0;
	public static int enablebluetoothmouse = 0;
	public int enableqmp = 0;
	public int enablevnc = 0;
	public String vnc_passwd = null;
	public int vnc_allow_external = 0;
	public String qemu_dev = null;
	public String qemu_dev_value = null;
	public String base_dir = Config.basefiledir;
	public String dns_addr = null;
	private boolean once = true;
	public static boolean zoomable = false;
	private String status = null;

	public static Handler handler;

	// This is what SDL runs in. It invokes SDL_main(), eventually
	private static Thread mSDLThread;

	// EGL private objects
	private static EGLContext mEGLContext;
	private static EGLSurface mEGLSurface;
	private static EGLDisplay mEGLDisplay;
	private static EGLConfig mEGLConfig;
	private static int mGLMajor, mGLMinor;

	public static int width;
	public static int height;
	public static int screen_width;
	public static int screen_height;

	private static Activity activity1;

	// public static void showTextInput(int x, int y, int w, int h) {
	// // Transfer the task to the main thread as a Runnable
	// // mSingleton.commandHandler.post(new ShowTextInputHandler(x, y, w, h));
	// }

	public static void singleClick(final MotionEvent event, final int pointer_id) {
		
		Thread t = new Thread(new Runnable() {
			public void run() {
				// Log.d("SDL", "Mouse Single Click");
				SDLActivity.onNativeTouch(event.getDeviceId(), Config.SDL_MOUSE_LEFT, MotionEvent.ACTION_DOWN, 0, 0, 0);
				try {
					Thread.sleep(100);
				} catch (InterruptedException ex) {
					// Log.v("singletap", "Could not sleep");
				}
				SDLActivity.onNativeTouch(event.getDeviceId(), Config.SDL_MOUSE_LEFT, MotionEvent.ACTION_UP, 0, 0, 0);
			}
		});
		t.start();
	}

	private void promptBluetoothMouse(final Activity activity) {
		

		final AlertDialog alertDialog;
		alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
		alertDialog.setTitle("Enable Bluetooth Mouse");

		LinearLayout mLayout = new LinearLayout(this);
        mLayout.setPadding(20,20,20,20);
        mLayout.setOrientation(LinearLayout.VERTICAL);

		TextView textView = new TextView(activity);
		textView.setVisibility(View.VISIBLE);
		textView.setText(
				"Step 1: Disable Mouse Acceleration inside the Guest OS.\n\tFor DSL use command: dsl@box:/>xset m 1\n"
						+ "Step 2: Pair your Bluetooth Mouse and press OK!\n"
						+ "Step 3: Move your mouse pointer to all desktop corners to calibrate.\n");

        LinearLayout.LayoutParams textViewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		mLayout.addView(textView, textViewParams);
		alertDialog.setView(mLayout);

		final Handler handler = this.handler;

		// alertDialog.setMessage(body);
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				MotionEvent a = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);
				VectrasSDLActivity.singleClick(a, 0);
				// SDLActivityCommon.onNativeMouseReset(0, 0,
				// MotionEvent.ACTION_MOVE, vm_width / 2, vm_height / 2, 0);
				VectrasSDLActivity.enablebluetoothmouse = 1;

			}
		});
		alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				VectrasSDLActivity.enablebluetoothmouse = 0;
				return;
			}
		});
		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				VectrasSDLActivity.enablebluetoothmouse = 0;
				return;

			}
		});
		alertDialog.show();

	}

	public static void sendCtrlAtlKey(int code) {

		SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_CTRL_LEFT);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ALT_LEFT);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SDLActivity.onNativeKeyDown(code);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_CTRL_LEFT);
		SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_ALT_LEFT);
		SDLActivity.onNativeKeyUp(code);
	}

	public void stopTimeListener() {
		Log.v("SaveVM", "Stopping Listener");
		synchronized (this.lockTime) {
			this.timeQuit = true;
			this.lockTime.notifyAll();
		}
	}

	public void onDestroy() {

		// Now wait for the SDL thread to quit
		Log.v("VectrasSDL", "Waiting for SDL thread to quit");
		if (mSDLThread != null) {
			try {
				mSDLThread.join();
			} catch (Exception e) {
				Log.v("SDL", "Problem stopping thread: " + e);
			}
			mSDLThread = null;

			Log.v("SDL", "Finished waiting for SDL thread");
		}
		this.stopTimeListener();
		super.onDestroy();
	}

	public void timeListener() {
		while (timeQuit != true) {
			status = checkCompletion();
			// Log.v("timeListener", "Status: " + status);
			if (status == null
                    || status.equals("")
                    || status.equals("DONE")
                    || status.equals("ERROR")
                    ) {
				Log.v("Inside", "Saving state is done: " + status);
				stopTimeListener();
				return;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
				Log.v("SaveVM", "Could not sleep");
			}
		}
		Log.v("SaveVM", "Save state complete");

	}

	public void startTimeListener() {
		this.stopTimeListener();
		timeQuit = false;
		try {
			Log.v("Listener", "Time Listener Started...");
			timeListener();
			synchronized (lockTime) {
				while (timeQuit == false) {
					lockTime.wait();
				}
				lockTime.notifyAll();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			Log.v("SaveVM", "Time listener thread error: " + ex.getMessage());
		}
		Log.v("Listener", "Time listener thread exited...");

	}

    public void onHideToolbar(){
        ActionBar bar = this.getSupportActionBar();
        if (bar != null) {
            bar.hide();
        }
    }


	private void onMouse() {

		MotionEvent a = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);
		VectrasSDLActivity.singleClick(a, 0);
		// SDLActivityCommon.onNativeMouseReset(0, 0, MotionEvent.ACTION_MOVE,
		// vm_width / 2, vm_height / 2, 0);
		//Toast.makeText(this.getApplicationContext(), "Mouse Trackpad Mode enabled", Toast.LENGTH_SHORT).show();
	}

	private void onCtrlAltDel() {
		
		SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_CTRL_RIGHT);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ALT_RIGHT);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_FORWARD_DEL);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_FORWARD_DEL);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_ALT_RIGHT);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_CTRL_RIGHT);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void onCtrlC() {
		
		SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_CTRL_RIGHT);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_C);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_C);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_CTRL_RIGHT);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void resetVM() {

		new AlertDialog.Builder(VectrasSDLActivity.activity, R.style.MainDialogTheme).setTitle("Reset VM")
				.setMessage("To avoid any corrupt data make sure you "
						+ "have already shutdown the Operating system from within the VM. Continue?")
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						new Thread(new Runnable() {
							public void run() {
								Log.v("SDL", "VM is reset");
								onRestartVM();
							}
						}).start();

					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
	}

	public static void stopVM(Context activity, boolean exit) {

		new AlertDialog.Builder(activity, R.style.MainDialogTheme).setTitle("Shutdown VM")
				.setMessage("To avoid any corrupt data make sure you "
						+ "have already shutdown the Operating system from within the VM. Continue?")
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						new Thread(new Runnable() {
							public void run() {
								Log.v("SDL", "VM is stopped");
								nativeQuit();
							}
						}).start();

					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
	}

	private static void setStretchToScreen() {
		

		new Thread(new Runnable() {
			public void run() {
				VectrasSDLActivity.stretchToScreen = true;
				VectrasSDLActivity.fitToScreen = false;
				sendCtrlAtlKey(KeyEvent.KEYCODE_6);
			}
		}).start();

	}

	private static void setFitToScreen() {
		

		new Thread(new Runnable() {
			public void run() {
				VectrasSDLActivity.stretchToScreen = false;
				VectrasSDLActivity.fitToScreen = true;
				sendCtrlAtlKey(KeyEvent.KEYCODE_5);

			}
		}).start();

	}

	private void setOneToOne() {
		
		new Thread(new Runnable() {
			public void run() {
				VectrasSDLActivity.stretchToScreen = false;
				VectrasSDLActivity.fitToScreen = false;
				sendCtrlAtlKey(KeyEvent.KEYCODE_U);
			}
		}).start();

	}

	public static void setFullScreen() {
		

		new Thread(new Runnable() {
			public void run() {
				sendCtrlAtlKey(KeyEvent.KEYCODE_F);
			}
		}).start();

	}

	public static void setZoomIn() {
		
		new Thread(new Runnable() {
			public void run() {
				VectrasSDLActivity.stretchToScreen = false;
				VectrasSDLActivity.fitToScreen = false;
				sendCtrlAtlKey(KeyEvent.KEYCODE_4);
			}
		}).start();

	}

	public static void setZoomOut() {
		

		new Thread(new Runnable() {
			public void run() {
				VectrasSDLActivity.stretchToScreen = false;
				VectrasSDLActivity.fitToScreen = false;
				sendCtrlAtlKey(KeyEvent.KEYCODE_3);

			}
		}).start();

	}

	public static void setZoomable() {
		
		zoomable = true;

	}

	public static void onMonitor() {
		new Thread(new Runnable() {
			public void run() {
				monitorMode = true;
				// final KeyEvent altDown = new KeyEvent(downTime, eventTime,
				// KeyEvent.ACTION_DOWN,
				// KeyEvent.KEYCODE_2, 1, KeyEvent.META_ALT_LEFT_ON);
				sendCtrlAtlKey(KeyEvent.KEYCODE_2);
				// sendCtrlAtlKey(altDown);
				Log.v("Vectras", "Monitor On");
			}
		}).start();

	}

	public static void onVMConsole() {
		monitorMode = false;
		sendCtrlAtlKey(KeyEvent.KEYCODE_1);
	}

	private void onSaveState(final String stateName) {
		// onMonitor();
		// try {
		// Thread.sleep(1000);
		// } catch (InterruptedException ex) {
		// Logger.getLogger(VectrasVNCActivity.class.getName()).log(
		// Level.SEVERE, null, ex);
		// }
		// vncCanvas.sendText("savevm " + stateName + "\n");
		// Toast.makeText(this.getApplicationContext(),
		// "Please wait while saving VM State", Toast.LENGTH_LONG).show();
		new Thread(new Runnable() {
			public void run() {
				Log.v("SDL", "Saving VM1");
				nativePause();
				// VectrasActivity.vmexecutor.saveVM1(stateName);

				nativeResume();

			}
		}).start();

		// try {
		// Thread.sleep(1000);
		// } catch (InterruptedException ex) {
		// Logger.getLogger(VectrasVNCActivity.class.getName()).log(
		// Level.SEVERE, null, ex);
		// }
		// onSDL();
		((MainActivity) MainActivity.activity).saveSnapshotDB(stateName);

		progDialog = ProgressDialog.show(activity, "Please Wait", "Saving VM State...", true);
		SaveVM a = new SaveVM();
		a.execute();

	}

	public void saveStateDB(String snapshot_name) {
		
	}

	private void onSaveState1(String stateName) {
		// Log.v("onSaveState1", stateName);
		monitorMode = true;
		sendCtrlAtlKey(KeyEvent.KEYCODE_2);
		
		sendText("savevm " + stateName + "\n");
		saveStateDB(stateName);
		
		sendCommand(COMMAND_SAVEVM, "vm");

	}

	// FIXME: We need this to able to catch complex characters strings like
	// grave and send it as text
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_MULTIPLE && event.getKeyCode() == KeyEvent.KEYCODE_UNKNOWN) {
			sendText(event.getCharacters().toString());
			return true;
		} else
			return super.dispatchKeyEvent(event);

	}

	private static void sendText(String string) {
		
		// Log.v("sendText", string);
		KeyCharacterMap keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
		KeyEvent[] keyEvents = keyCharacterMap.getEvents(string.toCharArray());
		if (keyEvents != null)
			for (int i = 0; i < keyEvents.length; i++) {

				if (keyEvents[i].getAction() == KeyEvent.ACTION_DOWN) {
					// Log.v("sendText", "Up: " + keyEvents[i].getKeyCode());
					SDLActivity.onNativeKeyDown(keyEvents[i].getKeyCode());
				} else if (keyEvents[i].getAction() == KeyEvent.ACTION_UP) {
					// Log.v("sendText", "Down: " + keyEvents[i].getKeyCode());
					SDLActivity.onNativeKeyUp(keyEvents[i].getKeyCode());
				}
			}
	}

	private class SaveVM extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {
			// Log.v("handler", "Save VM");
			startTimeListener();
			return null;
		}

		@Override
		protected void onPostExecute(Void test) {
			try {
				if (progDialog.isShowing()) {
					progDialog.dismiss();
				}
				monitorMode = false;
				sendCtrlAtlKey(KeyEvent.KEYCODE_1);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	private void fullScreen() {
		// AbstractScaling.getById(R.id.itemFitToScreen).setScaleTypeForActivity(
		// this);
		// showPanningState();
	}

	public void promptStateName(final Activity activity) {
		// Log.v("promptStateName", "ask");
		
		final AlertDialog alertDialog;
		alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
		alertDialog.setTitle("Snapshot/State Name");
		final EditText stateView = new EditText(activity);
		
		stateView.setEnabled(true);
		stateView.setVisibility(View.VISIBLE);
		stateView.setSingleLine();
		alertDialog.setView(stateView);

		// alertDialog.setMessage(body);
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Create", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {

				progDialog = ProgressDialog.show(activity, "Please Wait", "Saving VM State...", true);
				new Thread(new Runnable() {
					public void run() {
						// Log.v("promptStateName", a.getText().toString());
						onSaveState1(stateView.getText().toString());
					}
				}).start();

				return;
			}
		});
		alertDialog.show();

	}

	public void pausedVM() {

		MainActivity.vmexecutor.paused = 1;
		((MainActivity) MainActivity.activity).saveStateVMDB();

		new AlertDialog.Builder(this, R.style.MainDialogTheme).setTitle("Paused").setMessage("VM is now Paused tap OK to exit")
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (MainActivity.vmexecutor != null) {
                            MainActivity.vmexecutor.stopvm(0);
						} else if (activity.getParent() != null) {
							activity.getParent().finish();
						} else {
							activity.finish();
						}
					}
				}).show();
	}


    public void pausedErrorVM(String errStr) {


        new AlertDialog.Builder(this, R.style.MainDialogTheme).setTitle("Error").setMessage(errStr)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        Thread t = new Thread(new Runnable() {
                            public void run() {
                                String command = QmpClient.cont();
                                String msg = QmpClient.sendCommand(command);
                            }
                        });
                        t.start();
                    }
                }).show();
    }

	private String checkCompletion() {
		String save_state = "";
		String pause_state = "";
		if (MainActivity.vmexecutor != null) {
			// Get the state of saving full disk snapshot
			//save_state = MainActivity.vmexecutor.get_save_state();

			// Get the state of saving the VM memory only
			pause_state = MainActivity.vmexecutor.get_pause_state();
//			Log.d(TAG, "save_state = " + save_state);
//			Log.d(TAG, "pause_state = " + pause_state);
		}
		if (pause_state.equals("SAVING")) {
			return pause_state;
		} else if (pause_state.equals("DONE")) {
			// FIXME: We wait for 5 secs to complete the state save not ideal
			// for large OSes
			// we should find a way to detect when QMP is really done so we
			// don't get corrupt file states
			new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
				@Override
				public void run() {
					pausedVM();
				}
			}, 100);
			return pause_state;

		} else if (pause_state.equals("ERROR")) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    pausedErrorVM("Could not pause VM. View log file for details");
                }
            }, 100);
            return pause_state;
        }
		return save_state;
	}

	private static boolean fitToScreen = Config.enable_qemu_fullScreen;
	private static boolean stretchToScreen = false; // Start with fitToScreen

	// Setup
	protected void onCreate(Bundle savedInstanceState) {
		// Log.v("SDL", "onCreate()");
        activity = this;

		VectrasSDLActivity.activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		VectrasSDLActivity.activity.getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
						|View.SYSTEM_UI_FLAG_FULLSCREEN
						|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

		);

		super.onCreate(savedInstanceState);

		Log.v("SDL", "Max Mem = " + Runtime.getRuntime().maxMemory());
		this.handler = commandHandler;
		this.activity1 = this;

		// So we can call stuff from static callbacks
		mSingleton = this;

		createUI(0, 0);
        
        FrameLayout fragmentLayout = findViewById(R.id.fragmentLayout);
        
        setFragment(new ControlsFragment());

		// new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
		// @Override
		// public void run() {
		// UIUtils.setOrientation(activity);
		// }
		// }, 2000);

		UIUtils.showHints(this);

		this.resumeVM();

	}
    
    protected void setFragment(Fragment fragment) {
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.fragmentLayout, fragment);
        t.commit();
    }

	public SDLSurface getSDLSurface() {
		
		if (mSurface == null)
			mSurface = new SDLSurface(activity);
		return mSurface;
	}

	private void setScreenSize() {
		
		// WindowManager wm = (WindowManager) this
		// .getSystemService(Context.WINDOW_SERVICE);
		// Display display = wm.getDefaultDisplay();
		// this.screen_width = display.getWidth();
		// this.screen_height = display.getHeight();

	}

	private void createUI(int w, int h) {
		
		// Set up the surface
		mSurface = getSDLSurface();
		mSurface.setRenderer(new ClearRenderer());

		// mSurface.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		// setContentView(mSurface);

		int width = w;
		int height = h;
		if (width == 0) {
			width = RelativeLayout.LayoutParams.WRAP_CONTENT;
		}
		if (height == 0) {
			height = RelativeLayout.LayoutParams.WRAP_CONTENT;
		}

		setContentView(R.layout.main_sdl);

		RelativeLayout mLayout = (RelativeLayout) findViewById(R.id.sdl);
		RelativeLayout.LayoutParams surfaceParams = new RelativeLayout.LayoutParams(width, height);
		surfaceParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
		surfaceParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);

		mLayout.addView(mSurface, surfaceParams);

		SurfaceHolder holder = mSurface.getHolder();
		setScreenSize();
	}

	protected void onPause() {
		Log.v("SDL", "onPause()");
		super.onPause();

	}

	private void onKeyboard() {
		InputMethodManager inputMgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		// inputMgr.toggleSoftInput(0, 0);
		inputMgr.showSoftInput(this.mSurface, InputMethodManager.SHOW_FORCED);
	}

	public void onSelectMenuVol() {

		final AlertDialog alertDialog;
		alertDialog = new AlertDialog.Builder(activity).create();
		alertDialog.setTitle("Volume");

        LinearLayout.LayoutParams volParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

		LinearLayout t = createVolumePanel();
		t.setLayoutParams(volParams);

		ScrollView s = new ScrollView(activity);
		s.addView(t);
		alertDialog.setView(s);
		alertDialog.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				alertDialog.cancel();
			}
		});
		alertDialog.show();

	}

	public LinearLayout createVolumePanel() {
		LinearLayout layout = new LinearLayout (this);
		layout.setPadding(20, 20, 20, 20);

        LinearLayout.LayoutParams volparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

		SeekBar vol = new SeekBar(this);
		vol.setMax(maxVolume);
		int volume = getCurrentVolume();
		vol.setProgress(volume);
		vol.setLayoutParams(volparams);

		((SeekBar) vol).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar s, int progress, boolean touch) {
                setVolume(progress);
			}

			public void onStartTrackingTouch(SeekBar arg0) {

			}

			public void onStopTrackingTouch(SeekBar arg0) {

			}
		});

		layout.addView(vol);

		return layout;

	}

	protected void onResume() {
		Log.v("SDL", "onResume()");

		// if (status == null || status.equals("") || status.equals("DONE"))
		// SDLActivity.nativeResume();

		// mSurface.reSize();
		// new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
		// @Override
		// public void run() {
		// UIUtils.setOrientation(activity);
		// }
		// }, 1000);

		super.onResume();
		onMouse();
	}

	// static void resume() {
	// Log.v("Resume", "Resuming -> Full Screeen");
	// if (SDLActivityCommon.fitToScreen)
	// SDLActivityCommon.setFitToScreen();
	// if (SDLActivityCommon.stretchToScreen)
	// SDLActivityCommon.setStretchToScreen();
	// else
	// VectrasActivity.vmexecutor.toggleFullScreen();
	// }

	// Messages from the SDLMain thread
	static int COMMAND_CHANGE_TITLE = 1;
	static int COMMAND_SAVEVM = 2;

	public void loadLibraries() {
		// No loading of .so we do it outside
	}

	public void onRestartVM() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				if (MainActivity.vmexecutor != null) {
					Log.v(TAG, "Restarting the VM...");
					MainActivity.vmexecutor.stopvm(1);

				} else {
					Log.v(TAG, "Not running VM...");
				}
			}
		});
		t.start();
	}

	public void promptPause(final Activity activity) {

		final AlertDialog alertDialog;
		alertDialog = new AlertDialog.Builder(activity).create();
		alertDialog.setTitle("Pause VM");
		TextView stateView = new TextView(activity);
		stateView.setText("This make take a while depending on the RAM size used");
		stateView.setPadding(10, 10, 10, 10);
		alertDialog.setView(stateView);

		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Pause", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				onPauseVM();
				return;
			}
		});
		alertDialog.show();
	}

	public void startSaveVMListener() {
		stopTimeListener();
		timeQuit = false;
		try {
			Log.v("Listener", "Time Listener Started...");
			timeListener();
			synchronized (lockTime) {
				while (timeQuit == false) {
					lockTime.wait();
				}
				lockTime.notifyAll();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			Log.v("SaveVM", "Time listener thread error: " + ex.getMessage());
		}
//		new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//			@Override
//			public void run() {
//				Toast.makeText(getApplicationContext(), "VM State saved", Toast.LENGTH_LONG).show();
//			}
//		}, 1000);

		Log.v("Listener", "Time listener thread exited...");

	}

	// Currently not working due to SDL can only support 1 window for Android
	private void onHMP() {
		monitorMode = true;
		sendCtrlAtlKey(KeyEvent.KEYCODE_2);

	}
	// private void onPauseVM() {
	// Thread t = new Thread(new Runnable() {
	// public void run() {
	// // Delete any previous state file
	// if (VectrasActivity.vmexecutor.save_state_name != null) {
	// File file = new File(VectrasActivity.vmexecutor.save_state_name);
	// if (file.exists()) {
	// file.delete();
	// }
	// }
	//
	// VectrasActivity.vmexecutor.paused = 1;
	// ((VectrasActivity) VectrasActivity.activity).saveStateVMDB();
	//
	// onHMP();
	// new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
	// @Override
	// public void run() {
	// Toast.makeText(getApplicationContext(), "Please wait while saving VM
	// State", Toast.LENGTH_LONG)
	// .show();
	// }
	// }, 500);
	// try {
	// Thread.sleep(500);
	// } catch (InterruptedException ex) {
	// Logger.getLogger(VectrasVNCActivity.class.getName()).log(Level.SEVERE,
	// null, ex);
	// }
	//
	// String commandStop = "stop\n";
	// for (int i = 0; i < commandStop.length(); i++)
	// sendText(commandStop.charAt(i) + "");
	//
	// String commandMigrate = "migrate fd:"
	// +
	// VectrasActivity.vmexecutor.get_fd(VectrasActivity.vmexecutor.save_state_name)
	// + "\n";
	// for (int i = 0; i < commandMigrate.length(); i++)
	// sendText(commandMigrate.charAt(i) + "");
	//
	// new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
	// @Override
	// public void run() {
	// VMListener a = new VMListener();
	// a.execute();
	// }
	// }, 0);
	// }
	// });
	// t.start();
	//
	// }

	private void onPauseVM() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				// Delete any previous state file
				if ("VECTRAS" != null) {
					File file = new File("VECTRAS");
					if (file.exists()) {
						file.delete();
					}
				}

				new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(), "Please wait while saving VM State", Toast.LENGTH_SHORT)
								.show();
					}
				}, 0);

				String uri = "fd:" + MainActivity.vmexecutor.get_fd("VECTRAS");
				String command = QmpClient.stop();
				String msg = QmpClient.sendCommand(command);
				if (msg != null)
					Log.i(TAG, msg);
				command = QmpClient.migrate(false, false, uri);
				msg = QmpClient.sendCommand(command);
				if (msg != null) {
                    Log.i(TAG, msg);
                    processMigrationResponse(msg);
                }

				// XXX: We cant be sure that the machine state is completed
				// saving
				// new Handler(Looper.getMainLooper()).postDelayed(new
				// Runnable() {
				// @Override
				// public void run() {
				// pausedVM();
				// }
				// }, 1000);

				// XXX: Instead we poll to see if migration is complete
				new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
					@Override
					public void run() {
						VMListener a = new VMListener();
						a.execute();
					}
				}, 0);
				VectrasStatus.logInfo(String.format("VMPaused"));
			}
		});
		t.start();

	}

    private void processMigrationResponse(String response) {
        String errorStr = null;
        try {
            JSONObject object = new JSONObject(response);
            errorStr = object.getString("error");
        }catch (Exception ex) {

        }
            if (errorStr != null) {
                String descStr = null;

                try {
                    JSONObject descObj = new JSONObject(errorStr);
                    descStr = descObj.getString("desc");
                }catch (Exception ex) {

                }
                final String descStr1 = descStr;

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pausedErrorVM(descStr1!=null?descStr1:"Could not pause VM. View log for details");
                    }
                }, 100);

            }

    }

    private class VMListener extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {
			startSaveVMListener();
			return null;
		}

		@Override
		protected void onPostExecute(Void test) {
			// if (progDialog.isShowing()) {
			// progDialog.dismiss();
			// }

		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean res = this.mSurface.onTouchProcess(this.mSurface, event);
		res = this.mSurface.onTouchEventProcess(event);
		return false;
	}

	private void resumeVM() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				if (MainActivity.vmexecutor.paused == 1) {

					MainActivity.vmexecutor.paused = 0;
					// new Handler(Looper.getMainLooper()).postDelayed(new
					// Runnable() {
					// @Override
					// public void run() {
					// Toast.makeText(getApplicationContext(), "Please wait
					// while resuming VM State",
					// Toast.LENGTH_SHORT).show();
					// }
					// }, 500);

					String command = QmpClient.cont();
					String msg = QmpClient.sendCommand(command);
					if (msg != null)
						Log.i(TAG, msg);
					new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
						@Override
						public void run() {
							onMouse();
						}
					}, 500);
				}
				VectrasStatus.logInfo(String.format("VMResumed"));
			}
		});
		t.start();

	}

	public static void stop() {
		// Log.d(TAG, "Pressed Back");

		// super.onBackPressed();
		stopVM(activity, false);
	}

	public void onBackPressed() {
		// Log.d(TAG, "Pressed Back");

		// super.onBackPressed();
		stopVM(activity, false);
	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.supportInvalidateOptionsMenu();
    }
}
