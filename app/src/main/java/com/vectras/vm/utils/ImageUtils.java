package com.vectras.vm.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Objects;

public class ImageUtils {
    public static final String TAG = "ImageUtils";

    public static void convertToPng(Context context, Uri sourceUri, String outputFullPath) throws IOException {
        File outputFile = new File(outputFullPath);

        if (!Objects.requireNonNull(outputFile.getParentFile()).exists()) {
            outputFile.getParentFile().mkdirs();
        }

        Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(sourceUri));
        if (bitmap == null) {
            Log.e(TAG, "Failed to decode bitmap from URI: " + sourceUri);
            return;
        }

        FileOutputStream out = new FileOutputStream(outputFile);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.flush();
        out.close();

    }

    public static void saveBitmapToPNGFile(Bitmap bitmap, String saveTo, String fileName) {
        try {
            File folder = new File(saveTo);
            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    Log.e(TAG, "saveBitmapToPNGFile: Failed to create folder: " + saveTo);
                    return;
                }
            }

            File file = new File(saveTo, fileName);
            FileOutputStream out = new FileOutputStream(file);

            if (bitmap == null) {
                Log.e(TAG, "saveBitmapToPNGFile: Bitmap is null.");
            } else {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }

            out.flush();
            out.close();
        } catch (IOException e) {
            Log.e(TAG, "saveBitmapToPNGFile: Failed to save bitmap to file: " + e.getMessage());
        }
    }


    public static Bitmap ppmToBitmap(File ppmFile) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(ppmFile, "r");

        String magic = raf.readLine();
        String dimensions = raf.readLine();
        String maxVal = raf.readLine();

        String[] dim = dimensions.split(" ");
        int width = Integer.parseInt(dim[0]);
        int height = Integer.parseInt(dim[1]);


        int pixelCount = width * height;
        byte[] raw = new byte[pixelCount * 3];
        raf.readFully(raw);
        raf.close();


        int[] pixels = new int[pixelCount];
        for (int i = 0; i < pixelCount; i++) {
            int r = raw[i * 3] & 0xFF;
            int g = raw[i * 3 + 1] & 0xFF;
            int b = raw[i * 3 + 2] & 0xFF;
            pixels[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    public static Uri saveToGallery(Context context, Bitmap bitmap, String fileName) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/VectrasVM");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        ContentResolver resolver = context.getContentResolver();
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        assert uri != null;
        try (OutputStream os = resolver.openOutputStream(uri)) {
            assert os != null;
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
        }

        values.clear();
        values.put(MediaStore.Images.Media.IS_PENDING, 0);
        resolver.update(uri, values, null, null);

        return uri;
    }
}
