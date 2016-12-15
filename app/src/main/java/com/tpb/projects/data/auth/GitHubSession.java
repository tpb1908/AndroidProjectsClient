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
 *
 */
public class GitHubSession {
    private static final String TAG = GitHubSession.class.getSimpleName();

    private SharedPreferences prefs;

    private static final String SHARED = "GitHub_Preferences";
    private static final String API_USERNAME = "username";
    private static final String API_ID = "id";
    private static final String API_ACCESS_TOKEN = "access_token";

    public GitHubSession(Context context) {
        prefs = context.getSharedPreferences(SHARED, Context.MODE_PRIVATE);
    }

    /**
     *
     * @param accessToken
     * @param username
     */
    public void storeAccessToken(String accessToken, String id, String username) {
        Log.i(TAG, "Storing token " + accessToken);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(API_ID, id);
        editor.putString(API_ACCESS_TOKEN, accessToken);
        editor.putString(API_USERNAME, username);
        editor.apply();
    }

    public void storeAccessToken(String accessToken) {
        Log.i(TAG, "Storing token " + accessToken);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(API_ACCESS_TOKEN, accessToken);
        editor.apply();
    }

    /**
     * Reset access token and user name
     */
    public void resetAccessToken() {
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