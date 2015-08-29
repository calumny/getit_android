package com.getit;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Connor on 8/22/2015.
 */
public class TokenResponse {
    @SerializedName("key")
    private String key;

    public String getKey() {
        return key;
    }

}
