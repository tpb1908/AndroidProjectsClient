package com.tpb.projects.data.models;

import com.google.api.client.util.Key;

import java.util.List;

/**
 * Created by theo on 15/12/16.
 */

public class Repos extends GitResponse {

    @Key(KEY_DATA)
    private List<Repo> repositories;

    public final List<Repo> getRepos() {
        return repositories;
    }

    @Override
    public Repos clone() {
        return (Repos) super.clone();
    }

    @Override
    public Repos set(String fieldName, Object value) {
        return (Repos) super.set(fieldName, value);
    }
    
}
