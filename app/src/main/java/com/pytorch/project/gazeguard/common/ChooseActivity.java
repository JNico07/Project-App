    package com.pytorch.project.gazeguard.common;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pytorch.project.gazeguard.auth.LoginActivity;
import com.pytorch.project.gazeguard.monitoringmode.MainActivity;
import com.pytorch.project.gazeguard.parentdashboard.ParentDashboardActivity;

import org.pytorch.demo.objectdetection.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

    public class ChooseActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    FirebaseUser currentUser;
    private DatabaseReference childDatabase;
    TextView parentDashboard, monitorMode;
    private SharedPreferences prefsUserName;
    private String uid;
    //    Dialog Box
    private Button btnExit;
    private Dialog dialog;
    private Button btnDialogConfirm, btnDialogExit;
    private TextInputEditText editTextUserName;
    public String childNumber;
    private boolean isUsernameSet = false;
    private String savedUserName = "";
    private static final String PREF_USERNAME_KEY = "username";
    private static final String PREF_USERNAME_SET_KEY = "username_set";
    private static final String PREF_CHILD_NUMBER_KEY = "child_number"; // Define a key for child number

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose);

        // Initialization
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            uid = currentUser.getUid();
        } else {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        parentDashboard = findViewById(R.id.parent_dashboard);
        monitorMode = findViewById(R.id.monitor_mode);

        EULA();

        parentDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ParentDashboardActivity.class);
                startActivity(intent);
                finish();
            }
        });

        monitorMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SharedPrefsUtil.isUserNameSet(ChooseActivity.this)) {
                    childNumber = SharedPrefsUtil.getChildNumber(ChooseActivity.this);

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    showUsernameDialog();
                }
            }
        });
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

    private void showUsernameDialog() {
        dialog = new Dialog(ChooseActivity.this);
        dialog.setContentView(R.layout.custom_dialog_box);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.custom_dialog_bg));
        dialog.setCancelable(false);

        dialog.show();

        btnDialogExit = dialog.findViewById(R.id.btnDialogExit);
        btnDialogConfirm = dialog.findViewById(R.id.btnDialogConfirm);
        editTextUserName = dialog.findViewById(R.id.userName);

        childDatabase = FirebaseDatabase.getInstance().getReference().child("Registered Users").child(uid).child("Child");

        // Generate a random 6-digit number
        String childNumber = generateRandomChildNumber();

        btnDialogConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dateCreated = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                String childUserName = String.valueOf(editTextUserName.getText()).trim();

                // Input validation
                if (TextUtils.isEmpty(childUserName)) {
                    Toast.makeText(ChooseActivity.this, "Please enter Username", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check for spaces only
                if (childUserName.matches("^\\s*$")) {
                    Toast.makeText(ChooseActivity.this, "Username cannot contain only spaces", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check for valid username pattern (letters, numbers, and underscores only)
                if (!childUserName.matches("^[a-zA-Z0-9_]+$")) {
                    Toast.makeText(ChooseActivity.this, "can only contain letters, numbers, underscores", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (currentUser != null) {
                    childDatabase = FirebaseDatabase.getInstance().getReference()
                            .child("Registered Users").child(uid).child("Child").child(childNumber).child("name");
                    childDatabase.setValue(childUserName);
                    childDatabase = FirebaseDatabase.getInstance().getReference()
                            .child("Registered Users").child(uid).child("Child").child(childNumber).child("dateCreated");
                    childDatabase.setValue(dateCreated);

                    SharedPrefsUtil.setUserName(ChooseActivity.this, childUserName);
                    SharedPrefsUtil.setChildNumber(ChooseActivity.this, childNumber);
                }

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();

                dialog.dismiss();
            }
        });

        btnDialogExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ChooseActivity.class);
                startActivity(intent);
                dialog.dismiss();
            }
        });
    }

    private String generateRandomChildNumber() {
        Random random = new Random();
        int number = random.nextInt(900000) + 100000; // Generates a number between 100000 and 999999
        return "child" + number;
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(getApplicationContext(), WelcomeActivity.class);
        startActivity(intent);
    }
}