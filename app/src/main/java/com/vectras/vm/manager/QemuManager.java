package com.vectras.vm.manager;

import android.content.Context;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.settings.ItemSettingsSelector;
import com.vectras.vterm.Terminal;

public class QemuManager {
    public static final int DEFAULT_REFRESH_RATE = 60;
    public static final int DEFAULT_LOW_REFRESH_RATE = 30;

    public static boolean isSupportSetRefreshRate(Context context) {
        return Terminal.executeShellCommandWithResult(getQemuExecutableFile(context) + " -vnc help", context).contains("refresh-rate");
    }

    public static boolean isSupportAcpiTable(Context context) {
        String currentArch = MainSettingsManager.getArch(context);

        return currentArch.equals(MainSettingsManager.X86_64_ARCH) || currentArch.equals(MainSettingsManager.I386_ARCH);
    }

    public static int getAppropriateRefreshRate(Context context, String params, int max) {
        int result = DEFAULT_REFRESH_RATE;

        if (params.contains(" virio-vga") || params.contains(" virio-gpu")) result = max;

        if (params.contains("-vga vmware")
                || params.contains(" vmware-svga")
                || params.contains("-vga qxl")
                || params.contains(" qxl-vga")) result = 75;

        int refreshRateSetting = Integer.parseInt(ItemSettingsSelector.getVncRefreshRateValue(MainSettingsManager.getVncRefreshRate(context)));

        result = Math.min(result, refreshRateSetting);

        return result;
    }

    public static String getQemuExecutableFile(Context context) {
        if (MainSettingsManager.getArch(context).equals(MainSettingsManager.I386_ARCH))
            return "qemu-system-i386";
        else if (MainSettingsManager.getArch(context).equals(MainSettingsManager.ARM64_ARCH))
            return "qemu-system-aarch64";
        else if (MainSettingsManager.getArch(context).equals(MainSettingsManager.PPC_ARCH))
            return "qemu-system-ppc";

        return "qemu-system-x86_64";
    }
}
