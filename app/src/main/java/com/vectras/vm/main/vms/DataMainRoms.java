package com.vectras.vm.main.vms;

import com.google.gson.annotations.SerializedName;

public class DataMainRoms {
    public int cpu = 0;
    public int cores = 0;
    public int threads = 0;

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

    @SerializedName(
            value = "arch",
            alternate = { "imgArch" }
    )
    public String itemArch = "";

    @SerializedName(
            value = "drive",
            alternate = { "imgPath" }
    )
    public String itemPath = "";

    public String hd1 = "";

    @SerializedName(
            value = "qemu",
            alternate = { "imgExtra" }
    )
    public String itemExtra = "";

    @SerializedName(
            value = "cdrom",
            alternate = { "imgCdrom" }
    )
    public String imgCdrom = "";

    public String cdrom1 = "";

    public String fda = "";

    public String fdb = "";

    public boolean sharedFolder = false;

    public String vmID = "";

    public int qmpPort = 0;

    public int bootFrom = 0;

    public boolean isShowBootMenu = false;

    public boolean isUseLocalTime = true;

    public boolean isUseUefi = false;

    public boolean isUseDefaultBios = true;

    public boolean battery = false;
}
