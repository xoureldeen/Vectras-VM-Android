package com.vectras.vm.network;

import com.vectras.vm.model.GithubUser;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface GithubApiService {
    @GET("users/{username}")
    Call<GithubUser> getUser(@Path("username") String username);
}