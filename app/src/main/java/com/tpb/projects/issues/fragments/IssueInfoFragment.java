package com.tpb.projects.issues.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Editor;
import com.tpb.github.data.Loader;
import com.tpb.github.data.models.Issue;
import com.tpb.github.data.models.Milestone;
import com.tpb.github.data.models.Repository;
import com.tpb.github.data.models.State;
import com.tpb.github.data.models.User;
import com.tpb.mdtext.Markdown;
import com.tpb.mdtext.imagegetter.HttpImageGetter;
import com.tpb.mdtext.views.MarkdownTextView;
import com.tpb.projects.R;
import com.tpb.projects.common.FixedLinearLayoutManger;
import com.tpb.projects.common.NetworkImageView;
import com.tpb.projects.editors.CommentEditor;
import com.tpb.projects.editors.IssueEditor;
import com.tpb.projects.flow.IntentHandler;
import com.tpb.projects.issues.IssueActivity;
import com.tpb.projects.issues.IssueEventsAdapter;
import com.tpb.projects.markdown.Formatter;
import com.tpb.projects.user.UserActivity;
import com.tpb.projects.util.UI;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by theo on 14/03/17.
 */

public class IssueInfoFragment extends IssueFragment {

    private Unbinder unbinder;

    @BindView(R.id.issue_header_card) CardView mHeader;
    @BindView(R.id.issue_events_recycler) RecyclerView mRecycler;
    @BindView(R.id.issue_assignees) LinearLayout mAssigneesLayout;
    @BindView(R.id.text_issue_assignees) View mAssigneesTitle;
    @BindView(R.id.issue_menu_button) ImageButton mOverflowButton;
    @BindView(R.id.issue_user_avatar) NetworkImageView mUserAvatar;
    @BindView(R.id.issue_state) ImageView mImageState;
    @BindView(R.id.issue_title) MarkdownTextView mTitle;
    @BindView(R.id.issue_info) MarkdownTextView mInfo;
    @BindView(R.id.issue_events_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.viewholder_milestone_card) CardView mMilestoneCard;

    private Issue mIssue;

    private Repository.AccessLevel mAccessLevel = Repository.AccessLevel.NONE;
    private Editor mEditor;

    private IssueEventsAdapter mAdapter;

