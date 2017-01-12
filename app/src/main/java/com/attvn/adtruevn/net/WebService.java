package com.attvn.adtruevn.net;

import com.attvn.adtruevn.model.AuthInfo;
import com.attvn.adtruevn.model.DeviceInfo;
import com.attvn.adtruevn.model.Playlist;
import com.attvn.adtruevn.model.SessionInfo;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import rx.Observable;

/**
 * Created by app on 1/5/17.
 */

public interface WebService {

    // login and authorization
    @Headers("Content-Type: application/json")
    @POST("user/register.json")
    Observable<DeviceInfo> register(@Body DeviceInfo userInfo);

    @Headers("Content-Type: application/json")
    @POST("user/login.json")
    Observable<SessionInfo> login(@Body AuthInfo authInfo);

    // get data from json file
    @GET("playlist.json")
    Observable<Playlist> getPlaylist();

    // check on/off status
    @POST("status.json")
    Observable<String> checkOnStatus();
}
