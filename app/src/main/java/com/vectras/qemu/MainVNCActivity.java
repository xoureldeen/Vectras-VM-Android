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
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vectras.vm.*;

import com.vectras.qemu.utils.FileUtils;
import com.vectras.vm.Fragment.ControlersOptionsFragment;
import com.vectras.vm.Fragment.LoggerDialogFragment;
import com.vectras.vm.MainActivity;
import com.vectras.vm.R;
import com.vectras.qemu.utils.QmpClient;
import com.vectras.vm.utils.UIUtils;
import com.vectras.vm.widgets.JoystickView;
import com.vectras.vterm.Terminal;

import java.io.File;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;


/**
 * @author Dev
 */
public class MainVNCActivity extends VncCanvasActivity {

    public static boolean started = false;
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
    public static MainVNCActivity activity;
    public static LinearLayout desktop;
    public static LinearLayout gamepad;
    @Override
    public void onCreate(Bundle b) {

        if (MainSettingsManager.getFullscreen(this))
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(b);
        activity = this;

        this.vncCanvas.setFocusableInTouchMode(true);

        setDefaulViewMode();

//        setUIModeMobile();

        Toolbar mainToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mainToolbar);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        onFitToScreen();

        desktop = findViewById(R.id.desktop);
        gamepad = findViewById(R.id.gamepad);

