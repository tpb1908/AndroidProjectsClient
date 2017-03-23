package com.tpb.projects.util;

import android.content.Context;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.util.Base64;
import android.widget.EditText;

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

public class Util {
    private static final String TAG = Util.class.getSimpleName();

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

    /**
     * @param values The set of values to search
     * @param key The key to find
     * @return The index of key in values. -1 if key not in values
     */
    public static int indexOf(int[] values, int key) {
        for(int i = 0; i < values.length; i++) if(values[i] == key) return i;
        return -1;
    }

    /**
     * @param values The set of values to search
     * @param key The key to find
     * @return The index of key in values. -1 if key not in values
     */
    public static int indexOf(String[] values, String key) {
        for(int i = 0; i < values.length; i++) if(values[i].equals(key)) return i;
        return -1;
    }

    /**
     * Formats a size in kilobytes to a 2 d.p value for the largest valid unit suffix
     * @param kb The size in kilobytes
     * @return The formatted size. E.g. 1024 -> "1 MB"
     */
    public static String formatKB(int kb) {
        if(kb < 1024) return Integer.toString(kb) + " KB";
        if(kb < 1024 * 1024) return String.format("%.2f", kb / 1024f) + " MB";
        return String.format("%.2f", kb / (1024f * 1024f)) + " GB";
    }

    /**
     * Formats a size in bytes to a 2 d.p value for the largest valid unit suffix
     * @param b The size in bytes
     * @return The formatted size. E.g. 1024 -> "1 KB"
     */
    public static String formatBytes(int b) {
        if(b < 1024) return Integer.toString(b) + " B";
        if(b < 1024 * 1024) return String.format("%.2f", b / 1024f) + " KB";
        if(b < 1024 * 1024 * 1024) return String.format("%.2f", b / (1024f * 1024f)) + " MB";
        return String.format("%.2f", b / (1024f * 1024f * 1024f)) + " GB";
    }

    /**
     * @param base64 A base64 encoded String
     * @return The decoded value with Base64.DEFAULT
     */
    public static String base64Decode(String base64) {
        return new String(Base64.decode(base64, Base64.DEFAULT));
    }

    //http://stackoverflow.com/a/10621553/4191572
    private static final SimpleDateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    /**
     * Converts a UNIX time value in seconds to an ISO8061 string
     * @param t The time since 1970 in seconds
     * @return Time formatted as yyyy-MM-dd'T'HH:mm:ssZ
     */
    public static String toISO8061FromSeconds(long t) {
        return ISO8601.format(new Date(t * 1000));
    }

    /**
     * Converts a UNIX time value in milliseconds to an ISO8061 string
     * @param t The time since 1970 in milliseconds
     * @return Time formatted as yyyy-MM-dd'T'HH:mm:ssZ
     */
    public static String toISO8061FromMilliseconds(long t) {
        final String time = ISO8601.format(new Date(t));
        int zoneIndex = Math.max(time.indexOf('+'), time.indexOf('-'));
        return time.substring(0, zoneIndex) + 'Z';
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
            throw new ParseException("Invalid length", iso8601string.length());
        }
        final Date date = ISO8601.parse(s);
        calendar.setTime(date);
        return calendar;
    }

    public static String formatDateLocally(Context context, Date date) {
        return DateFormat.getDateFormat(context).format(date);
    }

    /**
     * Counts the instances of a string within another string
     * @param s1 The string to search
     * @param s2 The string to count instances of
     * @return The number of instances of s2 in s1
     */
    public static int instancesOf(@NonNull String s1, @NonNull String s2) {
        int last = 0;
        int count = 0;
        while(last != -1) {
            last = s1.indexOf(s2, last);
            if(last != -1) {
                count++;
                last += s2.length();
            }
        }
        return count;
    }

    /**
     *
     * @return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
     */
    public static boolean hasLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static void insertString(@NonNull EditText et, @NonNull String insert) {
        insertString(et, insert, 0);
    }

    public static void insertString(@NonNull EditText et, @NonNull String insert, @IntRange(from = 0) int relativePosition) {
        final int start = Math.max(et.getSelectionStart(), 0);
        et.getText().insert(start, insert);
        et.setSelection(start + relativePosition);
    }
}
