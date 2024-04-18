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

public class StartVM {
    public static String cache;

    static String[] qemu = new String[]{"qemu-system-x86_64"};

    public static String env(Activity activity, String extras, String img) {

        String filesDir = activity.getFilesDir().getAbsolutePath();

        ArrayList<String> params = new ArrayList<>(Arrays.asList(qemu));

        String ifType = MainSettingsManager.getIfType(activity);

        String cdrom;
        String hdd1;
        String hdd2;

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

        File hdd2File = new File(filesDir + "/data/Vectras/hdd2.qcow2");

        if (hdd2File.exists()) {
            hdd2 = "-drive";
            hdd2 += " index=3";
            hdd2 += ",media=disk";
            hdd2 += ",if=" + ifType;
            hdd2 += ",file='" + hdd2File.getPath() + "'";
            params.add(hdd2);
        }

        /*if (MainSettingsManager.getSharedFolder(activity)) {
            params.add("-net user,smb='" + FileUtils.getExternalFilesDirectory(activity).getPath() + "/SharedFolder" + "'");
            params.add("-net nic,model=virtio");
        }*/

        boolean kvm = MainSettingsManager.getKvm(activity);
        boolean avx = MainSettingsManager.getAvx(activity);

        String cpuStr = "-cpu " + MainSettingsManager.getCpu(activity);
        if (avx)
            cpuStr += ",+avx";

        String smpCores = "-smp ";
        smpCores += MainSettingsManager.getCpuCores(activity);

        String memoryStr = "-m ";
        memoryStr += RamInfo.vectrasMemory();

        String acclerationStr;
        if (kvm)
            acclerationStr = "-accel kvm";
        else
            acclerationStr = "-accel tcg,thread=multi";
        acclerationStr += ",tb-size=" + MainSettingsManager.getTbSize(activity);

        String boot = "-boot ";
        boot += MainSettingsManager.getBoot(activity);

        String soundDevice = "-audiodev pa,id=pa -device AC97,audiodev=pa";

        //params.add(soundDevice);

        String bios = "-bios ";
        bios += AppConfig.basefiledir + "/bios-vectras.bin";

        String machine = "-M ";
        machine += "pc";

        params.add(machine);

        params.add("-overcommit");
        params.add("mem-lock=off");

        params.add("-rtc");
        params.add("base=localtime");

        params.add("-nodefaults");

        params.add(bios);

        params.add(boot);

        params.add(memoryStr);

        params.add(cpuStr);

        params.add(smpCores);

        params.add(acclerationStr);

        params.add(MainSettingsManager.getCustomParams(activity));

        if (MainSettingsManager.getVmUi(activity).equals("VNC")) {
            String vncStr = "-vnc ";
            params.add(vncStr);
            // Allow connections only from localhost using localsocket without a password
            //params.add(Config.defaultVNCHost+":" + Config.defaultVNCPort);
            String qmpParams = "unix:";
            qmpParams += Config.getLocalVNCSocketPath();
            params.add(qmpParams);
            params.add("-monitor");
            params.add("vc");

            //XXX: monitor, serial, and parallel display crashes cause SDL doesn't support more than 1 window
            params.add("-monitor");
            params.add("none");

            params.add("-serial");
            params.add("none");

            params.add("-parallel");
            params.add("none");

        } else if (MainSettingsManager.getVmUi(activity).equals("SPICE")) {
            String spiceStr = "-spice ";
            spiceStr += "port=6999,disable-ticketing=on";
            params.add(spiceStr);
        } else if (MainSettingsManager.getVmUi(activity).equals("X11")) {

        }
        params.add("-k");
        params.add("en-us");

        params.add("-usb");

        if (!MainSettingsManager.getMouse(activity).equals("ps2-mouse")) {
            params.add("-device");
            params.add(MainSettingsManager.getMouse(activity));
        }

        if (!MainSettingsManager.getKeyboard(activity).equals("ps2-kbd")) {
            params.add("-device");
            params.add(MainSettingsManager.getKeyboard(activity));
        }

        params.add(extras);

        params.add(MainSettingsManager.getCustomParams(activity));

        return String.join(" ", params);
    }

}
