package com.vectras.vm.home.core;

import static android.os.Build.VERSION.SDK_INT;
import static com.vectras.vm.utils.LibraryChecker.isPackageInstalled2;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;

import com.termux.app.TermuxService;
import com.vectras.qemu.Config;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.MainVNCActivity;
import com.vectras.vm.R;
import com.vectras.vm.core.ShellExecutor;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.x11.X11Activity;
import com.vectras.vterm.Terminal;

import java.util.HashSet;
import java.util.Set;

public class DisplaySystem {
    public static void launch(Activity activity) {
        if (MainSettingsManager.getVmUi(activity).equals("VNC")) {
            activity.startActivity(new Intent(activity, MainVNCActivity.class));
        } else if (MainSettingsManager.getVmUi(activity).equals("X11")) {
            DisplaySystem.launchX11(activity, false);
        }
    }

    public static void reLaunchVNC(Activity activity) {
        if (MainSettingsManager.getVmUi(activity).equals("VNC") &&
                FileUtils.isFileExists(Config.getLocalQMPSocketPath()) &&
                !activity.isFinishing() &&
                MainVNCActivity.started)
            activity.startActivity(new Intent(activity, MainVNCActivity.class));
    }

    public static void launchX11(Activity activity, boolean isKillXFCE) {
        if (SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            DialogUtils.oneDialog(
                    activity,
                    activity.getString(R.string.x11_feature_not_supported),
                    activity.getString(R.string.the_x11_feature_is_currently_not_supported_on_android_14_and_above_please_use_a_device_with_android_13_or_below_for_x11_functionality),
                    activity.getString(R.string.ok),
                    true, R.drawable.error_96px,
                    true,
                    null,
                    null
            );
        } else {
            // XFCE4 meta-package
            String xfce4Package = "xfce4";

            // Check if XFCE4 is installed
            isPackageInstalled2(activity, xfce4Package, (output, errors) -> {
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
                    DialogUtils.twoDialog(
                            activity,
                            "Install XFCE4",
                            "XFCE4 is not installed. Would you like to install it?",
                            activity.getString(R.string.install),
                            activity.getString(R.string.cancel),
                            true,
                            R.drawable.desktop_24px,
                            true,
                            () -> {
                                String installCommand = "apk add " + xfce4Package;
                                new Terminal(activity).executeShellCommand(installCommand, true, true, activity);
                            },
                            null,
                            null
                    );
                } else {
                    if (isKillXFCE)
                        new Terminal(activity).executeShellCommand2("killall xfce4-session", false, activity);
                    activity.startActivity(new Intent(activity, X11Activity.class));
                    new Terminal(activity).executeShellCommand2("xfce4-session", false, activity);
                }
            });
        }
    }

    public static void startTermuxX11() {
        if (Build.VERSION.SDK_INT < 34) {
            ShellExecutor shellExec = new ShellExecutor();
            shellExec.exec(TermuxService.PREFIX_PATH + "/bin/termux-x11 :0");
        }
    }
}