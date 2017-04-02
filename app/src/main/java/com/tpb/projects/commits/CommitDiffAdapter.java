package com.tpb.projects.commits;

import android.animation.ObjectAnimator;
import android.content.res.Resources;
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
        final Resources res = holder.itemView.getResources();
        holder.mInfo.setText(
                String.format(
                        res.getString(R.string.text_diff_changes),
                        mDiffs[position].getStatus(),
                        res.getQuantityString(R.plurals.text_commit_additions,
                                mDiffs[position].getAdditions(), mDiffs[position].getAdditions()
                        ),
                        res.getQuantityString(R.plurals.text_commit_deletions,
                                mDiffs[position].getDeletions(), mDiffs[position].getDeletions()
                        )
                )
        );
        if(mDiffs[position].getPatch() != null) {
            holder.mDiff.setVisibility(View.VISIBLE);
            holder.mDiff.setText(Spanner.buildDiffSpan(mDiffs[position].getPatch()));
            holder.mDiff.post(() -> {
                final int maxLines = holder.mDiff.getLineCount();
                holder.mDiff.setMaxLines(3);
                holder.itemView.setOnClickListener(v -> {
                    if(holder.mDiff.getLineCount() < maxLines) {
                        ObjectAnimator.ofInt(
                                holder.mDiff,
                                "maxLines",
                                3,
                                maxLines
                        ).setDuration(res.getInteger(android.R.integer.config_mediumAnimTime))
                                      .start();
                    } else {
                        ObjectAnimator.ofInt(
                                holder.mDiff,
                                "maxLines",
                                maxLines,
                                3
                        ).setDuration(res.getInteger(android.R.integer.config_mediumAnimTime))
                                      .start();
                    }
                });
            });


        } else {
            holder.mDiff.setVisibility(View.GONE);
        }
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
