package com.tpb.projects.user;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 14/12/16.
 */

class UserReposAdapter extends RecyclerView.Adapter<UserReposAdapter.RepoHolder> implements Loader.GITModelsLoader<Repository> {
    private static final String TAG = UserReposAdapter.class.getSimpleName();

    private final Loader mLoader;
    private final SwipeRefreshLayout mRefresher;
    private final AnimatingRecycler mRecycler;
    private final ArrayList<Repository> mRepos = new ArrayList<>();
    private final String mAuthenticatedUser;
    private String mUser;
    private final RepoPinChecker mPinChecker;
    private final RepositoriesManager mManager;

    private int mPage = 1;
    private boolean mIsLoading = false;
    private boolean mMaxPageReached = false;

    UserReposAdapter(Context context, RepositoriesManager opener, AnimatingRecycler recycler, SwipeRefreshLayout refresher) {
        mLoader = new Loader(context);
        mManager = opener;
        mRecycler = recycler;
        mRefresher = refresher;
        mRefresher.setRefreshing(true);
        mRefresher.setOnRefreshListener(() -> {
            mRepos.clear();
            mPage = 1;
            mMaxPageReached = false;
            notifyDataSetChanged();
            loadReposForUser(true);
        });
        mPinChecker = new RepoPinChecker(context);
        mAuthenticatedUser = GitHubSession.getSession(context).getUserLogin();
    }

    void setUser(String user) {
        mUser = user;
    }


    void notifyBottomReached() {
        if(!mIsLoading && !mMaxPageReached) {
            mPage++;
            loadReposForUser(false);
        }
    }

    void loadReposForUser(boolean resetPage) {
        mIsLoading = true;
        mRefresher.setRefreshing(true);
        if(resetPage) {
            mPage = 1;
            mMaxPageReached = false;
        }
        if(mUser.equals(mAuthenticatedUser)) { //The session user
            mLoader.loadRepositories(this, mPage);
        } else {
            mLoader.loadRepositories(this, mUser, mPage);
        }
        mPinChecker.setKey(mUser);

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
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        if(r.getDescription() != null && !r.getDescription().equals(Constants.JSON_NULL)) {
            builder.append(r.getDescription());
        }
        if(r.getLanguage() != null && !r.getLanguage().equals(Constants.JSON_NULL)) {
            if(builder.length() > 0) builder.append("\n");
            builder.append(r.getLanguage(), new ForegroundColorSpan(Color.WHITE), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        holder.mDescription.setText(builder);
        final boolean isPinned = mPinChecker.isPinned(r.getFullName());
        holder.isPinned = isPinned;
        holder.mPin.setImageResource(isPinned ? R.drawable.ic_pinned : R.drawable.ic_not_pinned);
        holder.mForks.setText(Integer.toString(r.getForks()));
        holder.mStars.setText(Integer.toString(r.getStarGazers()));
    }

    private void togglePin(int pos) {
        final Repository r = mRepos.get(pos);
        if(mPinChecker.isPinned(r.getFullName())) {
            mRepos.remove(pos);
            final int newPos = mPinChecker.initialPosition(r.getFullName());
            mRepos.add(newPos, r);
            mPinChecker.unpin(r.getFullName());
            notifyItemMoved(pos, newPos);
        } else {
            mRepos.remove(pos);
            mRepos.add(0, r);
            mPinChecker.pin(r.getFullName());
            notifyItemMoved(pos, 0);

        }
    }

    @Override
    public int getItemCount() {
        return mRepos.size();
    }

    @Override
    public void loadComplete(Repository[] repos) {
        mRefresher.setRefreshing(false);
        mIsLoading = false;
        if(repos.length > 0) {
            int oldLength = mRepos.size();

            if(mPage == 1) {
                // mRecycler.enableAnimation();
                Log.i(TAG, "repositoriesLoaded: Setting repositories");
                mRepos.clear();
                for(Repository r : repos) {
                    if(mPinChecker.isPinned(r.getFullName())) {
                        mRepos.add(0, r);
                    } else {
                        mRepos.add(r);
                    }
                }
                mPinChecker.setInitialPositions(mRepos.toArray(new Repository[0]));
                ensureLoadOfPinnedRepos();
            } else {
                Log.i(TAG, "repositoriesLoaded: Adding repositories");
                for(Repository repo : repos) {
                    if(!mRepos.contains(repo)) mRepos.add(repo);
                }
                mPinChecker.appendInitialPositions(repos);
            }
            if(mPage == 1) {
                notifyDataSetChanged();
            } else {
                notifyItemRangeInserted(oldLength, mRepos.size());
            }
        } else {
            mMaxPageReached = true;
        }
    }

    @Override
    public void loadError(APIHandler.APIError error) {

    }

    private void ensureLoadOfPinnedRepos() {
        Log.i(TAG, "ensureLoadOfPinnedRepos: Ensuring that repos are loaded ");
        for(String repo : mPinChecker.findNonLoadedPinnedRepositories()) {
            Log.i(TAG, "ensureLoadOfPinnedRepos: Loading " + repo);
            mLoader.loadRepository(new Loader.GITModelLoader<Repository>() {
                @Override
                public void loadComplete(Repository data) {
                    if(!mRepos.contains(data)) {
                        mRepos.add(0, data);
                        mRecycler.disableAnimation();
                        mPinChecker.appendPinnedPosition(data.getFullName());
                        notifyItemInserted(0);
                    }
                }

                @Override
                public void loadError(APIHandler.APIError error) {

                }
            }, repo);
        }
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

    private class RepoPinChecker {

        private final SharedPreferences prefs;
        private static final String PREFS_KEY = "PINS";
        private String KEY;
        private final ArrayList<String> pins = new ArrayList<>();
        private final ArrayList<String> mInitialPositions = new ArrayList<>();

        RepoPinChecker(Context context) {
            prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        }

        void setKey(String key) {
            KEY = key;
            final String[] savedPins = Data.stringArrayFromPrefs(prefs.getString(KEY, ""));
            pins.clear();
            pins.addAll(Arrays.asList(savedPins));
            Log.i(TAG, "RepoPinChecker: Loaded pin positions " + pins.toString());
        }

        void pin(String path) {
            if(!pins.contains(path)) {
                pins.add(path);
                prefs.edit().putString(KEY, Data.stringArrayForPrefs(pins)).apply();
            }
        }

        void unpin(String path) {
            pins.remove(path);
            prefs.edit().putString(KEY, Data.stringArrayForPrefs(pins)).apply();
        }

        void setInitialPositions(Repository[] repos) {
            mInitialPositions.clear();
            for(Repository r : repos) mInitialPositions.add(r.getFullName());
        }

        void appendInitialPositions(Repository[] repos) {
            for(Repository r : repos) mInitialPositions.add(r.getFullName());
        }

        void appendPinnedPosition(String key) {
            mInitialPositions.add(0, key);
        }

        int initialPosition(String key) {
            return mInitialPositions.indexOf(key);
        }

        boolean isPinned(String path) {
            return pins.contains(path);
        }

        List<String> findNonLoadedPinnedRepositories() {
            final List<String> repos = new ArrayList<>();
            for(String pin : pins) {
                if(!mInitialPositions.contains(pin)) repos.add(pin);
            }
            return repos;
        }

    }

    interface RepositoriesManager {

        void openRepo(Repository repo, View view);


    }

}
