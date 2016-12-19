package com.tpb.projects.data.models;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

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

    private int id;

    private static final String NOTE = "note";
    private String note;

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

    public static Card parse(JSONObject object) {
        final Card c = new Card();
        try {
            c.id = object.getInt(ID);
            c.columnUrl = object.getString(COLUMN_URL);
            c.contentUrl = object.getString(CONTENT_URL);
            c.note = object.getString(NOTE);
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return c;
    }

}
