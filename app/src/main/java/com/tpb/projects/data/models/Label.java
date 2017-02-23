package com.tpb.projects.data.models;

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

    private Label() {

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

    public static Label parse(JSONObject obj) {
        final Label l = new Label();
        try {
            l.id = obj.getInt(ID);
            l.url = obj.getString(URL);
            l.name = obj.getString(NAME);
            l.color = Color.parseColor("#" + obj.getString(COLOR));
            l.isDefault = obj.getBoolean(DEFAULT);
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return l;
    }

    @Override
    public long getCreatedAt() {
        return 0;
    }

    public static JSONObject parse(Label label) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(ID, label.id);
            obj.put(URL, label.url);
            obj.put(NAME, label.name);
            obj.put(COLOR, label.color);
            obj.put(DEFAULT, label.isDefault);
        } catch(JSONException jse) {
            Log.e(TAG, "parse: ", jse);
        }
        return obj;
    }

    public static void appendLabels(StringBuilder builder, Label[] labels, String spacer) {
        for(Label label : labels) {
            builder.append("<font color=\"");
            builder.append(String.format("#%06X", (0xFFFFFF & label.getColor())));
            builder.append("\">");
            builder.append(label.getName());
            builder.append("</font>");
            builder.append(spacer);
        }
        builder.setLength(Math.max(0, builder.length() - spacer.length())); //Strip the last spacer
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
