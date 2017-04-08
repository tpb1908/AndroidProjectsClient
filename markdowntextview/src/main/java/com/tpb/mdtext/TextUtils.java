package com.tpb.mdtext;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;

import com.tpb.mdtext.views.spans.CleanURLSpan;
import com.tpb.mdtext.views.spans.RoundedBackgroundEndSpan;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by theo on 21/03/17.
 */

public class TextUtils {

    private static final Pattern REGEX_ESCAPE_CHARS =
            Pattern.compile("[\\<\\(\\[\\{\\\\\\^\\-\\=\\$\\!\\|\\]\\}\\)‌​\\?\\*\\+\\.\\>]");

    static String replace(@Nullable String s, Map<String, String> replacements) {
        return replace(s, replacements, generatePattern(replacements.keySet()));
    }

    static String replace(@Nullable String s, Map<String, String> replacements, Pattern pattern) {
        if(s == null) return null;
        final StringBuffer buffer = new StringBuffer();
        final Matcher matcher = pattern.matcher(s);
        while(matcher.find()) {
            matcher.appendReplacement(buffer, replacements.get(matcher.group()));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    static Pattern generatePattern(@NonNull Set<String> keys) {
        final StringBuilder b = new StringBuilder();
        int i = 0;
        for(String s : keys) {
            b.append(REGEX_ESCAPE_CHARS.matcher(s).replaceAll("\\\\$0"));
            if(++i != keys.size()) b.append('|');
        }
        return Pattern.compile(b.toString());
    }

    public static boolean addLinks(@NonNull Spannable spannable, @NonNull Pattern pattern) {
        boolean hasMatches = false;
        final Matcher m = pattern.matcher(spannable);

        while(m.find()) {
            spannable.setSpan(
                    new CleanURLSpan(m.group(0)),
                    m.start(),
                    m.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            hasMatches = true;
        }

        return hasMatches;
    }

    public static void addRoundedBackgroundSpan(Editable editable, String subsequence, int bg) {
        final int start = editable.length();
        editable.append(" ");
        editable.append(subsequence);
        editable.append(" ");
        editable.setSpan(
                new RoundedBackgroundEndSpan(bg, false),
                start,
                start + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        editable.setSpan(
                new BackgroundColorSpan(bg),
                start,
                editable.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        editable.setSpan(
                new ForegroundColorSpan(
                        TextUtils.getTextColorForBackground(bg)
                ),
                start,
                editable.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        editable.setSpan(
                new RoundedBackgroundEndSpan(bg, true),
                editable.length() - 1,
                editable.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
    }

    public static int getTextColorForBackground(int bg) {
        double r = Color.red(bg) / 255d;
        if(r <= 0.03928) {
            r = r / 12.92;
        } else {
            r = Math.pow((r + 0.055) / 1.055, 2.4);
        }
        double g = Color.green(bg) / 255d;
        if(g <= 0.03928) {
            g = g / 12.92;
        } else {
            g = Math.pow((g + 0.055) / 1.055, 2.4);
        }
        double b = Color.blue(bg) / 255d;
        if(b <= 0.03928) {
            b = b / 12.92;
        } else {
            b = Math.pow((b + 0.055) / 1.055, 2.4);
        }
        return (0.2126 * r + 0.7152 * g + 0.0722 * b) > 0.35 ? Color.BLACK : Color.WHITE;
    }

    public static boolean isValidURL(String possible) {
        return URLPattern.AUTOLINK_WEB_URL.matcher(possible).matches();
    }

    public static String capitaliseFirst(String s) {
        if(s == null || s.length() == 0) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
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


}
