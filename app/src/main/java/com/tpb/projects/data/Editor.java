package com.tpb.projects.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.OkHttpResponseListener;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Column;
import com.tpb.projects.data.models.Comment;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Milestone;
import com.tpb.projects.data.models.Project;
import com.tpb.projects.data.models.State;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Response;

/**
 * Created by theo on 18/12/16.
 */

public class Editor extends APIHandler {
    private static final String TAG = Editor.class.getSimpleName();

    private static final String NAME = "name";
    private static final String BODY = "body";
    private static final String NOTE = "note";
    private static final String TITLE = "title";
    private static final String ASSIGNEES = "assignees";
    private static final String LABELS = "labels";

    private static final String STATE = "state";
    private static final String STATE_CLOSED = "closed";
    private static final String STATE_OPEN = "open";

    private static final String CONTENT_TYPE = "content_type";
    private static final String CONTENT_TYPE_ISSUE = "Issue";
    private static final String CONTENT_ID = "content_id";

    private static final String POSITION = "position";
    private static final String POSITION_FIRST = "first";
    private static final String POSITION_AFTER = "after:";
    private static final String POSITION_TOP = "top";
    private static final String COLUMN_ID = "column_id";


    public Editor(Context context) {
        super(context);
    }

    public void createProject(final GITModelCreationListener<Project> listener, Project project, String repoFullName) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(NAME, project.getName());
            obj.put(BODY, project.getBody());
        } catch(JSONException jse) {
            Log.e(TAG, "createProject: ", jse);
        }
        AndroidNetworking.post(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_PROJECTS)
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: " + response.toString());
                        if(listener != null) listener.created(Project.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: " + anError.getErrorBody());
                        if(listener != null) listener.creationError(parseError(anError));
                    }
                });
    }

    public void updateProject(final GITModelUpdateListener<Project> listener, Project project) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(NAME, project.getName());
            obj.put(BODY, project.getBody());
        } catch(JSONException jse) {
            Log.e(TAG, "createProject: ", jse);
        }
        AndroidNetworking.patch(GIT_BASE + SEGMENT_PROJECTS + "/" + project.getId())
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: " + response.toString());
                        if(listener != null) listener.updated(Project.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: " + anError.getErrorBody());
                        if(listener != null) listener.updateError(parseError(anError));
                    }
                });
    }

    public void deleteProject(final GITModelDeletionListener<Project> listener, Project project) {
        /*
        It seems that on a successful deletion, this returns an error with null body
         */
        AndroidNetworking.delete(GIT_BASE + SEGMENT_PROJECTS + "/" + project.getId())
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: " + response.toString());
                        listener.deletionError(APIError.UNKNOWN);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(anError.getErrorCode() == 0 && anError.getErrorBody() == null && listener != null) {
                            listener.deleted(project);
                        } else if(listener != null) {
                            listener.deletionError(parseError(anError));
                        }
                    }
                });
    }

    public void updateColumnName(GITModelUpdateListener<Column> listener, int columnId, String newName) {
        final JSONObject obj = new JSONObject();
        // Again, if we use .addBodyParameter("name", newName), GitHub throws a parsing error

        try {
            obj.put(NAME, newName);
        } catch(JSONException jse) {
            Log.e(TAG, "updateColumnName: ", jse);
        }
        AndroidNetworking.patch(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + columnId)
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Column update: " + response.toString());
                        if(listener != null) listener.updated(Column.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Column update: " + anError.getErrorBody());
                        if(listener != null) listener.updateError(parseError(anError));
                    }
                });
    }

    public void addColumn(GITModelCreationListener<Column> listener, int projectId, String name) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(NAME, name);
        } catch(JSONException jse) {
            Log.e(TAG, "addColumn: ", jse);
        }
        AndroidNetworking.post(GIT_BASE + SEGMENT_PROJECTS + "/" + projectId + SEGMENT_COLUMNS)
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Column created " + response.toString());
                        if(listener != null) listener.created(Column.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: " + anError.getErrorBody());
                        if(listener != null) listener.creationError(parseError(anError));
                    }
                });
    }

    public void moveColumn(GITModelUpdateListener<Integer> listener, int columnId, int dropPositionId, int position) {
        final JSONObject obj = new JSONObject();
        try {
            if(position == 0) {
                obj.put(POSITION, POSITION_FIRST);
            } else {
                obj.put(POSITION, POSITION_AFTER + dropPositionId);
            }
        } catch(JSONException jse) {
            Log.e(TAG, "moveColumn: ", jse);
        }
        AndroidNetworking.post(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + columnId + SEGMENT_MOVES)
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(listener != null) listener.updated(columnId);
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Column not moved: " + anError.getErrorBody());
                        if(listener != null) listener.updateError(parseError(anError));
                    }
                });
    }

    public void deleteColumn(GITModelDeletionListener<Integer> listener, int columnId) {
        AndroidNetworking.delete(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + columnId)
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Column delete: " + response.toString());
                        if(listener != null) listener.deletionError(APIError.UNKNOWN);
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Column delete: " + anError.getErrorBody());
                        Log.i(TAG, "onError: Column delete: " + anError.getErrorCode());
                        if(listener != null) {
                            if(anError.getErrorCode() == 0) {
                                listener.deleted(columnId);
                            } else {
                                listener.deletionError(parseError(anError));
                            }
                        }
                    }
                });
    }

    public void createCard(GITModelCreationListener<Pair<Integer, Card>> listener, int columnId, String note) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(NOTE, note);
        } catch(JSONException jse) {
            Log.e(TAG, "createCard: ", jse);
        }
        AndroidNetworking.post(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + columnId + SEGMENT_CARDS)
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Card: " + response.toString());
                        if(listener != null) listener.created(new Pair<>(columnId, Card.parse(response)));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Card: " + anError.getErrorBody());
                        if(listener != null) listener.creationError(parseError(anError));
                    }
                });
    }

    public void createCard(GITModelCreationListener<Pair<Integer, Card>> listener, int columnId, int issueId) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(CONTENT_TYPE, CONTENT_TYPE_ISSUE);
            obj.put(CONTENT_ID, issueId);
        } catch(JSONException jse) {
            Log.e(TAG, "createCard: ", jse);
        }
        AndroidNetworking.post(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + columnId + SEGMENT_CARDS)
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Card: " + response.toString());
                        if(listener != null) listener.created(new Pair<>(columnId, Card.parse(response)));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Card: " + anError.getErrorBody());
                        if(listener != null) listener.creationError(parseError(anError));
                    }
                });
    }

    public void updateCard(GITModelUpdateListener<Card> listener, int cardId, String note) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(NOTE, note);
        } catch(JSONException jse) {
            Log.e(TAG, "updateCard: ", jse);
        }
        AndroidNetworking.patch(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + SEGMENT_CARDS + "/" + cardId)
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Card update: " + response.toString());
                        if(listener != null) listener.updated(Card.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Card update: " + anError.getErrorBody());
                        if(listener != null) listener.updateError(parseError(anError));
                    }
                });
    }

    public void moveCard(GITModelUpdateListener<Integer> listener, int columnId, int cardId, int afterId) {
        final JSONObject obj = new JSONObject();
        try {
            if(afterId == -1) {
                obj.put(POSITION, POSITION_TOP);
            } else {
                obj.put(POSITION, POSITION_AFTER + afterId);
            }
            if(columnId != -1) obj.put(COLUMN_ID, columnId);
        } catch(JSONException jse) {
            Log.e(TAG, "moveCard: ", jse);
        }
        Log.i(TAG, "moveCard: " + obj.toString() + ", card " + cardId);
        AndroidNetworking.post(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + SEGMENT_CARDS + "/" + cardId + SEGMENT_MOVES)
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Card moved " + response.toString());
                        if(listener != null) listener.updated(cardId);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.updateError(parseError(anError));
                    }
                });
    }

    public void deleteCard(GITModelDeletionListener<Card> listener, Card card) {
        AndroidNetworking.delete(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + SEGMENT_CARDS + "/" + card.getId())
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(listener != null) listener.deletionError(APIError.UNKNOWN);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(anError.getErrorCode() == 0 && listener != null) {
                            listener.deleted(card);
                        } else if(listener != null) {
                            listener.deletionError(parseError(anError));
                        }
                    }
                });
    }

    public void createIssue(GITModelCreationListener<Issue> listener, String repoFullName, String title, String body, @Nullable String[] assignees, @Nullable String[] labels) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(TITLE, title);
            obj.put(BODY, body);
            if(assignees != null) obj.put(ASSIGNEES, new JSONArray(assignees));
            if(labels != null) obj.put(LABELS, new JSONArray(labels));
        } catch(JSONException jse) {
            Log.e(TAG, "createIssue: ", jse);
        }
        Log.i(TAG, "createIssue: " + obj.toString());
        AndroidNetworking.post(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_ISSUES)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Issue created ");
                        if(listener != null) listener.created(Issue.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Issue not created " + anError.getErrorBody());
                        if(listener != null) listener.creationError(parseError(anError));
                    }
                });
    }

    public void closeIssue(GITModelUpdateListener<Issue> listener, String fullRepoPath, int issueNumber) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(STATE, STATE_CLOSED);
        } catch(JSONException jse) {
            Log.e(TAG, "closeIssue: ", jse);
        }
        AndroidNetworking.patch(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoPath + SEGMENT_ISSUES + "/" + issueNumber)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Issue closed " + response.toString());
                        if(listener != null) listener.updated(Issue.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Issue close error " + anError.getErrorBody());
                        if(listener != null) listener.updateError(parseError(anError));
                    }
                });
    }

    public void openIssue(GITModelUpdateListener<Issue> listener, String fullRepoPath, int issueNumber) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(STATE, STATE_OPEN);
        } catch(JSONException jse) {
            Log.e(TAG, "openIssue: ", jse);
        }
        AndroidNetworking.patch(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoPath + SEGMENT_ISSUES + "/" + issueNumber)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Issue opened " + response.toString());
                        if(listener != null) listener.updated(Issue.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Issue open error " + anError.getErrorBody());
                        if(listener != null) listener.updateError(parseError(anError));
                    }
                });
    }

    public void updateIssue(GITModelUpdateListener<Issue> listener, String fullRepoPath, Issue issue, @Nullable String[] assignees, @Nullable String[] labels) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(TITLE, issue.getTitle());
            obj.put(BODY, issue.getBody());
            if(assignees != null) obj.put(ASSIGNEES, new JSONArray(assignees));
            if(labels != null) obj.put(LABELS, new JSONArray(labels));
        } catch(JSONException jse) {
            Log.e(TAG, "createIssue: ", jse);
        }
        AndroidNetworking.patch(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoPath + SEGMENT_ISSUES + "/" + issue.getNumber())
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Issue edited " + response.toString());
                        if(listener != null) listener.updated(Issue.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Issue not edited");
                        if(listener != null) listener.updateError(parseError(anError));
                    }
                });
    }

    public void createComment(GITModelCreationListener<Comment> listener, String fullRepoPath, int issueNumber, String body) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(BODY, body);
        } catch(JSONException jse) {
            Log.e(TAG, "createComment: ", jse);
        }
        AndroidNetworking.post(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoPath + SEGMENT_ISSUES + "/" + issueNumber + SEGMENT_COMMENTS)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Comment created: " + response.toString());
                        if(listener != null) listener.created(Comment.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Comment not created: " + anError.getErrorBody());
                        if(listener != null) listener.creationError(parseError(anError));
                    }
                });
    }

    public void updateComment(GITModelUpdateListener<Comment> listener, String fullRepoPath, int commentId, String body) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(BODY, body);
        } catch(JSONException jse) {
            Log.e(TAG, "createComment: ", jse);
        }
        AndroidNetworking.patch(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoPath + SEGMENT_ISSUES + SEGMENT_COMMENTS + "/" + commentId)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(listener != null) listener.updated(Comment.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Comment not edited " + anError.getErrorBody());
                        if(listener != null) listener.updateError(parseError(anError));
                    }
                });
    }

    public void deleteComment(GITModelDeletionListener<Integer> listener, String fullRepoPath, int commentId) {
        AndroidNetworking.delete(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoPath + SEGMENT_ISSUES + SEGMENT_COMMENTS + "/" + commentId)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(listener != null) listener.deletionError(APIError.UNKNOWN);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(anError.getErrorCode() == 0 && listener != null) {
                            listener.deleted(commentId);
                        } else if(listener != null) {
                            listener.deletionError(parseError(anError));
                        }
                        Log.i(TAG, "onError: Comment deletion error: " + anError.getErrorBody());
                    }
                });

    }

    public void starRepo(GITModelUpdateListener<Boolean> listener, String fullRepoName) {
        AndroidNetworking.put(GIT_BASE + SEGMENT_USER + SEGMENT_STARRED + "/" + fullRepoName)
                .addHeaders(API_AUTH_HEADERS)
                .addHeaders("Content-Length", "0")
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        if(response.code() == 204) {
                            if(listener != null) listener.updated(true);
                        } else {
                            if(listener != null) listener.updated(false);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.updated(false);
                    }
                });
    }

    public void unstarRepo(GITModelUpdateListener<Boolean> listener, String fullRepoName) {
        AndroidNetworking.delete(GIT_BASE + SEGMENT_USER + SEGMENT_STARRED + "/" + fullRepoName)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        if(response.code() == 204) {
                            if(listener != null) listener.updated(false);
                        } else {
                            if(listener != null) listener.updated(true);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.updated(true);
                    }
                });
    }

    public void watchRepo(GITModelUpdateListener<Boolean> listener, String fullRepoName) {
        AndroidNetworking.put(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_SUBSCRIPTION)
                .addHeaders(API_AUTH_HEADERS)
                .addPathParameter("subscribed", "true")
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Subscription change " + response.toString());
                        try {
                            if(response.has("subscribed")) {
                                if(listener != null)
                                    listener.updated(response.getBoolean("subscribed"));
                            } else {
                                if(listener != null) listener.updated(false);
                            }
                        } catch(JSONException jse) {
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.updated(false);
                    }
                });
    }

    public void unwatchRepo(GITModelUpdateListener<Boolean> listener, String fullRepoName) {
        AndroidNetworking.put(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_SUBSCRIPTION)
                .addHeaders(API_AUTH_HEADERS)
                .addPathParameter("subscribed", "false")
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Subscription change " + response.toString());
                        try {
                            if(response.has("subscribed")) {
                                if(listener != null)
                                    listener.updated(response.getBoolean("subscribed"));
                            } else {
                                if(listener != null) listener.updated(true);
                            }
                        } catch(JSONException jse) {
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.updated(true);
                    }
                });
    }

    public void createMilestone(GITModelCreationListener<Milestone> listener, String fullRepoName, @NonNull String title, @Nullable String description, @Nullable String dueOn) {
        final JSONObject obj =  new JSONObject();
        try {
            obj.put(TITLE, title);
            if(description != null) obj.put("description", description);
            if(dueOn != null) obj.put("due_on", dueOn);

        } catch(JSONException jse) {
            Log.e(TAG, "updateMilestone: ", jse);
        }
        AndroidNetworking.post(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_MILESTONES)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(listener != null) listener.created(Milestone.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: " + anError.getErrorDetail());
                        Log.i(TAG, "onError: " + anError.toString());
                        Log.i(TAG, "onError: " + anError.getErrorCode());
                        Log.i(TAG, "onError: " + anError.getErrorBody());
                        if(listener != null) listener.creationError(parseError(anError));
                    }
                });
    }

    public void updateMilestone(GITModelUpdateListener<Milestone> listener, String fullRepoName, int number, @Nullable String title, @Nullable String description, @Nullable String dueOn, @NonNull State state) {
        final JSONObject obj =  new JSONObject();
        try {
            if(title != null) obj.put(TITLE, title);
            if(description != null) obj.put("description", description);
            if(dueOn != null) obj.put("due_on", dueOn);
            if(state != State.ALL) obj.put("state", state.toString().toLowerCase());
        } catch(JSONException jse) {
            Log.e(TAG, "updateMilestone: ", jse);
        }
        AndroidNetworking.patch(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_MILESTONES + "/" + number)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(listener != null) listener.updated(Milestone.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: " + anError.getErrorDetail());
                        if(listener != null) listener.updateError(parseError(anError));
                    }
                });

    }

    public interface GITModelCreationListener<T> {

        void created(T t);

        void creationError(APIError error);

    }

    public interface GITModelDeletionListener<T> {

        void deleted(T t);

        void deletionError(APIError error);

    }

    public interface GITModelUpdateListener<T> {

        void updated(T t);

        void updateError(APIError error);



    }

}
