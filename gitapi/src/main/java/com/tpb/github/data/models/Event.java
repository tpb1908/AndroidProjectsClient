package com.tpb.github.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.tpb.github.data.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Arrays;

/**
 * Created by theo on 07/01/17.
 */

public class Event extends DataModel implements Parcelable {
    private static final String TAG = Event.class.getSimpleName();

    private int id;

    private static final String ACTION = "action";
    private GitAction action;

    private static final String TYPE = "type";
    private GitEvent event;

    private static final String COMMENT = "comment";
    private Comment comment;

    private static final String REF_TYPE = "ref_type";
    private String ref_type;

    private static final String REF = "ref";
    private String ref;

    private static final String DESCRIPTION = "description";
    private String description;

    private static final String DEPLOYMENT = "deployment";

    private static final String REPOSITORY = "repository";
    private Repository repository;

    private static final String DEPLOYMENT_STATUS = "deployment_status";

    private static final String STATUS = "status";
    private String status;

    private static final String TARGET = "target";

    private static final String FORKEE = "forkee";

    private static final String GIST = "gist";
    private Gist gist;

    private static final String ISSUE = "issue";
    private Issue issue;

    private static final String LABELS = "labels";
    private Label[] labels;

    private static final String LABEL = "label";
    private Label label;

    private static final String MEMBER = "member";

    private static final String MILESTONE = "milestone";
    private Milestone milestone;

    private static final String BLOCKED_USER = "blocked_user";

    private static final String PROJECT_CARD = "project_card";
    private Card card;

    private static final String PROJECT_COLUMN = "project_column";
    private Column column;

    private static final String PROJECT = "project";
    private Project project;

    private User actor;
    private User effected;
    private static final String SENDER = "sender";
    private User sender;


    @Override
    public long getCreatedAt() {
        return createdAt;
    }

    public Event(JSONObject obj) {
        try {
            if(obj.has(ID)) id = obj.getInt(ID);
            if(obj.has(ACTION)) action = GitAction.fromString(obj.getString(ACTION));
            if(obj.has(TYPE)) event = GitEvent.fromString(obj.getString(TYPE));
            if(obj.has(SENDER)) sender = User.parse(obj.getJSONObject(SENDER));
            if(obj.has(COMMENT)) comment = Comment.parse(obj.getJSONObject(COMMENT));
            if(obj.has(REF_TYPE)) ref_type = obj.getString(REF_TYPE);
            if(obj.has(REF)) ref = obj.getString(REF);
            if(obj.has(DESCRIPTION)) description = obj.getString(DESCRIPTION);
            if(obj.has(REPOSITORY)) repository = Repository.parse(obj.getJSONObject(REPOSITORY));
            if(obj.has(STATUS)) status = obj.getString(STATUS);
            if(obj.has(TARGET)) effected = User.parse(obj.getJSONObject(TARGET));
            if(obj.has(FORKEE)) repository = Repository.parse(obj.getJSONObject(FORKEE));
            if(obj.has(GIST)) gist = Gist.parse(obj.getJSONObject(GIST));
            if(obj.has(ISSUE)) issue = Issue.parse(obj.getJSONObject(ISSUE));
            if(obj.has(LABELS)) {
                final JSONArray labeljson = obj.getJSONArray(LABELS);
                labels = new Label[labeljson.length()];
                for(int i = 0; i < labeljson.length(); i++) {
                    labels[i] = Label.parse(labeljson.getJSONObject(i));
                }
            }
            if(obj.has(LABEL)) label = Label.parse(obj.getJSONObject(LABEL));
            if(obj.has(MEMBER)) effected = User.parse(obj.getJSONObject(MEMBER));
            if(obj.has(MILESTONE)) milestone = Milestone.parse(obj.getJSONObject(MILESTONE));
            if(obj.has(BLOCKED_USER)) effected = User.parse(obj.getJSONObject(BLOCKED_USER));
            if(obj.has(PROJECT_CARD)) card = Card.parse(obj.getJSONObject(PROJECT_CARD));
            if(obj.has(PROJECT_COLUMN)) column = Column.parse(obj.getJSONObject(PROJECT_COLUMN));
            if(obj.has(PROJECT)) project = Project.parse(obj.getJSONObject(PROJECT));
            try {
                createdAt = Util.toCalendar(obj.getString(CREATED_AT)).getTimeInMillis();
            } catch(ParseException pe) {
                Log.e(TAG, "parse: ", pe);
            }
        } catch(JSONException jse) {

        }
    }

    public int getId() {
        return id;
    }

