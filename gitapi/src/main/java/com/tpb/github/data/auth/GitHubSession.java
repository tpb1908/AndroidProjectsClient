package com.tpb.github.data.auth;

import android.content.Context;
import android.content.SharedPreferences;

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

    void storeCredentials(String accessToken, int id, String login) {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(API_ID, id);
        editor.putString(API_ACCESS_TOKEN, accessToken);
        editor.putString(API_LOGIN, login);
        editor.apply();
    }

    void storeUser(JSONObject user) {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(INFO_USER, user.toString());
        editor.apply();
    }

    public void updateUserLogin(String login) {
        prefs.edit().putString(API_LOGIN, login).apply();
    }

    void storeAccessToken(String accessToken) {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(API_ACCESS_TOKEN, accessToken);
        editor.apply();
    }

    void resetAccessToken() {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(API_ID, null);
        editor.putString(API_ACCESS_TOKEN, null);
        editor.putString(API_LOGIN, null);
        editor.apply();
    }

    public User getUser() {
        try {
            final JSONObject obj = new JSONObject(prefs.getString(INFO_USER, null));
            return User.parse(obj);
        } catch(JSONException jse) {
            return null;
        }
    }

    public String getUserLogin() {
        return prefs.getString(API_LOGIN, null);
    }

    public int getUserId() {
        return prefs.getInt(API_ID, -1);
    }

    /**
     * Get the OAuth access token
     *
     * @return Access token
     */
    public String getAccessToken() {
        return prefs.getString(API_ACCESS_TOKEN, null);
    }

}