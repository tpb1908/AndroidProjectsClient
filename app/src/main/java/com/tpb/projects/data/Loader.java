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
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Column;
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

/**
 * Created by theo on 14/12/16.
 *
 */

public class Loader extends APIHandler {
    private static final String TAG = Loader.class.getSimpleName();

    public Loader(Context context) {
        super(context);
    }

    public void loadAuthenticateUser(AuthenticatedUserLoader loader) {
        AndroidNetworking.get(GIT_BASE + "user")
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
                        if(loader != null) loader.authenticatedUserLoadError();
                    }
                });
    }

    public void loadRepositories(RepositoriesLoader loader, String user) {
        AndroidNetworking.get(GIT_BASE + "users/" + user + "/repos")
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
                    }
                });
    }

    public void loadRepositories(RepositoriesLoader loader) {
        AndroidNetworking.get(GIT_BASE + "user/repos")
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
                    }
                });
    }

    public void loadRepository(RepositoryLoader loader, String fullRepoName) {
        AndroidNetworking.get(GIT_BASE + "repos/" + fullRepoName)
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
                    }
                });
    }

    public void loadReadMe(ReadMeLoader loader, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + "repos/" + repoFullName + "/readme")
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            final String readme = Data.base64Decode(response.getString("content"));
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
                    }
                });
    }

    public void loadCollaborators(final CollaboratorsLoader loader, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + "repos/" + repoFullName + "/collaborators")
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
                    }
                });
    }

    public void loadLabels(LabelsLoader loader, String fullRepoName) {
        AndroidNetworking.get(GIT_BASE + "repos/" + fullRepoName + "/labels")
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
                        if(loader != null) loader.labelLoadError();
                    }
                });
    }

    public void loadProject(ProjectLoader loader, int id) {
        AndroidNetworking.get(GIT_BASE + "projects/" + Integer.toString(id))
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(loader != null) loader.projectLoaded(Project.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(loader != null) loader.projectLoadError();
                    }
                });
    }

    public void loadProjects(ProjectsLoader loader, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + "repos/" + repoFullName + "/projects")
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
                    }
                });
    }

    public void loadColumns(ColumnsLoader loader, int projectId) {
        AndroidNetworking.get(GIT_BASE + "projects/" + Integer.toString(projectId) + "/columns")
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
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
                        loader.columnsLoadError();
                    }
                });
    }

    public void loadCards(CardsLoader loader, int columnId) {
        AndroidNetworking.get(GIT_BASE + "projects/columns/" + Integer.toString(columnId) + "/cards")
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
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
                        if(loader != null) loader.cardsLoadError();
                    }
                });
    }

    public void loadIssue(IssueLoader loader, String fullRepoName, int issueNumber) {
        AndroidNetworking.get(GIT_BASE + "repos/" + fullRepoName + "/issues/" + Integer.toString(issueNumber))
                .addHeaders(PROJECTS_PREVIEW_API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(loader != null) loader.issueLoaded(Issue.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Issue: " + anError.getErrorBody());
                        if(loader != null) loader.issueLoadError();
                    }
                });
    }

    public void loadOpenIssues(IssuesLoader loader, String fullRepoName) {
        AndroidNetworking.get(GIT_BASE + "repos/" + fullRepoName + "/issues")
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
                        if(loader != null) loader.issuesLoadError();
                    }
                });

    }

    public void loadUser(UserLoader loader, String username) {
        AndroidNetworking.get(GIT_BASE + "users/" + username)
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
                        if(loader != null) loader.userLoadError();
                    }
                });

    }

    public void checkAccess(AccessCheckListener listener, String login, String repoFullname) {
        AndroidNetworking.get(GIT_BASE + "repos/" + repoFullname + "/collaborators/" + login + "/permission")
                .addHeaders(ORGANIZATIONS_PREVIEW_ACCEPT_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String permission = "none";
                        if(response.has("permission")) {
                            try {
                                permission = response.getString("permission");
                            } catch(JSONException ignored) {}
                        }
                        if(listener != null) {
                            switch(permission) {
                                case "admin":
                                    listener.accessCheckComplete(Repository.AccessLevel.ADMIN);
                                    break;
                                case "write":
                                    listener.accessCheckComplete(Repository.AccessLevel.WRITE);
                                    break;
                                case "read":
                                     listener.accessCheckComplete(Repository.AccessLevel.READ);
                                    break;
                                case "none":
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
                                listener.accessCheckError();
                            }
                        }
                    }
                });
    }

    public interface UserLoader {

        void userLoaded(User user);

        void userLoadError();

    }

    public interface AuthenticatedUserLoader {

        void userLoaded(User user);

        void authenticatedUserLoadError();

    }

    public interface RepositoriesLoader {

        void repositoriesLoaded(Repository[] repos);

        void repositoryLoadError();

    }

    public interface RepositoryLoader {

        void repoLoaded(Repository repo);

        void repoLoadError();
    }

    public interface ReadMeLoader {

        void readMeLoaded(String readMe);

        void readmeLoadError();

    }

    public interface ProjectLoader {

        void projectLoaded(Project project);

        void projectLoadError();
    }

    public interface ProjectsLoader {

        void projectsLoaded(Project[] projects);

    }

    public interface ColumnsLoader {

        void columnsLoaded(Column[] columns);

        void columnsLoadError();

    }

    public interface CardsLoader {

        void cardsLoaded(Card[] cards);

        void cardsLoadError();

    }

    public interface CollaboratorsLoader {

        void collaboratorsLoaded(User[] collaborators);

        void collaboratorsLoadError();

    }

    public interface LabelsLoader {

        void labelsLoaded(Label[] labels);

        void labelLoadError();

    }

    public interface IssueLoader {

        void issueLoaded(Issue issue);

        void issueLoadError();

    }

    public interface IssuesLoader {

        void issuesLoaded(Issue[] issues);

        void issuesLoadError();

    }

    public interface AccessCheckListener {

        void accessCheckComplete(Repository.AccessLevel accessLevel);

        void accessCheckError();

    }

    public enum LoadError {
        NOT_FOUND, AUTHENTICATION_FAILED, UNKNOWN
    }

}
