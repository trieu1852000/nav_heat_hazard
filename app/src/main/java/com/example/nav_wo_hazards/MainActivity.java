package com.example.nav_wo_hazards;

import okhttp3.logging.HttpLoggingInterceptor;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String BASE_URL = "https://maps.googleapis.com/";
    private static final String TAG = "MainActivity";
    private GoogleMap mMap;
    private AutoCompleteTextView originInput;
    private AutoCompleteTextView destinationInput;
    private TextView durationViewDriving;
    private TextView durationViewWalking;
    private TextView averageTempView;
    private OkHttpClient client;
    private Retrofit retrofit;
    private GeocodingService geocodingService;
    private DirectionsService directionsService;
    private String apiKey = "AIzaSyBr5cW5jLQYwJvSUAPWEFaiHCPjS5pZOak";
    private static final String WEATHER_API_BASE_URL = "https://api.tomorrow.io/";
    private static final String WEATHER_API_KEY = "wmEqVXq7GgE3wZ9Or8cFzVOMKOMprZYt";
    private WeatherService weatherService;
    private Button collapseButton, expandButton, exitButton;
    private ImageButton settingsButton;

    private FusedLocationProviderClient fusedLocationClient;
    private RadioGroup routesGroup;
    private PlacesClient placesClient;
    private Button startButton;
    private Polyline selectedRoute;
    private String selectedRoutePolyline;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private LatLng currentLocation;
    private boolean isNavigationStarted = false;
    private boolean userInteractingWithMap = false;
    private LocationCallback locationCallback;
    private ImageButton recenterButton;
    private boolean shouldRecenterMap = true;
    private TextView remainingTimeView;
    private AutoCompleteTextView secondDestinationInput;
    private Button searchSecondDestinationButton;
    private LatLng destinationLatLng;
    private LatLng secondDestinationLatLng;
    private SharedPreferences sharedPreferences;
    private TextView navigationInstructions;
    private TextView weatherDataTextView;
    private Button fetchWeatherButton;
    private CardView weatherCard;
    private Button searchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        secondDestinationInput = findViewById(R.id.second_destination_input);
        searchSecondDestinationButton = findViewById(R.id.search_second_destination_button);

        // Hide the second destination input and button initially
        secondDestinationInput.setVisibility(View.GONE);
        searchSecondDestinationButton.setVisibility(View.GONE);
        OkHttpClient weatherClient = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();

        Retrofit weatherRetrofit = new Retrofit.Builder()
                .baseUrl(WEATHER_API_BASE_URL)
                .client(weatherClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }

        // Initialize UI elements
        weatherCard = findViewById(R.id.weather_card);
        weatherService = weatherRetrofit.create(WeatherService.class);
        weatherDataTextView = findViewById(R.id.weather_data_textview);
        fetchWeatherButton = findViewById(R.id.fetch_weather_button);
        remainingTimeView = findViewById(R.id.remaining_time_view);
        placesClient = Places.createClient(this);
        recenterButton = findViewById(R.id.recenter_button);
        originInput = findViewById(R.id.origin_input);
        destinationInput = findViewById(R.id.destination_input);
        durationViewDriving = findViewById(R.id.duration_view_driving);
        durationViewWalking = findViewById(R.id.duration_view_walking);
        searchButton = findViewById(R.id.search_button);
        ImageButton collapseButton = findViewById(R.id.collapse_button);
        ImageButton expandButton = findViewById(R.id.expand_button);
        exitButton = findViewById(R.id.exit_button);
        routesGroup = findViewById(R.id.routes_group);
        startButton = findViewById(R.id.start_button);
        settingsButton = findViewById(R.id.settings_button);
        secondDestinationInput = findViewById(R.id.second_destination_input);
        searchSecondDestinationButton = findViewById(R.id.search_second_destination_button);
        sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE);
        navigationInstructions = findViewById(R.id.navigation_instructions);
        averageTempView = findViewById(R.id.average_temp_view);  // Add this line
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupAutoCompleteTextView(secondDestinationInput, placesClient);

        // Setup recenter button click listener
        recenterButton.setOnClickListener(v -> {
            if (currentLocation != null) {
                shouldRecenterMap = true; // Allow the map to recenter
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 18));
            }
        });

        // Setup autofill for origin and destination inputs
        setupAutoCompleteTextView(originInput, placesClient);
        setupAutoCompleteTextView(destinationInput, placesClient);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Setting up an HTTP client with logging capabilities and configuring a Retrofit instance for making network requests
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        client = new OkHttpClient.Builder().addInterceptor(logging).build();
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        geocodingService = retrofit.create(GeocodingService.class);
        directionsService = retrofit.create(DirectionsService.class);

        collapseButton.setVisibility(View.VISIBLE); // Ensure the collapse button is visible initially
        expandButton.setVisibility(View.GONE); // Ensure the expand button is hidden initially

        fetchWeatherButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        fetchWeatherData(currentLatLng);
                    } else {
                        weatherDataTextView.setText("Current location not available");
                        Log.d(TAG, "Current location is null");
                    }
                }).addOnFailureListener(e -> {
                    weatherDataTextView.setText("Failed to get location: " + e.getMessage());
                    Log.e(TAG, "Failed to get location", e);
                });
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            }
        });

        // Setup "Start" button click listener
        startButton.setOnClickListener(v -> {
            if (selectedRoute != null) {
                // Set navigation started to true
                isNavigationStarted = true;

                // Collapse the table and start navigation
                collapseTableAndStartNavigation();

                // Check if location permissions are granted
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    // Permissions are granted, get the current location and zoom
                    if (currentLocation != null) {
                        zoomToCurrentLocation(currentLocation);
                        startNavigation(); // Start navigation after getting the current location
                    } else {
                        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                            if (location != null) {
                                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                zoomToCurrentLocation(currentLocation);
                                startNavigation(); // Start navigation after getting the current location
                            } else {
                                Toast.makeText(MainActivity.this, "Current location not available", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    // Permissions are not granted, request permissions
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE);
                }
            } else {
                Toast.makeText(MainActivity.this, "No route selected", Toast.LENGTH_SHORT).show();
            }
        });

        searchButton.setOnClickListener(v -> {
            try {
                String origin = originInput.getText().toString().trim();
                String destination = destinationInput.getText().toString().trim();

                if (!destination.isEmpty()) {
                    // Make the second destination input and button visible
                    secondDestinationInput.setVisibility(View.VISIBLE);
                    searchSecondDestinationButton.setVisibility(View.VISIBLE);

                    // Force layout update to show the changes immediately
                    findViewById(R.id.constraint_layout).requestLayout();

                    // Fetch coordinates or process destination (your existing logic)
                    fetchCoordinates(origin, destination, null);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a destination", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in searchButton click listener: " + e.getMessage());
                Toast.makeText(MainActivity.this, "An error occurred while searching", Toast.LENGTH_SHORT).show();
            }
        });


        exitButton.setOnClickListener(v -> {
            exitNavigation();
        });

        // Show second destination input and button when user enters a destination
        destinationInput.setOnItemClickListener((parent, view, position, id) -> {
            secondDestinationInput.setVisibility(View.VISIBLE);
            searchSecondDestinationButton.setVisibility(View.VISIBLE);
        });

        searchSecondDestinationButton.setOnClickListener(v -> {
            String secondDestination = secondDestinationInput.getText().toString().trim();
            if (!secondDestination.isEmpty()) {
                fetchSecondDestinationCoordinates(secondDestination);
            } else {
                Toast.makeText(MainActivity.this, "Please enter the second destination", Toast.LENGTH_SHORT).show();
            }
        });

        collapseButton.setOnClickListener(v -> {
            ConstraintLayout constraintLayout = findViewById(R.id.constraint_layout);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);

            // Adjust constraints to collapse the layout while keeping the start button visible
            constraintSet.connect(R.id.origin_input, ConstraintSet.TOP, R.id.collapse_button, ConstraintSet.BOTTOM, 8);
            constraintSet.connect(R.id.map, ConstraintSet.TOP, R.id.start_button, ConstraintSet.BOTTOM, 8);

            constraintSet.applyTo(constraintLayout);
            expandButton.setVisibility(View.VISIBLE); // Show the expand button
            collapseButton.setVisibility(View.GONE); // Hide the collapse button after collapsing

            // Hide the search input fields and other elements when collapsed
            originInput.setVisibility(View.GONE);
            destinationInput.setVisibility(View.GONE);
            secondDestinationInput.setVisibility(View.GONE); // Hide second destination input
            searchSecondDestinationButton.setVisibility(View.GONE); // Hide second destination search button
            searchButton.setVisibility(View.GONE);
            durationViewDriving.setVisibility(View.GONE);
            durationViewWalking.setVisibility(View.GONE);
            routesGroup.setVisibility(View.GONE);
            fetchWeatherButton.setVisibility(View.GONE); // Hide fetch weather button
            weatherCard.setVisibility(View.GONE); // Hide weather card
            averageTempView.setVisibility(View.GONE); // Hide average temperature view
        });

        expandButton.setOnClickListener(v -> {
            ConstraintLayout constraintLayout = findViewById(R.id.constraint_layout);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);

            // Adjust constraints to expand the layout while keeping the start button visible
            constraintSet.connect(R.id.origin_input, ConstraintSet.TOP, R.id.constraint_layout, ConstraintSet.TOP, 8);
            constraintSet.connect(R.id.map, ConstraintSet.TOP, R.id.start_button, ConstraintSet.BOTTOM, 8);

            constraintSet.applyTo(constraintLayout);
            expandButton.setVisibility(View.GONE); // Hide the expand button after expanding
            collapseButton.setVisibility(View.VISIBLE); // Show the collapse button

            // Show the search input fields and other elements when expanded
            originInput.setVisibility(View.VISIBLE);
            destinationInput.setVisibility(View.VISIBLE);
            secondDestinationInput.setVisibility(View.VISIBLE); // Show second destination input
            searchSecondDestinationButton.setVisibility(View.VISIBLE); // Show second destination search button
            searchButton.setVisibility(View.VISIBLE);
            durationViewDriving.setVisibility(View.VISIBLE);
            durationViewWalking.setVisibility(View.VISIBLE);
            routesGroup.setVisibility(View.VISIBLE);
            fetchWeatherButton.setVisibility(View.VISIBLE); // Show fetch weather button
            weatherCard.setVisibility(View.VISIBLE); // Show weather card
            averageTempView.setVisibility(View.VISIBLE); // Show average temperature view
        });





        // Settings button click listener
        settingsButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Ensure the start button is always visible
        startButton.setVisibility(View.VISIBLE);
    }

    private void startNavigation() {
        if (!isNavigationStarted) {
            Toast.makeText(MainActivity.this, "Navigation not started. Click 'Start' to begin navigation.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedRoutePolyline == null) {
            Toast.makeText(MainActivity.this, "No route selected", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(MainActivity.this, "Navigation Started", Toast.LENGTH_SHORT).show();
        shouldRecenterMap = true; // Enable recentering on start
        exitButton.setVisibility(View.VISIBLE);
        navigationInstructions.setVisibility(View.VISIBLE); // Show navigation instructions

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

                LocationRequest locationRequest = LocationRequest.create();
                locationRequest.setInterval(5000);
                locationRequest.setFastestInterval(2000);
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult == null) {
                            return;
                        }
                        for (android.location.Location location : locationResult.getLocations()) {
                            if (location != null) {
                                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                currentLocation = currentLatLng; // Update current location
                                if (shouldRecenterMap) {
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18));
                                }
                                updateNavigation(currentLatLng);
                            }
                        }
                    }
                };

                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

                // Zoom to current location when starting navigation
                if (currentLocation != null) {
                    zoomToCurrentLocation(currentLocation);
                } else {
                    // If current location is not available, get the current location
                    fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                        if (location != null) {
                            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            zoomToCurrentLocation(currentLocation);
                        }
                    });
                }

                // Add markers for origin and final destination
                mMap.clear(); // Clear existing markers and polylines
                mMap.addMarker(new MarkerOptions().position(currentLocation).title("Current Location"));
                if (destinationLatLng != null) {
                    mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Final Destination"));
                }

                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                boundsBuilder.include(currentLocation);
                if (destinationLatLng != null) {
                    boundsBuilder.include(destinationLatLng);
                }
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));

            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Error getting current location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        // Ensure the start button is visible after starting navigation
        startButton.setVisibility(View.VISIBLE);
    }
    private void collapseLayout() {
        secondDestinationInput.setVisibility(View.GONE);
        searchSecondDestinationButton.setVisibility(View.GONE);
        findViewById(R.id.constraint_layout).requestLayout(); // Refresh layout
    }

    private void expandLayout() {
        secondDestinationInput.setVisibility(View.VISIBLE);
        searchSecondDestinationButton.setVisibility(View.VISIBLE);
        findViewById(R.id.constraint_layout).requestLayout(); // Refresh layout
    }

    private void collapseTableAndStartNavigation() {
        // Collapse the table (similar to what collapseButton does)
        ConstraintLayout constraintLayout = findViewById(R.id.constraint_layout);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);

        constraintSet.connect(R.id.origin_input, ConstraintSet.TOP, R.id.collapse_button, ConstraintSet.BOTTOM, 8);
        constraintSet.connect(R.id.map, ConstraintSet.TOP, R.id.collapse_button, ConstraintSet.BOTTOM, 8);

        constraintSet.applyTo(constraintLayout);
        expandButton.setVisibility(View.VISIBLE); // Show the expand button
        collapseButton.setVisibility(View.GONE); // Hide the collapse button after collapsing

        // Hide the search input fields and other elements when collapsed
        originInput.setVisibility(View.GONE);
        destinationInput.setVisibility(View.GONE);
        secondDestinationInput.setVisibility(View.GONE); // Hide second destination input
        searchSecondDestinationButton.setVisibility(View.GONE); // Hide second destination search button
        searchButton.setVisibility(View.GONE);
        durationViewDriving.setVisibility(View.GONE);
        durationViewWalking.setVisibility(View.GONE);
        routesGroup.setVisibility(View.GONE);
        fetchWeatherButton.setVisibility(View.GONE); // Hide fetch weather button
        weatherCard.setVisibility(View.GONE); // Hide weather card

        // Start the navigation
        startNavigation();
    }

    private void fetchSecondDestinationCoordinates(String secondDestination) {
        geocodingService.getGeocoding(secondDestination, apiKey).enqueue(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GeocodingResponse geocodingResponse = response.body();
                    if (geocodingResponse.results != null && !geocodingResponse.results.isEmpty()) {
                        secondDestinationLatLng = new LatLng(geocodingResponse.results.get(0).geometry.location.lat,
                                geocodingResponse.results.get(0).geometry.location.lng);

                        if (currentLocation != null && secondDestinationLatLng != null && destinationLatLng != null) {
                            fetchDirectionsForNewRoute(currentLocation, secondDestinationLatLng, destinationLatLng);
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to get second destination coordinates", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Failed to get second destination coordinates", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Geocoding API request failed for second destination: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchDirectionsForNewRoute(LatLng origin, LatLng secondDestination, LatLng finalDestination) {
        String originStr = origin.latitude + "," + origin.longitude;
        String secondDestinationStr = secondDestination.latitude + "," + secondDestination.longitude;
        String finalDestinationStr = finalDestination.latitude + "," + finalDestination.longitude;

        directionsService.getDirections(originStr, secondDestinationStr, "driving", true, apiKey).enqueue(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DirectionsResponse directionsResponse = response.body();
                    if (directionsResponse.routes != null && !directionsResponse.routes.isEmpty()) {
                        DirectionsResponse.Route firstLeg = directionsResponse.routes.get(0);
                        String firstLegPath = firstLeg.overviewPolyline.points;

                        directionsService.getDirections(secondDestinationStr, finalDestinationStr, "driving", true, apiKey).enqueue(new Callback<DirectionsResponse>() {
                            @Override
                            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    DirectionsResponse directionsResponse = response.body();
                                    if (directionsResponse.routes != null && !directionsResponse.routes.isEmpty()) {
                                        DirectionsResponse.Route secondLeg = directionsResponse.routes.get(0);
                                        String secondLegPath = secondLeg.overviewPolyline.points;

                                        List<LatLng> combinedPath = new ArrayList<>();
                                        combinedPath.addAll(PolyUtil.decode(firstLegPath));
                                        combinedPath.addAll(PolyUtil.decode(secondLegPath));

                                        mMap.clear();
                                        selectedRoute = mMap.addPolyline(new PolylineOptions()
                                                .addAll(combinedPath)
                                                .width(10)
                                                .color(Color.BLUE));
                                        selectedRoutePolyline = PolyUtil.encode(combinedPath); // Store combined polyline

                                        mMap.addMarker(new MarkerOptions().position(origin).title("Origin"));
                                        mMap.addMarker(new MarkerOptions().position(secondDestination).title("Second Destination"));
                                        mMap.addMarker(new MarkerOptions().position(finalDestination).title("Final Destination"));

                                        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                                        for (LatLng point : combinedPath) {
                                            boundsBuilder.include(point);
                                        }
                                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
                                    } else {
                                        Toast.makeText(MainActivity.this, "No routes found for the second leg", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(MainActivity.this, "Failed to fetch directions for the second leg", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                                Toast.makeText(MainActivity.this, "Directions API request failed for the second leg: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(MainActivity.this, "No routes found for the first leg", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Failed to fetch directions for the first leg", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Directions API request failed for the first leg: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void exitNavigation() {
        // Reset the navigation state and UI
        isNavigationStarted = false;

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;  // Ensure the callback is null to prevent re-adding it
        }

        if (mMap != null) {
            mMap.clear(); // Clear all markers and polylines from the map
        }

        if (selectedRoute != null) {
            selectedRoute.remove(); // Remove the polyline from the map
            selectedRoute = null;
        }

        if (originInput != null) {
            originInput.setText("");
        }

        if (destinationInput != null) {
            destinationInput.setText("");
        }

        if (durationViewDriving != null) {
            durationViewDriving.setText("");
        }

        if (durationViewWalking != null) {
            durationViewWalking.setText("");
        }

        if (routesGroup != null) {
            routesGroup.removeAllViews();
        }

        if (exitButton != null) {
            exitButton.setVisibility(View.GONE);
        }

        if (navigationInstructions != null) {
            navigationInstructions.setVisibility(View.GONE); // Hide navigation instructions
        }

        // Ensure the start button remains visible
        if (startButton != null) {
            startButton.setVisibility(View.VISIBLE);
        }

        // Hide search bars and weather data
        originInput.setVisibility(View.GONE);
        destinationInput.setVisibility(View.GONE);
        secondDestinationInput.setVisibility(View.GONE);
        searchSecondDestinationButton.setVisibility(View.GONE);
        searchButton.setVisibility(View.GONE);
        durationViewDriving.setVisibility(View.GONE);
        durationViewWalking.setVisibility(View.GONE);
        routesGroup.setVisibility(View.GONE);
        fetchWeatherButton.setVisibility(View.GONE); // Hide fetch weather button
        weatherCard.setVisibility(View.GONE); // Hide weather card

        // Reset the polyline data
        selectedRoutePolyline = null;

        // Reset the remaining time view
        if (remainingTimeView != null) {
            remainingTimeView.setText("Remaining Time: 0");
        }

        // Clear the second destination LatLng
        secondDestinationLatLng = null;
        destinationLatLng = null; // Ensure this is also cleared

        // Reset the map to the initial state
        if (mMap != null && currentLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
        }
    }

    private void zoomToCurrentLocation(LatLng currentLatLng) {
        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18));
        }
    }
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with location-based actions
            } else {
                // Permission denied
            }
        }
    }
    private void updateNavigation(LatLng currentLatLng) {
        if (mMap != null && selectedRoutePolyline != null) {
            List<LatLng> decodedPath = PolyUtil.decode(selectedRoutePolyline);
            LatLng finalDestinationLatLng = decodedPath.get(decodedPath.size() - 1);

            // Check if the user is off the route
            if (isUserOffRoute(currentLatLng, decodedPath)) {
                fetchMultipleModesDirections(currentLatLng, finalDestinationLatLng);
            } else {
                LatLng nearestPoint = findNearestPoint(currentLatLng, decodedPath);
                List<LatLng> remainingPath = new ArrayList<>();
                boolean foundNearestPoint = false;

                for (LatLng point : decodedPath) {
                    if (!foundNearestPoint && point.equals(nearestPoint)) {
                        foundNearestPoint = true;
                    }
                    if (foundNearestPoint) {
                        remainingPath.add(point);
                    }
                }

                mMap.clear(); // Clear only polylines
                if (secondDestinationLatLng != null) {
                    mMap.addMarker(new MarkerOptions().position(secondDestinationLatLng).title("Second Destination"));
                }
                mMap.addMarker(new MarkerOptions().position(finalDestinationLatLng).title("Final Destination"));

                if (!remainingPath.isEmpty()) {
                    PolylineOptions remainingPolylineOptions = new PolylineOptions()
                            .addAll(remainingPath)
                            .width(10)
                            .color(Color.BLUE);
                    selectedRoute = mMap.addPolyline(remainingPolylineOptions);
                }

                if (shouldRecenterMap) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18));
                }

                updateRemainingTime(currentLatLng, finalDestinationLatLng, getNavigationMode());
                updateNavigationInstructions(currentLatLng, remainingPath);
                fetchAverageTemperature(decodedPath);
            }
        }
    }

    private LatLng findNearestPoint(LatLng currentLatLng, List<LatLng> path) {
        LatLng nearestPoint = null;
        double shortestDistance = Double.MAX_VALUE;

        for (LatLng point : path) {
            double distance = distanceBetweenPoints(currentLatLng, point);
            if (distance < shortestDistance) {
                nearestPoint = point;
                shortestDistance = distance;
            }
        }

        return nearestPoint;
    }

    private double distanceBetweenPoints(LatLng point1, LatLng point2) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(point1.latitude, point1.longitude, point2.latitude, point2.longitude, results);
        return results[0];
    }

    private void setupAutoCompleteTextView(AutoCompleteTextView autoCompleteTextView, PlacesClient placesClient) {
        autoCompleteTextView.setThreshold(1);

        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 1) {
                    FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                            .setQuery(s.toString())
                            .build();

                    placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
                        List<String> suggestions = new ArrayList<>();
                        for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                            suggestions.add(prediction.getFullText(null).toString());
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                                android.R.layout.simple_dropdown_item_1line, suggestions);
                        autoCompleteTextView.setAdapter(adapter);
                    }).addOnFailureListener(exception -> {
                        Log.e(TAG, "Place not found: " + exception.getMessage());
                    });
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        Log.d(TAG, "Map is ready");
        checkLocationPermission();

        // Set up listeners for user interaction
        mMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                userInteractingWithMap = true;
                recenterButton.setVisibility(View.VISIBLE);
                shouldRecenterMap = false;
            }
        });

        mMap.setOnCameraIdleListener(() -> {
            userInteractingWithMap = false;
        });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                LocationRequest locationRequest = LocationRequest.create();
                locationRequest.setInterval(10000);
                locationRequest.setFastestInterval(5000);
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                LocationCallback locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult == null) {
                            Toast.makeText(MainActivity.this, "Location not detected", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        for (android.location.Location location : locationResult.getLocations()) {
                            if (location != null) {
                                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                                fusedLocationClient.removeLocationUpdates(this);
                                break;
                            }
                        }
                    }
                };

                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Error getting current location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchCoordinates(String origin, String destination, LatLng currentLocation) {
        if (origin != null && !origin.isEmpty()) {
            geocodingService.getGeocoding(origin, apiKey).enqueue(new Callback<GeocodingResponse>() {
                @Override
                public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        GeocodingResponse geocodingResponse = response.body();
                        if (geocodingResponse.results != null && !geocodingResponse.results.isEmpty()) {
                            LatLng originLatLng = new LatLng(geocodingResponse.results.get(0).geometry.location.lat,
                                    geocodingResponse.results.get(0).geometry.location.lng);
                            fetchCoordinatesForDestination(originLatLng, destination);
                        } else {
                            Toast.makeText(MainActivity.this, "Failed to get origin coordinates", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to get origin coordinates", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                    Toast.makeText(MainActivity.this, "Geocoding API request failed for origin: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else if (currentLocation != null) {
            fetchCoordinatesForDestination(currentLocation, destination);
        } else {
            Toast.makeText(MainActivity.this, "Please enter origin or ensure location services are enabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchCoordinatesForDestination(LatLng originLatLng, String destination) {
        geocodingService.getGeocoding(destination, apiKey).enqueue(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GeocodingResponse geocodingResponse = response.body();
                    if (geocodingResponse.results != null && !geocodingResponse.results.isEmpty()) {
                        destinationLatLng = new LatLng(geocodingResponse.results.get(0).geometry.location.lat,
                                geocodingResponse.results.get(0).geometry.location.lng);

                        mMap.clear();
                        mMap.addMarker(new MarkerOptions().position(originLatLng).title("Origin"));
                        mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destination"));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(originLatLng, 10));

                        fetchMultipleModesDirections(originLatLng, destinationLatLng);
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to get destination coordinates", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Failed to get destination coordinates", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Geocoding API request failed for destination: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchMultipleModesDirections(LatLng origin, LatLng destination) {
        fetchDirectionsForMode(origin, destination, getNavigationMode());
    }

    public interface DirectionsService {
        @GET("maps/api/directions/json")
        Call<DirectionsResponse> getDirections(
                @Query("origin") String origin,
                @Query("destination") String destination,
                @Query("mode") String mode,
                @Query("alternatives") boolean alternatives,
                @Query("key") String apiKey
        );
    }

    private void fetchDirectionsForMode(LatLng origin, LatLng destination, String mode) {
        String originStr = origin.latitude + "," + origin.longitude;
        String destinationStr = destination.latitude + "," + destination.longitude;

        directionsService.getDirections(originStr, destinationStr, mode, true, apiKey).enqueue(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        DirectionsResponse directionsResponse = response.body();
                        if (directionsResponse.routes != null && !directionsResponse.routes.isEmpty()) {
                            routesGroup.removeAllViews();
                            mMap.clear();  // Clear existing routes on the map
                            int[] colors = {Color.BLUE, Color.RED, Color.GREEN};
                            for (int i = 0; i < directionsResponse.routes.size(); i++) {
                                DirectionsResponse.Route route = directionsResponse.routes.get(i);
                                String encodedPath = route.overviewPolyline.points;
                                List<LatLng> decodedPath = PolyUtil.decode(encodedPath);

                                // Store the polyline string
                                if (i == 0) {  // Assuming we want to navigate using the first route
                                    selectedRoutePolyline = encodedPath;
                                }

                                // Add polyline to the map
                                mMap.addPolyline(new PolylineOptions()
                                        .addAll(decodedPath)
                                        .width(10)
                                        .color(colors[i % colors.length]));

                                String duration = route.legs.get(0).duration.text;
                                RadioButton routeOption = new RadioButton(MainActivity.this);
                                routeOption.setText("Route " + (i + 1) + " (" + duration + ")");
                                int finalI = i;
                                routeOption.setOnClickListener(v -> {
                                    selectedRoute = mMap.addPolyline(new PolylineOptions()
                                            .addAll(decodedPath)
                                            .width(10)
                                            .color(colors[finalI % colors.length]));
                                    selectedRoutePolyline = encodedPath;  // Update the selected route polyline string
                                    fetchAverageTemperature(decodedPath); // Fetch average temperature for selected route
                                });
                                routesGroup.addView(routeOption);
                            }

                            // Zoom out to show all routes
                            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                            for (LatLng point : PolyUtil.decode(selectedRoutePolyline)) {
                                boundsBuilder.include(point);
                            }
                            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
                        } else {
                            Toast.makeText(MainActivity.this, "No routes found", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "No routes found in the response");
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to fetch directions", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed response: " + response.toString());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing directions response: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "An error occurred while processing directions", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Directions API request failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "API call failed: ", t);
            }
        });
    }

    private String getNavigationMode() {
        return sharedPreferences.getString("navigation_mode", "driving");
    }

    private boolean isUserOffRoute(LatLng currentLatLng, List<LatLng> path) {
        // Define a maximum distance the user can be off the route to trigger a reroute (e.g., 50 meters)
        double maxDistance = 50.0; // in meters
        for (LatLng point : path) {
            if (distanceBetweenPoints(currentLatLng, point) < maxDistance) {
                return false; // User is on the route
            }
        }
        return true; // User is off the route
    }

    private void updateRemainingTime(LatLng currentLatLng, LatLng destinationLatLng, String mode) {
        String originStr = currentLatLng.latitude + "," + currentLatLng.longitude;
        String destinationStr = destinationLatLng.latitude + "," + destinationLatLng.longitude;

        directionsService.getDirections(originStr, destinationStr, mode, false, apiKey).enqueue(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DirectionsResponse directionsResponse = response.body();
                    if (directionsResponse.routes != null && !directionsResponse.routes.isEmpty()) {
                        DirectionsResponse.Route route = directionsResponse.routes.get(0);
                        String duration = route.legs.get(0).duration.text;
                        remainingTimeView.setText("Remaining Time: " + duration);
                    } else {
                        remainingTimeView.setText("Remaining Time: N/A");
                    }
                } else {
                    remainingTimeView.setText("Remaining Time: N/A");
                    Log.e(TAG, "Failed to fetch directions");
                }
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                remainingTimeView.setText("Remaining Time: N/A");
                Log.e(TAG, "API call failed: ", t);
            }
        });
    }

    private void updateNavigationInstructions(LatLng currentLatLng, List<LatLng> path) {
        if (path == null || path.isEmpty()) {
            navigationInstructions.setText("No path available");
            return;
        }

        LatLng nextTurnPoint = getNextTurn(currentLatLng, path);
        String turnDirection = nextTurnPoint != null ? getTurnDirection(currentLatLng, nextTurnPoint, path) : "";

        if (nextTurnPoint != null) {
            double distanceToNextTurn = distanceBetweenPoints(currentLatLng, nextTurnPoint);
            double distanceToNextTurnInFeet = distanceToNextTurn * 3.28084; // Convert meters to feet

            if (distanceToNextTurnInFeet < 5280) { // Less than a mile
                navigationInstructions.setText(String.format("Turn %s in %.0f feet", turnDirection, distanceToNextTurnInFeet));
            } else { // More than a mile, convert feet to miles
                double distanceToNextTurnInMiles = distanceToNextTurnInFeet / 5280;
                navigationInstructions.setText(String.format("Turn %s in %.2f miles", turnDirection, distanceToNextTurnInMiles));
            }
            navigationInstructions.setVisibility(View.VISIBLE); // Make sure it's visible
        } else {
            navigationInstructions.setText("You are on the route");
        }
    }

    private LatLng getNextTurn(LatLng currentLocation, List<LatLng> path) {
        for (int i = 0; i < path.size() - 2; i++) {
            LatLng point = path.get(i);
            LatLng nextPoint = path.get(i + 1);
            LatLng nextNextPoint = path.get(i + 2);

            if (isSignificantTurn(point, nextPoint, nextNextPoint)) {
                return nextPoint;
            }
        }
        return null;
    }

    private boolean isSignificantTurn(LatLng point, LatLng nextPoint, LatLng nextNextPoint) {
        double angle = getAngle(point, nextPoint, nextNextPoint);
        return Math.abs(angle) > 30; // Adjust the angle threshold as needed
    }

    private String getTurnDirection(LatLng point, LatLng nextPoint, List<LatLng> path) {
        int index = path.indexOf(nextPoint);

        if (index > 0 && index < path.size() - 1) {
            LatLng prev = path.get(index - 1);
            LatLng next = path.get(index + 1);

            double angle = getAngle(prev, point, nextPoint);
            if (angle > 0) {
                return "right";
            } else {
                return "left";
            }
        }
        return "";
    }

    private double getAngle(LatLng from, LatLng to, LatLng next) {
        double angle1 = Math.atan2(to.latitude - from.latitude, to.longitude - from.longitude);
        double angle2 = Math.atan2(next.latitude - to.latitude, next.longitude - to.longitude);
        double angle = Math.toDegrees(angle2 - angle1);
        if (angle < -180) angle += 360;
        if (angle > 180) angle -= 360;
        return angle;
    }

    private void fetchWeatherData(LatLng location) {
        if (location == null) {
            Log.d(TAG, "Location is null");
            weatherDataTextView.setText("Location not available");
            return;
        }

        String locationString = location.latitude + "," + location.longitude;
        Log.d(TAG, "Fetching weather data for location: " + locationString);

        weatherService.getWeatherData(locationString, "temperature,humidity", "metric", WEATHER_API_KEY)
                .enqueue(new Callback<WeatherResponse>() {
                    @Override
                    public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            WeatherResponse weatherResponse = response.body();
                            if (weatherResponse.data != null && !weatherResponse.data.timelines.isEmpty()) {
                                WeatherResponse.Timeline timeline = weatherResponse.data.timelines.get(0);
                                if (timeline.intervals != null && !timeline.intervals.isEmpty()) {
                                    WeatherResponse.Interval interval = timeline.intervals.get(0);
                                    if (interval.values != null) {
                                        double temperature = interval.values.temperature;
                                        double humidity = interval.values.humidity;
                                        double heatIndex = calculateHeatIndex(temperature, humidity);

                                        String weatherData = String.format("Current Temperature: %.2f°C\nHumidity: %.2f%%\nHeat Index: %.2f°C",
                                                temperature, humidity, heatIndex);
                                        weatherDataTextView.setText(weatherData);
                                    } else {
                                        weatherDataTextView.setText("No weather values found");
                                    }
                                } else {
                                    weatherDataTextView.setText("No weather intervals found");
                                }
                            } else {
                                weatherDataTextView.setText("No timelines found");
                            }
                        } else {
                            try {
                                String errorBody = response.errorBody().string();
                                weatherDataTextView.setText("Failed to fetch weather data: " + response.message() + "\n" + errorBody);
                                Log.e(TAG, "Response Error: " + errorBody);
                            } catch (Exception e) {
                                weatherDataTextView.setText("Failed to fetch weather data: " + response.message());
                                Log.e(TAG, "Error reading error body", e);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<WeatherResponse> call, Throwable t) {
                        weatherDataTextView.setText("API request failed: " + t.getMessage());
                        Log.e(TAG, "API Failure: ", t);
                    }
                });
    }


    // Method to calculate heat index
    private double calculateHeatIndex(double temperature, double humidity) {
        double c1 = -8.78469475556;
        double c2 = 1.61139411;
        double c3 = 2.33854883889;
        double c4 = -0.14611605;
        double c5 = -0.012308094;
        double c6 = -0.0164248277778;
        double c7 = 0.002211732;
        double c8 = 0.00072546;
        double c9 = -0.000003582;

        double T = temperature;
        double R = humidity;

        double heatIndex = c1 + c2*T + c3*R + c4*T*R + c5*T*T + c6*R*R + c7*T*T*R + c8*T*R*R + c9*T*T*R*R;

        return heatIndex;
    }

    private void fetchAverageTemperature(List<LatLng> decodedPath) {
        int pathSize = decodedPath.size();
        if (pathSize < 3) {
            averageTempView.setText("N/A");
            return;
        }

        List<LatLng> points = new ArrayList<>();
        points.add(decodedPath.get(0));
        points.add(decodedPath.get(pathSize / 2));
        points.add(decodedPath.get(pathSize - 1));

        // Implement caching mechanism
        SharedPreferences preferences = getSharedPreferences("weather_cache", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        List<Double> temperatures = new ArrayList<>();
        for (LatLng point : points) {
            String locationKey = point.latitude + "," + point.longitude;
            long lastFetchedTime = preferences.getLong(locationKey + "_time", 0);
            long currentTime = System.currentTimeMillis();
            double cachedTemperature = preferences.getFloat(locationKey + "_temperature", Float.NaN);

            if (currentTime - lastFetchedTime < 600000 && !Double.isNaN(cachedTemperature)) { // 10 minutes cache duration
                temperatures.add(cachedTemperature);
                if (temperatures.size() == 3) {
                    double averageTemp = (temperatures.get(0) + temperatures.get(1) + temperatures.get(2)) / 3.0;
                    averageTempView.setText(String.format("Average Temperature: %.2f°C", averageTemp));
                }
                continue;
            }

            // Exponential backoff implementation
            final int MAX_RETRIES = 5;
            int retryCount = 0;
            long backoffTime = 1000; // initial backoff time in ms

            while (retryCount < MAX_RETRIES) {
                try {
                    weatherService.getWeatherData(point.latitude + "," + point.longitude, "temperature", "metric", WEATHER_API_KEY)
                            .enqueue(new Callback<WeatherResponse>() {
                                @Override
                                public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        WeatherResponse weatherResponse = response.body();
                                        if (weatherResponse.data != null && !weatherResponse.data.timelines.isEmpty()) {
                                            WeatherResponse.Timeline timeline = weatherResponse.data.timelines.get(0);
                                            if (timeline.intervals != null && !timeline.intervals.isEmpty()) {
                                                WeatherResponse.Interval interval = timeline.intervals.get(0);
                                                if (interval.values != null) {
                                                    double temperature = interval.values.temperature;
                                                    temperatures.add(temperature);
                                                    editor.putLong(locationKey + "_time", currentTime);
                                                    editor.putFloat(locationKey + "_temperature", (float) temperature);
                                                    editor.apply();
                                                    if (temperatures.size() == 3) {
                                                        double averageTemp = (temperatures.get(0) + temperatures.get(1) + temperatures.get(2)) / 3.0;
                                                        averageTempView.setText(String.format("Average Temperature: %.2f°C", averageTemp));
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        handleRateLimit(response);
                                    }
                                }

                                @Override
                                public void onFailure(Call<WeatherResponse> call, Throwable t) {
                                    Log.e(TAG, "API request failed: ", t);
                                }
                            });
                    break; // exit the retry loop if the call was successful
                } catch (Exception e) {
                    retryCount++;
                    try {
                        Thread.sleep(backoffTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    backoffTime *= 2; // exponential backoff
                }
            }
        }
    }

    private void handleRateLimit(Response<WeatherResponse> response) {
        if (response.code() == 429) {
            // Handle rate limiting error
            Log.e(TAG, "Rate limit exceeded: " + response.message());
            averageTempView.setText("Rate limit exceeded. Please try again later.");
        }
    }


}
