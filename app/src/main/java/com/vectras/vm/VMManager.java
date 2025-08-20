package com.vectras.vm;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;
import static com.vectras.vm.utils.FileUtils.isFileExists;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vectras.qemu.Config;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.utils.QmpClient;
import com.vectras.vm.MainRoms.AdapterMainRoms;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.JSONUtils;
import com.vectras.vm.utils.UIUtils;
import com.vectras.vterm.Terminal;

import org.jetbrains.annotations.Contract;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

public class VMManager {

    public static final String TAG = "VMManager";
    public static HashMap<String, Object> mapForCreateNewVM = new HashMap<>();
    public static ArrayList<HashMap<String, Object>> listmapForCreateNewVM = new ArrayList<>();
    public static ArrayList<HashMap<String, Object>> listmapForRemoveVM = new ArrayList<>();
    public static ArrayList<HashMap<String, Object>> listmapForHideVMID = new ArrayList<>();
    public static String finalJson = "";
    public static String pendingJsonContent = "";
    public static String pendingVMID = "";
    public static int pendingPosition = 0;
    public static String pendingDeviceID = "";
    public static int restoredVMs = 0;
    public static boolean isKeptSomeFiles = false;

    public static String latestUnsafeCommandReason = "";
    public static String lastQemuCommand = "";

    public static void createNewVM(String name, String thumbnail, String drive, String arch, String cdrom, String params, String vmID, int port) {
        mapForCreateNewVM.clear();
        mapForCreateNewVM.put("imgName", name);
        mapForCreateNewVM.put("imgIcon", thumbnail);
        mapForCreateNewVM.put("imgPath", drive);
        mapForCreateNewVM.put("imgCdrom", cdrom);
        mapForCreateNewVM.put("imgExtra", params);
        mapForCreateNewVM.put("imgArch", arch);
        mapForCreateNewVM.put("vmID", vmID);
        mapForCreateNewVM.put("qmpPort", port);

        listmapForCreateNewVM.clear();
        listmapForCreateNewVM = new Gson().fromJson(FileUtils.readAFile(AppConfig.romsdatajson), new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());

        listmapForCreateNewVM.add(0,mapForCreateNewVM);
        finalJson = new Gson().toJson(listmapForCreateNewVM);

        FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", finalJson);
        finalJson = new Gson().toJson(mapForCreateNewVM);
        FileUtils.writeToFile(AppConfig.maindirpath + "/roms/" + Objects.requireNonNull(mapForCreateNewVM.get("vmID")).toString(), "rom-data.json", finalJson.replace("\\u003d", "="));
        FileUtils.writeToFile(AppConfig.maindirpath + "/roms/" + Objects.requireNonNull(mapForCreateNewVM.get("vmID")).toString(), "vmID.txt", Objects.requireNonNull(mapForCreateNewVM.get("vmID")).toString());
    }

    public static void editVM(String name, String thumbnail, String drive, String arch, String cdrom, String params, int position) {
        listmapForCreateNewVM.clear();
        listmapForCreateNewVM = new Gson().fromJson(FileUtils.readAFile(AppConfig.romsdatajson), new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());

        mapForCreateNewVM.clear();
        mapForCreateNewVM.put("imgName", name);
        mapForCreateNewVM.put("imgIcon", thumbnail);
        mapForCreateNewVM.put("imgPath", drive);
        mapForCreateNewVM.put("imgCdrom", cdrom);
        mapForCreateNewVM.put("imgExtra", params);
        mapForCreateNewVM.put("imgArch", arch);
        if (listmapForCreateNewVM.get(position).containsKey("qmpPort")) {
            mapForCreateNewVM.put("qmpPort", listmapForCreateNewVM.get(position).get("qmpPort"));
        } else {
            mapForCreateNewVM.put("qmpPort", startRandomPort());
        }

        if (listmapForCreateNewVM.get(position).containsKey("vmID")) {
            mapForCreateNewVM.put("vmID", Objects.requireNonNull(listmapForCreateNewVM.get(position).get("vmID")).toString());
        } else {
            mapForCreateNewVM.put("vmID", idGenerator());
        }

        listmapForCreateNewVM.set(position,mapForCreateNewVM);
        finalJson = new Gson().toJson(listmapForCreateNewVM);
        FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", finalJson);
        finalJson = new Gson().toJson(mapForCreateNewVM);
        FileUtils.writeToFile(AppConfig.maindirpath + "/roms/" + Objects.requireNonNull(mapForCreateNewVM.get("vmID")).toString(), "rom-data.json", finalJson.replace("\\u003d", "="));
        FileUtils.writeToFile(AppConfig.maindirpath + "/roms/" + Objects.requireNonNull(mapForCreateNewVM.get("vmID")).toString(), "vmID.txt", Objects.requireNonNull(mapForCreateNewVM.get("vmID")).toString());
    }

    public static void deleteVMDialog(String _vmName, int _position, Activity _activity) {
        pendingPosition = _position;
        pendingJsonContent = FileUtils.readAFile(AppConfig.maindirpath + "roms-data.json");

        DialogUtils.threeDialog(_activity, _activity.getString(R.string.remove)+ " " + _vmName, _activity.getString(R.string.remove_vm_content), _activity.getString(R.string.remove_and_do_not_keep_files), _activity.getString(R.string.remove_but_keep_files), _activity.getString(R.string.cancel),true, R.drawable.delete_24px, true,
                () -> {
                    isKeptSomeFiles = false;
                    deleteVM();
                    removeInRomsDataJson(_activity, _vmName, _position);
                },
                () -> {
                    hideVMIDWithPosition();
                    removeInRomsDataJson(_activity, _vmName, _position);
                },
                () -> {

                },
                null);
    }

    public static void removeInRomsDataJson(Activity _activity, String _vmName, int _position) {
        MainActivity.mMainAdapter = new AdapterMainRoms(MainActivity.activity, MainActivity.data);
        MainActivity.data.remove(_position);
        MainActivity.mRVMainRoms.setAdapter(MainActivity.mMainAdapter);
        MainActivity.mRVMainRoms.setLayoutManager(new GridLayoutManager(MainActivity.activity, 2));
        MainActivity.jArray.remove(_position);
        try {
            Writer output = null;
            File jsonFile = new File(AppConfig.maindirpath + "roms-data" + ".json");
            output = new BufferedWriter(new FileWriter(jsonFile));
            output.write(MainActivity.jArray.toString());
            output.close();
        } catch (Exception e) {
            UIUtils.toastLong(_activity, e.toString());
        }
        UIUtils.toastLong(_activity, _vmName + _activity.getString(R.string.are_removed_successfully));
        if (!FileUtils.readAFile(AppConfig.maindirpath + "roms-data.json").contains("{")) {
            MainActivity.mdatasize2();
        }
    }

