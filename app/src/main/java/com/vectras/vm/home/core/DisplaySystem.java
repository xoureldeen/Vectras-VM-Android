package com.vectras.vm.home.core;

import static android.os.Build.VERSION.SDK_INT;
import static com.vectras.vm.utils.LibraryChecker.isPackageInstalled2;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.termux.app.TermuxService;
import com.vectras.qemu.Config;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.MainVNCActivity;
import com.vectras.vm.R;
import com.vectras.vm.core.ShellExecutor;
import com.vectras.vm.core.TermuxX11;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.PackageUtils;
import com.vectras.vm.x11.X11Activity;
import com.vectras.vterm.Terminal;

import java.util.HashSet;
import java.util.Set;

public class DisplaySystem {
    private static final String TAG = "DisplaySystem";

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

    public static void launchX11(Activity activity, boolean isKill) {
        if (!DeviceUtils.is64bit()) {
            DialogUtils.oneDialog(
                    activity,
                    activity.getString(R.string.x11_feature_not_supported),
                    activity.getString(R.string.cpu_not_support_64_xfce),
                    activity.getString(R.string.ok),
                    true, R.drawable.error_96px,
                    true,
                    null,
                    null
            );
        } else {
            if (SDK_INT >= 34 && !PackageUtils.isInstalled("com.termux.x11", activity)) {
                DialogUtils.needInstallTermuxX11(activity);
                return;
            }

            // XFCE4 meta-package
            String necessaryPackage = "fluxbox";

            // Check if XFCE4 is installed
            isPackageInstalled2(activity, necessaryPackage, (output, errors) -> {
                boolean isInstalled = false;

                // Check if the package exists in the installed packages output
                if (output != null) {
                    Set<String> installedPackages = new HashSet<>();
                    for (String installedPackage : output.split("\n")) {
                        installedPackages.add(installedPackage.trim());
                    }

                    isInstalled = installedPackages.contains(necessaryPackage.trim());
                }

                // If not installed, show a dialog to install it
                if (!isInstalled) {
                    DialogUtils.twoDialog(
                            activity,
                            activity.getString(R.string.action_needed),
                            activity.getString(R.string.the_required_package_is_not_installed_content),
                            activity.getString(R.string.install),
                            activity.getString(R.string.cancel),
                            true,
                            R.drawable.desktop_24px,
                            true,
                            () -> {
                                String installCommand = "apk add " + necessaryPackage;
                                new Terminal(activity).executeShellCommand(installCommand, true, true, activity.getString(R.string.just_a_moment), activity);
                            },
                            null,
                            null
                    );
                } else {
                    if (SDK_INT >= 34) {
//                        activity.startActivity(new Intent(activity, XServerActivity.class));
                        Intent intent = new Intent();
                        intent.setClassName("com.termux.x11", "com.termux.x11.MainActivity");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        activity.startActivity(intent);
                        try {
                            TermuxX11.main(new String[]{":0"});
                        } catch (Exception e) {
                            Log.e(TAG, "TermuxX11.main: ", e);
                        }
                    } else {
                        activity.startActivity(new Intent(activity, X11Activity.class));
                    }

                    if (isKill)
                        new Terminal(activity).executeShellCommand2("killall fluxbox", false, activity);
                    new Terminal(activity).executeShellCommand2(SDK_INT >= 34 ? "export DISPLAY=:0 && sleep 5 && fluxbox" : "fluxbox", false, activity);
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