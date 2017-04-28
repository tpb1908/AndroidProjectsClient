package com.tpb.projects.common;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Loader;
import com.tpb.github.data.auth.GitHubSession;
import com.tpb.github.data.models.Repository;
import com.tpb.github.data.models.State;
import com.tpb.mdtext.Markdown;
import com.tpb.mdtext.views.MarkdownTextView;
import com.tpb.projects.R;
import com.tpb.projects.flow.IntentHandler;
import com.tpb.projects.repo.RepoActivity;
import com.tpb.projects.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 14/12/16.
 */

public class RepositoriesAdapter extends RecyclerView.Adapter<RepositoriesAdapter.RepoHolder> implements Loader.ListLoader<Repository> {

    private final Loader mLoader;
    private final SwipeRefreshLayout mRefresher;
    private final ArrayList<Repository> mRepos = new ArrayList<>();
    private final String mAuthenticatedUser;
    private String mUser;
    private final RepoPinChecker mPinChecker;
    private final Activity mActivity;
    private int mPage = 1;
    private boolean mIsLoading = false;
    private boolean mMaxPageReached = false;

    private boolean mIsShowingStars = false;

    public RepositoriesAdapter(Activity activity, SwipeRefreshLayout refresher) {
        mActivity = activity;
        mLoader = Loader.getLoader(activity);
        mRefresher = refresher;
        mRefresher.setRefreshing(true);
        mRefresher.setOnRefreshListener(() -> {
            mPage = 1;
            mMaxPageReached = false;
            final int oldSize = mRepos.size();
            mRepos.clear();
            notifyItemRangeRemoved(0, oldSize);
            loadReposForUser(true);
        });
        mPinChecker = new RepoPinChecker(activity);
        mAuthenticatedUser = GitHubSession.getSession(activity).getUserLogin();
    }

    public void setUser(String user, boolean isShowingStars) {
        mUser = user;
        mIsShowingStars = isShowingStars;
        mRepos.clear();
        loadReposForUser(true);
    }

    public void notifyBottomReached() {
        if(!mIsLoading && !mMaxPageReached) {
            mPage++;
            loadReposForUser(false);
        }
    }

    private void loadReposForUser(boolean resetPage) {
        mIsLoading = true;
        mRefresher.setRefreshing(true);
        if(resetPage) {
            mPage = 1;
            mMaxPageReached = false;
        }
        if(mIsShowingStars) {
            mLoader.loadStarredRepositories(this, mUser, mPage);
        } else if(mUser.equals(mAuthenticatedUser)) { //The session user
            mLoader.loadRepositories(this, mPage);
        } else {
            mLoader.loadRepositories(this, mUser, mPage);
        }
        mPinChecker.setKey(mUser);

    }

