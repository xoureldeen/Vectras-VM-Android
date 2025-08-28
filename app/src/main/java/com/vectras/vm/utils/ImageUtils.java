package com.vectras.vm.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtils {
    public static final String TAG = "ImageUtils";

    public static void convertToPng(Context context, Uri sourceUri, String outputFullPath) throws IOException {
        File outputFile = new File(outputFullPath);

        if (!outputFile.getParentFile().exists()) {
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

}
