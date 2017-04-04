package com.tpb.github.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.tpb.github.data.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Created by theo on 04/04/17.
 */

public class Notification extends DataModel implements Parcelable {

    private long id;

    private static final String SUBJECT = "subject";

    private static final String REASON = "reason";
    private GitNotificationReason reason;

    private static final String REPOSITORY = "repository";
    private Repository repository;

    private static final String UNREAD = "unread";
    private boolean unread;

    private static final String LAST_READ_AT = "last_read_at";
    private long lastReadAt;

    private static final String UPDATED_AT = "updated_at";
    private long updatedAt;

    private static final String TITLE = "title";
    private String title;

    private static final String TYPE = "type";
    private String type;

    private String url;

    public Notification(JSONObject obj) {
        try {
            id = obj.getLong(ID);
            reason = GitNotificationReason.fromString(obj.getString(REASON));
            if(obj.has(REPOSITORY)) repository = Repository.parse(obj.getJSONObject(REPOSITORY));
            unread = obj.getBoolean(UNREAD);
            try {
                lastReadAt = Util.toCalendar(obj.getString(LAST_READ_AT)).getTimeInMillis();
                updatedAt = Util.toCalendar(obj.getString(UPDATED_AT)).getTimeInMillis();
            } catch(ParseException ignored) {}

            final JSONObject info = obj.getJSONObject(SUBJECT);
            title = info.getString(TITLE);
            url = info.getString(URL).replace("api.", "").replace("/issues", "");
            type = info.getString(TYPE);

        } catch(JSONException ignored) {
        }
    }

    @Override
    public long getCreatedAt() {
        return updatedAt;
    }

    public long getId() {
        return id;
    }

    public GitNotificationReason getReason() {
        return reason;
    }

    public Repository getRepository() {
        return repository;
    }

    public boolean isUnread() {
        return unread;
    }

    public long getLastReadAt() {
        return lastReadAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", reason=" + reason +
                ", repository=" + repository +
                ", unread=" + unread +
                ", lastReadAt=" + lastReadAt +
                ", updatedAt=" + updatedAt +
                ", title='" + title + '\'' +
                ", type='" + type + '\'' +
                ", url='" + url + '\'' +
                '}';
    }



    public enum GitNotificationReason {
        ASSIGN,
        AUTHOR,
        COMMENT,
        INVITATION,
        MANUAL,
        MENTION,
        STATE_CHANGE,
        SUBSCRIBED,
        TEAM_MENTION,
        UNKNOWN;

        @Nullable String val;

        static GitNotificationReason fromString(@NonNull String val) {
            try {
                return GitNotificationReason.valueOf(val.toUpperCase());
            } catch(Exception e) {
                final GitNotificationReason reason = UNKNOWN;
                reason.val = val;
                return reason;
            }
        }

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeInt(this.reason == null ? -1 : this.reason.ordinal());
        dest.writeParcelable(this.repository, flags);
        dest.writeByte(this.unread ? (byte) 1 : (byte) 0);
        dest.writeLong(this.lastReadAt);
        dest.writeLong(this.updatedAt);
        dest.writeString(this.title);
        dest.writeString(this.type);
        dest.writeString(this.url);
        dest.writeLong(this.createdAt);
    }

    protected Notification(Parcel in) {
        this.id = in.readLong();
        int tmpReason = in.readInt();
        this.reason = tmpReason == -1 ? null : GitNotificationReason.values()[tmpReason];
        this.repository = in.readParcelable(Repository.class.getClassLoader());
        this.unread = in.readByte() != 0;
        this.lastReadAt = in.readLong();
        this.updatedAt = in.readLong();
        this.title = in.readString();
        this.type = in.readString();
        this.url = in.readString();
        this.createdAt = in.readLong();
    }

    public static final Parcelable.Creator<Notification> CREATOR = new Parcelable.Creator<Notification>() {
        @Override
        public Notification createFromParcel(Parcel source) {
            return new Notification(source);
        }

        @Override
        public Notification[] newArray(int size) {
            return new Notification[size];
        }
    };
}
