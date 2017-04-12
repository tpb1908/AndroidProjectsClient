package com.tpb.projects.user;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Loader;
import com.tpb.github.data.auth.GitHubSession;
import com.tpb.github.data.models.User;
import com.tpb.projects.R;
import com.tpb.projects.common.BaseActivity;
import com.tpb.projects.common.ShortcutDialog;
import com.tpb.projects.user.fragments.UserFollowersFragment;
import com.tpb.projects.user.fragments.UserFollowingFragment;
import com.tpb.projects.user.fragments.UserFragment;
import com.tpb.projects.user.fragments.UserGistsFragment;
import com.tpb.projects.user.fragments.UserInfoFragment;
import com.tpb.projects.user.fragments.UserReposFragment;
import com.tpb.projects.user.fragments.UserStarsFragment;
import com.tpb.projects.util.SettingsActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 10/03/17.
 */

public class UserActivity extends BaseActivity implements Loader.ItemLoader<User> {
    private static final String TAG = UserActivity.class.getSimpleName();
    private static final String URL = "https://github.com/tpb1908/AndroidProjectsClient/blob/master/app/src/main/java/com/tpb/projects/user/UserActivity.java";

    @BindView(R.id.title_user) TextView mTitle;
    @BindView(R.id.user_fragment_tablayout) TabLayout mTabs;
    @BindView(R.id.user_fragment_viewpager) ViewPager mPager;

    private UserFragmentAdapter mAdapter;
    private User mUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!mHasAccess) return;
        setTheme(SettingsActivity.Preferences.getPreferences(this).isDarkThemeEnabled()
                ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_user);
        ButterKnife.bind(this);
        postponeEnterTransition();

        if(mAdapter == null) mAdapter = new UserFragmentAdapter(getSupportFragmentManager());
        final Loader loader = Loader.getLoader(this);

        if(getIntent() != null && getIntent().hasExtra(getString(R.string.intent_username))) {
            final String user = getIntent().getStringExtra(getString(R.string.intent_username));
            mTitle.setText(user);
            loader.loadUser(this, user);
        } else {
            if(isTaskRoot()) {
                findViewById(R.id.back_button).setVisibility(View.GONE);
            }
            loadComplete(GitHubSession.getSession(this).getUser());
        }
        mTabs.setupWithViewPager(mPager);
        mPager.setAdapter(mAdapter);
        mPager.setOffscreenPageLimit(7);

    }

    @Override
    public void loadComplete(User user) {
        mUser = user;
        mTitle.setText(mUser.getLogin());
        mAdapter.notifyUserLoaded();
    }

    @Override
    public void loadError(APIHandler.APIError error) {

    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if(mAdapter == null) mAdapter = new UserFragmentAdapter(getSupportFragmentManager());
        mAdapter.ensureAttached((UserFragment) fragment);
    }

    private class UserFragmentAdapter extends FragmentPagerAdapter {

        private UserFragment[] mFragments = new UserFragment[6];

        UserFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0:
                    mFragments[0] = new UserInfoFragment();
                    break;
                case 1:
                    mFragments[1] = new UserReposFragment();
                    break;
                case 2:
                    mFragments[2] = new UserStarsFragment();
                    break;
                case 3:
                    mFragments[3] = new UserGistsFragment();
                    break;
                case 4:
                    mFragments[4] = new UserFollowingFragment();
                    break;
                case 5:
                    mFragments[5] = new UserFollowersFragment();
                    break;
            }
            if(mUser != null) mFragments[position].userLoaded(mUser);
            return mFragments[position];
        }

        void ensureAttached(UserFragment fragment) {
            if(fragment instanceof UserInfoFragment) mFragments[0] = fragment;
            else if(fragment instanceof UserReposFragment) mFragments[1] = fragment;
            else if(fragment instanceof UserStarsFragment) mFragments[2] = fragment;
            else if(fragment instanceof UserGistsFragment) mFragments[3] = fragment;
            else if(fragment instanceof UserFollowingFragment) mFragments[4] = fragment;
            else if(fragment instanceof UserFollowersFragment) mFragments[5] = fragment;
        }

        void notifyUserLoaded() {
            for(UserFragment f : mFragments) {
                if(f != null) f.userLoaded(mUser);
            }
        }

        @Override
        public int getCount() {
            return mFragments.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch(position) {
                case 0:
                    return getString(R.string.title_user_info_fragment);
                case 1:
                    return getString(R.string.title_user_repos_fragment);
                case 2:
                    return getString(R.string.title_user_stars_fragment);
                case 3:
                    return getString(R.string.title_user_gists_fragment);
                case 4:
                    return getString(R.string.title_user_following_fragment);
                case 5:
                    return getString(R.string.title_user_followers_fragment);
                default:
                    return "Error";
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //TODO Pass to fragment
        getMenuInflater().inflate(R.menu.menu_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_settings:
                startActivity(new Intent(UserActivity.this, SettingsActivity.class));
                break;
            case R.id.menu_source:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));
                break;
            case R.id.menu_share:
                final Intent share = new Intent();
                share.setAction(Intent.ACTION_SEND);
                share.putExtra(Intent.EXTRA_TEXT, mUser.getHtmlUrl());
                share.addCategory(Intent.CATEGORY_BROWSABLE);
                share.setType("text/plain");
                startActivity(share);
                break;
            case R.id.menu_save_to_homescreen:
                final ShortcutDialog dialog = new ShortcutDialog();
                final Bundle args = new Bundle();
                args.putInt(getString(R.string.intent_title_res), R.string.title_save_user_shortcut);
                args.putString(getString(R.string.intent_name), mUser.getLogin());

                dialog.setArguments(args);
                dialog.setListener((name, iconFlag) -> {
                    final Intent i = new Intent(getApplicationContext(), UserActivity.class);
                    i.putExtra(getString(R.string.intent_username), mUser.getLogin());

                    final Intent add = new Intent();
                    add.putExtra(Intent.EXTRA_SHORTCUT_INTENT, i);
                    add.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
                    add.putExtra("duplicate", false);
                    add.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_launcher));
                    add.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                    getApplicationContext().sendBroadcast(add);
                });
                dialog.show(getSupportFragmentManager(), TAG);
                break;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AndroidNetworking.cancelAll();
    }
}
