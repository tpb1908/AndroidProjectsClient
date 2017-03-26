package com.tpb.projects.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
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
import com.tpb.projects.data.models.Gist;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.data.models.Milestone;
import com.tpb.projects.data.models.Project;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.data.models.State;
import com.tpb.projects.data.models.User;
import com.tpb.projects.util.Util;

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

    public void loadAuthenticatedUser(@Nullable GITModelLoader<User> loader) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_USER)
                .addHeaders(API_AUTH_HEADERS)
                .setPriority(Priority.IMMEDIATE)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: User loaded " + response.toString());
                        if(loader != null) loader.loadComplete(User.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Authenticated user error " + anError.getErrorBody());
                        if(loader != null) loader.loadError(parseError(anError));
                    }
                });
    }

    public void loadUser(@Nullable GITModelLoader<User> loader, String username) {
        final ANRequest req = AndroidNetworking.get(GIT_BASE + SEGMENT_USERS + "/" + username)
                .addHeaders(API_AUTH_HEADERS)
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONObject(new JSONObjectRequestListener() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.i(TAG, "onResponse: User loaded");
                    loader.loadComplete(User.parse(response));
                }

                @Override
                public void onError(ANError anError) {
                    Log.i(TAG, "onError: User not loaded: " + anError.getErrorBody());
                    loader.loadError(parseError(anError));
                }
            });
        }

    }

    public void loadRepositories(@Nullable GITModelsLoader<Repository> loader, String user, int page) {
        final ANRequest req = AndroidNetworking.get(GIT_BASE + SEGMENT_USERS + "/" + user + SEGMENT_REPOS + "?sort=updated" + (page > 1 ? "?page=" + page : "" ))
                .addHeaders(LICENSES_API_API_AUTH_HEADERS)
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray jsa) {
                    try {
                        final Repository[] repos = new Repository[jsa.length()];
                        for(int i = 0; i < repos.length; i++) {
                            repos[i] = Repository.parse(jsa.getJSONObject(i));
                        }
                        Log.i(TAG, "onResponse: successfully parsed repos");
                        loader.loadComplete(repos);
                    } catch(JSONException jse) {
                        Log.i(TAG, "onResponse: " + jsa.toString());
                        Log.e(TAG, "onResponse: ", jse);
                    }
                }

                @Override
                public void onError(ANError anError) {
                    Log.i(TAG, "onError: " + anError.getErrorBody());
                    loader.loadError(parseError(anError));
                }
            });
        }
    }

    public void loadRepositories(@Nullable GITModelsLoader<Repository> loader, int page) {
        final ANRequest req = AndroidNetworking.get(GIT_BASE + SEGMENT_USER + SEGMENT_REPOS + "?sort=updated" + (page > 1 ? "?page=" + page : ""))
                .addHeaders(LICENSES_API_API_AUTH_HEADERS)
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray jsa) {
                    try {
                        final Repository[] repos = new Repository[jsa.length()];
                        for(int i = 0; i < repos.length; i++) {
                            repos[i] = Repository.parse(jsa.getJSONObject(i));
                        }
                        loader.loadComplete(repos);
                    } catch(JSONException jse) {
                        Log.i(TAG, "onResponse: " + jsa.toString());
                        Log.e(TAG, "onResponse: ", jse);
                    }
                }

                @Override
                public void onError(ANError anError) {
                    Log.i(TAG, "onError: User repos" + anError.getErrorBody());
                    loader.loadError(parseError(anError));
                }
            });
        }
    }

    public void loadStarredRepositories(@Nullable GITModelsLoader<Repository> loader, String user,  int page) {
        final ANRequest req = AndroidNetworking.get(GIT_BASE + SEGMENT_USERS + "/" + user + SEGMENT_STARRED + (page > 1 ? "?page=" + page : ""))
                .addHeaders(LICENSES_API_API_AUTH_HEADERS)
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray jsa) {
                    try {
                        final Repository[] repos = new Repository[jsa.length()];
                        for(int i = 0; i < repos.length; i++) {
                            repos[i] = Repository.parse(jsa.getJSONObject(i));
                        }
                        loader.loadComplete(repos);
                    } catch(JSONException jse) {
                        Log.i(TAG, "onResponse: " + jsa.toString());
                        Log.e(TAG, "onResponse: ", jse);
                    }
                }

                @Override
                public void onError(ANError anError) {
                    Log.i(TAG, "onError: User repos" + anError.getErrorBody());
                    loader.loadError(parseError(anError));
                }
            });
        }
    }

    public void loadRepository(@Nullable GITModelLoader<Repository> loader, String repoFullName) {
        final ANRequest req = AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName)
                .addHeaders(LICENSES_API_API_AUTH_HEADERS)
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONObject(new JSONObjectRequestListener() {
                @Override
                public void onResponse(JSONObject response) {
                    final Repository repo = Repository.parse(response);
                    loader.loadComplete(repo);
                }

                @Override
                public void onError(ANError anError) {
                    Log.i(TAG, "onError: load Repo: " + anError.getErrorBody());
                    loader.loadError(parseError(anError));
                }
            });
        }
    }

    public void loadGists(GITModelsLoader<Gist> loader, int page) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_GISTS + (page > 1 ? "?page=" + page : ""))
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final Gist[] gists = new Gist[response.length()];
                        for(int i = 0; i < response.length(); i++) {
                            try {
                                gists[i] = Gist.parse(response.getJSONObject(i));
                            } catch(JSONException ignored) {}
                        }
                        if(loader != null) loader.loadComplete(gists);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(loader != null) loader.loadError(parseError(anError));
                    }
                });

    }

    public void loadGists(GITModelsLoader<Gist> loader, String user, int page) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_USERS + "/" + user + SEGMENT_GISTS + (page > 1 ? "?page=" + page : ""))
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final Gist[] gists = new Gist[response.length()];
                        for(int i = 0; i < response.length(); i++) {
                            try {
                                gists[i] = Gist.parse(response.getJSONObject(i));
                            } catch(JSONException ignored) {}
                        }
                        if(loader != null) loader.loadComplete(gists);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(loader != null) loader.loadError(parseError(anError));
                    }
                });

    }

    public void loadReadMe(@Nullable GITModelLoader<String> loader, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_README)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            final String readme = Util.base64Decode(response.getString(CONTENT));
                            if(loader != null) loader.loadComplete(readme);
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: ", jse);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(loader != null) loader.loadError(parseError(anError));
                    }
                });
    }

    public void loadCollaborators(@Nullable GITModelsLoader<User> loader, String repoFullName) {
        final ANRequest req = AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_COLLABORATORS)
                .addHeaders(ORGANIZATIONS_API_AUTH_HEADERS)
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
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
                    loader.loadComplete(collabs);
                }

                @Override
                public void onError(ANError anError) {
                    loader.loadError(parseError(anError));
                }
            });
        }
    }

    public void loadLabels(@Nullable GITModelsLoader<Label> loader, String repoFullName) {
        final ANRequest req =
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_LABELS)
                .addHeaders(API_AUTH_HEADERS)
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
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
                    loader.loadComplete(labels);
                }

                @Override
                public void onError(ANError anError) {
                    Log.i(TAG, "onError: Labels: " + anError.getErrorBody());
                    loader.loadError(parseError(anError));
                }
            });
        }
    }

    public void loadProject(@Nullable GITModelLoader<Project> loader, int id) {
        final ANRequest req = AndroidNetworking.get(GIT_BASE + SEGMENT_PROJECTS + "/" + id)
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONObject(new JSONObjectRequestListener() {
                @Override
                public void onResponse(JSONObject response) {
                    loader.loadComplete(Project.parse(response));
                }

                @Override
                public void onError(ANError anError) {
                    loader.loadError(parseError(anError));
                }
            });
        }
    }

    public void loadProjects(@Nullable GITModelsLoader<Project> loader, String repoFullName) {
        final ANRequest req =
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_PROJECTS)
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .addQueryParameter("state", "all")
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    try {
                        final Project[] projects = new Project[response.length()];
                        for(int i = 0; i < response.length(); i++) {
                            projects[i] = Project.parse(response.getJSONObject(i));
                        }
                        Arrays.sort(projects, (p1, p2) -> {
                            if(p1.getState() == State.OPEN && p2.getState() != State.OPEN) return -1;
                            if(p2.getState() == State.OPEN && p1.getState() != State.OPEN) return 1;
                            return p1.getUpdatedAt() > p2.getUpdatedAt() ? -1 : 1;
                        });
                        loader.loadComplete(projects);
                    } catch(JSONException jse) {
                        Log.e(TAG, "onResponse: ", jse);
                    }
                }

                @Override
                public void onError(ANError anError) {
                    loader.loadError(parseError(anError));
                }
            });
        }
    }

    public void loadColumns(@Nullable GITModelsLoader<Column> loader, int projectId) {
        final ANRequest req = AndroidNetworking.get(GIT_BASE + SEGMENT_PROJECTS + "/" + projectId + SEGMENT_COLUMNS)
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .getResponseOnlyFromNetwork()
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    final Column[] columns = new Column[response.length()];
                    for(int i = 0; i < columns.length; i++) {
                        try {
                            columns[i] = Column.parse(response.getJSONObject(i));
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: ", jse);
                        }
                    }
                    loader.loadComplete(columns);
                }

                @Override
                public void onError(ANError anError) {
                    loader.loadError(parseError(anError));
                }
            });
        }
    }

    public void loadCards(@Nullable GITModelsLoader<Card> loader, int columnId, int page) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + columnId + SEGMENT_CARDS + (page > 1 ? "?page=" + page : ""))
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .getResponseOnlyFromNetwork()
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final Card[] cards = new Card[response.length()];
                        for(int i = 0; i < cards.length; i++) {
                            try {
                                cards[i] = Card.parse(response.getJSONObject(i));
                            } catch(JSONException jse) {
                                Log.e(TAG, "onResponse: ", jse);
                            }
                        }
                        if(loader != null) loader.loadComplete(cards);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(loader != null) loader.loadError(parseError(anError));
                    }
                });
    }

    public void loadIssue(@Nullable GITModelLoader<Issue> loader, String repoFullName, int issueNumber, boolean highPriority) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_ISSUES + "/" + issueNumber)
                .addHeaders(API_AUTH_HEADERS)
                .setPriority(highPriority ? Priority.HIGH : Priority.MEDIUM)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(loader != null) loader.loadComplete(Issue.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Issue: " + anError.getErrorBody());
                        if(loader != null) loader.loadError(parseError(anError));
                    }
                });
    }

    public void loadOpenIssues(@Nullable GITModelsLoader<Issue> loader, String repoFullName) {
        final ANRequest req =
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_ISSUES)
                .addHeaders(API_AUTH_HEADERS)
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    final ArrayList<Issue> issues = new ArrayList<>();
                    for(int i = 0; i < response.length(); i++) {
                        try {
                            final Issue is = Issue.parse(response.getJSONObject(i));
                            if(!is.isClosed()) issues.add(is);
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: Parsing open issues", jse);
                        }
                    }
                    loader.loadComplete(issues.toArray(new Issue[0]));
                }

                @Override
                public void onError(ANError anError) {
                    Log.i(TAG, "onError: Issue load: " + anError.getErrorBody());
                    loader.loadError(parseError(anError));
                }
            });
        }

    }

    public void loadIssues(@Nullable GITModelsLoader<Issue> loader, String repoFullName, State state, @Nullable String assignee, @Nullable List<String> labels, int page) {
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
        final ANRequest req =
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_ISSUES + (page > 1 ? "?page=" + page : ""))
                .addHeaders(API_AUTH_HEADERS)
                .addQueryParameter(params)
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    final ArrayList<Issue> issues = new ArrayList<>();
                    for(int i = 0; i < response.length(); i++) {
                        try {
                            if(!response.getJSONObject(i).has("pull_request")) {
                                issues.add(Issue.parse(response.getJSONObject(i)));
                            }
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: Parsing open issues", jse);
                        }
                    }
                    loader.loadComplete(issues.toArray(new Issue[issues.size()]));
                }

                @Override
                public void onError(ANError anError) {
                    loader.loadError(parseError(anError));
                }
            });
        }
    }

    public void loadComments(@Nullable GITModelsLoader<Comment> loader, String fullRepoName, int issueNumber, int page) {
        final ANRequest req = AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_ISSUES + "/" + issueNumber + SEGMENT_COMMENTS + (page > 1 ? "?page=" + page : ""))
                .addHeaders(API_AUTH_HEADERS)
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
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
                    loader.loadComplete(comments);
                }

                @Override
                public void onError(ANError anError) {
                    loader.loadError(parseError(anError));
                }
            });
        }
    }

    public void loadEvents(@Nullable GITModelsLoader<Event> loader, String repoFullName, int issueNumber, int page) {
        final ANRequest req = AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_ISSUES + "/" + issueNumber + SEGMENT_EVENTS + (page > 1 ? "?page=" + page : ""))
                .addHeaders(API_AUTH_HEADERS)
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    final Event[] events = new Event[response.length()];
                    for(int i = 0; i < response.length(); i++) {
                        try {
                            events[i] = Event.parse(response.getJSONObject(i));
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: Events: ", jse);
                        }
                    }
                    Arrays.sort(events, (e1, e2) -> e1.getCreatedAt() > e2.getCreatedAt() ? 1 : 0);
                    loader.loadComplete(events);
                }

                @Override
                public void onError(ANError anError) {
                    loader.loadError(parseError(anError));
                }
            });
        }

    }
    
    public void loadMilestone(@Nullable GITModelLoader<Milestone> loader, String repoFullName, int number) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_MILESTONES + "/" + number)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(loader != null) loader.loadComplete(Milestone.parse(response));
                        
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(loader != null) loader.loadError(parseError(anError));
                        
                    }
                });
    }

    public void loadMilestones(@Nullable GITModelsLoader<Milestone> loader, String repoFullName, State state, int page) {
        final ANRequest req =
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_MILESTONES + (page > 1 ? "?page=" + page : ""))
                .addHeaders(API_AUTH_HEADERS)
                .addPathParameter("state", state.toString().toLowerCase())
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    final Milestone[] milestones = new Milestone[response.length()];
                    for(int i = 0; i < response.length(); i++) {
                        try {
                            milestones[i] = Milestone.parse(response.getJSONObject(i));
                        } catch(JSONException ignored) {}
                    }
                    loader.loadComplete(milestones);
                }

                @Override
                public void onError(ANError anError) {
                    loader.loadError(parseError(anError));
                }
            });
        }
    }

    public void loadFollowers(GITModelsLoader<User> loader, String user, int page) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_USERS + "/" + user + SEGMENT_FOLLOWERS + (page > 1 ? "?page=" + page : ""))
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final User[] users = new User[response.length()];
                        for(int i = 0; i < users.length; i++) {
                            try {
                                users[i] = User.parse(response.getJSONObject(i));
                            } catch(JSONException ignored) {
                            }
                        }
                        loader.loadComplete(users);
                    }

                    @Override
                    public void onError(ANError anError) {
                        loader.loadError(parseError(anError));
                    }
                });
    }

    public void loadFollowing(GITModelsLoader<User> loader, String user, int page) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_USERS + "/" + user + SEGMENT_FOLLOWING + (page > 1 ? "?page=" + page : ""))
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final User[] users = new User[response.length()];
                        for(int i = 0; i < users.length; i++) {
                            try {
                                users[i] = User.parse(response.getJSONObject(i));
                            } catch(JSONException ignored) {
                            }
                        }
                        loader.loadComplete(users);
                    }

                    @Override
                    public void onError(ANError anError) {
                        loader.loadError(parseError(anError));
                    }
                });
    }

    public void checkAccessToRepository(@Nullable GITModelLoader<Repository.AccessLevel> listener, String login, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_COLLABORATORS + "/" + login + SEGMENT_PERMISSION)
                .addHeaders(ORGANIZATIONS_API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String permission = PERMISSION_NONE;
                        if(response.has(PERMISSION)) {
                            try {
                                permission = response.getString(PERMISSION);
                            } catch(JSONException ignored) {
                            }
                        }
                        if(listener != null) {
                            switch(permission) {
                                case PERMISSION_ADMIN:
                                    listener.loadComplete(Repository.AccessLevel.ADMIN);
                                    break;
                                case PERMISSION_WRITE:
                                    listener.loadComplete(Repository.AccessLevel.WRITE);
                                    break;
                                case PERMISSION_READ:
                                    listener.loadComplete(Repository.AccessLevel.READ);
                                    break;
                                case PERMISSION_NONE:
                                    listener.loadComplete(Repository.AccessLevel.NONE);
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
                                listener.loadComplete(Repository.AccessLevel.NONE);
                            } else {
                                listener.loadError(parseError(anError));
                            }
                        }
                    }
                });
    }

    public void checkIfCollaborator(@Nullable GITModelLoader<Repository.AccessLevel> listener, String login, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_COLLABORATORS + "/" + login)
                .addHeaders(ORGANIZATIONS_API_AUTH_HEADERS)
                .setPriority(Priority.IMMEDIATE)
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        if(response.code() == 204) {
                            if(listener != null)
                                listener.loadComplete(Repository.AccessLevel.ADMIN);
                        } else if(response.code() == 404) {
                            if(listener != null)
                                listener.loadComplete(Repository.AccessLevel.NONE);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) {
                            listener.loadError(parseError(anError));
                        }
                    }
                });
    }

    public void checkIfStarred(@Nullable GITModelLoader<Boolean> listener, String fullRepoName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_USER + SEGMENT_STARRED + "/" + fullRepoName)
                .addHeaders(API_AUTH_HEADERS)
                .setPriority(Priority.IMMEDIATE)
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        Log.i(TAG, "onResponse: Check if starred: " + response.toString());
                        if(response.code() == 204) {
                            if(listener != null) listener.loadComplete(true);
                        } else if(response.code() == 404) {
                            if(listener != null) listener.loadComplete(false);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.loadComplete(false);
                    }
                });
    }

    public void checkIfWatched(@Nullable GITModelLoader<Boolean> listener, String fullRepoName) {
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
                                if(listener != null)
                                    listener.loadComplete(response.getBoolean("subscribed"));
                            } else {
                                if(listener != null) listener.loadComplete(false);
                            }
                        } catch(JSONException jse) {
                            listener.loadComplete(false);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.loadComplete(false);
                    }
                });
    }

    public void renderMarkDown(@Nullable GITModelLoader<String> loader, String markdown) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put("text", markdown);
        } catch(JSONException ignored) {
        }
        AndroidNetworking.post(GIT_BASE + SEGMENT_MARKDOWN)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        Log.i(TAG, "onResponse: Markdown: " + response);
                        if(loader != null) loader.loadComplete(response);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(loader != null) loader.loadError(parseError(anError));
                    }
                });
    }

    public void renderMarkDown(@Nullable GITModelLoader<String> loader, String markdown, String context) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put("text", markdown);
            obj.put("mode", "gfm");
            obj.put("context", context);
        } catch(JSONException ignored) {
        }
        AndroidNetworking.post(GIT_BASE + SEGMENT_MARKDOWN)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        if(loader != null) loader.loadComplete(response);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(loader != null) loader.loadError(parseError(anError));
                    }
                });
    }

    public void loadLicenseBody(@NonNull GITModelLoader<String> loader, @NonNull String path) {
        AndroidNetworking.get(path)
                .addHeaders(LICENSES_API_API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(response.has("body")) {
                            try {
                                loader.loadComplete(response.getString("body"));
                            } catch(JSONException jse) {
                                loader.loadError(APIError.NOT_FOUND);
                            }
                        } else {
                            loader.loadError(APIError.NOT_FOUND);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        loader.loadError(parseError(anError));
                    }
                });

    }

    public interface GITModelsLoader<T> {

        void loadComplete(T[] data);

        void loadError(APIError error);

    }

    public interface GITModelLoader<T> {

        void loadComplete(T data);

        void loadError(APIError error);

    }

}
