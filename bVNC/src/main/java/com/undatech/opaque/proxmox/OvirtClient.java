package com.undatech.opaque.proxmox;

import android.os.Handler;
import android.util.Log;

import com.undatech.opaque.Connection;
import com.undatech.opaque.util.HttpsFileDownloader;

import org.apache.http.HttpException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.security.auth.login.LoginException;

public class OvirtClient extends RestClient {
    private static final String TAG = "OvirtClient";
    private static final int DEFAULT_OVIRT_PORT = 443;
    private String baseUrl;
    private String accessToken;

    /**
     * Initializes a connection to oVirt's API.
     */
    public OvirtClient(Connection connection, Handler handler) {
        super(connection, handler, DEFAULT_OVIRT_PORT);
        this.baseUrl = String.format("%s%s", getApiUrl(), "/ovirt-engine");
    }

    public void trySsoLogin(String user, String password)
            throws JSONException, IOException, HttpException, LoginException {
        String url = String.format(
                "%s/sso/oauth/token?grant_type=password&username=%s&password=%s&scope=ovirt-app-api", baseUrl, user, password
        );
        resetState(url);
        addHeader("Accept", "application/json");
        execute(RequestMethod.GET);
        int code = getResponseCode();
        if (code == HttpURLConnection.HTTP_OK) {
            JSONObject data = new JSONObject(getResponse());
            accessToken = data.getString("access_token");
        } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_BAD_REQUEST) {
            throw new LoginException(getErrorMessage());
        } else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
            Log.d(TAG, "SSO Not supported on this server, proceeding without sso token");
        } else {
            Log.e(TAG, "Throwing Exception due to unexpected HTTP code: " + code);
            throw new HttpException(getErrorMessage());
        }
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void downloadFromServer(HttpsFileDownloader.OnDownloadFinishedListener listener) {
        Log.d(TAG, "downloadFromServer");
        String address = String.format("%s%s", baseUrl, "/services/pki-resource?resource=ca-certificate&format=X509-PEM-CA");
        new HttpsFileDownloader(
                address, true, handler, connection.getX509KeySignature().trim(), listener
        ).initiateDownload();
    }
}
