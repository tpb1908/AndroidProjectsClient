package com.tpb.github.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by theo on 08/01/17.
 */

public class MergedModel<T extends DataModel> extends DataModel implements Parcelable {

    private List<T> data = new ArrayList<>();

    public MergedModel(ArrayList<T> data) {
        this.data.addAll(data);
    }

    public MergedModel(List<T> data) {
        this.data.addAll(data);
    }

    public List<T> getData() {
        return data;
    }

    @Override
    public long getCreatedAt() {
        return data.size() > 0 ? data.get(0).createdAt : 0;
    }

    @Override
    public String toString() {
        return "MergedModel{" +
                "data=" + data +
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(data.getClass().getName());
        dest.writeList(this.data);
    }

    protected MergedModel(Parcel in) {
        final String c = in.readString();
        this.data = new ArrayList<>();
        try {
            in.readList(this.data, Class.forName(c).getClassLoader());
        } catch(ClassNotFoundException cnfe) {
            Log.e(MergedModel.class.getSimpleName(), "Error finding class", cnfe);
        }
    }

    public static final Creator<MergedModel> CREATOR = new Creator<MergedModel>() {
        @Override
        public MergedModel createFromParcel(Parcel source) {
            return new MergedModel(source);
        }

        @Override
        public MergedModel[] newArray(int size) {
            return new MergedModel[size];
        }
    };
}
