package org.tensorflow.lite.examples.detection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ParentDashbaord extends AppCompatActivity {

    private TextView timeMeasured;

    private FirebaseAuth auth;
    private TextView textView;
    private Button button;
    private FirebaseUser user;
    private DatabaseReference screenTimeRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashbaord);

        auth = FirebaseAuth.getInstance();
        textView = findViewById(R.id.user_details);
        button = findViewById(R.id.logout);
        user = auth.getCurrentUser();
        timeMeasured = findViewById(R.id.timerTextPDash);

        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        } else {
            textView.setText(user.getEmail());
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getApplicationContext(), Welcome.class);
                startActivity(intent);
                finish();
            }
        });

        // Initialize database reference to the ScreenTime node
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        screenTimeRef = FirebaseDatabase.getInstance().getReference()
                .child("Registered Users").child(uid).child("Child").child("Child_1").child("ScreenTime");

        // Add ValueEventListener to retrieve the ScreenTime value
        screenTimeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String screenTime = dataSnapshot.getValue(String.class);
                    // Update the timeMeasured TextView with the retrieved value
                    timeMeasured.setText(screenTime);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle errors or onCancelled event as needed
            }
        });


        FloatingActionButton fab = findViewById(R.id.backButtonParentDashboard);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Choose.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
