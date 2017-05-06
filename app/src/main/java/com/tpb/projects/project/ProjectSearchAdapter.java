package com.tpb.projects.project;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import com.tpb.github.data.models.Card;
import com.tpb.github.data.models.Label;
import com.tpb.projects.R;
import com.tpb.projects.util.search.ArrayFilter;
import com.tpb.projects.util.search.FuzzyStringSearcher;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

/**
 * Created by theo on 02/02/17.
 */

class ProjectSearchAdapter extends ArrayAdapter<Card> {
    private static final String TAG = ProjectSearchAdapter.class.getSimpleName();

    private final List<Card> data;
    private ArrayFilter<Card> mFilter;
    private final FuzzyStringSearcher mSearcher;

    public ProjectSearchAdapter(Context context, @NonNull List<Card> data) {
        super(context, R.layout.viewholder_search_suggestion, data);
        this.data = data;
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
            convertView = LayoutInflater.from(parent.getContext())
                                        .inflate(R.layout.viewholder_search_suggestion, parent,
                                                false
                                        );
        }
        bindView(position, convertView);
        return convertView;
    }

    private void bindView(int pos, View view) {
        final int dp = data.indexOf(mFilter.getFiltered().get(pos));
        final String text;
        final Card c = data.get(dp);
        if(c.hasIssue()) {
            text = " #" + c.getIssue().getNumber() + " " + c.getIssue()
                                                                          .getTitle();
        } else {
            text = c.getNote();
        }
        final TextView tv = ButterKnife.findById(view, R.id.suggestion_text);
        tv.setText(text);
        if(c.hasIssue()) {
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            c.getIssue().isClosed() ?
                                    R.drawable.ic_state_closed : R.drawable.ic_state_open,
                            0, 0, 0
                    );
        } else {
            tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    @Override
    public int getCount() {
        return mFilter.getFiltered().size();
    }

}