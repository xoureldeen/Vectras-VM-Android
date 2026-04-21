package com.vectras.vm.setupwizard;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.vectras.vm.R;
import com.vectras.vm.VMManager;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Objects;

public class SetupFeatureCore {
    public static String TAG = "SetupFeatureCore";
    public static String lastErrorLog = "";

    public static boolean isInstalledSystemFiles(Context context) {
        return isInstalledProot(context) && isInstalledDistro(context);
    }

    public static boolean isInstalledProot(Context context) {
        return FileUtils.isFileExists(context.getFilesDir().getAbsolutePath() + "/usr/bin/proot");
    }

    public static boolean isInstalledDistro(Context context) {
        return FileUtils.isFileExists(context.getFilesDir().getAbsolutePath() + "/distro/bin/busybox");
    }

    public static boolean isInstalledQemu(Context context) {
        return FileUtils.isFileExists(context.getFilesDir().getAbsolutePath() + "/distro/usr/local/bin/qemu-system-x86_64") ||
                FileUtils.isFileExists(context.getFilesDir().getAbsolutePath() + "/distro/usr/bin/qemu-system-x86_64");
    }

    public static boolean startExtractSystemFiles(Context context) {
        String filesDir;

        if (isInstalledSystemFiles(context)) {
            return true;
        } else {
            filesDir = context.getFilesDir().getAbsolutePath();

            FileUtils.delete(filesDir + "/data");
            FileUtils.delete(filesDir + "/distro");
            FileUtils.delete(filesDir + "/usr");
        }

        lastErrorLog = "";

        File distroDir = new File(filesDir + "/distro");
        if (!distroDir.exists()) {
            if (!distroDir.mkdir()) {
                lastErrorLog = "Failed to create directory: " + filesDir + "/distro";
                return false;
            }
        }

        File binDir = new File(distroDir + "/bin");
        if (!binDir.exists()) {
            if (!isInstalledProot(context)) {
                if (!extractSystemFiles(context, "bootstrap", "", false)) {
                    if (!extractSystemFiles(context, "bootstrap", "", true)) return false;
                }

                /*if (!mkSymlinks(filesDir + "/usr/lib/")) {
                    lastErrorLog = "Failed to create symlinks.";
                    return false;
                }*/
            }

            if (isInstalledDistro(context)) {
                lastErrorLog = "Installed proot.";
                return true;
            }

            File tmpDir = new File(context.getFilesDir(), "usr/tmp");
            if (!tmpDir.isDirectory()) {
                if (tmpDir.mkdirs()) {
                    FileUtils.chmod(tmpDir, 0771);
                } else {
                    Log.e(TAG, "startExtractSystemFiles: Failed to create folder: tmp.");
                }
            }

            if (!extractSystemFiles(context, "alpine19", "distro", false)) {
                return extractSystemFiles(context, "alpine19", "distro", true);
            } else {
                return true;
            }
        }

        return false;
    }

