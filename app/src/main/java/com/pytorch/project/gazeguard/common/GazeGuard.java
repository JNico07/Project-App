package com.pytorch.project.gazeguard.common;

import android.app.Application;
import android.util.Log;
import com.google.firebase.database.FirebaseDatabase;

public class GazeGuard extends Application {
    private static final String TAG = "GazeGuardApp";

    @Override
    public void onCreate() {
        super.onCreate();
//        Log.d(TAG, "Application onCreate started.");
        // Enable Firebase Realtime Database persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
//        Log.d(TAG, "Firebase persistence enabled.");
    }
}

