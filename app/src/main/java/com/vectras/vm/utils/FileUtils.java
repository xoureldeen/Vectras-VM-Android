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
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.vectras.vm.MainActivity;
import com.vectras.vm.AppConfig;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * 
 * @author dev
 */
public class FileUtils {
	@NonNull
	public static File getExternalFilesDirectory(Context context) {
		return new File(Environment.getExternalStorageDirectory(), "Documents/VectrasVM");
	}

	public static void chmod(File file, int mode) {
		try {
			Os.chmod(file.getAbsolutePath(), mode);
		}
		catch (ErrnoException e) {}
	}

	private static Uri contentUri = null;

	@SuppressLint("NewApi")
	public static String getPath(Context context, final Uri uri) {
		// check here to KITKAT or new version
		final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
		String selection = null;
		String[] selectionArgs = null;
		// DocumentProvider
		if (isKitKat ) {
			// ExternalStorageProvider

			if (isExternalStorageDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				String fullPath = getPathFromExtSD(split);
				if (fullPath != "") {
					return fullPath;
				} else {
					return null;
				}
			}


			// DownloadsProvider

			if (isDownloadsDocument(uri)) {

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					final String id;
					Cursor cursor = null;
					try {
						cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null);
						if (cursor != null && cursor.moveToFirst()) {
							String fileName = cursor.getString(0);
							String path = Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
							if (!TextUtils.isEmpty(path)) {
								return path;
							}
						}
					}
					finally {
						if (cursor != null)
							cursor.close();
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
								final Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id));


								return getDataColumn(context, contentUri, null, null);
							} catch (NumberFormatException e) {
								//In Android 8 and Android P the id is not a number
								return uri.getPath().replaceFirst("^/document/raw:", "").replaceFirst("^raw:", "");
							}
						}


					}
				}
				else {
					final String id = DocumentsContract.getDocumentId(uri);

					if (id.startsWith("raw:")) {
						return id.replaceFirst("raw:", "");
					}
					try {
						contentUri = ContentUris.withAppendedId(
								Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
					}
					catch (NumberFormatException e) {
						e.printStackTrace();
					}
					if (contentUri != null) {

						return getDataColumn(context, contentUri, null, null);
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

			if(isWhatsAppFile(uri)){
				return getFilePathForWhatsApp(context, uri);
			}


			if ("content".equalsIgnoreCase(uri.getScheme())) {

				if (isGooglePhotosUri(uri)) {
					return uri.getLastPathSegment();
				}
				if (isGoogleDriveUri(uri)) {
					return getDriveFilePath(context, uri);
				}
				if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
				{

					// return getFilePathFromURI(context,uri);
					return copyFileToInternalStorage(context, uri,"userfiles");
					// return getRealPathFromURI(context,uri);
				}
				else
				{
					return getDataColumn(context, uri, null, null);
				}

			}
			if ("file".equalsIgnoreCase(uri.getScheme())) {
				return uri.getPath();
			}
		}
		else {

			if(isWhatsAppFile(uri)){
				return getFilePathForWhatsApp(context, uri);
			}

			if ("content".equalsIgnoreCase(uri.getScheme())) {
				String[] projection = {
						MediaStore.Images.Media.DATA
				};
				Cursor cursor = null;
				try {
					cursor = context.getContentResolver()
							.query(uri, projection, selection, selectionArgs, null);
					int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
					if (cursor.moveToFirst()) {
						return cursor.getString(column_index);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
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
		if (fileExists(fullPath)) {
			return fullPath;
		}

		return fullPath;
	}

	private static String getDriveFilePath(Context context, Uri uri) {
		Uri returnUri = uri;
		Cursor returnCursor = context.getContentResolver().query(returnUri, null, null, null, null);
		/*
		 * Get the column indexes of the data in the Cursor,
		 *     * move to the first row in the Cursor, get the data,
		 *     * and display it.
		 * */
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
			int maxBufferSize = 1 * 1024 * 1024;
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
			Log.e("Exception", e.getMessage());
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
		Uri returnUri = uri;

		Cursor returnCursor = context.getContentResolver().query(returnUri, new String[]{
				OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE
		}, null, null, null);


		/*
		 * Get the column indexes of the data in the Cursor,
		 *     * move to the first row in the Cursor, get the data,
		 *     * and display it.
		 * */
		int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
		int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
		returnCursor.moveToFirst();
		String name = (returnCursor.getString(nameIndex));
		String size = (Long.toString(returnCursor.getLong(sizeIndex)));

		File output;
		if(!newDirName.equals("")) {
			File dir = new File(context.getFilesDir() + "/" + newDirName);
			if (!dir.exists()) {
				dir.mkdir();
			}
			output = new File(context.getFilesDir() + "/" + newDirName + "/" + name);
		}
		else{
			output = new File(context.getFilesDir() + "/" + name);
		}
		try {
			InputStream inputStream = context.getContentResolver().openInputStream(uri);
			FileOutputStream outputStream = new FileOutputStream(output);
			int read = 0;
			int bufferSize = 1024;
			final byte[] buffers = new byte[bufferSize];
			while ((read = inputStream.read(buffers)) != -1) {
				outputStream.write(buffers, 0, read);
			}

			inputStream.close();
			outputStream.close();

		}
		catch (Exception e) {

			Log.e("Exception", e.getMessage());
		}

		return output.getPath();
	}

	private static String getFilePathForWhatsApp(Context context, Uri uri){
		return  copyFileToInternalStorage(context, uri,"whatsapp");
	}

	private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
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
		}
		finally {
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

	public static boolean isWhatsAppFile(Uri uri){
		return "com.whatsapp.provider.media".equals(uri.getAuthority());
	}

	private static boolean isGoogleDriveUri(Uri uri) {
		return "com.google.android.apps.docs.storage".equals(uri.getAuthority()) || "com.google.android.apps.docs.storage.legacy".equals(uri.getAuthority());
	}


	public String LoadFile(Activity activity, String fileName, boolean loadFromRawFolder) throws IOException {
		// Create a InputStream to read the file into
		InputStream iS;
		if (loadFromRawFolder) {
			// get the resource id from the file name
			int rID = activity.getResources().getIdentifier(getClass().getPackage().getName() + ":raw/" + fileName,
					null, null);
			// get the file as a stream
			iS = activity.getResources().openRawResource(rID);
		} else {
			// get the file as a stream
			iS = activity.getResources().getAssets().open(fileName);
		}

		ByteArrayOutputStream oS = new ByteArrayOutputStream();
		byte[] buffer = new byte[iS.available()];
		int bytesRead = 0;
		while ((bytesRead = iS.read(buffer)) > 0) {
			oS.write(buffer);
		}
		oS.close();
		iS.close();

		// return the output stream as a String
		return oS.toString();
	}

	public static void saveFileContents(String dBFile, String machinesToExport) {
		// TODO Auto-generated method stub
		byteArrayToFile(machinesToExport.getBytes(), new File(dBFile));
	}

	public static void byteArrayToFile(byte[] byteData, File filePath) {

		try {
			FileOutputStream fos = new FileOutputStream(filePath);
			fos.write(byteData);
			fos.close();

		} catch (FileNotFoundException ex) {
			System.out.println("FileNotFoundException : " + ex);
		} catch (IOException ioe) {
			System.out.println("IOException : " + ioe);
		}

	}

	public static String getDataDir() {

		String dataDir = MainActivity.activity.getApplicationInfo().dataDir;
		PackageManager m = MainActivity.activity.getPackageManager();
		String packageName = MainActivity.activity.getPackageName();
		Log.v("VMExecutor", "Found packageName: " + packageName);

		if (dataDir == null) {
			dataDir = "/data/data/" + packageName;
		}
		return dataDir;
	}

	public static boolean fileValid(Context context, String path) {

		if (path == null || path.equals(""))
			return true;
		if (path.startsWith("content://") || path.startsWith("/content/")) {
			int fd = get_fd(context, path);
			if (fd <= 0)
				return false;
		} else {
			File file = new File(path);
			return file.exists();
		}
		return true;
	}

	public static HashMap<Integer, ParcelFileDescriptor> fds = new HashMap<Integer, ParcelFileDescriptor>();

	public static int get_fd(final Context context, String path) {
		int fd = 0;
		if (path == null)
			return 0;

		if (path.startsWith("/content") || path.startsWith("content://")) {
			path = path.replaceFirst("/content", "content:");

			try {
				ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(Uri.parse(path), "rw");
				fd = pfd.getFd();
				fds.put(fd, pfd);
			} catch (final FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(context, "Error: " + e, Toast.LENGTH_SHORT).show();
					}
				});
			}
		} else {
			try {
				File file = new File(path);
				if (!file.exists())
					file.createNewFile();
				ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_WRITE_ONLY);
				fd = pfd.getFd();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return fd;
	}

	public static int close_fd(int fd) {

		if (FileUtils.fds.containsKey(fd)) {
			ParcelFileDescriptor pfd = FileUtils.fds.get(fd);
			try {
				pfd.close();
				FileUtils.fds.remove(fd);
				return 0; // success for Native side
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		return -1;
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
		String contents = null;
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
	
	public static boolean moveFile(String oldfilename, String newFolderPath, String newFilename) {
		File folder = new File(newFolderPath);
		if (!folder.exists())
		folder.mkdirs();
		
		File oldfile = new File(oldfilename);
		File newFile = new File(newFolderPath, newFilename);
		
		if (!newFile.exists())
		try {
			newFile.createNewFile();
			} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return oldfile.renameTo(newFile);
	}

}
