package com.tpb.projects.data.auth.models;

/**
 * Created by theo on 15/12/16.
 */

public class Card {

    private Card() {}

    private static final String COLUMN_URL = "column_url";
    private String columnUrl;

    private static final String CONTENT_URL = "content_url";
    private String contentUrl;

    private static final String ID = "id";
    private int id;

    private static final String NOTE = "note";
    private String note;

    private static final String CREATED_AT = "created_at";
    private long createdAt;

    private static final String UPDATED_AT = "updated_at";
    private long updatedAt;

}
