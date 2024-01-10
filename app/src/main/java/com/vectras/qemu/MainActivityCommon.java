package com.vectras.qemu;

import android.androidVNC.RfbProto;
import android.androidVNC.VncCanvas;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.vectras.qemu.jni.StartVM;
import com.vectras.qemu.utils.FileInstaller;
import com.vectras.qemu.utils.FileUtils;
import com.vectras.qemu.utils.Machine;
import com.vectras.qemu.utils.QmpClient;
import com.vectras.vm.R;
import com.vectras.vm.utils.UIUtils;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivityCommon {

    public static VMStatus currStatus = VMStatus.Ready;
    public static boolean vmStarted = false;
    public static StartVM vmexecutor;
    public static String vnc_passwd = "vectras";
    public static int vnc_allow_external = 1;
    public static int qmp_allow_external = 0;
    public static ProgressDialog progDialog;
    public static View parent;
    public static InstallerTask installerTaskTask;
    public static boolean timeQuit = false;
    public static Object lockTime = new Object();

    public static final String TAG = "VECTRAS";

    public static AppCompatActivity activity = null;

    public static boolean libLoaded;


    static public void onInstall(boolean force) {
        FileInstaller.installFiles(activity, force);
    }

    public static String getVnc_passwd() {
        return vnc_passwd;
    }

    public static void setVnc_passwd(String vnc_passwd) {
        vnc_passwd = vnc_passwd;
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
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
    public static void startvm(Activity activity, int UI) {
        QmpClient.allow_external = (qmp_allow_external == 1);
        vmexecutor.qmp_allow_external = qmp_allow_external;

        if (UI == Config.UI_VNC) {
            // disable sound card with VNC
            vmexecutor.enablevnc = 1;
            vmexecutor.enablespice = 0;
            vmexecutor.sound_card = null;
            vmexecutor.vnc_allow_external = vnc_allow_external;
            RfbProto.allow_external = (vnc_allow_external == 1);
            vmexecutor.vnc_passwd = vnc_passwd;
        } else if (UI == Config.UI_SDL) {
            vmexecutor.enablevnc = 0;
            vmexecutor.enablespice = 0;
        } else if (UI == Config.UI_SPICE) {
            vmexecutor.vnc_allow_external = vnc_allow_external;
            vmexecutor.vnc_passwd = vnc_passwd;
            vmexecutor.enablevnc = 0;
            vmexecutor.enablespice = 1;
        }
        vmexecutor.startvm(activity, UI);

    }


    public static void cleanup() {

        vmStarted = false;

        //XXX flush and close all file descriptors if we haven't already
        FileUtils.close_fds();

        ////XXX; we wait till fds flush and close
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //set the exit code
        MainSettingsManager.setExitCode(activity, 1);

        //XXX: SDL seems to lock the keyboard events
        // unless we finish the starting activity
        activity.finish();

        Log.v(TAG, "Exit");
        //XXX: We exit here to force unload the native libs
        System.exit(0);


    }

    public static void changeStatus(final VMStatus status_changed) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (status_changed == VMStatus.Running) {

                    vmStarted = true;
                } else if (status_changed == VMStatus.Ready || status_changed == VMStatus.Stopped) {

                } else if (status_changed == VMStatus.Saving) {

                } else if (status_changed == VMStatus.Paused) {

                }
            }
        });

    }

    public static void install(boolean force) {
        progDialog = ProgressDialog.show(activity, "Please Wait", "Installing BIOS...", true);
        installerTaskTask = new InstallerTask();
        installerTaskTask.force = force;
        installerTaskTask.execute();
    }

    public static void checkAndLoadLibs() {
        if (Config.loadNativeLibsEarly)
            if (Config.loadNativeLibsMainThread)
                setupNativeLibs();
            else
                setupNativeLibsAsync();
    }

    public static void clearNotifications() {
        NotificationManager notificationManager = (NotificationManager) activity.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    public static void setupNativeLibsAsync() {

        Thread thread = new Thread(new Runnable() {
            public void run() {
                setupNativeLibs();
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();

    }

    public static void savePendingEditText() {
        View currentView = activity.getCurrentFocus();
        if (currentView != null && currentView instanceof EditText) {
            ((EditText) currentView).setFocusable(false);
        }
    }

    public static void checkLog() {

        Thread t = new Thread(new Runnable() {
            public void run() {

                if (MainSettingsManager.getExitCode(activity) != 1) {
                    MainSettingsManager.setExitCode(activity, 1);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            UIUtils.promptShowLog(activity);
                        }
                    });
                }
            }
        });
        t.start();
    }

    public static void setupFolders() {
        Thread t = new Thread(new Runnable() {
            public void run() {

                Config.cacheDir = activity.getCacheDir().getAbsolutePath();
                Config.storagedir = Environment.getExternalStorageDirectory().toString();

                // Create Temp folder
                File folder = new File(Config.getTmpFolder());
                if (!folder.exists())
                    folder.mkdirs();


            }
        });
        t.start();
    }

    //XXX: sometimes this needs to be called from the main thread otherwise
    //  qemu crashes when it is started later
    public static void setupNativeLibs() {

        if (libLoaded)
            return;

        //Some devices need stl loaded upfront
        //System.loadLibrary("stlport_shared");

        //Compatibility lib
        System.loadLibrary("compat-vectras");

        //Glib deps
        System.loadLibrary("compat-musl");


        //Glib
        System.loadLibrary("glib-2.0");

        //Pixman for qemu
        System.loadLibrary("pixman-1");

        //Spice server
        if (Config.enable_SPICE) {
            System.loadLibrary("crypto");
            System.loadLibrary("ssl");
            System.loadLibrary("spice");
        }

        // //Load SDL library
        if (Config.enable_SDL) {
            System.loadLibrary("SDL2");
        }

        System.loadLibrary("compat-SDL2-ext");

        //Vectras needed for vmexecutor
        System.loadLibrary("vectras");

        loadQEMULib();

        libLoaded = true;
    }

    public static void loadQEMULib() {

        try {
            System.loadLibrary("qemu-system-i386");
        } catch (Error ex) {
            System.loadLibrary("qemu-system-x86_64");
        }

    }


    public static void setupStrictMode() {

        if (Config.debugStrictMode) {
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

    public static void onLicense() {
        PackageInfo pInfo = null;

        try {
            pInfo = activity.getPackageManager().getPackageInfo(activity.getClass().getPackage().getName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }

        final PackageInfo finalPInfo = pInfo;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });

    }

    // Main event function
    // Retrives values from saved preferences
    public static void onStartButton() {
        if (MainService.isRunning) {
            startvnc();
        } else {
            if (vmexecutor == null) {

                try {
                    vmexecutor = new StartVM(activity);
                } catch (Exception ex) {
                    UIUtils.toastLong(activity, "Error: " + ex);
                    return;

                }
            }
            // dns
            vmexecutor.dns_addr = Config.defaultDNSServer;

            vmexecutor.paused = 0;

            if (!vmStarted) {
                UIUtils.toastShort(activity, "Starting VM");
                //XXX: make sure that bios files are installed in case we ran out of space in the last
                //  run
                FileInstaller.installFiles(activity, false);
            } else {
                UIUtils.toastShort(activity, "Connecting to VM");
            }

            if (Config.ui.equals("VNC")) {
                vmexecutor.enableqmp = 1; // We enable qemu monitor
                startVNC();

            } else if (Config.ui.equals("SDL")) {
                vmexecutor.enableqmp = 0; // We disable qemu monitor
                startSDL();
            } else {
                vmexecutor.enableqmp = 1; // We enable qemu monitor
                startSPICE();
            }
        }
    }

    public static String getLanguageCode(int index) {
        // TODO: Add more languages from /assets/roms/keymaps
        switch (index) {
            case 0:
                return "en-us";
            case 1:
                return "es";
            case 2:
                return "fr";
        }
        return null;
    }

    public static void startSDL() {

        Thread tsdl = new Thread(new Runnable() {
            public void run() {
                startsdl();
            }
        });
        if (Config.maxPriority)
            tsdl.setPriority(Thread.MAX_PRIORITY);
        tsdl.start();
    }

    public static void startVNC() {

        VncCanvas.retries = 0;
        if (!vmStarted) {

            Thread tvm = new Thread(new Runnable() {
                public void run() {
                    startvm(activity, Config.UI_VNC);
                }
            });
            if (Config.maxPriority)
                tvm.setPriority(Thread.MAX_PRIORITY);
            tvm.start();
        } else {
            startvnc();
        }


    }

    public static void startSPICE() {

        if (!vmStarted) {

            Thread tvm = new Thread(new Runnable() {
                public void run() {
                    startvm(activity, Config.UI_SPICE);
                }
            });
            if (Config.maxPriority)
                tvm.setPriority(Thread.MAX_PRIORITY);
            tvm.start();
        }

    }

    public static void onStopButton(boolean exit) {
        stopVM(exit);
    }

    public static void onRestartButton() {

        execTimer();

        Machine.resetVM(activity);
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

    public static void toggleVisibility(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.GONE);
        } else if (view.getVisibility() == View.GONE || view.getVisibility() == View.INVISIBLE) {
            view.setVisibility(View.VISIBLE);
        }
    }

    public static boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            activity.moveTaskToBack(true);
            return true; // return
        }

        return false;
    }


    public static void startvnc() {

        // Wait till Qemu settles
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Logger.getLogger(activity.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        if (MainSettingsManager.getVncExternal(activity)) {

        } else {
            connectLocally();
        }
    }

    public static void promptConnectLocally(final Activity activity) {

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                final AlertDialog alertDialog;
                alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
                alertDialog.setTitle("VNC Started");
                TextView stateView = new TextView(activity);
                stateView.setText("VNC Server started: " + getLocalIpAddress() + ":" + Config.defaultVNCPort + "\n"
                        + "Warning: VNC Connection is Unencrypted and not secure make sure you're on a private network!\n");

                stateView.setPadding(20, 20, 20, 20);
                alertDialog.setView(stateView);

                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                    }
                });
                alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Connect Locally", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        connectLocally();
                    }
                });
                alertDialog.show();
            }
        }, 100);


    }

    public static void connectLocally() {
        //UIUtils.toastShort(MainActivity.this, "Connecting to VM Display");
        Intent intent = getVNCIntent();
        activity.startActivityForResult(intent, Config.VNC_REQUEST_CODE);
    }

    public static void startsdl() {

        Intent intent = null;

        intent = new Intent(activity, MainSDLActivity.class);

        android.content.ContentValues values = new android.content.ContentValues();
        activity.startActivityForResult(intent, Config.SDL_REQUEST_CODE);
    }


    public static void resumevm() {
        if (vmexecutor != null) {
            vmexecutor.resumevm();
            UIUtils.toastShort(activity, "VM Reset");
        } else {

            UIUtils.toastShort(activity, "VM not running");
        }

    }

    public static Intent getVNCIntent() {
        return new Intent(activity, com.vectras.qemu.MainVNCActivity.class);

    }


    public static void goToSettings() {
        Intent i = new Intent(activity, MainSettingsManager.class);
        activity.startActivity(i);
    }

    public static void onViewLog() {

        Thread t = new Thread(new Runnable() {
            public void run() {
                FileUtils.viewVectrasLog(activity);
            }
        });
        t.start();
    }

    public static void goToURL(String url) {

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        activity.startActivity(i);

    }

    public static void stopVM(boolean exit) {
        execTimer();
        Machine.stopVM(activity);
    }

    public static void stopTimeListener() {

        synchronized (lockTime) {
            timeQuit = true;
            lockTime.notifyAll();
        }
    }


    public static void timer() {
        //XXX: No timers just ping a few times
        for (int i = 0; i < 3; i++) {
            checkAndUpdateStatus(false);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

    }

    public static void checkAndUpdateStatus(boolean force) {
        if (vmexecutor != null) {
            VMStatus status = checkStatus();
            if (force || status != currStatus) {
                currStatus = status;
                changeStatus(status);
            }
        }
    }

    public static void execTimer() {

        Thread t = new Thread(new Runnable() {
            public void run() {
                startTimer();
            }
        });
        t.start();
    }

    public static void startTimer() {
        stopTimeListener();

        timeQuit = false;
        try {
            timer();
        } catch (Exception ex) {
            ex.printStackTrace();

        }

    }


    public static enum VMStatus {
        Ready, Stopped, Saving, Paused, Completed, Failed, Unknown, Running
    }

    public static VMStatus checkStatus() {
        VMStatus state = VMStatus.Ready;
        if (vmexecutor != null && libLoaded && vmexecutor.get_state().toUpperCase().equals("RUNNING")) {
            state = VMStatus.Running;
        }
        return state;
    }

    public static class InstallerTask extends AsyncTask<Void, Void, Void> {
        public boolean force;

        @Override
        protected Void doInBackground(Void... arg0) {
            onInstall(force);
            if (progDialog.isShowing()) {
                progDialog.dismiss();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void test) {

        }
    }

}
