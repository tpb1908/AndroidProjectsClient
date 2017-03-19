package com.tpb.projects.user;

import android.app.Activity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.androidnetworking.widget.ANImageView;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.User;
import com.tpb.projects.flow.IntentHandler;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 19/03/17.
 */

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserHolder> implements Loader.GITModelsLoader<User> {

    private ArrayList<User> mUsers = new ArrayList<>();

    private boolean mIsShowingFollowers = false;
    private String mUser;

    private int mPage = 1;
    private boolean mIsLoading = false;
    private boolean mMaxPageReached = false;

    private SwipeRefreshLayout mRefresher;
    private Loader mLoader;

    private Activity mLauncher;

    public UserAdapter(Activity activity, SwipeRefreshLayout refresher) {
        mLauncher = activity;
        mLoader = new Loader(activity);
        mRefresher = refresher;
        mRefresher.setRefreshing(true);
        mRefresher.setOnRefreshListener(() -> {
            mPage = 1;
            mMaxPageReached = false;
            notifyDataSetChanged();
            loadUsers(true);
        });
    }

    public void setUser(String user, boolean isShowingFollowers) {
        mUser = user;
        mIsShowingFollowers = isShowingFollowers;
        mUsers.clear();
        loadUsers(true);
    }

    public void notifyBottomReached() {
        if(!mIsLoading && !mMaxPageReached) {
            mPage++;
            loadUsers(false);
        }
    }

    private void loadUsers(boolean resetPage) {
        mIsLoading = true;
        mRefresher.setRefreshing(true);
        if(resetPage) {
            mPage = 1;
            mMaxPageReached = false;
        }
        if(mIsShowingFollowers) {
            mLoader.loadFollowers(this, mUser, mPage);
        } else {
            mLoader.loadFollowing(this, mUser, mPage);
        }

    }

    @Override
    public void loadComplete(User[] users) {
        mRefresher.setRefreshing(false);
        mIsLoading = false;
        if(users.length > 0) {
            int oldLength = mUsers.size();
            if(mPage == 1) mUsers.clear();
            mUsers.addAll(Arrays.asList(users));
            notifyItemRangeInserted(oldLength, mUsers.size());
        } else {
            mMaxPageReached = true;
        }
    }

    @Override
    public void loadError(APIHandler.APIError error) {
        mIsLoading = false;
    }

    @Override
    public UserHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new UserHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_user, parent, false));
    }

    @Override
    public void onBindViewHolder(UserHolder holder, int position) {

        holder.mAvatar.setImageUrl(mUsers.get(position).getAvatarUrl());
        holder.mName.setText(mUsers.get(position).getLogin());
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    private void openUser(int pos, ANImageView iv) {
        IntentHandler.openUser(mLauncher, iv, mUsers.get(pos).getLogin());
    }

    class UserHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.user_avatar) ANImageView mAvatar;
        @BindView(R.id.user_name) TextView mName;

        UserHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(v -> openUser(getAdapterPosition(), mAvatar));
        }
    }

}
