package com.vectras.vm.manager;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vectras.qemu.Config;
import com.vectras.vm.AppConfig;
import com.vectras.vm.main.vms.DataMainRoms;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.UniversalPickerDialog;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class VmListManager {
    public static ArrayList<HashMap<String, Object>> getAllVmForPickRunningNoVncSocketOnly(Context context) {
        ArrayList<HashMap<String, Object>> listVm = getAllVmForPickRunningOnly(context);
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();

        for (int i = 0; i < listVm.size(); i++) {
            if (!FileUtils.isFileExists(Config.getLocalVNCSocketPath(Objects.requireNonNull(listVm.get(i).get("value")).toString()))) {
                list.add(listVm.get(i));
            }
        }

        return list;
    }

    public static ArrayList<HashMap<String, Object>> getAllVmForPickRunningVncSocketOnly(Context context) {
        ArrayList<HashMap<String, Object>> listVm = getAllVmForPickRunningOnly(context);
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();

        for (int i = 0; i < listVm.size(); i++) {
            if (FileUtils.isFileExists(Config.getLocalVNCSocketPath(Objects.requireNonNull(listVm.get(i).get("value")).toString()))) {
                list.add(listVm.get(i));
            }
        }

        return list;
    }

    public static ArrayList<HashMap<String, Object>> getAllVmForPickRunningOnly(Context context) {
        ArrayList<HashMap<String, Object>> listVm = getAllVmForPick(context);
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();

        for (int i = 0; i < listVm.size(); i++) {
            if (FileUtils.isFileExists(Config.getLocalQMPSocketPath(Objects.requireNonNull(listVm.get(i).get("value")).toString()))) {
                list.add(listVm.get(i));
            }
        }

        return list;
    }

    public static ArrayList<HashMap<String, Object>> getAllVmForPick(Context context) {
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();

        List<DataMainRoms> data = getAllVm(context);

        for (int i = 0; i < data.size(); i++) {
            UniversalPickerDialog.putToList(list, data.get(i).itemName, data.get(i).vmID);
        }

        return list;
    }

    public static List<DataMainRoms> getAllVm(Context context) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<DataMainRoms>>(){}.getType();

        String json = FileUtils.readFromFile(context, new File(AppConfig.romsdatajson));

        return gson.fromJson(json, listType);
    }
}
