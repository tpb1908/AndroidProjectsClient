package com.tpb.projects.util;

import android.content.Context;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.format.DateFormat;
import android.widget.EditText;

import com.tpb.github.data.models.DataModel;
import com.tpb.github.data.models.Repository;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by theo on 16/12/16.
 */

public class Util {
    private static final String TAG = Util.class.getSimpleName();

    public static final Comparator<Repository> repoAlphaSort = (r1, r2) -> r1.getName()
                                                                             .compareToIgnoreCase(
                                                                                     r2.getName());

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
     * @param key    The key to find
     * @return The index of key in values. -1 if key not in values
     */
    public static int indexOf(int[] values, int key) {
        for(int i = 0; i < values.length; i++) if(values[i] == key) return i;
        return -1;
    }

    /**
     * @param values The set of values to search
     * @param key    The key to find
     * @return The index of key in values. -1 if key not in values
     */
    public static int indexOf(String[] values, String key) {
        for(int i = 0; i < values.length; i++) if(values[i].equals(key)) return i;
        return -1;
    }

    public static <T> int indexOf(@NonNull T[] values, @NonNull T key) {
        for(int i = 0; i < values.length; i++) {
            if(key.equals(values[i])) return i;
        }
        return -1;
    }

    /**
     * Formats a size in kilobytes to a 2 d.p value for the largest valid unit suffix
     *
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
     *
     * @param b The size in bytes
     * @return The formatted size. E.g. 1024 -> "1 KB"
     */
    public static String formatBytes(int b) {
        if(b < 1024) return Integer.toString(b) + " B";
        if(b < 1024 * 1024) return String.format("%.2f", b / 1024f) + " KB";
        if(b < 1024 * 1024 * 1024) return String.format("%.2f", b / (1024f * 1024f)) + " MB";
        return String.format("%.2f", b / (1024f * 1024f * 1024f)) + " GB";
    }

    public static boolean isNotNullOrEmpty(@Nullable String s) {
        return s != null && !s.isEmpty() && !DataModel.JSON_NULL.equals(s);
    }

    //http://stackoverflow.com/a/10621553/4191572
    private static final SimpleDateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");


    /**
     * Converts a UNIX time value in milliseconds to an ISO8061 string
     *
     * @param t The time since 1970 in milliseconds
     * @return Time formatted as yyyy-MM-dd'T'HH:mm:ssZ
     */
    public static String toISO8061FromMilliseconds(long t) {
        final String time = ISO8601.format(new Date(t));
        int zoneIndex = Math.max(time.indexOf('+'), time.indexOf('-'));
        return time.substring(0, zoneIndex) + 'Z';
    }

    public static String formatDateLocally(Context context, Date date) {
        return DateFormat.getMediumDateFormat(context).format(date);
    }

    /**
     * Counts the instances of a string within another string
     *
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
     * @return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
     */
    public static boolean hasLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static void insertString(@NonNull EditText et, @NonNull String insert) {
        insertString(et, insert, 0);
    }

    /**
     * Inserts a string into an EditText
     *
     * @param relativePosition Where to place the cursor, relative to the start of the inserted string
     */
    public static void insertString(@NonNull EditText et, @NonNull String insert, @IntRange(from = 0) int relativePosition) {
        final int start = Math.max(et.getSelectionStart(), 0);
        et.getText().insert(start, insert);
        et.setSelection(start + relativePosition);
    }

    public static int indexInPair(@NonNull Collection<? extends Pair> items, @NonNull Object o) {
        int i = 0;
        for(Pair p : items) {
            if(o.equals(p.first) || o.equals(p.second)) return i;
        }
        return -1;
    }
}
