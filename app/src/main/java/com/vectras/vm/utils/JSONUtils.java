package com.vectras.vm.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.vectras.vm.AppConfig;

import java.io.StringReader;
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

    public static boolean isValidArray(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        try {
            return JsonParser.parseString(content).isJsonArray();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidVmList() {
        try {
            String jsonRaw = FileUtils.readAFile(AppConfig.romsdatajson);
            ArrayList<HashMap<String, Object>> vmList = new Gson().fromJson(jsonRaw, new TypeToken<ArrayList<HashMap<String, Object>>>() {
            }.getType());
            return isValidArray(jsonRaw);
        } catch (Exception e) {
            return false;
        }
    }
}
