package com.example.orderstobeserved;

import android.content.SharedPreferences;

public class TestingModeManager {

    private static final String PREFS_KEY = "testing_mode_enabled";
    public static final String PREFIX = "zTesting_";

    public static boolean isEnabled(SharedPreferences prefs) {
        return prefs.getBoolean(PREFS_KEY, false);
    }

    public static void setEnabled(SharedPreferences prefs, boolean enabled) {
        prefs.edit().putBoolean(PREFS_KEY, enabled).apply();
    }

    /**
     * Returns the root collection name with the zTesting_ prefix when testing
     * mode is active. Embedded sub-collections are NOT affected — only the root
     * collection name passed here should be the top-level segment.
     */
    public static String col(SharedPreferences prefs, String name) {
        return isEnabled(prefs) ? PREFIX + name : name;
    }
}
