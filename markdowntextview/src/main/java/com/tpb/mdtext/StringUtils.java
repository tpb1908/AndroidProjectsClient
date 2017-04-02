package com.tpb.mdtext;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.Spanned;

import com.tpb.mdtext.spans.CleanURLSpan;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by theo on 21/03/17.
 */

public class StringUtils {

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

}
