package com.getit;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;

/**
 * Created by Connor on 8/19/2015.
 */
public interface AuthenticationService {
    @POST("/api/register/")
    void register(@Body Credentials credentials, Callback<TokenResponse> cb);

    @POST("/api/get_token/")
    void getToken(@Body Credentials credentials, Callback<TokenResponse> cb);
}