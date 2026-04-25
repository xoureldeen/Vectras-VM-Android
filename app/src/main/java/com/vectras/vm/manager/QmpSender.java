package com.vectras.vm.manager;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.vectras.qemu.Config;
import com.vectras.qemu.utils.QmpClient;
import com.vectras.vm.R;
import com.vectras.vm.VectrasApp;
import com.vectras.vm.utils.DialogUtils;

import java.io.StringReader;

public class QmpSender {
    public static final String TAG = "QmpSender";

    public static final String SEND_FAILED_MESSAGE = "The command was sent but failed. Please try again later.\r\n";

    public static final String DEFAULT_OPTICAL_DISC_1_ID = "ide1-cd0";
    public static final String DEFAULT_OPTICAL_DISC_2_ID = "ide2-cd0";
    public static final String DEFAULT_SECONDARY_OPTICAL_DISC_1_ID = "ide0-cd1";
    public static final String DEFAULT_SECONDARY_OPTICAL_DISC_2_ID = "ide1-cd0";
    public static final String DEFAULT_FLOPPY_DISK_0_ID = "floppy0";
    public static final String DEFAULT_FLOPPY_DISK_1_ID = "floppy1";
    public static final String DEFAULT_MEMORY_CARD_ID = "sd0";

    public static boolean isShowErrorDialog = true;

    public static String send(String command) {
        JsonObject arguments = new JsonObject();
        arguments.addProperty("command-line", command);

        JsonObject payload = new JsonObject();
        payload.addProperty("execute", "human-monitor-command");
        payload.add("arguments", arguments);

        return getResult(QmpClient.sendCommand(payload.toString()));
    }

    public static String getResult(String response) {
        try {
            String result = "";

            JsonReader reader = new JsonReader(new StringReader(response));
            reader.setStrictness(Strictness.LENIENT);

            while (reader.peek() != JsonToken.END_DOCUMENT) {
                JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();

                if (obj.has("return")) {
                    JsonElement returnVal = obj.get("return");
                    result = returnVal.isJsonPrimitive()
                            ? returnVal.getAsString()
                            : returnVal.toString();
                }

                if (obj.has("error")) {
                    result = obj.getAsJsonObject("error")
                            .get("desc").getAsString();
                }
            }

            Log.d(TAG, "getResult: " + result);
            return result;
        } catch (Exception e) {
            Log.d(TAG, "getResult: " + response, e);
            return SEND_FAILED_MESSAGE;
        }
    }

    public static boolean isSuccess(String response) {
        return response.trim().isEmpty();
    }

    public static boolean isDeviceNotFound(String response) {
        return response.trim().startsWith("Error: Device '") && response.trim().endsWith("not found");
    }

    public static void quickShutdown() {
        new Thread(() -> shutdown()).start();
    }

    public static String shutdown() {
        return send("quit");
    }

    public static void quickReset() {
        new Thread(() -> reset()).start();
    }

    public static String reset() {
        return send("system_reset");
    }

    public static void quickPause() {
        new Thread(() -> pause()).start();
    }

    public static String pause() {
        return send("stop");
    }

    public static void quickResume() {
        new Thread(() -> resume()).start();
    }

    public static String resume() {
        return send("cont");
    }

    public static void quickMigrate() {
        new Thread(() -> migrate()).start();
    }

    public static String migrate() {
        return send("migrate \"exec:cat > " + VmFileManager.getSnapshotBin(Config.vmID) + "\"");
    }

    public static boolean takeScreenshot() {
        return isSuccess(send("screendump " + VmFileManager.getScreenshotPpm(VectrasApp.getContext(), Config.vmID)));
    }

    public static String getAllDevice() {
        return send("info block").trim();
    }

    public static String changeOpticalDisc(Context context, String filePath, String infoBlock) {
        String device = infoBlock.trim().isEmpty() ? getAllDevice() : infoBlock;

        if (device.contains(DEFAULT_OPTICAL_DISC_2_ID)) {
            return changeOpticalDisc2(context, filePath);
        } else if (device.contains(DEFAULT_OPTICAL_DISC_1_ID)) {
            return changeOpticalDisc1(context, filePath);
        }

        return "";
    }

    public static String ejectOpticalDisc(Context context, String infoBlock) {
        String device = infoBlock.trim().isEmpty() ? getAllDevice() : infoBlock;

        if (device.contains(DEFAULT_OPTICAL_DISC_2_ID)) {
            return ejectOpticalDisc2(context);
        } else if (device.contains(DEFAULT_OPTICAL_DISC_1_ID)) {
            return ejectOpticalDisc1(context);
        }

        return "";
    }

