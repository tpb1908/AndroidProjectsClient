/*
 * Copyright  2016 Theo Pearson-Bray
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.tpb.projects.project;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.util.Data;
import com.tpb.projects.util.FuzzyStringSearcher;

import java.util.ArrayList;

/**
 * Created by theo on 02/02/17.
 */

public class ProjectSearchAdapter extends ArrayAdapter<Card> {
    private static final String TAG = ProjectSearchAdapter.class.getSimpleName();

    private ArrayList<Card> data;
    private Spanned[] parseCache;
    private ArrayList<Card> filtered;
    private ArrayFilter mFilter;
    private FuzzyStringSearcher mSearcher;

    public ProjectSearchAdapter(Context context, @NonNull ArrayList<Card> data) {
        super(context, R.layout.viewholder_search_suggestion, data);
        this.data = data;
        filtered = new ArrayList<>();
        parseCache = new Spanned[data.size()];
        final ArrayList<String> strings = new ArrayList<>();
        for(Card c : data) {

            if(c.hasIssue()) {
                strings.add("#" + c.getIssue().getNumber() + "\n" + c.getIssue().getTitle() + "\n" + c.getIssue().getBody());
            } else {
                strings.add(c.getNote());
            }
        }
        mSearcher = FuzzyStringSearcher.getInstance(strings);
    }


    @Override
    public long getItemId(int position) {
        return filtered.get(position).getId();
    }

    @Nullable
    @Override
    public Card getItem(int position) {
        return filtered.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        if(mFilter == null) {
            mFilter = new ArrayFilter();
        }
        return mFilter;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_search_suggestion, parent, false);
        }
        bindView(position, convertView);
        return convertView;
    }

    private void bindView(int pos, View view) {
        final int dataPos = data.indexOf(filtered.get(pos));
        if(parseCache[dataPos] == null) {
            if(data.get(dataPos).hasIssue()) {
                parseCache[dataPos] = Html.fromHtml(" #" + data.get(dataPos).getIssue().getNumber() + " "  +  Data.parseMD(data.get(dataPos).getIssue().getTitle()));
            } else {
                parseCache[dataPos] = Html.fromHtml(Data.formatMD(data.get(dataPos).getNote(), null));
            }
        }
        if(data.get(pos).hasIssue()) {
            ((TextView) view.findViewById(R.id.suggestion_text)).setCompoundDrawablesRelativeWithIntrinsicBounds(data.get(pos).getIssue().isClosed() ? R.drawable.ic_issue_closed : R.drawable.ic_issue_open, 0, 0, 0);
            ((TextView) view.findViewById(R.id.suggestion_text)).setText(parseCache[dataPos]);
        } else {
            ((TextView) view.findViewById(R.id.suggestion_text)).setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            Log.i(TAG, "bindView: Setting text " + parseCache[dataPos]);
            ((TextView) view.findViewById(R.id.suggestion_text)).setText(parseCache[dataPos]);
        }
    }

    @Override
    public int getCount() {
        return filtered.size();
    }

    private class ArrayFilter extends Filter {

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            filtered = (ArrayList<Card>) filterResults.values;
            if(filterResults.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            final FilterResults results = new FilterResults();
            final ArrayList<Integer> positions = mSearcher.search(charSequence.toString());
            final ArrayList<Card> items = new ArrayList<>(positions.size());

            for(int i : positions) {
                items.add(data.get(i));
            }
            results.values = items;
            results.count = items.size();

            return results;
        }
    }
}