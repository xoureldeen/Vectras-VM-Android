package au.com.darkside.xserver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.EditorInfo;
import android.text.InputType;
import android.os.Build;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.app.Instrumentation;
import android.os.Looper;
import android.os.Handler;
import android.view.inputmethod.InputMethodManager;
import android.app.Service;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;
import java.nio.charset.StandardCharsets;
import java.lang.Math; 

/**
 * This class implements an X Windows screen.
 * <p>
 * It also implements the screen's root window.
 * </p>
 *
 * @author Matthew Kwan
 */
public class ScreenView extends View {

    private interface PendingEvent {
        public void run();
    }

    private interface PendingPointerEvent extends PendingEvent {
    }

    private class PendingGrabButtonNotify implements PendingPointerEvent {

        private Window mWindow;
        private boolean mPressed;
        private int mMotionX;
        private int mMotionY;
        private int mButton;
        private int mGrabEventMask;
        private Client mGrabPointerClient;
        private boolean mGrabPointerOwnerEvents;

        public PendingGrabButtonNotify(Window w, boolean pressed, int motionX, int motionY, int button, int grabEventMask, Client grabPointerClient, boolean grabPointerOwnerEvents) {
            mWindow = w;
            mPressed = pressed;
            mMotionX = motionX;
            mMotionY = motionY;
            mButton = button;
            mGrabEventMask = grabEventMask;
            mGrabPointerClient = grabPointerClient;
            mGrabPointerOwnerEvents = grabPointerOwnerEvents;
        }

        public void run() {
            callGrabButtonNotify(mWindow, mPressed, mMotionX, mMotionY, mButton, mGrabEventMask, mGrabPointerClient, mGrabPointerOwnerEvents);
        }
    }

    private class PendingGrabMotionNotify implements PendingPointerEvent {

        private Window mWindow;
        private int mX;
        private int mY;
        private int mButtons;
        private int mGrabEventMask;
        private Client mGrabPointerClient;
        private boolean mGrabPointerOwnerEvents;

        public PendingGrabMotionNotify(Window w, int x, int y, int buttons, int grabEventMask, Client grabPointerClient, boolean grabPointerOwnerEvents) {
            mWindow = w;
            mX = x;
            mY = y;
            mButtons = buttons;
            mGrabEventMask = grabEventMask;
            mGrabPointerClient = grabPointerClient;
            mGrabPointerOwnerEvents = grabPointerOwnerEvents;
        }

        public void run() {
            callGrabMotionNotify(mWindow, mX, mY, mButtons, mGrabEventMask, mGrabPointerClient, mGrabPointerOwnerEvents);
        }
    }

    private interface PendingKeyboardEvent extends PendingEvent {
    }

    private class PendingGrabKeyNotify implements PendingKeyboardEvent {

        private Window mWindow;
        private boolean mPressed;
        private int mMotionX;
        private int mMotionY;
        private int mKeycode;
        private Client mGrabKeyboardClient;
        private boolean mGrabKeyboardOwnerEvents;

        public PendingGrabKeyNotify(Window w, boolean pressed, int motionX, int motionY, int keycode, Client grabKeyboardClient, boolean grabKeyboardOwnerEvents) {
            mWindow = w;
            mPressed = pressed;
            mMotionX = motionX;
            mMotionY = motionY;
            mKeycode = keycode;
            mGrabKeyboardClient = grabKeyboardClient;
            mGrabKeyboardOwnerEvents = grabKeyboardOwnerEvents;
        }

        public void run() {
            callGrabKeyNotify(mWindow, mPressed, mMotionX, mMotionY, mKeycode, mGrabKeyboardClient, mGrabKeyboardOwnerEvents);
        }
    }

    private static class PendingEventQueue<T extends PendingEvent> {

        private Queue<T> mQueue = new LinkedList<T>();

        public void add(T event) {
            if (mQueue.offer(event)) {
                return;
            }
        }

        public T next() {
            return mQueue.poll();
        }
    }

    private final XServer _xServer;
    private final int _rootId;
    private Window _rootWindow = null;
    private Window _sharedClipboardWindow = null;
    private Property _sharedClipboardProperty = null;
    private Property _sharedClipboardPrimaryProperty = null;
    private Colormap _defaultColormap = null;
    private final Vector<Colormap> _installedColormaps;
    private final float _pixelsPerMillimeter;

    private Cursor _currentCursor;
    private int _currentCursorX;
    private int _currentCursorY;
    private Cursor _drawnCursor = null;
    private int _drawnCursorX;
    private int _drawnCursorY;
    private Window _motionWindow = null;
    private int _motionX;
    private int _motionY;
    private int _buttons = 0;
    private boolean _isBlanked = false;
	private boolean	_arrowsAsButtons = false;
    private boolean	_inhibitBackButton = false;
    private boolean	_enableTouchClicks = true;
    private boolean _sharedClipboard = true;
    private Paint _paint;

    private Client _grabPointerClient = null;
    private Window _grabPointerWindow = null;
    private int _grabPointerTime = 0;
    private boolean _grabPointerOwnerEvents = false;
    private boolean _grabPointerSynchronous = false;
    private boolean _grabPointerPassive = false;
    private boolean _grabPointerAutomatic = false;
    private boolean _grabPointerFreezeNextEvent = false;
    private Client _grabKeyboardClient = null;
    private Window _grabKeyboardWindow = null;
    private int _grabKeyboardTime = 0;
    private boolean _grabKeyboardOwnerEvents = false;
    private boolean _grabKeyboardSynchronous = false;
    private boolean _grabKeyboardFreezeNextEvent = false;
    private Cursor _grabCursor = null;
    private Window _grabConfineWindow = null;
    private int _grabEventMask = 0;
    private PassiveKeyGrab _grabKeyboardPassiveGrab = null;

    private Window _focusWindow = null;
    private byte _focusRevertTo = 0;    // 0=None, 1=Root, 2=Parent.
    private int _focusLastChangeTime = 0;

    private PendingEventQueue<PendingPointerEvent> mPendingPointerEvents;
    private PendingEventQueue<PendingKeyboardEvent> mPendingKeyboardEvents;

    private boolean _ignoreLongPress = false;

    private static final int ACTION_CANCEL = 0;
    private static final int ACTION_CTRL_C = 1;
    private static final int ACTION_CTRL_V = 2;
    private static final int ACTION_CTRL_X = 3;
    private static final int ACTION_CTRL_A = 4;
    private static final int ACTION_R_CLICK = 5;
    private static final int ACTION_M_CLICK = 6;
    private static final int ACTION_ESC = 7;
    private static final int ACTION_KEYBOARD = 8;

    // -- helpers for movement thresholding, works around phones with cheap touch screens
    private double _totalMove = 0;
    private double _xPrec = 0;
    private double _yPrec = 0;

