package com.tpb.projects.project;

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
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.util.ArrayFilter;
import com.tpb.projects.util.FuzzyStringSearcher;
import com.tpb.projects.util.MDParser;

import java.util.ArrayList;

/**
 * Created by theo on 02/02/17.
 */

class ProjectSearchAdapter extends ArrayAdapter<Card> {
    private static final String TAG = ProjectSearchAdapter.class.getSimpleName();

    private final ArrayList<Card> data;
    private final Spanned[] parseCache;
    private ArrayFilter<Card> mFilter;
    private final FuzzyStringSearcher mSearcher;

    public ProjectSearchAdapter(Context context, @NonNull ArrayList<Card> data) {
        super(context, R.layout.viewholder_search_suggestion, data);
        this.data = data;
        parseCache = new Spanned[data.size()];
        final ArrayList<String> strings = new ArrayList<>();
        String s;
        for(Card c : data) {
            if(c.hasIssue()) {
                s = "#" + c.getIssue().getNumber();
                for(Label l : c.getIssue().getLabels()) s += "\n" + l.getName();
                strings.add(s + "\n" + c.getIssue().getBody());
            } else {
                strings.add(c.getNote());
            }
        }
        mSearcher = FuzzyStringSearcher.getInstance(strings);
    }


    @Override
    public long getItemId(int position) {
        return mFilter.getFiltered().get(position).getId();
    }

    @Nullable
    @Override
    public Card getItem(int position) {
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
            if(data.get(dataPos).hasIssue()) {
                parseCache[dataPos] = Html.fromHtml(" #" + data.get(dataPos).getIssue().getNumber() + " " + MDParser.parseMD(data.get(dataPos).getIssue().getTitle()));
            } else {
                parseCache[dataPos] = Html.fromHtml(MDParser.formatMD(data.get(dataPos).getNote(), null));
            }
        }
        if(data.get(dataPos).hasIssue()) {
            ((TextView) view.findViewById(R.id.suggestion_text)).setCompoundDrawablesRelativeWithIntrinsicBounds(data.get(dataPos).getIssue().isClosed() ? R.drawable.ic_issue_closed : R.drawable.ic_issue_open, 0, 0, 0);
            ((TextView) view.findViewById(R.id.suggestion_text)).setText(parseCache[dataPos]);
        } else {
            ((TextView) view.findViewById(R.id.suggestion_text)).setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            //Log.i(TAG, "bindView: Setting text " + parseCache[dataPos]);
            ((TextView) view.findViewById(R.id.suggestion_text)).setText(parseCache[dataPos]);
        }
    }

    @Override
    public int getCount() {
        return mFilter.getFiltered().size();
    }

}