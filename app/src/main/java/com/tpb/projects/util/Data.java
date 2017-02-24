package com.tpb.projects.util;

import android.util.Base64;

import com.tpb.projects.data.models.Repository;

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

    public static final Comparator<Repository> repoAlphaSort = (r1, r2) -> r1.getName().compareToIgnoreCase(r2.getName());

    public static String intArrayForPrefs(List<Integer> values) {
        final StringBuilder builder = new StringBuilder();
        for(int i : values) {
            builder.append(i).append(",");
        }
        return builder.toString();
    }

    public static String stringArrayForPrefs(List<String> values) {
        final StringBuilder builder = new StringBuilder();
        for(String s : values) {
            builder.append(s).append(",");
        }
        return builder.toString();
    }

    public static String[] stringArrayFromPrefs(String value) {
        return value.split(",");
    }

    public static int[] intArrayFromPrefs(String value) {
        final String[] values = value.split(",");
        final int[] ints = new int[values.length];
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

    public static String formatBytes(int b) {
        if(b < 1024) return Integer.toString(b) + " B";
        if(b < 1024 * 1024) return String.format("%.2f", b / 1024f) + " KB";
        if(b < 1024 * 1024 * 1024) return String.format("%.2f", b / (1024f * 1024f)) + " MB";
        return String.format("%.2f", b / (1024f * 1024f * 1024f)) + " GB";
    }

    public static String base64Decode(String base64) {
        return new String(Base64.decode(base64, Base64.DEFAULT));
    }

    //http://stackoverflow.com/a/10621553/4191572
    private static final SimpleDateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

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

    public static int instancesOf(String s, String i) {
        int last = 0;
        int count = 0;
        while(last != -1) {
            last = s.indexOf(i, last);
            if(last != -1) {
                count++;
                last += i.length();
            }
        }
        return count;
    }

}