    /**
     * Constructor.
     *
     * @param c                   The application context.
     * @param xServer             The X server.
     * @param rootId              The ID of the root window, to be created later.
     * @param pixelsPerMillimeter Screen resolution.
     */
    public ScreenView(Context c, XServer xServer, int rootId, float pixelsPerMillimeter) {
        super(c);

        setFocusable(true);
        setFocusableInTouchMode(true);

        _xServer = xServer;
        _rootId = rootId;
        _installedColormaps = new Vector<Colormap>();
        _pixelsPerMillimeter = pixelsPerMillimeter;
        _paint = new Paint();

        mPendingPointerEvents = new PendingEventQueue<PendingPointerEvent>();
        mPendingKeyboardEvents = new PendingEventQueue<PendingKeyboardEvent>();

        // ---- Listeners for touch input ----
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // do nothing
            }
        });

        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                synchronized (_xServer) {
                    if (_rootWindow == null) return false;
        
                    blank(false);    // Reset the screen saver.
                    updatePointerPosition((int) event.getX(), (int) event.getY(), 0);
        
                    if(_enableTouchClicks){
                        if(event.getActionMasked() == MotionEvent.ACTION_DOWN && event.getActionIndex() == 0)
                            updatePointerButtons (1, true);
                        if(event.getActionMasked() == MotionEvent.ACTION_UP && event.getActionIndex() == 0)
                            updatePointerButtons (1, false);
                        if(event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN && event.getActionIndex() == 1)
                            updatePointerButtons (3, true);
                        if((event.getActionMasked() == MotionEvent.ACTION_POINTER_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL)  && event.getActionIndex() == 1)
                            updatePointerButtons (3, false);
                    }
                }

                if(event.getActionMasked() == MotionEvent.ACTION_DOWN){
                    _totalMove = 0;
                    _xPrec = event.getX();
                    _yPrec = event.getY();
                }
                else if (event.getActionMasked() == MotionEvent.ACTION_MOVE){
                    final double dx = event.getX() - _xPrec;
                    final double dy = event.getY() - _yPrec;
                    final double dl = Math.sqrt(dx * dx + dy * dy);
                    _totalMove += dl;
                    _xPrec = event.getX();
                    _yPrec = event.getY();
                }


                if(_totalMove < 20){ // -- workaround for phones with cheap touchscreens (which will constantly trigger ACTION_MOVE events)
                    _ignoreLongPress = false;
                    return false; // make longClick Listeners work!
                }

                _ignoreLongPress = true;
                return true;
            }
        });
        
        setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(_ignoreLongPress)
                    return true;

                ActionMode.Callback cb = new ActionMode.Callback(){
                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        menu.add(0, ACTION_CTRL_C, 0, "CTRL+C");
                        menu.add(0, ACTION_CTRL_V, 0, "CTRL+V");
                        menu.add(0, ACTION_CTRL_X, 0, "CTRL+X");
                        menu.add(0, ACTION_CTRL_A, 0, "CTRL+A");
                        menu.add(0, ACTION_ESC, 0, "ESC");
                        menu.add(0, ACTION_R_CLICK, 0, "M-Click");
                        menu.add(0, ACTION_R_CLICK, 0, "R-Click");
                        menu.add(0, ACTION_KEYBOARD, 0, "Keyboard");
                        menu.add(0, ACTION_CANCEL, 0, "Cancel");
                        return true;
                    }

                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        return false; // Return false if nothing is done
                    }

                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        switch (item.getItemId()) {
                            case ACTION_CTRL_C:
                                onKeyDown(KeyEvent.KEYCODE_CTRL_LEFT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT));
                                onKeyDown(KeyEvent.KEYCODE_C, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_C));
                                onKeyUp(KeyEvent.KEYCODE_C, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_C));
                                onKeyUp(KeyEvent.KEYCODE_CTRL_LEFT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT));
                                mode.finish();
                                return true;
                            case ACTION_CTRL_V:
                                if (_sharedClipboard) {
                                    Selection.setSelectionOwner(_xServer, _xServer.findAtom("CLIPBOARD"), _sharedClipboardWindow); // override owner to point to our clipboardwindow
                                    Selection.setSelectionOwner(_xServer, _xServer.findAtom("PRIMARY"), _sharedClipboardWindow);
                                    ClipboardManager clipboard = (ClipboardManager) _xServer.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData.Item clipitem = clipboard.getPrimaryClip().getItemAt(0);

                                    _sharedClipboardProperty.setData(clipitem.getText().toString());
                                    _sharedClipboardPrimaryProperty.setData(clipitem.getText().toString());
                                }
                            
                                onKeyDown(KeyEvent.KEYCODE_CTRL_LEFT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT));
                                onKeyDown(KeyEvent.KEYCODE_V, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_V));
                                onKeyUp(KeyEvent.KEYCODE_V, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_V));
                                onKeyUp(KeyEvent.KEYCODE_CTRL_LEFT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT));
                                mode.finish();
                                return true;
                            case ACTION_CTRL_X:
                                onKeyDown(KeyEvent.KEYCODE_CTRL_LEFT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT));
                                onKeyDown(KeyEvent.KEYCODE_X, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_X));
                                onKeyUp(KeyEvent.KEYCODE_X, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_X));
                                onKeyUp(KeyEvent.KEYCODE_CTRL_LEFT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT));
                                mode.finish();
                                return true;
                            case ACTION_CTRL_A:
                                onKeyDown(KeyEvent.KEYCODE_CTRL_LEFT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT));
                                onKeyDown(KeyEvent.KEYCODE_A, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A));
                                onKeyUp(KeyEvent.KEYCODE_A, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_A));
                                onKeyUp(KeyEvent.KEYCODE_CTRL_LEFT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT));
                                mode.finish();
                                return true;
                            case ACTION_R_CLICK:
                                if (_sharedClipboard) {
                                    Selection.setSelectionOwner(_xServer, _xServer.findAtom("CLIPBOARD"), _sharedClipboardWindow); // override owner to point to our clipboardwindow
                                    Selection.setSelectionOwner(_xServer, _xServer.findAtom("PRIMARY"), _sharedClipboardWindow);
                                    ClipboardManager clipboard = (ClipboardManager) _xServer.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData.Item clipitem = clipboard.getPrimaryClip().getItemAt(0);

                                    _sharedClipboardProperty.setData(clipitem.getText().toString());
                                    _sharedClipboardPrimaryProperty.setData(clipitem.getText().toString());
                                }
                                updatePointerButtons(3, true);
                                updatePointerButtons(3, false);
                                mode.finish();
                                return true;
                            case ACTION_M_CLICK:
                                if (_sharedClipboard) {
                                    Selection.setSelectionOwner(_xServer, _xServer.findAtom("CLIPBOARD"), _sharedClipboardWindow); // override owner to point to our clipboardwindow
                                    Selection.setSelectionOwner(_xServer, _xServer.findAtom("PRIMARY"), _sharedClipboardWindow);
                                    ClipboardManager clipboard = (ClipboardManager) _xServer.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData.Item clipitem = clipboard.getPrimaryClip().getItemAt(0);

                                    _sharedClipboardProperty.setData(clipitem.getText().toString());
                                    _sharedClipboardPrimaryProperty.setData(clipitem.getText().toString());
                                }
                                updatePointerButtons(2, true);
                                updatePointerButtons(2, false);
                                mode.finish();
                                return true;
                            case ACTION_ESC:
                                onKeyDown(KeyEvent.KEYCODE_ESCAPE, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE));
                                onKeyUp(KeyEvent.KEYCODE_ESCAPE, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ESCAPE));
                                mode.finish();
                                return true;
                            case ACTION_KEYBOARD:
                                InputMethodManager imm = (InputMethodManager) _xServer.getContext().getSystemService(Service.INPUT_METHOD_SERVICE);
                                requestFocus();
                                imm.hideSoftInputFromWindow(getWindowToken(), 0);
                                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                                mode.finish();
                                return true;
                            case ACTION_CANCEL:
                                mode.finish();
                                return true;
                            default:
                                mode.finish();
                                return false;
                        }
                    }

                    // Called when the user exits the action mode
                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                        mode = null;
                    } 
                };

                // use floating mode on newer android versions
                if(Build.VERSION.SDK_INT >= 23){
                    startActionMode(cb, ActionMode.TYPE_FLOATING);
                }
                else{
                    startActionMode(cb);
                }

                return false;
            }
        });
        requestFocus();
    }

    /**
     * needed make softkeyboard work in landscape mode and to capture backspace.
     */
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs){
        outAttrs.inputType = InputType.TYPE_TEXT_VARIATION_NORMAL;
        outAttrs.imeOptions = EditorInfo.IME_ACTION_NONE | EditorInfo.IME_FLAG_NO_FULLSCREEN;
        return null;
    }
    

    /**
     * Placeholder constructor to prevent a compiler warning.
     *
     * @param c
     */
    public ScreenView(Context c) {
        super(c);

        _xServer = null;
        _rootId = 0;
        _installedColormaps = null;
        _pixelsPerMillimeter = 0;
    }

    /**
     * Return the screen's root window.
     *
     * @return The screen's root window.
     */
    public Window getRootWindow() {
        return _rootWindow;
    }

    /**
     * Return the screen's default colormap.
     *
     * @return The screen's default colormap.
     */
    public Colormap getDefaultColormap() {
        return _defaultColormap;
    }

    /**
     * Return the current cursor.
     *
     * @return The current cursor.
     */
    public Cursor getCurrentCursor() {
        return _currentCursor;
    }

    /**
     * Return the current pointer X coordinate.
     *
     * @return The current pointer X coordinate.
     */
    public int getPointerX() {
        return _currentCursorX;
    }

    /**
     * Return the current pointer Y coordinate.
     *
     * @return The current pointer Y coordinate.
     */
    public int getPointerY() {
        return _currentCursorY;
    }

    /**
     * Return a mask indicating the current state of the pointer and
     * modifier buttons.
     *
     * @return A mask indicating the current state of the buttons.
     */
    public int getButtons() {
        return _buttons;
    }

    /**
     * Return the window that has input focus. Can be null.
     *
     * @return The window that has input focus.
     */
    public Window getFocusWindow() {
        return _focusWindow;
    }

    /**
     * Blank/unblank the screen.
     *
     * @param flag If true, blank the screen. Otherwise unblank it.
     */
    public void blank(boolean flag) {
        if (_isBlanked == flag) return;

        _isBlanked = flag;
        postInvalidate();

        if (!_isBlanked) _xServer.resetScreenSaver();
    }

    /**
     * Add an installed colormap.
     *
     * @param cmap The colormap to add.
     */
    public void addInstalledColormap(Colormap cmap) {
        _installedColormaps.add(cmap);
        if (_defaultColormap == null) _defaultColormap = cmap;
    }

    /**
     * Remove an installed colormap.
     *
     * @param cmap The colormap to remove.
     */
    public void removeInstalledColormap(Colormap cmap) {
        _installedColormaps.remove(cmap);
        if (_defaultColormap == cmap) {
            if (_installedColormaps.size() == 0) _defaultColormap = null;
            else _defaultColormap = _installedColormaps.firstElement();
        }
    }

    /**
     * Remove all colormaps except the default one.
     */
    public void removeNonDefaultColormaps() {
        if (_installedColormaps.size() < 2) return;

        _installedColormaps.clear();
        if (_defaultColormap != null) _installedColormaps.add(_defaultColormap);
    }

    /**
     * Called when a window is deleted, usually due to a client disconnecting.
     * Removes all references to the window.
     *
     * @param w The window being deleted.
     */
    public void deleteWindow(Window w) {
        if (_grabPointerWindow == w || _grabConfineWindow == w) {
            _grabPointerClient = null;
            _grabPointerWindow = null;
            _grabCursor = null;
            _grabConfineWindow = null;
            updatePointer(2);
        } else {
            updatePointer(0);
        }

        revertFocus(w);
    }

    /**
     * Called when the window is unmapped.
     * If the window had keyboard focus, update the focus window.
     *
     * @param w
     */
    public void revertFocus(Window w) {
        if (w == _grabKeyboardWindow) {
            Window pw = _rootWindow.windowAtPoint(_motionX, _motionY);

            Window.focusInOutNotify(_grabKeyboardWindow, _focusWindow, pw, _rootWindow, 2);
            _grabKeyboardClient = null;
            _grabKeyboardWindow = null;
        }

        if (w == _focusWindow) {
            Window pw = _rootWindow.windowAtPoint(_motionX, _motionY);

            if (_focusRevertTo == 0) {
                _focusWindow = null;
            } else if (_focusRevertTo == 1) {
                _focusWindow = _rootWindow;
            } else {
                _focusWindow = w.getParent();
                while (!_focusWindow.isViewable()) _focusWindow = _focusWindow.getParent();
            }

            _focusRevertTo = 0;
            Window.focusInOutNotify(w, _focusWindow, pw, _rootWindow, _grabKeyboardWindow == null ? 0 : 3);
        }
    }

    /**
     * Called when the view needs drawing.
     *
     * @param canvas The canvas on which the view will be drawn.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (_rootWindow == null) {
            super.onDraw(canvas);
            return;
        }

        synchronized (_xServer) {
            if (_isBlanked) {
                canvas.drawColor(0xff000000);
                return;
            }

            _paint.reset();
            _rootWindow.draw(canvas, _paint);
            canvas.drawBitmap(_currentCursor.getBitmap(), _currentCursorX - _currentCursor.getHotspotX(), _currentCursorY - _currentCursor.getHotspotY(), null);

            _drawnCursor = _currentCursor;
            _drawnCursorX = _currentCursorX;
            _drawnCursorY = _currentCursorY;
        }
    }

    /**
     * Called when the size changes.
     * Create the root window.
     *
     * @param width     The new width.
     * @param height    The new height.
     * @param oldWidth  The old width.
     * @param oldHeight The old height.
     */
    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        _rootWindow = new Window(_rootId, _xServer, null, this, null, 0, 0, width, height, 0, false, true);
        _sharedClipboardWindow = new Window(_xServer.nextFreeResourceId()+1, _xServer, null, this, _rootWindow, -1, -1, 1, 1, 0, true, false); // hidden window managing android <-> xServer clipboard
        _sharedClipboardWindow.setIsServerWindow(true); // flag as functional server only window (there is a urgent need to introduce interfaces..)
        _sharedClipboardProperty = new Property(_xServer.findAtom("CLIPBOARD").getId(), _xServer.findAtom("CLIPBOARD").getId(), (byte)32); // property which will hold the clipboard data
        _sharedClipboardPrimaryProperty = new Property(_xServer.findAtom("PRIMARY").getId(), _xServer.findAtom("PRIMARY").getId(), (byte)32); // property which will hold the clipboard data

        Property.OnPropertyChangedListener cb = new Property.OnPropertyChangedListener(){ // -- executed on a per client thread basis
            @Override
            public void onPropertyChanged(byte[] data, Atom type){
                switch(type.getName()){
                    case "UTF8_STRING":
                        String s = new String(data, StandardCharsets.UTF_8); // convert to UTF8 string

                        // create task for UI thread
                        class OneShotTask implements Runnable {
                            private String d;
                            OneShotTask(String s) { d = s; }
                            public void run() {
                                ClipboardManager clipboard = (ClipboardManager) _xServer.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("cb", d);
                                clipboard.setPrimaryClip(clip); // store to clipboard
                            }
                        }
                        new Handler(Looper.getMainLooper()).post(new OneShotTask(s));
                        break;
                    default: // different types can be implemented here (binary etc.)
                        break;
                }
            }
        };


        // capture data changes on this property
        _sharedClipboardProperty.setOnPropertyChangedListener(cb);
        _sharedClipboardPrimaryProperty.setOnPropertyChangedListener(cb);

        _sharedClipboardWindow.addProperty(_sharedClipboardPrimaryProperty);
        _sharedClipboardWindow.addProperty(_sharedClipboardProperty);

        _xServer.addResource(_rootWindow);
        _xServer.addResource(_sharedClipboardWindow);

        _currentCursor = _rootWindow.getCursor();
        _currentCursorX = width / 2;
        _currentCursorY = height / 2;
        _drawnCursorX = _currentCursorX;
        _drawnCursorY = _currentCursorY;
        _motionX = _currentCursorX;
        _motionY = _currentCursorY;
        _motionWindow = _rootWindow;
        _focusWindow = _rootWindow;

        // Everything set up, so start listening for clients.
        _xServer.start();
    }

    /**
     * Move the pointer on the screen.
     *
     * @param x      New X coordinate.
     * @param y      New Y coordinate.
     * @param cursor The cursor to draw.
     */
    private void movePointer(int x, int y, Cursor cursor) {
        if (_drawnCursor != null) {
            int left = _drawnCursorX - _drawnCursor.getHotspotX();
            int top = _drawnCursorY - _drawnCursor.getHotspotY();
            Bitmap bm = _drawnCursor.getBitmap();

            postInvalidate(left, top, left + bm.getWidth(), top + bm.getHeight());
            _drawnCursor = null;
        }

        _currentCursor = cursor;
        _currentCursorX = x;
        _currentCursorY = y;

        int left = x - cursor.getHotspotX();
        int top = y - cursor.getHotspotY();
        Bitmap bm = cursor.getBitmap();

        postInvalidate(left, top, left + bm.getWidth(), top + bm.getHeight());
    }

    /**
     * Update the location of the pointer.
     *
     * @param x    New X coordinate.
     * @param y    New Y coordinate.
     * @param mode 0=Normal, 1=Grab, 2=Ungrab
     */
    public void updatePointerPosition(int x, int y, int mode) {
        Window w;
        Cursor c;

        if (_grabConfineWindow != null) {
            Rect rect = _grabConfineWindow.getIRect();

            if (x < rect.left) x = rect.left;
            else if (x >= rect.right) x = rect.right - 1;

            if (y < rect.top) y = rect.top;
            else if (y >= rect.bottom) y = rect.bottom - 1;
        }

        if (_grabPointerWindow != null) w = _grabPointerWindow;
        else w = _rootWindow.windowAtPoint(x, y);

        if (_grabCursor != null) c = _grabCursor;
        else c = w.getCursor();

        if (c != _currentCursor || x != _currentCursorX || y != _currentCursorY)
            movePointer(x, y, c);

        if (w != _motionWindow) {
            _motionWindow.leaveEnterNotify(x, y, w, mode);
            _motionWindow = w;
            _motionX = x;
            _motionY = y;
        } else if (x != _motionX || y != _motionY) {
            if (_grabPointerWindow == null) {
                w.motionNotify(x, y, _buttons & 0xff00, null);
            } else if (!_grabPointerSynchronous) {
                callGrabMotionNotify(w, x, y, _buttons, _grabEventMask, _grabPointerClient, _grabPointerOwnerEvents);
            } else {
                PendingPointerEvent e;
                e = new PendingGrabMotionNotify(w, x, y, _buttons, _grabEventMask, _grabPointerClient, _grabPointerOwnerEvents);
                mPendingPointerEvents.add(e);
            }

            _motionX = x;
            _motionY = y;
        }
    }

    /**
     * Update the pointer in case its glyph has changed.
     *
     * @param mode 0=Normal, 1=Grab, 2=Ungrab
     */
    public void updatePointer(int mode) {
        updatePointerPosition(_currentCursorX, _currentCursorY, mode);
    }

    /**
     * Called when a pointer button is pressed/released.
     *
     * @param button  The button that was pressed/released.
     * @param pressed True if the button was pressed.
     */
    public void updatePointerButtons(int button, boolean pressed) {
        Pointer p = _xServer.getPointer();

        button = p.mapButton(button);
        if (button == 0) return;

        int mask = 0x80 << button;

        if (pressed) {
            if ((_buttons & mask) != 0) return;

            _buttons |= mask;
        } else {
            if ((_buttons & mask) == 0) return;

            _buttons &= ~mask;
        }

        if (_grabPointerWindow == null) {
            Window w = _rootWindow.windowAtPoint(_motionX, _motionY);
            PassiveButtonGrab pbg = null;

            if (pressed) pbg = w.findPassiveButtonGrab(_buttons, null);

            if (pbg != null) {
                _grabPointerClient = pbg.getGrabClient();
                _grabPointerWindow = pbg.getGrabWindow();
                _grabPointerPassive = true;
                _grabPointerAutomatic = false;
                _grabPointerTime = _xServer.getTimestamp();
                _grabConfineWindow = pbg.getConfineWindow();
                _grabEventMask = pbg.getEventMask();
                _grabPointerOwnerEvents = pbg.getOwnerEvents();
                _grabPointerSynchronous = pbg.getPointerSynchronous();
                _grabKeyboardSynchronous = pbg.getKeyboardSynchronous();

                _grabCursor = pbg.getCursor();
                if (_grabCursor == null) _grabCursor = _grabPointerWindow.getCursor();

                updatePointer(1);
            } else {
                int timestamp = _xServer.getTimestamp();
                Window ew = w.buttonNotify(pressed, _motionX, _motionY, button, timestamp, null);
                reflectPointerFreezeNextEvent();
                Client c = null;

                if (pressed && ew != null) {
                    Vector<Client> sc;

                    sc = ew.getSelectingClients(EventCode.MaskButtonPress);
                    if (sc != null) c = sc.firstElement();
                }

                // Start an automatic key grab.
                if (c != null) {
                    int em = ew.getClientEventMask(c);

                    _grabPointerClient = c;
                    _grabPointerWindow = ew;
                    _grabPointerPassive = false;
                    _grabPointerAutomatic = true;
                    _grabPointerTime = timestamp;
                    _grabCursor = ew.getCursor();
                    _grabConfineWindow = null;
                    _grabEventMask = em & EventCode.MaskAllPointer;
                    _grabPointerOwnerEvents = (em & EventCode.MaskOwnerGrabButton) != 0;
                    _grabPointerSynchronous = false;
                    _grabKeyboardSynchronous = false;
                    updatePointer(1);
                }
            }
        } else {
            if (!_grabPointerSynchronous) {
                callGrabButtonNotify(_grabPointerWindow, pressed, _motionX, _motionY, button, _grabEventMask, _grabPointerClient, _grabPointerOwnerEvents);
            } else {
                PendingPointerEvent e;
                e = new PendingGrabButtonNotify(_grabPointerWindow, pressed, _motionX, _motionY, button, _grabEventMask, _grabPointerClient, _grabPointerOwnerEvents);
                mPendingPointerEvents.add(e);
            }

            if (_grabPointerAutomatic && !pressed && (_buttons & 0xff00) == 0) {
                _grabPointerClient = null;
                _grabPointerWindow = null;
                _grabCursor = null;
                _grabConfineWindow = null;
                updatePointer(2);
            }
        }
    }

    /**
     * Updates keycodes for modifier keys (i.e. shift/alt).
     */
    private void updateModifiers() {
        int mask = 0;

		Keyboard kb = _xServer.getKeyboard();
		mask = kb.getState();

        _buttons = (_buttons & 0xff00) | mask;
    }

    /**
     * Called when a key is pressed or released.
     *
     * @param keycode Keycode of the key.
     * @param pressed True if pressed, false if released.
     */
    public void notifyKeyPressedReleased(int keycode, boolean pressed) {
        if (_grabKeyboardWindow == null && _focusWindow == null) return;

        Keyboard kb = _xServer.getKeyboard();

        keycode = kb.translateToXKeycode(keycode);

        if (pressed && _grabKeyboardWindow == null) {
            PassiveKeyGrab pkg = _focusWindow.findPassiveKeyGrab(keycode, _buttons & 0xff, null);

            if (pkg == null) {
                Window w = _rootWindow.windowAtPoint(_motionX, _motionY);

                if (w.isAncestor(_focusWindow))
                    pkg = w.findPassiveKeyGrab(keycode, _buttons & 0xff, null);
            }

            if (pkg != null) {
                _grabKeyboardPassiveGrab = pkg;
                _grabKeyboardClient = pkg.getGrabClient();
                _grabKeyboardWindow = pkg.getGrabWindow();
                _grabKeyboardTime = _xServer.getTimestamp();
                _grabKeyboardOwnerEvents = pkg.getOwnerEvents();
                _grabPointerSynchronous = pkg.getPointerSynchronous();
                _grabKeyboardSynchronous = pkg.getKeyboardSynchronous();
            }
        }

        if (_grabKeyboardWindow == null) {
            Window w = _rootWindow.windowAtPoint(_motionX, _motionY);

            if (w.isAncestor(_focusWindow)) w.keyNotify(pressed, _motionX, _motionY, keycode, null);
            else _focusWindow.keyNotify(pressed, _motionX, _motionY, keycode, null);
            reflectKeyboardFreezeNextEvent();
        } else if (!_grabKeyboardSynchronous) {
            callGrabKeyNotify(_grabKeyboardWindow, pressed, _motionX, _motionY, keycode, _grabKeyboardClient, _grabKeyboardOwnerEvents);
        } else {
            PendingKeyboardEvent e;
            e = new PendingGrabKeyNotify(_grabKeyboardWindow, pressed, _motionX, _motionY, keycode, _grabKeyboardClient, _grabKeyboardOwnerEvents);
            mPendingKeyboardEvents.add(e);
        }

        kb.updateKeymap(keycode, pressed);

        if (!pressed && _grabKeyboardPassiveGrab != null) {
            int rk = _grabKeyboardPassiveGrab.getKey();

            if (rk == 0 || rk == keycode) {
                _grabKeyboardPassiveGrab = null;
                _grabKeyboardClient = null;
                _grabKeyboardWindow = null;
            }
        }
    }

    /**
     * Called when there is a key down event.
     *
     * @param keycode The value in event.getKeyCode().
     * @param event   The key event.
     * @return True if the event was handled.
     */
    @Override
    public boolean onKeyDown(int keycode, KeyEvent event) {
        synchronized (_xServer) {
            if (_rootWindow == null) return false;

            blank(false);    // Reset the screen saver.

            boolean sendEvent = false;

			if (_arrowsAsButtons) {
				switch (keycode) {
				case KeyEvent.KEYCODE_DPAD_LEFT:
				case KeyEvent.KEYCODE_DPAD_CENTER:
					updatePointerButtons (1, true);
					return true;
				case KeyEvent.KEYCODE_DPAD_UP:
				case KeyEvent.KEYCODE_DPAD_DOWN:
					updatePointerButtons (2, true);
					return true;
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					updatePointerButtons (3, true);
					return true;
				}
			}

			switch (keycode) {
				case KeyEvent.KEYCODE_BACK:
					if (! _inhibitBackButton)
						return false;
					keycode = 128 - _xServer.getKeyboard().getMinimumKeycodeDiff(); // Special keycode since keycode value 5 is out of range
					sendEvent = true;
					break;
				case KeyEvent.KEYCODE_MENU:
					return false;
				case KeyEvent.KEYCODE_VOLUME_UP:
					updatePointerButtons (1, true);
					break;
				case KeyEvent.KEYCODE_VOLUME_DOWN:
					updatePointerButtons (3, true);
					break;
				default:
					sendEvent = true;
					break;
			}
	
			updateModifiers();
            if (sendEvent) notifyKeyPressedReleased(keycode, true);
        }

        return true;
    }

    /**
     * Called when there is a key up event.
     *
     * @param keycode The value in event.getKeyCode().
     * @param event   The key event.
     * @return True if the event was handled.
     */
    @Override
    public boolean onKeyUp(int keycode, KeyEvent event) {
        synchronized (_xServer) {
            if (_rootWindow == null) return false;

            blank(false);    // Reset the screen saver.

            boolean sendEvent = false;

			if (_arrowsAsButtons) {
				switch (keycode) {
				case KeyEvent.KEYCODE_DPAD_LEFT:
				case KeyEvent.KEYCODE_DPAD_CENTER:
					updatePointerButtons (1, false);
					return true;
				case KeyEvent.KEYCODE_DPAD_UP:
				case KeyEvent.KEYCODE_DPAD_DOWN:
					updatePointerButtons (2, false);
					return true;
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					updatePointerButtons (3, false);
					return true;
				}
			}

			switch (keycode) {
				case KeyEvent.KEYCODE_BACK:
					if (! _inhibitBackButton)
						return false;
					keycode = 128 - _xServer.getKeyboard().getMinimumKeycodeDiff(); // Special keycode since keycode value 5 is out of range
					sendEvent = true;
					break;
				case KeyEvent.KEYCODE_MENU:
					return false;
				case KeyEvent.KEYCODE_VOLUME_UP:
					updatePointerButtons (1, false);
					break;
				case KeyEvent.KEYCODE_VOLUME_DOWN:
					updatePointerButtons (3, false);
					break;
				default:
					sendEvent = true;
					break;
			}
	
			updateModifiers();
            if (sendEvent) notifyKeyPressedReleased(keycode, false);
        }

        return true;
    }

    /**
     * Write details of the screen.
     *
     * @param io The input/output stream.
     * @throws IOException
     */
    public void write(InputOutput io) throws IOException {
        Visual vis = _xServer.getRootVisual();

        io.writeInt(_rootWindow.getId());        // Root window ID.
        io.writeInt(_defaultColormap.getId());    // Default colormap ID.
        io.writeInt(_defaultColormap.getWhitePixel());    // White pixel.
        io.writeInt(_defaultColormap.getBlackPixel());    // Black pixel.
        io.writeInt(0);    // Current input masks.
        io.writeShort((short) getWidth());    // Width in pixels.
        io.writeShort((short) getHeight());    // Height in pixels.
        io.writeShort((short) (getWidth() / _pixelsPerMillimeter));    // Width in millimeters.
        io.writeShort((short) (getHeight() / _pixelsPerMillimeter));    // Height in millimeters.
        io.writeShort((short) 1);    // Minimum installed maps.
        io.writeShort((short) 1);    // Maximum installed maps.
        io.writeInt(vis.getId());    // Root visual ID.
        io.writeByte(vis.getBackingStoreInfo());
        io.writeByte((byte) (vis.getSaveUnder() ? 1 : 0));
        io.writeByte((byte) vis.getDepth());    // Root depth.
        io.writeByte((byte) 1);    // Number of allowed depths.

        // Write the only allowed depth.
        io.writeByte((byte) vis.getDepth());    // Depth.
        io.writeByte((byte) 0);    // Unused.
        io.writeShort((short) 1);    // Number of visuals with this depth.
        io.writePadBytes(4);    // Unused.
        vis.write(io);        // The visual at this depth.
    }

    /**
     * Write the screen's installed colormaps.
     *
     * @param client The remote client.
     * @throws IOException
     */
    public void writeInstalledColormaps(Client client) throws IOException {
        InputOutput io = client.getInputOutput();
        int n = _installedColormaps.size();

        synchronized (io) {
            Util.writeReplyHeader(client, (byte) 0);
            io.writeInt(n);    // Reply length.
            io.writeShort((short) n);    // Number of colormaps.
            io.writePadBytes(22);    // Unused.

            for (Colormap cmap : _installedColormaps)
                io.writeInt(cmap.getId());
        }
        io.flush();
    }

    /**
     * Process a screen-related request.
     *
     * @param xServer        The X server.
     * @param client         The remote client.
     * @param opcode         The request's opcode.
     * @param arg            Optional first argument.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    public void processRequest(XServer xServer, Client client, byte opcode, byte arg, int bytesRemaining) throws IOException {
        InputOutput io = client.getInputOutput();

        switch (opcode) {
            case RequestCode.SendEvent:
                if (bytesRemaining != 40) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    processSendEventRequest(_xServer, client, arg == 1);
                }
                break;
            case RequestCode.GrabPointer:
                if (bytesRemaining != 20) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    processGrabPointerRequest(_xServer, client, arg == 1);
                }
                break;
            case RequestCode.UngrabPointer:
                if (bytesRemaining != 4) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int time = io.readInt();    // Time.
                    int now = _xServer.getTimestamp();

                    if (time == 0) time = now;

                    if (time >= _grabPointerTime && time <= now && _grabPointerClient == client) {
                        _grabPointerClient = null;
                        _grabPointerWindow = null;
                        _grabCursor = null;
                        _grabConfineWindow = null;
                        updatePointer(2);
                    }
                }
                break;
            case RequestCode.GrabButton:
                if (bytesRemaining != 20) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    processGrabButtonRequest(_xServer, client, arg == 1);
                }
                break;
            case RequestCode.UngrabButton:
                if (bytesRemaining != 8) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int wid = io.readInt();    // Grab window.
                    int modifiers = io.readShort();    // Modifiers.
                    Resource r = _xServer.getResource(wid);

                    io.readSkip(2);    // Unused.

                    if (r == null || r.getType() != Resource.WINDOW) {
                        ErrorCode.write(client, ErrorCode.Window, opcode, wid);
                    } else {
                        Window w = (Window) r;

                        w.removePassiveButtonGrab(arg, modifiers);
                    }
                }
                break;
            case RequestCode.ChangeActivePointerGrab:
                if (bytesRemaining != 12) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int cid = io.readInt();    // Cursor.
                    int time = io.readInt();    // Time.
                    int mask = io.readShort();    // Event mask.
                    Cursor c = null;

                    io.readSkip(2);    // Unused.

                    if (cid != 0) {
                        Resource r = _xServer.getResource(cid);

                        if (r == null || r.getType() != Resource.CURSOR)
                            ErrorCode.write(client, ErrorCode.Cursor, opcode, 0);
                        else c = (Cursor) r;
                    }

                    int now = _xServer.getTimestamp();

                    if (time == 0) time = now;

                    if (_grabPointerWindow != null && !_grabPointerPassive && _grabPointerClient == client && time >= _grabPointerTime && time <= now && (cid == 0 || c != null)) {
                        _grabEventMask = mask;
                        if (c != null) _grabCursor = c;
                        else _grabCursor = _grabPointerWindow.getCursor();
                    }
                }
                break;
            case RequestCode.GrabKeyboard:
                if (bytesRemaining != 12) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    processGrabKeyboardRequest(_xServer, client, arg == 1);
                }
                break;
            case RequestCode.UngrabKeyboard:
                if (bytesRemaining != 4) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int time = io.readInt();    // Time.
                    int now = _xServer.getTimestamp();

                    if (time == 0) time = now;

                    if (time >= _grabKeyboardTime && time <= now) {
                        Window pw = _rootWindow.windowAtPoint(_motionX, _motionY);

                        Window.focusInOutNotify(_grabKeyboardWindow, _focusWindow, pw, _rootWindow, 2);
                        _grabKeyboardClient = null;
                        _grabKeyboardWindow = null;
                    }
                }
                break;
            case RequestCode.GrabKey:
                if (bytesRemaining != 12) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    processGrabKeyRequest(_xServer, client, arg == 1);
                }
                break;
            case RequestCode.UngrabKey:
                if (bytesRemaining != 8) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int wid = io.readInt();    // Grab window.
                    int modifiers = io.readShort();    // Modifiers.
                    Resource r = _xServer.getResource(wid);

                    io.readSkip(2);    // Unused.

                    if (r == null || r.getType() != Resource.WINDOW) {
                        ErrorCode.write(client, ErrorCode.Window, opcode, wid);
                    } else {
                        Window w = (Window) r;

                        w.removePassiveKeyGrab(arg, modifiers);
                    }
                }
                break;
            case RequestCode.AllowEvents:
                processAllowEvents(client, opcode, io, bytesRemaining, arg);
                break;
            case RequestCode.SetInputFocus:
                if (bytesRemaining != 8) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    processSetInputFocusRequest(_xServer, client, arg);
                }
                break;
            case RequestCode.GetInputFocus:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int wid;

                    if (_focusWindow == null) wid = 0;
                    else if (_focusWindow == _rootWindow) wid = 1;
                    else wid = _focusWindow.getId();

                    synchronized (io) {
                        Util.writeReplyHeader(client, _focusRevertTo);
                        io.writeInt(0);    // Reply length.
                        io.writeInt(wid);    // Focus window.
                        io.writePadBytes(20);    // Unused.
                    }
                    io.flush();
                }
                break;
        }
    }

    private void processAllowEvents(Client client, byte opcode, InputOutput io, int bytesRemaining, byte mode) throws IOException {
        if (bytesRemaining != 4) {
            io.readSkip(bytesRemaining);
            ErrorCode.write(client, ErrorCode.Length, opcode, 0);
            return;
        }

        int t = io.readInt();
        int now = _xServer.getTimestamp();
        int time = t == 0 ? now : t;
        if ((now < time) || (time < _grabPointerTime) || (time < _grabKeyboardTime)) {
            return;
        }

        String message;
        switch (mode) {
            case RequestCode.AllowEventsMode.AsyncPointer:
                flushPendingPointerEvents();
                _grabPointerSynchronous = false;
                _grabPointerFreezeNextEvent = false;
                break;
            case RequestCode.AllowEventsMode.SyncPointer:
                flushPendingPointerEvents();
                _grabPointerSynchronous = false;
                _grabPointerFreezeNextEvent = true;
                break;
            case RequestCode.AllowEventsMode.AsyncKeyboard:
                flushPendingKeyboardEvents();
                _grabKeyboardSynchronous = false;
                _grabKeyboardFreezeNextEvent = false;
                break;
            case RequestCode.AllowEventsMode.SyncKeyboard:
                flushPendingKeyboardEvents();
                _grabKeyboardSynchronous = false;
                _grabKeyboardFreezeNextEvent = true;
                break;
            case RequestCode.AllowEventsMode.AsyncBoth:
            case RequestCode.AllowEventsMode.SyncBoth:
            case RequestCode.AllowEventsMode.ReplayPointer:
            case RequestCode.AllowEventsMode.ReplayKeyboard:
                String fmt = "unsupported AllowEvents mode: %d (%s)";
                String name = RequestCode.AllowEventsMode.toString(mode);
                message = String.format(fmt, mode, name);
                reportError(client, ErrorCode.Implementation, opcode, message);
                break;
            default:
                message = String.format("unknown AllowEvents mode: %d", mode);
                reportError(client, ErrorCode.Value, opcode, message);
                break;
        }
    }

    private void reportError(Client client, byte error, byte opcode, String message) throws IOException {
        ErrorCode.write(client, error, opcode, 0);
    }

    /**
     * Toggle Arrows As Buttons.
     *
     * Switch between key and button events for arrow keys
     *
     * @return new state of switch
     */
    public boolean toggleArrowsAsButtons() {
        _arrowsAsButtons = !_arrowsAsButtons;
        return _arrowsAsButtons;
    }

    /**
     * Toggle shared clipboard. Shared clipboard works when using the long press 
     * action shortcuts.
     *
     * @return new state of switch
     */
    public boolean toggleSharedClipboard() {
        _sharedClipboard = !_sharedClipboard;
        return _sharedClipboard;
    }

    /**
     * Toggle Inhibit Back Button.
     *
     * @return new state of switch
     */
    public boolean toggleInhibitBackButton() {
        _inhibitBackButton = !_inhibitBackButton;
        return _inhibitBackButton;
    }

    /**
     * Toggle touchscreen mouse click emulation.
     *
     * @return new state of switch
     */
    public boolean toggleEnableTouchClicks() {
        _enableTouchClicks = !_enableTouchClicks;
        return _enableTouchClicks;
    }

    /**
     * Process a SendEvent request.
     *
     * @param xServer   The X server.
     * @param client    The remote client.
     * @param propagate Propagate flag.
     * @throws IOException
     */
    private void processSendEventRequest(XServer xServer, Client client, boolean propagate) throws IOException {
        InputOutput io = client.getInputOutput();
        int wid = io.readInt();    // Destination window.
        int mask = io.readInt();    // Event mask.
        byte[] event = new byte[32];
        Window w;

        io.readBytes(event, 0, 32);    // Event.

        if (wid == 0) {        // Pointer window.
            w = _rootWindow.windowAtPoint(_motionX, _motionY);
        } else if (wid == 1) {    // Input focus.
            if (_focusWindow == null) {
                ErrorCode.write(client, ErrorCode.Window, RequestCode.SendEvent, wid);
                return;
            }

            Window pw = _rootWindow.windowAtPoint(_motionX, _motionY);

            if (pw.isAncestor(_focusWindow)) w = pw;
            else w = _focusWindow;
        } else {
            Resource r = _xServer.getResource(wid);

            if (r == null || r.getType() != Resource.WINDOW) {
                ErrorCode.write(client, ErrorCode.Window, RequestCode.SendEvent, wid);
                return;
            } else w = (Window) r;
        }

        Vector<Client> dc = null;

        if (mask == 0) {
            dc = new Vector<Client>();
            dc.add(w.getClient());
        } else if (!propagate) {
            dc = w.getSelectingClients(mask);
        } else {
            for (; ; ) {
                if ((dc = w.getSelectingClients(mask)) != null) break;

                mask &= ~w.getDoNotPropagateMask();
                if (mask == 0) break;

                w = w.getParent();
                if (w == null) break;
                if (wid == 1 && w == _focusWindow) break;
            }
        }

        if (dc == null) return;

        for (Client c : dc) {
            if (c == null) continue;
            InputOutput dio = c.getInputOutput();

            synchronized (dio) {
                dio.writeByte((byte) (event[0] | 128));

                if (event[0] == EventCode.KeymapNotify) {
                    dio.writeBytes(event, 1, 31);
                } else {
                    dio.writeByte(event[1]);
                    dio.writeShort((short) (c.getSequenceNumber() & 0xffff));
                    dio.writeBytes(event, 4, 28);
                }
            }
            dio.flush();
        }
    }

    /**
     * Process a GrabPointer request.
     *
     * @param xServer     The X server.
     * @param client      The remote client.
     * @param ownerEvents Owner-events flag.
     * @throws IOException
     */
    private void processGrabPointerRequest(XServer xServer, Client client, boolean ownerEvents) throws IOException {
        InputOutput io = client.getInputOutput();
        int wid = io.readInt();    // Grab window.
        int mask = io.readShort();    // Event mask.
        boolean psync = (io.readByte() == 0);    // Pointer mode.
        boolean ksync = (io.readByte() == 0);    // Keyboard mode.
        int cwid = io.readInt();    // Confine-to.
        int cid = io.readInt();    // Cursor.
        int time = io.readInt();    // Time.
        Resource r = _xServer.getResource(wid);

        if (r == null || r.getType() != Resource.WINDOW) {
            ErrorCode.write(client, ErrorCode.Window, RequestCode.GrabPointer, wid);
            return;
        }

        Window w = (Window) r;
        Cursor c = null;
        Window cw = null;

        if (cwid != 0) {
            r = _xServer.getResource(cwid);

            if (r == null || r.getType() != Resource.WINDOW) {
                ErrorCode.write(client, ErrorCode.Window, RequestCode.GrabPointer, cwid);
                return;
            }
            cw = (Window) r;
        }

        if (cid != 0) {
            r = _xServer.getResource(cid);
            if (r != null && r.getType() != Resource.CURSOR) {
                ErrorCode.write(client, ErrorCode.Cursor, RequestCode.GrabPointer, cid);
                return;
            }

            c = (Cursor) r;
        }

        if (c == null) c = w.getCursor();

        byte status = 0;    // Success.
        int now = _xServer.getTimestamp();

        if (time == 0) time = now;

        if (time < _grabPointerTime || time > now) {
            status = 2;    // Invalid time.
        } else if (_grabPointerWindow != null && _grabPointerClient != client) {
            status = 1;    // Already grabbed.
        } else {
            _grabPointerClient = client;
            _grabPointerWindow = w;
            _grabPointerPassive = false;
            _grabPointerAutomatic = false;
            _grabPointerTime = time;
            _grabCursor = c;
            _grabConfineWindow = cw;
            _grabEventMask = mask;
            _grabPointerOwnerEvents = ownerEvents;
            _grabPointerSynchronous = psync;
            _grabKeyboardSynchronous = ksync;
        }

        synchronized (io) {
            Util.writeReplyHeader(client, status);
            io.writeInt(0);    // Reply length.
            io.writePadBytes(24);    // Unused.
        }
        io.flush();

        if (status == 0) updatePointer(1);
    }

    /**
     * Process a GrabButton request.
     *
     * @param xServer     The X server.
     * @param client      The remote client.
     * @param ownerEvents Owner-events flag.
     * @throws IOException
     */
    private void processGrabButtonRequest(XServer xServer, Client client, boolean ownerEvents) throws IOException {
        InputOutput io = client.getInputOutput();
        int wid = io.readInt();    // Grab window.
        int mask = io.readShort();    // Event mask.
        boolean psync = (io.readByte() == 0);    // Pointer mode.
        boolean ksync = (io.readByte() == 0);    // Keyboard mode.
        int cwid = io.readInt();    // Confine-to.
        int cid = io.readInt();    // Cursor.
        byte button = (byte) io.readByte();    // Button.
        int modifiers;
        Resource r = _xServer.getResource(wid);

        io.readSkip(1);    // Unused.
        modifiers = io.readShort();    // Modifiers.

        if (r == null || r.getType() != Resource.WINDOW) {
            ErrorCode.write(client, ErrorCode.Window, RequestCode.GrabPointer, wid);
            return;
        }

        Window w = (Window) r;
        Cursor c = null;
        Window cw = null;

        if (cwid != 0) {
            r = _xServer.getResource(cwid);

            if (r == null || r.getType() != Resource.WINDOW) {
                ErrorCode.write(client, ErrorCode.Window, RequestCode.GrabPointer, cwid);
                return;
            }
            cw = (Window) r;
        }

        if (cid != 0) {
            r = _xServer.getResource(cid);

            if (r != null && r.getType() != Resource.CURSOR) {
                ErrorCode.write(client, ErrorCode.Cursor, RequestCode.GrabPointer, cid);
                return;
            }
            c = (Cursor) r;
        }

        w.addPassiveButtonGrab(new PassiveButtonGrab(client, w, button, modifiers, ownerEvents, mask, psync, ksync, cw, c));
    }

    /**
     * Process a GrabKeyboard request.
     *
     * @param xServer     The X server.
     * @param client      The remote client.
     * @param ownerEvents Owner-events flag.
     * @throws IOException
     */
    private void processGrabKeyboardRequest(XServer xServer, Client client, boolean ownerEvents) throws IOException {
        InputOutput io = client.getInputOutput();
        int wid = io.readInt();    // Grab window.
        int time = io.readInt();    // Time.
        boolean psync = (io.readByte() == 0);    // Pointer mode.
        boolean ksync = (io.readByte() == 0);    // Keyboard mode.
        Resource r = _xServer.getResource(wid);

        io.readSkip(2);    // Unused.

        if (r == null || r.getType() != Resource.WINDOW) {
            ErrorCode.write(client, ErrorCode.Window, RequestCode.GrabKeyboard, wid);
            return;
        }

        Window w = (Window) r;
        byte status = 0;    // Success.
        int now = _xServer.getTimestamp();

        if (time == 0) time = now;

        if (time < _grabKeyboardTime || time > now) {
            status = 2;    // Invalid time.
        } else if ((_grabKeyboardWindow != null) && (_grabKeyboardClient != client)) {
            status = 1;    // Already grabbed.
        } else {
            _grabKeyboardClient = client;
            _grabKeyboardWindow = w;
            _grabKeyboardTime = time;
            _grabKeyboardOwnerEvents = ownerEvents;
            _grabPointerSynchronous = psync;
            _grabKeyboardSynchronous = ksync;
        }

        synchronized (io) {
            Util.writeReplyHeader(client, status);
            io.writeInt(0);    // Reply length.
            io.writePadBytes(24);    // Unused.
        }
        io.flush();

        if (status == 0)
            Window.focusInOutNotify(_focusWindow, w, _rootWindow.windowAtPoint(_motionX, _motionY), _rootWindow, 1);
    }

    /**
     * Process a GrabKey request.
     *
     * @param xServer     The X server.
     * @param client      The remote client.
     * @param ownerEvents Owner-events flag.
     * @throws IOException
     */
    private void processGrabKeyRequest(XServer xServer, Client client, boolean ownerEvents) throws IOException {
        InputOutput io = client.getInputOutput();
        int wid = io.readInt();    // Grab window.
        int modifiers = io.readShort();    // Modifiers.
        byte keycode = (byte) io.readByte();    // Key.
        boolean psync = (io.readByte() == 0);    // Pointer mode.
        boolean ksync = (io.readByte() == 0);    // Keyboard mode.
        Resource r = _xServer.getResource(wid);

        io.readSkip(3);    // Unused.

        if (r == null || r.getType() != Resource.WINDOW) {
            ErrorCode.write(client, ErrorCode.Window, RequestCode.GrabPointer, wid);
            return;
        }

        Window w = (Window) r;

        w.addPassiveKeyGrab(new PassiveKeyGrab(client, w, keycode, modifiers, ownerEvents, psync, ksync));
    }

    /**
     * Process a SetInputFocus request.
     *
     * @param xServer  The X server.
     * @param client   The remote client.
     * @param revertTo 0=None, 1=Root, 2=Parent.
     * @throws IOException
     */
    private void processSetInputFocusRequest(XServer xServer, Client client, byte revertTo) throws IOException {
        InputOutput io = client.getInputOutput();
        int wid = io.readInt();    // Focus window.
        int time = io.readInt();    // Time.
        Window w;

        if (wid == 0) {
            w = null;
            revertTo = 0;
        } else if (wid == 1) {
            w = _rootWindow;
            revertTo = 0;
        } else {
            Resource r = xServer.getResource(wid);

            if (r == null || r.getType() != Resource.WINDOW) {
                ErrorCode.write(client, ErrorCode.Window, RequestCode.GrabPointer, wid);
                return;
            }

            w = (Window) r;
        }

        int now = xServer.getTimestamp();

        if (time == 0) time = now;

        if (time < _focusLastChangeTime || time > now) return;

        Window.focusInOutNotify(_focusWindow, w, _rootWindow.windowAtPoint(_motionX, _motionY), _rootWindow, _grabKeyboardWindow == null ? 0 : 3);

        _focusWindow = w;
        _focusRevertTo = revertTo;
        _focusLastChangeTime = time;
    }

    private void reflectPointerFreezeNextEvent() {
        _grabPointerSynchronous = _grabPointerFreezeNextEvent;
    }

    private void reflectKeyboardFreezeNextEvent() {
        _grabKeyboardSynchronous = _grabKeyboardFreezeNextEvent;
    }

    private void callGrabButtonNotify(Window w, boolean pressed, int motionX, int motionY, int button, int grabEventMask, Client grabPointerClient, boolean grabPointerOwnerEvents) {
        w.grabButtonNotify(pressed, motionX, motionY, button, grabEventMask, grabPointerClient, grabPointerOwnerEvents);
        reflectPointerFreezeNextEvent();
    }

    private void callGrabMotionNotify(Window w, int x, int y, int buttons, int grabEventMask, Client grabPointerClient, boolean grabPointerOwnerEvents) {
        w.grabMotionNotify(x, y, buttons & 0xff00, grabEventMask, grabPointerClient, grabPointerOwnerEvents);
    }

    private void callGrabKeyNotify(Window w, boolean pressed, int motionX, int motionY, int keycode, Client grabKeyboardClient, boolean grabKeyboardOwnerEvents) {
        w.grabKeyNotify(pressed, motionX, motionY, keycode, grabKeyboardClient, grabKeyboardOwnerEvents);
        reflectKeyboardFreezeNextEvent();
    }

    private void flushPendingPointerEvents() {
        flushPendingEvents(mPendingPointerEvents);
    }

    private void flushPendingKeyboardEvents() {
        flushPendingEvents(mPendingKeyboardEvents);
    }

    private void flushPendingEvents(PendingEventQueue q) {
        PendingEvent e;
        while ((e = q.next()) != null) {
            e.run();
        }
    }

    /**
     * @return true if shared clipboard is enabled, false otherwise.
     */
    public boolean hasSharedClipboard() {
        return _sharedClipboard;
    }

    /**
     * @return Window used for shared clipboard.
     */
    public Window getSharedClipboardWindow(){
        return _sharedClipboardWindow;
    }
}
