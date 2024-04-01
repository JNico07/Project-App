package org.tensorflow.lite.examples.detection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class Login extends AppCompatActivity {

    // Declare objects
    FirebaseAuth mAuth;
    TextInputEditText editTextEmail, editTextPassword;
    Button buttonLogin;
    ProgressBar progressBar;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Initialize the objects
        mAuth = FirebaseAuth.getInstance();
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progressBar);
        textView = findViewById(R.id.registerNow);


        // on click listener for TextView
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Register.class); // Intent, to open Login activity
                startActivity(intent); // to Start the activity
                finish(); // finish the current activity
            }
        });

        // on click listener for Button
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // set progress bar visibility
                progressBar.setVisibility(View.VISIBLE);
                // read text from EditText
                String email, password;
                email = String.valueOf(editTextEmail.getText());
                password = String.valueOf(editTextPassword.getText());

                // check if Email or Password is empty of not
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(Login.this, "Enter Email", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(Login.this, "Enter Password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // authenticate, sign in with email and password (Firebase)
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    // check if user's Email is Verified
                                    if (Objects.requireNonNull(mAuth.getCurrentUser()).isEmailVerified()) {
                                        // Sign in success, display a message to the user.
                                        Toast.makeText(Login.this, "Login success.", Toast.LENGTH_SHORT).show();
                                        // open the "Choose" Activity
                                        Intent intent = new Intent(getApplicationContext(), Choose.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                    else {
                                        Toast.makeText(Login.this, "Please verify your Email address", Toast.LENGTH_LONG).show();
                                    }


                                } else {
                                    // If sign in fails, display a message to the user.
                                    Toast.makeText(Login.this, "Login failed. Email or Password is incorrect", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
        FloatingActionButton fab = findViewById(R.id.backButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Welcome.class);
                startActivity(intent);
                finish();
            }
        });

    }

    // Check if user is already logged in, then it will open the "Choose" Activity
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null && currentUser.isEmailVerified()){
            Intent intent = new Intent(getApplicationContext(), Choose.class);
            startActivity(intent);
            finish();
        }
    }

}