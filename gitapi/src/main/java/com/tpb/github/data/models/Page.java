package com.tpb.github.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by theo on 04/04/17.
 */

public class Page extends DataModel implements Parcelable {

    private static final String STATUS = "status";
    private String status;

    private static final String CNAME = "cname";
    private String cname;

    private static final String CUSTOM_404 = "custom_404";
    private boolean custom404;

    private String url;
    private static final String HTML_URL = "html_url";
    private String htmlUrl;

    public Page(JSONObject obj) {
        try {
            status = obj.getString(STATUS);
            cname = obj.getString(CNAME);
            custom404 = obj.getBoolean(CUSTOM_404);
            url = obj.getString(URL);
            htmlUrl = obj.getString(HTML_URL);
        } catch(JSONException ignored) {
        }
    }

    @Override
    public long getCreatedAt() {
        return 0;
    }

    @Override
    public String toString() {
        return "Page{" +
                "status='" + status + '\'' +
                ", cname='" + cname + '\'' +
                ", custom404=" + custom404 +
                ", url='" + url + '\'' +
                ", htmlUrl='" + htmlUrl + '\'' +
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.status);
        dest.writeString(this.cname);
        dest.writeByte(this.custom404 ? (byte) 1 : (byte) 0);
        dest.writeString(this.url);
        dest.writeString(this.htmlUrl);
    }

    protected Page(Parcel in) {
        this.status = in.readString();
        this.cname = in.readString();
        this.custom404 = in.readByte() != 0;
        this.url = in.readString();
        this.htmlUrl = in.readString();
    }

    public static final Parcelable.Creator<Page> CREATOR = new Parcelable.Creator<Page>() {
        @Override
        public Page createFromParcel(Parcel source) {
            return new Page(source);
        }

        @Override
        public Page[] newArray(int size) {
            return new Page[size];
        }
    };
}
