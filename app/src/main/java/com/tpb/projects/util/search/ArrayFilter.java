package com.tpb.projects.util.search;

import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by theo on 05/02/17.
 */

public class ArrayFilter<T> extends Filter {

    private final ArrayAdapter<T> mParent;
    private final FuzzyStringSearcher mSearcher;
    private final List<T> mData;
    private List<T> mFiltered;

    public ArrayFilter(ArrayAdapter<T> parent, FuzzyStringSearcher searcher, List<T> data) {
        mParent = parent;
        mSearcher = searcher;
        mData = data;
        mFiltered = new ArrayList<>();
    }

    public List<T> getFiltered() {
        return mFiltered;
    }

    @Override
    protected FilterResults performFiltering(CharSequence charSequence) {
        final FilterResults results = new FilterResults();
        if(charSequence == null) {
            results.values = new ArrayList<T>();
            results.count = 0;
        } else {
            final List<Integer> positions = mSearcher.search(charSequence.toString());
            final ArrayList<T> items = new ArrayList<>(positions.size());

            for(int i : positions) {
                items.add(mData.get(i));
            }
            results.values = items;
            results.count = items.size();
        }
        return results;
    }

    @Override
    protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
        mFiltered = (List<T>) filterResults.values;
        if(filterResults.count > 0) {
            mParent.notifyDataSetChanged();
        } else {
            mParent.notifyDataSetInvalidated();
        }
    }

}
