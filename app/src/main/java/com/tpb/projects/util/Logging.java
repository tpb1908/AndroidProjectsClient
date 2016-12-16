package com.tpb.projects.util;

import android.util.Log;

/**
 * Created by theo on 16/12/16.
 */

public class Logging {


    public static void largeDebugDump(String tag, String dump) {
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
