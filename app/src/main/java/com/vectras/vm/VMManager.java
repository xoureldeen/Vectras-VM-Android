package com.vectras.vm;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vectras.vm.MainRoms.AdapterMainRoms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

public class VMManager {

    public static HashMap<String, Object> mapForCreateNewVM = new HashMap<>();
    public static ArrayList<HashMap<String, Object>> listmapForCreateNewVM = new ArrayList<>();
    public static ArrayList<HashMap<String, Object>> listmapForRemoveVM = new ArrayList<>();
    public static ArrayList<HashMap<String, Object>> listmapForHideVMID = new ArrayList<>();
    public static String finalJson = "";
    public static String pendingJsonContent = "";
    public static String pendingVMID = "";
    public static int pendingPosition = 0;
    public static int restoredVMs = 0;

    public static void createNewVM(String name, String thumbnail, String drive, String arch, String cdrom, String params, String vmID) {
        mapForCreateNewVM.clear();
        mapForCreateNewVM.put("imgName", name);
        mapForCreateNewVM.put("imgIcon", thumbnail);
        mapForCreateNewVM.put("imgPath", drive);
        mapForCreateNewVM.put("imgCdrom", cdrom);
        mapForCreateNewVM.put("imgExtra", params);
        mapForCreateNewVM.put("imgArch", arch);
        mapForCreateNewVM.put("vmID", vmID);

        listmapForCreateNewVM.clear();
        listmapForCreateNewVM = new Gson().fromJson(VectrasApp.readFile(AppConfig.romsdatajson), new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());

        listmapForCreateNewVM.add(0,mapForCreateNewVM);
        finalJson = new Gson().toJson(listmapForCreateNewVM);

        VectrasApp.writeToFile(AppConfig.maindirpath, "roms-data.json", finalJson);
        finalJson = new Gson().toJson(mapForCreateNewVM);
        VectrasApp.writeToFile(AppConfig.maindirpath + "/roms/" + Objects.requireNonNull(mapForCreateNewVM.get("vmID")).toString(), "rom-data.json", finalJson);
        VectrasApp.writeToFile(AppConfig.maindirpath + "/roms/" + Objects.requireNonNull(mapForCreateNewVM.get("vmID")).toString(), "vmID.txt", Objects.requireNonNull(mapForCreateNewVM.get("vmID")).toString());
    }

    public static void editVM(String name, String thumbnail, String drive, String arch, String cdrom, String params, int position) {
        listmapForCreateNewVM.clear();
        listmapForCreateNewVM = new Gson().fromJson(VectrasApp.readFile(AppConfig.romsdatajson), new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());

        mapForCreateNewVM.clear();
        mapForCreateNewVM.put("imgName", name);
        mapForCreateNewVM.put("imgIcon", thumbnail);
        mapForCreateNewVM.put("imgPath", drive);
        mapForCreateNewVM.put("imgCdrom", cdrom);
        mapForCreateNewVM.put("imgExtra", params);
        mapForCreateNewVM.put("imgArch", arch);

        if (listmapForCreateNewVM.get(position).containsKey("vmID")) {
            mapForCreateNewVM.put("vmID", Objects.requireNonNull(listmapForCreateNewVM.get(position).get("vmID")).toString());
        } else {
            mapForCreateNewVM.put("vmID", idGenerator());
        }

        listmapForCreateNewVM.set(position,mapForCreateNewVM);
        finalJson = new Gson().toJson(listmapForCreateNewVM);
        VectrasApp.writeToFile(AppConfig.maindirpath, "roms-data.json", finalJson);
        finalJson = new Gson().toJson(mapForCreateNewVM);
        VectrasApp.writeToFile(AppConfig.maindirpath + "/roms/" + Objects.requireNonNull(mapForCreateNewVM.get("vmID")).toString(), "rom-data.json", finalJson);
        VectrasApp.writeToFile(AppConfig.maindirpath + "/roms/" + Objects.requireNonNull(mapForCreateNewVM.get("vmID")).toString(), "vmID.txt", Objects.requireNonNull(mapForCreateNewVM.get("vmID")).toString());
    }

