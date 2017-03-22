package org.sufficientlysecure.htmltext;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.util.Linkify;

import org.sufficientlysecure.htmltext.spans.CleanURLSpan;

import java.util.Locale;
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

    public static String replace(@Nullable String s, Map<String, String> replacements) {
        return replace(s, replacements, generatePattern(replacements.keySet()));
    }

    public static String replace(@Nullable String s, Map<String, String> replacements, Pattern pattern) {
        if(s == null) return null;
        final StringBuffer buffer = new StringBuffer();
        final Matcher matcher = pattern.matcher(s);
        while(matcher.find()) {
            matcher.appendReplacement(buffer, replacements.get(matcher.group()));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public static Pattern generatePattern(@NonNull Set<String> keys) {
        final StringBuilder b = new StringBuilder();
        int i = 0;
        for(String s : keys) {
            b.append(REGEX_ESCAPE_CHARS.matcher(s).replaceAll("\\\\$0"));
            if(++i != keys.size()) b.append('|');
        }
        return Pattern.compile(b.toString());
    }

    public static final boolean addLinks(@NonNull Spannable text, @NonNull Pattern pattern,
                                         @Nullable String scheme) {
        return addLinks(text, pattern, scheme, null, null, null);
    }

    public static boolean addLinks(@NonNull Spannable spannable, @NonNull Pattern pattern,
                                         @Nullable  String defaultScheme, @Nullable String[] schemes,
                                         @Nullable Linkify.MatchFilter matchFilter, @Nullable Linkify.TransformFilter transformFilter) {
        final String[] schemesCopy;
        if (defaultScheme == null) defaultScheme = "";
        if (schemes == null || schemes.length < 1) {
            schemes = new String[0];
        }

        schemesCopy = new String[schemes.length + 1];
        schemesCopy[0] = defaultScheme.toLowerCase(Locale.ROOT);
        for (int index = 0; index < schemes.length; index++) {
            String scheme = schemes[index];
            schemesCopy[index + 1] = (scheme == null) ? "" : scheme.toLowerCase(Locale.ROOT);
        }

        boolean hasMatches = false;
        final Matcher m = pattern.matcher(spannable);

        while (m.find()) {
            int start = m.start();
            int end = m.end();
            boolean allowed = true;

            if (matchFilter != null) {
                allowed = matchFilter.acceptMatch(spannable, start, end);
            }

            if (allowed) {
                String url = makeUrl(m.group(0), schemesCopy, m, transformFilter);

                applyLink(url, start, end, spannable);
                hasMatches = true;
            }
        }

        return hasMatches;
    }


    private static void applyLink(String url, int start, int end, Spannable text) {
        text.setSpan(new CleanURLSpan(url), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static String makeUrl(@NonNull String url, @NonNull String[] prefixes,
                                        Matcher matcher, @Nullable Linkify.TransformFilter filter) {
        if (filter != null) {
            url = filter.transformUrl(matcher, url);
        }

        boolean hasPrefix = false;

        for(String prefixe : prefixes) {
            if(url.regionMatches(true, 0, prefixe, 0, prefixe.length())) {
                hasPrefix = true;

                // Fix capitalization if necessary
                if(!url.regionMatches(false, 0, prefixe, 0, prefixe.length())) {
                    url = prefixe + url.substring(prefixe.length());
                }
                break;
            }
        }

        if (!hasPrefix && prefixes.length > 0) {
            url = prefixes[0] + url;
        }

        return url;
    }
}
