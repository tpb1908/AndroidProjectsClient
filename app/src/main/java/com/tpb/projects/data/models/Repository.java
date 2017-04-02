package com.tpb.projects.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.tpb.projects.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Created by theo on 15/12/16.
 */

public class Repository extends DataModel implements Parcelable {
    private static final String TAG = Repository.class.getSimpleName();

    private Repository() {
    }

    private int id;

    private String name;

    private long updatedAt;

    private static final String OWNER = "owner";
    private static final String USER_LOGIN = "login";
    private static final String USER_AVATAR = "avatar_url";
    private String userLogin;
    private String userAvatarUrl;
    private int userId;

    private static final String FULL_NAME = "full_name";
    private String fullName;

    private static final String DESCRIPTION = "description";
    private String description;

    private static final String PRIVATE = "private";
    private boolean isPrivate;

    private static final String FORK = "fork";
    private boolean isFork;

    private String url;

    private static final String HTML_URL = "html_url";
    private String htmlUrl;

    private static final String LANGUAGE = "language";
    private String language;

    private static final String HAS_ISSUES = "has_issues";
    private boolean hasIssues;

    private static final String STAR_GAZERS = "stargazers_count";
    private int starGazers;

    private static final String FORKS = "forks_count";
    private int forks;

    private static final String WATCHERS = "watchers_count";
    private int watchers;

    private static final String ISSUES = "open_issues_count";
    private int issues;

    private static final String SIZE = "size";
    private int size;

    private static final String PARENT = "parent";
    private Repository parent;

    private static final String SOURCE = "source";
    private Repository source;

    private static final String LICENSE = "license";
    private static final String KEY = "key";
    private String licenseKey;
    private static final String LICENSE_NAME = "name";
    private String licenseName;
    private static final String SPDX_ID = "spdx_id";
    private String licenseShortName;
    private static final String LICENSE_URL = "url";
    private String licenseUrl;


    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public boolean isFork() {
        return isFork;
    }

