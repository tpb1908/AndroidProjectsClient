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
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.widget.ANImageView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.tpb.animatingrecyclerview.AnimatingRecycler;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.auth.GitHubSession;
import com.tpb.projects.data.models.Comment;
import com.tpb.projects.data.models.Event;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.data.models.User;
import com.tpb.projects.dialogs.CommentDialog;
import com.tpb.projects.dialogs.EditIssueDialog;
import com.tpb.projects.user.UserActivity;
import com.tpb.projects.util.Analytics;
import com.tpb.projects.util.Data;
import com.tpb.projects.util.ShortcutDialog;

import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * Created by theo on 06/01/17.
 */

public class IssueActivity extends AppCompatActivity implements Loader.IssueLoader, Loader.CommentsLoader, Loader.EventsLoader {
    private static final String TAG = IssueActivity.class.getSimpleName();
    private static final String URL = "https://github.com/tpb1908/AndroidProjectsClient/blob/master/app/src/main/java/com/tpb/projects/issues/IssueActivity.java";

    private FirebaseAnalytics mAnalytics;

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
    private Repository.AccessLevel mAccessLevel = Repository.AccessLevel.NONE;

    private IssueContentAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_issue);
        ButterKnife.bind(this);
        mAnalytics = FirebaseAnalytics.getInstance(this);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mEditor = new Editor(this);
        mLoader = new Loader(this);

        mRefresher.setRefreshing(true);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new IssueContentAdapter(this);
        mRecycler.setAdapter(mAdapter);
        mRefresher.setOnRefreshListener(() -> {
            mAdapter.clear();
            mLoader.loadIssue(IssueActivity.this, mIssue.getRepoPath(), mIssue.getNumber(), true);
        });

        mOpenInfo.setShowUnderLines(false);
        mInfo.setShowUnderLines(false);
        mInfo.setImageHandler(new HtmlTextView.ImageDialog(this));

        if(getIntent().getExtras() != null && getIntent().getExtras().containsKey(getString(R.string.parcel_issue))) {
            mIssue = getIntent().getExtras().getParcelable(getString(R.string.parcel_issue));
            mNumber.setText(String.format("#%1$d", mIssue.getNumber()));
            issueLoaded(mIssue);
        } else {
            final int issueNumber = getIntent().getIntExtra(getString(R.string.intent_issue_number), -1);
            final String fullRepoName = getIntent().getStringExtra(getString(R.string.intent_repo));
            mNumber.setText(String.format("#%1$d", issueNumber));
            mLoader.loadIssue(this, fullRepoName, issueNumber, true);
        }

       // mOverflowButton.setOnClickListener((v) -> displayCommentMenu(v, null));
    }

    @Override
    public void issueLoaded(Issue issue) {
        mIssue = issue;
        mAdapter.setIssue(mIssue);
        displayAssignees();
        mLoader.loadComments(this,  mIssue.getRepoPath(), mIssue.getNumber());

        bindIssue();

        final String login = GitHubSession.getSession(IssueActivity.this).getUserLogin();
        if(issue.getOpenedBy().getLogin().equals(login)) {
            mAccessLevel = Repository.AccessLevel.ADMIN;
            enableAccess();
            mFab.postDelayed(mFab::show, 300);
            mAccessLevel = Repository.AccessLevel.ADMIN;
        } else {
            mLoader.checkIfCollaborator(new Loader.AccessCheckListener() {

                public void accessCheckComplete(Repository.AccessLevel accessLevel) {
                    mAccessLevel = accessLevel;
                    if(mAccessLevel == Repository.AccessLevel.ADMIN || mAccessLevel == Repository.AccessLevel.WRITE) {
                        enableAccess();
                    }
                }

                @Override
                public void accessCheckError(APIHandler.APIError error) {

                }

            }, GitHubSession.getSession(this).getUserLogin(), mIssue.getRepoPath());
        }
        mLoader.loadEvents(this, mIssue.getRepoPath(), mIssue.getNumber());
    }

    private void bindIssue() {
        final StringBuilder builder = new StringBuilder();
        builder.append("<h1>");
        builder.append(mIssue.getTitle().replace("\n", "</h1><h1>")); //h1 won;t do multiple lines
        builder.append("</h1>");
        if(mIssue.getBody() != null) {
            builder.append("\n");
            builder.append(mIssue.getBody());
            builder.append("<br>");
        }
        if(mIssue.getLabels() != null && mIssue.getLabels().length > 0) {
            builder.append("<br>");
            Label.appendLabels(builder, mIssue.getLabels(), "   ");
        }
        mInfo.setHtml(Data.parseMD(builder.toString(), mIssue.getRepoPath()), new HtmlHttpImageGetter(mInfo));

        builder.setLength(0);
        builder.append(
                String.format(
                        getString(R.string.text_opened_this_issue),
                        String.format(getString(R.string.text_href),
                                "https://github.com/" + mIssue.getOpenedBy().getLogin(),
                                mIssue.getOpenedBy().getLogin()
                        ),
                        DateUtils.getRelativeTimeSpanString(mIssue.getCreatedAt())
                )
        );

        mOpenInfo.setHtml(Data.parseMD(builder.toString(), mIssue.getRepoPath()));

        if(mIssue.isClosed()) {
            mImageState.setImageResource(R.drawable.ic_issue_closed);
        } else {
            mImageState.setImageResource(R.drawable.ic_issue_open);
        }
    }

    private void enableAccess() {
        mFab.postDelayed(() -> mFab.show(), 300);
        mScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if(scrollY - oldScrollY > 10) {
                    mFab.hide();
                } else if(scrollY - oldScrollY < -10) {
                    mFab.show();
                }
            }
        });
    }

    @Override
    public void issueLoadError(APIHandler.APIError error) {

    }
    
    private void displayAssignees() {

        if(mIssue != null && mIssue.getAssignees() != null && mIssue.getAssignees().length > 0) {
            mAssignees.setVisibility(View.VISIBLE);
            for(int i = 0; i < mIssue.getAssignees().length; i++) {
                final User u = mIssue.getAssignees()[i];
                final LinearLayout user = (LinearLayout) getLayoutInflater().inflate(R.layout.shard_user, null);
                user.setId(i);
                mAssignees.addView(user);
                final ANImageView imageView = (ANImageView) user.findViewById(R.id.user_image);
                imageView.setId(10 * i);
                imageView.setImageUrl(u.getAvatarUrl());
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                final TextView login = (TextView) user.findViewById(R.id.user_login);
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
        } else {
            mAssignees.setVisibility(View.GONE);
        }
    }

    @Override
    public void commentsLoaded(Comment[] comments) {
        mCount.setText(Integer.toString(comments.length));
        mAdapter.loadComments(comments);
    }

    @Override
    public void commentsLoadError(APIHandler.APIError error) {

    }

    @OnClick(R.id.issue_comment_fab)
    void newComment() {
        final CommentDialog cd = new CommentDialog();
        cd.setListener(new CommentDialog.CommentDialogListener() {
            @Override
            public void onPositive(String body) {
                mRefresher.setRefreshing(true);
                mEditor.createComment(new Editor.CommentCreationListener() {
                    @Override
                    public void commentCreated(Comment comment) {
                        mRefresher.setRefreshing(false);
                        mAdapter.addComment(comment);
                        mScrollView.post(() -> mScrollView.smoothScrollTo(0, mScrollView.getBottom()));
                       // mRecycler.scrollToPosition(mAdapter.getItemCount());
                    }

                    @Override
                    public void commentCreationError(APIHandler.APIError error) {
                        mRefresher.setRefreshing(false);
                    }
                }, mIssue.getRepoPath(), mIssue.getNumber(), body);
            }

            @Override
            public void onCancelled() {

            }
        });
        cd.show(getSupportFragmentManager(), TAG);
    }

    private void editComment(Comment comment) {
        final  CommentDialog cd = new CommentDialog();
        cd.setTitleResource(R.string.title_edit_comment);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(getString(R.string.intent_comment), comment);
        cd.setArguments(bundle);
        cd.setListener(new CommentDialog.CommentDialogListener() {
            @Override
            public void onPositive(String body) {
                mRefresher.setRefreshing(true);
                mEditor.editComment(new Editor.CommentEditListener() {
                    @Override
                    public void commentEdited(Comment comment) {
                        mRefresher.setRefreshing(false);
                        mAdapter.updateComment(comment);
                    }

                    @Override
                    public void commentEditError(APIHandler.APIError error) {
                        mRefresher.setRefreshing(false);
                    }
                }, mIssue.getRepoPath(), comment.getId(), body);
            }

            @Override
            public void onCancelled() {

            }
        });
        cd.show(getSupportFragmentManager(), TAG);
    }

    public void displayCommentMenu(View view, Comment comment) {
        final PopupMenu menu = new PopupMenu(this, view);
        menu.inflate(R.menu.menu_comment);
        if(comment.getUser().getLogin().equals(GitHubSession.getSession(IssueActivity.this).getUserLogin())) {
            menu.getMenu().add(0, 1, 0, "Edit");
        }
        menu.setOnMenuItemClickListener(menuItem -> {
            switch(menuItem.getItemId()) {
                case 1:
                    editComment(comment);
                    break;
            }
            return false;
        });
        menu.show();
    }

    @OnClick(R.id.issue_menu_button)
    public void displayIssueMenu(View view) {
        final PopupMenu menu = new PopupMenu(this, view);
        menu.inflate(R.menu.menu_issue);
        if(mAccessLevel == Repository.AccessLevel.ADMIN) {
            menu.getMenu().add(0, 1, Menu.NONE, mIssue.isClosed() ? R.string.menu_reopen_issue : R.string.menu_close_issue);
            menu.getMenu().add(0, 2, Menu.NONE, R.string.menu_edit_issue);
        }
        menu.setOnMenuItemClickListener(menuItem -> {
            switch(menuItem.getItemId()) {
                case 1:
                    toggleIssueState();
                    break;
                case 2:
                    editIssue();
                    break;
            }
            return false;
        });
        menu.show();
    }

    private void editIssue() {
        final EditIssueDialog editDialog = new EditIssueDialog();
        editDialog.setListener(new EditIssueDialog.EditIssueDialogListener() {
            @Override
            public void issueEdited(Issue issue, @Nullable String[] assignees, @Nullable String[] labels) {
                mRefresher.setRefreshing(true);
                mEditor.editIssue(new Editor.IssueEditListener() {
                    int issueCreationAttempts = 0;

                    @Override
                    public void issueEdited(Issue issue) {
                        mIssue = issue;
                        bindIssue();
                        mRefresher.setRefreshing(false);
                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                        mAnalytics.logEvent(Analytics.TAG_ISSUE_EDIT, bundle);
                    }

                    @Override
                    public void issueEditError(APIHandler.APIError error) {
                        if(error == APIHandler.APIError.NO_CONNECTION) {
                            mRefresher.setRefreshing(false);
                            Toast.makeText(IssueActivity.this, error.resId, Toast.LENGTH_SHORT).show();
                        } else {
                            if(issueCreationAttempts < 5) {
                                issueCreationAttempts++;
                                mEditor.editIssue(this, mIssue.getRepoPath(), issue, assignees, labels);
                            } else {
                                Toast.makeText(IssueActivity.this, error.resId, Toast.LENGTH_SHORT).show();
                                mRefresher.setRefreshing(false);
                            }
                        }

                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                        mAnalytics.logEvent(Analytics.TAG_ISSUE_EDIT, bundle);
                    }
                }, mIssue.getRepoPath(), issue, assignees, labels);
            }

            @Override
            public void issueEditCancelled() {

            }
        });
        final Bundle c = new Bundle();
        c.putParcelable(getString(R.string.parcel_issue), mIssue);
        c.putString(getString(R.string.intent_repo), mIssue.getRepoPath());
        editDialog.setArguments(c);
        editDialog.show(getSupportFragmentManager(), TAG);
    }
    // TODO Diff events and comments rather than just clearing
    private void toggleIssueState() {
        final Editor.IssueStateChangeListener listener = new Editor.IssueStateChangeListener() {
            @Override
            public void issueStateChanged(Issue issue) {
                mLoader.loadEvents(IssueActivity.this, mIssue.getRepoPath(), mIssue.getNumber());
                mIssue = issue;
                mImageState.setImageResource(mIssue.isClosed() ? R.drawable.ic_issue_closed : R.drawable.ic_issue_open);
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                mAnalytics.logEvent(issue.isClosed() ? Analytics.TAG_ISSUE_CLOSED : Analytics.TAG_ISSUE_OPENED, bundle);
            }

            @Override
            public void issueStateChangeError(APIHandler.APIError error) {
                mRefresher.setRefreshing(false);
                if(error == APIHandler.APIError.NO_CONNECTION) {
                    mRefresher.setRefreshing(false);
                    Toast.makeText(IssueActivity.this, error.resId, Toast.LENGTH_SHORT).show();
                }

                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                mAnalytics.logEvent(Analytics.TAG_ISSUE_EDIT, bundle);
            }
        };

        final CommentDialog dialog = new CommentDialog();
        dialog.setListener(new CommentDialog.CommentDialogListener() {
            @Override
            public void onPositive(String body) {
                mEditor.createComment(new Editor.CommentCreationListener() {
                    @Override
                    public void commentCreated(Comment comment) {
                        mRefresher.setRefreshing(true);
                        if(mIssue.isClosed()) {
                            mEditor.openIssue(listener, mIssue.getRepoPath(), mIssue.getNumber());
                            mAdapter.clear();
                            mAdapter.addComment(comment);
                        } else {
                            mEditor.closeIssue(listener, mIssue.getRepoPath(), mIssue.getNumber());
                        }
                        mLoader.loadComments(IssueActivity.this, mIssue.getRepoPath(), mIssue.getNumber());
                    }

                    @Override
                    public void commentCreationError(APIHandler.APIError error) {
                        if(error == APIHandler.APIError.NO_CONNECTION) {
                            mRefresher.setRefreshing(false);
                            Toast.makeText(IssueActivity.this, error.resId, Toast.LENGTH_SHORT).show();
                        }
                    }
                }, mIssue.getRepoPath(), mIssue.getNumber(), body);

            }

            @Override
            public void onCancelled() {
                mRefresher.setRefreshing(true);
                mAdapter.clear();
                if(mIssue.isClosed()) {
                    mEditor.openIssue(listener, mIssue.getRepoPath(), mIssue.getNumber());
                } else {
                    mEditor.closeIssue(listener, mIssue.getRepoPath(), mIssue.getNumber());
                }
            }
        });
        dialog.show(getSupportFragmentManager(), TAG);
    }

    @Override
    public void eventsLoaded(Event[] events) {
        mRecycler.enableAnimation();
        mAdapter.loadEvents(events);
        mRefresher.setRefreshing(false);
    }

    @Override
    public void eventsLoadError(APIHandler.APIError error) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_settings:
                startActivity(new Intent(IssueActivity.this, SettingsActivity.class));
                break;
            case R.id.menu_source:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));
                break;
            case R.id.menu_share:
                if(mIssue != null) {
                    final Intent share = new Intent();
                    share.setAction(Intent.ACTION_SEND);
                    share.putExtra(Intent.EXTRA_TEXT, "https://github.com/" + mIssue.getRepoPath() + "/issues/" + mIssue.getNumber());
                    share.setType("text/plain");
                    startActivity(share);
                }
                break;
            case R.id.menu_save_to_homescreen:
                final ShortcutDialog dialog = new ShortcutDialog();
                final Bundle args = new Bundle();
                args.putInt(getString(R.string.intent_title_res), R.string.title_save_issue_shortcut);
                args.putBoolean(getString(R.string.intent_drawable), false);
                args.putString(getString(R.string.intent_name), "#" + mIssue.getNumber());

                dialog.setArguments(args);
                dialog.setListener((name, iconFlag) -> {
                    final Intent i = new Intent(getApplicationContext(), IssueActivity.class);
                    i.putExtra(getString(R.string.intent_repo), mIssue.getRepoPath());
                    i.putExtra(getString(R.string.intent_issue_number), mIssue.getNumber());

                    final Intent add = new Intent();
                    add.putExtra(Intent.EXTRA_SHORTCUT_INTENT, i);
                    add.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
                    add.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_launcher));
                    add.putExtra("duplicate", false);
                    add.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                    getApplicationContext().sendBroadcast(add);
                });
                dialog.show(getSupportFragmentManager(), TAG);
                break;
        }
        return true;
    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }

}
