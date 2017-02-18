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
import java.util.Stack;

/**
 * Created by theo on 17/02/17.
 */

public class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.NodeViewHolder> implements FileLoader.DirectoryLoader {
    private static final String TAG = ContentAdapter.class.getSimpleName();

    private List<Node> mRootNodes = new ArrayList<>();
    private List<Node> mCurrentNodes = new ArrayList<>();
    private Node mPreviousNode;
    private int mCurrentDepth = 0;

    private ContentActivity mParent;
    private String mRepo;
    private FileLoader mLoader;

    ContentAdapter(FileLoader loader, ContentActivity parent, String repo, @Nullable String path) {
        mLoader = loader;
        mParent = parent;
        mRepo = repo;
        mLoader.loadDirectory(this, repo, path, null);
    }

    @Override
    public NodeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new NodeViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_node, parent, false));
    }

    @Override
    public void onBindViewHolder(NodeViewHolder holder, int position) {
        if(mCurrentNodes.get(position).getType() == Node.NodeType.SYMLINK) {
            holder.mText.setText(mCurrentNodes.get(position).getPath());
        } else {
            holder.mText.setText(mCurrentNodes.get(position).getName());
        }
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
        mCurrentDepth = 0;
        notifyDataSetChanged();
    }

    void moveBack() {
        /*
        If we are at the root, mPreviousNode is null
        If we are one layer down, mPreviousNode is non null, but its parent is null
        If we are further down, both mPreviousNode and its parent are non null
         */
        if(mPreviousNode == null) {
            Log.i(TAG, "moveBack: We are already at the root");
        } else if(mPreviousNode.getParent() == null) {
            mPreviousNode = mPreviousNode.getParent();
            mCurrentNodes = mRootNodes;
            mCurrentDepth = 0;
            notifyDataSetChanged();
        } else {
            mCurrentNodes = mPreviousNode.getParent().getChildren();
            mPreviousNode = mPreviousNode.getParent();
            mCurrentDepth = Data.instancesOf(mPreviousNode.getPath(), "/") + 1;
            notifyDataSetChanged();
        }

    }

    void appendNode(Node parent, List<Node> node) {
        //TODO Traverse and insert, checking current nodes first
    }

    private void loadNode(int pos) {
        final Node node  = mCurrentNodes.get(pos);
        if(node.getType() == Node.NodeType.FILE) {
            //TODO- Open file
        } else if(node.getType() == Node.NodeType.SUBMODULE) {
            //TODO Open the submodule in another instance
        } else {
            Log.i(TAG, "loadNode: Loading path " + node.getPath());
            mPreviousNode = node;
            if(node.getChildren() == null) {
                mParent.mRefresher.setRefreshing(true);
                mLoader.loadDirectory(this, mRepo, node.getPath(), node);
            } else {
                directoryLoaded(node.getChildren());
            }
        }
    }

    private FileLoader.DirectoryLoader backgroundLoader = new FileLoader.DirectoryLoader() {

        @Override
        public void directoryLoaded(List<Node> directory) {
            if(directory.size() > 0) {
                final Node parent = directory.get(0).getParent();
                boolean foundParent = false;
                for(Node n : mCurrentNodes) { //Most likely here
                    if(parent.equals(n)) {
                        Log.i(TAG, "directoryLoaded: Found parent");
                        n.setChildren(directory);
                        foundParent = true;
                        break;
                    }
                }
                if(!foundParent) {
                    final Stack<Node> stack =  new Stack<>();
                    Node current;
                    int depth = 0;
                    for(Node n : mRootNodes) {
                        stack.push(n);
                        while(!stack.isEmpty()) {
                            current = stack.pop();
                            if(current.getChildren() != null) {
                                for(Node child : current.getChildren()) {
                                    if(parent.equals(child)) {
                                        parent.setChildren(directory);
                                        return;
                                    }
                                    stack.push(child);
                                }
                                depth += 1;
                                Log.i(TAG, "directoryLoaded: Traversing at depth " + depth);
                            }
                        }
                    }
                }
            }

        }

        @Override
        public void directoryLoadError(APIHandler.APIError error) {

        }
    };

    @Override
    public void directoryLoaded(List<Node> directory) {
        if(mPreviousNode == null) { //We are at the root
            mRootNodes = directory;
            setNodes(directory);
        } else {
            mPreviousNode.setChildren(directory);
            mCurrentNodes = directory;
            notifyDataSetChanged();
        }
        mParent.mRefresher.setRefreshing(false);
        for(Node n : directory) {
            if(n.getType() == Node.NodeType.DIRECTORY && n.getChildren() == null) {
                mLoader.loadDirectory(backgroundLoader, mRepo, n.getPath(), n);
            }
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
