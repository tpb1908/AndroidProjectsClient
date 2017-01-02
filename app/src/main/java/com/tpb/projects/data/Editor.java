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
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Column;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Project;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    private static final String SEGMENT_REPOS = "repos/";
    private static final String SEGMENT_PROJECTS = "/projects";
    private static final String SEGMENT_COLUMNS = "/columns";
    private static final String SEGMENT_MOVES = "/moves";
    private static final String SEGMENT_CARDS = "/cards";
    private static final String SEGMENT_ISSUES = "/issues";

    public Editor(Context context) {
        super(context);
    }

    public void createProject(final ProjectCreationListener listener, Project project, String fullRepoName) {
        Log.i(TAG, "createProject: Project " + project.toString());
        JSONObject obj = new JSONObject();
        //Unsure why GitHub can't parse the JSON if I add these as body parameters
        try {
            obj.put(NAME, project.getName());
            obj.put(BODY, project.getBody());
        } catch(JSONException jse) {
            Log.e(TAG, "createProject: ", jse);
        }
        AndroidNetworking.post(GIT_BASE + SEGMENT_REPOS + fullRepoName + SEGMENT_PROJECTS)
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
                        if(listener != null) listener.projectCreationError();
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
        AndroidNetworking.patch(GIT_BASE + SEGMENT_PROJECTS + project.getId())
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
                        if(listener != null) listener.projectEditError();
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
                        listener.projectDeletionError();
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: " + anError.getErrorCode());
                        Log.i(TAG, "onError: " + anError.getErrorBody());
                        if(anError.getErrorCode() == 0 && anError.getErrorBody() == null && listener != null) {
                            listener.projectDeleted(project);
                        } else {
                            listener.projectDeletionError();
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
        AndroidNetworking.patch(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + Integer.toString(columnId))
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //TODO Use the listener
                        Log.i(TAG, "onResponse: Column update: " + response.toString());
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Column update: " + anError.getErrorBody());
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
        AndroidNetworking.post(GIT_BASE + SEGMENT_PROJECTS + "/" + Integer.toString(projectId) + SEGMENT_COLUMNS)
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
                        if(listener != null) listener.columnAdditionError();
                    }
                });
    }

    public void moveColumn(ColumnMovementListener listener, int columnId, int dropPositionId, int position) {
        final JSONObject obj = new JSONObject();
        try {
            if(position == 0) {
                obj.put(POSITION, POSITION_FIRST);
            } else {
                obj.put(POSITION, POSITION_AFTER + Integer.toString(dropPositionId));
            }
        } catch(JSONException jse) {
            Log.e(TAG, "moveColumn: ", jse);
        }
        AndroidNetworking.post(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + Integer.toString(columnId) + SEGMENT_MOVES)
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
                        if(listener != null) listener.columnMovementError();
                    }
                });
    }

    public void deleteColumn(ColumnDeletionListener listener, int columnId) {
        AndroidNetworking.delete(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + Integer.toString(columnId))
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Column delete: " + response.toString());
                        if(listener != null) listener.columnDeletionError();
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Column delete: " + anError.getErrorBody());
                        Log.i(TAG, "onError: Column delete: " + anError.getErrorCode());
                        if(listener != null) listener.columnDeleted();
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
        AndroidNetworking.post(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + Integer.toString(columnId) + SEGMENT_CARDS)
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
                        if(listener != null) listener.cardCreationError();
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
        AndroidNetworking.post(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + Integer.toString(columnId) + SEGMENT_CARDS)
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
                        if(listener != null) listener.cardCreationError();
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
        AndroidNetworking.patch(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + SEGMENT_CARDS + "/" + Integer.toString(cardId))
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
                        if(listener != null) listener.cardUpdateError();
                    }
                });
    }

    public void moveCard(CardMovementListener listener, int columnId, int cardId, int afterId) {
        final JSONObject obj = new JSONObject();
        try {
            if(afterId == -1) {
                obj.put(POSITION, POSITION_TOP);
            } else {
                obj.put(POSITION, POSITION_AFTER + Integer.toString(afterId));
            }
            if(columnId != -1) obj.put(COLUMN_ID, columnId);
        } catch(JSONException jse) {
            Log.e(TAG, "moveCard: ", jse);
        }
        Log.i(TAG, "moveCard: " + obj.toString() + ", card " + cardId);
        AndroidNetworking.post(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + SEGMENT_CARDS + "/" + Integer.toString(cardId) + SEGMENT_MOVES)
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //TODO Use listener
                        Log.i(TAG, "onResponse: Card moved " + response.toString());

                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Card move error " + anError.getErrorBody());
                    }
                });
    }

    public void deleteCard(CardDeletionListener listener, Card card) {
        AndroidNetworking.delete(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + SEGMENT_CARDS + "/" + Integer.toString(card.getId()))
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Card deleted: " + response.toString());
                        if(listener != null) listener.cardDeletionError();
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(anError.getErrorCode() == 0 && listener != null) {
                            listener.cardDeleted(card);
                        }
                        Log.i(TAG, "onError: Card deleted: " + anError.getErrorBody());
                        Log.i(TAG, "onError: Card deleted: " + anError.getErrorCode());
                    }
                });
    }

    public void createIssue(IssueCreationListener listener, String fullRepoName, String title, String body, @Nullable String[] assignees, @Nullable String[] labels) {
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
        AndroidNetworking.post(GIT_BASE + SEGMENT_REPOS + fullRepoName + SEGMENT_ISSUES)
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
                        if(listener != null) listener.issueCreationError();
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
        AndroidNetworking.patch(GIT_BASE + SEGMENT_REPOS + fullRepoPath + SEGMENT_ISSUES + "/" + Integer.toString(issueNumber))
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
                        if(listener != null) listener.issueStateChangeError();
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
        AndroidNetworking.patch(GIT_BASE + SEGMENT_REPOS + fullRepoPath + SEGMENT_ISSUES + "/" + Integer.toString(issueNumber))
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
                        if(listener != null) listener.issueStateChangeError();
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
        AndroidNetworking.patch(GIT_BASE + SEGMENT_REPOS + fullRepoPath +  SEGMENT_ISSUES + "/" + Integer.toString(issue.getNumber()))
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
                        if(listener != null) listener.issueEditError();
                    }
                });
    }

    public interface ProjectCreationListener {

        void projectCreated(Project project);

        void projectCreationError();

    }

    public interface ProjectEditListener {

        void projectEdited(Project project);

        void projectEditError();

    }

    public interface ProjectDeletionListener {

        void projectDeleted(Project project);

        void projectDeletionError();

    }

    public interface ColumnNameChangeListener {

        void columnNameChanged(Column column);

        void columnNameChangeError();

    }

    public interface ColumnAdditionListener {

        void columnAdded(Column column);

        void columnAdditionError();

    }

    public interface ColumnDeletionListener {

        void columnDeleted();

        void columnDeletionError();

    }

    public interface ColumnMovementListener {

        void columnMoved(int columnId);

        void columnMovementError();
    }

    public interface CardCreationListener {

        void cardCreated(int columnId, Card card);

        void cardCreationError();

    }

    public interface CardUpdateListener {

        void cardUpdated(Card card);

        void cardUpdateError();

    }

    public interface CardMovementListener {

        void cardMoved(int cardId);

        void cardMovementError();

    }

    public interface CardDeletionListener {

        void cardDeleted(Card card);

        void cardDeletionError();

    }

    public interface IssueCreationListener {

        void issueCreated(Issue issue);

        void issueCreationError();

    }

    public interface IssueStateChangeListener {

        void issueStateChanged(Issue issue);

        void issueStateChangeError();

    }

    public interface IssueEditListener {

        void issueEdited(Issue issue);

        void issueEditError();
    }

}
