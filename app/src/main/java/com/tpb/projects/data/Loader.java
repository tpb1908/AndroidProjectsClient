package com.tpb.projects.data;

import android.os.AsyncTask;
import android.util.Log;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

/**
 * Created by theo on 14/12/16.
 */

public class Loader {
    private static final String TAG = Loader.class.getSimpleName();



    //https://developer.github.com/v3/repos/
    //https://developer.github.com/v3/projects/
    //https://developer.github.com/v3/projects/columns/#get-a-project-column
    //https://developer.github.com/v3/projects/cards/

    public static void tryLogin(String username, String password) {
        final GitHubClient client = new GitHubClient();
        client.setCredentials(username, password);
        AsyncTask.execute(() -> {
            final RepositoryService service = new RepositoryService(client);
            try {
                for(Repository r : service.getRepositories()) {
                    Log.i(TAG, "tryLogin: " + r.getName());
                }
            } catch(Exception e) {
                Log.e(TAG, "tryLogin: ", e);
            }
        });

    }

    public static void largeDebugDump(String tag, String dump) {
        Log.i(TAG, "largeDebugDump: " + dump.length());
        final int len = dump.length();
        for(int i = 0; i < len; i += 1024) {
            if(i + 1024 < len) {
                Log.d(tag, dump.substring(i, i + 1024));
            } else {
                Log.d(tag, dump.substring(i, len));
            }
        }
    }

}
