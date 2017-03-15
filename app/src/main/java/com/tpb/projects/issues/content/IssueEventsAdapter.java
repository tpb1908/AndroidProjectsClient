package com.tpb.projects.issues.content;

import android.content.res.Resources;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidnetworking.widget.ANImageView;
import com.tpb.projects.BuildConfig;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.DataModel;
import com.tpb.projects.data.models.Event;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.MergedEvent;
import com.tpb.projects.flow.IntentHandler;
import com.tpb.projects.markdown.Markdown;
import com.tpb.projects.util.MultiOnRefreshListener;

import org.sufficientlysecure.htmltext.htmltextview.HtmlTextView;
import org.sufficientlysecure.htmltext.imagegetter.HtmlHttpImageGetter;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 15/03/17.
 */

public class IssueEventsAdapter extends RecyclerView.Adapter<IssueEventsAdapter.EventHolder> implements Loader.GITModelsLoader<Event>{
    private final ArrayList<Pair<DataModel, SpannableString>> mEvents = new ArrayList<>();
    private Issue mIssue;
    private final IssueEventsFragment mParent;

    private int mPage = 1;
    private boolean mIsLoading = false;
    private boolean mMaxPageReached = false;

    private SwipeRefreshLayout mRefresher;
    private Loader mLoader;

    IssueEventsAdapter(IssueEventsFragment parent, SwipeRefreshLayout refresher, MultiOnRefreshListener listener) {
        mParent = parent;
        mLoader = new Loader(parent.getContext());
        mRefresher = refresher;
        mRefresher.setRefreshing(true);
        listener.addListener(() -> {
            mPage = 1;
            mMaxPageReached = false;
            notifyDataSetChanged();
            loadEvents(true);
        });
    }

    void clear() {
        mEvents.clear();
        notifyDataSetChanged();
    }

    void setIssue(Issue issue) {
        mIssue = issue;
        mEvents.clear();
    }

    @Override
    public void loadComplete(Event[] data) {
        mRefresher.setRefreshing(false);
        mIsLoading = false;
        if(data.length > 0) {
            int oldLength = mEvents.size();
            if(mPage == 1) mEvents.clear();
            for(Event e : data) {
                mEvents.add(new Pair<>(e, null));
            }
            notifyItemRangeInserted(oldLength, mEvents.size());
        } else {
            mMaxPageReached = true;
        }
    }

    @Override
    public void loadError(APIHandler.APIError error) {

    }

    void notifyBottomReached() {
        if(!mIsLoading && !mMaxPageReached) {
            mPage++;
            loadEvents(false);
        }
    }

    void loadEvents(boolean resetPage) {
        mIsLoading = true;
        mRefresher.setRefreshing(true);
        if(resetPage) {
            mPage = 1;
            mMaxPageReached = false;
        }
        mLoader.loadEvents(this, mIssue.getRepoPath(), mIssue.getNumber());
    }

    void addEvent(Event event) {
        mEvents.add(new Pair<>(event, null));
        notifyItemInserted(mEvents.size());
    }

    @Override
    public EventHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new EventHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_event, parent, false));
    }

    @Override
    public void onBindViewHolder(EventHolder holder, int position) {
        if(mEvents.get(position).first instanceof Event) {
            bindEvent(holder, (Event) mEvents.get(position).first);
        } else {
            bindMergedEvent(holder, (MergedEvent) mEvents.get(position).first);
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
            case REFERENCED:
                final StringBuilder commits = new StringBuilder();
                for(Event e : me.getEvents()) {
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
                    mParent.getActivity(),
                    eventHolder.mAvatar, me.getEvents().get(0).getActor().getLogin());
            IntentHandler.addGitHubIntentHandler(mParent.getActivity(),
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
            IntentHandler.addGitHubIntentHandler(mParent.getActivity(), eventHolder.mAvatar, event.getActor().getLogin());
            IntentHandler.addGitHubIntentHandler(mParent.getActivity(), eventHolder.mText, eventHolder.mAvatar, event.getActor().getLogin());
        } else {
            eventHolder.mAvatar.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mEvents.size();
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


}
