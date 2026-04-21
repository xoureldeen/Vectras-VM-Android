package com.vectras.vm;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.vectras.qemu.Config;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.utils.RamInfo;
import com.vectras.vm.creator.VMCreatorSelector;
import com.vectras.vm.main.vms.DataMainRoms;
import com.vectras.vm.manager.VmFileManager;
import com.vectras.vm.setupwizard.SetupFeatureCore;
import com.vectras.vm.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class StartVM {
    public static String cache;
    private static DataMainRoms vmConfigs;

    public static String env(Activity activity, DataMainRoms vmData) {
        vmConfigs = vmData;

        if (VMManager.isNeedLoadMigrate() && FileUtils.isFileExists(VmFileManager.getSnapshotSh(Config.vmID))) {
            String snapshotParams = FileUtils.readAFile(VmFileManager.getSnapshotSh(Config.vmID)).replace("\n", "");
            if (VMManager.isthiscommandsafe(snapshotParams, activity)) {
                snapshotParams = removeQemuSystem(snapshotParams);
                snapshotParams = removeQmpParams(snapshotParams);
                snapshotParams = removeDisplayParams(snapshotParams);
                snapshotParams = getQmpParams() + " " + snapshotParams;
                snapshotParams = getQemuSystem(activity) + " " + snapshotParams;
                snapshotParams += " " + getDisplayParams(activity);
                if (!snapshotParams.contains("-incoming defer")) snapshotParams += " -incoming defer";
                Log.d("StartVM.env", snapshotParams);
                return snapshotParams;
            }
        }

        String extraParams = vmData.itemExtra;

        String bootFromParams = Objects.requireNonNull(VMCreatorSelector.getBootFrom(activity, vmData.bootFrom).get("value")).toString();
        String showBootMenuParams = vmData.isShowBootMenu ? "menu=on" : "";
        String bootParams = "";
        if (!bootFromParams.isEmpty() || !showBootMenuParams.isEmpty()) {
            bootParams = "-boot " + bootFromParams + (!bootFromParams.isEmpty() && !showBootMenuParams.isEmpty() ? "," : "") + showBootMenuParams + " ";
        }

        extraParams = bootParams + extraParams;
        return env(activity, extraParams, vmData.itemPath, false);
    }

    public static String env(Activity activity, String extras, String img, boolean isQuickRun) {
        if (isQuickRun) {
            vmConfigs = new DataMainRoms();
            vmConfigs.isUseLocalTime = false;
        }

        String filesDir = activity.getFilesDir().getAbsolutePath();

        String[] qemu = new String[0];

        String bios = "";

        String finalextra = extras;

        ArrayList<String> params = new ArrayList<>(Arrays.asList(qemu));

        if (!isQuickRun) {
            if (MainSettingsManager.getArch(activity).equals("I386"))
                params.add("qemu-system-i386");
            else if (MainSettingsManager.getArch(activity).equals("X86_64"))
                params.add("qemu-system-x86_64");
            else if (MainSettingsManager.getArch(activity).equals("ARM64"))
                params.add("qemu-system-aarch64");
            else if (MainSettingsManager.getArch(activity).equals("PPC"))
                params.add("qemu-system-ppc");

            params.add(getQmpParams());

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

            if (vmConfigs.imgCdrom.isEmpty()) {
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
                    cdrom += " if=none,id=cdrom,format=raw,media=cdrom,file='" + vmConfigs.imgCdrom + "'";
                } else {
                    if (ifType.isEmpty()) {
                        cdrom = "-cdrom";
                        cdrom += " '" + vmConfigs.imgCdrom + "'";
                    } else {
                        cdrom = "-drive";
                        cdrom += " index=1";
                        cdrom += ",media=cdrom";
                        cdrom += ",file='" + vmConfigs.imgCdrom + "'";
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

            if (MainSettingsManager.getSharedFolder(activity) && !MainSettingsManager.getArch(activity).equals("I386")) {
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
            if (MainSettingsManager.getArch(activity).equals("PPC") && RamInfo.vectrasMemory(activity) > 2048) {
                memoryStr += 2048;
            } else {
                memoryStr += RamInfo.vectrasMemory(activity);
            }

//            String boot = "-boot ";
//            if (extras.contains(".iso ")) {
//
//                boot += MainSettingsManager.getBoot(activity);
//            } else {
//                boot += "c";
//            }

            //String soundDevice = "-audiodev pa,id=pa -device AC97,audiodev=pa";

            //params.add(soundDevice);

            if (MainSettingsManager.useDefaultBios(activity)) {
                if (MainSettingsManager.getArch(activity).equals("PPC")) {
                    bios = "-L ";
                    bios += "pc-bios";
                } else if (MainSettingsManager.getArch(activity).equals("ARM64")) {
                    bios = "-drive ";
                    bios += "file=" + AppConfig.basefiledir + "QEMU_EFI.img,format=raw,readonly=on,if=pflash";
                    bios += " -drive ";
                    bios += "file=" + AppConfig.basefiledir + "QEMU_VARS.img,format=raw,if=pflash";
                } else if (MainSettingsManager.getArch(activity).equals("X86_64") && (vmConfigs.isUseUefi)) {
                    bios = "-drive ";
                    bios += "file=" + AppConfig.basefiledir + "RELEASEX64_OVMF.fd,format=raw,readonly=on,if=pflash";
                    bios += " -drive ";
                    bios += "file=" + AppConfig.basefiledir + "RELEASEX64_OVMF_VARS.fd,format=raw,if=pflash";
                } else {
                    bios = "-bios ";
                    bios += AppConfig.basefiledir + "bios-vectras.bin";
                }

                extractFirmware(activity);
            }

            String machine = "-M ";
            if (Objects.equals(MainSettingsManager.getArch(activity), "X86_64")) {
                machine += "pc";
                params.add(machine);
            } else if (Objects.equals(MainSettingsManager.getArch(activity), "ARM64")) {
                machine += "virt";
                params.add(machine);
            }

            if (MainSettingsManager.useMemoryOvercommit(activity)) {
                params.add("-overcommit");
                params.add("mem-lock=off");
            }


            if (vmConfigs.isUseLocalTime) {
                params.add("-rtc");
                params.add("base=localtime");
            }

            //if (!MainSettingsManager.getArch(activity).equals("PPC")) {
            //params.add("-nodefaults");
            //}

            //if (!Objects.equals(MainSettingsManager.getArch(activity), "ARM64")) {
            params.add(bios);
            //}

//            params.add(boot);

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

        params.add(finalextra);

        if (isQuickRun) {
            params.add(getQmpParams());
        }

        params.add(getDisplayParams(activity));

        if (VMManager.isNeedLoadMigrate()) {
            params.add("-incoming");
            params.add("defer");
        }

        return String.join(" ", params);
    }

    public static String getQemuSystem(Context context) {
        if (MainSettingsManager.getArch(context).equals("I386"))
            return "qemu-system-i386";
        else if (MainSettingsManager.getArch(context).equals("X86_64"))
            return "qemu-system-x86_64";
        else if (MainSettingsManager.getArch(context).equals("ARM64"))
            return "qemu-system-aarch64";
        else if (MainSettingsManager.getArch(context).equals("PPC"))
            return "qemu-system-ppc";

        return "qemu-system-x86_64";
    }

    public static String removeQemuSystem(String params) {
        return params.replaceAll("^qemu-system-\\S+\\s*", "");
    }


    public static String getQmpParams() {
        return "-qmp unix:" + Config.getLocalQMPSocketPath() + ",server,nowait";
    }
    public static String removeQmpParams(String params) {
        return params.replaceAll("(?<=\\s|^)-qmp\\s+(\"[^\"]+\"|\\S+)", "");
    }


    public static String getDisplayParams(Context context) {
        String params = "";

        if (MainSettingsManager.getVmUi(context).equals("VNC")) {
            if (!MainSettingsManager.getVncExternalPassword(context).isEmpty()) {
                params += "-object secret,id=vncpass,data=\"" + MainSettingsManager.getVncExternalPassword(context) + "\"";
            }

            params += (params.isEmpty() ? "" : " ") + "-vnc ";
            // Allow connections only from localhost using localsocket without a password
            if (MainSettingsManager.getVncExternal(context)) {
                String vncParams = Config.defaultVNCHost + ":" + Config.defaultVNCPort;

                if (!MainSettingsManager.getVncExternalPassword(context).isEmpty())
                    vncParams += ",password-secret=vncpass";

                params += vncParams;
            } else {
                params += "unix:" + Config.getLocalVNCSocketPath();
            }

            params += " -monitor vc";
        } else if (MainSettingsManager.getVmUi(context).equals("SPICE")) {
            params += "-spice port=6999,disable-ticketing=on";
        } else if (MainSettingsManager.getVmUi(context).equals("X11")) {
            params += "-display ";
            params += MainSettingsManager.getUseSdl(context) ? "sdl" : "gtk" + ",gl=on";
            params += " -monitor ";
            params += MainSettingsManager.getRunQemuWithXterm(context) ? "stdio" : "vc";
        }

        return params;
    }

    public static String removeDisplayParams(String params) {
        return params
                .replaceAll("(?<=\\s|^)-object\\s+secret,id=vncpass[^\\s]*", "")
                .replaceAll("(?<=\\s|^)-vnc\\b.*?(?=\\s+-monitor\\s+\\S+|$)", "")
                .replaceAll("(?<=\\s|^)-vnc\\b[^\\s]*", "")
                .replaceAll("(?<=\\s|^)-monitor\\s+\\S+", "")
                .replaceAll("(?<=\\s|^)-display\\b.*?(?=\\s+-monitor\\s+\\S+|$)", "")
                .replaceAll("(?<=\\s|^)-display\\s+\\S+", "")
                .replaceAll("(?<=\\s|^)-spice\\s+\\S+", "")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    public static void extractFirmware(Context context) {
        if (MainSettingsManager.useDefaultBios(context)) {
            String arch = MainSettingsManager.getArch(context);

            FileUtils.createDirectory(AppConfig.basefiledir);

            if (arch.equals("ARM64")) {
                if (!FileUtils.isFileExists(AppConfig.basefiledir + "QEMU_EFI.img"))
                    SetupFeatureCore.copyAssetToFile(context, "roms/QEMU_EFI.img", AppConfig.basefiledir + "QEMU_EFI.img");

                if (!FileUtils.isFileExists(AppConfig.basefiledir + "QEMU_VARS.img"))
                    SetupFeatureCore.copyAssetToFile(context, "roms/QEMU_VARS.img", AppConfig.basefiledir + "QEMU_VARS.img");
            } else if (arch.equals("X86_64") && (MainSettingsManager.getuseUEFI(context) || vmConfigs.isUseUefi)) {
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
}