        if (Objects.equals(MainSettingsManager.getControlMode(activity), "D")) {
            desktop.setVisibility(View.VISIBLE);
            gamepad.setVisibility(View.GONE);
        } else if (Objects.equals(MainSettingsManager.getControlMode(activity), "G")) {
            desktop.setVisibility(View.GONE);
            gamepad.setVisibility(View.VISIBLE);
        } else if (Objects.equals(MainSettingsManager.getControlMode(activity), "H")) {
            desktop.setVisibility(View.GONE);
            gamepad.setVisibility(View.GONE);
        }

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
        ImageButton btnVterm = findViewById(R.id.btnVterm);
        Button eBtn = findViewById(R.id.eBtn);
        Button rBtn = findViewById(R.id.rBtn);
        Button qBtn = findViewById(R.id.qBtn);
        Button xBtn = findViewById(R.id.xBtn);
        ImageButton ctrlGameBtn = findViewById(R.id.ctrlGameBtn);
        Button spaceBtn = findViewById(R.id.spaceBtn);
        Button tabGameBtn = findViewById(R.id.tabGameBtn);
        Button tabBtn = findViewById(R.id.tabBtn);
        ImageButton upGameBtn = findViewById(R.id.upGameBtn);
        ImageButton downGameBtn = findViewById(R.id.downGameBtn);
        ImageButton leftGameBtn = findViewById(R.id.leftGameBtn);
        ImageButton rightGameBtn = findViewById(R.id.rightGameBtn);
        ImageButton enterGameBtn = findViewById(R.id.enterGameBtn);
        qmpBtn = findViewById(R.id.btnQmp);
        upGameBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    sendKey(KeyEvent.KEYCODE_DPAD_UP, false);
                    v.animate().scaleXBy(-0.2f).setDuration(200).start();
                    v.animate().scaleYBy(-0.2f).setDuration(200).start();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendKey(KeyEvent.KEYCODE_DPAD_UP, true);
                    v.animate().cancel();
                    v.animate().scaleX(1f).setDuration(200).start();
                    v.animate().scaleY(1f).setDuration(200).start();
                    return true;
                }
                return false;
            }
        });
        leftGameBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    sendKey(KeyEvent.KEYCODE_DPAD_LEFT, false);
                    v.animate().scaleXBy(-0.2f).setDuration(200).start();
                    v.animate().scaleYBy(-0.2f).setDuration(200).start();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendKey(KeyEvent.KEYCODE_DPAD_LEFT, true);
                    v.animate().cancel();
                    v.animate().scaleX(1f).setDuration(200).start();
                    v.animate().scaleY(1f).setDuration(200).start();
                    return true;
                }
                return false;
            }
        });
        downGameBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    sendKey(KeyEvent.KEYCODE_DPAD_DOWN, false);
                    v.animate().scaleXBy(-0.2f).setDuration(200).start();
                    v.animate().scaleYBy(-0.2f).setDuration(200).start();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendKey(KeyEvent.KEYCODE_DPAD_DOWN, true);
                    v.animate().cancel();
                    v.animate().scaleX(1f).setDuration(200).start();
                    v.animate().scaleY(1f).setDuration(200).start();
                    return true;
                }
                return false;
            }
        });
        rightGameBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    sendKey(KeyEvent.KEYCODE_DPAD_RIGHT, false);
                    v.animate().scaleXBy(-0.2f).setDuration(200).start();
                    v.animate().scaleYBy(-0.2f).setDuration(200).start();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendKey(KeyEvent.KEYCODE_DPAD_RIGHT, true);
                    v.animate().cancel();
                    v.animate().scaleX(1f).setDuration(200).start();
                    v.animate().scaleY(1f).setDuration(200).start();
                    return true;
                }
                return false;
            }
        });
        JoystickView joystick = (JoystickView) findViewById(R.id.joyStick);
        joystick.setVisibility(View.GONE);
        tabBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyDownUp(KeyEvent.KEYCODE_TAB);
            }
        });
        tabGameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyDownUp(KeyEvent.KEYCODE_TAB);
            }
        });
        enterGameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyDownUp(KeyEvent.KEYCODE_ENTER);
            }
        });
        eBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyDownUp(KeyEvent.KEYCODE_E);
            }
        });
        rBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyDownUp(KeyEvent.KEYCODE_R);
            }
        });
        qBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyDownUp(KeyEvent.KEYCODE_Q);
            }
        });
        xBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyDownUp(KeyEvent.KEYCODE_X);
            }
        });
        ctrlGameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyDownUp(KeyEvent.KEYCODE_CTRL_LEFT);
            }
        });
        spaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyDownUp(KeyEvent.KEYCODE_SPACE);
            }
        });
        btnVterm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                // Create and show the dialog.
                LoggerDialogFragment newFragment = new LoggerDialogFragment();
                newFragment.show(ft, "Logger");
            }
        });
        shutdownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(activity, R.style.MainDialogTheme)
                        .setTitle("Shutdown")
                        .setMessage("Are you sure you want to shutdown vm?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                started = false;
                                // Stop the service
                                MainService.stopService();
                                // Finish the activity
                                activity.finish();
                            }

                        })
                        .setNegativeButton("No", null)
                        .show();
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
                    v.animate().scaleXBy(-0.2f).setDuration(200).start();
                    v.animate().scaleYBy(-0.2f).setDuration(200).start();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendKey(KeyEvent.KEYCODE_DPAD_UP, true);
                    v.animate().cancel();
                    v.animate().scaleX(1f).setDuration(200).start();
                    v.animate().scaleY(1f).setDuration(200).start();
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
                    v.animate().scaleXBy(-0.2f).setDuration(200).start();
                    v.animate().scaleYBy(-0.2f).setDuration(200).start();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendKey(KeyEvent.KEYCODE_DPAD_LEFT, true);
                    v.animate().cancel();
                    v.animate().scaleX(1f).setDuration(200).start();
                    v.animate().scaleY(1f).setDuration(200).start();
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
                    v.animate().scaleXBy(-0.2f).setDuration(200).start();
                    v.animate().scaleYBy(-0.2f).setDuration(200).start();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendKey(KeyEvent.KEYCODE_DPAD_DOWN, true);
                    v.animate().cancel();
                    v.animate().scaleX(1f).setDuration(200).start();
                    v.animate().scaleY(1f).setDuration(200).start();
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
                    v.animate().scaleXBy(-0.2f).setDuration(200).start();
                    v.animate().scaleYBy(-0.2f).setDuration(200).start();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendKey(KeyEvent.KEYCODE_DPAD_RIGHT, true);
                    v.animate().cancel();
                    v.animate().scaleX(1f).setDuration(200).start();
                    v.animate().scaleY(1f).setDuration(200).start();
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
        Button rightClickBtn = findViewById(R.id.rightClickBtn);
        Button middleClickBtn = findViewById(R.id.middleBtn);
        Button leftClickBtn = findViewById(R.id.leftClickBtn);
        ImageButton winBtn = findViewById(R.id.winBtn);

        rightClickBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MotionEvent e = MotionEvent.obtain(1000, 1000, MotionEvent.ACTION_DOWN, vncCanvas.mouseX, vncCanvas.mouseY,
                        0);
                ((TouchpadInputHandler) VncCanvasActivity.inputHandler).rightClick(e);
            }
        });
        middleClickBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MotionEvent e = MotionEvent.obtain(1000, 1000, MotionEvent.ACTION_DOWN, vncCanvas.mouseX, vncCanvas.mouseY,
                        0);
                ((TouchpadInputHandler) VncCanvasActivity.inputHandler).middleClick(e);
            }
        });
        leftClickBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MotionEvent e = MotionEvent.obtain(1000, 1000, MotionEvent.ACTION_DOWN, vncCanvas.mouseX, vncCanvas.mouseY,
                        0);
                ((TouchpadInputHandler) VncCanvasActivity.inputHandler).leftClick(e);
            }
        });
        winBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendKey(KeyEvent.KEYCODE_CTRL_LEFT, false);
                sendKey(KeyEvent.KEYCODE_ESCAPE, false);
                sendKey(KeyEvent.KEYCODE_CTRL_LEFT, false);
                sendKey(KeyEvent.KEYCODE_ESCAPE, false);
            }
        });

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

    public boolean rightClick(final MotionEvent e, final int i) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                Log.d("SDL", "Mouse Right Click");
                //MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_RIGHT, MotionEvent.ACTION_DOWN, 1, -1, -1);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
