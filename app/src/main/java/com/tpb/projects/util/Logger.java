package com.tpb.projects.util;

import android.util.Log;

/**
 * Created by theo on 11/03/17.
 */

public class Logger {

    public static void logLong(String TAG, String s) {
        if (s.length() > 4000) {
            Log.d(TAG, s.substring(0, 4000));
            logLong(TAG, s.substring(4000));
        } else
            Log.d(TAG, s);
    }

}
