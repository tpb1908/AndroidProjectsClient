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
 * Created by theo on 03/01/17.
 */

public class Comment extends DataModel implements Parcelable {
    private static final String TAG = Comment.class.getSimpleName();

    private Comment() {}

    private int id;

    private String url;

    private static final String HTML_URL = "html_url";
    private String htmlUrl;

    private static final String BODY = "body";
    private String body;

    private static final String USER = "user";
    private User user;

    private long createdAt;

    private long updatedAt;

    public int getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public String getBody() {
        return body;
    }

    public User getUser() {
        return user;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public static Comment parse(JSONObject obj) {
        final Comment c = new Comment();
        try {
            c.id = obj.getInt(ID);
            c.url = obj.getString(URL);
            c.htmlUrl = obj.getString(HTML_URL);
            c.body = obj.getString(BODY);
            c.user = User.parse(obj.getJSONObject(USER));
            try {
                c.createdAt = Data.toCalendar(obj.getString(CREATED_AT)).getTimeInMillis();
                c.updatedAt = Data.toCalendar(obj.getString(UPDATED_AT)).getTimeInMillis();
            } catch(ParseException pe) {
                Log.e(TAG, "parse: ", pe);
            }
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return c;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", htmlUrl='" + htmlUrl + '\'' +
                ", body='" + body + '\'' +
                ", user=" + user +
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
        dest.writeString(this.url);
        dest.writeString(this.htmlUrl);
        dest.writeString(this.body);
        dest.writeParcelable(this.user, flags);
        dest.writeLong(this.createdAt);
        dest.writeLong(this.updatedAt);
    }

    protected Comment(Parcel in) {
        this.id = in.readInt();
        this.url = in.readString();
        this.htmlUrl = in.readString();
        this.body = in.readString();
        this.user = in.readParcelable(User.class.getClassLoader());
        this.createdAt = in.readLong();
        this.updatedAt = in.readLong();
    }

    public static final Parcelable.Creator<Comment> CREATOR = new Parcelable.Creator<Comment>() {
        @Override
        public Comment createFromParcel(Parcel source) {
            return new Comment(source);
        }

        @Override
        public Comment[] newArray(int size) {
            return new Comment[size];
        }
    };
}
