package com.tpb.github.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by theo on 15/04/17.
 */

public class Reaction implements Parcelable {

    private static final String PLUS = "+1";
    int plus;
    private static final String MINUS = "-1";
    int minus;
    private static final String LAUGH = "laugh";
    int laugh;
    private static final String HOORAY = "hooray";
    int hooray;
    private static final String CONFUSED = "confused";
    int confused;
    private static final String HEART = "heart";
    int heart;

    public Reaction(JSONObject obj) {
        try {
            plus = obj.getInt(PLUS);
            minus = obj.getInt(MINUS);
            laugh = obj.getInt(LAUGH);
            hooray = obj.getInt(HOORAY);
            confused = obj.getInt(CONFUSED);
            heart = obj.getInt(HEART);
        } catch(JSONException jse) {

        }
    }

    @Override
    public String toString() {
        return "Reaction{" +
                "plus=" + plus +
                ", minus=" + minus +
                ", laugh=" + laugh +
                ", hooray=" + hooray +
                ", confused=" + confused +
                ", heart=" + heart +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.plus);
        dest.writeInt(this.minus);
        dest.writeInt(this.laugh);
        dest.writeInt(this.hooray);
        dest.writeInt(this.confused);
        dest.writeInt(this.heart);
    }

    protected Reaction(Parcel in) {
        this.plus = in.readInt();
        this.minus = in.readInt();
        this.laugh = in.readInt();
        this.hooray = in.readInt();
        this.confused = in.readInt();
        this.heart = in.readInt();
    }

    public static final Parcelable.Creator<Reaction> CREATOR = new Parcelable.Creator<Reaction>() {
        @Override
        public Reaction createFromParcel(Parcel source) {
            return new Reaction(source);
        }

        @Override
        public Reaction[] newArray(int size) {
            return new Reaction[size];
        }
    };
}
