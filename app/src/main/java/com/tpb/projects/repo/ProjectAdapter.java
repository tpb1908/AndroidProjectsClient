package com.tpb.projects.repo;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.auth.models.Project;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 17/12/16.
 */

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> implements Loader.ProjectsLoader {
    private static final String TAG = ProjectAdapter.class.getSimpleName();

    private Project[] mProjects = new Project[0];

    @Override
    public ProjectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ProjectViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_project, parent, false));
    }

    @Override
    public void onBindViewHolder(ProjectViewHolder holder, int position) {
        holder.mName.setText(mProjects[position].getName());
        //TODO last update
    }

    @Override
    public int getItemCount() {
        return mProjects.length;
    }

    @Override
    public void projectsLoaded(Project[] projects) {
        Log.i(TAG, "projectsLoaded: " + Arrays.toString(projects));
        mProjects = projects;
        notifyDataSetChanged();
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.project_name) TextView mName;
        @BindView(R.id.project_last_updated) TextView mLastUpdate;

        public ProjectViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

    }

}