//					Log.v("SDLSurface", "Interrupted: " + ex);
                }
                //MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_RIGHT, MotionEvent.ACTION_UP, 1, -1, -1);
            }
        });
        t.start();
        return true;

    }

    public boolean leftClick(final MotionEvent e, final int i) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                Log.d("SDL", "Mouse Left Click");
                //MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_LEFT, MotionEvent.ACTION_DOWN, 1, -1, -1);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
//					Log.v("SDLSurface", "Interrupted: " + ex);
                }
                //MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_LEFT, MotionEvent.ACTION_UP, 1, -1, -1);
            }
        });
        t.start();
        return true;

    }

    public boolean middleClick(final MotionEvent e, final int i) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                Log.d("SDL", "Mouse Middle Click");
                //MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_MIDDLE, MotionEvent.ACTION_DOWN, 1, -1, -1);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
//                    Log.v("SDLSurface", "Interrupted: " + ex);
                }
                //MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_MIDDLE, MotionEvent.ACTION_UP, 1, -1, -1);
            }
        });
        t.start();
        return true;

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
        Terminal.killQemuProcess();
    }

    public void onPause() {
        //MainService.updateServiceNotification("Vectras VM Running in Background");
        super.onPause();
    }

    String TAG = "MainVNCActivity";
/*

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
            //Machine.resetVM(activity);
        } else if (item.getItemId() == R.id.itemShutdown) {
            UIUtils.hideKeyboard(this, vncCanvas);
            //Machine.stopVM(activity);
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
        } else if (item.getItemId() == this.QUIT) {
        } else if (item.getItemId() == R.id.itemCenterMouse) {
            onMouseMode();
        } else if (item.getItemId() == R.id.itemHelp) {

        } else if (item.getItemId() == R.id.itemHideToolbar) {
            this.onHideToolbar();
        } else if (item.getItemId() == R.id.itemViewLog) {

        }

        this.invalidateOptionsMenu();

        return true;
    }
*/

    public void onMouseMode() {

        String[] items = {"Trackpad Mouse (Phone)",
                "Bluetooth/USB Mouse (Desktop mode)", //Physical mouse for Chromebook, Android x86 PC, or Bluetooth Mouse
        };
        final AlertDialog.Builder mBuilder = new AlertDialog.Builder(this, R.style.MainDialogTheme);
        mBuilder.setTitle("Mouse mode");
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
        String desktopInstructions = this.getString(R.string.desktopInstructions);
        if (!checkVMResolutionFits()) {
            String resolutionWarning = "Warning: MainActivity.vmexecutor resolution "
                    + vncCanvas.rfb.framebufferWidth + "x" + vncCanvas.rfb.framebufferHeight +
                    " is too high for Desktop Mode. " +
                    "Scaling will be used and Mouse Alignment will not be accurate. " +
                    "Reduce display resolution within the Guest OS for better experience.\n\n";
            desktopInstructions = resolutionWarning + desktopInstructions;
        }
        alertDialog.setMessage(desktopInstructions);
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

    public void onStart() {
        super.onStart();
        started = true;
    }
    private void resumeVM() {
    }

    private void onPauseVM() {
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
        FrameLayout l = findViewById(R.id.mainControl);
        if (l != null) {
            if (l.getVisibility() == View.VISIBLE) {
                l.setVisibility(View.GONE);
            } else
                l.setVisibility(View.VISIBLE);
        }
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
}
