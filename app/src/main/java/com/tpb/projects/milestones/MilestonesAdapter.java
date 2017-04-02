package com.tpb.projects.milestones;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.tpb.github.data.models.Milestone;
import com.tpb.github.data.models.State;
import com.tpb.projects.R;
import com.tpb.projects.flow.IntentHandler;
import com.tpb.mdtext.Markdown;
import com.tpb.projects.util.NetworkImageView;

import com.tpb.mdtext.mdtextview.MarkdownTextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 04/03/17.
 */

public class MilestonesAdapter extends RecyclerView.Adapter<MilestonesAdapter.MilestoneHolder> {

    private Activity mParent;
    private String mRepo;

    private ArrayList<Milestone> mMilestones = new ArrayList<>();
    private ArrayList<SpannableString> mParseCache = new ArrayList<>();

    MilestonesAdapter(Activity parent, String repo) {
        mParent = parent;
        mRepo = repo;
    }

    void clear() {
        mMilestones.clear();
        notifyDataSetChanged();
    }

    void setMilestones(List<Milestone> milestones) {
        mMilestones.clear();
        mParseCache.clear();
        for(Milestone m : milestones) {
            mMilestones.add(m);
            mParseCache.add(null);
        }
        notifyItemRangeInserted(0, mMilestones.size());
    }

    void addMilestones(List<Milestone> milestones) {
        final int oldSize = mMilestones.size();
        for(Milestone m : milestones) {
            mMilestones.add(m);
            mParseCache.add(null);
        }
        notifyItemRangeInserted(oldSize, mMilestones.size());
    }

    void addMilestone(Milestone milestone) {
        mMilestones.add(0, milestone);
        mParseCache.add(0, null);
        notifyItemInserted(0);
    }

    void updateMilestone(Milestone milestone) {
        final int index = mMilestones.indexOf(milestone);
        if(index != -1) {
            mMilestones.set(index, milestone);
            mParseCache.set(index, null);
            notifyItemChanged(index);
        }
    }

    @Override
    public MilestoneHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MilestoneHolder(LayoutInflater.from(parent.getContext())
                                                 .inflate(R.layout.viewholder_milestone, parent,
                                                         false
                                                 ));
    }

    @Override
    public void onBindViewHolder(MilestoneHolder holder, int position) {
        if(mParseCache.get(position) == null) {
            final Milestone milestone = mMilestones.get(position);

            IntentHandler.addOnClickHandler(mParent, holder.mContent, holder.mAvatar,
                    milestone.getCreator().getLogin()
            );
            holder.mState.setImageResource(milestone
                    .getState() == State.OPEN ? R.drawable.ic_state_open : R.drawable.ic_state_closed);
            holder.mAvatar.setImageUrl(milestone.getCreator().getAvatarUrl());

            final Resources res = holder.itemView.getResources();
            final StringBuilder builder = new StringBuilder();

            builder.append("<b>");
            builder.append(Markdown.escape(milestone.getTitle()));
            builder.append("</b>");
            builder.append("<br>");
            if(milestone.getOpenIssues() > 0 || milestone.getClosedIssues() > 0) {
                builder.append("<br>");
                builder.append(String.format(res.getString(R.string.text_milestone_completion),
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
                            res.getString(R.string.text_milestone_opened_by),
                            String.format(res.getString(R.string.text_href),
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
                                res.getString(R.string.text_last_updated),
                                DateUtils.getRelativeTimeSpanString(milestone.getUpdatedAt())
                        )
                );
            }
            if(milestone.getClosedAt() != 0) {
                builder.append("<br>");
                builder.append(
                        String.format(
                                res.getString(R.string.text_milestone_closed_at),
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
                                    res.getString(R.string.text_milestone_due_on),
                                    DateUtils.getRelativeTimeSpanString(milestone.getDueOn())
                            )
                    );
                } else {
                    builder.append("<font color=\"");
                    builder.append(String.format("#%06X", (0xFFFFFF & Color.RED)));
                    builder.append("\">");
                    builder.append(
                            String.format(
                                    res.getString(R.string.text_milestone_due_on),
                                    DateUtils.getRelativeTimeSpanString(milestone.getDueOn())
                            )
                    );
                    builder.append("</font>");
                }
            }
            holder.mContent.setMarkdown(Markdown.formatMD(builder.toString(), null),
                    null,
                    text -> mParseCache.set(position, text)
            );
        } else {
            holder.mContent.setText(mParseCache.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return mMilestones.size();
    }

    private void openMilestone(View view, int pos) {
        IntentHandler.openMilestone(mParent, view, mMilestones.get(pos));
    }

    public class MilestoneHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.milestone_drawable) ImageView mState;
        @BindView(R.id.milestone_user_avatar) NetworkImageView mAvatar;
        @BindView(R.id.milestone_content_markdown) MarkdownTextView mContent;
        @BindView(R.id.milestone_menu_button) ImageButton mMenu;

        public MilestoneHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(v -> openMilestone(v, getAdapterPosition()));
        }


    }

}
