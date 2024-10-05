package com.example.nav_wo_hazards;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DirectionsService {
    @GET("maps/api/directions/json")
    Call<DirectionsResponse> getDirections(
            @Query("origin") String origin,
            @Query("destination") String destination,
            @Query("mode") String mode,
            @Query("key") String apiKey
    );
}
