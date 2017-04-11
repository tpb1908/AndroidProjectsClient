package com.tpb.github.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by theo on 11/03/17.
 */

public class GistFile extends DataModel implements Parcelable {

    private static final String FILE_NAME = "filename";
    private String name;

    private static final String SIZE = "size";
    private int size;

    private static final String RAW_URL = "raw_url";
    private String rawUrl;

    private static final String TYPE = "type";
    private String type;

    private static final String TRUNCATED = "truncated";
    private boolean truncated;

    private static final String LANGUAGE = "language";
    private String language;

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public String getType() {
        return type;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public String getLanguage() {
        return language;
    }

    public String getRawUrl() {
        return rawUrl;
    }

    @Override
    public long getCreatedAt() {
        return 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeInt(this.size);
        dest.writeString(this.rawUrl);
        dest.writeString(this.type);
        dest.writeByte(this.truncated ? (byte) 1 : (byte) 0);
        dest.writeString(this.language);
    }

    public GistFile(JSONObject obj) {
        try {
            name = obj.getString(FILE_NAME);
            size = obj.getInt(SIZE);
            rawUrl = obj.getString(RAW_URL);
            type = obj.getString(TYPE);
            truncated = obj.has(TRUNCATED) && obj.getBoolean(TRUNCATED);
            language = obj.getString(LANGUAGE);
        } catch(JSONException jse) {
            Log.e(GistFile.class.getSimpleName(), "parse: ", jse);
        }
    }

    protected GistFile(Parcel in) {
        this.name = in.readString();
        this.size = in.readInt();
        this.rawUrl = in.readString();
        this.type = in.readString();
        this.truncated = in.readByte() != 0;
        this.language = in.readString();
    }

    public static final Creator<GistFile> CREATOR = new Creator<GistFile>() {
        @Override
        public GistFile createFromParcel(Parcel source) {
            return new GistFile(source);
        }

        @Override
        public GistFile[] newArray(int size) {
            return new GistFile[size];
        }
    };

    @Override
    public String toString() {
        return "GistFile{" +
                "name='" + name + '\'' +
                ", size=" + size +
                ", rawUrl='" + rawUrl + '\'' +
                ", type='" + type + '\'' +
                ", truncated=" + truncated +
                ", language='" + language + '\'' +
                '}';
    }
}
