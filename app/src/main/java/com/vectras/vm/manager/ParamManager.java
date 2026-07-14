package com.vectras.vm.manager;

import java.util.regex.Pattern;

public class ParamManager {
    public static boolean hasMachine(String param) {
        return Pattern.compile("(?<![\\w-])--?(M|machine)\\s+[a-zA-Z0-9._-]+").matcher(param).find();
    }

    public static boolean hasCpu(String param) {
        return Pattern.compile("(?<![\\w-])--?cpu\\s+[a-zA-Z0-9_-]+").matcher(param).find();
    }

    public static boolean hasSmp(String param) {
        return Pattern.compile("(?<![\\w-])--?smp\\s+[a-zA-Z0-9,=]+").matcher(param).find();
    }

    public static boolean hasMemory(String param) {
        return Pattern.compile("(?<![\\w-])--?m\\s+(\\d+)([a-zA-Z]*)").matcher(param).find();
    }
}
