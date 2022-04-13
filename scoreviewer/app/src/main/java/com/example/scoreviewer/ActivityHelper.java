package com.example.scoreviewer;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.preference.PreferenceManager;

public class ActivityHelper extends Application {
    // Log
    @SuppressWarnings("unused")
    private final static String TAG = ActivityHelper.class.getSimpleName();

    // Data
    private static boolean mIsActivityVisible;

    // region Lifecycle
    @Override
    public void onCreate() {
        super.onCreate();

        // Setup handler for uncaught exceptions.
//        Thread.setDefaultUncaughtExceptionHandler(this::handleUncaughtException);
    }

    // endregion

/*
    private void handleUncaughtException(Thread thread, Throwable e) {
        Log.e(TAG, "Error: handleUncaughtException");
        e.printStackTrace();
        BleScanner.getInstance().disconnectFromAll();

        System.exit(1);
    }*/


    // region Detect app in background: https://stackoverflow.com/questions/3667022/checking-if-an-android-application-is-running-in-the-background

    public static boolean isActivityVisible() {
        return mIsActivityVisible;
    }

    public static void activityResumed() {
        mIsActivityVisible = true;
    }

    public static void activityPaused() {
        mIsActivityVisible = false;
    }

    // endregion
}
