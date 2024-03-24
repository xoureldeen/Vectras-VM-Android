package com.undatech.opaque.util;

import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpsFileDownloader {
    public static String TAG = HttpsFileDownloader.class.toString();

    String url;
    boolean verifySslCerts;
    boolean useCustomCaCert;
    OnDownloadFinishedListener listener;
    Handler handler;
    String certString;
    Certificate cert;

    public HttpsFileDownloader(
            String url, boolean useCustomCaCert, Handler handler, String certString, OnDownloadFinishedListener listener
    ) {
        this.url = url;
        this.useCustomCaCert = useCustomCaCert;
        this.listener = listener;
        this.handler = handler;
        this.certString = certString;
        tryGetCertificateForCertString(certString);
    }

    private void tryGetCertificateForCertString(String certString) {
        try {
            if (certString != null && !certString.equals("")) {
                ByteArrayInputStream in = new ByteArrayInputStream(Base64.decode(certString, Base64.DEFAULT));
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                cert = (X509Certificate) certFactory.generateCertificate(in);
            }
        } catch (CertificateException e) {
            e.printStackTrace();
        }
    }

    public void initDefaultTrustManager(boolean verifySslCerts) {
        Log.d(TAG, "initDefaultTrustManager");
        if (!useCustomCaCert) {
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) throws CertificateException {
                            if (certString != null && !certString.isEmpty() && !cert.equals(certs[0])) {
                                throw new CertificateException("The x509 cert does not match.");
                            }
                        }
                    }
            };

            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void resetDefaultTrustManager() {
        Log.d(TAG, "resetDefaultTrustManager");
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, null, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initiateDownload() {
        initDefaultTrustManager(this.useCustomCaCert);
        // Spin up a thread to grab the file over the network.
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    URL url = new URL(HttpsFileDownloader.this.url);
                    URLConnection ucon = url.openConnection();

                    StringBuilder textBuilder = new StringBuilder();
                    try (Reader reader = new BufferedReader(new InputStreamReader
                            (ucon.getInputStream(), Charset.forName("UTF-8")))) {
                        int c = 0;
                        while ((c = reader.read()) != -1) {
                            textBuilder.append((char) c);
                        }
                    }
                    resetDefaultTrustManager();
                    listener.onDownload(textBuilder.toString());
                } catch (IOException e) {
                    listener.onDownloadFailure();
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    public interface OnDownloadFinishedListener {
        void onDownload(String contents);

        void onDownloadFailure();
    }
}
