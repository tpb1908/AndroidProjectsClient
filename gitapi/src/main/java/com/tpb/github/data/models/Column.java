package com.tpb.github.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.tpb.github.data.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Created by theo on 15/12/16.
 */

public class Column extends DataModel implements Parcelable {
    private static final String TAG = Column.class.getSimpleName();

    public Column(JSONObject obj) {
        try {
            id = obj.getInt(ID);
            name = obj.getString(NAME);
            projectUrl = obj.getString(PROJECT_URL);
            try {
                createdAt = Util.toCalendar(obj.getString(CREATED_AT)).getTimeInMillis();
                updatedAt = Util.toCalendar(obj.getString(UPDATED_AT)).getTimeInMillis();
            } catch(ParseException pe) {
                Log.e(TAG, "parse: ", pe);
            }
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
    }

    private int id;

    private String name;

    private static final String PROJECT_URL = "project_url";
    private String projectUrl;

    private long updatedAt;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getProjectUrl() {
        return projectUrl;
    }

    @Override
    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Column && ((Column) obj).id == id;
    }

    @Override
    public String toString() {
        return "Column{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", projectUrl='" + projectUrl + '\'' +
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
        dest.writeString(this.name);
        dest.writeString(this.projectUrl);
        dest.writeLong(this.updatedAt);
        dest.writeLong(this.createdAt);
    }

    protected Column(Parcel in) {
        this.id = in.readInt();
        this.name = in.readString();
        this.projectUrl = in.readString();
        this.updatedAt = in.readLong();
        this.createdAt = in.readLong();
    }

    public static final Creator<Column> CREATOR = new Creator<Column>() {
        @Override
        public Column createFromParcel(Parcel source) {
            return new Column(source);
        }

        @Override
        public Column[] newArray(int size) {
            return new Column[size];
        }
    };
}
