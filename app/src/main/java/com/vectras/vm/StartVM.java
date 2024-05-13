package com.vectras.vm;

import android.app.Activity;
import android.content.Intent;

import com.vectras.qemu.Config;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.utils.RamInfo;
import com.vectras.vm.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class StartVM {
    public static String cache;

    public static String env(Activity activity, String extras, String img, String cpu) {

        String filesDir = activity.getFilesDir().getAbsolutePath();

        String[] qemu = new String[0];


        ArrayList<String> params = new ArrayList<>(Arrays.asList(qemu));

        if (MainSettingsManager.getArch(activity).equals("I386"))
            params.add("qemu-system-i386");
        else if (MainSettingsManager.getArch(activity).equals("X86_64"))
            params.add("qemu-system-x86_64");
        else if (MainSettingsManager.getArch(activity).equals("ARM64"))
            params.add("qemu-system-aarch64");
        else if (MainSettingsManager.getArch(activity).equals("PPC"))
            params.add("qemu-system-ppc");

        String ifType = MainSettingsManager.getIfType(activity);

        String cdrom;
        String hdd1;

        String hdd0 = "-drive";
        hdd0 += " index=0";
        hdd0 += ",media=disk";
        hdd0 += ",if=" + ifType;
        hdd0 += ",file='" + img + "'";

        params.add(hdd0);

        File cdromFile = new File(filesDir + "/data/Vectras/drive.iso");

        if (cdromFile.exists()) {
            cdrom = "-drive";
            cdrom += " index=1";
            cdrom += ",media=cdrom";
            cdrom += ",file='" + cdromFile.getPath() + "'";
            params.add(cdrom);
        }

        File hdd1File = new File(filesDir + "/data/Vectras/hdd1.qcow2");

        if (hdd1File.exists()) {
            hdd1 = "-drive";
            hdd1 += " index=2";
            hdd1 += ",media=disk";
            hdd1 += ",if=" + ifType;
            hdd1 += ",file='" + hdd1File.getPath() + "'";
            params.add(hdd1);
        }


        if (MainSettingsManager.getSharedFolder(activity)) {
            String driveParams = "-drive ";
            driveParams += "index=3,media=disk,file=fat:";
            driveParams += "rw:"; //Disk Drives are always Read/Write
            driveParams += FileUtils.getExternalFilesDirectory(activity).getPath() + "/SharedFolder,format=raw";
            params.add(driveParams);
        }

        String memoryStr = "-m ";
        memoryStr += RamInfo.vectrasMemory();

        String boot = "-boot ";
        boot += MainSettingsManager.getBoot(activity);

        String soundDevice = "-audiodev pa,id=pa -device AC97,audiodev=pa";

        //params.add(soundDevice);

        String bios = "-bios ";
        bios += AppConfig.basefiledir + "bios-vectras.bin";

        String machine = "-M ";
        if (Objects.equals(MainSettingsManager.getArch(activity), "X86_64")) {
            machine += "pc";
            params.add(machine);
        } else if (Objects.equals(MainSettingsManager.getArch(activity), "ARM64")) {
            machine += "virt";
            params.add(machine);
        }

        params.add("-overcommit");
        params.add("mem-lock=off");

        params.add("-rtc");
        params.add("base=localtime");

        params.add("-nodefaults");

        if (Objects.equals(MainSettingsManager.getArch(activity), "ARM64")) {
            params.add(bios);
        }

        params.add(boot);

        params.add(memoryStr);

        if (MainSettingsManager.getVmUi(activity).equals("VNC")) {
            String vncStr = "-vnc ";
            params.add(vncStr);
            // Allow connections only from localhost using localsocket without a password
            if (MainSettingsManager.getVncExternal(activity))
                params.add(Config.defaultVNCHost + ":" + Config.defaultVNCPort);
            else {
                String qmpParams = "unix:";
                qmpParams += Config.getLocalVNCSocketPath();
                params.add(qmpParams);
            }

            params.add("-monitor");
            if (MainSettingsManager.getArch(activity).equals("X86_64"))
                params.add("vc");
            else if (MainSettingsManager.getArch(activity).equals("ARM64"))
                params.add("stdio");
        } else if (MainSettingsManager.getVmUi(activity).equals("SPICE")) {
            String spiceStr = "-spice ";
            spiceStr += "port=6999,disable-ticketing=on";
            params.add(spiceStr);
        } else if (MainSettingsManager.getVmUi(activity).equals("X11")) {

        }

        params.add(extras);

        return String.join(" ", params);
    }

}
