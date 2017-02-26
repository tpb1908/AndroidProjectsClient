package com.tpb.projects.data.models;

/**
 * Created by theo on 24/02/17.
 */

public enum State {

    OPEN, CLOSED, ALL;

    public static State fromString(String s) {
        if("open".equalsIgnoreCase(s)) return OPEN;
        if("closed".equalsIgnoreCase(s)) return CLOSED;
        if("all".equalsIgnoreCase(s)) return ALL;
        throw new IllegalArgumentException("Invalid value for state: " + s);
    }

}
