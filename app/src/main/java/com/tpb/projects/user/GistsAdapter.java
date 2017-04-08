package com.tpb.projects.user;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Loader;
import com.tpb.github.data.auth.GitHubSession;
import com.tpb.github.data.models.Gist;
import com.tpb.projects.R;
import com.tpb.projects.common.NetworkImageView;
import com.tpb.projects.util.Util;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 11/03/17.
 */

public class GistsAdapter extends RecyclerView.Adapter<GistsAdapter.GistHolder> implements Loader.ListLoader<Gist> {

    private String mUser;
    private String mAuthenticatedUser;
    private boolean mIsShowingPublic;

    private ArrayList<Gist> mGists = new ArrayList<>();

    private int mPage = 1;
    private boolean mIsLoading = false;
    private boolean mMaxPageReached = false;

    private SwipeRefreshLayout mRefresher;
    private Loader mLoader;
    private GistOpener mOpener;

    public GistsAdapter(Context context, GistOpener opener, SwipeRefreshLayout refresher) {
        mLoader = new Loader(context);
        mOpener = opener;
        mRefresher = refresher;
        mRefresher.setRefreshing(true);
        mRefresher.setOnRefreshListener(() -> {
            mPage = 1;
            mMaxPageReached = false;
            final int oldSize = mGists.size();
            mGists.clear();
            notifyItemRangeRemoved(0, oldSize);
            loadGistsForUser(true);
        });
        mAuthenticatedUser = GitHubSession.getSession(context).getUserLogin();
    }

    public void setUser(String user, boolean isShowingPublic) {
        mUser = user;
        mIsShowingPublic = isShowingPublic;
        mGists.clear();
        loadGistsForUser(true);
    }

    public void notifyBottomReached() {
        if(!mIsLoading && !mMaxPageReached) {
            mPage++;
            loadGistsForUser(false);
        }
    }

    private void loadGistsForUser(boolean resetPage) {
        mIsLoading = true;
        mRefresher.setRefreshing(true);
        if(resetPage) {
            mPage = 1;
            mMaxPageReached = false;
        }
        if(mIsShowingPublic) {
            mLoader.loadGists(this, mUser, mPage);
        } else if(mUser.equals(mAuthenticatedUser)) { //The session user
            mLoader.loadGists(this, mPage);
        } else {
            mLoader.loadGists(this, mUser, mPage);
        }

    }

    private void open(View view, int pos) {
        mOpener.openGist(mGists.get(pos), view);
    }

    @Override
    public void listLoadComplete(List<Gist> gists) {
        mRefresher.setRefreshing(false);
        mIsLoading = false;
        if(gists.size() > 0) {
            int oldLength = mGists.size();
            if(mPage == 1) mGists.clear();
            mGists.addAll(gists);
            notifyItemRangeInserted(oldLength, mGists.size());
        } else {
            mMaxPageReached = true;
        }
    }

    @Override
    public void listLoadError(APIHandler.APIError error) {
        mIsLoading = false;
    }

    @Override
    public GistHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new GistHolder(LayoutInflater.from(parent.getContext())
                                            .inflate(R.layout.viewholder_gist, parent, false));
    }

    @Override
    public void onBindViewHolder(GistHolder holder, int position) {
        final Gist g = mGists.get(position);
        if(mIsShowingPublic) {
            holder.mTitle.setText(
                    String.format(
                            holder.itemView.getResources()
                                           .getString(R.string.text_gist_viewholder_title),
                            g.getOwner().getLogin(),
                            g.getFiles()[0].getName()
                    )
            );
            holder.mAvatar.setImageUrl(g.getOwner().getAvatarUrl());
        } else {
            holder.mTitle.setText(g.getFiles()[0].getName());
            holder.mAvatar.setVisibility(View.GONE);
        }
        if(Util.isNotNullOrEmpty(g.getDescription())) {
            holder.mInfo.setVisibility(View.VISIBLE);
            holder.mInfo.setText(g.getDescription());
        } else {
            holder.mInfo.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mGists.size();
    }

    class GistHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.gist_title) TextView mTitle;
        @BindView(R.id.gist_info) TextView mInfo;
        @BindView(R.id.gist_user_avatar) NetworkImageView mAvatar;

        GistHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(v -> open(itemView, getAdapterPosition()));
        }


    }

    public interface GistOpener {

        void openGist(Gist gist, View view);

    }
}
