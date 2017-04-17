package com.tpb.mdtext.emoji;

import android.content.res.AssetManager;
import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by theo on 16/04/17.
 */

public class EmojiLoader {

    private static final Map<String, Emoji> ALIAS_MAP = new HashMap<>();
    private static final Map<String, Set<Emoji>> TAG_MAP = new HashMap<>();
    private static final List<Emoji> EMOJIS  = new ArrayList<>();

    public static void loadEmojis(AssetManager assets) {
        if(EMOJIS.size() > 0) return;
        try {
            final JSONArray JSON = new JSONArray(inputStreamToString(assets.open("json/emojis.json")));
            for(int i = 0; i < JSON.length(); i++) {
                final Emoji emoji = buildEmojiFromJSON(JSON.getJSONObject(i));
                if(emoji != null) {
                    EMOJIS.add(emoji);
                    for(String tag : emoji.getTags()) {
                        if(TAG_MAP.get(tag) == null) TAG_MAP.put(tag, new HashSet<Emoji>());
                        TAG_MAP.get(tag).add(emoji);
                    }
                    for(String alias : emoji.getAliases()) {
                        ALIAS_MAP.put(alias, emoji);
                    }
                }
            }
        } catch(Exception ignored) {}
    }

    private static String inputStreamToString(InputStream is) throws IOException {
        final BufferedReader reader = new BufferedReader(  new InputStreamReader(is));
        String line;
        final StringBuilder builder = new StringBuilder();
        while((line =  reader.readLine()) != null){
            builder.append(line);
        }
        is.close();
        return builder.toString();
    }

    private static Emoji buildEmojiFromJSON(JSONObject json) throws JSONException {
        if (!json.has("emoji")) {
            return null;
        }
        String emoji  = json.getString("emoji");
        String description = null;
        if (json.has("description")) {
            description = json.getString("description");
        }

        List<String> aliases = JSONArrayToStringList(json.getJSONArray("aliases"));
        List<String> tags = JSONArrayToStringList(json.getJSONArray("tags"));
        return new Emoji(emoji, description, aliases, tags);
    }

    private static List<String> JSONArrayToStringList(JSONArray array) throws JSONException {
        final List<String> strings = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            strings.add(array.getString(i));
        }
        return strings;
    }

    public static List<Emoji> getAllEmoji() {
        return EMOJIS;
    }

    public static Set<Emoji> getEmojiForTag(@NonNull String tag) {
        return TAG_MAP.get(tag);
    }

    public static Emoji getEmojiForAlias(@NonNull String alias) {
        if(alias.startsWith(":")) alias = alias.substring(1, alias.length());
        if(alias.endsWith(":")) alias = alias.substring(0, alias.length() - 1);
        return ALIAS_MAP.get(alias);
    }

}
