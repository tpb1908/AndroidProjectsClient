package com.tpb.projects.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;

import com.tpb.projects.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Created by theo on 18/12/16.
 */

public class User extends DataModel implements Parcelable {
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


    private static final String HTML_URL = "html_url";
    private String htmlUrl;

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

    public String getHtmlUrl() {
        return htmlUrl;
    }

    @Override
    public long getCreatedAt() {
        return 0;
    }

    public static User parse(JSONObject obj) {
        final User u = new User();
        try {
            u.id = obj.getInt(ID);
            u.login = obj.getString(LOGIN);
            u.avatarUrl = obj.getString(AVATAR_URL);
            u.url = obj.getString(URL);

            if(obj.has(CREATED_AT)) {
                try {
                    u.createdAt = Util.toCalendar(obj.getString(CREATED_AT)).getTimeInMillis();
                } catch(ParseException pe) {
                    Log.e(TAG, "parse: ", pe);
                }
            }
            if(obj.has(HTML_URL)) {
                u.htmlUrl = obj.getString(HTML_URL);
            } else {
                u.htmlUrl = "https://github.com/" + u.getLogin();
            }
            if(obj.has(REPOS_URL)) u.reposUrl = obj.getString(REPOS_URL);
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

    public static JSONObject parse(User user) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(ID, user.id);
            obj.put(LOGIN, user.login);
            obj.put(AVATAR_URL, user.avatarUrl);
            obj.put(URL, user.url);
            obj.put(REPOS_URL, user.reposUrl);
            obj.put(REPOS, user.repos);
            obj.put(FOLLOWERS, user.followers);
            if(user.bio != null) obj.put(BIO, user.bio);
            if(user.email != null) obj.put(EMAIL, user.email);
            if(user.location != null) obj.put(LOCATION, user.location);
            if(user.name != null) obj.put(NAME, user.name);
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return obj;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof User && ((User) obj).id == id;
    }

    @Override
    public String toString() {
        return "User{" +
                "login='" + login + '\'' +
                ", id=" + id +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", url='" + url + '\'' +
                ", htmlUrl='" + htmlUrl + '\'' +
                ", reposUrl='" + reposUrl + '\'' +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", email='" + email + '\'' +
                ", repos=" + repos +
                ", followers=" + followers +
                ", bio='" + bio + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.login);
        dest.writeInt(this.id);
        dest.writeString(this.avatarUrl);
        dest.writeString(this.url);
        dest.writeString(this.htmlUrl);
        dest.writeString(this.reposUrl);
        dest.writeString(this.name);
        dest.writeString(this.location);
        dest.writeString(this.email);
        dest.writeInt(this.repos);
        dest.writeInt(this.followers);
        dest.writeString(this.bio);
        dest.writeLong(this.createdAt);
    }

    User(Parcel in) {
        this.login = in.readString();
        this.id = in.readInt();
        this.avatarUrl = in.readString();
        this.url = in.readString();
        this.htmlUrl = in.readString();
        this.reposUrl = in.readString();
        this.name = in.readString();
        this.location = in.readString();
        this.email = in.readString();
        this.repos = in.readInt();
        this.followers = in.readInt();
        this.bio = in.readString();
        this.createdAt = in.readLong();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel source) {
            return new User(source);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };
}
