package com.getit;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Connor on 8/24/2015.
 */
public class GetItLocation {

    @SerializedName("lat")
    private double latitude;

    @SerializedName("lon")
    private double longitude;

    public GetItLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

}
