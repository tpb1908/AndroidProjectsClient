package com.tpb.projects.commits;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.models.DiffFile;
import com.tpb.projects.markdown.Spanner;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 01/04/17.
 */

public class CommitDiffAdapter extends RecyclerView.Adapter<CommitDiffAdapter.DiffHolder> {

    private DiffFile[] mDiffs = new DiffFile[0];

    public void setDiffs(DiffFile[] diffs) {
        mDiffs = diffs;
        notifyItemRangeInserted(0, mDiffs.length);
    }

    public void clear() {
        final int size = mDiffs.length;
        mDiffs = new DiffFile[0];
        notifyItemRangeRemoved(0, size);
    }

    @Override
    public DiffHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DiffHolder(LayoutInflater.from(parent.getContext())
                                            .inflate(R.layout.viewholder_diff, parent, false));
    }

    @Override
    public void onBindViewHolder(DiffHolder holder, int position) {
        holder.mFileName.setText(mDiffs[position].getFileName());
        holder.itemView.setOnClickListener(v -> {
            if(holder.mDiff.getLineCount() > 1) {
                final ObjectAnimator anim = ObjectAnimator.ofInt(
                        holder.mDiff,
                        "maxLines",
                        holder.mDiff.getLineCount(),
                        1
                ).setDuration(holder.itemView.getContext().getResources()
                                             .getInteger(android.R.integer.config_mediumAnimTime));
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        holder.mDiff.setText(R.string.text_placeholder);
                        holder.mDiff.setMaxLines(Integer.MAX_VALUE);
                        super.onAnimationEnd(animation);
                    }
                });
                anim.start();

            } else {
                holder.mDiff.setText(Spanner.buildDiffSpan(mDiffs[position].getPatch()));
                ObjectAnimator.ofInt(
                        holder.mDiff,
                        "maxLines",
                        1,
                        holder.mDiff.getLineCount()
                ).setDuration(holder.itemView.getContext().getResources()
                                             .getInteger(android.R.integer.config_mediumAnimTime))
                              .start();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDiffs.length;
    }

    class DiffHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.diff_filename) TextView mFileName;
        @BindView(R.id.diff_info) TextView mInfo;
        @BindView(R.id.diff_diff) TextView mDiff;

        DiffHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

}
