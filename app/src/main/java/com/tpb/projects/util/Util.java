package com.tpb.projects.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.format.DateFormat;
import android.widget.EditText;

import com.tpb.github.data.models.DataModel;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created by theo on 16/12/16.
 */

public class Util {

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

    /**
     * @param values The set of values to search
     * @param key    The key to find
     * @return The index of key in values. -1 if key not in values
     */
    public static int indexOf(@NonNull int[] values, int key) {
        for(int i = 0; i < values.length; i++) if(values[i] == key) return i;
        return -1;
    }

    /**
     * @param values The set of values to search
     * @param key    The key to find
     * @return The index of key in values. -1 if key not in values
     */
    public static int indexOf(@NonNull String[] values, @NonNull String key) {
        for(int i = 0; i < values.length; i++) if(key.equals(values[i])) return i;
        return -1;
    }

    public static <T> int indexOf(@NonNull T[] values, @NonNull T key) {
        for(int i = 0; i < values.length; i++) {
            if(key.equals(values[i])) return i;
        }
        return -1;
    }

    public static int indexOf(@NonNull Collection<? extends Pair> items, @NonNull Object key) {
        int i = 0;
        for(Pair p : items) {
            if(key.equals(p.first) || key.equals(p.second)) return i;
            i++;
        }
        return -1;
    }

    /**
     * Formats a size in kilobytes to a 2 d.p value for the largest valid unit suffix
     *
     * @param kb The size in kilobytes
     * @return The formatted size. E.g. 1000 -> "1 MB"
     */
    @SuppressLint("DefaultLocale")
    public static String formatKB(int kb) {
        if(kb < 1000) return Integer.toString(kb) + " KB";
        if(kb < 1000 * 1000) return String.format("%.2f", kb / 1000f) + " MB";
        return String.format("%.2f", kb / (1000f * 1000f)) + " GB";
    }

    /**
     * Formats a size in bytes to a 2 d.p value for the largest valid unit suffix
     *
     * @param b The size in bytes
     * @return The formatted size. E.g. 1000 -> "1 KB"
     */
    @SuppressLint("DefaultLocale")
    public static String formatBytes(int b) {
        if(b < 1000) return Integer.toString(b) + " B";
        if(b < 1000 * 1000) return String.format("%.2f", b / 1000f) + " KB";
        if(b < 1000 * 1000 * 1000) return String.format("%.2f", b / (1000f * 1000f)) + " MB";
        return String.format("%.2f", b / (1000f * 1000f * 1000f)) + " GB";
    }

    public static String formatDateLocally(Context context, Date date) {
        return DateFormat.getMediumDateFormat(context).format(date);
    }

    public static boolean isNotNullOrEmpty(@Nullable String s) {
        return s != null && !s.isEmpty() && !DataModel.JSON_NULL.equals(s);
    }

    /**
     * @return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
     */
    public static boolean hasLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static void insertString(@NonNull EditText et, @NonNull String insert) {
        insertString(et, insert, insert.length());
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

}
