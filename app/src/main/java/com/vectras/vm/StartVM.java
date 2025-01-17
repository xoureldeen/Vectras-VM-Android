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

    public static String cdrompath;

    public static String env(Activity activity, String extras, String img, String cpu) {

        String filesDir = activity.getFilesDir().getAbsolutePath();

        String[] qemu = new String[0];

        String bios = "";

        String finalextra = extras;

        ArrayList<String> params = new ArrayList<>(Arrays.asList(qemu));

        if (cpu != null) {
            if (cpu.isEmpty()) {
                if (MainSettingsManager.getArch(activity).equals("I386"))
                    params.add("qemu-system-i386");
                else if (MainSettingsManager.getArch(activity).equals("X86_64"))
                    params.add("qemu-system-x86_64");
                else if (MainSettingsManager.getArch(activity).equals("ARM64"))
                    params.add("qemu-system-aarch64");
                else if (MainSettingsManager.getArch(activity).equals("PPC"))
                    params.add("qemu-system-ppc");

                String ifType;
                ifType= MainSettingsManager.getIfType(activity);

                String cdrom = "";
                String hdd0;
                String hdd1;

                if (!img.isEmpty()) {
                    if (ifType.isEmpty()) {
                        hdd0 = "-hda";
                        hdd0 += " '" + img + "'";
                    } else {
                        hdd0 = "-drive";
                        hdd0 += " index=0";
                        hdd0 += ",media=disk";
                        hdd0 += ",if=" + ifType;
                        hdd0 += ",file='" + img + "'";

                        if ((MainSettingsManager.getArch(activity).equals("ARM64") && ifType.equals("ide")) || MainSettingsManager.getArch(activity).equals("PPC")) {
                            hdd0 = "-drive";
                            hdd0 += " index=0";
                            hdd0 += ",media=disk";
                            hdd0 += ",file='" + img + "'";
                        }
                    }
                    params.add(hdd0);
                }

                if (cdrompath.isEmpty()) {
                    File cdromFile = new File(filesDir + "/data/Vectras/drive.iso");

                    if (cdromFile.exists()) {
                        if (MainSettingsManager.getArch(activity).equals("ARM64")) {
                            cdrom = " -drive";
                            cdrom += " if=none,id=cdrom,format=raw,media=cdrom,file='" + cdromFile.getPath() + "'";
                            cdrom += "-device";
                            cdrom += " usb-storage,drive=cdrom";
                            if (!extras.contains("-device nec-usb-xhci")) {
                                cdrom += " -device";
                                cdrom += " qemu-xhci";
                                cdrom += " -device";
                                cdrom += " nec-usb-xhci";
                            }
                        } else {
                            if (ifType.isEmpty()) {
                                cdrom = "-cdrom";
                                cdrom += " '" + cdromFile.getPath() + "'";
                            } else {
                                cdrom = "-drive";
                                cdrom += " index=1";
                                cdrom += ",media=cdrom";
                                cdrom += ",file='" + cdromFile.getPath() + "'";
                            }
                        }

                        params.add(cdrom);
                    }
                } else {
                    if (MainSettingsManager.getArch(activity).equals("ARM64")) {
                        cdrom += " -device";
                        cdrom += " nec-usb-xhci,id=defaultxhci";
                        cdrom += " -device";
                        cdrom += " usb-storage,bus=defaultxhci.0,drive=cdrom";
                        cdrom += " -drive";
                        cdrom += " if=none,id=cdrom,format=raw,media=cdrom,file='" + cdrompath + "'";
                    } else {
                        if (ifType.isEmpty()) {
                            cdrom = "-cdrom";
                            cdrom += " '" + cdrompath + "'";
                        } else {
                            cdrom = "-drive";
                            cdrom += " index=1";
                            cdrom += ",media=cdrom";
                            cdrom += ",file='" + cdrompath + "'";
                        }
                    }
                    params.add(cdrom);
                }

                File hdd1File = new File(filesDir + "/data/Vectras/hdd1.qcow2");

                if (hdd1File.exists()) {
                    if (ifType.isEmpty()) {
                        hdd1 = "-hdb";
                        hdd1 += " '" + hdd1File.getPath() + "'";
                    } else {
                        hdd1 = "-drive";
                        hdd1 += " index=2";
                        hdd1 += ",media=disk";
                        hdd1 += ",if=" + ifType;
                        hdd1 += ",file='" + hdd1File.getPath() + "'";
                    }

                    params.add(hdd1);
                }

                if (MainSettingsManager.getSharedFolder(activity)) {
                    String driveParams = "-drive ";
                    if (ifType.isEmpty()) {
                        driveParams += "media=disk,file=fat:";
                    } else {
                        driveParams += "index=3,media=disk,file=fat:";
                    }
                    driveParams += "rw:"; //Disk Drives are always Read/Write
                    driveParams += FileUtils.getExternalFilesDirectory(activity).getPath() + "/SharedFolder,format=raw";
                    params.add(driveParams);
                }

                String memoryStr = "-m ";
                if (MainSettingsManager.getArch(activity).equals("PPC") && RamInfo.vectrasMemory() > 2048) {
                    memoryStr += 2048;
                } else {
                    memoryStr += RamInfo.vectrasMemory();
                }

                String boot = "-boot ";
                if (extras.contains(".iso ")) {

                    boot += MainSettingsManager.getBoot(activity);
                } else {
                    boot += "c";
                }

                String soundDevice = "-audiodev pa,id=pa -device AC97,audiodev=pa";

                //params.add(soundDevice);

                if (MainSettingsManager.useDefaultBios(MainActivity.activity)) {
                    if (MainSettingsManager.getArch(activity).equals("PPC")) {
                        bios = "-L ";
                        bios += "pc-bios";
                    } else if (MainSettingsManager.getArch(activity).equals("ARM64")) {
                        bios = "-drive ";
                        bios += "file=" + AppConfig.basefiledir + "QEMU_EFI.img,format=raw,readonly=on,if=pflash";
                        bios += " -drive ";
                        bios += "file=" + AppConfig.basefiledir + "QEMU_VARS.img,format=raw,if=pflash";
                    } else {
                        bios = "-bios ";
                        bios += AppConfig.basefiledir + "bios-vectras.bin";
                    }
                }

                String machine = "-M ";
                if (Objects.equals(MainSettingsManager.getArch(activity), "X86_64")) {
                    machine += "pc";
                    params.add(machine);
                } else if (Objects.equals(MainSettingsManager.getArch(activity), "ARM64")) {
                    machine += "virt";
                    params.add(machine);
                }

                if (MainSettingsManager.useMemoryOvercommit(MainActivity.activity)) {
                    params.add("-overcommit");
                    params.add("mem-lock=off");
                }


                if (MainSettingsManager.useLocalTime(MainActivity.activity)) {
                    params.add("-rtc");
                    params.add("base=localtime");
                }

                //if (!MainSettingsManager.getArch(activity).equals("PPC")) {
                //params.add("-nodefaults");
                //}

                //if (!Objects.equals(MainSettingsManager.getArch(activity), "ARM64")) {
                params.add(bios);
                //}

                params.add(boot);

                params.add(memoryStr);

                if (ifType.isEmpty()) {
                    if (extras.contains("-drive index=1,media=cdrom,file=")) {
                        finalextra = extras.replace("-drive index=1,media=cdrom,file=", "-cdrom ");
                    }
                } else {
                    if (extras.contains("-cdrom ")) {
                        finalextra = extras.replace("-cdrom ", "-drive index=1,media=cdrom,file=");
                    }
                }
            }
        }

        params.add(finalextra);

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

            //if (!MainSettingsManager.getArch(activity).equals("PPC") || !MainSettingsManager.getArch(activity).equals("ARM64")) {
            params.add("-monitor");
            params.add("vc");
            //}
        } else if (MainSettingsManager.getVmUi(activity).equals("SPICE")) {
            String spiceStr = "-spice ";
            spiceStr += "port=6999,disable-ticketing=on";
            params.add(spiceStr);
        } else if (MainSettingsManager.getVmUi(activity).equals("X11")) {
            params.add("-display");
            params.add("gtk");
        }

        //params.add("-full-screen");

        params.add("-qmp");
        params.add("tcp:localhost:4444,server,nowait");

        return String.join(" ", params);
    }

}
