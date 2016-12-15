package com.tpb.projects.data.models;

/**
 * Created by theo on 15/12/16.
 */

public class GitResponse extends ClientErrors {

    public static final String KEY_DATA = "data";

    private Pagination pagination;

    public final Pagination getPagination() {
        return pagination;
    }

    public final GitResponse setPagination(Pagination pagination) {
        this.pagination = pagination;
        return this;
    }

    @Override
    public GitResponse clone() {
        return (GitResponse) super.clone();
    }

    @Override
    public GitResponse set(String fieldName, Object value) {
        return (GitResponse) super.set(fieldName, value);
    }

}
