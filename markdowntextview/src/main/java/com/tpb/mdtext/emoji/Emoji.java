package com.tpb.mdtext.emoji;

import java.util.Collections;
import java.util.List;

/**
 * Created by theo on 16/04/17.
 */

public class Emoji {

    private final String description;
    private final List<String> aliases;
    private final List<String> tags;
    private final String unicode;

    protected Emoji(String unicode, String description, List<String> aliases, List<String> tags) {
        this.unicode = unicode;
        this.description = description;
        this.aliases = Collections.unmodifiableList(aliases);
        this.tags = Collections.unmodifiableList(tags);
    }

    public String getDescription() {
        return description;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getUnicode() {
        return unicode;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Emoji && unicode.equals(((Emoji) o).unicode);
    }

    @Override
    public int hashCode() {
        return unicode.hashCode();
    }

    @Override
    public String toString() {
        return "Emoji{" +
                "description='" + description + '\'' +
                ", aliases=" + aliases +
                ", tags=" + tags +
                ", unicode='" + unicode + '\'' +
                '}';
    }
}
