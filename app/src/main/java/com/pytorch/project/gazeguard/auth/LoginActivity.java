package com.pytorch.project.gazeguard.auth;

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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pytorch.project.gazeguard.common.ChooseActivity;
import com.pytorch.project.gazeguard.common.EULA;
import com.pytorch.project.gazeguard.common.WelcomeActivity;

import org.pytorch.demo.objectdetection.R;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    // Declare objects
    public FirebaseAuth mAuth;
    TextInputEditText editTextEmail, editTextPassword;
    Button buttonLogin;
    ProgressBar progressBar;
    TextView textViewRegister;

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
        textViewRegister = findViewById(R.id.registerNow);

        // Check if EULA needs to be shown
        EULA();

        // on click listener for TextView
        textViewRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
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
                    Toast.makeText(LoginActivity.this, "Enter Email", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(LoginActivity.this, "Enter Password", Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(LoginActivity.this, "Login success.", Toast.LENGTH_SHORT).show();
                                        // open the "Choose" Activity
                                        Intent intent = new Intent(getApplicationContext(), ChooseActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                    else {
                                        Toast.makeText(LoginActivity.this, "Please verify your Email address", Toast.LENGTH_LONG).show();
                                    }


                                } else {
                                    // If sign in fails, display a message to the user.
                                    Toast.makeText(LoginActivity.this, "Login failed. Email or Password is incorrect", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
//        FloatingActionButton fab = findViewById(R.id.backButton);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(getApplicationContext(), WelcomeActivity.class);
//                startActivity(intent);
//                finish();
//            }
//        });

    }
    private void EULA() {
        // Check if EULA needs to be shown
        if (!EULA.isEULAAccepted(this)) {
            EULA.showEULA(this, new EULA.EULAListener() {
                @Override
                public void onEULAAccepted() {
                    // Continue with normal login flow
                }

                @Override
                public void onEULADeclined() {
                    // Exit the app
                    finishAffinity();
                }
            });
        }
    }

    // Check if user is already logged in, then it will open the "Choose" Activity
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null && currentUser.isEmailVerified()){
            Intent intent = new Intent(getApplicationContext(), ChooseActivity.class);
            startActivity(intent);
            finish();
        }
    }


    //
    private boolean backToExitPressedOnce = false;
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(getApplicationContext(), WelcomeActivity.class);
        startActivity(intent);
    }
//    @Override
//    public boolean onSupportNavigateUp() {
//        onBackPressed();
//        return true;
//    }
}