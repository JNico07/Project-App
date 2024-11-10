package com.pytorch.project.gazeguard.parentdashboard;

import static com.pytorch.project.gazeguard.common.RecommendationsManager.showRecommendationsDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.pytorch.project.gazeguard.common.ChooseActivity;
import com.pytorch.project.gazeguard.common.EULA;
import com.pytorch.project.gazeguard.common.RecommendationsAdapter;
import com.pytorch.project.gazeguard.common.RecommendationsManager;
import com.pytorch.project.gazeguard.common.WelcomeActivity;
import com.pytorch.project.gazeguard.parentdashboard.childdatafragment.ChildDataFragment;
import com.pytorch.project.gazeguard.parentdashboard.optionfragments.AboutFragment;
import com.pytorch.project.gazeguard.parentdashboard.optionfragments.HomeFragment;
import com.pytorch.project.gazeguard.parentdashboard.optionfragments.SetLimitFragment;
import com.pytorch.project.gazeguard.parentdashboard.optionfragments.SettingsFragment;

import org.pytorch.demo.objectdetection.R;

import java.util.Arrays;
import java.util.List;

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
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
                // Force toolbar menu update
                invalidateOptionsMenu();
                break;

            case R.id.nav_set_limit:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SetLimitFragment())
                        .commit();
                // Force toolbar menu update
                invalidateOptionsMenu();
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

            case R.id.eula:
                EULA.showEULA(this, new EULA.EULAListener() {
                    @Override
                    public void onEULAAccepted() {
                        // Continue
                    }
                    @Override
                    public void onEULADeclined() {
                        // Exit the app
                        finishAffinity();
                    }
                });
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        // Get current fragment
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        // Hide recommendations icon by default
        MenuItem recommendationsItem = menu.findItem(R.id.action_recommendations);
        if (recommendationsItem != null) {
            recommendationsItem.setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_recommendations) {
            showRecommendationsDialog(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}