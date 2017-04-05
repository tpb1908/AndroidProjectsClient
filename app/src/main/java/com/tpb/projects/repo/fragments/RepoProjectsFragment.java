package com.tpb.projects.repo.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import com.tpb.animatingrecyclerview.AnimatingRecyclerView;
import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Editor;
import com.tpb.github.data.models.Project;
import com.tpb.github.data.models.Repository;
import com.tpb.github.data.models.State;
import com.tpb.projects.R;
import com.tpb.projects.common.fab.FabHideScrollListener;
import com.tpb.projects.common.fab.FloatingActionButton;
import com.tpb.projects.editors.ProjectEditor;
import com.tpb.projects.repo.RepoActivity;
import com.tpb.projects.repo.RepoProjectsAdapter;
import com.tpb.projects.util.UI;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by theo on 25/03/17.
 */

public class RepoProjectsFragment extends RepoFragment {

    private Unbinder unbinder;

    @BindView(R.id.repo_projects_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.repo_projects_recycler) AnimatingRecyclerView mRecycler;
    private FabHideScrollListener mFabHideScrollListener;
    private RepoProjectsAdapter mAdapter;

    public static RepoProjectsFragment newInstance(RepoActivity parent) {
        final RepoProjectsFragment rpf = new RepoProjectsFragment();
        rpf.mParent = parent;
        return rpf;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_repo_projects, container, false);
        unbinder = ButterKnife.bind(this, view);
        mAdapter = new RepoProjectsAdapter(this, mRefresher);
        mRecycler.enableLineDecoration();
        mRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecycler.setAdapter(mAdapter);
        mRefresher.setOnRefreshListener(() -> mAdapter.reload());
        mAreViewsValid = true;
        if(mRepo != null) repoLoaded(mRepo);
        mParent.notifyFragmentViewCreated(this);
        return view;
    }

    @Override
    public void repoLoaded(Repository repo) {
        mRepo = repo;
        if(!mAreViewsValid) return;
        mAdapter.setRepository(repo);

    }

    @Override
    public void handleFab(FloatingActionButton fab) {
        fab.show(true);
        if(mFabHideScrollListener == null) {
            mFabHideScrollListener = new FabHideScrollListener(fab);
            mRecycler.addOnScrollListener(mFabHideScrollListener);
        }
        fab.setOnClickListener(v -> {
            final Intent i = new Intent(getContext(), ProjectEditor.class);
            UI.setViewPositionForIntent(i, fab);
            startActivityForResult(i, ProjectEditor.REQUEST_CODE_NEW_PROJECT);
        });
    }

    private void toggleProjectState(Project project) {
        mRefresher.setRefreshing(true);
        final Editor.UpdateListener<Project> listener = new Editor.UpdateListener<Project>() {
            @Override
            public void updated(Project updated) {
                mRefresher.setRefreshing(false);
                mAdapter.updateProject(updated);
            }

            @Override
            public void updateError(APIHandler.APIError error) {
                mRefresher.setRefreshing(false);
            }
        };
        if(project.getState() == State.OPEN) {
            new Editor(getContext()).closeProject(listener, project.getId());
        } else {
            new Editor(getContext()).openProject(listener, project.getId());
        }
    }

    private void deleteProject(Project project) {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_delete_project)
                .setMessage(R.string.text_delete_project_warning)
                .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                    mRefresher.setRefreshing(true);
                    new Editor(getContext()).deleteProject(
                            new Editor.DeletionListener<Project>() {
                                @Override
                                public void deleted(Project project1) {
                                    mRefresher.setRefreshing(false);
                                    mAdapter.removeProject(project1);
                                }

                                @Override
                                public void deletionError(APIHandler.APIError error) {
                                    mRefresher.setRefreshing(false);
                                }
                            }, project);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void editProject(Project project, View view) {
        final Intent i = new Intent(getContext(), ProjectEditor.class);
        i.putExtra(getString(R.string.parcel_project), project);
        UI.setViewPositionForIntent(i, view);
        startActivityForResult(i, ProjectEditor.REQUEST_CODE_EDIT_PROJECT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK) {

            if(requestCode == ProjectEditor.REQUEST_CODE_NEW_PROJECT) {
                final String name = data.getStringExtra(getString(R.string.intent_name));
                final String body = data.getStringExtra(getString(R.string.intent_markdown));
                mRefresher.setRefreshing(true);
                new Editor(getContext()).createProject(
                        new Editor.CreationListener<Project>() {
                            @Override
                            public void created(Project project) {
                                mRefresher.setRefreshing(false);
                                mAdapter.addProject(project);
                            }

                            @Override
                            public void creationError(APIHandler.APIError error) {
                                mRefresher.setRefreshing(false);
                            }
                        }, name, body, mRepo.getFullName());
            } else if(requestCode == ProjectEditor.REQUEST_CODE_EDIT_PROJECT) {
                mRefresher.setRefreshing(true);
                final int id = data.getIntExtra(getString(R.string.intent_project_number), -1);
                final String name = data.getStringExtra(getString(R.string.intent_name));
                final String body = data.getStringExtra(getString(R.string.intent_markdown));
                new Editor(getContext()).updateProject(
                        new Editor.UpdateListener<Project>() {
                            @Override
                            public void updated(Project project) {
                                mRefresher.setRefreshing(false);
                                mAdapter.updateProject(project);
                            }

                            @Override
                            public void updateError(APIHandler.APIError error) {
                                mRefresher.setRefreshing(false);
                            }
                        }, name, body, id);
            }
        }
    }

    public void showMenu(View view, Project project) {
        final PopupMenu pm = new PopupMenu(getContext(), view);
        pm.inflate(R.menu.menu_project);
        if(project.getState() == State.OPEN) {
            pm.getMenu().add(0, R.id.menu_toggle_project_state, 0, R.string.menu_close_project);
        } else {
            pm.getMenu().add(0, R.id.menu_toggle_project_state, 0, R.string.menu_reopen_project);
        }
        pm.setOnMenuItemClickListener(item -> {
            switch(item.getItemId()) {
                case R.id.menu_toggle_project_state:
                    toggleProjectState(project);
                    break;
                case R.id.menu_edit_project:
                    editProject(project, view);
                    break;
                case R.id.menu_delete_project:
                    deleteProject(project);
                    break;
            }
            return true;
        });
        pm.show();
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
