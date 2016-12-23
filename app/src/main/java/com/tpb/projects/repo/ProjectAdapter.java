package com.tpb.projects.repo;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.commonsware.cwac.anddown.AndDown;
import com.tpb.projects.R;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Project;
import com.tpb.projects.util.Constants;
import com.tpb.projects.util.Data;
import com.tpb.projects.views.AnimatingRecycler;

import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTextView;

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
    private AnimatingRecycler mRecycler;
    private boolean canAccessRepo = false;
    private AndDown md = new AndDown();

    public ProjectAdapter(ProjectEditor editor, AnimatingRecycler recycler) {
        mEditor = editor;
        mRecycler = recycler;
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
        if(!Constants.JSON_NULL.equals(mProjects.get(position).getBody())) {
            holder.mBody.setVisibility(View.VISIBLE);
            holder.mBody.setHtml(
                    md.markdownToHtml(
                            mProjects.get(holder.getAdapterPosition()).getBody()
                    ),
                    new HtmlHttpImageGetter(holder.mBody)
            );
        }
    }

    void enableRepoAccess() {
        canAccessRepo = true;
        if(mProjects.size() > 0) notifyDataSetChanged();
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
        mRecycler.enableAnimation();
    }

    void clearProjects() {
        mProjects.clear();
        notifyDataSetChanged();
        mRecycler.enableAnimation();
    }

    void addProject(Project project) {
        mProjects.add(0, project);
        notifyItemInserted(0);
    }

    void updateProject(Project project) {
        final int pos = mProjects.indexOf(project);
        if(pos != -1) {
            mProjects.set(pos, project);
            notifyItemChanged(pos);
        }
        Log.i(TAG, "updateProject: At " + pos);
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.project_name) TextView mName;
        @BindView(R.id.project_last_updated) TextView mLastUpdate;
        @BindView(R.id.project_body) HtmlTextView mBody;

        ProjectViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.findViewById(R.id.project_edit_button).setOnClickListener((v) -> mEditor.editProject(mProjects.get(getAdapterPosition())));
            view.findViewById(R.id.project_edit_button).setVisibility(canAccessRepo ? View.VISIBLE : View.INVISIBLE);
            view.setOnClickListener((v) -> mEditor.openProject(mProjects.get(getAdapterPosition()), mName));
        }

    }

    interface ProjectEditor {

        void openProject(Project project, View name);

        void editProject(Project project);

        void deleteProject(Project project);
    }

}
