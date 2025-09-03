package com.vectras.vm.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
                    Log.e(TAG, "Failed to create folder: " + saveTo);
                    return;
                }
            }

            File file = new File(saveTo, fileName);
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to save bitmap to file: " + e.getMessage());
        }
    }

}
