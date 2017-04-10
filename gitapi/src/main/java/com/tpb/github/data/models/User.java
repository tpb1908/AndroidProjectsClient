package com.tpb.github.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;

import com.tpb.github.data.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Created by theo on 18/12/16.
 */

public class User extends DataModel implements Parcelable {
    private static final String TAG = User.class.getSimpleName();
    
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

    private static final String FOLLOWING = "following";
    private int following;

    private static final String BIO = "bio";
    private String bio;

    private static final String COMPANY = "company";
    private String company;

    private static final String BLOG = "blog";
    private String blog;

    private static final String GISTS = "public_gists";
    private int gists;

    private static final String CONTRIBUTIONS = "contributions";
    private int contributions;

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

    public String getCompany() {
        return company;
    }

    public String getBlog() {
        return blog;
    }

    public int getGists() {
        return gists;
    }

    public int getFollowing() {
        return following;
    }

    public int getContributions() {
        return contributions;
    }

    @Override
    public long getCreatedAt() {
        return createdAt;
    }
    
    public User(JSONObject obj) {
        try {
            id = obj.getInt(ID);
            login = obj.getString(LOGIN);
            avatarUrl = obj.getString(AVATAR_URL);
            url = obj.getString(URL);

            if(obj.has(CREATED_AT)) {
                try {
                    createdAt = Util.toCalendar(obj.getString(CREATED_AT)).getTimeInMillis();
                } catch(ParseException pe) {
                    Log.e(TAG, "parse: ", pe);
                }
            }
            if(obj.has(HTML_URL)) {
                htmlUrl = obj.getString(HTML_URL);
            } else {
                htmlUrl = "https://github.com/" + getLogin();
            }
            if(obj.has(REPOS_URL) && !JSON_NULL.equals(obj.getString(REPOS_URL)))
                reposUrl = obj.getString(REPOS_URL);
            if(obj.has(REPOS)) repos = obj.getInt(REPOS);
            if(obj.has(FOLLOWERS)) followers = obj.getInt(FOLLOWERS);
            if(obj.has(BIO) && !JSON_NULL.equals(obj.getString(BIO))) bio = obj.getString(BIO);
            if(obj.has(EMAIL) && !JSON_NULL.equals(obj.getString(EMAIL)))
                email = obj.getString(EMAIL);
            if(obj.has(LOCATION) && !JSON_NULL.equals(obj.getString(LOCATION)))
                location = obj.getString(LOCATION);
            if(obj.has(NAME) && !JSON_NULL.equals(obj.getString(NAME)))
                name = obj.getString(NAME);
            if(obj.has(BLOG) && !JSON_NULL.equals(obj.getString(BLOG)))
                blog = obj.getString(BLOG);
            if(obj.has(COMPANY) && !JSON_NULL.equals(obj.getString(COMPANY)))
                company = obj.getString(COMPANY);
            if(obj.has(GISTS)) gists = obj.getInt(GISTS);
            if(obj.has(FOLLOWING)) following = obj.getInt(FOLLOWING);
            if(obj.has(CONTRIBUTIONS)) contributions = obj.getInt(CONTRIBUTIONS);
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }   
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
                ", following=" + following +
                ", bio='" + bio + '\'' +
                ", company='" + company + '\'' +
                ", blog='" + blog + '\'' +
                ", gists=" + gists +
                ", contributions=" + contributions +
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
        dest.writeInt(this.following);
        dest.writeString(this.bio);
        dest.writeString(this.company);
        dest.writeString(this.blog);
        dest.writeInt(this.gists);
        dest.writeInt(this.contributions);
        dest.writeLong(this.createdAt);
    }

    protected User(Parcel in) {
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
        this.following = in.readInt();
        this.bio = in.readString();
        this.company = in.readString();
        this.blog = in.readString();
        this.gists = in.readInt();
        this.contributions = in.readInt();
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