    public static boolean extractSystemFiles(Context context, String fromAsset, String extractTo, boolean tryNoSameOwner) {
        String randomFileName = VMManager.startRamdomVMID();
        String filesDir = context.getFilesDir().getAbsolutePath();
        String abi = Build.SUPPORTED_ABIS[0];
        String assetPath = fromAsset + "/" + abi + ".tar";
        String extractedFilePath = filesDir + "/" + randomFileName + ".tar";
        File destDir = new File(filesDir + "/" + extractTo);
        if (!destDir.exists()) if (!destDir.mkdir()) Log.e(TAG, "extractSystemFiles: Unable to create folder " + filesDir + "/" + extractTo);

        boolean isCompleted;

        // Step 1: Copy asset to filesDir
        isCompleted = copyAssetToFile(context, assetPath, extractedFilePath);

        // Step 2: Run tar extraction
        if (isCompleted) {
            String[] cmdline = {"tar", "xf", extractedFilePath, "-C", filesDir + "/" + extractTo};
            String[] cmdline2 = {"tar", "xf", extractedFilePath, "-C", filesDir + "/" + extractTo, "--no-same-owner"};

            Process process = null;
            try {
                process = Runtime.getRuntime().exec(tryNoSameOwner ? cmdline2 : cmdline);

                // Capture standard error output (stderr)
                BufferedReader errorReader =
                        new BufferedReader(new InputStreamReader(process.getErrorStream()));
                StringBuilder errorOutput = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
                errorReader.close();

                // Wait for the process to complete
                int exitCode = process.waitFor();

                if (fromAsset.contains("alpine")) {
                    setDNS(context);
                }

                // If there was any output in stderr, treat it as an error
                if (exitCode == 0 && errorOutput.length() <= 0) {
                    return true;
                } else {
                    lastErrorLog = errorOutput.toString();
                    return false;
                }
            } catch (IOException | InterruptedException e) {
                lastErrorLog = lastErrorLog.isEmpty() ? e.toString() : lastErrorLog + "\n" + e;
                Log.e(TAG, "extractSystemFiles: ", e);
            } finally {
                FileUtils.delete(extractedFilePath);

                if (process != null) {
                    process.destroy();
                }
            }
        }
        return false;
    }

    public static boolean mkSymlinks(String libPath) {
        Log.d(TAG, "mkSymlinks: Creating...");
        return FileUtils.symlink(libPath + "libtalloc.so.2.4.2", libPath + "libtalloc.so.2") &&
                FileUtils.symlink(libPath + "libtalloc.so.2", libPath + "libtalloc.so");
    }

    public static void fixPermissions(String distroPath) {
        File binDir = new File(distroPath + "/bin");
        File usrBinDir = new File(distroPath + "/usr/bin");
        File sbinDir = new File(distroPath + "/sbin");
        File usrSbinDir = new File(distroPath + "/usr/sbin");

        for (File dir : new File[]{binDir, usrBinDir, sbinDir, usrSbinDir}) {
            if (dir.exists() && dir.listFiles() != null) {
                for (File f : dir.listFiles()) {
                    f.setExecutable(true, false);
                }
            }
        }
    }

    public static boolean copyAssetToFile(Context context, String assetPath, String outputPath) {
        try (InputStream in = context.getAssets().open(assetPath);
             OutputStream out = new FileOutputStream(outputPath)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            return true;
        } catch (IOException e) {
            lastErrorLog = e.toString();
            Log.e(TAG, "copyAssetToFile: ", e);
            return false;
        }
    }

    public static void setDNS(Context context) {
        String filesDir = context.getFilesDir().getAbsolutePath();
        File rootDir = new File(filesDir + "/distro/root");
        if (!rootDir.exists()) if(!rootDir.mkdir()) Log.e(TAG, "extractSystemFiles: Unable to create folder " + filesDir + "/distro/root");

        File resolv = new File(filesDir + "/distro/etc/resolv.conf");
        if(!Objects.requireNonNull(resolv.getParentFile()).mkdirs()) Log.e(TAG, "extractSystemFiles: Unable to add DNS.");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(resolv))) {
            writer.write("nameserver 1.1.1.1\n");
            writer.write("nameserver 1.0.0.1\n");
            writer.write("nameserver 8.8.8.8\n");
            writer.write("nameserver 8.8.4.4\n");
        } catch (IOException e) {
            Log.e(TAG, "extractSystemFiles: resolv: ", e);
        }
    }

    public static void checkabi(Context context) {
        if (!DeviceUtils.is64bit())
            DialogUtils.oneDialog((Activity) context,
                    context.getResources().getString(R.string.warning),
                    context.getResources().getString(R.string.cpu_not_support_64),
                    context.getString(R.string.ok),
                    true,
                    R.drawable.error_96px,
                    true,
                    null,
                    null);
    }
}
