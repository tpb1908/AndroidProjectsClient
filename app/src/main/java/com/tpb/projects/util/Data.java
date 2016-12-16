package com.tpb.projects.util;

import com.tpb.projects.data.auth.models.Repository;

import java.util.Comparator;

/**
 * Created by theo on 16/12/16.
 */

public class Data {

    public static Comparator<Repository> repoAlphaSort = (r1, r2) -> r1.getName().compareToIgnoreCase(r2.getName());



}
