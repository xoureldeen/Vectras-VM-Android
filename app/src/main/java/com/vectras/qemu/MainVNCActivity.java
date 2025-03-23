package com.vectras.qemu;

import android.androidVNC.AbstractScaling;
import android.androidVNC.VncCanvasActivity;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.app.Activity;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vectras.vm.*;

import com.vectras.vm.Fragment.ControlersOptionsFragment;
import com.vectras.vm.Fragment.LoggerDialogFragment;
import com.vectras.vm.R;
import com.vectras.vm.utils.UIUtils;
import com.vectras.vm.widgets.JoystickView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;


/**
 * @author Dev
 */
public class MainVNCActivity extends VncCanvasActivity {

    private Timer _timer = new Timer();
    private TimerTask timerTask;

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

    private LinearLayout sendkeylayout;
    private RecyclerView sendkeylist;
    private EditText sendtextEdittext;
    private ImageButton sendtextButton;
    private ImageButton hidesendkeyButton;
    private ImageButton sendselectallkeyButton;
    private ImageButton sendcutButton;
    private ImageButton sendcopykeyButton;
    private ImageButton sendpastekeyButton;
    private ImageButton senddelkeyButton;
    private ArrayList<HashMap<String, Object>> listmapForSendKey = new ArrayList<>();
    private LinearLayoutManager rvLayoutManager;

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
        ImageButton ctrlaltdelBtn = findViewById(R.id.ctrlaltdelBtn);
        sendkeylayout = findViewById(R.id.sendkeylayout);
        sendkeylist = findViewById(R.id.sendkeylist);
        sendtextEdittext = findViewById(R.id.sendtextEdittext);
        sendtextButton = findViewById(R.id.sendtextButton);
        hidesendkeyButton = findViewById(R.id.hidesendkeyButton);
        sendselectallkeyButton = findViewById(R.id.sendselectallkeyButton);
        sendpastekeyButton = findViewById(R.id.sendpastekeyButton);
        sendcutButton = findViewById(R.id.sendcutButton);
        sendcopykeyButton = findViewById(R.id.sendcopykeyButton);
        senddelkeyButton = findViewById(R.id.senddelkeyButton);
        qmpBtn = findViewById(R.id.btnQmp);
        ImageButton appsBtn = findViewById(R.id.btnPrograms);
        appsBtn.setVisibility(View.GONE);
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
        ImageButton btnFit = findViewById(R.id.btnFit);
        btnFit.setVisibility(View.GONE);
        tabBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyDownUp(KeyEvent.KEYCODE_TAB);
            }
        });
        ctrlaltdelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sendkeylayout.getVisibility() == View.VISIBLE) {
                    sendkeylayout.setVisibility(View.GONE);
                    sendtextEdittext.setEnabled(false);
                    sendtextEdittext.setEnabled(true);
                } else {
                    sendkeylayout.setVisibility(View.VISIBLE);
                }
                //sendCtrlAtlDelKey();
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
                        .setTitle(getString(R.string.shutdown))
                        .setMessage(getString(R.string.are_you_sure_you_want_to_shutdown_vm))
                        .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                            started = false;
                            // Stop the service
                            MainService.stopService();
                            //Terminal.killQemuProcess();
                            //VectrasApp.killcurrentqemuprocess(getApplicationContext());
                            shutdownthisvm();
                        })
                        .setNegativeButton(getString(R.string.no), null)
                        .show();
            }
        });

        shutdownBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
                alertDialog.setTitle("Exit");
                alertDialog.setMessage("You will be left here but the virtual machine will continue to run.");
                alertDialog.setCancelable(true);
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Exit", (dialog, which) -> {
                    started = false;
                    finish();
                });
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", (dialog, which) -> {

                });
                alertDialog.show();
                return false;
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
        keyboardBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (sendkeylayout.getVisibility() == View.VISIBLE) {
                    sendkeylayout.setVisibility(View.GONE);
                    sendtextEdittext.setEnabled(false);
                    sendtextEdittext.setEnabled(true);
                } else {
                    sendkeylayout.setVisibility(View.VISIBLE);
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendtextEdittext.requestFocus();
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(sendtextEdittext, InputMethodManager.SHOW_IMPLICIT);
                        }
                    }, 500);
                }
                return false;
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
        sendselectallkeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vncCanvas.sendCtrlA();
            }
        });
        sendcutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vncCanvas.sendCtrlX();
            }
        });
        sendcopykeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vncCanvas.sendCtrlC();
            }
        });
        sendpastekeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vncCanvas.sendCtrlV();
            }
        });
        senddelkeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_FORWARD_DEL));
                dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_FORWARD_DEL));
            }
        });
        hidesendkeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendkeylayout.setVisibility(View.GONE);
                sendtextEdittext.setEnabled(false);
                sendtextEdittext.setEnabled(true);
            }
        });
        sendtextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vncCanvas.sendText(sendtextEdittext.getText().toString());
                sendtextEdittext.setText("");
            }
        });

        sendtextEdittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    sendtextButton.setVisibility(View.GONE);
                } else {
                    sendtextButton.setVisibility(View.VISIBLE);
                }
            }
        });

        sendtextEdittext.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    vncCanvas.sendText(sendtextEdittext.getText().toString());
                    sendtextEdittext.setText("");
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(sendtextEdittext.getWindowToken(), 0);
                    handled = true;
                }
                return handled;
            }
        });

                ArrayAdapter < String > adapter = new ArrayAdapter<>(this,
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
        sendkeylayout.setVisibility(View.GONE);
        sendtextButton.setVisibility(View.GONE);
        sendkeydialog();
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
        vncCanvas.sendCtrlAltDel();
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
        if (VectrasApp.isQemuRunning() && started) {
            activity.startActivity(new Intent(activity, MainVNCActivity.class));
        }
        super.onDestroy();
        this.stopTimeListener();
        //Terminal.killQemuProcess();
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
        return vncCanvas.rfb.framebufferWidth < vncCanvas.getWidth()
                && vncCanvas.rfb.framebufferHeight < vncCanvas.getHeight();
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (sendkeylayout.getVisibility() == View.VISIBLE) {
            sendkeylayout.setVisibility(View.GONE);
        } else {
            FrameLayout l = findViewById(R.id.mainControl);
            if (l != null) {
                if (l.getVisibility() == View.VISIBLE) {
                    l.setVisibility(View.GONE);
                } else
                    l.setVisibility(View.VISIBLE);
            }
            started = false;
            finish();
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

    private void shutdownthisvm() {
        sendtextEdittext.setEnabled(false);
        vncCanvas.sendMetaKey1(50, 6);
        timerTask = new TimerTask() {
            @Override
            public void run() {
                dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Q));
                dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Q));
                dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_U));
                dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_U));
                dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_I));
                dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_I));
                dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_T));
                dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_T));
                dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                Config.setDefault();
                finish();
            }
        };
        _timer.schedule(timerTask, 1000);
    }

    private void sendkeydialog() {
        VectrasApp.setupSendKeyListForListmap(listmapForSendKey);
        rvLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,false);
        sendkeylist.setAdapter(new Recyclerview1Adapter(listmapForSendKey));
        sendkeylist.setLayoutManager(rvLayoutManager);
        sendkeylist.setHasFixedSize(true);
    }

    public class Recyclerview1Adapter extends RecyclerView.Adapter<Recyclerview1Adapter.ViewHolder> {

        ArrayList<HashMap<String, Object>> _data;

        public Recyclerview1Adapter(ArrayList<HashMap<String, Object>> _arr) {
            _data = _arr;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater _inflater = getLayoutInflater();
            View _v = _inflater.inflate(R.layout.layout_for_send_keys, null);
            RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            _v.setLayoutParams(_lp);
            return new ViewHolder(_v);
        }

        @Override
        public void onBindViewHolder(ViewHolder _holder, final int _position) {
            View _view = _holder.itemView;
            RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            _view.setLayoutParams(_lp);
            final LinearLayout _all = _view.findViewById(R.id.all);
            final TextView _textViewKeyName = _view.findViewById(R.id.textViewKeyName);
            final ImageView _imageViewKey = _view.findViewById(R.id.imageViewKey);
            _textViewKeyName.setTextColor(0xff000000);


            if ((boolean) _data.get(_position).get("useIcon")) {
                _textViewKeyName.setVisibility(View.GONE);
                _imageViewKey.setVisibility(View.VISIBLE);
                _imageViewKey.setImageResource((int) _data.get(_position).get("rIcon"));
            } else {
                _imageViewKey.setVisibility(View.GONE);
                _textViewKeyName.setVisibility(View.VISIBLE);
                _textViewKeyName.setText(_data.get(_position).get("keyname").toString());
            }

            _all.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View _view) {
                    if (_position == 0) {
                        sendCtrlAtlDelKey();
                    } else {
                        if ((boolean) _data.get(_position).get("useKeyEvent")) {
                            dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, (int) _data.get(_position).get("keycode")));
                            dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, (int) _data.get(_position).get("keycode")));
                        } else {
                            vncCanvas.sendAKey((int) _data.get(_position).get("keycode"));
                        }
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return _data.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View v) {
                super(v);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pointerCount = event.getPointerCount();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (pointerCount == 3) {
                    MotionEvent e = MotionEvent.obtain(1000, 1000, MotionEvent.ACTION_DOWN, vncCanvas.mouseX, vncCanvas.mouseY,
                            0);
                    ((TouchpadInputHandler) VncCanvasActivity.inputHandler).middleClick(e);
                } else if (pointerCount == 2) {
                    MotionEvent e = MotionEvent.obtain(1000, 1000, MotionEvent.ACTION_DOWN, vncCanvas.mouseX, vncCanvas.mouseY,
                            0);
                    ((TouchpadInputHandler) VncCanvasActivity.inputHandler).rightClick(e);
                }
                break;
        }

        return super.onTouchEvent(event);
    }
}
