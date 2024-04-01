package org.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

public class MeasureScreenTime extends AppCompatActivity {

    private Chronometer chronometer;
    private long PauseOffSet = 0;
    private boolean isPlaying = false;
    private ToggleButton toggleButton;
    private Button reset_button, move_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure_screen_time);

        chronometer = findViewById(R.id.chronometer);
        toggleButton = findViewById(R.id.Toggle);
        reset_button = findViewById(R.id.reset_btn);
        move_button = findViewById(R.id.move_btn);

        toggleButton.setText(null);
        toggleButton.setTextOff(null);
        toggleButton.setTextOn(null);

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                // stopwatch
                if (isChecked) {
                    chronometer.setBase(SystemClock.elapsedRealtime() - PauseOffSet);
                    chronometer.start();
                    isPlaying = true;
                } else {
                    chronometer.stop();
                    PauseOffSet = SystemClock.elapsedRealtime() - chronometer.getBase();
                    isPlaying = false;
                }
            }
        });

        reset_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPlaying) {
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    PauseOffSet = 0;
                    chronometer.start();
                    isPlaying = true;
                }
            }
        });
        move_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DetectorActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }
}