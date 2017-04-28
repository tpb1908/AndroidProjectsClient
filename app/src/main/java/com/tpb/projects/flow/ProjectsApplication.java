package com.tpb.projects.flow;

import android.app.Application;

import com.androidnetworking.AndroidNetworking;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.squareup.leakcanary.LeakCanary;
import com.tpb.mdtext.emoji.EmojiLoader;
import com.tpb.projects.BuildConfig;
import com.tpb.projects.util.Logger;

import okhttp3.OkHttpClient;

/**
 * Created by theo on 05/03/17.
 */

public class ProjectsApplication extends Application {

    public static FirebaseAnalytics mAnalytics;

    @Override
    public void onCreate() {
        super.onCreate();
        if(LeakCanary.isInAnalyzerProcess(this)) return; //Heap analysis process, not our stuff
        if(BuildConfig.IS_IN_DEBUG) {
            LeakCanary.install(this);
            AndroidNetworking.initialize(this, new OkHttpClient.Builder()
                    .addNetworkInterceptor(new Logger.LoggingInterceptor()).build());
        } else {
            AndroidNetworking.initialize(this);
        }
        EmojiLoader.loadEmojis(getAssets());
        mAnalytics = FirebaseAnalytics.getInstance(this);
    }

}
