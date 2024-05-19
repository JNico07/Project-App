package org.tensorflow.lite.examples.detection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import org.tensorflow.lite.examples.detection.parentDB.ParentDashbaord;

public class Choose extends AppCompatActivity {

    private FirebaseAuth mAuth;
    FirebaseUser currentUser;
    private DatabaseReference userNameDatabase;
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
        parentDashboard = findViewById(R.id.parent_dashboard);
        monitorMode = findViewById(R.id.monitor_mode);

        prefsUserName = PreferenceManager.getDefaultSharedPreferences(this);

        // Firebase DB
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            uid = currentUser.getUid();
        } else {
            // Handle case where user is not logged in
            // For example, redirect to login activity
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
            return;
        }

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

                isUsernameSet = prefsUserName.getBoolean(PREF_USERNAME_SET_KEY, false);
                if (isUsernameSet) {
                    savedUserName = prefsUserName.getString(PREF_USERNAME_KEY, "");
                    childNumber = prefsUserName.getString(PREF_CHILD_NUMBER_KEY, ""); // Retrieve the child number

                    Intent intent = new Intent(getApplicationContext(), DetectorActivity.class);
                    startActivity(intent);
                    finish();

                } else {
                    showUsernameDialog();
                }

            }
        });

    }

    private void showUsernameDialog() {

        dialog = new Dialog(Choose.this);
        dialog.setContentView(R.layout.custom_dialog_box);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.custom_dialog_bg));
        dialog.setCancelable(false);

        dialog.show();

        btnDialogExit = dialog.findViewById(R.id.btnDialogExit);
        btnDialogConfirm = dialog.findViewById(R.id.btnDialogConfirm);
        editTextUserName = dialog.findViewById(R.id.userName);

        userNameDatabase = FirebaseDatabase.getInstance().getReference().child("Registered Users").child(uid).child("Child");

        userNameDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int childCount = (int) dataSnapshot.getChildrenCount();
                childNumber = "Child_" + (childCount + 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // CONFIRM BUTTON
        btnDialogConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = String.valueOf(editTextUserName.getText());

                if (TextUtils.isEmpty(userName)) {
                    Toast.makeText(Choose.this, "Please enter Username", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (currentUser != null) {
                    // save username on Database
                    userNameDatabase = FirebaseDatabase.getInstance().getReference()
                            .child("Registered Users").child(uid).child("Child").child(childNumber).child("name");
                    userNameDatabase.setValue(userName);

                    // save username and child number Locally
                    SharedPreferences.Editor editor = prefsUserName.edit();
                    editor.putString(PREF_USERNAME_KEY, userName);
                    editor.putBoolean(PREF_USERNAME_SET_KEY, true);
                    editor.putString(PREF_CHILD_NUMBER_KEY, childNumber); // Save the child number
                    editor.apply();
                }

                Intent intent = new Intent(getApplicationContext(), DetectorActivity.class);
                startActivity(intent);
                finish();

                dialog.dismiss();
            }
        });

        // CANCEL BUTTON
        btnDialogExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Choose.class);
                startActivity(intent);
                dialog.dismiss();
            }
        });
    }
}
