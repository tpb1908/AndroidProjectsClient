package com.tpb.projects.issues;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.util.Pair;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.widget.ANImageView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.models.Comment;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.data.models.Milestone;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.data.models.State;
import com.tpb.projects.data.models.User;
import com.tpb.projects.editors.CommentEditor;
import com.tpb.projects.editors.IssueEditor;
import com.tpb.projects.flow.IntentHandler;
import com.tpb.projects.issues.content.IssueCommentsFragment;
import com.tpb.projects.issues.content.IssueEventsFragment;
import com.tpb.projects.markdown.Markdown;
import com.tpb.projects.user.UserActivity;
import com.tpb.projects.util.Analytics;
import com.tpb.projects.util.CircularRevealActivity;
import com.tpb.projects.util.MultiOnRefreshListener;
import com.tpb.projects.util.UI;

import org.sufficientlysecure.htmltext.dialogs.CodeDialog;
import org.sufficientlysecure.htmltext.dialogs.ImageDialog;
import org.sufficientlysecure.htmltext.htmltextview.HtmlTextView;
import org.sufficientlysecure.htmltext.imagegetter.HtmlHttpImageGetter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by theo on 15/03/17.
 */

public class IssueActivity2 extends CircularRevealActivity implements Loader.GITModelLoader<Issue> {
    private static final String TAG = IssueActivity2.class.getSimpleName();
    private static final String URL = "https://github.com/tpb1908/AndroidProjectsClient/blob/master/app/src/main/java/com/tpb/projects/issues/IssueActivity.java";

    private FirebaseAnalytics mAnalytics;

    @BindView(R.id.issue_appbar) AppBarLayout mAppbar;
    @BindView(R.id.issue_toolbar) Toolbar mToolbar;
    @BindView(R.id.issue_number) TextView mNumber;
    @BindView(R.id.issue_user_avatar) ANImageView mUserAvatar;
    @BindView(R.id.issue_state) ImageView mImageState;
    @BindView(R.id.issue_info) HtmlTextView mInfo;
    @BindView(R.id.issue_open_info) HtmlTextView mOpenInfo;
    @BindView(R.id.issue_comment_count) TextView mCount;
    @BindView(R.id.issue_scrollview) NestedScrollView mScrollView;
    @BindView(R.id.issue_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.issue_comment_fab) FloatingActionButton mFab;
    @BindView(R.id.issue_assignees) LinearLayout mAssigneesLayout; //http://stackoverflow.com/a/29430226/4191572
    @BindView(R.id.viewholder_milestone_card) CardView mMilestoneCard;
    @BindView(R.id.issue_menu_button) ImageButton mOverflowButton;


    private Editor mEditor;
    private Loader mLoader;

    private IssueFragmentAdapter mAdapter;
    private MultiOnRefreshListener mRefreshListener;

    private Issue mIssue;
    private Repository.AccessLevel mAccessLevel = Repository.AccessLevel.NONE;

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

        mEditor = new Editor(this);
        mLoader = new Loader(this);

        mRefreshListener = new MultiOnRefreshListener();
        mAdapter = new IssueFragmentAdapter(getSupportFragmentManager());

        mRefresher.setRefreshing(true);
        mRefresher.setOnRefreshListener(mRefreshListener);

        mOpenInfo.setShowUnderLines(false);
        mInfo.setShowUnderLines(false);
        mInfo.setImageHandler(new ImageDialog(this));
        mInfo.setCodeClickHandler(new CodeDialog(this));

