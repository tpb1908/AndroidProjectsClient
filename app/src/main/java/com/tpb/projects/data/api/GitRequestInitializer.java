package com.tpb.projects.data.api;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest;
import com.google.api.client.googleapis.services.json.CommonGoogleJsonClientRequestInitializer;
import com.google.api.client.util.Preconditions;

import java.io.IOException;

/**
 * Created by theo on 15/12/16.
 */

public class GitRequestInitializer extends CommonGoogleJsonClientRequestInitializer {

    private final Credential credential;

    public GitRequestInitializer(Credential credential) {
        super();
        this.credential = Preconditions.checkNotNull(credential);
    }

    @Override
    protected void initializeJsonRequest(AbstractGoogleJsonClientRequest<?> request)
            throws IOException {
        super.initializeJsonRequest(request);
        initializeGitHubRequest((GitRequest<?>) request);
    }

    protected void initializeGitHubRequest(GitRequest<?> request)
            throws java.io.IOException {
        request.setAccessToken(credential.getAccessToken());
    }

}