package com.vectras.vm.creator.utils;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;

import com.vectras.vm.manager.VmFileManager;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.ProgressDialog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class CreatorUtils {
    final String TAG = "CreatorUtils";

    public static final String PENDING_FOLDER = "pending/";

    Activity activity;
    String vmId;

    ProgressDialog progressDialog;

    public CreatorUtils(Activity activity, String vmId) {
        this.activity = activity;
        this.vmId = vmId;
    }

    public void showProgressDialog(String message) {
        if (progressDialog == null) progressDialog = new ProgressDialog(activity);
        progressDialog.setText(message);
        if (!activity.isFinishing() && !activity.isDestroyed()) progressDialog.show();
    }
    
    public void dissmissProgressDialog() {
        if (progressDialog != null && !activity.isFinishing() && !activity.isDestroyed()) {
            progressDialog.dismiss();
        }
    }

    public boolean makeTempDirectory() {
        return FileUtils.createDirectory(VmFileManager.getTempPath(activity, vmId, PENDING_FOLDER));
    }

    public String getTempPath(String fileName) {
        return VmFileManager.getTempPath(activity, vmId, PENDING_FOLDER + fileName);
    }

    public boolean removeTemp() {
        return VmFileManager.removeTemp(activity, vmId, PENDING_FOLDER);
    }

    public String copyToTemp(Uri uri) throws IOException {
        String fileName = FileUtils.getFileNameFromUri(activity, uri);

        if (fileName == null || fileName.isEmpty()) {
            fileName = String.valueOf(System.currentTimeMillis());
        }

        FileUtils.copyFileFromUri(activity, uri, VmFileManager.getTempPath(activity, vmId, PENDING_FOLDER + fileName));
        return VmFileManager.getPath(vmId, fileName);
    }

    public void moveAllFromTemp() {
        ArrayList<String> fileList = new ArrayList<>();
        FileUtils.getAListOfAllFilesAndFoldersInADirectory(VmFileManager.getTempPath(activity, vmId, PENDING_FOLDER), fileList);
        for (int position = 0; position < fileList.size(); position++) {
            FileUtils.moveToFolder(fileList.get(position), VmFileManager.getPath(vmId));
        }

        ArrayList<String> fileListInternal = new ArrayList<>();
        FileUtils.getAListOfAllFilesAndFoldersInADirectory(VmFileManager.getInternalTempPath(activity, vmId, PENDING_FOLDER), fileListInternal);
        for (int position = 0; position < fileListInternal.size(); position++) {
            FileUtils.moveToFolder(fileListInternal.get(position), VmFileManager.getPath(vmId));
        }
    }

    public void deleteTemp(String fileName) {
        VmFileManager.removeTemp(activity, vmId, PENDING_FOLDER + fileName);
    }
    
    
    public void markDelete(String path) {
        VmFileManager.markPendingDelete(path);
        deleteTemp(new File(path).getName());
    }

    public void unMarkDelete(String path) {
        VmFileManager.unMarkPendingDelete(path);
    }

    public FilePathData getFilePath(Uri uri) {
        try {
            String filePath = FileUtils.getDirectoryPath(activity, uri);

            if (filePath != null && FileUtils.isFileExists(filePath)) {
                return new FilePathData() {
                    {
                        isValid = true;
                        path = filePath;
                        name = new File(filePath).getName();
                    }
                };
            }
        } catch (Exception e) {
            Log.e(TAG, "getFilePath: " + uri.toString(), e);
            return new FilePathData();
        }

        return new FilePathData();
    }

    public static class FilePathData {
        public boolean isValid = false;
        public String path;
        public String name;
    }
}
