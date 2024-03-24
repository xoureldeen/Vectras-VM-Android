/**
 * Copyright (C) 2013- Iordan Iordanov
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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


package com.undatech.opaque;

import static com.undatech.opaque.RemoteClientLibConstants.ACTION_USB_PERMISSION;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.undatech.opaque.input.RemoteKeyboard;
import com.undatech.opaque.input.RemotePointer;
import com.undatech.opaque.util.GeneralUtils;
import com.undatech.opaque.util.UsbDeviceManager;

import org.freedesktop.gstreamer.GStreamer;

import java.util.ArrayList;
import java.util.Objects;

public class SpiceCommunicator extends RfbConnectable {

    final static int LCONTROL = 29;
    final static int RCONTROL = 285;
    final static int LALT = 56;
    final static int RALT = 312;
    final static int LSHIFT = 42;
    final static int RSHIFT = 54;
    final static int LWIN = 347;
    final static int RWIN = 348;
    private final static String TAG = "SpiceCommunicator";
    private static SpiceCommunicator myself = null;

    static {
        System.loadLibrary("gstreamer_android");
        System.loadLibrary("spice");
    }

    boolean isInNormalProtocol = false;
    UsbDeviceManager usbDeviceManager;
    private final BroadcastReceiver usbPermissionRequestedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                Log.d(TAG, "Requesting permission for USB device.");
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null && !usbDeviceManager.isRequested(device)) {
                            Log.d(TAG, "Permission granted for USB device: " + device);
                            int fDesc = usbDeviceManager.getFileDescriptorForUsbDevice(device);
                            spiceAttachUsbDeviceByFileDescriptor(fDesc);
                            usbDeviceManager.setPermission(device, true, fDesc);
                        }
                    } else {
                        Log.d(TAG, "Permission denied for USB device: " + device);
                        if (device != null) {
                            usbDeviceManager.setPermission(device, false, -1);
                        }
                    }
                    if (isInNormalProtocol) {
                        usbDeviceManager.getUsbDevicePermissions();
                    }
                }
            }
        }
    };
    BroadcastReceiver usbStateChangedReceiver = new BroadcastReceiver() {
        synchronized public void onReceive(Context context, Intent intent) {
            if (Objects.requireNonNull(intent.getExtras()).getBoolean("host_connected") ||
                    intent.getExtras().getBoolean("connected")) {
                Log.d(TAG, "Detected USB device connected");
                if (isInNormalProtocol) {
                    usbDeviceManager.getUsbDevicePermissions();
                }
            } else {
                Log.d(TAG, "Detected USB device disconnected");
                UsbDevice d = usbDeviceManager.getRemoved();
                while (d != null) {
                    int fDesc = usbDeviceManager.getFileDescriptorForDevice(d);
                    Log.d(TAG, "Disconnected USB fd: " + fDesc + " device: " + d);
                    if (fDesc >= 0) {
                        spiceDetachUsbDeviceByFileDescriptor(fDesc);
                    }
                    usbDeviceManager.removeRequested(d);
                    d = usbDeviceManager.getRemoved();
                }
            }
        }
    };
    private int width = 0;
    private int height = 0;
    private Thread thread = null;
    private int resolutionRequests = -1;
    private int maxResolutionRequests = 5;
    private boolean debugLogging;
    private Viewable canvas = null;
    private Bitmap bitmap = null;
    private ArrayList<String> vmNames = null;
    private boolean isRequestingNewDisplayResolution = false;
    private boolean usbEnabled = false;
    private Context context = null;

    public SpiceCommunicator(Context context, Handler handler, Viewable canvas, boolean res,
                             boolean usb, boolean debugLogging) {
        super(debugLogging, handler);
        this.context = context;
        this.canvas = canvas;
        this.isRequestingNewDisplayResolution = res;
        this.usbEnabled = usb;
        this.debugLogging = debugLogging;
        myself = this;

        try {
            GStreamer.init(context);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        usbDeviceManager = new UsbDeviceManager(context, usbEnabled);
        registerReceiversForUsbDevices(context);
        modifierMap.put(RemoteKeyboard.CTRL_MASK, LCONTROL);
        modifierMap.put(RemoteKeyboard.RCTRL_MASK, RCONTROL);
        modifierMap.put(RemoteKeyboard.ALT_MASK, LALT);
        modifierMap.put(RemoteKeyboard.RALT_MASK, RALT);
        modifierMap.put(RemoteKeyboard.SUPER_MASK, LWIN);
        modifierMap.put(RemoteKeyboard.RSUPER_MASK, RWIN);
        modifierMap.put(RemoteKeyboard.SHIFT_MASK, LSHIFT);
        modifierMap.put(RemoteKeyboard.RSHIFT_MASK, RSHIFT);

    }

    private void registerReceiversForUsbDevices(Context context) {
        if (usbEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(usbPermissionRequestedReceiver, new IntentFilter(ACTION_USB_PERMISSION), Context.RECEIVER_NOT_EXPORTED);
            } else {
                context.registerReceiver(usbPermissionRequestedReceiver, new IntentFilter(ACTION_USB_PERMISSION));
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.hardware.usb.action.USB_STATE");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(usbStateChangedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                context.registerReceiver(usbStateChangedReceiver, filter);
            }
        }
    }

    public static void sendMessage(int message) {
        android.util.Log.d(TAG, "sendMessage called with message: " + message);
        myself.handler.sendEmptyMessage(message);
    }

    public static void sendMessageWithText(int messageId, String messageText) {
        android.util.Log.d(TAG, "sendMessageWithText called with messageId: "
                + messageId + " and messageText: " + messageText);
        Bundle b = new Bundle();
        b.putString("message", messageText);
        Message m = myself.handler.obtainMessage();
        m.what = messageId;
        m.setData(b);
        myself.handler.sendMessage(m);
    }

    public static void LaunchVncViewer(String address, String port, String password) {
        android.util.Log.d(TAG, "LaunchVncViewer called");
        Bundle b = new Bundle();
        b.putString("address", address);
        b.putString("port", port);
        b.putString("password", password);
        Message msg = new Message();
        msg.what = RemoteClientLibConstants.LAUNCH_VNC_VIEWER;
        msg.setData(b);
        myself.handler.sendMessage(msg);
    }

    private static void AddVm(String vmname) {
        android.util.Log.d(TAG, "Adding VM: " + vmname + "to list of VMs");
        myself.vmNames.add(vmname);
    }

    private static void OnSettingsChanged(int inst, int width, int height, int bpp) {
        myself.onSettingsChanged(width, height, bpp);
    }

    private static void OnGraphicsUpdate(int inst, int x, int y, int width, int height) {
        //android.util.Log.i(TAG, "OnGraphicsUpdate called: " + x +", " + y + " + " + width + "x" + height );
        Bitmap bitmap = myself.canvas.getBitmap();
        if (bitmap != null) {
            synchronized (myself.canvas) {
                myself.UpdateBitmap(bitmap, x, y, width, height);
            }
            myself.canvas.reDraw(x, y, width, height);
        }
        //myself.onGraphicsUpdate(x, y, width, height);
    }

    private static void OnMouseUpdate(int x, int y) {
        //android.util.Log.i(TAG, "OnMouseUpdate called: " + x +", " + y);
        myself.canvas.setMousePointerPosition(x, y);
    }

    private static void OnMouseMode(boolean relative) {
        android.util.Log.i(TAG, "OnMouseMode called, relative: " + relative);
        myself.canvas.mouseMode(relative);
    }

    private static void ShowMessage(java.lang.String message) {
        android.util.Log.i(TAG, "ShowMessage called, message: " + message);
        sendMessageWithText(RemoteClientLibConstants.SHOW_TOAST, message);
    }

    public static void OnRemoteClipboardChanged(String data) {
        android.util.Log.d(TAG, "OnRemoteClipboardChanged called.");
        myself.remoteClipboardChanged(data);
    }

    public native int CreateOvirtSession(
            String uri,
            String user,
            String password,
            String sslCaFile,
            boolean sound,
            boolean sslStrict,
            String ssoToken,
            boolean onlyFetchVmNames
    );

    public native int StartSessionFromVvFile(String fileName, boolean sound);

    public native int SpiceClientConnect(String ip,
                                         String port,
                                         String tport,
                                         String password,
                                         String ca_file,
                                         String ca_cert,
                                         String cert_subj,
                                         boolean sound);

    public native void SpiceClientDisconnect();

    public native void SpiceButtonEvent(int x, int y, int metaState, int pointerMask, boolean rel);

    public native void SpiceKeyEvent(boolean keyDown, int virtualKeyCode);

    public native void UpdateBitmap(Bitmap bitmap, int x, int y, int w, int h);

    public native void SpiceRequestResolution(int x, int y);

    public native boolean SpiceAttachUsbDeviceByFileDescriptor(int fileDescriptor);

    public native boolean SpiceDetachUsbDeviceByFileDescriptor(int fileDescriptor);

    public native void SpiceClientCutText(String text);

    public ArrayList<String> getVmNames() {
        return vmNames;
    }

    /**
     * Launches a new thread which performs a plain SPICE connection.
     */
    public void connectSpice(String ip, String port, String tport, String password, String cf, String ca, String cs, boolean sound) {
        android.util.Log.d(TAG, "connectSpice: " + ip + ", " + port + ", " + tport + ", " + cf + ", " + cs);
        thread = new SpiceThread(ip, port, tport, password, cf, ca, cs, sound);
        thread.start();
    }

    /**
     * Launches a new thread which performs an oVirt/RHEV session connection
     */
    public void connectOvirt(
            String ip,
            String vmname,
            String user,
            String password,
            String sslCaFile,
            boolean sound,
            boolean sslStrict,
            String ssoToken
    ) {
        boolean tokenNull = ssoToken == null;
        android.util.Log.d(TAG, "connectOvirt: " + ip + ", " + vmname + ", " + user + ", tokenNull: " + tokenNull);
        thread = new OvirtThread(ip, vmname, user, password, sslCaFile, sound, sslStrict, ssoToken);
        thread.start();
    }

    /**
     * Connects to an oVirt/RHEV server to fetch the names of all VMs available to the specified user.
     */
    public int startSessionFromVvFile(String vvFileName, boolean sound) {
        android.util.Log.d(TAG, "Starting connection from vv file: " + vvFileName);
        return StartSessionFromVvFile(vvFileName, sound);
    }

    /**
     * Connects to an oVirt/RHEV server to fetch the names of all VMs available to the specified user.
     */
    public int fetchOvirtVmNames(
            String ip,
            String user,
            String password,
            String sslCaFile,
            boolean sslStrict,
            String ssoToken
    ) {
        vmNames = new ArrayList<String>();
        return CreateOvirtSession(
                "https://" + ip + "/",
                user,
                password,
                sslCaFile,
                false,
                sslStrict,
                ssoToken,
                true
        );
    }

    public void disconnect() {
        if (isInNormalProtocol) {
            SpiceClientDisconnect();
        }
        setIsInNormalProtocol(false);
    }

    public void sendMouseEvent(int x, int y, int metaState, int pointerMask, boolean rel) {
        //android.util.Log.d(TAG, "sendMouseEvent: " + x +"x" + y + "," + "metaState: " +
        //                   metaState + ", pointerMask: " + pointerMask);
        SpiceButtonEvent(x, y, metaState, pointerMask, rel);
    }

    public synchronized void sendSpiceKeyEvent(boolean keyDown, int virtualKeyCode) {
        GeneralUtils.debugLog(this.debugLogging, TAG, "sendSpiceKeyEvent: down: " + keyDown +
                " code: " + virtualKeyCode);
        SpiceKeyEvent(keyDown, virtualKeyCode);
    }

    public int framebufferWidth() {
        return width;
    }

    public int framebufferHeight() {
        return height;
    }

    public void setFramebufferWidth(int w) {
        width = w;
    }

    public void setFramebufferHeight(int h) {
        height = h;
    }

    public String desktopName() {
        // TODO Auto-generated method stub
        return "";
    }

    public void requestUpdate(boolean incremental) {
        // TODO Auto-generated method stub
    }

    public void writeClientCutText(String text) {
        Log.i(TAG, "writeClientCutText");
        if (isInNormalProtocol()) {
            SpiceClientCutText(text);
        }
    }

    public void setIsInNormalProtocol(boolean state) {
        isInNormalProtocol = state;
    }

    public boolean isInNormalProtocol() {
        return isInNormalProtocol;
    }

    public String getEncoding() {
        // TODO Auto-generated method stub
        return "";
    }

    @Override
    public synchronized void writePointerEvent(int x, int y, int metaState, int pointerMask, boolean rel) {
        this.metaState = metaState;
        if ((pointerMask & RemotePointer.POINTER_DOWN_MASK) != 0)
            sendModifierKeys(true);
        sendMouseEvent(x, y, metaState, pointerMask, rel);
        if ((pointerMask & RemotePointer.POINTER_DOWN_MASK) == 0)
            sendModifierKeys(false);
    }

    /* Callbacks from jni and corresponding non-static methods */

    private void sendModifierKeys(boolean down) {
        for (int modifierMask : modifierMap.keySet()) {
            if (remoteKeyboardState.shouldSendModifier(metaState, modifierMask, down)) {
                int modifier = modifierMap.get(modifierMask);
                GeneralUtils.debugLog(this.debugLogging, TAG, "sendModifierKeys, modifierMask:" +
                        modifierMask + ", sending: " + modifier + ", down: " + down);
                sendSpiceKeyEvent(down, modifier);
                remoteKeyboardState.updateRemoteMetaState(modifierMask, down);
            }
        }
    }

    public synchronized void writeKeyEvent(int key, int metaState, boolean keyDown) {
        if (keyDown) {
            this.metaState = metaState;
            sendModifierKeys(true);
        }

        GeneralUtils.debugLog(this.debugLogging, TAG,
                "writeKeyEvent: Sending scanCode: " + key + ". Is it down: " + keyDown);
        sendSpiceKeyEvent(keyDown, key);

        if (!keyDown) {
            sendModifierKeys(false);
            this.metaState = 0;
        }
    }

    public void writeSetPixelFormat(int bitsPerPixel, int depth,
                                    boolean bigEndian, boolean trueColour, int redMax, int greenMax,
                                    int blueMax, int redShift, int greenShift, int blueShift,
                                    boolean fGreyScale) {
        // TODO Auto-generated method stub

    }

    public void writeFramebufferUpdateRequest(int x, int y, int w, int h, boolean b) {
        // TODO Auto-generated method stub
    }

    public void close() {
        disconnect();
    }

    @Override
    public boolean isCertificateAccepted() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setCertificateAccepted(boolean certificateAccepted) {
        // TODO Auto-generated method stub
    }
    /* END Callbacks from jni and corresponding non-static methods */

    public void requestResolution(int width, int height) {
        android.util.Log.d(TAG, "requestResolution()");
        if (!isRequestingNewDisplayResolution) {
            android.util.Log.d(TAG, "Requesting remote resolution is disabled");
            return;
        }
        if (isInNormalProtocol) {
            int currentWidth = this.width;
            int currentHeight = this.height;
            // Request new resolution at least once or keep requesting up to a maximum number of times.
            if ((resolutionRequests == -1 || currentWidth != width || currentHeight != height) &&
                    resolutionRequests < maxResolutionRequests) {
                canvas.waitUntilInflated();
                android.util.Log.d(TAG, "Requesting new resolution: " + width + "x" + height);
                SpiceRequestResolution(width, height);
                writePointerEvent(0, 0, 0, 0, true);
                resolutionRequests++;
            } else if (currentWidth == width && currentHeight == height) {
                android.util.Log.d(TAG, "Resolution request satisfied, resetting resolutionRequests count");
                resolutionRequests = 0;
            } else {
                android.util.Log.d(TAG, "Resolution request disabled or last request unsatisfied (resolution request loop?).");
                isRequestingNewDisplayResolution = false;
            }
        }
    }

    synchronized public void spiceAttachUsbDeviceByFileDescriptor(int fDesc) {
        Log.d(TAG, "Attaching USB device by fd: " + fDesc);
        if (!SpiceAttachUsbDeviceByFileDescriptor(fDesc)) {
            Log.d(TAG, "Failed to attach USB device by fd: " + fDesc);
        }
    }

    synchronized public void spiceDetachUsbDeviceByFileDescriptor(int fDesc) {
        Log.d(TAG, "Detaching USB device by fd: " + fDesc);
        if (!SpiceDetachUsbDeviceByFileDescriptor(fDesc)) {
            Log.d(TAG, "Failed to detach USB device by fd: " + fDesc);
        }
    }

    public void onSettingsChanged(int width, int height, int bpp) {
        android.util.Log.i(TAG, "onSettingsChanged called, wxh: " + width + "x" + height);

        setFramebufferWidth(width);
        setFramebufferHeight(height);

        canvas.reallocateDrawable(width, height);

        setIsInNormalProtocol(true);
        handler.sendEmptyMessage(RemoteClientLibConstants.SPICE_CONNECT_SUCCESS);

        if (isRequestingNewDisplayResolution) {
            requestResolution(canvas.getDesiredWidth(), canvas.getDesiredHeight());
            handler.postDelayed(new Runnable() {
                public void run() {
                    requestResolution(canvas.getDesiredWidth(), canvas.getDesiredHeight());
                }
            }, 2000);
        }
        usbDeviceManager.getUsbDevicePermissions();
    }

    class SpiceThread extends Thread {
        boolean sound;
        private String ip, port, tport, password, cf, ca, cs;

        public SpiceThread(String ip, String port, String tport, String password, String cf, String ca, String cs, boolean sound) {
            this.ip = ip;
            this.port = port;
            this.tport = tport;
            this.password = password;
            this.cf = cf;
            this.ca = ca;
            this.cs = cs;
            this.sound = sound;
        }

        public void run() {
            SpiceClientConnect(ip, port, tport, password, cf, ca, cs, sound);
            android.util.Log.d(TAG, "SpiceClientConnect returned.");

            // If we've exited SpiceClientConnect, the connection is certainly
            // interrupted or was never established.
            if (handler != null) {
                handler.sendEmptyMessage(RemoteClientLibConstants.SPICE_CONNECT_FAILURE);
            }
        }
    }

    class OvirtThread extends Thread {
        boolean sound, sslStrict;
        final private String ip, vmname, user, password, sslCaFile, ssoToken;

        public OvirtThread(
                String ip,
                String vmname,
                String user,
                String password,
                String sslCaFile,
                boolean sound,
                boolean sslStrict,
                String ssoToken
        ) {
            this.ip = ip;
            this.vmname = vmname;
            this.user = user;
            this.password = password;
            this.sslCaFile = sslCaFile;
            this.sound = sound;
            this.sslStrict = sslStrict;
            this.ssoToken = ssoToken;
        }

        public void run() {
            CreateOvirtSession(
                    "ovirt://" + ip + "/" + vmname,
                    user,
                    password,
                    sslCaFile,
                    sound,
                    sslStrict,
                    ssoToken,
                    false
            );
            android.util.Log.d(TAG, "CreateOvirtSession returned.");

            // If we've exited CreateOvirtSession, the connection is certainly
            // interrupted or was never established.
            if (handler != null) {
                handler.sendEmptyMessage(RemoteClientLibConstants.SPICE_CONNECT_FAILURE);
            }
        }
    }
}
