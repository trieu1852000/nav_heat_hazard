package com.example.nav_wo_hazards;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GeocodingResponse {
    @SerializedName("results")
    public List<Result> results;

    public static class Result {
        @SerializedName("geometry")
        public Geometry geometry;
    }

    public static class Geometry {
        @SerializedName("location")
        public Location location;
    }

    public static class Location {
        @SerializedName("lat")
        public double lat;

        @SerializedName("lng")
        public double lng;
    }
}
