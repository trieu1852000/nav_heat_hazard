package com.example.nav_wo_hazards;

import com.google.gson.annotations.SerializedName;

public class Step {
    @SerializedName("start_location")
    public Location startLocation;
    @SerializedName("end_location")
    public Location endLocation;
    @SerializedName("polyline")
    public Polyline polyline;
    @SerializedName("travel_mode")
    public String travelMode;
}
