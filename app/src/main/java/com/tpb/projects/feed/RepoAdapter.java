package com.tpb.projects.feed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.auth.GitHubSession;
import com.tpb.projects.data.auth.models.Repository;
import com.tpb.projects.util.Constants;
import com.tpb.projects.util.Data;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 14/12/16.
 */

public class RepoAdapter extends RecyclerView.Adapter<RepoAdapter.RepoHolder> implements Loader.RepositoryLoader {
    private static final String TAG = RepoAdapter.class.getSimpleName();

    private Context mContext;
    private Loader mLoader;
    private SwipeRefreshLayout mRefresher;
    private Repository[] mRepos = new Repository[0];
    private String mUser;

    public RepoAdapter(Context context, SwipeRefreshLayout refresher) {
        mContext = context;
        mLoader = new Loader(context);
        mLoader.loadRepositories(this);
        mRefresher = refresher;
        mRefresher.setRefreshing(true);
        mRefresher.setOnRefreshListener(() -> {
            mRepos = new Repository[0];
            notifyDataSetChanged();
            mLoader.loadRepositories(RepoAdapter.this);
        });
        mUser = new GitHubSession(context).getUsername();
    }

    /*
    Use the ItemTouchHelper.Simplecallback methods and override getMoveThreshold to either
    drag in the recyclerview or drag outside of the view
     */

    @Override
    public RepoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RepoHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_repo, parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(RepoHolder holder, int position) {
        final int pos = holder.getAdapterPosition();
        final Repository r = mRepos[pos];
        holder.mName.setText(r.getFullName().contains(mUser) ? r.getName() : r.getFullName());
        if(r.getDescription() != null && !r.getDescription().equals(Constants.JSON_NULL)) {
            holder.mDescription.setText(r.getDescription());
        } else {
            holder.mDescription.setText(null);
        }
        holder.mForks.setText(Integer.toString(r.getForks()));
        holder.mStars.setText(Integer.toString(r.getStarGazers()));
    }

    @Override
    public int getItemCount() {
        return mRepos.length;
    }

    @Override
    public void reposLoaded(Repository[] repos) {
        Log.i(TAG, "reposLoaded: " + repos.length);
        mRepos = repos;
        Arrays.sort(mRepos, Data.repoAlphaSort);
        mRefresher.setRefreshing(false);
        notifyDataSetChanged();
    }

    @Override
    public void loadError() {

    }



    class RepoHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.repo_name) TextView mName;
        @BindView(R.id.repo_description) TextView mDescription;
        @BindView(R.id.repo_forks) TextView mForks;
        @BindView(R.id.repo_stars) TextView mStars;
        @BindView(R.id.repo_private) TextView mPrivate;

        public RepoHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

    }

}
