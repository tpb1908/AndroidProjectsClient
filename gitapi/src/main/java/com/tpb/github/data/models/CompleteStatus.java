package com.tpb.github.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by theo on 02/04/17.
 */

public class CompleteStatus extends DataModel implements Parcelable {

    private static final String STATE = "state";
    private String state;

    private static final String SHA = "sha";
    private String sha;

    private static final String TOTAL_COUNT = "total_count";
    private int totalCount;

    private static final String STATUSES = "statuses";
    private List<Status> statuses;

    private static final String REPOSITORY = "repository";
    private Repository repo;

    public CompleteStatus(JSONObject obj) {
        try {
            state = obj.getString(STATE);
            sha = obj.getString(SHA);
            totalCount = obj.getInt(TOTAL_COUNT);
            repo = Repository.parse(obj.getJSONObject(REPOSITORY));
            statuses = new ArrayList<>();
            final JSONArray array = obj.getJSONArray(STATUSES);
            try {
                for(int i = 0; i < array.length(); i++) {
                    statuses.add(new Status(array.getJSONObject(i)));
                }
            } catch(JSONException ignored) {
            }
        } catch(JSONException jse) {
        }
    }

    @Override
    public long getCreatedAt() {
        return createdAt;
    }

    public String getState() {
        return state;
    }

    public String getSha() {
        return sha;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public List<Status> getStatuses() {
        return statuses;
    }

    public Repository getRepo() {
        return repo;
    }

    @Override
    public String toString() {
        return "CompleteStatus{" +
                "state='" + state + '\'' +
                ", sha='" + sha + '\'' +
                ", totalCount=" + totalCount +
                ", statuses=" + statuses +
                ", repo=" + repo +
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.state);
        dest.writeString(this.sha);
        dest.writeInt(this.totalCount);
        dest.writeTypedList(this.statuses);
        dest.writeParcelable(this.repo, flags);
        dest.writeLong(this.createdAt);
    }

    protected CompleteStatus(Parcel in) {
        this.state = in.readString();
        this.sha = in.readString();
        this.totalCount = in.readInt();
        this.statuses = in.createTypedArrayList(Status.CREATOR);
        this.repo = in.readParcelable(Repository.class.getClassLoader());
        this.createdAt = in.readLong();
    }

    public static final Parcelable.Creator<CompleteStatus> CREATOR = new Parcelable.Creator<CompleteStatus>() {
        @Override
        public CompleteStatus createFromParcel(Parcel source) {
            return new CompleteStatus(source);
        }

        @Override
        public CompleteStatus[] newArray(int size) {
            return new CompleteStatus[size];
        }
    };
}
