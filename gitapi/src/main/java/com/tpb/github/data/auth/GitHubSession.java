package com.tpb.github.data.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.tpb.github.data.models.User;

import org.json.JSONException;
import org.json.JSONObject;

public class GitHubSession {
    private static final String TAG = GitHubSession.class.getSimpleName();

    private static GitHubSession session;
    private final SharedPreferences prefs;

    private static final String SHARED = "GitHub_Preferences";
    private static final String API_LOGIN = "username";
    private static final String API_ID = "id";
    private static final String API_ACCESS_TOKEN = "access_token";
    private static final String INFO_USER = "user_json";

    private GitHubSession(Context context) {
        prefs = context.getSharedPreferences(SHARED, Context.MODE_PRIVATE);
    }

    public static GitHubSession getSession(Context context) {
        if(session == null) session = new GitHubSession(context);
        return session;
    }

    void storeUser(JSONObject json) {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(INFO_USER, json.toString());
        final User user = new User(json);
        editor.putInt(API_ID, user.getId());
        editor.putString(API_LOGIN, user.getLogin());
        editor.apply();
    }

    void storeAccessToken(@NonNull String accessToken) {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(API_ACCESS_TOKEN, accessToken);
        editor.apply();
    }

    public User getUser() {
        try {
            final JSONObject obj = new JSONObject(prefs.getString(INFO_USER, ""));
            return new User(obj);
        } catch(JSONException jse) {
            return null;
        }
    }

    public String getUserLogin() {
        return prefs.getString(API_LOGIN, null);
    }

    public String getAccessToken() {
        return prefs.getString(API_ACCESS_TOKEN, null);
    }

    public boolean hasAccessToken() {
        return getAccessToken() != null;
    }

}