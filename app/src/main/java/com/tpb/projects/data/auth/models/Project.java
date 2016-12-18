package com.tpb.projects.data.auth.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.tpb.projects.util.Data;

import org.json.JSONObject;

/**
 * Created by theo on 15/12/16.
 */

public class Project extends DataModel implements Parcelable{
    private static final String TAG = Project.class.getSimpleName();

    public Project() {}

    private static final String OWNER_URL = "owner_url";
    private String ownerUrl;

    private String url;

    private static final String CREATOR = "creator";
    private String name;

    private static final String BODY = "body";
    private String body;

    private static final String NUMBER = "number";
    private int number;

    private static final String LOGIN = "login";
    private String creatorUserName;

    private long createdAt;

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

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public static Project parse(JSONObject object) {
        final Project p = new Project();
        try {
            p.creatorUserName = object.getJSONObject(CREATOR).getString(LOGIN);
            p.number = object.getInt(NUMBER);
            p.body = object.getString(BODY);
            p.name = object.getString(NAME);
            p.url = object.getString(URL);
            p.ownerUrl = object.getString(OWNER_URL);
            p.createdAt = Data.toCalendar(object.getString(CREATED_AT)).getTimeInMillis() / 1000;
            p.updatedAt = Data.toCalendar(object.getString(UPDATED_AT)).getTimeInMillis() / 1000;
        } catch(Exception jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return p;
    }



    @Override
    public String toString() {
        return "Project{" +
                "ownerUrl='" + ownerUrl + '\'' +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", body='" + body + '\'' +
                ", number=" + number +
                ", creatorUserName=" + creatorUserName +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.ownerUrl);
        dest.writeString(this.url);
        dest.writeString(this.name);
        dest.writeString(this.body);
        dest.writeInt(this.number);
        dest.writeString(this.creatorUserName);
        dest.writeLong(this.createdAt);
        dest.writeLong(this.updatedAt);
    }

    protected Project(Parcel in) {
        this.ownerUrl = in.readString();
        this.url = in.readString();
        this.name = in.readString();
        this.body = in.readString();
        this.number = in.readInt();
        this.creatorUserName = in.readString();
        this.createdAt = in.readLong();
        this.updatedAt = in.readLong();
    }

    public static final Creator<Project> PARCEL_CREATOR = new Creator<Project>() {
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
