package com.vectras.qemu.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.vectras.qemu.Config;
import com.vectras.vm.MainActivity;
import com.vectras.vm.R;
import com.vectras.vm.utils.UIUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class Machine {

    public static final String EMPTY = "empty";
    public static String TAG = "Machine";
    
    public static void promptPausedVM(final Activity activity) {

        new AlertDialog.Builder(activity, R.style.MainDialogTheme).setCancelable(false).setTitle("Paused").setMessage("VM is now Paused tap OK to exit")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        Log.i(TAG, "VM Paused, Shutting Down");
                        if (activity.getParent() != null) {
                            activity.getParent().finish();
                        } else {
                            activity.finish();
                        }

                        if (MainActivity.vmexecutor != null) {
                            MainActivity.vmexecutor.stopvm(0);
                        }
                    }
                }).show();
    }


    public static void onRestartVM(final Context context) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                if (MainActivity.vmexecutor != null) {
                    Log.v(TAG, "Restarting the VM...");
                    MainActivity.vmexecutor.stopvm(1);

                    MainActivity.vmStarted = true;
                    if(Config.showToast)
                        UIUtils.toastShort(context, "VM Reset");

                } else {
                    if(Config.showToast)
                        UIUtils.toastShort(context, "VM Not Running");
                }
            }
        });
        t.start();
    }

    public static void pausedErrorVM(Activity activity, String errStr) {

        errStr = errStr != null ? errStr : "Could not pause VM. View log for details";

        new AlertDialog.Builder(activity, R.style.MainDialogTheme).setTitle("Error").setMessage(errStr)
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

    public static void stopVM(final Activity activity) {

        new AlertDialog.Builder(activity, R.style.MainDialogTheme).setTitle("Shutdown VM")
                .setMessage("To avoid any corrupt data make sure you "
                        + "have already shutdown the Operating system from within the VM. Continue?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (activity.getParent() != null) {
                            activity.getParent().finish();
                        } else {
                            activity.finish();
                        }

                        if (MainActivity.vmexecutor != null) {
                            MainActivity.vmexecutor.stopvm(0);
                        }
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        }).show();
    }

    public static MainActivity.VMStatus checkSaveVMStatus(final Activity activity) {
        String pause_state = "";
        if (MainActivity.vmexecutor != null) {

            String command = QmpClient.query_migrate();
            String res = QmpClient.sendCommand(command);

            if (res != null && !res.equals("")) {
                //Log.d(TAG, "Migrate status: " + res);
                try {
                    JSONObject resObj = new JSONObject(res);
                    String resInfo = resObj.getString("return");
                    JSONObject resInfoObj = new JSONObject(resInfo);
                    pause_state = resInfoObj.getString("status");
                } catch (JSONException e) {
                    if (Config.debug)
                        Log.e(TAG,e.getMessage());
                        //e.printStackTrace();
                }
                if (pause_state != null && pause_state.toUpperCase().equals("FAILED")) {
                    Log.e(TAG, "Error: " + res);
                }
            }
        }

        if (pause_state.toUpperCase().equals("ACTIVE")) {
            return MainActivity.VMStatus.Saving;
        } else if (pause_state.toUpperCase().equals("COMPLETED")) {
            MainActivity.vmexecutor.paused = 1;
            
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    promptPausedVM(activity);
                }
            }, 1000);
            return MainActivity.VMStatus.Completed;

        } else if (pause_state.toUpperCase().equals("FAILED")) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    pausedErrorVM(activity, null);
                }
            }, 100);
            return MainActivity.VMStatus.Failed;
        }
        return MainActivity.VMStatus.Unknown;
    }

    public static boolean isHostX86_64() {
    	if(Build.SUPPORTED_64_BIT_ABIS != null)
		{
			for(int i=0; i< Build.SUPPORTED_64_BIT_ABIS.length ; i++)
				if(Build.SUPPORTED_64_BIT_ABIS[i].equals("x86_64"))
					return true;
		}
		return false;
	}

    public static boolean isHostX86() {
		if(Build.SUPPORTED_32_BIT_ABIS != null)
		{
			for(int i=0; i< Build.SUPPORTED_32_BIT_ABIS.length ; i++)
				if(Build.SUPPORTED_32_BIT_ABIS[i].equals("x86"))
					return true;
		}
		return false;
	}

    public static boolean isHostArm() {
		if(Build.SUPPORTED_32_BIT_ABIS != null)
		{
			for(int i=0; i< Build.SUPPORTED_32_BIT_ABIS.length ; i++)
				if(Build.SUPPORTED_32_BIT_ABIS[i].equals("armeabi-v7a"))
					return true;
		}
		return false;
	}

    public static boolean isHostArmv8() {
		if(Build.SUPPORTED_64_BIT_ABIS != null)
		{
			for(int i=0; i< Build.SUPPORTED_64_BIT_ABIS.length ; i++)
				if(Build.SUPPORTED_64_BIT_ABIS[i].equals("arm64-v8a"))
					return true;
		}
		return false;
	}

    public static boolean isHost64Bit() {
        return Build.SUPPORTED_64_BIT_ABIS!=null && Build.SUPPORTED_64_BIT_ABIS.length > 0 ;
    }


    public static void resetVM(final Activity activity) {

        new AlertDialog.Builder(activity, R.style.MainDialogTheme).setTitle("Reset VM")
                .setMessage("To avoid any corrupt data make sure you "
                        + "have already shutdown the Operating system from within the VM. Continue?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(new Runnable() {
                            public void run() {
                                Log.v(TAG, "VM is reset");
                                Machine.onRestartVM(activity);
                            }
                        }).start();

                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        }).show();
    }
}
