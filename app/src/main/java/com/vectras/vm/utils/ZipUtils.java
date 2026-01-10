package com.vectras.vm.utils;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    public static final String TAG = "ZipUtils";
    public static int lastProgress = 0;
    public static double lastZipFileSize = 0;
    public static String lastErrorContent = "";

    public static void reset() {
        lastProgress = 0;
        lastZipFileSize = 0;
    }

    public static boolean extract(
            Context context,
            String fileZip,
            String destDir,
            TextView statusTextView,
            ProgressBar progressBar
    ) {
        if (MainSettingsManager.getCheckBeforeExtract(context)) {
            updateStatus(statusTextView, progressBar, context.getString(R.string.checking), 0);
            if (isZipFileCorrupted(context, fileZip)) return false;
        }

        try {
            File outdir = new File(destDir);
            if (!outdir.exists()) outdir.mkdirs();

            long totalSize = 0;
            ZipFile zipFile = new ZipFile(fileZip);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                totalSize += entry.getSize();
            }
            zipFile.close();

            long extractedSize = 0;

            ZipInputStream zin = new ZipInputStream(new FileInputStream(fileZip));
            ZipEntry entry;
            byte[] buffer;
            if (DeviceUtils.totalMemoryCapacity(context) < 4L * 1024 * 1024 * 1024)
                buffer = new byte[64 * 1024];
            else
                buffer = new byte[128 * 1024];

            while ((entry = zin.getNextEntry()) != null) {
                if (isAllowExtract(entry, destDir)) {
                    File newFile = new File(outdir, entry.getName());

                    if (entry.isDirectory()) {
                        newFile.mkdirs();
                        continue;
                    }

                    newFile.getParentFile().mkdirs();

                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    long fileExtracted = 0;

                    long lastProgress = -1;
                    while ((len = zin.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                        fileExtracted += len;
                        extractedSize += len;

                        long progress = (totalSize > 0) ? (extractedSize * 100 / totalSize) : 0;

                        if (progress > lastProgress) {
                            lastProgress = progress;
                            updateStatus(
                                    statusTextView,
                                    progressBar,
                                    (progress == 0 || progress > 100 ?
                                            context.getString(R.string.importing) :
                                            context.getString(R.string.completed) + " " + progress + "%")
                                            + "\n" + context.getString(R.string.please_stay_here),
                                    (int) progress
                            );
                        }
                    }
                    fos.close();
                    zin.closeEntry();
                }
            }
            zin.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "extract: ", e);
            lastErrorContent = e.toString();
            return false;
        }
    }

    public static boolean extract(
            Context context,
            Uri fileZip,
            String destDir,
            TextView statusTextView,
            ProgressBar progressBar
    ) {
        try {
            File outdir = new File(destDir);
            if (!outdir.exists()) outdir.mkdirs();
            long totalSize = 0;

            boolean sizeCalculation;

            if (MainSettingsManager.getSmartSizeCalculation(context)) {
                // >7GB
                sizeCalculation = DeviceUtils.totalMemoryCapacity(context) > 7L * 1024 * 1024 * 1024;
            } else {
                sizeCalculation = true;
            }

            //Can be skipped as it may not get the size and is time consuming.
            if (sizeCalculation) {
                InputStream inputStream = context.getContentResolver().openInputStream(fileZip);

                ZipInputStream sizeCounter = new ZipInputStream(new BufferedInputStream(inputStream));
                ZipEntry e;
                while ((e = sizeCounter.getNextEntry()) != null) {
                    if (isAllowExtract(e, destDir)) {
                        if (!e.isDirectory() && e.getSize() > 0)
                            totalSize += e.getSize();
                    }
                }
                //sizeCounter also closes the inputstream
                sizeCounter.close();
            } else {
                totalSize = -1;
            }

            long extractedSize = 0;

            InputStream inputStream2 = context.getContentResolver().openInputStream(fileZip);

            ZipInputStream zin = new ZipInputStream(new BufferedInputStream(inputStream2));
            ZipEntry entry;
            byte[] buffer;
            if (DeviceUtils.totalMemoryCapacity(context) < 4L * 1024 * 1024 * 1024)
                buffer = new byte[64 * 1024];
            else
                buffer = new byte[128 * 1024];

            while ((entry = zin.getNextEntry()) != null) {
                File newFile = new File(outdir, entry.getName());

                if (entry.isDirectory()) {
                    newFile.mkdirs();
                    continue;
                }

                newFile.getParentFile().mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                long fileExtracted = 0;

                long lastProgress = -1;
                while ((len = zin.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);

                    if (totalSize < 0) {
                        fileExtracted += len;
                        extractedSize += len;

                        long progress = (totalSize > 0) ? (extractedSize * 100 / totalSize) : 0;

                        if (progress > lastProgress) {
                            lastProgress = progress;
                            updateStatus(
                                    statusTextView,
                                    progressBar,
                                    (progress == 0 || progress > 100 ?
                                            context.getString(R.string.importing) :
                                            context.getString(R.string.completed) + " " + progress + "%")
                                            + "\n" + context.getString(R.string.please_stay_here),
                                    (int) progress
                            );
                        }
                    }
                }
                fos.close();
                zin.closeEntry();
            }
            //zin also closes the inputstream2
            zin.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "extract: ", e);
            lastErrorContent = e.toString();
            return false;
        }
    }

    public static boolean isZipFileCorrupted(Context context, String path) {
        try (ZipFile zip = new ZipFile(path)) {

            Enumeration<? extends ZipEntry> entries = zip.entries();
            byte[] buffer;
            if (DeviceUtils.totalMemoryCapacity(context) < 4L * 1024 * 1024 * 1024)
                buffer = new byte[64 * 1024];
            else if (DeviceUtils.totalMemoryCapacity(context) < 11L * 1024 * 1024 * 1024)
                buffer = new byte[128 * 1024];
            else
                buffer = new byte[256 * 1024];

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                try (InputStream is = zip.getInputStream(entry)) {
                    while (is.read(buffer) != -1) {

                    }
                }
            }

            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean compress(
            Context context,
            String[] filePaths,
            String outputZip,
            TextView statusTextView,
            ProgressBar progressBar
    ) {
        try {
            try (FileOutputStream fos = new FileOutputStream(outputZip);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                compressCore(context, filePaths, zos, statusTextView, progressBar);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "compress: ", e);
            lastErrorContent = e.toString();
            return false;
        }
    }

    public static boolean compress(
            Context context,
            String[] filePaths,
            Uri outputZip,
            TextView statusTextView,
            ProgressBar progressBar
    ) {
        try {
            try (OutputStream os = context.getContentResolver().openOutputStream(outputZip);
                 ZipOutputStream zos = new ZipOutputStream(os)) {

                compressCore(context, filePaths, zos, statusTextView, progressBar);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "compress: ", e);
            lastErrorContent = e.toString();
            return false;
        }
    }

    public static void compressCore(
            Context context,
            String[] filePaths,
            ZipOutputStream zos,
            TextView statusTextView,
            ProgressBar progressBar) throws Exception {

        long totalBytes = 0;
        for (String path : filePaths) {
            File f = new File(path);
            if (f.isFile()) totalBytes += f.length();
        }

        long bytesWritten = 0;
        byte[] buffer;

        for (String filePath : filePaths) {
            File file = new File(filePath);
            long size = file.length();
            long crc = calculateCrc(context, file);

            try (FileInputStream fis = new FileInputStream(file)) {
                ZipEntry entry = new ZipEntry(file.getName());

                entry.setMethod(ZipEntry.DEFLATED);
                entry.setSize(size);

                if (MainSettingsManager.getCyclicRedundancyCheck(context))
                    entry.setCrc(crc);

                zos.putNextEntry(entry);

                if (DeviceUtils.totalMemoryCapacity(context) < 4L * 1024 * 1024 * 1024)
                    buffer = new byte[64 * 1024];
                else
                    buffer = new byte[128 * 1024];

                int len;
                long lastProgress = -1;
                while ((len = fis.read(buffer)) != -1) {
                    zos.write(buffer, 0, len);
                    bytesWritten += len;

                    final int progress = (int) ((bytesWritten * 100L) / totalBytes);

                    if (progress > lastProgress) {
                        lastProgress = progress;
                        updateStatus(
                                statusTextView,
                                progressBar,
                                (totalBytes > 0 && (progress == 0 || progress > 100) ?
                                        context.getString(R.string.exporting) :
                                        context.getString(R.string.completed) + " " + progress + "%")
                                        + "\n" + context.getString(R.string.please_stay_here),
                                progress
                        );
                    }
                }

                zos.closeEntry();
            }
        }
    }

    private static void updateStatus(TextView statusTextView, ProgressBar progressbar, String msg, int progress) {
        if (!(statusTextView == null || statusTextView.getContext() == null)) {
            ((Activity) statusTextView.getContext()).runOnUiThread(() -> statusTextView.setText(msg));
        }

        if (!(progressbar == null || progressbar.getContext() == null)) {
            ((Activity) progressbar.getContext()).runOnUiThread(() -> {
                progressbar.setIndeterminate(progress == 0 || progress > 100);

                if (progressbar.getMax() != 100) progressbar.setMax(100);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    progressbar.setProgress(progress, true);
                } else {
                    progressbar.setProgress(progress);
                }
            });
        }
    }

    public static long calculateCrc(Context context, File file) throws IOException {
        CRC32 crc = new CRC32();
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer;
            if (DeviceUtils.totalMemoryCapacity(context) < 4L * 1024 * 1024 * 1024)
                buffer = new byte[64 * 1024];
            else
                buffer = new byte[128 * 1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                crc.update(buffer, 0, len);
            }
        }
        return crc.getValue();
    }

    public static boolean isAllowExtract(ZipEntry entry, String targetDir) throws IOException {
        String entryName = entry.getName();

        File destDir = new File(targetDir);
        File outFile = new File(destDir, entryName);

        String destPath = destDir.getCanonicalPath();
        String outPath  = outFile.getCanonicalPath();

        if (!outPath.startsWith(destPath + File.separator)) {
            Log.w(TAG, "ZipSlip detected: " + entryName);
            return false;
        }
        return true;
    }
}
