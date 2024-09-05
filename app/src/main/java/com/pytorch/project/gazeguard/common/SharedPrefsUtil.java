package com.pytorch.project.gazeguard.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SharedPrefsUtil {

    private static final String PREF_CHILD_NUMBER_KEY = "child_number";

    // Method to retrieve the childNumber from SharedPreferences
    public static String getChildNumber(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_CHILD_NUMBER_KEY, "");
    }

}
