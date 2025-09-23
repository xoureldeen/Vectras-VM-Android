package com.vectras.qemu;

import android.androidVNC.AbstractScaling;
import android.androidVNC.VncCanvasActivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.app.Activity;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
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
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

import com.vectras.vm.*;

import com.vectras.vm.Fragment.ControlersOptionsFragment;
import com.vectras.vm.Fragment.LoggerDialogFragment;
import com.vectras.vm.R;
import com.vectras.vm.databinding.ActivityVncBinding;
import com.vectras.vm.databinding.ControlsFragmentBinding;
import com.vectras.vm.databinding.DesktopControlsBinding;
import com.vectras.vm.databinding.GameControlsBinding;
import com.vectras.vm.databinding.SendKeyDialogBinding;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.ListUtils;
import com.vectras.vm.utils.NetworkUtils;
import com.vectras.vm.utils.SimulateKeyEvent;
import com.vectras.vm.utils.UIUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import org.json.JSONObject;


/**
 * @author Dev
 */
public class MainVNCActivity extends VncCanvasActivity {

    private final String TAG = "MainVNCActivity";
    private final int retryLimit = 3;
    private ActivityVncBinding binding;
    private ControlsFragmentBinding bindingControls;
    private DesktopControlsBinding bindingDesktopControls;
    private GameControlsBinding bindingGameControls;
    private SendKeyDialogBinding bindingSendKey;
    public static boolean started = false;
    public static final int KEYBOARD = 10000;
    public static final int QUIT = 10001;
    public static final int HELP = 10002;
    private static boolean monitorMode = false;
    private boolean mouseOn = false;
    private Object lockTime = new Object();
    private static boolean firstConnection;
    String[] functionsArray = {"F1", "F2", "F3", "F4",
            "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12"};

    public boolean ctrlClicked = false;
    public boolean altClicked = false;
    public static MainVNCActivity activity;
    public static LinearLayout desktop;
    public static LinearLayout gamepad;
    private final ArrayList<HashMap<String, Object>> listmapForSendKey = new ArrayList<>();
    private boolean isConnected = false;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        activity = this;

        initializeControlFragment();
        initializeDesktopControl();
        initializeGameControl();
        initializeSendKeyDialog();
        initialize();
    }

    public void setContentView() {
        binding = ActivityVncBinding.inflate(getLayoutInflater());
        bindingControls = binding.controlsfragment;
        bindingDesktopControls = binding.controlsfragment.desktopcontrols;
        bindingGameControls = binding.controlsfragment.gamecontrols;
        bindingSendKey = binding.sendkeysdialog;
        setContentView(binding.getRoot());
    }


    private void initialize() {
        if (MainSettingsManager.getFullscreen(this))
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);

        this.vncCanvas.setFocusableInTouchMode(true);

        Toolbar mainToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mainToolbar);

        setDefaulViewMode();

