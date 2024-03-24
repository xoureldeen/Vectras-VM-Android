package com.undatech.opaque.proxmox.pojo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class PveRealm {

    private static final String TAG = "PveRealm";
    private String type;
    private String realm;
    private String tfa;
    private String comment;
    public PveRealm(JSONObject data) throws JSONException {
        if (!data.isNull("type"))
            type = data.getString("type");
        if (!data.isNull("realm"))
            realm = data.getString("realm");
        if (!data.isNull("tfa"))
            tfa = data.getString("tfa");
        if (!data.isNull("comment"))
            comment = data.getString("comment");
    }

    public static HashMap<String, PveRealm> getRealmsFromJsonArray(JSONArray array) throws JSONException {
        HashMap<String, PveRealm> result = new HashMap<String, PveRealm>();
        for (int i = 0; i < array.length(); i++) {
            PveRealm element = new PveRealm(array.getJSONObject(i));
            result.put(element.getRealm(), element);
        }
        return result;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getTfa() {
        return tfa;
    }

    public void setTfa(String tfa) {
        this.tfa = tfa;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
