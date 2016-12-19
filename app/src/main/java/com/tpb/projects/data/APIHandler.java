package com.tpb.projects.data;

import android.content.Context;

import com.tpb.projects.data.auth.GitHubSession;

import java.util.HashMap;

/**
 * Created by theo on 18/12/16.
 */

public abstract class APIHandler {
    public static final String TAG = APIHandler.class.getSimpleName();

    protected static final String GIT_BASE = "https://api.github.com/";
    private static final String ACCEPT_HEADER_KEY = "Accept";
    private static final String ACCEPT_HEADER = "application/vnd.github.v3+json";
    private static final String PREVIEW_ACCEPT_HEADER = "application/vnd.github.inertia-preview+json";
    private static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    private static final String AUTHORIZATION_TOKEN_FORMAT = "token %1$s";
    private static GitHubSession mSession;

    protected static HashMap<String, String> API_AUTH_HEADERS = new HashMap<>();
    static HashMap<String, String> PREVIEW_API_AUTH_HEADERS = new HashMap<>();


    public APIHandler(Context context) {
        if(mSession == null) {
            mSession = GitHubSession.getSession(context);
            initHeaders();
        }
    }

    public void initHeaders() {
        API_AUTH_HEADERS.put(ACCEPT_HEADER_KEY, ACCEPT_HEADER);
        API_AUTH_HEADERS.put(AUTHORIZATION_HEADER_KEY, String.format(AUTHORIZATION_TOKEN_FORMAT, mSession.getAccessToken()));
        PREVIEW_API_AUTH_HEADERS.put(ACCEPT_HEADER_KEY, PREVIEW_ACCEPT_HEADER);
        PREVIEW_API_AUTH_HEADERS.put(AUTHORIZATION_HEADER_KEY, String.format(AUTHORIZATION_TOKEN_FORMAT, mSession.getAccessToken()));
    }

}