    public static String idGenerator() {
        String _result = startRamdomVMID();

        if (VectrasApp.isFileExists(AppConfig.maindirpath + "/roms/" + _result)) {
            _result = startRamdomVMID();
        }

        if (VectrasApp.isFileExists(AppConfig.maindirpath + "/roms/" + _result)) {
            _result = startRamdomVMID();
        }

        return _result;
    }

    public static String startRamdomVMID() {
        String addAdb = "";
        Random random = new Random();
        int randomAbc = random.nextInt(12);
        if (randomAbc == 0) {
            addAdb = "a";
        } else if (randomAbc == 1) {
            addAdb = "b";
        } else if (randomAbc == 2) {
            addAdb = "c";
        } else if (randomAbc == 3) {
            addAdb = "d";
        } else if (randomAbc == 4) {
            addAdb = "e";
        } else if (randomAbc == 5) {
            addAdb = "f";
        } else if (randomAbc == 6) {
            addAdb = "g";
        } else if (randomAbc == 7) {
            addAdb = "h";
        } else if (randomAbc == 8) {
            addAdb = "i";
        } else if (randomAbc == 9) {
            addAdb = "j";
        } else if (randomAbc == 10) {
            addAdb = "k";
        } else if (randomAbc == 11) {
            addAdb = "l";
        }
        return addAdb + String.valueOf((long)(random.nextInt(10001)));
    }

