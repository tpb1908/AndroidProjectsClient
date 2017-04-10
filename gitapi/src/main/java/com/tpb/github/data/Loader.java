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
import com.tpb.github.data.models.Gist;
import com.tpb.github.data.models.Issue;
import com.tpb.github.data.models.IssueEvent;
import com.tpb.github.data.models.Label;
import com.tpb.github.data.models.Milestone;
import com.tpb.github.data.models.Notification;
import com.tpb.github.data.models.Page;
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
    private static final String SEGMENT_PAGES = "/pages";

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
                                 if(loader != null) loader.loadComplete(User.parse(response));
                             }

                             @Override
                             public void onError(ANError anError) {
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
                    loader.loadComplete(User.parse(response));
                }

                @Override
                public void onError(ANError anError) {
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
                        loader.listLoadError(APIError.UNPROCESSABLE);
                    }
                }

                @Override
                public void onError(ANError anError) {
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
                        loader.listLoadError(APIError.UNPROCESSABLE);
                    }
                }

                @Override
                public void onError(ANError anError) {
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
                    loader.loadComplete(Repository.parse(response));
                }

                @Override
                public void onError(ANError anError) {
                    loader.loadError(parseError(anError));
                }
            });
        }
    }

    public void loadPage(@NonNull final ItemLoader<Page> loader, String fullRepoName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_PAGES)
                         .addHeaders(PAGES_API_API_AUTH_HEADERS)
                         .build()
                         .getAsJSONObject(new JSONObjectRequestListener() {
                             @Override
                             public void onResponse(JSONObject response) {
                                 loader.loadComplete(new Page(response));
                             }

                             @Override
                             public void onError(ANError anError) {
                                 loader.loadError(parseError(anError));
                             }
                         });
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

    public void loadGists(@NonNull final ListLoader<Gist> loader, int page) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_GISTS + appendPage(page))
                         .addHeaders(API_AUTH_HEADERS)
                         .build()
                         .getAsJSONArray(new JSONArrayRequestListener() {
                             @Override
                             public void onResponse(JSONArray response) {
                                 try {
                                     final List<Gist> gists = new ArrayList<>(response.length());
                                     for(int i = 0; i < response.length(); i++) {
                                         gists.add(Gist.parse(response.getJSONObject(i)));
                                     }
                                     loader.listLoadComplete(gists);
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

    public void loadGists(@NonNull final ListLoader<Gist> loader, String user, int page) {
        AndroidNetworking
                .get(GIT_BASE + SEGMENT_USERS + "/" + user + SEGMENT_GISTS + appendPage(page))
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            final List<Gist> gists = new ArrayList<>(response.length());
                            for(int i = 0; i < response.length(); i++) {
                                    gists.add(Gist.parse(response.getJSONObject(i)));
                            }
                            loader.listLoadComplete(gists);
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

    public void loadReadMe(@NonNull final ItemLoader<String> loader, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_README)
                         .addHeaders(API_AUTH_HEADERS)
                         .build()
                         .getAsJSONObject(new JSONObjectRequestListener() {
                             @Override
                             public void onResponse(JSONObject response) {
                                 try {
                                     final String readme = Util
                                             .base64Decode(response.getString(CONTENT));
                                     loader.loadComplete(readme);
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
                    try {
                        final List<User> collabs = new ArrayList<>(response.length());
                        for(int i = 0; i < response.length(); i++) {
                            collabs.add(User.parse(response.getJSONObject(i)));
                        }
                        loader.listLoadComplete(collabs);
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
                    try {
                        final List<User> collabs = new ArrayList<>(response.length());
                        for(int i = 0; i < response.length(); i++) {
                            collabs.add(User.parse(response.getJSONObject(i)));
                        }
                        loader.listLoadComplete(collabs);
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
                    try {
                        final List<Label> labels = new ArrayList<>(response.length());
                        for(int i = 0; i < response.length(); i++) {

                            labels.add(Label.parse(response.getJSONObject(i)));
                        }
                        loader.listLoadComplete(labels);
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
    }

    public void loadProject(@Nullable final ItemLoader<Project> loader, int id) {
        final ANRequest req = AndroidNetworking.get(GIT_BASE + SEGMENT_PROJECTS + "/" + id)
                                               .addHeaders(PROJECTS_API_API_AUTH_HEADERS)
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
                        .addHeaders(PROJECTS_API_API_AUTH_HEADERS)
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
                        loader.listLoadError(APIError.UNPROCESSABLE);
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
                .addHeaders(PROJECTS_API_API_AUTH_HEADERS)
                .getResponseOnlyFromNetwork()
                .build();
        if(loader == null) {
            req.prefetch();
        } else {
            req.getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    try {
                        final List<Column> columns = new ArrayList<>(response.length());
                        for(int i = 0; i < response.length(); i++) {
                            columns.add(Column.parse(response.getJSONObject(i)));
                        }
                        loader.listLoadComplete(columns);
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
    }

    public void loadCards(@NonNull final ListLoader<Card> loader, int columnId, int page) {
        AndroidNetworking
                .get(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + columnId + SEGMENT_CARDS + appendPage(
                        page))
                .addHeaders(PROJECTS_API_API_AUTH_HEADERS)
                .getResponseOnlyFromNetwork()
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            final List<Card> cards = new ArrayList<>(response.length());
                            for(int i = 0; i < response.length(); i++) {
                                cards.add(Card.parse(response.getJSONObject(i)));
                            }
                            loader.listLoadComplete(cards);
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

    public void loadIssue(@NonNull final ItemLoader<Issue> loader, String repoFullName, int issueNumber, boolean highPriority) {
        AndroidNetworking
                .get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_ISSUES + "/" + issueNumber)
                .addHeaders(API_AUTH_HEADERS)
                .setPriority(highPriority ? Priority.HIGH : Priority.MEDIUM)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        loader.loadComplete(Issue.parse(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        loader.loadError(parseError(anError));
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
                    try {
                        final List<Issue> issues = new ArrayList<>();
                        for(int i = 0; i < response.length(); i++) {
                            final Issue is = Issue.parse(response.getJSONObject(i));
                            if(!is.isClosed()) issues.add(is);

                        }
                        loader.listLoadComplete(issues);
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
                    try {
                        final List<Issue> issues = new ArrayList<>();
                        for(int i = 0; i < response.length(); i++) {
                            if(!response.getJSONObject(i).has("pull_request")) {
                                issues.add(Issue.parse(response.getJSONObject(i)));
                            }
                        }
                        loader.listLoadComplete(issues);
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
                    try {
                        final List<Comment> comments = new ArrayList<>(response.length());
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
    }

    public void loadEvents(@Nullable final ListLoader<IssueEvent> loader, final String repoFullName, int issueNumber, int page) {
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
                    try {
                        final List<IssueEvent> events = new ArrayList<>(response.length());
                        for(int i = 0; i < response.length(); i++) {
                            events.add(IssueEvent.parse(response.getJSONObject(i)));

                        }
                        Collections.sort(events, new Comparator<IssueEvent>() {
                            @Override
                            public int compare(IssueEvent e1, IssueEvent e2) {
                                return e1.getCreatedAt() > e2.getCreatedAt() ? 1 : 0;
                            }
                        });
                        loader.listLoadComplete(events);
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

    }

    public void loadMilestone(@NonNull final ItemLoader<Milestone> loader, String repoFullName, int number) {
        AndroidNetworking
                .get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_MILESTONES + "/" + number)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        loader.loadComplete(Milestone.parse(response));

                    }

                    @Override
                    public void onError(ANError anError) {
                        loader.loadError(parseError(anError));

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

    public void checkAccessToRepository(@NonNull final ItemLoader<Repository.AccessLevel> listener, String login, String repoFullName) {
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

                    @Override
                    public void onError(ANError anError) {
                        if(anError.getErrorCode() == 403) {
                            //403 Must have push access to view collaborator permission
                            listener.loadComplete(Repository.AccessLevel.NONE);
                        } else {
                            listener.loadError(parseError(anError));
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

    public void checkIfStarred(@NonNull final ItemLoader<Boolean> listener, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_USER + SEGMENT_STARRED + "/" + repoFullName)
                         .addHeaders(API_AUTH_HEADERS)
                         .setPriority(Priority.IMMEDIATE)
                         .build()
                         .getAsOkHttpResponse(new OkHttpResponseListener() {
                             @Override
                             public void onResponse(Response response) {
                                 if(response.code() == 204) {
                                     listener.loadComplete(true);
                                 } else if(response.code() == 404) {
                                     listener.loadComplete(false);
                                 }
                             }

                             @Override
                             public void onError(ANError anError) {
                                 listener.loadComplete(false);
                             }
                         });
    }

    public void checkIfWatched(@NonNull final ItemLoader<Boolean> listener, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_SUBSCRIPTION)
                         .addHeaders(API_AUTH_HEADERS)
                         .setPriority(Priority.IMMEDIATE)
                         .build()
                         .getAsJSONObject(new JSONObjectRequestListener() {
                             @Override
                             public void onResponse(JSONObject response) {
                                 try {
                                     if(response.has("subscribed")) {
                                             listener.loadComplete(
                                                     response.getBoolean("subscribed"));
                                     } else {
                                         listener.loadComplete(false);
                                     }
                                 } catch(JSONException jse) {
                                     listener.loadComplete(false);
                                 }
                             }

                             @Override
                             public void onError(ANError anError) {
                                 listener.loadComplete(false);
                             }
                         });
    }

    public void checkIfFollowing(@NonNull final ItemLoader<Boolean> listener, String user) {
        AndroidNetworking.get(GIT_BASE + SEGMENT_USER + SEGMENT_FOLLOWING + "/" + user)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        if(response.code() == 204) {
                            listener.loadComplete(true);
                        } else if(response.code() == 404) {
                            listener.loadComplete(false);
                        }

                    }
                    @Override
                    public void onError(ANError anError) {
                        if(anError.getErrorCode() == 404) {
                            listener.loadComplete(false);
                        } else {
                            listener.loadError(parseError(anError));
                        }
                    }
                });
    }

    public void followUser(@NonNull final ItemLoader<Boolean> listener, String user) {
        AndroidNetworking.put(GIT_BASE + SEGMENT_USER + SEGMENT_FOLLOWING + "/" + user)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        if(response.code() == 204) {
                            listener.loadComplete(true);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.loadError(parseError(anError));
                    }
                });
    }

    public void unfollowUser(@NonNull final ItemLoader<Boolean> listener, String user) {
        AndroidNetworking.delete(GIT_BASE + SEGMENT_USER + SEGMENT_FOLLOWING + "/" + user)
                         .addHeaders(API_AUTH_HEADERS)
                         .build()
                         .getAsOkHttpResponse(new OkHttpResponseListener() {
                             @Override
                             public void onResponse(Response response) {
                                 if(response.code() == 204) {
                                     listener.loadComplete(false);
                                 }
                             }

                             @Override
                             public void onError(ANError anError) {
                                 listener.loadError(parseError(anError));
                             }
                         });
    }

    public void renderMarkDown(@NonNull final ItemLoader<String> loader, String markdown) {
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
                                 loader.loadComplete(response);
                             }

                             @Override
                             public void onError(ANError anError) {
                                 loader.loadError(parseError(anError));
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
        AndroidNetworking
                .get(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_COMMITS + "/" + sha + SEGMENT_STATUS)
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

    public void loadNotifications(@NonNull final ListLoader<Notification> loader, long from) {
        AndroidNetworking
                .get(GIT_BASE + SEGMENT_NOTIFICATIONS + (from == 0 ? "" : "?since=" + Util
                        .toISO8061FromMilliseconds(from)))
                .addHeaders(API_AUTH_HEADERS)
                .setPriority(Priority.HIGH)
                .getResponseOnlyFromNetwork()
                .doNotCacheResponse()
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final List<Notification> notifications = new ArrayList<>();
                        try {
                            for(int i = 0; i < response.length(); i++) {
                                notifications.add(new Notification(response.getJSONObject(i)));
                            }
                            loader.listLoadComplete(notifications);
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
