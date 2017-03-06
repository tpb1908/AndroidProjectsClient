package com.tpb.projects.issues;

import android.content.res.Resources;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
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
import com.tpb.projects.flow.IntentHandler;
import com.tpb.projects.markdown.Markdown;

import org.sufficientlysecure.htmltext.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltext.dialogs.CodeDialog;
import org.sufficientlysecure.htmltext.dialogs.ImageDialog;
import org.sufficientlysecure.htmltext.htmltextview.HtmlTextView;

import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 07/01/17.
 */

class IssueContentAdapter extends RecyclerView.Adapter {
    private static final String TAG = IssueContentAdapter.class.getSimpleName();

    private final ArrayList<Pair<DataModel, SpannableString>> mData = new ArrayList<>();
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
            //If we have two of the same event, happening at the same time
            if(events[i].getCreatedAt() == last.getCreatedAt() && events[i].getEvent() == last.getEvent()) {
                toMerge.add(events[i - 1]); //Add the previous event
                int j = i;
                //Loop until we find an event which shouldn't be merged
                while(j < events.length && events[j].getCreatedAt() == last.getCreatedAt() && events[j].getEvent() == last.getEvent()) {
                    toMerge.add(events[j++]);
                }
                i = j - 1; //Jump to the end of the merged positions
                merged.add(new MergedEvent(toMerge));
                toMerge = new ArrayList<>(); //Reset the list of merged events
            } else {
                merged.add(events[i]);
            }
            last = events[i]; //Set the last event
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
            bindComment((CommentHolder) holder);
        } else {
            if(mData.get(position).first instanceof Event) {
                bindEvent((EventHolder) holder, (Event) mData.get(position).first);
            } else {
                bindMergedEvent((EventHolder) holder, (MergedEvent) mData.get(position).first);
            }
        }
    }

    private void bindComment(CommentHolder commentHolder) {
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
            builder.append(Markdown.formatMD(comment.getBody(), mIssue.getRepoPath()));
            commentHolder.mText.setHtml(
                    Markdown.parseMD(builder.toString()),
                    new HtmlHttpImageGetter(commentHolder.mText, commentHolder.mText),
                    text -> mData.set(pos, new Pair<>(comment, text)));
        } else {
            commentHolder.mAvatar.setImageUrl(((Comment) mData.get(pos).first).getUser().getAvatarUrl());
            commentHolder.mText.setText(mData.get(pos).second);
        }
        IntentHandler.addGitHubIntentHandler(mParent, commentHolder.mText);
        IntentHandler.addGitHubIntentHandler(mParent, commentHolder.mAvatar, ((Comment) mData.get(pos).first).getUser().getLogin());
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
            case REFERENCED:
                final StringBuilder commits = new StringBuilder();
                for(Event e : me.getEvents()) {
                    Log.i(TAG, "bindMergedEvent: \n\n\nCommit id " + e.getCommitId());
                    commits.append("<br>");
                    commits.append(String.format(res.getString(R.string.text_href),
                            "https://github.com/" + mIssue.getRepoPath() + "/commit/" + e.getCommitId(),
                            String.format(res.getString(R.string.text_commit), e.getShortCommitId())));
                }
                commits.append("<br>");
                text = String.format(res.getString(R.string.text_event_referenced_multiple), commits.toString());
                break;
            case MENTIONED:
                final StringBuilder mentioned = new StringBuilder();
                for(Event e : me.getEvents()) {
                    mentioned.append("<br>");
                    mentioned.append(String.format(res.getString(R.string.text_href),
                            e.getActor().getHtmlUrl(),
                            e.getActor().getLogin()));
                }
                text = String.format(res.getString(R.string.text_event_mentioned_multiple),
                      mentioned.toString());
                break;
            case RENAMED:
                final StringBuilder named = new StringBuilder();
                for(Event e : me.getEvents()) {
                    named.append("<br>");
                    named.append(
                            String.format(
                                    res.getString(R.string.text_event_rename_multiple),
                                    e.getRenameFrom(),
                                    e.getRenameTo())
                    );
                }
                text = String.format(res.getString(R.string.text_event_renamed_multiple), named.toString());
                break;
            case MOVED_COLUMNS_IN_PROJECT:
                text = res.getString(R.string.text_event_moved_columns_in_project_multiple);
                break;
            default:
                Log.e(TAG, "bindMergedEvent: Should be binding merging event " + me.toString(), new Exception());
                bindEvent(eventHolder, me.getEvents().get(0));
                return;
        }
        text += " • " + DateUtils.getRelativeTimeSpanString(me.getCreatedAt());
        eventHolder.mText.setHtml(
                Markdown.parseMD(text),
                new HtmlHttpImageGetter(eventHolder.mText, eventHolder.mText),
                null
        );
        if(me.getEvents().get(0).getActor() != null) {
            eventHolder.mAvatar.setVisibility(View.VISIBLE);
            eventHolder.mAvatar.setImageUrl(me.getEvents().get(0).getActor().getAvatarUrl());
            IntentHandler.addGitHubIntentHandler(
                    mParent,
                    eventHolder.mAvatar, me.getEvents().get(0).getActor().getLogin());
            IntentHandler.addGitHubIntentHandler(mParent,
                    eventHolder.mText,
                    eventHolder.mAvatar,
                    me.getEvents().get(0).getActor().getLogin());
        } else {
            eventHolder.mAvatar.setVisibility(View.GONE);
        }
    }

    private void bindEvent(EventHolder eventHolder, Event event) {
        String text;
        final Resources res = eventHolder.itemView.getResources();
        switch(event.getEvent()) {
            case CLOSED:
                if(event.getShortCommitId()!= null) {
                    text = String.format(res.getString(R.string.text_event_closed_in),
                            String.format(res.getString(R.string.text_href),
                                    event.getActor().getHtmlUrl(),
                                    event.getActor().getLogin()),
                            String.format(res.getString(R.string.text_href),
                                    "https://github.com/" + mIssue.getRepoPath() + "/commit/" + event.getCommitId(),
                                    String.format(res.getString(R.string.text_commit), event.getShortCommitId())));
                } else {
                    text = String.format(res.getString(R.string.text_event_closed),
                            String.format(res.getString(R.string.text_href),
                                    event.getActor().getHtmlUrl(),
                                    event.getActor().getLogin()));
                }
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
                                String.format(res.getString(R.string.text_commit), event.getShortCommitId()))
                );
                break;
            case REFERENCED:
                text = String.format(res.getString(R.string.text_event_referenced),
                        String.format(res.getString(R.string.text_href),
                                "https://github.com/" + mIssue.getRepoPath() + "/commit/" + event.getCommitId(),
                                String.format(res.getString(R.string.text_commit), event.getShortCommitId())));
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
            case MOVED_COLUMNS_IN_PROJECT:
                text = res.getString(R.string.text_event_moved_columns_in_project);
                break;
            default:
                text = "An event type hasn't been implemented " + event.getEvent();
                text += "\nTell me here " + BuildConfig.BUG_EMAIL;
        }
        text += " • " + DateUtils.getRelativeTimeSpanString(event.getCreatedAt());
        eventHolder.mText.setHtml(
                Markdown.parseMD(text),
                new HtmlHttpImageGetter(eventHolder.mText, eventHolder.mText),
                null);
        if(event.getActor() != null) {
            eventHolder.mAvatar.setVisibility(View.VISIBLE);
            eventHolder.mAvatar.setImageUrl(event.getActor().getAvatarUrl());
            IntentHandler.addGitHubIntentHandler(mParent, eventHolder.mAvatar, event.getActor().getLogin());
            IntentHandler.addGitHubIntentHandler(mParent, eventHolder.mText, eventHolder.mAvatar, event.getActor().getLogin());
        } else {
            eventHolder.mAvatar.setVisibility(View.GONE);
        }
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
            mText.setImageHandler(new ImageDialog(mText.getContext()));
            mText.setCodeClickHandler(new CodeDialog(mText.getContext()));
            mMenu.setOnClickListener((v) -> displayMenu(v, getAdapterPosition()));
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
        }

    }

    public int getItemCount() {
        return mData.size();
    }
}
