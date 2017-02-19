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
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.androidnetworking.widget.ANImageView;
import com.tpb.projects.BuildConfig;
import com.tpb.projects.R;
import com.tpb.projects.data.models.Comment;
import com.tpb.projects.data.models.DataModel;
import com.tpb.projects.data.models.Event;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.MergedEvent;
import com.tpb.projects.data.models.User;
import com.tpb.projects.util.Data;

import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 07/01/17.
 */

class IssueContentAdapter extends RecyclerView.Adapter {
    private static final String TAG = IssueContentAdapter.class.getSimpleName();

    private final ArrayList<Pair<DataModel, String>> mData = new ArrayList<>();
    private Issue mIssue;
    private final IssueActivity mParent;
    private boolean mHasOtherContentLoaded = false;

    IssueContentAdapter(IssueActivity parent) {
        mParent = parent;
    }

    void clear() {
        mData.clear();
        mHasOtherContentLoaded = false;
        notifyDataSetChanged();
    }

    void setIssue(Issue issue) {
        mIssue = issue;
        //TODO Add pull request model and link to pull requests
    }

    void loadComments(Comment[] comments) {
        if(mHasOtherContentLoaded) {
            mParent.mRefresher.setRefreshing(false);
        } else {
            mHasOtherContentLoaded = true;
        }
        for(Comment c : comments) {
            mData.add(new Pair<>(c, null));
        }
        Collections.sort(mData, (d1, d2) -> d1.first.getCreatedAt() > d2.first.getCreatedAt() ? 1 : -1);
        notifyDataSetChanged();

    }

    void loadEvents(Event[] events) {
        if(mHasOtherContentLoaded) {
            mParent.mRefresher.setRefreshing(false);
        } else {
            mHasOtherContentLoaded = true;
        }
        for(DataModel e : mergeEvents(events)) {
            mData.add(new Pair<>(e, null));
        }
        Collections.sort(mData, (d1, d2) -> d1.first.getCreatedAt() > d2.first.getCreatedAt() ? 1 : -1);
        notifyDataSetChanged();

    }

    void addComment(Comment comment) {
        mData.add(new Pair<>(comment, null));
        notifyItemInserted(mData.size());
    }

    void removeComment(Comment comment) {
        int index = -1;
        for(int i = 0; i < mData.size(); i++) {
            if(mData.get(i).first instanceof Comment && ((Comment) mData.get(i).first).getId() == comment.getId()) {
                index = i;
                break;
            }
        }
        if(index != -1) {
            mData.remove(index);
            notifyItemRemoved(index);
        }
    }

