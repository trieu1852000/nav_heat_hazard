package com.example.nav_wo_hazards;

import java.util.List;

public class WeatherResponse {
    public Data data;

    public static class Data {
        public List<Timeline> timelines;
    }

    public static class Timeline {
        public String timestep;
        public String endTime;
        public String startTime;
        public List<Interval> intervals;
    }

    public static class Interval {
        public String startTime;
        public Values values;
    }

    public static class Values {
        public double temperature;
        public double humidity;
    }
}
