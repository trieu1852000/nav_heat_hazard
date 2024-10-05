package com.example.nav_wo_hazards;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherService {
    @GET("v4/timelines")
    Call<WeatherResponse> getWeatherData(
            @Query("location") String location,
            @Query("fields") String fields,
            @Query("units") String units,
            @Query("apikey") String apiKey
    );
}
