package com.tpb.projects.repo.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import com.tpb.animatingrecyclerview.AnimatingRecyclerView;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.models.Project;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.data.models.State;
import com.tpb.projects.repo.RepoProjectsAdapter;
import com.tpb.projects.util.fab.FabHideScrollListener;
import com.tpb.projects.util.fab.FloatingActionButton;

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
        return view;
    }

    @Override
    public void repoLoaded(Repository repo) {
        if(!mAreViewsValid) return;;
        mAdapter.setRepository(repo);
    }

    @Override
    public void handleFab(FloatingActionButton fab) {
        fab.show(true);
        if(mFabHideScrollListener == null) {
            mFabHideScrollListener = new FabHideScrollListener(fab);
            mRecycler.addOnScrollListener(mFabHideScrollListener);
        }
    }

    private void toggleProjectState(Project project) {
        mRefresher.setRefreshing(true);
        final Editor.GITModelUpdateListener<Project> listener = new Editor.GITModelUpdateListener<Project>() {
            @Override
            public void updated(Project updated) {
                mRefresher.setRefreshing(false);
                mAdapter.updateProject(updated);
                Log.i(RepoProjectsFragment.class.getSimpleName(), "Returned project " + updated.toString());
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

    }

    private void editProject(Project project) {

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
                    editProject(project);
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
