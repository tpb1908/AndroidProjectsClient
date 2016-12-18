package com.tpb.projects.data;

import android.content.Context;

import com.tpb.projects.data.auth.GitHubSession;

/**
 * Created by theo on 18/12/16.
 */

public abstract class APIHandler {

    public static final String GIT_BASE = "https://api.github.com/";
    static final String GIT_REPOS = "%1$s/repos/";
    public static final String ACCEPT_HEADER = "application/vnd.github.inertia-preview+json";
    static GitHubSession mSession;

    public APIHandler(Context context) {
        if(mSession == null) mSession = new GitHubSession(context);
    }


    public String appendAccessToken(String path) {
        return path + "?access_token=" + mSession.getAccessToken();
    }

}
