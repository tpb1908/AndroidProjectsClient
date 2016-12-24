package com.tpb.projects.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.tpb.projects.util.Constants;
import com.tpb.projects.util.Data;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Created by theo on 15/12/16.
 */

public class Card extends DataModel implements Parcelable {
    private static final String TAG = Card.class.getSimpleName();

    public Card() {
    }

    private static final String COLUMN_URL = "column_url";
    private String columnUrl;

    private static final String CONTENT_URL = "content_url";
    private String contentUrl;

    private int issueId;

    private int id;

    private static final String NOTE = "note";
    private String note;

    private boolean requiresLoadingFromIssue;

    private long createdAt;

    private long updatedAt;

    public String getColumnUrl() {
        return columnUrl;
    }

    public void setColumnUrl(String columnUrl) {
        this.columnUrl = columnUrl;
    }

    public String getContentUrl() {
        return contentUrl;
    }


    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note.replace("\n", "\n\n");
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getIssueId() {
        return issueId;
    }

    public boolean requiresLoadingFromIssue() {
        return requiresLoadingFromIssue;
    }

    public void setRequiresLoadingFromIssue(boolean requiresLoadingFromIssue) {
        this.requiresLoadingFromIssue = requiresLoadingFromIssue;
    }

    public void setFromIssue(Issue issue) {
        requiresLoadingFromIssue = false;

    }

    public static Card parse(JSONObject object) {
        final Card c = new Card();
        try {
            c.id = object.getInt(ID);
            c.columnUrl = object.getString(COLUMN_URL);
            if(object.has(CONTENT_URL)) {
                c.contentUrl = object.getString(CONTENT_URL);
                c.issueId = Integer.parseInt(c.contentUrl.substring(c.contentUrl.lastIndexOf('/') + 1));
            }
            c.note = object.getString(NOTE);
            if(Constants.JSON_NULL.equals(c.note)) {
                c.note = "";
                c.requiresLoadingFromIssue = true;
            } else {
                c.note = c.note.replace("\n", "\n\n");
            }
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
        return obj instanceof Card && ((Card) obj).id == id;
    }

    @Override
    public String toString() {
        return "Card{" +
                "columnUrl='" + columnUrl + '\'' +
                ", contentUrl='" + contentUrl + '\'' +
                ", issueId=" + issueId +
                ", id=" + id +
                ", note='" + note + '\'' +
                ", requiresLoadingFromIssue=" + requiresLoadingFromIssue +
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
        dest.writeString(this.columnUrl);
        dest.writeString(this.contentUrl);
        dest.writeInt(this.issueId);
        dest.writeInt(this.id);
        dest.writeString(this.note);
        dest.writeByte(this.requiresLoadingFromIssue ? (byte) 1 : (byte) 0);
        dest.writeLong(this.createdAt);
        dest.writeLong(this.updatedAt);
    }

    protected Card(Parcel in) {
        this.columnUrl = in.readString();
        this.contentUrl = in.readString();
        this.issueId = in.readInt();
        this.id = in.readInt();
        this.note = in.readString();
        this.requiresLoadingFromIssue = in.readByte() != 0;
        this.createdAt = in.readLong();
        this.updatedAt = in.readLong();
    }

    public static final Parcelable.Creator<Card> CREATOR = new Parcelable.Creator<Card>() {
        @Override
        public Card createFromParcel(Parcel source) {
            return new Card(source);
        }

        @Override
        public Card[] newArray(int size) {
            return new Card[size];
        }
    };
}
