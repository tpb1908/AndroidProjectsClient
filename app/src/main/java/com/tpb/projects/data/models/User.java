package com.tpb.projects.data.models;

import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by theo on 18/12/16.
 */

public class User extends DataModel {
    private static final String TAG = User.class.getSimpleName();

    private User() {
    }

    private static final String LOGIN = "login";
    private String login;

    private int id;

    private static final String AVATAR_URL = "avatar_url";
    private String avatarUrl;

    private static final String URL = "url";
    private String url;

    private static final String REPOS_URL = "repos_url";
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

    public int getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getUrl() {
        return url;
    }

    public String getReposUrl() {
        return reposUrl;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getLocation() {
        return location;
    }

    public String getEmail() {
        return email;
    }

    public int getRepos() {
        return repos;
    }

    public int getFollowers() {
        return followers;
    }

    public String getBio() {
        return bio;
    }

    public static User parse(JSONObject obj) {
        final User u = new User();
        try {
            u.id = obj.getInt(ID);
            u.login = obj.getString(LOGIN);
            u.avatarUrl = obj.getString(AVATAR_URL);
            u.url = obj.getString(URL);
            u.reposUrl = obj.getString(REPOS_URL);
            if(obj.has(REPOS)) u.repos = obj.getInt(REPOS);
            if(obj.has(FOLLOWERS)) u.followers = obj.getInt(FOLLOWERS);
            if(obj.has(BIO)) u.bio = obj.getString(BIO);
            if(obj.has(EMAIL)) u.email = obj.getString(EMAIL);
            if(obj.has(LOCATION)) u.location = obj.getString(LOCATION);
            if(obj.has(NAME)) u.name = obj.getString(NAME);
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }

        return u;
    }

    @Override
    public String toString() {
        return "User{" +
                "login='" + login + '\'' +
                ", id=" + id +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", url='" + url + '\'' +
                ", reposUrl='" + reposUrl + '\'' +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", email='" + email + '\'' +
                ", repos=" + repos +
                ", followers=" + followers +
                ", bio='" + bio + '\'' +
                '}';
    }
}