    public static void deleteVM() {
        listmapForRemoveVM.clear();
        listmapForRemoveVM = new Gson().fromJson(pendingJsonContent, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
        if (listmapForRemoveVM.get(pendingPosition).containsKey("vmID")) {
            pendingVMID = Objects.requireNonNull(listmapForRemoveVM.get(pendingPosition).get("vmID")).toString();
            Log.i("VMManager", "deleteVM: ID obtained: " + pendingVMID);
        } else {
            Log.e("VMManager", "deleteVM: Cannot get ID.");
            return;
        }
        listmapForRemoveVM.remove(pendingPosition);
        finalJson = new Gson().toJson(listmapForRemoveVM);
        if (!pendingVMID.isEmpty()) {
            int _startRepeat = 0;
            String _currentVMIDToScan = "";
            ArrayList<String> _filelist = new ArrayList<>();
            VectrasApp.listDir(AppConfig.vmFolder, _filelist);
            if (!_filelist.isEmpty()) {
                for (int _repeat = 0; _repeat < (int)(_filelist.size()); _repeat++) {
                    if (_startRepeat < _filelist.size()) {
                        if (VectrasApp.isFileExists(_filelist.get((int)(_startRepeat)) + "/vmID.txt")) {
                            _currentVMIDToScan = VectrasApp.readFile(_filelist.get((int)(_startRepeat)) + "/vmID.txt").replace("\n", "");
                            if (!_currentVMIDToScan.isEmpty()) {
                                if (_currentVMIDToScan.equals(pendingVMID)) {
                                    if (!finalJson.contains(_filelist.get((int)(_startRepeat)))) {
                                        VectrasApp.deleteDirectory(_filelist.get((int)(_startRepeat)));
                                    } else {
                                        AdapterMainRoms.isKeptSomeFiles = true;
                                        hideVMID(pendingVMID);
                                    }
                                }
                            }
                        }
                    }
                    _startRepeat++;
                }
            }
        }
    }

    public static void hideVMID(String _vmID) {
        if (!_vmID.isEmpty()) {
            int _startRepeat = 0;
            String _currentVMIDToScan = "";
            ArrayList<String> _filelist = new ArrayList<>();
            VectrasApp.listDir(AppConfig.vmFolder, _filelist);
            if (!_filelist.isEmpty()) {
                for (int _repeat = 0; _repeat < (int)(_filelist.size()); _repeat++) {
                    if (_startRepeat < _filelist.size()) {
                        if (VectrasApp.isFileExists(_filelist.get((int)(_startRepeat)) + "/vmID.txt")) {
                            _currentVMIDToScan = VectrasApp.readFile(_filelist.get((int)(_startRepeat)) + "/vmID.txt").replace("\n", "");
                            if (!_currentVMIDToScan.isEmpty()) {
                                if (_currentVMIDToScan.equals(_vmID)) {
                                    VectrasApp.moveAFile(_filelist.get((int)(_startRepeat)) + "/vmID.txt", _filelist.get((int)(_startRepeat)) + "/vmID.old.txt");
                                }
                            }
                        }
                    }
                    _startRepeat++;
                }
            }
        }
    }

    public static void hideVMIDWithPosition() {
        listmapForHideVMID.clear();
        listmapForHideVMID = new Gson().fromJson(pendingJsonContent, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
        if (listmapForHideVMID.get(pendingPosition).containsKey("vmID")) {
            pendingVMID = Objects.requireNonNull(listmapForHideVMID.get(pendingPosition).get("vmID")).toString();
        } else {
            return;
        }
        if (!pendingVMID.isEmpty()) {
            int _startRepeat = 0;
            String _currentVMIDToScan = "";
            ArrayList<String> _filelist = new ArrayList<>();
            VectrasApp.listDir(AppConfig.vmFolder, _filelist);
            if (!_filelist.isEmpty()) {
                for (int _repeat = 0; _repeat < (int)(_filelist.size()); _repeat++) {
                    if (_startRepeat < _filelist.size()) {
                        if (VectrasApp.isFileExists(_filelist.get((int)(_startRepeat)) + "/vmID.txt")) {
                            _currentVMIDToScan = VectrasApp.readFile(_filelist.get((int)(_startRepeat)) + "/vmID.txt").replace("\n", "");
                            if (!_currentVMIDToScan.isEmpty()) {
                                if (_currentVMIDToScan.equals(pendingVMID)) {
                                    VectrasApp.moveAFile(_filelist.get((int)(_startRepeat)) + "/vmID.txt", _filelist.get((int)(_startRepeat)) + "/vmID.old.txt");
                                }
                            }
                        }
                    }
                    _startRepeat++;
                }
            }
        }
    }

    public static void cleanUp() {
        finalJson = VectrasApp.readFile(AppConfig.romsdatajson);
        if (!finalJson.isEmpty()) {
            int _startRepeat = 0;
            ArrayList<String> _filelist = new ArrayList<>();
            VectrasApp.listDir(AppConfig.vmFolder, _filelist);
            if (!_filelist.isEmpty()) {
                for (int _repeat = 0; _repeat < (int)(_filelist.size()); _repeat++) {
                    if (_startRepeat < _filelist.size()) {
                        if (!VectrasApp.isFileExists(_filelist.get((int)(_startRepeat)) + "/vmID.txt")) {
                            if (!finalJson.contains(_filelist.get((int) (_startRepeat)))) {
                                VectrasApp.deleteDirectory(_filelist.get((int) (_startRepeat)));
                            }
                        }
                    }
                    _startRepeat++;
                }
            }
        }
    }

    public static void restoreVMs() {
        int _startRepeat = 0;
        String _resulttemp ="";
        String _result ="";
        restoredVMs = 0;
        ArrayList<String> _filelist = new ArrayList<>();
        VectrasApp.listDir(AppConfig.vmFolder, _filelist);
        if (!_filelist.isEmpty()) {
            for (int _repeat = 0; _repeat < (int)(_filelist.size()); _repeat++) {
                if (_startRepeat < _filelist.size()) {
                    if (!VectrasApp.isFileExists(_filelist.get((int)(_startRepeat)) + "/vmID.txt")) {
                        if (VectrasApp.isFileExists(_filelist.get((int)(_startRepeat)) + "/rom-data.json")) {
                            if (VectrasApp.checkJSONMapIsNormalFromString(VectrasApp.readFile(_filelist.get((int)(_startRepeat)) + "/rom-data.json"))) {
                                if (_resulttemp.contains("}")) {
                                    _resulttemp += "," + VectrasApp.readFile(_filelist.get((int)(_startRepeat)) + "/rom-data.json");
                                } else {
                                    _resulttemp = VectrasApp.readFile(_filelist.get((int)(_startRepeat)) + "/rom-data.json");
                                }
                                if (VectrasApp.checkJSONIsNormalFromString(VectrasApp.readFile(AppConfig.maindirpath + "/roms-data.json").replaceAll("]", _resulttemp + "]"))) {
                                    if (_result.contains("}")) {
                                        _result += "," + VectrasApp.readFile(_filelist.get((int)(_startRepeat)) + "/rom-data.json");
                                    } else {
                                        _result = VectrasApp.readFile(_filelist.get((int)(_startRepeat)) + "/rom-data.json");
                                    }
                                    if (VectrasApp.isFileExists(_filelist.get((int)(_startRepeat)) + "/vmID.old.txt")) {
                                        enableVMID(VectrasApp.readFile(_filelist.get((int)(_startRepeat)) + "/vmID.old.txt"));
                                    } else {
                                        VectrasApp.writeToFile(_filelist.get((int)(_startRepeat)), "/vmID.txt", VMManager.idGenerator());
                                    }
                                    restoredVMs++;
                                } else if (VectrasApp.checkJSONIsNormalFromString(VectrasApp.readFile(AppConfig.maindirpath + "/roms-data.json").replaceAll("]", "," + _resulttemp + "]"))) {
                                    if (_result.contains("}")) {
                                        _result += "," + VectrasApp.readFile(_filelist.get((int)(_startRepeat)) + "/rom-data.json");
                                    } else {
                                        _result = "," + VectrasApp.readFile(_filelist.get((int)(_startRepeat)) + "/rom-data.json");
                                    }
                                    if (VectrasApp.isFileExists(_filelist.get((int)(_startRepeat)) + "/vmID.old.txt")) {
                                        enableVMID(VectrasApp.readFile(_filelist.get((int)(_startRepeat)) + "/vmID.old.txt"));
                                    } else {
                                        VectrasApp.writeToFile(_filelist.get((int)(_startRepeat)), "/vmID.txt", VMManager.idGenerator());
                                    }
                                    restoredVMs++;
                                } else {
                                    Log.i("CqcmActivity", VectrasApp.readFile(AppConfig.maindirpath + "/roms-data.json").replaceAll("]", _resulttemp + "]"));
                                }
                            }
                        }
                    }

                    _startRepeat++;
                    if (_startRepeat == _filelist.size()) {
                        if (!_result.isEmpty()) {
                            if (VectrasApp.checkJSONIsNormalFromString("[" + _result + "]")) {
                                if (VectrasApp.isFileExists(AppConfig.romsdatajson)) {
                                    if (VectrasApp.checkJSONIsNormal(AppConfig.romsdatajson)) {
                                        String _JSONcontent = VectrasApp.readFile(AppConfig.romsdatajson);
                                        String _JSONcontentnew = _JSONcontent.replaceAll("]", _result + "]");
                                        if (VectrasApp.checkJSONIsNormalFromString(_JSONcontentnew)) {
                                            VectrasApp.writeToFile(AppConfig.maindirpath, "roms-data.json", _JSONcontentnew);
                                        } else {
                                            restoredVMs = 0;
                                        }
                                    } else {
                                        restoredVMs = 0;
                                    }
                                } else {
                                    VectrasApp.writeToFile(AppConfig.maindirpath, "roms-data.json", "[" + _result + "]");
                                }
                            } else {
                                restoredVMs = 0;
                            }
                        } else {
                            restoredVMs = 0;
                        }
                    }
                }
            }

        }
    }

    public static void enableVMID(String _vmID) {
        if (_vmID.isEmpty())
            return;
        int _startRepeat = 0;
        ArrayList<String> _filelist = new ArrayList<>();
        VectrasApp.listDir(AppConfig.vmFolder, _filelist);
        if (!_filelist.isEmpty()) {
            for (int _repeat = 0; _repeat < (int)(_filelist.size()); _repeat++) {
                if (_startRepeat < _filelist.size()) {
                    if (VectrasApp.isFileExists(_filelist.get((int)(_startRepeat)) + "/vmID.old.txt")) {
                        if (VectrasApp.readFile(_filelist.get((int)(_startRepeat)) + "/vmID.old.txt").equals(_vmID)) {
                            VectrasApp.moveAFile(_filelist.get((int)(_startRepeat)) + "/vmID.old.txt", _filelist.get((int)(_startRepeat)) + "/vmID.txt");
                        }
                    }
                }
                _startRepeat++;
            }
        }
    }
}
