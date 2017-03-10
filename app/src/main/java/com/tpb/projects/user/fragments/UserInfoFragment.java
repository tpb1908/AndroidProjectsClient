package com.tpb.projects.user.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androidnetworking.widget.ANImageView;
import com.tpb.contributionsview.ContributionsView;
import com.tpb.projects.R;
import com.tpb.projects.data.models.User;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by theo on 10/03/17.
 */

public class UserInfoFragment extends UserFragment {

    private Unbinder unbinder;

    @BindView(R.id.user_info_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.user_contributions) ContributionsView mContributions;
    @BindView(R.id.user_image) ANImageView mAvatar;
    @BindView(R.id.user_name) TextView mUserName;
    @BindView(R.id.user_info_layout) LinearLayout mInfoList;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_user_info, container, false);
        unbinder = ButterKnife.bind(this, view);
        mRefresher.setRefreshing(true);
        return view;
    }

    @Override
    public void userLoaded(User user) {
        mUserName.setText(user.getLogin());
        mAvatar.setImageUrl(user.getAvatarUrl());
        mContributions.loadUser(user.getLogin());
        listUserInfo(user);
        mRefresher.setRefreshing(false);
    }

    private void listUserInfo(User user) {
        mInfoList.removeAllViews();
        if(user.getName() != null) {
            final TextView tv = new TextView(getContext());
            tv.setText(user.getName());
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_person, 0, 0, 0);
            mInfoList.addView(tv);
        }
        if(user.getEmail() != null) {
            final TextView tv = new TextView(getContext());
            tv.setAutoLinkMask(Linkify.EMAIL_ADDRESSES);
            tv.setText(user.getEmail());
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_email, 0, 0, 0);
            mInfoList.addView(tv);
        }
        if(user.getBlog() != null) {
            final TextView tv = new TextView(getContext());
            tv.setAutoLinkMask(Linkify.WEB_URLS);
            tv.setText(user.getBlog());
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_blog, 0, 0, 0);
            mInfoList.addView(tv);
        }
        if(user.getCompany() != null) {
            final TextView tv = new TextView(getContext());
            tv.setText(user.getCompany());
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_company, 0, 0, 0);
            mInfoList.addView(tv);
        }
        if(user.getLocation() != null) {
            final TextView tv = new TextView(getContext());
            tv.setText(user.getLocation());
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_location, 0, 0, 0);
            mInfoList.addView(tv);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
