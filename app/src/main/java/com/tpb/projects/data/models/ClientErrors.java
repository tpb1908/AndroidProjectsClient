package com.tpb.projects.data.models;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

import java.util.List;

/**
 * Created by theo on 15/12/16.
 */

public class ClientErrors extends GenericJson {

    @Key("message")
    private String message;

    @Key("errors")
    private List<Error> errors;

    public final String getMessage() {
        return message;
    }

    public final List<Error> getErrors() {
        return errors;
    }

    @Override
    public ClientErrors clone() {
        return (ClientErrors) super.clone();
    }

    @Override
    public ClientErrors set(String fieldName, Object value) {
        return (ClientErrors) super.set(fieldName, value);
    }

}
