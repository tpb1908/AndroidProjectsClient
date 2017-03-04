package com.tpb.projects.milestones;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.androidnetworking.widget.ANImageView;
import com.tpb.projects.R;

import org.sufficientlysecure.htmltext.htmltextview.HtmlTextView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 04/03/17.
 */

public class MilestonesAdapter extends RecyclerView.Adapter<MilestonesAdapter.MilestoneHolder> {

    @Override
    public MilestoneHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MilestoneHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_milestone, parent, false));
    }

    @Override
    public void onBindViewHolder(MilestoneHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public class MilestoneHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.milestone_drawable) ImageView mState;
        @BindView(R.id.milestone_user_avatar) ANImageView mAvatar;
        @BindView(R.id.milestone_content_markdown) HtmlTextView mContent;
        @BindView(R.id.milestone_menu_button) ImageButton mMenu;

        public MilestoneHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }


    }

}
