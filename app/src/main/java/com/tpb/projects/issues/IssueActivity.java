package com.tpb.projects.issues;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.auth.GitHubSession;
import com.tpb.projects.data.models.Comment;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.editors.CommentEditor;
import com.tpb.projects.issues.content.IssueCommentsFragment;
import com.tpb.projects.issues.content.IssueInfoFragment;
import com.tpb.projects.util.CircularRevealActivity;
import com.tpb.projects.util.UI;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 15/03/17.
 */

public class IssueActivity extends CircularRevealActivity implements Loader.GITModelLoader<Issue> {
    private static final String TAG = IssueActivity.class.getSimpleName();
    private static final String URL = "https://github.com/tpb1908/AndroidProjectsClient/blob/master/app/src/main/java/com/tpb/projects/issues/IssueActivity.java";

    private FirebaseAnalytics mAnalytics;

    @BindView(R.id.issue_appbar) AppBarLayout mAppbar;
    @BindView(R.id.issue_toolbar) Toolbar mToolbar;
    @BindView(R.id.issue_content_viewpager) ViewPager mPager;
    @BindView(R.id.issue_fragment_tabs) TabLayout mTabs;
    @BindView(R.id.issue_number) TextView mNumber;

    private Loader mLoader;

    private IssueFragmentAdapter mAdapter;

    private Issue mIssue;
    public Repository.AccessLevel mAccessLevel = Repository.AccessLevel.NONE;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        UI.setStatusBarColor(getWindow(), getResources().getColor(R.color.colorPrimaryDark));
        setContentView(R.layout.activity_issue);
        ButterKnife.bind(this);
        mAnalytics = FirebaseAnalytics.getInstance(this);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mLoader = new Loader(this);
        mAdapter = new IssueFragmentAdapter(getSupportFragmentManager());

        if(getIntent().getExtras() != null && getIntent().getExtras().containsKey(getString(R.string.parcel_issue))) {
            mIssue = getIntent().getExtras().getParcelable(getString(R.string.parcel_issue));

            loadComplete(mIssue);
        } else {
            final int issueNumber = getIntent().getIntExtra(getString(R.string.intent_issue_number), -1);
            final String fullRepoName = getIntent().getStringExtra(getString(R.string.intent_repo));
            mLoader.loadIssue(this, fullRepoName, issueNumber, true);
        }

        mPager.setOffscreenPageLimit(2);
        mPager.setAdapter(mAdapter);
        mTabs.setupWithViewPager(mPager);
    }

    @Override
    public void loadComplete(Issue issue) {
        mIssue = issue;
        mNumber.setText(String.format("#%1$s", issue.getNumber()));
        final String login = GitHubSession.getSession(IssueActivity.this).getUserLogin();
        if(mIssue.getOpenedBy().getLogin().equals(login)) {
            mAccessLevel = Repository.AccessLevel.ADMIN;
            if(mAdapter.mInfoFragment != null) mAdapter.mInfoFragment.setAccessLevel(mAccessLevel);
        } else {
            mLoader.checkIfCollaborator(new Loader.GITModelLoader<Repository.AccessLevel>() {
                @Override
                public void loadComplete(Repository.AccessLevel data) {
                    mAccessLevel = data;
                    if(mAdapter.mInfoFragment != null) mAdapter.mInfoFragment.setAccessLevel(mAccessLevel);
                }

                @Override
                public void loadError(APIHandler.APIError error) {

                }
            }, GitHubSession.getSession(this).getUserLogin(), mIssue.getRepoPath());
        }

        mAdapter.setIssue();
    }

    private void enableAccess() {
//        mScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
//            @Override
//            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
//                if(scrollY - oldScrollY > 10) {
//                    mFab.hide();
//                } else if(scrollY - oldScrollY < -10) {
//                    mFab.show();
//                }
//            }
//        });
    }

    @Override
    public void loadError(APIHandler.APIError error) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == AppCompatActivity.RESULT_OK) {
            final Comment comment = data.getParcelableExtra(getString(R.string.parcel_comment));
            if(requestCode == CommentEditor.REQUEST_CODE_NEW_COMMENT) {
                mAdapter.mCommentsFragment.createComment(comment);
            } else if(requestCode == CommentEditor.REQUEST_CODE_EDIT_COMMENT) {
                mAdapter.mCommentsFragment.editComment(comment);
            } else if(requestCode == CommentEditor.REQUEST_CODE_COMMENT_FOR_STATE) {
                mAdapter.mCommentsFragment.createCommentForState(comment);
            }
        }
    }

    private class IssueFragmentAdapter extends FragmentPagerAdapter {

        IssueCommentsFragment mCommentsFragment;
        IssueInfoFragment mInfoFragment;

        IssueFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if(position == 0) {
                mInfoFragment = IssueInfoFragment.getInstance();
                if(mIssue != null) mInfoFragment.issueLoaded(mIssue);
                return mInfoFragment;
            } else {
                mCommentsFragment = IssueCommentsFragment.getInstance();
                if(mIssue != null) mCommentsFragment.issueLoaded(mIssue);
                return mCommentsFragment;
            }
        }

        void setIssue() {
            if(mCommentsFragment != null) mCommentsFragment.issueLoaded(mIssue);
            if(mInfoFragment != null) mInfoFragment.issueLoaded(mIssue);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if(position == 0) {
                return "Events";
            }  else {
                return "Comments";
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }
}
