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

import com.tpb.projects.util.Constants;
import com.tpb.projects.util.Data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Arrays;

/**
 * Created by theo on 20/12/16.
 */

public class Issue extends DataModel implements Parcelable {
    private static final String TAG = Issue.class.getSimpleName();

    public Issue() {
    }

    private int id;

    private static final String NUMBER = "number";
    private int number;

    private static final String STATE = "state";
    private String state;

    private static final String TITLE = "title";
    private String title;

    private static final String BODY = "body";
    private String body;

    private static final String CLOSED_AT = "closed_at";
    private long closedAt;
    private boolean closed;
    
    
    private User closedBy;
    
    private User assignee;

    private Label[] labels;

    public int getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }

    public String getState() {
        return state;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public long getClosedAt() {
        return closedAt;
    }

    public Label[] getLabels() {
        return labels;
    }

    public User getAssignee() {
        return assignee;
    }

    public User getClosedBy() {
        return closedBy;
    }

    public static Issue parse(JSONObject obj) {
        final Issue i = new Issue();
        try {
            i.id = obj.getInt(ID);
            i.number = obj.getInt(NUMBER);
            i.state = obj.getString(STATE);
            i.title = obj.getString(TITLE);
            i.body = obj.getString(BODY);
            if(!obj.getString(CLOSED_AT).equals(Constants.JSON_NULL)) {
                try {
                    i.closedAt = Data.toCalendar(obj.getString(CLOSED_AT)).getTimeInMillis() / 1000;
                } catch(ParseException pe) {
                    Log.e(TAG, "parse: ", pe);
                }
                i.closed = true;
            }
            if(obj.has("assignee") && !obj.getString("assignee").equals(Constants.JSON_NULL)) {
                i.assignee = User.parse(obj.getJSONObject("assignee"));
                Log.i(TAG, "parse: Parsed issue assignee " + i.assignee.toString());
            }
            if(obj.has("closed_by") && !obj.getString("closed_by").equals(Constants.JSON_NULL)) {
                i.closedBy = User.parse(obj.getJSONObject("closed_by"));
                Log.i(TAG, "parse: Parsed issue closed_by " + i.closedBy.toString());
            }
            try {
                final JSONArray lbs = obj.getJSONArray("labels");
                i.labels = new Label[lbs.length()];
                for(int j = 0; j < lbs.length(); j++) {
                    i.labels[j] = Label.parse(lbs.getJSONObject(j));
                }
            } catch(JSONException jse) {
                Log.e(TAG, "parse: Labels: ", jse);
            }
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return i;
    }

    public static JSONObject parse(Issue issue) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(ID, issue.id);
            obj.put(NUMBER, issue.number);
            obj.put(STATE, issue.state);
            obj.put(TITLE, issue.title);
            obj.put(BODY, issue.body);
            if(issue.closedAt != 0) obj.put(CLOSED_AT, issue.closedAt);
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return obj;
    }

    @Override
    public String toString() {
        return "Issue{" +
                "id=" + id +
                ", number=" + number +
                ", state='" + state + '\'' +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                ", closedAt=" + closedAt +
                ", closed=" + closed +
                ", closedBy=" + closedBy +
                ", assignee=" + assignee +
                ", labels=" + Arrays.toString(labels) +
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeInt(this.number);
        dest.writeString(this.state);
        dest.writeString(this.title);
        dest.writeString(this.body);
        dest.writeLong(this.closedAt);
        dest.writeByte(this.closed ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.closedBy, flags);
        dest.writeParcelable(this.assignee, flags);
        dest.writeTypedArray(this.labels, flags);
    }

    protected Issue(Parcel in) {
        this.id = in.readInt();
        this.number = in.readInt();
        this.state = in.readString();
        this.title = in.readString();
        this.body = in.readString();
        this.closedAt = in.readLong();
        this.closed = in.readByte() != 0;
        this.closedBy = in.readParcelable(User.class.getClassLoader());
        this.assignee = in.readParcelable(User.class.getClassLoader());
        this.labels = in.createTypedArray(Label.CREATOR);
    }

    public static final Creator<Issue> CREATOR = new Creator<Issue>() {
        @Override
        public Issue createFromParcel(Parcel source) {
            return new Issue(source);
        }

        @Override
        public Issue[] newArray(int size) {
            return new Issue[size];
        }
    };
}
