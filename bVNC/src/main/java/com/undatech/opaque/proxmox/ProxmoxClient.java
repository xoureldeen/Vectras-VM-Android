package com.undatech.opaque.proxmox;

import android.os.Handler;

import com.undatech.opaque.Connection;
import com.undatech.opaque.proxmox.pojo.PveRealm;
import com.undatech.opaque.proxmox.pojo.PveResource;
import com.undatech.opaque.proxmox.pojo.SpiceDisplay;
import com.undatech.opaque.proxmox.pojo.VmStatus;
import com.undatech.opaque.proxmox.pojo.VncDisplay;

import org.apache.http.HttpException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginException;

public class ProxmoxClient extends RestClient {
    private static final String TAG = "ProxmoxClient";
    private static final int DEFAULT_PROXMOX_PORT = 8006;
    private String baseUrl;
    private String ticket;
    private String csrfToken;

    /**
     * Initializes a connection to PVE's API.
     */
    public ProxmoxClient(Connection connection, Handler handler) {
        super(connection, handler, DEFAULT_PROXMOX_PORT);
        this.baseUrl = String.format("%s%s", getApiUrl(), "/api2/json");
    }

    public HashMap<String, PveRealm> getAvailableRealms()
            throws JSONException, IOException, HttpException {
        resetState(baseUrl + "/access/domains");
        execute(RestClient.RequestMethod.GET);

        HashMap<String, PveRealm> result = null;
        if (getResponseCode() == HttpURLConnection.HTTP_OK) {
            JSONArray array = new JSONObject(getResponse()).getJSONArray("data");
            result = PveRealm.getRealmsFromJsonArray(array);
        } else {
            throw new HttpException(getErrorMessage());
        }

        return result;
    }

    public void login(String user, String realm, String password, String otp)
            throws JSONException, IOException, HttpException, LoginException {
        resetState(baseUrl + "/access/ticket");

        addParam("username", user);
        addParam("password", password);
        addParam("realm", realm);
        if (otp != null && !"".equals(otp)) {
            addParam("otp", otp);
        }

        execute(RestClient.RequestMethod.POST);

        if (getResponseCode() == HttpURLConnection.HTTP_OK) {
            JSONObject data = new JSONObject(getResponse()).getJSONObject("data");
            ticket = data.getString("ticket");
            csrfToken = data.getString("CSRFPreventionToken");
        } else if (getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            throw new LoginException(getErrorMessage());
        } else {
            throw new HttpException(getErrorMessage());
        }
    }

    /**
     * Actually performs a request to PVE.
     *
     * @param resource    the REST resource to affect
     * @param method      the method (GET, POST, etc)
     * @param requestData the data to send
     * @return the data returned by PVE as a result of the request.
     * @throws JSONException
     * @throws LoginException
     * @throws IOException
     * @throws HttpException
     */
    private JSONObject request(String resource, RestClient.RequestMethod method, Map<String, String> requestData)
            throws IOException, JSONException, LoginException, HttpException {

        resetState(baseUrl + resource);

        addHeader("Cookie", "PVEAuthCookie=" + ticket);

        if (!method.equals(RestClient.RequestMethod.GET)) {
            addHeader("CSRFPreventionToken", csrfToken);
        }

        if (requestData != null)
            for (Map.Entry<String, String> p : requestData.entrySet()) {
                addParam(p.getKey(), p.getValue());
            }

        execute(method);

        if (isSuccessfulCode(getResponseCode())) {
            return new JSONObject(getResponse());
        } else if (getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            throw new LoginException(getErrorMessage());
        } else {
            // The API returned an unexpected 4xx or 5xx return code.
            throw new HttpException(getErrorMessage());
        }
    }

    /**
     * Checks if an HTTP code is successful (200 - 299) or not
     */
    boolean isSuccessfulCode(int code) {
        return (code / 100 == 2);
    }

    /**
     * Retrieves a representation of the VNC display of a node.
     *
     * @param node the name of the PVE node
     * @return the VNC display of the node
     * @throws LoginException
     * @throws JSONException
     * @throws IOException
     * @throws HttpException
     */
    public VncDisplay vncNode(String node) throws LoginException, JSONException, IOException, HttpException {
        JSONObject jObj = request("/nodes/" + node + "/vncshell", RestClient.RequestMethod.POST, null);
        return new VncDisplay(jObj.getJSONObject("data"));
    }

