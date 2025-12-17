package com.vectras.vm.network;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RequestNetworkController {
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";

    public static final int REQUEST_PARAM = 0;
    public static final int REQUEST_BODY = 1;

    private static final int SOCKET_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 25000;

    protected OkHttpClient client;

    private static RequestNetworkController mInstance;

    public static synchronized RequestNetworkController getInstance() {
        if(mInstance == null) {
            mInstance = new RequestNetworkController();
        }
        return mInstance;
    }

    private OkHttpClient getClient() {
        if (client == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();

            try {
                final TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[]{};
                            }
                        }
                };

                final SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new SecureRandom());
                final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
                builder.connectTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS);
                builder.readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);
                builder.writeTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);
                builder.cache(null);
                builder.hostnameVerifier((hostname, session) -> true);
            } catch (Exception ignored) {
            }

            client = builder.build();
        }

        return client;
    }

    public void execute(final RequestNetwork requestNetwork, String method, String url, final String tag, final RequestNetwork.RequestListener requestListener) {
        Request.Builder reqBuilder = new Request.Builder();
        Headers.Builder headerBuilder = new Headers.Builder();
        CacheControl.Builder cacheControlBuilder = new CacheControl.Builder()
                .noCache()
                .noStore();

        if (!requestNetwork.getHeaders().isEmpty()) {
            HashMap<String, Object> headers = requestNetwork.getHeaders();

            for (HashMap.Entry<String, Object> header : headers.entrySet()) {
                headerBuilder.add(header.getKey(), String.valueOf(header.getValue()));
            }
        }

        try {
            if (requestNetwork.getRequestType() == REQUEST_PARAM) {
                if (method.equals(GET)) {
                    HttpUrl.Builder httpBuilder;

                    try {
                        httpBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
                    } catch (NullPointerException ne) {
                        throw new NullPointerException("unexpected url: " + url);
                    }

                    if (!requestNetwork.getParams().isEmpty()) {
                        HashMap<String, Object> params = requestNetwork.getParams();

                        for (HashMap.Entry<String, Object> param : params.entrySet()) {
                            httpBuilder.addQueryParameter(param.getKey(), String.valueOf(param.getValue()));
                        }
                    }

                    reqBuilder.url(httpBuilder.build()).headers(headerBuilder.build()).cacheControl(cacheControlBuilder.build()).get();
                } else {
                    FormBody.Builder formBuilder = new FormBody.Builder();
                    if (!requestNetwork.getParams().isEmpty()) {
                        HashMap<String, Object> params = requestNetwork.getParams();

                        for (HashMap.Entry<String, Object> param : params.entrySet()) {
                            formBuilder.add(param.getKey(), String.valueOf(param.getValue()));
                        }
                    }

                    RequestBody reqBody = formBuilder.build();

                    reqBuilder.url(url).headers(headerBuilder.build()).method(method, reqBody).cacheControl(cacheControlBuilder.build());
                }
            } else {
                RequestBody reqBody = RequestBody.create(
                        new Gson().toJson(requestNetwork.getParams()), MediaType.get("application/json; charset=utf-8")
                );

                if (method.equals(GET)) {
                    reqBuilder.url(url).headers(headerBuilder.build()).cacheControl(cacheControlBuilder.build()).get();
                } else {
                    reqBuilder.url(url).headers(headerBuilder.build()).method(method, reqBody).cacheControl(cacheControlBuilder.build());
                }
            }

            Request req = reqBuilder.build();

            getClient().newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull final IOException e) {
                    requestNetwork.getActivity().runOnUiThread(() -> requestListener.onErrorResponse(tag, e.getMessage()));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
                    final String responseBody = response.body().string().trim();
                    requestNetwork.getActivity().runOnUiThread(() -> {
                        Headers b = response.headers();
                        HashMap<String, Object> map = new HashMap<>();
                        for (String s : b.names()) {
                            map.put(s, b.get(s) != null ? b.get(s) : "null");
                        }
                        requestListener.onResponse(tag, responseBody, map);
                    });
                }
            });
        } catch (Exception e) {
            requestListener.onErrorResponse(tag, e.getMessage());
        }
    }
}
