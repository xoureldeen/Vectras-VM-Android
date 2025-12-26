package com.vectras.vm.setupwizard;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.vectras.vm.AppConfig;
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
        if (isInstalledSystemFiles(context)) return true;
        lastErrorLog = "";

        String filesDir = context.getFilesDir().getAbsolutePath();
        File distroDir = new File(filesDir + "/distro");
        File binDir = new File(distroDir + "/bin");
        if (!binDir.exists()) {
            if (!isInstalledProot(context)) {
                if (!extractSystemFiles(context, "bootstrap", "")) return false;
            }

            if (isInstalledDistro(context)) return true;

            File tmpDir = new File(context.getFilesDir(), "usr/tmp");
            if (!tmpDir.isDirectory()) {
                if (tmpDir.mkdirs()) {
                    FileUtils.chmod(tmpDir, 0771);
                } else {
                    Log.e(TAG, "startExtractSystemFiles: Failed to create folder: tmp.");
                }
            }

            return extractSystemFiles(context, "alpine19", "distro");
        }
        return false;
    }

    public static boolean extractSystemFiles(Context context, String fromAsset, String extractTo) {
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
            Process process = null;
            try {
                process = Runtime.getRuntime().exec(cmdline);

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
                return exitCode == 0 && errorOutput.length() <= 0;
            } catch (IOException | InterruptedException e) {
                lastErrorLog = lastErrorLog.isEmpty() ? e.toString() : lastErrorLog + "\n" + e;
                Log.e(TAG, "extractSystemFiles: ", e);
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
        }
        return false;
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
