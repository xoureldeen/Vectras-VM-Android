package com.vectras.vm.utils;

import android.app.Activity;

import com.vectras.vterm.Terminal;

public class CommandUtils {
    public static String createForSelectedMirror(boolean _https, String _url, String _beforemain) {
        String command = "echo \"\" > /etc/apk/repositories && sed -i -e \"1ihttps://xssFjnj58Id/yttGkok69Je/edge/testing\" /etc/apk/repositories && sed -i -e \"1ihttps://xssFjnj58Id/yttGkok69Je/v3.19/community\" /etc/apk/repositories && sed -i -e \"1ihttps://xssFjnj58Id/yttGkok69Je/v3.19/main\" /etc/apk/repositories";
        command = command.replaceAll("/yttGkok69Je", _beforemain);
        if (!_https)
            command = command.replaceAll("https://", "http://");
        return command.replaceAll("xssFjnj58Id", _url);
    }

    public static void run(String _command, boolean _isShowResult, Activity _activity) {
        Terminal vterm = new Terminal(_activity);
        vterm.executeShellCommand2(_command, _isShowResult, _activity);
    }
}
