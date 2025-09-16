package com.vectras.vm.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class JSONUtils {
    public static boolean isValidFromString(String _content) {
        try {
            JsonElement element = JsonParser.parseString(_content);
            return element != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidFromFile(String _filepath) {
        if (FileUtils.isFileExists(_filepath)) {
            return isValidFromString(FileUtils.readAFile(_filepath));
        } else {
            return false;
        }
    }
}
