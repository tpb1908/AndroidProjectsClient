package com.tpb.projects.repo.content;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.FileLoader;
import com.tpb.projects.data.models.files.Node;
import com.tpb.projects.util.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by theo on 17/02/17.
 */

public class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.NodeViewHolder> implements FileLoader.DirectoryLoader {
    private static final String TAG = ContentAdapter.class.getSimpleName();

    private List<Node> mCurrentNodes = new ArrayList<>();
    private Node mPreviousNode;

    private String mRepo;
    private FileLoader mLoader;

    ContentAdapter(FileLoader loader, String repo, @Nullable String path) {
        mLoader = loader;
        mRepo = repo;
        mLoader.loadDirectory(this, repo, path);
    }

    @Override
    public NodeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new NodeViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_node, parent, false));
    }

    @Override
    public void onBindViewHolder(NodeViewHolder holder, int position) {
        holder.mText.setText(mCurrentNodes.get(position).getName());
        if(mCurrentNodes.get(position).getType() == Node.NodeType.FILE) {
            holder.mText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_file, 0, 0, 0);
            holder.mSize.setText(Data.formatBytes(mCurrentNodes.get(position).getSize()));
        } else {
            holder.mText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_folder, 0, 0, 0);
            holder.mSize.setText("");
        }

    }

    private void setNodes(List<Node> nodes) {
        mCurrentNodes = nodes;
        mPreviousNode = null;
        notifyDataSetChanged();
    }

    void appendNode(Node parent, List<Node> node) {
        //TODO Traverse and insert, checking current nodes first
    }

    private void loadNode(int pos) {
        final Node node  = mCurrentNodes.get(pos);
        if(node.getType() == Node.NodeType.FILE) {
            //TODO- Open file
        } else {
            Log.i(TAG, "loadNode: Loading path " + node.getPath());
            mLoader.loadDirectory(this, mRepo, node.getPath());
        }
    }

    private FileLoader.DirectoryLoader backgroundLoader = new FileLoader.DirectoryLoader() {
        @Override
        public void directoryLoaded(List<Node> directory) {
            //TODO Traverse the whole tree and find where we need to add the node
        }

        @Override
        public void directoryLoadError(APIHandler.APIError error) {

        }
    };

    @Override
    public void directoryLoaded(List<Node> directory) {
        if(mPreviousNode == null) { //We are at the root
            setNodes(directory);
        } else {
            mPreviousNode.setChildren(directory);
            mCurrentNodes = directory;
            notifyDataSetChanged();
        }
    }

    @Override
    public void directoryLoadError(APIHandler.APIError error) {

    }

    @Override
    public int getItemCount() {
        return mCurrentNodes.size();
    }

    class NodeViewHolder extends RecyclerView.ViewHolder {

        private TextView mText;
        private TextView mSize;

        public NodeViewHolder(View itemView) {
            super(itemView);
            mText = (TextView) itemView.findViewById(R.id.node_text);
            mSize = (TextView) itemView.findViewById(R.id.node_size);
            itemView.setOnClickListener((v) -> loadNode(getAdapterPosition()));
        }
    }

}
