package com.tpb.projects.data;

import android.content.Context;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.tpb.projects.data.auth.models.Project;

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
        final String path = appendAccessToken(GIT_BASE + "repos/" + fullRepoName + "/projects");
        Log.i(TAG, "createProject: " + path);
        Log.i(TAG, "createProject: Project " + project.toString());
        JSONObject obj = new JSONObject();
        //Unsure why GitHub can't parse the JSON if I add these as body parameters
        try {
            obj.put("name", project.getName());
            obj.put("body", project.getBody());
        } catch(JSONException jse) {
            Log.e(TAG, "createProject: ", jse);
        }
        AndroidNetworking.post(path)
                .addHeaders("Accept", ACCEPT_HEADER)
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
                        if(listener != null) listener.creationError();
                    }
                });
    }

    public void editProject(final ProjectEditListener listener, Project project) {
        final String path = appendAccessToken(GIT_BASE + "projects/" + project.getId());
        Log.i(TAG, "editProject: " + path);
        JSONObject obj = new JSONObject();
        //Unsure why GitHub can't parse the JSON if I add these as body parameters
        try {
            obj.put("name", project.getName());
            obj.put("body", project.getBody());
        } catch(JSONException jse) {
            Log.e(TAG, "createProject: ", jse);
        }
        AndroidNetworking.patch(path)
                .addHeaders("Accept", ACCEPT_HEADER)
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
                        if(listener != null) listener.editError();
                    }
                });
    }

    public void deleteProject(final ProjectDeletionListener listener, Project project) {
        final String path = appendAccessToken(GIT_BASE + "projects/" + project.getId());
        /*
        It seems that on a successful deletion, this returns an error with null body
         */
        AndroidNetworking.delete(path)
                .addHeaders("Accept", ACCEPT_HEADER)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: " + response.toString());
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: " + anError.getErrorCode());
                        Log.i(TAG, "onError: " + anError.getErrorBody());
                    }
                });
    }

    public interface ProjectCreationListener {

        void projectCreated(Project project);

        void creationError();

    }

    public interface ProjectEditListener {

        void projectEdited(Project project);

        void editError();

    }

    public interface ProjectDeletionListener {

        void projectDelete(Project project);

        void deletionError();

    }

}
