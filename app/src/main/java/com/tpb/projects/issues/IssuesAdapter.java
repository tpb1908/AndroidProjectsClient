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

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.tpb.projects.R;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.data.models.User;
import com.tpb.projects.util.Data;

import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 27/01/17.
 */

class IssuesAdapter extends RecyclerView.Adapter<IssuesAdapter.IssueHolder> {
    private static final String TAG = IssuesAdapter.class.getSimpleName();

    private IssuesActivity mParent;
    private ArrayList<Issue> mIssues = new ArrayList<>();
    private ArrayList<String> mParseCache = new ArrayList<>();

    private static final HandlerThread parseThread = new HandlerThread("card_parser");
    static {
        parseThread.start();
    }
    private static final Handler mParseHandler = new Handler(parseThread.getLooper());
    private Handler mUiHandler;

    IssuesAdapter(IssuesActivity parent) {
        mParent = parent;
        mUiHandler = new Handler(Looper.getMainLooper());
    }

    void loadIssues(Issue[] issues) {
        mIssues.clear();
        mParseCache.clear();
        for(Issue i : issues) {
            mIssues.add(i);
            mParseCache.add(null);
        }
        notifyDataSetChanged();
    }

    void addIssue(Issue issue) {
        mIssues.add(0, issue);
        mParseCache.add(0, null);
        notifyItemInserted(0);
    }

    void addIssues(Issue[] newIssues) {
        final int oldSize = mIssues.size();
        for(Issue i : newIssues) {
            mIssues.add(i);
            mParseCache.add(null);
        }
        notifyItemRangeInserted(oldSize, mIssues.size());
    }

    void clear() {
        mIssues.clear();
        notifyDataSetChanged();
    }

    void updateIssue(Issue issue) {
        int index = mIssues.indexOf(issue);
        if(index != -1) {
            mIssues.set(index, issue);
            mParseCache.set(index, null);
            notifyItemChanged(index);
        }
    }

    @Override
    public IssueHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new IssueHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_issue, parent, false));
    }

    @Override
    public void onBindViewHolder(IssueHolder holder, int position) {
        final Issue issue = mIssues.get(position);
        holder.mIssueIcon.setVisibility(View.VISIBLE);
        holder.mIssueIcon.setImageResource(issue.isClosed() ? R.drawable.ic_issue_closed : R.drawable.ic_issue_open);
        if(mParseCache.get(position) == null) {
            final Context context = holder.itemView.getContext();
            final StringBuilder builder = new StringBuilder();
            builder.append("<b>");
            builder.append(issue.getTitle());
            builder.append("</b><br><br>");

            builder.append(String.format(context.getString(R.string.text_opened_by),
                    String.format(context.getString(R.string.text_md_link),
                            "#" + Integer.toString(issue.getNumber()),
                            "https://github.com/" + issue.getRepoPath() + "/issues/" + Integer.toString(issue.getNumber())
                    ),
                    String.format(context.getString(R.string.text_md_link),
                            issue.getOpenedBy().getLogin(),
                            issue.getOpenedBy().getHtmlUrl()
                    ),
                    DateUtils.getRelativeTimeSpanString(issue.getCreatedAt()))
            );

            if(issue.getAssignees() != null) {
                builder.append("<br>");
                builder.append(context.getString(R.string.text_assigned_to));
                builder.append(' ');
                for(User u : issue.getAssignees()) {
                    builder.append(String.format(context.getString(R.string.text_md_link),
                            u.getLogin(),
                            u.getHtmlUrl()));
                    builder.append(' ');
                }
            }

            if(issue.isClosed() && issue.getClosedBy() != null) {
                builder.append("<br>");
                builder.append(String.format(context.getString(R.string.text_closed_by_link),
                        issue.getClosedBy().getLogin(),
                        issue.getClosedBy().getHtmlUrl(),
                        DateUtils.getRelativeTimeSpanString(issue.getClosedAt())));
            }

            if(issue.getLabels() != null && issue.getLabels().length > 0) {
                builder.append("<br>");
                Label.appendLabels(builder, issue.getLabels(), "   ");
            }
            if(issue.getComments() > 0) {
                builder.append("<br>");
                builder.append(context.getResources().getQuantityString(R.plurals.text_issue_comment_count, issue.getComments(), issue.getComments()));
            }
            mParseHandler.post(() -> {
                final String parsed = Data.parseMD(builder.toString());
                mParseCache.set(position, parsed);
                //We can't just set content as the view may be off screen
                //Log.i(TAG, "onBindViewHolder: Completed parsing for " + position);
                //Log.i(TAG, "onBindViewHolder: Parsed: " + mIssues.get(position).toString());
                mUiHandler.post(() -> notifyItemChanged(position));
            });
        } else {
           // Log.i(TAG, "onBindViewHolder: Binding position " + position + " with\n" + mIssues.get(position).second);
            holder.mContent.setHtml(mParseCache.get(position));
            //TODO Replace with separate arraylists
        }
    }

    @Override
    public int getItemCount() {
        return mIssues.size();
    }

    private void openIssue(int pos) {
        Log.i(TAG, "openIssue: Opening issue " + mIssues.get(pos).toString());
        Log.i(TAG, "openIssue: Parsed text " + mParseCache.get(pos));
       // mParent.openIssue(mIssues.get(pos));
    }

    private void openMenu(View view, int pos) {
        mParent.openMenu(view, mIssues.get(pos));
    }

    public class IssueHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.issue_content_markdown) HtmlTextView mContent;
        @BindView(R.id.issue_menu_button) ImageButton mMenuButton;
        @BindView(R.id.issue_drawable) ImageView mIssueIcon;

        IssueHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            mContent.setShowUnderLines(false);
            mContent.setParseHandler(mParseHandler);
            mMenuButton.setOnClickListener((v) -> openMenu(v, getAdapterPosition()));
            view.setOnClickListener((v) -> openIssue(getAdapterPosition()));
        }


    }
}
