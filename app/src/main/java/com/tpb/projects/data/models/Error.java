package com.tpb.projects.data.models;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

/**
 * Created by theo on 15/12/16.
 */

public class Error extends GenericJson {

    @Key("resource")
    private String resource;

    @Key("field")
    private String field;

    @Key("code")
    private String code;

    public final String getResource() {
        return resource;
    }

    public final String getField() {
        return field;
    }

    public final String getCode() {
        return code;
    }

    @Override
    public Error clone() {
        return (Error) super.clone();
    }

    @Override
    public Error set(String fieldName, Object value) {
        return (Error) super.set(fieldName, value);
    }

}
