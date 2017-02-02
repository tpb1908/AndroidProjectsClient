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
import android.support.annotation.LayoutRes;
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

import com.tpb.projects.data.models.Card;
import com.tpb.projects.util.Data;

import java.util.ArrayList;

/**
 * Created by theo on 02/02/17.
 */

public class ProjectSearchAdapter extends ArrayAdapter<Card> {

    private ArrayList<Card> data;
    private Spanned[] parseCache;
    private ArrayList<Card> filtered;
    private ArrayFilter mFilter;
    private int mRes;

    public ProjectSearchAdapter(Context context, @LayoutRes int res, @NonNull ArrayList<Card> data) {
        super(context, res, data);
        this.data = data;
        parseCache = new Spanned[data.size()];
        mRes = res;
    }

    @Override
    public long getItemId(int position) {
        return data.get(position).getId();
    }

    @Nullable
    @Override
    public Card getItem(int position) {
        return data.get(position);
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
            convertView = LayoutInflater.from(parent.getContext()).inflate(mRes, parent, false);
        }
        bindView(position, convertView);
        return convertView;
    }

    private void bindView(int pos, View view) {
        final int dataPos = data.indexOf(data.get(pos));
        if(parseCache[dataPos] == null) {
            if(data.get(dataPos).hasIssue()) {
                parseCache[dataPos] = Html.fromHtml(Data.parseMD(data.get(dataPos).getIssue().getTitle()));
            } else {
                final String line = data.get(dataPos).getNote().substring(0, Math.min(data.get(dataPos).getNote().indexOf("\n"), data.get(dataPos).getNote().length()));
                parseCache[dataPos] = Html.fromHtml(Data.parseMD(line));
            }
        }
        if(data.get(pos).hasIssue()) {
            ((TextView) view.findViewById(android.R.id.text1)).setText(parseCache[dataPos]);
        } else {
            ((TextView) view.findViewById(android.R.id.text1)).setText(parseCache[dataPos]);
        }
    }

    @Override
    public int getCount() {
        return data.size();
    }

    private class ArrayFilter extends Filter {

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            if(filterResults.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetChanged();
            }
        }

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            final FilterResults results = new FilterResults();
            results.values = data;
            results.count = data.size();


            return results;
        }
    }
}