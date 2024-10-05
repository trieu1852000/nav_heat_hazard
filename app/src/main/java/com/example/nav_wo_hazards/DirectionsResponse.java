package com.example.nav_wo_hazards;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DirectionsResponse {
    @SerializedName("routes")
    public List<Route> routes;

    public static class Route {
        @SerializedName("legs")
        public List<Leg> legs;
        @SerializedName("overview_polyline")
        public OverviewPolyline overviewPolyline;
    }

    public static class Leg {
        @SerializedName("duration")
        public Duration duration;
    }

    public static class Duration {
        @SerializedName("text")
        public String text;
    }

    public static class OverviewPolyline {
        @SerializedName("points")
        public String points;
    }
}
