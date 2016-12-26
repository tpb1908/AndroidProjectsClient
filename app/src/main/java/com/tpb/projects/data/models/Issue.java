package com.tpb.projects.data.models;

import android.util.Log;

import com.tpb.projects.util.Data;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Created by theo on 20/12/16.
 */

public class Issue extends DataModel {
    private static final String TAG = Issue.class.getSimpleName();

    private Issue() {
    }

    private int id;

    private static final String NUMBER = "number";
    private int number;

    private static final String STATE = "state";
    private String state;

    private static final String TITLE = "title";
    private String title;

    private static final String BODY = "body";
    private String body;

    private static final String CLOSED_AT = "closed_at";
    private long closedAt;
    private boolean closed;

    public int getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }

    public String getState() {
        return state;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public boolean isClosed() {
        return closed;
    }

    public static Issue parse(JSONObject obj) {
        final Issue i = new Issue();
        try {
            i.id = obj.getInt(ID);
            i.number = obj.getInt(NUMBER);
            i.state = obj.getString(STATE);
            i.title = obj.getString(TITLE);
            i.body = obj.getString(BODY);
            if(obj.has(CLOSED_AT)) {
                try {
                    i.closedAt = Data.toCalendar(obj.getString(CLOSED_AT)).getTimeInMillis() / 1000;
                } catch(ParseException pe) {
                    Log.e(TAG, "parse: ", pe);
                }
                i.closed = true;
            }
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return i;
    }

    public static JSONObject parse(Issue issue) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(ID, issue.id);
            obj.put(NUMBER, issue.number);
            obj.put(STATE, issue.state);
            obj.put(TITLE, issue.title);
            obj.put(BODY, issue.body);
            if(issue.closedAt != 0) obj.put(CLOSED_AT, issue.closedAt);
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return obj;
    }

    //TODO Labels

}