    public static String changeOpticalDisc1(Context context, String filePath) {
        if (context == null) {
            return changeDevice(DEFAULT_OPTICAL_DISC_1_ID, filePath);
        } else {
            quickChangeDevice(context, DEFAULT_OPTICAL_DISC_1_ID, filePath);
        }
        return "";
    }

    public static String ejectOpticalDisc1(Context context) {
        if (context == null) {
            return ejectDevice(DEFAULT_OPTICAL_DISC_1_ID);
        } else {
            quickEjectDevice(context, DEFAULT_OPTICAL_DISC_1_ID);
        }
        return "";
    }

    public static String changeOpticalDisc2(Context context, String filePath) {
        if (context == null) {
            return changeDevice(DEFAULT_OPTICAL_DISC_2_ID, filePath);
        } else {
            quickChangeDevice(context, DEFAULT_OPTICAL_DISC_2_ID, filePath);
        }
        return "";
    }

    public static String ejectOpticalDisc2(Context context) {
        if (context == null) {
            return ejectDevice(DEFAULT_OPTICAL_DISC_2_ID);
        } else {
            quickEjectDevice(context, DEFAULT_OPTICAL_DISC_2_ID);
        }
        return "";
    }

    public static boolean isHavingSecondaryOpticalDisc(String infoBlock) {
        String device = infoBlock.trim().isEmpty() ? getAllDevice() : infoBlock;
        return device.contains(DEFAULT_SECONDARY_OPTICAL_DISC_1_ID) || (device.contains(DEFAULT_OPTICAL_DISC_2_ID) && device.contains(DEFAULT_SECONDARY_OPTICAL_DISC_2_ID));
    }

    public static String changeDynamicSecondaryOpticalDisc(Context context, String filePath, String infoBlock) {
        String device = infoBlock.trim().isEmpty() ? getAllDevice() : infoBlock;

        if (isHavingSecondaryOpticalDisc(device)) {
            changeSecondaryOpticalDisc(context, filePath, device);
        } else {
            changeOpticalDisc(context, filePath, device);
        }

        return "";
    }

    public static String changeSecondaryOpticalDisc(Context context, String filePath, String infoBlock) {
        String device = infoBlock.trim().isEmpty() ? getAllDevice() : infoBlock;

        if (device.contains(DEFAULT_SECONDARY_OPTICAL_DISC_1_ID)) {
            return changeSecondaryOpticalDisc1(context, filePath);
        } else if (device.contains(DEFAULT_OPTICAL_DISC_2_ID) && device.contains(DEFAULT_SECONDARY_OPTICAL_DISC_2_ID)) {
            return changeSecondaryOpticalDisc2(context, filePath);
        }

        return "";
    }

    public static String ejectDynamicSecondaryOpticalDisc(Context context, String infoBlock) {
        String device = infoBlock.trim().isEmpty() ? getAllDevice() : infoBlock;

        if (isHavingSecondaryOpticalDisc(device)) {
            ejectSecondaryOpticalDisc(context, device);
        } else {
            ejectOpticalDisc(context, device);
        }

        return "";
    }

    public static String ejectSecondaryOpticalDisc(Context context, String infoBlock) {
        String device = infoBlock.trim().isEmpty() ? getAllDevice() : infoBlock;

        if (device.contains(DEFAULT_SECONDARY_OPTICAL_DISC_1_ID)) {
            return ejectSecondaryOpticalDisc1(context);
        } else if (device.contains(DEFAULT_OPTICAL_DISC_2_ID) && device.contains(DEFAULT_SECONDARY_OPTICAL_DISC_2_ID)) {
            return ejectSecondaryOpticalDisc2(context);
        }

        return "";
    }

    public static String changeSecondaryOpticalDisc1(Context context, String filePath) {
        if (context == null) {
            return changeDevice(DEFAULT_SECONDARY_OPTICAL_DISC_1_ID, filePath);
        } else {
            quickChangeDevice(context, DEFAULT_SECONDARY_OPTICAL_DISC_1_ID, filePath);
        }
        return "";
    }

    public static String ejectSecondaryOpticalDisc1(Context context) {
        if (context == null) {
            return ejectDevice(DEFAULT_SECONDARY_OPTICAL_DISC_1_ID);
        } else {
            quickEjectDevice(context, DEFAULT_SECONDARY_OPTICAL_DISC_1_ID);
        }
        return "";
    }

