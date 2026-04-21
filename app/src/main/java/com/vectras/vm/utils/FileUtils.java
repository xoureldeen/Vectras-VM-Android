package com.vectras.vm.utils;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import com.vectras.vm.R;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Objects;

/**
 *
 * @author dev
 */
public class FileUtils {
    public static final String TAG = "FileUtils";

    @NonNull
    public static File getExternalFilesDirectory(Context context) {
        return new File(Environment.getExternalStorageDirectory(), "Documents/VectrasVM");
    }

    public static void chmod(File file, int mode) {
        try {
            Os.chmod(file.getAbsolutePath(), mode);
        } catch (ErrnoException ignored) {
        }
    }

    @SuppressLint("NewApi")
    public static String getPath(Context context, final Uri uri) {
        if ((uri.toString().startsWith("content://ru.zdevs.zarchiver") ||
                uri.toString().startsWith("content://bin.mt.plus") ||
                uri.toString().startsWith("content://com.android.fileexplorer.myprovider") ||
                uri.toString().startsWith("content://com.estrongs.files")) &&
                uri.getPath() != null &&
                isFileExists(uri.getPath())) {
            String path = uri.getPath();

            if (uri.toString().startsWith("content://com.android.fileexplorer.myprovider/external_files")) {
                path = new File(Environment.getExternalStorageDirectory(), path.substring("/external_files".length())).getAbsolutePath();
            }

            return path;
        }

        // check here to KITKAT or new version
        final boolean isKitKat = true;
        String selection = null;
        String[] selectionArgs = null;
        // DocumentProvider
        // ExternalStorageProvider

        if (isExternalStorageDocument(uri)) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];

