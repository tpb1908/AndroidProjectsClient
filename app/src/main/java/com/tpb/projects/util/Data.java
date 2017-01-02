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

package com.tpb.projects.util;

import android.util.Base64;
import android.util.Log;

import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Column;
import com.tpb.projects.data.models.Project;
import com.tpb.projects.data.models.Repository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by theo on 16/12/16.
 */

public class Data {
    private static final String TAG = Data.class.getSimpleName();

    public static Comparator<Repository> repoAlphaSort = (r1, r2) -> r1.getName().compareToIgnoreCase(r2.getName());

    public static int countOccurrences(String s, char c) {
        int o = 0;
        for(char ci : s.toCharArray()) if(c == ci) o++;
        return o;
    }

    public static String stringArrayForPrefs(String[] values) {
        final StringBuilder builder = new StringBuilder();
        for(String s : values) {
            builder.append(s).append(",");
        }
        return builder.toString();
    }

    public static String[] stringArrayFromPrefs(String value) {
        return value.split(",");
    }

    public static String intArrayForPrefs(int[] values) {
        final StringBuilder builder = new StringBuilder();
        for(int i : values) {
            builder.append(i).append(",");
        }
        return builder.toString();
    }

    public static String intArrayForPrefs(List<Integer> values) {
        final StringBuilder builder = new StringBuilder();
        for(int i : values) {
            builder.append(i).append(",");
        }
        return builder.toString();
    }

    public static int[] intArrayFromPrefs(String value) {
        final String[] values = value.split(",");
        final int[] ints = new int[values.length ];
        if(value.length() == 0) return ints;
        for(int i = 0; i < values.length; i++) ints[i] = Integer.parseInt(values[i]);
        return ints;
    }

    public static int indexOf(int[] values, int key) {
        for(int i = 0; i < values.length; i++) if(values[i] == key) return i;
        return -1;
    }

    public static int indexOf(String[] values, String key) {
        for(int i = 0; i < values.length; i++) if(values[i].equals(key)) return i;
        return -1;
    }

    public static String formatKB(int kb) {
        if(kb < 1024) return Integer.toString(kb) + " KB";
        if(kb < 1024 * 1024) return String.format("%.2f", kb / 1024f) + " MB";
        return String.format("%.2f", kb / (1024f * 1024f)) + " GB";
    }

    public static String base64Decode(String base64) {
        return new String(Base64.decode(base64, Base64.DEFAULT));
    }

    //http://stackoverflow.com/a/10621553/4191572
    private static final SimpleDateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    /**
     * Transform Calendar to ISO 8601 string.
     */
    private static String fromCalendar(final Calendar calendar) {
        Date date = calendar.getTime();
        String formatted = ISO8601.format(date);
        return formatted.substring(0, 22) + ":" + formatted.substring(22);
    }

    /**
     * Get current date and time formatted as ISO 8601 string.
     */
    public static String now() {
        return fromCalendar(GregorianCalendar.getInstance());
    }

    public static String toISO8061(long t) {
        return ISO8601.format(new Date(t * 1000));
    }

    /**
     * Transform ISO 8601 string to Calendar.
     */
    public static Calendar toCalendar(final String iso8601string)
            throws ParseException {
        final Calendar calendar = GregorianCalendar.getInstance();
        String s = iso8601string.replace("Z", "+00:00");
        try {
            s = s.substring(0, 22) + s.substring(23);  // to get rid of the ":"
        } catch(IndexOutOfBoundsException e) {
            throw new ParseException("Invalid length", 0);
        }
        final Date date = ISO8601.parse(s);
        calendar.setTime(date);
        return calendar;
    }

    public static JSONObject save(Card[] cards) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(Constants.JSON_KEY_TIME, System.nanoTime() / 1000);
            final JSONArray arr = new JSONArray();
            for(Card c : cards) {
                arr.put(Card.parse(c));
            }
            obj.put(Constants.JSON_KEY_CARDS, arr);
        } catch(JSONException jse) {
            Log.e(TAG, "save: ", jse);
        }
        return obj;
    }

    public static JSONObject save(Project project, Column[] columns, Card[][] cards) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(Constants.JSON_KEY_TIME, System.nanoTime() / 1000);
            obj.put(Constants.JSON_KEY_PROJECT, Project.parse(project));
            final JSONArray carr = new JSONArray();
            for(int i = 0; i < columns.length; i++) {
                final JSONArray arr = new JSONArray();
                for(Card c : cards[i]) arr.put(Card.parse(c));
                carr.put(arr);
            }

            obj.put(Constants.JSON_KEY_COLUMNS, carr);
        } catch(JSONException jse) {
            Log.e(TAG, "save: ", jse);
        }
        return obj;
    }

    public static JSONObject save(Repository repo, Project[] projects, Column[][] columns, Card[][][] cards) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(Constants.JSON_KEY_TIME, System.nanoTime() / 1000);
            obj.put(Constants.JSON_KEY_REPOSITORY, Repository.parse(repo));

            final JSONArray parr = new JSONArray();
            for(int i = 0; i < projects.length; i++) {
                final JSONArray arr = new JSONArray();
                for(int j = 0; j < columns[i].length; j++) {
                    final JSONArray carr = new JSONArray();
                    for(Card c : cards[i][j]) carr.put(Card.parse(c));
                    arr.put(carr);
                }
                parr.put(arr);
            }
            obj.put(Constants.JSON_KEY_PROJECTS, parr);
        } catch(JSONException jse) {
            Log.e(TAG, "save: ", jse);
        }
        return obj;
    }


}