    void updateComment(Comment comment) {
        int index = -1;
        for(int i = 0; i < mData.size(); i++) {
            if(mData.get(i).first instanceof Comment && ((Comment) mData.get(i).first).getId() == comment.getId()) {
                index = i;
                break;
            }
        }
        Log.i(TAG, "updateComment: Index: " + index);
        if(index != -1) {
            mData.set(index, new Pair<>(comment, null));
            notifyItemChanged(index);
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
                // Log.i(TAG, "mergeEvents: Merging events from " + i + " to " + j);
                i = j - 1;
                merged.add(new MergedEvent(toMerge));
                //   Log.i(TAG, "mergeEvents: Merging " + toMerge.toString());
                toMerge = new ArrayList<>();
            } else {
                merged.add(events[i]);
            }
            last = events[i];
        }
        return merged;

    }

    @Override
    public int getItemViewType(int position) {
        return mData.get(position).first instanceof Comment ? 1 : 0;
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
            bindComment((CommentHolder) holder, position);
        } else {
            if(mData.get(position).first instanceof Event) {
                bindEvent((EventHolder) holder, (Event) mData.get(position).first);
            } else {
                bindMergedEvent((EventHolder) holder, (MergedEvent) mData.get(position).first);
            }
        }
    }

    private void bindComment(CommentHolder commentHolder, int position) {
        final int pos = commentHolder.getAdapterPosition();
        if(mData.get(pos).second == null) {
            final Comment comment = (Comment) mData.get(pos).first;
            commentHolder.mAvatar.setImageUrl(comment.getUser().getAvatarUrl());
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
            final String parsed = Data.parseMD(builder.toString());
            mData.set(pos, new Pair<>(comment, parsed));
            commentHolder.mText.setHtml(parsed, new HtmlHttpImageGetter(commentHolder.mText));
        } else {
            commentHolder.mAvatar.setImageUrl(((Comment) mData.get(pos).first).getUser().getAvatarUrl());
            commentHolder.mText.setHtml(mData.get(pos).second, new HtmlHttpImageGetter(commentHolder.mText));
        }
    }

    private void bindMergedEvent(EventHolder eventHolder, MergedEvent me) {
        String text;
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
                            e.getRequestedReviewer().getHtmlUrl(),
                            e.getRequestedReviewer().getLogin()));
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
            case LABELED:
                final StringBuilder labels = new StringBuilder();
                for(Event e : me.getEvents()) {
                    labels.append(String.format(res.getString(R.string.text_label),
                            String.format("#%06X", (0xFFFFFF & e.getLabelColor())),
                            e.getLabelName()));
                    labels.append(", ");
                }
                labels.setLength(labels.length() - 2);
                text = String.format(
                        res.getString(R.string.text_event_labels_added),
                        String.format(res.getString(R.string.text_href),
                                me.getEvents().get(0).getActor().getHtmlUrl(),
                                me.getEvents().get(0).getActor().getLogin()),
                        labels.toString());
                break;
            case UNLABELED:
                final StringBuilder unlabels = new StringBuilder();
                for(Event e : me.getEvents()) {
                    unlabels.append(
                            String.format(res.getString(R.string.text_label),
                                    String.format("#%06X", (0xFFFFFF & e.getLabelColor())),
                                    e.getLabelName()));
                    unlabels.append(", ");
                }
                unlabels.setLength(unlabels.length() - 2);
                text = String.format(res.getString(R.string.text_event_labels_removed),
                        String.format(res.getString(R.string.text_href),
                                me.getEvents().get(0).getActor().getHtmlUrl(),
                                me.getEvents().get(0).getActor().getLogin()),
                        unlabels.toString());
                break;
            case CLOSED:
                //Duplicate close events seem to happen
                bindEvent(eventHolder, me.getEvents().get(0));
                return;
            default:
                Log.e(TAG, "bindMergedEvent: Should be binding merging event " + me.toString(), new Exception());
                bindEvent(eventHolder, me.getEvents().get(0));
                return;
        }
        text += " • " + DateUtils.getRelativeTimeSpanString(me.getCreatedAt());
        eventHolder.mText.setHtml(Data.parseMD(text), new HtmlHttpImageGetter(eventHolder.mText));
        if(me.getEvents().get(0).getActor() != null) {
            eventHolder.mAvatar.setVisibility(View.VISIBLE);
            eventHolder.mAvatar.setImageUrl(me.getEvents().get(0).getActor().getAvatarUrl());
        } else {
            eventHolder.mAvatar.setVisibility(View.GONE);
        }
    }

    private void bindEvent(EventHolder eventHolder, Event event) {
        String text;
        final Resources res = eventHolder.itemView.getResources();
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
                if(event.getAssignee() != null && event.getActor().equals(event.getAssignee())) {
                    text = String.format(res.getString(R.string.text_event_assigned_themselves),
                            String.format(res.getString(R.string.text_href),
                                    event.getActor().getHtmlUrl(),
                                    event.getActor().getLogin()));
                } else {
                    text = String.format(res.getString(R.string.text_event_assigned),
                            String.format(res.getString(R.string.text_href),
                                    event.getActor().getHtmlUrl(),
                                    event.getActor().getLogin()));
                }
                break;
            case UNASSIGNED:
                if(event.getAssignee() != null && event.getActor().equals(event.getAssignee())) {
                    text = String.format(res.getString(R.string.text_event_unassigned_themselves),
                            String.format(res.getString(R.string.text_href),
                                    event.getActor().getHtmlUrl(),
                                    event.getActor().getLogin()));
                } else {
                    text = String.format(res.getString(R.string.text_event_unassigned),
                            String.format(res.getString(R.string.text_href),
                                    event.getActor().getHtmlUrl(),
                                    event.getActor().getLogin()));
                }
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
            case REMOVED_FROM_PROJECT:
                text = String.format(res.getString(R.string.text_event_removed_from_project),
                        String.format(res.getString(R.string.text_href),
                                event.getActor().getHtmlUrl(),
                                event.getActor().getLogin()));
                break;
            case ADDED_TO_PROJECT:
                text = String.format(res.getString(R.string.text_event_added_to_project),
                        String.format(res.getString(R.string.text_href),
                                event.getActor().getHtmlUrl(),
                                event.getActor().getLogin()));
                break;
            default:
                text = "An event type hasn't been implemented " + event.getEvent();
                text += "\nTell me here " + BuildConfig.BUG_EMAIL;
        }
        text += " • " + DateUtils.getRelativeTimeSpanString(event.getCreatedAt());
        eventHolder.mText.setHtml(Data.parseMD(text), new HtmlHttpImageGetter(eventHolder.mText));
        if(event.getActor() != null) {
            eventHolder.mAvatar.setVisibility(View.VISIBLE);
            eventHolder.mAvatar.setImageUrl(event.getActor().getAvatarUrl());
        } else {
            eventHolder.mAvatar.setVisibility(View.GONE);
        }
    }

    private void onAvatarClick(ANImageView view, int pos) {
        final User user;
        if(getItemViewType(pos) == 1) {
            user = ((Comment) mData.get(pos).first).getUser();
        } else {
            if(mData.get(pos).first instanceof Event) {
                user = ((Event) mData.get(pos).first).getActor();
            } else {
                user = ((MergedEvent) mData.get(pos).first).getEvents().get(0).getActor();
            }
        }
        mParent.openUser(view, user);
    }

    private void displayMenu(View view, int pos) {
        mParent.displayCommentMenu(view, (Comment) mData.get(pos).first);
    }

    private void displayInFullScreen(int pos) {
        mParent.showCardInFullscreen(((Comment) mData.get(pos).first).getBody());
    }

    class CommentHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.event_comment_avatar) ANImageView mAvatar;
        @BindView(R.id.comment_text) HtmlTextView mText;
        @BindView(R.id.comment_menu_button) ImageButton mMenu;

        CommentHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            mText.setShowUnderLines(false);
            mText.setImageHandler(new HtmlTextView.ImageDialog(mText.getContext()));
            mText.setCodeClickHandler(new HtmlTextView.CodeDialog(mText.getContext()));
            mMenu.setOnClickListener((v) -> displayMenu(v, getAdapterPosition()));
            mAvatar.setOnClickListener((v) -> onAvatarClick(mAvatar, getAdapterPosition()));
            view.setOnClickListener((v) -> displayInFullScreen(getAdapterPosition()));
        }

    }

    class EventHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.event_text) HtmlTextView mText;
        @BindView(R.id.event_user_avatar) ANImageView mAvatar;

        EventHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            mText.setShowUnderLines(false);
            mAvatar.setOnClickListener((v) -> onAvatarClick(mAvatar, getAdapterPosition()));
        }

    }

    public int getItemCount() {
        return mData.size();
    }
}
