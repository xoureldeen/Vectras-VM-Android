package com.vectras.vm.manager;

import android.content.Context;
import android.util.Log;

import com.vectras.vm.AppConfig;
import com.vectras.vm.utils.FileUtils;

import java.io.File;

public class VmFileManager {
    private static final String TAG = "VmFileManager";
    public static final String CONFIG_FILE_NAME = "rom-data.json";
    public static final String THUMBNAIL_FILE_NAME = "thumbnail.png";
    public static final String SCREENSHOT_PPM_FILE_NAME = "screenshot.ppm";
    public static final String SCREENSHOT_PNG_FILE_NAME = "screenshot.png";
    public static final String AUDIO_STREAM_FILE_NAME = "audio.raw";
    public static final String SNAPSHOT_SH_FILE_NAME = "snapshot.sh";
    public static final String SNAPSHOT_BIN_FILE_NAME = "snapshot.bin";
    public static final String CREATE_COMMAND_CONFIG_FILE_NAME = "cqcm.json";
    public static final String LOG_FILE_NAME = "vm.log";
    public static final String TEXT_MARK_VM_PATH = "OhnoIjustrealizeditsmidnightandIstillhavetodothis";
    public static final String HIDE_VM_SUFFIX = "_";


    public static boolean hide(String vmId) {
        return FileUtils.rename(getPath(vmId), HIDE_VM_SUFFIX + vmId);
    }

    public static String replaceToHide(String vmId, String content) {
        return content.replace(quickGetPath(vmId), quickGetPathHide(vmId));
    }

    public static String replaceToShow(String vmId, String content) {
        String finalVmId = vmId;
        if (finalVmId.startsWith(HIDE_VM_SUFFIX))
            finalVmId = vmId.substring(1);

        return content.replace(quickGetPathHide(finalVmId), quickGetPath(finalVmId));
    }

    public static boolean visible(String vmId) {
        return FileUtils.rename(getPath(HIDE_VM_SUFFIX + vmId), vmId);
    }

    public static String getPath(String vmId, String childFilePath) {
        return new File(getPath(vmId), childFilePath).getAbsolutePath();
    }

    public static String getPath(String vmId) {
        String path = new File(AppConfig.vmFolder, vmId).getAbsolutePath();
        FileUtils.createDirectory(path);
        return path + "/";
    }

    public static String getPathHide(String vmId) {
        String path = new File(AppConfig.vmFolder, HIDE_VM_SUFFIX + vmId).getAbsolutePath();
        return path + "/";
    }

    public static String quickGetPath(String vmId) {
        return new File(AppConfig.vmFolder, vmId).getAbsolutePath();
    }

    public static String quickGetPathHide(String vmId) {
        return new File(AppConfig.vmFolder, HIDE_VM_SUFFIX + vmId).getAbsolutePath();
    }

    public static boolean delete(Context context, String vmId) {
        VmFileManager.removeTemp(context, vmId);
        return FileUtils.delete(getPath(vmId));
    }

    public static String getTempPath(Context context, String vmId, String childFilePath) {
        return new File(getTempPath(context, vmId), childFilePath).getAbsolutePath();
    }

    public static String getTempPath(Context context, String vmId) {
        File externalCacheDir = context.getExternalCacheDir();
        String cachePath = externalCacheDir != null ? externalCacheDir.getAbsolutePath() : context.getCacheDir().getAbsolutePath();

        String path = new File(cachePath, "temp/" + vmId).getAbsolutePath();
        FileUtils.createDirectory(path);
        return path + "/";
    }

    public static String getInternalTempPath(Context context, String vmId, String childFilePath) {
        return new File(getInternalTempPath(context, vmId), childFilePath).getAbsolutePath();
    }

    public static String getInternalTempPath(Context context, String vmId) {
        String path = new File(context.getCacheDir().getAbsolutePath(), "temp/" + vmId).getAbsolutePath();
        FileUtils.createDirectory(path);
        return path + "/";
    }

