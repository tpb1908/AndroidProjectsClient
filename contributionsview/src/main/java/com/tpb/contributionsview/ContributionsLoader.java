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

package com.tpb.contributionsview;

import android.content.Context;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Created by theo on 12/01/17.
 */

public class ContributionsLoader {
    private static final String TAG = ContributionsLoader.class.getSimpleName();

    // Format string for svg path
    private static final String IMAGE_BASE = "https://github.com/users/%s/contributions";

    private final WeakReference<ContributionsRequestListener> mListener;

    ContributionsLoader(@NonNull ContributionsRequestListener listener) {
        mListener = new WeakReference<>(listener);
    }

    void beginRequest(Context context, String login) {
        final String URL = String.format(IMAGE_BASE, login);
        // Load the svg as a string
        final StringRequest req = new StringRequest(Request.Method.GET, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                parse(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(mListener.get() != null) mListener.get().onError(error);
            }
        });
        Volley.newRequestQueue(context).add(req);
    }

    private void parse(String response) {
        final ArrayList<GitDay> contribs = new ArrayList<>();
        int first = response.indexOf("<rect");
        int last;
        // Find each rectangle in the image
        while(first != -1) {
            last = response.indexOf("/>", first);
            contribs.add(new GitDay(response.substring(first, last)));
            first = response.indexOf("<rect", last);
        }
        if(mListener.get() != null) mListener.get().onResponse(contribs);

    }

    public static class GitDay implements Parcelable {
        private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        @ColorInt final int color;
        long date;
        public final int contributions;

        GitDay(String rect) {
            //rect is <rect class="day" width="10" height="10" x="" y="" fill="#FFFFF" data-count="n" data-date="yyyy-mm-dd"/>
            final int colorIndex = rect.indexOf("fill=\"") + 6;
            color = Color.parseColor(rect.substring(colorIndex, colorIndex + 7));

            final int countIndex = rect.indexOf("data-count=\"") + 12;
            final int countEndIndex = rect.indexOf("\"", countIndex);
            contributions = Integer.parseInt(rect.substring(countIndex, countEndIndex));

            final int dateIndex = rect.indexOf("data-date=\"") + 11;
            try {
                date = sdf.parse(rect.substring(dateIndex, dateIndex + 11)).getTime();
            } catch(ParseException ignored) {
            }
            //Log.i(TAG, "GitDay: Contributions" + color + ", " + date + ", " + contributions);
        }



        @Override
        public String toString() {
            return "GitDay{" +
                    "color=" + color +
                    ", date=" + date +
                    ", contributions=" + contributions +
                    '}';
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.color);
            dest.writeLong(this.date);
            dest.writeInt(this.contributions);
        }

        protected GitDay(Parcel in) {
            this.color = in.readInt();
            this.date = in.readLong();
            this.contributions = in.readInt();
        }

        public static final Creator<GitDay> CREATOR = new Creator<GitDay>() {
            @Override
            public GitDay createFromParcel(Parcel source) {
                return new GitDay(source);
            }

            @Override
            public GitDay[] newArray(int size) {
                return new GitDay[size];
            }
        };
    }

    interface ContributionsRequestListener {

        void onResponse(ArrayList<GitDay> contributions);

        void onError(VolleyError error);

    }


}
