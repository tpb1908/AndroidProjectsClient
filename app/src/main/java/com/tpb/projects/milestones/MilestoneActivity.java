package com.tpb.projects.milestones;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.widget.ImageView;

import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Loader;
import com.tpb.github.data.models.Issue;
import com.tpb.github.data.models.Milestone;
import com.tpb.github.data.models.State;
import com.tpb.mdtext.Markdown;
import com.tpb.mdtext.views.MarkdownTextView;
import com.tpb.projects.R;
import com.tpb.projects.common.CircularRevealActivity;
import com.tpb.projects.common.NetworkImageView;
import com.tpb.projects.flow.IntentHandler;
import com.tpb.projects.util.SettingsActivity;
import com.tpb.projects.util.UI;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 04/03/17.
 */

public class MilestoneActivity extends CircularRevealActivity implements Loader.ItemLoader<Milestone>, Loader.ListLoader<Issue> {

    @BindView(R.id.milestone_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.milestone_issues_recycler) RecyclerView mRecycler;
    @BindView(R.id.milestone_user_avatar) NetworkImageView mAvatar;
    @BindView(R.id.milestone_drawable) ImageView mStateImage;
    @BindView(R.id.milestone_content_markdown) MarkdownTextView mContent;

    private String mRepo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences
                .getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        UI.setStatusBarColor(getWindow(), getResources().getColor(R.color.colorPrimaryDark));
        setContentView(R.layout.activity_milestone);
        ButterKnife.bind(this);

        final Intent launchIntent = getIntent();
        if(launchIntent.hasExtra(getString(R.string.parcel_milestone))) {
            loadComplete(launchIntent.getParcelableExtra(getString(R.string.parcel_milestone)));
        } else if(launchIntent.hasExtra(getString(R.string.intent_repo)) && launchIntent
                .hasExtra(getString(R.string.intent_milestone_number))) {
            mRepo = launchIntent.getStringExtra(getString(R.string.intent_repo));
            final int number = launchIntent
                    .getIntExtra(getString(R.string.intent_milestone_number), -1);

            Loader.getLoader(this).loadMilestone(this, mRepo, number);
        } else if(launchIntent.hasExtra(getString(R.string.intent_repo))) {
            //TODO Create new milestone
        } else {
            finish();
        }

    }

    @Override
    public void loadComplete(Milestone milestone) {
        IntentHandler.addOnClickHandler(this, mContent, mAvatar, milestone.getCreator().getLogin());
        mStateImage.setImageResource(milestone
                .getState() == State.OPEN ? R.drawable.ic_state_open : R.drawable.ic_state_closed);
        mAvatar.setImageUrl(milestone.getCreator().getAvatarUrl());


        final StringBuilder builder = new StringBuilder();

        builder.append("<b>");
        builder.append(Markdown.escape(milestone.getTitle()));
        builder.append("</b>");
        builder.append("<br>");

        if(milestone.getDescription() != null) {
            builder.append("<br>");
            builder.append(milestone.getDescription());
            builder.append("<br>");
        }

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
                                "https://github.com/" + milestone.getCreator().getLogin(),
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
        if(milestone.getClosedAt() != 0) {
            builder.append("<br>");
            builder.append(
                    String.format(
                            getString(R.string.text_milestone_closed_at),
                            DateUtils.getRelativeTimeSpanString(milestone.getClosedAt())
                    )
            );
        }
        if(milestone.getDueOn() != 0) {
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
        mContent.setMarkdown(Markdown.formatMD(builder.toString(), mRepo, true));
    }

    @Override
    public void loadError(APIHandler.APIError error) {

    }

    @Override
    public void listLoadError(APIHandler.APIError error) {

    }

    @Override
    public void listLoadComplete(List<Issue> data) {

    }
}
