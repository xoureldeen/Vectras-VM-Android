package com.vectras.qemu;

import android.androidVNC.AbstractScaling;
import android.androidVNC.VncCanvas;
import android.androidVNC.VncCanvasActivity;
import androidx.appcompat.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Instrumentation;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.vectras.vm.R;
import com.vectras.qemu.utils.FileUtils;
import com.vectras.qemu.utils.Machine;
import com.vectras.qemu.utils.QmpClient;
import com.vectras.qemu.utils.UIUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import androidx.core.view.MenuItemCompat;


/**
 * 
 * @author Dev
 */
public class MainVNCActivity extends VncCanvasActivity {

	public static final int KEYBOARD = 10000;
	public static final int QUIT = 10001;
	public static final int HELP = 10002;
	private static boolean monitorMode = false;
	private boolean mouseOn = false;
	private Object lockTime = new Object();
	private boolean timeQuit = false;
	private Thread timeListenerThread;
	private ProgressDialog progDialog;
	private static boolean firstConnection;

	@Override
	public void onCreate(Bundle b) {

		if (MainSettingsManager.getFullscreen(this))
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);

		super.onCreate(b);

		this.vncCanvas.setFocusableInTouchMode(true);

		setDefaulViewMode();

        setControls();
	}


    private Timer _timer = new Timer();
    private TimerTask t;

    public static final int MAX_LINES = 1;
    public static final String TWO_SPACES = " ";
    public static LinearLayout BtnShutdown, kbdBtn, BtnMode, BtnHide;

    public static TextView txtCpu, txtMem, txtDev;
    public static ImageView ivHide, ivMode;

    ProcessBuilder processBuilder;
    String Holder = "";
    String[] DATA = {"/system/bin/cat", "/proc/cpuinfo"};
    InputStream inputStream;
    Process process;
    byte[] byteArry;

    private void setControls() {
        BtnShutdown = findViewById(R.id.shutdownBtn);
        BtnMode = findViewById(R.id.modeBtn);
        BtnHide = findViewById(R.id.hideBtn);
        kbdBtn = findViewById(R.id.kbdBtn);
        txtMem = findViewById(R.id.ramTxt);
        txtDev = findViewById(R.id.dvcTxt);
        txtCpu = findViewById(R.id.cpuTxt);

        ivHide = findViewById(R.id.ivHide);
        ivMode = findViewById(R.id.ivMode);

        txtDev.setText("Device Model: " + android.os.Build.MODEL);

        t = new TimerTask() {
            @Override
            public void run() {
                MainVNCActivity.activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //update
                        ActivityManager.MemoryInfo miI = new ActivityManager.MemoryInfo();
                        ActivityManager activityManagerr = (ActivityManager) MainVNCActivity.activity.getSystemService(MainVNCActivity.activity.ACTIVITY_SERVICE);
                        activityManagerr.getMemoryInfo(miI);
                        //update textview here
                        long freeMemory = miI.availMem / 1048576L;
                        long totalMemory = miI.totalMem / 1048576L;
                        long usedMemory = totalMemory - freeMemory;

                        txtMem.setText("Free Memory: " + freeMemory + " MB");
                    }
                });
            }
        };
        _timer.scheduleAtFixedRate(t, (int) (0), (int) (1000));

        //txtMem.setVisibility(View.GONE);
        //txtCpu.setVisibility(View.GONE);
        byteArry = new byte[1024];

        try {
            processBuilder = new ProcessBuilder(DATA);

            process = processBuilder.start();

            inputStream = process.getInputStream();

            while (inputStream.read(byteArry) != -1) {

                Holder = Holder + new String(byteArry);
            }

            inputStream.close();

        } catch (IOException ex) {

            ex.printStackTrace();
        }

        String cpuTxt = "Cpu Info:\n" + Holder;

        txtCpu.setText(cpuTxt);
        txtCpu.post(() -> {
            // Past the maximum number of lines we want to display.
            if (txtCpu.getLineCount() > MAX_LINES) {
                int lastCharShown = txtCpu.getLayout().getLineVisibleEnd(MAX_LINES - 1);

                txtCpu.setMaxLines(MAX_LINES);

                String moreString = "Show More";
                String suffix = TWO_SPACES + moreString;

                // 3 is a "magic number" but it's just basically the length of the ellipsis we're going to insert
                String actionDisplayText = cpuTxt.substring(0, lastCharShown - suffix.length() - 3) + "..." + suffix;

                SpannableString truncatedSpannableString = new SpannableString(actionDisplayText);
                int startIndex = actionDisplayText.indexOf(moreString);
                truncatedSpannableString.setSpan(new ForegroundColorSpan(Color.GRAY), startIndex, startIndex + moreString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                txtCpu.setText(truncatedSpannableString);
            }
        });

        txtCpu.setVisibility(View.GONE);

        kbdBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        toggleKeyboardFlag = UIUtils.onKeyboard(activity, toggleKeyboardFlag, vncCanvas);
                    }
                }, 200);
            }
        });

    }

    private void setDefaulViewMode() {
		

		// Fit to Screen
		AbstractScaling.getById(R.id.itemFitToScreen).setScaleTypeForActivity(this);
		showPanningState();

//        screenMode = VNCScreenMode.FitToScreen;
		setLayout(getResources().getConfiguration());

        //UIUtils.setOrientation(this);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setLayout(newConfig);
	}

	public enum VNCScreenMode {
	    Normal,
        FitToScreen,
        Fullscreen //fullscreen not implemented yet
    }

    public static VNCScreenMode screenMode = VNCScreenMode.FitToScreen;

	private void setLayout(Configuration newConfig) {

        boolean isLanscape =
                (newConfig!=null && newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
                || UIUtils.isLandscapeOrientation(this);

        View vnc_canvas_layout = (View) this.findViewById(R.id.vnc_canvas_layout);
        RelativeLayout.LayoutParams vnc_canvas_layout_params = null;
        RelativeLayout.LayoutParams vnc_params = null;
        //normal 1-1
        if(screenMode == VNCScreenMode.Normal) {
            if (isLanscape) {
                vnc_params = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
//                vnc_params.addRule(RelativeLayout.CENTER_IN_PARENT);
                vnc_params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                vnc_params.addRule(RelativeLayout.ALIGN_PARENT_TOP);

                vnc_canvas_layout_params = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
//                vnc_canvas_layout_params.addRule(RelativeLayout.CENTER_IN_PARENT);
                vnc_canvas_layout_params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                vnc_canvas_layout_params.addRule(RelativeLayout.ALIGN_PARENT_TOP);

            } else {
                vnc_params = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                vnc_params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                vnc_params.addRule(RelativeLayout.ALIGN_PARENT_TOP);

                vnc_canvas_layout_params = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                vnc_canvas_layout_params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                vnc_canvas_layout_params.addRule(RelativeLayout.CENTER_HORIZONTAL);
            }
        } else {
            //fittoscreen
            if (isLanscape) {
                vnc_params = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
                vnc_params.addRule(RelativeLayout.CENTER_IN_PARENT);
                vnc_canvas_layout_params = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
                vnc_canvas_layout_params.addRule(RelativeLayout.CENTER_IN_PARENT);
            } else {
                final Display display = getWindow().getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);

                int h = ViewGroup.LayoutParams.WRAP_CONTENT;
                if(vncCanvas!=null && vncCanvas.rfb!=null
                        && vncCanvas.rfb.framebufferWidth != 0
                && vncCanvas.rfb.framebufferHeight != 0) {
                    h = size.x * vncCanvas.rfb.framebufferHeight / vncCanvas.rfb.framebufferWidth;
                }
                vnc_params = new RelativeLayout.LayoutParams(
                        size.x,
                        h
                );
                vnc_params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                vnc_params.addRule(RelativeLayout.ALIGN_PARENT_TOP);

                vnc_canvas_layout_params = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                vnc_canvas_layout_params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                vnc_canvas_layout_params.addRule(RelativeLayout.CENTER_HORIZONTAL);
            }
        }
        this.vncCanvas.setLayoutParams(vnc_params);
        vnc_canvas_layout.setLayoutParams(vnc_canvas_layout_params);

        this.invalidateOptionsMenu();
	}

	public void stopTimeListener() {
		Log.v(TAG, "Stopping Listener");
		synchronized (this.lockTime) {
			this.timeQuit = true;
			this.lockTime.notifyAll();
		}
	}

	public void onDestroy() {
		super.onDestroy();
		this.stopTimeListener();

	}

	public void onPause() {
	    MainService.updateServiceNotification(Config.machinename + ": VM Running in Background");
		super.onPause();
	}

	public void onResume() {
	    MainService.updateServiceNotification(Config.machinename + ": VM Running");
		super.onResume();
	}

	public void checkStatus() {
		while (timeQuit != true) {
			MainActivityCommon.VMStatus status = Machine.checkSaveVMStatus(activity);
			Log.v(TAG, "Status: " + status);
			if (status == MainActivityCommon.VMStatus.Unknown
				|| status == MainActivityCommon.VMStatus.Completed
				|| status == MainActivityCommon.VMStatus.Failed
                    ) {
				//Log.v(TAG, "Saving state is done: " + status);
				stopTimeListener();
				return;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
				Log.w("SaveVM", "Interrupted");
			}
		}
		Log.v("SaveVM", "Save state complete");

	}

	public void startSaveVMListener() {
		this.stopTimeListener();
		timeQuit = false;
		try {
			Log.v("Listener", "Time Listener Started...");
			checkStatus();
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

	String TAG = "MainVNCActivity";

//	@Override
//	public boolean onOptionsItemSelected(final MenuItem item) {
//		super.onOptionsItemSelected(item);
//		if (item.getItemId() == this.KEYBOARD || item.getItemId() == R.id.itemKeyboard) {
//			new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    toggleKeyboardFlag = UIUtils.onKeyboard(activity, toggleKeyboardFlag, vncCanvas);
//                }
//            }, 200);
//		} else if (item.getItemId() == R.id.itemReset) {
//			Machine.resetVM(activity);
//		} else if (item.getItemId() == R.id.itemShutdown) {
//		    UIUtils.hideKeyboard(this, vncCanvas);
//			Machine.stopVM(activity);
//		} else if (item.getItemId() == R.id.itemMonitor) {
//			if (this.monitorMode) {
//				this.onVNC();
//			} else {
//				this.onMonitor();
//			}
//		} else if (item.getItemId() == R.id.itemSaveState) {
//			this.promptPause(activity);
//		} else if (item.getItemId() == R.id.itemSaveSnapshot) {
//			
//		} else if (item.getItemId() == R.id.itemFitToScreen) {
//			return onFitToScreen();
//		} else if (item.getItemId() == R.id.itemFullScreen) {
//			return toggleFullScreen();
//		} else if (item.getItemId() == this.QUIT) {
//		} else if (item.getItemId() == R.id.itemCenterMouse) {
//            onMouseMode();
//		} else if (item.getItemId() == R.id.itemCalibrateMouse) {
//            calibration();
//        }
//        else if (item.getItemId() == R.id.itemHelp) {
//			UIUtils.onHelp(this);
//		} else if (item.getItemId() == R.id.itemHideToolbar) {
//            this.onHideToolbar();
//        } else if (item.getItemId() == R.id.itemDisplay) {
//            this.onSelectMenuVNCDisplay();
//        } else if (item.getItemId() == R.id.itemViewLog) {
//            this.onViewLog();
//        }
//
//        this.invalidateOptionsMenu();
//
//		return true;
//	}

    private void onMouseMode() {

        String [] items = {"Trackpad Mouse (Phone)",
				"Bluetooth/USB Mouse (Desktop mode)", //Physical mouse for Chromebook, Android x86 PC, or Bluetooth Mouse
        };
        final AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        mBuilder.setTitle("Mouse");
        mBuilder.setSingleChoiceItems(items, Config.mouseMode.ordinal(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch(i){
                    case 0:
                        setUIModeMobile(true);
                        break;
                    case 1:
                        promptSetUIModeDesktop(MainVNCActivity.this, false);
                        break;
                    default:
                        break;
                }
                dialog.dismiss();
            }
        });
        final AlertDialog alertDialog = mBuilder.create();
        alertDialog.show();

    }

	public boolean checkVMResolutionFits() {
		if(vncCanvas.rfb.framebufferWidth < vncCanvas.getWidth()
				&& vncCanvas.rfb.framebufferHeight < vncCanvas.getHeight())
			return true;

		return false;
	}
    private void onDisplayMode() {

        String [] items = {
                "Normal (One-To-One)",
                "Fit To Screen"
                //"Full Screen" //Stretched
        };
        int currentScaleType = vncCanvas.getScaleType() == ImageView.ScaleType.FIT_CENTER? 1 : 0;

        final AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        mBuilder.setTitle("Display Mode");
        mBuilder.setSingleChoiceItems(items, currentScaleType, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch(i){
                    case 0:
                        onNormalScreen();
                        onMouse();
                        break;
                    case 1:
                        if(Config.mouseMode == Config.MouseMode.External){
                            UIUtils.toastShort(MainVNCActivity.this, "Fit to Screen disabled under Desktop mode");
                            dialog.dismiss();
                            return;
                        }
                        onFitToScreen();
                        onMouse();
                        break;
                    default:
                        break;
                }
                dialog.dismiss();
            }
        });
        final AlertDialog alertDialog = mBuilder.create();
        alertDialog.show();

    }

    private void setUIModeMobile(boolean fitToScreen){

        try {
            MotionEvent a = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);

            Config.mouseMode = Config.MouseMode.Trackpad;
            MainSettingsManager.setDesktopMode(this, false);
            if(fitToScreen)
                onFitToScreen();
            else
                onNormalScreen();
            onMouse();

            //UIUtils.toastShort(MainVNCActivity.this, "Trackpad Calibrating");
            invalidateOptionsMenu();
        } catch (Exception ex) {
            if(Config.debug)
                ex.printStackTrace();
        }
        }

    private void promptSetUIModeDesktop(final Activity activity, final boolean mouseMethodAlt) {


        final AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(activity).create();
        alertDialog.setTitle("Desktop mode");

        LinearLayout mLayout = new LinearLayout(this);
        mLayout.setPadding(20,20,20,20);
        mLayout.setOrientation(LinearLayout.VERTICAL);

        TextView textView = new TextView(activity);
        textView.setVisibility(View.VISIBLE);

        String desktopInstructions = this.getString(R.string.desktopInstructions);
        if(!checkVMResolutionFits()){
			String resolutionWarning = "Warning: Machine resolution "
					+ vncCanvas.rfb.framebufferWidth + "x" + vncCanvas.rfb.framebufferHeight +
					" is too high for Desktop Mode. " +
					"Scaling will be used and Mouse Alignment will not be accurate. " +
					"Reduce display resolution for better experience\n\n";
			desktopInstructions = resolutionWarning + desktopInstructions;
		}
        textView.setText(desktopInstructions);

        LinearLayout.LayoutParams textViewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(textView);
        mLayout.addView(scrollView, textViewParams);
        alertDialog.setView(mLayout);

        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                setUIModeDesktop();
                alertDialog.dismiss();
            }
        });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });
        alertDialog.show();

    }

    private void setUIModeDesktop() {

	    try {
            MotionEvent a = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);
            Config.mouseMode = Config.MouseMode.External;
            MainSettingsManager.setDesktopMode(this, true);
            if(Config.showToast)
                    UIUtils.toastShort(MainVNCActivity.this, "External Mouse Enabled");
            fullScreen();
            showPanningState();

            onMouse();
        } catch (Exception e) {
	        if(Config.debug)
	            e.printStackTrace();
        }
        //vncCanvas.reSize(false);
        invalidateOptionsMenu();
	}


    public void onViewLog() {
        FileUtils.viewVectrasLog(this);
    }

	public void setContentView() {
		
		setContentView(R.layout.activity_vnc);

	}

	private boolean toggleFullScreen() {
		
        UIUtils.toastShort(this, "VNC Fullscreen not supported");

		return false;
	}

	private boolean onFitToScreen() {

	    try {
            //UIUtils.setOrientation(this);
            ActionBar bar = this.getSupportActionBar();
            inputHandler = getInputHandlerById(R.id.itemInputTouchpad);
            connection.setInputMode(inputHandler.getName());
            connection.setFollowMouse(true);
            mouseOn = true;
            AbstractScaling.getById(R.id.itemFitToScreen).setScaleTypeForActivity(this);
            showPanningState();
            screenMode = VNCScreenMode.FitToScreen;
            setLayout(null);

            return true;
        } catch (Exception ex) {
	        if(Config.debug)
	            ex.printStackTrace();
        }
        return false;
	}

    private boolean onNormalScreen() {

	    try {
            //Force only landscape
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            ActionBar bar = MainVNCActivity.this.getSupportActionBar();
            if (bar != null) {
                bar.show();
            }

            inputHandler = getInputHandlerById(R.id.itemInputTouchpad);
            connection.setInputMode(inputHandler.getName());
            connection.setFollowMouse(true);
            mouseOn = true;
            AbstractScaling.getById(R.id.itemOneToOne).setScaleTypeForActivity(this);
            showPanningState();
            screenMode = VNCScreenMode.Normal;
            setLayout(null);

            return true;
        } catch (Exception ex) {
	        if(Config.debug)
	            ex.printStackTrace();
        } finally {

        }
        return false;
    }

	private boolean onMouse() {

		// Vectras: For now we disable other modes
        if(Config.disableMouseModes)
		    mouseOn = false;

		
		if (mouseOn == false) {
			inputHandler = getInputHandlerById(R.id.itemInputTouchpad);
			connection.setInputMode(inputHandler.getName());
			connection.setFollowMouse(true);
			mouseOn = true;
		} else {
			// XXX: Vectras
			// we disable panning for now
			// input1 = getInputHandlerById(R.id.itemFitToScreen);
			// input1 = getInputHandlerById(R.id.itemInputTouchPanZoomMouse);
			// connection.setFollowMouse(false);
			// mouseOn = false;
		}

		//Start calibration
        calibration();

		return true;
	}

	//XXX: We need to adjust the mouse inside the Guest
    // This is a known issue with QEMU under VNC mode
    // this only fixes things temporarily.
    // There is a workaround to choose USB Tablet for mouse emulation
    // though it might not work for all Guest OSes
    public void calibration() {
        Thread t = new Thread(new Runnable() {
            public void run() {
            try {

                int origX = vncCanvas.mouseX;
                int origY = vncCanvas.mouseY;
                MotionEvent event = null;

                for(int i = 0; i< 4*20; i++) {
                    int x = 0+i*50;
                    int y = 0+i*50;
                    if(i%4==1){
                        x=vncCanvas.rfb.framebufferWidth;
                    }else if (i%4==2) {
                        y=vncCanvas.rfb.framebufferHeight;
                    }else if (i%4==3) {
                        x=0;
                    }

                    event = MotionEvent.obtain(SystemClock.uptimeMillis(),
                            SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE,
                            x,y, 0);
                    Thread.sleep(10);
                    vncCanvas.processPointerEvent(event, false, false);


                }

                Thread.sleep(50);
                event = MotionEvent.obtain(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE,
                        origX,origY, 0);
                vncCanvas.processPointerEvent(event, false, false);

            }catch(Exception ex) {

            }
            }
        });
        t.start();
    }

