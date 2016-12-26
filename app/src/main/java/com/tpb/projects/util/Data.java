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
        final int[] ints = new int[values.length + 1];
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
    public static String fromCalendar(final Calendar calendar) {
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

    public static String timeAgo(Calendar time) {
        return timeAgo(time.getTime().getTime());
    }

    public static String timeAgo(long time) {
        final long now = System.currentTimeMillis() / 1000;
        final long delta = (now - time);
        if(delta / (365 * 24 * 3600) > 0) {
            final long div = delta / (365 * 24 * 3600);
            return div + (div == 1 ? " year" : " years");
        } else if(delta / (28 * 24 * 3600) > 0) {
            final long div = delta / (28 * 24 * 3600);
            return div + (div == 1 ? " month" : " months");
        } else if(delta / (7 * 24 * 3600) > 0) {
            final long div = delta / (7 * 24 * 3600);
            return div + (div == 1 ? " week" : " weeks");
        } else if(delta / (24 * 3600) > 0) {
            final long div = delta / (24 * 3600);
            return div + (div == 1 ? " day" : " days");
        } else if(delta / (3600) > 0) {
            final long div = delta / (3600);
            return div + (div == 1 ? " hour" : " hours");
        } else {
            final long div = delta / 60;
            if(div > 5) {
                return div + (div == 1 ? " minute" : " minutes");
            } else {
                return "just now";
            }
        }
    }
}
