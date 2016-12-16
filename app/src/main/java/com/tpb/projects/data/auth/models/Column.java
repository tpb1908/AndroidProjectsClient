package com.tpb.projects.data.auth.models;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by theo on 15/12/16.
 */

public class Column extends DataModel {
    private static final String TAG = Column.class.getSimpleName();

    private Column() {}

    private int id;

    private String name;

    private static final String PROJECT_URL = "project_url";
    private String projectUrl;

    private static final String CREATED_AT = "created_at";
    private long createdAt;

    private static final String UPDATED_AT = "updated_at";
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

    public static Column parse(JSONObject object) {
        final Column c = new Column();
        try {
            c.id = object.getInt(ID);
            c.name = object.getString(NAME);
            c.projectUrl = object.getString(PROJECT_URL);
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return c;
    }

}
