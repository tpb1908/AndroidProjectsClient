package com.tpb.projects.data.models;


import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

import java.security.acl.Owner;

/**
 * Created by theo on 15/12/16.
 */

public class Repo extends GenericJson {

    @Key("id")
    private Integer id;

    @Key("owner")
    private Owner owner;

    @Key("name")
    private String name;

    @Key("full_name")
    private String fullName;

    @Key("description")
    private String description;

    @Key("private")
    private Boolean isPrivate;

    @Key("fork")
    private Boolean fork;

    @Key("url")
    private String url;

    @Key("html_url")
    private String htmlUrl;

    @Key("language")
    private String language;

    @Key("forks_count")
    private Integer forksCount;

    @Key("watchers_count")
    private Integer watchersCount;

    public final Integer getId() {
        return id;
    }

    public final Owner getOwner() {
        return owner;
    }

    public final String getName() {
        return name;
    }

    public final String getFullName() {
        return fullName;
    }

    public final String getDescription() {
        return description;
    }

    public final Boolean isPrivate() {
        return isPrivate;
    }

    public final Boolean isFork() {
        return fork;
    }

    public final String getUrl() {
        return url;
    }

    public final String getHtmlUrl() {
        return htmlUrl;
    }

    public final String getLanguage() {
        return language;
    }

    public final Integer getForksCount() {
        return forksCount;
    }

    public final Integer getWatchersCount() {
        return watchersCount;
    }

    @Override
    public Repo clone() {
        return (Repo) super.clone();
    }

    @Override
    public Repo set(String fieldName, Object value) {
        return (Repo) super.set(fieldName, value);
    }

}
