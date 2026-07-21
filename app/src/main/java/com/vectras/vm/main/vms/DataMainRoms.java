package com.vectras.vm.main.vms;

import com.google.gson.annotations.SerializedName;

public class DataMainRoms {
    public String vmID = "";

    @SerializedName(
            value = "arch",
            alternate = { "imgArch" }
    )
    public String itemArch = "";

    // Personalize

    @SerializedName(
            value = "icon",
            alternate = { "imgIcon" }
    )
    public String itemIcon = "";

    @SerializedName(
            value = "title",
            alternate = { "imgName" }
    )
    public String itemName = "";

    // Board
    public int machine = 0;
    public boolean nvirt; // Nested virtualization

    public int cpu = 0;
    public int cores = 0;
    public int threads = 0;

    // Dummy devices

    public boolean battery;
    public boolean wifi;

    // Storage

    @SerializedName(
            value = "drive",
            alternate = { "imgPath" }
    )
    public String itemPath = "";

    public String hd1 = "";


    @SerializedName(
            value = "cdrom",
            alternate = { "imgCdrom" }
    )
    public String imgCdrom = "";

    public String cdrom1 = "";


    public String fda = "";

    public String fdb = "";


    public boolean sharedFolder;

    // Network

    public int networkCard = 0;

    // Firmware

    public int bootFrom = 0;

    public boolean isShowBootMenu;

    public boolean isUseLocalTime = true;

    public boolean isUseUefi;

    public boolean isUseDefaultBios = true;

    // Advanced

    @SerializedName(
            value = "qemu",
            alternate = { "imgExtra" }
    )
    public String itemExtra = "";

    // Deprecated

    @Deprecated
    public int qmpPort = 0;
}