        if(getIntent().getExtras() != null && getIntent().getExtras().containsKey(getString(R.string.parcel_issue))) {
            mIssue = getIntent().getExtras().getParcelable(getString(R.string.parcel_issue));
            mNumber.setText(String.format("#%1$d", mIssue.getNumber()));
            loadComplete(mIssue);
        } else {
            final int issueNumber = getIntent().getIntExtra(getString(R.string.intent_issue_number), -1);
            final String fullRepoName = getIntent().getStringExtra(getString(R.string.intent_repo));
            mNumber.setText(String.format("#%1$d", issueNumber));
            mLoader.loadIssue(this, fullRepoName, issueNumber, true);
        }
    }

    @Override
    public void loadComplete(Issue data) {

    }

    @Override
    public void loadError(APIHandler.APIError error) {

    }

    private void bindIssue() {
        final StringBuilder builder = new StringBuilder();
        builder.append("<h1>");
        builder.append(Markdown.escape(mIssue.getTitle()).replace("\n", "</h1><h1>")); //h1 won't do multiple lines
        builder.append("</h1>");
        builder.append("\n");

        // The title and body are formatted separately, because the title shouldn't be formatted
        String html = Markdown.formatMD(builder.toString(), null, false);
        builder.setLength(0); // Clear the builder to reuse it

        if(mIssue.getBody() != null && mIssue.getBody().trim().length() > 0) {
            builder.append(mIssue.getBody().replaceFirst("\\s++$", ""));
            builder.append("\n");
        }
        if(mIssue.getLabels() != null && mIssue.getLabels().length > 0) {
            Label.appendLabels(builder, mIssue.getLabels(), "   ");
        }

        html += Markdown.parseMD(builder.toString(), mIssue.getRepoPath());

        mInfo.setHtml(html, new HtmlHttpImageGetter(mInfo, mInfo), null);

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
        mOpenInfo.setHtml(Markdown.parseMD(builder.toString(), mIssue.getRepoPath()));
        mUserAvatar.setOnClickListener(v -> {
            IntentHandler.openUser(IssueActivity2.this, mUserAvatar, mIssue.getOpenedBy().getLogin());
        });
        mUserAvatar.setImageUrl(mIssue.getOpenedBy().getAvatarUrl());
        if(mIssue.isClosed()) {
            mImageState.setImageResource(R.drawable.ic_state_closed);
        } else {
            mImageState.setImageResource(R.drawable.ic_state_open);
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

    private void displayAssignees() {
        mAssigneesLayout.removeAllViews();
        if(mIssue != null && mIssue.getAssignees() != null && mIssue.getAssignees().length > 0) {
            mAssigneesLayout.setVisibility(View.VISIBLE);
            for(int i = 0; i < mIssue.getAssignees().length; i++) {
                final User u = mIssue.getAssignees()[i];
                final LinearLayout user = (LinearLayout) getLayoutInflater().inflate(R.layout.shard_user, null);
                user.setId(i);
                mAssigneesLayout.addView(user);
                final ANImageView imageView = (ANImageView) user.findViewById(R.id.user_image);
                imageView.setId(10 * i);
                imageView.setImageUrl(u.getAvatarUrl());
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                final TextView login = (TextView) user.findViewById(R.id.user_login);
                login.setId(20 * i); //Max 10 assignees
                login.setText(mIssue.getAssignees()[i].getLogin());
                user.setOnClickListener((v) -> {
                    final Intent us = new Intent(IssueActivity2.this, UserActivity.class);
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
            mAssigneesLayout.setVisibility(View.GONE);
        }
    }

    private void displayMilestone() {
        if(mIssue.getMilestone() != null) {
            mMilestoneCard.setVisibility(View.VISIBLE);
            final Milestone milestone = mIssue.getMilestone();
            final HtmlTextView tv = ButterKnife.findById(mMilestoneCard, R.id.milestone_content_markdown);
            tv.setShowUnderLines(false);
            final ImageView status = ButterKnife.findById(mMilestoneCard, R.id.milestone_drawable);
            final ANImageView user = ButterKnife.findById(mMilestoneCard, R.id.milestone_user_avatar);
            IntentHandler.addGitHubIntentHandler(this, tv, user, milestone.getCreator().getLogin());
            IntentHandler.addGitHubIntentHandler(this, user, milestone.getCreator().getLogin());
            status.setImageResource(milestone.getState() == State.OPEN ? R.drawable.ic_state_open : R.drawable.ic_state_closed);
            user.setImageUrl(milestone.getCreator().getAvatarUrl());

            final StringBuilder builder = new StringBuilder();

            builder.append("<b>");
            builder.append(Markdown.escape(milestone.getTitle()));
            builder.append("</b>");
            builder.append("<br>");
            if(milestone.getOpenIssues() > 0 || milestone.getClosedIssues() > 0) {
                builder.append("<br>");
                builder.append(String.format(getString(R.string.text_milestone_completion),
                        milestone.getOpenIssues(),
                        milestone.getClosedIssues(),
                        Math.round(100f * milestone.getClosedIssues()/(milestone.getOpenIssues() + milestone.getClosedIssues())))
                );
            }
            builder.append("<br>");
            builder.append(
                    String.format(
                            getString(R.string.text_milestone_opened_by),
                            String.format(getString(R.string.text_href),
                                    "https://github.com/" + mIssue.getMilestone().getCreator().getLogin(),
                                    mIssue.getMilestone().getCreator().getLogin()
                            ),
                            DateUtils.getRelativeTimeSpanString(milestone.getCreatedAt())
                    )
            );
            if(milestone.getUpdatedAt() != milestone.getCreatedAt()) {
                builder.append("<br>");
                builder.append(
                        String.format(
                                getString(R.string.text_last_updated),
                                DateUtils.getRelativeTimeSpanString(milestone.getUpdatedAt())
                        )
                );
            }
            if(milestone.getClosedAt() > 0) {
                builder.append("<br>");
                builder.append(
                        String.format(
                                getString(R.string.text_milestone_closed_at),
                                DateUtils.getRelativeTimeSpanString(milestone.getClosedAt())
                        )
                );
            }
            if(milestone.getDueOn() > 0) {
                builder.append("<br>");
                if(System.currentTimeMillis() < milestone.getDueOn() ||
                        (milestone.getClosedAt() != 0 && milestone.getClosedAt() < milestone.getDueOn())) {
                    builder.append(
                            String.format(
                                    getString(R.string.text_milestone_due_on),
                                    DateUtils.getRelativeTimeSpanString(milestone.getDueOn())
                            )
                    );
                } else {
                    builder.append("<font color=\"");
                    builder.append(String.format("#%06X", (0xFFFFFF & Color.RED)));
                    builder.append("\">");
                    builder.append(
                            String.format(
                                    getString(R.string.text_milestone_due_on),
                                    DateUtils.getRelativeTimeSpanString(milestone.getDueOn())
                            )
                    );
                    builder.append("</font>");
                }
            }
            tv.setHtml(Markdown.formatMD(builder.toString(), mIssue.getRepoPath()));

        } else {
            mMilestoneCard.setVisibility(View.GONE);
        }
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
                    editIssue(view);
                    break;
            }
            return false;
        });
        menu.show();
    }

    @OnClick(R.id.issue_header_card)
    void onHeaderClick(View view) {
        if(mIssue != null && mAccessLevel == Repository.AccessLevel.ADMIN) editIssue(view);
    }

    private void editIssue(View view) {
        final Intent i = new Intent(IssueActivity2.this, IssueEditor.class);
        i.putExtra(getString(R.string.intent_repo), mIssue.getRepoPath());
        i.putExtra(getString(R.string.parcel_issue), mIssue);
        UI.setViewPositionForIntent(i, view);
        startActivityForResult(i, IssueEditor.REQUEST_CODE_EDIT_ISSUE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == AppCompatActivity.RESULT_OK) {
            if(requestCode == IssueEditor.REQUEST_CODE_EDIT_ISSUE) {
                final Issue issue = data.getParcelableExtra(getString(R.string.parcel_issue));
                final String[] assignees;
                final String[] labels;
                if(data.hasExtra(getString(R.string.intent_issue_assignees))) {
                    assignees = data.getStringArrayExtra(getString(R.string.intent_issue_assignees));
                } else {
                    assignees = null;
                }
                if(data.hasExtra(getString(R.string.intent_issue_labels))) {
                    labels = data.getStringArrayExtra(getString(R.string.intent_issue_labels));
                } else {
                    labels = null;
                }

                mRefresher.setRefreshing(true);
                mEditor.updateIssue(new Editor.GITModelUpdateListener<Issue>() {
                    int issueCreationAttempts = 0;

                    @Override
                    public void updated(Issue issue) {
                        mIssue = issue;
                        bindIssue();
                        displayAssignees();
                        mRefresher.setRefreshing(false);
                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                        mAnalytics.logEvent(Analytics.TAG_ISSUE_EDIT, bundle);
                    }

                    @Override
                    public void updateError(APIHandler.APIError error) {
                        if(error == APIHandler.APIError.NO_CONNECTION) {
                            mRefresher.setRefreshing(false);
                            Toast.makeText(IssueActivity2.this, error.resId, Toast.LENGTH_SHORT).show();
                        } else {
                            if(issueCreationAttempts < 5) {
                                issueCreationAttempts++;
                                mEditor.updateIssue(this, mIssue.getRepoPath(), issue, assignees, labels);
                            } else {
                                Toast.makeText(IssueActivity2.this, error.resId, Toast.LENGTH_SHORT).show();
                                mRefresher.setRefreshing(false);
                            }
                        }

                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                        mAnalytics.logEvent(Analytics.TAG_ISSUE_EDIT, bundle);
                    }
                }, mIssue.getRepoPath(), issue, assignees, labels);
            } else if(requestCode == CommentEditor.REQUEST_CODE_NEW_COMMENT) {
                mRefresher.setRefreshing(true);
                final Comment comment = data.getParcelableExtra(getString(R.string.parcel_comment));
                mEditor.createComment(new Editor.GITModelCreationListener<Comment>() {
                    @Override
                    public void created(Comment comment) {
                        mRefresher.setRefreshing(false);
                       // mAdapter.addComment(comment);
                        mScrollView.post(() -> mScrollView.smoothScrollTo(0, mScrollView.getBottom()));
                    }

                    @Override
                    public void creationError(APIHandler.APIError error) {
                        mRefresher.setRefreshing(false);
                    }
                }, mIssue.getRepoPath(), mIssue.getNumber(), comment.getBody());
            } else if(requestCode == CommentEditor.REQUEST_CODE_EDIT_COMMENT) {
                mRefresher.setRefreshing(true);
                final Comment comment = data.getParcelableExtra(getString(R.string.parcel_comment));
                mEditor.updateComment(new Editor.GITModelUpdateListener<Comment>() {
                    @Override
                    public void updated(Comment comment) {
                        mRefresher.setRefreshing(false);
                     //   mAdapter.updateComment(comment);
                    }

                    @Override
                    public void updateError(APIHandler.APIError error) {
                        mRefresher.setRefreshing(false);
                    }
                }, mIssue.getRepoPath(), comment.getId(), comment.getBody());
            } else if(requestCode == CommentEditor.REQUEST_CODE_COMMENT_FOR_STATE) {
                mRefresher.setRefreshing(true);
                final Comment comment = data.getParcelableExtra(getString(R.string.parcel_comment));
                mEditor.createComment(new Editor.GITModelCreationListener<Comment>() {
                    @Override
                    public void created(Comment comment) {
                     //   mLoader.loadComments(mCommentLoader, mIssue.getRepoPath(), mIssue.getNumber());
                    }

                    @Override
                    public void creationError(APIHandler.APIError error) {
                        if(error == APIHandler.APIError.NO_CONNECTION) {
                            mRefresher.setRefreshing(false);
                            Toast.makeText(IssueActivity2.this, error.resId, Toast.LENGTH_SHORT).show();
                        }
                    }
                }, mIssue.getRepoPath(), mIssue.getNumber(), comment.getBody());
            }
        }
    }

    // TODO Diff events and comments rather than just clearing
    private void toggleIssueState() {
        final Editor.GITModelUpdateListener<Issue> listener = new Editor.GITModelUpdateListener<Issue>() {
            @Override
            public void updated(Issue issue) {



                mIssue = issue;
                mImageState.setImageResource(mIssue.isClosed() ? R.drawable.ic_state_closed : R.drawable.ic_state_open);
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                mAnalytics.logEvent(issue.isClosed() ? Analytics.TAG_ISSUE_CLOSED : Analytics.TAG_ISSUE_OPENED, bundle);
            }

            @Override
            public void updateError(APIHandler.APIError error) {
                mRefresher.setRefreshing(false);
                if(error == APIHandler.APIError.NO_CONNECTION) {
                    mRefresher.setRefreshing(false);
                    Toast.makeText(IssueActivity2.this, error.resId, Toast.LENGTH_SHORT).show();
                }

                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                mAnalytics.logEvent(Analytics.TAG_ISSUE_EDIT, bundle);
            }
        };

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_state_change_comment);
        builder.setPositiveButton(R.string.action_ok, (dialog, which) -> {
            final Intent i = new Intent(IssueActivity2.this, CommentEditor.class);
            startActivityForResult(i, CommentEditor.REQUEST_CODE_COMMENT_FOR_STATE);
            mRefresher.setRefreshing(true);
            if(mIssue.isClosed()) {
                mEditor.openIssue(listener, mIssue.getRepoPath(), mIssue.getNumber());
            } else {
                mEditor.closeIssue(listener, mIssue.getRepoPath(), mIssue.getNumber());
            }
        });
        builder.setNeutralButton(R.string.action_no, (dialog, which) -> {
            mRefresher.setRefreshing(true);
            if(mIssue.isClosed()) {
                mEditor.openIssue(listener, mIssue.getRepoPath(), mIssue.getNumber());
            } else {
                mEditor.closeIssue(listener, mIssue.getRepoPath(), mIssue.getNumber());
            }
        });
        builder.setNegativeButton(R.string.action_cancel, null);
        builder.create().show();
    }

    private class IssueFragmentAdapter extends FragmentPagerAdapter {

        public IssueFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if(position == 0) {
                return IssueCommentsFragment.getInstance(mRefresher, mRefreshListener);
            } else {
                return new IssueEventsFragment();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}