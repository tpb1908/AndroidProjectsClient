package com.tpb.projects.flow;

import android.app.Application;

import com.androidnetworking.AndroidNetworking;
import com.squareup.leakcanary.LeakCanary;
import com.tpb.projects.BuildConfig;
import com.tpb.projects.util.Logger;

import okhttp3.OkHttpClient;

/**
 * Created by theo on 05/03/17.
 */

public class ProjectsApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if(LeakCanary.isInAnalyzerProcess(this)) return;
        if(BuildConfig.IS_IN_DEBUG) {
            LeakCanary.install(this);
            Logger.i(ProjectsApplication.class.getSimpleName(), "onCreate: Installed canary");
            AndroidNetworking.initialize(this, new OkHttpClient.Builder()
                    .addNetworkInterceptor(new Logger.LoggingInterceptor()).build());
        } else {
            AndroidNetworking.initialize(this);
        }
    }
}
