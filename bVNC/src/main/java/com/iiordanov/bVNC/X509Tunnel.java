/*
 * Copyright (C) 2003 Sun Microsystems, Inc.
 * Copyright (C) 2003-2010 Martin Koegler
 * Copyright (C) 2006 OCCAM Financial Technology
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
import android.util.Base64;
import android.util.Log;

import com.undatech.opaque.RemoteClientLibConstants;
import com.undatech.opaque.RfbConnectable;

import java.io.ByteArrayInputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class X509Tunnel extends TLSTunnelBase {

    private static final String TAG = "X509Tunnel";
    Certificate cert;
    RfbConnectable rfb;
    Handler handler;

    public X509Tunnel(Socket sock_, String certstr, Handler handler, RfbConnectable rfb)
            throws CertificateException {
        super(sock_);

        Log.i(TAG, "X509Tunnel began.");
        this.rfb = rfb;
        this.handler = handler;
        if (certstr != null && !certstr.equals("")) {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream in = new ByteArrayInputStream(Base64.decode(certstr, Base64.DEFAULT));
            cert = (X509Certificate) certFactory.generateCertificate(in);
        }

        Log.i(TAG, "X509Tunnel ended.");
    }

    private boolean tlsIsOrNewerThan1_2(String[] protocols) {
        boolean result = false;
        for (String s : protocols) {
            if (s.matches("TLSv1.[2-9]") || s.matches("TLSv[2-9].*")) {
                result = true;
            }
        }
        return result;
    }

    protected void setParam(SSLSocket sock) {
        String[] supported;
        ArrayList<String> enabled = new ArrayList<String>();

        supported = sock.getSupportedCipherSuites();

        String[] protocols = sock.getEnabledProtocols();
        ;
        android.util.Log.d(TAG, "Supported TLS Protocols: " + Arrays.toString(protocols));

        for (int i = 0; i < supported.length; i++) {
            if (!supported[i].matches(".*DH_anon.*") &&
                    !(tlsIsOrNewerThan1_2(protocols) && supported[i].equals("TLS_FALLBACK_SCSV"))) {
                Log.d(TAG, "Adding cipher: " + supported[i]);
                enabled.add(supported[i]);
            } else {
                android.util.Log.d(TAG, "Omitting cipher: " + supported[i]);
            }
        }

        sock.setEnabledCipherSuites((String[]) enabled.toArray(new String[0]));
    }

    protected void initContext(SSLContext sc) throws java.security.GeneralSecurityException {
        TrustManager[] myTM;

        //if (cert != null) {
        myTM = new TrustManager[]{new X509TrustManager() {

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] certs, String authType)
                    throws CertificateException {
                throw new CertificateException("no clients");
            }

            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] certs, String authType)
                    throws CertificateException {

                if (certs == null || certs.length < 1) {
                    throw new CertificateException("no certs");
                }
                if (certs == null || certs.length > 1) {
                    throw new CertificateException("cert path too long");
                }

                boolean ca_match = false;

                if (cert == null) {
                    // Send a message containing the certificate to our handler.
                    Message m = new Message();
                    m.setTarget(handler);
                    m.what = RemoteClientLibConstants.DIALOG_X509_CERT;
                    m.obj = certs[0];
                    handler.sendMessage(m);

                    synchronized (rfb) {
                        // Block indefinitely until the x509 cert is accepted.
                        while (!rfb.isCertificateAccepted()) {
                            try {
                                rfb.wait();
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                    // We have exited the loop, thus the certificate was accepted.
                    ca_match = true;
                }

                if (!ca_match) {
                    try {
                        PublicKey cakey = cert.getPublicKey();
                        certs[0].verify(cakey);
                        ca_match = true;
                    } catch (Exception e) {
                        ca_match = false;
                    }
                    if (!cert.equals(certs[0])) {
                        throw new CertificateException("certificate does not match");
                    }
                }
            }
        }
        };
    /*  
    } else {
      TrustManagerFactory tmf = TrustManagerFactory.getInstance ("X509");
      KeyStore ks = KeyStore.getInstance (KeyStore.getDefaultType ());
      tmf.init (ks);
      myTM = tmf.getTrustManagers();
    }*/
        sc.init(null, myTM, new SecureRandom());
    }
}
