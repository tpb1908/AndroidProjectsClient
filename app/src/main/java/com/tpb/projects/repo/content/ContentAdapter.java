package com.tpb.projects.repo.content;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.models.files.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by theo on 17/02/17.
 */

public class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.NodeViewHolder> {

    private List<Node> root = new ArrayList<>();

    @Override
    public NodeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new NodeViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_node, parent, false));
    }

    @Override
    public void onBindViewHolder(NodeViewHolder holder, int position) {

    }

    void setNodes(List<Node> nodes) {
        root = nodes;
        notifyDataSetChanged();
    }

    void appendNode(Node parent, List<Node> node) {
        //TODO Traverse and insert
    }

    @Override
    public int getItemCount() {
        return root.size();
    }

    class NodeViewHolder extends RecyclerView.ViewHolder {

        private TextView mText;

        public NodeViewHolder(View itemView) {
            super(itemView);
            mText = (TextView) itemView.findViewById(R.id.node_text);
        }
    }

}
