package com.tpb.projects.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.tpb.projects.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by theo on 15/12/16.
 */

public class Project extends DataModel implements Parcelable {
    private static final String TAG = Project.class.getSimpleName();

    public Project() {
    }

    private int id;

    private static final String CREATOR_KEY = "creator";
    private static final String OWNER_URL = "owner_url";
    private String ownerUrl;

    private String url;

    private String name;

    private static final String BODY = "body";
    private String body;

    private static final String NUMBER = "number";
    private int number;

    private static final String LOGIN = "login";
    private String creatorUserName;

    private long updatedAt;

    public String getOwnerUrl() {
        return ownerUrl;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public String getBody() {
        return body;
    }

    public int getNumber() {
        return number;
    }

    public String getCreatorUserName() {
        return creatorUserName;
    }

    @Override
    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public int getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getRepoPath() {
        return ownerUrl.substring(ownerUrl.indexOf("s/") + 2);
    }

    public static Project parse(JSONObject object) {
        final Project p = new Project();
        try {
            p.id = object.getInt(ID);
            p.creatorUserName = object.getJSONObject(CREATOR_KEY).getString(LOGIN);
            p.number = object.getInt(NUMBER);
            p.body = object.getString(BODY);
            if(p.body != null) {
                p.body = p.body.replace("\n", "\n\n");
            }
            p.name = object.getString(NAME);
            p.url = object.getString(URL);
            p.ownerUrl = object.getString(OWNER_URL);
            p.createdAt = Util.toCalendar(object.getString(CREATED_AT)).getTimeInMillis();
            p.updatedAt = Util.toCalendar(object.getString(UPDATED_AT)).getTimeInMillis();
        } catch(Exception jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return p;
    }

    public static JSONObject parse(Project project) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(ID, project.id);
            final JSONObject creator = new JSONObject();
            creator.put(LOGIN, project.creatorUserName);
            obj.put(CREATOR_KEY, creator);
            obj.put(NUMBER, project.number);
            obj.put(BODY, project.body);
            obj.put(NAME, project.name);
            obj.put(URL, project.url);
            obj.put(OWNER_URL, project.ownerUrl);
            obj.put(CREATED_AT, Util.toISO8061FromSeconds(project.createdAt));
            obj.put(UPDATED_AT, Util.toISO8061FromSeconds(project.updatedAt));
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return obj;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Project && ((Project) obj).id == id;
    }

    @Override
    public String toString() {
        return "Project{" +
                "id=" + id +
                ", ownerUrl='" + ownerUrl + '\'' +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", body='" + body + '\'' +
                ", number=" + number +
                ", creatorUserName='" + creatorUserName + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.ownerUrl);
        dest.writeString(this.url);
        dest.writeString(this.name);
        dest.writeString(this.body);
        dest.writeInt(this.number);
        dest.writeString(this.creatorUserName);
        dest.writeLong(this.updatedAt);
        dest.writeLong(this.createdAt);
    }

    private Project(Parcel in) {
        this.id = in.readInt();
        this.ownerUrl = in.readString();
        this.url = in.readString();
        this.name = in.readString();
        this.body = in.readString();
        this.number = in.readInt();
        this.creatorUserName = in.readString();
        this.updatedAt = in.readLong();
        this.createdAt = in.readLong();
    }

    public static final Creator<Project> CREATOR = new Creator<Project>() {
        @Override
        public Project createFromParcel(Parcel source) {
            return new Project(source);
        }

        @Override
        public Project[] newArray(int size) {
            return new Project[size];
        }
    };
}
