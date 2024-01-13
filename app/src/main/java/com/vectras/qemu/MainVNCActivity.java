package com.vectras.qemu;

import android.androidVNC.AbstractScaling;
import android.androidVNC.VncCanvasActivity;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.BaseInputConnection;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.vectras.qemu.utils.FileUtils;
import com.vectras.vm.Fragment.ControlersOptionsFragment;
import com.vectras.vm.MainActivity;
import com.vectras.vm.R;
import com.vectras.qemu.utils.Machine;
import com.vectras.qemu.utils.QmpClient;
import com.vectras.vm.utils.UIUtils;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;


/**
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
    String[] functionsArray = {"F1", "F2", "F3", "F4",
            "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12"};

    public boolean ctrlClicked = false;
    public boolean altClicked = false;
    private ImageButton qmpBtn;

    @Override
    public void onCreate(Bundle b) {

        if (MainSettingsManager.getFullscreen(this))
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(b);

        this.vncCanvas.setFocusableInTouchMode(true);

        setDefaulViewMode();

//        setUIModeMobile();

        Toolbar mainToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mainToolbar);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        onFitToScreen();

        ImageButton shutdownBtn = findViewById(R.id.shutdownBtn);
        ImageButton settingBtn = findViewById(R.id.btnSettings);
        ImageButton keyboardBtn = findViewById(R.id.kbdBtn);
        ImageButton controllersBtn = findViewById(R.id.btnMode);
        ImageButton upBtn = findViewById(R.id.upBtn);
        ImageButton leftBtn = findViewById(R.id.leftBtn);
        ImageButton downBtn = findViewById(R.id.downBtn);
        ImageButton rightBtn = findViewById(R.id.rightBtn);
        ImageButton enterBtn = findViewById(R.id.enterBtn);
        ImageButton escBtn = findViewById(R.id.escBtn);
        ImageButton ctrlBtn = findViewById(R.id.ctrlBtn);
        ImageButton altBtn = findViewById(R.id.altBtn);
        ImageButton delBtn = findViewById(R.id.delBtn);
        ImageButton btnLogs = findViewById(R.id.btnLogs);
        Button ctrlAltDelBtn = findViewById(R.id.ctrlAltDelBtn);
        qmpBtn = findViewById(R.id.btnQmp);
        btnLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileUtils.viewVectrasLog(activity);
            }
        });
        shutdownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Machine.stopVM(activity);
            }
        });
        keyboardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        toggleKeyboardFlag = UIUtils.onKeyboard(activity, toggleKeyboardFlag, vncCanvas);
                    }
                }, 200);
            }
        });
        controllersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                // Create and show the dialog.
                ControlersOptionsFragment newFragment = new ControlersOptionsFragment();
                newFragment.show(ft, "Controllers");
            }
        });
        settingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog alertDialog = new Dialog(activity, R.style.MainDialogTheme);
                alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                alertDialog.setContentView(R.layout.dialog_setting);
                alertDialog.show();
            }
        });
        upBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    sendKey(KeyEvent.KEYCODE_DPAD_UP, false);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendKey(KeyEvent.KEYCODE_DPAD_UP, true);
                    return true;
                }
                return false;
            }
        });
        leftBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    sendKey(KeyEvent.KEYCODE_DPAD_LEFT, false);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendKey(KeyEvent.KEYCODE_DPAD_LEFT, true);
                    return true;
                }
                return false;
            }
        });
        downBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    sendKey(KeyEvent.KEYCODE_DPAD_DOWN, false);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendKey(KeyEvent.KEYCODE_DPAD_DOWN, true);
                    return true;
                }
                return false;
            }
        });
        rightBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    sendKey(KeyEvent.KEYCODE_DPAD_RIGHT, false);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendKey(KeyEvent.KEYCODE_DPAD_RIGHT, true);
                    return true;
                }
                return false;
            }
        });
        escBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyDownUp(KeyEvent.KEYCODE_ESCAPE);
            }
        });
        enterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyDownUp(KeyEvent.KEYCODE_ENTER);
            }
        });
        ctrlBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onClick(View v) {
                if (!ctrlClicked) {
                    sendKey(KeyEvent.KEYCODE_CTRL_LEFT, false);
                    ctrlBtn.setBackground(getResources().getDrawable(R.drawable.controls_button2));
                    ctrlClicked = true;
                } else {
                    sendKey(KeyEvent.KEYCODE_CTRL_LEFT, true);
                    ctrlBtn.setBackground(getResources().getDrawable(R.drawable.controls_button1));
                    ctrlClicked = false;
                }
            }
        });
        altBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onClick(View v) {
                if (!altClicked) {
                    sendKey(KeyEvent.KEYCODE_ALT_LEFT, false);
                    altBtn.setBackground(getResources().getDrawable(R.drawable.controls_button2));
                    altClicked = true;
                } else {
                    sendKey(KeyEvent.KEYCODE_ALT_LEFT, true);
                    altBtn.setBackground(getResources().getDrawable(R.drawable.controls_button1));
                    altClicked = false;
                }
            }
        });
        delBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyDownUp(KeyEvent.KEYCODE_DEL);
            }
        });
        ctrlAltDelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCtrlAtlDelKey();
            }
        });
        qmpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (monitorMode) {
                    onVNC();
                    qmpBtn.setImageDrawable(getResources().getDrawable(R.drawable.round_terminal_24));
                } else {
                    onMonitor();
                    qmpBtn.setImageDrawable(getResources().getDrawable(R.drawable.round_computer_24));
                }
            }
        });
        if (monitorMode) {
            qmpBtn.setImageDrawable(getResources().getDrawable(R.drawable.round_terminal_24));
        } else {
            qmpBtn.setImageDrawable(getResources().getDrawable(R.drawable.round_computer_24));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.container_function, functionsArray);

        ListView listView = findViewById(R.id.functions);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    keyDownUp(KeyEvent.KEYCODE_F1);
                } else if (position == 1) {
                    keyDownUp(KeyEvent.KEYCODE_F2);
                } else if (position == 2) {
                    keyDownUp(KeyEvent.KEYCODE_F3);
                } else if (position == 3) {
                    keyDownUp(KeyEvent.KEYCODE_F4);
                } else if (position == 4) {
                    keyDownUp(KeyEvent.KEYCODE_F5);
                } else if (position == 5) {
                    keyDownUp(KeyEvent.KEYCODE_F6);
                } else if (position == 6) {
                    keyDownUp(KeyEvent.KEYCODE_F7);
                } else if (position == 7) {
                    keyDownUp(KeyEvent.KEYCODE_F8);
                } else if (position == 8) {
                    keyDownUp(KeyEvent.KEYCODE_F9);
                } else if (position == 9) {
                    keyDownUp(KeyEvent.KEYCODE_F10);
                } else if (position == 10) {
                    keyDownUp(KeyEvent.KEYCODE_F11);
                } else if (position == 11) {
                    keyDownUp(KeyEvent.KEYCODE_F12);
                }
            }
        });
    }

    private void keyDownUp(int keyEventCode) {
        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    private void sendKey(int keyEventCode, boolean up) {
        if (up)
            dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
        else dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
    }

    public void sendCtrlAtlDelKey() {
        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT));
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ALT_LEFT));
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT));
        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ALT_LEFT));
        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
    }

    private void setDefaulViewMode() {


        // Fit to Screen
        AbstractScaling.getById(R.id.itemFitToScreen).setScaleTypeForActivity(this);
        showPanningState();

//        screenMode = VNCScreenMode.FitToScreen;
        setLayout(getResources().getConfiguration());

        UIUtils.setOrientation(this);
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
                (newConfig != null && newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
                        || UIUtils.isLandscapeOrientation(this);

        View vnc_canvas_layout = (View) this.findViewById(R.id.vnc_canvas_layout);
        RelativeLayout.LayoutParams vnc_canvas_layout_params = null;
        RelativeLayout.LayoutParams vnc_params = null;
        //normal 1-1
        if (screenMode == VNCScreenMode.Normal) {
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
                if (vncCanvas != null && vncCanvas.rfb != null
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
        MainService.updateServiceNotification("Vectras VM Running in Background");
        super.onPause();
    }

    public void onResume() {
        MainService.updateServiceNotification("Vectras VM Running");
        if (monitorMode) {
            qmpBtn.setImageDrawable(getResources().getDrawable(R.drawable.round_terminal_24));
        } else {
            qmpBtn.setImageDrawable(getResources().getDrawable(R.drawable.round_computer_24));
        }
        super.onResume();
    }

    public void checkStatus() {
        while (timeQuit != true) {
            MainActivity.VMStatus status = Machine.checkSaveVMStatus(activity);
            Log.v(TAG, "Status: " + status);
            if (status == MainActivity.VMStatus.Unknown
                    || status == MainActivity.VMStatus.Completed
                    || status == MainActivity.VMStatus.Failed
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

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == this.KEYBOARD || item.getItemId() == R.id.itemKeyboard) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    toggleKeyboardFlag = UIUtils.onKeyboard(activity, toggleKeyboardFlag, vncCanvas);
                }
            }, 200);
        } else if (item.getItemId() == R.id.itemReset) {
            Machine.resetVM(activity);
        } else if (item.getItemId() == R.id.itemShutdown) {
            UIUtils.hideKeyboard(this, vncCanvas);
            Machine.stopVM(activity);
        } else if (item.getItemId() == R.id.itemDrives) {

        } else if (item.getItemId() == R.id.itemMonitor) {
            if (this.monitorMode) {
                this.onVNC();
            } else {
                this.onMonitor();
            }
        } else if (item.getItemId() == R.id.itemSaveState) {
            this.promptPause(activity);
        } else if (item.getItemId() == R.id.itemFitToScreen) {
            return onFitToScreen();
        } else if (item.getItemId() == R.id.itemFullScreen) {
            return toggleFullScreen();
        } else if (item.getItemId() == this.QUIT) {
        } else if (item.getItemId() == R.id.itemCenterMouse) {
            onMouseMode();
        } else if (item.getItemId() == R.id.itemCalibrateMouse) {
            calibration();
        } else if (item.getItemId() == R.id.itemHelp) {

        } else if (item.getItemId() == R.id.itemHideToolbar) {
            this.onHideToolbar();
        } else if (item.getItemId() == R.id.itemDisplay) {
            this.onSelectMenuVNCDisplay();
        } else if (item.getItemId() == R.id.itemViewLog) {

        }

        this.invalidateOptionsMenu();

        return true;
    }

    private void onMouseMode() {

        String[] items = {"Trackpad Mouse (Phone)",
                "Bluetooth/USB Mouse (Desktop mode)", //Physical mouse for Chromebook, Android x86 PC, or Bluetooth Mouse
        };
        final AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        mBuilder.setTitle("Mouse");
        mBuilder.setSingleChoiceItems(items, Config.mouseMode.ordinal(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
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
        if (vncCanvas.rfb.framebufferWidth < vncCanvas.getWidth()
                && vncCanvas.rfb.framebufferHeight < vncCanvas.getHeight())
            return true;

        return false;
    }

    private void onDisplayMode() {

        String[] items = {
                "Normal (One-To-One)",
                "Fit To Screen"
                //"Full Screen" //Stretched
        };
        int currentScaleType = vncCanvas.getScaleType() == ImageView.ScaleType.FIT_CENTER ? 1 : 0;

        final AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        mBuilder.setTitle("Display Mode");
        mBuilder.setSingleChoiceItems(items, currentScaleType, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case 0:
                        onNormalScreen();
                        onMouse();
                        break;
                    case 1:
                        if (Config.mouseMode == Config.MouseMode.External) {
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

    private void setUIModeMobile(boolean fitToScreen) {

        try {
            MotionEvent a = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);

            Config.mouseMode = Config.MouseMode.Trackpad;
            MainSettingsManager.setDesktopMode(this, false);
            if (fitToScreen)
                onFitToScreen();
            else
                onNormalScreen();
            onMouse();

            //UIUtils.toastShort(MainVNCActivity.this, "Trackpad Calibrating");
            invalidateOptionsMenu();
        } catch (Exception ex) {
            if (Config.debug)
                ex.printStackTrace();
        }
    }

    private void promptSetUIModeDesktop(final Activity activity, final boolean mouseMethodAlt) {


        final AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
        alertDialog.setTitle("Desktop mode");

        LinearLayout mLayout = new LinearLayout(this);
        mLayout.setPadding(20, 20, 20, 20);
        mLayout.setOrientation(LinearLayout.VERTICAL);

        TextView textView = new TextView(activity);
        textView.setVisibility(View.VISIBLE);

        String desktopInstructions = this.getString(R.string.desktopInstructions);
        if (!checkVMResolutionFits()) {
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
            if (Config.showToast)
                UIUtils.toastShort(MainVNCActivity.this, "External Mouse Enabled");
            onNormalScreen();
            AbstractScaling.getById(R.id.itemOneToOne).setScaleTypeForActivity(MainVNCActivity.this);
            showPanningState();

            onMouse();
        } catch (Exception e) {
            if (Config.debug)
                e.printStackTrace();
        }
        //vncCanvas.reSize(false);
        invalidateOptionsMenu();
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
            UIUtils.setOrientation(this);
            ActionBar bar = this.getSupportActionBar();
            if (bar != null && !MainSettingsManager.getAlwaysShowMenuToolbar(this)) {
                bar.hide();
            }

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
            if (Config.debug)
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
                bar.hide();
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
            if (Config.debug)
                ex.printStackTrace();
        } finally {

        }
        return false;
    }

    private boolean onMouse() {

        // Main: For now we disable other modes
        if (Config.disableMouseModes)
            mouseOn = false;


        if (mouseOn == false) {
            inputHandler = getInputHandlerById(R.id.itemInputTouchpad);
            connection.setInputMode(inputHandler.getName());
            connection.setFollowMouse(true);
            mouseOn = true;
        } else {
            // XXX: Main
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

                    for (int i = 0; i < 4 * 20; i++) {
                        int x = 0 + i * 50;
                        int y = 0 + i * 50;
                        if (i % 4 == 1) {
                            x = vncCanvas.rfb.framebufferWidth;
                        } else if (i % 4 == 2) {
                            y = vncCanvas.rfb.framebufferHeight;
                        } else if (i % 4 == 3) {
                            x = 0;
                        }

                        event = MotionEvent.obtain(SystemClock.uptimeMillis(),
                                SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE,
                                x, y, 0);
                        Thread.sleep(10);
                        vncCanvas.processPointerEvent(event, false, false);


                    }

                    Thread.sleep(50);
                    event = MotionEvent.obtain(SystemClock.uptimeMillis(),
                            SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE,
                            origX, origY, 0);
                    vncCanvas.processPointerEvent(event, false, false);

                } catch (Exception ex) {

                }
            }
        });
        t.start();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        return this.setupMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        return this.setupMenu(menu);

    }

    public boolean setupMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.vnccanvasactivitymenu, menu);

        int maxMenuItemsShown = 4;
        int actionShow = MenuItemCompat.SHOW_AS_ACTION_IF_ROOM;
        if (UIUtils.isLandscapeOrientation(this)) {
            maxMenuItemsShown = 6;
            actionShow = MenuItemCompat.SHOW_AS_ACTION_ALWAYS;
        }

        if (vncCanvas.scaling != null) {
            menu.findItem(vncCanvas.scaling.getId()).setChecked(true);
        }

        if (this.monitorMode) {
            menu.findItem(R.id.itemMonitor).setTitle("VM Display");

        } else {
            menu.findItem(R.id.itemMonitor).setTitle("QEMU Monitor");

        }

        //XXX: We don't need these for now
        menu.removeItem(menu.findItem(R.id.itemEnterText).getItemId());
        menu.removeItem(menu.findItem(R.id.itemSendKeyAgain).getItemId());
        menu.removeItem(menu.findItem(R.id.itemSpecialKeys).getItemId());
        menu.removeItem(menu.findItem(R.id.itemInputMode).getItemId());
        menu.removeItem(menu.findItem(R.id.itemScaling).getItemId());
        menu.removeItem(menu.findItem(R.id.itemCtrlAltDel).getItemId());
        menu.removeItem(menu.findItem(R.id.itemCtrlC).getItemId());
        menu.removeItem(menu.findItem(R.id.itemColorMode).getItemId());
        menu.removeItem(menu.findItem(R.id.itemFullScreen).getItemId());

        if (MainSettingsManager.getAlwaysShowMenuToolbar(activity) || Config.mouseMode == Config.MouseMode.External) {
            menu.removeItem(menu.findItem(R.id.itemHideToolbar).getItemId());
            maxMenuItemsShown--;
        }

        // Menu inputMenu = menu.findItem(R.id.itemInputMode).getSubMenu();
        //
        // inputModeMenuItems = new MenuItem[inputModeIds.length];
        // for (int i = 0; i < inputModeIds.length; i++) {
        // inputModeMenuItems[i] = inputMenu.findItem(inputModeIds[i]);
        // }
        // updateInputMenu();
        // menu.removeItem(menu.findItem(R.id.itemCenterMouse).getItemId());

        // Main: Disable Panning for now
        // if (this.mouseOn) {
        // menu.findItem(R.id.itemCenterMouse).setTitle("Pan (Mouse Off)");
        // menu.findItem(R.id.itemCenterMouse).setIcon(R.drawable.pan);
        // } else {
        menu.findItem(R.id.itemCenterMouse).setTitle("Mouse");
        //
        // }


        for (int i = 0; i < menu.size() && i < maxMenuItemsShown; i++) {
            MenuItemCompat.setShowAsAction(menu.getItem(i), actionShow);
        }

        return true;

    }


    public static boolean toggleKeyboardFlag = true;

    private void onMonitor() {
        if (Config.showToast)
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
        if (MainActivity.vmexecutor == null) {
            return;
        }
        Thread t = new Thread(new Runnable() {
            public void run() {
                if (MainActivity.vmexecutor.paused == 1) {
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MainVNCActivity.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (vncCanvas == null)
                        return;

                    MainActivity.vmexecutor.paused = 0;
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
                if (MainActivity.vmexecutor.save_state_name != null) {
                    File file = new File(MainActivity.vmexecutor.save_state_name);
                    if (file.exists()) {
                        file.delete();
                    }
                }

                UIUtils.toastShort(getApplicationContext(), "Please wait while saving VM State");

                String uri = "fd:" + MainActivity.vmexecutor.get_fd(MainActivity.vmexecutor.save_state_name);
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

        if (response.contains("error")) {
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
            } catch (Exception ex) {
                if (Config.debug)
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
        alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
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
        super.onBackPressed();
        Machine.stopVM(activity);
        return;
    }

    public void onHideToolbar() {
        ActionBar bar = this.getSupportActionBar();
        if (bar != null) {
            bar.hide();
        }
    }

    @Override
    public void onConnected() {
        this.resumeVM();
        if (!firstConnection)
            UIUtils.showHints(this);
        firstConnection = true;

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {

                if (Config.mouseMode == Config.MouseMode.External)
                    setUIModeDesktop();
                else
                    setUIModeMobile(screenMode == VNCScreenMode.FitToScreen);
            }
        }, 1000);

    }

    public void onSelectMenuVNCDisplay() {

        final AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
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
        Button displayMode = new Button(this);

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
        value.setText("Display Refresh Rate: " + currRate + " Hz");
        layout.addView(value);
        value.setLayoutParams(params);

        SeekBar rate = new SeekBar(this);
        rate.setMax(Config.MAX_DISPLAY_REFRESH_RATE);

        rate.setProgress(currRate);
        rate.setLayoutParams(params);

        ((SeekBar) rate).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar s, int progress, boolean touch) {
                value.setText("Refresh Rate: " + (progress + 1) + " Hz");
            }

            public void onStartTrackingTouch(SeekBar arg0) {

            }

            public void onStopTrackingTouch(SeekBar arg0) {
                int progress = arg0.getProgress() + 1;
                int refreshMs = 1000 / progress;
                Log.v(TAG, "Changing display refresh rate (ms): " + refreshMs);
                MainActivity.vmexecutor.setvncrefreshrate(refreshMs);

            }
        });


        layout.addView(rate);

        return layout;

    }

    public int getCurrentVNCRefreshRate() {
        return 1000 / MainActivity.vmexecutor.getvncrefreshrate();
    }

}
