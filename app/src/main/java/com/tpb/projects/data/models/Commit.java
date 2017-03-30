package com.tpb.projects.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.tpb.projects.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Arrays;

/**
 * Created by theo on 29/03/17.
 */

public class Commit extends DataModel implements Parcelable {

    private String url;
    private static final String SHA = "sha";
    private String sha;
    private static final String HTML_URL = "html_url";
    private String htmlUrl;

    private static final String COMMIT = "commit";
    private static final String MESSAGE = "message";
    private String message;

    private static final String DATE = "date";
    private static final String COMMENT_COUNT = "comment_count";
    private int commentCount;

    private static final String AUTHOR = "author";
    private User author;
    private static final String COMMITTER = "committer";
    private User committer;

    private static final String STATS = "stats";
    private static final String ADDITIONS = "additions";
    private int additions;
    private static final String DELETIONS = "deletions";
    private int deletions;

    private static final String PARENTS = "parents";
    private String[] parents;

    private static final String FILES = "files";
    private DiffFile[] files;

    public Commit(JSONObject obj) {
        try {
            url = obj.getString(URL);
            sha = obj.getString(SHA);
            htmlUrl = obj.getString(HTML_URL);
            final JSONObject commit = obj.getJSONObject(COMMIT);
            message = commit.getString(MESSAGE);
            try {
                createdAt = Util.toCalendar(commit.getJSONObject(AUTHOR).getString(DATE)).getTimeInMillis();
            } catch(ParseException pe) {

            }
            commentCount = commit.getInt(COMMENT_COUNT);
            author = User.parse(obj.getJSONObject(AUTHOR));
            committer = User.parse(obj.getJSONObject(COMMITTER));

            if(obj.has(PARENTS)) {
                final JSONArray ps = obj.getJSONArray(PARENTS);
                parents = new String[ps.length()];
                for(int i = 0; i < ps.length(); i++) {
                    parents[i] = ps.getJSONObject(i).getString(SHA);
                }
            }

            if(obj.has(STATS)) {
                additions = obj.getJSONObject(STATS).getInt(ADDITIONS);
                deletions = obj.getJSONObject(STATS).getInt(DELETIONS);
            }

            if(obj.has(FILES)) {
                final JSONArray fs = obj.getJSONArray(FILES);
                files = new DiffFile[fs.length()];
                for(int i = 0; i < fs.length() ; i++) {
                    files[i] = new DiffFile(fs.getJSONObject(i));
                }
            }

        } catch(JSONException jse) {}
    }

    @Override
    public long getCreatedAt() {
        return createdAt;
    }

    public String getUrl() {
        return url;
    }

    public String getSha() {
        return sha;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public String getMessage() {
        return message;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public User getAuthor() {
        return author;
    }

    public User getCommitter() {
        return committer;
    }

    public int getAdditions() {
        return additions;
    }

    public int getDeletions() {
        return deletions;
    }

    public String[] getParents() {
        return parents;
    }

    public DiffFile[] getFiles() {
        return files;
    }

    public String getFullRepoName() {
        final int repoStart = url.indexOf("repos") + "repos/".length();
        return url.substring(repoStart, url.indexOf('/', url.indexOf('/', repoStart)+1));
    }

    @Override
    public String toString() {
        return "Commit{" +
                "url='" + url + '\'' +
                ", sha='" + sha + '\'' +
                ", htmlUrl='" + htmlUrl + '\'' +
                ", message='" + message + '\'' +
                ", commentCount=" + commentCount +
                ", author=" + author +
                ", committer=" + committer +
                ", additions=" + additions +
                ", deletions=" + deletions +
                ", parents=" + Arrays.toString(parents) +
                ", files=" + Arrays.toString(files) +
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.url);
        dest.writeString(this.sha);
        dest.writeString(this.htmlUrl);
        dest.writeString(this.message);
        dest.writeInt(this.commentCount);
        dest.writeParcelable(this.author, flags);
        dest.writeParcelable(this.committer, flags);
        dest.writeInt(this.additions);
        dest.writeInt(this.deletions);
        dest.writeStringArray(this.parents);
        dest.writeTypedArray(this.files, flags);
        dest.writeLong(this.createdAt);
    }

    protected Commit(Parcel in) {
        this.url = in.readString();
        this.sha = in.readString();
        this.htmlUrl = in.readString();
        this.message = in.readString();
        this.commentCount = in.readInt();
        this.author = in.readParcelable(User.class.getClassLoader());
        this.committer = in.readParcelable(User.class.getClassLoader());
        this.additions = in.readInt();
        this.deletions = in.readInt();
        this.parents = in.createStringArray();
        this.files = in.createTypedArray(DiffFile.CREATOR);
        this.createdAt = in.readLong();
    }

    public static final Parcelable.Creator<Commit> CREATOR = new Parcelable.Creator<Commit>() {
        @Override
        public Commit createFromParcel(Parcel source) {
            return new Commit(source);
        }

        @Override
        public Commit[] newArray(int size) {
            return new Commit[size];
        }
    };
}
