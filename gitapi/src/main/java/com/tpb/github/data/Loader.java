package com.tpb.github.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.OkHttpResponseListener;
import com.androidnetworking.interfaces.StringRequestListener;
import com.tpb.github.data.models.Card;
import com.tpb.github.data.models.Column;
import com.tpb.github.data.models.Comment;
import com.tpb.github.data.models.Commit;
import com.tpb.github.data.models.CompleteStatus;
import com.tpb.github.data.models.Event;
import com.tpb.github.data.models.Gist;
import com.tpb.github.data.models.Issue;
import com.tpb.github.data.models.Label;
import com.tpb.github.data.models.Milestone;
import com.tpb.github.data.models.Project;
import com.tpb.github.data.models.Repository;
import com.tpb.github.data.models.State;
import com.tpb.github.data.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    private static final String SEGMENT_BRANCHES = "/branches";
    private static final String SEGMENT_CONTRIBUTORS = "/contributors";
    private static final String SEGMENT_STATUS = "/status";

    public Loader(Context context) {
        super(context);
    }

    public void loadAuthenticatedUser(@Nullable final ItemLoader<User> loader) {
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
                                 Log.i(TAG, "onError: Authenticated user error " + anError
                                         .getErrorBody());
                                 if(loader != null) loader.loadError(parseError(anError));
                             }
                         });
    }

    public void loadUser(@Nullable final ItemLoader<User> loader, String username) {
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

    public void loadRepositories(@Nullable final ListLoader<Repository> loader, String user, int page) {
        final ANRequest req = AndroidNetworking
                .get(GIT_BASE + SEGMENT_USERS + "/" + user + SEGMENT_REPOS + "?sort=updated" + appendPage(
                        page))
                .addHeaders(LICENSES_API_API_AUTH_HEADERS)
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    try {
                        final List<Repository> repos = new ArrayList<>(response.length());
                        for(int i = 0; i < response.length(); i++) {
                            repos.add(Repository.parse(response.getJSONObject(i)));
                        }
                        loader.listLoadComplete(repos);
                    } catch(JSONException jse) {
                        loader.listLoadError(APIError.UNPROCESSABLE);
                    }
                }

                @Override
                public void onError(ANError anError) {
                    Log.i(TAG, "onError: " + anError.getErrorBody());
                    loader.listLoadError(parseError(anError));
                }
            });
        }
    }

    public void loadRepositories(@Nullable final ListLoader<Repository> loader, int page) {
        final ANRequest req = AndroidNetworking
                .get(GIT_BASE + SEGMENT_USER + SEGMENT_REPOS + "?sort=updated" + appendPage(page))
                .addHeaders(LICENSES_API_API_AUTH_HEADERS)
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    try {
                        final List<Repository> repos = new ArrayList<>(response.length());
                        for(int i = 0; i < response.length(); i++) {
                            repos.add(Repository.parse(response.getJSONObject(i)));
                        }
                        loader.listLoadComplete(repos);
                    } catch(JSONException jse) {
                        Log.i(TAG, "onResponse: " + response.toString());
                        Log.e(TAG, "onResponse: ", jse);
                    }
                }

                @Override
                public void onError(ANError anError) {
                    Log.i(TAG, "onError: User repos" + anError.getErrorBody());
                    loader.listLoadError(parseError(anError));
                }
            });
        }
    }

    public void loadStarredRepositories(@Nullable final ListLoader<Repository> loader, String user, int page) {
        final ANRequest req = AndroidNetworking
                .get(GIT_BASE + SEGMENT_USERS + "/" + user + SEGMENT_STARRED + appendPage(page))
                .addHeaders(LICENSES_API_API_AUTH_HEADERS)
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    try {
                        final List<Repository> repos = new ArrayList<>(response.length());
                        for(int i = 0; i < response.length(); i++) {
                            repos.add(Repository.parse(response.getJSONObject(i)));
                        }
                        loader.listLoadComplete(repos);
                    } catch(JSONException jse) {
                        Log.i(TAG, "onResponse: " + response.toString());
                        Log.e(TAG, "onResponse: ", jse);
                    }
                }

                @Override
                public void onError(ANError anError) {
                    Log.i(TAG, "onError: User repos" + anError.getErrorBody());
                    loader.listLoadError(parseError(anError));
                }
            });
        }
    }

    public void loadRepository(@Nullable final ItemLoader<Repository> loader, String repoFullName) {
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

    public void loadBranches(@NonNull final ListLoader<android.support.v4.util.Pair<String, String>> loader, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_BRANCHES)
                         .addHeaders(API_AUTH_HEADERS)
                         .build()
                         .getAsJSONArray(new JSONArrayRequestListener() {
                             @Override
                             public void onResponse(JSONArray response) {
                                 final List<Pair<String, String>> branches = new ArrayList<>();
                                 try {
                                     for(int i = 0; i < response.length(); i++) {
                                         branches.add(Pair.create(
                                                 response.getJSONObject(i).getString("name"),
                                                 response.getJSONObject(i).getJSONObject("commit")
                                                         .getString("sha")
                                                 )
                                         );
                                     }
                                     loader.listLoadComplete(branches);
                                 } catch(JSONException jse) {
                                     loader.listLoadError(APIError.UNPROCESSABLE);
                                 }
                             }

                             @Override
                             public void onError(ANError anError) {
                                 loader.listLoadError(parseError(anError));
                             }
                         });
    }

    public void loadGists(@Nullable final ListLoader<Gist> loader, int page) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_GISTS + appendPage(page))
                         .addHeaders(API_AUTH_HEADERS)
                         .build()
                         .getAsJSONArray(new JSONArrayRequestListener() {
                             @Override
                             public void onResponse(JSONArray response) {
                                 final List<Gist> gists = new ArrayList<>(response.length());
                                 for(int i = 0; i < response.length(); i++) {
                                     try {
                                         gists.add(Gist.parse(response.getJSONObject(i)));
                                     } catch(JSONException ignored) {
                                     }
                                 }
                                 if(loader != null) loader.listLoadComplete(gists);
                             }

                             @Override
                             public void onError(ANError anError) {
                                 if(loader != null) loader.listLoadError(parseError(anError));
                             }
                         });

    }

    public void loadGists(@Nullable final ListLoader<Gist> loader, String user, int page) {
        AndroidNetworking
                .get(GIT_BASE + SEGMENT_USERS + "/" + user + SEGMENT_GISTS + appendPage(page))
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final List<Gist> gists = new ArrayList<>(response.length());
                        for(int i = 0; i < response.length(); i++) {
                            try {
                                gists.add(Gist.parse(response.getJSONObject(i)));
                            } catch(JSONException ignored) {
                            }
                        }
                        if(loader != null) loader.listLoadComplete(gists);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(loader != null) loader.listLoadError(parseError(anError));
                    }
                });

    }

    public void loadReadMe(@Nullable final ItemLoader<String> loader, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_README)
                         .addHeaders(API_AUTH_HEADERS)
                         .build()
                         .getAsJSONObject(new JSONObjectRequestListener() {
                             @Override
                             public void onResponse(JSONObject response) {
                                 try {
                                     final String readme = Util
                                             .base64Decode(response.getString(CONTENT));
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

    public void loadCollaborators(@Nullable final ListLoader<User> loader, String repoFullName) {
        final ANRequest req = AndroidNetworking
                .get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_COLLABORATORS)
                .addHeaders(ORGANIZATIONS_API_AUTH_HEADERS)
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    final List<User> collabs = new ArrayList<>(response.length());
                    for(int i = 0; i < response.length(); i++) {
                        try {
                            collabs.add(User.parse(response.getJSONObject(i)));
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: ", jse);
                        }
                    }
                    loader.listLoadComplete(collabs);
                }

                @Override
                public void onError(ANError anError) {
                    loader.listLoadError(parseError(anError));
                }
            });
        }
    }

    public void loadContributors(@Nullable final ListLoader<User> loader, String repoFullName) {
        final ANRequest req = AndroidNetworking
                .get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_CONTRIBUTORS)
                .addHeaders(API_AUTH_HEADERS)
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    final List<User> collabs = new ArrayList<>(response.length());
                    for(int i = 0; i < response.length(); i++) {
                        try {
                            collabs.add(User.parse(response.getJSONObject(i)));
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: ", jse);
                        }
                    }
                    loader.listLoadComplete(collabs);
                }

                @Override
                public void onError(ANError anError) {
                    loader.listLoadError(parseError(anError));
                }
            });
        }
    }

    public void loadLabels(@Nullable final ListLoader<Label> loader, String repoFullName) {
        final ANRequest req =
                AndroidNetworking
                        .get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_LABELS)
                        .addHeaders(API_AUTH_HEADERS)
                        .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    final List<Label> labels = new ArrayList<>(response.length());
                    for(int i = 0; i < labels.size(); i++) {
                        try {
                            labels.add(Label.parse(response.getJSONObject(i)));
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: Label parsing: ", jse);
                        }
                    }
                    loader.listLoadComplete(labels);
                }

                @Override
                public void onError(ANError anError) {
                    loader.listLoadError(parseError(anError));
                }
            });
        }
    }

    public void loadProject(@Nullable final ItemLoader<Project> loader, int id) {
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

    public void loadProjects(@Nullable final ListLoader<Project> loader, String repoFullName) {
        final ANRequest req =
                AndroidNetworking
                        .get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_PROJECTS)
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
                        final List<Project> projects = new ArrayList<>(response.length());
                        for(int i = 0; i < response.length(); i++) {
                            projects.add(Project.parse(response.getJSONObject(i)));
                        }
                        Collections.sort(projects, new Comparator<Project>() {
                            @Override
                            public int compare(Project p1, Project p2) {
                                if(p1.getState() == State.OPEN && p2.getState() != State.OPEN)
                                    return -1;
                                if(p2.getState() == State.OPEN && p1.getState() != State.OPEN)
                                    return 1;
                                return p1.getUpdatedAt() > p2.getUpdatedAt() ? -1 : 1;
                            }
                        });
                        loader.listLoadComplete(projects);
                    } catch(JSONException jse) {
                        Log.e(TAG, "onResponse: ", jse);
                    }
                }

                @Override
                public void onError(ANError anError) {
                    loader.listLoadError(parseError(anError));
                }
            });
        }
    }

    public void loadColumns(@Nullable final ListLoader<Column> loader, int projectId) {
        final ANRequest req = AndroidNetworking
                .get(GIT_BASE + SEGMENT_PROJECTS + "/" + projectId + SEGMENT_COLUMNS)
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .getResponseOnlyFromNetwork()
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    final List<Column> columns = new ArrayList<>(response.length());
                    for(int i = 0; i < response.length(); i++) {
                        try {
                            columns.add(Column.parse(response.getJSONObject(i)));
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: ", jse);
                        }
                    }
                    loader.listLoadComplete(columns);
                }

                @Override
                public void onError(ANError anError) {
                    loader.listLoadError(parseError(anError));
                }
            });
        }
    }

    public void loadCards(@Nullable final ListLoader<Card> loader, int columnId, int page) {
        AndroidNetworking
                .get(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + columnId + SEGMENT_CARDS + appendPage(
                        page))
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .getResponseOnlyFromNetwork()
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final List<Card> cards = new ArrayList<>(response.length());
                        for(int i = 0; i < response.length(); i++) {
                            try {
                                cards.add(Card.parse(response.getJSONObject(i)));
                            } catch(JSONException jse) {
                                Log.e(TAG, "onResponse: ", jse);
                            }
                        }
                        if(loader != null) loader.listLoadComplete(cards);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(loader != null) loader.listLoadError(parseError(anError));
                    }
                });
    }

    public void loadIssue(@Nullable final ItemLoader<Issue> loader, String repoFullName, int issueNumber, boolean highPriority) {
        AndroidNetworking
                .get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_ISSUES + "/" + issueNumber)
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

    public void loadOpenIssues(@Nullable final ListLoader<Issue> loader, String repoFullName) {
        final ANRequest req =
                AndroidNetworking
                        .get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_ISSUES)
                        .addHeaders(API_AUTH_HEADERS)
                        .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    final List<Issue> issues = new ArrayList<>();
                    for(int i = 0; i < response.length(); i++) {
                        try {
                            final Issue is = Issue.parse(response.getJSONObject(i));
                            if(!is.isClosed()) issues.add(is);
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: Parsing open issues", jse);
                        }
                    }
                    loader.listLoadComplete(issues);
                }

                @Override
                public void onError(ANError anError) {
                    Log.i(TAG, "onError: Issue load: " + anError.getErrorBody());
                    loader.listLoadError(parseError(anError));
                }
            });
        }

    }

    public void loadIssues(@Nullable final ListLoader<Issue> loader, String repoFullName, State state, @Nullable String assignee, @Nullable List<String> labels, int page) {
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
                AndroidNetworking
                        .get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_ISSUES + appendPage(
                                page))
                        .addHeaders(API_AUTH_HEADERS)
                        .addQueryParameter(params)
                        .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    final List<Issue> issues = new ArrayList<>();
                    for(int i = 0; i < response.length(); i++) {
                        try {
                            if(!response.getJSONObject(i).has("pull_request")) {
                                issues.add(Issue.parse(response.getJSONObject(i)));
                            }
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: Parsing open issues", jse);
                        }
                    }
                    loader.listLoadComplete(issues);
                }

                @Override
                public void onError(ANError anError) {
                    loader.listLoadError(parseError(anError));
                }
            });
        }
    }

    public void loadIssueComments(@Nullable final ListLoader<Comment> loader, String repoFullName, int issueNumber, int page) {
        final ANRequest req = AndroidNetworking
                .get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_ISSUES + "/" + issueNumber + SEGMENT_COMMENTS + appendPage(
                        page))
                .addHeaders(API_AUTH_HEADERS)
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    final List<Comment> comments = new ArrayList<>(response.length());
                    for(int i = 0; i < response.length(); i++) {
                        try {
                            comments.add(Comment.parse(response.getJSONObject(i)));
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: ", jse);
                        }
                    }
                    loader.listLoadComplete(comments);
                }

                @Override
                public void onError(ANError anError) {
                    loader.listLoadError(parseError(anError));
                }
            });
        }
    }

    public void loadEvents(@Nullable final ListLoader<Event> loader, String repoFullName, int issueNumber, int page) {
        final ANRequest req = AndroidNetworking
                .get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_ISSUES + "/" + issueNumber + SEGMENT_EVENTS + appendPage(
                        page))
                .addHeaders(API_AUTH_HEADERS)
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    final List<Event> events = new ArrayList<>(response.length());
                    for(int i = 0; i < response.length(); i++) {
                        try {
                            events.add(Event.parse(response.getJSONObject(i)));
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: Events: ", jse);
                        }
                    }
                    Collections.sort(events, new Comparator<Event>() {
                        @Override
                        public int compare(Event e1, Event e2) {
                            return e1.getCreatedAt() > e2.getCreatedAt() ? 1 : 0;
                        }
                    });
                    loader.listLoadComplete(events);
                }

                @Override
                public void onError(ANError anError) {
                    loader.listLoadError(parseError(anError));
                }
            });
        }

    }

    public void loadMilestone(@Nullable final ItemLoader<Milestone> loader, String repoFullName, int number) {
        AndroidNetworking
                .get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_MILESTONES + "/" + number)
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

    public void loadMilestones(@Nullable final ListLoader<Milestone> loader, String repoFullName, State state, int page) {
        final ANRequest req =
                AndroidNetworking
                        .get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_MILESTONES + appendPage(
                                page))
                        .addHeaders(API_AUTH_HEADERS)
                        .addPathParameter("state", state.toString().toLowerCase())
                        .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    final List<Milestone> milestones = new ArrayList<>(response.length());
                    for(int i = 0; i < response.length(); i++) {
                        try {
                            milestones.add(Milestone.parse(response.getJSONObject(i)));
                        } catch(JSONException ignored) {
                        }
                    }
                    loader.listLoadComplete(milestones);
                }

                @Override
                public void onError(ANError anError) {
                    loader.listLoadError(parseError(anError));
                }
            });
        }
    }

    public void loadFollowers(@NonNull final ListLoader<User> loader, String user, int page) {
        AndroidNetworking
                .get(GIT_BASE + SEGMENT_USERS + "/" + user + SEGMENT_FOLLOWERS + appendPage(page))
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final List<User> users = new ArrayList<>(response.length());
                        for(int i = 0; i < response.length(); i++) {
                            try {
                                users.add(User.parse(response.getJSONObject(i)));
                            } catch(JSONException ignored) {
                            }
                        }
                        loader.listLoadComplete(users);
                    }

                    @Override
                    public void onError(ANError anError) {
                        loader.listLoadError(parseError(anError));
                    }
                });
    }

    public void loadFollowing(@NonNull final ListLoader<User> loader, String user, int page) {
        AndroidNetworking
                .get(GIT_BASE + SEGMENT_USERS + "/" + user + SEGMENT_FOLLOWING + appendPage(page))
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final List<User> users = new ArrayList<>(response.length());
                        for(int i = 0; i < response.length(); i++) {
                            try {
                                users.add(User.parse(response.getJSONObject(i)));
                            } catch(JSONException ignored) {
                            }
                        }
                        loader.listLoadComplete(users);
                    }

                    @Override
                    public void onError(ANError anError) {
                        loader.listLoadError(parseError(anError));
                    }
                });
    }

    public void checkAccessToRepository(@Nullable final ItemLoader<Repository.AccessLevel> listener, String login, String repoFullName) {
        AndroidNetworking
                .get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_COLLABORATORS + "/" + login + SEGMENT_PERMISSION)
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
                        Log.i(TAG,
                                "onError: Access check: " + anError.getErrorCode() + " " + anError
                                        .getErrorBody()
                        );
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

    public void checkIfCollaborator(@Nullable final ItemLoader<Repository.AccessLevel> listener, String login, String repoFullName) {
        AndroidNetworking
                .get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_COLLABORATORS + "/" + login)
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

    public void checkIfStarred(@Nullable final ItemLoader<Boolean> listener, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_USER + SEGMENT_STARRED + "/" + repoFullName)
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

    public void checkIfWatched(@Nullable final ItemLoader<Boolean> listener, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_SUBSCRIPTION)
                         .addHeaders(API_AUTH_HEADERS)
                         .setPriority(Priority.IMMEDIATE)
                         .build()
                         .getAsJSONObject(new JSONObjectRequestListener() {
                             @Override
                             public void onResponse(JSONObject response) {
                                 Log.i(TAG,
                                         "onResponse: Subscription check " + response.toString()
                                 );
                                 try {
                                     if(response.has("subscribed")) {
                                         if(listener != null)
                                             listener.loadComplete(
                                                     response.getBoolean("subscribed"));
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

    public void renderMarkDown(@Nullable final ItemLoader<String> loader, String markdown) {
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

    public void renderMarkDown(@Nullable final ItemLoader<String> loader, String markdown, String context) {
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

    public void loadLicenseBody(@NonNull final ItemLoader<String> loader, @NonNull String path) {
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

    public void loadCommit(@NonNull final ItemLoader<Commit> loader, String repoFullName, String sha) {
        AndroidNetworking
                .get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_COMMITS + "/" + sha)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        loader.loadComplete(new Commit(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        loader.loadError(parseError(anError));
                    }
                });
    }

    public void loadCommits(@NonNull final ListLoader<Commit> loader, String repoFullName, @Nullable String branch, int page) {
        AndroidNetworking
                .get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_COMMITS + appendPage(
                        page) + (branch == null ? "" : "?sha=" + branch))
                .addHeaders(API_AUTH_HEADERS)
                .getResponseOnlyFromNetwork()
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final List<Commit> commits = new ArrayList<>(response.length());
                        try {
                            for(int i = 0; i < response.length(); i++) {
                                commits.add(new Commit(response.getJSONObject(i)));
                            }
                            loader.listLoadComplete(commits);
                        } catch(JSONException jse) {
                            loader.listLoadError(APIError.UNPROCESSABLE);
                        }

                    }

                    @Override
                    public void onError(ANError anError) {
                        loader.listLoadError(parseError(anError));
                    }
                });
    }

    public void loadCommitComments(@NonNull final ListLoader<Comment> loader, String repoFullName, String sha, int page) {
        AndroidNetworking
                .get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_COMMITS + "/" + sha + SEGMENT_COMMENTS + appendPage(
                        page))
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final List<Comment> comments = new ArrayList<>();
                        try {
                            for(int i = 0; i < response.length(); i++) {
                                comments.add(Comment.parse(response.getJSONObject(i)));
                            }
                            loader.listLoadComplete(comments);
                        } catch(JSONException jse) {
                            loader.listLoadError(APIError.UNPROCESSABLE);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        loader.listLoadError(parseError(anError));
                    }
                });
    }

    public void loadCommitStatuses(@NonNull final ItemLoader<CompleteStatus> loader, final String repoFullName, String sha) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_COMMITS + "/" + sha + SEGMENT_STATUS)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        loader.loadComplete(new CompleteStatus(response));

                    }

                    @Override
                    public void onError(ANError anError) {
                        loader.loadError(parseError(anError));
                    }
                });
    }

    private static String appendPage(int page) {
        return page > 1 ? "?page=" + page : "";
    }

    public interface ListLoader<T> {

        void listLoadComplete(List<T> data);

        void listLoadError(APIError error);

    }

    public interface ItemLoader<T> {

        void loadComplete(T data);

        void loadError(APIError error);

    }

}
