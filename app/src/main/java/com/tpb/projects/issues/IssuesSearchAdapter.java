package com.tpb.projects.issues;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Spanned;
import android.widget.ArrayAdapter;

import com.tpb.projects.R;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.util.FuzzyStringSearcher;

import java.util.ArrayList;

/**
 * Created by theo on 05/02/17.
 */

public class IssuesSearchAdapter extends ArrayAdapter<Issue> {

    private ArrayList<Issue> data;
    private Spanned[] parseCache;
    private ArrayList<Issue> filtered;
    private FuzzyStringSearcher mSearcher;

    public IssuesSearchAdapter(Context context, @NonNull ArrayList<Issue> data) {
        super(context, R.layout.viewholder_search_suggestion, data);
    }


}
