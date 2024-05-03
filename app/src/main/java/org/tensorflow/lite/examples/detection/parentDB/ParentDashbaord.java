package org.tensorflow.lite.examples.detection.parentDB;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.tensorflow.lite.examples.detection.Choose;
import org.tensorflow.lite.examples.detection.Login;
import org.tensorflow.lite.examples.detection.R;
import org.tensorflow.lite.examples.detection.Welcome;

public class ParentDashbaord extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private TextView timeMeasured;

    private FirebaseAuth auth;
//    private TextView textView;
    private Button buttton;
    private FirebaseUser currentUser;
    private DatabaseReference screenTimeRef;
    private RecyclerView recyclerView;
    private String uid;
    ParentAdapter parentAdapter;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashbaord);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav,
                R.string.close_nav);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
            navigationView.setCheckedItem(R.id.nav_home);
        }

        auth = FirebaseAuth.getInstance();
//        textView = findViewById(R.id.user_details);
//        button = findViewById(R.id.logout);
//        currentUser = auth.getCurrentUser();
//        timeMeasured = findViewById(R.id.timerTextPDash);
//        recyclerView = (RecyclerView) findViewById(R.id.rv);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));

//        if (currentUser == null) {
//            Intent intent = new Intent(getApplicationContext(), Login.class);
//            startActivity(intent);
//            finish();
//        } else {
//            textView.setText(currentUser.getEmail());
//        }

//        uid = currentUser.getUid();
//        FirebaseRecyclerOptions<ParentModel> options =
//                new FirebaseRecyclerOptions.Builder<ParentModel>()
//                        .setQuery(FirebaseDatabase.getInstance().getReference("Registered Users").child(uid).child("Child")
//                                , ParentModel.class)
//                        .build();

//        parentAdapter = new ParentAdapter(options);
//        recyclerView.setAdapter(parentAdapter);



//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                FirebaseAuth.getInstance().signOut();
//                Intent intent = new Intent(getApplicationContext(), Welcome.class);
//                startActivity(intent);
//                finish();
//            }
//        });

//        // Initialize database reference to the ScreenTime node
//        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        screenTimeRef = FirebaseDatabase.getInstance().getReference()
//                .child("Registered Users").child(uid).child("Child").child("Child_1").child("ScreenTime");
//
//        // Add ValueEventListener to retrieve the ScreenTime value
//        screenTimeRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                if (dataSnapshot.exists()) {
//                    String screenTime = dataSnapshot.getValue(String.class);
//                    // Update the timeMeasured TextView with the retrieved value
//                    timeMeasured.setText(screenTime);
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                // Handle errors or onCancelled event as needed
//            }
//        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_home:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
                break;

            case R.id.nav_settings:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SettingsFragment()).commit();
                break;

            case R.id.nav_share:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ShareFragment()).commit();
                break;

            case R.id.nav_about:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new AboutFragment()).commit();
                break;

            case R.id.nav_back:
                intent = new Intent(getApplicationContext(), Choose.class);
                startActivity(intent);
                break;

            case R.id.nav_logout:
                Toast.makeText(this, "Logout!", Toast.LENGTH_SHORT).show();
                FirebaseAuth.getInstance().signOut();
                intent = new Intent(getApplicationContext(), Welcome.class);
                startActivity(intent);
                break;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        parentAdapter.startListening();
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        parentAdapter.stopListening();
//    }
}