    public static boolean removeTemp(Context context, String vmId, String childFilePath) {
        return FileUtils.delete(new File(getTempPath(context, vmId, childFilePath))) ||
                FileUtils.delete(new File(getInternalTempPath(context, vmId, childFilePath)));
    }

    public static boolean removeTemp(Context context, String vmId) {
        return FileUtils.delete(getTempPath(context, vmId));
    }

    public static boolean isConfigFileExists(String vmId) {
        return FileUtils.isFileExists(getConfigFile(vmId));
    }

    public static String getConfigFile(String vmId) {
        Log.d(TAG, VmFileManager.getPath(vmId, CONFIG_FILE_NAME));
        return VmFileManager.getPath(vmId, CONFIG_FILE_NAME);
    }

    public static String getThumbnail(String vmId) {
        return VmFileManager.getPath(vmId, THUMBNAIL_FILE_NAME);
    }

    public static String getScreenshotPpm(Context context, String vmId) {
        return VmFileManager.getTempPath(context, vmId, SCREENSHOT_PPM_FILE_NAME);
    }

    // Find the location of the ppm file if getExternalCacheDir() is null.
    public static String findScreenshotPpm(Context context, String vmId) {
        String ppmFilePath = VmFileManager.getInternalTempPath(context, vmId, SCREENSHOT_PPM_FILE_NAME);
        if (FileUtils.isFileExists(ppmFilePath)) return ppmFilePath;
        return VmFileManager.getScreenshotPpm(context, vmId);
    }

    public static boolean removeScreenshotPpm(Context context, String vmId) {
        return VmFileManager.removeTemp(context, vmId, SCREENSHOT_PPM_FILE_NAME);
    }

    public static boolean isScreenshotPngExists(String vmId) {
        return FileUtils.isFileExists(getScreenshotPng(vmId));
    }

    public static String getScreenshotPng(String vmId) {
        return VmFileManager.getPath(vmId, SCREENSHOT_PNG_FILE_NAME);
    }

    public static String getAudioRaw(Context context, String vmId) {
        return VmFileManager.getTempPath(context, vmId, AUDIO_STREAM_FILE_NAME);
    }

    // Find the location of the audio file if getExternalCacheDir() is null.
    public static String findAudioRaw(Context context, String vmId) {
        String audioFilePath = VmFileManager.getInternalTempPath(context, vmId, AUDIO_STREAM_FILE_NAME);
        if (FileUtils.isFileExists(audioFilePath)) return audioFilePath;
        return VmFileManager.getAudioRaw(context, vmId);
    }

    public static boolean removeAudioRaw(Context context, String vmId) {
        return VmFileManager.removeTemp(context, vmId, AUDIO_STREAM_FILE_NAME);
    }

    public static boolean isSnapshotShExists(String vmId) {
        return FileUtils.isFileExists(getSnapshotSh(vmId));
    }

    public static String getSnapshotSh(String vmId) {
        return VmFileManager.getPath(vmId, SNAPSHOT_SH_FILE_NAME);
    }

    public static boolean isSnapshotBinExists(String vmId) {
        return FileUtils.isFileExists(getSnapshotBin(vmId));
    }

    public static String getSnapshotBin(String vmId) {
        return VmFileManager.getPath(vmId, SNAPSHOT_BIN_FILE_NAME);
    }

    public static String getLog(Context context, String vmId) {
        return VmFileManager.getTempPath(context, vmId, LOG_FILE_NAME);
    }

    public static boolean isCreateCommandConfigFileExists(String vmId) {
        return FileUtils.isFileExists(getCreateCommandConfigFile(vmId));
    }

    public static String getCreateCommandConfigFile(String vmId) {
        return VmFileManager.getPath(vmId, CREATE_COMMAND_CONFIG_FILE_NAME);
    }

    public static String textMarkToPath(String vmId, String content) {
        return content.replace(TEXT_MARK_VM_PATH, getPath(vmId));
    }

    public static String pathToTextMark(String vmId, String content) {
        return content.replace(getPath(vmId), TEXT_MARK_VM_PATH);
    }
}