    public static IssueInfoFragment getInstance() {
        return new IssueInfoFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_issue_info, container, false);
        unbinder = ButterKnife.bind(this, view);
        mAccessLevel = ((IssueActivity) getActivity()).mAccessLevel;
        mEditor = Editor.getEditor(getContext());
        mAdapter = new IssueEventsAdapter(this, mRefresher);
        final LinearLayoutManager manager = new FixedLinearLayoutManger(getContext());
        mRecycler.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(manager.findFirstVisibleItemPosition() + 20 > manager.getItemCount()) {
                    mAdapter.notifyBottomReached();
                }
            }
        });
        mRecycler.setAdapter(mAdapter);
        mRecycler.setLayoutManager(manager);


        mRefresher.setOnRefreshListener(() -> {
            mAdapter.clear();
            Loader.getLoader(getContext()).loadIssue(new Loader.ItemLoader<Issue>() {
                @Override
                public void loadComplete(Issue issue) {
                    issueLoaded(issue);
                }

                @Override
                public void loadError(APIHandler.APIError error) {
                    mRefresher.setRefreshing(false);
                }
            }, mIssue.getRepoFullName(), mIssue.getNumber(), true);
        });
        checkSharedElementEntry();
        if(mIssue != null) issueLoaded(mIssue);
        return view;
    }


    @Override
    public void issueLoaded(Issue issue) {
        mIssue = issue;
        if(mAdapter != null) {
            mAdapter.setIssue(issue);
            displayIssue();
            displayAssignees();
            displayMilestone();
        }
    }

    private void displayIssue() {
        mTitle.setMarkdown(Formatter.header(mIssue.getTitle(), 1));
        mInfo.setMarkdown(
                Formatter.buildIssueSpan(
                        getContext(),
                        mIssue,
                        false, //Show title
                        false, //No numbered link
                        false, //No assignees
                        true, //Closed at
                        false //No comment count
                ).toString(),
                new HttpImageGetter(mInfo), null
        );
        mUserAvatar.setImageUrl(mIssue.getOpenedBy().getAvatarUrl());
        IntentHandler.addOnClickHandler(getActivity(), mUserAvatar, mIssue.getOpenedBy().getLogin());
        mImageState.setOnClickListener(v -> toggleIssueState());
        if(mIssue.isClosed()) {
            mImageState.setImageResource(R.drawable.ic_state_closed);
        } else {
            mImageState.setImageResource(R.drawable.ic_state_open);
        }
    }

    private void displayAssignees() {
        mAssigneesLayout.removeAllViews();
        if(mIssue != null && mIssue.getAssignees() != null && mIssue.getAssignees().length > 0) {
            mAssigneesLayout.setVisibility(View.VISIBLE);
            mAssigneesTitle.setVisibility(View.VISIBLE);
            for(User u : mIssue.getAssignees()) {
                final LinearLayout user = (LinearLayout) getActivity().getLayoutInflater()
                                                                      .inflate(R.layout.shard_user,
                                                                              mAssigneesLayout,
                                                                              false
                                                                      );
                user.setId(View.generateViewId());
                mAssigneesLayout.addView(user);
                final NetworkImageView avatar = ButterKnife.findById(user, R.id.user_avatar);
                avatar.setId(View.generateViewId());
                avatar.setImageUrl(u.getAvatarUrl());
                avatar.setScaleType(ImageView.ScaleType.FIT_XY);
                final TextView login = ButterKnife.findById(user, R.id.user_login);
                login.setId(View.generateViewId());
                login.setText(u.getLogin());
                user.setOnClickListener((v) -> {
                    final Intent us = new Intent(getActivity(), UserActivity.class);
                    us.putExtra(getString(R.string.intent_username), u.getLogin());
                    UI.setDrawableForIntent(avatar, us);
                    getActivity().startActivity(us,
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    getActivity(),
                                    Pair.create(login, getString(R.string.transition_username)),
                                    Pair.create(avatar, getString(R.string.transition_user_image))
                            ).toBundle()
                    );
                });
            }
        } else {
            mAssigneesLayout.setVisibility(View.GONE);
            mAssigneesTitle.setVisibility(View.GONE);
        }
    }

    private void displayMilestone() {
        final Milestone milestone = mIssue.getMilestone();
        if(milestone != null) {
            mMilestoneCard.setVisibility(View.VISIBLE);
            final MarkdownTextView tv = ButterKnife
                    .findById(mMilestoneCard, R.id.milestone_content_markdown);
            final ImageView status = ButterKnife.findById(mMilestoneCard, R.id.milestone_drawable);
            final NetworkImageView user = ButterKnife
                    .findById(mMilestoneCard, R.id.milestone_user_avatar);
            IntentHandler
                    .addOnClickHandler(getActivity(), tv, user, milestone.getCreator().getLogin());
            IntentHandler.addOnClickHandler(getActivity(), user, milestone.getCreator().getLogin());
            status.setImageResource(milestone
                    .getState() == State.OPEN ? R.drawable.ic_state_open : R.drawable.ic_state_closed);
            user.setImageUrl(milestone.getCreator().getAvatarUrl());

            final StringBuilder builder = new StringBuilder();

            Formatter.bold(Markdown.escape(milestone.getTitle()));
            builder.append("<br>");
            if(milestone.getOpenIssues() > 0 || milestone.getClosedIssues() > 0) {
                builder.append("<br>");
                builder.append(String.format(getString(R.string.text_milestone_completion),
                        milestone.getOpenIssues(),
                        milestone.getClosedIssues(),
                        Math.round(100f * milestone.getClosedIssues() / (milestone
                                .getOpenIssues() + milestone.getClosedIssues()))
                        )
                );
            }
            builder.append("<br>");
            builder.append(
                    String.format(
                            getString(R.string.text_milestone_opened_by),
                            String.format(getString(R.string.text_href),
                                    "https://github.com/" + milestone.getCreator()
                                                                  .getLogin(),
                                    milestone.getCreator().getLogin()
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
                        (milestone.getClosedAt() != 0 && milestone.getClosedAt() < milestone
                                .getDueOn())) {
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
            tv.setMarkdown(Markdown.formatMD(builder.toString(), mIssue.getRepoFullName()));

        } else {
            mMilestoneCard.setVisibility(View.GONE);
        }
    }

    public void updateIssue(Issue issue, String[] assignees, String[] labels) {
        mRefresher.setRefreshing(true);
        mEditor.updateIssue(new Editor.UpdateListener<Issue>() {
            int issueCreationAttempts = 0;

            @Override
            public void updated(Issue issue) {
                int matchCount = 0;
                final Issue old = mIssue;
                mIssue = issue;
                if(old.getAssignees() != null && mIssue.getAssignees() != null) {
                    for(User u : old.getAssignees()) {
                        for(User v : old.getAssignees()) {
                            if(u.equals(v)) matchCount++;
                        }
                    }
                    if(matchCount != old.getAssignees().length || 
                            matchCount != mIssue.getAssignees().length) {
                        displayAssignees();
                    }
                }

                displayIssue();
                displayMilestone();
                mRefresher.setRefreshing(false);
            }

            @Override
            public void updateError(APIHandler.APIError error) {
                if(error == APIHandler.APIError.NO_CONNECTION) {
                    mRefresher.setRefreshing(false);
                    Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
                } else {
                    if(issueCreationAttempts < 5) {
                        issueCreationAttempts++;
                        mEditor.updateIssue(this, mIssue.getRepoFullName(), issue, assignees,
                                labels
                        );
                    } else {
                        Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
                        mRefresher.setRefreshing(false);
                    }
                }
            }
        }, mIssue.getRepoFullName(), issue, assignees, labels);
    }

    private void editIssue(View view) {
        final Intent i = new Intent(getContext(), IssueEditor.class);
        i.putExtra(getString(R.string.intent_repo), mIssue.getRepoFullName());
        i.putExtra(getString(R.string.parcel_issue), mIssue);
        UI.setViewPositionForIntent(i, view);
        startActivityForResult(i, IssueEditor.REQUEST_CODE_EDIT_ISSUE);
    }

    private void toggleIssueState() {
        if(mIssue == null || mAccessLevel != Repository.AccessLevel.ADMIN) return;
        final Editor.UpdateListener<Issue> listener = new Editor.UpdateListener<Issue>() {
            @Override
            public void updated(Issue issue) {
                mIssue = issue;
                mImageState.setImageResource(
                        mIssue.isClosed() ? R.drawable.ic_state_closed : R.drawable.ic_state_open);
            }

            @Override
            public void updateError(APIHandler.APIError error) {
                mRefresher.setRefreshing(false);
                if(error == APIHandler.APIError.NO_CONNECTION) {
                    mRefresher.setRefreshing(false);
                    Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
                }
            }
        };

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.title_state_change_comment);
        builder.setPositiveButton(R.string.action_ok, (dialog, which) -> {
            final Intent i = new Intent(getContext(), CommentEditor.class);
            startActivityForResult(i, CommentEditor.REQUEST_CODE_COMMENT_FOR_STATE);
            mRefresher.setRefreshing(true);
            if(mIssue.isClosed()) {
                mEditor.openIssue(listener, mIssue.getRepoFullName(), mIssue.getNumber());
            } else {
                mEditor.closeIssue(listener, mIssue.getRepoFullName(), mIssue.getNumber());
            }
        });
        builder.setNeutralButton(R.string.action_no, (dialog, which) -> {
            mRefresher.setRefreshing(true);
            if(mIssue.isClosed()) {
                mEditor.openIssue(listener, mIssue.getRepoFullName(), mIssue.getNumber());
            } else {
                mEditor.closeIssue(listener, mIssue.getRepoFullName(), mIssue.getNumber());
            }
        });
        builder.setNegativeButton(R.string.action_cancel, null);
        builder.create().show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK) {
            if(requestCode == IssueEditor.REQUEST_CODE_EDIT_ISSUE) {
                final Issue issue = data.getParcelableExtra(getString(R.string.parcel_issue));
                final String[] assignees;
                final String[] labels;
                if(data.hasExtra(getString(R.string.intent_issue_assignees))) {
                    assignees = data
                            .getStringArrayExtra(getString(R.string.intent_issue_assignees));
                } else {
                    assignees = null;
                }
                if(data.hasExtra(getString(R.string.intent_issue_labels))) {
                    labels = data.getStringArrayExtra(getString(R.string.intent_issue_labels));
                } else {
                    labels = null;
                }
                updateIssue(issue, assignees, labels);
            }
        }
    }

    @OnClick({R.id.issue_header_card, R.id.issue_info, R.id.issue_title})
    void onHeaderClick() {
        if(mIssue != null && mAccessLevel == Repository.AccessLevel.ADMIN) editIssue(mInfo);
    }

    @OnClick(R.id.issue_menu_button)
    public void displayIssueMenu(View view) {
        final PopupMenu menu = new PopupMenu(getContext(), view);
        menu.inflate(R.menu.menu_issue);
        if(mAccessLevel == Repository.AccessLevel.ADMIN) {
            menu.getMenu().add(0, 1, Menu.NONE,
                    mIssue.isClosed() ? R.string.menu_reopen_issue : R.string.menu_close_issue
            );
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
                case R.id.menu_fullscreen:
                    IntentHandler.showFullScreen(getContext(), mIssue.getBody(),
                            mIssue.getRepoFullName(), getFragmentManager()
                    );
                    break;
            }
            return false;
        });
        menu.show();
    }

    @Override
    public void setAccessLevel(Repository.AccessLevel level) {
        mAccessLevel = level;
    }

    private void checkSharedElementEntry() {
        final Intent i = getActivity().getIntent();
        if(i.hasExtra(getString(R.string.transition_card))) {
            mHeader.getViewTreeObserver()
                   .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                       @Override
                       public boolean onPreDraw() {
                           mHeader.getViewTreeObserver().removeOnPreDrawListener(this);
                           if(i.hasExtra(getString(R.string.intent_drawable))) {
                               mUserAvatar.setImageBitmap(
                                       i.getParcelableExtra(getString(R.string.intent_drawable)));
                           }
                           getActivity().startPostponedEnterTransition();
                           return true;
                       }
                   });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