    public static String idGenerator() {
        String _result = startRamdomVMID();

        if (isFileExists(AppConfig.maindirpath + "/roms/" + _result)) {
            _result = startRamdomVMID();
        }

        if (isFileExists(AppConfig.maindirpath + "/roms/" + _result)) {
            _result = startRamdomVMID();
        }

        return _result;
    }

    @NonNull
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
        } else {
            addAdb = "l";
        }
        return addAdb + String.valueOf((long)(random.nextInt(65535)));
    }

    public static int startRandomPort() {
        int _result;
        Random _random = new Random();
        int _min = 10000;
        int _max = 65535;
        _result = _random.nextInt(_max - _min + 1) + _min;

        if (FileUtils.isFileExists(AppConfig.romsdatajson)) {
            if (FileUtils.readAFile(AppConfig.romsdatajson).contains("\"qmpPort\":" + _result)) {
                _result = _random.nextInt(_max - _min + 1) + _min;
            }
            if (FileUtils.readAFile(AppConfig.romsdatajson).contains("\"qmpPort\":" + _result)) {
                _result = _random.nextInt(_max - _min + 1) + _min;
            }
        }

        return _result;
    }

    public static void deleteVM() {
        listmapForRemoveVM.clear();
        listmapForRemoveVM = new Gson().fromJson(pendingJsonContent, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
        if (listmapForRemoveVM.get(pendingPosition).containsKey("vmID")) {
            pendingVMID = Objects.requireNonNull(listmapForRemoveVM.get(pendingPosition).get("vmID")).toString();
            FileUtils.deleteDirectory(Config.getCacheDir()+ "/" + pendingVMID);
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
            FileUtils.getAListOfAllFilesAndFoldersInADirectory(AppConfig.vmFolder, _filelist);
            if (!_filelist.isEmpty()) {
                for (int _repeat = 0; _repeat < (int)(_filelist.size()); _repeat++) {
                    if (_startRepeat < _filelist.size()) {
                        if (isFileExists(_filelist.get((int)(_startRepeat)) + "/vmID.txt")) {
                            _currentVMIDToScan = FileUtils.readAFile(_filelist.get((int)(_startRepeat)) + "/vmID.txt").replace("\n", "");
                            if (!_currentVMIDToScan.isEmpty()) {
                                if (_currentVMIDToScan.equals(pendingVMID)) {
                                    if (!finalJson.contains(_filelist.get((int)(_startRepeat)))) {
                                        FileUtils.deleteDirectory(_filelist.get((int)(_startRepeat)));
                                    } else {
                                        isKeptSomeFiles = true;
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

    public static void hideVMID(@NonNull String _vmID) {
        if (!_vmID.isEmpty()) {
            int _startRepeat = 0;
            String _currentVMIDToScan = "";
            ArrayList<String> _filelist = new ArrayList<>();
            FileUtils.getAListOfAllFilesAndFoldersInADirectory(AppConfig.vmFolder, _filelist);
            if (!_filelist.isEmpty()) {
                for (int _repeat = 0; _repeat < (int)(_filelist.size()); _repeat++) {
                    if (_startRepeat < _filelist.size()) {
                        if (isFileExists(_filelist.get((int)(_startRepeat)) + "/vmID.txt")) {
                            _currentVMIDToScan = FileUtils.readAFile(_filelist.get((int)(_startRepeat)) + "/vmID.txt").replace("\n", "");
                            if (!_currentVMIDToScan.isEmpty()) {
                                if (_currentVMIDToScan.equals(_vmID)) {
                                    FileUtils.moveAFile(_filelist.get((int)(_startRepeat)) + "/vmID.txt", _filelist.get((int)(_startRepeat)) + "/vmID.old.txt");
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
            FileUtils.getAListOfAllFilesAndFoldersInADirectory(AppConfig.vmFolder, _filelist);
            if (!_filelist.isEmpty()) {
                for (int _repeat = 0; _repeat < (int)(_filelist.size()); _repeat++) {
                    if (_startRepeat < _filelist.size()) {
                        if (isFileExists(_filelist.get((int)(_startRepeat)) + "/vmID.txt")) {
                            _currentVMIDToScan = FileUtils.readAFile(_filelist.get((int)(_startRepeat)) + "/vmID.txt").replace("\n", "");
                            if (!_currentVMIDToScan.isEmpty()) {
                                if (_currentVMIDToScan.equals(pendingVMID)) {
                                    FileUtils.moveAFile(_filelist.get((int)(_startRepeat)) + "/vmID.txt", _filelist.get((int)(_startRepeat)) + "/vmID.old.txt");
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
        finalJson = FileUtils.readAFile(AppConfig.romsdatajson);
        if (!finalJson.isEmpty()) {
            int _startRepeat = 0;
            ArrayList<String> _filelist = new ArrayList<>();
            FileUtils.getAListOfAllFilesAndFoldersInADirectory(AppConfig.vmFolder, _filelist);
            if (!_filelist.isEmpty()) {
                for (int _repeat = 0; _repeat < (int)(_filelist.size()); _repeat++) {
                    if (_startRepeat < _filelist.size()) {
                        if (!isFileExists(_filelist.get((int)(_startRepeat)) + "/vmID.txt")) {
                            if (!finalJson.contains(_filelist.get((int) (_startRepeat)))) {
                                FileUtils.deleteDirectory(_filelist.get((int) (_startRepeat)));
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
        StringBuilder _resulttemp = new StringBuilder();
        StringBuilder _result = new StringBuilder();
        restoredVMs = 0;
        ArrayList<String> _filelist = new ArrayList<>();
        FileUtils.getAListOfAllFilesAndFoldersInADirectory(AppConfig.vmFolder, _filelist);
        if (!_filelist.isEmpty()) {
            for (int _repeat = 0; _repeat < (int)(_filelist.size()); _repeat++) {
                if (_startRepeat < _filelist.size()) {
                    if (!isFileExists(_filelist.get((int)(_startRepeat)) + "/vmID.txt")) {
                        if (isFileExists(_filelist.get((int)(_startRepeat)) + "/rom-data.json")) {
                            if (JSONUtils.isMapValidFromString(FileUtils.readAFile(_filelist.get((int)(_startRepeat)) + "/rom-data.json"))) {
                                if (_resulttemp.toString().contains("}")) {
                                    _resulttemp.append(",").append(FileUtils.readAFile(_filelist.get((int) (_startRepeat)) + "/rom-data.json"));
                                } else {
                                    _resulttemp = new StringBuilder(FileUtils.readAFile(_filelist.get((int) (_startRepeat)) + "/rom-data.json"));
                                }
                                if (JSONUtils.isValidFromString(FileUtils.readAFile(AppConfig.maindirpath + "/roms-data.json").replaceAll("]", _resulttemp + "]"))) {
                                    if (_result.toString().contains("}")) {
                                        _result.append(",").append(FileUtils.readAFile(_filelist.get((int) (_startRepeat)) + "/rom-data.json"));
                                    } else {
                                        _result = new StringBuilder(FileUtils.readAFile(_filelist.get((int) (_startRepeat)) + "/rom-data.json"));
                                    }
                                    if (isFileExists(_filelist.get((int)(_startRepeat)) + "/vmID.old.txt")) {
                                        enableVMID(FileUtils.readAFile(_filelist.get((int)(_startRepeat)) + "/vmID.old.txt"));
                                    } else {
                                        FileUtils.writeToFile(_filelist.get((int)(_startRepeat)), "/vmID.txt", VMManager.idGenerator());
                                    }
                                    restoredVMs++;
                                } else if (JSONUtils.isValidFromString(FileUtils.readAFile(AppConfig.maindirpath + "/roms-data.json").replaceAll("]", "," + _resulttemp + "]"))) {
                                    if (_result.toString().contains("}")) {
                                        _result.append(",").append(FileUtils.readAFile(_filelist.get((int) (_startRepeat)) + "/rom-data.json"));
                                    } else {
                                        _result = new StringBuilder("," + FileUtils.readAFile(_filelist.get((int) (_startRepeat)) + "/rom-data.json"));
                                    }
                                    if (isFileExists(_filelist.get((int)(_startRepeat)) + "/vmID.old.txt")) {
                                        enableVMID(FileUtils.readAFile(_filelist.get((int)(_startRepeat)) + "/vmID.old.txt"));
                                    } else {
                                        FileUtils.writeToFile(_filelist.get((int)(_startRepeat)), "/vmID.txt", VMManager.idGenerator());
                                    }
                                    restoredVMs++;
                                } else {
                                    Log.i("CqcmActivity", FileUtils.readAFile(AppConfig.maindirpath + "/roms-data.json").replaceAll("]", _resulttemp + "]"));
                                }
                            }
                        }
                    }

                    _startRepeat++;
                    if (_startRepeat == _filelist.size()) {
                        if (_result.length() > 0) {
                            if (JSONUtils.isValidFromString("[" + _result + "]")) {
                                if (isFileExists(AppConfig.romsdatajson)) {
                                    if (JSONUtils.isValidFromFile(AppConfig.romsdatajson)) {
                                        String _JSONcontent = FileUtils.readAFile(AppConfig.romsdatajson);
                                        String _JSONcontentnew = _JSONcontent.replaceAll("]", _result + "]");
                                        if (JSONUtils.isValidFromString(_JSONcontentnew)) {
                                            FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", _JSONcontentnew);
                                        } else {
                                            restoredVMs = 0;
                                        }
                                    } else {
                                        restoredVMs = 0;
                                    }
                                } else {
                                    FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", "[" + _result + "]");
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

    public static void startFixRomsDataJson() {
        int _startRepeat = 0;
        StringBuilder _resulttemp = new StringBuilder();
        StringBuilder _result = new StringBuilder();
        restoredVMs = 0;
        ArrayList<String> _filelist = new ArrayList<>();
        FileUtils.getAListOfAllFilesAndFoldersInADirectory(AppConfig.vmFolder, _filelist);
        if (!_filelist.isEmpty()) {
            for (int _repeat = 0; _repeat < (int)(_filelist.size()); _repeat++) {
                if (_startRepeat < _filelist.size()) {
                    if (isFileExists(_filelist.get((int)(_startRepeat)) + "/vmID.txt")) {
                        if (isFileExists(_filelist.get((int)(_startRepeat)) + "/rom-data.json")) {
                            if (JSONUtils.isMapValidFromString(FileUtils.readAFile(_filelist.get((int)(_startRepeat)) + "/rom-data.json"))) {
                                if (_resulttemp.toString().contains("}")) {
                                    _resulttemp.append(",").append(FileUtils.readAFile(_filelist.get((int) (_startRepeat)) + "/rom-data.json"));
                                } else {
                                    _resulttemp = new StringBuilder(FileUtils.readAFile(_filelist.get((int) (_startRepeat)) + "/rom-data.json"));
                                }
                                if (JSONUtils.isValidFromString(FileUtils.readAFile(AppConfig.maindirpath + "/roms-data.json").replaceAll("]", _resulttemp + "]"))) {
                                    if (_result.toString().contains("}")) {
                                        _result.append(",").append(FileUtils.readAFile(_filelist.get((int) (_startRepeat)) + "/rom-data.json"));
                                    } else {
                                        _result = new StringBuilder(FileUtils.readAFile(_filelist.get((int) (_startRepeat)) + "/rom-data.json"));
                                    }
                                    restoredVMs++;
                                } else if (JSONUtils.isValidFromString(FileUtils.readAFile(AppConfig.maindirpath + "/roms-data.json").replaceAll("]", "," + _resulttemp + "]"))) {
                                    if (_result.toString().contains("}")) {
                                        _result.append(",").append(FileUtils.readAFile(_filelist.get((int) (_startRepeat)) + "/rom-data.json"));
                                    } else {
                                        _result = new StringBuilder("," + FileUtils.readAFile(_filelist.get((int) (_startRepeat)) + "/rom-data.json"));
                                    }
                                    restoredVMs++;
                                } else {
                                    Log.i("CqcmActivity", FileUtils.readAFile(AppConfig.maindirpath + "/roms-data.json").replaceAll("]", _resulttemp + "]"));
                                }
                            }
                        }
                    }

                    _startRepeat++;
                    if (_startRepeat == _filelist.size()) {
                        if (_result.length() > 0) {
                            if (JSONUtils.isValidFromString("[" + _result + "]")) {
                                if (isFileExists(AppConfig.romsdatajson)) {
                                    if (JSONUtils.isValidFromFile(AppConfig.romsdatajson)) {
                                        String _JSONcontent = FileUtils.readAFile(AppConfig.romsdatajson);
                                        String _JSONcontentnew = _JSONcontent.replaceAll("]", _result + "]");
                                        if (JSONUtils.isValidFromString(_JSONcontentnew)) {
                                            FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", _JSONcontentnew);
                                        } else {
                                            restoredVMs = 0;
                                        }
                                    } else {
                                        restoredVMs = 0;
                                    }
                                } else {
                                    FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", "[" + _result + "]");
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

    public static void enableVMID(@NonNull String _vmID) {
        if (_vmID.isEmpty())
            return;
        int _startRepeat = 0;
        ArrayList<String> _filelist = new ArrayList<>();
        FileUtils.getAListOfAllFilesAndFoldersInADirectory(AppConfig.vmFolder, _filelist);
        if (!_filelist.isEmpty()) {
            for (int _repeat = 0; _repeat < (int)(_filelist.size()); _repeat++) {
                if (_startRepeat < _filelist.size()) {
                    if (isFileExists(_filelist.get((int)(_startRepeat)) + "/vmID.old.txt")) {
                        if (FileUtils.readAFile(_filelist.get((int)(_startRepeat)) + "/vmID.old.txt").equals(_vmID)) {
                            FileUtils.moveAFile(_filelist.get((int)(_startRepeat)) + "/vmID.old.txt", _filelist.get((int)(_startRepeat)) + "/vmID.txt");
                        }
                    }
                }
                _startRepeat++;
            }
        }
    }

    public static void movetoRecycleBin() {
        File vDir = new File(AppConfig.recyclebin);
        if (!vDir.exists()) {
            if (!vDir.mkdirs()) {
                return;
            }
        }
        finalJson = FileUtils.readAFile(AppConfig.romsdatajson);
        if (!finalJson.isEmpty()) {
            int _startRepeat = 0;
            ArrayList<String> _filelist = new ArrayList<>();
            FileUtils.getAListOfAllFilesAndFoldersInADirectory(AppConfig.vmFolder, _filelist);
            if (!_filelist.isEmpty()) {
                for (int _repeat = 0; _repeat < (int)(_filelist.size()); _repeat++) {
                    if (_startRepeat < _filelist.size()) {
                        if (!finalJson.contains(Objects.requireNonNull(Uri.parse(_filelist.get((int) (_startRepeat))).getLastPathSegment()))) {
                            FileUtils.moveAFile(_filelist.get((int) (_startRepeat)), AppConfig.recyclebin + Uri.parse(_filelist.get((int) (_startRepeat))).getLastPathSegment());
                        }
                    }
                    _startRepeat++;
                }
            }
        }
    }

    public static String quickScanDiskFileInFolder(@NonNull String _foderpath) {
        if (!_foderpath.isEmpty()) {
            int _startRepeat = 0;
            ArrayList<String> _filelist = new ArrayList<>();
            FileUtils.getAListOfAllFilesAndFoldersInADirectory(_foderpath, _filelist);
            if (!_filelist.isEmpty()) {
                for (int _repeat = 0; _repeat < (int)(_filelist.size()); _repeat++) {
                    if (_startRepeat < _filelist.size()) {
                        if (isADiskFile(_filelist.get((int)(_startRepeat)))) {
                            return _filelist.get((int)(_startRepeat));
                        }
                    }
                    _startRepeat++;
                }
            }
        }
        return "";
    }

    public static boolean isADiskFile (@NonNull String _filepath) {
        if (_filepath.contains(".")) {
            String _getFileName = Objects.requireNonNull(Uri.parse(_filepath).getLastPathSegment()).toLowerCase();
            String _getFileFormat = _getFileName.substring((int)(_getFileName.lastIndexOf(".") + 1), (int)(_getFileName.length()));
            return "qcow2,img,vhd,vhdx,vdi,qcow,vmdk,vpc".contains(_getFileFormat);
        }
        return false;
    }

    public static String quickScanISOFileInFolder(@NonNull String _foderpath) {
        if (!_foderpath.isEmpty()) {
            int _startRepeat = 0;
            ArrayList<String> _filelist = new ArrayList<>();
            FileUtils.getAListOfAllFilesAndFoldersInADirectory(_foderpath, _filelist);
            if (!_filelist.isEmpty()) {
                for (int _repeat = 0; _repeat < (int)(_filelist.size()); _repeat++) {
                    if (_startRepeat < _filelist.size()) {
                        if (isAISOFile(_filelist.get((int)(_startRepeat)))) {
                            return _filelist.get((int)(_startRepeat));
                        }
                    }
                    _startRepeat++;
                }
            }
        }
        return "";
    }

    public static boolean isAISOFile (@NonNull String _filepath) {
        if (_filepath.contains(".")) {
            String _getFileName = Objects.requireNonNull(Uri.parse(_filepath).getLastPathSegment()).toLowerCase();
            String _getFileFormat = _getFileName.substring((int)(_getFileName.lastIndexOf(".") + 1), (int)(_getFileName.length()));
            return "iso".contains(_getFileFormat);
        }
        return false;
    }

    public static void setArch(@NonNull String _arch, Activity _activity) {
        switch (_arch) {
            case "I386":
                MainSettingsManager.setArch(_activity, "I386");
                break;
            case "ARM64":
                MainSettingsManager.setArch(_activity, "ARM64");
                break;
            case "PPC":
                MainSettingsManager.setArch(_activity, "PPC");
                break;
            default:
                MainSettingsManager.setArch(_activity, "X86_64");
                break;
        }
    }

    public static boolean isExecutedCommandError(@NonNull String _command, String _result, Activity _activity) {
        if (!_command.contains("qemu-system"))
            return false;
        if (_command.contains("qemu-system") && _result.contains("Killed"))
            return true;
        //Error code: PROOT_IS_MISSING_0
        if (_result.contains("proot\": error=2,")) {
            DialogUtils.twoDialog(_activity, _activity.getResources().getString(R.string.problem_has_been_detected), _activity.getResources().getString(R.string.error_PROOT_IS_MISSING_0), _activity.getString(R.string.continuetext), _activity.getString(R.string.cancel), true, R.drawable.build_24px, true,
                    () -> {
                        MainActivity.isActivate = false;
                        FileUtils.deleteDirectory(_activity.getFilesDir().getAbsolutePath() + "/data");
                        FileUtils.deleteDirectory(_activity.getFilesDir().getAbsolutePath() + "/distro");
                        FileUtils.deleteDirectory(_activity.getFilesDir().getAbsolutePath() + "/usr");
                        Intent intent = new Intent();
                        intent.setClass(_activity, SplashActivity.class);
                        _activity.startActivity(intent);
                        _activity.finish();
                    },
                    null, null);
            return true;
        } else if (_result.contains(") exists") && _result.contains("drive with bus")) {
            //Error code: DRIVE_INDEX_0_EXISTS
            DialogUtils.oneDialog(_activity, _activity.getString(R.string.problem_has_been_detected), _activity.getString(R.string.error_DRIVE_INDEX_0_EXISTS) + "\n\n" + _result, _activity.getString(R.string.ok),true, R.drawable.hard_drive_24px, true,null, null);
            return true;
        } else if (_result.contains("gtk initialization failed") || _result.contains("x11 not available")) {
            //Error code: X11_NOT_AVAILABLE
            DialogUtils.twoDialog(_activity, _activity.getString(R.string.problem_has_been_detected), _activity.getString(R.string.error_X11_NOT_AVAILABLE), _activity.getString(R.string.continuetext), _activity.getString(R.string.cancel), true, R.drawable.cast_24px, true,
                    () -> {
                        MainSettingsManager.setVmUi(_activity, "VNC");
                        DialogUtils.oneDialog(_activity, _activity.getString(R.string.done), _activity.getString(R.string.switched_to_VNC), _activity.getString(R.string.ok),true, R.drawable.check_24px, true,null, null);
                    },
                    null, null);
            return true;
        } else if (_result.contains("No such file or directory")) {
            //Error code: NO_SUCH_FILE_OR_DIRECTORY
            DialogUtils.oneDialog(_activity, _activity.getString(R.string.problem_has_been_detected), _activity.getString(R.string.error_NO_SUCH_FILE_OR_DIRECTORY), _activity.getString(R.string.ok),true, R.drawable.file_copy_24px, true,null, null);
            _activity.stopService(new Intent(_activity, MainService.class));
            return true;
        } else {
            return false;
        }
    }

    public static boolean isRomsDataJsonNormal(Boolean _needfix, Activity _context) {
        if (isFileExists(AppConfig.romsdatajson)) {
            if (!JSONUtils.isValidFromFile(AppConfig.romsdatajson)) {
                if (_needfix) {
                    DialogUtils.twoDialog(_context, _context.getString(R.string.problem_has_been_detected), _context.getString(R.string.need_fix_json_before_create), _context.getString(R.string.continuetext), _context.getString(R.string.cancel), true, R.drawable.build_24px, true,
                            () -> {
                                FileUtils.moveAFile(AppConfig.maindirpath + "roms-data.json", AppConfig.maindirpath + "roms-data.old.json");
                                FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", "[]");
                                startFixRomsDataJson();
                                fixRomsDataJsonResult(_context);
                            },
                            null, null);
                }
                return false;
            } else {
                return true;
            }
        } else {
            FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", "[]");
            return true;
        }
    }

    public static void fixRomsDataJsonResult(Activity _context) {
        if (restoredVMs == 0) {
            DialogUtils.oneDialog(_context, _context.getString(R.string.done), _context.getString(R.string.roms_data_json_fixed_unsuccessfully), _context.getString(R.string.ok),true, R.drawable.error_96px, true,null, null);
        } else {
            DialogUtils.oneDialog(_context, _context.getString(R.string.done), _context.getString(R.string.roms_data_json_fixed_successfully), _context.getString(R.string.ok),true, R.drawable.check_24px, true,null, null);
        }
        MainActivity.mMainAdapter.notifyDataSetChanged();
        MainActivity.mdatasize2();
        movetoRecycleBin();
    }

    public static boolean isthiscommandsafe(@NonNull String _command, Context _context) {
        if (_command.startsWith("qemu")) {
            if (!_command.contains("&")) {
                if (!_command.contains("\n")) {
                    if (!_command.contains(";")) {
                        if (!_command.contains("|")) {
                            return true;
                        } else {
                            latestUnsafeCommandReason = _context.getString(R.string.command_are_not_allowed_to_contain_vertical_bars);
                        }
                    } else {
                        latestUnsafeCommandReason = _context.getString(R.string.command_are_not_allowed_to_contain_semicolons);
                    }
                } else {
                    latestUnsafeCommandReason = _context.getString(R.string.command_are_not_allowed_to_contain_multiple_lines);
                }
            } else {
                latestUnsafeCommandReason = _context.getString(R.string.command_are_not_allowed_to_contain_amp);
            }
        } else {
            latestUnsafeCommandReason = _context.getString(R.string.not_the_command_to_run_qemu);
        }
        return false;
    }

    public static boolean isthiscommandsafeimg(@NonNull String _command, Context _context) {
        if (!_command.contains("qcow2")) {
            String _getsize = _command.substring(_command.lastIndexOf(" ") + 1);
            if (_getsize.toLowerCase().endsWith("t") || _getsize.toLowerCase().endsWith("p")  || _getsize.toLowerCase().endsWith("e")) {
                latestUnsafeCommandReason = _context.getString(R.string.size_too_large_try_qcow2_format);
                return false;
            }
            if (_getsize.toLowerCase().endsWith("g")) {
                if (_getsize.length() <= 2) {
                    return true;
                } else {
                    latestUnsafeCommandReason = _context.getString(R.string.size_too_large_try_qcow2_format);
                    return false;
                }
            }
            if (_getsize.toLowerCase().endsWith("m")) {
                if (_getsize.length() <= 4) {
                    return true;
                } else {
                    latestUnsafeCommandReason = _context.getString(R.string.size_too_large_try_qcow2_format);
                    return false;
                }
            }
            if (_getsize.toLowerCase().endsWith("k")) {
                if (_getsize.length() <= 8) {
                    return true;
                } else {
                    latestUnsafeCommandReason = _context.getString(R.string.size_too_large_try_qcow2_format);
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isThisVMRunning(String intemExtra, String itemPath) {
        Terminal vterm = new Terminal(MainActivity.activity);
        vterm.executeShellCommand2("ps -e", false, MainActivity.activity);
        if (AppConfig.temporaryLastedTerminalOutput.contains(intemExtra) && AppConfig.temporaryLastedTerminalOutput.contains(itemPath)) {
            Log.d("VMManager.isThisVMRunning", "Yes");
            return true;
        } else {
            Log.d("VMManager.isThisVMRunning", "No");
            return false;
        }
    }

    public static boolean isQemuRunning() {
        Terminal vterm = new Terminal(MainActivity.activity);
        vterm.executeShellCommand2("ps -e", false, MainActivity.activity);
        if (AppConfig.temporaryLastedTerminalOutput.contains("qemu-system")) {
            Log.d("VMManager.isQemuRunning", "Yes");
            return true;
        } else {
            Log.d("VMManager.isQemuRunning", "No");
            return false;
        }
    }

    public static boolean isHaveADisk(String env) {
        return env.contains("-drive") || env.contains("-hda") || env.contains("-hdb") || env.contains("-cdrom") || env.contains("-fda") || env.contains("-fdb");
    }

    public static void setIconWithName(ImageView imageview, String name) {
        String itemName = name.toLowerCase();
        if (itemName.contains("linux") || itemName.contains("ubuntu")  || itemName.contains("debian") || itemName.contains("arch") || itemName.contains("kali")) {
            imageview.setImageResource(R.drawable.linux);
        } else if (itemName.contains("windows")) {
            imageview.setImageResource(R.drawable.windows);
        } else if (itemName.contains("macos") || itemName.contains("mac os")) {
            imageview.setImageResource(R.drawable.macos);
        } else if (itemName.contains("android")) {
            imageview.setImageResource(R.drawable.android);
        } else {
            imageview.setImageResource(R.drawable.ic_computer_180dp_with_padding);
        }
    }

    public static void requestKillAllQemuProcess(Activity activity) {
        DialogUtils.twoDialog(activity, activity.getString(R.string.do_you_want_to_kill_all_qemu_processes), activity.getString(R.string.all_running_vms_will_be_forcibly_shut_down), activity.getString(R.string.kill_all), activity.getString(R.string.cancel), true, R.drawable.power_settings_new_24px, true,
                () -> killallqemuprocesses(activity), null, null);
    }

    public static void killcurrentqemuprocess(Context context) {
        Terminal vterm = new Terminal(context);
        String env = "killall -9 ";
        switch (MainSettingsManager.getArch(MainActivity.activity)) {
            case "ARM64":
                env += "qemu-system-aarch64";
                break;
            case "PPC":
                env += "qemu-system-ppc";
                break;
            case "I386":
                env += "qemu-system-i386";
                break;
            default:
                env += "qemu-system-x86_64";
                break;
        }
        vterm.executeShellCommand2(env, false, MainActivity.activity);
    }

    public static void killallqemuprocesses(Context context) {
        Terminal vterm = new Terminal(context);
        vterm.executeShellCommand2("killall -9 qemu-system-i386", false, MainActivity.activity);
        vterm.executeShellCommand2("killall -9 qemu-system-x86_64", false, MainActivity.activity);
        vterm.executeShellCommand2("killall -9 qemu-system-aarch64", false, MainActivity.activity);
        vterm.executeShellCommand2("killall -9 qemu-system-ppc", false, MainActivity.activity);
    }

    public static void shutdownCurrentVM() {
        QmpClient.sendCommand("{ \"execute\": \"quit\" }");
    }

    public static void resetCurrentVM() {
        QmpClient.sendCommand("{ \"execute\": \"system_reset\" }");
    }

    public static void showChangeRemovableDevicesDialog(Activity _activity, boolean isMainVNCActivity) {

        String allDevice = getAllDevicesInQemu();

        View _view = LayoutInflater.from(_activity).inflate(R.layout.dialog_change_removable_devices, null);
        AlertDialog _dialog = new MaterialAlertDialogBuilder(_activity, R.style.CenteredDialogTheme)
                .setView(_view)
                .create();

        if (allDevice.contains("ide1-cd0")
                || allDevice.contains("ide2-cd0")
                || allDevice.contains("floppy0")
                || allDevice.contains("floppy1")
                || allDevice.contains("sd0")) {

            if (allDevice.contains("ide1-cd0")
                    || allDevice.contains("ide2-cd0")) {

                _view.findViewById(R.id.ln_cdrom).setOnClickListener(v -> {
                    Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    _activity.startActivityForResult(intent, 120);
                    _dialog.dismiss();
                });

                _view.findViewById(R.id.iv_ejectcdrom).setOnClickListener(v -> {
                    ejectCDROM(_activity);
                    _dialog.dismiss();
                });
            } else {
                _view.findViewById(R.id.ln_cdrom).setVisibility(View.GONE);
            }

            if (allDevice.contains("floppy0")) {
                _view.findViewById(R.id.ln_fda).setOnClickListener(v -> {
                    Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    _activity.startActivityForResult(intent, 889);
                    _dialog.dismiss();
                });

                _view.findViewById(R.id.iv_ejectfda).setOnClickListener(v -> {
                    ejectFloppyDriveA(_activity);
                    _dialog.dismiss();
                });

                if (!allDevice.contains("floppy1")) {
                    TextView tvFda = _view.findViewById(R.id.tv_fda);
                    tvFda.setText(R.string.floppy_drive);
                }
            } else {
                _view.findViewById(R.id.ln_fda).setVisibility(View.GONE);
            }

            if (allDevice.contains("floppy1")) {
                _view.findViewById(R.id.ln_fdb).setOnClickListener(v -> {
                    Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    _activity.startActivityForResult(intent, 13335);
                    _dialog.dismiss();
                });

                _view.findViewById(R.id.iv_ejectfdb).setOnClickListener(v -> {
                    ejectFloppyDriveB(_activity);
                    _dialog.dismiss();
                });

                if (!allDevice.contains("floppy0")) {
                    TextView tvFdb = _view.findViewById(R.id.tv_fdb);
                    tvFdb.setText(R.string.floppy_drive);
                }
            } else {
                _view.findViewById(R.id.ln_fdb).setVisibility(View.GONE);
            }

            if (allDevice.contains("sd0")) {
                _view.findViewById(R.id.ln_sd).setOnClickListener(v -> {
                    Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    _activity.startActivityForResult(intent, 32);
                    _dialog.dismiss();
                });

                _view.findViewById(R.id.iv_ejectsd).setOnClickListener(v -> {
                    ejectSDCard(_activity);
                    _dialog.dismiss();
                });
            } else {
                _view.findViewById(R.id.ln_sd).setVisibility(View.GONE);
            }

            _view.findViewById(R.id.ln_otherdevice).setOnClickListener(v -> {
                showChangeRemovableDevicesWithIDDialog(_activity);
                _dialog.dismiss();
            });
        } else {
            TextView tvFdb = _view.findViewById(R.id.tv_otherdevice);
            tvFdb.setText(R.string.change_or_eject_a_device);
        }

        if (isMainVNCActivity) {
            _view.findViewById(R.id.ln_mouse).setOnClickListener(v -> {
                final Dialog alertDialog = new Dialog(_activity, R.style.MainDialogTheme);
                alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                Objects.requireNonNull(alertDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                alertDialog.setContentView(R.layout.dialog_setting);
                alertDialog.show();
                _dialog.dismiss();
            });
        } else {
            _view.findViewById(R.id.ln_user_interface).setVisibility(View.GONE);
        }

        _dialog.show();
    }

    public static void showChangeRemovableDevicesWithIDDialog(Activity _activity) {
        View _view = LayoutInflater.from(_activity).inflate(R.layout.widget_edittext_dialog, null);
        AlertDialog _dialog = new MaterialAlertDialogBuilder(_activity, R.style.CenteredDialogTheme)
                .setTitle(_activity.getString(R.string.change_a_removable_device))
                .setView(_view)
                .create();

        EditText _edittext = _view.findViewById(R.id.editText);
        TextInputLayout _textInputLayout = _view.findViewById(R.id.textInputLayout);
        _textInputLayout.setHint(_activity.getString(R.string.enter_device_id));

        _dialog.setButton(DialogInterface.BUTTON_POSITIVE, _activity.getString(R.string.change_disk_file), (dialog, which) -> {
            if (!_edittext.getText().toString().isEmpty()) {
                pendingDeviceID = _edittext.getText().toString();

                Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                _activity.startActivityForResult(intent, 1996);
                _dialog.dismiss();
            } else {
                Toast.makeText(_activity, _activity.getString(R.string.you_need_to_enter_the_device_id), Toast.LENGTH_SHORT).show();
            }
        });

        _dialog.setButton(DialogInterface.BUTTON_NEUTRAL, _activity.getString(R.string.eject), (dialog, which) -> {
            if (!_edittext.getText().toString().isEmpty()) {
                ejectRemovableDevice(_edittext.getText().toString(), _activity);
                _dialog.dismiss();
            } else {
                Toast.makeText(_activity, _activity.getString(R.string.you_need_to_enter_the_device_id), Toast.LENGTH_SHORT).show();
            }
        });

        _dialog.setButton(DialogInterface.BUTTON_NEGATIVE, _activity.getString(R.string.close), (dialog, which) -> _dialog.dismiss());

        _dialog.show();
    }

    public static void changeCDROM(String _path, Activity _activity) {
        if (isUsingQ35(lastQemuCommand)) {
            if (isQMPCommandSuccess(QmpClient.sendCommand(changeRemovableDevicesQMPCommand("ide2-cd0", _path)))) {
                if (_activity != null && !_activity.isFinishing())
                    Toast.makeText(_activity, _activity.getString(R.string.changed), Toast.LENGTH_SHORT).show();
            } else {
                if (_activity != null && !_activity.isFinishing())
                    Toast.makeText(_activity, _activity.getString(R.string.change_failed), Toast.LENGTH_SHORT).show();
            }
        } else {
            if (isQMPCommandSuccess(QmpClient.sendCommand(changeRemovableDevicesQMPCommand("ide1-cd0", _path)))) {
                if (_activity != null && !_activity.isFinishing())
                    Toast.makeText(_activity, _activity.getString(R.string.changed), Toast.LENGTH_SHORT).show();
            } else {
                if (_activity != null && !_activity.isFinishing())
                    Toast.makeText(_activity, _activity.getString(R.string.change_failed), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void changeFloppyDriveA(String _path, Activity _activity) {
        if (isQMPCommandSuccess(QmpClient.sendCommand(changeRemovableDevicesQMPCommand("floppy0", _path)))) {
            if (_activity != null && !_activity.isFinishing())
                Toast.makeText(_activity, _activity.getString(R.string.changed), Toast.LENGTH_SHORT).show();
        } else {
            if (_activity != null && !_activity.isFinishing())
                Toast.makeText(_activity, _activity.getString(R.string.change_failed), Toast.LENGTH_SHORT).show();
        }
    }

    public static void changeFloppyDriveB(String _path, Activity _activity) {
        if (isQMPCommandSuccess(QmpClient.sendCommand(changeRemovableDevicesQMPCommand("floppy1", _path)))) {
            if (_activity != null && !_activity.isFinishing())
                Toast.makeText(_activity, _activity.getString(R.string.changed), Toast.LENGTH_SHORT).show();
        } else {
            if (_activity != null && !_activity.isFinishing())
                Toast.makeText(_activity, _activity.getString(R.string.change_failed), Toast.LENGTH_SHORT).show();
        }
    }

    public static void changeSDCard(String _path, Activity _activity) {
        if (isQMPCommandSuccess(QmpClient.sendCommand(changeRemovableDevicesQMPCommand("sd0", _path)))) {
            if (_activity != null && !_activity.isFinishing())
                Toast.makeText(_activity, _activity.getString(R.string.changed), Toast.LENGTH_SHORT).show();
        } else {
            if (_activity != null && !_activity.isFinishing())
                Toast.makeText(_activity, _activity.getString(R.string.change_failed), Toast.LENGTH_SHORT).show();
        }
    }

    public static void ejectCDROM(Activity _activity) {
        if (isUsingQ35(lastQemuCommand)) {
            if (isQMPCommandSuccess(QmpClient.sendCommand(ejectRemovableDevicesQMPCommand("ide2-cd0")))) {
                if (_activity != null && !_activity.isFinishing())
                    Toast.makeText(_activity, _activity.getString(R.string.ejected), Toast.LENGTH_SHORT).show();
            } else {
                if (_activity != null && !_activity.isFinishing())
                    Toast.makeText(_activity, _activity.getString(R.string.eject_failed), Toast.LENGTH_SHORT).show();
            }
        } else {
            if (isQMPCommandSuccess(QmpClient.sendCommand(ejectRemovableDevicesQMPCommand("ide1-cd0")))) {
                if (_activity != null && !_activity.isFinishing())
                    Toast.makeText(_activity, _activity.getString(R.string.ejected), Toast.LENGTH_SHORT).show();
            } else {
                if (_activity != null && !_activity.isFinishing())
                    Toast.makeText(_activity, _activity.getString(R.string.eject_failed), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void ejectFloppyDriveA(Activity _activity) {
        if (isQMPCommandSuccess(QmpClient.sendCommand(ejectRemovableDevicesQMPCommand("floppy0")))) {
            if (_activity != null && !_activity.isFinishing())
                Toast.makeText(_activity, _activity.getString(R.string.ejected), Toast.LENGTH_SHORT).show();
        } else {
            if (_activity != null && !_activity.isFinishing())
                Toast.makeText(_activity, _activity.getString(R.string.eject_failed), Toast.LENGTH_SHORT).show();
        }
    }

    public static void ejectFloppyDriveB(Activity _activity) {
        if (isQMPCommandSuccess(QmpClient.sendCommand(ejectRemovableDevicesQMPCommand("floppy1")))) {
            if (_activity != null && !_activity.isFinishing())
                Toast.makeText(_activity, _activity.getString(R.string.ejected), Toast.LENGTH_SHORT).show();
        } else {
            if (_activity != null && !_activity.isFinishing())
                Toast.makeText(_activity, _activity.getString(R.string.eject_failed), Toast.LENGTH_SHORT).show();
        }
    }

    public static void ejectSDCard(Activity _activity) {
        if (isQMPCommandSuccess(QmpClient.sendCommand(ejectRemovableDevicesQMPCommand("sd0")))) {
            if (_activity != null && !_activity.isFinishing())
                Toast.makeText(_activity, _activity.getString(R.string.ejected), Toast.LENGTH_SHORT).show();
        } else {
            if (_activity != null && !_activity.isFinishing())
                Toast.makeText(_activity, _activity.getString(R.string.eject_failed), Toast.LENGTH_SHORT).show();
        }
    }

    public static void changeRemovableDevice(String _deviceID, String _filepath, Activity _activity) {
        String _result = QmpClient.sendCommand(changeRemovableDevicesQMPCommand(_deviceID, _filepath));
        if (isQMPCommandSuccess(_result)) {
            if (_activity != null && !_activity.isFinishing())
                Toast.makeText(_activity, _activity.getString(R.string.changed), Toast.LENGTH_SHORT).show();
        } else {
            if (_activity != null && !_activity.isFinishing()) {
                if (_result.contains("is not removable")) {
                    Toast.makeText(_activity, _activity.getString(R.string.this_is_not_a_removable_device), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(_activity, _activity.getString(R.string.change_failed), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public static void ejectRemovableDevice(String _deviceID, Activity _activity) {
        String _result = QmpClient.sendCommand(ejectRemovableDevicesQMPCommand(_deviceID));
        if (isQMPCommandSuccess(_result)) {
            if (_activity != null && !_activity.isFinishing())
                Toast.makeText(_activity, _activity.getString(R.string.ejected), Toast.LENGTH_SHORT).show();
        } else {
            if (_activity != null && !_activity.isFinishing()) {
                if (_result.contains("is not removable")) {
                    Toast.makeText(_activity, _activity.getString(R.string.this_is_not_a_removable_device), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(_activity, _activity.getString(R.string.eject_failed), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public static void setVNCPasswordWithDelay(String _password, Activity _activity) {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                setVNCPassword(_password, _activity);
            } catch (InterruptedException e) {
                Log.d(TAG, "setVNCPasswordWithDelay: " + e.getMessage());
            }
        }).start();
    }

    public static void setVNCPassword(String _password, Activity _activity) {
        String _result = QmpClient.sendCommand(changeVNCPasswordQMPCommand(_password));
        if (isQMPCommandSuccess(_result)) {
            Log.d(TAG, "setVNCPassword: Success");
        } else {
            Log.d(TAG, "setVNCPassword: Failed");
        }
    }

    @NonNull
    @Contract(pure = true)
    public static String changeRemovableDevicesQMPCommand(String _device, String _filepath) {
        return "{ \n" +
                "  \"execute\": \"blockdev-change-medium\", \n" +
                "  \"arguments\": { \n" +
                "    \"device\": \"" + _device + "\", \n" +
                "    \"filename\": \"" + _filepath + "\", \n" +
                "    \"format\": \"raw\" \n" +
                "  } \n" +
                "}";
    }


    @NonNull
    @Contract(pure = true)
    public static String ejectRemovableDevicesQMPCommand(String _device) {
        return "{ \"execute\": \"eject\", \"arguments\": { \"device\": \""+ _device +"\" } }";
    }

    public static String getAllDevicesInQemu() {
        return QmpClient.sendCommand("{ \"execute\": \"query-block\" }");
    }

    public static String changeVNCPasswordQMPCommand(String _password) {
        return "{ \"execute\": \"change-vnc-password\", \"arguments\": { \"password\": \"" + _password +"\" } }";
    }

    public static boolean isQMPCommandSuccess(String _result) {
        if (_result == null) return false;

        Log.d("VMManager", "isQMPCommandSuccess: " + _result);
        return _result.contains("\"return\": {}");
    }


    @Contract(pure = true)
    public static boolean isUsingQemuARM(@NonNull String _qemuCommand) {
        return _qemuCommand.contains("qemu-system-a");
    }

    @Contract(pure = true)
    public static boolean isUsingQemuPowerPC(@NonNull String _qemuCommand) {
        return _qemuCommand.contains("qemu-system-p");
    }

    public static boolean isUsingQ35(@NonNull String _qemuCommand) {
        return _qemuCommand.contains("-M q35")
                || _qemuCommand.contains("-machine q35")
                || _qemuCommand.contains("-M pc-q35")
                || _qemuCommand.contains("-machine pc-q35");
    }
}
