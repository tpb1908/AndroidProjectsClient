package com.tpb.projects.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by theo on 29/03/17.
 */

public class DiffFile extends DataModel implements Parcelable {

    private static final String FILE_NAME = "filename";
    private String fileName;
    private static final String ADDITIONS = "additions";
    private int additions;
    private static final String DELETIONS = "deletions";
    private int deletions;
    private static final String STATUS = "status";
    private String status;
    private static final String RAW_URL = "raw_url";
    private String rawUrl;
    private static final String BLOB_URL = "blob_url";
    private String blobUrl;
    private static final String PATCH = "patch";
    private String patch;

    public DiffFile(JSONObject obj) {
        try {
            fileName = obj.getString(FILE_NAME);
            additions = obj.getInt(ADDITIONS);
            deletions = obj.getInt(DELETIONS);
            status = obj.getString(STATUS);
            rawUrl = obj.getString(RAW_URL);
            blobUrl = obj.getString(BLOB_URL);
            patch = obj.getString(PATCH);
        } catch(JSONException jse) {}
    }

    @Override
    public long getCreatedAt() {
        return 0;
    }

    public String getFileName() {
        return fileName;
    }

    public int getAdditions() {
        return additions;
    }

    public int getDeletions() {
        return deletions;
    }

    public String getStatus() {
        return status;
    }

    public String getRawUrl() {
        return rawUrl;
    }

    public String getBlobUrl() {
        return blobUrl;
    }

    public String getPatch() {
        return patch;
    }

    @Override
    public String toString() {
        return "DiffFile{" +
                "fileName='" + fileName + '\'' +
                ", additions=" + additions +
                ", deletions=" + deletions +
                ", status='" + status + '\'' +
                ", rawUrl='" + rawUrl + '\'' +
                ", blobUrl='" + blobUrl + '\'' +
                ", patch='" + patch + '\'' +
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.fileName);
        dest.writeInt(this.additions);
        dest.writeInt(this.deletions);
        dest.writeString(this.status);
        dest.writeString(this.rawUrl);
        dest.writeString(this.blobUrl);
        dest.writeString(this.patch);
    }

    protected DiffFile(Parcel in) {
        this.fileName = in.readString();
        this.additions = in.readInt();
        this.deletions = in.readInt();
        this.status = in.readString();
        this.rawUrl = in.readString();
        this.blobUrl = in.readString();
        this.patch = in.readString();
    }

    public static final Parcelable.Creator<DiffFile> CREATOR = new Parcelable.Creator<DiffFile>() {
        @Override
        public DiffFile createFromParcel(Parcel source) {
            return new DiffFile(source);
        }

        @Override
        public DiffFile[] newArray(int size) {
            return new DiffFile[size];
        }
    };
}
