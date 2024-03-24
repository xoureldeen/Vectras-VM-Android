package com.undatech.opaque.proxmox.pojo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class SpiceDisplay {
    private String password;
    private int tlsPort;
    private String host;
    private String title;
    private String ca;
    private String hostSubject;
    private String proxy;
    private int deleteThisFile;
    private String secureAttention;
    private String type;
    private String toggleFullscreen;
    private String releaseCursor;

    public SpiceDisplay(JSONObject data) throws JSONException {
        password = data.getString("password");
        tlsPort = data.getInt("tls-port");
        host = data.getString("host");
        title = data.getString("title");
        ca = data.getString("ca");
        hostSubject = data.getString("host-subject");
        proxy = data.getString("proxy");
        deleteThisFile = data.getInt("delete-this-file");
        secureAttention = data.getString("secure-attention");
        type = data.getString("type");
        toggleFullscreen = data.getString("toggle-fullscreen");
        releaseCursor = data.getString("release-cursor");
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getTlsPort() {
        return tlsPort;
    }

    public void setTlsPort(int tlsPort) {
        this.tlsPort = tlsPort;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCa() {
        return ca;
    }

    public void setCa(String ca) {
        this.ca = ca;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public String getHostSubject() {
        return hostSubject;
    }

    public void setHostSubject(String hostSubject) {
        this.hostSubject = hostSubject;
    }

    public int getDeleteThisFile() {
        return deleteThisFile;
    }

    public void setDeleteThisFile(int deleteThisFile) {
        this.deleteThisFile = deleteThisFile;
    }

    public String getSecureAttention() {
        return secureAttention;
    }

    public void setSecureAttention(String secureAttention) {
        this.secureAttention = secureAttention;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getToggleFullscreen() {
        return toggleFullscreen;
    }

    public void setToggleFullscreen(String toggleFullscreen) {
        this.toggleFullscreen = toggleFullscreen;
    }

    public String getReleaseCursor() {
        return releaseCursor;
    }

    public void setReleaseCursor(String releaseCursor) {
        this.releaseCursor = releaseCursor;
    }

    public void outputToFile(String tempVvFile, String proxyReplacement) throws IOException {
        File file = new File(tempVvFile);
        FileOutputStream fos = new FileOutputStream(file);
        fos.write("[virt-viewer]\n".getBytes());
        fos.write(("tls-port=" + Integer.toString(tlsPort) + "\n").getBytes());
        fos.write(("ca=" + ca + "\n").getBytes());
        fos.write(("host=" + host + "\n").getBytes());
        fos.write(("host-subject=" + hostSubject + "\n").getBytes());
        fos.write(("password=" + password + "\n").getBytes());
        if (proxyReplacement != null) {
            fos.write(("proxy=" + proxy.replaceAll("//.*:", "//" + proxyReplacement + ":") + "\n").getBytes());
        } else {
            fos.write(("proxy=" + proxy + "\n").getBytes());
        }
        fos.write(("title=" + title + "\n").getBytes());
        fos.write(("delete-this-file=" + Integer.toString(deleteThisFile) + "\n").getBytes());
        fos.write(("release-cursor=" + releaseCursor + "\n").getBytes());
        fos.write(("secure-attention=" + secureAttention + "\n").getBytes());
        fos.write(("toggle-fullscreen=" + toggleFullscreen + "\n").getBytes());
        fos.write(("type=" + type + "\n").getBytes());
        fos.close();
    }
}
