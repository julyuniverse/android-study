package com.benection.androidstudy;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface NetworkManager {

    @POST("/api/auth/login/uuid")
    @Headers("Platform: Android")
    Call<UuidLoginResponse> loginWithSocialProvider(@Body UuidLoginRequest uuidLoginRequest);
}