    public GitAction getAction() {
        return action;
    }

    public GitEvent getEvent() {
        return event;
    }

    public Comment getComment() {
        return comment;
    }

    public String getRef_type() {
        return ref_type;
    }

    public String getRef() {
        return ref;
    }

    public String getDescription() {
        return description;
    }

    public Repository getRepository() {
        return repository;
    }

    public String getStatus() {
        return status;
    }

    public Gist getGist() {
        return gist;
    }

    public Issue getIssue() {
        return issue;
    }

    public Label[] getLabels() {
        return labels;
    }

    public Label getLabel() {
        return label;
    }

    public Milestone getMilestone() {
        return milestone;
    }

    public Column getColumn() {
        return column;
    }

    public User getActor() {
        return actor;
    }

    public User getEffected() {
        return effected;
    }

    public User getSender() {
        return sender;
    }

    //        CLOSED,
//        REOPENED,
//        SUBSCRIBED,
//        MERGED,
//        REFERENCED,
//        MENTIONED,
//        ASSIGNED,
//        UNASSIGNED,
//        LABELED,
//        UNLABELED,
//        MILESTONED,
//        DEMILESTONED,
//        RENAMED,
//        LOCKED,
//        UNLOCKED,
//        HEAD_REF_DELETED,
//        HEAD_REF_RESTORED,
//        REVIEW_DISMISSED,
//        PULL_REVIEW_REQUESTED,
//        PULL_REVIEW_REQUEST_REMOVED,
//        ADDED_TO_PROJECT,
//        REMOVED_FROM_PROJECT,
//        MOVED_COLUMNS_IN_PROJECT,
//        PROJECT_CARD_MOVED,
//        PROJECT_CARD_CREATED,
//        PROJECT_CARD_DELETED,
//        COMMIT_COMMENT, //Comment
//        CREATE, //Repo branch or tag created- the object, and description if repo
//        DELETE, //Deleted branch or tag- the object
//        DEPLOYMENT, //Not visible in timelines
//        DEPLOYMENT_STATUS, //Not visible in timelines
//        DOWNLOAD, //Download- No longer created but may exist
//        FOLLOW, //No longer created but may exist- user
//        FORK, //When a user forks a repo- the fork repo
//        FORK_APPLY, //No longer created
//        GIST, //No longer created. Has a gist
//        GOLLUM, //When a wiki is created or updated- repo and array of pages
//        ISSUE_COMMENT, //has action {created, edited, deleted}, issue, comment, and changes
//        ISSUES, // {"assigned", "unassigned", "labeled", "unlabeled", "opened", "edited", "milestoned", "demilestoned", "closed", "reopened"} has issue
//        LABEL, // has label, action and changes
//        MEMBER, //Collaborator changed- member,
//        MEMBERSHIP, //Added or removed from a team- action, member, team
//        MILESTONE, //action, mielstone, changes
//        ORGANIZATION, //action, organization
//        ORG_BLOCK, //organization blocks or unblocks user
//        PAGE_BUILD, // build- the page
//        PROJECT,
//        PUBLIC, //When a private repo is made open- contains repo
//        PULL_REQUEST,
//        PUSH,
//        RELEASE,
//        REPOSITORY,
//        STATUS,
//        TEAM,
//        TEAM_ADD,
//        WATCH,
//        UNKNOWN;


    public enum GitAction {
        CREATE,
        UPDATE,
        CREATED,
        EDITED,
        DELETED,
        ASSIGNED,
        UNASSIGNED,
        LABELED,
        UNLABELED,
        OPENED,
        MILESTONED,
        DEMILESTONED,
        CLOSED,
        REOPENED,
        ADDED,
        REMOVED,
        MEMBER_ADDED,
        MEMBER_REMOVED,
        MEMBER_INVITED,
        BLOCKED,
        UNBLOCKED,
        CONVERTED,
        MOVED,
        REVIEW_REQUESTED,
        REVIEW_REQUEST_REMOVED,
        SUBMITTED,
        DISMISSED,
        PUBLISHED,
        PUBLICIZED,
        PRIVATIZED,
        ADDED_TO_REPOSITORY,
        REMOVED_FROM_REPOSITORY,
        STARTED,
        UNKNOWN;

        @Nullable String val;

        static GitAction fromString(@NonNull String val) {
            try {
                return GitAction.valueOf(val.toUpperCase());
            } catch(Exception e) {
                final GitAction action = UNKNOWN;
                action.val = val;
                return action;
            }
        }

        @Override
        public String toString() {
            return val == null ? super.toString() : val;
        }

    }

