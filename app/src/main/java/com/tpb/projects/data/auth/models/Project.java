package com.tpb.projects.data.auth.models;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by theo on 15/12/16.
 */

public class Project extends DataModel {
    private static final String TAG = Project.class.getSimpleName();

    private Project() {}

    private static final String OWNER_URL = "owner_url";
    private String ownerUrl;

    private String url;

    private String name;

    private static final String BODY = "body";
    private String body;

    private static final String NUMBER = "number";
    private int number;

    private static final String CREATOR = "creator";
    private int creator;

    private long createdAt;

    private long updatedAt;

    public String getOwnerUrl() {
        return ownerUrl;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public String getBody() {
        return body;
    }

    public int getNumber() {
        return number;
    }

    public int getCreator() {
        return creator;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public static Project parse(JSONObject object) {
        final Project p = new Project();
        try {
            p.creator = object.getInt(CREATOR);
            p.number = object.getInt(NUMBER);
            p.body = object.getString(BODY);
            p.name = object.getString(NAME);
            p.url = object.getString(URL);
            p.ownerUrl = object.getString(OWNER_URL);
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return p;
    }

}
