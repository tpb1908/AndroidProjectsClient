package com.tpb.projects.data.auth.models;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by theo on 18/12/16.
 */

public class User extends DataModel {
    private static final String TAG = User.class.getSimpleName();

    private User() {}

    private static final String LOGIN = "login";
    private String login;

    private int id;

    private static final String AVATAR_URL = "avatar_url";
    private String avatarUrl;

    private static final String URL = "url";
    private String url;

    private static final String REPOS_URL ="repos_url";
    private String reposUrl;

    private static final String NAME = "name";
    private String name;

    private static final String LOCATION = "location";
    private String location;

    private static final String EMAIL = "email";
    private String email;

    private static final String REPOS = "public_repos";
    private int repos;

    private static final String FOLLOWERS = "followers";
    private int followers;

    private static final String BIO = "bio";
    private String bio;

    public static User parse(JSONObject obj) {
        final User u = new User();
        try {
            u.id = obj.getInt(ID);
            u.login = obj.getString(NAME);
            u.avatarUrl = obj.getString(AVATAR_URL);
            u.url = obj.getString(URL);
            u.reposUrl = obj.getString(REPOS_URL);
            u.name = obj.getString(NAME);
            u.location = obj.getString(LOCATION);
            u.repos = obj.getInt(REPOS);
            u.followers = obj.getInt(FOLLOWERS);
            u.bio = obj.getString(BIO);
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }

        return u;
    }

}
