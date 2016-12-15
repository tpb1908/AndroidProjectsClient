package com.tpb.projects.data;

import android.content.Context;
import android.util.Log;


/**
 * Created by theo on 14/12/16.
 */

public class Loader {
    private static final String TAG = Loader.class.getSimpleName();

    private static final String GIT_BASE = "https://api.github.com";

    //https://developer.github.com/v3/repos/
    //https://developer.github.com/v3/projects/
    //https://developer.github.com/v3/projects/columns/#get-a-project-column
    //https://developer.github.com/v3/projects/cards/

    public static void tryLogin(final Context context, String username, String password) {





    }

    public static void largeDebugDump(String tag, String dump) {
        Log.i(TAG, "largeDebugDump: " + dump.length());
        final int len = dump.length();
        for(int i = 0; i < len; i += 1024) {
            if(i + 1024 < len) {
                Log.d(tag, dump.substring(i, i + 1024));
            } else {
                Log.d(tag, dump.substring(i, len));
            }
        }
    }

}
