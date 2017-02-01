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

package com.tpb.projects.data;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.OkHttpResponseListener;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Column;
import com.tpb.projects.data.models.Comment;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Project;

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

    public void createProject(final ProjectCreationListener listener, Project project, String repoFullName) {
        Log.i(TAG, "createProject: Project " + project.toString());
        JSONObject obj = new JSONObject();
        //Unsure why GitHub can't parse the JSON if I add these as body parameters
        try {
            obj.put(NAME, project.getName());
            obj.put(BODY, project.getBody());
        } catch(JSONException jse) {
            Log.e(TAG, "createProject: ", jse);
        }
        AndroidNetworking.post(GIT_BASE +  SEGMENT_REPOS + "/" + repoFullName + SEGMENT_PROJECTS)
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: " + response.toString());
                        if(listener != null) listener.projectCreated(Project.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: " + anError.getErrorBody());
                        if(listener != null) listener.projectCreationError(parseError(anError));
                    }
                });
    }

    public void editProject(final ProjectEditListener listener, Project project) {
        final JSONObject obj = new JSONObject();
        //Unsure why GitHub can't parse the JSON if I add these as body parameters
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
                        if(listener != null) listener.projectEdited(Project.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: " + anError.getErrorBody());
                        if(listener != null) listener.projectEditError(parseError(anError));
                    }
                });
    }

    public void deleteProject(final ProjectDeletionListener listener, Project project) {
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
                        listener.projectDeletionError(APIError.UNKNOWN);
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: " + anError.getErrorCode());
                        Log.i(TAG, "onError: " + anError.getErrorBody());
                        if(anError.getErrorCode() == 0 && anError.getErrorBody() == null && listener != null) {
                            listener.projectDeleted(project);
                        } else if(listener != null){
                            listener.projectDeletionError(parseError(anError));
                        }
                    }
                });
    }

    public void updateColumnName(ColumnNameChangeListener listener, int columnId, String newName) {
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
                        if(listener != null) listener.columnNameChanged(Column.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Column update: " + anError.getErrorBody());
                        if(listener != null) listener.columnNameChangeError(parseError(anError));
                    }
                });
    }

    public void addColumn(ColumnAdditionListener listener, int projectId, String name) {
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
                        if(listener != null) listener.columnAdded(Column.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: " + anError.getErrorBody());
                        if(listener != null) listener.columnAdditionError(parseError(anError));
                    }
                });
    }

    public void moveColumn(ColumnMovementListener listener, int columnId, int dropPositionId, int position) {
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
                        Log.i(TAG, "onResponse: Column moved: " + response.toString());
                        if(listener != null) listener.columnMoved(columnId);
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Column not moved: " + anError.getErrorBody());
                        if(listener != null) listener.columnMovementError(parseError(anError));
                    }
                });
    }

    public void deleteColumn(ColumnDeletionListener listener, int columnId) {
        AndroidNetworking.delete(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + columnId)
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Column delete: " + response.toString());
                        if(listener != null) listener.columnDeletionError(APIError.UNKNOWN);
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Column delete: " + anError.getErrorBody());
                        Log.i(TAG, "onError: Column delete: " + anError.getErrorCode());
                        if(listener != null) {
                            if(anError.getErrorCode() == 0) {
                                listener.columnDeleted();
                            } else {
                                listener.columnDeletionError(parseError(anError));
                            }
                        }
                    }
                });
    }

    public void createCard(CardCreationListener listener, int columnId, String note) {
        /*
        Process
        * Show dialog for the issue title and body
        * Delete the card
        * Create the issue
        * Create the new card with the issue id
         */
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
                        if(listener != null) listener.cardCreated(columnId, Card.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Card: " + anError.getErrorBody());
                        if(listener != null) listener.cardCreationError(parseError(anError));
                    }
                });
    }

    public void createCard(CardCreationListener listener, int columnId, int issueId) {
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
                        if(listener != null) listener.cardCreated(columnId, Card.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Card: " + anError.getErrorBody());
                        if(listener != null) listener.cardCreationError(parseError(anError));
                    }
                });
    }

    public void updateCard(CardUpdateListener listener, int cardId, String note) {
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
                        if(listener != null) listener.cardUpdated(Card.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Card update: " + anError.getErrorBody());
                        if(listener != null) listener.cardUpdateError(parseError(anError));
                    }
                });
    }

    public void moveCard(CardMovementListener listener, int columnId, int cardId, int afterId) {
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
                        if(listener != null) listener.cardMoved(cardId);
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Card move error " + anError.getErrorBody());
                        if(listener != null) listener.cardMovementError(parseError(anError));
                    }
                });
    }

    public void deleteCard(CardDeletionListener listener, Card card) {
        AndroidNetworking.delete(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + SEGMENT_CARDS + "/" + card.getId())
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Card deleted: " + response.toString());
                        if(listener != null) listener.cardDeletionError(APIError.UNKNOWN);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(anError.getErrorCode() == 0 && listener != null) {
                            listener.cardDeleted(card);
                        } else if(listener != null){
                            listener.cardDeletionError(parseError(anError));
                        }
                        Log.i(TAG, "onError: Card deleted: " + anError.getErrorBody());
                        Log.i(TAG, "onError: Card deleted: " + anError.getErrorCode());
                    }
                });
    }

    public void createIssue(IssueCreationListener listener, String repoFullName, String title, String body, @Nullable String[] assignees, @Nullable String[] labels) {
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
                        if(listener != null) listener.issueCreated(Issue.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Issue not created " + anError.getErrorBody());
                        if(listener != null) listener.issueCreationError(parseError(anError));
                    }
                });
    }

    public void closeIssue(IssueStateChangeListener listener, String fullRepoPath, int issueNumber) {
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
                        if(listener != null) listener.issueStateChanged(Issue.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Issue close error " + anError.getErrorBody());
                        if(listener != null) listener.issueStateChangeError(parseError(anError));
                    }
                });
    }

    public void openIssue(IssueStateChangeListener listener, String fullRepoPath, int issueNumber) {
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
                        if(listener != null) listener.issueStateChanged(Issue.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Issue open error " + anError.getErrorBody());
                        if(listener != null) listener.issueStateChangeError(parseError(anError));
                    }
                });
    }

    public void editIssue(IssueEditListener listener, String fullRepoPath, Issue issue, @Nullable String[] assignees,  @Nullable String[] labels) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(TITLE, issue.getTitle());
            obj.put(BODY, issue.getBody());
            if(assignees != null) obj.put(ASSIGNEES, new JSONArray(assignees));
            if(labels != null) obj.put(LABELS, new JSONArray(labels));
        } catch(JSONException jse) {
            Log.e(TAG, "createIssue: ", jse);
        }
        AndroidNetworking.patch(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoPath +  SEGMENT_ISSUES + "/" + issue.getNumber())
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Issue edited " + response.toString());
                        if(listener != null) listener.issueEdited(Issue.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Issue not edited");
                        if(listener != null) listener.issueEditError(parseError(anError));
                    }
                });
    }

    public void createComment(CommentCreationListener listener, String fullRepoPath, int issueNumber, String body) {
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
                        if(listener != null) listener.commentCreated(Comment.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Comment not created: " + anError.getErrorBody());
                        if(listener != null) listener.commentCreationError(parseError(anError));
                    }
                });
    }

    public void editComment(CommentEditListener listener, String fullRepoPath, int commentId, String body) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(BODY, body);
        } catch(JSONException jse) {
            Log.e(TAG, "createComment: ", jse);
        }
        AndroidNetworking.patch(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoPath +  SEGMENT_ISSUES + SEGMENT_COMMENTS + "/" + commentId)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(listener != null) listener.commentEdited(Comment.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Comment not edited " + anError.getErrorBody());
                        if(listener != null) listener.commentEditError(parseError(anError));
                    }
                });
    }

    public void deleteComment(CommentDeletionListener listener, String fullRepoPath, int commentId) {
        AndroidNetworking.delete(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoPath + SEGMENT_ISSUES + SEGMENT_COMMENTS + "/" + commentId)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(listener != null) listener.commentDeletionError(APIError.UNKNOWN);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(anError.getErrorCode() == 0 && listener != null) {
                            listener.commentDeleted();
                        } else if(listener != null){
                            listener.commentDeletionError(parseError(anError));
                        }
                        Log.i(TAG, "onError: Comment deletion error: " + anError.getErrorBody());
                    }
                });

    }

    public void starRepo(StarChangeListener listener, String fullRepoName) {
        AndroidNetworking.put(GIT_BASE + SEGMENT_USER + SEGMENT_STARRED + "/" + fullRepoName)
                .addHeaders(API_AUTH_HEADERS)
                .addHeaders("Content-Length", "0")
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        if(response.code() == 204) {
                            if(listener != null) listener.starStatusChanged(true);
                        } else {
                            if(listener != null) listener.starStatusChanged(false);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.starStatusChanged(false);
                    }
                });
    }

    public void unstarRepo(StarChangeListener listener, String fullRepoName) {
        AndroidNetworking.delete(GIT_BASE + SEGMENT_USER + SEGMENT_STARRED + "/" + fullRepoName)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        if(response.code() == 204) {
                            if(listener != null) listener.starStatusChanged(false);
                        } else {
                            if(listener != null) listener.starStatusChanged(true);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.starStatusChanged(true);
                    }
                });
    }

    public void watchRepo(WatchChangeListener listener, String fullRepoName) {
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
                                if(listener != null) listener.watchStatusChanged(response.getBoolean("subscribed"));
                            } else {
                                if(listener != null) listener.watchStatusChanged(false);
                            }
                        } catch(JSONException jse) {}
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.watchStatusChanged(false);
                    }
                });
    }

    public void unwatchRepo(WatchChangeListener listener, String fullRepoName) {
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
                                if(listener != null) listener.watchStatusChanged(response.getBoolean("subscribed"));
                            } else {
                                if(listener != null) listener.watchStatusChanged(true);
                            }
                        } catch(JSONException jse) {}
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.watchStatusChanged(true);
                    }
                });
    }

    public interface ProjectCreationListener {

        void projectCreated(Project project);

        void projectCreationError(APIError error);

    }

    public interface ProjectEditListener {

        void projectEdited(Project project);

        void projectEditError(APIError error);

    }

    public interface ProjectDeletionListener {

        void projectDeleted(Project project);

        void projectDeletionError(APIError error);

    }

    public interface ColumnNameChangeListener {

        void columnNameChanged(Column column);

        void columnNameChangeError(APIError error);

    }

    public interface ColumnAdditionListener {

        void columnAdded(Column column);

        void columnAdditionError(APIError error);

    }

    public interface ColumnDeletionListener {

        void columnDeleted();

        void columnDeletionError(APIError error);

    }

    public interface ColumnMovementListener {

        void columnMoved(int columnId);

        void columnMovementError(APIError error);
    }

    public interface CardCreationListener {

        void cardCreated(int columnId, Card card);

        void cardCreationError(APIError error);

    }

    public interface CardUpdateListener {

        void cardUpdated(Card card);

        void cardUpdateError(APIError error);

    }

    public interface CardMovementListener {

        void cardMoved(int cardId);

        void cardMovementError(APIError error);

    }

    public interface CardDeletionListener {

        void cardDeleted(Card card);

        void cardDeletionError(APIError error);

    }

    public interface IssueCreationListener {

        void issueCreated(Issue issue);

        void issueCreationError(APIError error);

    }

    public interface IssueStateChangeListener {

        void issueStateChanged(Issue issue);

        void issueStateChangeError(APIError error);

    }

    public interface IssueEditListener {

        void issueEdited(Issue issue);

        void issueEditError(APIError error);
    }

    public interface CommentCreationListener {

        void commentCreated(Comment comment);

        void commentCreationError(APIError error);

    }

    public interface CommentEditListener {

        void commentEdited(Comment comment);

        void commentEditError(APIError error);

    }

    public interface CommentDeletionListener {

        void commentDeleted();

        void commentDeletionError(APIError error);

    }

    public interface StarChangeListener {

        void starStatusChanged(boolean isStarred);

    }

    public interface WatchChangeListener {

        void watchStatusChanged(boolean isWatched);

    }

}
