package com.get;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Connor on 8/22/2015.
 */
public class TokenResponse {
    @SerializedName("key")
    private String key;

    public TokenResponse(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

}
