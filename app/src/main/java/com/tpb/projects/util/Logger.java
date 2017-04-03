package com.tpb.projects.util;

import android.annotation.SuppressLint;
import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

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

    public static class LoggingInterceptor implements Interceptor {

        private static final String TAG = LoggingInterceptor.class.getSimpleName();

        @SuppressLint("DefaultLocale")
        @Override
        public Response intercept(Chain chain) throws IOException {
            final Request request = chain.request();
            final long ts = System.nanoTime();
            Logger.i(TAG, String.format("Sending request %s on %s%n%s",
                    request.url(), chain.connection(), request.headers()));

            final Response response = chain.proceed(request);

            Logger.i(TAG, String.format("Received response for %s in %.1fms%n%s",
                    response.request().url(), (System.nanoTime() - ts) / 1e6d, response.headers()));

            return response;
        }
    }

}
