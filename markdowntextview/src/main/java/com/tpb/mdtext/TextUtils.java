package com.tpb.mdtext;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.Spanned;

import com.tpb.mdtext.views.spans.CleanURLSpan;

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

    public static boolean addLinks(@NonNull Spannable spannable) {
        boolean hasMatches = false;
        final Matcher m = MDPattern.SPACED_MATCH_PATTERN.matcher(spannable);
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

    public static int getTextColorForBackground(int bg) {
        double r = Color.red(bg) / 255d;
        if(r <= 0.04045) {
            r = r / 12.92;
        } else {
            r = Math.pow((r + 0.055) / 1.055, 2.4);
        }
        double g = Color.green(bg) / 255d;
        if(g <= 0.04045) {
            g = g / 12.92;
        } else {
            g = Math.pow((g + 0.055) / 1.055, 2.4);
        }
        double b = Color.blue(bg) / 255d;
        if(b <= 0.04045) {
            b = b / 12.92;
        } else {
            b = Math.pow((b + 0.055) / 1.055, 2.4);
        }
        return (0.2126 * r + 0.7152 * g + 0.0722 * b) > 0.35 ? Color.BLACK : Color.WHITE;
    }

    public static boolean isValidURL(String possible) {
        return MDPattern.AUTOLINK_WEB_URL.matcher(possible).matches();
    }

    public static String capitaliseFirst(String s) {
        if(s == null || s.length() == 0) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    /**
     * Counts the instances of a string within another string
     *
     * @param s The string to search
     * @param sub The string to count instances of
     * @return The number of instances of s2 in s1
     */
    public static int instancesOf(@NonNull String s, @NonNull String sub) {
        if(s.length() == 0 || sub.length() == 0 || sub.length() > s.length()) return 0;
        int last = 0;
        int count = 0;
        while(last != -1) {
            last = s.indexOf(sub, last);
            if(last != -1) {
                count++;
                last++;
            }
        }
        return count;
    }

    public static boolean isInteger(String s) {
        return isInteger(s, 10);
    }

    public static boolean isInteger(String s, int radix) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i), radix) < 0) return false;
        }
        return true;
    }

}
