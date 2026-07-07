package com.vectras.vm.main.core;

import static android.os.Build.VERSION.SDK_INT;
import static com.vectras.vm.utils.LibraryChecker.isPackageInstalled2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.termux.app.TermuxService;
import com.vectras.qemu.Config;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.MainVNCActivity;
import com.vectras.vm.R;
import com.vectras.vm.VectrasApp;
import com.vectras.vm.core.ShellExecutor;
import com.vectras.vm.core.TermuxX11;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.PackageUtils;
import com.vectras.vm.x11.X11Activity;
import com.vectras.vterm.Terminal;
import com.vectras.vterm.Terminal2;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class DisplaySystem {
    private static final String TAG = "DisplaySystem";
    private static boolean isTermuxClassLoaded = false;

    public static boolean isUseBuiltInX11() {
        //return SDK_INT < 34 && DeviceUtils.isArm();
        return !MainSettingsManager.getExternalX11(VectrasApp.getContext());
    }

    public static void launch(Context context) {
        if (MainSettingsManager.getVmUi(context).equals("VNC")) {
            context.startActivity(new Intent(context, MainVNCActivity.class));
        } else if (MainSettingsManager.getVmUi(context).equals("X11")) {
            DisplaySystem.launchX11(context, false);
        }
    }

    public static void reLaunchVNC(Activity activity) {
        if (MainSettingsManager.getVmUi(activity).equals("VNC") &&
                FileUtils.isFileExists(Config.getLocalQMPSocketPath()) &&
                !activity.isFinishing() &&
                MainVNCActivity.started)
            activity.startActivity(new Intent(activity, MainVNCActivity.class));
    }

    public static void launchX11(Context context, boolean isKill) {
        if (!isUseBuiltInX11() && !PackageUtils.isInstalled("com.termux.x11", context)) {
            DialogUtils.needInstallTermuxX11(context);
            return;
        }

        // XFCE4 meta-package
        String necessaryPackage = "fluxbox";

        // Check if XFCE4 is installed
        isPackageInstalled2(context, necessaryPackage, (output, errors) -> {
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
                        context,
                        context.getString(R.string.action_needed),
                        context.getString(R.string.the_required_package_is_not_installed_content),
                        context.getString(R.string.install),
                        context.getString(R.string.cancel),
                        true,
                        R.drawable.desktop_24px,
                        true,
                        () -> {
                            String installCommand = "apk add " + necessaryPackage + " && echo \"Installed: " + necessaryPackage + "\"";

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
                                            launchX11(context, isKill);
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
            } else {
                if (!isUseBuiltInX11() ) {
                    if (!PackageUtils.isInstalled("com.termux.x11", context)) {
                        DialogUtils.needInstallTermuxX11(context);
                        return;
                    }

                    Log.d(TAG, "launchX11: Opened: com.termux.x11.MainActivity.");
                    Intent intent = new Intent();
                    intent.setClassName("com.termux.x11", "com.termux.x11.MainActivity");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    context.startActivity(intent);

                    startTermuxX11(context);
                } else {
                    context.startActivity(new Intent(context, X11Activity.class));
                }
                startDesktop(context);
            }
        });
    }

    public static void startDesktop(Context context) {
        Terminal2 terminal2 = new Terminal2(context);
        terminal2.setDefaultShellBash();
        terminal2.execute("export DISPLAY=:0 && fluxbox > /dev/null");
    }

    public static void startTermuxX11(Context context) {
        if (isTermuxClassLoaded || !MainSettingsManager.getVmUi(context).equals("X11")) return;
        isTermuxClassLoaded = true;

        Log.d(TAG, "startTermuxX11...");
        if (isUseBuiltInX11()) {
            if (SDK_INT >= 34) {
                File loaderApk = new File(TermuxService.PREFIX_PATH + "/libexec/termux-x11/loader.apk");
                loaderApk.setWritable(false, false);
            }

            ShellExecutor shellExec = new ShellExecutor();
            shellExec.exec(TermuxService.PREFIX_PATH + "/bin/termux-x11 :0");
        } else {
            if (PackageUtils.isInstalled("com.termux.x11", context)){
                try {
                    TermuxX11.main(new String[]{":0"});
                } catch (Exception e) {
                    Log.e(TAG, "TermuxX11.main: ", e);
                }
            }
        }
    }
}