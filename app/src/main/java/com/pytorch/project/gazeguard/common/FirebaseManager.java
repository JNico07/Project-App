package com.pytorch.project.gazeguard.common;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseManager {
    private static FirebaseAuth mAuth;
    private static FirebaseUser currentUser;
    private static FirebaseFirestore firestore;

    // Private constructor to prevent instantiation
    private FirebaseManager() {}

    public static FirebaseAuth getAuth() {
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance();
        }
        return mAuth;
    }

    public static FirebaseUser getCurrentUser() {
        if (currentUser == null) {
            currentUser = getAuth().getCurrentUser();
        }
        return currentUser;
    }

    public static FirebaseFirestore getFirestore() {
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        return firestore;
    }
}

