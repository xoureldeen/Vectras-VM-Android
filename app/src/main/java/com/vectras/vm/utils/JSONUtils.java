package com.vectras.vm.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class JSONUtils {
    public static boolean isValidFromString(String _content) {
        ArrayList<HashMap<String, Objects>> mmap = new ArrayList<>();
        try {
            mmap.clear();
            mmap = new Gson().fromJson(_content, new TypeToken<ArrayList<HashMap<String, Object>>>() {
            }.getType());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isMapValidFromString(String _content) {
        HashMap<String, Object> mmap = new HashMap<>();
        try {
            mmap.clear();
            mmap= new Gson().fromJson(_content, new TypeToken<HashMap<String, Object>>(){}.getType());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidFromFile(String _filepath) {
        ArrayList<HashMap<String, Objects>> mmap = new ArrayList<>();
        String contentjson = "";
        if (FileUtils.isFileExists(_filepath)) {
            contentjson = FileUtils.readAFile(_filepath);
            try {
                mmap.clear();
                mmap = new Gson().fromJson(contentjson, new TypeToken<ArrayList<HashMap<String, Object>>>() {
                }.getType());
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            return false;
        }
    }
}
