package com.vectras.vm;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.vectras.qemu.Config;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.utils.RamInfo;
import com.vectras.vm.creator.VMCreatorSelector;
import com.vectras.vm.main.vms.DataMainRoms;
import com.vectras.vm.manager.BatteryEmulatorManager;
import com.vectras.vm.manager.FirmwareManager;
import com.vectras.vm.manager.ParamManager;
import com.vectras.vm.manager.QemuManager;
import com.vectras.vm.manager.VmFileManager;
import com.vectras.vm.settings.ItemSettingsSelector;
import com.vectras.vm.setupwizard.SetupFeatureCore;
import com.vectras.vm.utils.CpuHelper;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class StartVM {
    public static String cache;
    private static DataMainRoms vmConfigs;

    public static String env(Activity activity, DataMainRoms vmData) {
        if (VMManager.isVMRunning(activity, vmData.vmID)) return "";

        vmConfigs = vmData;

        if (VMManager.isNeedLoadMigrate() && FileUtils.isFileExists(VmFileManager.getSnapshotSh(Config.vmID))) {
            String snapshotParams = FileUtils.readAFile(VmFileManager.getSnapshotSh(Config.vmID)).replace("\n", "");
            if (VMManager.isthiscommandsafe(snapshotParams, activity)) {
                snapshotParams = removeQemuSystem(snapshotParams);
                snapshotParams = removeQmpParams(snapshotParams);
                snapshotParams = removeDisplayParams(snapshotParams);
                snapshotParams = getQmpParams() + " " + snapshotParams;
                snapshotParams = QemuManager.getQemuExecutableFile(activity) + " " + snapshotParams;
                snapshotParams += " " + getDisplayParams(activity, vmConfigs.itemExtra);
                if (!snapshotParams.contains("-incoming defer"))
                    snapshotParams += " -incoming defer";
                Log.d("StartVM.env", snapshotParams);
                return snapshotParams;
            }
        }

        String extraParams = vmData.itemExtra;

        String cpuParams = "";

        String cpu = Objects.requireNonNull(VMCreatorSelector.getCpu(activity, MainSettingsManager.getArch(activity), vmData.cpu).get("value")).toString();
        if (!cpu.isEmpty() && !extraParams.contains("-cpu ")) {
            cpuParams = " -cpu " + cpu;
        }

        CpuHelper cpuHelper = new CpuHelper();

        int cores = Integer.parseInt(Objects.requireNonNull(VMCreatorSelector.getCpuCore(MainSettingsManager.getArch(activity), vmConfigs.cores).get("value")).toString());
        int threads = Math.max(1, vmConfigs.threads + 1);
        if (!extraParams.contains("-smp ")) {
            if (cores * threads > cpuHelper.getCpuThreads()) {
                cpuParams += " -smp sockets=1,cores=" + cpuHelper.getCpuCores() + ",threads=1";
            } else {
                cpuParams += " -smp sockets=1,cores=" + cores + ",threads=" + threads;
            }
        }

        if (!cpuParams.isEmpty()) extraParams = cpuParams + " " + extraParams;

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
            ifType = MainSettingsManager.getIfType(activity);

            String cdrom = "";
            String hdd0;
            String hdd1;

            if (!img.isEmpty()) {
                if (ifType.isEmpty()) {
                    hdd0 = "-hda";
                    hdd0 += " '" + img + "'";
                } else {
                    hdd0 = "-drive";
                    hdd0 += " media=disk";
                    hdd0 += ",if=" + ifType;
                    hdd0 += ",file='" + img + "'";

                    if ((MainSettingsManager.getArch(activity).equals("ARM64") && ifType.equals("ide")) || MainSettingsManager.getArch(activity).equals("PPC")) {
                        hdd0 = "-drive";
                        hdd0 += " media=disk";
                        hdd0 += ",file='" + img + "'";
                    }
                }
                params.add(hdd0);
            }

            if (!vmConfigs.hd1.isEmpty()) {
                if (ifType.isEmpty()) {
                    hdd0 = "-hdb";
                    hdd0 += " '" + vmConfigs.hd1 + "'";
                } else {
                    hdd0 = "-drive";
                    hdd0 += " media=disk";
                    hdd0 += ",if=" + ifType;
                    hdd0 += ",file='" + vmConfigs.hd1 + "'";

                    if ((MainSettingsManager.getArch(activity).equals("ARM64") && ifType.equals("ide")) || MainSettingsManager.getArch(activity).equals("PPC")) {
                        hdd0 = "-drive";
                        hdd0 += " media=disk";
                        hdd0 += ",file='" + vmConfigs.hd1 + "'";
                    }
                }
                params.add(hdd0);
            }

            if (!vmConfigs.imgCdrom.isEmpty()) {
                if (MainSettingsManager.getArch(activity).equals("ARM64")) {
                    cdrom += " -device";
                    cdrom += " nec-usb-xhci,id=usbopticaldiscreader0";
                    cdrom += " -device";
                    cdrom += " usb-storage,bus=usbopticaldiscreader0.0,drive=cdromdrive0";
                    cdrom += " -drive";
                    cdrom += " if=none,id=cdromdrive0,format=raw,media=cdrom,file='" + vmConfigs.imgCdrom + "'";
                } else {
                    if (!extras.contains("-cdrom ")) {
                        cdrom = "-cdrom";
                        cdrom += " '" + vmConfigs.imgCdrom + "'";
                    } else {
                        cdrom = "-drive";
                        cdrom += " media=cdromdrive1";
                        cdrom += ",file='" + vmConfigs.imgCdrom + "'";
                    }
                }
                params.add(cdrom);
            }

            if (!vmConfigs.cdrom1.isEmpty()) {
                String cdromParams;
                String controllerId = vmConfigs.imgCdrom.isEmpty() ? "usbopticaldiscreader0" : "usbopticaldiscreader1";

                if (MainSettingsManager.getArch(activity).equals("ARM64")) {
                    cdromParams = " -device";
                    cdromParams += " nec-usb-xhci,id=" + controllerId;
                    cdromParams += " -device";
                    cdromParams += " usb-storage,bus=" + controllerId + ".0,drive=cdromdrive1";
                    cdromParams += " -drive";
                    cdromParams += " if=none,id=cdromdrive1,format=raw,media=cdrom,file='" + vmConfigs.cdrom1 + "'";
                } else {
                    if (vmConfigs.imgCdrom.isEmpty() && !extras.contains("-cdrom ")) {
                        cdromParams = "-cdrom";
                        cdromParams += " '" + vmConfigs.cdrom1 + "'";
                    } else {
                        cdromParams = "-drive";
                        cdromParams += " media=cdromdrive1";
                        cdromParams += ",file='" + vmConfigs.cdrom1 + "'";
                    }
                }
                params.add(cdromParams);
            }

            File hdd1File = new File(filesDir + "/data/Vectras/hdd1.qcow2");

            if (hdd1File.exists()) {
                if (ifType.isEmpty()) {
                    hdd1 = "-hdb";
                    hdd1 += " '" + hdd1File.getPath() + "'";
                } else {
                    hdd1 = "-drive";
                    hdd1 += " media=disk";
                    hdd1 += ",if=" + ifType;
                    hdd1 += ",file='" + hdd1File.getPath() + "'";
                }

                params.add(hdd1);
            }

            if (!vmConfigs.fda.isEmpty() && !MainSettingsManager.getArch(activity).equals("ARM64")) {
                if (extras.contains("-fda ")) {
                    params.add("-drive if=floppy,file='" + vmConfigs.fda + "'");
                } else {
                    params.add("-fda");
                    params.add("'" + vmConfigs.fda + "'");
                }
            }

            if (!vmConfigs.fdb.isEmpty() && !MainSettingsManager.getArch(activity).equals("ARM64")) {
                if (vmConfigs.fda.isEmpty()) {
                    if (extras.contains("-fda ")) {
                        params.add("-drive if=floppy,file='" + vmConfigs.fdb + "'");
                    } else {
                        params.add("-fda");
                        params.add("'" + vmConfigs.fdb + "'");
                    }
                } else {
                    if (extras.contains("-fdb ")) {
                        params.add("-drive if=floppy,file='" + vmConfigs.fdb + "'");
                    } else {
                        params.add("-fdb");
                        params.add("'" + vmConfigs.fdb + "'");
                    }
                }
            }

            if (vmConfigs.sharedFolder) {
                String driveParams = "-drive ";
                driveParams += "file=fat:rw:'"; //Disk Drives are always Read/Write
                driveParams += AppConfig.sharedFolder + "',format=raw";
                params.add(driveParams);
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

            if (vmConfigs.isUseDefaultBios) {
                if (MainSettingsManager.getArch(activity).equals("PPC")) {
                    bios = "-L ";
                    bios += "pc-bios";
                } else if (MainSettingsManager.getArch(activity).equals("ARM64")) {
                    bios = "-drive ";
                    bios += "file=" + AppConfig.basefiledir + "QEMU_EFI.img,format=raw,readonly=on,if=pflash";
                    bios += " -drive ";
                    bios += "file=" + AppConfig.basefiledir + "QEMU_VARS.img,format=raw,if=pflash";
                } else if (vmConfigs.isUseUefi) {
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

            if (!ParamManager.hasMemory(extras)) {
                String memoryStr = "-m ";
                if (MainSettingsManager.getArch(activity).equals("PPC") && RamInfo.vectrasMemory(activity) > 2048) {
                    memoryStr += 2048;
                } else {
                    memoryStr += RamInfo.vectrasMemory(activity);
                }
                params.add(memoryStr);
            }

            if (ifType.isEmpty()) {
                if (extras.contains("-drive media=cdrom,file=")) {
                    finalextra = extras.replace("-drive media=cdrom,file=", "-cdrom ");
                }
            } else {
                if (extras.contains("-cdrom ")) {
                    finalextra = extras.replace("-cdrom ", "-drive media=cdrom,file=");
                }
            }
        }

        params.add(finalextra);

        if (isQuickRun) {
            params.add(getQmpParams());
        }

        params.add(getDisplayParams(activity, extras));

        if (VMManager.isNeedLoadMigrate()) {
            params.add("-incoming");
            params.add("defer");
        }

        if (vmConfigs.battery) {
            if (BatteryEmulatorManager.setup(activity, vmConfigs.vmID))
                params.add("-acpitable file=" + VmFileManager.getCompiledBatteryAcpi(activity, vmConfigs.vmID));
        }

        return String.join(" ", params);
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


    public static String getDisplayParams(Context context, String mainParams) {
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
                params += "unix:" + Config.getLocalVNCSocketPath() + (MainSettingsManager.getVncLosslessQuality(context) ? ",lossy=off" : "");

                if (QemuManager.isSupportSetRefreshRate(context))
                    params += ",refresh-rate=" + ((context instanceof Activity activity) ? QemuManager.getAppropriateRefreshRate(activity, mainParams, DeviceUtils.getMaxRefreshRate(activity)) : Integer.parseInt(ItemSettingsSelector.getVncRefreshRateValue(MainSettingsManager.getVncRefreshRate(context))));
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
        FirmwareManager.extract(context, vmConfigs.isUseUefi);
    }
}
