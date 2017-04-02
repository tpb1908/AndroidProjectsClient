package com.tpb.projects.user.fragments;

import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tpb.contributionsview.ContributionsLoader;
import com.tpb.contributionsview.ContributionsView;
import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Loader;
import com.tpb.github.data.models.User;
import com.tpb.projects.R;
import com.tpb.projects.common.NetworkImageView;
import com.tpb.projects.util.UI;
import com.tpb.projects.util.Util;

import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


/**
 * Created by theo on 10/03/17.
 */

public class UserInfoFragment extends UserFragment implements ContributionsView.ContributionsLoadListener {
    private static final String TAG = UserInfoFragment.class.getSimpleName();

    private Unbinder unbinder;

    @BindView(R.id.user_info_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.user_contributions) ContributionsView mContributions;
    @BindView(R.id.user_avatar) NetworkImageView mAvatar;
    @BindView(R.id.user_name) TextView mUserName;
    @BindView(R.id.user_info_layout) LinearLayout mInfoList;
    @BindView(R.id.user_contributions_info) TextView mContributionsInfo;

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
                       mAvatar.getViewTreeObserver().removeOnPreDrawListener(this);
                       if(getActivity().getIntent() != null && getActivity().getIntent().hasExtra(
                               getString(R.string.intent_drawable))) {
                           final Bitmap bm = getActivity().getIntent().getParcelableExtra(
                                   getString(R.string.intent_drawable));
                           mUserName.setText(getActivity().getIntent().getStringExtra(
                                   getString(R.string.intent_username)));
                           mAvatar.setImageBitmap(bm);
                       }
                       getActivity().startPostponedEnterTransition();
                       return true;
                   }
               });
        mRefresher.setOnRefreshListener(
                () -> new Loader(getContext()).loadUser(new Loader.ItemLoader<User>() {
                    @Override
                    public void loadComplete(User data) {
                        userLoaded(data);
                    }

                    @Override
                    public void loadError(APIHandler.APIError error) {
                        mRefresher.setRefreshing(false);
                    }
                }, getParent().getUser().getLogin()));

        mAreViewsValid = true;
        return view;
    }

    @Override
    public void userLoaded(User user) {
        if(getActivity() == null) return;
        mUser = user;
        mUserName.setText(user.getLogin());
        mAvatar.setImageUrl(user.getAvatarUrl());

        mContributions.setListener(this);
        mContributions.loadUser(user.getLogin());
        listUserInfo(user);
        mRefresher.setRefreshing(false);
    }

    private void listUserInfo(User user) {
        mInfoList.removeAllViews();
        TextView tv;
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, UI.pxFromDp(4), 0, UI.pxFromDp(4));
        if(user.getName() != null) {
            tv = getInfoTextView(R.drawable.ic_person);
            tv.setText(user.getName());
            mInfoList.addView(tv, params);
        }
        tv = getInfoTextView(R.drawable.ic_date);
        tv.setText(
                String.format(
                        getString(R.string.text_user_created_at),
                        Util.formatDateLocally(
                                getContext(),
                                new Date(user.getCreatedAt())
                        )
                )
        );
        mInfoList.addView(tv, params);
        if(user.getEmail() != null) {
            tv = getInfoTextView(R.drawable.ic_email);
            tv.setAutoLinkMask(Linkify.EMAIL_ADDRESSES);
            tv.setText(user.getEmail());
            mInfoList.addView(tv, params);
        }
        if(user.getBlog() != null) {
            tv = getInfoTextView(R.drawable.ic_blog);
            tv.setAutoLinkMask(Linkify.WEB_URLS);
            tv.setText(user.getBlog());
            mInfoList.addView(tv, params);
        }
        if(user.getCompany() != null) {
            tv = getInfoTextView(R.drawable.ic_company);
            tv.setText(user.getCompany());
            mInfoList.addView(tv, params);
        }
        if(user.getLocation() != null) {
            tv = getInfoTextView(R.drawable.ic_location);
            tv.setText(user.getLocation());
            mInfoList.addView(tv, params);
        }
        if(user.getRepos() > 0) {
            tv = getInfoTextView(R.drawable.ic_repo);
            tv.setText(getResources().getQuantityString(
                    R.plurals.text_user_repositories,
                    user.getRepos(),
                    user.getRepos()
                    )
            );
            mInfoList.addView(tv, params);
        }
        if(user.getGists() > 0) {
            tv = getInfoTextView(R.drawable.ic_gist);
            tv.setText(getResources().getQuantityString(
                    R.plurals.text_user_gists,
                    user.getGists(),
                    user.getGists()
                    )
            );
            mInfoList.addView(tv, params);
        }

        UI.expand(mInfoList);
    }

    private TextView getInfoTextView(@DrawableRes int drawableRes) {
        final TextView tv = new TextView(getContext());
        tv.setCompoundDrawablePadding(UI.pxFromDp(4));
        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(drawableRes, 0, 0, 0);
        return tv;
    }

    @Override
    public void contributionsLoaded(List<ContributionsLoader.GitDay> contributions) {
        if(!mAreViewsValid) return;
        int total = 0;
        int daysActive = 0;
        int daysTotal = contributions.size();
        int max = 0;
        int streak = 0;
        int maxStreak = 0;
        for(ContributionsLoader.GitDay gd : contributions) {
            total += gd.contributions;
            if(gd.contributions > 0) {
                daysActive += 1;
                if(gd.contributions > max) {
                    max = gd.contributions;
                }
                streak += 1;
            } else {
                if(streak > maxStreak) {
                    maxStreak = streak;
                }
                streak = 0;
            }
        }
        if(total > 0) {

            final String info = getResources()
                    .getQuantityString(R.plurals.text_user_contributions, total, total) +
                    "\n" +
                    String.format(getString(R.string.text_user_average),
                            (float) total / daysTotal
                    ) +
                    "\n" +
                    String.format(getString(R.string.text_user_average_active),
                            ((float) total / daysActive)
                    ) +
                    "\n" +
                    String.format(getString(R.string.text_user_max_contributions), max) +
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
