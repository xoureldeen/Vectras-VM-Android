package com.vectras.vm.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.vectras.vm.AppConfig;
import com.vectras.vm.R;
import com.vectras.vterm.Terminal2;

import java.util.HashSet;
import java.util.Set;

public class LibraryChecker {
    private Context context;

    public interface LibraryCheckerCallback {
        void onCommandCompleted(String output, String errors);
    }

    public LibraryChecker(Context context) {
        this.context = context;
    }

    public void checkMissingLibraries(Activity activity) {
        // List of required libraries
        String[] requiredLibraries = DeviceUtils.is64bit() ? AppConfig.neededPkgs().split(" ") : AppConfig.neededPkgs32bit().split(" ");

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
        DialogUtils.twoDialog(
                activity,
                activity.getString(R.string.missing_packages),
                activity.getString(R.string.missing_packages_content) + "\n\n" + missingLibraries,
                activity.getString(R.string.install),
                activity.getString(R.string.cancel),
                true,
                R.drawable.warning_48px,
                true,
                () -> {
                    // Create the installation command
                    String installCommand = "apk add " + missingLibraries.replace("\n", " ");

                    Terminal2 terminal2 = new Terminal2(context);
                    terminal2.setShowProgressDialog(true);
                    terminal2.execute(installCommand, new Terminal2.Terminal2Callback() {
                        @Override
                        public void onRunning(String command, String newLine) {
                            // Nothing to do.
                        }

                        @Override
                        public void onFinished(String command, String log, int status) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (status == terminal2.SUCCESS) {
                                    DialogUtils.oneDialog(
                                            activity,
                                            activity.getString(R.string.done),
                                            activity.getString(R.string.the_necessary_packages_have_been_installed),
                                            R.drawable.check_24px
                                    );
                                } else {
                                    DialogUtils.oopsDialog(context, log);
                                }
                            });
                        }

                        @Override
                        public void onError(String command, Exception exception) {
                            new Handler(Looper.getMainLooper()).post(() -> DialogUtils.oopsDialog(context, exception.getMessage()));
                        }
                    });
                },
                null,
                null
        );
    }

    // Method to check if the package is installed
    public void isPackageInstalled(String packageName, LibraryCheckerCallback callback) {
        String command = "apk info";

        Terminal2 terminal2 = new Terminal2(context);
        terminal2.execute(command, new Terminal2.Terminal2Callback() {
            @Override
            public void onRunning(String command, String newLine) {
                // Nothing to do.
            }

            @Override
            public void onFinished(String command, String log, int status) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) {
                        if (status == terminal2.SUCCESS) {
                            callback.onCommandCompleted(log, "");
                        } else {
                            callback.onCommandCompleted(log, log);
                        }
                    }
                });
            }

            @Override
            public void onError(String command, Exception exception) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onCommandCompleted("", exception.getMessage()));
            }
        });
    }

    // Method to check if the package is installed
    public static void isPackageInstalled2(Context context, String packageName, LibraryCheckerCallback callback) {
        String command = "apk info";

        Terminal2 terminal2 = new Terminal2(context);
        terminal2.execute(command, new Terminal2.Terminal2Callback() {
            @Override
            public void onRunning(String command, String newLine) {
                // Nothing to do.
            }

            @Override
            public void onFinished(String command, String log, int status) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) {
                        if (status == terminal2.SUCCESS) {
                            callback.onCommandCompleted(log, "");
                        } else {
                            callback.onCommandCompleted(log, log);
                        }
                    }
                });
            }

            @Override
            public void onError(String command, Exception exception) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onCommandCompleted("", exception.getMessage()));
            }
        });
    }
}