            String fullPath = getPathFromExtSD(split);
            if (!fullPath.isEmpty()) {
                return fullPath;
            } else {
                return null;
            }
        }


        // DownloadsProvider

        if (isDownloadsDocument(uri)) {
            final String id;
            try (Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    String fileName = cursor.getString(0);
                    String path = Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
                    if (!TextUtils.isEmpty(path)) {
                        return path;
                    }
                }
            }
            id = DocumentsContract.getDocumentId(uri);
            if (!TextUtils.isEmpty(id)) {
                if (id.startsWith("raw:")) {
                    return id.replaceFirst("raw:", "");
                }
                String[] contentUriPrefixesToTry = new String[]{
                        "content://downloads/public_downloads",
                        "content://downloads/my_downloads"
                };
                for (String contentUriPrefix : contentUriPrefixesToTry) {
                    try {
                        final Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.parseLong(id));


                        return getDataColumn(context, contentUri, null, null);
                    } catch (NumberFormatException e) {
                        //In Android 8 and Android P the id is not a number
                        return Objects.requireNonNull(uri.getPath()).replaceFirst("^/document/raw:", "").replaceFirst("^raw:", "");
                    }
                }
            }
        }


        // MediaProvider
        if (isMediaDocument(uri)) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];

            Uri contentUri = null;

            if ("image".equals(type)) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if ("video".equals(type)) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if ("audio".equals(type)) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }
            selection = "_id=?";
            selectionArgs = new String[]{split[1]};


            return getDataColumn(context, contentUri, selection,
                    selectionArgs);
        }

        if (isGoogleDriveUri(uri)) {
            return getDriveFilePath(context, uri);
        }

        if (isWhatsAppFile(uri)) {
            return getFilePathForWhatsApp(context, uri);
        }


        if ("content".equalsIgnoreCase(uri.getScheme())) {

            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }
            if (isGoogleDriveUri(uri)) {
                return getDriveFilePath(context, uri);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                // return getFilePathFromURI(context,uri);
                return copyFileToInternalStorage(context, uri, "userfiles");
                // return getRealPathFromURI(context,uri);
            } else {
                return getDataColumn(context, uri, null, null);
            }

        }
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    private static boolean fileExists(String filePath) {
        File file = new File(filePath);

        return file.exists();
    }

    private static String getPathFromExtSD(String[] pathData) {
        final String type = pathData[0];
        final String relativePath = "/" + pathData[1];
        String fullPath = "";

        // on my Sony devices (4.4.4 & 5.1.1), `type` is a dynamic string
        // something like "71F8-2C0A", some kind of unique id per storage
        // don't know any API that can get the root path of that storage based on its id.
        //
        // so no "primary" type, but let the check here for other devices
        if ("primary".equalsIgnoreCase(type)) {
            fullPath = Environment.getExternalStorageDirectory() + relativePath;
            if (fileExists(fullPath)) {
                return fullPath;
            }
        }

        // Environment.isExternalStorageRemovable() is `true` for external and internal storage
        // so we cannot relay on it.
        //
        // instead, for each possible path, check if file exists
        // we'll start with secondary storage as this could be our (physically) removable sd card
        fullPath = System.getenv("SECONDARY_STORAGE") + relativePath;
        if (fileExists(fullPath)) {
            return fullPath;
        }

        fullPath = System.getenv("EXTERNAL_STORAGE") + relativePath;

        return fullPath;
    }

    private static String getDriveFilePath(Context context, Uri uri) {
        Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
        /*
         * Get the column indexes of the data in the Cursor,
         *     * move to the first row in the Cursor, get the data,
         *     * and display it.
         * */
        assert returnCursor != null;
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String name = (returnCursor.getString(nameIndex));
        String size = (Long.toString(returnCursor.getLong(sizeIndex)));
        File file = new File(context.getCacheDir(), name);
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            int read = 0;
            int maxBufferSize = 1024 * 1024;
            assert inputStream != null;
            int bytesAvailable = inputStream.available();

            //int bufferSize = 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);

            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
            Log.e("File Size", "Size " + file.length());
            inputStream.close();
            outputStream.close();
            Log.e("File Path", "Path " + file.getPath());
            Log.e("File Size", "Size " + file.length());
        } catch (Exception e) {
            Log.e("Exception", Objects.requireNonNull(e.getMessage()));
        }
        return file.getPath();
    }

    /***
     * Used for Android Q+
     * @param uri
     * @param newDirName if you want to create a directory, you can set this variable
     * @return
     */
    private static String copyFileToInternalStorage(Context context, Uri uri, String newDirName) {
        Cursor returnCursor = context.getContentResolver().query(uri, new String[]{
                OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
        }, null, null, null);


        /*
         * Get the column indexes of the data in the Cursor,
         *     * move to the first row in the Cursor, get the data,
         *     * and display it.
         * */
        assert returnCursor != null;
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String name = (returnCursor.getString(nameIndex));
        String size = (Long.toString(returnCursor.getLong(sizeIndex)));

        File output;
        if (!newDirName.equals("")) {
            File dir = new File(context.getFilesDir() + "/" + newDirName);
            if (!dir.exists()) {
                dir.mkdir();
            }
            output = new File(context.getFilesDir() + "/" + newDirName + "/" + name);
        } else {
            output = new File(context.getFilesDir() + "/" + name);
        }
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(output);
            int read;

            byte[] buffer;
            if (DeviceUtils.totalMemoryCapacity(context) < 4L * 1024 * 1024 * 1024)
                buffer = new byte[64 * 1024];
            else
                buffer = new byte[128 * 1024];

            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            inputStream.close();
            outputStream.close();

        } catch (Exception e) {

            Log.e("Exception", Objects.requireNonNull(e.getMessage()));
        }

        return output.getPath();
    }

    private static String getFilePathForWhatsApp(Context context, Uri uri) {
        return copyFileToInternalStorage(context, uri, "whatsapp");
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        if (uri == null) return null;

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);

            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static boolean isWhatsAppFile(Uri uri) {
        return "com.whatsapp.provider.media".equals(uri.getAuthority());
    }

    private static boolean isGoogleDriveUri(Uri uri) {
        return "com.google.android.apps.docs.storage".equals(uri.getAuthority()) || "com.google.android.apps.docs.storage.legacy".equals(uri.getAuthority());
    }

    public static void writeToFile(String data, File file, Context context) {
        try {
            FileOutputStream fileOutStream = new FileOutputStream(file);
            OutputStreamWriter outputWriter = new OutputStreamWriter(fileOutStream);
            outputWriter.write(data);
            outputWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String readFromFile(Context context, File file) {
        String contents;
        try {
            int length = (int) file.length();

            byte[] bytes = new byte[length];

            FileInputStream in = new FileInputStream(file);
            try {
                in.read(bytes);
            } finally {
                in.close();
            }

            contents = new String(bytes);
        } catch (Exception e) {
            UIUtils.toastLong(context, e.toString());
            return "error";
        }
        return contents;
    }

    public static boolean isFileExists(String filePath) {
        File file = new File(filePath.replaceAll("\n", ""));
        return file.exists();
    }

    public static void copyFileFromUri(Context context, Uri sourceUri, String destFile) throws IOException {

        File file = new File(destFile);
        if (!Objects.requireNonNull(file.getParentFile()).exists()) {
            if (!file.getParentFile().mkdirs())
                throw new IOException("Failed to create directory: " + file.getParentFile().getAbsolutePath());
        }

        try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri); OutputStream outputStream = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[32 * 1024];
            if (DeviceUtils.totalMemoryCapacity(context) < 3L * 1024 * 1024 * 1024) {
                buffer = new byte[4 * 1024];
            } else if (DeviceUtils.totalMemoryCapacity(context) < 5L * 1024 * 1024 * 1024) {
                buffer = new byte[8 * 1024];
            } else if (DeviceUtils.totalMemoryCapacity(context) < 7L * 1024 * 1024 * 1024) {
                buffer = new byte[16 * 1024];
            }
            int bytesRead;
            while (true) {
                assert inputStream != null;
                if ((bytesRead = inputStream.read(buffer)) == -1) break;
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
        }
    }

    public static void copyFileToUri(
            Context context,
            String sourcePath,
            Uri uri
    ) throws Exception {

        InputStream in = new FileInputStream(sourcePath);
        OutputStream out = context.getContentResolver().openOutputStream(uri);
        byte[] buffer = new byte[32 * 1024];
        if (DeviceUtils.totalMemoryCapacity(context) < 3L * 1024 * 1024 * 1024) {
            buffer = new byte[4 * 1024];
        } else if (DeviceUtils.totalMemoryCapacity(context) < 5L * 1024 * 1024 * 1024) {
            buffer = new byte[8 * 1024];
        } else if (DeviceUtils.totalMemoryCapacity(context) < 7L * 1024 * 1024 * 1024) {
            buffer = new byte[16 * 1024];
        }
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }


    public static String getFileNameFromUri(Context context, Uri uri) {
        String result = null;
        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(
                    uri,
                    null,
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                result = cursor.getString(nameIndex);
            }

            if (cursor != null) {
                cursor.close();
            }
        } catch (SecurityException | IllegalArgumentException ignored) {

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (result == null) {
            String path = uri.getLastPathSegment();
            if (path != null) {
                int cut = path.lastIndexOf('/');
                result = cut != -1 ? path.substring(cut + 1) : path;
            }
        }

        return result;
    }

    public static boolean createDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            return dir.mkdirs();
        }
        return true;
    }

    public static boolean rename(String path, String newName) {
        if (!isFileExists(path) || newName.isEmpty()) return false;
        String parent = new File(path).getParent();
        if (parent == null) return false;
        return move(path, parent + "/" + newName);
    }

    public static boolean moveToFolder(String src, String dst) {
        createDirectory(dst);
        return move(src, dst + "/" + new File(src).getName());
    }

    public static boolean move(String oldPath, String newPath) {
        if (!new File(oldPath).exists() || newPath.isEmpty()) return false;
        createDirectory(new File(newPath).getParent());
        File src = new File(oldPath);
        File dst = new File(newPath);
        if (dst.getParentFile() == null || !dst.getParentFile().exists()) return false;
        Log.d(TAG, "move: " + oldPath + " to " + newPath);
        if (!src.renameTo(dst)) {
            try {
                Log.d(TAG, "move: Moving with copy and delete.");
                copy(new File(oldPath), new File(newPath));
                return delete(new File(oldPath));
            } catch (Exception e) {
                Log.e(TAG, "move: ", e);
            }
        }
        return true;
    }

    static void copy(File src, File dst) throws IOException {
        if (!src.exists()) return;
        if (src.isDirectory()) {
            if (!dst.exists()) dst.mkdirs();
            File[] files = src.listFiles();
            if (files != null) {
                for (File f : files) {
                    copy(f, new File(dst, f.getName()));
                }
            }
        } else {
            if (dst.getParentFile() != null && !dst.getParentFile().exists()) {
                createDirectory(dst.getParent());
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                FileInputStream inStream = new FileInputStream(src);
                FileOutputStream outStream = new FileOutputStream(dst);

                byte[] buffer = new byte[8192];
                int length;
                while ((length = inStream.read(buffer))
                        > 0) {
                    outStream.write(buffer, 0, length);
                }

                inStream.close();
                outStream.close();
            }
        }
    }

    static boolean copyFile(String filePath, String destFolderPath) {
        File file = new File(filePath);

        if (!file.exists()) return false;

        try {
            copy(file, new File(destFolderPath, file.getName()));
            return true;
        } catch (Exception ignored) {

        }

        return false;
    }

    public static boolean delete(String path) {
        try {
            return delete(new File(path));
        } catch (Exception ignored) {

        }

        return false;
    }

    public static boolean delete(File file) {
        if (!file.exists()) return true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Files.isSymbolicLink(file.toPath())) {
                try {
                    return Files.deleteIfExists(file.toPath());
                } catch (IOException e) {
                    return false;
                }
            }
        }

        if (file.isDirectory()) {
            String[] children = file.list();

            if (children == null) {
                Log.e(TAG, "delete: Try deleteFolderHard " + file);
                return deleteFolderHard(file);
            }

            for (int i = 0; i < children.length; i++) {
                File temp = new File(file, children[i]);
                delete(temp);
            }
        }
        boolean success = file.delete();
        if (!success) {
            Log.e(TAG, "delete: Try deleteFolderHard " + file);
            success = deleteFolderHard(file);
        }

        return success;
    }

    public static boolean deleteFolderHard(File file) {
        try {
            String command = "rm -rf " + file.getAbsolutePath();
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean canRead(String filePath) {
        File file = new File(filePath);
        return file.canRead();
    }

    public static String readAFile(String filePath) {
        if (!FileUtils.isFileExists(filePath)) return "";

        StringBuilder content = new StringBuilder();
        try (FileInputStream inputStream = new FileInputStream(filePath);
             BufferedReader reader = new BufferedReader(new
                     InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return content.toString();

    }

    public static boolean writeToFile(String folderPath, String fileName, String content) {
        File vDir = new File(folderPath);
        if (!vDir.exists()) {
            if (!vDir.mkdirs()) return false;
        }
        File file = new File(folderPath, fileName);
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file);
            outputStream.write(content.getBytes());
            outputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "writeToFile: ", e);
            return false;
        }
        return true;
    }

    public static void getAListOfAllFilesAndFoldersInADirectory(String path, ArrayList<String> list) {
        File dir = new File(path);
        if (!dir.exists() || dir.isFile()) return;

        File[] listFiles = dir.listFiles();
        if (listFiles == null || listFiles.length <= 0) return;

        if (list == null) return;
        list.clear();
        for (File file : listFiles) {
            list.add(file.getAbsolutePath());
        }
    }

    public static int getFileSize(String _path) {
        try {
            File file = new File(_path);
            if (!file.exists()) {
                return 0;
            }
            return (int) file.length();
        } catch (Exception _e) {
            return 0;
        }
    }

    public static long getFolderSize(String _path) {
        try {
            File file = new File(_path);
            if (!file.exists()) {
                return 0;
            }
            if (!file.isDirectory()) {
                return (int) file.length();
            }
            final List<File> dirs = new LinkedList<>();
            dirs.add(file);
            long result = 0;
            while (!dirs.isEmpty()) {
                final File dir = dirs.remove(0);
                if (!dir.exists()) {
                    continue;
                }
                final File[] listFiles = dir.listFiles();
                if (listFiles == null || listFiles.length == 0) {
                    continue;
                }
                for (final File child : listFiles) {
                    result += child.length();
                    if (child.isDirectory()) {
                        dirs.add(child);
                    }
                }
            }
            return result;
        } catch (Exception _e) {
            return 0;
        }
    }

    public static String getFilePathFromUri(Context context, Uri uri) {
        String filePath = null;
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {MediaStore.Files.FileColumns.DATA};
            try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
                    filePath = cursor.getString(index);
                }
            } catch (Exception e) {
                Log.e(TAG, "getFilePathFromUri: ", e);
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            filePath = uri.getPath();
        }
        return filePath;
    }

    public static boolean isValidFilePath(Activity activity, String filePath, boolean isShowDialog) {
        if (filePath == null || filePath.isEmpty()) {
            if (isShowDialog) {
                DialogUtils.oneDialog(activity,
                        activity.getString(R.string.problem_has_been_detected),
                        activity.getString(R.string.invalid_file_path_content),
                        activity.getString(R.string.ok),
                        true,
                        R.drawable.folder_24px,
                        true,
                        null,
                        null);
            }
            return false;
        }
        return true;
    }

    public static void openFolder(Context context, String folderPath) {
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory() || folderPath.equals(Environment.getExternalStorageDirectory().toString())) {
            DialogUtils.oneDialog(
                    context,
                    context.getString(R.string.oops),
                    context.getString(R.string.directory_does_not_exist),
                    context.getString(R.string.ok),
                    true,
                    R.drawable.error_96px,
                    true,
                    null,
                    null
            );
            Log.e(TAG, "openFolder: Folder not found!");
            return;
        }

        Uri uri;

        try {
            uri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".provider",
                    folder
            );
        } catch (IllegalArgumentException e) {
            DialogUtils.oneDialog(
                    context,
                    context.getString(R.string.oops),
                    context.getString(R.string.this_folder_cannot_be_opened),
                    context.getString(R.string.ok),
                    true,
                    R.drawable.error_96px,
                    true,
                    null,
                    null
            );
            Log.e(TAG, "openFolder: Folder not found!");
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "resource/folder");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            DialogUtils.oneDialog(
                    context,
                    context.getString(R.string.oops),
                    context.getString(R.string.there_is_no_app_to_perform_this_action),
                    context.getString(R.string.ok),
                    true,
                    R.drawable.error_96px,
                    true,
                    null,
                    null
            );
            Log.e(TAG, "openFolder: " + e.getMessage());
        }
    }

    public static boolean symlink(String targetPath, String linkPath) {
        File target = new File(targetPath);
        File linkFile = new File(linkPath);

        if (target.exists()) {
            if (linkFile.exists() && !linkFile.delete()) return false;
            try {
                Os.symlink(target.getAbsolutePath(), linkFile.getAbsolutePath());
                Log.d(TAG, "Symlink: " + linkFile.getAbsolutePath() + " → " + target.getAbsolutePath());
            } catch (ErrnoException e) {
                Log.e(TAG, "Symlink failed: " + e.getMessage());
                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    public static String getMd5(String filePath) {
        try (InputStream inputStream = new FileInputStream(filePath)) {
            byte[] buffer = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance("MD5");
            int numRead = 0;
            while (numRead != -1) {
                numRead = inputStream.read(buffer);
                if (numRead > 0)
                    digest.update(buffer, 0, numRead);
            }
            byte[] md5Bytes = digest.digest();
            return convertHashToString(md5Bytes);
        } catch (Exception e) {
            return null;
        }
    }

    private static String convertHashToString(byte[] md5Bytes) {
        String returnVal = "";
        for (byte md5Byte : md5Bytes) {
            returnVal += Integer.toString((md5Byte & 0xff) + 0x100, 16).substring(1);
        }
        return returnVal.toLowerCase();
    }
}
