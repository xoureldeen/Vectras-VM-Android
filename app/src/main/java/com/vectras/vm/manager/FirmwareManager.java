package com.vectras.vm.manager;

import android.content.Context;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.AppConfig;
import com.vectras.vm.setupwizard.SetupFeatureCore;
import com.vectras.vm.utils.FileUtils;

public class FirmwareManager {
    public static boolean isExistOne() {
        return FileUtils.isFileExists(AppConfig.basefiledir + "bios-vectras.bin") ||
                FileUtils.isFileExists(AppConfig.basefiledir + "QEMU_EFI.img") ||
                FileUtils.isFileExists(AppConfig.basefiledir + "QEMU_VARS.img") ||
                FileUtils.isFileExists(AppConfig.basefiledir + "RELEASEX64_OVMF.fd") ||
                FileUtils.isFileExists(AppConfig.basefiledir + "RELEASEX64_OVMF_VARS.fd");
    }

    public static void extract(Context context, boolean isUseUefi) {
        if (MainSettingsManager.useDefaultBios(context)) {
            String arch = MainSettingsManager.getArch(context);

            FileUtils.createDirectory(AppConfig.basefiledir);

            if (arch.equals("ARM64")) {
                if (!FileUtils.isFileExists(AppConfig.basefiledir + "QEMU_EFI.img"))
                    SetupFeatureCore.copyAssetToFile(context, "roms/QEMU_EFI.img", AppConfig.basefiledir + "QEMU_EFI.img");

                if (!FileUtils.isFileExists(AppConfig.basefiledir + "QEMU_VARS.img"))
                    SetupFeatureCore.copyAssetToFile(context, "roms/QEMU_VARS.img", AppConfig.basefiledir + "QEMU_VARS.img");
            } else if (arch.equals("X86_64") && (MainSettingsManager.getuseUEFI(context) || isUseUefi)) {
                if (!FileUtils.isFileExists(AppConfig.basefiledir + "RELEASEX64_OVMF.fd"))
                    SetupFeatureCore.copyAssetToFile(context, "roms/RELEASEX64_OVMF.fd", AppConfig.basefiledir + "RELEASEX64_OVMF.fd");

                if (!FileUtils.isFileExists(AppConfig.basefiledir + "RELEASEX64_OVMF_VARS.fd"))
                    SetupFeatureCore.copyAssetToFile(context, "roms/RELEASEX64_OVMF_VARS.fd", AppConfig.basefiledir + "RELEASEX64_OVMF_VARS.fd");
            } else {
                if (!FileUtils.isFileExists(AppConfig.basefiledir + "bios-vectras.bin"))
                    SetupFeatureCore.copyAssetToFile(context, "roms/bios-vectras.bin", AppConfig.basefiledir + "bios-vectras.bin");
            }
        }
    }

    public static void deleteAll() {
        FileUtils.delete(AppConfig.basefiledir + "bios-vectras.bin");
        FileUtils.delete(AppConfig.basefiledir + "QEMU_EFI.img");
        FileUtils.delete(AppConfig.basefiledir + "QEMU_VARS.img");
        FileUtils.delete(AppConfig.basefiledir + "RELEASEX64_OVMF.fd");
        FileUtils.delete(AppConfig.basefiledir + "RELEASEX64_OVMF_VARS.fd");
    }
}
