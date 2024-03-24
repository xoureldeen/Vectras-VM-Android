/**
 * Copyright (C) 2012-2017 Iordan Iordanov
 * Copyright (C) 2010 Michael A. MacDonald
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */

//
// CanvasView is the Activity for showing VNC Desktop.
//
package com.iiordanov.bVNC;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.iiordanov.android.bc.BCFactory;
import com.iiordanov.bVNC.dialogs.EnterTextDialog;
import com.iiordanov.bVNC.dialogs.MetaKeyDialog;
import com.iiordanov.bVNC.input.MetaKeyBean;
import com.iiordanov.bVNC.input.Panner;
import com.iiordanov.bVNC.input.RemoteCanvasHandler;
import com.iiordanov.bVNC.input.RemoteClientsInputListener;
import com.iiordanov.bVNC.input.RemoteKeyboard;
import com.iiordanov.bVNC.input.TouchInputHandler;
import com.iiordanov.bVNC.input.TouchInputHandlerDirectDragPan;
import com.iiordanov.bVNC.input.TouchInputHandlerDirectSwipePan;
import com.iiordanov.bVNC.input.TouchInputHandlerSingleHanded;
import com.iiordanov.bVNC.input.TouchInputHandlerTouchpad;
import com.iiordanov.bVNC.protocol.RemoteConnection;
import com.iiordanov.bVNC.protocol.RemoteConnectionFactory;
import com.iiordanov.util.SamsungDexUtils;
import com.iiordanov.util.UriIntentParser;
import com.undatech.opaque.Connection;
import com.undatech.opaque.ConnectionSettings;
import com.undatech.opaque.MessageDialogs;
import com.undatech.opaque.RemoteClientLibConstants;
import com.undatech.opaque.dialogs.SelectTextElementFragment;
import com.undatech.opaque.util.FileUtils;
import com.undatech.opaque.util.OnTouchViewMover;
import com.undatech.opaque.util.RemoteToolbar;
import com.undatech.remoteClientUi.R;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

