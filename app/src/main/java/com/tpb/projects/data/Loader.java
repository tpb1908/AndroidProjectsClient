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
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.OkHttpResponseListener;
import com.androidnetworking.interfaces.StringRequestListener;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Column;
import com.tpb.projects.data.models.Comment;
import com.tpb.projects.data.models.Event;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.data.models.Project;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.data.models.User;
import com.tpb.projects.util.Data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import okhttp3.Response;

/**
 * Created by theo on 14/12/16.
 *
 */

public class Loader extends APIHandler {
    private static final String TAG = Loader.class.getSimpleName();

    private static final String PERMISSION = "permission";
    private static final String PERMISSION_NONE = "none";
    private static final String PERMISSION_ADMIN = "admin";
    private static final String PERMISSION_WRITE = "write";
    private static final String PERMISSION_READ = "read";
    private static final String CONTENT = "content";
    private static final String SEGMENT_MARKDOWN = "/markdown";

    public Loader(Context context) {
        super(context);
    }

    public void loadAuthenticateUser(AuthenticatedUserLoader loader) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_USER)
                .addHeaders(API_AUTH_HEADERS)
                .setPriority(Priority.IMMEDIATE)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: User loaded " + response.toString());
                        if(loader != null) loader.userLoaded(User.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Authenticated user error");
                        if(loader != null) loader.authenticatedUserLoadError(parseError(anError));
                    }
                });
    }

    public void loadUser(UserLoader loader, String username) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_USERS + "/" + username)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: User loaded");
                        if(loader != null) loader.userLoaded(User.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: User not loaded: " + anError.getErrorBody());
                        if(loader != null) loader.userLoadError(parseError(anError));
                    }
                });

    }

    public void loadRepositories(RepositoriesLoader loader, String user) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_USERS + "/" + user + SEGMENT_REPOS)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray jsa) {
                        try {
                            final Repository[] repos = new Repository[jsa.length()];
                            for(int i = 0; i < repos.length; i++) {
                                repos[i] = Repository.parse(jsa.getJSONObject(i));
                            }
                            Log.i(TAG, "onResponse: successfully parsed repos");
                            if(loader != null) loader.repositoriesLoaded(repos);
                        } catch(JSONException jse) {
                            Log.i(TAG, "onResponse: " + jsa.toString());
                            Log.e(TAG, "onResponse: ", jse);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: " + anError.getErrorBody());
                        if(loader != null) loader.repositoryLoadError(parseError(anError));
                    }
                });
    }

    public void loadRepositories(RepositoriesLoader loader) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_USER + SEGMENT_REPOS)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray jsa) {
                        try {
                            final Repository[] repos = new Repository[jsa.length()];
                            for(int i = 0; i < repos.length; i++) {
                                repos[i] = Repository.parse(jsa.getJSONObject(i));
                            }
                            Log.i(TAG, "onResponse: successfully parsed repos");
                            if(loader != null) loader.repositoriesLoaded(repos);
                        } catch(JSONException jse) {
                            Log.i(TAG, "onResponse: " + jsa.toString());
                            Log.e(TAG, "onResponse: ", jse);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: User repos" + anError.getErrorBody());
                        if(loader != null) loader.repositoryLoadError(parseError(anError));
                    }
                });
    }

    public void loadRepository(RepositoryLoader loader, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        final Repository repo = Repository.parse(response);
                        if(loader != null) loader.repoLoaded(repo);
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: load Repo: " + anError.getErrorBody());
                        if(loader != null) loader.repoLoadError(parseError(anError));
                    }
                });
    }

    public void loadReadMe(ReadMeLoader loader, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_README)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            final String readme = Data.base64Decode(response.getString(CONTENT));
                            Log.i(TAG, "onResponse: " + readme);
                            if(loader != null) loader.readMeLoaded(readme);
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: ", jse);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.e(TAG, "onError: load ReadMe: ", anError);
                        Log.i(TAG, "onError: load ReadMe: " + anError.getErrorBody());
                        if(loader != null) loader.readmeLoadError(parseError(anError));
                    }
                });
    }

    public void loadCollaborators(final CollaboratorsLoader loader, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_COLLABORATORS)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final User[] collabs = new User[response.length()];
                        for(int i = 0; i < collabs.length; i++) {
                            try {
                                collabs[i] = User.parse(response.getJSONObject(i));
                            } catch(JSONException jse) {
                                Log.e(TAG, "onResponse: ", jse);
                            }
                        }
                        if(loader != null) loader.collaboratorsLoaded(collabs);
                        Log.i(TAG, "onResponse: Collaborators" + Arrays.toString(collabs));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Collaborators" + anError.getErrorBody());
                        if(loader != null) loader.collaboratorsLoadError(parseError(anError));
                    }
                });
    }

    public void loadLabels(LabelsLoader loader, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_LABELS)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final Label[] labels = new Label[response.length()];
                        for(int i = 0; i < labels.length; i++) {
                            try {
                                labels[i] = Label.parse(response.getJSONObject(i));
                            } catch(JSONException jse) {
                                Log.e(TAG, "onResponse: Label parsing: ", jse);
                            }
                        }
                        if(loader != null) loader.labelsLoaded(labels);
                        Log.i(TAG, "onResponse: Labels: " + response.toString());
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Labels: " + anError.getErrorBody());
                        if(loader != null) loader.labelLoadError(parseError(anError));
                    }
                });
    }

    public void loadProject(ProjectLoader loader, int id) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_PROJECTS + "/" + id)
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(loader != null) loader.projectLoaded(Project.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(loader != null) loader.projectLoadError(parseError(anError));
                    }
                });
    }

    public void loadProjects(ProjectsLoader loader, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_PROJECTS)
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            final Project[] projects = new Project[response.length()];
                            for(int i = 0; i < response.length(); i++) {
                                projects[i] = Project.parse(response.getJSONObject(i));
                            }
                            if(loader != null) loader.projectsLoaded(projects);
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: ", jse);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.e(TAG, "onError: load Projects: ", anError);
                        Log.i(TAG, "onError: load Projects: " + anError.getErrorBody());
                        if(loader != null) loader.projectsLoadError(parseError(anError));
                    }
                });
    }

    public void loadColumns(ColumnsLoader loader, int projectId) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_PROJECTS + "/" + projectId + SEGMENT_COLUMNS)
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .getResponseOnlyFromNetwork()
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.i(TAG, "onResponse: " + response.toString());
                        final Column[] columns = new Column[response.length()];
                        for(int i = 0; i < columns.length; i++) {
                            try {
                                columns[i] = Column.parse(response.getJSONObject(i));
                            } catch(JSONException jse) {
                                Log.e(TAG, "onResponse: ", jse);
                            }
                        }
                        if(loader != null) loader.columnsLoaded(columns);
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: load columnes: " + anError.getErrorBody());
                        loader.columnsLoadError(parseError(anError));
                    }
                });
    }

    public void loadCards(CardsLoader loader, int columnId) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + columnId + SEGMENT_CARDS)
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .getResponseOnlyFromNetwork()
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.i(TAG, "onResponse: Cards " + response.length());
                        final Card[] cards = new Card[response.length()];
                        for(int i = 0; i < cards.length; i++) {
                            try {
                                cards[i] = Card.parse(response.getJSONObject(i));
                            } catch(JSONException jse) {
                                Log.e(TAG, "onResponse: ", jse);
                            }
                        }
                        if(loader != null) loader.cardsLoaded(cards);
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Cards: " + anError.getErrorBody());
                        if(loader != null) loader.cardsLoadError(parseError(anError));
                    }
                });
    }

    public void loadIssue(IssueLoader loader, String repoFullName, int issueNumber, boolean highPriority) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" +  repoFullName + SEGMENT_ISSUES + "/" + issueNumber)
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .setPriority(highPriority ? Priority.HIGH : Priority.MEDIUM)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(loader != null) loader.issueLoaded(Issue.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Issue: " + anError.getErrorBody());
                        if(loader != null) loader.issueLoadError(parseError(anError));
                    }
                });
    }

    public void loadOpenIssues(IssuesLoader loader, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_ISSUES)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final ArrayList<Issue> issues = new ArrayList<>();
                        for(int i = 0; i < response.length() ; i++) {
                            try {
                                final Issue is = Issue.parse(response.getJSONObject(i));
                                if(!is.isClosed()) issues.add(is);
                            } catch(JSONException jse) {
                                Log.e(TAG, "onResponse: Parsing open issues", jse);
                            }
                        }
                        if(loader != null) loader.issuesLoaded(issues.toArray(new Issue[0]));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Issue load: " + anError.getErrorBody());
                        if(loader != null) loader.issuesLoadError(parseError(anError));
                    }
                });

    }

    public void loadIssues(IssuesLoader loader, String repoFullName) {
        loadIssues(loader, repoFullName, Issue.IssueState.OPEN, null, null, 1);
    }

    public void loadIssues(IssuesLoader loader, String repoFullName, Issue.IssueState state, @Nullable String assignee, @Nullable List<String> labels) {
        loadIssues(loader, repoFullName, state, assignee, labels, 1);
    }

    public void loadIssues(IssuesLoader loader, String repoFullName, Issue.IssueState state, @Nullable String assignee, @Nullable List<String> labels, int page) {
        final HashMap<String, String> params = new HashMap<>();
        params.put("state", state.toString().toLowerCase());
        if(assignee != null) {
            params.put("assignee", assignee);
        }

        if(labels != null) {
            if(labels.size() != 0) {
                final StringBuilder builder = new StringBuilder();
                for(String s : labels) {
                    builder.append(s);
                    builder.append(",");
                }
                builder.setLength(builder.length() - 1);
                params.put("labels", builder.toString());
            }
        }
        Log.i(TAG, "loadIssues: Params: " + params.toString());
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_ISSUES + (page > 1 ? "?page=" + page : ""))
                .addHeaders(API_AUTH_HEADERS)
                .addQueryParameter(params)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final ArrayList<Issue> issues = new ArrayList<>();
                        Log.i(TAG, "onResponse: Returned " + response.length() + " issues");
                        for(int i = 0; i < response.length() ; i++) {
                            try {
                                if(!response.getJSONObject(i).has("pull_request")) {
                                    issues.add(Issue.parse(response.getJSONObject(i)));
                                }
                            } catch(JSONException jse) {
                                Log.e(TAG, "onResponse: Parsing open issues", jse);
                            }
                        }
                        if(loader != null) loader.issuesLoaded(issues.toArray(new Issue[issues.size()]));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Issue load: " + anError.getErrorBody());
                        if(loader != null) loader.issuesLoadError(parseError(anError));
                    }
                });
    }

    public void loadComments(CommentsLoader loader, String fullRepoName, int issueNumber) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_ISSUES + "/" + issueNumber + SEGMENT_COMMENTS)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final Comment[] comments = new Comment[response.length()];
                        for(int i = 0; i < response.length(); i++) {
                            try {
                                comments[i] = Comment.parse(response.getJSONObject(i));
                            } catch(JSONException jse) {
                                Log.e(TAG, "onResponse: ", jse);
                            }
                        }
                        if(loader != null) loader.commentsLoaded(comments);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(loader != null) loader.commentsLoadError(parseError(anError));
                    }
                });
    }

    public void loadEvents(EventsLoader loader, String repoFullName, int issueNumber) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_ISSUES + "/" + issueNumber  + SEGMENT_EVENTS)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.i(TAG, "onResponse: " + response.toString());
                        final Event[] events = new Event[response.length()];
                        for(int i = 0; i < response.length(); i++) {
                            try {
                                events[i] = Event.parse(response.getJSONObject(i));
                            } catch(JSONException jse) {
                                Log.e(TAG, "onResponse: Events: ", jse);
                            }
                        }
                        Arrays.sort(events, (e1, e2) -> e1.getCreatedAt() > e2.getCreatedAt() ? 1 : 0);
                        if(loader != null) loader.eventsLoaded(events);
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Events: " + anError.getErrorDetail());
                        if(loader != null) loader.eventsLoadError(parseError(anError));
                    }
                });

    }

    public void loadEvents(EventsLoader loader, String login) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_USERS + "/" + login + SEGMENT_EVENTS)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final Event[] events = new Event[response.length()];
                        for(int i = 0; i < response.length(); i++) {
                            try {
                                events[i] = Event.parse(response.getJSONObject(i));
                            } catch(JSONException jse) {
                                Log.e(TAG, "onResponse: User Events: ", jse);
                            }
                        }
                        if(loader != null) loader.eventsLoaded(events);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(loader != null) loader.eventsLoadError(parseError(anError));
                    }
                });
    }

    public void checkAccessToRepository(AccessCheckListener listener, String login, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_COLLABORATORS + "/" + login + SEGMENT_PERMISSION)
                .addHeaders(ORGANIZATIONS_PREVIEW_ACCEPT_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String permission = PERMISSION_NONE;
                        if(response.has(PERMISSION)) {
                            try {
                                permission = response.getString(PERMISSION);
                            } catch(JSONException ignored) {}
                        }
                        if(listener != null) {
                            switch(permission) {
                                case PERMISSION_ADMIN:
                                    listener.accessCheckComplete(Repository.AccessLevel.ADMIN);
                                    break;
                                case PERMISSION_WRITE:
                                    listener.accessCheckComplete(Repository.AccessLevel.WRITE);
                                    break;
                                case PERMISSION_READ:
                                     listener.accessCheckComplete(Repository.AccessLevel.READ);
                                    break;
                                case PERMISSION_NONE:
                                    listener.accessCheckComplete(Repository.AccessLevel.NONE);
                                    break;
                            }
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Access check: " + anError.getErrorCode() + " " + anError.getErrorBody());
                        if(listener != null) {
                            if(anError.getErrorCode() == 403) {
                                //403 Must have push access to view collaborator permission
                                listener.accessCheckComplete(Repository.AccessLevel.NONE);
                            } else {
                                listener.accessCheckError(parseError(anError));
                            }
                        }
                    }
                });
    }

    public void checkIfCollaborator(AccessCheckListener listener, String login, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_COLLABORATORS + "/"  + login)
                .addHeaders(API_AUTH_HEADERS)
                .setPriority(Priority.IMMEDIATE)
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        if(response.code() == 204) {
                            if(listener != null) listener.accessCheckComplete(Repository.AccessLevel.ADMIN);
                        } else if(response.code() == 404) {
                            if(listener != null) listener.accessCheckComplete(Repository.AccessLevel.NONE);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) {
                            listener.accessCheckError(parseError(anError));
                        }
                    }
                });
    }

    public void checkIfStarred(StarCheckListener listener, String fullRepoName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_USER + SEGMENT_STARRED + "/" + fullRepoName)
                .addHeaders(API_AUTH_HEADERS)
                .setPriority(Priority.IMMEDIATE)
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        Log.i(TAG, "onResponse: Check if starred: "+ response.toString());
                        if(response.code() == 204) {
                            if(listener != null) listener.starCheckComplete(true);
                        } else if(response.code() == 404) {
                            if(listener != null) listener.starCheckComplete(false);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.starCheckComplete(false);
                    }
                });
    }

    public void checkIfWatched(WatchCheckListener listener, String fullRepoName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_SUBSCRIPTION)
                .addHeaders(API_AUTH_HEADERS)
                .setPriority(Priority.IMMEDIATE)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: Subscription check " + response.toString());
                        try {
                            if(response.has("subscribed")) {
                                if(listener != null) listener.watchCheckComplete(response.getBoolean("subscribed"));
                            } else {
                                if(listener != null) listener.watchCheckComplete(false);
                            }
                        } catch(JSONException jse) {}
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.watchCheckComplete(false);
                    }
                });
    }

    public void renderMarkDown(MarkDownRenderLoader loader, String markdown) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put("text", markdown);
        } catch(JSONException ignored) {}
        AndroidNetworking.post(GIT_BASE + SEGMENT_MARKDOWN)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        Log.i(TAG, "onResponse: Markdown: " + response);
                        if(loader != null) loader.rendered(response);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(loader != null) loader.renderError(parseError(anError));
                    }
                });
    }

    public void renderMarkDown(MarkDownRenderLoader loader, String markdown, String context) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put("text", markdown);
            obj.put("mode", "gfm");
            obj.put("context", context);
        } catch(JSONException ignored) {}
        AndroidNetworking.post(GIT_BASE + SEGMENT_MARKDOWN)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        Log.i(TAG, "onResponse: Markdown: " + response);
                        if(loader != null) loader.rendered(response);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(loader != null) loader.renderError(parseError(anError));
                    }
                });
    }

    public interface UserLoader {

        void userLoaded(User user);

        void userLoadError(APIError error);

    }

    public interface AuthenticatedUserLoader {

        void userLoaded(User user);

        void authenticatedUserLoadError(APIError error);

    }

    public interface RepositoriesLoader {

        void repositoriesLoaded(Repository[] repos);

        void repositoryLoadError(APIError error);

    }

    public interface RepositoryLoader {

        void repoLoaded(Repository repo);

        void repoLoadError(APIError error);
    }

    public interface ReadMeLoader {

        void readMeLoaded(String readMe);

        void readmeLoadError(APIError error);

    }

    public interface ProjectLoader {

        void projectLoaded(Project project);

        void projectLoadError(APIError error);
    }

    public interface ProjectsLoader {

        void projectsLoaded(Project[] projects);

        void projectsLoadError(APIError error);

    }

    public interface ColumnsLoader {

        void columnsLoaded(Column[] columns);

        void columnsLoadError(APIError error);

    }

    public interface CardsLoader {

        void cardsLoaded(Card[] cards);

        void cardsLoadError(APIError error);

    }

    public interface CollaboratorsLoader {

        void collaboratorsLoaded(User[] collaborators);

        void collaboratorsLoadError(APIError error);

    }

    public interface LabelsLoader {

        void labelsLoaded(Label[] labels);

        void labelLoadError(APIError error);

    }

    public interface IssueLoader {

        void issueLoaded(Issue issue);

        void issueLoadError(APIError error);

    }

    public interface IssuesLoader {

        void issuesLoaded(Issue[] issues);

        void issuesLoadError(APIError error);

    }

    public interface CommentsLoader {

        void commentsLoaded(Comment[] comments);

        void commentsLoadError(APIError error);

    }

    public interface EventsLoader {

        void eventsLoaded(Event[] events);

        void eventsLoadError(APIError error);

    }

    public interface MarkDownRenderLoader {

        void rendered(String html);

        void renderError(APIError error);

    }

    public interface StarCheckListener {

        void starCheckComplete(boolean isStarred);

    }

    public interface WatchCheckListener {

        void watchCheckComplete(boolean isWatching);

    }

    public interface AccessCheckListener {

        void accessCheckComplete(Repository.AccessLevel accessLevel);

        void accessCheckError(APIError error);

    }


}
