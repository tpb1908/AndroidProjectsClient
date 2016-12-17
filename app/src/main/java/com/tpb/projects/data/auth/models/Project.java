package com.tpb.projects.data.auth.models;

import android.util.Log;

import com.tpb.projects.util.Data;

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
    private static final String LOGIN = "login";
    private String creatorUserName;

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

    public String getCreatorUserName() {
        return creatorUserName;
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
            p.creatorUserName = object.getJSONObject(CREATOR).getString(LOGIN);
            p.number = object.getInt(NUMBER);
            p.body = object.getString(BODY);
            p.name = object.getString(NAME);
            p.url = object.getString(URL);
            p.ownerUrl = object.getString(OWNER_URL);
            p.createdAt = Data.toCalendar(object.getString(CREATED_AT)).getTimeInMillis() / 1000;
            p.updatedAt = Data.toCalendar(object.getString(UPDATED_AT)).getTimeInMillis() / 1000;
        } catch(Exception jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return p;
    }

    @Override
    public String toString() {
        return "Project{" +
                "ownerUrl='" + ownerUrl + '\'' +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", body='" + body + '\'' +
                ", number=" + number +
                ", creatorUserName=" + creatorUserName +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