    /**
     * Retrieves a representation of the SPICE display of a node.
     *
     * @param node the name of the PVE node
     * @return the SPICE display of the node
     * @throws LoginException
     * @throws JSONException
     * @throws IOException
     * @throws HttpException
     */
    public SpiceDisplay spiceNode(String node) throws LoginException, JSONException, IOException, HttpException {
        JSONObject jObj = request("/nodes/" + node + "/spiceshell", RestClient.RequestMethod.POST, null);
        return new SpiceDisplay(jObj.getJSONObject("data"));
    }

    /**
     * Retrieves a representation of the VNC display of a VM.
     *
     * @param node the name of the PVE node
     * @param type of VM, one of qemu, lxc, or openvz (deprecated)
     * @param vmid the numeric VM ID
     * @return The VNC display of the VM
     * @throws LoginException
     * @throws JSONException
     * @throws IOException
     * @throws HttpException
     */
    public VncDisplay vncVm(String node, String type, int vmid) throws LoginException, JSONException, IOException, HttpException {
        JSONObject jObj = request("/nodes/" + node + "/" + type + "/" + vmid + "/vncproxy", RestClient.RequestMethod.POST, null);
        return new VncDisplay(jObj.getJSONObject("data"));
    }

    /**
     * Retrieves a representation of the SPICE display of a VM.
     *
     * @param node the name of the PVE node
     * @param type of VM, one of qemu, lxc, or openvz (deprecated)
     * @param vmid the numeric VM ID
     * @return The SPICE display of the VM
     * @throws LoginException
     * @throws JSONException
     * @throws IOException
     * @throws HttpException
     */
    public SpiceDisplay spiceVm(String node, String type, int vmid) throws LoginException, JSONException, IOException, HttpException {
        JSONObject jObj = request("/nodes/" + node + "/" + type + "/" + vmid + "/spiceproxy", RestClient.RequestMethod.POST, null);
        return new SpiceDisplay(jObj.getJSONObject("data"));
    }

    /**
     * Starts a VM.
     *
     * @param node the name of the PVE node
     * @param type of VM, one of qemu, lxc, or openvz (deprecated)
     * @param vmid the numeric VM ID
     * @return status string
     * @throws LoginException
     * @throws JSONException
     * @throws IOException
     * @throws HttpException
     */
    public String startVm(String node, String type, int vmid) throws LoginException, JSONException, IOException, HttpException {
        JSONObject jObj = request("/nodes/" + node + "/" + type + "/" + vmid + "/status/start", RestClient.RequestMethod.POST, null);
        return jObj.getString("data");
    }

    /**
     * Gets the current status of a VM.
     *
     * @param node the name of the PVE node
     * @param type of VM, one of qemu, lxc, or openvz (deprecated)
     * @param vmid the numeric VM ID
     * @return status of VM
     * @throws LoginException
     * @throws JSONException
     * @throws IOException
     * @throws HttpException
     */
    public VmStatus getCurrentStatus(String node, String type, int vmid) throws LoginException, JSONException, IOException, HttpException {
        JSONObject jObj = request("/nodes/" + node + "/" + type + "/" + vmid + "/status/current", RestClient.RequestMethod.GET, null);
        return new VmStatus(jObj.getJSONObject("data"));
    }

    /**
     * Shows what resources are currently available on the PVE cluster
     *
     * @return object representing resources available
     * @throws LoginException
     * @throws JSONException
     * @throws IOException
     * @throws HttpException
     */
    public Map<String, PveResource> getResources() throws LoginException, JSONException, IOException, HttpException {
        JSONObject jObj = request("/cluster/resources", RestClient.RequestMethod.GET, null);
        JSONArray jArr = jObj.getJSONArray("data");
        HashMap<String, PveResource> result = new HashMap<String, PveResource>();
        for (int i = 0; i < jArr.length(); i++) {
            PveResource r = new PveResource(jArr.getJSONObject(i));
            if (r.getName() != null && r.getNode() != null && r.getType() != null && r.getVmid() != null) {
                result.put(r.getVmid(), r);
            }
        }
        return result;
    }
}
