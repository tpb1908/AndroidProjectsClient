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
import android.widget.ImageButton;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.auth.GitHubSession;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.util.Constants;
import com.tpb.projects.util.Data;
import com.tpb.projects.views.AnimatingRecycler;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 14/12/16.
 */

class ReposAdapter extends RecyclerView.Adapter<ReposAdapter.RepoHolder> implements Loader.RepositoriesLoader {
    private static final String TAG = ReposAdapter.class.getSimpleName();

    private Loader mLoader;
    private SwipeRefreshLayout mRefresher;
    private AnimatingRecycler mRecycler;
    private Repository[] mRepos = new Repository[0];
    private String mUser;
    private RepoPinSorter mSorter;
    private ReposManager mManager;

    ReposAdapter(Context context, ReposManager opener, AnimatingRecycler recycler, SwipeRefreshLayout refresher) {
        mLoader = new Loader(context);
        mLoader.loadRepositories(this);
        mManager = opener;
        mRecycler = recycler;
        mRefresher = refresher;
        mRefresher.setRefreshing(true);
        mRefresher.setOnRefreshListener(() -> {
            mRepos = new Repository[0];
            notifyDataSetChanged();
            mLoader.loadRepositories(ReposAdapter.this);
        });
        mUser = GitHubSession.getSession(context).getUsername();
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
                final int tpos = target.getAdapterPosition();
                mRepos[viewHolder.getAdapterPosition()] = mRepos[tpos];
                mRepos[tpos] = r;
                mSorter.savePosition(mRepos);
                notifyItemMoved(viewHolder.getAdapterPosition(), tpos);
                if(mSorter.isPinned(mRepos[tpos].getId())) {
                    ((RepoHolder) viewHolder).mPin.setImageResource(R.drawable.ic_pinned);
                } else {
                    ((RepoHolder) viewHolder).mPin.setImageResource(R.drawable.ic_not_pinned);
                }
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

            }

        }).attachToRecyclerView(recycler);
    }

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
        holder.mPin.setImageResource(mSorter.isPinned(mRepos[pos].getId()) ? R.drawable.ic_pinned : R.drawable.ic_not_pinned);
        holder.mForks.setText(Integer.toString(r.getForks()));
        holder.mStars.setText(Integer.toString(r.getStarGazers()));
    }

    private void togglePin(int pos) {
       //TODO Pin to top or move back to original pos

    }

    @Override
    public int getItemCount() {
        return mRepos.length;
    }

    @Override
    public void reposLoaded(Repository[] repos) {
        Log.i(TAG, "reposLoaded: " + repos.length);
        mRecycler.enableAnimation();
        mRepos = repos;
        mSorter.sort(mRepos);
        mRefresher.setRefreshing(false);
        notifyDataSetChanged();
        if(mRepos.length > 0) mManager.displayUserAvatar(mRepos[0].getUserAvatarUrl());
    }

    @Override
    public void loadError() {

    }

    private void openItem(View view, int pos) {
        mManager.openRepo(mRepos[pos], view);
    }

    class RepoHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.repo_name) TextView mName;
        @BindView(R.id.repo_description) TextView mDescription;
        @BindView(R.id.repo_forks) TextView mForks;
        @BindView(R.id.repo_stars) TextView mStars;
        @BindView(R.id.repo_private) TextView mPrivate;
        @BindView(R.id.repo_pin_button) ImageButton mPin;

        RepoHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener((v) -> ReposAdapter.this.openItem(view, getAdapterPosition()));
            mPin.setOnClickListener((v) -> togglePin(getAdapterPosition()));
        }

    }

    private class RepoPinSorter {

        private SharedPreferences prefs;
        private static final String PREFS_KEY = "PINS";
        private final String KEY;
        private int[] pinnedRepos;
        private int[] standardPositions;

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

        int initialPosition(int key) {
            return  Data.indexOf(standardPositions, key);
        }

        boolean isPinned(int key) {
            final int pinPos = Data.indexOf(pinnedRepos, key);
            return pinPos != -1 && pinPos < Data.indexOf(standardPositions, key);
        }

        void sort(Repository[] repos) {
            standardPositions = new int[repos.length];
            for(int i = 0; i < repos.length; i++) {
                standardPositions[i] = repos[i].getId();
            }
            Arrays.sort(repos, (r1, r2) -> {
                final int i1 = Data.indexOf(pinnedRepos, r1.getId());
                final int i2 = Data.indexOf(pinnedRepos, r2.getId());
                return i1 > i2 ? 1 : i1 == i2 ? Data.repoAlphaSort.compare(r1, r2) : -1;
            });
        }

    }

    interface ReposManager {

        void openRepo(Repository repo, View view);

        void displayUserAvatar(String userImagePath);

    }

}
