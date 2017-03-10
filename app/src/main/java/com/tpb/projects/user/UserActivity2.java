package com.tpb.projects.user;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.auth.GitHubSession;
import com.tpb.projects.data.models.User;
import com.tpb.projects.editors.CircularRevealActivity;
import com.tpb.projects.user.fragments.UserEventsFragment;
import com.tpb.projects.user.fragments.UserFragment;
import com.tpb.projects.user.fragments.UserGistsFragment;
import com.tpb.projects.user.fragments.UserInfoFragment;
import com.tpb.projects.user.fragments.UserReposFragment;
import com.tpb.projects.user.fragments.UserStarredFragment;
import com.tpb.projects.util.UI;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 10/03/17.
 */

public class UserActivity2 extends CircularRevealActivity {


    @BindView(R.id.user_fragment_viewpager) ViewPager mPager;

    private UserFragmentAdapter mAdapter;
    private User mUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Transparent_Dark : R.style.AppTheme_Transparent);
        UI.setStatusBarColor(getWindow(), getResources().getColor(R.color.colorPrimaryDark));
        setContentView(R.layout.activity_user_viewpager);
        ButterKnife.bind(this);

        final String user;
        final Loader loader = new Loader(this);
        if(getIntent() != null && getIntent().hasExtra(getString(R.string.intent_username))) {
            user = getIntent().getStringExtra(getString(R.string.intent_username));
            ((TextView) findViewById(R.id.title_user)).setText(R.string.title_activity_user);
            loader.loadUser(new Loader.GITModelLoader<User>() {
                @Override
                public void loadComplete(User u) {
                    mUser = u;
                    mAdapter.notifyUserLoaded();
                }

                @Override
                public void loadError(APIHandler.APIError error) {

                }
            }, user);

        } else {
            if(isTaskRoot()) {
                findViewById(R.id.back_button).setVisibility(View.GONE);
            }
            loader.loadAuthenticatedUser(new Loader.GITModelLoader<User>() {
                @Override
                public void loadComplete(User user) {
                    mUser = user;
                    mAdapter.notifyUserLoaded();
                    GitHubSession.getSession(UserActivity2.this).updateUserLogin(user.getLogin());
                }

                @Override
                public void loadError(APIHandler.APIError error) {

                }
            });

        }
        mAdapter = new UserFragmentAdapter(getSupportFragmentManager());
        mPager.setAdapter(mAdapter);
        mPager.setOffscreenPageLimit(5);
    }

    private class UserFragmentAdapter extends FragmentPagerAdapter {

        private UserFragment[] fragments = new UserFragment[5];

        UserFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0:
                    fragments[0] = new UserInfoFragment();
                    break;
                case 1:
                    fragments[1] = new UserReposFragment();
                    break;
                case 2:
                    fragments[2] = new UserStarredFragment();
                    break;
                case 3:
                    fragments[3] = new UserGistsFragment();
                    break;
                case 4:
                    fragments[4] = new UserEventsFragment();
                    break;
            }
            if(mUser != null) fragments[position].userLoaded(mUser);
            return fragments[position];
        }

        void notifyUserLoaded() {
            for(UserFragment f : fragments) {
                if(f != null) f.userLoaded(mUser);
            }
        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Title";
        }
    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }


}
