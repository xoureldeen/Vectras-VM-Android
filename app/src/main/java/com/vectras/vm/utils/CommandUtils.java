package com.vectras.vm.utils;

import android.app.Activity;

import com.vectras.vm.VectrasApp;
import com.vectras.vterm.Terminal;

public class CommandUtils {
    public static String createForSelectedMirror(boolean _https, String _url, String _beforemain) {
        String command = "echo \"\" > /etc/apk/repositories && sed -i -e \"1ihttps://xssFjnj58Id/yttGkok69Je/edge/testing\" /etc/apk/repositories && sed -i -e \"1ihttps://xssFjnj58Id/yttGkok69Je/"
                + (DeviceUtils.is64bit() ? "v3.22" : "v3.21") + "/community\" /etc/apk/repositories && sed -i -e \"1ihttps://xssFjnj58Id/yttGkok69Je/v3.22/main\" /etc/apk/repositories";

        command = command.replaceAll("/yttGkok69Je", _beforemain);
        if (!_https)
            command = command.replaceAll("https://", "http://");
        return command.replaceAll("xssFjnj58Id", _url);
    }

    public static void run(String _command, boolean _isShowResult, Activity _activity) {
        Terminal vterm = new Terminal(_activity);
        vterm.executeShellCommand2(_command, _isShowResult, _activity);
    }

    public static String getQemuVersionName() {
        String qemuVersion = getQemuVersion();

        if (qemuVersion.toLowerCase().contains("failed")) return "";

        return (qemuVersion.contains("Error") ? qemuVersion.substring(0, qemuVersion.indexOf("Error")) : qemuVersion) + (is3dfxVersion() ? " - 3dfx" : "");
    }

    public static String getQemuVersion() {
        return VectrasApp.getContext() == null ? "Unknow" : Terminal.executeShellCommandWithResult("qemu-system-x86_64 --version | head -n1 | awk '{print $4}'", VectrasApp.getContext()).replaceAll("\n", "");
    }

    public static boolean is3dfxVersion() {
        return VectrasApp.getContext() != null && Terminal.executeShellCommandWithResult("qemu-system-x86_64 --version", VectrasApp.getContext()).contains("3dfx");
    }
}
