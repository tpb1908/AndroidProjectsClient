package com.tpb.projects.repo.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.tpb.animatingrecyclerview.AnimatingRecyclerView;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Comment;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.data.models.State;
import com.tpb.projects.data.models.User;
import com.tpb.projects.editors.CommentEditor;
import com.tpb.projects.editors.IssueEditor;
import com.tpb.projects.editors.MultiChoiceDialog;
import com.tpb.projects.repo.RepoIssuesAdapter;
import com.tpb.projects.util.UI;
import com.tpb.projects.util.fab.FabHideScrollListener;
import com.tpb.projects.util.fab.FloatingActionButton;

import org.sufficientlysecure.htmltext.htmltextview.HtmlTextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static android.content.ContentValues.TAG;
import static com.tpb.projects.data.models.State.ALL;
import static com.tpb.projects.data.models.State.CLOSED;
import static com.tpb.projects.data.models.State.OPEN;

/**
 * Created by theo on 25/03/17.
 */

public class RepoIssuesFragment extends RepoFragment {

    private Unbinder unbinder;

    @BindView(R.id.repo_issues_recycler) AnimatingRecyclerView mRecyclerView;
    private FabHideScrollListener mFabHideScrollListener;
    @BindView(R.id.repo_issues_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.issues_search_view) SearchView mSearchView;
    private RepoIssuesAdapter mAdapter;

    private State mFilter = State.OPEN;
    private String mAssigneeFilter;
    private final ArrayList<String> mLabelsFilter = new ArrayList<>();

