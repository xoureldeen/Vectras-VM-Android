/*
 * Copyright (C) 2014 Dell Inc.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */
package com.iiordanov.bVNC;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.undatech.opaque.RemoteClientLibConstants;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Locale;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

// this is a secure tunnel meant to be established prior to creating the rfb connection
// it can then be used with standard vnc security
public class SecureTunnel implements X509TrustManager {
    private static final String TAG = "SecureTunnel";

    ConnectionBean m_connection;
    String m_address;
    int m_port;
    int m_idHashAlgorithm;
    String m_hash;
    String m_cert;
    SSLSocket m_sslsock;
    Handler m_messageBus;
    boolean m_certMatched = false;

    public SecureTunnel(String address, int port, int hashAlgorithm, String hash, String cert, Handler messageBus) {
        m_address = address;
        m_port = port;
        m_idHashAlgorithm = hashAlgorithm;
        m_hash = hash;
        m_cert = cert;
        m_messageBus = messageBus;
    }

    public static boolean isSignatureEqual(int hashAlgorithm, String expectedSignature, byte[] remoteData) throws Exception {
        if (Utils.isNullOrEmptry(expectedSignature))
            return false;
        String trimExpectedSignature = expectedSignature.trim();
        if (Utils.isNullOrEmptry(trimExpectedSignature))
            return false;
        String remoteSig = computeSignatureByAlgorithm(hashAlgorithm, remoteData);
        if (remoteSig.equalsIgnoreCase(trimExpectedSignature))
            return true;
        return false;
    }

    public static String computeSignatureByAlgorithm(int idHashAlgorithm, byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = null;
        switch (idHashAlgorithm) {
            case Constants.ID_HASH_MD5:
                digest = MessageDigest.getInstance("MD5");
                break;
            case Constants.ID_HASH_SHA1:
                digest = MessageDigest.getInstance("SHA-1");
                break;
            case Constants.ID_HASH_SHA256:
                digest = MessageDigest.getInstance("SHA-256");
                break;
            default:
                throw new IllegalArgumentException("Unsupported hash algorithm.");
        }
        byte[] fingerprint = digest.digest(data);
        String fingerprintHex = Utils.toHexString(fingerprint).trim();
        return fingerprintHex;
    }

    public void setup() throws Exception {
        Socket sock =new Socket();
        sock.setSoTimeout(Constants.SOCKET_CONN_TIMEOUT);
        sock.setKeepAlive(true);
        sock.connect(new InetSocketAddress(m_address, m_port), Constants.SOCKET_CONN_TIMEOUT);
        sock.setSoTimeout(0);
        sock.setTcpNoDelay(true);

        // create secure tunnel
        Log.i(TAG, "Generating TLS context.");
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, new TrustManager[]{this}, null);
        SSLSocketFactory sslfactory = sc.getSocketFactory();
        m_sslsock = (SSLSocket) sslfactory.createSocket(sock, sock.getInetAddress().getHostName(), sock.getPort(), true);

        m_sslsock.setTcpNoDelay(true);
        // this can hang without a timeout
        m_sslsock.setSoTimeout(Constants.SOCKET_CONN_TIMEOUT);
        setParam(m_sslsock);

        Log.i(TAG, "Performing TLS handshake.");
        m_sslsock.startHandshake();
        Log.i(TAG, "Secure tunnel established.");

        SSLSession session = m_sslsock.getSession();
        String info = String.format(Locale.US, "Using Protocol:%s CipherSuite:%s", session.getProtocol(), session.getCipherSuite());
        Log.i(TAG, info);

        // bVNC has not used timeouts and I suppose a session could last a long time without activity
        // however servers will probably kill the connection after a reasonable inactivity period.
        m_sslsock.setSoTimeout(0);
    }

    //cc.setStreams (sslsock.getInputStream(), sslsock.getOutputStream());
    public SSLSocket getSocket() {
        return m_sslsock;
    }

    protected void setParam(SSLSocket sock) {
        String[] supported;
        ArrayList<String> enabled = new ArrayList<String>();
        supported = sock.getSupportedCipherSuites();
        for (int i = 0; i < supported.length; i++) {
            // skip weak ciphers
            if (supported[i].contains("EXPORT"))
                continue;
            if (supported[i].contains("NULL"))
                continue;
            if (supported[i].contains("EMPTY"))
                continue;
            // Some servers (iDRAC) take ciphers in order
            // so add stronger TLS ciphers before SSL
            if (supported[i].contains("TLS")) {
                enabled.add(supported[i]);
                Log.i(TAG, "Adding cipher: " + supported[i]);
            }
        }
        for (int i = 0; i < supported.length; i++) {
            // skip weak ciphers
            if (supported[i].contains("EXPORT"))
                continue;
            if (supported[i].contains("NULL"))
                continue;
            if (supported[i].contains("EMPTY"))
                continue;
            // add legacy ciphers for compatability
            if (supported[i].contains("SSL")) {
                enabled.add(supported[i]);
                Log.i(TAG, "Adding cipher: " + supported[i]);
            }
        }
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        // check against supplied hash if available
        if (chain == null || chain.length == 0 || chain[0] == null) {
            throw new CertificateException();
        }
        X509Certificate remoteCertificate = chain[0];
        Message msg = Message.obtain(m_messageBus, RemoteClientLibConstants.DIALOG_X509_CERT, remoteCertificate);
        m_messageBus.sendMessage(msg);
    }

    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }

}
