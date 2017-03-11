package com.tpb.projects.user;

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

import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.auth.GitHubSession;
import com.tpb.projects.data.models.User;
import com.tpb.projects.user.fragments.UserEventsFragment;
import com.tpb.projects.user.fragments.UserFragment;
import com.tpb.projects.user.fragments.UserGistsFragment;
import com.tpb.projects.user.fragments.UserInfoFragment;
import com.tpb.projects.user.fragments.UserReposFragment;
import com.tpb.projects.user.fragments.UserStarredFragment;
import com.tpb.projects.util.CircularRevealActivity;
import com.tpb.projects.util.UI;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 10/03/17.
 */

public class UserActivity extends CircularRevealActivity {

    @BindView(R.id.title_user) TextView mTitle;
    @BindView(R.id.user_fragment_tablayout) TabLayout mTabs;
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

        postponeEnterTransition();

        final String user;
        final Loader loader = new Loader(this);
        if(getIntent() != null && getIntent().hasExtra(getString(R.string.intent_username))) {
            user = getIntent().getStringExtra(getString(R.string.intent_username));
            mTitle.setText(user);
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
            final GitHubSession session = GitHubSession.getSession(this);
            mTitle.setText(session.getUserLogin());
            loader.loadAuthenticatedUser(new Loader.GITModelLoader<User>() {
                @Override
                public void loadComplete(User user) {
                    mUser = user;
                    mAdapter.notifyUserLoaded();
                    mTitle.setText(user.getLogin());
                    session.updateUserLogin(user.getLogin());
                }

                @Override
                public void loadError(APIHandler.APIError error) {

                }
            });

        }
        mAdapter = new UserFragmentAdapter(getSupportFragmentManager());
        mTabs.setupWithViewPager(mPager);
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
            switch(position) {
                case 0:
                    return getString(R.string.title_user_info_fragment);
                case 1:
                    return getString(R.string.title_user_repos_fragment);
                case 2:
                    return getString(R.string.title_user_starred_fragment);
                case 3:
                    return getString(R.string.title_user_gists_fragment);
                case 4:
                    return getString(R.string.title_user_events_fragment);
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
//        switch(item.getItemId()) {
//            case R.id.menu_settings:
//                startActivity(new Intent(UserActivity.this, SettingsActivity.class));
//                break;
//            case R.id.menu_source:
//                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));
//                break;
//            case R.id.menu_share:
//                final Intent share = new Intent();
//                share.setAction(Intent.ACTION_SEND);
//                share.putExtra(Intent.EXTRA_TEXT, "https://github.com/" + mUser.getLogin());
//                share.setType("text/plain");
//                startActivity(share);
//                break;
//            case R.id.menu_save_to_homescreen:
//                final ShortcutDialog dialog = new ShortcutDialog();
//                final Bundle args = new Bundle();
//                args.putInt(getString(R.string.intent_title_res), R.string.title_save_user_shortcut);
//                args.putBoolean(getString(R.string.intent_drawable), mUserImage.getDrawable() != null);
//                args.putString(getString(R.string.intent_name), mUserName.getText().toString());
//
//                dialog.setArguments(args);
//                dialog.setListener((name, iconFlag) -> {
//                    final Intent i = new Intent(getApplicationContext(), UserActivity.class);
//                    i.putExtra(getString(R.string.intent_username), mUserName.getText().toString());
//
//                    final Intent add = new Intent();
//                    add.putExtra(Intent.EXTRA_SHORTCUT_INTENT, i);
//                    add.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
//                    add.putExtra("duplicate", false);
//                    if(iconFlag) {
//                        add.putExtra(Intent.EXTRA_SHORTCUT_ICON, ((BitmapDrawable) mUserImage.getDrawable()).getBitmap());
//                    } else {
//                        add.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_launcher));
//                    }
//                    add.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
//                    getApplicationContext().sendBroadcast(add);
//                });
//                dialog.show(getSupportFragmentManager(), TAG);
//                break;
//        }
        return true;
    }


    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }
}
