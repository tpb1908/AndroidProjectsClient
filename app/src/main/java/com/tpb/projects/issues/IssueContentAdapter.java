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

import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.tpb.projects.R;
import com.tpb.projects.data.models.Comment;
import com.tpb.projects.data.models.DataModel;
import com.tpb.projects.data.models.Event;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.util.Data;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 07/01/17.
 */

class IssueContentAdapter extends RecyclerView.Adapter {

    private ArrayList<DataModel> mData = new ArrayList<>();
    private Issue mIssue;

    private static final Parser parser = Parser.builder().build();
    private static final HtmlRenderer renderer = HtmlRenderer.builder().build();

    void setIssue(Issue issue) {
        mIssue = issue;
    }

    void loadComments(Comment[] comments) {
        if(mData.size() == 0) {
            mData = new ArrayList<>(Arrays.asList(comments));
            notifyDataSetChanged();
        } else {
            mData.addAll(Arrays.asList(comments));
            Collections.sort(mData, comparator);
        }
    }

    void loadEvents(Event[] events) {
        if(mData.size() == 0) {
            mData = new ArrayList<>(Arrays.asList(events));
            notifyDataSetChanged();
        } else {
            mData.addAll(Arrays.asList(events));
            Collections.sort(mData, comparator);
        }
    }

    private Comparator<DataModel> comparator = (d1, d2) -> (d1 instanceof Comment ? ((Comment) d1).getCreatedAt() :
            ((Event) d1).getCreatedAt()) > (d2 instanceof Comment ? ((Comment) d2).getCreatedAt() :
            ((Event) d2).getCreatedAt()) ?
            1 : 0;

    @Override
    public int getItemViewType(int position) {
        return mData.get(position) instanceof Comment ? 1 : 0;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == 1) {
            return new CommentHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_comment, parent, false));
        } else {
            return new EventHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_event, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof CommentHolder) {
            bindComment((CommentHolder) holder, (Comment) mData.get(position));
        } else {
            bindEvent((EventHolder) holder, (Event) mData.get(position));
        }
    }

    private void bindComment(CommentHolder commentHolder, Comment comment) {
        final StringBuilder builder = new StringBuilder();
        builder.append(String.format(commentHolder.itemView.getResources().getString(R.string.text_comment_by),
                String.format(commentHolder.itemView.getResources().getString(R.string.text_href),
                        comment.getUser().getHtmlUrl(),
                        comment.getUser().getLogin()),
                DateUtils.getRelativeTimeSpanString(comment.getCreatedAt())));
        if(comment.getUpdatedAt() != comment.getCreatedAt()) {
            builder.append(" • ");
            builder.append(commentHolder.itemView.getResources().getString(R.string.text_comment_edited));
        }
        builder.append("<br><br>");
        builder.append(Data.formatMD(comment.getBody(), mIssue.getRepoPath()));
        commentHolder.mText.setHtml(renderer.render(parser.parse(builder.toString())), new HtmlHttpImageGetter(commentHolder.mText));
    }

    private void bindEvent(EventHolder eventHolder, Event event) {
        eventHolder.mText.setHtml(renderer.render(parser.parse(event.getEvent().toString())));
    }

    class CommentHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.comment_text) HtmlTextView mText;
        @BindView(R.id.comment_menu_button) ImageButton mMenu;

        CommentHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            mText.setShowUnderLines(false);
        }

    }

    class EventHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.event_text) HtmlTextView mText;

        EventHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            mText.setShowUnderLines(false);
        }

    }
    public int getItemCount() {
        return mData.size();
    }
}
