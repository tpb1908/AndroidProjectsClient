package com.tpb.projects.feed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
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
    private RepoPinSorter mSorter;

    public RepoAdapter(Context context, RecyclerView recycler, SwipeRefreshLayout refresher) {
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
        mSorter = new RepoPinSorter(context, mUser);
        new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                        ItemTouchHelper.DOWN | ItemTouchHelper.UP);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                final Repository r = mRepos[viewHolder.getAdapterPosition()];
                mRepos[viewHolder.getAdapterPosition()] = mRepos[target.getAdapterPosition()];
                mRepos[target.getAdapterPosition()] = r;
                notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                mSorter.savePosition(mRepos);
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

            }

        }).attachToRecyclerView(recycler);
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
        mSorter.sort(mRepos);
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

        RepoHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

    }

    private static class RepoPinSorter {
        private static final String TAG = RepoPinSorter.class.getSimpleName();

        private SharedPreferences prefs;
        private static final String PREFS_KEY = "PINS";
        private final String KEY;
        private int[] pinnedRepos;

        RepoPinSorter(Context context, String key) {
            prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
            KEY = key;
            pinnedRepos = Data.intArrayFromPrefs(prefs.getString(KEY, ""));
        }

        void savePosition(Repository[] repos) {
            final int[] ids = new int[repos.length];
            for(int i = 0; i < ids.length; i++) ids[i] = repos[i].getId();
            pinnedRepos = ids;
            prefs.edit().putString(KEY, Data.intArrayForPrefs(ids)).apply();
        }

        void sort(Repository[] repos) {
            Arrays.sort(repos, (r1, r2) -> {
                final int i1 = Data.indexOf(pinnedRepos, r1.getId());
                final int i2 = Data.indexOf(pinnedRepos, r2.getId());
                return i1 > i2 ? 1 : i1 == i2 ? Data.repoAlphaSort.compare(r1, r2) : -1;
            });
        }

    }

}
