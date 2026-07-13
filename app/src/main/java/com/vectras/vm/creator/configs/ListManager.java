package com.vectras.vm.creator.configs;

import android.content.Context;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.R;
import com.vectras.vm.utils.CpuHelper;
import com.vectras.vm.utils.UniversalPickerDialog;

import java.util.ArrayList;
import java.util.HashMap;

public class ListManager {
    public static ArrayList<HashMap<String, Object>> bootFrom(Context context) {
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        UniversalPickerDialog.putToList(list, context.getString(R.string.defaulttext), "");
        UniversalPickerDialog.putToList(list, context.getString(R.string.hard_disk), "c");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cdrom), "d");
        UniversalPickerDialog.putToList(list, context.getString(R.string.floppy_disk), "a");
        UniversalPickerDialog.putToList(list, context.getString(R.string.network), "n");
        return list;
    }

    public static ArrayList<HashMap<String, Object>> cpus(Context context, String arch) {
        return switch (arch) {
            case MainSettingsManager.ARM64_ARCH -> cpusArm64(context);
            case MainSettingsManager.PPC_ARCH -> cpusPpc(context);
            default -> cpusX8664(context);
        };
    }

    public static ArrayList<HashMap<String, Object>> cpusX8664(Context context) {
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        UniversalPickerDialog.putToList(list, context.getString(R.string.defaulttext), "");
        UniversalPickerDialog.putToList(list, context.getString(R.string.qemu64), "qemu64");
        UniversalPickerDialog.putToList(list, context.getString(R.string.qemu32), "qemu32");
        UniversalPickerDialog.putToList(list, context.getString(R.string.base_cpu), "base");
        UniversalPickerDialog.putToList(list, context.getString(R.string.max_cpu), "max");
        //UniversalPickerDialog.putToList(list, context.getString(R.string.kvm64), "kvm64");
        //UniversalPickerDialog.putToList(list, context.getString(R.string.kvm32), "kvm32");
        //UniversalPickerDialog.putToList(list, context.getString(R.string.host_cpu), "host");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_snow_ridge), "Snowridge");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_cooper_lake), "Cooperlake");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_cascade_lake_server), "Cascadelake-Server");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_knights_mill), "KnightsMill");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_denverton), "Denverton");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_skylake_server), "Skylake-Server");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_skylake_client), "Skylake-Client");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_broadwell), "Broadwell");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_haswell), "Haswell");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_ivy_bridge), "IvyBridge");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_sandy_bridge), "SandyBridge");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_westmere), "Westmere");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_nehalem), "Nehalem");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_altom_n270), "n270");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_penryn), "Penryn");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_core_2_duo), "core2duo");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_core_duo), "coreduo");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_conroe), "Conroe");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_pentium_iii), "pentium3");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_pentium_ii), "pentium2");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_pentium), "pentium");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_486), "486");
        UniversalPickerDialog.putToList(list, context.getString(R.string.amd_epyc_milan), "EPYC-Milan");
        UniversalPickerDialog.putToList(list, context.getString(R.string.amd_epyc_rome), "EPYC-Rome");
        UniversalPickerDialog.putToList(list, context.getString(R.string.amd_epyc), "EPYC");
        UniversalPickerDialog.putToList(list, context.getString(R.string.amd_opteron_g5), "Opteron_G5");
        UniversalPickerDialog.putToList(list, context.getString(R.string.amd_opteron_g4), "Opteron_G4");
        UniversalPickerDialog.putToList(list, context.getString(R.string.amd_phenom), "phenom");
        UniversalPickerDialog.putToList(list, context.getString(R.string.amd_opteron_g3), "Opteron_G3");
        UniversalPickerDialog.putToList(list, context.getString(R.string.amd_opteron_g2), "Opteron_G2");
        UniversalPickerDialog.putToList(list, context.getString(R.string.amd_opteron_g1), "Opteron_G1");
        UniversalPickerDialog.putToList(list, context.getString(R.string.amd_athlon), "athlon");
        UniversalPickerDialog.putToList(list, context.getString(R.string.hygon_dhyana), "Dhyana");
        return list;
    }

    public static ArrayList<HashMap<String, Object>> cpusArm64(Context context) {
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        UniversalPickerDialog.putToList(list, context.getString(R.string.defaulttext), "max");
        UniversalPickerDialog.putToList(list, context.getString(R.string.max_cpu), "max");
        //UniversalPickerDialog.putToList(list, context.getString(R.string.host_cpu), "host");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cortex_a76), "cortex-a76");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cortex_a72), "cortex-a72");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cortex_a57), "cortex-a57");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cortex_a55), "cortex-a55");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cortex_a53), "cortex-a53");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cortex_a35), "cortex-a35");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cortex_a15), "cortex-a15");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cortex_a9), "cortex-a9");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cortex_a8), "cortex-a8");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cortex_a7), "cortex-a7");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cortex_m55), "cortex-m55");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cortex_m33), "cortex-m33");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cortex_m7), "cortex-m7");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cortex_m4), "cortex-m4");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cortex_m3), "cortex-m3");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cortex_m0), "cortex-m0");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cortex_r52), "cortex-r52");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cortex_r5f), "cortex-r5f");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cortex_r5), "cortex-r5");
        return list;
    }

    public static ArrayList<HashMap<String, Object>> cpusPpc(Context context) {
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        UniversalPickerDialog.putToList(list, context.getString(R.string.defaulttext), "g3");
        UniversalPickerDialog.putToList(list, context.getString(R.string.cpu_460ex), "460ex");
        UniversalPickerDialog.putToList(list, context.getString(R.string.e500v2_cpu), "e500v2");
        UniversalPickerDialog.putToList(list, context.getString(R.string.g4_cpu_turbo), "7457");
        UniversalPickerDialog.putToList(list, context.getString(R.string.g4_cpu_basic), "g4");
        UniversalPickerDialog.putToList(list, context.getString(R.string.g3_cpu), "g3");
        UniversalPickerDialog.putToList(list, context.getString(R.string.g2_cpu_workstation), "604e");
        UniversalPickerDialog.putToList(list, context.getString(R.string.g2_cpu_low_power), "603e");
        return list;
    }

    public static ArrayList<HashMap<String, Object>> cores(String arch) {
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();

        if (arch.equals(MainSettingsManager.PPC_ARCH)) {
            UniversalPickerDialog.putToList(list, "1", "1");
            UniversalPickerDialog.putToList(list, "2", "2");
        } else {
            CpuHelper cpuHelper = new CpuHelper();
            int cores = cpuHelper.getCpuCores();
            int addedCore = 1;
            while (addedCore < cores + 1 && addedCore <= 8) {
                if (addedCore == 1 || addedCore % 2 == 0)
                    UniversalPickerDialog.putToList(list, String.valueOf(addedCore), String.valueOf(addedCore));
                addedCore++;
            }
        }

        return list;
    }

    public static ArrayList<HashMap<String, Object>> threads(String arch) {
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        UniversalPickerDialog.putToList(list, "1", "1");

        if (!(arch.equals(MainSettingsManager.ARM64_ARCH)
                || arch.equals(MainSettingsManager.PPC_ARCH)))
            UniversalPickerDialog.putToList(list, "2", "2");

        return list;
    }

    public static ArrayList<HashMap<String, Object>> networkCards(Context context) {
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        UniversalPickerDialog.putToList(list, context.getString(R.string.none), "");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_e1000e_82574l), "e1000e");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_igb_82576), "igb");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_e1000_82540em), "e1000");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_e1000_82545em), "e1000-82545em");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_e1000_82544gc), "e1000-82544gc");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_i82801), "i82801");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_i82562), "i82562");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_i82559er), "i82559er");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_i82558b), "i82558b");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_i82557c), "i82557c");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_i82551), "i82551");
        UniversalPickerDialog.putToList(list, context.getString(R.string.intel_i82550), "i82550");
        UniversalPickerDialog.putToList(list, context.getString(R.string.realtek_rtl8139), "rtl8139");
        UniversalPickerDialog.putToList(list, context.getString(R.string.amd_pcnet), "pcnet");
        UniversalPickerDialog.putToList(list, context.getString(R.string.novell_ne2000), "ne2k_pci");
        UniversalPickerDialog.putToList(list, context.getString(R.string.vmware_vmxnet3), "vmxnet3");
        UniversalPickerDialog.putToList(list, context.getString(R.string.virtio_net), "virtio-net-pci");
        UniversalPickerDialog.putToList(list, context.getString(R.string.virtio_net_transitional), "virtio-net-pci-transitional");
        return list;
    }
}
