package com.vectras.vm.manager;

import android.content.Context;
import android.net.Uri;

import com.vectras.qemu.Config;
import com.vectras.vm.AppConfig;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.ImageUtils;

import java.io.File;

public class VmActions {
    public static boolean takeScreenshot(Context context, boolean isSaveToGallery) {
        boolean isSaved = QmpSender.takeScreenshot();
        if (isSaved) {
            try {
                if (isSaveToGallery) {
                    Uri imageFile = ImageUtils.saveToGallery(
                            context,
                            ImageUtils.ppmToBitmap(new File(AppConfig.vmFolder + Config.vmID + "/screenshot.ppm")),
                            String.valueOf(System.currentTimeMillis())
                    );

                    FileUtils.copyFileFromUri(context, imageFile, AppConfig.vmFolder + Config.vmID + "/screenshot.png");
                } else {
                    ImageUtils.saveBitmapToPNGFile(
                            ImageUtils.ppmToBitmap(new File(AppConfig.vmFolder + Config.vmID + "/screenshot.ppm")),
                            AppConfig.vmFolder + Config.vmID,
                            "screenshot.png"
                    );
                }
            } catch (Exception e) {
                isSaved = false;
            }
        }

        FileUtils.delete(AppConfig.vmFolder + Config.vmID + "/screenshot.ppm");

        return isSaved;
    }
}
