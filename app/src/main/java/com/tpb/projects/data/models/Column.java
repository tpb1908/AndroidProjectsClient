package com.tpb.projects.data.models;

import android.util.Log;

import com.tpb.projects.util.Data;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

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

    private long createdAt;

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

}
