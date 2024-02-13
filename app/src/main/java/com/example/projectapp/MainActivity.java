package com.example.projectapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    // Declare objects
    FirebaseAuth auth;
    TextView textView;
    Button button;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize the objects
        auth = FirebaseAuth.getInstance();
        textView = findViewById(R.id.user_details);
        button = findViewById(R.id.logout);
        user = auth.getCurrentUser();

        // check if the user is null or not
        if (user == null) {
            // open the "Log in" activity and close the "Main Activity"
            Intent intent =  new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        } else {
            textView.setText(user.getEmail());
        }

        // set onclick listener for Button
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // to sign-out the user
                FirebaseAuth.getInstance().signOut();
                // open the "Log in" activity and close the "Main Activity"
                Intent intent =  new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
                finish();
            }
        });

    }
}