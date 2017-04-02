package com.tpb.projects.util.search;

import android.widget.ArrayAdapter;
import android.widget.Filter;

import com.tpb.projects.data.models.Card;

import java.util.ArrayList;

/**
 * Created by theo on 05/02/17.
 */

public class ArrayFilter<T> extends Filter {

    private final ArrayAdapter<T> parent;
    private final FuzzyStringSearcher searcher;
    private final ArrayList<T> data;
    private ArrayList<T> filtered;

    public ArrayFilter(ArrayAdapter<T> parent, FuzzyStringSearcher searcher, ArrayList<T> data) {
        this.parent = parent;
        this.searcher = searcher;
        this.data = data;
        this.filtered = new ArrayList<>();
    }

    public ArrayList<T> getFiltered() {
        return filtered;
    }

    @Override
    protected FilterResults performFiltering(CharSequence charSequence) {
        final FilterResults results = new FilterResults();
        if(charSequence == null) {
            results.values = new ArrayList<Card>();
            results.count = 0;
        } else {
            final ArrayList<Integer> positions = searcher.search(charSequence.toString());
            final ArrayList<T> items = new ArrayList<>(positions.size());

            for(int i : positions) {
                items.add(data.get(i));
            }
            results.values = items;
            results.count = items.size();
        }
        return results;
    }

    @Override
    protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
        filtered = (ArrayList<T>) filterResults.values;
        if(filterResults.count > 0) {
            parent.notifyDataSetChanged();
        } else {
            parent.notifyDataSetInvalidated();
        }
    }
}
