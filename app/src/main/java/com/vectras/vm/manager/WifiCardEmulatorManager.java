package com.vectras.vm.manager;

import android.content.Context;

import com.vectras.vm.setupwizard.SetupFeatureCore;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vterm.Terminal2;

import java.io.File;

public class WifiCardEmulatorManager {
    private static final String TAG = "WifiCardEmulatorManager";

    public static boolean setup(Context context, String vmId) {
        if (!QemuManager.isSupportAcpiTable(context)) return false;

        File vmTemp = new File(VmFileManager.getTempPath(context, vmId));

        try {
            String wifiAsl = SetupFeatureCore.readTextFromAssets(context, "roms/wifi.asl");

            FileUtils.writeToFile(vmTemp.getAbsolutePath(), "wifi.asl", wifiAsl);

            if (FileUtils.isFileExists(VmFileManager.getTempPath(context, vmId, "wifi.asl"))) {
                String respond = new Terminal2(context).executeOnThisThread("cd " + vmTemp.getAbsolutePath() + " && iasl wifi.asl && echo Compiled");
                //Log.d(TAG, respond);
                return respond.contains("Compiled");
            } else {
                return false;
            }
        } catch (Exception e) {
            //Log.d(TAG, "Error: \n", e);
            return false;
        }
    }
}