    public enum GitEvent {
        COMMIT_COMMENT,
        CREATE,
        DELETE,
        DEPLOYMENT,
        DEPLOYMENT_STATUS,
        DOWNLOAD,
        FOLLOW,
        FORK,
        FORK_APPLY,
        GIST,
        GOLLUM,
        ISSUE_COMMENT,
        ISSUES,
        LABEL,
        MEMBER,
        MEMBERSHIP,
        MILESTONE,
        ORGANIZATION,
        ORG_BLOCK,
        PAGE_BUILD,
        PROJECT_CARD,
        PROJECT_COLUMN,
        PROJECT,
        PUBLIC,
        PULL_REQUEST,
        PULL_REQUEST_REVIEW,
        PULL_REQUEST_REVIEW_COMMENT,
        PUSH,
        RELEASE,
        REPOSITORY,
        STATUS,
        TEAM,
        TEAM_ADD,
        WATCH,
        UNKNOWN;

        @Nullable String val;

        public static GitEvent fromString(@NonNull String val) {
            try {
                return  GitEvent.valueOf(val);
            } catch(Exception e) {
                final GitEvent event = UNKNOWN;
                event.val = val;
                return event;
            }
        }

        @Override
        public String toString() {
            return val == null ? super.toString() : val;
        }
    }

    /*
    Actions-

     */

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Event && ((Event) obj).id == id;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", action=" + action +
                ", event=" + event +
                ", comment=" + comment +
                ", ref_type='" + ref_type + '\'' +
                ", ref='" + ref + '\'' +
                ", description='" + description + '\'' +
                ", repository=" + repository +
                ", status='" + status + '\'' +
                ", gist=" + gist +
                ", issue=" + issue +
                ", labels=" + Arrays.toString(labels) +
                ", label=" + label +
                ", milestone=" + milestone +
                ", card=" + card +
                ", column=" + column +
                ", project=" + project +
                ", actor=" + actor +
                ", effected=" + effected +
                ", sender=" + sender +
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeInt(this.action == null ? -1 : this.action.ordinal());
        dest.writeInt(this.event == null ? -1 : this.event.ordinal());
        dest.writeParcelable(this.comment, flags);
        dest.writeString(this.ref_type);
        dest.writeString(this.ref);
        dest.writeString(this.description);
        dest.writeParcelable(this.repository, flags);
        dest.writeString(this.status);
        dest.writeParcelable(this.gist, flags);
        dest.writeParcelable(this.issue, flags);
        dest.writeTypedArray(this.labels, flags);
        dest.writeParcelable(this.label, flags);
        dest.writeParcelable(this.milestone, flags);
        dest.writeParcelable(this.card, flags);
        dest.writeParcelable(this.column, flags);
        dest.writeParcelable(this.project, flags);
        dest.writeParcelable(this.actor, flags);
        dest.writeParcelable(this.effected, flags);
        dest.writeParcelable(this.sender, flags);
        dest.writeLong(this.createdAt);
    }

    protected Event(Parcel in) {
        this.id = in.readInt();
        int tmpAction = in.readInt();
        this.action = tmpAction == -1 ? null : GitAction.values()[tmpAction];
        int tmpEvent = in.readInt();
        this.event = tmpEvent == -1 ? null : GitEvent.values()[tmpEvent];
        this.comment = in.readParcelable(Comment.class.getClassLoader());
        this.ref_type = in.readString();
        this.ref = in.readString();
        this.description = in.readString();
        this.repository = in.readParcelable(Repository.class.getClassLoader());
        this.status = in.readString();
        this.gist = in.readParcelable(Gist.class.getClassLoader());
        this.issue = in.readParcelable(Issue.class.getClassLoader());
        this.labels = in.createTypedArray(Label.CREATOR);
        this.label = in.readParcelable(Label.class.getClassLoader());
        this.milestone = in.readParcelable(Milestone.class.getClassLoader());
        this.card = in.readParcelable(Card.class.getClassLoader());
        this.column = in.readParcelable(Column.class.getClassLoader());
        this.project = in.readParcelable(Project.class.getClassLoader());
        this.actor = in.readParcelable(User.class.getClassLoader());
        this.effected = in.readParcelable(User.class.getClassLoader());
        this.sender = in.readParcelable(User.class.getClassLoader());
        this.createdAt = in.readLong();
    }

    public static final Creator<Event> CREATOR = new Creator<Event>() {
        @Override
        public Event createFromParcel(Parcel source) {
            return new Event(source);
        }

        @Override
        public Event[] newArray(int size) {
            return new Event[size];
        }
    };
}
