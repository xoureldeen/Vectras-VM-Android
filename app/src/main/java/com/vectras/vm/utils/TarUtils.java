package com.vectras.vm.utils;

import android.util.Log;

import com.vectras.vm.VectrasApp;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TarUtils {
    private static final String TAG = "TarUtils";

    private TarUtils() {

    }

    public static void compress(String name, File... files) throws IOException {
        try (TarArchiveOutputStream out = getTarArchiveOutputStream(name)){
            for (File file : files){
                addToArchiveCompression(out, file, ".");
            }
        }
    }

    public static void decompress(String in, File out) throws IOException {
        try (TarArchiveInputStream fin = new TarArchiveInputStream(new FileInputStream(in))){
            TarArchiveEntry entry;
            while ((entry = fin.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                File curfile = new File(out, entry.getName());
                File parent = curfile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                IOUtils.copy(fin, new FileOutputStream(curfile));
            }
        }
    }

    private static TarArchiveOutputStream getTarArchiveOutputStream(String name) throws IOException {
        TarArchiveOutputStream taos = new TarArchiveOutputStream(new FileOutputStream(name));
        // TAR has an 8 gig file limit by default, this gets around that
        taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
        // TAR originally didn't support long file names, so enable the support for it
        taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        taos.setAddPaxHeadersForNonAsciiNames(true);
        return taos;
    }

    private static void addToArchiveCompression(TarArchiveOutputStream out, File file, String dir) throws IOException {
        String entry = dir + File.separator + file.getName();
        if (file.isFile()){
            out.putArchiveEntry(new TarArchiveEntry(file, entry));
            try (FileInputStream in = new FileInputStream(file)){
                IOUtils.copy(in, out);
            }
            out.closeArchiveEntry();
        } else if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null){
                for (File child : children){
                    addToArchiveCompression(out, child, entry);
                }
            }
        } else {
            System.out.println(file.getName() + " is not supported");
        }
    }

    public static boolean isAllowExtract(String tarFile) throws Exception {

        File cacheDir = VectrasApp.getContext().getCacheDir();
        File tarTestDir = new File(cacheDir, "tartest");

        if (!tarTestDir.exists() && !tarTestDir.mkdirs()) {
            Log.e(TAG, "isAllowExtract: Failed to create tartest directory!");
            return false;
        }

        File canonicalDestDir = tarTestDir.getCanonicalFile();

        long MAX_FILE_SIZE = 200L * 1024 * 1024;   // 200 MB
        long MAX_TOTAL_SIZE = 2000L * 1024 * 1024;   // 2000 MB
        int  MAX_FILE_COUNT = 10_000;

        long totalSize = 0;
        int fileCount = 0;

        InputStream in;

        if (tarFile.endsWith(".tar.gz")) {
            in = new GzipCompressorInputStream(new FileInputStream(tarFile));
        } else {
            in = new FileInputStream(tarFile);
        }

        try (TarArchiveInputStream tarIn = new TarArchiveInputStream( new BufferedInputStream(in))) {

            TarArchiveEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {

                String name = entry.getName();

                if (entry.isSymbolicLink() || entry.isLink()) {
                    Log.w(TAG, "Symlink in TAR: " + name);
                    return false;
                }

                if (!isSafePath(name, canonicalDestDir.getAbsolutePath())) {
                    return false;
                }

                long size = entry.getSize();
                if (size < 0 || size > MAX_FILE_SIZE) {
                    Log.w(TAG, "Invalid entry size: " + name);
                    return false;
                }

                totalSize += size;
                fileCount++;

                if (totalSize > MAX_TOTAL_SIZE || fileCount > MAX_FILE_COUNT) {
                    Log.w(TAG, "Tar bomb detected!");
                    return false;
                }
            }
        }

        return true;
    }


    public static boolean isSafePath(String name, String targetDir) throws IOException {
        if (name.startsWith("/") || name.startsWith("\\")) {
            Log.w(TAG, "Absolute path blocked: " + name);
            return false;
        }

        File destDir = new File(targetDir);
        File outFile = new File(destDir, name);

        String destPath = destDir.getCanonicalPath();
        String outPath  = outFile.getCanonicalPath();

        if (!outPath.startsWith(destPath + File.separator)) {
            Log.w(TAG, "TarSlip detected: " + name);
            return false;
        }
        return true;
    }
}