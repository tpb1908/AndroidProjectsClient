/*
 * Copyright  2016 Theo Pearson-Bray
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.tpb.projects.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by theo on 08/01/17.
 */

public class MergedEvent extends DataModel implements Parcelable {

    private ArrayList<Event> events;

    public MergedEvent(ArrayList<Event> events) {
        this.events = events;
    }

    public MergedEvent(Event[] events) {
        this.events = new ArrayList<>(Arrays.asList(events));
    }

    public ArrayList<Event> getEvents() {
        return events;
    }

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

    protected MergedEvent(Parcel in) {
        this.events = in.createTypedArrayList(Event.CREATOR);
    }

    public static final Parcelable.Creator<MergedEvent> CREATOR = new Parcelable.Creator<MergedEvent>() {
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
