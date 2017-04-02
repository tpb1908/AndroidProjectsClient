package com.tpb.projects.util;

import android.util.Log;

/**
 * Created by theo on 11/03/17.
 */

public class Logger {
    private static final boolean DEBUG = com.tpb.projects.BuildConfig.IS_IN_DEBUG;


    public static void logLong(String TAG, String s) {
        if(s.length() > 4000) {
            Log.d(TAG, s.substring(0, 4000));
            logLong(TAG, s.substring(4000));
        } else
            Log.d(TAG, s);
    }

    public static void v(String tag, String msg) {
        if(DEBUG) Log.v(tag, msg);
    }

    public static void v(String tag, String msg, Throwable tr) {
        if(DEBUG) Log.v(tag, msg, tr);
    }

    public static void d(String tag, String msg) {
        if(DEBUG) Log.d(tag, msg);
    }

    public static void d(String tag, String msg, Throwable tr) {
        if(DEBUG) Log.d(tag, msg, tr);
    }

    public static void w(String tag, String msg) {
        if(DEBUG) Log.w(tag, msg);
    }

    public static void w(String tag, String msg, Throwable tr) {
        if(DEBUG) Log.w(tag, msg, tr);
    }

    public static void w(String tag, Throwable tr) {
        if(DEBUG) Log.w(tag, tr);
    }

    public static void i(String tag, String msg) {
        if(DEBUG) Log.i(tag, msg);
    }

    public static void i(String tag, String msg, Throwable tr) {
        if(DEBUG) Log.i(tag, msg, tr);
    }

    public static void e(String tag, String msg) {
        if(DEBUG) Log.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        if(DEBUG) Log.e(tag, msg, tr);
    }

}
