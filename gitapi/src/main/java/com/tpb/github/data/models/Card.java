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

public class Card extends DataModel implements Parcelable {
    private static final String TAG = Card.class.getSimpleName();

    public Card() {
    }
    
    public Card(JSONObject obj) {
        try {
            id = obj.getInt(ID);
            columnUrl = obj.getString(COLUMN_URL);
            if(obj.has(CONTENT_URL)) {
                contentUrl = obj.getString(CONTENT_URL);
                issueId = Integer
                        .parseInt(contentUrl.substring(contentUrl.lastIndexOf('/') + 1));

            }
            note = obj.getString(NOTE);
            if(JSON_NULL.equals(note)) {
                note = "";
                requiresLoadingFromIssue = true;
            }
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

    private static final String COLUMN_URL = "column_url";
    private String columnUrl;

    private static final String CONTENT_URL = "content_url";
    private String contentUrl;

    private int issueId;

    private Issue issue;

    private int id;

    private static final String NOTE = "note";
    private String note;

    private boolean requiresLoadingFromIssue;

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
        this.note = note;
    }

    @Override
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

    public boolean hasIssue() {
        return issue != null;
    }

    public Issue getIssue() {
        return issue;
    }

    public boolean requiresLoadingFromIssue() {
        return requiresLoadingFromIssue && issue == null;
    }


    public void setFromIssue(Issue issue) {
        note = issue.getTitle() + "\n" + (issue.getBody() != null && !issue.getBody()
                                                                           .isEmpty() ? '\n' + issue
                .getBody() : "");
        this.issue = issue;
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
                ", issue=" + issue +
                ", id=" + id +
                ", note='" + note + '\'' +
                ", requiresLoadingFromIssue=" + requiresLoadingFromIssue +
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
        dest.writeParcelable(this.issue, flags);
        dest.writeInt(this.id);
        dest.writeString(this.note);
        dest.writeByte(this.requiresLoadingFromIssue ? (byte) 1 : (byte) 0);
        dest.writeLong(this.updatedAt);
        dest.writeLong(this.createdAt);
    }

    protected Card(Parcel in) {
        this.columnUrl = in.readString();
        this.contentUrl = in.readString();
        this.issueId = in.readInt();
        this.issue = in.readParcelable(Issue.class.getClassLoader());
        this.id = in.readInt();
        this.note = in.readString();
        this.requiresLoadingFromIssue = in.readByte() != 0;
        this.updatedAt = in.readLong();
        this.createdAt = in.readLong();
    }

    public static final Creator<Card> CREATOR = new Creator<Card>() {
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
