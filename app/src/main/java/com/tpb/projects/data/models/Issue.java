package com.tpb.projects.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
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

    private static final String USER = "user";
    private User openedBy;

    private static final String CLOSED_BY = "closed_by";
    private User closedBy;

    private static final String ASSIGNEE = "assignee";
    private static final String ASSIGNEES = "assignees";
    private User[] assignees;

    private static final String LABELS = "labels";
    private Label[] labels;

    private static final String COMMENTS = "comments";
    private int comments;

    private static final String REPOSITORY_URL = "repository_url";
    private String repoPath;

    private static final String LOCKED = "locked";
    private boolean isLocked;

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

    public User getOpenedBy() {
        return openedBy;
    }

    public int getComments() {
        return comments;
    }

    @Override
    public long getCreatedAt() {
        return createdAt;
    }

    public String getRepoPath() {
        return repoPath;
    }

    public boolean isLocked() {
        return isLocked;
    }

    @Nullable
    public Label[] getLabels() {
        return labels;
    }

    @Nullable
    public User[] getAssignees() {
        return assignees;
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
            i.comments = obj.getInt(COMMENTS);
            i.isLocked = obj.getBoolean(LOCKED);
            //https://api.github.com/repos/user/repo
            i.repoPath = obj.getString(REPOSITORY_URL).substring(29);
            if(!obj.getString(CLOSED_AT).equals(Constants.JSON_NULL)) {
                try {
                    i.closedAt = Data.toCalendar(obj.getString(CLOSED_AT)).getTimeInMillis();
                    i.closed = true;
                } catch(ParseException pe) {
                    Log.e(TAG, "parse: ", pe);
                }
            }
            try {
                i.createdAt = Data.toCalendar(obj.getString(CREATED_AT)).getTimeInMillis();
            } catch(ParseException pe) {
                Log.e(TAG, "parse: ", pe);
            }
            if(obj.has(ASSIGNEE) && !obj.getString(ASSIGNEE).equals(Constants.JSON_NULL)) {
                i.assignees = new User[] {User.parse(obj.getJSONObject(ASSIGNEE))};
            }
            if(obj.has(ASSIGNEES) && obj.getJSONArray(ASSIGNEES).length() > 0) {
                final JSONArray as = obj.getJSONArray(ASSIGNEES);
                i.assignees = new User[as.length()];
                for(int j = 0; j < as.length(); j++) {
                    i.assignees[j] = User.parse(as.getJSONObject(j));
                }
            }
            i.openedBy = User.parse(obj.getJSONObject(USER));
            if(obj.has(CLOSED_BY) && !obj.getString(CLOSED_BY).equals(Constants.JSON_NULL)) {
                i.closedBy = User.parse(obj.getJSONObject(CLOSED_BY));
                Log.i(TAG, "parse: Parsed issue closed_by " + i.closedBy.toString());
            }
            try {
                final JSONArray lbs = obj.getJSONArray(LABELS);
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

    public enum IssueState {

        OPEN, CLOSED, ALL

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
                ", openedBy=" + openedBy +
                ", closedBy=" + closedBy +
                ", assignees=" + Arrays.toString(assignees) +
                ", labels=" + Arrays.toString(labels) +
                ", comments=" + comments +
                ", repoPath='" + repoPath + '\'' +
                ", isLocked=" + isLocked +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Issue && ((Issue) obj).id == id;
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
        dest.writeParcelable(this.openedBy, flags);
        dest.writeParcelable(this.closedBy, flags);
        dest.writeTypedArray(this.assignees, flags);
        dest.writeTypedArray(this.labels, flags);
        dest.writeInt(this.comments);
        dest.writeString(this.repoPath);
        dest.writeByte(this.isLocked ? (byte) 1 : (byte) 0);
        dest.writeLong(this.createdAt);
    }

    Issue(Parcel in) {
        this.id = in.readInt();
        this.number = in.readInt();
        this.state = in.readString();
        this.title = in.readString();
        this.body = in.readString();
        this.closedAt = in.readLong();
        this.closed = in.readByte() != 0;
        this.openedBy = in.readParcelable(User.class.getClassLoader());
        this.closedBy = in.readParcelable(User.class.getClassLoader());
        this.assignees = in.createTypedArray(User.CREATOR);
        this.labels = in.createTypedArray(Label.CREATOR);
        this.comments = in.readInt();
        this.repoPath = in.readString();
        this.isLocked = in.readByte() != 0;
        this.createdAt = in.readLong();
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
