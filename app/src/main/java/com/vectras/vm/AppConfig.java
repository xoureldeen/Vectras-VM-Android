package com.vectras.vm;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.utils.DeviceUtils;

import java.io.File;
import java.util.Objects;

/**
 * @author dev
 */
public class AppConfig {

    // App Config
    public static String vectrasVersion = "3.5.0";
    public static int vectrasVersionCode = 54;
    public static final int standardSetupVersion = 9221;
    public static String vectrasWebsite = "https://vectras.vercel.app/";
    public static String vectrasWebsiteRaw = "https://raw.githubusercontent.com/AnBui2004/Vectras-VM-Emu-Android/refs/heads/master/web/";
    public static String bootstrapfileslink = vectrasWebsiteRaw + "/data/setupfiles3.json";
    public static String vectrasHelp = vectrasWebsite + "how.html";
    public static String community = vectrasWebsite + "community.html";
    public static String vectrasRaw = vectrasWebsiteRaw + "data/";
    public static String vectrasLicense = vectrasRaw + "LICENSE.md";
    public static String vectrasPrivacy = vectrasRaw + "PRIVACYANDPOLICY.md";
    public static String vectrasTerms = vectrasRaw + "TERMSOFSERVICE.md";
    public static String vectrasInfo = vectrasRaw + "info.md";
    public static String vectrasRepo = "https://github.com/xoureldeen/Vectras-VM-Android";
    public static String updateJson = vectrasRaw + "UpdateConfig.json";
    public static String blogJson = vectrasRaw + "news_list.json";
    // public static final String storeJson = vectrasRaw + "store_list.json";
    public static String storeJson = vectrasWebsiteRaw + "store_list.json";

    public static String releaseUrl = vectrasWebsite;

    public static String getSetupFiles() {
        String abi = Build.SUPPORTED_ABIS[0];
        return releaseUrl + "vectras-vm-" + abi + ".tar.gz";
    }

    public static String romsJson(Activity activity) {
        if (Objects.equals(MainSettingsManager.getArch(activity), "X86_64")) {
            return vectrasRaw + "roms-x86_64.json";
        } else if (Objects.equals(MainSettingsManager.getArch(activity), "I386")) {
            return vectrasRaw + "roms-i386.json";
        } else if (Objects.equals(MainSettingsManager.getArch(activity), "ARM64")) {
            return vectrasRaw + "roms-aarch64.json";
        } else if (Objects.equals(MainSettingsManager.getArch(activity), "PPC")) {
            return vectrasRaw + "roms-ppc.json";
        } else {
            return null;
        }
    }

    // App config
    public static String datadirpath(Context activity) {
        File f = new File(activity.getExternalFilesDir("data") + "/Vectras");
        return activity.getExternalFilesDir("data") + "/Vectras";
        //return FileUtils.getExternalFilesDirectory(activity).getPath();
    }
    public static String internalDataDirPath = "/data/data/com.vectras.vm/files/";
    public static String basefiledir = "";
    public static String maindirpath = "/sdcard/Documents/VectrasVM";
    public static String recyclebin = "";
    //public static String basefiledir = datadirpath(SplashActivity.activity) + "/.qemu/";
    //public static String maindirpath = FileUtils.getExternalFilesDirectory(SplashActivity.activity).getPath() + "/";
    public static String sharedFolder = maindirpath + "SharedFolder/";
    public static String downloadsFolder = maindirpath + "Downloads/";
    public static String romsdatajson = maindirpath + "roms-data.json";
    public static String vmFolder = maindirpath + "roms/";
    public static String importedDriveFolder = maindirpath + "drive/";
    public static String cvbiFolder = maindirpath + "cvbi/";
    public static String pendingCommand = "";

    public static String neededPkgs() {
        if (DeviceUtils.isArm()) {
            return "bash aria2 tar dwm xterm libslirp libslirp-dev pulseaudio-dev glib-dev pixman-dev zlib-dev spice-dev" +
                    " libusbredirparser usbredir-dev sdl2 sdl2-dev sdl2_image-dev libepoxy-dev virglrenderer-dev rdma-core fluxbox" +
                    " libusb libaio ncurses-libs curl libnfs gtk+3.0 gtk+3.0-dev fuse libpulse libseccomp jack pipewire liburing pulseaudio pulseaudio-alsa alsa-plugins-pulse" +
                    " mesa-dri-gallium mesa-vulkan-swrast vulkan-loader mesa-utils mesa-egl mesa-gbm mesa-vulkan-ati mesa-vulkan-broadcom mesa-vulkan-freedreno mesa-vulkan-panfrost" +
                    " qemu-audio-sdl capstone libcbor snappy lzo ndctl keyutils-libs vde2-libs libdw libbpf sndio-libs linux-pam fuse3-libs libssh vte3";
        } else {
            return "bash aria2 tar dwm xterm libslirp libslirp-dev pulseaudio-dev glib-dev pixman-dev zlib-dev spice-dev" +
                    " libusbredirparser usbredir-dev sdl2 sdl2-dev sdl2_image-dev libepoxy-dev virglrenderer-dev rdma-core fluxbox" +
                    " libusb libaio ncurses-libs curl libnfs gtk+3.0 gtk+3.0-dev fuse libpulse libseccomp jack pipewire liburing pulseaudio pulseaudio-alsa alsa-plugins-pulse" +
                    " mesa-dri-gallium mesa-vulkan-swrast vulkan-loader mesa-utils mesa-egl" +
                    " qemu-audio-sdl capstone libcbor snappy lzo ndctl keyutils-libs vde2-libs libdw libbpf sndio-libs linux-pam fuse3-libs libssh vte3";
        }
    }

    public static String neededPkgs32bit() {
        if (DeviceUtils.isArm()) {
            return "bash aria2 tar dwm xterm libslirp libslirp-dev pulseaudio-dev glib-dev pixman-dev zlib-dev spice-dev" +
                    " libusbredirparser usbredir-dev libiscsi-dev sdl2 sdl2-dev libepoxy-dev virglrenderer-dev rdma-core pulseaudio pulseaudio-alsa alsa-plugins-pulse" +
                    " libusb ncurses-libs curl libnfs gtk+3.0 fuse libpulse libseccomp jack pipewire liburing tigervnc qemu-audio-sdl fluxbox";
        } else {
            return "bash aria2 tar dwm xterm libslirp libslirp-dev pulseaudio-dev glib-dev pixman-dev zlib-dev spice-dev" +
                    " libusbredirparser usbredir-dev sdl2 sdl2-dev sdl2_image-dev libepoxy-dev virglrenderer-dev rdma-core fluxbox" +
                    " libusb libaio ncurses-libs curl libnfs gtk+3.0 gtk+3.0-dev fuse libpulse libseccomp jack pipewire liburing pulseaudio pulseaudio-alsa alsa-plugins-pulse" +
                    " mesa-dri-gallium mesa-vulkan-swrast vulkan-loader mesa-utils mesa-egl" +
                    " qemu-audio-sdl capstone libcbor snappy lzo ndctl keyutils-libs vde2-libs libdw libbpf sndio-libs linux-pam fuse3-libs libssh vte3 libatomic";
        }
    }

    public static boolean needreinstallsystem = false;

    public static String temporaryLastedTerminalOutput = "";

    public static String telegramLink = "https://t.me/vectras_os";

    public static String patreonLink = "https://www.patreon.com/VectrasTeam";

}
