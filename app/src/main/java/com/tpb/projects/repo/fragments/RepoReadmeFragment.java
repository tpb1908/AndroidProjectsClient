package com.tpb.projects.repo.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mittsu.markedview.MarkedView;
import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Loader;
import com.tpb.github.data.models.Repository;
import com.tpb.projects.R;
import com.tpb.mdtext.Markdown;
import com.tpb.projects.repo.RepoActivity;
import com.tpb.projects.util.fab.FloatingActionButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by theo on 26/03/17.
 */

public class RepoReadmeFragment extends RepoFragment {

    private Unbinder unbinder;

    private Loader mLoader;

    @BindView(R.id.repo_readme_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.repo_readme) MarkedView mReadme;

    public static RepoReadmeFragment newInstance(RepoActivity parent) {
        final RepoReadmeFragment rrf = new RepoReadmeFragment();
        rrf.mParent = parent;
        return rrf;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_repo_readme, container, false);
        unbinder = ButterKnife.bind(this, view);
        mAreViewsValid = true;
        mRefresher.setRefreshing(true);
        mLoader = new Loader(getContext());
        mRefresher.setOnRefreshListener(() -> {
            new Loader(getContext()).loadRepository(new Loader.ItemLoader<Repository>() {
                @Override
                public void loadComplete(Repository data) {
                    repoLoaded(data);
                }

                @Override
                public void loadError(APIHandler.APIError error) {

                }
            }, mRepo.getFullName());
        });
        if(mRepo != null) repoLoaded(mRepo);
        mReadme.enableDarkTheme();
        mParent.notifyFragmentViewCreated(this);
        return view;
    }

    @Override
    public void repoLoaded(Repository repo) {
        mRepo = repo;
        if(!mAreViewsValid) return;
        mLoader.loadReadMe(new Loader.ItemLoader<String>() {
            @Override
            public void loadComplete(String data) {
                mLoader.renderMarkDown(new Loader.ItemLoader<String>() {
                    @Override
                    public void loadComplete(String data) {
                        if(!mAreViewsValid) return;
                        mRefresher.setRefreshing(false);
                        mReadme.setVisibility(View.VISIBLE);
                        mReadme.setMarkdown(Markdown.fixRelativeLinks(data, mRepo.getFullName()));
                        mReadme.reload();
                    }

                    @Override
                    public void loadError(APIHandler.APIError error) {
                        Toast.makeText(getContext(), R.string.error_rendering_readme,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }, data, mRepo.getDescription());
            }

            @Override
            public void loadError(APIHandler.APIError error) {
                if(!mAreViewsValid) return;
                mRefresher.setRefreshing(false);
                if(error == APIHandler.APIError.NOT_FOUND) {
                    Toast.makeText(getContext(), R.string.error_readme_not_found,
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
                }
            }
        }, mRepo.getFullName());
    }

    @Override
    public void handleFab(FloatingActionButton fab) {
        fab.hide(true);
    }

    @Override
    public void notifyBackPressed() {
        mReadme.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
