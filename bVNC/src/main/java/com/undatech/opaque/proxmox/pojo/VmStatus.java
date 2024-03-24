package com.undatech.opaque.proxmox.pojo;

import org.json.JSONException;
import org.json.JSONObject;

public class VmStatus {
    public static String STOPPED = "stopped";
    public static String RUNNING = "running";

    private String status;

    public VmStatus(JSONObject data) throws JSONException {
        status = data.getString("status");
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
