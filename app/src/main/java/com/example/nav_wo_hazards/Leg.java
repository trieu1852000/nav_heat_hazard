package com.example.nav_wo_hazards;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Leg {
    @SerializedName("steps")
    public List<Step> steps;
    @SerializedName("duration")
    public Duration duration;
}
