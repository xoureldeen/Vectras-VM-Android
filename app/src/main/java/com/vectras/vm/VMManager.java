package com.vectras.vm;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;
import static com.vectras.vm.utils.FileUtils.isFileExists;

import android.androidVNC.ConnectionBean;
import android.androidVNC.VncCanvasActivity;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.vectras.qemu.Config;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.MainVNCActivity;
import com.vectras.qemu.VNCConfig;
import com.vectras.qemu.utils.QmpClient;
import com.vectras.vm.main.MainActivity;
import com.vectras.vm.main.core.MainStartVM;
import com.vectras.vm.main.vms.DataMainRoms;
import com.vectras.vm.settings.VNCSettingsActivity;
import com.vectras.vm.settings.X11DisplaySettingsActivity;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.JSONUtils;
import com.vectras.vm.utils.TextUtils;
import com.vectras.vm.utils.UIUtils;
import com.vectras.vterm.Terminal;

import org.jetbrains.annotations.Contract;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class VMManager {

    public static final String TAG = "VMManager";
    public static String finalJson = "";
    public static String pendingDeviceID = "";
    public static String generatedVMId = "";
    public static int restoredVMs = 0;
    public static boolean isKeptSomeFiles = false;
    public static boolean isQemuStopedWithError = false;
    public static boolean isTryAgain = false;
    public static String latestUnsafeCommandReason = "";
    public static String lastQemuCommand = "";

    public static DataMainRoms getVMConfig(int position) {
        JsonArray arr = JsonParser.parseString(FileUtils.readAFile(AppConfig.romsdatajson)).getAsJsonArray();
        return new Gson().fromJson(arr.get(position), DataMainRoms.class);
    }

    public static boolean isVMExist(String vmId) {
        String vmJsonListContent = FileUtils.readAFile(AppConfig.romsdatajson);

        if (!JSONUtils.isValidFromString(vmJsonListContent) || vmId.isEmpty()) return false;

        ArrayList<HashMap<String, Object>> vmList = new Gson().fromJson(vmJsonListContent, new TypeToken<ArrayList<HashMap<String, Object>>>() {
        }.getType());

        if (vmList == null) return false;

        for (int _repeat = 0; _repeat < vmList.size(); _repeat++) {
            if (vmList.get(_repeat).containsKey("vmID")
                    && Objects.requireNonNull(vmList.get(_repeat).get("vmID")).toString().equals(vmId)) {
                Log.i(TAG, "isVMExist: " + vmId + " - YES.");
                return true;
            }
        }

        Log.i(TAG, "isVMExist: " + vmId + " - NO.");
        return false;
    }

    public static boolean addToVMList(String vmConfigJson, String vmID) {
        String vmListJson = FileUtils.readAFile(AppConfig.romsdatajson);
        if (!JSONUtils.isValidFromString(vmListJson) || !JSONUtils.isValidFromString(vmConfigJson))
            return false;

        ArrayList<HashMap<String, Object>> vmList = new Gson().fromJson(vmListJson, new TypeToken<ArrayList<HashMap<String, Object>>>() {
        }.getType());

        if (vmList == null) return false;

        HashMap<String, Object> vmConfigMap = new Gson().fromJson(vmConfigJson, new TypeToken<HashMap<String, Object>>() {
        }.getType());

        if (!vmID.isEmpty()) {
            generatedVMId = vmID;
            vmConfigMap.put("vmID", generatedVMId);
        }

        vmList.add(0, vmConfigMap);
        return writeToVMList(new Gson().toJson(vmList)) &&
                writeToVMConfig(Objects.requireNonNull(vmConfigMap.get("vmID")).toString(), new Gson().toJson(vmConfigMap));
    }

    public static boolean addToVMList(HashMap<String, Object> vmConfigMap, String vmID) {
        String vmListJson = FileUtils.readAFile(AppConfig.romsdatajson);
        if (!JSONUtils.isValidFromString(vmListJson)) return false;

        ArrayList<HashMap<String, Object>> vmList = new Gson().fromJson(vmListJson, new TypeToken<ArrayList<HashMap<String, Object>>>() {
        }.getType());

        if (vmList == null) return false;

        if (!vmID.isEmpty()) {
            generatedVMId = vmID;
            vmConfigMap.put("vmID", generatedVMId);
        }

        vmList.add(0, vmConfigMap);
        return writeToVMList(new Gson().toJson(vmList)) &&
                writeToVMConfig(Objects.requireNonNull(vmConfigMap.get("vmID")).toString(), new Gson().toJson(vmConfigMap));
    }

    public static boolean replaceToVMList(int postion, String vmId, String vmConfigJson) {
        String vmListJson = FileUtils.readAFile(AppConfig.romsdatajson);
        if (!JSONUtils.isValidFromString(vmListJson) || !JSONUtils.isValidFromString(vmConfigJson))
            return false;

        int finalPosition = postion;
        ArrayList<HashMap<String, Object>> vmList = new Gson().fromJson(vmListJson, new TypeToken<ArrayList<HashMap<String, Object>>>() {
        }.getType());

        if (vmList == null) return false;

        HashMap<String, Object> vmConfigMap = new Gson().fromJson(vmConfigJson, new TypeToken<HashMap<String, Object>>() {
        }.getType());

        if (postion == -1) {
            for (int _repeat = 0; _repeat < vmList.size(); _repeat++) {
                if (vmList.get(_repeat).containsKey("vmID")
                        && ((!vmId.isEmpty() && Objects.requireNonNull(vmList.get(_repeat).get("vmID")).toString().equals(vmId)) || Objects.requireNonNull(vmList.get(_repeat).get("vmID")).toString().equals(Objects.requireNonNull(vmConfigMap.get("vmID")).toString()))) {
                    finalPosition = _repeat;
                    break;
                }
            }
        }

        if (finalPosition >= 0 && finalPosition < vmList.size()) {
            vmList.set(finalPosition, vmConfigMap);
        } else {
            return false;
        }

        return writeToVMList(new Gson().toJson(vmList)) &&
                writeToVMConfig(Objects.requireNonNull(vmConfigMap.get("vmID")).toString(), new Gson().toJson(vmConfigMap));
    }

    public static boolean replaceToVMList(int postion, String vmId, HashMap<String, Object> vmConfigMap) {
        String vmListJson = FileUtils.readAFile(AppConfig.romsdatajson);
        if (!JSONUtils.isValidFromString(vmListJson)) return false;

        int finalPosition = postion;
        ArrayList<HashMap<String, Object>> vmList = new Gson().fromJson(vmListJson, new TypeToken<ArrayList<HashMap<String, Object>>>() {
        }.getType());

        if (vmList == null) return false;

        if (postion == -1) {
            for (int _repeat = 0; _repeat < vmList.size(); _repeat++) {
                if (vmList.get(_repeat).containsKey("vmID")
                        && ((!vmId.isEmpty() && Objects.requireNonNull(vmList.get(_repeat).get("vmID")).toString().equals(vmId)) || Objects.requireNonNull(vmList.get(_repeat).get("vmID")).toString().equals(Objects.requireNonNull(vmConfigMap.get("vmID")).toString()))) {
                    finalPosition = _repeat;
                    break;
                }
            }
        }

        if (finalPosition >= 0 && finalPosition < vmList.size()) {
            vmList.set(finalPosition, vmConfigMap);
        } else {
            return false;
        }

        return writeToVMList(new Gson().toJson(vmList)) &&
                writeToVMConfig(Objects.requireNonNull(vmConfigMap.get("vmID")).toString(), new Gson().toJson(vmConfigMap));
    }

    public static boolean writeToVMList(String content) {
        return FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", content);
    }

    public static boolean writeToVMConfig(String vmID, String content) {
        return FileUtils.writeToFile(AppConfig.maindirpath + "/roms/" + vmID, "rom-data.json", content.replace("\\u003d", "=")) &&
                FileUtils.writeToFile(AppConfig.maindirpath + "/roms/" + vmID, "vmID.txt", vmID);
        // TODO: vmID.txt can be removed, it is being retained for backward compatibility.
    }

    public static boolean addVM(HashMap<String, Object> vmConfigMap, int position) {
        return position == -1 ? addToVMList(vmConfigMap, Objects.requireNonNull(vmConfigMap.get("vmID")).toString()) : replaceToVMList(position, "", vmConfigMap);
    }

    public static boolean createNewVM(String name, String thumbnail, String drive, String arch, String cdrom, String params, String vmID, int port) {
        HashMap<String, Object> vmConfigMap = new HashMap<>();
        vmConfigMap.put("imgName", name);
        vmConfigMap.put("imgIcon", thumbnail);
        vmConfigMap.put("imgPath", drive);
        vmConfigMap.put("imgCdrom", cdrom);
        vmConfigMap.put("imgExtra", params);
        vmConfigMap.put("imgArch", arch);
        vmConfigMap.put("vmID", vmID);
        vmConfigMap.put("qmpPort", port);

        return addToVMList(vmConfigMap, vmID);
    }

    public static boolean editVM(String name, String thumbnail, String drive, String arch, String cdrom, String params, int position) {
        ArrayList<HashMap<String, Object>> vmList;

        vmList = new Gson().fromJson(FileUtils.readAFile(AppConfig.romsdatajson), new TypeToken<ArrayList<HashMap<String, Object>>>() {
        }.getType());

        HashMap<String, Object> vmConfigMap = new HashMap<>();
        vmConfigMap.put("imgName", name);
        vmConfigMap.put("imgIcon", thumbnail);
        vmConfigMap.put("imgPath", drive);
        vmConfigMap.put("imgCdrom", cdrom);
        vmConfigMap.put("imgExtra", params);
        vmConfigMap.put("imgArch", arch);
        if (!vmList.isEmpty() && vmList.get(position).containsKey("qmpPort")) {
            vmConfigMap.put("qmpPort", vmList.get(position).get("qmpPort"));
        } else {
            vmConfigMap.put("qmpPort", startRandomPort());
        }

        if (!vmList.isEmpty() && vmList.get(position).containsKey("vmID")) {
            vmConfigMap.put("vmID", Objects.requireNonNull(vmList.get(position).get("vmID")).toString());
        } else {
            vmConfigMap.put("vmID", idGenerator());
        }

        return replaceToVMList(position, "", vmConfigMap);
    }

    public static void deleteVMDialog(String _vmName, int _position, Activity _activity) {
        DialogUtils.threeDialog(_activity, _activity.getString(R.string.remove) + " " + _vmName, _activity.getString(R.string.remove_vm_content), _activity.getString(R.string.remove_and_do_not_keep_files), _activity.getString(R.string.remove_but_keep_files), _activity.getString(R.string.cancel), true, R.drawable.delete_24px, true,
                () -> {
                    View progressView = LayoutInflater.from(_activity).inflate(R.layout.dialog_progress_style, null);
                    TextView progress_text = progressView.findViewById(R.id.progress_text);
                    progress_text.setText(_activity.getString(R.string.just_a_moment));
                    AlertDialog progressDialog = new MaterialAlertDialogBuilder(_activity, R.style.CenteredDialogTheme)
                            .setView(progressView)
                            .setCancelable(false)
                            .create();
                    progressDialog.show();

                    new Thread(() -> {
                        isKeptSomeFiles = false;
                        deleteVm(_activity, _position, false);
                        _activity.runOnUiThread(() -> new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            progressDialog.dismiss();
                            MainActivity.refeshVMListNow();
                        }, 500));
                    }).start();
                },
                () -> {
                    View progressView = LayoutInflater.from(_activity).inflate(R.layout.dialog_progress_style, null);
                    TextView progress_text = progressView.findViewById(R.id.progress_text);
                    progress_text.setText(_activity.getString(R.string.just_a_moment));
                    AlertDialog progressDialog = new MaterialAlertDialogBuilder(_activity, R.style.CenteredDialogTheme)
                            .setView(progressView)
                            .setCancelable(false)
                            .create();
                    progressDialog.show();

                    new Thread(() -> {
                        deleteVm(_activity, _position, true);
                        _activity.runOnUiThread(() -> new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            progressDialog.dismiss();
                            MainActivity.refeshVMListNow();
                        }, 500));
                    }).start();
                },
                null,
                null);
    }

    public static void deleteVm(Context context, int position, boolean isKeepFiles) {
        if (!JSONUtils.isValidVmList()) return;
        String vmList = FileUtils.readFromFile(context, new File(AppConfig.maindirpath + "roms-data.json"));
        JsonArray arr = JsonParser.parseString(vmList).getAsJsonArray();
        if (position < 0 || position > arr.size() - 1) return;
        JsonObject obj = arr.get(position).getAsJsonObject();
        String vmId = obj.has("vmID") ? obj.get("vmID").getAsString() : null;
        arr.remove(position);

        vmList = new Gson().toJson(arr);

        if (vmId == null || vmId.isEmpty()) return;
        if (isKeepFiles) {
            FileUtils.rename(AppConfig.vmFolder + vmId, "_" + vmId);
            if (isVmFilesInUse(vmId, vmList)) {
                vmList = vmList.replace(AppConfig.vmFolder + vmId, AppConfig.vmFolder + "_" + vmId);
            }
        } else {
            if (isVmFilesInUse(vmId, vmList)) {
                isKeptSomeFiles = true;
                FileUtils.rename(AppConfig.vmFolder + vmId, "_" + vmId);
                vmList = vmList.replace(AppConfig.vmFolder + vmId, AppConfig.vmFolder + "_" + vmId);
            } else {
                FileUtils.delete(new File(AppConfig.vmFolder + vmId));
            }
        }

        FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", vmList);
    }

    public static int restoreAll() {
        if (!JSONUtils.isValidVmList()) return 0;
        JsonArray arr = JsonParser.parseString(FileUtils.readAFile(AppConfig.romsdatajson)).getAsJsonArray();
        File[] vmFolders = new File(AppConfig.vmFolder).listFiles();
        if (vmFolders == null) return 0;
        List<String> restoredVms = new ArrayList<>();
        for (File f : vmFolders) {
            if (f.getName().startsWith("_") && isFileExists(f.getAbsolutePath() + "/rom-data.json")) {
                String vmConfig = FileUtils.readAFile(f.getAbsolutePath() + "/rom-data.json");
                if (JSONUtils.isValidFromString(vmConfig)) {
                    if (f.getName().startsWith("_"))
                        FileUtils.rename(f.getAbsolutePath(), f.getName().replace("_", ""));
                    arr.add(JsonParser.parseString(vmConfig));
                    restoredVms.add(f.getName().replaceAll("_", ""));
                }
            }
        }

        String finalvmList = new Gson().toJson(arr);

        for (int i = 0; i < restoredVms.size(); i++) {
            if (finalvmList.contains(AppConfig.vmFolder + "_" + restoredVms.get(i))) {
                finalvmList = finalvmList.replace(AppConfig.vmFolder + "_" + restoredVms.get(i), AppConfig.vmFolder + restoredVms.get(i));
            }
        }

        FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", finalvmList);
        return restoredVms.size();
    }

    public static int cleanUp() {
        int cleared = 0;
        String vmList = FileUtils.readAFile(AppConfig.romsdatajson);
        File[] vmFolders = new File(AppConfig.vmFolder).listFiles();
        if (vmFolders == null) return 0;
        for (File f : vmFolders) {
            if (!isVmFilesInUse(f.getName(), vmList)) {
                if (f.getName().startsWith("_")) {
                    FileUtils.delete(new File(f.getAbsolutePath()));
                    cleared++;
                } else if (!isFileExists(f.getAbsolutePath() + "/rom-data.json")) {
                    FileUtils.moveToFolder(f.getAbsolutePath(), AppConfig.recyclebin);
                    cleared++;
                }
            }
        }
        return cleared;
    }

    public static boolean isVmFilesInUse(String vmId) {
        return isVmFilesInUse(vmId, FileUtils.readAFile(AppConfig.romsdatajson));
    }

    public static boolean isVmFilesInUse(String vmId, String vmList) {
        File[] files = new File(AppConfig.vmFolder + vmId).listFiles();
        if (files == null) return false;
        for (File f : files) {
            if (vmList.contains(f.getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }

    public static int moveAllBrokenVMRecycleBin() {
        if (!isFileExists(AppConfig.vmFolder)) return 0;
        int moved = 0;
        FileUtils.createDirectory(AppConfig.recyclebin);
        String vmList = FileUtils.readAFile(AppConfig.romsdatajson);
        if (!vmList.isEmpty()) {
            File[] vmFolders = new File(AppConfig.vmFolder).listFiles();
            if (vmFolders == null) return 0;
            for (File f : vmFolders) {
                if (!vmList.contains(f.getName())) {
                    FileUtils.moveToFolder(f.getAbsolutePath(), AppConfig.recyclebin);
                    moved++;
                }
            }
        }
        return moved;
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
        Random random = new Random();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < 10; i++) {
            result.append(random.nextInt(2) > 0 ? TextUtils.randomALetter() : String.valueOf(random.nextInt(10)));
        }

        return result.toString();
    }

    // TODO: This can be removed because QMP currently uses sockets instead of open ports.
    @Deprecated
    public static int startRandomPort() {
        int _result;
        Random _random = new Random();
        int _min = 10000;
        int _max = 65535;
        _result = _random.nextInt(_max - _min + 1) + _min;

        if (isFileExists(AppConfig.romsdatajson) || FileUtils.canRead(AppConfig.romsdatajson)) {
            if (FileUtils.readAFile(AppConfig.romsdatajson).contains("\"qmpPort\":" + _result)) {
                _result = _random.nextInt(_max - _min + 1) + _min;
            }
            if (FileUtils.readAFile(AppConfig.romsdatajson).contains("\"qmpPort\":" + _result)) {
                _result = _random.nextInt(_max - _min + 1) + _min;
            }
        } else {
            _result = 8080;
        }

        return _result;
    }

    public static void startFixRomsDataJson() {
        int _startRepeat = 0;
        String tempRomData;
        JsonArray arr = new JsonArray();
        restoredVMs = 0;
        ArrayList<String> _filelist = new ArrayList<>();
        FileUtils.getAListOfAllFilesAndFoldersInADirectory(AppConfig.vmFolder, _filelist);
        if (!_filelist.isEmpty()) {
            for (int _repeat = 0; _repeat < _filelist.size(); _repeat++) {
                if (_startRepeat < _filelist.size()) {
                    if (isFileExists(_filelist.get(_startRepeat) + "/vmID.txt")) {
                        if (isFileExists(_filelist.get(_startRepeat) + "/rom-data.json")) {
                            tempRomData = FileUtils.readAFile(_filelist.get(_startRepeat) + "/rom-data.json");
                            if (JSONUtils.isValidFromString(tempRomData)) {
                                arr.add(JsonParser.parseString(tempRomData));
                                restoredVMs++;
                            }
                        }
                    }

                    _startRepeat++;
                    if (_startRepeat == _filelist.size()) {
                        if (restoredVMs > 0) {
                            FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", arr.toString());
                        }
                    }
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
                for (int _repeat = 0; _repeat < _filelist.size(); _repeat++) {
                    if (_startRepeat < _filelist.size()) {
                        if (isADiskFile(_filelist.get(_startRepeat))) {
                            return _filelist.get(_startRepeat);
                        }
                    }
                    _startRepeat++;
                }
            }
        }
        return "";
    }

    public static boolean isADiskFile(@NonNull String _filepath) {
        if (_filepath.contains(".")) {
            String _getFileName = Objects.requireNonNull(Uri.parse(_filepath).getLastPathSegment()).toLowerCase();
            String _getFileFormat = _getFileName.substring(_getFileName.lastIndexOf(".") + 1);
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
                for (int _repeat = 0; _repeat < _filelist.size(); _repeat++) {
                    if (_startRepeat < _filelist.size()) {
                        if (isAISOFile(_filelist.get(_startRepeat))) {
                            return _filelist.get(_startRepeat);
                        }
                    }
                    _startRepeat++;
                }
            }
        }
        return "";
    }

    public static boolean isAISOFile(@NonNull String _filepath) {
        if (_filepath.contains(".")) {
            String _getFileName = Objects.requireNonNull(Uri.parse(_filepath).getLastPathSegment()).toLowerCase();
            String _getFileFormat = _getFileName.substring(_getFileName.lastIndexOf(".") + 1);
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

    public static boolean isExecutedCommandError(@NonNull String _command, String _result, Context _activity) {
        if (!_command.contains("qemu-system")) {
            isQemuStopedWithError = false;
            return false;
        }

        if (_command.contains("qemu-system") && _result.contains("Killed")) {
            isQemuStopedWithError = true;
            return true;
        }
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
                    },
                    null, null);
            isQemuStopedWithError = true;
            return true;
        } else if (_result.contains(") exists") && _result.contains("drive with bus")) {
            //Error code: DRIVE_INDEX_0_EXISTS
            DialogUtils.oneDialog(_activity, _activity.getString(R.string.problem_has_been_detected), _activity.getString(R.string.error_DRIVE_INDEX_0_EXISTS) + "\n\n" + _result, R.drawable.hard_drive_24px);
            isQemuStopedWithError = true;
            return true;
        } else if (_result.contains("gtk initialization failed") || _result.contains("x11 not available")) {
            //Error code: X11_NOT_AVAILABLE
            DialogUtils.twoDialog(_activity, _activity.getString(R.string.problem_has_been_detected), _activity.getString(R.string.error_X11_NOT_AVAILABLE), _activity.getString(R.string.switch_to_vnc), _activity.getString(R.string.cancel), true, R.drawable.cast_24px, true,
                    () -> {
                        MainSettingsManager.setVmUi(_activity, "VNC");
                        DialogUtils.oneDialog(_activity, _activity.getString(R.string.done), _activity.getString(R.string.switched_to_VNC), R.drawable.check_24px);
                    },
                    null, null);
            isQemuStopedWithError = true;
            return true;
        } else if (_result.contains("Couldn't connect to XServer")) {
            if (isTryAgain) {
                DialogUtils.oneDialog(_activity, _activity.getString(R.string.problem_has_been_detected), _activity.getString(R.string.x11_display_cannot_be_used_at_this_time_content) + "\n\n" + _result, R.drawable.cast_warning_24px);
                _activity.stopService(new Intent(_activity, MainService.class));
                isQemuStopedWithError = true;
                isTryAgain = false;
            } else {
                MainStartVM.startTryAgain(_activity);
                isTryAgain = true;
            }
            return true;
        } else if (_result.contains("No such file or directory")) {
            //Error code: NO_SUCH_FILE_OR_DIRECTORY
            DialogUtils.oneDialog(_activity, _activity.getString(R.string.problem_has_been_detected), _activity.getString(R.string.error_NO_SUCH_FILE_OR_DIRECTORY) + "\n\n" + _result, R.drawable.file_copy_24px);
            _activity.stopService(new Intent(_activity, MainService.class));
            isQemuStopedWithError = true;
            return true;
        } else if (_result.contains("another process using")) {
            //Error code: ANOTHER_PROCESS_USING_IMAGE
            DialogUtils.oneDialog(_activity, _activity.getString(R.string.problem_has_been_detected), _activity.getString(R.string.error_ANOTHER_PROCESS_USING_IMAGE) + "\n\n" + _result, R.drawable.file_copy_24px);
            _activity.stopService(new Intent(_activity, MainService.class));
            isQemuStopedWithError = true;
            return true;
        } else if (_result.contains("mesapt: invalid sdl display")) {
            DialogUtils.twoDialog(_activity,
                    _activity.getResources().getString(R.string.problem_has_been_detected),
                    _activity.getResources().getString(R.string.you_need_to_switch_to_sdl_to_use_3dfx),
                    _activity.getString(R.string.go_to_settings),
                    _activity.getString(R.string.close),
                    true,
                    R.drawable.desktop_24px,
                    true,
                    () -> {
                        Intent intent = new Intent();
                        intent.setClass(_activity, X11DisplaySettingsActivity.class);
                        _activity.startActivity(intent);
                    },
                    null, null);
            return false;
        } else if (_command.contains("qemu-system") && _result.contains("qemu-system") && !_result.contains("warning:")) {
            //Error code: UNKNOW_ERROR
            DialogUtils.oneDialog(_activity, _activity.getString(R.string.problem_has_been_detected), _activity.getString(R.string.vm_could_not_be_run_content) + "\n\n" + _result, R.drawable.error_96px);
            _activity.stopService(new Intent(_activity, MainService.class));
            isQemuStopedWithError = true;
            return true;
        } else {
            isQemuStopedWithError = false;
            return false;
        }
    }

    public static boolean isRomsDataJsonValid(Boolean _needfix, Activity _context) {
        if (isFileExists(AppConfig.romsdatajson)) {
            if (!JSONUtils.isValidVmList()) {
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
        DialogUtils.oneDialog(
                _context,
                _context.getString(R.string.done),
                restoredVMs == 0 ? _context.getString(R.string.roms_data_json_fixed_unsuccessfully) : _context.getString(R.string.roms_data_json_fixed_successfully),
                R.drawable.error_96px
        );
        MainActivity.refeshVMListNow();
        moveAllBrokenVMRecycleBin();
    }

    public static boolean isthiscommandsafe(@NonNull String _command, Context _context) {
        Log.d("VMManager.isthiscommandsafe", _command);

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
            if (_getsize.toLowerCase().endsWith("t") || _getsize.toLowerCase().endsWith("p") || _getsize.toLowerCase().endsWith("e")) {
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

    public static boolean isVMRunning(Context context, String vmID) {
        String result = Terminal.executeShellCommandWithResult("ps -e", context);
        if (result.contains(Config.getCacheDir() + "/" + vmID + "/qmpsocket")) {
            Log.d("VMManager.isThisVMRunning", "Yes");
            return true;
        } else {
            Log.d("VMManager.isThisVMRunning", "No");
            return false;
        }
    }

    public static boolean isQemuRunning(Activity activity) {
        Terminal vterm = new Terminal(activity);
        vterm.executeShellCommand2("ps -e", false, activity);
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
        if (itemName.contains("linux") || itemName.contains("ubuntu") || itemName.contains("debian") || itemName.contains("arch") || itemName.contains("kali")) {
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

    public static void requestKillAllQemuProcess(Activity activity, Runnable runnable) {
        DialogUtils.twoDialog(activity, activity.getString(R.string.do_you_want_to_kill_all_qemu_processes), activity.getString(R.string.all_running_vms_will_be_forcibly_shut_down), activity.getString(R.string.kill_all), activity.getString(R.string.cancel), true, R.drawable.power_settings_new_24px, true,
                () -> {
                    killallqemuprocesses(activity);
                    if (runnable != null) runnable.run();
                }, null, null);
    }

    public static void killcurrentqemuprocess(Activity activity) {
        Terminal vterm = new Terminal(activity);
        String env = "killall -15 ";
        switch (MainSettingsManager.getArch(activity)) {
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
        vterm.executeShellCommand2(env, false, null);
    }

    public static void killallqemuprocesses(Context context) {
        Terminal vterm = new Terminal(context);
        vterm.executeShellCommand2("killall -15 qemu-system-i386 && killall -15 qemu-system-x86_64 && killall -15 qemu-system-aarch64 && killall -15 qemu-system-ppc", false, null);
    }

    public static void shutdownCurrentVM() {
        new Thread(() -> QmpClient.sendCommand("{ \"execute\": \"quit\" }")).start();
    }

    public static void resetCurrentVM() {
        new Thread(() -> QmpClient.sendCommand("{ \"execute\": \"system_reset\" }")).start();
    }

    public static void showChangeRemovableDevicesDialog(Activity _activity, VncCanvasActivity vncCanvasActivity) {
        new Thread(() -> {
            String allDevice = getAllDevicesInQemu();

            _activity.runOnUiThread(() -> {
                View _view = LayoutInflater.from(_activity).inflate(R.layout.dialog_change_removable_devices, null);
                AlertDialog _dialog = new MaterialAlertDialogBuilder(_activity, R.style.CenteredDialogTheme)
                        .setView(_view)
                        .create();

                if (allDevice != null && (allDevice.contains("ide1-cd0")
                        || allDevice.contains("ide2-cd0")
                        || allDevice.contains("floppy0")
                        || allDevice.contains("floppy1")
                        || allDevice.contains("sd0"))) {

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

                    _view.findViewById(R.id.ln_cdrom).setVisibility(View.GONE);
                    _view.findViewById(R.id.ln_fda).setVisibility(View.GONE);
                    _view.findViewById(R.id.ln_fdb).setVisibility(View.GONE);
                    _view.findViewById(R.id.ln_sd).setVisibility(View.GONE);
                }

                if (vncCanvasActivity != null) {
                    _view.findViewById(R.id.ln_refresh).setOnClickListener(v -> {
                        _activity.startActivity(new Intent(_activity, MainVNCActivity.class));
                        _activity.overridePendingTransition(0, 0);
                        _activity.finish();
                        _dialog.dismiss();
                    });

                    if (ConnectionBean.useLocalCursor) {
                        TextView tvvirtualmouse = _view.findViewById(R.id.tv_virtualmouse);
                        tvvirtualmouse.setText(_activity.getString(R.string.hide_virtual_mouse));
                    }

                    _view.findViewById(R.id.ln_virtualmouse).setOnClickListener(v -> {
                        MainSettingsManager.setShowVirtualMouse(_activity, !ConnectionBean.useLocalCursor);
                        ConnectionBean.useLocalCursor = !ConnectionBean.useLocalCursor;
                        _dialog.dismiss();
                    });

                    _view.findViewById(R.id.ln_mouse).setOnClickListener(v -> {
                        MainVNCActivity.getContext.onMouseMode();
                        _dialog.dismiss();
                    });

                    _view.findViewById(R.id.ln_settings).setOnClickListener(v -> {
                        _activity.startActivity(new Intent(_activity, VNCSettingsActivity.class));
                        _dialog.dismiss();
                    });

                    if (MainSettingsManager.getVNCScaleMode(_activity) == VNCConfig.oneToOne) {
                        _view.findViewById(R.id.iv_screenOneToOne).setBackgroundResource(R.drawable.dialog_shape_single_button);
                    } else {
                        _view.findViewById(R.id.iv_screenFit).setBackgroundResource(R.drawable.dialog_shape_single_button);
                    }

                    _view.findViewById(R.id.iv_screenOneToOne).setOnClickListener(v -> {
                        MainSettingsManager.setVNCScaleMode(_activity, VNCConfig.oneToOne);
                        _activity.startActivity(new Intent(_activity, MainVNCActivity.class));
                        _activity.overridePendingTransition(0, 0);
                        _activity.finish();
                        _dialog.dismiss();
                    });

                    _view.findViewById(R.id.iv_screenFit).setOnClickListener(v -> {
                        MainSettingsManager.setVNCScaleMode(_activity, VNCConfig.fitToScreen);
                        _activity.startActivity(new Intent(_activity, MainVNCActivity.class));
                        _activity.overridePendingTransition(0, 0);
                        _activity.finish();
                        _dialog.dismiss();
                    });
                } else {
                    _view.findViewById(R.id.ln_user_interface).setVisibility(View.GONE);
                }

                if (!DialogUtils.isAllowShow(_activity)) return;
                _dialog.show();
            });
        }).start();
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

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            _edittext.requestFocus();
            _edittext.setSelection(_edittext.getText().length());
            InputMethodManager imm = (InputMethodManager) _activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(_edittext, InputMethodManager.SHOW_IMPLICIT);
        }, 200);
    }

    public static void changeCDROM(String _path, Activity _activity) {
        new Thread(() -> {
            if (isUsingQ35(lastQemuCommand)) {
                if (isQMPCommandSuccess(QmpClient.sendCommand(changeRemovableDevicesQMPCommand("ide2-cd0", _path)))) {
                    if (_activity != null && !_activity.isFinishing())
                        _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.changed), Toast.LENGTH_SHORT).show());
                } else {
                    if (_activity != null && !_activity.isFinishing())
                        _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.change_failed), Toast.LENGTH_SHORT).show());
                }
            } else {
                if (isQMPCommandSuccess(QmpClient.sendCommand(changeRemovableDevicesQMPCommand("ide1-cd0", _path)))) {
                    if (_activity != null && !_activity.isFinishing())
                        _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.changed), Toast.LENGTH_SHORT).show());
                } else {
                    if (_activity != null && !_activity.isFinishing())
                        _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.change_failed), Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    public static void changeFloppyDriveA(String _path, Activity _activity) {
        new Thread(() -> {
            if (isQMPCommandSuccess(QmpClient.sendCommand(changeRemovableDevicesQMPCommand("floppy0", _path)))) {
                if (_activity != null && !_activity.isFinishing())
                    _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.changed), Toast.LENGTH_SHORT).show());
            } else {
                if (_activity != null && !_activity.isFinishing())
                    _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.change_failed), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    public static void changeFloppyDriveB(String _path, Activity _activity) {
        new Thread(() -> {
            if (isQMPCommandSuccess(QmpClient.sendCommand(changeRemovableDevicesQMPCommand("floppy1", _path)))) {
                if (_activity != null && !_activity.isFinishing())
                    _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.changed), Toast.LENGTH_SHORT).show());
            } else {
                if (_activity != null && !_activity.isFinishing())
                    _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.change_failed), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    public static void changeSDCard(String _path, Activity _activity) {
        new Thread(() -> {
            if (isQMPCommandSuccess(QmpClient.sendCommand(changeRemovableDevicesQMPCommand("sd0", _path)))) {
                if (_activity != null && !_activity.isFinishing())
                    _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.changed), Toast.LENGTH_SHORT).show());
            } else {
                if (_activity != null && !_activity.isFinishing())
                    _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.change_failed), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    public static void ejectCDROM(Activity _activity) {
        new Thread(() -> {
            if (isUsingQ35(lastQemuCommand)) {
                if (isQMPCommandSuccess(QmpClient.sendCommand(ejectRemovableDevicesQMPCommand("ide2-cd0")))) {
                    if (_activity != null && !_activity.isFinishing())
                        _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.ejected), Toast.LENGTH_SHORT).show());
                } else {
                    if (_activity != null && !_activity.isFinishing())
                        _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.eject_failed), Toast.LENGTH_SHORT).show());
                }
            } else {
                if (isQMPCommandSuccess(QmpClient.sendCommand(ejectRemovableDevicesQMPCommand("ide1-cd0")))) {
                    if (_activity != null && !_activity.isFinishing())
                        _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.ejected), Toast.LENGTH_SHORT).show());
                } else {
                    if (_activity != null && !_activity.isFinishing())
                        _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.eject_failed), Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    public static void ejectFloppyDriveA(Activity _activity) {
        new Thread(() -> {
            if (isQMPCommandSuccess(QmpClient.sendCommand(ejectRemovableDevicesQMPCommand("floppy0")))) {
                if (_activity != null && !_activity.isFinishing())
                    _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.ejected), Toast.LENGTH_SHORT).show());
            } else {
                if (_activity != null && !_activity.isFinishing())
                    _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.eject_failed), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    public static void ejectFloppyDriveB(Activity _activity) {
        new Thread(() -> {
            if (isQMPCommandSuccess(QmpClient.sendCommand(ejectRemovableDevicesQMPCommand("floppy1")))) {
                if (_activity != null && !_activity.isFinishing())
                    _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.ejected), Toast.LENGTH_SHORT).show());
            } else {
                if (_activity != null && !_activity.isFinishing())
                    _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.eject_failed), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    public static void ejectSDCard(Activity _activity) {
        new Thread(() -> {
            if (isQMPCommandSuccess(QmpClient.sendCommand(ejectRemovableDevicesQMPCommand("sd0")))) {
                if (_activity != null && !_activity.isFinishing())
                    _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.ejected), Toast.LENGTH_SHORT).show());
            } else {
                if (_activity != null && !_activity.isFinishing())
                    _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.eject_failed), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    public static void changeRemovableDevice(String _deviceID, String _filepath, Activity _activity) {
        new Thread(() -> {
            String _result = QmpClient.sendCommand(changeRemovableDevicesQMPCommand(_deviceID, _filepath));
            if (isQMPCommandSuccess(_result)) {
                if (_activity != null && !_activity.isFinishing())
                    _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.changed), Toast.LENGTH_SHORT).show());
            } else {
                if (_activity != null && !_activity.isFinishing()) {
                    if (_result.contains("is not removable")) {
                        _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.this_is_not_a_removable_device), Toast.LENGTH_SHORT).show());
                    } else {
                        _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.change_failed), Toast.LENGTH_SHORT).show());
                    }
                }
            }
        }).start();
    }

    public static void ejectRemovableDevice(String _deviceID, Activity _activity) {
        new Thread(() -> {
            String _result = QmpClient.sendCommand(ejectRemovableDevicesQMPCommand(_deviceID));
            if (isQMPCommandSuccess(_result)) {
                if (_activity != null && !_activity.isFinishing())
                    _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.ejected), Toast.LENGTH_SHORT).show());
            } else {
                if (_activity != null && !_activity.isFinishing()) {
                    if (_result.contains("is not removable")) {
                        _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.this_is_not_a_removable_device), Toast.LENGTH_SHORT).show());
                    } else {
                        _activity.runOnUiThread(() -> Toast.makeText(_activity, _activity.getString(R.string.eject_failed), Toast.LENGTH_SHORT).show());
                    }
                }
            }
        }).start();
    }

    public static void pressPowerButton() {
        new Thread(() -> QmpClient.sendCommand("{ \"execute\": \"system_powerdown\" }")).start();
    }

    public static void sendLeftMouseKey() {
        pressAKey("left");
    }

    public static void sendRightMouseKey() {
        pressAKey("right");
    }

    public static void sendMiddleMouseKey() {
        pressAKey("middle");
    }

    public static void sendSuperKey() {
        keyDown("KEY_LEFTMETA");
    }

    public static void sendHoldSuperKey() {
        keyDown("KEY_LEFTMETA");
    }

    public static void sendReleaseSuperKey() {
        keyUp("KEY_LEFTMETA");
    }

    public static void pressAKey(String key) {
        new Thread(() -> {
            try {
                keyDown(key);
                Thread.sleep(50);
                keyUp(key);
            } catch (InterruptedException e) {
                Log.d(TAG, "pressAKey: " + e.getMessage());
            }
        }).start();
    }

    public static void keyDown(String key) {
        QmpClient.sendCommand(sendKeyCommand(key, true));
    }

    public static void keyUp(String key) {
        QmpClient.sendCommand(sendKeyCommand(key, false));
    }

    public static String sendKeyCommand(String key, Boolean isDown) {
        return "{" +
                "  \"execute\": \"input-send-event\"," +
                "  \"arguments\": {" +
                "    \"events\": [" +
                "      {" +
                "        \"type\": \"btn\"," +
                "        \"data\": {" +
                "          \"button\": \"" + key + "\"," +
                "          \"down\": " + (isDown ? "true" : "false") +
                "        }" +
                "      }" +
                "    ]" +
                "  }" +
                "}";
    }

    public static void setVNCPasswordWithDelay(String _password) {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                setVNCPassword(_password);
            } catch (InterruptedException e) {
                Log.d(TAG, "setVNCPasswordWithDelay: " + e.getMessage());
            }
        }).start();
    }

    public static void setVNCPassword(String _password) {
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
        return "{ \"execute\": \"eject\", \"arguments\": { \"device\": \"" + _device + "\" } }";
    }

    public static String getAllDevicesInQemu() {
        return QmpClient.sendCommand("{ \"execute\": \"query-block\" }");
    }

    public static String changeVNCPasswordQMPCommand(String _password) {
        return "{ \"execute\": \"change-vnc-password\", \"arguments\": { \"password\": \"" + _password + "\" } }";
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

    public static boolean isNeedUseVirtualMouse() {
        return lastQemuCommand.contains("-vga qxl") ||
                lastQemuCommand.contains("-vga virtio") ||
                lastQemuCommand.contains("-device qxl-vga") ||
                lastQemuCommand.contains("-device virtio-vga") ||
                lastQemuCommand.contains("-device virtio-gpu");
    }

    public static String addAudioDevSdl(String env) {
        final String audioDevParam = ",audiodev=defaultaudiodev -audiodev sdl,id=defaultaudiodev ";
        String result = env;
        if (env.startsWith("-device hda-duplex ") || env.contains(" -device hda-duplex ") || env.endsWith(" -device hda-duplex")) {
            result = result.replaceFirst(" -device hda-duplex", " -device hda-duplex" + audioDevParam);
        } else if (env.startsWith("-device cs4231a ") || env.contains(" -device cs4231a ") || env.endsWith(" -device cs4231a")) {
            result = result.replaceFirst(" -device cs4231a", " -device cs4231a" + audioDevParam);
        } else if (env.startsWith("-device ac97 ") || env.contains(" -device ac97 ") || env.endsWith(" -device ac97")) {
            result = result.replaceFirst(" -device ac97", " -device ac97" + audioDevParam);
        } else if (env.startsWith("-device es1370 ") || env.contains(" -device es1370 ") || env.endsWith(" -device es1370")) {
            result = result.replaceFirst(" -device es1370", " -device es1370" + audioDevParam);
        } else if (env.startsWith("-device sb16 ") || env.contains(" -device sb16 ") || env.endsWith(" -device sb16")) {
            result = result.replaceFirst(" -device sb16", " -device sb16" + audioDevParam);
        } else if (env.startsWith("-device adlib ") || env.contains(" -device adlib ") || env.endsWith(" -device adlib")) {
            result = result.replaceFirst(" -device adlib", " -device adlib" + audioDevParam);
        }
        return result;
    }
}