//	@Override
//	public boolean onPrepareOptionsMenu(Menu menu) {
//		menu.clear();
//		return this.setupMenu(menu);
//	}
//
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		menu.clear();
//		return this.setupMenu(menu);
//
//	}
//
//	public boolean setupMenu(Menu menu) {
//		getMenuInflater().inflate(R.menu.vnccanvasactivitymenu, menu);
//
//        int maxMenuItemsShown = 4;
//        int actionShow = MenuItemCompat.SHOW_AS_ACTION_IF_ROOM;
//        if(UIUtils.isLandscapeOrientation(this)) {
//            maxMenuItemsShown = 6;
//            actionShow = MenuItemCompat.SHOW_AS_ACTION_ALWAYS;
//        }
//
//		if (vncCanvas.scaling != null) {
//			menu.findItem(vncCanvas.scaling.getId()).setChecked(true);
//		}
//
//		if (this.monitorMode) {
//			menu.findItem(R.id.itemMonitor).setTitle("VM Display");
//            menu.findItem(R.id.itemMonitor).setIcon(R.mipmap.ic_launcher);
//
//		} else {
//			menu.findItem(R.id.itemMonitor).setTitle("QEMU Monitor");
//            menu.findItem(R.id.itemMonitor).setIcon(R.drawable.round_terminal_24);
//
//		}
//
//		//XXX: We don't need these for now
//		menu.removeItem(menu.findItem(R.id.itemEnterText).getItemId());
//		menu.removeItem(menu.findItem(R.id.itemSendKeyAgain).getItemId());
//		menu.removeItem(menu.findItem(R.id.itemSpecialKeys).getItemId());
//		menu.removeItem(menu.findItem(R.id.itemInputMode).getItemId());
//        menu.removeItem(menu.findItem(R.id.itemScaling).getItemId());
//        menu.removeItem(menu.findItem(R.id.itemCtrlAltDel).getItemId());
//        menu.removeItem(menu.findItem(R.id.itemCtrlC).getItemId());
//        menu.removeItem(menu.findItem(R.id.itemColorMode).getItemId());
//        menu.removeItem(menu.findItem(R.id.itemFullScreen).getItemId());
//
//        if (MainSettingsManager.getAlwaysShowMenuToolbar(activity) || Config.mouseMode == Config.MouseMode.External) {
//            menu.removeItem(menu.findItem(R.id.itemHideToolbar).getItemId());
//            maxMenuItemsShown--;
//        }
//
//		// Menu inputMenu = menu.findItem(R.id.itemInputMode).getSubMenu();
//		//
//		// inputModeMenuItems = new MenuItem[inputModeIds.length];
//		// for (int i = 0; i < inputModeIds.length; i++) {
//		// inputModeMenuItems[i] = inputMenu.findItem(inputModeIds[i]);
//		// }
//		// updateInputMenu();
//		// menu.removeItem(menu.findItem(R.id.itemCenterMouse).getItemId());
//
//		// Vectras: Disable Panning for now
//		// if (this.mouseOn) {
//		// menu.findItem(R.id.itemCenterMouse).setTitle("Pan (Mouse Off)");
//		// menu.findItem(R.id.itemCenterMouse).setIcon(R.drawable.pan);
//		// } else {
//		menu.findItem(R.id.itemCenterMouse).setTitle("Mouse");
//		menu.findItem(R.id.itemCenterMouse).setIcon(R.drawable.round_mouse_24);
//		//
//		// }
//
//
//		for (int i = 0; i < menu.size() && i < maxMenuItemsShown; i++) {
//			MenuItemCompat.setShowAsAction(menu.getItem(i), actionShow);
//		}
//
//		return true;
//
//	}





	public static boolean toggleKeyboardFlag = true;

	private void onMonitor() {
        if(Config.showToast)
            UIUtils.toastShort(this, "Connecting to QEMU Monitor");

        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                monitorMode = true;
                vncCanvas.sendMetaKey1(50, 6);

            }
        });
        t.start();
	}

	private void onVNC() {
        UIUtils.toastShort(this, "Connecting to VM");

        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                monitorMode = false;
                vncCanvas.sendMetaKey1(49, 6);
            }
        });
        t.start();


	}

	// FIXME: We need this to able to catch complex characters strings like
	// grave and send it as text
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_MULTIPLE && event.getKeyCode() == KeyEvent.KEYCODE_UNKNOWN) {
			vncCanvas.sendText(event.getCharacters().toString());
			return true;
		} else
			return super.dispatchKeyEvent(event);

	}

	private void resumeVM() {
		if(MainActivityCommon.vmexecutor == null){
			return;
		}
		Thread t = new Thread(new Runnable() {
			public void run() {
				if (MainActivityCommon.vmexecutor.paused == 1) {
					try {
						Thread.sleep(4000);
					} catch (InterruptedException ex) {
						Logger.getLogger(MainVNCActivity.class.getName()).log(Level.SEVERE, null, ex);
					}
					if(vncCanvas == null)
					    return;

					MainActivityCommon.vmexecutor.paused = 0;
					String command = QmpClient.cont();
					String msg = QmpClient.sendCommand(command);
					new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
						@Override
						public void run() {
                            setUIModeMobile(screenMode == VNCScreenMode.FitToScreen);
						}
					}, 500);

				}
			}
		});
		t.start();

	}

	private void onPauseVM() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				// Delete any previous state file
				if (MainActivityCommon.vmexecutor.save_state_name != null) {
					File file = new File(MainActivityCommon.vmexecutor.save_state_name);
					if (file.exists()) {
						file.delete();
					}
				}

				UIUtils.toastShort(getApplicationContext(), "Please wait while saving VM State");

				String uri = "fd:" + MainActivityCommon.vmexecutor.get_fd(MainActivityCommon.vmexecutor.save_state_name);
				String command = QmpClient.stop();
				String msg = QmpClient.sendCommand(command);
