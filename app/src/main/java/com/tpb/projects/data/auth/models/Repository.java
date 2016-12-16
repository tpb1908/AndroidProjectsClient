package com.tpb.projects.data.auth.models;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by theo on 15/12/16.
 */

public class Repository extends DataModel {
    private static final String TAG = Repository.class.getSimpleName();

    private Repository() {}

    private int id;

    private String name;

    private static final String FULL_NAME = "full_name";
    private String fullName;

    private static final String DESCRIPTION = "description";
    private String description;

    private static final String PRIVATE = "private";
    private boolean isPrivate;

    private static final String FORK = "fork";
    private boolean isFork;

    private String url;

    private static final String HTML_URL = "html_url";
    private String htmlUrl;

    private static final String LANGUAGE = "language";
    private String language;

    private static final String ISSUES = "has_issues";
    private boolean hasIssues;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public boolean isFork() {
        return isFork;
    }

    public String getUrl() {
        return url;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isHasIssues() {
        return hasIssues;
    }

    public static Repository parse(JSONObject object) {
        final Repository r = new Repository();
        try {
            r.id = object.getInt(ID);
            r.name = object.getString(NAME);
            r.fullName = object.getString(FULL_NAME);
            r.description = object.getString(DESCRIPTION);
            r.isPrivate = object.getBoolean(PRIVATE);
            r.isFork = object.getBoolean(FORK);
            r.url = object.getString(URL);
            r.htmlUrl = object.getString(HTML_URL);
            r.language = object.getString(LANGUAGE);
            r.hasIssues = object.getBoolean(ISSUES);
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }

        return r;
    }

    @Override
    public String toString() {
        return "Repository{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", fullName='" + fullName + '\'' +
                ", description='" + description + '\'' +
                ", isPrivate=" + isPrivate +
                ", isFork=" + isFork +
                ", url='" + url + '\'' +
                ", htmlUrl='" + htmlUrl + '\'' +
                ", language='" + language + '\'' +
                ", hasIssues=" + hasIssues +
                '}';
    }
}