    public String getUrl() {
        return url;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public String getLanguage() {
        return language;
    }

    public boolean hasIssues() {
        return hasIssues;
    }

    public int getStarGazers() {
        return starGazers;
    }

    public int getForks() {
        return forks;
    }

    public int getWatchers() {
        return watchers;
    }

    public int getIssues() {
        return issues;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public int getUserId() {
        return userId;
    }

    public int getSize() {
        return size;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public Repository getParent() {
        return parent;
    }

    public Repository getSource() {
        return source;
    }

    public static String getTAG() {
        return TAG;
    }

    public boolean isHasIssues() {
        return hasIssues;
    }

    public boolean hasLicense() {
        return licenseName != null;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public String getLicenseShortName() {
        return licenseShortName;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    @Override
    public long getCreatedAt() {
        return createdAt;
    }

    public static Repository parse(JSONObject obj) {
        final Repository r = new Repository();

        try {
            r.id = obj.getInt(ID);
            r.userLogin = obj.getJSONObject(OWNER).getString(USER_LOGIN);
            r.userId = obj.getJSONObject(OWNER).getInt(ID);
            r.userAvatarUrl = obj.getJSONObject(OWNER).getString(USER_AVATAR);
            r.name = obj.getString(NAME);
            r.fullName = obj.getString(FULL_NAME);
            r.description = obj.getString(DESCRIPTION);
            r.isPrivate = obj.getBoolean(PRIVATE);
            r.isFork = obj.getBoolean(FORK);
            r.url = obj.getString(URL);
            r.htmlUrl = obj.getString(HTML_URL);
            r.language = obj.getString(LANGUAGE);
            r.hasIssues = obj.getBoolean(HAS_ISSUES);
            r.starGazers = obj.getInt(STAR_GAZERS);
            r.forks = obj.getInt(FORKS);
            r.watchers = obj.getInt(WATCHERS);
            r.issues = obj.getInt(ISSUES);
            r.size = obj.getInt(SIZE);
            if(obj.has(PARENT)) r.parent = Repository.parse(obj.getJSONObject(PARENT));
            if(obj.has(SOURCE)) r.source = Repository.parse(obj.getJSONObject(SOURCE));
            if(obj.has(LICENSE) && !JSON_NULL.equals(obj.getString(LICENSE))) {
                final JSONObject license = obj.getJSONObject(LICENSE);
                r.licenseKey = license.getString(KEY);
                r.licenseName = license.getString(LICENSE_NAME);
                r.licenseShortName = license.getString(SPDX_ID);
                r.licenseUrl = license.getString(LICENSE_URL);
            }
            try {
                r.createdAt = Util.toCalendar(obj.getString(CREATED_AT)).getTimeInMillis();
                r.updatedAt = Util.toCalendar(obj.getString(UPDATED_AT)).getTimeInMillis();
            } catch(ParseException pe) {
                Log.e(TAG, "parse: ", pe);
            }

        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }

        return r;
    }

    public static JSONObject parse(Repository repo) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(ID, repo.id);
            final JSONObject owner = new JSONObject();
            owner.put(USER_LOGIN, repo.userLogin);
            owner.put(ID, repo.userId);
            owner.put(USER_AVATAR, repo.userAvatarUrl);
            obj.put(OWNER, owner);
            obj.put(NAME, repo.name);
            obj.put(FULL_NAME, repo.fullName);
            obj.put(DESCRIPTION, repo.description);
            obj.put(PRIVATE, repo.isPrivate);
            obj.put(FORK, repo.isFork);
            obj.put(URL, repo.url);
            obj.put(HTML_URL, repo.htmlUrl);
            obj.put(LANGUAGE, repo.language);
            obj.put(HAS_ISSUES, repo.hasIssues);
            obj.put(STAR_GAZERS, repo.starGazers);
            obj.put(FORK, repo.forks);
            obj.put(WATCHERS, repo.watchers);
            obj.put(ISSUES, repo.issues);
            obj.put(SIZE, repo.size);
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return obj;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Repository && fullName
                .equals(((Repository) obj).fullName) && updatedAt == ((Repository) obj).updatedAt;
    }

    @Override
    public String toString() {
        return "Repository{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", updatedAt=" + updatedAt +
                ", userLogin='" + userLogin + '\'' +
                ", userAvatarUrl='" + userAvatarUrl + '\'' +
                ", userId=" + userId +
                ", fullName='" + fullName + '\'' +
                ", description='" + description + '\'' +
                ", isPrivate=" + isPrivate +
                ", isFork=" + isFork +
                ", url='" + url + '\'' +
                ", htmlUrl='" + htmlUrl + '\'' +
                ", language='" + language + '\'' +
                ", hasIssues=" + hasIssues +
                ", starGazers=" + starGazers +
                ", forks=" + forks +
                ", watchers=" + watchers +
                ", issues=" + issues +
                ", size=" + size +
                ", parent=" + parent +
                ", source=" + source +
                ", licenseKey='" + licenseKey + '\'' +
                ", licenseName='" + licenseName + '\'' +
                ", licenseShortName='" + licenseShortName + '\'' +
                ", licenseUrl='" + licenseUrl + '\'' +
                '}';
    }


    public enum AccessLevel {

        ADMIN, WRITE, READ, NONE

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.name);
        dest.writeLong(this.updatedAt);
        dest.writeString(this.userLogin);
        dest.writeString(this.userAvatarUrl);
        dest.writeInt(this.userId);
        dest.writeString(this.fullName);
        dest.writeString(this.description);
        dest.writeByte(this.isPrivate ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isFork ? (byte) 1 : (byte) 0);
        dest.writeString(this.url);
        dest.writeString(this.htmlUrl);
        dest.writeString(this.language);
        dest.writeByte(this.hasIssues ? (byte) 1 : (byte) 0);
        dest.writeInt(this.starGazers);
        dest.writeInt(this.forks);
        dest.writeInt(this.watchers);
        dest.writeInt(this.issues);
        dest.writeInt(this.size);
        dest.writeParcelable(this.parent, flags);
        dest.writeParcelable(this.source, flags);
        dest.writeString(this.licenseKey);
        dest.writeString(this.licenseName);
        dest.writeString(this.licenseShortName);
        dest.writeString(this.licenseUrl);
        dest.writeLong(this.createdAt);
    }

    protected Repository(Parcel in) {
        this.id = in.readInt();
        this.name = in.readString();
        this.updatedAt = in.readLong();
        this.userLogin = in.readString();
        this.userAvatarUrl = in.readString();
        this.userId = in.readInt();
        this.fullName = in.readString();
        this.description = in.readString();
        this.isPrivate = in.readByte() != 0;
        this.isFork = in.readByte() != 0;
        this.url = in.readString();
        this.htmlUrl = in.readString();
        this.language = in.readString();
        this.hasIssues = in.readByte() != 0;
        this.starGazers = in.readInt();
        this.forks = in.readInt();
        this.watchers = in.readInt();
        this.issues = in.readInt();
        this.size = in.readInt();
        this.parent = in.readParcelable(Repository.class.getClassLoader());
        this.source = in.readParcelable(Repository.class.getClassLoader());
        this.licenseKey = in.readString();
        this.licenseName = in.readString();
        this.licenseShortName = in.readString();
        this.licenseUrl = in.readString();
        this.createdAt = in.readLong();
    }

    public static final Creator<Repository> CREATOR = new Creator<Repository>() {
        @Override
        public Repository createFromParcel(Parcel source) {
            return new Repository(source);
        }

        @Override
        public Repository[] newArray(int size) {
            return new Repository[size];
        }
    };
}
