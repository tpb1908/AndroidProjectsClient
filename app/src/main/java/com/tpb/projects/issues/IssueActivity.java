/*
 * Copyright  2016 Theo Pearson-Bray
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.tpb.projects.issues;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androidnetworking.widget.ANImageView;
import com.tpb.animatingrecyclerview.AnimatingRecycler;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.auth.GitHubSession;
import com.tpb.projects.data.models.Comment;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.data.models.User;
import com.tpb.projects.user.UserActivity;

import org.sufficientlysecure.htmltextview.HtmlTextView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 06/01/17.
 */

public class IssueActivity extends AppCompatActivity implements Loader.IssueLoader, Loader.CommentsLoader {
    private static final String TAG = IssueActivity.class.getSimpleName();

    @BindView(R.id.issue_appbar) AppBarLayout mAppbar;
    @BindView(R.id.issue_toolbar) Toolbar mToolbar;
    @BindView(R.id.issue_number) TextView mNumber;
    @BindView(R.id.issue_state) ImageView mImageState;
    @BindView(R.id.issue_info) HtmlTextView mInfo;
    @BindView(R.id.issue_open_info) HtmlTextView mOpenInfo;
    @BindView(R.id.issue_comment_count) TextView mCount;
    @BindView(R.id.issue_scrollview) NestedScrollView mScrollView;
    @BindView(R.id.issue_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.issue_comments_recycler) AnimatingRecycler mRecycler;
    @BindView(R.id.issue_comment_fab) FloatingActionButton mFab;
    @BindView(R.id.issue_assignees) LinearLayout mAssignees; //http://stackoverflow.com/a/29430226/4191572
    @BindView(R.id.issue_menu_button) ImageButton mOverflowButton;

    private Editor mEditor;
    private Loader mLoader;
    
    private Issue mIssue;
    private boolean mCanComment;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_issue);
        ButterKnife.bind(this);

        mEditor = new Editor(this);
        mLoader = new Loader(this);

        if(getIntent().getExtras() != null && getIntent().getExtras().containsKey(getString(R.string.parcel_issue))) {
            mIssue = getIntent().getExtras().getParcelable(getString(R.string.parcel_issue));
            mNumber.setText(String.format("#%1$d", mIssue.getNumber()));
            issueLoaded(mIssue);
        } else {
            final int issueNumber = getIntent().getIntExtra(getString(R.string.intent_issue_number), -1);
            final String repoName = getIntent().getStringExtra(getString(R.string.intent_repo));
            mNumber.setText(String.format("#%1$d", issueNumber));
            mLoader.loadIssue(this, repoName, issueNumber);
        }
    }

    @Override
    public void issueLoaded(Issue issue) {
        mIssue = issue;
        if(issue.getAssignees() != null) displayAssignees();
        mLoader.loadComments(this,  mIssue.getRepoPath(), mIssue.getNumber());
        final StringBuilder builder = new StringBuilder();
        builder.append("<b>");
        builder.append(mIssue.getTitle());
        builder.append("</b>");
        if(mIssue.getLabels() != null && mIssue.getLabels().length > 0) {
            builder.append("<br>");
            Label.appendLabels(builder, mIssue.getLabels(), "   ");
        }

        mInfo.setHtml(builder.toString());
        builder.setLength(0);
        builder.append(
                String.format(
                        getString(R.string.text_opened_this_issue),
                        String.format(getString(R.string.text_href),
                                "https://github.com/" + issue.getOpenedBy().getLogin(),
                                issue.getOpenedBy().getLogin()
                        ),
                        DateUtils.getRelativeTimeSpanString(issue.getCreatedAt())
                )
        );
        mOpenInfo.setShowUnderLines(false);
        mOpenInfo.setHtml(builder.toString());
        if(mIssue.isClosed()) {
            mImageState.setImageResource(R.drawable.ic_issue_closed);
        } else {
            mImageState.setImageResource(R.drawable.ic_issue_open);
        }

        final String login = GitHubSession.getSession(IssueActivity.this).getUserLogin();
        if(issue.getOpenedBy().getLogin().equals(login)) {
            mOverflowButton.setVisibility(View.VISIBLE);
            mCanComment = true;
            mFab.postDelayed(() -> mFab.show(), 300);
        } else {
            mLoader.loadCollaborators(new Loader.CollaboratorsLoader() {
                @Override
                public void collaboratorsLoaded(User[] collaborators) {
                    for(User u : collaborators) {
                        if(u.getLogin().equals(login)) {
                            mCanComment = true;
                            mFab.postDelayed(() -> mFab.show(), 300);
                            mOverflowButton.setVisibility(View.VISIBLE);
                            return;
                        }
                    }
                }

                @Override
                public void collaboratorsLoadError(APIHandler.APIError error) {

                }
            }, mIssue.getRepoPath());
        }
    }

    @Override
    public void issueLoadError(APIHandler.APIError error) {

    }
    
    private void displayAssignees() {
        for(int i = 0; i < mIssue.getAssignees().length; i++) {
            final User u = mIssue.getAssignees()[i];
            final LinearLayout user = (LinearLayout) getLayoutInflater().inflate(R.layout.shard_user, null);
            user.setId(i);
            mAssignees.addView(user);
            final ANImageView imageView = (ANImageView) user.findViewById(R.id.user_image);
            imageView.setId(10 * i);
            imageView.setImageUrl(u.getAvatarUrl());
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            final  TextView login = (TextView) user.findViewById(R.id.user_login);
            login.setId(20 * i); //Max 10 assignees
            login.setText(mIssue.getAssignees()[i].getLogin());
            user.setOnClickListener((v) -> {
                final Intent us = new Intent(IssueActivity.this, UserActivity.class);
                us.putExtra(getString(R.string.intent_username), u.getLogin());

                if(imageView.getDrawable() != null) {
                    us.putExtra(getString(R.string.intent_drawable), ((BitmapDrawable) imageView.getDrawable()).getBitmap());
                }
                startActivity(us,
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                                this,
                                new Pair<>(login, getString(R.string.transition_username)),
                                new Pair<>(imageView, getString(R.string.transition_user_image))
                        ).toBundle());

            });

        }
    }

    @Override
    public void commentsLoaded(Comment[] comments) {
        mCount.setText(Integer.toString(comments.length));
    }

    @Override
    public void commentsLoadError(APIHandler.APIError error) {

    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }

}