    private Editor mEditor;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_repo_issues, container, false);
        unbinder = ButterKnife.bind(this, view);
        mEditor = new Editor(getContext());
        mAdapter = new RepoIssuesAdapter(this, mRefresher);
        final LinearLayoutManager manager = new LinearLayoutManager(getContext());
        mRecyclerView.enableLineDecoration();
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(manager.findFirstVisibleItemPosition() + 20 > manager.getItemCount()) {
                    mAdapter.notifyBottomReached();
                }
            }
        });
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mAdapter.search(newText);
                return false;
            }
        });
        mSearchView.setOnCloseListener(() -> {
            mAdapter.closeSearch();
            return false;
        });
        mAreViewsValid = true;
        if(mRepo != null) repoLoaded(mRepo);
        return view;
    }

    @Override
    public void repoLoaded(Repository repo) {
        mRepo = repo;
        if(!mAreViewsValid) return;
        mAdapter.setRepo(repo);
    }

    @Override
    public void handleFab(FloatingActionButton fab) {
        fab.show(true);
        fab.setOnClickListener(v -> {
            final Intent intent = new Intent(getContext(), IssueEditor.class);
            intent.putExtra(getString(R.string.intent_repo), mRepo.getFullName());
            UI.setViewPositionForIntent(intent, fab);
            startActivityForResult(intent, IssueEditor.REQUEST_CODE_NEW_ISSUE);
        });
        if(mFabHideScrollListener == null) {
            mFabHideScrollListener = new FabHideScrollListener(fab);
            mRecyclerView.addOnScrollListener(mFabHideScrollListener);
        }
    }

    @OnClick(R.id.issues_filter_button)
    void filter(View v) {
        final PopupMenu menu = new PopupMenu(getContext(), v);
        menu.inflate(R.menu.menu_issues_filter);
        switch(mFilter) {
            case ALL:
                menu.getMenu().getItem(2).setChecked(true);
                break;
            case OPEN:
                menu.getMenu().getItem(0).setChecked(true);
                break;
            case CLOSED:
                menu.getMenu().getItem(1).setChecked(true);
                break;
        }
        menu.setOnMenuItemClickListener(menuItem -> {
            switch(menuItem.getItemId()) {

                case R.id.menu_filter_assignees:
                    showAssigneesDialog();
                    break;
                case R.id.menu_filter_labels:
                    showLabelsDialog();
                    break;
                case R.id.menu_filter_all:
                    mFilter = ALL;
                    refresh();
                    break;
                case R.id.menu_filter_closed:
                    mFilter = CLOSED;
                    refresh();
                    break;
                case R.id.menu_filter_open:
                    mFilter = OPEN;
                    refresh();
                    break;
            }
            return false;
        });
        menu.show();
    }

    private void refresh() {
        mAdapter.applyFilter(mFilter, mAssigneeFilter, mLabelsFilter);
    }

    private void showLabelsDialog() {
        final ProgressDialog pd = new ProgressDialog(getContext());
        pd.setTitle(R.string.text_loading_labels);
        pd.setCancelable(false);
        pd.show();
        new Loader(getContext()).loadLabels(new Loader.GITModelsLoader<Label>() {
            @Override
            public void loadComplete(Label[] labels) {
                final MultiChoiceDialog mcd = new MultiChoiceDialog();

                final Bundle b = new Bundle();
                b.putInt(getString(R.string.intent_title_res), R.string.title_choose_labels);
                mcd.setArguments(b);

                final String[] labelTexts = new String[labels.length];
                final int[] colors = new int[labels.length];
                final boolean[] choices = new boolean[labels.length];
                for(int i = 0; i < labels.length; i++) {
                    labelTexts[i] = labels[i].getName();
                    colors[i] = labels[i].getColor();
                    choices[i] = mLabelsFilter.indexOf(labels[i].getName()) != -1;
                }


                mcd.setChoices(labelTexts, choices);
                mcd.setTextColors(colors);
                mcd.setListener(new MultiChoiceDialog.MultiChoiceDialogListener() {
                    @Override
                    public void ChoicesComplete(String[] choices, boolean[] checked) {
                        mLabelsFilter.clear();
                        for(int i = 0; i < choices.length; i++) {
                            if(checked[i]) {
                                mLabelsFilter.add(choices[i]);
                            }
                        }
                        refresh();
                    }

                    @Override
                    public void ChoicesCancelled() {

                    }
                });
                pd.dismiss();

                mcd.show(getActivity().getSupportFragmentManager(), TAG);
            }

            @Override
            public void loadError(APIHandler.APIError error) {
                Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
            }
        }, mRepo.getFullName());

    }

    private void showAssigneesDialog() {
        final ProgressDialog pd = new ProgressDialog(getContext());
        pd.setTitle(R.string.text_loading_collaborators);
        pd.setCancelable(false);
        pd.show();
        new Loader(getContext()).loadCollaborators(new Loader.GITModelsLoader<User>() {
            @Override
            public void loadComplete(User[] collaborators) {
                final String[] collabNames = new String[collaborators.length + 2];
                collabNames[0] = getString(R.string.text_assignee_all);
                collabNames[1] = getString(R.string.text_assignee_none);
                int pos = 0;
                for(int i = 2; i < collabNames.length; i++) {
                    collabNames[i] = collaborators[i - 2].getLogin();
                    if(collabNames[i].equals(mAssigneeFilter)) {
                        pos = i;
                    }
                }

                final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.title_choose_assignee);
                builder.setSingleChoiceItems(collabNames, pos, (dialogInterface, i) -> mAssigneeFilter = collabNames[i]);
                builder.setPositiveButton(R.string.action_ok, (dialogInterface, i) -> refresh());
                builder.setNegativeButton(R.string.action_cancel, null);
                builder.create().show();
                pd.dismiss();
            }

            @Override
            public void loadError(APIHandler.APIError error) {
                Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
            }
        }, mRepo.getFullName());
    }

    public void openMenu(View view, final Issue issue) {
        final PopupMenu menu = new PopupMenu(getContext(), view);
        menu.inflate(R.menu.menu_issue);
        menu.getMenu().add(0, R.id.menu_toggle_issue_state, Menu.NONE, issue.isClosed() ? R.string.menu_reopen_issue : R.string.menu_close_issue);
        menu.getMenu().add(0, R.id.menu_edit_issue, Menu.NONE, getString(R.string.menu_edit_issue));

        menu.setOnMenuItemClickListener(menuItem -> {
            switch(menuItem.getItemId()) {
                case R.id.menu_toggle_issue_state:
                    toggleIssueState(issue);
                    break;
                case R.id.menu_edit_issue:
                    editIssue(view, issue);
                    break;
            }
            return false;
        });
        menu.show();
    }

    private void editIssue(View view, Issue issue) {
        final Intent intent = new Intent(getContext(), IssueEditor.class);
        intent.putExtra(getString(R.string.intent_repo), mRepo.getFullName());
        intent.putExtra(getString(R.string.parcel_issue), issue);
        final Loader loader = new Loader(getContext());
        loader.loadLabels(null, issue.getRepoPath());
        loader.loadCollaborators(null, issue.getRepoPath());
        if(view instanceof HtmlTextView) {
            UI.setClickPositionForIntent(getActivity(), intent, ((HtmlTextView) view).getLastClickPosition());
        } else {
            UI.setViewPositionForIntent(intent, view);
        }
        startActivityForResult(intent, IssueEditor.REQUEST_CODE_EDIT_ISSUE);

    }

    private void toggleIssueState(Issue issue) {
        final Editor.GITModelUpdateListener<Issue> listener = new Editor.GITModelUpdateListener<Issue>() {
            @Override
            public void updated(Issue toggled) {
                mAdapter.updateIssue(toggled);
                mRefresher.setRefreshing(false);
            }

            @Override
            public void updateError(APIHandler.APIError error) {
                mRefresher.setRefreshing(false);
                if(error == APIHandler.APIError.NO_CONNECTION) {
                    Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
                }
            }
        };

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.title_state_change_comment);
        builder.setPositiveButton(R.string.action_ok, (dialog, which) -> {
            mRefresher.setRefreshing(true);
            final Intent i = new Intent(getContext(), CommentEditor.class);
            i.putExtra(getString(R.string.parcel_issue), issue);
            startActivityForResult(i, CommentEditor.REQUEST_CODE_COMMENT_FOR_STATE);
            if(issue.isClosed()) {
                mEditor.openIssue(listener, issue.getRepoPath(), issue.getNumber());
            } else {
                mEditor.closeIssue(listener, issue.getRepoPath(), issue.getNumber());
            }

        });
        builder.setNegativeButton(R.string.action_no, (dialog, which) -> {
            mRefresher.setRefreshing(true);
            if(issue.isClosed()) {
                mEditor.openIssue(listener, issue.getRepoPath(), issue.getNumber());
            } else {
                mEditor.closeIssue(listener, issue.getRepoPath(), issue.getNumber());
            }
        });
        builder.setNeutralButton(R.string.action_cancel, null);
        builder.create().show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == IssueEditor.RESULT_OK) {
            final String[] assignees;
            final String[] labels;
            if(data.hasExtra(getString(R.string.intent_issue_assignees))) {
                assignees = data.getStringArrayExtra(getString(R.string.intent_issue_assignees));
            } else {
                assignees = null;
            }
            if(data.hasExtra(getString(R.string.intent_issue_labels))) {
                labels = data.getStringArrayExtra(getString(R.string.intent_issue_labels));
            } else {
                labels = null;
            }
            final Issue issue = data.getParcelableExtra(getString(R.string.parcel_issue));
            if(requestCode == IssueEditor.REQUEST_CODE_NEW_ISSUE) {

                mRefresher.setRefreshing(true);
                mEditor.createIssue(new Editor.GITModelCreationListener<Issue>() {
                    @Override
                    public void created(Issue issue) {
                        mRefresher.setRefreshing(false);
                        mAdapter.addIssue(issue);
                        mRecyclerView.scrollToPosition(0);
                    }

                    @Override
                    public void creationError(APIHandler.APIError error) {
                        mRefresher.setRefreshing(false);
                    }
                }, mRepo.getFullName(), issue.getTitle(), issue.getBody(), assignees, labels);
            } else if(requestCode == IssueEditor.REQUEST_CODE_EDIT_ISSUE) {
                mRefresher.setRefreshing(true);
                mEditor.updateIssue(new Editor.GITModelUpdateListener<Issue>() {
                    int issueCreationAttempts = 0;

                    @Override
                    public void updated(Issue issue) {
                        mAdapter.updateIssue(issue);
                        mRefresher.setRefreshing(false);
                    }

                    @Override
                    public void updateError(APIHandler.APIError error) {
                        if(error == APIHandler.APIError.NO_CONNECTION) {
                            mRefresher.setRefreshing(false);
                            Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
                        } else {
                            if(issueCreationAttempts < 5) {
                                issueCreationAttempts++;
                                mEditor.updateIssue(this, mRepo.getFullName(), issue, assignees, labels);
                            } else {
                                Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
                                mRefresher.setRefreshing(false);
                            }
                        }
                    }
                }, mRepo.getFullName(), issue, assignees, labels);
            } else if(requestCode == CommentEditor.REQUEST_CODE_COMMENT_FOR_STATE) {
                final Comment comment = data.getParcelableExtra(getString(R.string.parcel_comment));
                mEditor.createComment(new Editor.GITModelCreationListener<Comment>() {
                    @Override
                    public void created(Comment comment) {
                        mRefresher.setRefreshing(false);
                    }

                    @Override
                    public void creationError(APIHandler.APIError error) {
                        mRefresher.setRefreshing(false);
                        if(error == APIHandler.APIError.NO_CONNECTION) {
                            Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
                        }
                    }
                }, issue.getRepoPath(), issue.getNumber(), comment.getBody());
            }
        }
    }

    @Override
    public void notifyBackPressed() {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

}
