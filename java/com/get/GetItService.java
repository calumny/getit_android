package com.get;

import org.joda.time.DateTime;

import java.util.List;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;

/**
 * Created by Connor on 8/19/2015.
 */
public interface GetItService {
    @POST("/api/get_it/")
    void getIt(@Body GetItLocation location, Callback<Boolean> cb);

    @POST("/api/give_it/")
    void giveIt(@Body GetItLocation location, Callback<Boolean> cb);

    @POST("/api/confirm_location/")
    void confirmLocation(@Body GetItLocation location, Callback<Boolean> cb);

    @GET("/api/status/")
    void getStatus(Callback<Boolean> cb);

    @GET("/api/countdown/")
    void getCountdown(Callback<String> cb);

    @GET("/api/get_stats/")
    void getStats(Callback<Statistics> cb);

    @GET("/api/get_generations/")
    void getGenerations(Callback<List<Generation>> cb);

    @GET("/api/count/")
    void getCount(Callback<Integer> cb);

    @GET("/api/did_i_give_it/")
    void checkGaveIt(Callback<Integer> cb);
}
