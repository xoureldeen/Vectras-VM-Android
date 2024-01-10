package com.vectras.qemu.jni;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

import com.vectras.qemu.Config;
import com.vectras.qemu.MainActivityCommon;
import com.vectras.qemu.MainSDLActivity;
import com.vectras.qemu.MainService;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.utils.FileUtils;
import com.vectras.qemu.utils.Machine;
import com.vectras.qemu.utils.QmpClient;
import com.vectras.qemu.utils.RamInfo;
import com.vectras.vm.logger.VectrasStatus;
import com.vectras.vm.utils.UIUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONObject;

public class StartVM {

    private static final String TAG = "StartVM";
    private static Context context;

    String[] params = null;

    //native lib
    private String libqemu = null;

    //qmp server
    public int enableqmp;
    private String qmp_server;
    private int qmp_port;

    //state
    public int paused;
    public String snapshot_name = null;
    public String save_state_name = null;
    private String save_dir;
    public int current_fd = 0;

    public String base_dir;
    public String dns_addr;
    public String append = "";
    public boolean busy = false;
    public String name;

    //ui
    public int enablespice = 0;
    public String keyboard_layout = Config.defaultKeyboardLayout;

    public String mouse = null;
    public int enablevnc;
    public int vnc_allow_external = 0;
    public int qmp_allow_external = 0;
    public String vnc_passwd = "vectras";

    // cpu/board settings
    private String cpu;
    private String arch = "x86";
    private String machine_type;
    private int memory = 128;
    private int cpuNum = 1;
    public int enablekvm;
    public int enable_mttcg;

    // disks
    public String hda_img_path;
    private String hdb_img_path;
    private String hdc_img_path;
    private String hdd_img_path;
    public String shared_folder_path;
    public int shared_folder_readonly = 1;
    private String hd_cache = "default";

    //removable devices
    public String cd_iso_path;
    public String fda_img_path;
    public String fdb_img_path;
    public String sd_img_path;

    //boot options
    private String bootdevice = null;
    private String kernel;
    private String initrd;

    //graphics
    private String vga_type = "std";

    //audio
    public String sound_card;

    // net
    private String net_cfg = "None";
    private String nic_card = null;
    private String hostfwd = null;
    private String guestfwd = null;

    //advanced
    private int disableacpi = 0;
    private int disablehpet = 0;
    private int disabletsc = 0;
    public String extra_params;

    /**
     * @throws Exception
     */
    public StartVM(Context context) throws Exception {

        name = Config.machinename;
        base_dir = Config.getBasefileDir();
        save_dir = Config.getMachineDir() + name;
        save_state_name = save_dir + "/" + Config.state_filename;
        hda_img_path = Config.hda_path;
        extra_params = Config.extra_params;
        shared_folder_path = Config.sharedFolder;
        //extra_params = Config.extra_params;
        this.context = context;
        this.libqemu = FileUtils.getNativeLibDir(context) + "/libqemu-system-x86_64.so";
        this.arch = "x86_64";
        this.cpuNum = MainSettingsManager.getCpuNum(MainActivityCommon.activity);
        if (MainSettingsManager.getMTTCG(MainActivityCommon.activity))
            this.enable_mttcg = 1;
        else
            this.enable_mttcg = 0;
        this.vnc_allow_external = 0;

    }

    public static void onVMResolutionChanged(int width, int height) {

        if (MainSDLActivity.mIsSurfaceReady)
            MainSDLActivity.onVMResolutionChanged(width, height);
    }

    public void print(String[] params) {
        VectrasStatus.logInfo("Params:");
        Log.d(TAG, "Params:");
        for (int i = 0; i < params.length; i++) {
            VectrasStatus.logInfo(i + ": " + params[i]);
            Log.d(TAG, i + ": " + params[i]);
        }

    }

