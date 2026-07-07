package com.vectras.vm.utils;

import android.app.Activity;
import android.content.Context;

import com.vectras.vm.VectrasApp;
import com.vectras.vterm.Terminal;
import com.vectras.vterm.Terminal2;

public class CommandUtils {
    public static String createForSelectedMirror(boolean _https, String _url, String _beforemain) {
        String version = "v3.19";
        String command = "echo \"\" > /etc/apk/repositories && sed -i -e \"1ihttps://xssFjnj58Id/yttGkok69Je/"
                + version + "/community\" /etc/apk/repositories && sed -i -e \"1ihttps://xssFjnj58Id/yttGkok69Je/" + version + "/main\" /etc/apk/repositories";

        command = command.replaceAll("/yttGkok69Je", _beforemain);
        if (!_https)
            command = command.replaceAll("https://", "http://");
        return command.replaceAll("xssFjnj58Id", _url);
    }

    public static void run(String _command, boolean _isShowResult, Activity _activity) {
        new Terminal2(_activity).execute(_command);
    }

    public static String getQemuVersionName(Context context) {
        String qemuVersion = getQemuVersion(context);

        if (qemuVersion.toLowerCase().contains("failed") || qemuVersion.toLowerCase().contains("not found"))
            return "";

        return (qemuVersion.contains("Error") ? qemuVersion.substring(0, qemuVersion.indexOf("Error")) : qemuVersion) + (is3dfxVersion(context) ? " - 3dfx" : "");
    }

    public static String getQemuVersion(Context context) {
        return VectrasApp.getContext() == null ? "Unknow" : new Terminal2(context).executeOnThisThread("qemu-system-x86_64 --version | head -n1 | awk '{print $4}'").replaceAll("\n", "");
    }

    public static boolean is3dfxVersion(Context context) {
        return VectrasApp.getContext() != null && new Terminal2(context).executeOnThisThread("qemu-system-x86_64 --version").contains("3dfx");
    }
}
