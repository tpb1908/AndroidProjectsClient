package com.tpb.projects.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by theo on 11/03/17.
 */

public class File extends DataModel implements Parcelable {

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

    public static File parse(JSONObject obj) {
        final File f = new File();
        try {
            f.name = obj.getString(FILE_NAME);
            f.size = obj.getInt(SIZE);
            f.rawUrl = obj.getString(RAW_URL);
            f.type = obj.getString(TYPE);
            f.truncated = obj.has(TRUNCATED) && obj.getBoolean(TRUNCATED);
            f.language = obj.getString(LANGUAGE);
        } catch(JSONException jse) {
            Log.e(File.class.getSimpleName(), "parse: ", jse);
        }
        return f;
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

    public File() {
    }

    protected File(Parcel in) {
        this.name = in.readString();
        this.size = in.readInt();
        this.rawUrl = in.readString();
        this.type = in.readString();
        this.truncated = in.readByte() != 0;
        this.language = in.readString();
    }

    public static final Creator<File> CREATOR = new Creator<File>() {
        @Override
        public File createFromParcel(Parcel source) {
            return new File(source);
        }

        @Override
        public File[] newArray(int size) {
            return new File[size];
        }
    };

    @Override
    public String toString() {
        return "File{" +
                "name='" + name + '\'' +
                ", size=" + size +
                ", rawUrl='" + rawUrl + '\'' +
                ", type='" + type + '\'' +
                ", truncated=" + truncated +
                ", language='" + language + '\'' +
                '}';
    }
}
