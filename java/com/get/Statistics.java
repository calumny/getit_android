package com.get;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Connor on 9/12/2015.
 */
public class Statistics {
    @SerializedName("has_it_count")
    private Integer hasItCount;

    @SerializedName("distance")
    private Float distance;

    @SerializedName("players_per_second")
    private Float playersPerSecond;

    @SerializedName("mph")
    private Float milesPerHour;

    @SerializedName("reset_date")
    private String resetDateString;

    public Integer getHasItCount(){
        return hasItCount;
    }

    public Float getDistance() {
        return distance;
    }

    public Float getPlayersPerSecond() {
        return playersPerSecond;
    }

    public Float getMilesPerHour() {
        return milesPerHour;
    }

    public String getResetDateString() {
        return resetDateString;
    }

    public Statistics(Integer hasItCount, Float distance, Float playersPerSecond, Float milesPerHour, String resetDateString) {
        this.hasItCount = hasItCount;
        this.distance = distance;
        this.playersPerSecond = playersPerSecond;
        this.milesPerHour = milesPerHour;
        this.resetDateString = resetDateString;
    }
}
