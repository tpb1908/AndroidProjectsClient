package com.tpb.projects.data.models;

import android.util.Log;

import com.tpb.projects.util.Constants;
import com.tpb.projects.util.Data;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Created by theo on 15/12/16.
 */

public class Card extends DataModel {
    private static final String TAG = Card.class.getSimpleName();

    private Card() {}

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
        this.note = note;
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

    public static Card parse(JSONObject object) {
        final Card c = new Card();
        try {
            c.id = object.getInt(ID);
            c.columnUrl = object.getString(COLUMN_URL);
            if(object.has(CONTENT_URL) ){
                c.contentUrl = object.getString(CONTENT_URL);
                c.issueId = Integer.parseInt(c.contentUrl.substring(c.contentUrl.lastIndexOf('/') + 1));
            }
            c.note = object.getString(NOTE);
            if(Constants.JSON_NULL.equals(c.note)) {
                c.note = "";
                c.requiresLoadingFromIssue = true;
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
}
