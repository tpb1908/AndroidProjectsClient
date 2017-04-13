package com.tpb.projects.user.fragments;

import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tpb.contributionsview.ContributionsLoader;
import com.tpb.contributionsview.ContributionsView;
import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Editor;
import com.tpb.github.data.Loader;
import com.tpb.github.data.auth.GitHubSession;
import com.tpb.github.data.models.User;
import com.tpb.projects.R;
import com.tpb.projects.common.NetworkImageView;
import com.tpb.projects.markdown.Formatter;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


/**
 * Created by theo on 10/03/17.
 */

public class UserInfoFragment extends UserFragment implements ContributionsView.ContributionsLoadListener, Editor.UpdateListener<Boolean>, Loader.ItemLoader<Boolean> {
    private static final String TAG = UserInfoFragment.class.getSimpleName();

    private Unbinder unbinder;

    @BindView(R.id.user_login) TextView mUserLogin;
    @BindView(R.id.user_avatar) NetworkImageView mAvatar;
    @BindView(R.id.user_info_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.user_contributions) ContributionsView mContributions;
    @BindView(R.id.user_contributions_info) TextView mContributionsInfo;
    @BindView(R.id.user_details) LinearLayout mUserInfoParent;
    private Button mFollowButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_user_info, container, false);
        unbinder = ButterKnife.bind(this, view);
        mRefresher.setRefreshing(true);
        mAvatar.getViewTreeObserver()
               .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                   @Override
                   public boolean onPreDraw() {
                       if(getActivity().getIntent() != null && getActivity().getIntent().hasExtra(
                               getString(R.string.intent_drawable))) {
                           final Bitmap bm = getActivity().getIntent().getParcelableExtra(
                                   getString(R.string.intent_drawable));
                           mUserLogin.setText(getActivity().getIntent().getStringExtra(
                                   getString(R.string.intent_username)));
                           mAvatar.setImageBitmap(bm);
                       }
                       mAvatar.getViewTreeObserver().removeOnPreDrawListener(this);
                       getActivity().startPostponedEnterTransition();
                       return true;
                   }
               });
        mRefresher.setOnRefreshListener(
                () -> Loader.getLoader(getContext()).loadUser(new Loader.ItemLoader<User>() {
                    @Override
                    public void loadComplete(User user) {
                        userLoaded(user);
                    }

                    @Override
                    public void loadError(APIHandler.APIError error) {
                        mRefresher.setRefreshing(false);
                    }
                }, mUser.getLogin())
        );

        mAreViewsValid = true;
        if(mUser != null) userLoaded(mUser);
        return view;
    }

    @Override
    public void userLoaded(User user) {
        mUser = user;
        if(!areViewsValid()) return;
        mContributions.setListener(this);
        mContributions.loadContributions(user.getLogin());
        Formatter.displayUser(mUserInfoParent, mUser);

        if(!GitHubSession.getSession(getContext()).getUserLogin().equals(user.getLogin())) {
            Loader.getLoader(getContext()).checkIfFollowing(this, user.getLogin());
        }
        mRefresher.setRefreshing(false);
    }

    @Override
    public void loadComplete(Boolean isFollowing) {
        updated(isFollowing);
    }

    @Override
    public void loadError(APIHandler.APIError error) {
        mRefresher.setRefreshing(false);
    }

    @Override
    public void updated(Boolean isFollowing) {
        if(mFollowButton == null) {
            mFollowButton = new Button(getContext());
            mFollowButton.setBackground(null);
            mFollowButton.setLayoutParams(
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    ));
            mUserInfoParent.addView(mFollowButton);
        }

        if(isFollowing) {
            mFollowButton.setText(R.string.text_unfollow_user);
        } else {
            mFollowButton.setText(R.string.text_follow_user);
        }
        mFollowButton.setOnClickListener(v -> {
            mFollowButton.setEnabled(false);
            mRefresher.setRefreshing(true);
            if(isFollowing) {
                Editor.getEditor(getContext()).unfollowUser(UserInfoFragment.this, mUser.getLogin());
            } else {
                Editor.getEditor(getContext()).followUser(UserInfoFragment.this, mUser.getLogin());
            }
        });
        mFollowButton.setEnabled(true);
        mRefresher.setRefreshing(false);
    }

    @Override
    public void updateError(APIHandler.APIError error) {
        Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void contributionsLoaded(List<ContributionsLoader.ContributionsDay> contributions) {
        if(!areViewsValid()) return;
        int totalContributions = 0;
        int daysActive = 0;
        int maxContributions = 0;
        int streak = 0;
        int maxStreak = 0;
        for(ContributionsLoader.ContributionsDay gd : contributions) {
            if(gd.contributions > 0) {
                totalContributions += gd.contributions;
                daysActive += 1;
                if(gd.contributions > maxContributions) {
                    maxContributions = gd.contributions;
                }
                streak += 1;
                if(streak > maxStreak) {
                    maxStreak = streak;
                }
            } else {
                streak = 0;
            }
        }
        if(totalContributions > 0) {
            final String info = getResources()
                    .getQuantityString(R.plurals.text_user_contributions, totalContributions, totalContributions) +
                    "\n" +
                    String.format(getString(R.string.text_user_average),
                            (float) totalContributions / contributions.size()
                    ) +
                    "\n" +
                    String.format(getString(R.string.text_user_average_active),
                            ((float) totalContributions / daysActive)
                    ) +
                    "\n" +
                    String.format(getString(R.string.text_user_max_contributions), maxContributions) +
                    "\n" +
                    String.format(getString(R.string.text_user_streak), maxStreak);
            final boolean isEmpty = mContributionsInfo.getText().toString().isEmpty();
            mContributionsInfo.setText(info);
            if(isEmpty) {
                ObjectAnimator.ofInt(
                        mContributionsInfo,
                        "maxLines",
                        0,
                        mContributionsInfo.getLineCount()
                ).setDuration(200).start();
            }
        } else {
            mContributionsInfo.setText(getString(R.string.text_user_no_contributions));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
