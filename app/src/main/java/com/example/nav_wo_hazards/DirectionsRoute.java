package com.example.nav_wo_hazards;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DirectionsRoute {
    @SerializedName("overview_polyline")
    public Polyline overviewPolyline;

    public List<Leg> legs;

    // Additional fields can be added as necessary
}