//        setUIModeMobile();

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

        binding.lnNosignal.setOnClickListener(v -> {
            // In VNCCanvasActivity.
            // Do not attempt to reconnect while connected.
            reconnect();
        });
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

        View vnc_canvas_layout = this.findViewById(R.id.vnc_canvas_layout);
        RelativeLayout.LayoutParams vnc_canvas_layout_params;
        RelativeLayout.LayoutParams vnc_params;
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
            boolean timeQuit = true;
            this.lockTime.notifyAll();
        }
    }

    public void onDestroy() {
        if (NetworkUtils.isPortOpen("127.0.0.1", Config.QMPPort, 100) && started) {
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
        mBuilder.setSingleChoiceItems(items, Config.mouseMode.ordinal(), (dialog, i) -> {
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
        mBuilder.setSingleChoiceItems(items, currentScaleType, (dialog, i) -> {
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
                Log.e(TAG, "setUIModeMobile: ", ex);
        }

        //Apply settings when connection is successful.
        try {
            if (MainSettingsManager.getVNCScaleMode(this) == VNCConfig.oneToOne) {
                AbstractScaling.getById(R.id.itemOneToOne)
                        .setScaleTypeForActivity(this);
            }
        } catch (Exception e) {
            MainSettingsManager.setVNCScaleMode(this, VNCConfig.fitToScreen);
            Log.e(TAG, "oneToOne: ", e);
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
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", (dialog, which) -> {

            setUIModeDesktop();
            alertDialog.dismiss();
        });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", (dialog, which) -> alertDialog.dismiss());
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
                Log.e(TAG, "setUIModeDesktop: ", e);
        }
        //vncCanvas.reSize(false);
        invalidateOptionsMenu();
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
                Log.e(TAG, "onFitToScreen: ", ex);
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
                Log.e(TAG, "onNormalScreen: ", ex);
        }
        return false;
    }

    private boolean onMouse() {

        // Main: For now we disable other modes
        if (Config.disableMouseModes)
            mouseOn = false;


        if (!mouseOn) {
            inputHandler = getInputHandlerById(R.id.itemInputTouchpad);
            connection.setInputMode(inputHandler.getName());
            connection.setFollowMouse(true);
            mouseOn = true;
//        } else {
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
        Thread t = new Thread(() -> {
            try {

                int origX = vncCanvas.mouseX;
                int origY = vncCanvas.mouseY;
                MotionEvent event;

                for (int i = 0; i < 4 * 20; i++) {
                    int x = i * 50;
                    int y = i * 50;
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
                Log.e(TAG, "calibration: ", ex);
            }
        });
        t.start();
    }

    public static boolean toggleKeyboardFlag = true;

    private void onMonitor() {
        if (Config.showToast)
            UIUtils.toastShort(this, "Connecting to QEMU Monitor");

        Thread t = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Log.e(TAG, "onMonitor: ", e);
            }
            monitorMode = true;
            vncCanvas.sendMetaKey1(50, 6);

        });
        t.start();
    }

    private void onVNC() {
        UIUtils.toastShort(this, "Connecting to VM");

        Thread t = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Log.e(TAG, "onVNC: ", e);
            }
            monitorMode = false;
            vncCanvas.sendMetaKey1(49, 6);
        });
        t.start();


    }

    // FIXME: We need this to able to catch complex characters strings like
    // grave and send it as text
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_MULTIPLE && event.getKeyCode() == KeyEvent.KEYCODE_UNKNOWN) {
            vncCanvas.sendText(event.getCharacters());
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
                    Log.e(TAG, "processMigrationResponse: ", ex);
            }
        }
        if (errorStr != null && errorStr.contains("desc")) {
            String descStr = null;

            try {
                JSONObject descObj = new JSONObject(errorStr);
                descStr = descObj.getString("desc");
            } catch (Exception ex) {
                if (Config.debug)
                    Log.e(TAG, "processMigrationResponse: ", ex);
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

        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Pause", (dialog, which) -> onPauseVM());
        alertDialog.show();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (bindingSendKey.sendkeylayout.getVisibility() == View.VISIBLE) {
            bindingSendKey.sendkeylayout.setVisibility(View.GONE);
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

    @Override
    public void onConnected() {
        runOnUiThread(() -> {
            isConnected = true;
            this.resumeVM();
            if (!firstConnection)
                UIUtils.showHints(this);
            firstConnection = true;

            if (Config.mouseMode == Config.MouseMode.External)
                setUIModeDesktop();
            else
                setUIModeMobile(screenMode == VNCScreenMode.FitToScreen);

            binding.lnNosignal.setVisibility(View.GONE);
        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(() -> {
            isConnected = false;
            binding.lnNosignal.setVisibility(View.VISIBLE);
            isQMPPortOpening(firstConnection);
        });
    }

    private void shutdownthisvm() {
        started = false;
        bindingSendKey.sendtextEdittext.setEnabled(false);
        VMManager.shutdownCurrentVM();
        Config.setDefault();
        MainService.stopService();
        finish();
    }

    private void isQMPPortOpening(boolean isFinish) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        if (!FileUtils.isFileExists(Config.getLocalQMPSocketPath())) {
            // Finish when the virtual machine is shut down.
            started = false;
            if (isFinish) {
                finish();
            }
//            } else {
//                DialogUtils.oneDialog(activity, getResources().getString(R.string.there_seems_to_be_no_signal), getResources().getString(R.string.do_you_want_to_exit), getString(R.string.ok), true, R.drawable.cast_24px, true,
//                        this::finish, null);
//            }
        } else {
            // Try reconnect.
            tryReconnect();
        }
    }

    private void tryReconnect() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            int count = 0;

            @Override
            public void run() {
                count++;

                if (!isFinishing() && !isConnected && count < retryLimit) {
                    // Do not attempt to reconnect while connected.
                    reconnect();
                    new Handler(Looper.getMainLooper()).postDelayed(this, 1000);
                }
            }
        }, 0);

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri content_describer = data.getData();
            File selectedFilePath = new File(getPath(content_describer));

            switch (requestCode) {
                case 120:
                    VMManager.changeCDROM(selectedFilePath.getAbsolutePath(), MainVNCActivity.this);
                    break;
                case 889:
                    VMManager.changeFloppyDriveA(selectedFilePath.getAbsolutePath(), MainVNCActivity.this);
                    break;
                case 13335:
                    VMManager.changeFloppyDriveB(selectedFilePath.getAbsolutePath(), MainVNCActivity.this);
                    break;
                case 32:
                    VMManager.changeSDCard(selectedFilePath.getAbsolutePath(), MainVNCActivity.this);
                    break;
                case 1996:
                    VMManager.changeRemovableDevice(VMManager.pendingDeviceID, selectedFilePath.getAbsolutePath(), MainVNCActivity.this);
                    break;
            }
        }
    }

    private void initializeControlFragment() {
        bindingControls.btnPrograms.setVisibility(View.GONE);

        bindingControls.btnVterm.setOnClickListener(v -> {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            // Create and show the dialog.
            LoggerDialogFragment newFragment = new LoggerDialogFragment();
            newFragment.show(ft, "Logger");
        });

        bindingControls.shutdownBtn.setOnClickListener(v -> DialogUtils.threeDialog(activity, getString(R.string.power), getString(R.string.shutdown_or_reset_content), getString(R.string.shutdown), getString(R.string.reset), getString(R.string.close), true, R.drawable.power_settings_new_24px, true,
                this::shutdownthisvm, VMManager::resetCurrentVM, null, null));

        bindingControls.shutdownBtn.setOnLongClickListener(view -> {
            DialogUtils.twoDialog(activity, "Exit", "You will be left here but the virtual machine will continue to run.", "Exit", getString(R.string.cancel), true, R.drawable.exit_to_app_24px, true,
                    () -> {
                        started = false;
                        finish();
                    }, null, null);
            return false;
        });

        bindingControls.kbdBtn.setOnClickListener(v -> new Handler(Looper.getMainLooper()).postDelayed(() -> toggleKeyboardFlag = UIUtils.onKeyboard(activity, toggleKeyboardFlag, vncCanvas), 200));

        bindingControls.kbdBtn.setOnLongClickListener(v -> {
            if (bindingSendKey.sendkeylayout.getVisibility() == View.VISIBLE) {
                bindingSendKey.sendkeylayout.setVisibility(View.GONE);
                bindingSendKey.sendtextEdittext.setEnabled(false);
                bindingSendKey.sendtextEdittext.setEnabled(true);
            } else {
                bindingSendKey.sendkeylayout.setVisibility(View.VISIBLE);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    bindingSendKey.sendtextEdittext.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(bindingSendKey.sendtextEdittext, InputMethodManager.SHOW_IMPLICIT);
                }, 500);
            }
            return false;
        });

        bindingControls.btnMode.setOnClickListener(v -> {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            // Create and show the dialog.
            ControlersOptionsFragment newFragment = new ControlersOptionsFragment();
            newFragment.show(ft, "Controllers");
        });

        bindingControls.btnSettings.setOnClickListener(v -> VMManager.showChangeRemovableDevicesDialog(MainVNCActivity.this, this));
    }

    private void initializeDesktopControl() {
        bindingDesktopControls.upBtn.setOnTouchListener((v, event) -> {
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
        });

        bindingDesktopControls.leftBtn.setOnTouchListener((v, event) -> {
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
        });

        bindingDesktopControls.downBtn.setOnTouchListener((v, event) -> {
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
        });

        bindingDesktopControls.rightBtn.setOnTouchListener((v, event) -> {
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
        });

        bindingDesktopControls.escBtn.setOnClickListener(v -> simulateKeyPress(KeyEvent.KEYCODE_ESCAPE));

        bindingDesktopControls.enterBtn.setOnClickListener(v -> simulateKeyPress(KeyEvent.KEYCODE_ENTER));

        bindingDesktopControls.ctrlBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onClick(View v) {
                if (!ctrlClicked) {
                    sendKey(KeyEvent.KEYCODE_CTRL_LEFT, false);
                    bindingDesktopControls.ctrlBtn.setBackgroundResource(R.drawable.controls_button2);
                    ctrlClicked = true;
                } else {
                    sendKey(KeyEvent.KEYCODE_CTRL_LEFT, true);
                    bindingDesktopControls.ctrlBtn.setBackgroundResource(R.drawable.controls_button1);
                    ctrlClicked = false;
                }
            }
        });

        bindingDesktopControls.altBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onClick(View v) {
                if (!altClicked) {
                    sendKey(KeyEvent.KEYCODE_ALT_LEFT, false);
                    bindingDesktopControls.altBtn.setBackgroundResource(R.drawable.controls_button2);
                    altClicked = true;
                } else {
                    sendKey(KeyEvent.KEYCODE_ALT_LEFT, true);
                    bindingDesktopControls.altBtn.setBackgroundResource(R.drawable.controls_button1);
                    altClicked = false;
                }
            }
        });

        bindingDesktopControls.delBtn.setOnClickListener(v -> simulateKeyPress(KeyEvent.KEYCODE_DEL));

        bindingControls.btnQmp.setOnClickListener(v -> {
            if (monitorMode) {
                onVNC();
                bindingControls.btnQmp.setImageResource(R.drawable.round_terminal_24);
            } else {
                onMonitor();
                bindingControls.btnQmp.setImageResource(R.drawable.round_computer_24);
            }
        });

        bindingDesktopControls.rightClickBtn.setOnClickListener(v -> {
            MotionEvent e = MotionEvent.obtain(1000, 1000, MotionEvent.ACTION_DOWN, vncCanvas.mouseX, vncCanvas.mouseY,
                    0);
            ((TouchpadInputHandler) VncCanvasActivity.inputHandler).rightClick(e);
        });

        bindingDesktopControls.middleBtn.setOnClickListener(v -> {
            MotionEvent e = MotionEvent.obtain(1000, 1000, MotionEvent.ACTION_DOWN, vncCanvas.mouseX, vncCanvas.mouseY,
                    0);
            ((TouchpadInputHandler) VncCanvasActivity.inputHandler).middleClick(e);
        });

        bindingDesktopControls.leftClickBtn.setOnClickListener(v -> {
            MotionEvent e = MotionEvent.obtain(1000, 1000, MotionEvent.ACTION_DOWN, vncCanvas.mouseX, vncCanvas.mouseY,
                    0);
            ((TouchpadInputHandler) VncCanvasActivity.inputHandler).leftClick(e);
        });

        bindingDesktopControls.winBtn.setOnClickListener(v -> {
            sendKey(KeyEvent.KEYCODE_CTRL_LEFT, false);
            sendKey(KeyEvent.KEYCODE_ESCAPE, false);
            sendKey(KeyEvent.KEYCODE_CTRL_LEFT, false);
            sendKey(KeyEvent.KEYCODE_ESCAPE, false);
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.container_function, functionsArray);


        bindingDesktopControls.functions.setAdapter(adapter);
        bindingDesktopControls.functions.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                simulateKeyPress(KeyEvent.KEYCODE_F1);
            } else if (position == 1) {
                simulateKeyPress(KeyEvent.KEYCODE_F2);
            } else if (position == 2) {
                simulateKeyPress(KeyEvent.KEYCODE_F3);
            } else if (position == 3) {
                simulateKeyPress(KeyEvent.KEYCODE_F4);
            } else if (position == 4) {
                simulateKeyPress(KeyEvent.KEYCODE_F5);
            } else if (position == 5) {
                simulateKeyPress(KeyEvent.KEYCODE_F6);
            } else if (position == 6) {
                simulateKeyPress(KeyEvent.KEYCODE_F7);
            } else if (position == 7) {
                simulateKeyPress(KeyEvent.KEYCODE_F8);
            } else if (position == 8) {
                simulateKeyPress(KeyEvent.KEYCODE_F9);
            } else if (position == 9) {
                simulateKeyPress(KeyEvent.KEYCODE_F10);
            } else if (position == 10) {
                simulateKeyPress(KeyEvent.KEYCODE_F11);
            } else if (position == 11) {
                simulateKeyPress(KeyEvent.KEYCODE_F12);
            }
        });
    }

    private void initializeGameControl() {
        bindingGameControls.upGameBtn.setOnTouchListener((v, event) -> {
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
        });

        bindingGameControls.leftGameBtn.setOnTouchListener((v, event) -> {
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
        });

        bindingGameControls.downGameBtn.setOnTouchListener((v, event) -> {
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
        });

        bindingGameControls.rightGameBtn.setOnTouchListener((v, event) -> {
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
        });

        bindingGameControls.joyStick.setVisibility(View.GONE);
        bindingControls.btnFit.setVisibility(View.GONE);
        bindingDesktopControls.tabBtn.setOnClickListener(v -> simulateKeyPress(KeyEvent.KEYCODE_TAB));
        bindingDesktopControls.ctrlaltdelBtn.setOnClickListener(v -> {
            if (bindingSendKey.sendkeylayout.getVisibility() == View.VISIBLE) {
                bindingSendKey.sendkeylayout.setVisibility(View.GONE);
                bindingSendKey.sendtextEdittext.setEnabled(false);
                bindingSendKey.sendtextEdittext.setEnabled(true);
            } else {
                bindingSendKey.sendkeylayout.setVisibility(View.VISIBLE);
            }
            //sendCtrlAtlDelKey();
        });

        bindingGameControls.tabGameBtn.setOnClickListener(v -> simulateKeyPress(KeyEvent.KEYCODE_TAB));
        bindingGameControls.enterGameBtn.setOnClickListener(v -> simulateKeyPress(KeyEvent.KEYCODE_ENTER));
        bindingGameControls.eBtn.setOnClickListener(v -> simulateKeyPress(KeyEvent.KEYCODE_E));
        bindingGameControls.rBtn.setOnClickListener(v -> simulateKeyPress(KeyEvent.KEYCODE_R));
        bindingGameControls.qBtn.setOnClickListener(v -> simulateKeyPress(KeyEvent.KEYCODE_Q));
        bindingGameControls.xBtn.setOnClickListener(v -> simulateKeyPress(KeyEvent.KEYCODE_X));
        bindingGameControls.ctrlGameBtn.setOnClickListener(v -> simulateKeyPress(KeyEvent.KEYCODE_CTRL_LEFT));
        bindingGameControls.spaceBtn.setOnClickListener(v -> simulateKeyPress(KeyEvent.KEYCODE_SPACE));
    }

    private void initializeSendKeyDialog() {
        ListUtils.setupSendKeyListForListmap(listmapForSendKey);
        LinearLayoutManager rvLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        bindingSendKey.sendkeylist.setAdapter(new Recyclerview1Adapter(listmapForSendKey));
        bindingSendKey.sendkeylist.setLayoutManager(rvLayoutManager);
        bindingSendKey.sendkeylist.setHasFixedSize(true);

        bindingSendKey.sendselectallkeyButton.setOnClickListener(v -> vncCanvas.sendCtrlA());
        bindingSendKey.sendcutButton.setOnClickListener(v -> vncCanvas.sendCtrlX());
        bindingSendKey.sendcopykeyButton.setOnClickListener(v -> vncCanvas.sendCtrlC());
        bindingSendKey.sendpastekeyButton.setOnClickListener(v -> vncCanvas.sendCtrlV());
        bindingSendKey.senddelkeyButton.setOnClickListener(v -> {
            dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_FORWARD_DEL));
            dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_FORWARD_DEL));
        });

        bindingSendKey.hidesendkeyButton.setOnClickListener(v -> {
            bindingSendKey.sendkeylayout.setVisibility(View.GONE);
            bindingSendKey.sendtextEdittext.setEnabled(false);
            bindingSendKey.sendtextEdittext.setEnabled(true);
        });

        bindingSendKey.sendtextButton.setOnClickListener(v -> {
            vncCanvas.sendText(bindingSendKey.sendtextEdittext.getText().toString());
            bindingSendKey.sendtextEdittext.setText("");
        });

        bindingSendKey.sendtextEdittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    bindingSendKey.sendtextButton.setVisibility(View.GONE);
                } else {
                    bindingSendKey.sendtextButton.setVisibility(View.VISIBLE);
                }
            }
        });

        bindingSendKey.sendtextEdittext.setOnEditorActionListener((v, actionId, event) -> {
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                vncCanvas.sendText(bindingSendKey.sendtextEdittext.getText().toString());
                bindingSendKey.sendtextEdittext.setText("");
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(bindingSendKey.sendtextEdittext.getWindowToken(), 0);
                handled = true;
            }
            return handled;
        });

        bindingSendKey.sendkeylayout.setVisibility(View.GONE);
        bindingSendKey.sendtextButton.setVisibility(View.GONE);
    }

    public class Recyclerview1Adapter extends RecyclerView.Adapter<Recyclerview1Adapter.ViewHolder> {

        ArrayList<HashMap<String, Object>> _data;

        public Recyclerview1Adapter(ArrayList<HashMap<String, Object>> _arr) {
            _data = _arr;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater _inflater = getLayoutInflater();
            View _v = _inflater.inflate(R.layout.layout_for_send_keys, null);
            RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            _v.setLayoutParams(_lp);
            return new ViewHolder(_v);
        }

        @Override
        public void onBindViewHolder(ViewHolder _holder, final int position) {

            int _position = _holder.getBindingAdapterPosition();

            View _view = _holder.itemView;
            RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            _view.setLayoutParams(_lp);
            final LinearLayout _all = _view.findViewById(R.id.all);
            final TextView _textViewKeyName = _view.findViewById(R.id.textViewKeyName);
            final ImageView _imageViewKey = _view.findViewById(R.id.imageViewKey);
            _textViewKeyName.setTextColor(0xff000000);

            Boolean useIcon = (Boolean) _data.get(_position).get("useIcon");
            if (useIcon != null && useIcon) {
                _textViewKeyName.setVisibility(View.GONE);
                _imageViewKey.setVisibility(View.VISIBLE);
                _imageViewKey.setImageResource(Integer.parseInt(Objects.requireNonNull(_data.get(_position).get("rIcon")).toString()));
            } else {
                _imageViewKey.setVisibility(View.GONE);
                _textViewKeyName.setVisibility(View.VISIBLE);
                _textViewKeyName.setText(Objects.requireNonNull(_data.get(_position).get("keyname")).toString());
            }

            _all.setOnClickListener(_view1 -> {
                if (_position == 0) {
                    sendCtrlAtlDelKey();
                } else {
                    Boolean useKeyEvent = (Boolean) _data.get(_position).get("useKeyEvent");
                    if (useKeyEvent != null && useKeyEvent) {
                        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, Integer.parseInt(Objects.requireNonNull(_data.get(_position).get("keycode")).toString())));
                        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, Integer.parseInt(Objects.requireNonNull(_data.get(_position).get("keycode")).toString())));
                    } else {
                        vncCanvas.sendAKey(Integer.parseInt(Objects.requireNonNull(_data.get(_position).get("keycode")).toString()));
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return _data.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View v) {
                super(v);
            }
        }
    }

    private void simulateKeyPress(int keyEventCode) {
        SimulateKeyEvent.press(this, keyEventCode);
    }

    private void sendKey(int keyEventCode, boolean up) {
        if (up)
            SimulateKeyEvent.releaseNow(this, keyEventCode);
        else SimulateKeyEvent.pressAndHold(this, keyEventCode);
    }

    public void sendCtrlAtlDelKey() {
        vncCanvas.sendCtrlAltDel();
    }

    public boolean leftClick(final MotionEvent e, final int i) {
        Thread t = new Thread(() -> {
            Log.d("SDL", "Mouse Left Click");
            //MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_LEFT, MotionEvent.ACTION_DOWN, 1, -1, -1);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
//					Log.v("SDLSurface", "Interrupted: " + ex);
            }
            //MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_LEFT, MotionEvent.ACTION_UP, 1, -1, -1);
        });
        t.start();
        return true;

    }

    public boolean rightClick(final MotionEvent e, final int i) {
        Thread t = new Thread(() -> {
            Log.d("SDL", "Mouse Right Click");
            //MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_RIGHT, MotionEvent.ACTION_DOWN, 1, -1, -1);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
//					Log.v("SDLSurface", "Interrupted: " + ex);
            }
            //MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_RIGHT, MotionEvent.ACTION_UP, 1, -1, -1);
        });
        t.start();
        return true;

    }

    public boolean middleClick(final MotionEvent e, final int i) {
        Thread t = new Thread(() -> {
            Log.d("SDL", "Mouse Middle Click");
            //MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_MIDDLE, MotionEvent.ACTION_DOWN, 1, -1, -1);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
//                    Log.v("SDLSurface", "Interrupted: " + ex);
            }
            //MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_MIDDLE, MotionEvent.ACTION_UP, 1, -1, -1);
        });
        t.start();
        return true;

    }

    public String getPath(Uri uri) {
        return FileUtils.getPath(this, uri);
    }
}
