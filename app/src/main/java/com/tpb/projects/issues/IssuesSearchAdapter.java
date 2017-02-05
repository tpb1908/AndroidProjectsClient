package com.tpb.projects.issues;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.util.ArrayFilter;
import com.tpb.projects.util.Data;
import com.tpb.projects.util.FuzzyStringSearcher;

import java.util.ArrayList;

/**
 * Created by theo on 05/02/17.
 */

public class IssuesSearchAdapter extends ArrayAdapter<Issue> {

    private ArrayList<Issue> data;
    private ArrayFilter<Issue> mFilter;
    private Spanned[] parseCache;
    private FuzzyStringSearcher mSearcher;

    public IssuesSearchAdapter(Context context, @NonNull ArrayList<Issue> data) {
        super(context, R.layout.viewholder_search_suggestion, data);
        this.data = data;
        this.parseCache = new Spanned[data.size()];
        final ArrayList<String> strings = new ArrayList<>();
        String s;
        for(Issue i : data) {
            s = "#" + i.getNumber();
            for(Label l : i.getLabels()) s += "\n" +  l.getName();
            s += i.getTitle() + "\n" + i.getBody();
            strings.add(s);
        }
        mSearcher = FuzzyStringSearcher.getInstance(strings);
    }

    @Override
    public long getItemId(int position) {
        return mFilter.getFiltered().get(position).getId();
    }

    @Nullable
    @Override
    public Issue getItem(int position) {
        return mFilter.getFiltered().get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        if(mFilter == null) {
            mFilter = new ArrayFilter<>(this, mSearcher, data);
        }
        return mFilter;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_search_suggestion, parent, false);
        }
        bindView(position, convertView);
        return convertView;
    }

    private void bindView(int pos, View view) {
        final int dataPos = data.indexOf(mFilter.getFiltered().get(pos));
        if(parseCache[dataPos] == null) {
            parseCache[dataPos] = Html.fromHtml(" #" + data.get(dataPos).getNumber() + " " + Data.parseMD(data.get(dataPos).getTitle()));
        }

        ((TextView) view.findViewById(R.id.suggestion_text)).setCompoundDrawablesRelativeWithIntrinsicBounds(data.get(dataPos).isClosed() ? R.drawable.ic_issue_closed : R.drawable.ic_issue_open, 0, 0, 0);
        ((TextView) view.findViewById(R.id.suggestion_text)).setText(parseCache[dataPos]);

    }

    @Override
    public int getCount() {
        return mFilter.getFiltered().size();
    }
}
