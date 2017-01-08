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

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.tpb.projects.R;
import com.tpb.projects.data.models.Comment;
import com.tpb.projects.data.models.DataModel;
import com.tpb.projects.data.models.Event;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.MergedEvent;
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
    private static final String TAG = IssueContentAdapter.class.getSimpleName();

    private ArrayList<DataModel> mData = new ArrayList<>();
    private Issue mIssue;
    private IssueActivity mParent;

    IssueContentAdapter(IssueActivity parent) {
        mParent = parent;
    }

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
            notifyDataSetChanged();
        }
    }

    void loadEvents(Event[] events) {
        if(mData.size() == 0) {
            mData = new ArrayList<>(mergeEvents(events));
            notifyDataSetChanged();
        } else {
            mData.addAll(mergeEvents(events));
            Collections.sort(mData, comparator);
            notifyDataSetChanged();
        }

    }

    private ArrayList<DataModel> mergeEvents(Event[] events) {
        final ArrayList<DataModel> merged = new ArrayList<>();
        ArrayList<Event> toMerge = new ArrayList<>();
        Event last = new Event();
        for(int i = 0; i < events.length; i++) {
            if(events[i].getCreatedAt() == last.getCreatedAt() && events[i].getEvent() == last.getEvent()) {
                toMerge.add(events[i - 1]);
                int j = i;
                while(j < events.length && events[j].getCreatedAt() == last.getCreatedAt() && events[j].getEvent() == last.getEvent()) {
                    toMerge.add(events[j++]);
                }
                Log.i(TAG, "mergeEvents: Merging events from " + i + " to " + j);
                i = j - 1;
                merged.add(new MergedEvent(toMerge));
                Log.i(TAG, "mergeEvents: Merging " + toMerge.toString());
                toMerge = new ArrayList<>();
            } else {
                merged.add(events[i]);
            }
            last = events[i];
        }
        return merged;

    }

    //TODO Eww
    private Comparator<DataModel> comparator = (d1, d2) -> (d1 instanceof Comment ? ((Comment) d1).getCreatedAt() : (d1 instanceof Event ? ((Event) d1).getCreatedAt() : ((MergedEvent) d1).getCreatedAt())) > (d2 instanceof Comment ? ((Comment) d2).getCreatedAt() : (d2 instanceof Event ? ((Event) d2).getCreatedAt() : ((MergedEvent) d2).getCreatedAt())) ?
            1 : -1;

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
            if(mData.get(position) instanceof Event) {
                bindEvent((EventHolder) holder, (Event) mData.get(position));
            } else {
                bindMergedEvent((EventHolder) holder, (MergedEvent) mData.get(position));
            }
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

    private void bindMergedEvent(EventHolder eventHolder, MergedEvent me) {
        String text = "";
        final Resources res = eventHolder.itemView.getResources();
        switch(me.getEvent()) {
            case ASSIGNED:
                final StringBuilder assignees = new StringBuilder();
                for(Event e : me.getEvents()) {
                    assignees.append(String.format(res.getString(R.string.text_href),
                            e.getActor().getHtmlUrl(),
                            e.getActor().getLogin()));
                    assignees.append(", ");
                }
                assignees.setLength(assignees.length() - 2); //Remove final comma
                text = String.format(res.getString(R.string.text_event_assigned_multiple), assignees.toString());
                break;
            case UNASSIGNED:
                final StringBuilder unassignees = new StringBuilder();
                for(Event e : me.getEvents()) {
                    unassignees.append(String.format(res.getString(R.string.text_href),
                            e.getActor().getHtmlUrl(),
                            e.getActor().getLogin()));
                    unassignees.append(", ");
                }
                unassignees.setLength(unassignees.length() - 2); //Remove final comma
                text = String.format(res.getString(R.string.text_event_unassigned_multiple), unassignees.toString());
                break;
            case REVIEW_REQUESTED:
                final StringBuilder requested = new StringBuilder();
                for(Event e : me.getEvents()) {
                    requested.append(String.format(res.getString(R.string.text_href),
                            e.getReviewRequester().getHtmlUrl(),
                            e.getReviewRequester().getLogin()));
                    requested.append(", ");
                }
                requested.setLength(requested.length() - 2); //Remove final comma
                text = String.format(res.getString(R.string.text_event_review_requested_multiple),
                        String.format(res.getString(R.string.text_href),
                        me.getEvents().get(0).getReviewRequester().getHtmlUrl(),
                                me.getEvents().get(0).getReviewRequester().getLogin()),
                        requested.toString());
                break;
            case REVIEW_REQUEST_REMOVED:
                final StringBuilder derequested = new StringBuilder();
                for(Event e : me.getEvents()) {
                    derequested.append(String.format(res.getString(R.string.text_href),
                            e.getReviewRequester().getHtmlUrl(),
                            e.getReviewRequester().getLogin()));
                    derequested.append(", ");
                }
                derequested.setLength(derequested.length() - 2); //Remove final comma
                text = String.format(res.getString(R.string.text_event_review_request_removed_multiple),
                        String.format(res.getString(R.string.text_href),
                                me.getEvents().get(0).getActor().getHtmlUrl(),
                                me.getEvents().get(0).getActor().getLogin()),
                        derequested.toString());
                break;
            case CLOSED:
                //Duplicate close events seem to happen
                bindEvent(eventHolder, me.getEvents().get(0));
                return;
            default:
                Log.e(TAG, "bindMergedEvent: Should be binding merging event " + me.toString(), new Exception());
                bindEvent(eventHolder, me.getEvents().get(0));
        }
        eventHolder.mText.setHtml(renderer.render(parser.parse(text)));
    }

    private void bindEvent(EventHolder eventHolder, Event event) {
        String text;
        final Resources res = eventHolder.itemView.getResources();
        //TODO Merge events, for example multiple assignees to a single event
        switch(event.getEvent()) {
            case CLOSED:
                text = String.format(res.getString(R.string.text_event_closed),
                        String.format(res.getString(R.string.text_href),
                                event.getActor().getHtmlUrl(),
                                event.getActor().getLogin()));
                break;
            case REOPENED:
                text = String.format(res.getString(R.string.text_event_reopened),
                        String.format(res.getString(R.string.text_href),
                                event.getActor().getHtmlUrl(),
                                event.getActor().getLogin()));
                break;
            case SUBSCRIBED:
                text = String.format(res.getString(R.string.text_event_subscribed),
                        String.format(res.getString(R.string.text_href),
                                event.getActor().getHtmlUrl(),
                                event.getActor().getLogin()));
                break;
            case MERGED:
                text = String.format(res.getString(R.string.text_event_merged),
                        String.format(res.getString(R.string.text_href),
                                event.getActor().getHtmlUrl(),
                                event.getActor().getLogin()),
                        String.format(res.getString(R.string.text_href),
                                "https://github.com/" + mIssue.getRepoPath() + "/commit/" + event.getCommitId(),
                                res.getString(R.string.text_commit))
                );
                break;
            case REFERENCED:
                text = String.format(res.getString(R.string.text_event_referenced),
                        String.format(res.getString(R.string.text_href),
                                "https://github.com/" + mIssue.getRepoPath() + "/commit/" + event.getCommitId(),
                                res.getString(R.string.text_commit)));
                break;
            case MENTIONED:
                text = String.format(res.getString(R.string.text_event_mentioned),
                        String.format(res.getString(R.string.text_href),
                                event.getActor().getHtmlUrl(),
                                event.getActor().getLogin()));
                break;
            case ASSIGNED:
                text = String.format(res.getString(R.string.text_event_assigned),
                        String.format(res.getString(R.string.text_href),
                                event.getActor().getHtmlUrl(),
                                event.getActor().getLogin()));
                break;
            case UNASSIGNED:
                text = String.format(res.getString(R.string.text_event_unassigned),
                        String.format(res.getString(R.string.text_href),
                                event.getActor().getHtmlUrl(),
                                event.getActor().getLogin()));
                break;
            case LABELED:
                text = String.format(res.getString(R.string.text_event_labeled),
                        String.format(res.getString(R.string.text_href),
                                event.getActor().getHtmlUrl(),
                                event.getActor().getLogin()),
                        String.format(res.getString(R.string.text_label),
                                String.format("#%06X", (0xFFFFFF & event.getLabelColor())),
                                event.getLabelName()));
                break;
            case UNLABELED:
                text = String.format(res.getString(R.string.text_event_unlabeled),
                        String.format(res.getString(R.string.text_href),
                                event.getActor().getHtmlUrl(),
                                event.getActor().getLogin()),
                        String.format(res.getString(R.string.text_label),
                                String.format("#%06X", (0xFFFFFF & event.getLabelColor())),
                                event.getLabelName()));
                break;
            case MILESTONED:
                text = "Milestoned"; //TODO
                break;
            case DEMILESTONED:
                text = "De-milestoned"; //TODO
                break;
            case RENAMED:
                text = String.format(res.getString(R.string.text_event_renamed),
                        String.format(res.getString(R.string.text_href),
                                event.getActor().getHtmlUrl(),
                                event.getActor().getLogin()),
                        event.getRenameFrom(),
                        event.getRenameTo());
                break;
            case LOCKED:
                text = String.format(res.getString(R.string.text_event_locked),
                        String.format(res.getString(R.string.text_href),
                                event.getActor().getHtmlUrl(),
                                event.getActor().getLogin()));
                break;
            case UNLOCKED:
                text = String.format(res.getString(R.string.text_event_unlocked),
                        String.format(res.getString(R.string.text_href),
                                event.getActor().getHtmlUrl(),
                                event.getActor().getLogin()));
                break;
            case HEAD_REF_DELETED:
                text = res.getString(R.string.text_event_head_ref_deleted);
                break;
            case HEAD_REF_RESTORED:
                text = res.getString(R.string.text_event_head_ref_restored);
                break;
            case REVIEW_DISMISSED:
                text = String.format(res.getString(R.string.text_event_review_dismissed),
                        String.format(res.getString(R.string.text_href),
                                event.getActor().getHtmlUrl(),
                                event.getActor().getLogin()));
                break;
            case REVIEW_REQUESTED:
                if(event.getReviewRequester().getId() == event.getRequestedReviewer().getId()) {
                    text = String.format(res.getString(R.string.text_event_own_review_request),
                            String.format(res.getString(R.string.text_href),
                                    event.getReviewRequester().getHtmlUrl(),
                                    event.getReviewRequester().getLogin())
                    );
                } else {
                    text = String.format(res.getString(R.string.text_event_review_requested),
                            String.format(res.getString(R.string.text_href),
                                    event.getReviewRequester().getHtmlUrl(),
                                    event.getReviewRequester().getLogin()),
                            String.format(res.getString(R.string.text_href),
                                    event.getRequestedReviewer().getHtmlUrl(),
                                    event.getRequestedReviewer().getLogin()));
                }
                break;
            case REVIEW_REQUEST_REMOVED:
                if(event.getReviewRequester().getId() == event.getRequestedReviewer().getId()) {
                    text = String.format(res.getString(R.string.text_event_removed_own_review_request),
                            String.format(res.getString(R.string.text_href),
                                    event.getReviewRequester().getHtmlUrl(),
                                    event.getReviewRequester().getLogin())
                    );
                } else {
                    text = String.format(res.getString(R.string.text_event_review_request_removed),
                            String.format(res.getString(R.string.text_href),
                                    event.getActor().getHtmlUrl(),
                                    event.getActor().getLogin()),
                            String.format(res.getString(R.string.text_href),
                                    event.getRequestedReviewer().getHtmlUrl(),
                                    event.getRequestedReviewer().getLogin()));
                }
                break;
            default:
                text = "Something that I haven't bothered to implement " + event.getEvent();
        }
        text += " • " + DateUtils.getRelativeTimeSpanString(event.getCreatedAt());
        eventHolder.mText.setHtml(renderer.render(parser.parse(text)));
    }

    private void displayMenu(View view, int pos) {
        mParent.displayCommentMenu(view, (Comment) mData.get(pos));
    }

    class CommentHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.comment_text) HtmlTextView mText;
        @BindView(R.id.comment_menu_button) ImageButton mMenu;

        CommentHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            mText.setShowUnderLines(false);
            mMenu.setOnClickListener((v) -> displayMenu(v, getAdapterPosition()));
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
