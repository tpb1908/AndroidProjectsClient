package com.tpb.github.data.models;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by theo on 25/12/16.
 */

public class Label extends DataModel implements Parcelable {
    private static final String TAG = Label.class.getSimpleName();

    public Label(JSONObject obj) {
        try {
            id = obj.getInt(ID);
            url = obj.getString(URL);
            name = obj.getString(NAME);
            color = Color.parseColor("#" + obj.getString(COLOR));
            isDefault = obj.getBoolean(DEFAULT);
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
    }

    private int id;

    private String url;

    private String name;

    private static final String COLOR = "color";
    private int color;

    private static final String DEFAULT = "default";
    private boolean isDefault;

    public int getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public long getCreatedAt() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Label && ((Label) obj).id == id;
    }

    @Override
    public String toString() {
        return "Label{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", color=" + color +
                ", isDefault=" + isDefault +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.url);
        dest.writeString(this.name);
        dest.writeInt(this.color);
        dest.writeByte(this.isDefault ? (byte) 1 : (byte) 0);
    }

    private Label(Parcel in) {
        this.id = in.readInt();
        this.url = in.readString();
        this.name = in.readString();
        this.color = in.readInt();
        this.isDefault = in.readByte() != 0;
    }

    public static final Parcelable.Creator<Label> CREATOR = new Parcelable.Creator<Label>() {
        @Override
        public Label createFromParcel(Parcel source) {
            return new Label(source);
        }

        @Override
        public Label[] newArray(int size) {
            return new Label[size];
        }
    };
}
