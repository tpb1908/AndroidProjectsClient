package com.tpb.projects.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.tpb.projects.data.File;
import com.tpb.projects.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by theo on 11/03/17.
 */

public class Gist extends DataModel implements Parcelable {

    private String id;

    private static final String URL = "url";
    private String url;

    private static final String HTML_URL = "html_url";
    private String htmlUrl;

    private static final String COMMITS_URL = "commits_url";
    private String commitsUrl;

    private static final String FORKS_URL = "forks_url";
    private String forksUrl;

    private static final String DESCRIPTION = "description";
    private String description;

    private static final String PUBLIC = "public";
    private boolean isPublic;

    private static final String OWNER = "owner";
    private User owner;

    private static final String USER = "user";
    private User user;

    private static final String FILES = "files";
    private File[] files;

    private static final String TRUNCATED = "truncated";
    private boolean isTruncated;

    private static final String COMMENTS = "comments";
    private int comments;

    private static final String UPDATED_AT = "updated_at";
    private long updatedAt;


    public static Gist parse(JSONObject obj) {
        final Gist g = new Gist();
        try {
            g.url = obj.getString(URL);
            g.id = obj.getString(ID);
            g.description = obj.getString(DESCRIPTION);
            g.isPublic = obj.getBoolean(PUBLIC);
            g.owner = User.parse(obj.getJSONObject(OWNER));
            if(obj.has(USER) && !JSON_NULL.equals(obj.getString(USER))) g.user = User.parse(obj.getJSONObject(USER));
            g.htmlUrl = obj.getString(HTML_URL);
            g.commitsUrl = obj.getString(COMMITS_URL);
            g.forksUrl = obj.getString(FORKS_URL);
            g.isTruncated = obj.getBoolean(TRUNCATED);
            g.comments = obj.getInt(COMMENTS);
            try {
                g.createdAt = Util.toCalendar(obj.getString(CREATED_AT)).getTimeInMillis();
                g.updatedAt = Util.toCalendar(obj.getString(UPDATED_AT)).getTimeInMillis();
            } catch(ParseException pe) {
                Log.e(Gist.class.getSimpleName(), "parse: ", pe);
            }
            final JSONObject filesObj = obj.getJSONObject(FILES);
            final Iterator<String> keys = filesObj.keys();
            final List<File> files = new ArrayList<>();
            while(keys.hasNext()) {
                files.add(File.parse(filesObj.getJSONObject(keys.next())));
            }
            g.files = files.toArray(new File[0]);
        } catch(JSONException jse) {
            Log.e(Gist.class.getSimpleName(), "parse: ", jse);
        }
        return g;
    }

    @Override
    public long getCreatedAt() {
        return createdAt;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public String getCommitsUrl() {
        return commitsUrl;
    }

    public String getForksUrl() {
        return forksUrl;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public User getOwner() {
        return owner;
    }

    public User getUser() {
        return user;
    }

    public File[] getFiles() {
        return files;
    }

    public boolean isTruncated() {
        return isTruncated;
    }

    public int getComments() {
        return comments;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "Gist{" +
                "id='" + id + '\'' +
                "\n, url='" + url + '\'' +
                "\n, htmlUrl='" + htmlUrl + '\'' +
                "\n, commitsUrl='" + commitsUrl + '\'' +
                "\n, forksUrl='" + forksUrl + '\'' +
                "\n, description='" + description + '\'' +
                "\n, isPublic=" + isPublic +
                "\n, owner=" + owner +
                "\n, user=" + user +
                "\n, files=" + Arrays.toString(files) +
                "\n, isTruncated=" + isTruncated +
                "\n, comments=" + comments +
                "\n, updatedAt=" + updatedAt +
                '}';
    }
    
    


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.url);
        dest.writeString(this.htmlUrl);
        dest.writeString(this.commitsUrl);
        dest.writeString(this.forksUrl);
        dest.writeString(this.description);
        dest.writeByte(this.isPublic ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.owner, flags);
        dest.writeParcelable(this.user, flags);
        dest.writeTypedArray(this.files, flags);
        dest.writeByte(this.isTruncated ? (byte) 1 : (byte) 0);
        dest.writeInt(this.comments);
        dest.writeLong(this.updatedAt);
        dest.writeLong(this.createdAt);
    }

    public Gist() {
    }

    protected Gist(Parcel in) {
        this.id = in.readString();
        this.url = in.readString();
        this.htmlUrl = in.readString();
        this.commitsUrl = in.readString();
        this.forksUrl = in.readString();
        this.description = in.readString();
        this.isPublic = in.readByte() != 0;
        this.owner = in.readParcelable(User.class.getClassLoader());
        this.user = in.readParcelable(User.class.getClassLoader());
        this.files = in.createTypedArray(File.CREATOR);
        this.isTruncated = in.readByte() != 0;
        this.comments = in.readInt();
        this.updatedAt = in.readLong();
        this.createdAt = in.readLong();
    }

    public static final Creator<Gist> CREATOR = new Creator<Gist>() {
        @Override
        public Gist createFromParcel(Parcel source) {
            return new Gist(source);
        }

        @Override
        public Gist[] newArray(int size) {
            return new Gist[size];
        }
    };
}