    public static String changeSecondaryOpticalDisc2(Context context, String filePath) {
        if (context == null) {
            return changeDevice(DEFAULT_SECONDARY_OPTICAL_DISC_2_ID, filePath);
        } else {
            quickChangeDevice(context, DEFAULT_SECONDARY_OPTICAL_DISC_2_ID, filePath);
        }
        return "";
    }

    public static String ejectSecondaryOpticalDisc2(Context context) {
        if (context == null) {
            return ejectDevice(DEFAULT_SECONDARY_OPTICAL_DISC_2_ID);
        } else {
            quickEjectDevice(context, DEFAULT_SECONDARY_OPTICAL_DISC_2_ID);
        }
        return "";
    }

    public static String changeFloppyDiskA(Context context, String filePath) {
        if (context == null) {
            return changeDevice(DEFAULT_FLOPPY_DISK_0_ID, filePath);
        } else {
            quickChangeDevice(context, DEFAULT_FLOPPY_DISK_0_ID, filePath);
        }
        return "";
    }

    public static String ejectFloppyDiskA(Context context) {
        if (context == null) {
            return ejectDevice(DEFAULT_FLOPPY_DISK_0_ID);
        } else {
            quickEjectDevice(context, DEFAULT_FLOPPY_DISK_0_ID);
        }
        return "";
    }

    public static String changeFloppyDiskB(Context context, String filePath) {
        if (context == null) {
            return changeDevice(DEFAULT_FLOPPY_DISK_1_ID, filePath);
        } else {
            quickChangeDevice(context, DEFAULT_FLOPPY_DISK_1_ID, filePath);
        }
        return "";
    }

    public static String ejectFloppyDiskB(Context context) {
        if (context == null) {
            return ejectDevice(DEFAULT_FLOPPY_DISK_1_ID);
        } else {
            quickEjectDevice(context, DEFAULT_FLOPPY_DISK_1_ID);
        }
        return "";
    }

    public static String changeMemoryCard(Context context, String filePath) {
        if (context == null) {
            return changeDevice(DEFAULT_MEMORY_CARD_ID, filePath);
        } else {
            quickChangeDevice(context, DEFAULT_MEMORY_CARD_ID, filePath);
        }
        return "";
    }

    public static String ejectMemoryCard(Context context) {
        if (context == null) {
            return ejectDevice(DEFAULT_MEMORY_CARD_ID);
        } else {
            quickEjectDevice(context, DEFAULT_MEMORY_CARD_ID);
        }
        return "";
    }

    public static void quickChangeDevice(Context context, String deviceId, String filePath) {
        new Thread(() -> {
            String response = changeDevice(deviceId, filePath);

            if (context == null) return;

            new Handler(Looper.getMainLooper()).post(() -> {
                if (isSuccess(response)) {
                    Toast.makeText(context.getApplicationContext(), context.getString(R.string.changed), Toast.LENGTH_SHORT).show();
                } else {
                    if (!isShowErrorDialog) return;

                    String message = context.getString(R.string.change_failed);
                    if (response.contains("is not removable")) {
                        message = context.getString(R.string.this_is_not_a_removable_device);
                    } else if (response.contains("is locked")) {
                        message = context.getString(R.string.locked_and_cannot_be_ejected_content);
                    }
                    DialogUtils.oopsDialog(context, message);
                }
            });
        }).start();
    }

    public static String changeDevice(String deviceId, String filePath) {
        if (filePath.trim().isEmpty()) return ejectDevice(deviceId);

        return send("change " + deviceId + " " + filePath);
    }

    public static void quickEjectDevice(Context context, String deviceId) {
        new Thread(() -> {
            String response = ejectDevice(deviceId);

            if (context == null) return;

            new Handler(Looper.getMainLooper()).post(() -> {
                if (isSuccess(response)) {
                    Toast.makeText(context.getApplicationContext(), context.getString(R.string.ejected), Toast.LENGTH_SHORT).show();
                } else {
                    if (!isShowErrorDialog) return;

                    String message = context.getString(R.string.eject_failed);
                    if (response.contains("is not removable")) {
                        message = context.getString(R.string.this_is_not_a_removable_device);
                    } else if (response.contains("is locked")) {
                        message = context.getString(R.string.locked_and_cannot_be_ejected_content);
                    }
                    DialogUtils.oopsDialog(context, message);
                }
            });
        }).start();
    }

    public static String ejectDevice(String deviceId) {
        return send("eject " + deviceId);
    }
}
