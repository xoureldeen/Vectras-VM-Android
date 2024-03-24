package com.undatech.opaque.util;

import static android.content.Context.MODE_PRIVATE;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import com.undatech.opaque.RemoteClientLibConstants;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class FileUtils {
    public static String TAG = "FileUtils";

    public static String join(String path1, String path2) {
        File file1 = new File(path1);
        File file2 = new File(file1, path2);
        return file2.getPath();
    }

    public static void deleteFile(String path) {
        new File(path).delete();
    }

    /**
     * Outputs the given InputStream to a file.
     *
     * @param is
     * @param file
     * @throws IOException
     */
    public static void outputToFile(InputStream is, File file) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        byte[] data = new byte[RemoteClientLibConstants.URL_BUFFER_SIZE];
        int current = 0;

        while ((current = bis.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, current);
        }

        FileOutputStream fos = new FileOutputStream(file);
        fos.write(buffer.toByteArray());
        fos.close();
    }

    public static InputStream getInputStreamFromUri(ContentResolver resolver, Uri uri) {
        InputStream in = null;
        try {
            in = resolver.openInputStream(uri);
        } catch (Exception e) {
            Log.e(TAG, "getInputStreamFromUri: uri: " + uri + ", exception: " + e);
            e.printStackTrace();
        }
        return in;
    }

    public static OutputStream getOutputStreamFromUri(ContentResolver resolver, Uri uri) {
        OutputStream out = null;
        try {
            out = resolver.openOutputStream(uri, "wt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;
    }

    public static List<String> listFiles(Context context, String dirFrom) throws IOException {
        Resources res = context.getResources();
        AssetManager am = res.getAssets();
        String fileList[] = am.list(dirFrom);

        if (fileList != null) {
            for (int i = 0; i < fileList.length; i++) {
                Log.d("", fileList[i]);
            }
        }
        return (List<String>) Arrays.asList(fileList);
    }

    /**
     * Writes out a file from context.getResources().getAssets() to an output file in context.getFilesDir()
     * the file is written only if the application version changed since the previous time it was
     * saved, avoiding unnecessary flash writes.
     * <p>
     * e.g. FileUtils.writeFileOutFromAssetsIfNeeded(getContext(), "ssl/certs/ca-certificates.crt", "ca-certificates.crt");
     *
     * @param context     the context to get assets and resources from.
     * @param fileName    path to file in assets
     * @param outFileName file name of file to write out.
     * @throws IOException
     * @throws PackageManager.NameNotFoundException
     */
    public static void writeFileOutFromAssetsIfNeeded(Context context, String fileName, String outFileName) throws IOException, PackageManager.NameNotFoundException {
        PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        String currentVersion = pInfo.versionName;
        SharedPreferences sp = context.getSharedPreferences("generalSettings", MODE_PRIVATE);
        String lastVersion = sp.getString(outFileName + "_lastVersion", "");

        // If currentVersion != lastVersion, or if the file isn't present,
        // we write out the file and save the current version in the prefs.
        android.util.Log.e(TAG, "Will output from assets to file named: " + outFileName);
        File file = new File(context.getFilesDir() + "/" + outFileName);
        if (!file.exists() || !currentVersion.equals(lastVersion)) {
            // Open the asset file
            InputStream in = context.getAssets().open(fileName);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int size = 0;
            // Read the entire asset
            byte[] buffer = new byte[1024];
            while ((size = in.read(buffer, 0, 1024)) >= 0) {
                outputStream.write(buffer, 0, size);
            }
            in.close();
            buffer = outputStream.toByteArray();

            // Write the the file out
            FileOutputStream fos = context.openFileOutput(outFileName, MODE_PRIVATE);
            fos.write(buffer);
            fos.close();

            // Update version
            android.util.Log.i(TAG, "Updating version of file: " + outFileName);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(outFileName + "_lastVersion", currentVersion);
            editor.apply();
        } else {
            android.util.Log.i(TAG, "Current version of file already exists: " + outFileName);
        }
    }

    public static void logFilesInPrivateStorage(Context context) {
        Log.d(TAG, "logFilesInPrivateStorage");
        for (File f : getListFiles(context.getFilesDir())) {
            Log.d(TAG, f.toString());
        }
    }

    public static List<File> getListFiles(File parentDir) {
        List<File> inFiles = new ArrayList<>();
        Queue<File> files = new LinkedList<>(Arrays.asList(parentDir.listFiles()));
        while (!files.isEmpty()) {
            File file = files.remove();
            if (file.isDirectory()) {
                files.addAll(Arrays.asList(file.listFiles()));
            } else {
                inFiles.add(file);
            }
        }
        return inFiles;
    }

    public static void deletePrivateFileIfExisting(Context context, String path) {
        String privatePath = context.getFilesDir().getPath() + "/" + path;
        File file = new File(privatePath);
        try {
            deleteDirectoryRecursively(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean deleteDirectoryRecursively(File file) throws IOException {
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    return deleteDirectoryRecursively(entry);
                }
            }
        }
        if (!file.delete()) {
            Log.d(TAG, "Failed to delete the file or directory: " + file.toString());
            return false;
        }
        Log.d(TAG, "Successfully deleted the file or directory: " + file.toString());
        return true;
    }
}