    public String startvm() {

        String res = null;
        try {
            prepareParams();
        } catch (Exception ex) {
            UIUtils.toastLong(context, ex.getMessage());
            return res;
        }

        //set the exit code
        MainSettingsManager.setExitCode(context, 2);

        try {
            res = start(Config.storagedir, this.base_dir, this.libqemu, Config.SDLHintScale, params, this.paused, this.save_state_name);
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, "Vectras Exception: " + ex.toString());
        }
        return res;
    }

    public void prepareParams() throws Exception {

        params = null;
        ArrayList<String> paramsList = new ArrayList<String>();

        paramsList.add(libqemu);

        addUIOptions(paramsList);

        addCpuBoardOptions(paramsList);

        addDrives(paramsList);

        addRemovableDrives(paramsList);

        addBootOptions(paramsList);

        addGraphicsOptions(paramsList);

        addAudioOptions(paramsList);

        addNetworkOptions(paramsList);

        addAdvancedOptions(paramsList);

        addGenericOptions(paramsList);

        addStateOptions(paramsList);

        params = (String[]) paramsList.toArray(new String[paramsList.size()]);

        print(params);

    }

    private void addStateOptions(ArrayList<String> paramsList) {
        if (paused == 1 && this.save_state_name != null && !save_state_name.equals("")) {
            int fd_tmp = FileUtils.get_fd(context, save_state_name);
            if (fd_tmp < 0) {
                Log.e(TAG, "Error while getting fd for: " + save_state_name);
            } else {
                //Log.i(TAG, "Got new fd "+fd_tmp + " for: " +save_state_name);
                paramsList.add("-incoming");
                paramsList.add("fd:" + fd_tmp);
            }
        }
    }

    private void addUIOptions(ArrayList<String> paramsList) {
        if (enablevnc != 0) {
            Log.v(TAG, "Enable VNC server");
            paramsList.add("-vnc");

            if (vnc_allow_external != 0) {
                //TODO: Allow connections from External
                // Use with x509 auth and TLS for encryption
                paramsList.add(":1");
            } else {
                // Allow connections only from localhost using localsocket without a password
                //paramsList.add(Config.defaultVNCHost+":" + Config.defaultVNCPort);
                String qmpParams = "unix:";
                qmpParams += Config.getLocalVNCSocketPath();
                paramsList.add(qmpParams);
            }
            //Allow monitor console only for VNC,
            // SDL for android doesn't support more
            // than 1 window
            paramsList.add("-monitor");
            paramsList.add("vc");

        } else if (enablespice != 0) {
            //Not working right now
            Log.v(TAG, "Enable SPICE server");
            paramsList.add("-spice");
            String spiceParams = "port=5902";

            if (vnc_allow_external != 0 && vnc_passwd != null) {
                spiceParams += ",password=";
                spiceParams += vnc_passwd;
            } else
                spiceParams += ",addr=127.0.0.1"; // Allow only connections from localhost without password

            spiceParams += ",disable-ticketing";
            //argv.add("-chardev");
            //argv.add("spicevm");
        } else {
            //SDL needs explicit keyboard layout
            Log.v(TAG, "Disabling VNC server, using SDL instead");
            if (keyboard_layout == null) {
                paramsList.add("-k");
                paramsList.add("en-us");
            }

            //XXX: monitor, serial, and parallel display crashes cause SDL doesn't support more than 1 window
            paramsList.add("-monitor");
            paramsList.add("none");

            paramsList.add("-serial");
            paramsList.add("none");

            paramsList.add("-parallel");
            paramsList.add("none");
        }

        if (keyboard_layout != null) {
            paramsList.add("-k");
            paramsList.add(keyboard_layout);
        }

        if (mouse != null && !mouse.equals("ps2")) {
            paramsList.add("-usb");
            paramsList.add("-device");
            paramsList.add(mouse);
        }

    }

    private void addAdvancedOptions(ArrayList<String> paramsList) {

        if (disableacpi != 0) {
            paramsList.add("-no-acpi"); //disable ACPI
        }
        if (disablehpet != 0) {
            paramsList.add("-no-hpet"); //        disable HPET
        }

        //TODO:Extra options
        if (extra_params != null && !extra_params.trim().equals("")) {
            String[] paramsTmp = extra_params.split(" ");
            paramsList.addAll(Arrays.asList(paramsTmp));
        }

    }

    private void addAudioOptions(ArrayList<String> paramsList) {

        if (sound_card != null && !sound_card.equals("None")) {
            paramsList.add("-soundhw");
            paramsList.add(sound_card);
        }

    }

    private void addGenericOptions(ArrayList<String> paramsList) {

        paramsList.add("-L");
        paramsList.add(base_dir);

        //XXX: Snapshots not working currently, use migrate/incoming instead
        if (snapshot_name != null && !snapshot_name.equals("")) {
            paramsList.add("-loadvm");
            paramsList.add(snapshot_name);
        }

        if (enableqmp != 0) {

            paramsList.add("-qmp");

            if (qmp_allow_external != 0) {
                String qmpParams = "tcp:";
                qmpParams += (":" + this.qmp_port);
                qmpParams += ",server,nowait";
                paramsList.add(qmpParams);
            } else {
                //Specify a unix local domain as localhost to limit to local connections only
                String qmpParams = "unix:";
                qmpParams += Config.getLocalQMPSocketPath();
                qmpParams += ",server,nowait";
                paramsList.add(qmpParams);

            }


        }

        //Enable Tracing log
        //    argv.add("-D");
        //    argv.add("/sdcard/vectras/log.txt");
        //    argv.add("--trace");
        //    argv.add("events=/sdcard/vectras/tmp/events");
        //    argv.add("--trace");
        //    argv.add("file=/sdcard/vectras/tmp/trace");

//        paramsList.add("-tb-size");
//        paramsList.add("32M"); //Don't increase it crashes

        paramsList.add("-overcommit");
        paramsList.add("mem-lock=off");

        paramsList.add("-rtc");
        paramsList.add("base=localtime");

        paramsList.add("-nodefaults");


        //XXX: Usb redir not working under User mode
        //Redirect ports (SSH)
        //	argv.add("-redir");
        //	argv.add("5555::22");

    }

    private void addCpuBoardOptions(ArrayList<String> paramsList) {

        //XXX: SMP is not working correctly for some guest OSes
        //so we enable multi core only under KVM
        // anyway regular emulation is not gaining any benefit unless mttcg is enabled but that
        // doesn't work for x86 guests yet
        if (this.cpuNum > 1 &&
                (enablekvm == 1 || enable_mttcg == 1 || !Config.enableSMPOnlyOnKVM)) {
            paramsList.add("-smp");
            paramsList.add(this.cpuNum + "");
        }

        if (machine_type != null && !machine_type.equals("Default")) {
            paramsList.add("-M");
            paramsList.add(machine_type);
        }

        //FIXME: something is wrong with quoting that doesn't let sparc qemu find the cpu def
        // for now we remove the cpu drop downlist items for sparc
        if (this.cpu != null && this.cpu.contains(" "))
            cpu = "'" + cpu + "'"; // XXX: needed for sparc cpu names

        //XXX: we disable tsc feature for x86 since some guests are kernel panicking
        // if the cpu has not specified by user we use the internal qemu32/64
        if (disabletsc == 1 && (arch.equals("x86") || arch.equals("x86_64"))) {
            if (cpu == null || cpu.equals("Default")) {
                if (arch.equals("x86"))
                    cpu = "qemu32";
                else if (arch.equals("x86_64"))
                    cpu = "qemu64";
            }
            cpu += ",-tsc";
        }

        if (this.cpu != null && !cpu.equals("Default")) {
            paramsList.add("-cpu");
            paramsList.add(cpu);

        }

        paramsList.add("-m");
        paramsList.add(RamInfo.vectrasMemory() + "");


        if (enablekvm != 0) {
            paramsList.add("-enable-kvm");
        } else if (this.enable_mttcg != 0 && Machine.isHost64Bit()) {
            //XXX: we should only do this for 64bit hosts
            paramsList.add("-accel");
            String tcgParams = "tcg";
            if (cpuNum > 1)
                tcgParams += ",thread=multi";
            paramsList.add(tcgParams);
            //#endif
        }

    }

    private void addNetworkOptions(ArrayList<String> paramsList) throws Exception {

        if (this.net_cfg != null) {
            paramsList.add("-net");
            if (net_cfg.equals("user")) {
                String netParams = net_cfg;
                if (hostfwd != null) {

                    //hostfwd=[tcp|udp]:[hostaddr]:hostport-[guestaddr]:guestport{,hostfwd=...}
                    // example forward ssh from guest port 2222 to guest port 22:
                    // hostfwd=tcp::2222-:22
                    if (hostfwd.startsWith("hostfwd")) {
                        throw new Exception("Invalid format for Host Forward, should be: tcp:hostport1:guestport1,udp:hostport2:questport2,...");
                    }
                    String[] hostfwdparams = hostfwd.split(",");
                    for (int i = 0; i < hostfwdparams.length; i++) {
                        netParams += ",";
                        String[] hostfwdparam = hostfwdparams[i].split(":");
                        netParams += ("hostfwd=" + hostfwdparam[0] + "::" + hostfwdparam[1] + "-:" + hostfwdparam[2]);
                    }
                }
                if (guestfwd != null) {
                    netParams += ",";
                    netParams += guestfwd;
                }
                paramsList.add(netParams);
            } else if (net_cfg.equals("tap")) {
                paramsList.add("tap,vlan=0,ifname=tap0,script=no");
            } else if (net_cfg.equals("none")) {
                paramsList.add("none");
            } else {
                //Unknown interface
                paramsList.add("none");
            }
        }

        if (nic_card != null) {
            paramsList.add("-net");
            String nicParams = "nic";
            if (net_cfg.equals("tap"))
                nicParams += ",vlan=0";
            if (!nic_card.equals("Default"))
                nicParams += (",model=" + nic_card);
            paramsList.add(nicParams);
        }
    }

    private void addGraphicsOptions(ArrayList<String> paramsList) {
        if (vga_type != null) {
            if (vga_type.equals("Default")) {
                //do nothing
            } else if (vga_type.equals("virtio-gpu-pci")) {
                paramsList.add("-device");
                paramsList.add(vga_type);
            } else if (vga_type.equals("nographic")) {
                paramsList.add("-nographic");
            } else {
                paramsList.add("-vga");
                paramsList.add(vga_type);
            }
        }


    }

    private void addBootOptions(ArrayList<String> paramsList) {
        if (this.bootdevice != null) {
            paramsList.add("-boot");
            paramsList.add(bootdevice);
        }

        if (this.kernel != null && !this.kernel.equals("")) {
            paramsList.add("-kernel");
            paramsList.add(this.kernel);
        }

        if (initrd != null && !initrd.equals("")) {
            paramsList.add("-initrd");
            paramsList.add(initrd);
        }

        if (append != null && !append.equals("")) {
            paramsList.add("-append");
            paramsList.add(append);
        }
    }

    public void addDrives(ArrayList<String> paramsList) {
        if (hda_img_path != null) {
            paramsList.add("-drive"); //empty
            String param = "index=0";
            if (Config.enable_hd_if) {
                param += ",if=";
                param += Config.hd_if_type;
            }
            param += ",media=disk";
            if (!hda_img_path.equals("")) {
                param += ",file=" + hda_img_path;
            }
            paramsList.add(param);
        }

        if (hdb_img_path != null) {
            paramsList.add("-drive"); //empty
            String param = "index=1";
            if (Config.enable_hd_if) {
                param += ",if=";
                param += Config.hd_if_type;
            }
            param += ",media=disk";
            if (!hdb_img_path.equals("")) {
                param += ",file=" + hdb_img_path;
            }
            paramsList.add(param);
        }

        if (hdc_img_path != null) {
            paramsList.add("-drive"); //empty
            String param = "index=2";
            if (Config.enable_hd_if) {
                param += ",if=";
                param += Config.hd_if_type;
            }
            param += ",media=disk";
            if (!hdc_img_path.equals("")) {
                param += ",file=" + hdc_img_path;
            }
            paramsList.add(param);
        }

        if (hdd_img_path != null) {
            paramsList.add("-drive"); //empty
            String param = "index=3";
            if (Config.enable_hd_if) {
                param += ",if=";
                param += Config.hd_if_type;
            }
            param += ",media=disk";
            if (!hdd_img_path.equals("")) {
                param += ",file=" + hdd_img_path;
            }
            paramsList.add(param);
        } else if (shared_folder_path != null) {
            //XXX; We use hdd to mount any virtual fat drives
            paramsList.add("-drive"); //empty
            String driveParams = "index=3";
            driveParams += ",media=disk";
            if (Config.enable_hd_if) {
                driveParams += ",if=";
                driveParams += Config.hd_if_type;
            }
            driveParams += ",format=raw";
            driveParams += ",file=fat:";
            driveParams += "rw:"; //Always Read/Write
            driveParams += shared_folder_path;
            paramsList.add(driveParams);
        }

    }

    public void addRemovableDrives(ArrayList<String> paramsList) {

        if (cd_iso_path != null) {
            paramsList.add("-drive"); //empty
            String param = "index=2";
            if (Config.enable_hd_if) {
                param += ",if=";
                param += Config.hd_if_type;
            }
            param += ",media=cdrom";
            if (!cd_iso_path.equals("")) {
                param += ",file=" + cd_iso_path;
            }
            paramsList.add(param);
        }

        if (Config.enableEmulatedFloppy && fda_img_path != null) {
            paramsList.add("-drive"); //empty
            String param = "index=0,if=floppy";
            if (!fda_img_path.equals("")) {
                param += ",file=" + fda_img_path;
            }
            paramsList.add(param);
        }

        if (Config.enableEmulatedFloppy && fdb_img_path != null) {
            paramsList.add("-drive"); //empty
            String param = "index=1,if=floppy";
            if (!fdb_img_path.equals("")) {
                param += ",file=" + fdb_img_path;
            }
            paramsList.add(param);
        }

        if (Config.enableEmulatedSDCard && sd_img_path != null) {
            paramsList.add("-device");
            paramsList.add("sd-card,drive=sd0,bus=sd-bus");
            paramsList.add("-drive");
            String param = "if=none,id=sd0";
            if (!sd_img_path.equals("")) {
                param += ",file=" + sd_img_path;
            }
            paramsList.add(param);
        }

    }

    //JNI Methods
    public native String start(String storage_dir, String base_dir, String lib_path, int sdl_scale_hint, Object[] params, int paused, String save_state_name);

    public native String stop(int restart);

    public native void setsdlrefreshrate(int value);

    public native void setvncrefreshrate(int value);

    public native int getsdlrefreshrate();

    public native int getvncrefreshrate();

    private native int onmouse(int button, int action, int relative, float x, float y);

    private native int setrelativemousemode(int relativemousemode);

    protected void vncchangepassword(String vnc_passwd) {
        String res = QmpClient.sendCommand(QmpClient.changevncpasswd(vnc_passwd));
        String desc = null;
        if (res != null && !res.equals("")) {
            try {
                JSONObject resObj = new JSONObject(res);
                if (resObj != null && !resObj.equals("") && res.contains("error")) {
                    String resInfo = resObj.getString("error");
                    if (resInfo != null && !resInfo.equals("")) {
                        JSONObject resInfoObj = new JSONObject(resInfo);
                        desc = resInfoObj.getString("desc");
                        UIUtils.toastLong(context, "Could not set VNC Password: " + desc);
                        Log.e(TAG, desc);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    protected String changedev(String dev, String dev_value) {
        QmpClient.sendCommand(QmpClient.changedev(dev, dev_value));
        String display_dev_value = FileUtils.getFullPathFromDocumentFilePath(dev_value);
        return "Changed device: " + dev + " to " + display_dev_value;
    }

    protected String ejectdev(String dev) {
        QmpClient.sendCommand(QmpClient.ejectdev(dev));
        return "Ejected device: " + dev;
    }

    public String startvm(Context context, int ui) {
        MainService.executor = this;
        Intent i = new Intent(Config.ACTION_START, null, context, MainService.class);
        Bundle b = new Bundle();
        // b.putString("machine_type", this.machine_type);
        b.putInt("ui", ui);
        i.putExtras(b);
        context.startService(i);
        Log.v(TAG, "start VM service");
        return "startVMService";

    }

    public void stopvm(final int restart) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                doStopVM(restart);
            }
        }).start();
    }

    public void doStopVM(final int restart) {

        if (restart == 0) {
            MainService.stopService();

            //XXX: Wait till service goes down
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (restart != 0) {
            QmpClient.sendCommand(QmpClient.reset());
        } else {
            //XXX: Qmp command only halts the VM but doesn't exit
            //  so we use force close
//            QmpClient.sendCommand(QmpClient.powerDown());
            stop(restart);
        }

    }

    public String savevm(String statename) {
        // Set to delete previous snapshots after vm resumed
        Log.v(TAG, "Save Snapshot");
        this.snapshot_name = statename;

        String res = null;
        //TODO:
        //res = QmpClient.sendCommand(QmpClient.saveSnapshot());
        return res;
    }

    public String resumevm() {
        // Set to delete previous snapshots after vm resumed
        Log.v(TAG, "Resume the VM");
        String res = startvm();
        Log.d(TAG, res);
        return res;
    }

    public void change_vnc_password() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                vncchangepassword(vnc_passwd);
            }
        });
        thread.start();
    }

    public String get_state() {

        return null;
    }

    public void change_dev(final String dev, final String image_path) {

        Thread thread = new Thread(new Runnable() {
            public void run() {
                String image_path_conv = FileUtils.convertDocumentFilePath(image_path);
                if (image_path_conv == null || image_path_conv.trim().equals("")) {
                    StartVM.this.busy = true;
                    String res = StartVM.this.ejectdev(dev);
                    Log.d(TAG, res);
                    StartVM.this.busy = false;
                } else if (FileUtils.fileValid(context, image_path_conv)) {
                    StartVM.this.busy = true;
                    String res = StartVM.this.changedev(dev, image_path_conv);
                    Log.d(TAG, res);
                    StartVM.this.busy = false;
                } else {
                    Log.d(TAG, "File does not exist");
                }
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();


    }

    public int get_fd(String path) {
        int fd = FileUtils.get_fd(context, path);
        return fd;

    }

    public int close_fd(int fd) {
        int res = FileUtils.close_fd(fd);
        return res;

    }

    public void prepPaths() {
        File destDir = new File(save_dir);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        // Protect the paths from qemu thinking they contain a protocol in the string

        this.hda_img_path = FileUtils.convertDocumentFilePath(this.hda_img_path);
        if (this.hda_img_path != null && hda_img_path.equals("")) {
            hda_img_path = null;
        }
        this.hdb_img_path = FileUtils.convertDocumentFilePath(this.hdb_img_path);
        if (this.hdb_img_path != null && hdb_img_path.equals("")) {
            hdb_img_path = null;
        }
        this.hdc_img_path = FileUtils.convertDocumentFilePath(this.hdc_img_path);
        if (this.hdc_img_path != null && hdc_img_path.equals("")) {
            hdc_img_path = null;
        }
        this.hdd_img_path = FileUtils.convertDocumentFilePath(this.hdd_img_path);
        if (this.hdd_img_path != null && hdd_img_path.equals("")) {
            hdd_img_path = null;
        }

        // Removable disks
        this.cd_iso_path = FileUtils.convertDocumentFilePath(this.cd_iso_path);
        this.fda_img_path = FileUtils.convertDocumentFilePath(this.fda_img_path);

        this.fdb_img_path = FileUtils.convertDocumentFilePath(this.fdb_img_path);
        this.sd_img_path = FileUtils.convertDocumentFilePath(this.sd_img_path);

        this.kernel = FileUtils.convertDocumentFilePath(this.kernel);
        this.initrd = FileUtils.convertDocumentFilePath(this.initrd);
    }

    public int setRelativeMouseMode(int relative) {
        return setrelativemousemode(relative);
    }

    public int onVectrasMouse(int button, int action, int relative, float x, float y) {
        //XXX: Make sure that mouse motion is not triggering crashes in SDL while resizing
        if (!MainSDLActivity.mIsSurfaceReady || MainSDLActivity.isResizing) {
//				Log.w(TAG, "onVectrasMouse: Ignoring mouse event surface not ready");
            return -1;
        }

        //XXX: Check boundaries, perhaps not necessary since SDL is also doing the same thing
        if (relative == 1
                || (x >= 0 && x <= MainSDLActivity.vm_width && y >= 0 && y <= MainSDLActivity.vm_height)
                || (action == MotionEvent.ACTION_SCROLL)) {
//			Log.d(TAG, "onVectrasMouse: B: " + button + ", A: " + action + ", R: " + relative + ", X: " + x + ", Y: " + y);
            return onmouse(button, action, relative, x, y);
        }
        return -1;
    }
}

