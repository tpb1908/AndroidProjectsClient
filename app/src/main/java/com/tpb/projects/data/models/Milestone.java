package com.tpb.projects.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.tpb.projects.util.Data;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Created by theo on 24/02/17.
 */

public class Milestone extends DataModel implements Parcelable {
    private static final String TAG = Milestone.class.getSimpleName();

    private int id;
    private static final String NUMBER = "number";
    private int number;
    private String url;
    private static final String HTML_URL = "html_url";
    private String htmlUrl;
    private static final String STATE = "state";
    private String stateString;
    private State state;
    private static final String TITLE = "title";
    private String title;
    private static final String DESCRIPTION = "description";
    private String description;
    private static final String KEY_CREATOR = "creator";
    private User creator;
    private static final String OPEN_ISSUES = "open_issues";
    private int openIssues;
    private static final String CLOSED_ISSUES = "closed_issues";
    private int closedIssues;
    private long createdAt;
    private long updatedAt;
    private static final String CLOSED_AT = "closed_at";
    private long closedAt;
    private static final String DUE_ON = "due_on";
    private long dueOn;

    public static Milestone parse(JSONObject obj) {
        final Milestone m = new Milestone();
        try {
            m.id = obj.getInt(ID);
            m.number = obj.getInt(NUMBER);
            m.url = obj.getString(URL);
            m.htmlUrl = obj.getString(HTML_URL);
            m.stateString = obj.getString(STATE);
            m.state = State.fromString(m.stateString);
            m.title = obj.getString(TITLE);
            m.description = obj.getString(DESCRIPTION);
            m.creator = User.parse(obj.getJSONObject(KEY_CREATOR));
            m.openIssues = obj.getInt(OPEN_ISSUES);
            m.closedIssues = obj.getInt(CLOSED_ISSUES);
            try {
                m.createdAt = Data.toCalendar(obj.getString(CREATED_AT)).getTimeInMillis();
            } catch(ParseException pe) {
                Log.e(TAG, "parse: ", pe);
            }
            try {
                m.updatedAt = Data.toCalendar(obj.getString(UPDATED_AT)).getTimeInMillis();
            } catch(ParseException pe) {
                Log.e(TAG, "parse: ", pe);
            }
            if(!obj.getString(CLOSED_AT).equals(JSON_NULL)) {
                try {
                    m.closedAt = Data.toCalendar(obj.getString(CLOSED_AT)).getTimeInMillis();
                } catch(ParseException pe) {
                    Log.e(TAG, "parse: ", pe);
                }
            }
            if(!obj.getString(DUE_ON).equals(JSON_NULL)) {
                try {
                    m.dueOn = Data.toCalendar(obj.getString(DUE_ON)).getTimeInMillis();
                } catch(ParseException pe) {
                    Log.e(TAG, "parse: ", pe);
                }
            }
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return m;
    }

    @Override
    public long getCreatedAt() {
        return createdAt;
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

    public State getState() {
        return state;
    }

    public String getTitle() {
        return title;
    }

    public User getCreator() {
        return creator;
    }

    public int getOpenIssues() {
        return openIssues;
    }

    public int getClosedIssues() {
        return closedIssues;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getClosedAt() {
        return closedAt;
    }

    public long getDueOn() {
        return dueOn;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    @Override
    public String toString() {
        return "Milestone{" +
                "id=" + id +
                ", number=" + number +
                ", url='" + url + '\'' +
                ", htmlUrl='" + htmlUrl + '\'' +
                ", stateString='" + stateString + '\'' +
                ", state=" + state +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", creator=" + creator +
                ", openIssues=" + openIssues +
                ", closedIssues=" + closedIssues +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", closedAt=" + closedAt +
                ", dueOn=" + dueOn +
                '}';
    }

    private Milestone() {

    }

    public Milestone(String title, String description, long dueOn) {
        this.title = title;
        this.description = description;
        this.dueOn = dueOn;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Milestone && ((Milestone) obj).id == id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeInt(this.number);
        dest.writeString(this.url);
        dest.writeString(this.htmlUrl);
        dest.writeString(this.stateString);
        dest.writeInt(this.state == null ? -1 : this.state.ordinal());
        dest.writeString(this.title);
        dest.writeString(this.description);
        dest.writeParcelable(this.creator, flags);
        dest.writeInt(this.openIssues);
        dest.writeInt(this.closedIssues);
        dest.writeLong(this.createdAt);
        dest.writeLong(this.updatedAt);
        dest.writeLong(this.closedAt);
        dest.writeLong(this.dueOn);
        dest.writeLong(this.createdAt);
    }

    protected Milestone(Parcel in) {
        this.id = in.readInt();
        this.number = in.readInt();
        this.url = in.readString();
        this.htmlUrl = in.readString();
        this.stateString = in.readString();
        int tmpState = in.readInt();
        this.state = tmpState == -1 ? null : State.values()[tmpState];
        this.title = in.readString();
        this.description = in.readString();
        this.creator = in.readParcelable(User.class.getClassLoader());
        this.openIssues = in.readInt();
        this.closedIssues = in.readInt();
        this.createdAt = in.readLong();
        this.updatedAt = in.readLong();
        this.closedAt = in.readLong();
        this.dueOn = in.readLong();
        this.createdAt = in.readLong();
    }

    public static final Creator<Milestone> CREATOR = new Creator<Milestone>() {
        @Override
        public Milestone createFromParcel(Parcel source) {
            return new Milestone(source);
        }

        @Override
        public Milestone[] newArray(int size) {
            return new Milestone[size];
        }
    };
}
