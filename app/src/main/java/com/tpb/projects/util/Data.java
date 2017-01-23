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
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

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

    private static final Parser parser = Parser.builder().build();
    private static final HtmlRenderer renderer = HtmlRenderer.builder().build();

    public static Comparator<Repository> repoAlphaSort = (r1, r2) -> r1.getName().compareToIgnoreCase(r2.getName());

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


    public static String parseMD(String s, String fullReopName) {
        return renderer.render(parser.parse(formatMD(s, fullReopName)));
    }

    public static String parseMD(String s) {
        return renderer.render(parser.parse(s));
    }

    public static String formatMD(String s, String fullRepoPath) {

        final StringBuilder builder = new StringBuilder();
        char p = ' ';
        char pp = ' ';
        final char[] cs = s.toCharArray();
        for(int i = 0; i < s.length(); i++) {
            if(pp != '\n' && cs[i] == '\n' && i != cs.length - 1) {
                builder.append("\n");
            }
            if(cs[i] == '@' && (p == ' '  || p == '\n')) {
                //Max username length is 39 characters
                //Usernames can be alphanumeric with single hyphens
                i = parseUsername(builder, cs, i);
            } else if(cs[i] == '-' && p == '-' && pp == '-') {
                //TODO Find out if there is a way of computing characters per line and filling the string
                //I could try using the strike tag
                builder.setLength(builder.length() - 2);
                builder.append("─────\n");

            } else if(cs[i] == '#'  && (p == ' '  || p == '\n')) {
                i = parseIssue(builder, cs, i, fullRepoPath);
            } else {
                builder.append(cs[i]);
            }
            pp = p;
            p = cs[i];
        }
        return builder.toString();
    }

    //TODO
    //Parse urls
    //Search for www
    //CHeck if valid url by moving forward
    //If so, move backward.
    //Check if previous character
    // - Doesn't exist
    // - is http:// or https://
    // - is space
    // If so, wrap in href

    private static int parseUsername(StringBuilder builder, char[] cs, int pos) {
        final StringBuilder nameBuilder = new StringBuilder();
        char p = ' ';
        for(int i = ++pos; i < cs.length; i++) {
            if(((cs[i] >= 'A' && cs[i] <= 'Z') ||
                    (cs[i] >= '0' && cs[i] <= '9') ||
                    (cs[i] >= 'a' && cs[i] <= 'z') ||
                    (cs[i] == '-' && p != '-')) &&
                    i - pos < 38 &&
                    i != cs.length - 1) {
                nameBuilder.append(cs[i]);
                p = cs[i];
                //nameBuilder.length() > 0 stop us linking a single @
            } else if((cs[i] == ' ' || cs[i] == '\n' || i == cs.length - 1) && nameBuilder.length() > 0) {
                if(i == cs.length - 1) {
                    nameBuilder.append(cs[i]); //Otherwise we would miss the last char of the name
                }
                builder.append("[@");
                builder.append(nameBuilder.toString());
                builder.append(']');
                builder.append('(');
                builder.append("https://github.com/");
                builder.append(nameBuilder.toString());
                builder.append(')');
                if(i != cs.length - 1) {
                    builder.append(cs[i]); // We still need to append the space or newline
                }
                return i;
            } else {
                builder.append("@");
                return --pos;
            }

        }
        builder.append("@");
        return --pos;
    }

    private static int parseIssue(StringBuilder builder, char[] cs, int pos, String fullRepoPath) {
        final StringBuilder numBuilder = new StringBuilder();
        for(int i = ++pos; i < cs.length; i++) {
            if(cs[i] >= '0' && cs[i] <= '9' && i != cs.length - 1) {
                numBuilder.append(cs[i]);
            } else if(cs[i] == ' ' || cs[i] == '\n' || i == cs.length - 1) {
                if(i == cs.length - 1) {
                    if(cs[i] >= '0' && cs[i] <= '9') {
                        numBuilder.append(cs[i]);
                    } else if(numBuilder.length() == 0) {
                        builder.append("#");
                        return --pos;
                    }
                }
                builder.append("[#");
                builder.append(numBuilder.toString());
                builder.append("]");
                builder.append("(");
                builder.append("https://github.com/");
                builder.append(fullRepoPath);
                builder.append("/issues/");
                builder.append(numBuilder.toString());
                builder.append(")");
                if(i != cs.length - 1) {
                    builder.append(cs[i]); // We still need to append the space or newline
                }
                return i;
            } else {
                builder.append("#");
                return --pos;
            }
        }
        builder.append("#");
        return --pos;
    }


}