//				if (msg != null)
//					Log.i(TAG, msg);
				command = QmpClient.migrate(false, false, uri);
				msg = QmpClient.sendCommand(command);
				if (msg != null) {
//					Log.i(TAG, msg);
					processMigrationResponse(msg);
				}

				new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
					@Override
					public void run() {
						VMListener a = new VMListener();
						a.execute();
					}
				}, 0);
			}
		});
		t.start();

	}

	private void processMigrationResponse(String response) {
		String errorStr = null;

		if(response.contains("error")) {
            try {
                JSONObject object = new JSONObject(response);
                errorStr = object.getString("error");
            } catch (Exception ex) {
                if (Config.debug)
                    ex.printStackTrace();
            }
        }
		if (errorStr != null && errorStr.contains("desc")) {
			String descStr = null;

			try {
				JSONObject descObj = new JSONObject(errorStr);
				descStr = descObj.getString("desc");
			}catch (Exception ex) {
				if(Config.debug)
					ex.printStackTrace();
			}
			final String descStr1 = descStr;

			new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
				@Override
				public void run() {
					Machine.pausedErrorVM(activity, descStr1);
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

	private void fullScreen() {
		AbstractScaling.getById(R.id.itemFitToScreen).setScaleTypeForActivity(this);
		showPanningState();
	}

	public void promptPause(final Activity activity) {
		final AlertDialog alertDialog;
		alertDialog = new AlertDialog.Builder(activity).create();
		alertDialog.setTitle("Pause VM");
		TextView stateView = new TextView(activity);
		stateView.setText("This make take a while depending on the RAM size used");
		stateView.setPadding(20, 20, 20, 20);
		alertDialog.setView(stateView);

		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Pause", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
					onPauseVM();
				return;
			}
		});
		alertDialog.show();

	}

	public void onBackPressed() {
        onStop();
		super.onBackPressed();

	}

	public void onHideToolbar(){
			ActionBar bar = this.getSupportActionBar();
			if (bar != null) {
					bar.hide();
			}
    }

	@Override
	public void onConnected() {
        this.resumeVM();
        Config.paused = 0;
        firstConnection = true;

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {

                if(Config.mouseMode == Config.MouseMode.External)
                    setUIModeDesktop();
                else
                    setUIModeMobile(screenMode == VNCScreenMode.FitToScreen);
            }
        },1000);

	}

    public void onSelectMenuVNCDisplay() {

        final AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(activity).create();
        alertDialog.setTitle("Display");

        LinearLayout.LayoutParams volParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        LinearLayout t = createVNCDisplayPanel();
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


    public LinearLayout createVNCDisplayPanel() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int currRate = getCurrentVNCRefreshRate();

        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        Button displayMode = new Button (this);

        displayMode.setText("Display Mode");
        displayMode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onDisplayMode();
            }
        });
        buttonsLayout.addView(displayMode);


        Button colors = new Button(this);
        colors.setText("Color Mode");
        colors.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                selectColorModel();

            }
        });
        buttonsLayout.addView(colors);

        layout.addView(buttonsLayout);

        final TextView value = new TextView(this);
        value.setText("Display Refresh Rate: " + currRate+" Hz");
        layout.addView(value);
        value.setLayoutParams(params);

        SeekBar rate = new SeekBar(this);
        rate.setMax(Config.MAX_DISPLAY_REFRESH_RATE);

        rate.setProgress(currRate);
        rate.setLayoutParams(params);

        ((SeekBar) rate).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar s, int progress, boolean touch) {
                value.setText("Refresh Rate: " + (progress+1)+" Hz");
            }

            public void onStartTrackingTouch(SeekBar arg0) {

            }

            public void onStopTrackingTouch(SeekBar arg0) {
                int progress = arg0.getProgress()+1;
				int refreshMs = 1000 / progress;
				Log.v(TAG, "Changing display refresh rate (ms): " + refreshMs);
                MainActivityCommon.vmexecutor.setvncrefreshrate(refreshMs);

            }
        });


        layout.addView(rate);

        return layout;

    }
    public int getCurrentVNCRefreshRate() {
        return 1000 / MainActivityCommon.vmexecutor.getvncrefreshrate();
    }

    private void sendCommand(int KeyCode) {
        new Thread() {
            @Override
            public void run() {
                try {
                    Instrumentation inst = new Instrumentation();
                    inst.sendKeyDownUpSync(KeyCode);
                } catch (Exception e) {
                    Log.e("SendKey", e.toString());
                }
            }

        }.start();
    }
}
