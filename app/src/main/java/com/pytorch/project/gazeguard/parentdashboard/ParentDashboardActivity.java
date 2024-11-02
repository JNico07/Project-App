package com.pytorch.project.gazeguard.parentdashboard;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.pytorch.project.gazeguard.common.ChooseActivity;
import com.pytorch.project.gazeguard.common.WelcomeActivity;
import com.pytorch.project.gazeguard.parentdashboard.optionfragments.AboutFragment;
import com.pytorch.project.gazeguard.parentdashboard.optionfragments.HomeFragment;
import com.pytorch.project.gazeguard.parentdashboard.optionfragments.SetLimitFragment;
import com.pytorch.project.gazeguard.parentdashboard.optionfragments.SettingsFragment;

import org.pytorch.demo.objectdetection.R;

public class ParentDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
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
        setContentView(R.layout.activity_parent_dashboard);

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
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_home:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
                break;

            case R.id.nav_set_limit:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SetLimitFragment()).commit();
                break;

            case R.id.nav_settings:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SettingsFragment()).commit();
                break;

            case R.id.nav_about:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new AboutFragment()).commit();
                break;

            case R.id.nav_back:
                intent = new Intent(getApplicationContext(), ChooseActivity.class);
                startActivity(intent);
                break;

            case R.id.nav_logout:
                Toast.makeText(this, "Logout!", Toast.LENGTH_SHORT).show();
                FirebaseAuth.getInstance().signOut();
                intent = new Intent(getApplicationContext(), WelcomeActivity.class);
                startActivity(intent);
                break;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
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

    private long lastBackPressedTime = 0;
    private static final int DOUBLE_PRESS_INTERVAL = 2000; // 2 seconds
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (fragment instanceof HomeFragment) {
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastBackPressedTime <= DOUBLE_PRESS_INTERVAL) {
                // Double-click detected, navigate to ChooseActivity
                Intent intent = new Intent(getApplicationContext(), ChooseActivity.class);
                startActivity(intent);
                finish();
            } else {
                // First press, update last pressed time
                lastBackPressedTime = currentTime;
                Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Replace the current fragment with HomeFragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

//    @Override
//    public boolean onSupportNavigateUp() {
//        onBackPressed();
//        return true;
//    }

}