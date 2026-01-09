package com.vectras.vm.creator;

import android.content.Context;

import com.vectras.vm.R;

import java.util.ArrayList;
import java.util.HashMap;

public class ListManager {
    public static ArrayList<HashMap<String, Object>> bootFrom(Context context) {
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        putToList(list, context.getString(R.string.defaulttext), "");
        putToList(list, context.getString(R.string.hard_disk), "c");
        putToList(list, context.getString(R.string.cdrom), "d");
        putToList(list, context.getString(R.string.floppy_disk), "a");
        putToList(list, context.getString(R.string.network), "n");
        return list;
    }

    public static void putToList
            (
                    ArrayList<HashMap<String, Object>> listMap,
                    String name,
                    String value
            ) {
        HashMap<String, Object> thisItem = new HashMap<>();
        thisItem.put("name", name);
        thisItem.put("value", value);
        listMap.add(thisItem);
    }
}
