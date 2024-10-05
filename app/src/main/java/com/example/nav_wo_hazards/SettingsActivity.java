package com.example.nav_wo_hazards;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    private RadioGroup modeGroup;
    private RadioButton drivingRadioButton, walkingRadioButton;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        modeGroup = findViewById(R.id.mode_group);
        drivingRadioButton = findViewById(R.id.radio_driving);
        walkingRadioButton = findViewById(R.id.radio_walking);
        sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE);

        // Load saved mode
        String savedMode = sharedPreferences.getString("navigation_mode", "driving");
        if (savedMode.equals("walking")) {
            walkingRadioButton.setChecked(true);
        } else {
            drivingRadioButton.setChecked(true);
        }

        // Save selected mode
        modeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (checkedId == R.id.radio_walking) {
                editor.putString("navigation_mode", "walking");
            } else {
                editor.putString("navigation_mode", "driving");
            }
            editor.apply();
        });
    }
}
