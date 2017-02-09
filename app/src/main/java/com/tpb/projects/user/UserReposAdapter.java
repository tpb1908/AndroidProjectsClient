/*
 * Copyright  2016 Theo Pearson-Bray
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.tpb.projects.user;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tpb.animatingrecyclerview.AnimatingRecycler;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.auth.GitHubSession;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.util.Constants;
import com.tpb.projects.util.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 14/12/16.
 */

class UserReposAdapter extends RecyclerView.Adapter<UserReposAdapter.RepoHolder> implements Loader.RepositoriesLoader {
    private static final String TAG = UserReposAdapter.class.getSimpleName();

    private final Loader mLoader;
    private final SwipeRefreshLayout mRefresher;
    private final AnimatingRecycler mRecycler;
    private ArrayList<Repository> mRepos = new ArrayList<>();
    private String mUser;
    private final RepoPinSorter mSorter;
    private final RepositoriesManager mManager;

    UserReposAdapter(Context context, RepositoriesManager opener, AnimatingRecycler recycler, SwipeRefreshLayout refresher) {
        mLoader = new Loader(context);
        mManager = opener;
        mRecycler = recycler;
        mRefresher = refresher;
        mRefresher.setRefreshing(true);
        mRefresher.setOnRefreshListener(() -> {
            mRepos.clear();
            notifyDataSetChanged();
            mLoader.loadRepositories(UserReposAdapter.this, mUser);
        });
        mSorter = new RepoPinSorter(context);
        mUser = GitHubSession.getSession(context).getUserLogin();
    }

    void loadReposForUser(String user) {
        if(user.equals(mUser)) { //The session user
            mLoader.loadRepositories(this);
        } else {
            mUser = user;
            mLoader.loadRepositories(this, user);
        }
        mSorter.setKey(mUser);

    }

    @Override
    public RepoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RepoHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_repo, parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(RepoHolder holder, int position) {
        final int pos = holder.getAdapterPosition();
        final Repository r = mRepos.get(pos);
        holder.mName.setText(r.getFullName().contains(mUser) ? r.getName() : r.getFullName());
        if(r.getDescription() != null && !r.getDescription().equals(Constants.JSON_NULL)) {
            holder.mDescription.setText(r.getDescription());
        } else {
            holder.mDescription.setText(null);
        }
        final boolean isPinned = mSorter.isPinned(r.getId());
        holder.isPinned = isPinned;
        holder.mPin.setImageResource(isPinned ? R.drawable.ic_pinned : R.drawable.ic_not_pinned);
        holder.mForks.setText(Integer.toString(r.getForks()));
        holder.mStars.setText(Integer.toString(r.getStarGazers()));
    }

    private void togglePin(int pos) {
        final Repository r = mRepos.get(pos);
        if(mSorter.isPinned(r.getId())) {
            mRepos.remove(pos);
            final int newPos = mSorter.initialPosition(r.getId());
            mRepos.add(newPos, r);
            mSorter.unpin(r.getId());
            notifyItemMoved(pos, newPos);
        } else {
            mRepos.remove(pos);
            mRepos.add(0, r);
            mSorter.pin(r.getId());
            notifyItemMoved(pos, 0);
        }
    }

    @Override
    public int getItemCount() {
        return mRepos.size();
    }

    @Override
    public void repositoriesLoaded(Repository[] repos) {
        Log.i(TAG, "repositoriesLoaded: " + repos.length);
        mRecycler.enableAnimation();
        mRepos = new ArrayList<>(Arrays.asList(repos));
        mSorter.sort(mRepos);
        mRefresher.setRefreshing(false);
        notifyDataSetChanged();
    }

    @Override
    public void repositoryLoadError(APIHandler.APIError error) {

    }

    private void openItem(View view, int pos) {
        mManager.openRepo(mRepos.get(pos), view);
    }

    class RepoHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.repo_name) TextView mName;
        @BindView(R.id.repo_description) TextView mDescription;
        @BindView(R.id.repo_forks) TextView mForks;
        @BindView(R.id.repo_stars) TextView mStars;
        @BindView(R.id.repo_pin_button) ImageButton mPin;
        private boolean isPinned = false;

        RepoHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener((v) -> UserReposAdapter.this.openItem(mName, getAdapterPosition()));
            mPin.setOnClickListener((v) -> {
                togglePin(getAdapterPosition());
                isPinned = !isPinned;
                mPin.setImageResource(isPinned ? R.drawable.ic_pinned : R.drawable.ic_not_pinned);
            });
        }

    }

    private class RepoPinSorter {

        private final SharedPreferences prefs;
        private static final String PREFS_KEY = "PINS";
        private String KEY;
        private final ArrayList<Integer> pins = new ArrayList<>();
        private int[] standardPositions;

        RepoPinSorter(Context context) {
            prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        }

        void setKey(String key) {
            KEY = key;
            final int[] savedPins = Data.intArrayFromPrefs(prefs.getString(KEY, ""));
            pins.clear();
            for(int i : savedPins) pins.add(i);
            Log.i(TAG, "RepoPinSorter: Loaded pin positions " + pins.toString());
        }

        void pin(int id) {
            if(!pins.contains(id)) {
                pins.add(id);
                prefs.edit().putString(KEY, Data.intArrayForPrefs(pins)).apply();
            }
        }

        void unpin(int id) {
            pins.remove(Integer.valueOf(id));
            prefs.edit().putString(KEY, Data.intArrayForPrefs(pins)).apply();
        }

        int initialPosition(int key) {
            return Data.indexOf(standardPositions, key);
        }

        boolean isPinned(int key) {
            return pins.contains(key);
        }

        void sort(ArrayList<Repository> repos) {
            standardPositions = new int[repos.size()];
            for(int i = 0; i < repos.size(); i++) {
                standardPositions[i] = repos.get(i).getId();
            }
            Collections.sort(repos, (r1, r2) -> {
                final int i1 =  pins.indexOf(r1.getId());
                final int i2 = pins.indexOf(r2.getId());
                return i1 < i2 ? 1 : i1 == i2 ? Data.repoAlphaSort.compare(r1, r2) : -1;
            });
        }

    }

    interface RepositoriesManager {

        void openRepo(Repository repo, View view);


    }

}
