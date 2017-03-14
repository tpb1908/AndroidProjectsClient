package com.tpb.projects.util;

import android.support.v4.widget.SwipeRefreshLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by theo on 14/03/17.
 */

public class MultiOnRefreshListener implements SwipeRefreshLayout.OnRefreshListener {

    private ArrayList<WeakReference<SwipeRefreshLayout.OnRefreshListener>> mListeners = new ArrayList<>();

    @Override
    public void onRefresh() {
        final Iterator<WeakReference<SwipeRefreshLayout.OnRefreshListener>> iter = mListeners.iterator();
        while(iter.hasNext()) {
            final SwipeRefreshLayout.OnRefreshListener listener = iter.next().get();
            if(listener == null) {
                iter.remove();
            } else {
                listener.onRefresh();
            }
        }
    }

    public void addListener(SwipeRefreshLayout.OnRefreshListener listener) {
        mListeners.add(new WeakReference<>(listener));
    }

    public boolean removeListener(SwipeRefreshLayout.OnRefreshListener listener) {
        for(int i = 0; i < mListeners.size(); i++) {
            if(listener.equals(mListeners.get(i).get())) {
                mListeners.remove(i);
                return true;
            }
        }
        return false;
    }
}
