package com.tpb.projects.util;

import android.app.Application;
import android.util.Log;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by theo on 05/03/17.
 */

public class ProjectsApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if(LeakCanary.isInAnalyzerProcess(this)) return;
        LeakCanary.install(this);
        Log.i(ProjectsApplication.class.getSimpleName(), "onCreate: Installed canary");
    }
}
