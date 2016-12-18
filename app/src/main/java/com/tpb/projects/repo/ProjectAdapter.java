package com.tpb.projects.repo;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.auth.models.Project;
import com.tpb.projects.util.Data;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 17/12/16.
 */

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> implements Loader.ProjectsLoader {
    private static final String TAG = ProjectAdapter.class.getSimpleName();

    private ArrayList<Project> mProjects = new ArrayList<>();
    private ProjectEditor mEditor;

    public ProjectAdapter(ProjectEditor editor, RecyclerView recycler) {
        mEditor = editor;
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                editor.deleteProject(mProjects.get(viewHolder.getAdapterPosition()));
                mProjects.remove(viewHolder.getAdapterPosition());
                notifyItemRemoved(viewHolder.getAdapterPosition());
            }
        }).attachToRecyclerView(recycler);
    }

    @Override
    public ProjectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ProjectViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_project, parent, false));
    }

    @Override
    public void onBindViewHolder(ProjectViewHolder holder, int position) {
        holder.mName.setText(mProjects.get(position).getName());
        holder.mLastUpdate.setText(
                String.format(
                        holder.itemView.getContext().getString(R.string.text_last_updated),
                        Data.timeAgo(mProjects.get(position).getUpdatedAt())
                )
        );
    }

    @Override
    public int getItemCount() {
        return mProjects.size();
    }

    @Override
    public void projectsLoaded(Project[] projects) {
        Log.i(TAG, "projectsLoaded: " + Arrays.toString(projects));
        mProjects = new ArrayList<>(Arrays.asList(projects));
        notifyDataSetChanged();
    }

    void clearProjects() {
        mProjects.clear();
        notifyDataSetChanged();
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.project_name) TextView mName;
        @BindView(R.id.project_last_updated) TextView mLastUpdate;

        ProjectViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener((v) -> mEditor.editProject(mProjects.get(getAdapterPosition())));
        }

    }

    interface ProjectEditor {

        void editProject(Project project);

        void deleteProject(Project project);
    }

}
