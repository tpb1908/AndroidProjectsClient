package com.tpb.projects.data.api;

import android.text.TextUtils;

import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.util.Key;
import com.google.api.client.util.ObjectParser;
import com.tpb.projects.data.models.GitResponse;
import com.tpb.projects.data.models.Pagination;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Created by theo on 15/12/16.
 */

public class GitRequest<T> extends AbstractGoogleJsonClientRequest<T> {


    public GitRequest(GitHub client, String requestMethod,
                      String uriTemplate, Object content, Class<T> responseClass) {
        super(client,
                requestMethod,
                uriTemplate,
                content,
                responseClass);
    }

    @Key("access_token")
    private String accessToken;

    public final String getAccessToken() {
        return accessToken;
    }

    public final GitRequest<T> setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    @Override
    public GitHub getAbstractGoogleClient() {
        return (GitHub) super.getAbstractGoogleClient();
    }

    @Override
    public GitRequest<T> setDisableGZipContent(boolean disableGZipContent) {
        return (GitRequest<T>) super.setDisableGZipContent(disableGZipContent);
    }

    @Override
    public GitRequest<T> setRequestHeaders(HttpHeaders headers) {
        return (GitRequest<T>) super.setRequestHeaders(headers);
    }

    @Override
    public GitRequest<T> set(String fieldName, Object value) {
        return (GitRequest<T>) super.set(fieldName, value);
    }

    @Override
    public T execute() throws IOException {
        HttpResponse response = super.executeUnparsed();
        ObjectParser parser = response.getRequest().getParser();
        // This will degrade parsing performance but is an inevitable workaround
        // for the inability to parse JSON arrays.
        String content = response.parseAsString();
        if (response.isSuccessStatusCode()
                && !TextUtils.isEmpty(content)
                && content.charAt(0) == '[') {
            content = TextUtils.concat("{\"", GitResponse.KEY_DATA, "\":", content, "}")
                    .toString();
        }
        Reader reader = new StringReader(content);
        T parsedResponse = parser.parseAndClose(reader, getResponseClass());

        // parse pagination from Link header
        if (parsedResponse instanceof GitResponse) {
            Pagination pagination =
                    new Pagination(response.getHeaders().getFirstHeaderStringValue("Link"));
            ((GitResponse) parsedResponse).setPagination(pagination);
        }

        return parsedResponse;
    }

}