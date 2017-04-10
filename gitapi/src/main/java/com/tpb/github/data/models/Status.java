package com.tpb.github.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.tpb.github.data.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Created by theo on 02/04/17.
 */

public class Status extends DataModel implements Parcelable {

    private long updatedAt;

    private static final String STATE = "state";
    private String state;

    private static final String TARGET_URL = "target_url";
    private String targetUrl;

    private int id;

    private static final String DESCRIPTION = "description";
    private String description;

    private String url;

    private static final String CONTEXT = "context";
    private String context;

    private static final String KEY_CREATOR = "creator";
    private User creator;

    public Status(JSONObject obj) {
        try {
            try {
                createdAt = Util.toCalendar(obj.getString(CREATED_AT)).getTimeInMillis();
                updatedAt = Util.toCalendar(obj.getString(UPDATED_AT)).getTimeInMillis();
            } catch(ParseException ignored) {
            }
            state = obj.getString(STATE);
            targetUrl = obj.getString(TARGET_URL);
            id = obj.getInt(ID);
            description = obj.getString(DESCRIPTION);
            url = obj.getString(URL);
            context = obj.getString(CONTEXT);
            if(obj.has(KEY_CREATOR)) creator = new User(obj.getJSONObject(KEY_CREATOR));
        } catch(JSONException jse) {
        }
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public String getState() {
        return state;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public String getContext() {
        return context;
    }

    public User getCreator() {
        return creator;
    }

    @Override
    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Status{" +
                "updatedAt=" + updatedAt +
                ", state='" + state + '\'' +
                ", targetUrl='" + targetUrl + '\'' +
                ", id=" + id +
                ", description='" + description + '\'' +
                ", url='" + url + '\'' +
                ", context='" + context + '\'' +
                ", creator=" + creator +
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.updatedAt);
        dest.writeString(this.state);
        dest.writeString(this.targetUrl);
        dest.writeInt(this.id);
        dest.writeString(this.description);
        dest.writeString(this.url);
        dest.writeString(this.context);
        dest.writeParcelable(this.creator, flags);
        dest.writeLong(this.createdAt);
    }

    protected Status(Parcel in) {
        this.updatedAt = in.readLong();
        this.state = in.readString();
        this.targetUrl = in.readString();
        this.id = in.readInt();
        this.description = in.readString();
        this.url = in.readString();
        this.context = in.readString();
        this.creator = in.readParcelable(User.class.getClassLoader());
        this.createdAt = in.readLong();
    }

    public static final Parcelable.Creator<Status> CREATOR = new Parcelable.Creator<Status>() {
        @Override
        public Status createFromParcel(Parcel source) {
            return new Status(source);
        }

        @Override
        public Status[] newArray(int size) {
            return new Status[size];
        }
    };
}
