package com.vectras.vm.manager;

import android.content.Context;

import com.vectras.vm.setupwizard.SetupFeatureCore;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vterm.Terminal2;

import java.io.File;

public class BatteryEmulatorManager {
    private static final String TAG = "BatteryEmulatorManager";
    private static final int DESIGN_CAPACITY = 9000;
    private static final double PROPERTY_CAPACITY_DEPLETED_EACH_CYCLE = 0.04;

    private static final String CHARGING_STATUS_CHARGING = "0x02";
    private static final String CHARGING_STATUS_PLUGGED_IN_BUT_NOT_CHARGING = "0x00";

    private static final String DESIGN_CAPACITY_MARK = "${remaining-capacity}";
    private static final String LAST_FULL_CHARGE_CAPACITY_MARK = "${last-full-charge-capacity}";
    private static final String CHARGING_STATUS_MARK = "${charging-status}";

    public static boolean setup(Context context, String vmId) {
        if (!QemuManager.isSupportAcpiTable(context)) return false;

        int batteryPropertyCapacity = DeviceUtils.getBatteryPropertyCapacity(context);
        if (batteryPropertyCapacity < 1) batteryPropertyCapacity = 100;

        int batteryCycleCount = DeviceUtils.getBatteryCycleCount(context);
        if (batteryCycleCount < 1) batteryCycleCount = 1;

        File vmTemp = new File(VmFileManager.getTempPath(context, vmId));

        try {
            String batteryAsl = SetupFeatureCore.readTextFromAssets(context, "roms/battery.asl");
            int remainingCapacity;
            int lastFullChargeCapacity = (int) (DESIGN_CAPACITY - ((double) (DESIGN_CAPACITY / 100) * (batteryCycleCount * PROPERTY_CAPACITY_DEPLETED_EACH_CYCLE)));

            if (batteryCycleCount < 2) {
                remainingCapacity = DESIGN_CAPACITY / 100 * batteryPropertyCapacity;
            } else {
                remainingCapacity = lastFullChargeCapacity / 100 * batteryPropertyCapacity;
            }

            batteryAsl = batteryAsl
                    .replace(DESIGN_CAPACITY_MARK, String.valueOf(remainingCapacity))
                    .replace(LAST_FULL_CHARGE_CAPACITY_MARK, String.valueOf(lastFullChargeCapacity))
                    .replace(CHARGING_STATUS_MARK, batteryPropertyCapacity < 100 ? CHARGING_STATUS_CHARGING : CHARGING_STATUS_PLUGGED_IN_BUT_NOT_CHARGING);

            FileUtils.writeToFile(vmTemp.getAbsolutePath(), "battery.asl", batteryAsl);

            if (FileUtils.isFileExists(VmFileManager.getTempPath(context, vmId, "battery.asl"))) {
                String respond = new Terminal2(context).executeOnThisThread("cd " + vmTemp.getAbsolutePath() + " && iasl battery.asl && echo Compiled");
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
