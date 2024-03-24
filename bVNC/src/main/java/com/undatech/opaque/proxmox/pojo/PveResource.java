package com.undatech.opaque.proxmox.pojo;

import org.json.JSONException;
import org.json.JSONObject;

public class PveResource {
    private String node;
    private String type;
    private String id;
    private String vmid;
    private String name;
    public PveResource(JSONObject data) throws JSONException {
        if (!data.isNull("node"))
            node = data.getString("node");
        if (!data.isNull("type"))
            type = data.getString("type");
        if (!data.isNull("id"))
            id = data.getString("id");
        if (!data.isNull("vmid"))
            vmid = data.getString("vmid");
        if (!data.isNull("name"))
            name = data.getString("name");
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVmid() {
        return vmid;
    }

    public void setVmid(String vmid) {
        this.vmid = vmid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static class Types {
        public static String LXC = "lxc";
        public static String QEMU = "qemu";
        public static String OPENVZ = "openvz";
        public static String NODE = "node";
        public static String STORAGE = "storage";
    }
}
