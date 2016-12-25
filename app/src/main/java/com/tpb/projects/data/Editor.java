package com.tpb.projects.data;

import android.content.Context;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Column;
import com.tpb.projects.data.models.Project;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by theo on 18/12/16.
 */

public class Editor extends APIHandler {
    private static final String TAG = Editor.class.getSimpleName();

    public Editor(Context context) {
        super(context);
    }

    public void createProject(final ProjectCreationListener listener, Project project, String fullRepoName) {
        Log.i(TAG, "createProject: Project " + project.toString());
        JSONObject obj = new JSONObject();
        //Unsure why GitHub can't parse the JSON if I add these as body parameters
        try {
            obj.put("name", project.getName());
            obj.put("body", project.getBody());
        } catch(JSONException jse) {
            Log.e(TAG, "createProject: ", jse);
        }
        AndroidNetworking.post(GIT_BASE + "repos/" + fullRepoName + "/projects")
                .addHeaders(PREVIEW_API_AUTH_HEADERS)
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
            obj.put("name", project.getName());
            obj.put("body", project.getBody());
        } catch(JSONException jse) {
            Log.e(TAG, "createProject: ", jse);
        }
        AndroidNetworking.patch(GIT_BASE + "projects/" + project.getId())
                .addHeaders(PREVIEW_API_AUTH_HEADERS)
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
        AndroidNetworking.delete(GIT_BASE + "projects/" + project.getId())
                .addHeaders(PREVIEW_API_AUTH_HEADERS)
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
            obj.put("name", newName);
        } catch(JSONException jse) {
            Log.e(TAG, "updateColumnName: ", jse);
        }
        AndroidNetworking.patch(GIT_BASE + "projects/columns/" + Integer.toString(columnId))
                .addHeaders(PREVIEW_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
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
            obj.put("name", name);
        } catch(JSONException jse) {
            Log.e(TAG, "addColumn: ", jse);
        }
        AndroidNetworking.post(GIT_BASE + "projects/" + Integer.toString(projectId) + "/columns")
                .addHeaders(PREVIEW_API_AUTH_HEADERS)
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

    public void deleteColumn(ColumnDeletionListener listener, int columnId) {
        AndroidNetworking.delete(GIT_BASE + "projects/columns/" + Integer.toString(columnId))
                .addHeaders(PREVIEW_API_AUTH_HEADERS)
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

    //https://developer.github.com/v3/projects/cards/#create-a-project-card
    public void createCard(CardCreationListener listener, int columnId, Card card) {
        //TODO Support creating issue cards
        /*
        Process
        * Show dialog for the issue title and body
        * Delete the card
        * Create the issue
        * Create the new card with the issue id
         */
        final JSONObject obj = new JSONObject();
        try {
            obj.put("note", card.getNote());
        } catch(JSONException jse) {
            Log.e(TAG, "createCard: ", jse);
        }
        AndroidNetworking.post(GIT_BASE + "projects/columns/" + Integer.toString(columnId) + "/cards")
                .addHeaders(PREVIEW_API_AUTH_HEADERS)
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
            obj.put("note", note);
        } catch(JSONException jse) {
            Log.e(TAG, "updateCard: ", jse);
        }
        AndroidNetworking.patch(GIT_BASE + "projects/columns/cards/" + Integer.toString(cardId))
                .addHeaders(PREVIEW_API_AUTH_HEADERS)
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
                obj.put("position", "top");
            } else {
                obj.put("position", "after:" + Integer.toString(afterId));
            }
            if(columnId != -1) obj.put("column_id", columnId);
        } catch(JSONException jse) {
            Log.e(TAG, "moveCard: ", jse);
        }
        Log.i(TAG, "moveCard: " + obj.toString() + ", card " + cardId);
        AndroidNetworking.post(GIT_BASE + "projects/columns/cards/" + Integer.toString(cardId) + "/moves")
                .addHeaders(PREVIEW_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Card moved " + response.toString());

                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Card move error " + anError.getErrorBody());
                    }
                });
    }

    public void deleteCard(CardDeletionListener listener, Card card) {
        AndroidNetworking.delete(GIT_BASE + "projects/columns/cards/" + Integer.toString(card.getId()))
                .addHeaders(PREVIEW_API_AUTH_HEADERS)
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

}
