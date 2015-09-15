package com.get;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Connor on 8/22/2015.
 */
public class Credentials {
    @SerializedName("password")
    private String password;

    @SerializedName("username")
    private String username;

    public Credentials(String username, String password) {
        this.password = password;
        this.username = username;
    }
}
