package com.tpb.github.data.models;

/**
 * Created by theo on 15/12/16.
 */

public abstract class DataModel {

    static final String ID = "id";
    static final String NAME = "name";
    static final String CREATED_AT = "created_at";
    static final String UPDATED_AT = "updated_at";
    static final String URL = "url";
    public static final String JSON_NULL = "null";

    long createdAt;

    public abstract long getCreatedAt();

}
