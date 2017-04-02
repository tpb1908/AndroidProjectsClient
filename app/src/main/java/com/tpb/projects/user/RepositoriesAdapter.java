package com.tpb.projects.user;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Loader;
import com.tpb.github.data.auth.GitHubSession;
import com.tpb.github.data.models.DataModel;
import com.tpb.github.data.models.Repository;
import com.tpb.projects.R;
import com.tpb.mdtext.Markdown;
import com.tpb.projects.util.Util;

import com.tpb.mdtext.mdtextview.MarkdownTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 14/12/16.
 */

public class RepositoriesAdapter extends RecyclerView.Adapter<RepositoriesAdapter.RepoHolder> implements Loader.ListLoader<Repository> {
    private static final String TAG = RepositoriesAdapter.class.getSimpleName();

    private final Loader mLoader;
    private final SwipeRefreshLayout mRefresher;
    private final ArrayList<Repository> mRepos = new ArrayList<>();
    private final String mAuthenticatedUser;
    private String mUser;
    private final RepoPinChecker mPinChecker;
    private final RepoOpener mManager;

    private int mPage = 1;
    private boolean mIsLoading = false;
    private boolean mMaxPageReached = false;

    private boolean mIsShowingStars = false;

    public RepositoriesAdapter(Context context, RepoOpener opener, SwipeRefreshLayout refresher) {
        mLoader = new Loader(context);
        mManager = opener;
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
        mPinChecker = new RepoPinChecker(context);
        mAuthenticatedUser = GitHubSession.getSession(context).getUserLogin();
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
        Log.i(TAG, "loadReposForUser: ");
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
        if(!DataModel.JSON_NULL.equals(r.getLanguage())) {
            holder.mLanguage.setVisibility(View.VISIBLE);
            holder.mLanguage.setText(r.getLanguage());
        } else {
            holder.mLanguage.setVisibility(View.GONE);
        }
        if(!DataModel.JSON_NULL.equals(r.getDescription())) {
            holder.mDescription.setVisibility(View.VISIBLE);
            holder.mDescription.setMarkdown(Markdown.formatMD(r.getDescription(), r.getFullName()));
        } else {
            holder.mDescription.setVisibility(View.GONE);
        }
        final boolean isPinned = mPinChecker.isPinned(r.getFullName());
        holder.isPinned = isPinned;
        holder.mPin.setImageResource(isPinned ? R.drawable.ic_pinned : R.drawable.ic_not_pinned);
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
        if(repos.size() > 0) {
            int oldLength = mRepos.size();
            if(mPage == 1) mRepos.clear();
            if(mIsShowingStars) {
                mRepos.addAll(repos);
            } else {
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
            notifyItemRangeInserted(oldLength, mRepos.size());

        } else {
            mMaxPageReached = true;
        }
    }

    @Override
    public void listLoadError(APIHandler.APIError error) {
        mIsLoading = false;
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

    private void openItem(View view, int pos) {
        mManager.openRepo(mRepos.get(pos), view);
    }

    class RepoHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.repo_name) TextView mName;
        @BindView(R.id.repo_description) MarkdownTextView mDescription;
        @BindView(R.id.repo_forks) TextView mForks;
        @BindView(R.id.repo_stars) TextView mStars;
        @BindView(R.id.repo_language) TextView mLanguage;
        @BindView(R.id.repo_last_updated) TextView mLastUpdated;
        @BindView(R.id.repo_pin_button) ImageButton mPin;
        private boolean isPinned = false;

        RepoHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            mDescription.setConsumeNonUrlClicks(false);
            view.setOnClickListener(
                    (v) -> RepositoriesAdapter.this.openItem(mName, getAdapterPosition()));
            if(mIsShowingStars) {
                mPin.setVisibility(View.GONE);
            } else {
                mPin.setOnClickListener((v) -> {
                    togglePin(getAdapterPosition());
                    isPinned = !isPinned;
                    mPin.setImageResource(
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
            Log.i(TAG, "RepoPinChecker: Loaded pin positions " + pins.toString());
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

    public interface RepoOpener {

        void openRepo(Repository repo, View view);


    }

}
