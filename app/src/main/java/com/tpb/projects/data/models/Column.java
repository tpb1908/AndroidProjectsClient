/*
 * Copyright  2016 Theo Pearson-Bray
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

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
                c.createdAt = Data.toCalendar(object.getString(CREATED_AT)).getTimeInMillis();
                c.updatedAt = Data.toCalendar(object.getString(UPDATED_AT)).getTimeInMillis();
            } catch(ParseException pe) {
                Log.e(TAG, "parse: ", pe);
            }
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return c;
    }

    public static JSONObject parse(Column column) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(ID, column.id);
            obj.put(NAME, column.name);
            obj.put(PROJECT_URL, column.projectUrl);
            obj.put(CREATED_AT, Data.toISO8061(column.createdAt));
            obj.put(UPDATED_AT, Data.toISO8061(column.updatedAt));
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return obj;
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