    @Override
    public RepoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RepoHolder(LayoutInflater.from(parent.getContext())
                                            .inflate(R.layout.viewholder_repo, parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(RepoHolder holder, int position) {
        final int pos = holder.getAdapterPosition();
        final Repository r = mRepos.get(pos);
        holder.mName.setText(
                (r.getUserLogin().equals(mUser) ? r.getName() : r.getFullName()) + (r
                        .isFork() ? " (Forked) " : "")
        );
        if(Util.isNotNullOrEmpty(r.getLanguage())) {
            holder.mLanguage.setVisibility(View.VISIBLE);
            holder.mLanguage.setText(r.getLanguage());
        } else {
            holder.mLanguage.setVisibility(View.GONE);
        }
        if(Util.isNotNullOrEmpty(r.getDescription())) {
            holder.mDescription.setVisibility(View.VISIBLE);
            holder.mDescription.setMarkdown(Markdown.formatMD(r.getDescription(), r.getFullName()));
        } else {
            holder.mDescription.setVisibility(View.GONE);
        }
        if(mIsShowingStars) {
            holder.mImage.setImageUrl(r.getUserAvatarUrl());
            IntentHandler.addOnClickHandler(mActivity, holder.mImage, r.getUserLogin());
        } else {
            final boolean isPinned = mPinChecker.isPinned(r.getFullName());
            holder.isPinned = isPinned;
            holder.mImage
                    .setImageResource(isPinned ? R.drawable.ic_pinned : R.drawable.ic_not_pinned);
        }
        holder.mForks.setText(Integer.toString(r.getForks()));
        holder.mStars.setText(Integer.toString(r.getStarGazers()));
        holder.mLastUpdated.setText(DateUtils.getRelativeTimeSpanString(r.getUpdatedAt()));
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
    public void listLoadComplete(List<Repository> repos) {
        mRefresher.setRefreshing(false);
        mIsLoading = false;
        if(!repos.isEmpty()) {
            final int oldLength = mRepos.size();
            if(mIsShowingStars) {
                mRepos.addAll(repos);
            } else {
                insertPinnedRepos(repos);
            }
            notifyItemRangeInserted(oldLength, mRepos.size());

        } else {
            mMaxPageReached = true;
        }
    }

    private void insertPinnedRepos(List<Repository> repos) {
        if(mPage == 1) {
            for(Repository r : repos) {
                if(mPinChecker.isPinned(r.getFullName())) {
                    mRepos.add(0, r);
                } else {
                    mRepos.add(r);
                }
            }
            mPinChecker.setInitialPositions(mRepos);
            ensureLoadOfPinnedRepos();
        } else {
            for(Repository repo : repos) {
                if(!mRepos.contains(repo)) mRepos.add(repo);
            }
            mPinChecker.appendInitialPositions(repos);
        }
    }

    @Override
    public void listLoadError(APIHandler.APIError error) {
        mIsLoading = false;
        mRefresher.setRefreshing(false);
    }

    private void ensureLoadOfPinnedRepos() {
        for(String repo : mPinChecker.findNonLoadedPinnedRepositories()) {
            mLoader.loadRepository(new Loader.ItemLoader<Repository>() {
                @Override
                public void loadComplete(Repository data) {
                    if(!mRepos.contains(data)) {
                        mRepos.add(0, data);
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

    private void openItem(int pos) {
        final Repository repo = mRepos.get(pos);
        final Intent i = new Intent(mActivity, RepoActivity.class);
        i.putExtra(mActivity.getString(R.string.intent_repo), repo);
        Loader.getLoader(mActivity)
              .loadProjects(null, repo.getFullName())
              .loadIssues(null, repo.getFullName(), State.OPEN, null, null, 0)
              .loadProjects(null, repo.getFullName());
        mActivity.startActivity(i);
        mActivity.overridePendingTransition(R.anim.slide_up, R.anim.none);
    }

    class RepoHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.repo_name) TextView mName;
        @BindView(R.id.repo_description) MarkdownTextView mDescription;
        @BindView(R.id.repo_forks) TextView mForks;
        @BindView(R.id.repo_stars) TextView mStars;
        @BindView(R.id.repo_language) TextView mLanguage;
        @BindView(R.id.repo_last_updated) TextView mLastUpdated;
        @BindView(R.id.repo_icon) NetworkImageView mImage;
        private boolean isPinned = false;

        RepoHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(v -> openItem(getAdapterPosition()));
            mDescription.setOnClickListener(v -> openItem(getAdapterPosition()));
            if(!mIsShowingStars) {
                mImage.setOnClickListener((v) -> {
                    togglePin(getAdapterPosition());
                    isPinned = !isPinned;
                    mImage.setImageResource(
                            isPinned ? R.drawable.ic_pinned : R.drawable.ic_not_pinned);
                });
            }
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
            final String[] savedPins = Util.stringArrayFromPrefs(prefs.getString(KEY, ""));
            pins.clear();
            pins.addAll(Arrays.asList(savedPins));
        }

        void pin(String path) {
            if(!pins.contains(path)) {
                pins.add(path);
                prefs.edit().putString(KEY, Util.stringArrayForPrefs(pins)).apply();
            }
        }

        void unpin(String path) {
            pins.remove(path);
            prefs.edit().putString(KEY, Util.stringArrayForPrefs(pins)).apply();
        }

        void setInitialPositions(List<Repository> repos) {
            mInitialPositions.clear();
            for(Repository r : repos) mInitialPositions.add(r.getFullName());
        }

        void appendInitialPositions(List<Repository> repos) {
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

}
