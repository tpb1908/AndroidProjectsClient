package com.tpb.github.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;

import com.tpb.github.data.Util;

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
    private String stateString;
    private State state;

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
    private String repoFullName;

    private static final String LOCKED = "locked";
    private boolean isLocked;

    private static final String MILESTONE = "milestone";
    private Milestone milestone;
    
    public Issue(JSONObject obj) {
        try {
            id = obj.getInt(ID);
            number = obj.getInt(NUMBER);
            stateString = obj.getString(STATE);
            state = State.fromString(stateString);
            title = obj.getString(TITLE);
            body = obj.getString(BODY);
            comments = obj.getInt(COMMENTS);
            isLocked = obj.getBoolean(LOCKED);
            repoFullName = obj.getString(REPOSITORY_URL).substring(29);
            if(!obj.getString(CLOSED_AT).equals(JSON_NULL)) {
                try {
                    closedAt = Util.toCalendar(obj.getString(CLOSED_AT)).getTimeInMillis();
                    closed = true;
                } catch(ParseException pe) {
                    Log.e(TAG, "parse: ", pe);
                }
            }
            try {
                createdAt = Util.toCalendar(obj.getString(CREATED_AT)).getTimeInMillis();
            } catch(ParseException pe) {
                Log.e(TAG, "parse: ", pe);
            }
            if(obj.has(ASSIGNEE) && !obj.getString(ASSIGNEE).equals(JSON_NULL)) {
                assignees = new User[] {new User(obj.getJSONObject(ASSIGNEE))};
            }
            if(obj.has(ASSIGNEES) && obj.getJSONArray(ASSIGNEES).length() > 0) {
                final JSONArray as = obj.getJSONArray(ASSIGNEES);
                assignees = new User[as.length()];
                for(int j = 0; j < as.length(); j++) {
                    assignees[j] = new User(as.getJSONObject(j));
                }
            }
            openedBy = new User(obj.getJSONObject(USER));
            if(obj.has(CLOSED_BY) && !obj.getString(CLOSED_BY).equals(JSON_NULL)) {
                closedBy = new User(obj.getJSONObject(CLOSED_BY));
            }
            try {
                final JSONArray lbs = obj.getJSONArray(LABELS);
                labels = new Label[lbs.length()];
                for(int j = 0; j < lbs.length(); j++) {
                    labels[j] = new Label(lbs.getJSONObject(j));
                }
            } catch(JSONException jse) {
                Log.e(TAG, "parse: Labels: ", jse);
            }
            if(obj.has(MILESTONE) && !obj.getString(MILESTONE).equals(JSON_NULL)) {
                milestone = new Milestone(obj.getJSONObject(MILESTONE));
            }
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
    }

    public int getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }

    public String getStateString() {
        return stateString;
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

    public String getRepoFullName() {
        return repoFullName;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public State getState() {
        return state;
    }

    public Milestone getMilestone() {
        return milestone;
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

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Issue && ((Issue) obj).id == id;
    }

    @Override
    public String toString() {
        return "Issue{" +
                "id=" + id +
                ", number=" + number +
                ", stateString='" + stateString + '\'' +
                ", state=" + state +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                ", closedAt=" + closedAt +
                ", closed=" + closed +
                ", openedBy=" + openedBy +
                ", closedBy=" + closedBy +
                ", assignees=" + Arrays.toString(assignees) +
                ", labels=" + Arrays.toString(labels) +
                ", comments=" + comments +
                ", repoFullName='" + repoFullName + '\'' +
                ", isLocked=" + isLocked +
                ", milestone=" + milestone +
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
        dest.writeString(this.stateString);
        dest.writeInt(this.state == null ? -1 : this.state.ordinal());
        dest.writeString(this.title);
        dest.writeString(this.body);
        dest.writeLong(this.closedAt);
        dest.writeByte(this.closed ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.openedBy, flags);
        dest.writeParcelable(this.closedBy, flags);
        dest.writeTypedArray(this.assignees, flags);
        dest.writeTypedArray(this.labels, flags);
        dest.writeInt(this.comments);
        dest.writeString(this.repoFullName);
        dest.writeByte(this.isLocked ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.milestone, flags);
        dest.writeLong(this.createdAt);
    }

    protected Issue(Parcel in) {
        this.id = in.readInt();
        this.number = in.readInt();
        this.stateString = in.readString();
        int tmpState = in.readInt();
        this.state = tmpState == -1 ? null : State.values()[tmpState];
        this.title = in.readString();
        this.body = in.readString();
        this.closedAt = in.readLong();
        this.closed = in.readByte() != 0;
        this.openedBy = in.readParcelable(User.class.getClassLoader());
        this.closedBy = in.readParcelable(User.class.getClassLoader());
        this.assignees = in.createTypedArray(User.CREATOR);
        this.labels = in.createTypedArray(Label.CREATOR);
        this.comments = in.readInt();
        this.repoFullName = in.readString();
        this.isLocked = in.readByte() != 0;
        this.milestone = in.readParcelable(Milestone.class.getClassLoader());
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
