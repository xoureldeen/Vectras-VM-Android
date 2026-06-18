package com.vectras.vm.manager;

import java.util.regex.Pattern;

public class ParamManager {
    public static boolean hasMemory(String param) {
        return Pattern.compile("(?<![\\\\w-])--?m\\s+(\\d+)([a-zA-Z]*)").matcher(param).find();
    }
}
