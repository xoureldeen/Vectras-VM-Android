package com.vectras.vm.utils;

import android.app.Activity;
import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.vectras.vm.AppConfig;
import com.vectras.vm.R;
import com.vectras.vterm.Terminal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LibraryChecker {
    private Context context;

    public LibraryChecker(Context context) {
        this.context = context;
    }

    public void checkMissingLibraries(Activity activity) {
        // List of required libraries
        String[] requiredLibraries = AppConfig.neededPkgs.split(" ");

        // Get the list of installed packages
        isPackageInstalled(null, (output, errors) -> {
            // Split the installed packages output into an array and convert to a set for fast lookup
            Set<String> installedPackages = new HashSet<>();
            for (String installedPackage : output.split("\n")) {
                installedPackages.add(installedPackage.trim());
            }

            // StringBuilder to collect missing libraries
            StringBuilder missingLibraries = new StringBuilder();

            // Loop over required libraries and check if they're installed
            for (String lib : requiredLibraries) {
                if (!installedPackages.contains(lib.trim())) {
                    missingLibraries.append(lib).append("\n");
                }
            }

            // Show dialog if any libraries are missing
            if (missingLibraries.toString().trim().length() > 0) {
                showMissingLibrariesDialog(activity, missingLibraries.toString());
            } else {
                // show a dialog if all libraries are installed
                // showAllLibrariesInstalledDialog(activity);
            }
        });
    }

    // Method to show the missing libraries dialog
    private void showMissingLibrariesDialog(Activity activity, String missingLibraries) {
        new AlertDialog.Builder(activity, R.style.MainDialogTheme)
                .setTitle("Missing Libraries")
                .setMessage("The following libraries are missing:\n\n" + missingLibraries)
                .setCancelable(false)
                .setPositiveButton("Install", (dialog, which) -> {
                    // Create the install command
                    String installCommand = "apk add " + missingLibraries.replace("\n", " ");
                    new Terminal(context).executeShellCommand(installCommand, true, activity);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Method to show the "All Libraries Installed" dialog
    private void showAllLibrariesInstalledDialog(Activity activity) {
        new AlertDialog.Builder(activity, R.style.MainDialogTheme)
                .setTitle("All Libraries Installed")
                .setMessage("All required libraries are already installed.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Method to check if the package is installed
    public void isPackageInstalled(String packageName, Terminal.CommandCallback callback) {
        String command = "apk info";

        Terminal terminal = new Terminal(context);
        terminal.executeShellCommand(command, (Activity) context, (output, errors) -> {
            if (callback != null) {
                callback.onCommandCompleted(output, errors);
            }
        });
    }

    // Method to check if the package is installed
    public static void isPackageInstalled2(Activity activity, String packageName, Terminal.CommandCallback callback) {
        String command = "apk info";

        Terminal terminal = new Terminal(activity);
        terminal.executeShellCommand(command, activity, (output, errors) -> {
            if (callback != null) {
                callback.onCommandCompleted(output, errors);
            }
        });
    }

    public void checkAndInstallXFCE4(Activity activity) {
        // XFCE4 meta-package
        String xfce4Package = "xfce4";

        // Check if XFCE4 is installed
        isPackageInstalled(xfce4Package, (output, errors) -> {
            boolean isInstalled = false;

            // Check if the package exists in the installed packages output
            if (output != null) {
                Set<String> installedPackages = new HashSet<>();
                for (String installedPackage : output.split("\n")) {
                    installedPackages.add(installedPackage.trim());
                }

                isInstalled = installedPackages.contains(xfce4Package.trim());
            }

            // If not installed, show a dialog to install it
            if (!isInstalled) {
                showInstallDialog(activity, xfce4Package);
            } else {
                showAlreadyInstalledDialog(activity);
            }
        });
    }

    private void showInstallDialog(Activity activity, String packageName) {
        new AlertDialog.Builder(activity, R.style.MainDialogTheme)
                .setTitle("Install XFCE4")
                .setMessage("XFCE4 is not installed. Would you like to install it?")
                .setCancelable(false)
                .setPositiveButton("Install", (dialog, which) -> {
                    String installCommand = "apk add " + packageName;
                    new Terminal(context).executeShellCommand(installCommand, true, activity);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showAlreadyInstalledDialog(Activity activity) {
        new AlertDialog.Builder(activity, R.style.MainDialogTheme)
                .setTitle("XFCE4 Installed")
                .setMessage("XFCE4 is already installed on this system.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
