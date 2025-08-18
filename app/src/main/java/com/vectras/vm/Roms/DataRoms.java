package com.vectras.vm.Roms;

import com.google.gson.annotations.SerializedName;

public class DataRoms {
    @SerializedName("rom_icon")
    public String romIcon;
    @SerializedName("rom_name")
    public String romName;
    @SerializedName("rom_arch")
    public String romArch;
    @SerializedName("rom_kernel")
    public String romKernel;
    @SerializedName("rom_avail")
	public Boolean romAvail;
    @SerializedName("rom_size")
    public String romSize;
    @SerializedName("rom_url")
    public String romUrl;
    @SerializedName("rom_path")
    public String romPath;
    @SerializedName("finalromfilename")
    public String finalromfilename;
    @SerializedName("rom_extra")
    public String romExtra;
    @SerializedName("desc")
    public String desc;
    @SerializedName("filesize")
    public String fileSize;
    @SerializedName("creator")
    public String creator;
    @SerializedName("verified")
    public String verified;
}
