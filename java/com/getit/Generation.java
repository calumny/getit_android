package com.getit;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Connor on 8/24/2015.
 */
public class Generation {

    @SerializedName("generation")
    private int generation;

    @SerializedName("count")
    private int count;

    public int getGeneration() {
        return generation;
    }

    public int getCount() {
        return count;
    }

}
