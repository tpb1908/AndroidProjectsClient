package com.tpb.projects.repo.content;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.tpb.projects.data.models.files.Node;

import java.util.ArrayList;

/**
 * Created by theo on 17/02/17.
 */

public class ContentAdapter extends RecyclerView.Adapter {

    private ArrayList<Node> root = new ArrayList<>();

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
