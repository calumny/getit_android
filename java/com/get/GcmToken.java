package com.get;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Connor on 9/12/2015.
 */
public class GcmToken {
    @SerializedName("dev_id")
    private String deviceId;

    @SerializedName("reg_id")
    private String gcmToken;

    public GcmToken(String deviceId, String gcmToken) {
        this.deviceId = deviceId;
        this.gcmToken = gcmToken;
    }
}
