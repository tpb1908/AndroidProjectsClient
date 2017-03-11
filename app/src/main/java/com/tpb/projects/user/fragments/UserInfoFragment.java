package com.tpb.projects.user.fragments;

import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.Space;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androidnetworking.widget.ANImageView;
import com.tpb.contributionsview.ContributionsLoader;
import com.tpb.contributionsview.ContributionsView;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.User;
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
    @BindView(R.id.user_image) ANImageView mAvatar;
    @BindView(R.id.user_name) TextView mUserName;
    @BindView(R.id.user_info_layout) LinearLayout mInfoList;
    @BindView(R.id.user_contributions_info) TextView mContributionsInfo;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_user_info, container, false);
        unbinder = ButterKnife.bind(this, view);
        mRefresher.setRefreshing(true);
        mAvatar.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mAvatar.getViewTreeObserver().removeOnPreDrawListener(this);
                if(getActivity().getIntent() != null && getActivity().getIntent().hasExtra(getString(R.string.intent_drawable))) {
                    final Bitmap bm = getActivity().getIntent().getParcelableExtra(getString(R.string.intent_drawable));
                    mUserName.setText(getActivity().getIntent().getStringExtra(getString(R.string.intent_username)));
                    mAvatar.setBackgroundDrawable(new BitmapDrawable(getResources(), bm));
                }
                getActivity().startPostponedEnterTransition();
                return true;
            }
        });
        mRefresher.setOnRefreshListener(() -> {
            new Loader(getContext()).loadUser(new Loader.GITModelLoader<User>() {
                @Override
                public void loadComplete(User data) {
                    userLoaded(data);
                }

                @Override
                public void loadError(APIHandler.APIError error) {
                    mRefresher.setRefreshing(false);
                }
            }, getParent().getUser().getLogin());
        });

        mAreViewsValid = true;
        return view;
    }

    @Override
    public void userLoaded(User user) {
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
            tv = new TextView(getContext());
            tv.setText(user.getName());
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_person, 0, 0, 0);
            mInfoList.addView(tv, params);
        }
        tv = new TextView(getContext());
        tv.setText(
                String.format(
                    getString(R.string.text_user_created_at),
                    Util.formatDateLocally(
                        getContext(),
                        new Date(user.getCreatedAt())
                )
            )
        );
        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_date, 0, 0, 0);
        mInfoList.addView(tv, params);
        mInfoList.addView(new Space(getContext()));
        if(user.getEmail() != null) {
            tv = new TextView(getContext());
            tv.setAutoLinkMask(Linkify.EMAIL_ADDRESSES);
            tv.setText(user.getEmail());
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_email, 0, 0, 0);
            mInfoList.addView(tv, params);
        }
        if(user.getBlog() != null) {
            tv = new TextView(getContext());
            tv.setAutoLinkMask(Linkify.WEB_URLS);
            tv.setText(user.getBlog());
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_blog, 0, 0, 0);
            mInfoList.addView(tv, params);
        }
        if(user.getCompany() != null) {
            tv = new TextView(getContext());
            tv.setText(user.getCompany());
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_company, 0, 0, 0);
            mInfoList.addView(tv, params);
        }
        if(user.getLocation() != null) {
            tv = new TextView(getContext());
            tv.setText(user.getLocation());
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_location, 0, 0, 0);
            mInfoList.addView(tv, params);
        }
        UI.expand(mInfoList);
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
            String builder = String.format(getString(R.string.text_user_commits), total, daysTotal) +
                    "\n" +
                    String.format(getString(R.string.text_user_average), (float) total / daysTotal) +
                    "\n" +
                    String.format(getString(R.string.text_user_average_active), ((float) total / daysActive)) +
                    "\n" +
                    String.format(getString(R.string.text_user_max_commits), max) +
                    "\n" +
                    String.format(getString(R.string.text_user_streak), maxStreak);
            final boolean isEmpty = mContributionsInfo.getText().toString().isEmpty();
            mContributionsInfo.setText(builder);
            if(isEmpty) {
                ObjectAnimator.ofInt(
                        mContributionsInfo,
                        "maxLines",
                        0,
                        mContributionsInfo.getLineCount()
                ).setDuration(200).start();
            }
        } else {
            mContributionsInfo.setText(getString(R.string.text_user_no_commits));
        }
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
