package com.tuhoang.pocketmind.utils;

import android.util.Log;

public class AppLogger {
    private static final String DEFAULT_TAG = "PocketMindLog";

    public static void d(String tag, String message) {
        Log.d(tag, message);
    }

    public static void d(String message) {
        Log.d(DEFAULT_TAG, message);
    }

    public static void e(String tag, String message, Throwable t) {
        // Funnel all error logs here to avoid raw e.printStackTrace() and potential data leaks
        Log.e(tag, message, t);
    }

    public static void e(String tag, String message) {
        Log.e(tag, message);
    }
}
