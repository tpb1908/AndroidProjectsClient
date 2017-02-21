package com.tpb.projects.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by theo on 08/01/17.
 */

public class MergedEvent extends DataModel implements Parcelable {

    private final ArrayList<Event> events;

    public MergedEvent(ArrayList<Event> events) {
        this.events = events;
    }

    public MergedEvent(Event[] events) {
        this.events = new ArrayList<>(Arrays.asList(events));
    }

    public ArrayList<Event> getEvents() {
        return events;
    }

    @Override
    public long getCreatedAt() {
        return events.get(0).getCreatedAt();
    }

    public Event.GITEvent getEvent() {
        return events.get(0).getEvent();
    }

    @Override
    public String toString() {
        return "MergedEvent{" +
                "events=" + events +
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(this.events);
    }

    private MergedEvent(Parcel in) {
        this.events = in.createTypedArrayList(Event.CREATOR);
    }

    public static final Creator<MergedEvent> CREATOR = new Creator<MergedEvent>() {
        @Override
        public MergedEvent createFromParcel(Parcel source) {
            return new MergedEvent(source);
        }

        @Override
        public MergedEvent[] newArray(int size) {
            return new MergedEvent[size];
        }
    };
}
