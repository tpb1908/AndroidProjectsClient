package com.tpb.projects.data;

import android.content.Context;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.androidnetworking.interfaces.StringRequestListener;
import com.tpb.projects.data.auth.GitHubSession;
import com.tpb.projects.data.auth.models.Repository;
import com.tpb.projects.util.Logging;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by theo on 14/12/16.
 */

public class Loader {
    private static final String TAG = Loader.class.getSimpleName();

    private static final String GIT_BASE = "https://api.github.com/";
    private static final String GIT_REPOS = "%1$s/repos/";

    private static GitHubSession mSession;

    //https://developer.github.com/v3/repos/
    //https://developer.github.com/v3/projects/
    //https://developer.github.com/v3/projects/columns/#get-a-project-column
    //https://developer.github.com/v3/projects/cards/

    public Loader(Context context) {
        if(mSession == null) mSession = new GitHubSession(context);
    }


    public void loadRepositories(String user) {
        final String path = appendAccessToken(GIT_BASE + "users/" + user + "/repos");
        AndroidNetworking.get(path)
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        Log.i(TAG, "onResponse: " + response);
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: " + anError.getErrorCode());
                        Log.i(TAG, "onError: " + anError.getErrorDetail());
                        Log.i(TAG, "onError: " + anError.getErrorBody());
                    }
                });
    }

    public void loadRepositories() {
        final String path = appendAccessToken(GIT_BASE + "user/repos");
        Log.i(TAG, "loadRepositories: " + path);
        AndroidNetworking.get(path)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray jsa) {
                        try {
                            final Repository[] repos = new Repository[jsa.length()];
                            for(int i = 0; i < repos.length; i++) {
                                repos[i] = Repository.parse(jsa.getJSONObject(i));
                                Log.i(TAG, "onResponse: " + repos[i].toString());
                            }
                            Log.i(TAG, "onResponse: successfully parsed repos");
                        } catch(JSONException jse) {
                            Log.i(TAG, "onResponse: " + jsa.toString());
                            Log.e(TAG, "onResponse: ", jse);
                            Logging.largeDebugDump(TAG, jse.getMessage());
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: " + anError.getErrorBody());
                    }
                });
    }

    private String appendAccessToken(String path) {
        return path + "?access_token=" + mSession.getAccessToken();
    }

}