@SuppressLint("ClickableViewAccessibility")
public class RemoteCanvasActivity extends AppCompatActivity implements
        SelectTextElementFragment.OnFragmentDismissedListener {

    public static final int[] inputModeIds = {R.id.itemInputTouchpad,
            R.id.itemInputTouchPanZoomMouse,
            R.id.itemInputDragPanZoomMouse,
            R.id.itemInputSingleHanded};
    public static final Map<Integer, String> inputModeMap;
    private final static String TAG = "RemoteCanvasActivity";
    private static final int[] scalingModeIds = {R.id.itemZoomable, R.id.itemFitToScreen,
            R.id.itemOneToOne};

    static {
        Map<Integer, String> temp = new HashMap<>();
        temp.put(R.id.itemInputTouchpad, TouchInputHandlerTouchpad.ID);
        temp.put(R.id.itemInputDragPanZoomMouse, TouchInputHandlerDirectDragPan.ID);
        temp.put(R.id.itemInputTouchPanZoomMouse, TouchInputHandlerDirectSwipePan.ID);
        temp.put(R.id.itemInputSingleHanded, TouchInputHandlerSingleHanded.ID);
        inputModeMap = Collections.unmodifiableMap(temp);
    }

    final long hideToolbarDelay = 2500;
    TouchInputHandler touchInputHandler;
    Panner panner;
    Handler handler;
    RelativeLayout layoutKeys;
    LinearLayout layoutArrowKeys;
    ImageButton keyCtrl;
    boolean keyCtrlToggled;
    ImageButton keySuper;
    boolean keySuperToggled;
    ImageButton keyAlt;
    boolean keyAltToggled;
    ImageButton keyTab;
    ImageButton keyEsc;
    ImageButton keyShift;
    boolean keyShiftToggled;
    ImageButton keyUp;
    ImageButton keyDown;
    ImageButton keyLeft;
    ImageButton keyRight;
    boolean extraKeysHidden = false;
    volatile boolean softKeyboardUp;
    RemoteToolbar toolbar;
    View rootView;
    ActionBarHider actionBarHider = new ActionBarHider();
    ActionBarShower actionBarShower = new ActionBarShower();
    private Vibrator myVibrator;
    private RemoteCanvas canvas;
    private RemoteConnection remoteConnection;
    private MenuItem[] inputModeMenuItems;
    private MenuItem[] scalingModeMenuItems;
    private TouchInputHandler[] inputModeHandlers;
    private Connection connection;
    public RemoteClientsInputListener inputListener;

    /**
     * This runnable enables immersive mode.
     */
    private final Runnable immersiveEnabler = new Runnable() {
        public void run() {
            try {
                if (Utils.querySharedPreferenceBoolean(RemoteCanvasActivity.this,
                        Constants.disableImmersiveTag)) {
                    return;
                }

                if (Constants.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    canvas.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }

            } catch (Exception e) {
                Log.d(TAG, "Ignored Exception while enabling immersive mode");
            }
        }
    };
    /**
     * This runnable disables immersive mode.
     */
    private final Runnable immersiveDisabler = new Runnable() {
        public void run() {
            try {
                if (Constants.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    canvas.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                }

            } catch (Exception e) {
                Log.d(TAG, "Ignored Exception while disabling immersive mode");
            }
        }
    };
    /**
     * This runnable fixes things up after a rotation.
     */
    private final Runnable rotationCorrector = () -> {
        try {
            correctAfterRotation();
        } catch (Exception e) {
            Log.d(TAG, "Ignoring Exception on rotationCorrector run.");
        }
    };

    private void correctAfterRotation() throws Exception {
        Log.d(TAG, "correctAfterRotation");
        canvas.waitUntilInflated();
        // Its quite common to see NullPointerExceptions here when this function is called
        // at the point of disconnection. Hence, we catch and ignore the error.
        float oldScale = canvas.canvasZoomer.getZoomFactor();
        int x = canvas.absoluteXPosition;
        int y = canvas.absoluteYPosition;
        canvas.canvasZoomer.setScaleTypeForActivity(this);
        float newScale = canvas.canvasZoomer.getZoomFactor();
        canvas.canvasZoomer.changeZoom(this, oldScale / newScale, 0, 0);
        newScale = canvas.canvasZoomer.getZoomFactor();
        if (newScale <= oldScale &&
                canvas.canvasZoomer.getScaleType() != ImageView.ScaleType.FIT_CENTER) {
            canvas.absoluteXPosition = x;
            canvas.absoluteYPosition = y;
            canvas.resetScroll();
        }
        remoteConnection.correctAfterRotation();
    }

    private MetaKeyBean lastSentKey;

    /**
     * Enables sticky immersive mode if supported.
     */
    private void enableImmersive() {
        handler.removeCallbacks(immersiveEnabler);
        handler.postDelayed(immersiveEnabler, 200);
    }

    /**
     * Disables sticky immersive mode.
     */
    private void disableImmersive() {
        handler.removeCallbacks(immersiveDisabler);
        handler.postDelayed(immersiveDisabler, 200);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableImmersive();
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    @Override
    public void onCreate(Bundle icicle) {
        Log.d(TAG, "OnCreate called");
        super.onCreate(icicle);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Utils.showMenu(this);

        setContentView(R.layout.canvas);

        canvas = findViewById(R.id.canvas);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canvas.setDefaultFocusHighlightEnabled(false);
        }
        if (android.os.Build.VERSION.SDK_INT >= 9) {
            android.os.StrictMode.ThreadPolicy policy = new android.os.StrictMode.ThreadPolicy.Builder().permitAll().build();
            android.os.StrictMode.setThreadPolicy(policy);
        }

        myVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener
                (visibility -> {
                    try {
                        remoteConnection.correctAfterRotation();
                    } catch (Exception e) {
                        Log.d(TAG, "Ignoring Exception during SystemUiVisibilityChangeListener execution");
                    }
                });

        Runnable setModes = () -> {
            try {
                setModes();
            } catch (NullPointerException e) {
                Log.d(TAG, "Ignored Exception while running setModes");
            }
        };
        Runnable hideKeyboardAndExtraKeys = () -> {
            try {
                hideKeyboardAndExtraKeys();
            } catch (NullPointerException e) {
                Log.d(TAG, "Ignoring NullPointerException while running hideKeyboardAndExtraKeys.");
            }
        };

        String vvFileName = null;
        if (Utils.isOpaque(this)) {
            vvFileName = retrieveVvFileFromIntent(getIntent());
            connection = getOpaqueConnection(vvFileName);
        } else {
            connection = getNonOpaqueConnection();
        }
        if (connection != null && connection.isReadyForConnection()) {
            remoteConnection = new RemoteConnectionFactory(this, connection, canvas, vvFileName, hideKeyboardAndExtraKeys).build();
            handler = new RemoteCanvasHandler(this, canvas, remoteConnection, connection, setModes);
            handler.sendEmptyMessage(RemoteClientLibConstants.REINIT_SESSION);
            continueConnecting();
        }


        AbstractScaling.getById(R.id.itemFitToScreen).setScaleTypeForActivity(this);
        showPanningState(false);

        boolean inputModeSet = setInputMode(R.id.itemInputTouchpad);
        if (inputModeSet)
            android.util.Log.d(TAG, "OnCreate complete");
    }

    @SuppressLint("SourceLockedOrientationActivity")
    Connection getNonOpaqueConnection() {
        Connection connection;
        if (Utils.querySharedPreferenceBoolean(this, Constants.keepScreenOnTag))
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Utils.querySharedPreferenceBoolean(this, Constants.forceLandscapeTag))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        Intent i = getIntent();
        Uri data = i.getData();
        boolean isSupportedScheme = isSupportedScheme(data);
        if (isSupportedScheme || !Utils.isNullOrEmptry(i.getType())) {
            connection = handleSupportedUri(data);
        } else {
            connection = loadSerializedConnection(i);
        }
        return connection;
    }

    private Connection loadSerializedConnection(Intent i) {
        Log.d(TAG, "Initializing serialized connection");
        Connection connection = new ConnectionBean(this);
        Bundle extras = i.getExtras();

        if (extras != null) {
            Log.d(TAG, "Loading values from serialized connection");
            connection.populateFromContentValues((ContentValues) extras.getParcelable(Utils.getConnectionString(this)));
            connection.load(this);
        }
        return connection;
    }

    private Connection handleSupportedUri(Uri data) {
        Log.d(TAG, "Initializing classic connection from Intent.");
        if (isMasterPasswordEnabled()) {
            Utils.showFatalErrorMessage(this, getResources().getString(R.string.master_password_error_intents_not_supported));
            return null;
        }

        Connection connection = createConnectionFromUri(data);
        showConnectionScreenOrExitIfNotReadyForConnecting(connection);
        return connection;
    }

    private Connection createConnectionFromUri(Uri data) {
        Connection connection = UriIntentParser.loadFromUriOrCreateNew(data, this);
        String host = null;
        if (data != null) {
            host = data.getHost();
        }
        if (host != null && !host.startsWith(Utils.getConnectionString(this))) {
            UriIntentParser.parseFromUri(this, connection, data);
        }
        return connection;
    }

    private boolean isSupportedScheme(Uri data) {
        boolean isSupportedScheme = false;
        if (data != null) {
            String s = data.getScheme();
            isSupportedScheme = "rdp".equals(s) || "spice".equals(s) || "vnc".equals(s);
        }
        return isSupportedScheme;
    }

    private void showConnectionScreenOrExitIfNotReadyForConnecting(Connection connection) {
        // we need to save the connection to display the loading screen, so otherwise we should exit
        if (!connection.isReadyForConnection()) {
            Toast.makeText(this, getString(R.string.error_uri_noinfo_nosave), Toast.LENGTH_LONG).show();
            if (connection.isReadyToBeSaved()) {
                Log.i(TAG, "Exiting - Insufficent information to connect and connection was not saved.");
            } else {
                Log.i(TAG, "Insufficent information to connect, showing connection dialog.");
                // launch appropriate activity
                Class cls = bVNC.class;
                if (Utils.isRdp(this)) {
                    cls = aRDP.class;
                } else if (Utils.isSpice(this)) {
                    cls = aSPICE.class;
                }
                Intent Intent = new Intent(this, cls);
                startActivity(Intent);
            }
            Utils.justFinish(this);
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private Connection getOpaqueConnection(String vvFileName) {
        Connection connection;
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        Intent i = getIntent();
        if (vvFileName == null) {
            android.util.Log.d(TAG, "Initializing session from connection settings.");
            connection = (ConnectionSettings) i.getSerializableExtra(Constants.opaqueConnectionSettingsClassPath);
        } else {
            connection = new ConnectionSettings(RemoteClientLibConstants.DEFAULT_SETTINGS_FILE);
            connection.load(getApplicationContext());
        }
        return connection;
    }

    @SuppressLint("RtlHardcoded")
    public void continueConnecting() {
        android.util.Log.d(TAG, "continueConnecting");
        // Initialize and define actions for on-screen keys.
        initializeOnScreenKeys();

        canvas.setFocusableInTouchMode(true);
        canvas.setDrawingCacheEnabled(false);

        // This code detects when the soft keyboard is up and sets an appropriate visibleHeight in vncCanvas.
        // When the keyboard is gone, it resets visibleHeight and pans zero distance to prevent us from being
        // below the desktop image (if we scrolled all the way down when the keyboard was up).
        // TODO: Move this into a separate thread, and post the visibility changes to the handler.
        //       to avoid occupying the UI thread with this.
        rootView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> relayoutViews(rootView));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);

        if (Utils.querySharedPreferenceBoolean(this, Constants.leftHandedModeTag)) {
            params.gravity = Gravity.CENTER | Gravity.LEFT;
        } else {
            params.gravity = Gravity.CENTER | Gravity.RIGHT;
        }

        panner = new Panner(this, handler);

        toolbar = (RemoteToolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        toolbar.getBackground().setAlpha(64);
        toolbar.setLayoutParams(params);
        setSupportActionBar(toolbar);
        showActionBar();
    }

    void relayoutViews(View rootView) {
        android.util.Log.d(TAG, "onGlobalLayout: start");
        if (canvas == null) {
            android.util.Log.d(TAG, "onGlobalLayout: canvas null, returning");
            return;
        }

        Rect r = new Rect();

        rootView.getWindowVisibleDisplayFrame(r);
        android.util.Log.d(TAG, "onGlobalLayout: getWindowVisibleDisplayFrame: " + r);

        // To avoid setting the visible height to a wrong value after an screen unlock event
        // (when r.bottom holds the width of the screen rather than the height due to a rotation)
        // we make sure r.top is zero (i.e. there is no notification bar and we are in full-screen mode)
        // It's a bit of a hack.
        // One additional situation that needed handling was that devices with notches / cutouts don't
        // ever have r.top equal to zero. so a special case for them.
        Rect re = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(re);
        if (r.top == 0 || re.top > 0) {
            if (canvas.getDrawable() != null) {
                android.util.Log.d(TAG, "onGlobalLayout: Setting VisibleDesktopHeight to: " + (r.bottom - re.top));
                canvas.setVisibleDesktopHeight(r.bottom - re.top);
                canvas.relativePan(0, 0);
            } else {
                android.util.Log.d(TAG, "onGlobalLayout: canvas.myDrawable is null");
            }
        } else {
            android.util.Log.d(TAG, "onGlobalLayout: Found r.top to be non-zero");
        }

        // Enable/show the toolbar if the keyboard is gone, and disable/hide otherwise.
        // We detect the keyboard if more than 19% of the screen is covered.
        // Use the visible display frame of the decor view to compute notch dimensions.
        int rootViewHeight = rootView.getHeight();

        int layoutKeysBottom = layoutKeys.getBottom();
        int toolbarBottom = toolbar.getBottom();
        int rootViewBottom = layoutKeys.getRootView().getBottom();
        int diffArrowKeysPosition = r.right - re.left - layoutArrowKeys.getRight();
        int diffLayoutKeysPosition = r.bottom - re.top - layoutKeysBottom;
        int diffToolbarPosition = r.bottom - re.top - toolbarBottom - r.bottom / 2;
        int diffToolbarPositionRightAbsolute = r.right - toolbar.getWidth();
        int diffToolbarPositionTopAbsolute = r.bottom - re.top - toolbar.getHeight() - r.bottom / 2;
        android.util.Log.d(TAG, "onGlobalLayout: before: r.bottom: " + r.bottom +
                " rootViewHeight: " + rootViewHeight + " re.top: " + re.top + " re.bottom: " + re.bottom +
                " layoutKeysBottom: " + layoutKeysBottom + " rootViewBottom: " + rootViewBottom + " toolbarBottom: " + toolbarBottom +
                " diffLayoutKeysPosition: " + diffLayoutKeysPosition + " diffToolbarPosition: " + diffToolbarPosition);

        boolean softKeyboardPositionChanged = false;
        if (r.bottom > rootViewHeight * 0.81) {
            android.util.Log.d(TAG, "onGlobalLayout: Less than 19% of screen is covered");
            if (softKeyboardUp) {
                softKeyboardPositionChanged = true;
            }
            softKeyboardUp = false;

            // Soft Kbd gone, shift the meta keys and arrows down.
            if (layoutKeys != null) {
                android.util.Log.d(TAG, "onGlobalLayout: shifting on-screen buttons down by: " + diffLayoutKeysPosition);
                layoutKeys.offsetTopAndBottom(diffLayoutKeysPosition);
                if (!connection.getUseLastPositionToolbar() || !connection.getUseLastPositionToolbarMoved()) {
                    toolbar.offsetTopAndBottom(diffToolbarPosition);
                } else {
                    toolbar.setPositionToMakeVisible(connection.getUseLastPositionToolbarX(),
                            connection.getUseLastPositionToolbarY(),
                            r.right,
                            r.bottom,
                            diffToolbarPositionRightAbsolute,
                            diffToolbarPositionTopAbsolute);
                }
                android.util.Log.d(TAG, "onGlobalLayout: shifting arrow keys by: " + diffArrowKeysPosition);
                layoutArrowKeys.offsetLeftAndRight(diffArrowKeysPosition);
                if (softKeyboardPositionChanged) {
                    android.util.Log.d(TAG, "onGlobalLayout: hiding on-screen buttons");
                    setExtraKeysVisibility(View.GONE, false);
                    canvas.invalidate();
                }
            }
        } else {
            android.util.Log.d(TAG, "onGlobalLayout: More than 19% of screen is covered");
            softKeyboardUp = true;

            //  Soft Kbd up, shift the meta keys and arrows up.
            if (layoutKeys != null) {
                Log.d(TAG, "onGlobalLayout: shifting on-screen buttons up by: " + diffLayoutKeysPosition);
                layoutKeys.offsetTopAndBottom(diffLayoutKeysPosition);
                if (!connection.getUseLastPositionToolbar() || !connection.getUseLastPositionToolbarMoved()) {
                    toolbar.offsetTopAndBottom(diffToolbarPosition);
                } else {
                    toolbar.setPositionToMakeVisible(connection.getUseLastPositionToolbarX(),
                            connection.getUseLastPositionToolbarY(),
                            r.right,
                            r.bottom,
                            diffToolbarPositionRightAbsolute,
                            diffToolbarPositionTopAbsolute);
                }
                Log.d(TAG, "onGlobalLayout: shifting arrow keys by: " + diffArrowKeysPosition);
                layoutArrowKeys.offsetLeftAndRight(diffArrowKeysPosition);
                if (extraKeysHidden) {
                    Log.d(TAG, "onGlobalLayout: on-screen buttons should be hidden");
                    setExtraKeysVisibility(View.GONE, false);
                } else {
                    Log.d(TAG, "onGlobalLayout: on-screen buttons should be showing");
                    setExtraKeysVisibility(View.VISIBLE, true);
                }
                canvas.invalidate();
            }
        }
        if (layoutKeys != null) {
            layoutKeysBottom = layoutKeys.getBottom();
            rootViewBottom = layoutKeys.getRootView().getBottom();
        }
        android.util.Log.d(TAG, "onGlobalLayout: after: r.bottom: " + r.bottom +
                " rootViewHeight: " + rootViewHeight + " re.top: " + re.top + " re.bottom: " + re.bottom +
                " layoutKeysBottom: " + layoutKeysBottom + " rootViewBottom: " + rootViewBottom + " toolbarBottom: " + toolbarBottom +
                " diffLayoutKeysPosition: " + diffLayoutKeysPosition + " diffToolbarPosition: " + diffToolbarPosition);
    }

    /**
     * Retrieves a vv file from the intent if possible and returns the path to it.
     *
     * @param i intent that started the activity
     * @return the vv file name or NULL if no file was discovered.
     */
    private String retrieveVvFileFromIntent(Intent i) {
        final Uri data = i.getData();
        String vvFileName = null;
        final String tempVvFile = getFilesDir() + "/tempfile.vv";
        int msgId = 0;

        android.util.Log.d(TAG, "Got intent: " + i);

        if (data != null) {
            android.util.Log.d(TAG, "Got data: " + data);
            final String dataString = data.toString();
            if (dataString.startsWith("http")) {
                android.util.Log.d(TAG, "Intent is with http scheme.");
                msgId = R.string.error_failed_to_download_vv_http;
                FileUtils.deleteFile(tempVvFile);

                // Spin up a thread to grab the file over the network.
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        try {
                            // Download the file and write it out.
                            URL url = new URL(data.toString());
                            File file = new File(tempVvFile);
                            URLConnection ucon = url.openConnection();
                            FileUtils.outputToFile(ucon.getInputStream(), file);

                            synchronized (RemoteCanvasActivity.this) {
                                RemoteCanvasActivity.this.notify();
                            }
                        } catch (IOException e) {
                            int what = RemoteClientLibConstants.VV_OVER_HTTP_FAILURE;
                            if (dataString.startsWith("https")) {
                                what = RemoteClientLibConstants.VV_OVER_HTTPS_FAILURE;
                            }
                            // Quit with an error we could not download the .vv file.
                            handler.sendEmptyMessage(what);
                        }
                    }
                };
                t.start();

                synchronized (this) {
                    try {
                        this.wait(RemoteClientLibConstants.VV_GET_FILE_TIMEOUT);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    vvFileName = tempVvFile;
                }
            } else if (dataString.startsWith("file")) {
                android.util.Log.d(TAG, "Intent is with file scheme.");
                msgId = R.string.error_failed_to_obtain_vv_file;
                vvFileName = data.getPath();
            } else if (dataString.startsWith("content")) {
                android.util.Log.d(TAG, "Intent is with content scheme.");
                msgId = R.string.error_failed_to_obtain_vv_content;
                FileUtils.deleteFile(tempVvFile);

                try {
                    FileUtils.outputToFile(getContentResolver().openInputStream(data), new File(tempVvFile));
                    vvFileName = tempVvFile;
                } catch (IOException e) {
                    android.util.Log.e(TAG, "Could not write temp file: IOException.");
                    e.printStackTrace();
                } catch (SecurityException e) {
                    android.util.Log.e(TAG, "Could not write temp file: SecurityException.");
                    e.printStackTrace();
                }
            }

            // Check if we were successful in obtaining a file and put up an error dialog if not.
            if (dataString.startsWith("http") || dataString.startsWith("file") || dataString.startsWith("content")) {
                if (vvFileName == null)
                    MessageDialogs.displayMessageAndFinish(this, msgId, R.string.error_dialog_title);
            }
            android.util.Log.d(TAG, "Got filename: " + vvFileName);
        }

        return vvFileName;
    }

    public void extraKeysToggle(MenuItem m) {
        if (layoutKeys.getVisibility() == View.VISIBLE) {
            extraKeysHidden = true;
            setExtraKeysVisibility(View.GONE, false);
        } else {
            extraKeysHidden = false;
            setExtraKeysVisibility(View.VISIBLE, true);
        }
        setKeyStowDrawableAndVisibility(m);
        relayoutViews(rootView);
    }

    private void setKeyStowDrawableAndVisibility(MenuItem m) {
        if (m == null) {
            return;
        }
        Drawable replacer;
        m.setVisible(connection.getExtraKeysToggleType() != Constants.EXTRA_KEYS_OFF);
        if (layoutKeys.getVisibility() == View.GONE)
            replacer = getResources().getDrawable(R.drawable.showkeys);
        else
            replacer = getResources().getDrawable(R.drawable.hidekeys);

        m.setIcon(replacer);
    }

    public void sendShortVibration() {
        if (myVibrator != null) {
            myVibrator.vibrate(Constants.SHORT_VIBRATION);
        } else {
            Log.i(TAG, "Device cannot vibrate, not sending vibration");
        }
    }

    /**
     * Initializes the on-screen keys for meta keys and arrow keys.
     */
    private void initializeOnScreenKeys() {
        layoutKeys = (RelativeLayout) findViewById(R.id.layoutKeys);
        layoutArrowKeys = (LinearLayout) findViewById(R.id.layoutArrowKeys);

        // Define action of tab key and meta keys.
        keyTab = (ImageButton) findViewById(R.id.keyTab);
        keyTab.setOnTouchListener((arg0, e) -> {
            RemoteKeyboard k = remoteConnection.getKeyboard();
            int key = KeyEvent.KEYCODE_TAB;
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                BCFactory.getInstance().getBCHaptic().performLongPressHaptic(canvas);
                keyTab.setImageResource(R.drawable.tabon);
                k.repeatKeyEvent(key, new KeyEvent(e.getAction(), key));
                return true;
            } else if (e.getAction() == MotionEvent.ACTION_UP) {
                keyTab.setImageResource(R.drawable.taboff);
                resetOnScreenKeys(0);
                k.stopRepeatingKeyEvent();
                return true;
            }
            return false;
        });

        keyEsc = (ImageButton) findViewById(R.id.keyEsc);
        keyEsc.setOnTouchListener((arg0, e) -> {
            RemoteKeyboard k = remoteConnection.getKeyboard();
            int key = 111; /* KEYCODE_ESCAPE */
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                BCFactory.getInstance().getBCHaptic().performLongPressHaptic(canvas);
                keyEsc.setImageResource(R.drawable.escon);
                k.repeatKeyEvent(key, new KeyEvent(e.getAction(), key));
                return true;
            } else if (e.getAction() == MotionEvent.ACTION_UP) {
                keyEsc.setImageResource(R.drawable.escoff);
                resetOnScreenKeys(0);
                k.stopRepeatingKeyEvent();
                return true;
            }
            return false;
        });

        keyCtrl = (ImageButton) findViewById(R.id.keyCtrl);
        keyCtrl.setOnClickListener(arg0 -> {
            boolean on = remoteConnection.getKeyboard().onScreenCtrlToggle();
            keyCtrlToggled = false;
            if (on)
                keyCtrl.setImageResource(R.drawable.ctrlon);
            else
                keyCtrl.setImageResource(R.drawable.ctrloff);
        });

        keyCtrl.setOnLongClickListener(arg0 -> {
            sendShortVibration();
            boolean on = remoteConnection.getKeyboard().onScreenCtrlToggle();
            keyCtrlToggled = true;
            if (on)
                keyCtrl.setImageResource(R.drawable.ctrlon);
            else
                keyCtrl.setImageResource(R.drawable.ctrloff);
            return true;
        });

        keySuper = (ImageButton) findViewById(R.id.keySuper);
        keySuper.setOnClickListener(arg0 -> {
            boolean on = remoteConnection.getKeyboard().onScreenSuperToggle();
            keySuperToggled = false;
            if (on)
                keySuper.setImageResource(R.drawable.superon);
            else
                keySuper.setImageResource(R.drawable.superoff);
        });

        keySuper.setOnLongClickListener(arg0 -> {
            sendShortVibration();
            boolean on = remoteConnection.getKeyboard().onScreenSuperToggle();
            keySuperToggled = true;
            if (on)
                keySuper.setImageResource(R.drawable.superon);
            else
                keySuper.setImageResource(R.drawable.superoff);
            return true;
        });

        keyAlt = (ImageButton) findViewById(R.id.keyAlt);
        keyAlt.setOnClickListener(arg0 -> {
            boolean on = remoteConnection.getKeyboard().onScreenAltToggle();
            keyAltToggled = false;
            if (on)
                keyAlt.setImageResource(R.drawable.alton);
            else
                keyAlt.setImageResource(R.drawable.altoff);
        });

        keyAlt.setOnLongClickListener(arg0 -> {
            sendShortVibration();
            boolean on = remoteConnection.getKeyboard().onScreenAltToggle();
            keyAltToggled = true;
            if (on)
                keyAlt.setImageResource(R.drawable.alton);
            else
                keyAlt.setImageResource(R.drawable.altoff);
            return true;
        });

        keyShift = (ImageButton) findViewById(R.id.keyShift);
        keyShift.setOnClickListener(arg0 -> {
            boolean on = remoteConnection.getKeyboard().onScreenShiftToggle();
            keyShiftToggled = false;
            if (on)
                keyShift.setImageResource(R.drawable.shifton);
            else
                keyShift.setImageResource(R.drawable.shiftoff);
        });

        keyShift.setOnLongClickListener(arg0 -> {
            sendShortVibration();
            boolean on = remoteConnection.getKeyboard().onScreenShiftToggle();
            keyShiftToggled = true;
            if (on)
                keyShift.setImageResource(R.drawable.shifton);
            else
                keyShift.setImageResource(R.drawable.shiftoff);
            return true;
        });

        // TODO: Evaluate whether I should instead be using:
        // remoteConnection.sendMetaKey(MetaKeyBean.keyArrowLeft);

        // Define action of arrow keys.
        keyUp = (ImageButton) findViewById(R.id.keyUpArrow);
        keyUp.setOnTouchListener((arg0, e) -> {
            RemoteKeyboard k = remoteConnection.getKeyboard();
            int key = KeyEvent.KEYCODE_DPAD_UP;
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                sendShortVibration();
                keyUp.setImageResource(R.drawable.upon);
                k.repeatKeyEvent(key, new KeyEvent(e.getAction(), key));
                return true;
            } else if (e.getAction() == MotionEvent.ACTION_UP) {
                keyUp.setImageResource(R.drawable.upoff);
                resetOnScreenKeys(0);
                k.stopRepeatingKeyEvent();
                return true;
            }
            return false;
        });

        keyDown = (ImageButton) findViewById(R.id.keyDownArrow);
        keyDown.setOnTouchListener((arg0, e) -> {
            RemoteKeyboard k = remoteConnection.getKeyboard();
            int key = KeyEvent.KEYCODE_DPAD_DOWN;
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                sendShortVibration();
                keyDown.setImageResource(R.drawable.downon);
                k.repeatKeyEvent(key, new KeyEvent(e.getAction(), key));
                return true;
            } else if (e.getAction() == MotionEvent.ACTION_UP) {
                keyDown.setImageResource(R.drawable.downoff);
                resetOnScreenKeys(0);
                k.stopRepeatingKeyEvent();
                return true;
            }
            return false;
        });

        keyLeft = (ImageButton) findViewById(R.id.keyLeftArrow);
        keyLeft.setOnTouchListener((arg0, e) -> {
            RemoteKeyboard k = remoteConnection.getKeyboard();
            int key = KeyEvent.KEYCODE_DPAD_LEFT;
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                sendShortVibration();
                keyLeft.setImageResource(R.drawable.lefton);
                k.repeatKeyEvent(key, new KeyEvent(e.getAction(), key));
                return true;
            } else if (e.getAction() == MotionEvent.ACTION_UP) {
                keyLeft.setImageResource(R.drawable.leftoff);
                resetOnScreenKeys(0);
                k.stopRepeatingKeyEvent();
                return true;
            }
            return false;
        });

        keyRight = (ImageButton) findViewById(R.id.keyRightArrow);
        keyRight.setOnTouchListener((arg0, e) -> {
            RemoteKeyboard k = remoteConnection.getKeyboard();
            int key = KeyEvent.KEYCODE_DPAD_RIGHT;
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                sendShortVibration();
                keyRight.setImageResource(R.drawable.righton);
                k.repeatKeyEvent(key, new KeyEvent(e.getAction(), key));
                return true;
            } else if (e.getAction() == MotionEvent.ACTION_UP) {
                keyRight.setImageResource(R.drawable.rightoff);
                resetOnScreenKeys(0);
                k.stopRepeatingKeyEvent();
                return true;
            }
            return false;
        });
    }

    /**
     * Resets the state and image of the on-screen keys.
     */
    private int resetOnScreenKeys(int keyCode) {
        // Do not reset on-screen keys if keycode is SHIFT.
        switch (keyCode) {
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                return keyCode;
        }
        if (!keyCtrlToggled) {
            keyCtrl.setImageResource(R.drawable.ctrloff);
            remoteConnection.getKeyboard().onScreenCtrlOff();
        }
        if (!keyAltToggled) {
            keyAlt.setImageResource(R.drawable.altoff);
            remoteConnection.getKeyboard().onScreenAltOff();
        }
        if (!keySuperToggled) {
            keySuper.setImageResource(R.drawable.superoff);
            remoteConnection.getKeyboard().onScreenSuperOff();
        }
        if (!keyShiftToggled) {
            keyShift.setImageResource(R.drawable.shiftoff);
            remoteConnection.getKeyboard().onScreenShiftOff();
        }
        return keyCode;
    }

    /**
     * Sets the visibility of the extra keys appropriately.
     */
    private void setExtraKeysVisibility(int visibility, boolean forceVisible) {
        Configuration config = getResources().getConfiguration();

        boolean makeVisible = forceVisible;
        if (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO)
            makeVisible = true;

        if (!extraKeysHidden && makeVisible &&
                connection.getExtraKeysToggleType() == Constants.EXTRA_KEYS_ON) {
            layoutKeys.setVisibility(View.VISIBLE);
            layoutKeys.invalidate();
            return;
        }

        if (visibility == View.GONE) {
            layoutKeys.setVisibility(View.GONE);
            layoutKeys.invalidate();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause called.");
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(canvas.getWindowToken(), 0);
        } catch (NullPointerException e) {
            Log.d(TAG, "Ignoring NullPointerException during onPause");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume called.");
        try {
            canvas.postInvalidateDelayed(600);
        } catch (NullPointerException e) {
            Log.d(TAG, "Ignoring NullPointerException during onResume");
        }
    }

    /**
     * Set modes on start to match what is specified in the ConnectionBean;
     * color mode (already done) scaling, input mode
     */
    void setModes() {
        Log.d(TAG, "setModes");
        setInputHandler(getInputHandlerByName(connection.getInputMode()));
        AbstractScaling.getByScaleType(connection.getScaleMode()).setScaleTypeForActivity(this);
        initializeOnScreenKeys();
        try {
            COLORMODEL cm = COLORMODEL.valueOf(connection.getColorModel());
            remoteConnection.setColorModel(cm);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        canvas.setFocusableInTouchMode(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canvas.setFocusedByDefault(true);
        }
        canvas.requestFocus();
        canvas.setDrawingCacheEnabled(false);

        SamsungDexUtils.INSTANCE.dexMetaKeyCapture(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == R.layout.entertext) {
            return new EnterTextDialog(this);
        } else if (id == R.id.itemHelpInputMode) {
            return createHelpDialog();
        }

        // Default to meta key dialog
        return new MetaKeyDialog(this);
    }

    /**
     * Creates the help dialog for this activity.
     */
    private Dialog createHelpDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this)
                .setMessage(R.string.input_mode_help_text)
                .setPositiveButton(R.string.close,
                        (dialog, whichButton) -> {
                            // We don't have to do anything.
                        });
        Dialog d = adb.setView(new ListView(this)).create();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = d.getWindow();
        if (window != null) {
            lp.copyFrom(window.getAttributes());
        }
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        d.show();
        d.getWindow().setAttributes(lp);
        return d;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        if (dialog instanceof ConnectionSettable)
            ((ConnectionSettable) dialog).setConnection(connection);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        enableImmersive();
        try {
            setExtraKeysVisibility(View.GONE, false);

            // Correct a few times just in case. There is no visual effect.
            handler.postDelayed(rotationCorrector, 300);
        } catch (NullPointerException e) {
            Log.d(TAG, "Ignoring NullPointerException during onConfigurationChanged");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart called.");
        try {
            canvas.postInvalidateDelayed(800);
        } catch (NullPointerException e) {
            Log.d(TAG, "Ignoring NullPointerException during onStart");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop called.");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart called.");
        try {
            canvas.postInvalidateDelayed(1000);
        } catch (NullPointerException e) {
            Log.d(TAG, "Ignoring NullPointerException during onRestart");

        }
    }

    @Override
    public void onPanelClosed(int featureId, @NonNull Menu menu) {
        super.onPanelClosed(featureId, menu);
        showActionBar();
        enableImmersive();
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu != null) {
            android.util.Log.i(TAG, "Menu opened, disabling hiding action bar");
            handler.removeCallbacks(actionBarHider);
            updateScalingMenu();
            updateInputMenu();
            disableImmersive();
        }
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Make sure extra keys stow item is gone if extra keys are disabled and vice versa.
        setKeyStowDrawableAndVisibility(menu.findItem(R.id.extraKeysToggle));
        menu.findItem(R.id.itemColorMode).setVisible(remoteConnection.canUpdateColorModelConnected());
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "OnCreateOptionsMenu called");
        try {
            getMenuInflater().inflate(R.menu.canvasactivitymenu, menu);

            Menu inputMenu = menu.findItem(R.id.itemInputMode).getSubMenu();
            inputModeMenuItems = new MenuItem[inputModeIds.length];
            for (int i = 0; i < inputModeIds.length; i++) {
                if (inputMenu != null) {
                    inputModeMenuItems[i] = inputMenu.findItem(inputModeIds[i]);
                }
            }
            updateInputMenu();

            Menu scalingMenu = menu.findItem(R.id.itemScaling).getSubMenu();
            scalingModeMenuItems = new MenuItem[scalingModeIds.length];
            for (int i = 0; i < scalingModeIds.length; i++) {
                if (scalingMenu != null) {
                    scalingModeMenuItems[i] = scalingMenu.findItem(scalingModeIds[i]);
                }
            }
            updateScalingMenu();

            // Set the text of the Extra Keys menu item appropriately.
            // TODO: Implement for Opaque
            if (connection != null && connection.getExtraKeysToggleType() == Constants.EXTRA_KEYS_ON)
                menu.findItem(R.id.itemExtraKeys).setTitle(R.string.extra_keys_disable);
            else
                menu.findItem(R.id.itemExtraKeys).setTitle(R.string.extra_keys_enable);

            OnTouchListener moveListener = new OnTouchViewMover(toolbar, handler, actionBarHider, hideToolbarDelay);
            ImageButton moveButton = new ImageButton(this);

            moveButton.setBackgroundResource(R.drawable.ic_all_out_gray_36dp);
            MenuItem moveToolbar = menu.findItem(R.id.moveToolbar);
            moveToolbar.setActionView(moveButton);
            moveButton.setOnTouchListener(moveListener);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "OnCreateOptionsMenu complete");
        return true;
    }

    /**
     * Change the scaling mode sub-menu to reflect available scaling modes.
     */
    void updateScalingMenu() {
        try {
            for (MenuItem item : scalingModeMenuItems) {
                // If the entire framebuffer is NOT contained in the bitmap, fit-to-screen is meaningless.
                if (item.getItemId() == R.id.itemFitToScreen) {
                    item.setEnabled(canvas == null || canvas.myDrawable == null ||
                            (canvas.myDrawable.getBitmapHeight() == canvas.myDrawable.getFramebufferHeight() &&
                                    canvas.myDrawable.getBitmapWidth() == canvas.myDrawable.getFramebufferWidth()));
                } else {
                    item.setEnabled(true);
                }

                AbstractScaling scaling = AbstractScaling.getById(item.getItemId());
                if (scaling.scaleType == connection.getScaleMode()) {
                    item.setChecked(true);
                }
            }
        } catch (NullPointerException e) {
            Log.d(TAG, "Ignoring NullPointerException during updateScalingMenu");
        }
    }

    /**
     * Change the input mode sub-menu to reflect change in scaling
     */
    void updateInputMenu() {
        try {
            for (MenuItem item : inputModeMenuItems) {
                item.setEnabled(canvas.canvasZoomer.isValidInputMode(item.getItemId()));
                if (getInputHandlerById(item.getItemId()) == touchInputHandler)
                    item.setChecked(true);
            }
        } catch (NullPointerException e) {
            Log.d(TAG, "Ignoring NullPointerException during updateInputMenu");
        }
    }

    /**
     * If id represents an input handler, return that; otherwise return null
     */
    TouchInputHandler getInputHandlerById(int id) {
        myVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        if (inputModeHandlers == null) {
            inputModeHandlers = new TouchInputHandler[inputModeIds.length];
        }
        for (int i = 0; i < inputModeIds.length; ++i) {
            if (inputModeIds[i] == id) {
                if (inputModeHandlers[i] == null) {
                    if (id == R.id.itemInputTouchpad) {
                        inputModeHandlers[i] = new TouchInputHandlerTouchpad(this, canvas, remoteConnection, App.debugLog);
                    } else if (id == R.id.itemInputDragPanZoomMouse) {
                        inputModeHandlers[i] = new TouchInputHandlerDirectDragPan(this, canvas, remoteConnection, App.debugLog);
                    } else if (id == R.id.itemInputTouchPanZoomMouse) {
                        inputModeHandlers[i] = new TouchInputHandlerDirectSwipePan(this, canvas, remoteConnection, App.debugLog);
                    } else if (id == R.id.itemInputSingleHanded) {
                        inputModeHandlers[i] = new TouchInputHandlerSingleHanded(this, canvas, remoteConnection, App.debugLog);
                    } else {
                        throw new IllegalStateException("Unexpected value: " + id);
                    }
                }
                return inputModeHandlers[i];
            }
        }
        return null;
    }

    TouchInputHandler getInputHandlerByName(String name) {
        TouchInputHandler result = null;
        for (int id : inputModeIds) {
            TouchInputHandler handler = getInputHandlerById(id);
            if (handler.getId().equals(name)) {
                result = handler;
                break;
            }
        }
        if (result == null) {
            result = getInputHandlerById(R.id.itemInputTouchpad);
        }
        return result;
    }

    int getModeIdFromHandler(TouchInputHandler handler) {
        for (int id : inputModeIds) {
            if (handler == getInputHandlerById(id))
                return id;
        }
        return R.id.itemInputTouchpad;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        RemoteKeyboard k = remoteConnection.getKeyboard();
        if (k != null) {
            k.setAfterMenu(true);
        }
        int itemId = item.getItemId();
        if (itemId == R.id.itemInfo) {
            remoteConnection.showConnectionInfo();
            return true;
        } else if (itemId == R.id.itemSpecialKeys) {
            showDialog(R.layout.metakey);
            return true;
        } else if (itemId == R.id.itemColorMode) {
            selectColorModel();
            return true;
            // Following sets one of the scaling options
        } else if (itemId == R.id.itemZoomable || itemId == R.id.itemOneToOne || itemId == R.id.itemFitToScreen) {
            AbstractScaling.getById(item.getItemId()).setScaleTypeForActivity(this);
            item.setChecked(true);
            showPanningState(false);
            return true;
        } else if (itemId == R.id.itemCenterMouse) {
            remoteConnection.getPointer().movePointer(canvas.absoluteXPosition + canvas.getVisibleDesktopWidth() / 2,
                    canvas.absoluteYPosition + canvas.getVisibleDesktopHeight() / 2);
            return true;
        } else if (itemId == R.id.itemDisconnect) {
            remoteConnection.closeConnection();
            Utils.justFinish(this);
            return true;
        } else if (itemId == R.id.itemEnterText) {
            showDialog(R.layout.entertext);
            return true;
        } else if (itemId == R.id.itemCtrlAltDel) {
            remoteConnection.getKeyboard().sendMetaKey(MetaKeyBean.keyCtrlAltDel);
            return true;
        } else if (itemId == R.id.itemSendKeyAgain) {
            sendSpecialKeyAgain();
            return true;
        } else if (itemId == R.id.itemExtraKeys) {
            if (connection.getExtraKeysToggleType() == Constants.EXTRA_KEYS_ON) {
                connection.setExtraKeysToggleType(Constants.EXTRA_KEYS_OFF);
                item.setTitle(R.string.extra_keys_enable);
                setExtraKeysVisibility(View.GONE, false);
            } else {
                connection.setExtraKeysToggleType(Constants.EXTRA_KEYS_ON);
                item.setTitle(R.string.extra_keys_disable);
                setExtraKeysVisibility(View.VISIBLE, false);
                extraKeysHidden = false;
            }
            invalidateOptionsMenu();
            connection.save(this);
            return true;
        } else if (itemId == R.id.itemHelpInputMode) {
            showDialog(R.id.itemHelpInputMode);
            return true;
        } else {
            boolean inputModeSet = setInputMode(item.getItemId());
            item.setChecked(inputModeSet);
            if (inputModeSet) {
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean setInputMode(int id) {
        TouchInputHandler input = getInputHandlerById(id);
        if (input != null) {
            setInputHandler(input);
            connection.setInputMode(input.getId());
            if (input.getId().equals(TouchInputHandlerTouchpad.ID)) {
                connection.setFollowMouse(true);
                connection.setFollowPan(true);
            } else {
                connection.setFollowMouse(false);
                connection.setFollowPan(false);
                remoteConnection.getPointer().setRelativeEvents(false);
            }

            showPanningState(true);
            connection.save(this);
            return true;
        }
        return false;
    }

    private void setInputHandler(TouchInputHandler input) {
        touchInputHandler = input;
        inputListener = new RemoteClientsInputListener(
                this,
                remoteConnection,
                touchInputHandler,
                this::resetOnScreenKeys,
                connection.getUseDpadAsArrows()
        );
        canvas.setOnKeyListener(inputListener);
    }

    private void sendSpecialKeyAgain() {
        if (lastSentKey == null
                || lastSentKey.get_Id() != connection.getLastMetaKeyId()) {
            ArrayList<MetaKeyBean> keys = new ArrayList<>();
            Database database = new Database(this);
            Cursor c = database.getReadableDatabase().rawQuery(
                    MessageFormat.format("SELECT * FROM {0} WHERE {1} = {2}",
                            MetaKeyBean.GEN_TABLE_NAME,
                            MetaKeyBean.GEN_FIELD__ID, connection
                                    .getLastMetaKeyId()),
                    MetaKeyDialog.EMPTY_ARGS);
            MetaKeyBean.Gen_populateFromCursor(c, keys, MetaKeyBean.NEW);
            c.close();
            database.close();
            if (keys.size() > 0) {
                lastSentKey = keys.get(0);
            } else {
                lastSentKey = null;
            }
        }
        if (lastSentKey != null)
            remoteConnection.getKeyboard().sendMetaKey(lastSentKey);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy called.");
        if (canvas != null)
            remoteConnection.closeConnection();
        System.gc();
    }

    public void showPanningState(boolean showLonger) {
        if (showLonger) {
            final Toast t = Toast.makeText(this, touchInputHandler.getDescription(), Toast.LENGTH_LONG);
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    t.show();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Ignored InterruptedException during showPanningState");
                    }
                    t.show();
                }
            };
            new Timer().schedule(tt, 2000);
            t.show();
        } else {
            Toast t = Toast.makeText(this, touchInputHandler.getDescription(), Toast.LENGTH_SHORT);
            t.show();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onTrackballEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        boolean consumed = false;
        if (inputListener != null) {
            consumed = inputListener.onTrackballEvent(event);
        }
        if (!consumed) {
            consumed = super.onTrackballEvent(event);
        }
        return consumed;
    }

    // Send touch events or mouse events like button clicks to be handled.
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean consumed = false;
        if (inputListener != null) {
            consumed = inputListener.onTouchEvent(event);
        }
        if (!consumed) {
            consumed = super.onTrackballEvent(event);
        }
        return consumed;
    }

    // Send e.g. mouse events like hover and scroll to be handled.
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        boolean consumed = false;
        if (inputListener != null) {
            consumed = inputListener.onGenericMotionEvent(event);
        }
        if (!consumed) {
            consumed = super.onGenericMotionEvent(event);
        }
        return consumed;
    }

    private void selectColorModel() {

        String[] choices = new String[COLORMODEL.values().length];
        int currentSelection = -1;
        for (int i = 0; i < choices.length; i++) {
            COLORMODEL cm = COLORMODEL.values()[i];
            choices[i] = cm.toString();
            if (remoteConnection.isColorModel(cm))
                currentSelection = i;
        }

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ListView list = new ListView(this);
        list.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_checked, choices));
        list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        list.setItemChecked(currentSelection, true);
        list.setOnItemClickListener((arg0, arg1, arg2, arg3) -> {
            dialog.dismiss();
            COLORMODEL cm = COLORMODEL.values()[arg2];
            remoteConnection.setColorModel(cm);
            connection.setColorModel(cm.nameString());
            connection.save(RemoteCanvasActivity.this);
            Toast.makeText(RemoteCanvasActivity.this, getString(R.string.info_update_color_model_to) + cm, Toast.LENGTH_SHORT).show();
        });
        dialog.setContentView(list);
        dialog.show();
    }

    public void showActionBar() {
        handler.removeCallbacks(actionBarShower);
        handler.postAtTime(actionBarShower, SystemClock.uptimeMillis() + 50);
        handler.removeCallbacks(actionBarHider);
        handler.postAtTime(actionBarHider, SystemClock.uptimeMillis() + hideToolbarDelay);
    }

    @Override
    public void onTextSelected(String selectedString) {
        android.util.Log.i(TAG, "onTextSelected called with selectedString: " + selectedString);
        remoteConnection.pd.show();
        connection.setVmname(remoteConnection.vmNameToId.get(selectedString));
        connection.save(this);
        synchronized (remoteConnection.getRfbConn()) {
            remoteConnection.getRfbConn().notify();
        }
    }

    public void toggleKeyboard(MenuItem menuItem) {
        if (softKeyboardUp) {
            hideKeyboard();
        } else {
            showKeyboard();
        }
    }

    public void showKeyboard() {
        android.util.Log.i(TAG, "Showing keyboard and hiding action bar");
        canvas.requestFocus();
        Utils.showKeyboard(this, canvas);
        softKeyboardUp = true;
        Objects.requireNonNull(getSupportActionBar()).hide();
    }

    public void hideKeyboard() {
        android.util.Log.i(TAG, "Hiding keyboard and hiding action bar");
        canvas.requestFocus();
        Utils.hideKeyboard(this, getCurrentFocus());
        softKeyboardUp = false;
        Objects.requireNonNull(getSupportActionBar()).hide();
    }

    public void hideKeyboardAndExtraKeys() {
        hideKeyboard();
        if (layoutKeys.getVisibility() == View.VISIBLE) {
            extraKeysHidden = true;
            setExtraKeysVisibility(View.GONE, false);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public RemoteCanvas getCanvas() {
        return canvas;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
        super.onSaveInstanceState(outState);
    }

    private boolean isMasterPasswordEnabled() {
        SharedPreferences sp = getSharedPreferences(Constants.generalSettingsTag, Context.MODE_PRIVATE);
        return sp.getBoolean(Constants.masterPasswordEnabledTag, false);
    }

    @Override
    public void onBackPressed() {
        if (inputListener != null) {
            inputListener.onKey(canvas, KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
        }
    }

    public RemoteConnection getRemoteConnection() {
        return remoteConnection;
    }

    private class ActionBarHider implements Runnable {
        public void run() {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                Log.d(TAG, "ActionBarHider: Hiding ActionBar");
                actionBar.hide();
            }
        }
    }

    private class ActionBarShower implements Runnable {
        public void run() {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                Log.d(TAG, "ActionBarShower: Showing ActionBar");
                actionBar.show();
            }
        }
    }


}
