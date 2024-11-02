package com.pytorch.project.gazeguard.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SharedPrefsUtil {

    private static final String PREF_USERNAME_KEY = "username";
    private static final String PREF_USERNAME_SET_KEY = "username_set";
    private static final String PREF_CHILD_NUMBER_KEY = "child_number";

    // Get SharedPreferences
    private static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    // Get username
    public static String getUserName(Context context) {
        SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getString(PREF_USERNAME_KEY, "");
    }

    // Set username
    public static void setUserName(Context context, String userName) {
        SharedPreferences prefs = getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_USERNAME_KEY, userName);
        editor.putBoolean(PREF_USERNAME_SET_KEY, true);
        editor.apply();
    }

    // Get child number
    public static String getChildNumber(Context context) {
        SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getString(PREF_CHILD_NUMBER_KEY, "");
    }

    // Set child number
    public static void setChildNumber(Context context, String childNumber) {
        SharedPreferences prefs = getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_CHILD_NUMBER_KEY, childNumber);
        editor.apply();
    }

    // Check if username is set
    public static boolean isUserNameSet(Context context) {
        SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getBoolean(PREF_USERNAME_SET_KEY, false);
    }
}
