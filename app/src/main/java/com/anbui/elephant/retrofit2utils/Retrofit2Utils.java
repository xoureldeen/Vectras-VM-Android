package com.anbui.elephant.retrofit2utils;

import androidx.annotation.NonNull;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class Retrofit2Utils {
    public static final int NETWORK_ERROR = -1;
    public static final int TIMEOUT = -2;

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://dummy.com/")
            .client(client)
            .build();

    private static final ApiService api = retrofit.create(ApiService.class);

    public interface Retrofit2Callback {
        void onResult(boolean isSuccess, String body, int status, Throwable error);
    }

    public static void get(String url, Retrofit2Callback callback) {
        api.getRawJson(url).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                try (ResponseBody responseBody = response.body()) {
                    String body = responseBody == null ? "" : responseBody.string();
                    callback.onResult(response.isSuccessful(), body, response.code(),null);
                } catch (Exception e) {
                    callback.onResult(false, "", response.code(), e);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (t instanceof SocketTimeoutException) {
                    callback.onResult(false, "" ,TIMEOUT, t);
                } else {
                    callback.onResult(false, "", NETWORK_ERROR, t);
                }
            }
        });
    }

    public static void post(String url, String jsonRaw, Retrofit2Callback callback) {
        RequestBody body = RequestBody.create(
                jsonRaw,
                MediaType.get("application/json; charset=utf-8")
        );

        api.post(url, body).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                try (ResponseBody responseBody = response.body()) {
                    String body = responseBody == null ? "" : responseBody.string();
                    callback.onResult(response.isSuccessful(), body, response.code(), null);
                } catch (Exception e) {
                    callback.onResult(false, "", response.code(), e);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (t instanceof SocketTimeoutException) {
                    callback.onResult(false, "" ,TIMEOUT, t);
                } else {
                    callback.onResult(false, "", NETWORK_ERROR, t);
                }
            }
        });
    }
}
