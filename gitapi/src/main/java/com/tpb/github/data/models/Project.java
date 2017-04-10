package com.tpb.github.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.tpb.github.data.Util;

import org.json.JSONObject;

/**
 * Created by theo on 15/12/16.
 */

public class Project extends DataModel implements Parcelable {
    private static final String TAG = Project.class.getSimpleName();

    public Project(JSONObject obj) {
        try {
            id = obj.getInt(ID);
            creatorUserName = obj.getJSONObject(CREATOR_KEY).getString(LOGIN);
            number = obj.getInt(NUMBER);
            body = obj.getString(BODY);
            if(body != null) {
                body = body.replace("\n", "\n\n");
            }
            name = obj.getString(NAME);
            url = obj.getString(URL);
            ownerUrl = obj.getString(OWNER_URL);
            createdAt = Util.toCalendar(obj.getString(CREATED_AT)).getTimeInMillis();
            updatedAt = Util.toCalendar(obj.getString(UPDATED_AT)).getTimeInMillis();
            state = State.fromString(obj.getString(STATE));
        } catch(Exception jse) {
            Log.e(TAG, "parse: ", jse);
        }
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

    private static final String STATE = "state";
    private State state;

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

    public State getState() {
        return state;
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
                ", state=" + state +
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
        dest.writeInt(this.state == null ? -1 : this.state.ordinal());
        dest.writeLong(this.updatedAt);
        dest.writeLong(this.createdAt);
    }

    protected Project(Parcel in) {
        this.id = in.readInt();
        this.ownerUrl = in.readString();
        this.url = in.readString();
        this.name = in.readString();
        this.body = in.readString();
        this.number = in.readInt();
        this.creatorUserName = in.readString();
        int tmpState = in.readInt();
        this.state = tmpState == -1 ? null : State.values()[tmpState];
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
