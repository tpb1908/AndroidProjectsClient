/*
 * Copyright  2016 Theo Pearson-Bray
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.tpb.projects.data.models;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.tpb.projects.util.Constants;
import com.tpb.projects.util.Data;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Created by theo on 07/01/17.
 */

public class Event extends DataModel implements Parcelable {
    private static final String TAG = Event.class.getSimpleName();

    private int id;

    private static final String ACTOR = "actor";
    private User actor;

    private static final String EVENT = "event";
    private GITEvent event;

    private static final String COMMIT_ID = "commit_id";
    private String commitId;

    private static final String COMMIT_URL = "commit_url";
    private String commitUrl;

    private static final String LABEL = "label";
    private static final String NAME = "name";
    private String labelName;
    private static final String COLOR = "color";
    private int labelColor;

    private static final String ASSIGNEE = "assignee";
    private User assignee;
    private static final String ASSIGNER = "assigner";
    private User assigner;

    private static final String REVIEW_REQUESTER = "review_requester";
    private User reviewRequester;
    private static final String REQUESTED_REVIEWER = "requested_reviewer";
    private User requestedReviewer;

    private static final String RENAME = "rename";
    private static final String RENAME_FROM = "from";
    private static final String RENAME_TO = "to";
    private String renameFrom;
    private String renameTo;

    public int getId() {
        return id;
    }

    public User getActor() {
        return actor;
    }

    public GITEvent getEvent() {
        return event;
    }

    public String getCommitId() {
        return commitId;
    }

    public String getCommitUrl() {
        return commitUrl;
    }

    @Override
    public long getCreatedAt() {
        return createdAt;
    }

    public String getLabelName() {
        return labelName;
    }

    public int getLabelColor() {
        return labelColor;
    }

    public User getAssignee() {
        return assignee;
    }

    public User getAssigner() {
        return assigner;
    }

    public User getReviewRequester() {
        return reviewRequester;
    }

    public User getRequestedReviewer() {
        return requestedReviewer;
    }

    public String getRenameFrom() {
        return renameFrom;
    }

    public String getRenameTo() {
        return renameTo;
    }

    public static Event parse(JSONObject obj) {
        final Event e = new Event();
        try {
            e.id = obj.getInt(ID);
            e.actor = User.parse(obj.getJSONObject(ACTOR));
            if(obj.has(EVENT)) e.event = GITEvent.valueOf(obj.getString(EVENT).toUpperCase());
            if(e.event == GITEvent.ADDED_TO_PROJECT || e.event == GITEvent.REMOVED_FROM_PROJECT) {
                Log.i(TAG, "parse: " + obj.toString());
            }

            try {
                e.createdAt = Data.toCalendar(obj.getString(CREATED_AT)).getTimeInMillis();
            } catch(ParseException pe) {
                Log.e(TAG, "parse: ", pe);
            }
            if(obj.has(COMMIT_ID)) {
                e.commitId = obj.getString(COMMIT_ID);
            }
            if(obj.has(COMMIT_URL)) {
                e.commitUrl = obj.getString(COMMIT_URL);
            }

            if(obj.has(LABEL) && !Constants.JSON_NULL.equals(obj.getString(LABEL))) {
                e.labelName = obj.getJSONObject(LABEL).getString(NAME);
                e.labelColor = Color.parseColor("#" + obj.getJSONObject(LABEL).getString(COLOR));
            }
            if(obj.has(ASSIGNEE)) {
                e.assignee = User.parse(obj.getJSONObject(ASSIGNEE));
            }
            if(obj.has(ASSIGNER)) {
                e.assigner = User.parse(obj.getJSONObject(ASSIGNER));
            }
            if(obj.has(REVIEW_REQUESTER)) {
                e.reviewRequester = User.parse(obj.getJSONObject(REVIEW_REQUESTER));
            }
            if(obj.has(REQUESTED_REVIEWER)) {
                e.requestedReviewer = User.parse(obj.getJSONObject(REQUESTED_REVIEWER));
            }
            if(obj.has(RENAME)) {
                e.renameFrom = obj.getJSONObject(RENAME).getString(RENAME_FROM);
                e.renameTo = obj.getJSONObject(RENAME).getString(RENAME_TO);
            }
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
            Log.i(TAG, "parse: " + obj.toString());
        }
        return e;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", actor=" + actor +
                ", event=" + event +
                ", commitId='" + commitId + '\'' +
                ", commitUrl='" + commitUrl + '\'' +
                ", labelName='" + labelName + '\'' +
                ", labelColor=" + labelColor +
                ", assignee=" + assignee +
                ", assigner=" + assigner +
                ", reviewRequester=" + reviewRequester +
                ", requestedReviewer=" + requestedReviewer +
                ", renameFrom='" + renameFrom + '\'' +
                ", renameTo='" + renameTo + '\'' +
                '}';
    }

    public enum GITEvent {
        CLOSED("closed"),
        REOPENED("reopened"),
        SUBSCRIBED("subscribed"),
        MERGED("merged"),
        REFERENCED("referenced"),
        MENTIONED("mentioned"),
        ASSIGNED("assigned"),
        UNASSIGNED("unassigned"),
        LABELED("labeled"),
        UNLABELED("unlabeled"),
        MILESTONED("milestoned"),
        DEMILESTONED("demilestoned"),
        RENAMED("renamed"),
        LOCKED("locked"),
        UNLOCKED("unlocked"),
        HEAD_REF_DELETED("head_ref_deleted"),
        HEAD_REF_RESTORED("head_ref_restored"),
        REVIEW_DISMISSED("review_dismissed"),
        REVIEW_REQUESTED("review_requested"),
        REVIEW_REQUEST_REMOVED("review_request_removed"),
        ADDED_TO_PROJECT("added_to_project"),
        REMOVED_FROM_PROJECT("removed_from_project");


        GITEvent(String key) {
        }

    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeParcelable(this.actor, flags);
        dest.writeInt(this.event == null ? -1 : this.event.ordinal());
        dest.writeString(this.commitId);
        dest.writeString(this.commitUrl);
        dest.writeString(this.labelName);
        dest.writeInt(this.labelColor);
        dest.writeParcelable(this.assignee, flags);
        dest.writeParcelable(this.assigner, flags);
        dest.writeParcelable(this.reviewRequester, flags);
        dest.writeParcelable(this.requestedReviewer, flags);
        dest.writeString(this.renameFrom);
        dest.writeString(this.renameTo);
        dest.writeLong(this.createdAt);
    }

    public Event() {
    }

    private Event(Parcel in) {
        this.id = in.readInt();
        this.actor = in.readParcelable(User.class.getClassLoader());
        int tmpEvent = in.readInt();
        this.event = tmpEvent == -1 ? null : GITEvent.values()[tmpEvent];
        this.commitId = in.readString();
        this.commitUrl = in.readString();
        this.labelName = in.readString();
        this.labelColor = in.readInt();
        this.assignee = in.readParcelable(User.class.getClassLoader());
        this.assigner = in.readParcelable(User.class.getClassLoader());
        this.reviewRequester = in.readParcelable(User.class.getClassLoader());
        this.requestedReviewer = in.readParcelable(User.class.getClassLoader());
        this.renameFrom = in.readString();
        this.renameTo = in.readString();
        this.createdAt = in.readLong();
    }

    public static final Creator<Event> CREATOR = new Creator<Event>() {
        @Override
        public Event createFromParcel(Parcel source) {
            return new Event(source);
        }

        @Override
        public Event[] newArray(int size) {
            return new Event[size];
        }
    };
}
