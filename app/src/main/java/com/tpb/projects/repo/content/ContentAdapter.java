package com.tpb.projects.repo.content;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tpb.github.data.APIHandler;
import com.tpb.github.data.FileLoader;
import com.tpb.github.data.Loader;
import com.tpb.github.data.models.content.Node;
import com.tpb.projects.R;
import com.tpb.projects.repo.RepoActivity;
import com.tpb.projects.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 17/02/17.
 */

public class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.NodeViewHolder> implements Loader.ListLoader<Node> {
    private static final String TAG = ContentAdapter.class.getSimpleName();

    private List<Node> mRootNodes = new ArrayList<>();
    private List<Node> mCurrentNodes = new ArrayList<>();
    private Node mPreviousNode;

    private final ContentActivity mParent;
    private final String mRepo;
    private final FileLoader mLoader;
    private boolean mIsLoading = false;
    private String mRef;

    ContentAdapter(FileLoader loader, ContentActivity parent, String repo, @Nullable String path) {
        mLoader = loader;
        mParent = parent;
        mRepo = repo;
        mParent.mRefresher.setRefreshing(true);
        mIsLoading = true;
        mLoader.loadDirectory(this, repo, path, null, mRef);
    }

    @Override
    public NodeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new NodeViewHolder(LayoutInflater.from(parent.getContext())
                                                .inflate(R.layout.viewholder_node, parent, false));
    }

    @Override
    public void onBindViewHolder(NodeViewHolder holder, int position) {
        if(mCurrentNodes.get(position).getType() == Node.NodeType.SYMLINK) {
            holder.mText.setText(mCurrentNodes.get(position).getPath());
        } else {
            holder.mText.setText(mCurrentNodes.get(position).getName());
        }
        if(mCurrentNodes.get(position).getType() == Node.NodeType.FILE) {
            holder.mText
                    .setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_file, 0, 0, 0);
            holder.mSize.setText(Util.formatBytes(mCurrentNodes.get(position).getSize()));
        } else {
            holder.mText
                    .setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_folder, 0, 0, 0);
            holder.mSize.setText("");
        }

    }

    void setRef(String ref) {
        if(mRef == null) {
            mRef = ref;
        } else {
            mRef = ref;
            mPreviousNode = null;
            reload();
        }


    }

    void reload() {
        mCurrentNodes.clear();
        notifyDataSetChanged();
        if(mPreviousNode == null) {
            mLoader.loadDirectory(this, mRepo, null, null, mRef);
        } else {
            mLoader.loadDirectory(this, mRepo, mPreviousNode.getPath(), mPreviousNode, mRef);
        }
    }

    void moveToStart() {
        mCurrentNodes = mRootNodes;
        mPreviousNode = null;
        notifyDataSetChanged();
    }

    void moveTo(Node node) {
        mPreviousNode = node;
        mCurrentNodes = node.getChildren();
        notifyDataSetChanged();
    }

    void moveBack() {
        /*
        If we are at the root, mPreviousNode is null
        If we are one layer down, mPreviousNode is non null, but its parent is null
        If we are further down, both mPreviousNode and its parent are non null
         */
        if(mPreviousNode != null) {
            if(mPreviousNode.getParent() == null) {
                mPreviousNode = mPreviousNode.getParent();
                mCurrentNodes = mRootNodes;
                notifyDataSetChanged();
            } else {
                mCurrentNodes = mPreviousNode.getParent().getChildren();
                mPreviousNode = mPreviousNode.getParent();
                notifyDataSetChanged();
            }
        }

    }

    private void loadNode(int pos) {
        if(mIsLoading) return;
        final Node node = mCurrentNodes.get(pos);
        if(node.getType() == Node.NodeType.FILE) {
            ContentActivity.mLaunchNode = node;
            final Intent file = new Intent(mParent, FileActivity.class);
            mParent.startActivity(file);
        } else if(node.getType() == Node.NodeType.SUBMODULE) {
            final Intent i = new Intent(mParent, RepoActivity.class);
            String path = node.getHtmlUrl();
            path = path.substring(path.indexOf("com/") + 4, path.indexOf("/tree"));
            i.putExtra(mParent.getString(R.string.intent_repo), path);
            mParent.startActivity(i);
        } else {
            mParent.addRibbonItem(node);
            mPreviousNode = node;
            mParent.mRefresher.setRefreshing(true);
            mIsLoading = true;
            if(node.getChildren().size() == 0) {
                mLoader.loadDirectory(this, mRepo, node.getPath(), node, mRef);
            } else {
                mParent.mRefresher.setRefreshing(true);
                listLoadComplete(node.getChildren());
            }
        }
    }

    private Loader.ListLoader<Node> backgroundLoader = new Loader.ListLoader<Node>() {
        @Override
        public void listLoadComplete(List<Node> directory) {
            if(directory.size() == 0) return;
            final Node parent = directory.get(0).getParent();
            for(Node n : mCurrentNodes) { //Most likely here
                if(parent.equals(n)) {
                    n.setChildren(directory);
                    return;
                }
            }

            final Stack<Node> stack = new Stack<>();
            Node current;
            for(Node n : mRootNodes) {
                stack.push(n);
                while(!stack.isEmpty()) {
                    current = stack.pop();
                    for(Node child : current.getChildren()) {
                        if(parent.equals(child)) {
                            parent.setChildren(directory);
                            return;
                        }
                        stack.push(child);
                    }
                }
            }
        }

        @Override
        public void listLoadError(APIHandler.APIError error) {

        }
    };

    @Override
    public void listLoadComplete(List<Node> directory) {
        if(mPreviousNode == null) { //We are at the root
            mRootNodes = directory;
            mCurrentNodes = directory;
            mPreviousNode = null;
            notifyItemRangeInserted(0, mCurrentNodes.size());
            if(mCurrentNodes.size() > 0) mParent.setDefaultRef(mCurrentNodes.get(0).getRef());
        } else {
            mPreviousNode.setChildren(directory);
            mCurrentNodes = directory;
            notifyDataSetChanged();
        }
        mIsLoading = false;
        mParent.mRefresher.setRefreshing(false);
        for(Node n : directory) {
            if(n.getType() == Node.NodeType.DIRECTORY && n.getChildren().size() == 0) {
                mLoader.loadDirectory(backgroundLoader, mRepo, n.getPath(), n, mRef);
            }
        }
    }

    @Override
    public void listLoadError(APIHandler.APIError error) {
        mIsLoading = false;
        mParent.mRefresher.setRefreshing(false);
    }

    @Override
    public int getItemCount() {
        return mCurrentNodes.size();
    }

    class NodeViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.node_text) TextView mText;
        @BindView(R.id.node_size) TextView mSize;

        NodeViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener((v) -> loadNode(getAdapterPosition()));
        }
    }

}
