package org.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.tensorflow.lite.examples.detection.parentDB.ParentDashbaord;

public class Choose extends AppCompatActivity {

    TextView parentDashboard, monitorMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose);
        parentDashboard = findViewById(R.id.parent_dashboard);
        monitorMode = findViewById(R.id.monitor_mode);

        // to Parent's Dashboard
        parentDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ParentDashbaord.class);
                startActivity(intent);
                finish();
            }
        });

        // to Track Screen-time
        monitorMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DetectorActivity.class);
                startActivity(intent);
                finish();
            }
        });


    }
}