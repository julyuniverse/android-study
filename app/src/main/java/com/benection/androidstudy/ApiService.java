package com.benection.androidstudy;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/api/auth/login/uuid")
    Call<UuidLoginResponse> loginWithUuid(@Body UuidLoginRequest uuidLoginRequest);

    @POST("/api/auth/login/social")
    Call<SocialLoginResponse> loginWithSocial(@Body SocialLoginRequest socialLoginRequest);

    @POST("/api/auth/token/reissue")
    Call<Token> reissueToken(@Body ReissueTokenRequest reissueTokenRequest);

    @GET("/api/posts")
    Call<Posts> getPosts();
}
