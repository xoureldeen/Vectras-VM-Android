package com.vectras.vm.main.vms;

import com.google.gson.annotations.SerializedName;

public class DataMainRoms {
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

    public String itemCpu = "";

    @SerializedName(
            value = "drive",
            alternate = { "imgPath" }
    )
    public String itemPath = "";

    public String itemDrv1 = "";

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

    public String vmID = "";

    public int qmpPort = 0;

    public int bootFrom = 0;

    public boolean isShowBootMenu = false;
}
