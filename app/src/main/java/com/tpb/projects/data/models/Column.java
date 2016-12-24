package com.tpb.projects.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.tpb.projects.util.Data;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Created by theo on 15/12/16.
 */

public class Column extends DataModel implements Parcelable {
    private static final String TAG = Column.class.getSimpleName();

    private Column() {
    }

    private int id;

    private String name;

    private static final String PROJECT_URL = "project_url";
    private String projectUrl;

    private long createdAt;

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

    public static Column parse(JSONObject object) {
        final Column c = new Column();
        try {
            c.id = object.getInt(ID);
            c.name = object.getString(NAME);
            c.projectUrl = object.getString(PROJECT_URL);
            try {
                c.createdAt = Data.toCalendar(object.getString(CREATED_AT)).getTimeInMillis() / 1000;
                c.updatedAt = Data.toCalendar(object.getString(UPDATED_AT)).getTimeInMillis() / 1000;
            } catch(ParseException pe) {
                Log.e(TAG, "parse: ", pe);
            }
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return c;
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
        dest.writeInt(this.id);
        dest.writeString(this.name);
        dest.writeString(this.projectUrl);
        dest.writeLong(this.createdAt);
        dest.writeLong(this.updatedAt);
    }

    protected Column(Parcel in) {
        this.id = in.readInt();
        this.name = in.readString();
        this.projectUrl = in.readString();
        this.createdAt = in.readLong();
        this.updatedAt = in.readLong();
    }

    public static final Parcelable.Creator<Column> CREATOR = new Parcelable.Creator<Column>() {
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
