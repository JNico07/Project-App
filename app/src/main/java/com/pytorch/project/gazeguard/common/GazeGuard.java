package com.pytorch.project.gazeguard.common;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

public class GazeGuard extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase App
        FirebaseApp.initializeApp(this);
        
        // Initialize Firebase App Check
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());

        // Enable Firebase Realtime Database persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}

