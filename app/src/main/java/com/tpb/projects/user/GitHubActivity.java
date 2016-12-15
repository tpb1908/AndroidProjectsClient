package com.tpb.projects.user;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Window;

import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.util.Lists;
import com.tpb.projects.R;
import com.tpb.projects.data.api.AsyncResourceLoader;
import com.tpb.projects.data.api.GitHub;
import com.tpb.projects.data.api.GitRequestInitializer;
import com.tpb.projects.data.api.OAuth;
import com.tpb.projects.data.models.Error;
import com.tpb.projects.data.models.Repos;
import com.tpb.projects.util.Constants;


/**
 * Created by theo on 15/12/16.
 */

public class GitHubActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    public static class GitHubLoader extends AsyncResourceLoader<Repos> {

        private final OAuth oauth;
        private GitHub.User.ListReposRequest request;
        private final int page;

        public GitHubLoader(FragmentActivity activity, int since) {
            super(activity);
            this.page = since;
            this.oauth = OAuth.newInstance(activity.getApplicationContext(),
                    activity.getSupportFragmentManager(),
                    new ClientParametersAuthentication(Constants.CLIENT_ID,
                            Constants.CLIENT_SECRET),
                    Constants.AUTH_SERVER_URL,
                    Constants.TOKEN_SERVER_URL,
                    Constants.REDIRECT_URL,
                    Lists.<String> newArrayList());
        }

        public boolean isLoadMoreRequest() {
            return page != 0;
        }

        @Override
        public Repos loadResourceInBackground() throws Exception {
            Credential credential = oauth.authorizeExplicitly("github").getResult();

            GitHub github =
                    new GitHub.Builder(OAuth.HTTP_TRANSPORT, OAuth.JSON_FACTORY, credential)
                            .setApplicationName(getContext().getString(R.string.app_name))
                            .setGitHubRequestInitializer(new GitRequestInitializer(credential))
                            .build();
            request = github.user().repos();
            if (isLoadMoreRequest()) {
                request.setPage(page);
            }
            Repos repositories = request.execute();
            return repositories;
        }

        @Override
        public void updateErrorStateIfApplicable(AsyncResourceLoader.Result<Repos> result) {
            Repos data = result.data;
            result.success = request.getLastStatusCode() == HttpStatusCodes.STATUS_CODE_OK;
            if (result.success) {
                result.errorMessage = null;
            } else {
                result.errorMessage = data.getMessage();
                if (data.getErrors() != null && data.getErrors().size() > 0) {
                    Error error = data.getErrors().get(0);
                    result.errorMessage += (error.getCode());
                }
            }
        }

    }

}