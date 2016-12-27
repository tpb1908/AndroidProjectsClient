package com.tpb.projects.data.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Manage access token and user name. Uses shared preferences to store access
 * token and user name.
 *
 * @author Thiago Locatelli <thiago.locatelli@gmail.com>
 * @author Lorensius W. L T <lorenz@londatiga.net>
 */
public class GitHubSession {
    private static final String TAG = GitHubSession.class.getSimpleName();

    private static GitHubSession session;
    private SharedPreferences prefs;

    private static final String SHARED = "GitHub_Preferences";
    private static final String API_USERNAME = "username";
    private static final String API_ID = "id";
    private static final String API_ACCESS_TOKEN = "access_token";

    private GitHubSession(Context context) {
        prefs = context.getSharedPreferences(SHARED, Context.MODE_PRIVATE);
    }

    public static GitHubSession getSession(Context context) {
        if(session == null) session = new GitHubSession(context);
        return session;
    }

    void storeAccessToken(String accessToken, int id, String username) {
        Log.i(TAG, "Storing token " + accessToken);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(API_ID, id);
        editor.putString(API_ACCESS_TOKEN, accessToken);
        editor.putString(API_USERNAME, username);
        editor.apply();
    }

    void storeAccessToken(String accessToken) {
        Log.i(TAG, "Storing token " + accessToken);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(API_ACCESS_TOKEN, accessToken);
        editor.apply();
    }

    /**
     * Reset access token and user name
     */
    void resetAccessToken() {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(API_ID, null);
        editor.putString(API_ACCESS_TOKEN, null);
        editor.putString(API_USERNAME, null);
        editor.apply();
    }

    /**
     * Get user name
     *
     * @return User name
     */
    public String getUsername() {
        return prefs.getString(API_USERNAME, null);
    }

    public int getUserId() {
        return prefs.getInt(API_ID, -1);
    }

    /**
     * Get access token
     *
     * @return Access token
     */
    public String getAccessToken() {
        Log.i(TAG, "getAccessToken: " + prefs.contains(API_ACCESS_TOKEN));
        return prefs.getString(API_ACCESS_TOKEN, null);
    }

}